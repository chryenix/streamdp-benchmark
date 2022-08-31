package mechansims;

import java.util.ArrayList;

/**
 * Re-implementation of BA mechanism without grouping from 
 * G. Kellaris et al. 2014. Differentially Private Event Sequences over Infinite Streams. Proc. VLDB Endow. 7, 12 (2014), 1155–1166.
 * 
 * @author b1074672
 *
 */
public class BA extends Mechansim {
	double[] last_published;
	public BA(){
		super();
	}
	
	public BA(int w, double epsilon) {
		super(w,epsilon);
	}

	public ArrayList<double[]> run(ArrayList<double[]> org_stream){
		final int length = org_stream.size();
		final int dim = org_stream.get(0).length;
		ArrayList<double[]> sanitized_stream = new ArrayList<double[]>(length);

		// do budget shifting Sec 4.5
		double lambda_1_per_timestamp = (double) 1 / (double) epsilon;// yes I know it is 1.0d, that is the trick.
		double epsilon_2_per_window = epsilon - w * (epsilon/dim);
			
		if(epsilon_2_per_window<this.epsilon/2){ // no budget shifting
			lambda_1_per_timestamp = (w*sensitivity)/(this.epsilon*0.5*(double)dim);//XXX changed since last validation
			epsilon_2_per_window = this.epsilon-(this.epsilon*0.5);
		}
        final double[] used_budget = new double[org_stream.size()];
        
		//We certainly publish the first time stamp.
		int t = 0;
		double eps_2_per_timestamp = epsilon_2_per_window / w;//Like in uniform.
		this.last_published =  sanitize(org_stream.get(t), lambda(eps_2_per_timestamp));//one time stamp sanitized
		sanitized_stream.add(t, this.last_published);
		used_budget[t] = eps_2_per_timestamp;
		double used_budget_in_this_window = eps_2_per_timestamp;
		t++;	
		int timePointsSkipped = 0;
		
		while(t<length){
			// Phase 1: Similarity
			/** sad - sanitized_avg_dissimilarity */
			double s_a_d;
			s_a_d = avg_dissimilarity(this.last_published, org_stream.get(t), lambda_1_per_timestamp);
    
			//Phase 2: Publishing decision
			if(t>=w){//budget recovery
				used_budget_in_this_window-=used_budget[t-w];//this one is not relevant anymore
			}
			eps_2_per_timestamp = (epsilon_2_per_window/(double)w)*(1.0d+(double)timePointsSkipped);
			if(eps_2_per_timestamp>0.001d+epsilon_2_per_window-used_budget_in_this_window){//XXX - Java...
				System.err.println("Budget exceeds available remaining budget: "+(eps_2_per_timestamp-(epsilon_2_per_window-used_budget_in_this_window))+" spend "+used_budget_in_this_window+" wanna spend "+eps_2_per_timestamp+" must no exceed "+epsilon_2_per_window);
			}		
			
			//only publish if the newly introduced error probably is smaller than publishing the last values again.
			if (s_a_d > (this.sensitivity / eps_2_per_timestamp)) {
				this.last_published = sanitize(org_stream.get(t), lambda(eps_2_per_timestamp));//one time stamp sanitized
				sanitized_stream.add(t, this.last_published);
				used_budget[t] = eps_2_per_timestamp;
				used_budget_in_this_window +=eps_2_per_timestamp;

				if(timePointsSkipped>0){
					final int skip_until = t+timePointsSkipped;
					
					while(t<skip_until){
						t++;
		        		if(t==length){
		        			break;//XXX
		        		}
		        		sanitized_stream.add(t, last_published);
		        		if(t>=w){//budget recovery
		    				used_budget_in_this_window-=used_budget[t-w];//this one is not relevant anymore
		    			}
		        	}
					timePointsSkipped=0;
				}
			}else{
				sanitized_stream.add(t, this.last_published);
				if(timePointsSkipped<w-1){timePointsSkipped++;}
			}
			
			t++;
		}
		//System.out.println(outTSV(used_budget));
		//System.out.println("w="+w+"\t"+mae(org_stream,sanitized_stream)+"\t"+outTSV(analyze(used_budget, w)));

		return sanitized_stream;
	}

	@Override
	public String name() {
		return "BA";
	}
}
