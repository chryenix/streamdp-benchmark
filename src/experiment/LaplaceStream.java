package experiment;

import cern.jet.random.Distributions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class LaplaceStream {
	static LaplaceStream instance = null;
	
	int size;
	int dim;
	int counter = 0;
	int current_line = 0;
	double[][] my_numbers;
	
	public static int USAGE_MODE_RANDOM 		= 0;
	public static int USAGE_MODE_PRECOMPUTED 	= 1;
	public static int USAGE_MODE_STUPID 		= 2;
	public static int USAGE_MODE 				=USAGE_MODE_RANDOM; //USAGE_MODE_STUPID; //USAGE_MODE_RANDOM;
	
	static{
		instance = new LaplaceStream();
	}
	static cern.jet.random.engine.RandomEngine generator;

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
			File randomNumbersFile = new File(".\\data\\laplace_numbers_per_mechanism_100_iterations.csv");

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
	
	/*public static boolean changeUsageMode(int mode){
		LaplaceStream.USAGE_MODE = mode;
		instance = new LaplaceStream();
		return true;
	}*/
	
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
	
	public static double nextNumber(){
		if(USAGE_MODE==USAGE_MODE_STUPID){
			return 1.0d;
		}
		
		double num = instance.my_numbers[instance.current_line][instance.counter++];
		
		instance.counter = (instance.counter==instance.size) ? 0 : instance.counter;
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
