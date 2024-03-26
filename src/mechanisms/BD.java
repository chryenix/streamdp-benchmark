package mechanisms;

import java.util.ArrayList;

/**
 * Re-implementation of BD mechanism without grouping from 
 * G. Kellaris et al. 2014. Differentially Private Event Sequences over Infinite Streams. Proc. VLDB Endow. 7, 12 (2014), 1155-1166.
 * 
 * @author b1074672
 *
 */
public class BD extends Mechansim {
	double[] last_published;
	public BD(){
		super();
	}
	
	public BD(int w, double epsilon) {
		super(w,epsilon);
	}

	public ArrayList<double[]> run(ArrayList<double[]> org_stream){
		final int length = org_stream.size();
		final int dim = org_stream.get(0).length;
		ArrayList<double[]> sanitized_stream = new ArrayList<double[]>(length);

		// do budget shifting
		double lambda_1_per_timestamp = (double) 1 / (double) epsilon;
		double epsilon_2_per_window = epsilon - w * (epsilon/dim);
		if(epsilon_2_per_window<this.epsilon/2){ // no budget shifting
			lambda_1_per_timestamp = (w*sensitivity)/(this.epsilon*0.5*(double)dim);
			epsilon_2_per_window = this.epsilon-(this.epsilon*0.5);
		}
		
        final double[] used_budget = new double[org_stream.size()];
        final double[] remainig_budget = new double[org_stream.size()];
        ArrayList<Integer> when_published = new ArrayList<Integer>();
        
		//We certainly publish the first time stamp.
		int t = 0;
		double eps_2_per_timestamp = epsilon_2_per_window / 2.0;//Initially, use half of the available budget.
		this.last_published =  sanitize(org_stream.get(t), lambda(eps_2_per_timestamp));//one time stamp sanitized
		sanitized_stream.add(t, this.last_published);
		used_budget[t] = eps_2_per_timestamp;
		when_published.add(0);
		remainig_budget[0]=eps_2_per_timestamp;//the other half
		double used_budget_in_this_window = eps_2_per_timestamp;
		t++;	
		
		while(t<length){
			// Phase 1: Similarity
			/** sad - sanitized_avg_dissimilarity */
			double s_a_d;
			s_a_d = avg_dissimilarity(this.last_published, org_stream.get(t), lambda_1_per_timestamp);
    
			//Phase 2: Publishing decision
			if(t>=w){//budget recovery
				used_budget_in_this_window-=used_budget[t-w];//this one is not relevant anymore
			}
			eps_2_per_timestamp = (epsilon_2_per_window-used_budget_in_this_window)/2;//use half of the remaining budget
			remainig_budget[t]=eps_2_per_timestamp;
			//only publish if the newly introduced error probably is smaller than publishing the last values again.
			if (s_a_d > (this.sensitivity / eps_2_per_timestamp)) {
				this.last_published = sanitize(org_stream.get(t), lambda(eps_2_per_timestamp));//one time stamp sanitized
				sanitized_stream.add(t, this.last_published);
				used_budget[t] = eps_2_per_timestamp;
				used_budget_in_this_window +=eps_2_per_timestamp;
				when_published.add(t);
			}else{
				sanitized_stream.add(t, this.last_published);
			}
			
			t++;
		}

		return sanitized_stream;
	}
	
	@Override
	public String name() {
		return "BD";
	}
}
