package mechanisms;

import java.util.ArrayList;

/**
 * Re-implementation of baseline mechanism Sample from 
 * G. Kellaris et al. 2014. Differentially Private Event Sequences over Infinite Streams. Proc. VLDB Endow. 7, 12 (2014), 1155–1166.
 * 
 * @author b1074672
 *
 */
public class Sample extends Mechansim {
		
	public Sample(){
		super();
	}
	
	public Sample(int w, double epsilon) {
		super(w,epsilon);
	}

	public ArrayList<double[]> run(ArrayList<double[]> org_stream){
		final int length = org_stream.size();
		final double lambda = this.sensitivity/this.epsilon;//since I publish only once, I can use the whole budget for the w-interval 
		ArrayList<double[]> sanitized_stream = new ArrayList<double[]>(length);
		
		for(int t=0;t<length;){
			double[] san_t = sanitize(org_stream.get(t), lambda);//one time stamp sanitized
			sanitized_stream.add(t, san_t);
			t++;
			//now skip the next w time stamps
			int later=t;
			for(;later<Math.min(t+w-1,length);later++){
				sanitized_stream.add(later, san_t.clone());// Note, I clone the last release to allow post processing.
			}
			t=later;//yes, we skip them.
		}
		return sanitized_stream;
	}
	
	@Override
	public String name() {
		return "Sample";
	}
}
