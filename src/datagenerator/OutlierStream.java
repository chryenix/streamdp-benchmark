package datagenerator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;

import experiment.Experiment;
import experiment.LaplaceStream;
import mechanisms.*;

import util.*;

public class OutlierStream {
	public static final int PLATEAU_OUTLIER 		  = 0;
	public static final int EXTREMUM_OUTLIER 		  = 1;
	public static final int PATTERN_EXTENSION_OUTLIER = 2;
	
	public static final double IS_OUTLIER = 1.0d;
	public static final double NO_OUTLIER = 0.0d;
	
	private static Random rand = new Random(12345);
	
	//Not supposed to be modified
	//private final ArrayList<double[]> org_generated_stream;
	
	public final ArrayList<double[]> outlier_stream;
	private final int stream_dimensionality;
	double[] outlier_score;
	final int outlier_type;
	/**
	 * Maximum query result in the sub stream.
	 */
	double q_max;
	//TODO make configurable
	int expected_season_length = 80;
	
	@SuppressWarnings("unchecked")
	public OutlierStream(final ArrayList<double[]> org_generated_stream, final int outlier_type) {
		//this.org_generated_stream 	= (ArrayList<double[]>) org_generated_stream.clone();
		this.outlier_score 			= new double[org_generated_stream.size()];//init with 0
		this.outlier_type 			= outlier_type;
		this.stream_dimensionality = org_generated_stream.get(0).length;
		if(stream_dimensionality!=1) {
			System.err.println("OutlierStream: stream_dimensionality!=1");
		}
		
		q_max = get_max(org_generated_stream);
		outlier_stream = new ArrayList<double[]>(org_generated_stream.size());
		for(double[] d : org_generated_stream) {
			outlier_stream.add(d.clone());
		}
		
		//Place outlier based on type
		if(outlier_type == PLATEAU_OUTLIER){
			place_plateaus();
		}else if(outlier_type == EXTREMUM_OUTLIER){
			place_extrema();
		}else if(outlier_type == PATTERN_EXTENSION_OUTLIER){
			place_pattern_extension();
		}else{
			System.err.println("OutlierStream: unknown outlier type "+outlier_type);
		}
	}
	
    private void place_plateaus() {
     	final double plateau_threshold = 0.1*q_max;//make class member
     	double p = 0.3;//XXX
     	
    	for(int season_start = 0;season_start<outlier_stream.size();) {
    		//dice in [0,1]
    		double dice = rand.nextDouble();
    		if(dice<=p) {
    			//we place an outlier
    			int next_season_start = find_next_season_start(season_start, expected_season_length, outlier_stream);
    			for(int t=season_start;t<next_season_start;t++) {
    				double[] time_stamp_data = outlier_stream.get(t);
    				for(int dim=0;dim<time_stamp_data.length;dim++) {//usually multidimensional release per time stamp. Cut them all
    					if(time_stamp_data[dim]>plateau_threshold) {
    						time_stamp_data[dim] = plateau_threshold;//Implicit min(plateau_threshold,time_stamp_data[dim])
    						outlier_score[t]   = IS_OUTLIER;
    					}
    				}
    			}
    		}
    		season_start = find_next_season_start (season_start, expected_season_length, outlier_stream);
    	}
	}
    
    /**
     * Places *one* pattern extension outlier
     */
    private void place_pattern_extension() {
    	int random_ts = expected_season_length+rand.nextInt(outlier_stream.size()-2*expected_season_length);//dice some random time stamp. Not in the first or last season.
    	//find the peaks to the left and right
    	
    	//to the left
    	int max_t_left = -1;
    	double max_val_left = Double.MIN_VALUE;
    	for(int t=random_ts-expected_season_length;t<random_ts;t++) {
    		double val = outlier_stream.get(t)[0];
    		if(val>max_val_left) {
    			max_val_left = val;
    			max_t_left = t;
    		}
    	}
    	//to the right
    	int max_t_right = -1;
    	double max_val_right = Double.MIN_VALUE;
    	for(int t=random_ts;t<=random_ts+expected_season_length;t++) {
    		double val = outlier_stream.get(t)[0];
    		if(val>max_val_right) {
    			max_val_right = val;
    			max_t_right = t;
    		}
    	}
    	//At the lowest Q(D_t) between them, we place the outlier. Find it now.
    	double min = Double.MAX_VALUE;
		int min_t = -1;
    	for(int t=max_t_left;t<max_t_right;t++) {
    		double val = outlier_stream.get(t)[0];
			if(val<min) {
				min = val;
				min_t = t;
			}
    	}
    	//Really place the outlier
    	final int num_outlier_ts = expected_season_length / 4;//TODO make config parameter
    	final int dim = outlier_stream.get(min_t).length;
    	
    	for(int i=0;i<num_outlier_ts;i++) {
    		double[] new_ts = new double[dim];
    		Arrays.fill(new_ts, Math.max(0, min+rand.nextGaussian()));//N(0,1) 
    		outlier_stream.add(min_t,new_ts);//Inserts the specified element at the specified position in this list. Shifts the element currently at that position (if any) and any subsequent elements to the right (adds one to their indices).
    	}
    	//the size of the stream prefix changed. Need to change the outlier score array as well
    	this.outlier_score = new double[outlier_stream.size()];
    	for(int i=0;i<num_outlier_ts;i++) {
    		outlier_score[min_t+i] = IS_OUTLIER;
    	}
    }
    
    //TODO shall we bound the maximum number of extrema?
    private void place_extrema() {
    	double p = 0.01;//XXX
    	double multiplier = 1.5;//TODO make class member
     	final double extremum_value = multiplier*q_max;//make class member
    	
    	for(int t = 0;t<outlier_stream.size();t++) {
    		//dice in [0,1]
    		double dice = rand.nextDouble();
    		if(dice<=p) {
    			//we place an outlier
    			outlier_stream.get(t)[0] = extremum_value;
    			outlier_score[t] 	     = IS_OUTLIER;
    		}
    	}
	}

	private static double get_max(ArrayList<double[]> stream) {
		double max = Double.MIN_VALUE;
		for(double[] ts : stream){
			for(double v : ts){
				if(v>max) {max = v;}
			}
		}
		return max;
	}
	
	/**
	 * Tested only for 1D streams. Jump to a position close to the season maximum. Finds the minimum between to season peaks
	 * TOOD vlt einfach im Generator merken?
	 * 
	 * @param season_start
	 * @param expexted_season_length
	 * @param stream
	 * @return
	 */
	private int find_next_season_start(int season_start, int expexted_season_length, ArrayList<double[]> stream) {
		int t = season_start+expexted_season_length/2;//that's where expect the peak of this season to be
		int expected_peak_next_season = t+expexted_season_length;
		
		double min = Double.MAX_VALUE;
		int min_t = -1;
		for(;t<expected_peak_next_season;t++) {
			if(t==stream.size()) {return stream.size();}
			double val = stream.get(t)[0];
			if(val<min) {
				min = val;
				min_t = t;
			}
		}
		return min_t;
	}
	
	public String toString(){
		String ret = "Stream with outliers of type "+outlier_type;
		ret += "\n"+out_tsv(outlier_stream);
		ret += "\n"+Generator.outTSV(outlier_score);
		return ret;
	}
	
	public static String to_file_output_string(final ArrayList<double[]> stream, double[] outlier_score) {
		if(stream.get(0).length!=1) {
			return null;
		}
		final String SPLIT_CHAR = ",";
		StringBuffer buffer = new StringBuffer(10000);//Some size, hopefully big enough.
		buffer.append("timestamp"+SPLIT_CHAR+"value"+SPLIT_CHAR+"is_anomaly");
		for(int t=0;t<stream.size();t++) {
			double val = stream.get(t)[0];//Recap 1D stream only
			buffer.append("\n"+t+SPLIT_CHAR+val+SPLIT_CHAR+outlier_score[t]);
		}
		return buffer.toString();
	}
	
	public String to_file_output_string() {
		return to_file_output_string(outlier_stream, outlier_score);
	}
	
	public static void main(String[] arg){
		//generate_artificial_outlier_streams();
		load_dodgers_dataset();
	}
	
	static void generate_artificial_outlier_streams() {
        int[] desriredStreamMaxValues = {1000};
        double basis = 1.2;//TODO 1.3?
        int[] pLen = {80};
        int streamLength = 1000;
        int num_iterations_per_Mechanism = 1;
        int num_iterations_per_dataParameterCombi = 1;
        
        //Experiment related stuff
        int[] all_mechanisms = {Experiment.KalmanFilterPID_FAST_w, Experiment.UNIFROM, Experiment.SAMPLE, Experiment.BD, Experiment.BA, Experiment.RESCUE_DP_MD, Experiment.PEGASUS, Experiment.DSATWEVENT, Experiment.ADAPUB};
        int[] mechanisms = all_mechanisms;
        double[] epsilons = {0.1, 0.3, 0.5, 0.7, 0.9, 1.0};
        int w = 120;
        double epsilon = 1.0;
        int[] w_s = {40, 80, 120, 160, 200};
        int num_iterations = 100;
        
        for (int period_length : pLen) {
            Generator gen = new Generator(streamLength, period_length, basis);
            StreamScalerAndStretcher s = new StreamScalerAndStretcher(gen, num_iterations_per_Mechanism);
            for (int desired_max_value : desriredStreamMaxValues) {
                for (int i = 0; i < num_iterations_per_dataParameterCombi; i++) {
                    ArrayList<double[]> data_set = s.get_stream(i, desired_max_value, 1);
                    String data_set_identifier = "period_length" + period_length + "_a" + desired_max_value + "_iter" + i;
                    System.out.println(out_tsv(data_set));
                    
                    OutlierStream plateaus = new OutlierStream(data_set, PLATEAU_OUTLIER);
                    System.out.println(plateaus);
                    create_sanitized_streams_vary_e(mechanisms, plateaus, data_set_identifier+"_plateaus", epsilons, num_iterations, w);
                    create_sanitized_streams_vary_w(mechanisms, plateaus, data_set_identifier+"_plateaus", epsilon, num_iterations, w_s);
                    
                    OutlierStream extrema = new OutlierStream(data_set, EXTREMUM_OUTLIER);
                    System.out.println(extrema);
                    create_sanitized_streams_vary_e(mechanisms, plateaus, data_set_identifier+"_extrema", epsilons, num_iterations, w);
                    create_sanitized_streams_vary_w(mechanisms, plateaus, data_set_identifier+"_extrema", epsilon, num_iterations, w_s);
                    
                    OutlierStream pattern_extensions = new OutlierStream(data_set, PATTERN_EXTENSION_OUTLIER);
                    System.out.println(pattern_extensions);
                    create_sanitized_streams_vary_e(mechanisms, plateaus, data_set_identifier+"_pattern", epsilons, num_iterations, w);
                    create_sanitized_streams_vary_w(mechanisms, plateaus, data_set_identifier+"_pattern", epsilon, num_iterations, w_s);
                    
                    System.out.println(out_tsv(data_set));
                }
                System.out.println("****************** End of period_length=" + period_length);
            }
        }   
	}
	
	public static String out_tsv(ArrayList<double[]> stream) {
		if(stream.get(0).length!=1) return "out_tsv(ArrayList<double[]>) No 1D Stream";
		String ret = "Stream\n";
		for(double[] ts : stream) {
			ret+=ts[0]+"\t";
		}
		return ret;
	}
	
	public static String CSV_DIR = "./csv";
    static void create_sanitized_streams_vary_e(int[] mechanisms, OutlierStream stream, String data_set, double[] epsilons, int num_iterations, int w) {
        System.out.println("w-event Experimentator for Outlier Stream testing for mechanisms " + Arrays.toString(mechanisms) + " e in " + Arrays.toString(epsilons) + " and w = " + w);
        double start = System.currentTimeMillis();
        Arrays.sort(mechanisms);
        Arrays.sort(epsilons);

        System.out.println(+stream.outlier_stream.size() + " [done] in " + (System.currentTimeMillis() - start) + " ms");

        BDwithGrouping.NUM_GROUPS = (int) Math.max(1, (int) stream.outlier_stream.get(0).length * 0.002);
        System.out.println("BDNumGroups " + BDwithGrouping.NUM_GROUPS);

        for (int i = 0; i < mechanisms.length; i++) {
            Mechansim m = Experiment.getInstance(mechanisms[i]);
            System.out.println(m.name());
            for (int e_i = 0; e_i < epsilons.length; e_i++) {
                LaplaceStream.setLine(0);
                double eps = epsilons[e_i];
                m.epsilon = eps;
                for (int loop = 0; loop < num_iterations; loop++) {
                    ArrayList<double[]> sanStream = m.run(stream.outlier_stream, w, eps);
                    try {
                        FileWriter file = new FileWriter(CSV_DIR + "/eps-" + data_set + "-" + m.name().replaceAll("\\s", "").replaceAll("_", "") + "-"+loop+".csv");
                        BufferedWriter bufferedWriter = new BufferedWriter(file);
                        String file_content = to_file_output_string(sanStream, stream.outlier_score);
                        bufferedWriter.append(file_content);
                        bufferedWriter.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }     
                    LaplaceStream.line();
                }
            }  
            System.gc();
        }
        if (Mechansim.TRUNCATE) {
            System.out.println("Truncate: get rid of negative counts and round to next integer");
        }
        if (Mechansim.USE_NON_PRIVATE_SAMPLING) {
            System.out.println("Non-private sampling");
        }
        if (LaplaceStream.USAGE_MODE == LaplaceStream.USAGE_MODE_STUPID) {
            System.out.println("Using determininstic noise of size lambda - use this for correctness checks only.");
        }

        System.out.println("Data=" + data_set + " with dim=" + stream.outlier_stream.get(0).length + " length=" + stream.outlier_stream.size());
    }
    static void create_sanitized_streams_vary_w(int[] mechanisms, OutlierStream stream, String data_set, double epsilon, int num_iterations, int[] ws) {
        System.out.println("w-event Experimentator for Outlier Stream testing for mechanisms " + Arrays.toString(mechanisms) + " w in " + Arrays.toString(ws) + " and e = " + epsilon);
        double start = System.currentTimeMillis();
        Arrays.sort(mechanisms);
        Arrays.sort(ws);

        System.out.println(+stream.outlier_stream.size() + " [done] in " + (System.currentTimeMillis() - start) + " ms");

        BDwithGrouping.NUM_GROUPS = (int) Math.max(1, (int) stream.outlier_stream.get(0).length * 0.002);
        System.out.println("BDNumGroups " + BDwithGrouping.NUM_GROUPS);

        for (int i = 0; i < mechanisms.length; i++) {
            Mechansim m = Experiment.getInstance(mechanisms[i]);
            System.out.println(m.name());
            for (int w_i = 0; w_i < ws.length; w_i++) {
                LaplaceStream.setLine(0);
                double eps = epsilon;
                int w = ws[w_i];
                m.epsilon = eps;
                for (int loop = 0; loop < num_iterations; loop++) {
                    ArrayList<double[]> sanStream = m.run(stream.outlier_stream, w, eps);
                    try {
                        FileWriter file = new FileWriter(CSV_DIR + "/w-" + data_set + "-" + m.name().replaceAll("\\s", "").replaceAll("_", "") + "-"+loop+".csv");
                        BufferedWriter bufferedWriter = new BufferedWriter(file);
                        String file_content = to_file_output_string(sanStream, stream.outlier_score);
                        bufferedWriter.append(file_content);
                        bufferedWriter.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }     
                    LaplaceStream.line();
                }
            }  
            System.gc();
        }
        if (Mechansim.TRUNCATE) {
            System.out.println("Truncate: get rid of negative counts and round to next integer");
        }
        if (Mechansim.USE_NON_PRIVATE_SAMPLING) {
            System.out.println("Non-private sampling");
        }
        if (LaplaceStream.USAGE_MODE == LaplaceStream.USAGE_MODE_STUPID) {
            System.out.println("Using determininstic noise of size lambda - use this for correctness checks only.");
        }

        System.out.println("Data=" + data_set + " with dim=" + stream.outlier_stream.get(0).length + " length=" + stream.outlier_stream.size());
    }
    
    
    static final String DODGERS_EVENTS = ".\\data\\Dodgers.events";
    static final String DODGERS_DATA = ".\\data\\Dodgers.data";
    /**
     * Download file at https://archive.ics.uci.edu/ml/datasets/Dodgers+Loop+Sensor
     * @param file_location
     */
    static void load_dodgers_dataset() {
    	//(1) Process .event file
    	ArrayList<Event> events = new ArrayList<Event>();
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(DODGERS_EVENTS));
			String line;
	        while ((line = br.readLine()) != null) {
	        	Event e = new Event(line);
	        	events.add(e);
	        }
	        br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		/*for(Event e : events) {
			System.out.println(e);
		}*/
		//(2) Process .data file
		ArrayList<Count> counts = new ArrayList<Count>();
		try {
			br = new BufferedReader(new FileReader(DODGERS_DATA));
			String line;
	        while ((line = br.readLine()) != null) {
	        	Count c = new Count(line);
	        	counts.add(c);
	        }
	        br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		label_outlier(counts, events);
		/*for(Count c : counts) {
			System.out.println(c);		
		}*/
		/*for(int i=1;i<counts.size();i++) {
			LocalDateTime before, now;
			before = counts.get(i-1).my_time;
			now = counts.get(i).my_time;
			if(!before.plusMinutes(5).equals(now)) {
				System.err.println(before+" mintues != "+now);
			}
		}*/
		int num_timestamps_to_aggregate = 12;
		counts = aggregate(counts,num_timestamps_to_aggregate);
		for(Count c : counts) {
			System.out.println(c);		
		}
    }

	private static ArrayList<Count> aggregate(ArrayList<Count> counts, int num_timestamps_to_aggregate) {
		ArrayList<Count> ret = new ArrayList<Count>(counts.size()/num_timestamps_to_aggregate+1);
		for(int i=0;i<counts.size();) {
			//group
			int count = 0;
			double outlier_label = NO_OUTLIER;
			Count c=null;
			for(int g=0;g<num_timestamps_to_aggregate;g++,i++) {
				c = counts.get(i);
				count+=c.count;
				outlier_label = Math.max(outlier_label, c.outlier_label);//IS_OUTLIER is dominant
			}
			ret.add(new Count(c.my_time,count,outlier_label));
		}
		
		return ret;
	}

	private static void label_outlier(ArrayList<Count> counts, ArrayList<Event> events) {
		final int size_counts = counts.size();
		int i_counts=0;
		int i_events=0;
		Count c;
		
		while(i_counts<size_counts) {
			Event current_event = events.get(i_events);
			//Find first count inside event
			while((c=counts.get(i_counts)).my_time.isBefore(current_event.begin)) {
				i_counts++;
				//TODO return with > i_counts
			}
			
			while((c=counts.get(i_counts)).my_time.isBefore(current_event.end)) {
				c.outlier_label = IS_OUTLIER;
				i_counts++;
				//TODO return with > i_counts
			}
			i_events++;
			if(i_events==events.size()) {return;}//marked all outlier events
		}
	}
}
