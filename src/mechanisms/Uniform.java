package mechanisms;

import java.util.ArrayList;

/**
 * Re-implementation of baseline mechanism Uniform from 
 * G. Kellaris et al. 2014. Differentially Private Event Sequences over Infinite Streams. Proc. VLDB Endow. 7, 12 (2014), 1155-1166.
 * 
 * @author b1074672
 *
 */
public class Uniform extends Mechansim{
	
	public Uniform(){
		super();
	}
	
	public Uniform(int w, double epsilon) {
		super(w,epsilon);
	}
	
	public ArrayList<double[]> run(ArrayList<double[]> org_stream){
		final int length = org_stream.size();
		final double lambda = this.sensitivity/(this.epsilon/(double)this.w);//evenly distribute whole \epsilon to each time stamp in window 
		ArrayList<double[]> sanitized_stream = new ArrayList<double[]>(length);
		
		for(int t=0;t<length;t++){
			double[] san_t = sanitize(org_stream.get(t), lambda);// Query result of one time stamp sanitized
			sanitized_stream.add(t, san_t);						 // Publish the sanitized Query result
		}
		return sanitized_stream;
	}

	@Override
	public String name() {
		return "Uniform";
	}
}

