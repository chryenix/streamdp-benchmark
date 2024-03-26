package mechanisms;

import experiment.LaplaceStream;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.ArrayList;

/**
 * Re-implementation of Pegasus mechanism without grouping from 
 *  Y. Chen, A. Machanavajjhala, M. Hay, and G. Miklau. 2017. PeGaSus: Data-
 *  Adaptive Differentially Private Stream Processing. In Proc. Intl Conf. on Computer
 *  and Communications Security (CCS). ACM, 1375-1388.
 * 
 * @author b1074672
 *
 */
public class Pegasus extends Mechansim {

    static boolean DEBUG_FORCE_NEW_GROUP_EACH_TIMESTAMP = true;
    //smoother
    static int AVG_SMOOTHER 	= 0;
    static int MEDIAN_SMOOTHER 	= 1;
    static int JS_SMOOTHER 		= 2;
    static int USE_SMOOTHER = MEDIAN_SMOOTHER;

    double eps_per_ts;

    double eps_p;
    double eps_g;
    //grouper
    double init_theta;
    PegasusDataDim[] tempDataDim;

    public Pegasus() {
        super();
        init();

    }

    public Pegasus(int w, double epsilon) {
        super(w, epsilon);
        init();
    }

    void init() {
        this.eps_per_ts = this.epsilon / this.w; // as pegasus is event-level private
        // as in paper (sec. 6.1)
        this.eps_p = this.eps_per_ts * 0.8;
        this.eps_g = this.eps_per_ts * 0.2;
        //grouper
        this.init_theta = 5.0d / eps_g; // as in paper (sec. 6.1)

    }

    public ArrayList<double[]> run(ArrayList<double[]> org_stream) {

        //init
        init(); //aederung des w von aussen..
        final int length = org_stream.size();
        ArrayList<double[]> sanitized_stream = new ArrayList<double[]>(length);
        int no_dim = org_stream.get(0).length;
        this.tempDataDim = new PegasusDataDim[no_dim];
        //lets go

        for (int t = 0; t < length; t++) {
            double[] origStream_t = org_stream.get(t);
            double[] sanStream_t = new double[no_dim];

            for (int dim = 0; dim < no_dim; dim++) { // separate pegasus for each dimension
                //init
                PegasusDataDim tempDataOfDim;
                if (t == 0) {
                    tempDataOfDim = new PegasusDataDim();
                    tempDataDim[dim] = tempDataOfDim;
                } else {
                    tempDataOfDim = tempDataDim[dim];

                }
                //go
                double c_t = origStream_t[dim];
                double san_c_t = this.perturber(c_t, this.eps_p);
                grouper(t, c_t, tempDataOfDim, this.eps_g);
                //tempDataOfDim.perturbedStream_last_group.add(san_c_t); // important. otherwise, smoother does not work correctly.
                tempDataOfDim.perturbedStream_last_group_stat.addValue(san_c_t);
                //
                double smoothed_c_t = smoother(tempDataOfDim.perturbedStream_last_group_stat);
                // replace with smoothed value

                if (smoothed_c_t == 0)
                    System.out.print("");

                if (Mechansim.TRUNCATE) {
                    double smoothed_c_t_and_trunc = truncate(smoothed_c_t);
                    smoothed_c_t = smoothed_c_t_and_trunc;
                }
                if (Math.abs(smoothed_c_t - c_t) > Math.abs(san_c_t - c_t))
                    System.out.print("");

                //tempDataOfDim.perturbedStream_last_group.set(tempDataOfDim.perturbedStream_last_group.size() - 1, smoothed_c_t);
                //tempDataOfDim.perturbedStream_last_group.remove(tempDataOfDim.perturbedStream_last_group.size() - 1);
                //tempDataOfDim.perturbedStream_last_group.add(smoothed_c_t);

                sanStream_t[dim] = smoothed_c_t;

            }
            //System.out.println(Math.abs(start-System.currentTimeMillis())/1000d + " ms for one timestamp");
            sanitized_stream.add(sanStream_t);

        }

        return sanitized_stream;
    }

    public double perturber(double c_t, double eps_p) {
        return sanitize(c_t, this.lambda(eps_p));
    }


    public double dev(ArrayList<Double> trueStream_last_group, double c_t) {
        double dev = 0;

        double average_count = (trueStream_last_group.stream().mapToDouble(a -> a).sum() + c_t) / (trueStream_last_group.size() + 1);
        for (double c : trueStream_last_group) {
            dev += Math.abs(c - average_count);
        }
        dev += Math.abs(c_t - average_count);
        return dev;
    }

    public ArrayList<Double> grouper(double t, double c_t, PegasusDataDim tempDataDim, double eps_g) {
        double noisy_theta = 0;

        // Line 1-5  -- already done
        //if (t==0) {
        //ArrayList<Double> last_group = new ArrayList<Double>();
        //	last_group = new ArrayList<Double>();
        //	this.last_group_closed = true;
        //} else {
        //last_group = last_partition_P_t_1.get(last_partition_P_t_1.size() - 1);
        //}
        // Line 6-8
        if (DEBUG_FORCE_NEW_GROUP_EACH_TIMESTAMP || tempDataDim.last_group_closed) {
            tempDataDim.idx_last_group.clear(); //new group
            tempDataDim.idx_last_group.add(t);
            tempDataDim.trueStream_last_group.clear(); //new group
            tempDataDim.trueStream_last_group.add(c_t);
            //tempDataDim.perturbedStream_last_group.clear(); //new group
            tempDataDim.perturbedStream_last_group_stat.clear();
            tempDataDim.last_group_closed = false;

            double lamdba_thres = 4.0 / this.eps_g;
            noisy_theta = this.init_theta + LaplaceStream.nextNumber() * lamdba_thres; //reset theta
            // Line 9-16
        } else {
            noisy_theta = tempDataDim.noisy_theta_prev;

            double dev = dev(tempDataDim.trueStream_last_group, c_t);
            double lamdba_dev = 8.0 / this.eps_g;
            double noisy_dev = dev + LaplaceStream.nextNumber() * lamdba_dev;//sanitize(dev, lamdba_dev);
            if (Math.abs(noisy_dev) < Math.abs(noisy_theta)) { 
                tempDataDim.idx_last_group.add(t);
                tempDataDim.trueStream_last_group.add(c_t);
                tempDataDim.last_group_closed = false;
            } else {
                tempDataDim.idx_last_group.clear(); // new ArrayList<Double>(); //new group
                tempDataDim.idx_last_group.add(t);
                tempDataDim.trueStream_last_group.clear(); //new group
                tempDataDim.trueStream_last_group.add(c_t);
//				tempDataDim.perturbedStream_last_group.clear(); //new group
                tempDataDim.perturbedStream_last_group_stat.clear();
                tempDataDim.last_group_closed = true;
            }
        }
        // Line 17
        tempDataDim.noisy_theta_prev = noisy_theta;
        return tempDataDim.idx_last_group;
    }

    public double smoother(ArrayList<Double> sanStream_last_group) {
        double smoothed_c_t = 0;
        if (Pegasus.USE_SMOOTHER == Pegasus.AVG_SMOOTHER) {
            smoothed_c_t = averageSmoother(sanStream_last_group);
        } else if (Pegasus.USE_SMOOTHER == Pegasus.MEDIAN_SMOOTHER) {
            smoothed_c_t = medianSmoother(sanStream_last_group);
        } else if (Pegasus.USE_SMOOTHER == Pegasus.JS_SMOOTHER) {
            smoothed_c_t = jsSmoother(sanStream_last_group);
        }
        return smoothed_c_t;
    }

    public double smoother(DescriptiveStatistics sanStream_last_group) {
        double smoothed_c_t = 0;
        if (Pegasus.USE_SMOOTHER == Pegasus.AVG_SMOOTHER) {
            smoothed_c_t = averageSmoother(sanStream_last_group);
        } else if (Pegasus.USE_SMOOTHER == Pegasus.MEDIAN_SMOOTHER) {
            smoothed_c_t = medianSmoother(sanStream_last_group);
        } else if (Pegasus.USE_SMOOTHER == Pegasus.JS_SMOOTHER) {
            smoothed_c_t = jsSmoother(sanStream_last_group);
        }
        return smoothed_c_t;
    }

    public double averageSmoother(ArrayList<Double> sanStream_last_group) {
        return sanStream_last_group.stream().mapToDouble(a -> a).average().getAsDouble();
    }

    public double averageSmoother(DescriptiveStatistics sanStream_last_group) {
        return sanStream_last_group.getMean();
    }

    public double medianSmoother(ArrayList<Double> sanStream_last_group) {
        DescriptiveStatistics stats = new DescriptiveStatistics();
        sanStream_last_group.forEach(d -> stats.addValue(d));
        return stats.getPercentile(0.5);
    }

    public double medianSmoother(DescriptiveStatistics sanStream_last_group) {
        double median = sanStream_last_group.getPercentile(50.0D);
        if (median == 0)
            System.err.print("");
        return median;
    }

    public double jsSmoother(ArrayList<Double> sanStream_last_group) {
        double avg = averageSmoother(sanStream_last_group);
        double group_size = sanStream_last_group.size();
        double noisy_c_t = sanStream_last_group.get((int) (group_size - 1));
        return (noisy_c_t - avg) / group_size + avg;
    }

    public double jsSmoother(DescriptiveStatistics sanStream_last_group) {
        double avg = averageSmoother(sanStream_last_group);
        double group_size = sanStream_last_group.getN();
        double noisy_c_t = sanStream_last_group.getElement((int) (group_size - 1));
        return (noisy_c_t - avg) / group_size + avg;
    }

    @Override
    public String name() {
        return "Pegasus";
    }

    class PegasusDataDim {
        double noisy_theta_prev = init_theta;
        //Line 1-5 from grouper
        ArrayList<Double> idx_last_group = new ArrayList<Double>();
        ArrayList<Double> trueStream_last_group = new ArrayList<Double>();
        //	ArrayList<Double> perturbedStream_last_group = new ArrayList<Double>(); // NOT smoothed, only perturbed!!
        DescriptiveStatistics perturbedStream_last_group_stat = new DescriptiveStatistics(); // NOT smoothed, only perturbed!!
        boolean last_group_closed = true;

    }
}
