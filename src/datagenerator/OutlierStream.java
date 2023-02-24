package datagenerator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import experiment.Experiment;
import experiment.LaplaceStream;
import mechanisms.*;

import util.*;

/**
 * This is an extension of the data generator (i.e., Generator class). 
 * It is used to inject outliers into streams, load real-world outlier streams and run the common experiments.
 * 
 * @author Martin
 *
 */
public class OutlierStream {
	//Enums for outlier types
	public static final int PLATEAU_OUTLIER 		  = 0;
	public static final int EXTREMUM_OUTLIER 		  = 1;
	public static final int PATTERN_EXTENSION_OUTLIER = 2;
	public static final int REAL_WORLD_OUTLIER 		  = 3;
	
	//Labels for ground truth
	public static final double IS_OUTLIER = 1.0d;
	public static final double NO_OUTLIER = 0.0d;
	
	private static Random rand = new Random(12345);
	
	//Path to real-world data
	public static String CSV_DIR = "./csv";
	static final int OUTPUT_MODE_CONSOLE = 0;
	static final int OUTPUT_MODE_FILE = 1;
	static int OUTPUT_MODE = OUTPUT_MODE_FILE; 
	
    //Experiment related stuff
	static int[] mechanisms; 
	static double[] epsilons; 
	static int w; 
	static double epsilon; 
	static int[] w_s; 
	static int num_iterations;
	
	static {//config TODO refactor me to real config
        //Experiment related stuff
        int[] all_mechanisms = {Experiment.KalmanFilterPID_FAST_w, Experiment.UNIFROM, Experiment.SAMPLE, Experiment.BD, Experiment.BA, Experiment.RESCUE_DP_MD, Experiment.PEGASUS, Experiment.DSATWEVENT, Experiment.ADAPUB};
        //int[] some_mechanisms = {Experiment.UNIFROM};
        mechanisms = all_mechanisms;
        double[] temp = {0.1, 0.3, 0.5, 0.7, 0.9, 1.0}; 
        epsilons = temp;
        w = 2;//was 40 until 06.01., was 8 until 20.01.
        epsilon = 1.0;
        //int[] temp_2 = {40, 80, 120, 160, 200};
        int[] temp_2 = {1, 2, 4, 8, 16, 32};
        w_s = temp_2;
        num_iterations = 10;
	}
	
	/**
	 * This is the class member that carries the data of a stream with outliers. it is in line with our common interface of stream being an ArrayList<double[]>
	 */
	public final ArrayList<double[]> outlier_stream;
	private final int stream_dimensionality;
	/**
	 * outlier_score[t] refers to the outlier score at time stamp t. It should be in [0,1] i.e., in [NO_OUTLIER,IS_OUTLIER]. Conceptually, it can be some floating point.
	 */
	double[] outlier_score;
	/**
	 * Outlier type e.g., OutlierStream.PLATEAU_OUTLIER
	 */
	final int outlier_type;
	/**
	 * Maximum query result in the sub stream.
	 */
	double q_max;
	//TODO make configurable
	static int expected_season_length = 80;
	
	/**
	 * Constructor for real-world data not following our API, i.e., stream is no ArrayList<double[]>
	 * Here, we assume that the outliers are already in the data.
	 * 
	 * @param stream
	 */
	public OutlierStream(final ArrayList<Count> stream) {
		this.stream_dimensionality = 1;//By definition of the Count class
		if(stream_dimensionality!=1) {
			System.err.println("OutlierStream: stream_dimensionality!=1");
		}
		this.outlier_type = REAL_WORLD_OUTLIER;
		this.outlier_stream = new ArrayList<double[]>(stream.size());
		this.outlier_score = new double[stream.size()];
		for(int i=0;i<stream.size();i++) {
			Count c = stream.get(i);
			double[] temp = {c.count};
			outlier_stream.add(temp);
			outlier_score[i] = c.outlier_label;
		}
	}
	
	/**
	 * Constructor in line with our common interface. We place outliers according to given outlier_type Enum
	 * 
	 * @param org_generated_stream
	 * @param outlier_type
	 */
	public OutlierStream(final ArrayList<double[]> org_generated_stream, final int outlier_type) {
		this.outlier_score 			= new double[org_generated_stream.size()];//init with 0, i.e., NO_OUTLIER
		this.outlier_type 			= outlier_type;
		this.stream_dimensionality  = org_generated_stream.get(0).length;
		if(stream_dimensionality!=1) {
			System.err.println("OutlierStream: stream_dimensionality!=1");//Usually, we use 1D streams
		}
		
		q_max = get_max(org_generated_stream);
		outlier_stream = new ArrayList<double[]>(org_generated_stream.size());//prepare empty stream
		for(double[] d : org_generated_stream) {
			outlier_stream.add(d.clone()); //Note, we do not modify the original stream (without outliers)
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
	
    void place_plateaus() {
     	final double plateau_threshold = 0.1*q_max;//max query value in a plateau outlier
     	double p = 0.3;//XXX - hard coded probability of season to contain a plateau outlier
     	
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
    						outlier_score[t] = IS_OUTLIER;
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
    void place_pattern_extension() {
    	int random_ts = expected_season_length+rand.nextInt(outlier_stream.size()-2*expected_season_length);//dice some random time stamp. Not in the first or last season.
    	//find the peaks to the left and right
    	
    	//find season peak to the left
    	int max_t_left = -1;
    	double max_val_left = Double.MIN_VALUE;
    	for(int t=random_ts-expected_season_length;t<random_ts;t++) {
    		double val = outlier_stream.get(t)[0];
    		if(val>max_val_left) {
    			max_val_left = val;
    			max_t_left = t;
    		}
    	}
    	//find season peak to the right
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
    	final int num_outlier_ts = expected_season_length / 4;// XXX - hard coded length of the pattern extension anomaly
    	final int dim = outlier_stream.get(min_t).length;
    	
    	for(int i=0;i<num_outlier_ts;i++) {
    		double[] new_ts = new double[dim];
    		Arrays.fill(new_ts, Math.max(0, min+rand.nextGaussian()));// min query value with some random noise from N(0,1) 
    		outlier_stream.add(min_t,new_ts);//Inserts the specified element at the specified position in this list. Shifts the element currently at that position (if any) and any subsequent elements to the right (adds one to their indices).
    	}
    	//the size of the stream prefix changed. Need to change the outlier score array as well
    	this.outlier_score = new double[outlier_stream.size()];
    	for(int i=0;i<num_outlier_ts;i++) {
    		outlier_score[min_t+i] = IS_OUTLIER;
    	}
    }
    
    //TODO shall we bound the maximum number of extrema?
    void place_extrema() {
    	double p = 0.01;			//hard coded probability of a time stamp to become an outlier 
    	double multiplier = 1.5;	//TODO make class member - Used to compute the query value of an extremum anomaly
     	final double extremum_value = multiplier*q_max;
    	
    	for(int t = 0;t<outlier_stream.size();t++) {
    		//dice in [0,1]
    		double dice = rand.nextDouble();
    		if(dice<=p) {
    			//we place an outlier
    			outlier_stream.get(t)[0] = extremum_value;//only modify the release of the first dimension
    			outlier_score[t] 	     = IS_OUTLIER;
    		}
    	}
	}

	static double get_max(ArrayList<double[]> stream) {
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
	 * TOOD maybe store upon generation?
	 * 
	 * @param season_start
	 * @param expexted_season_length
	 * @param stream
	 * @return
	 */
	int find_next_season_start(int season_start, int expexted_season_length, ArrayList<double[]> stream) {
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
	
	static String to_file_output_string(final ArrayList<double[]> stream, double[] outlier_score) {
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
	
	String to_file_output_string() {
		return to_file_output_string(outlier_stream, outlier_score);
	}
	
	/**
	 * This methods illustrates how to start vary-e and vary-\epsilon experiments. The output are the files, which are then given to the outlier detection framework of
	 * https://hpi-information-systems.github.io/timeeval-evaluation-paper/ 
	 * @param arg
	 */
	public static void main(String[] arg){
		generate_artificial_outlier_streams();
		load_dodgers_dataset();
	}
	
	public static void generate_artificial_outlier_streams() {
        int[] desriredStreamMaxValues = {1000};
        double basis = 1.2;//TODO 1.3?
        int[] pLen = {expected_season_length};
        int streamLength = 1000;
        int num_iterations_per_Mechanism = 1;
        int num_iterations_per_dataParameterCombi = 1;
        
        for (int period_length : pLen) {
            Generator gen = new Generator(streamLength, period_length, basis);
            StreamScalerAndStretcher s = new StreamScalerAndStretcher(gen, num_iterations_per_Mechanism);
            for (int desired_max_value : desriredStreamMaxValues) {
                for (int i = 0; i < num_iterations_per_dataParameterCombi; i++) {
                	//Create stream without anomalies
                    ArrayList<double[]> data_set = s.get_stream(i, desired_max_value, 1);
                    String data_set_identifier = "period_length" + period_length + "_a" + desired_max_value + "_iter" + i;
                    System.out.println(out_tsv(data_set));
                    
                    OutlierStream plateaus = new OutlierStream(data_set, PLATEAU_OUTLIER);
                    System.out.println(plateaus);
                    write_non_private_stream(plateaus, data_set_identifier+"_plateaus");
                    create_sanitized_streams_vary_e(mechanisms, plateaus, data_set_identifier+"_plateaus", epsilons, num_iterations, w);
                    create_sanitized_streams_vary_w(mechanisms, plateaus, data_set_identifier+"_plateaus", epsilon, num_iterations, w_s);
                    
                    OutlierStream extrema = new OutlierStream(data_set, EXTREMUM_OUTLIER);
                    System.out.println(extrema);
                    write_non_private_stream(extrema, data_set_identifier+"_extrema");
                    create_sanitized_streams_vary_e(mechanisms, plateaus, data_set_identifier+"_extrema", epsilons, num_iterations, w);
                    create_sanitized_streams_vary_w(mechanisms, plateaus, data_set_identifier+"_extrema", epsilon, num_iterations, w_s);
                    
                    OutlierStream pattern_extensions = new OutlierStream(data_set, PATTERN_EXTENSION_OUTLIER);
                    System.out.println(pattern_extensions);
                    write_non_private_stream(pattern_extensions, data_set_identifier+"_pattern");
                    create_sanitized_streams_vary_e(mechanisms, plateaus, data_set_identifier+"_pattern", epsilons, num_iterations, w);
                    create_sanitized_streams_vary_w(mechanisms, plateaus, data_set_identifier+"_pattern", epsilon, num_iterations, w_s);
                    
                    System.out.println(out_tsv(data_set));
                }
                System.out.println("****************** End of period_length=" + period_length);
            }
        }   
	}
	/**
	 * For comparison, we also create an output for the original non-private outlier streams
	 * @param stream
	 * @param data_set
	 */
	static void write_non_private_stream(OutlierStream stream, String data_set) {
        if(OUTPUT_MODE == OUTPUT_MODE_FILE) {
        	to_file(stream.outlier_stream, "/org-" + data_set +".csv", stream.outlier_score);	
        }else{
        	System.out.println(out_tsv(stream.outlier_stream));
        } 
	}
	/**
	 * For outputting our streams to console.
	 * @param stream
	 * @return
	 */
	static String out_tsv(ArrayList<double[]> stream) {
		if(stream.get(0).length!=1) return "out_tsv(ArrayList<double[]>) No 1D Stream";
		String ret = "Stream\t";
		for(double[] ts : stream) {
			ret+=ts[0]+"\t";
		}
		return ret;
	}
	
	/**
	 * Creates and outputs the sanitized release stream for the all combinations of the vary-epsilon experiment
	 * @param mechanisms
	 * @param stream
	 * @param data_set
	 * @param epsilons
	 * @param num_iterations
	 * @param w
	 */
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
                    if(OUTPUT_MODE == OUTPUT_MODE_FILE) {
                    	to_file(sanStream, "/eps="+eps+"-" + data_set + "-" + m.name().replaceAll("\\s", "").replaceAll("_", "") + "-"+loop+".csv", stream.outlier_score);	
                    }else{
                    	System.out.println(out_tsv(sanStream));
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
    /**
     * Creates a CSV file having the expected format of the outlier detection framework
     * @param sanStream
     * @param file_name
     * @param outlier_score
     */
    static void to_file(ArrayList<double[]> sanStream, String file_name, double[] outlier_score){
    	try {
            FileWriter file = new FileWriter(CSV_DIR + file_name);
            BufferedWriter bufferedWriter = new BufferedWriter(file);
            String file_content = to_file_output_string(sanStream, outlier_score);
            bufferedWriter.append(file_content);
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    static boolean create_w_experiments = false; 
	/**
	 * Creates and outputs the sanitized release stream for the all combinations of the vary-w experiment
	 * @param mechanisms
	 * @param stream
	 * @param data_set
	 * @param epsilons
	 * @param num_iterations
	 * @param w
	 */
    static void create_sanitized_streams_vary_w(int[] mechanisms, OutlierStream stream, String data_set, double epsilon, int num_iterations, int[] ws) {
    	if(!create_w_experiments) return;
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
                    if(OUTPUT_MODE == OUTPUT_MODE_FILE) {
                    	to_file(sanStream, "/w="+w+"-" + data_set + "-" + m.name().replaceAll("\\s", "").replaceAll("_", "") + "-"+loop+".csv", stream.outlier_score);	
                    }else{
                    	System.out.println(out_tsv(sanStream));
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
    public static void load_dodgers_dataset() {
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
				System.err.println(before+" minutes != "+now);
			}
		}*/
		int num_timestamps_to_aggregate = 12;
		counts = aggregate(counts,num_timestamps_to_aggregate);
		/*for(Count c : counts) {
			System.out.println(c);		
		}*/
		
		OutlierStream my_stream = new OutlierStream(counts);
		System.out.println(my_stream);
		String data_set_name = "dodgers-"+num_timestamps_to_aggregate;
		write_non_private_stream(my_stream, data_set_name);
		
		//Create the sanitized releases
		create_sanitized_streams_vary_e(mechanisms, my_stream, data_set_name, epsilons, num_iterations, w);
        create_sanitized_streams_vary_w(mechanisms, my_stream, data_set_name, epsilon, num_iterations, w_s);
    }

    /**
     * Used to change the temporal resolution of a COUNT-Object stream, e.g., for the Dodgers stream.
     * Note, upon aggregation a time stamp is labeled outlier if input timestamps contain at least one outlier timestamp, i.e., outliers are dominant.
     * @param counts
     * @param num_timestamps_to_aggregate
     * @return
     */
	static ArrayList<Count> aggregate(ArrayList<Count> counts, int num_timestamps_to_aggregate) {
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
	/**
	 * The Dodgers stream has one file for for the data (.data) and another one for the outlier events. This method maps them together, s.t. there is a COUNT object for every timestamp.
	 * @param counts
	 * @param events
	 */
	static void label_outlier(ArrayList<Count> counts, ArrayList<Event> events) {
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
