package mechanisms;

import experiment.Experiment;
import experiment.LaplaceStream;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * All w-event mechanisms must extend from this class. It offers basic methods, our Experiment infrstructure relies on.
 * 
 *  
 * @author Martin
 *
 */
public abstract class Mechansim {
    private static final int DEFAULT_W = 10;
    private static final double DEFAULT_EPS = 1.0d;
    /**
     *  if true post-processes every sanitized release san_t as follow: max(round(san_t),0). For sparse streams this highly increases utility.
     */
    public static boolean TRUNCATE = false;
    /**
     *  if enabled, it does not add noise to dissimilarity values. however, budget is deemed as used in BA, BD.
     */
    public static boolean USE_NON_PRIVATE_SAMPLING = false; 
    /**
     * Window size w of the w-event DP framework
     */
    public int w;
    /**
     * Amount of privacy budget for each running window of size w
     */
    public double epsilon;
    /**
     * Sensitivity of the Query
     */
    protected double sensitivity = 1.0;//XXX only holds for counts

    public Mechansim() {
        this(DEFAULT_W, DEFAULT_EPS);
    }

    public Mechansim(int w, double epsilon) {
        this.w = w;
        this.epsilon = epsilon;
    }

    /**
     * Sanitizes the query release of one time stamp according to the provided noise scale lambda
     * 
     * @param org_stream_t original n-dimensional query result at time t
     * @param lambda noise scale to add to each query dimension
     * 
     * @return
     */
    public static final double[] sanitize(final double[] org_stream_t, final double lambda) {
        final int dim = org_stream_t.length;
        final double[] sanitized_stream_t = new double[dim];
        for (int d = 0; d < dim; d++) {
        	//Because we assume that the dimensions are independent, we can add the noise independently to each dimension of the transcript. I.e., the dimensions do not share the budget.
            final double noise = LaplaceStream.nextNumber() * lambda;
            sanitized_stream_t[d] = org_stream_t[d] + noise;
        }
        if (TRUNCATE) {
        	//Round query result to a non-negative number. Especially, if the stream is sparse (i.e., contains a lot of zero counts), this highly improves utility.
            for (int i = 0; i < sanitized_stream_t.length; i++) {
                double temp = sanitized_stream_t[i];
                temp = Math.max(0, temp);//allow no negative values
                temp = Math.round(temp);
                sanitized_stream_t[i] = temp;
            }
        }
        return sanitized_stream_t;
    }

    /**
     * Special method for 1-D Query results.
     * Sanitizes the query release of one time stamp according to the provided noise scale lambda
     * 
     * @param val original 1-dimensional query result at time t
     * @param lambda noise scale to add to each query dimension
     * 
     * @return
     */
    public static final double sanitize(final double val, final double lambda) {
        double noise = LaplaceStream.nextNumber() * lambda;
        double sanVal = val + noise;        
        
        if (TRUNCATE) {
        	//Round query result to a non-negative number. Especially, if the stream is sparse (i.e., contains a lot of zero counts), this highly improves utility.
            sanVal = truncate(sanVal);
        }
        return sanVal;
    }

    /**
     * Compute shape parameter \lambda for Lap(0,\lambda) for noise scaling using the provided budget \epsilon_t and query sensitivity \Delta Q
     * 
     * @param budget privacy budget \epsilon_t at current time stamp
     * @param sensitivity query sensitivity \Delta Q. Usually it is 1.
     * @return
     */
    public static double lambda(double budget, double sensitivity) {
        return sensitivity / budget;
    }

    /**
     * Map to query domain \mathcal{N}
     * Round query result to a non-negative number. Especially, if the stream is sparse (i.e., contains a lot of zero counts), this highly improves utility.
     *
     * @param val
     * @return
     */
    public static double truncate(double val) {
        if (TRUNCATE)
            return forceTruncate(val);
        else return val;
    }

    public static double forceTruncate(double val) {
        return Math.max(0, Math.round(val));
    }

    /**
     * Mean Absolute Error (MAE)
     *
     * @param org_values
     * @param new_values
     * @return
     */
    public static final double mae(final ArrayList<double[]> org_values, ArrayList<double[]> new_values) {
        double delta = 0.0d;
        final double numValues = org_values.size() * org_values.get(0).length;
        for (int line = 0; line < org_values.size(); line++) {
            final double[] arr_1 = org_values.get(line);
            final double[] arr_2 = new_values.get(line);
            for (int col = 0; col < arr_1.length; col++) {
                delta += Math.abs(arr_1[col] - arr_2[col]);
            }
        }
        final double mae = delta / numValues;
        //System.out.println(outTSV_(org_values));
        //System.out.println(outTSV_(new_values));
        return mae;
    }

    /**
     * Average Relative Error (ARE) aka MRE
     *
     * @param org_values
     * @param new_values
     * @return
     */
    public static final double are(final ArrayList<double[]> org_values, ArrayList<double[]> new_values) {
        double delta = 0.0d;
        final int length = org_values.size();
        final int dim = org_values.get(0).length;
        final double numValues = length * dim;
        double[] threshold = new double[dim];

        for (int line = 0; line < length; line++) {
            final double[] arr_2 = org_values.get(line);
            for (int col = 0; col < dim; col++) {
                threshold[col] += arr_2[col];
            }
        }

        for (int col = 0; col < dim; col++) {
            threshold[col] *= 0.01;//one percent of the cummulated values.
        }

        for (int line = 0; line < length; line++) {
            final double[] arr_1 = org_values.get(line);
            final double[] arr_2 = new_values.get(line);

            for (int col = 0; col < dim; col++) {
                double temp = Math.abs(arr_1[col] - arr_2[col]) / Math.max(arr_1[col], threshold[col]);
                delta += temp;
            }
        }
        final double are = delta / numValues;//normalize
        return are;
    }

    public static String outTSV(@SuppressWarnings("rawtypes") ArrayList array) {
        if (array == null)
            return "Null array";

        String ret = "";
        for (int index = 0; index < array.size() - 1; index++) {
            ret += array.get(index) + "\t ";
        }
        ret += array.get(array.size() - 1) + "\t";

        return ret;
    }

    public static String outTSVFirstDim(ArrayList<double[]> array) {
        if (array == null)
            return "Null array";

        String ret = "";
        for (int index = 0; index < array.size() - 1; index++) {
            ret += array.get(index)[0] + "\t ";
        }
        ret += array.get(array.size() - 1)[0] + "\t";

        return ret;
    }

    public static String outAsColFirstDim(ArrayList<double[]> array) {
        if (array == null)
            return "Null array";

        String ret = "";
        for (int index = 0; index < array.size() - 1; index++) {
            ret += array.get(index)[0] + "\n ";
        }
        ret += array.get(array.size() - 1)[0] + "\n";

        return ret;
    }

    public static String outCSV(Collection<Integer> col) {
        Integer[] array = (Integer[]) (col.toArray(col.toArray(new Integer[col.size()])));
        if (array == null)
            return "Null array";

        String ret = "";
        for (int index = 0; index < array.length - 1; index++) {
            ret += array[index] + ", ";
        }
        ret += array[array.length - 1] + "";

        return ret;
    }

    public static void toCSVFile(ArrayList<double[]> array, String fileName) {
        try (FileWriter writer = new FileWriter(Experiment.RESLT_DIR + "\\" + fileName);
            BufferedWriter bw = new BufferedWriter(writer)) {
            bw.write(outTSV_(array).replace("\t", ","));
        } catch (IOException e) {
            System.err.println("Cannot create file");
            e.printStackTrace();
        }
    }

    public static String outTSVFirstDimension(ArrayList<double[]> array) {
        if (array == null)
            return "Null array";

        String ret = "";
        for (int index = 0; index < array.size() - 1; index++) {
            ret += array.get(index)[0] + "\t ";
        }
        ret += array.get(array.size() - 1)[0] + "\t";

        return ret;
    }

    public static String outTSVFirstDimension(double[][] array) {
        if (array == null)
            return "Null array";

        String ret = "";
        for (int index = 0; index < array.length - 1; index++) {
            ret += array[index][0] + "\t ";
        }
        ret += array[array.length - 1][0] + "\t";

        return ret;
    }

    public static String outTSV_(ArrayList<double[]> array) {
        if (array == null)
            return "Null array";

        String ret = "";
        for (int index = 0; index < array.size() - 1; index++) {
            ret += outTSV(array.get(index)) + "\n ";
        }
        ret += outTSV(array.get(array.size() - 1));

        return ret;
    }

    public static String outTSV(double[] array) {
        if (array == null)
            return "Null array";

        String ret = "";
        for (int index = 0; index < array.length - 1; index++) {
            ret += array[index] + "\t ";
        }
        ret += array[array.length - 1];// + "\t";

        return ret;
    }

    public static String outTSV(boolean[] array) {
        if (array == null)
            return "Null array";

        String ret = "";
        for (int index = 0; index < array.length - 1; index++) {
            ret += array[index] + "\t ";
        }
        ret += array[array.length - 1] + "\t";

        return ret;
    }

    public static String outTSV(Boolean[] array) {
        if (array == null)
            return "Null array";

        String ret = "";
        for (int index = 0; index < array.length - 1; index++) {
            ret += array[index] + "\t ";
        }
        ret += array[array.length - 1] + "\t";

        return ret;
    }

    public static String outTSV(int[] array) {
        if (array == null)
            return "Null array";

        String ret = "";
        for (int index = 0; index < array.length - 1; index++) {
            ret += array[index] + "\t ";
        }
        ret += array[array.length - 1];//+ "\t";

        return ret;
    }

    public static String outTSVasCol(int[] array) {
        if (array == null)
            return "Null array";

        String ret = "";
        for (int index = 0; index < array.length - 1; index++) {
            ret += array[index] + "\n ";
        }
        ret += array[array.length - 1];//+ "\t";

        return ret;
    }

    /**
     * This method helps to check whether your mechanism implementation complies with the w-event definition. 
     * Simply, keep track of the epsilon spend per time stamp in parameter eps_used. Then, this method checks
     * budget spending for each rolling window of size w.
     * 
     * In addition, you may use this method to analyze whether your mechanisms actually spends all the available budget.
     *
     * @param eps_used
     * @param w
     * @return
     */
    public static double[] sum_eps_w(final double[] eps_used, final int w) {
        final int length = eps_used.length;
        final double[] sum_eps_w = new double[length];
        for (int t = 0; t < length; t++) {
            int start = Math.max(t - w + 1, 0);//start earliest at index 0
            double eps = 0.0d;
            for (int i = start; i <= t; i++) {
                eps += eps_used[i];
            }
            sum_eps_w[t] = eps;//the sum of the eps window *ending* at t
        }
        return sum_eps_w;
    }

    /**
     * computes lambda using *all* of the provided budget
     */
    double lambda(double budget) {
        double temp = budget;
        return sensitivity / temp;
    }

    /**
     * Method used in the sampling decision of BA/DB.
     * 
     * @param last_published
     * @param org_stream_t
     * @param lambda
     * @return
     */
    double avg_dissimilarity(final double[] last_published, final double[] org_stream_t, final double lambda) {
        double avg = 0;
        //calculate dissimilarity
        for (int location = 0; location < last_published.length; location++) {
            avg += Math.abs(last_published[location] - org_stream_t[location]);
        }
        avg /= (double) (last_published.length);//normalize by dimensionality
        if (Mechansim.USE_NON_PRIVATE_SAMPLING == false) {
            avg += LaplaceStream.nextNumber() * lambda;//add noise
        }
        return avg;
    }

    /**
     * Method used in the sampling decision of BA/DB when dynamic grouping is enabled.
     * 
     * @param last_published
     * @param org_stream_t
     * @param lambda
     * @param group
     * @return
     */
    double avg_dissimilarity_group(final double[] last_published, final double[] org_stream_t, final double lambda, final util.MyArrayList group) {
        double avg = 0;
        //calculate dissimilarity
        for (int i = 0; i < group.size(); i++) {
            int location = group.get(i);
            avg += Math.abs(last_published[location] - org_stream_t[location]);
        }
        avg /= (double) (group.size());//normalize by dimensionality
        if (Mechansim.USE_NON_PRIVATE_SAMPLING == false) {
            avg += LaplaceStream.nextNumber() * lambda;//add noise
        }
        return avg;
    }

    /**
     * Just a wrapper for the normal run(ArrayList) method in case you want to provide new values for w and eps without creating a new Mechanism object.
     * 
     * @param org_stream
     * @param w
     * @param eps
     * @return
     */
    public ArrayList<double[]> run(ArrayList<double[]> org_stream, int w, double eps) {
        this.epsilon = eps;
        this.w = w;
        ArrayList<double[]> sanitized_stream = run(org_stream);
        return sanitized_stream;
    }

    /**
     * Refers to maximum Hamming Distance of two neighboring Databases. For Histograms it is usually 1 (default value). If you need a special sensitivity e.g., for sum(), use this method.
     *
     * @param value
     * @return
     */
    public boolean setSensitivity(double value) {
        if (value < 0) return false;
        this.sensitivity = value;
        return true;
    }

    /**
     * This is the method that the Experiment class calls. Implement it for you own class. 
     * For getting an intuition how to do that refer to the implementation of Uniform and Sample. 
     * 
     * @param org_stream stream prefix org_stream.get(t) gives the n-dimensional original query result at time t
     * 
     * @return sanitized stream prefix
     */
    public abstract ArrayList<double[]> run(ArrayList<double[]> org_stream);

    /**
     * This is the mechanism name the Experiment outputs in the console and result files.
     * @return
     */
    public abstract String name();
}
