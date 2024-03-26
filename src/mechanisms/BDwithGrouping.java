package mechanisms;

import util.MyArrayList;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Re-implementation of BD mechanism without grouping from 
 * G. Kellaris et al. 2014. Differentially Private Event Sequences over Infinite Streams. Proc. VLDB Endow. 7, 12 (2014), 1155-1166.
 * 
 * @author b1074672
 *
 */
public class BDwithGrouping extends Mechansim {
    static final int FROM = 0;
    static final int TO = 1;
    public static int NUM_GROUPS = 1;
    /**
     * After PERIOD time stamps, we perform a re-grouping.
     */
    private static int PERIOD = 10;
    /**
     * This one is essential, each partition that wants to sample updates its values, while the others simply do nothing. Then, we publish the whole vector-copy.
     */
    double[] last_published;
    /**
     * Contains for each group the from-to indexes of locations after weighting their change.
     */
    int[][] FROM_TO;
    /**
     * Noise scale of M_1 is the same for each Partition, and does not change over time, even if we re-assign the locations.
     */
    double lambda_1_per_timestamp;
    /**
     * Budget of M_2. It is the same for each Partition, and does not change over time, even if we re-assign the locations.
     */
    double epsilon_2_per_window;
    /**
     * Overall budget usage per dimension [time][dim]
     */
    double[][] used_budget;
    /**
     * budget spent in current window
     */
    double used_budget_in_window[];
    ArrayList<double[]> sanitized_stream = null;

    public BDwithGrouping() {
        super();
    }

    public BDwithGrouping(int w, double epsilon) {
        super(w, epsilon);
    }

    /**
     * Optimization using pre-allocated buffers from last run if possible.
     *
     * @param length
     * @param dim
     */
    void initReleaseStream(int length, int dim) {
        //pre-allocate memory for output
        if (sanitized_stream == null) {
            this.sanitized_stream = new ArrayList<double[]>(length);
            for (int t = 0; t < length; t++) {
                sanitized_stream.add(t, new double[dim]);
            }
        } else {// we do not change the data set within one instance
            if (sanitized_stream.size() != length) {
                System.err.println("initReleaseStream() length does not fit: Did you change the data set?");
                sanitized_stream = null;
                initReleaseStream(length, dim);
            }
            if (sanitized_stream.get(0).length != dim) {
                System.err.println("initReleaseStream() dim does not fit: Did you change the data set?");
                sanitized_stream = null;
                initReleaseStream(length, dim);
            }
        }
    }

    public ArrayList<double[]> run(ArrayList<double[]> org_stream) {
        final int length = org_stream.size();
        final int dim = org_stream.get(0).length;

        initReleaseStream(length, dim);
        this.last_published = new double[dim];
        used_budget = new double[length][dim];
        used_budget_in_window = new double[dim];

        /* ** Begin Grouping Stuff ***/
        this.FROM_TO = new int[NUM_GROUPS][2];
        final int size_group = dim / NUM_GROUPS;
        if (size_group > dim) {
            System.err.println(name() + " more groups than dimensions.");
        }
        int counter = 0;
        for (int g = 0; g < NUM_GROUPS; g++) {
            FROM_TO[g][FROM] = counter;
            counter += size_group;
            FROM_TO[g][TO] = counter;
        }
        //Ensure last group contains all remaining tuples. It may be larger than other group, due to int divisions.
        FROM_TO[NUM_GROUPS - 1][TO] = dim;
        /* ** End Grouping Stuff ***/


        // XXX - We know how large the groups are at least. Thus, I can compute the budget per Mechanism once.


        // do shifting
        lambda_1_per_timestamp = (double) 1 / (double) epsilon;// yes I know it is 1.0d, that is the trick.
        double epsilon_2_per_window = epsilon - w * (epsilon/dim);

        // do not shift
        if (epsilon_2_per_window < epsilon / 2) {//M_2 should get at least half of the budget per window w
            lambda_1_per_timestamp = (w * sensitivity) / (this.epsilon * 0.5 * (double) size_group);
            epsilon_2_per_window = epsilon - (epsilon * 0.5);
        }

        //We certainly publish the first time stamp.
        int t = 0;
        final ColumnPartition[] groups = getInitialPartitions(dim, length);
        double[] values_t = org_stream.get(t);
        for (ColumnPartition g : groups) {
            g.publish(values_t, t);
            //g.publish() only prepares output. Push() does the rest.
            push(t, sanitized_stream);
        }
        t++;

        for (; t < length; t++) {//for each remaining time stamp t
            if (t >= w) {//budget recovery
                double[] not_relevant_eps = used_budget[t - w];
                for (int d = 0; d < dim; d++) {
                    used_budget_in_window[d] -= not_relevant_eps[d];//this one is not relevant anymore
                }
            }

            values_t = org_stream.get(t);
            for (ColumnPartition g : groups) {
                g.publish(values_t, t);
                //g.publish() only prepares output. Push() does the rest.
                push(t, sanitized_stream);
            }
            if (t % PERIOD == 0) {
                re_group(t, groups, sanitized_stream);
            }
        }
        //System.out.println(w+"\t"+outTSV(groups[0].used_budget));
        //System.out.println(w+"sanitized:\t"+outTSV(sanitized_stream));
        return sanitized_stream;
    }

    void re_group(int t, ColumnPartition[] groups, ArrayList<double[]> sanititzed_stream) {
        if (NUM_GROUPS == 1) {
            return; // Do nothing.
        }
        final int dim = sanititzed_stream.get(0).length;

        ArrayList<LocationWeight> weights = new ArrayList<LocationWeight>(dim);
        final double[] last_period_releases = new double[PERIOD];
        final int LAST_PERIOD = t - PERIOD;
        for (int d = 0; d < dim; d++) {
            for (int i = 0; i < PERIOD; i++) {
                last_period_releases[i] = sanititzed_stream.get(LAST_PERIOD + i)[d];
            }
            weights.add(new LocationWeight(d, rms(last_period_releases)));
        }
        Collections.sort(weights);

        for (int group = 0; group < NUM_GROUPS; group++) {
            MyArrayList my_loations = groups[group].group;
            my_loations.clear();//delete old group-location-mapping
            for (int i = this.FROM_TO[group][FROM]; i < FROM_TO[group][TO]; i++) {
                my_loations.add(weights.get(i).location);
            }
            //Collections.sort(my_loations);
        }
    }

    /**
     * Root mean square error (RMS) determining the magnitude of change.
     *
     * @param last_period_releases
     * @return
     */
    double rms(final double[] last_period_releases) {
        double rms = 0.0d;
        for (double val : last_period_releases) {
            rms += val * val;
        }
        rms /= last_period_releases.length;
        rms = Math.sqrt(rms);
        return rms;
    }

    //ArrayList<Double> debug_release_0 = new ArrayList<Double>();

    /**
     * The problem is, i can no longer publish last_published vector by reference, as I would overwrite older data.
     * So I am publishing a copy. This is done after each group updated it's releases.
     *
     * @param time
     * @param sanitized_stream
     */
    void push(final int time, ArrayList<double[]> sanitized_stream) {
        //debug_release_0.add(time,last_published[0]);
        double[] to_publish = sanitized_stream.get(time);
        System.arraycopy(last_published, 0, to_publish, 0, last_published.length);
    }

    /**
     * In the Beginning all locations equally distrusted by location number..
     *
     * @return
     */
    ColumnPartition[] getInitialPartitions(final int dim, final int length) {
        ColumnPartition[] groups = new ColumnPartition[NUM_GROUPS];
        for (int group = 0; group < NUM_GROUPS; group++) {
            MyArrayList my_loations = new MyArrayList(dim);
            for (int l = this.FROM_TO[group][FROM]; l < FROM_TO[group][TO]; l++) {
                my_loations.add(l);
            }
            groups[group] = new ColumnPartition(length, my_loations);
        }
        return groups;
    }

    @Override
    public String name() {
        return "BDwithG";
    }

    class LocationWeight implements Comparable<LocationWeight> {
        final int location;
        final double weight;

        LocationWeight(int l, double w) {
            this.location = l;
            this.weight = w;
        }

        @Override
        public int compareTo(LocationWeight other) {
            if (this.weight > other.weight) {
                return 1;
            } else if (this.weight < other.weight) {
                return -1;
            } else {//same weight, order by location.
                return this.location - other.location;
            }
        }

        public String toString() {
            return location + "->" + weight;
        }
    }
    
    /*static void runAll_w(int max_groups, int data_set, int[] w_s, int num_iterations){
    	System.out.println("w-event Experimentator for BD with max_g="+max_groups+" w in "+Arrays.toString(w_s));
    	System.out.print("Load Data "+data_set+" "); double start = System.currentTimeMillis();
    	Arrays.sort(w_s);
    	
    	ArrayList<double[]>stream = Experiment.load(data_set);
    	max_groups = Math.min(max_groups, stream.get(0).length);//no more groups than dim of data set
    	System.out.println(+stream.size()+" [done] in "+(System.currentTimeMillis()-start)+" ms");
    	
    	double[][] mae_results = new double[w_s.length][max_groups];
    	String table_head = (Experiment.ERROR_MEASURE==Experiment.MAE) ? "MAE" : "ARE"; 
    	table_head+="\tno group";
    	
    	Mechansim m = Experiment.getInstance(Experiment.BD_GROUPING);
    	for(int g=1;g<max_groups;g++){
    		System.out.println("g="+g);
    		BDwithGrouping.NUM_GROUPS = g;
    		table_head += "\t"+m.name();
        	for(int w_i=0;w_i<w_s.length;w_i++){
        		LaplaceStream.setLine(0);
        		int w = w_s[w_i];
        		for(int loop=0;loop<num_iterations;loop++){
        			start = System.currentTimeMillis();
        			double error;
        			if(Experiment.ERROR_MEASURE==Experiment.MAE){
        				error = Mechansim.mae(stream,m.run(stream,w,1.0));
        			}else{
        				error = Mechansim.are(stream,m.run(stream,w,1.0));
        			}
        			System.out.println(w+"\t"+error+"\tin\t"+(System.currentTimeMillis()-start));
        			mae_results[w_i][g]+=error;
        			LaplaceStream.line();
        		}
        		mae_results[w_i][g] /=(double) num_iterations;//average the mae
        	}
    	}    
    	if(Mechansim.TRUNCATE){
    		System.out.println("Truncate: get rid of negative counts and round to next integer");
    	}
    	if(Mechansim.USE_NON_PRIVATE_SAMPLING){
    		System.out.println("Non-private sampling");
    	}
    	if(LaplaceStream.USAGE_MODE==LaplaceStream.USAGE_MODE_STUPID){
    		System.out.println("Using determininstic noise of size lampda - use this for correctness checks only.");
    	}
    	
    	System.out.println("Data="+data_set+" with dim="+stream.get(0).length+" length="+stream.size());
    	System.out.println(table_head);
    	for(int w_i=0;w_i<w_s.length;w_i++){
    		int w = w_s[w_i];
    		System.out.println("w="+w+"\t"+Mechansim.outTSV(mae_results[w_i]));
    	}
    	stream.clear();
    	double stop = System.currentTimeMillis();
    	System.out.println("Experiment [DONE] in "+(stop-start)+" ms");
    }*/

    /**
     * All groups have a minimum size. So class member lambda_1 etc. hold for each partition.
     *
     * @author schaeler
     */
    public class ColumnPartition {
        /**
         * length of the entire stream
         */
        final int length;
        /**
         * Current locations in this group
         */
        final MyArrayList group;
        //final ArrayList<Integer> when_published = new ArrayList<Integer>();

        int dim;

        ColumnPartition(int length, MyArrayList group) {
            this.length = length;
            this.group = group;
            this.dim = group.size();
        }

        void publish(final double[] values_t, final int t) {
            //Execute M_1

            final double s_a_d = avg_dissimilarity_group(last_published, values_t, lambda_1_per_timestamp, group);

            /** Budget for M_2: determine min budget remaining */
            double eps_rm = epsilon_2_per_window;//upper bound
            for (int i = 0; i < dim; i++) {
                int location = group.get(i);
                double my_eps_rm = (epsilon_2_per_window - used_budget_in_window[location]) / 2;//XXX - here I half the budget already
                if (my_eps_rm < eps_rm) {
                    eps_rm = my_eps_rm;
                }
            }
            final double budget = eps_rm;//use min eps_rm as budget - just renaming it for conformity with paper.

            //only publish if the newly introduced error probably is smaller than publishing the last values again.
            if (t == 0 || s_a_d > (sensitivity / budget)) { //publish first time stamp for sure
                double lambda_2 = lambda(budget);
                //when_published.add(t);
                double[] used_budget_t = used_budget[t];

                for (int i = 0; i < dim; i++) {
                    int location = group.get(i);
                    used_budget_t[location] = budget;
                    used_budget_in_window[location] += budget;
                    double noisy_val = sanitize(values_t[location], lambda_2);//XXX - We need to determine a noise value per location independently.
                    last_published[location] = noisy_val;
                }
            } else {
                // This group does not publish, while others might publish.
                // this.last_published still contains the old values, so I need to do nothing
            }
        }

        public String toString() {//for debug
            return this.dim + "->" + this.group.toString();
        }
    }
}
