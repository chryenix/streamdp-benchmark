package experiment;


import datagenerator.Generator;
import datagenerator.StreamScalerAndStretcher;
import mechanisms.*;

import java.io.*;
import java.util.*;

/**
 * This is the class that contains all the methods to start experiments, load data, and all enum to handle mechanisms etc.
 * 
 * @author Martin
 *
 */
public class Experiment {

    // Data sets
    public static final int WORLD_CUP 		= 0;
    public static final int FLU_NUM_DEATH 	= 1;
    public static final int STAT_FLU 		= 2;
    public static final int TDRIVE_EXTENDED = 3;
    public static final int ENERGY_DATA 	= 4;
    public static final int UNEMPLOY 		= 5;
    public static final int FAST_FLU_OUTPATIENT_EXTENDED = 6;
    public static final int WANG_TAXI_ECPK 	= 7;
    public static final int ADAPUB_RETAIL 	= 8;
    /**
     * Determines the used error measure
     */
    public static final int MAE = 0;
    /**
     * Determines the used error measure
     */
    public static final int ARE = 1;
    //Mechanisms
    public static final int UNIFROM 	= 0;
    public static final int SAMPLE 		= 1;
    public static final int BD 			= 2;
    public static final int BA 			= 3;
    public static final int BD_BA_MIX 	= 4;
    public static final int RESCUE_DP_MD= 5;
    public static final int RESCUE_DP_1D= 6;
    public static final int BD_GROUPING = 7;
    public static final int PEGASUS 	= 8;
    public static final int DSATWEVENT 	= 9;
    public static final int KalmanFilterPID_FAST_w = 10;
    public static final int ADAPUB 		= 11;

    //public static final String[] FILE_NAMES = {"Wang_Taxi_ECMLPKDD_d=3750_len=672.csv","FAST_Flu_Outpatient[5-24]_seasons06-10_d=1_len=209.csv","FAST_unemploy_black_women_[16-19]_d=1_len=478.csv","wc_kellaris_data1320","Flu_NumInfDeath[18-64]_seasons12-19_d=1_len=374.csv","StatFlu_infLikeIllness_d=51_len=492.csv","DSAT_Taxi_TDrive_d=100_len=169.csv","GEFComDay"};

    //Data set paths    
    static final String WORLD_CUP_DATA_SET = ".\\data\\wc_kellaris_data1320.csv";
    static final String STAT_FLU_DATA_SET = ".\\data\\StatFlu_infLikeIllness_d=51_len=492.csv";// old: NATIONAL_CUSTOMS_DATA_SET
    static final String TDRIVE_DATA_SET = ".\\data\\DSAT_Taxi_TDrive_d=100_len=168.csv";
    static final String FAST_FLU_OUTPATIENT_DATA_SET = ".\\data\\Flu_outpatient[5-24]-1997-2021_d=1_len1250.csv";
    static final String FAST_UNEMPLOY_DATA_SET = ".\\data\\FAST_unemploy_black_women_[16-19]_d=1_len=478.csv";
    static final String WANG_TAXI_ECPK_DATA_SET = ".\\data\\Wang_Taxi_ECMLPKDD_d=3750_len=672.csv";
    // unkown where used
    static final String INFLUENZA_NUM_DEATH_DATA_SET 	= ".\\data\\Flu_NumInfDeath[18-64]_seasons12-19_d=1_len=374.csv";
    private static final String ONLINE_RETAIL_DATA 		= ".\\data\\AdaPub-OnlineRetail-l=374_d=1289.csv";
    
    public static String RESLT_DIR = "./results";
    /**
     * Determines the used error measure. Use MAE or ARE.
     */
    public static int ERROR_MEASURE = MAE;
    /**
     * Query sensitivity \Delta Q. It equals 1 for all count and histogram queries.
     */
    static double sensitivity = 1.0d;

    public static void main(String[] args) {
        boolean runRealWoldExperiments = false;
        System.out.println("w-event Experimentator runRealWoldExperiments = " + runRealWoldExperiments);
        if (runRealWoldExperiments) {
            runRealWorldExperiments();
        } else {
            runArtificalExperiments();
        }
    }

    /**
     * 
     * @param mechanism e.g., Experiment.UNIFROM
     * @return
     */
    public static Mechansim getInstance(final int mechanism) {
        if (mechanism == UNIFROM) {
            return new Uniform();
        } else if (mechanism == SAMPLE) {
            return new Sample();
        } else if (mechanism == BD) {
            return new BD();
        } else if (mechanism == BA) {
            return new BA();
            //}else if(mechanism==BD_BA_MIX){
            //	return new BD_BA_MIX();
        } else if (mechanism == RESCUE_DP_MD) {
            return new RescueDP_MD();
        } else if (mechanism == RESCUE_DP_1D) {
            return new RescueDP1D();
        } else if (mechanism == BD_GROUPING) {
            return new BDwithGrouping();
        } else if (mechanism == PEGASUS) {
            return new Pegasus();
        } else if (mechanism == DSATWEVENT) {
            return new DSAT();
        } else if (mechanism == KalmanFilterPID_FAST_w) {
            return new FAST_w();
        } else if (mechanism == ADAPUB) {
            return new AdaPub();
        } else {
            System.err.println("No such mechnanism: " + mechanism);
            return null;
        }
    }

    /**
     * 
     * @param data e.g., Experiment.WORLD_CUP
     * @return
     */
    static ArrayList<double[]> load(final int data) {
        ArrayList<double[]> stream;
        if (data == WORLD_CUP) {
            stream = loadWorldCupSampled();
        } else if (data == FLU_NUM_DEATH) {
            stream = loadInfluenza();
        } else if (data == STAT_FLU) {
            stream = loadNationalCustoms();
            //}else if(data==ENERGY_DATA){
            //		return loadEnergyData();
        } else if (data == TDRIVE_EXTENDED) {
            stream = loadTDrive();
        } else if (data == UNEMPLOY) {
            stream = loadUnemploy();
        } else if (data == FAST_FLU_OUTPATIENT_EXTENDED) {
            stream = loadFluOutpatient();
        } else if (data == WANG_TAXI_ECPK) {
            stream = loadWangTaxiECPK();
        } else if (data == ADAPUB_RETAIL) {
            stream = loadRetail();
        } else {
            System.err.println("No such data set: " + data);
            return null;
        }

        int totalSumOverTime = 0;
        for (double[] ts : stream) {
            totalSumOverTime += Arrays.stream(ts).sum();
        }
        if (totalSumOverTime == 0) {//Sanity check
            System.err.println(data + " is empty!");
        }
        return stream;
    }

    static ArrayList<double[]> loadWangTaxiECPK() {
        final int LENGTH_TIME_SERIES = 672;
        final int HISTOGRAM_BINS = 3750;

        final int NO_SAMPLED_DIM = 1289;
        Random rand = new Random(31); // makes sure that we have dimensions with values > 0
		Set<Integer> sampledDim = new HashSet<Integer>();
        int[] dims = (rand.ints(NO_SAMPLED_DIM, 0, HISTOGRAM_BINS).sorted().toArray());
        for (int d : dims) {
            sampledDim.add(d);
        }
        while (sampledDim.size() != NO_SAMPLED_DIM) {
            sampledDim.add(rand.nextInt(HISTOGRAM_BINS));
        }

        double[][] histogram = new double[LENGTH_TIME_SERIES][NO_SAMPLED_DIM]; //raw data
        LoadWangTaxi(Experiment.WANG_TAXI_ECPK_DATA_SET, histogram, sampledDim);

        //to my format
        ArrayList<double[]> stream = new ArrayList<double[]>(LENGTH_TIME_SERIES);
        for (int t = 0; t < LENGTH_TIME_SERIES; t++) {
            stream.add(histogram[t]);
        }
        return stream;
    }

    static ArrayList<double[]> loadFluOutpatient() {
        final int LENGTH_TIME_SERIES = 1250;
        //final int HISTOGRAM_BINS	 = 1;
        double[] histogram = new double[LENGTH_TIME_SERIES]; //raw data
        loadDataUnemploy(Experiment.FAST_FLU_OUTPATIENT_DATA_SET, histogram); //same format, so can use same method
        //to my format
        ArrayList<double[]> stream = new ArrayList<double[]>(LENGTH_TIME_SERIES);
        for (int t = 0; t < LENGTH_TIME_SERIES; t++) {
            double[] temp = new double[1];//bissl stupid, aber so ist halt die API...
            temp[0] = histogram[t];
            stream.add(temp);
        }
        return stream;
    }

    static ArrayList<double[]> loadUnemploy() {
        final int LENGTH_TIME_SERIES = 478;
        //final int HISTOGRAM_BINS	 = 1;
        double[] histogram = new double[LENGTH_TIME_SERIES]; //raw data

        loadDataUnemploy(Experiment.FAST_UNEMPLOY_DATA_SET, histogram);
        //to my format
        ArrayList<double[]> stream = new ArrayList<double[]>(LENGTH_TIME_SERIES);
        for (int t = 0; t < LENGTH_TIME_SERIES; t++) {
            double[] temp = new double[1];//bissl stupid, aber so ist halt die API...
            temp[0] = histogram[t];
            stream.add(temp);
        }
        return stream;
    }

    static ArrayList<double[]> loadInfluenza() {
        final int LENGTH_TIME_SERIES = 374;
        //final int HISTOGRAM_BINS	 = 1;
        double[] histogram = new double[LENGTH_TIME_SERIES]; //raw data
        LoadDataInfluenza(Experiment.INFLUENZA_NUM_DEATH_DATA_SET, histogram);
        //to my format
        ArrayList<double[]> stream = new ArrayList<double[]>(LENGTH_TIME_SERIES);
        for (int t = 0; t < LENGTH_TIME_SERIES; t++) {
            double[] temp = new double[1];//bissl stupid, aber so ist halt die API...
            temp[0] = histogram[t];
            stream.add(temp);
        }
        return stream;
    }

    static void LoadDataInfluenza(String filename, double[] stream) {
        File trajFile = new File(filename);
        int counter = 0;
        try {
            if (trajFile.exists()) {
                BufferedReader inFile = new BufferedReader(new FileReader(trajFile));
                inFile.readLine();//skip first line
                String line;
                while ((line = inFile.readLine()) != null) {
                    java.util.StringTokenizer st = new java.util.StringTokenizer(line, ",");
                    st.nextToken();//skip id
                    int c = Integer.parseInt(st.nextToken());
                    stream[counter] = (double) (c);
                    counter++;
                }
                inFile.close();
            } else {
                System.err.println("File not found");
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    static void loadDataUnemploy(String filename, double[] stream) {
        File trajFile = new File(filename);
        int counter = 0;
        try {
            if (trajFile.exists()) {
                BufferedReader inFile = new BufferedReader(new FileReader(trajFile));
                //inFile.readLine();//skip first line
                String line;
                while ((line = inFile.readLine()) != null) {

                    int c = Integer.parseInt(line);
                    stream[counter] = (double) (c);
                    counter++;
                }
                inFile.close();
            } else {
                System.err.println("File not found");
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    static ArrayList<double[]> loadNationalCustoms() {
        final int LENGTH_TIME_SERIES = 492;
        final int HISTOGRAM_BINS = 51;//#US states
        double[][] histogram = new double[LENGTH_TIME_SERIES][HISTOGRAM_BINS]; //raw data
        LoadDataNationalCustoms(Experiment.STAT_FLU_DATA_SET, histogram);
        //to my format
        ArrayList<double[]> stream = new ArrayList<double[]>(LENGTH_TIME_SERIES);
        for (int t = 0; t < LENGTH_TIME_SERIES; t++) {
            stream.add(histogram[t]);
        }
        return stream;
    }

    static ArrayList<double[]> loadTDrive() {
        final int LENGTH_TIME_SERIES = 168 * 4; // 168 * 4 => strecken auf vier wochen daten
        final int HISTOGRAM_BINS = 100;//#locations
        double[][] histogram = new double[LENGTH_TIME_SERIES][HISTOGRAM_BINS]; //raw data
        LoadDataTDriveExtendedToFourWeeks(Experiment.TDRIVE_DATA_SET, histogram);
        //to my format
        ArrayList<double[]> stream = new ArrayList<double[]>(LENGTH_TIME_SERIES);
        for (int t = 0; t < LENGTH_TIME_SERIES; t++) {
            stream.add(histogram[t]);
        }
        return stream;
    }

    static void LoadDataTDriveExtendedToFourWeeks(String filename, double[][] histogram) {
        File trajFile = new File(filename);
        int timestamp = 0;
        int len_ts_oneweek = histogram.length / 4;
        try {
            if (trajFile.exists()) {
                BufferedReader inFile = new BufferedReader(new FileReader(trajFile));
                inFile.readLine();//skip CSV head
                String line = inFile.readLine();
                while (line != null) {
                    java.util.StringTokenizer st = new java.util.StringTokenizer(line, ",");
                    st.nextToken();//skip index date

                    double[] line_d = histogram[timestamp];
                    for (int i = 0; i < line_d.length; i++) {
                        int c = Integer.parseInt(st.nextToken());
                        line_d[i] = (double) (c);
                    }
                    //extend to 4 weeks
                    for (int offset_scale = 1; offset_scale < 4; offset_scale++) {
                        int offsent_scale_timestamps_later = timestamp + offset_scale * len_ts_oneweek;
                        double[] line_extend = histogram[offsent_scale_timestamps_later];
                        for (int i = 0; i < line_d.length; i++) {
                            line_extend[i] = line_d[i];
                        }
                    }
                    timestamp++;
                    line = inFile.readLine();
                }
                inFile.close();
            } else {
                System.err.println("File not found");
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    static void LoadDataNationalCustoms(String filename, double[][] histogram) {
        File trajFile = new File(filename);
        int counter = 0;
        try {
            if (trajFile.exists()) {
                BufferedReader inFile = new BufferedReader(new FileReader(trajFile));
                inFile.readLine();//skip CSV head
                String line = inFile.readLine();
                while (line != null) {
                    java.util.StringTokenizer st = new java.util.StringTokenizer(line, ",");
                    st.nextToken();
                    st.nextToken();
                    double[] line_d = histogram[counter];
                    for (int i = 0; i < line_d.length; i++) {
                        int c = Integer.parseInt(st.nextToken());
                        line_d[i] = (double) (c);
                    }
                    counter++;
                    line = inFile.readLine();
                }
                inFile.close();
            } else {
                System.err.println("File does not exist! " + filename);
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    static void LoadWangTaxi(String filename, double[][] histogram, Collection<Integer> sampledDim) {
        File trajFile = new File(filename);
        int timestamp = 0;
        //System.out.println("Load Wang Taxi -- use " + sampledDim.size() + " dimensions");
        //System.out.println(Mechansim.outCSV(sampledDim));
        try {
            if (trajFile.exists()) {
                BufferedReader inFile = new BufferedReader(new FileReader(trajFile));
                String line = inFile.readLine();
                while (line != null) {
                    java.util.StringTokenizer st = new java.util.StringTokenizer(line, ",");
                    int numberDimensionsInFile = st.countTokens();
                    //		st.nextToken();
                    //			st.nextToken();
                    double[] shallBeAllSampledDimensionsOfOneTimestamp = histogram[timestamp];

                    int indx_dim = 0;
                    for (int dim = 0; dim < numberDimensionsInFile; dim++) {
                        if (sampledDim.contains(dim)) {
                            int c = (int) Double.parseDouble(st.nextToken());
                            shallBeAllSampledDimensionsOfOneTimestamp[indx_dim] = (double) (c);
                            indx_dim++;
                        } else {
                            //skip it
                            st.nextToken();
                        }
                    }
                    timestamp++;
                    line = inFile.readLine();
                }
                inFile.close();
            } else {
                System.err.println("File does not exist! " + filename);
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    static ArrayList<double[]> loadWorldCupSampled() {

        ArrayList<double[]> orig_histogram = loadWorldCup();
        ArrayList<double[]> sampled_histogram = new ArrayList<>(orig_histogram.size());

        int origNoDim = orig_histogram.get(0).length;

        final int NO_SAMPLED_DIM = 1289;
        Random rand = new Random(12345);
        HashSet<Integer> sampledDim = new HashSet<>();
        while (sampledDim.size() != NO_SAMPLED_DIM) {
            sampledDim.add(rand.nextInt(origNoDim));
        }
        System.out.println(Mechansim.outCSV(sampledDim));
        int[] sampled_dims = new int[sampledDim.size()];
        int i = 0;
        for (Integer dim : sampledDim) {
            sampled_dims[i++] = dim;
        }
        Arrays.sort(sampled_dims);

        for (int t = 0; t < orig_histogram.size(); t++) {
            double[] temp = new double[sampled_dims.length];
            sample(orig_histogram.get(t), temp, sampled_dims);
            sampled_histogram.add(temp);
        }
        return sampled_histogram;
    }

    static void sample(final double[] orig_histogram_t, final double[] sampled_histogram_t, final int[] sampled_dims) {
        for (int i = 0; i < sampled_dims.length; i++) {
            int dim = sampled_dims[i];
            sampled_histogram_t[i] = orig_histogram_t[dim];
        }
    }

    static ArrayList<double[]> loadWorldCup() {
        final int LENGTH_TIME_SERIES = 1320; //number of timestamps to read
        final int HISTOGRAM_BINS = 89997; //number of columns, i.e., location

        double[][] histogram = new double[LENGTH_TIME_SERIES][HISTOGRAM_BINS]; //raw data
        LoadData(Experiment.WORLD_CUP_DATA_SET, histogram);
        //to my format
        ArrayList<double[]> stream = new ArrayList<double[]>(LENGTH_TIME_SERIES);
        for (int t = 0; t < LENGTH_TIME_SERIES; t++) {
            stream.add(histogram[t]);
        }
        return stream;
    }

    /**
     * Reduce the dimensionality of the data set to the first *dim* dimensions.
     *
     * @param dim
     * @return
     */
    static ArrayList<double[]> loadWorldCup(int dim) {
    	final int LENGTH_TIME_SERIES = 1320; //number of timestamps to read
        final int HISTOGRAM_BINS = 89997; //number of columns, i.e., location
        
        double[][] histogram = new double[LENGTH_TIME_SERIES][HISTOGRAM_BINS]; //raw data
        LoadData(Experiment.WORLD_CUP_DATA_SET, histogram);
        //to my format
        ArrayList<double[]> stream = new ArrayList<double[]>(LENGTH_TIME_SERIES);
        int new_dim = Math.min(HISTOGRAM_BINS, dim);
        for (int t = 0; t < LENGTH_TIME_SERIES; t++) {
            double[] temp = new double[new_dim];
            System.arraycopy(histogram[t], 0, temp, 0, temp.length);
            stream.add(temp);
        }
        return stream;
    }

    static ArrayList<double[]> loadRetail() {
        final int LENGTH_TIME_SERIES = 374;
        final int HISTOGRAM_BINS = 1289;
        double[][] histogram = new double[LENGTH_TIME_SERIES][HISTOGRAM_BINS]; //raw data
        LoadRetailData(Experiment.ONLINE_RETAIL_DATA, histogram);
        //to my format
        ArrayList<double[]> stream = new ArrayList<double[]>(LENGTH_TIME_SERIES);
        for (int t = 0; t < LENGTH_TIME_SERIES; t++) {
            stream.add(histogram[t]);
        }
        return stream;
    }

    //read data and store them in histogram
    //each row in the data file is a column
    //for each column the value of each timestamp is seperated by a ';'
    static void LoadData(String filename, double[][] histogram) {
        File trajFile = new File(filename);
        int counter = 0;
        try {
            if (trajFile.exists()) {
                BufferedReader inFile = new BufferedReader(new FileReader(trajFile));
                String line = inFile.readLine();
                while (line != null) {
                    java.util.StringTokenizer st = new java.util.StringTokenizer(line, ";");
                    st.nextToken();
                    st.nextToken();
                    for (int i = 0; i < histogram.length; i++) {
                        int c = Integer.parseInt(st.nextToken());
                        histogram[i][counter] = (double) (c);
                    }
                    counter++;
                    line = inFile.readLine();
                }
                inFile.close();
            } else {
                System.err.println("File does not exist! " + filename);
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    static void LoadRetailData(String filename, double[][] histogram) {
        File trajFile = new File(filename);
        int counter = 0;
        try {
            if (trajFile.exists()) {
                BufferedReader inFile = new BufferedReader(new FileReader(trajFile));
                String line = inFile.readLine();
                line = inFile.readLine(); //skip header
                while (line != null) {
                    java.util.StringTokenizer st = new java.util.StringTokenizer(line, ",");
                    st.nextToken(); // skip the date
                    for (int i = 0; i < histogram.length; i++) {
                        int c = Integer.parseInt(st.nextToken());
                        histogram[i][counter] = (double) (c);
                    }
                    counter++;
                    line = inFile.readLine();
                }
                inFile.close();
            } else {
                System.err.println("File does not exist! " + filename);
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    /**
     * Runs all provided mechanism for w in [start_w,stop_w) with increment of 1 on the given data set
     * 
     * @param mechanisms List of mechanism enums, e.g., [Experiment.UNIFROM, Experiment.SAMPLE]
     * @param start_w
     * @param stop_w
     * @param data_set e.g., Experiment.WORLD_CUP
     */
    static void runAll(int[] mechanisms, int start_w, int stop_w, int data_set) {
        System.out.println("w-event Experimentator for " + Arrays.toString(mechanisms));
        System.out.print("Load Data " + data_set);
        double start = System.currentTimeMillis();
        ArrayList<double[]> stream = Experiment.load(data_set);
        System.out.println(+stream.size() + " [done] in " + (System.currentTimeMillis() - start) + " ms");
        Arrays.sort(mechanisms);
        double[][] mae_results = new double[stop_w][mechanisms.length];
        String table_head = "";

        for (int i = 0; i < mechanisms.length; i++) {
            Mechansim m = getInstance(mechanisms[i]);


            table_head += "\t" + m.name();
            for (int w = start_w; w < stop_w; w++) {
                start = System.currentTimeMillis();
                double mae = Mechansim.mae(stream, m.run(stream, w, 1.0));
                System.out.println(w + "\t" + mae + "\tin\t" + (System.currentTimeMillis() - start));
                mae_results[w][i] = mae;
            }
        }

        System.out.println("Data=" + data_set + " with dim=" + stream.get(0).length + " length=" + stream.size());
        System.out.println(table_head);
        for (int w = start_w; w < stop_w; w++) {
            System.out.println(w + "\t" + Mechansim.outTSV(mae_results[w]));
        }
    }

    /**
     * Method to start vary-w experiments. The method intrinsically calls runAll_wHist2(int[] mechanisms, ArrayList<double[]> stream, String data_set, int[] w_s, int num_iterations, double eps, boolean printHist)
     * 
     * @param mechanisms
     * @param data_set
     * @param w_s
     * @param num_iterations
     * @param eps
     * @param printHist
     */
    static void runAll_wHist2(int[] mechanisms, int data_set, int[] w_s, int num_iterations, double eps, boolean printHist) {
        System.out.print("Load Data " + data_set + " ");
        ArrayList<double[]> stream = Experiment.load(data_set);
        runAll_wHist2(mechanisms, stream, String.valueOf(data_set), w_s, num_iterations, eps, printHist);
    }

    /**
     * Method to start vary-w experiments. 
     * 
     * @param mechanisms
     * @param data_set
     * @param w_s
     * @param num_iterations
     * @param eps
     * @param printHist
     */
    static void runAll_wHist2(int[] mechanisms, ArrayList<double[]> stream, String data_set, int[] w_s, int num_iterations, double eps, boolean printHist) {
        System.out.println("w-event Experimentator for " + Arrays.toString(mechanisms) + " w in " + Arrays.toString(w_s));
        double start = System.currentTimeMillis();
        Arrays.sort(mechanisms);
        Arrays.sort(w_s);

        System.out.println(+stream.size() + " [done] in " + (System.currentTimeMillis() - start) + " ms");

        double[][][] mae_results = new double[w_s.length][mechanisms.length][num_iterations];
        double[][][] mre_results = new double[w_s.length][mechanisms.length][num_iterations];
        double[][] mae_resultsAgg = new double[w_s.length][mechanisms.length];
        double[][] mre_resultsAgg = new double[w_s.length][mechanisms.length];

        String table_head = Mechansim.outTSV(w_s) + "\n";
        String table_headMAEAgg = "" + MAE;// + "\t"+m.name().replaceAll("\\s","").replaceAll("_","");
        String table_headMREAgg = "" + ARE;// + "\t"+m.name().replaceAll("\\s","").replaceAll("_","");

        for (int i = 0; i < mechanisms.length; i++) {
            Mechansim m = getInstance(mechanisms[i]);
            table_headMAEAgg += "\t" + m.name().replaceAll("\\s", "").replaceAll("_", "");
            table_headMREAgg += "\t" + m.name().replaceAll("\\s", "").replaceAll("_", "");
            try {
                FileWriter mae_file = new FileWriter(RESLT_DIR + "/w-" + MAE + "-" + data_set + "-" + m.name().replaceAll("\\s", "").replaceAll("_", "") + ".tsv");
                BufferedWriter bufferedWriterMAE = new BufferedWriter(mae_file);
                FileWriter mre_file = new FileWriter(RESLT_DIR + "/w-" + ARE + "-" + data_set + "-" + m.name().replaceAll("\\s", "").replaceAll("_", "") + ".tsv");
                BufferedWriter bufferedWriterMRE = new BufferedWriter(mre_file);
                //System.out.println(table_head);

                for (int w_i = 0; w_i < w_s.length; w_i++) {
                    LaplaceStream.setLine(0);
                    m.epsilon = eps;
                    m.w = w_s[w_i];
                    int w = w_s[w_i];
                    start = System.currentTimeMillis();

                    for (int loop = 0; loop < num_iterations; loop++) {
                        double mae_error, are_error;
                        mae_error = Mechansim.mae(stream, m.run(stream, w, eps));
                        are_error = Mechansim.are(stream, m.run(stream, w, eps));
                        //System.out.println(w+"\t"+error+"\tin\t"+(System.currentTimeMillis()-start));
                        mae_results[w_i][i][loop] = mae_error;
                        mre_results[w_i][i][loop] = are_error;
                        mae_resultsAgg[w_i][i] += mae_error;
                        mre_resultsAgg[w_i][i] += are_error;

                        LaplaceStream.line();
                    }
                    mae_resultsAgg[w_i][i] /= num_iterations;

                    mre_resultsAgg[w_i][i] /= num_iterations;
                    System.out.println("data set=" + data_set + ", w=" + w + ", mechanism=" + m.name() + " took " + Math.abs(start - System.currentTimeMillis()) / 1000l + " sec. for " + num_iterations + " iterations");
                }
                // write to file (need one line per iteration..)
                if (printHist) {
                    bufferedWriterMAE.write(table_head);
                    bufferedWriterMRE.write(table_head);

                    for (int loop = 0; loop < num_iterations; loop++) {
                        String lineMAE = "";
                        String lineMRE = "";

                        for (int w_i = 0; w_i < w_s.length; w_i++) {
                            if (w_i > 0) {
                                lineMAE += "\t";
                                lineMRE += "\t";
                            }
                            lineMAE += mae_results[w_i][i][loop];
                            lineMRE += mre_results[w_i][i][loop];
                        }
                        //line += "\t";
                        bufferedWriterMAE.write(lineMAE + "\n");
                        bufferedWriterMRE.write(lineMRE + "\n");
                    }
                }
                bufferedWriterMAE.close();
                bufferedWriterMRE.close();
            } catch (IOException e) {
                e.printStackTrace();
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

        //printing
        FileWriter fileMaeAgg = null;
        BufferedWriter bufferedWriterMAEAgg = null;
        FileWriter fileAREAgg = null;
        BufferedWriter bufferedWriterAREAgg = null;
        try {
            fileMaeAgg = new FileWriter(RESLT_DIR + "/w-" + MAE + "-" + data_set + "-laplaceMode" + LaplaceStream.USAGE_MODE + ".tsv");

            bufferedWriterMAEAgg = new BufferedWriter(fileMaeAgg);
            fileAREAgg = new FileWriter(RESLT_DIR + "/w-" + ARE + "-" + data_set + "-laplaceMode" + LaplaceStream.USAGE_MODE + ".tsv");

            bufferedWriterAREAgg = new BufferedWriter(fileAREAgg);

            System.out.println(table_headMAEAgg);
            bufferedWriterMAEAgg.write(table_headMAEAgg + "\n");
            bufferedWriterAREAgg.write(table_headMREAgg + "\n");
            for (int w_i = 0; w_i < w_s.length; w_i++) {
                double w = w_s[w_i];
                System.out.println(w + "\t" + Mechansim.outTSV(mae_resultsAgg[w_i]));
                bufferedWriterMAEAgg.write(w + "\t" + Mechansim.outTSV(mae_resultsAgg[w_i]) + "\n");

                bufferedWriterAREAgg.write(w + "\t" + Mechansim.outTSV(mre_resultsAgg[w_i]) + "\n");

            }
            bufferedWriterMAEAgg.close();
            bufferedWriterAREAgg.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Data=" + data_set + " with dim=" + stream.get(0).length + " length=" + stream.size());
    }

    /**
     * Method to start vary \epsilon experiments. It intrinsically calls runAll_eHist(int[] mechanisms, ArrayList<double[]> stream, String data_set, double[] epsilons, int num_iterations, int w, boolean writeHist)
     * 
     * @param mechanisms
     * @param data_set
     * @param epsilons
     * @param num_iterations
     * @param w
     * @param writeHist
     */
    static void runAll_eHist(int[] mechanisms, int data_set, double[] epsilons, int num_iterations, int w, boolean writeHist) {
        System.out.print("Load Data " + data_set + " ");
        ArrayList<double[]> stream = Experiment.load(data_set);
        runAll_eHist(mechanisms, stream, String.valueOf(data_set), epsilons, num_iterations, w, writeHist);
    }

    /**
     * Method to start \epsilon experiments.
     * 
     * @param mechanisms
     * @param stream
     * @param data_set
     * @param epsilons
     * @param num_iterations
     * @param w
     * @param writeHist
     */
    static void runAll_eHist(int[] mechanisms, ArrayList<double[]> stream, String data_set, double[] epsilons, int num_iterations, int w, boolean writeHist) {
        System.out.println("w-event Experimentator for mechanisms " + Arrays.toString(mechanisms) + " e in " + Arrays.toString(epsilons) + " and w = " + w);
        double start = System.currentTimeMillis();
        Arrays.sort(mechanisms);
        Arrays.sort(epsilons);

        System.out.println(+stream.size() + " [done] in " + (System.currentTimeMillis() - start) + " ms");

        // num groups = 2 promille
        BDwithGrouping.NUM_GROUPS = (int) Math.max(1, (int) stream.get(0).length * 0.002);
        System.out.println("BDNumGroups " + BDwithGrouping.NUM_GROUPS);

        double[][][] mae_results = new double[epsilons.length][mechanisms.length][num_iterations];
        double[][][] mre_results = new double[epsilons.length][mechanisms.length][num_iterations];
        double[][] mae_resultsAgg = new double[epsilons.length][mechanisms.length];
        double[][] mre_resultsAgg = new double[epsilons.length][mechanisms.length];

        String table_head = Mechansim.outTSV(epsilons) + "\n";
        String table_headMAEAgg = "" + MAE;// + "\t"+m.name().replaceAll("\\s","").replaceAll("_","");
        String table_headMREAgg = "" + ARE;// + "\t"+m.name().replaceAll("\\s","").replaceAll("_","");

        for (int i = 0; i < mechanisms.length; i++) {
            Mechansim m = getInstance(mechanisms[i]);
            System.out.println(m.name());

            table_headMAEAgg += "\t" + m.name().replaceAll("\\s", "").replaceAll("_", "");
            table_headMREAgg += "\t" + m.name().replaceAll("\\s", "").replaceAll("_", "");
            try {
                FileWriter mae_file = new FileWriter(RESLT_DIR + "/eps-" + MAE + "-" + data_set + "-" + m.name().replaceAll("\\s", "").replaceAll("_", "") + ".tsv");
                BufferedWriter bufferedWriterMAE = new BufferedWriter(mae_file);
                FileWriter mre_file = new FileWriter(RESLT_DIR + "/eps-" + ARE + "-" + data_set + "-" + m.name().replaceAll("\\s", "").replaceAll("_", "") + ".tsv");
                BufferedWriter bufferedWriterMRE = new BufferedWriter(mre_file);
                //System.out.println(table_head);

                for (int e_i = 0; e_i < epsilons.length; e_i++) {
                    LaplaceStream.setLine(0);
                    double eps = epsilons[e_i];
                    m.epsilon = eps;
                    for (int loop = 0; loop < num_iterations; loop++) {
                        start = System.currentTimeMillis();
                        double mae_error, are_error;
                        ArrayList<double[]> sanStream = m.run(stream, w, eps);
                        mae_error = Mechansim.mae(stream, sanStream);
                        are_error = Mechansim.are(stream, sanStream);
                        //System.out.println(w+"\t"+error+"\tin\t"+(System.currentTimeMillis()-start));
                        mae_results[e_i][i][loop] = mae_error;
                        mre_results[e_i][i][loop] = are_error;
                        mae_resultsAgg[e_i][i] += mae_error;
                        mre_resultsAgg[e_i][i] += are_error;

                        LaplaceStream.line();
                    }
                    mae_resultsAgg[e_i][i] /= num_iterations;
                    mre_resultsAgg[e_i][i] /= num_iterations;
                }
                // write to file (need one line per iteration..)
                if (writeHist) {
                    bufferedWriterMAE.write(table_head);
                    bufferedWriterMRE.write(table_head);

                    for (int loop = 0; loop < num_iterations; loop++) {
                        String lineMAE = "";
                        String lineMRE = "";

                        for (int e_i = 0; e_i < epsilons.length; e_i++) {
                            if (e_i > 0) {
                                lineMAE += "\t";
                                lineMRE += "\t";
                            }
                            lineMAE += mae_results[e_i][i][loop];
                            lineMRE += mre_results[e_i][i][loop];
                        }
                        //line += "\t";
                        bufferedWriterMAE.write(lineMAE + "\n");
                        bufferedWriterMRE.write(lineMRE + "\n");
                    }
                }
                bufferedWriterMAE.close();
                bufferedWriterMRE.close();
            } catch (IOException e) {
                e.printStackTrace();
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

        //printing
        FileWriter fileMaeAgg = null;
        BufferedWriter bufferedWriterMAEAgg = null;
        FileWriter fileAREAgg = null;
        BufferedWriter bufferedWriterAREAgg = null;
        try {
            fileMaeAgg = new FileWriter(RESLT_DIR + "/eps-" + MAE + "-" + data_set + "-laplaceMode" + LaplaceStream.USAGE_MODE + ".tsv");

            bufferedWriterMAEAgg = new BufferedWriter(fileMaeAgg);
            fileAREAgg = new FileWriter(RESLT_DIR + "/eps-" + ARE + "-" + data_set + "-laplaceMode" + LaplaceStream.USAGE_MODE + ".tsv");

            bufferedWriterAREAgg = new BufferedWriter(fileAREAgg);

            System.out.println(table_headMAEAgg);
            bufferedWriterMAEAgg.write(table_headMAEAgg + "\n");
            bufferedWriterAREAgg.write(table_headMREAgg + "\n");
            for (int e_i = 0; e_i < epsilons.length; e_i++) {
                double eps = epsilons[e_i];
                System.out.println(eps + "\t" + Mechansim.outTSV(mae_resultsAgg[e_i]));
                bufferedWriterMAEAgg.write(eps + "\t" + Mechansim.outTSV(mae_resultsAgg[e_i]) + "\n");
                bufferedWriterAREAgg.write(eps + "\t" + Mechansim.outTSV(mre_resultsAgg[e_i]) + "\n");
            }
            bufferedWriterMAEAgg.close();
            bufferedWriterAREAgg.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Data=" + data_set + " with dim=" + stream.get(0).length + " length=" + stream.size());
    }

  
    static int[] getArray(int start, int stop) {
        int[] array = new int[stop - start];
        for (int i = 0; i < stop - start; i++) {
            array[i] = start + i;
        }
        return array;
    }

    public static void runArtificalExperiments() {
        double start = System.currentTimeMillis();
        Mechansim.TRUNCATE = false;// We recommend to always deactivate truncating for tests on artificial data
        RescueDP1D.USE_KALMAN_FILTER = true;
        LaplaceStream.USAGE_MODE = LaplaceStream.USAGE_MODE_RANDOM;
        int num_iterations_per_Mechanism = 100;
        int num_iterations_per_dataParameterCombi = 1;
        int[] all_mechanisms = {KalmanFilterPID_FAST_w, UNIFROM, SAMPLE, BD, BA, RESCUE_DP_MD, PEGASUS, DSATWEVENT, ADAPUB};
        int[] mechanisms = all_mechanisms;

        double[] eps = {0.1, 0.3, 0.5, 0.7, 0.9, 1.0};
        int streamLength = 1000;
        int w = 120;
        double epsilon = 1.0;
        int[] w_s = {40, 80, 120, 160, 200};

        int[] desriredStreamMaxValues = {10, 100, 1000, 10000};
        double basis = 1.3;
        int[] pLen = {40, 60, 80, 100, 120};

        for (int period_length : pLen) {
            Generator gen = new Generator(streamLength, period_length, basis);
            StreamScalerAndStretcher s = new StreamScalerAndStretcher(gen, num_iterations_per_Mechanism);
            for (int desired_max_value : desriredStreamMaxValues) {
                for (int i = 0; i < num_iterations_per_dataParameterCombi; i++) {
                    ArrayList<double[]> data_set = s.get_stream(i, desired_max_value, 1);
                    String data_set_identifier = "period_length" + period_length + "_a" + desired_max_value + "_iter" + i;
                    runAll_eHist(mechanisms, data_set, data_set_identifier, eps, num_iterations_per_Mechanism, w, true);
                    runAll_wHist2(mechanisms, data_set, data_set_identifier, w_s, num_iterations_per_Mechanism, epsilon, true);
                }
                System.out.println("****************** End of period_length=" + period_length);
            }
        }

        System.out.println("[DONE] Experimentor in " + (System.currentTimeMillis() - start) + " ms");
    }

    @SuppressWarnings("unused")
    public static void runRealWorldExperiments() {
        double start = System.currentTimeMillis();
        Mechansim.TRUNCATE = true;
        RescueDP1D.USE_KALMAN_FILTER = true;

        int num_iterations = 100;
        final int[] data_sets_1d = {FAST_FLU_OUTPATIENT_EXTENDED, UNEMPLOY, FLU_NUM_DEATH};
        final int[] data_sets_md = {STAT_FLU, TDRIVE_EXTENDED, ADAPUB_RETAIL, WORLD_CUP, WANG_TAXI_ECPK}; //WORLD_CUP, WANG_TAXI_ECPK,
        final int[] all_data_sets= {FAST_FLU_OUTPATIENT_EXTENDED, UNEMPLOY, FLU_NUM_DEATH, STAT_FLU, TDRIVE_EXTENDED, ADAPUB_RETAIL, WORLD_CUP, WANG_TAXI_ECPK};

        final int[] data_sets = all_data_sets; //{ADAPUB_RETAIL}; //; {TDRIVE_EXTENDED}; //data_sets_1d; //  // { WORLD_CUP}; //data_sets_md; //data_sets_1d; //{FAST_FLU_OUTPATIENT_EXTENDED, UNEMPLOY, INFLUENZA_NUM_DEATH};
        for (int data_set : data_sets) {//FIXME all data sets

            int[] all_mechanisms = {KalmanFilterPID_FAST_w, UNIFROM, SAMPLE, BD, BA, RESCUE_DP_MD, PEGASUS, DSATWEVENT, ADAPUB};
            int[] mechanisms = all_mechanisms;//
            //int[] mechanisms =  all_mechanisms;
            //int[] mechanisms = {UNIFROM, BD, BD_GROUPING, BA, SAMPLE,KalmanFilterPID_FAST_w,  DSATWEVENT, ADAPUB};//PEGASUS, RESCUE_DP_MD,
            //int[] mechanisms = {UNIFROM, PEGASUS};
            //int[] mechanisms = all_mechanisms;
            //int[] mechanisms = {RESCUE_DP_1D, BD,BD};
            //int[] mechanisms = {UNIFROM,DSATUserLevel};
            //int[] w_s = getArray(start_w, stop_w);
            int[] w_s = {40, 80, 120, 160, 200};
            //int[] w_s = {200};

            double[] eps = {0.1, 0.3, 0.5, 0.7, 0.9, 1.0};
            // double[] eps = {1.0};

            int w = 120;
            double epsilon = 1.0;

            /*LaplaceStream.USAGE_MODE = LaplaceStream.USAGE_MODE_STUPID;
            runAll_eHist(mechanisms, data_set, eps, 1, w, false);
            runAll_wHist2(mechanisms, data_set, w_s, 1, epsilon, false);*/
            
            LaplaceStream.USAGE_MODE = LaplaceStream.USAGE_MODE_RANDOM;
            runAll_eHist(mechanisms, data_set, eps, num_iterations, w, true);
            runAll_wHist2(mechanisms, data_set, w_s, num_iterations, epsilon, true);

            System.out.println("[DONE] Experimentor in " + (System.currentTimeMillis() - start) + " ms");
        }
    }
}
