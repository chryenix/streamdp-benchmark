package mechanisms;

import experiment.LaplaceStream;

import java.util.*;
import java.util.Map.Entry;

/**
 * Re-implementation of AdaPub proposed in
 * Adaptive Differentially Private Data Stream Publishing in Spatio-temporal Monitoring of IoT
 *
 * @author b1074672
 */
public class AdaPub extends Mechansim {


    // for debugging only!
    //static boolean USE_TRUE_MAX_COUNT = true;


    // repair 'upper limit of range of pivots'. use as maximum 0. use as maximum either a fixed value, or determine it using privacy budget.
    //static boolean USE_PERTURBED_COUNT = true;
    //static int MAX_COUNT = 10000;// used if USE_PERTURBED_COUNT = false
    //static double SHARE_EPS_FOR_MAX_COUNT = 0.1;// used if USE_PERTURBED_COUNT = true. The share for EPS_P and EPS_C is automatically reduced by half of this share.

    public static final long seed = 123456L;
    static int g = 20;//number of hash functions to use; set as in paper (20)
    static double SHARE_EPS_P = 0.8d; // as in paper
    static double SHARE_EPS_C = 0.2d; // as in paper
    static double KP = 0.9d;// as in paper
    static double KI = 0.1d;// as in paper
    static double KD = 0.0d;// as in paper
    static double GAMMA_FEEDBACK_ERROR = 1; // as in paper
    static boolean DEBUG_FORCE_NEW_GROUP_EACH_TIMESTAMP = false;
    public boolean DEBUG_ENABLE_DIMENSIONGROUPING = false;
    public boolean DEBUG_ENABLE_TIMEGROUPINGFILTER = true;
    Random rand = new Random();
    Integer[] partition_buffer;
    Cluster[] all_clusters;
    int[] newGrou;

    public AdaPub() {
        if (!DEBUG_ENABLE_DIMENSIONGROUPING) {
            System.err.println("AdaPub does not use dimension grouping");
        }
        if (!DEBUG_ENABLE_TIMEGROUPINGFILTER) {
            System.err.println("AdaPub does not use time grouping filter");
        }
    }

    @Override
    public ArrayList<double[]> run(ArrayList<double[]> org_stream) {
        newGrou = new int[org_stream.size()];

        final int length = org_stream.size();
        init(g);
        double eps_p = SHARE_EPS_P * epsilon;
        double eps_c = SHARE_EPS_C * epsilon;
        if (!DEBUG_ENABLE_TIMEGROUPINGFILTER) {
            eps_p = epsilon;
            eps_c = 0;
        }


        final double lambda_perturb = this.sensitivity / (eps_p / (double) this.w);//evenly distribute whole \epsilon to each time stamp in window
        // System.out.println("Lambda perturb: " + lambda_perturb);
        ArrayList<double[]> sanitized_stream = new ArrayList<double[]>(length);
        double[] san_t;

        //t=0 is special and not considered in the paper: I cannot use a prior release for grouping.
        san_t = sanitize(org_stream.get(0), lambda_perturb);//one time stamp sanitized as in uniform
        sanitized_stream.add(0, san_t);
        //init the clusters
        all_clusters = new Cluster[san_t.length];//one cluster per dimension
        for (int dim = 0; dim < san_t.length; dim++) {
            all_clusters[dim] = new Cluster(org_stream.size(), dim);//Also performs init with meaningful values
        }

        for (int t = 1; t < length; t++) { 
            //Group
            HashMap<Integer, ArrayList<Integer>> partions;

            if (!DEBUG_ENABLE_DIMENSIONGROUPING)
                partions = get_dummy_partition(sanitized_stream.get(t - 1), g);//provide last release as argument
            else
                partions = get_partition(sanitized_stream.get(t - 1), g);//provide last release as argument

            //Perturb individual dimensions exploiting the groups
            san_t = laplace_pertubation(partions, org_stream.get(t), lambda_perturb);
            //Smooth sanitized values
            if (DEBUG_ENABLE_TIMEGROUPINGFILTER) {
                for (int dim = 0; dim < san_t.length; dim++) {
                    all_clusters[dim].cluster(t, eps_c, org_stream, sanitized_stream, san_t);
                    san_t[dim] = all_clusters[dim].median_smoother(sanitized_stream, all_clusters[dim].current_group_begin_timestamp, t - 1, san_t[dim]);
                    if (Mechansim.TRUNCATE)
                        san_t[dim] = Mechansim.truncate(san_t[dim]);
                }
            }
            sanitized_stream.add(t, san_t);
        }

        clear_buffers();

        return sanitized_stream;
    }

    void init(final int g) {
        this.partition_buffer = new Integer[g + 1]; // use g+1 to have a dummy group for values smaller than first pivot
        //rand.setSeed(seed);//for reproducibility
    }

    void clear_buffers() {
        this.partition_buffer = null;
    }

    HashMap<Integer, ArrayList<Integer>> get_dummy_partition(final double[] last_release, int g) {
        final HashMap<Integer, ArrayList<Integer>> groups = new HashMap<>();//{group_key,{dims}}

        for (int dim = 0; dim < last_release.length; dim++) {
            ArrayList<Integer> myGroup = new ArrayList<>();
            myGroup.add(dim);
            groups.put(dim, myGroup);


        }
        return groups;
    }

    HashMap<Integer, ArrayList<Integer>> get_partition(final double[] last_release, int g) {
        final int d = last_release.length;//dimensionality of the stream
        final HashMap<Integer, ArrayList<Integer>> groups = new HashMap<>();//{group_key,{dims}}

        //(1) Dice the pivots used for grouping.
        //Note, the usage of bit vectors in the paper is overly complex. In the end, they simply map each value to the smallest pivot larger than the value itself.

        // dummy group for values <= first pivot
        partition_buffer[0] = Integer.MIN_VALUE;

        // determine range
        int max_count = 0;
		/*if (USE_PERTURBED_COUNT) {
			double lambda = this.sensitivity*this.w/(eps_h);
			if (USE_TRUE_MAX_COUNT) {
			max_count = (int) Arrays.stream(last_release).max().getAsDouble(); }
		//	else {
		//	max_count=	(int) sanitize(Arrays.stream(last_release).max().getAsDouble(), lambda);
	//		}
		} else {
			max_count = MAX_COUNT;
		}*/
        max_count = (int) Arrays.stream(last_release).max().getAsDouble();
        int min_count = (int) Arrays.stream(last_release).min().getAsDouble();
        ///
	/*	int sizePerGroup = MAX_COUNT/last_release.length;
		int init = sizePerGroup;
		for(int i=1;i<=g;i++){
			init += sizePerGroup;
			final int pivot = init ;
			//rand.nextInt(Math.max(1, max_count));// must be > 0
			partition_buffer[i] = pivot;//duplicates may occur according to the paper //-- das hier ist nicht schuld
		}*/
        //

        for (int i = 1; i <= g; i++) {
            int pivot = 0;
            if (min_count == max_count) {
                pivot = min_count;
            } else {
                pivot = rand.ints(1, min_count, Math.max(1, max_count)).sum();// must be > 0; ints() function returns only one, but needed to specify lower limit
            }
            partition_buffer[i] = pivot;//duplicates may occur according to the paper
        }
        Arrays.sort(partition_buffer, Collections.reverseOrder());//I want the pivots to be sorted. This makes determining the partition simpler.

        //(2) Use the pivots for grouping
        for (int dim = 0; dim < d; dim++) {
            final double old_release = last_release[dim];

            for (int p = 0; p < partition_buffer.length; p++) {
                final int group_key = partition_buffer[p];
                if (old_release > group_key) {//Recap: pivots are orderer. So, its the first one to big.
                    ArrayList<Integer> my_group = groups.get(group_key);
                    if (my_group == null) {//Group does not exist so far. Create it.
                        my_group = new ArrayList<Integer>();
                        groups.put(group_key, my_group);
                    }
                    my_group.add(dim);
                    break;
                }
            }
        }

        return groups;
    }

    double[] laplace_pertubation(final HashMap<Integer, ArrayList<Integer>> groups, final double[] org_values, final double lambda) {
        final double[] sant_t = new double[org_values.length];//first version of the sanitized timestamp data
        //for each partition
        for (Entry<Integer, ArrayList<Integer>> e : groups.entrySet()) {
            double sum = 0.0d;

            //(1) Apply first filter exploiting that the values in the group have almost the same value, due to correlation.
            ArrayList<Integer> group = e.getValue();
            final int group_size = group.size();
            for (int i = 0; i < group_size; i++) {
                int dim = group.get(i);
                sum += org_values[dim];//We compute a sum() query
            }
            double san_sum = sanitize(sum, lambda); //This is the trick: we add the noise to the sum meaning that we reduce the noise by 1/|p|
            san_sum /= (double) group_size;
            final double x_k_t = san_sum;//value for partition p at time t

            //(2) create sanitized release values. Those values are smoothed shortly.
            for (int i = 0; i < group_size; i++) {
                int dim = group.get(i);
                sant_t[dim] = x_k_t;
            }
        }
        return sant_t;
    }

    @Override
    public String name() {
    	String ret = "AdaPub";
    	if(DEBUG_FORCE_NEW_GROUP_EACH_TIMESTAMP) ret += " DEBUG_FORCE_NEW_GROUP_EACH_TIMESTAMP=true";
    	if(!DEBUG_ENABLE_DIMENSIONGROUPING) ret += " DEBUG_ENABLE_DIMENSIONGROUPING=false";
    	if(!DEBUG_ENABLE_TIMEGROUPINGFILTER) ret += " DEBUG_ENABLE_TIMEGROUPINGFILTER=false";
        return ret;
    }

    /**
     * Computes the deviation on original values. Thus, we need to add noise to it to use it somewhere.
     *
     * @param org_stream
     * @param t
     * @param dim
     * @param t_group_start
     * @return
     */
    public double dev(ArrayList<double[]> org_stream, int t, int dim, int t_group_start) {
        double dev = 0;
        double average_count = 0;

        for (int i = t_group_start; i <= t; i++) {//Including current value, i.e., <= t
            average_count += org_stream.get(i)[dim];
        }
        double num_values = t - t_group_start + 1;
        average_count /= num_values;

        for (int i = t_group_start; i <= t; i++) {//Including current value, i.e., <= t
            dev += Math.abs(org_stream.get(i)[dim] - average_count);
        }
        return dev;
    }

    public double feedbackError(double lastRelease, double currentPerturbedValue) {
        return Math.abs(lastRelease - currentPerturbedValue) / Math.max(currentPerturbedValue, GAMMA_FEEDBACK_ERROR);
    }

    public double pid_error(final int t, Cluster cluster, ArrayList<double[]> san_stream, double[] intermediate_san_stream_t) {

        double feedbackError = this.feedbackError(san_stream.get(t - 1)[cluster.my_dim], intermediate_san_stream_t[cluster.my_dim]);
        cluster.feedback_errors[t] = feedbackError;

        double pidError = feedbackError * KP;
        pidError += KI * cluster.feedback_error_integral(t);
        pidError += KD * (feedbackError - cluster.feedback_errors[t - 1]) / 1d; // t-(t-1) is wrong in paper. however, we devide here by 1, as we do not sample.

        return pidError;
    }

    class Cluster {
        final double[] feedback_errors;
        final int my_dim;
        int current_group_begin_timestamp;
        boolean is_closed;

        Cluster(final int num_timestamps, int dim) {
            current_group_begin_timestamp = 0;
            is_closed = false;
            feedback_errors = new double[num_timestamps];
            my_dim = dim;
        }

        //public ArrayList<Double> grouper(double t, double c_t, double lastRelease, double san_t, PegasusDataDim tempDataDim, double eps_c) {
        void cluster(int t, double eps_c, ArrayList<double[]> org_stream, ArrayList<double[]> san_stream, double[] san_stream_t) {
            double theta = 0;

            if (t == 0) {
                theta = 1.0d / epsilon; // never used; nothing to group at first timestamp
            }
            if (t > 0) {
                double delta_err_k_t = pid_error(t, this, san_stream, san_stream_t);
                theta = Math.max(1.0d, delta_err_k_t * delta_err_k_t / epsilon);//In this approach the theta is not updated but entirely recomputed each time
                //    pidError[t]=theta;
            } else {
                System.err.println("called grouper() at t=0");
            }

            if (is_closed) {
                current_group_begin_timestamp = t;//open new group
                is_closed = false;
                if (DEBUG_FORCE_NEW_GROUP_EACH_TIMESTAMP)
                    is_closed = true;
                newGrou[t] = 1;

            } else {
                newGrou[t] = 0;

                double dev = dev(org_stream, t, my_dim, current_group_begin_timestamp);
                double lamdba_dev = 2 * w / eps_c;
                double noisy_dev = Math.max(dev + LaplaceStream.nextNumber() * lamdba_dev, 0);
                noisy_dev = theta + 1;
                if (noisy_dev < theta) {
                    //Nothing to do. The feedback error is added in any case above when computing the pid_error.
                } else {
                    current_group_begin_timestamp = t;//singular value group
                    is_closed = true;
                }
            }
        }

        /**
         * Average Sum of the feedback errors [current_group_begin_timestamp,t)
         *
         * @param t
         * @return
         */
        double feedback_error_integral(int t) {
            double num_timestamps = t - current_group_begin_timestamp;//without t
            double sum = 0.0d;
            for (int i = current_group_begin_timestamp; i < t; i++) {
                sum += feedback_errors[i];
            }
            return sum / num_timestamps;
        }

        double median_smoother(ArrayList<double[]> san_stream, final int begin, final int end, double san_t) {
            ArrayList<Double> sorted_sanStream_last_group = new ArrayList<Double>();
            sorted_sanStream_last_group.add(san_t);
            for (int i = begin; i <= end; i++) {//t has no valid san_stream yet
                sorted_sanStream_last_group.add(san_stream.get(i)[my_dim]);
            }

            Collections.sort(sorted_sanStream_last_group);
            double group_size = sorted_sanStream_last_group.size();
            double median;

            if (group_size % 2 == 0)
                median = (sorted_sanStream_last_group.get((int) (group_size / 2)) + sorted_sanStream_last_group.get((int) (group_size / 2 - 1))) / 2;
            else {
                median = sorted_sanStream_last_group.get((int) (group_size / 2));
            }
            return median;
        }
    }
}
