package mechanisms;

import java.util.*;

/**
 * Re-implementation of RescueDP mechanism  
 * Q.Wang et al. 2018. Real-time and spatio-temporal crowd-sourced social network
 * data publishing with differential privacy. IEEE Trans. on Dependable and Secure
 * Computing (TDSC) 15, 4 (2018), 591–606.
 * 
 * @author b1074672
 *
 */
public class RescueDP_MD extends Mechansim{
	boolean USE_KALMAN_FILTER = true;
	/** Specifics of lambda_r calculation upon determining next sample interval. If true the version from the paper is used. false usually leads to better utility.*/
	public static boolean USE_LAMBDA_R_PAPER = true;
	
	/**Dynamic Grouping thresholds*/
	/** Dynamic Grouping to_large threshold */
	static final double tau_1 = 30.d;// if <0 mimic no grouping
	/** Dynamic Grouping is_correlated threshold */
	static final double tau_2 = 0.5d;
	/** Dynamic Grouping same_group_by_value_difference threshold */
	static final double tau_3 = 25.d;
	/** last kappa releases to include in dynamic grouping*/
	static final int kappa = 3;
	static boolean USE_DYNAMIC_GROUPING = true;
	
	/**PID-Coefficients */
	static final double K_p = 0.9d;
	static final double K_i = 0.1d;
	static final double K_d = 0.0d;

	/** Number of sampling points to include in PID error */
	final int pi=3;
	/** Some scale factor to determine the weight of the fraction of sampling error and perturbation error. 
	 * Used to determine next sample point - As in Paper experiments, set to 10, for some reason.*/
	final double theta = 10.0d;
	/** Another scale factor in (0,1) used in adaptive budget allocation. */
	final double phi = 0.2d;
	/** Maximum fraction of remaining epsilon to use */
	final double p_max = 0.6d;
	/**Maximum absolute size of a budget portion per sampling point */
	double e_max; // = 0.2;// say so, according to paper: interpreted as this.epsilon / 5.
	
	/** initial length of the sample interval for all locations. Default is 1.*/
	final int INITIAL_SAMPLE_INTERVAL;
	/** I_l in the paper */
	double[] sampleIntervalLength;
	/** time when location l (nextSamplePoint[l]) samples next */
	int nextSamplePoint[];
	
	// Kalman Filter stuff
	/** POST_ESTIMATE  */
	double[] p; //for each location
	/** ERROR_VARIANCE Modeling process as N(0,q) - shall partially mitigate Laplace noise perturbation. */
	double[] q; //for each location
	/** PROCESS_MODEL_VARIANCE  */
	double r;
	/** k  */
	double k;
	
	/** don't wanna pass it as argument all the time*/
	ArrayList<double[]> sanitized_stream;
	/** don't wanna pass it as argument all the time*/
	ArrayList<Integer>[] sampling_points;
	private double eps;

	public RescueDP_MD(){
		this(1);//set initial sample interval to 1
	}
	
	public RescueDP_MD(int initialSampleInterval){
		super();
		this.INITIAL_SAMPLE_INTERVAL = initialSampleInterval;
	}
	
	/**
	 * 
	 * @param location $j$ in the paper
	 * @return
	 */
	double feedback_error(final int location){
		int current_release = n(location);//$n$
		return feedback_error(current_release, location);
	}
	
	double feedback_error(final int sampling_point_number, final int location){
		int k_o 		= this.sampling_points[location].get(sampling_point_number);
		int k_o_before 	= this.sampling_points[location].get(sampling_point_number-1);
		double release_now 		= sanitized_stream.get(k_o)[location];			//$r^j_k_o$ or $k_o[j]$
		double release_before 	= sanitized_stream.get(k_o_before)[location];	//$r^j_k_{o-1}$ or $k_{o-1}[j]$
		return Math.abs(release_now-release_before);
	}
	
	/**
	 * Time index of last release.
	 * @return
	 */
	int n(final int location){
		return sampling_points[location].size()-1;
	}
	
	/**
	 * Value of last release. Internally calls n().
	 * @return
	 */
	double k_n(final int location){
		int n = n(location);
		return sanitized_stream.get(n)[location];
	}
	
	/**
	 * 
	 * @param location $j$ in the paper
	 * @return
	 */
	double pid_error(final int location){
		double delta_j = 0.0d;
		
		//(1) proportional gain
		double proportional_gain=K_p*feedback_error(location);
		//(2)integral gain
		double integral_gain = 0.0d;
		int n = n(location);//current release number 
		for(int o = Math.max(1, n-pi+1);o<=n;o++){// we need at least two sampling points
			integral_gain += feedback_error(o,location);
		}
		integral_gain /= Math.min(pi, n);
		integral_gain *= K_i;
		//(3)derivative gain
		int k_n = this.sampling_points[location].get(n(location));
		int k_n_before = this.sampling_points[location].get(n(location)-1);
		double derivative_gain = K_d*feedback_error(location)/(double)(k_n-k_n_before);
		
		delta_j = proportional_gain+integral_gain+derivative_gain;
		return delta_j;
	}
	
	/**
	 * Computes and sets the next sampling interval. I.e., sampleIntervalLength member and nextSamplePoint.
	 * @param location
	 * @param lambda_r - almost a lower bound for the noise in t+1.
	 */
	private void next_sampling_interval(final double lambda_r, final int location){
		double interval = this.sampleIntervalLength[location];
		double delta_j = pid_error(location);
		double expected_error = lambda_r;
		//System.out.println("delta, lambda_r "+delta_j+" "+lambda_r);
		double temp = (delta_j/expected_error)*(delta_j/expected_error);//squared fraction of approximation and perturbation error.

		double next_interval = Math.max(1.0d, interval+(theta*(1.0d-temp))); 
		sampleIntervalLength[location] = next_interval;//Note, this one remains a double...
		nextSamplePoint[location] += (int)this.sampleIntervalLength[location];//...this one is an integer
		//System.out.println(this.sampleIntervalLength+" i.e. at "+this.nextSamplePoint);
	}
	
	private double adaptive_budget_allocation(final double eps_remaining, final int location){
		final double interval = sampleIntervalLength[location];
		double p = phi*Math.log(interval+1.0d); //portion of eps_r used now
		p = Math.min(p, p_max);//bound fraction of e_rm to use
		double e_i = Math.min(p*eps_remaining, e_max);//bound absolute size of portion
		return e_i;
	}
	double[][] lambda;
	/**
	 * inits: (all class members)
	 * - sanitized_stream also entirely pre-allocating the required memory.
	 * - sampling_points
	 * - sampleIntervalLength
	 * - (if used) KalmanFilter
	 * @param length
	 * @param dim
	 * @param org_stream
	 */
	@SuppressWarnings("unchecked")
	void init(final int length, final int dim, final ArrayList<double[]> org_stream){

		this.sanitized_stream = new ArrayList<double[]>(length);
		for(int t=0;t<length;t++){
			sanitized_stream.add(t, new double[dim]);
		}
		this.sampling_points = new ArrayList[dim];
		for(int d=0;d<dim;d++){
			sampling_points[d] = new ArrayList<Integer>();
		}
		if(USE_KALMAN_FILTER){
			initKalmanFilter(org_stream);
		}
		this.sampleIntervalLength = new double[dim];
		Arrays.fill(sampleIntervalLength, this.INITIAL_SAMPLE_INTERVAL);
		
		this.nextSamplePoint = new int[dim];

		this.lambda_r = new double[length][dim];
		this.lambda = new double[length][dim];

	}
	
	/**
	 * Called if we use dynamic_grouping.
	 * @param t
	 * @param sample_me
	 * @param values
	 * @param used_budget
	 * @param used_budget_in_this_window
	 * @param releases
	 */
	void performGroupedRelases(final int t, final boolean[] sample_me, final double[] values, final double[][] used_budget
			, final double[] used_budget_in_this_window, final double[] releases){
		final int dim = sample_me.length;
		//Step 1 and 2
		ArrayList<DynamicGrouingLocation> group_me = new ArrayList<DynamicGrouingLocation>();

		for(int location=0;location<dim;location++){
			if(sample_me[location]){// we sample
				DynamicGrouingLocation current = new DynamicGrouingLocation(location);//also computes the prediction
				if(current.prediction>tau_1){// To big, publish individually.
					publish(t,location,used_budget,values,used_budget_in_this_window, releases);
				}else if(current.last_kappa_releases.length<kappa){
					//XXX - location has not published at least kappa releases, i.e., we cannot say whether correlation holds
					publish(t,location,used_budget,values, used_budget_in_this_window, releases);
				}else{
					group_me.add(current);//remember me for grouping
				}
			}
		}
		//Step 3
		Collections.sort(group_me, new Comparator<DynamicGrouingLocation>() {
			@Override
			public int compare(DynamicGrouingLocation o1, DynamicGrouingLocation o2) {
				if(o1.prediction>o2.prediction){
					return 1;
				}else if(o1.prediction<o2.prediction){
					return -1;
				}else{//o1.prediction==o2.prediction
					return 0;
				}
			}
		});
		
		//Step 4: Group by some kind of pivot mechanism
		while(!group_me.isEmpty()){
			// new group
			ArrayList<DynamicGrouingLocation> g = new ArrayList<DynamicGrouingLocation>();
			DynamicGrouingLocation groupPivot = group_me.remove(0);
			g.add(groupPivot);
			boolean group_published = false;// last group not published
			/** Group weight */
			double sum_g = groupPivot.prediction;
			
			/****** pre-compute some Pearson correlation value of the group pivot *****/
			double[] x_arr = groupPivot.last_kappa_releases;
			final int length = x_arr.length;
			
			double x_mean = 0.0d;
			for(int i=0;i<length;i++){
				x_mean += x_arr[i];
			}
			x_mean /= (double)length;
			
			double var_x = 0.0d;
			for(int i=0;i<length;i++){
				double x = x_arr[i];
				var_x += (x-x_mean)*(x-x_mean);
			}
			//pseudo output of in-lined function
			final double pivot_mean = x_mean;
			final double pivot_variance = var_x;
			/****** pre-compute some Pearson correlation value of the group pivot *****/
			
			// Step 5
			for(int i=0;i<group_me.size();i++){
				DynamicGrouingLocation l = group_me.get(i);
				final double difference = Math.abs(groupPivot.prediction-l.prediction);
				if(difference<tau_3 && sum_g<tau_1){
					//Step 6
					double similarity = pearson_correlation(groupPivot,l, pivot_mean, pivot_variance);
					if(similarity>tau_2){
						sum_g+=l.prediction;
						g.add(l);
						group_me.remove(l);
						i--;//because I deleted one item.
					}//else skip
				}else{
					//Step 7: first location with: (1) to large prediction (recap. ordered by prediction) or (2) group full
					// Group is done. Let's publish.
					if(g.size()==1){// We can save some computation, if group contains only one location.
						publish(t, groupPivot.location, used_budget, values, used_budget_in_this_window, releases);
					}else{
						publish(t, g, used_budget, values, used_budget_in_this_window, releases);
					}
					//Do not need to look for further location to assign to this group, since we are done.
					group_published = true;
					break;
				}
			}
			if(!group_published){//last group may not reach step 7, so we publish it manually.
				if(g.size()==1){
					publish(t, groupPivot.location, used_budget, values, used_budget_in_this_window, releases);
				}else{
					publish(t, g, used_budget, values, used_budget_in_this_window, releases);
				}
			}
		}// End Step 4.	
	}
	
	@Override
	public ArrayList<double[]> run(ArrayList<double[]> org_stream) {

		this.e_max = this.epsilon / 5.0d;
		final int length = org_stream.size();
		final int dim = org_stream.get(0).length;
		init(length, dim, org_stream);
		
		// for debug
		//final double[] eps_debug = new double[length];
		//final double[] debug_eps_rm = new double[length];
		//final double[] release_debug = new double[length];
		
		int next = Math.max(1, (int) INITIAL_SAMPLE_INTERVAL);
		Arrays.fill(nextSamplePoint, next);
		 boolean[] sample_me = new boolean[dim];
		Arrays.fill(sample_me, true);// all sampled at t=0
		
		/** [time][location] */
		 double[][] used_budget = new double[length][dim];
		for(int t=0;t<length;t++){
			used_budget[t] = new double[dim];
		}
		 double[] used_budget_in_this_window = new double[dim];
		// init with zero, Java does that automatically.
		
		/*******  perform first release [BEGIN] ************/
		int t=0;
		// (1) -> get org data
		double[] values = org_stream.get(t);
		// (2) -> all regions publish at t=0
		for(ArrayList<Integer> list : sampling_points){list.add(t);}
		// (3) -> no grouping until t>2 -> Need at least two releases to compute correlation of locations.
		// (4) -> budget in first step all the same
		double eps_rm = this.epsilon;
		//debug_eps_rm[t] = eps_rm;
		//By definition the same for all locations at t=0.
		eps = adaptive_budget_allocation(eps_rm, 0);
		Arrays.fill(used_budget[t], eps);
		Arrays.fill(used_budget_in_this_window, eps);
		//eps_debug[t] = eps;
		
		// (5) add noise
		double[] releases = sanitized_stream.get(t);
		for(int d=0;d<dim;d++){
			if(sample_me[d]){//Tautology
				double val = values[d];
				releases[d] = sanitize(val, lambda(eps));
				//release_debug[t] = releases[d];
			}
		}
		
		// (6) Filtering not in first release, need at least one prior release
		// (7) new sampling interval -> no need here, is controlled by initial sampling interval member
		/*******  perform first release [END] ************/
		
		for(t=1;t<length;t++){
			//if(t%100==0)System.out.println(t);
			if(t>=w){//budget recovery
				double[] not_relevant_eps = used_budget[t-w]; 
				for(int d=0;d<dim;d++){
					used_budget_in_this_window[d] -= not_relevant_eps[d];//this one is not relevant anymore
				}
			}
			
			// (1) -> get org data
			releases = sanitized_stream.get(t);
			values 	 = org_stream.get(t);
			// (2) -> Whom to publish?
			sample_me(sample_me,t,nextSamplePoint);
			
			//perform_sample(values,releases,sample_me);
			if(USE_DYNAMIC_GROUPING && t>2){// I need at least two published values, otherwise Pearson correlation is not defined
				performGroupedRelases(t, sample_me, values, used_budget, used_budget_in_this_window, releases);
			}else{
				for(int d=0;d<dim;d++){
					if(sample_me[d]){// we need to publish now
						publish(t, d, used_budget, values, used_budget_in_this_window, releases);
					}
				}
			}
			
			{//for all remaining locations, publish prior release, and update filter.	
				for(int location=0;location<dim;location++){
					if(sample_me[location]==false){// we must NOT sample
						releases[location]=sanitized_stream.get(t-1)[location];
						//release_debug[t] = releases[location];
						if(USE_KALMAN_FILTER){
							p[location] = p[location]+q[location];	//one needs to execute the predict part (according to FAST Paper)
							//k = p / (p + r); 
							//p = p * (1.0d - k);
						}
					}
				}
			}
		}
		
		//System.out.println("w="+w+" eps\t"+outTSV(eps_debug));
		//System.out.println("w="+w+" e_rm\t"+outTSV(debug_eps_rm));
		//System.out.println("w="+w+" release\t"+outTSV(release_debug));

		/*System.out.println("eps="+ epsilon +"\t"+outTSV(sampling_points[0]));
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(Experiment.RESLT_DIR + "/RDPSampling-"+epsilon + ".csv", true));
			writer.write("" + sampling_points[0].size()+"\n");

			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}*/
		/*BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(Experiment.RESLT_DIR + "/RDPBugdet-"+epsilon + ".csv", true));
			writer.write("" + Arrays.toString(used_budget[0]) +"\n");

			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}*/
	/*	System.out.println("\t" + Mechansim.outTSVFirstDimension(this.lambda_r));

		System.out.println(Mechansim.outTSVFirstDimension(this.lambda));*/

		// lets help the GC ..
		this.sampling_points = null;
		this.nextSamplePoint = null;
		this.p = null;
		sample_me = null;
		used_budget = null;
		used_budget_in_this_window = null;
		System.gc();
		return sanitized_stream;
	}

	double[][] lambda_r; //debugging


	/**
	 * Publishes an entire group of locations including dynamic grouping optimization.
	 * @param t
	 * @param g
	 * @param used_budget
	 * @param values
	 * @param used_budget_in_this_window
	 * @param releases
	 */
	void publish(final int t, final ArrayList<DynamicGrouingLocation> g, final double[][] used_budget, 
			final double[] values, final double[] used_budget_in_this_window, final double[] releases
	) {
		final double[] used_budget_t = used_budget[t]; 	
		double sum_statisitics = 0.0d;
		double min_eps = this.e_max;//pretend spending max epsilon to find group with min eps. 
		
		//for each location: compute remaining budget and compute the epsilon it wants to spend
		for(DynamicGrouingLocation l : g){
			final int d = l.location;
				
			double val = values[d];
			sum_statisitics+= val;
			sampling_points[d].add(t);
			
			// budget
			double eps_rm = this.epsilon-used_budget_in_this_window[d];//That is what we could spend.
			double eps = adaptive_budget_allocation(eps_rm, d);//This is what we want to spend: Recap. we spend epsilon of location with min(eps).
			if(eps<min_eps){
				min_eps = eps;//min() - location that wants to spend min eps.
			}
		}
		
		// (5) add noise
		double group_release = sanitize(sum_statisitics, lambda(min_eps));
		//XXX - essential trick, as grouping is performed on private data, group size is no private info, i.e., this is a post processing.
		group_release /= g.size();
		group_release = truncate(group_release);
		
		for(DynamicGrouingLocation l : g){
			final int d = l.location;
			double my_release = group_release;
			//(6) This is the trick of filtering - we do not really release the same value per location in group.
			if(USE_KALMAN_FILTER){
				my_release=applyKalmanFilter(sanitized_stream.get(t-1)[d], releases[d], d);
			}
			releases[d] = my_release;
			used_budget_t[d] = min_eps;					// surely only the budget we really spent.
			used_budget_in_this_window[d] += min_eps;	// Dito.
			
			// (7) new sampling interval
			final double lambda_r = lambda_r(used_budget_in_this_window, t, used_budget, d);
			next_sampling_interval(lambda_r, d);
			this.lambda_r[t][d] = lambda_r;

		}
	}


	//static final double NOT_CORRELATED = 0.0d;
	/*****************************************
	 * Returns Pearson correlation of last kappa releases.
	 * @param groupPivot
	 * @param l - location
	 * @param pivot_mean - pre-computed mean of the group pivot
	 * @param pivot_variance - pre-computed variance of the group pivot
	 * @return
	 ****************************************/
	double pearson_correlation(DynamicGrouingLocation groupPivot, DynamicGrouingLocation l, final double pivot_mean, final double pivot_variance) {
		final double[] x_arr = groupPivot.last_kappa_releases;
		final double[] y_arr = l.last_kappa_releases;
		 
		final int length = x_arr.length;
		final double x_mean = pivot_mean;
		double y_mean = 0.0d;
		for(int i=0;i<length;i++){
			y_mean += y_arr[i];
		}	
		y_mean /= (double)length;

		double cov = 0.0d;
		final double var_x = pivot_variance;
		double var_y = 0.0d;
		
		for(int i=0;i<length;i++){
			double x = x_arr[i];
			double y = y_arr[i];
			cov += (x-x_mean)*(y-y_mean);
			var_y += (y-y_mean)*(y-y_mean);
		}
		
		double correlation_1 = cov / (Math.sqrt(var_x*var_y));
		//double correlation = this.pc.correlation(groupPivot.last_kappa_releases, l.last_kappa_releases);

		return correlation_1;
	}

	/**
	 * Publishes a single location or group containing only one location at time t. 
	 * @param t - time
	 * @param d - location
	 * @param used_budget
	 * @param values - vector of values
	 * @param used_budget_in_this_window
	 * @param releases
	 */
	void publish(final int t, final int d
			, final double[][] used_budget, final double[] values
			, final double[] used_budget_in_this_window, final double[] releases
	){
		final double[] used_budget_t = used_budget[t]; 	
		final double val = values[d];
		sampling_points[d].add(t);
		
		// (3) -> no grouping
		// (4) -> budget
		final double eps_rm = this.epsilon-used_budget_in_this_window[d];
		final double eps = adaptive_budget_allocation(eps_rm, d);
		this.lambda[t][d] = lambda(eps);
		// (5) add noise
		releases[d] = sanitize(val, lambda(eps));
		used_budget_t[d] = eps;
		used_budget_in_this_window[d] += eps;
		//debug_eps_rm[t] = eps_rm;

		// (6) Filtering 
		if(USE_KALMAN_FILTER){
			releases[d]=applyKalmanFilter(sanitized_stream.get(t-1)[d], releases[d], d);
		}
		//release_debug[t] = releases[d];
		//eps_debug[t] = used_budget_t[d];
		
		// (7) new sampling interval
		final double lambda_r = lambda_r(used_budget_in_this_window, t, used_budget, d);
		next_sampling_interval(lambda_r, d);
		this.lambda_r[t][d] = lambda_r;

	}

	void sample_me(boolean[] sample_me, int time, int[] nextSamplePoints) {
		final int dim = nextSamplePoints.length;
		for(int d=0;d<dim;d++){
			sample_me[d] = (time == nextSamplePoints[d]);
		}
	}

	void initKalmanFilter(final ArrayList<double[]> stream) {
		//init Kalman Filter - XXX don't ask me why like this all the mechanisms just do it like this
		final int dim = stream.get(0).length;
		final int length = stream.size();
		
		this.p = new double[dim];
		Arrays.fill(p, 100.0d);
		this.q = new double[dim];
		double[] temp = new double[length]; 
		for(int d=0;d<dim;d++){
			for(int t=0;t<length;t++){
				temp[t] = stream.get(t)[d];
			}
			this.q[d] = variance(temp);
		}
				
		this.r = sensitivity * sensitivity * w * w / (epsilon*epsilon);
		this.k = 0.0d;
	}

	static final double variance(final double[] data) {
		// The mean average
		double mean = 0.0;
		for (double val : data) {
		        mean += val;
		}
		mean /= data.length;

		// The variance
		double variance=0.0d;
		for (double val : data) {
		    variance += (val- mean) * (val - mean);
		}
		variance /= data.length;
		return variance;
	}

	/**
	 * Algo 3 in the paper
	 * @param last_release x_{j|i}
	 * @param noisy_observation
	 * @return
	 */
	double applyKalmanFilter(final double last_release, final double noisy_observation, final int location) {
		// Prediction
		p[location] = p[location]+q[location]; // (2) add to old estimate assumed to be from N(0,q) - approximates measurement noise
		
		// Correction
		k = p[location] / (p[location] + r); // (3)
		//Correction
		double posterior_estimation = last_release + k * (noisy_observation-last_release);
        p[location] = p[location] * (1.0d - k);
		if(TRUNCATE){
			posterior_estimation = Math.max(0, Math.round(posterior_estimation));//map to query domain;
		}
        return posterior_estimation;
	}

	/**
	 * Computes at bound for the lambda in t+1. Required to determine the next sample interval.
	 */
	private double lambda_r(final double[] used_budget_in_this_window, final int t, double[][] used_budget, final int d) {
		double used_budget_in_prior_window = used_budget_in_this_window[d];
		if(t+1>=w){//budget recovery
			used_budget_in_prior_window-=used_budget[t+1-w][d];//By reference - we do not modify, just look.
		}
		double eps_remaining =this.epsilon-used_budget_in_prior_window;
		//double eps_possible = (USE_LAMBDA_R_PAPER) ? Math.min(eps_remaining, this.e_max) : adaptive_budget_allocation(eps_remaining,d);

		double eps_possible = (USE_LAMBDA_R_PAPER) ? eps_remaining: adaptive_budget_allocation(eps_remaining,d);
		return lambda(eps_possible);
	}

	@Override
	public String name() {
		/*return "RescueDPMD(g="+USE_DYNAMIC_GROUPING+"I="+this.INITIAL_SAMPLE_INTERVAL
				+ ((USE_KALMAN_FILTER)? " Kalman Filter": "")
				+ ((USE_LAMBDA_R_PAPER)? " my l_r" : "")+")";*/
		return "RescueDPMD";
	}
	
	class DynamicGrouingLocation{
		final double[] last_kappa_releases;
		final int location;
		final double prediction;
		
		DynamicGrouingLocation(final int location){
			this.location = location;
			ArrayList<Integer> my_releases = sampling_points[location];

			int from = Math.max(0, my_releases.size()-kappa);//last kappa or all releases
			int to = my_releases.size();
			List<Integer> temp = my_releases.subList(from, to);
			this.last_kappa_releases = new double[temp.size()];
			for(int i=0;i<temp.size();i++){
				int t = temp.get(i);
				double val = sanitized_stream.get(t)[location];
				this.last_kappa_releases[i]=val;
			}
			this.prediction = predict(last_kappa_releases);
		}

		DynamicGrouingLocation(final int location, final double[] last_kappa_releases){
			this.location = location;
			this.last_kappa_releases = last_kappa_releases;
			this.prediction = predict(last_kappa_releases);
		}
		
		double predict(final double[] last_kappa_releases){
			double estimate=0.0d;
			for(int o=0;o<last_kappa_releases.length;o++){
				double x_k_o = last_kappa_releases[o];
				estimate+=x_k_o;
			}
			estimate /= last_kappa_releases.length;//normalize
			return estimate;
		}
		
		public String toString(){
			return "l="+ this.location +" p="+this.prediction;
		}
	}
}
