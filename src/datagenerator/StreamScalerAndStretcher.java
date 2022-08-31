package datagenerator;

import java.util.ArrayList;

public class StreamScalerAndStretcher {
    private static final boolean VERBOSE = true;
    final Generator generator;
    int num_streams_per_param_combination;
    double max_value_in_streams = 0;//Dummy value

	ArrayList<double[]>[] generated_streams;

    @SuppressWarnings("unchecked")
	public StreamScalerAndStretcher(Generator generator, int num_streams_per_param_combination) {
        this.generator = generator;
        this.num_streams_per_param_combination = num_streams_per_param_combination;

        generated_streams = new ArrayList[num_streams_per_param_combination];
        generate_unscaled_streams();
    }

    public static String outTSV(ArrayList<double[]> oneDstream) {
        if (oneDstream == null)
            return "Null stream";

        String ret = "";
        for (int index = 0; index < oneDstream.size() - 1; index++) {
            ret += oneDstream.get(index)[0] + "\t ";
        }
        ret += oneDstream.get(oneDstream.size() - 1)[0] + "\t";

        return ret;
    }

    /**
     * The main illustrates how to use this class.
     * 
     * @param args
     */
    public static void main(String[] args) {
        int streamLength = 1000;//desired length of the stream prefix
        int pLen = 40; 			//desired average period length
        double basis = 1.3;		//Basis for the exponential growing function
        int num_streams = 5;
        Generator gen = new Generator(streamLength, pLen, basis);
        StreamScalerAndStretcher s = new StreamScalerAndStretcher(gen, num_streams);
        double[] desriredStreamMaxValues = {10, 100, 1000, 10000};
        for (double desired_max : desriredStreamMaxValues) {
            out("Desrired max=" + desired_max);
            for (int i = 0; i < num_streams; i++) {
                ArrayList<double[]> stream = s.get_stream(i, desired_max, 1);
                out(outTSV(stream));
            }
        }
    }

    static void out(String toPrint) {
        System.out.println(toPrint);
    }

    static void out_as_error(String toPrint) {
        System.err.println(toPrint);
    }

    void generate_unscaled_streams() {
        this.max_value_in_streams = 0;
        for (int i = 0; i < this.num_streams_per_param_combination; i++) {
            ArrayList<double[]> temp = generator.generate();
            //Do we find a larger max value?
            for (int t = 0; t < temp.size(); t++) {
                if (this.max_value_in_streams < temp.get(t)[0]) {
                    this.max_value_in_streams = temp.get(t)[0];
                }
            }
            generated_streams[i] = temp;
        }
        if (VERBOSE) {
            System.out.println("Generated original 1D streams");
            for (int i = 0; i < this.num_streams_per_param_combination; i++) {
                out(outTSV(generated_streams[i]));
            }
        }
    }

    public ArrayList<double[]> get_stream(int id) {
        return this.generated_streams[id];
    }

    public ArrayList<double[]> get_stream(int id, double desired_max_value, int period_scaling_factor) {
        ArrayList<double[]> stream = get_stream(id);

        stream = scale_max_value(stream, this.max_value_in_streams, desired_max_value, generator.min_period_value);

        if (period_scaling_factor != 1) {
            out_as_error("period_scaling_factor!=1 currently not supported");
        }
        return stream;
    }

    ArrayList<double[]> scale_max_value(ArrayList<double[]> org, double actual_max_value_in_batch, double desired_max_value, double min_period_value) {
        ArrayList<double[]> to_return = new ArrayList<double[]>();

        final double expected_max_value = actual_max_value_in_batch;

        for (int t = 0; t < org.size(); t++) {
            double val = org.get(t)[0];
            val -= min_period_value;//to ensure that after normalization min_period_value as min value still holds
            val /= expected_max_value;//normalization s.t. all values are in [0,1]
            val *= desired_max_value;
            val += min_period_value;
            double[] temp = {val};
            to_return.add(temp);
        }
        return to_return;
    }
}
