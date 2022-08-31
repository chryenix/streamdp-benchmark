package datagenerator;

import mechansims.Mechansim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
/**
 * 
 * @author b1074672
 * 
 * Generates artificial one-dimensional streams. For seeing how to use it have a look at the main() method of the StreamScalerAndStretcher class.
 *  
 */
public class Generator {
    static final long seed = 12345678;
    static final int min_period_length = 4;
    @SuppressWarnings("unused")
	private static final boolean MAKE_NOISY = false;//Optional extension adding random Gaussian noise to each time stamp
    private static final boolean DO_ROUND = false;
    static Random rand = new Random();
    final double[] buffer;

    //int num_periods;
    final int avg_period_length;
    final double basis;
    double deviation_period_length = 2.0d;//sollte man das mitschleppen, so dass die sich wieder einfangen?
    double min_period_value = 2;
    double avg_period_value_delta = 10;//???
    double avg_deviation_period_value = 2.0d;//Delta zwischen 2 Perioden
    double period_value_trend = 0;

    public Generator(final int length) {
        this.buffer = new double[length];
        this.avg_period_length = 20;
        this.basis = 2d;
        Generator.rand.setSeed(seed);
    }

    public Generator(int length, int periodLength, double basis) {
        this.buffer = new double[length];
        this.avg_period_length = periodLength;
        this.basis = basis;
        Generator.rand.setSeed(seed);
    }

    /**
     * We generate artificial data period-wise. This method dices the length of a specific period. 
     * 
     * @param avg_period_length
     * @param deviation_period_length
     * @return period length in time stamps
     */
    static int dice_period_length(final double avg_period_length, final double deviation_period_length) {
        int length;
        do {
            length = (int) (rand.nextGaussian() * deviation_period_length + avg_period_length);
        } while (length < min_period_length);//ensure some minimal length

        return length;
    }

    static double markov_shrink_log(final double last_value, final double grow_factor, final double avg_deviation_value) {
        return markov_grow_log(last_value, 1 / grow_factor, avg_deviation_value);//inverse of growing phase
    }

    static double markov_grow_log(final double last_value, final double grow_factor, final double avg_deviation_value) {
        double delta = rand.nextGaussian() * avg_deviation_value + last_value * grow_factor;
        return last_value + delta;
    }

    static double markov_grow(final double last_value, final double avg_value_delta, final double avg_deviation_value) {
        double delta = rand.nextGaussian() * avg_deviation_value + avg_value_delta;
        return last_value + delta;
    }

    static double markov_shrink(final double last_value, final double avg_value_delta, final double avg_deviation_value) {
        return markov_grow(last_value, -avg_value_delta, avg_deviation_value);//inverse of growing phase
    }

    public static String outTSV(double[] array) {
        if (array == null)
            return "Null array";

        String ret = "";
        for (int index = 0; index < array.length - 1; index++) {
            ret += array[index] + "\t ";
        }
        ret += array[array.length - 1] + "\t";

        return ret;
    }

    public static void main(String[] args) {
        //double[] exps = {1.0, 1.02, 1.05, 1.1, 1.2, 1.4, 1.6, 1.8, 2.0};
        double[] exps = {1.3};
        int length = 1000;
        int period_length = 40;
        for (double exp : exps) {
            System.out.println("exp=" + exp);
            Generator g = new Generator(length, period_length, exp);
            int i = 0;
            while (i++ < 5) {
                //System.out.println(outTSV(g.generate_internal()));
                g.generate_and_scale(100);
            }
        }
    }

    public ArrayList<double[]> toArrayList(double[] oneDstream) {
        ArrayList<double[]> multiDstream = new ArrayList<>();
        for (int t = 0; t < oneDstream.length; t++) {
            double[] multiDim = {oneDstream[t]};
            multiDstream.add(multiDim);
        }
        return multiDstream;
    }

    public ArrayList<double[]> generate_and_scale(int desired_max_value) {
        double[] oneDstream = generate_internal();
        System.out.println(outTSV(oneDstream));
        scale_max_value(oneDstream, desired_max_value, this.basis, this.min_period_value);
        System.out.println("after scaling\t" + outTSV(oneDstream));
        return toArrayList(oneDstream);
    }

    /**
     * @param desired_max_value
     * @param basis
     * @param min_period_value
     * @return
     */
    void scale_max_value(final double[] oneDstream, int desired_max_value, double basis, double min_period_value) {
        if (basis == 1.3) {
            //Max values are usually in [100,1000]
            final double expected_max_value = 1000;
            for (int t = 0; t < oneDstream.length; t++) {
                double val = oneDstream[t];
                val -= min_period_value;//to ensure that after normalization min_period_value as min value still holds
                val /= expected_max_value;//normalization s.t. all values are in [0,1]
                val *= desired_max_value;
                val += min_period_value;
                oneDstream[t] = val;
            }
        } else if (basis == 1.5) {
//Max values are usually in [100,1000]
            final double expected_max_value = 10000;
            for (int t = 0; t < oneDstream.length; t++) {
                double val = oneDstream[t];
                val -= min_period_value;//to ensure that after normalization min_period_value as min value still holds
                val /= expected_max_value;//normalization s.t. all values are in [0,1]
                val *= desired_max_value;
                val += min_period_value;
                oneDstream[t] = val;
            }
        } else {
            out("Base=" + basis + " currently supported 1.3 or 1.5");
        }
    }

    void out(String toPrint) {
        System.out.println(toPrint);
    }

    void out_as_error(String toPrint) {
        System.err.println(toPrint);
    }


    public ArrayList<double[]> generate() {
        double[] oneDstream = generate_internal();
        System.out.println(outTSV(oneDstream));
        return toArrayList(oneDstream);
    }

    final double[] generate_internal() {
        System.out.print("Generating stream with avg_period_length=" + avg_period_length + " deviation_period_length=" + deviation_period_length + " min_period_value=" + min_period_value + " avg_period_value_delta=" + avg_period_value_delta + " avg_deviation_period_value=" + avg_deviation_period_value + "\t");
        Arrays.fill(buffer, -1);

        int time_index = 0;
        int period = 0;

        while (time_index < buffer.length) {
            time_index = generate_period(time_index, buffer, (double) period);
            period++;
        }

        if (DO_ROUND) {
            for (int t = 0; t < buffer.length; t++) {
                buffer[t] = Mechansim.forceTruncate(buffer[t]);
            }
        }

        return buffer.clone();
    }

    private int generate_period(final int period_start_time_index, final double[] array, final double period) {
        final int period_length = dice_period_length((double) avg_period_length, deviation_period_length);

        int t_in_period = 0;
        int global_time_stamp;
        array[period_start_time_index] = rand.nextDouble() * avg_deviation_period_value + min_period_value;
        t_in_period++;


        //growing phase
        while ((global_time_stamp = period_start_time_index + t_in_period) < array.length && t_in_period < period_length / 2) {
            double next_val = array[global_time_stamp - 1] * basis;//TODO Wachstumsfunktion
            array[global_time_stamp] = next_val;
            t_in_period++;
        }

        //shrinking phase
        while ((global_time_stamp = period_start_time_index + t_in_period) < array.length && t_in_period < period_length) {
            double next_val = array[global_time_stamp - 1] * (1 / (basis));//TODO Wachstumsfunktion
            array[global_time_stamp] = next_val;
            t_in_period++;
        }

        return period_start_time_index + period_length;
    }

}
