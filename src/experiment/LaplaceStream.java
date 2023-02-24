package experiment;

import cern.jet.random.Distributions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * This is the class that generates random Laplace numbers. We use it for adding the right amount of noise to achieve differential privacy.
 * 
 * The class generates a stream of Laplace numbers following the the zero mean Laplace distribution with shape parameter \lambda=1. There, are three ways to generate the stream steered with the USAGE_MODE parameter:
 * (1) USAGE_MODE_RANDOM - This is what you want to conduct real experiments.
 * (2) USAGE_MODE_STUPID - This means that we always return \lambda, i.e., the expected amount of noise. It is supposed for debugging. The name shall indicate that you should use it for experiments.
 * (3) USAGE_MODE_PRECOMPUTED - This may additionally help for debugging.
 * 
 * Technically, the stream contains numbers for Lap(0,1), and we need stretch them (not here) to be conform with Lap(0,\lambda), e.g., when adding noise.
 * 
 * @author Martin
 *
 */
public class LaplaceStream {
	static LaplaceStream instance = null;
	
	int size;
	int dim;
	int counter = 0;
	int current_line = 0;
	/**
	 * my_numbers[i] refers to one stream. We cycle through them
	 * my_numbers[i][dim] refers to a specific Laplace random number.
	 */
	double[][] my_numbers;
	
	public static int USAGE_MODE_RANDOM 		= 0;
	public static int USAGE_MODE_PRECOMPUTED 	= 1;
	public static int USAGE_MODE_STUPID 		= 2;
	public static int USAGE_MODE 				=USAGE_MODE_RANDOM; //USAGE_MODE_STUPID; //USAGE_MODE_RANDOM;
	
	static{
		instance = new LaplaceStream();
	}
	static cern.jet.random.engine.RandomEngine generator;

	/**
	 * The class is designed to be a singleton. I.e., there is one stream.
	 */
	private LaplaceStream(){
		System.out.print("Loading random numbers with mode "+ USAGE_MODE);
		if(USAGE_MODE==USAGE_MODE_RANDOM){
			size = 10000000;
			dim = 1;
			my_numbers = new double[dim][size];
	        generator = new cern.jet.random.engine.MersenneTwister(new java.util.Date());
			for(int i=0;i<size;i++){
				double d = Distributions.nextLaplace(generator);
				my_numbers[0][i]=d;
			}
		}else if(USAGE_MODE==USAGE_MODE_PRECOMPUTED){
			File randomNumbersFile = new File(".\\data\\laplace_numbers_per_mechanism_100_iterations.csv");//The file location

	        try {
	            if (randomNumbersFile.exists()) {
	            	size = 90047;
	            	dim = 55;
	            	ArrayList<double[]> temp_arr = new ArrayList<double[]>();
	    			my_numbers = new double[dim][size];
	            	BufferedReader inFile = new BufferedReader(new FileReader(randomNumbersFile));
	                inFile.readLine();//skip first line
	                String line; 
	                while ((line = inFile.readLine())!= null) {
	                	String[] tokens = line.split(",");
	                	double[] temp = new double[size];
	                	temp_arr.add(temp);
	                	for(int i=1;i<tokens.length;i++){//skip first toke (line number)
	                		double c = Double.parseDouble(tokens[i]);
	                		temp[i-1] = c;
	                	}
	                	/*if(temp_arr.size()%10==0){
	                		System.out.print(" "+temp_arr.size()+" ");
	                	}*/
	                }
	                inFile.close();
	                my_numbers = new double[temp_arr.size()][size];
	                for(int i=0;i<temp_arr.size();i++){
	                	my_numbers[i]=temp_arr.get(i);
	                }
	            }
	        } catch (IOException e) {
	            System.err.println(e);
	        }
		}else{
			System.err.println("For testing only returns always lambda");
		}
		System.out.println(" [Done]");
		
	}
	
	public static boolean line(){
		if(USAGE_MODE==USAGE_MODE_PRECOMPUTED){
			instance.current_line++;
			instance.current_line = (instance.current_line==instance.dim) ? 0 : instance.current_line;
			return true;
		}else{
			//System.err.println("LaplaceStream.nextLine() called in wrong usage mode");
			return false;
		}
	}
	
	/**
	 * Returns some number from Lap(0,1)
	 */
	public static double nextNumber(){
		if(USAGE_MODE==USAGE_MODE_STUPID){
			return 1.0d;
		}
		
		double num = instance.my_numbers[instance.current_line][instance.counter++];
		
		instance.counter = (instance.counter==instance.size) ? 0 : instance.counter;//Start at the front, if you reached the end of the stream.
		return num;
	}
	
	public static boolean setLine(int line) {
		if(USAGE_MODE==USAGE_MODE_PRECOMPUTED){
			instance.current_line = line;
			return true;
		}else{
			//System.err.println("LaplaceStream.setLine() called in wrong usage mode");
			return false;
		}
	}
}
