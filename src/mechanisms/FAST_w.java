package mechanisms;

import java.util.ArrayList;
import java.util.BitSet;

/**
 * 
 * @author schaeler
 * 
 * Re-implementation of the original FAST implementation published as
 * L. Fan and L. Xiong. Real-time aggregate monitoring with differential privacy. In CIKM, 2012.
 * 
 * The original implementation supports event-level privacy. That is, for w-event DP the DP guarantee holds for w=1 only. 
 * Thus, we extended FAST using uniform budget distribution as suggested in 
 * G. Kellaris et al. 2014. Differentially Private Event Sequences over Infinite Streams. Proc. VLDB Endow. 7, 12 (2014), 1155–1166.
 * 
 * Note, the original authors made binaries of their implementation available under http://www.mathcs.emory.edu/~lxiong/aims/FAST/ 
 * The binaries are not available anymore and did not contain any license agreement.
 * To this end, we clarify that assuming that the authors intended to comply with reproducibility standards all major database conferences endorse, 
 * we used the binaries of the original authors to verify our own genuine implementation. 
 * 
 */
public class FAST_w extends Mechansim {
    double[] predict;
    int interval;
    double P = 100.0D;
    double Q = 100000.0D;
    double R = 1000000.0D;
    double K = 0.0D;
    double Cp = 0.9D;
    double Ci = 0.1D;
    double Cd = 0.0D;
    double theta = 5.0D;
    double xi = 0.1D;
    int minIntvl = 1;
    int windowPID = 5; // = T_i integral time window indicating how many recent errors are taken
    //double ratioM = 0.15D; -- paper
    double ratioM = 0.075D; // --  due to algorithm repair -- half of DSAT
    //init in publish()
    double[] publish;
    BitSet query;
    double epsilon_per_sample = 0.0D;
    // initn in run()
    double total_budget;
    private double[] orig;

    public FAST_w() {
        super();
    }

    public FAST_w(int w, double epsilon) {
        super(w, epsilon);
    }

    public double[] getOrig() {
        return orig;
    }

    public void setOrig(double[] orig) {
        this.orig = orig;
    }

    public void publish() {
        this.query = new BitSet(this.getOrig().length);
        this.predict = new double[this.getOrig().length];
        this.publish = new double[this.getOrig().length];

        int M = (int) (this.ratioM * this.getOrig().length); // christine: M = maximum number of samples
        if (M <= 0) {
            M = 1;
        }
        this.epsilon_per_sample = (this.total_budget / M);
        this.interval = 1;

        int nextQuery = Math.max(1, this.windowPID) + this.interval - 1;
        for (int i = 0; i < this.getOrig().length; i++) {
            // begin: Algorithm 3
            if (i == 0) {
                this.publish[i] = Mechansim.sanitize(this.getOrig()[i], 1.0d / this.epsilon_per_sample);
                this.query.set(i);
            } else {
                // Line 2 'obtain prior estimate from prediction'
                double predct = predictKF(i);
                //this.predict[i] = ((int) predct);

                this.predict[i] = (int) predct;
                if (Mechansim.TRUNCATE)
                    this.predict[i] = Mechansim.truncate(predct);
                // (not covered in algo just in text) exception handling; integral value can only be calculated when there have been >= windowPID samples
                if ((this.query.cardinality() < this.windowPID) && (this.query.cardinality() < M)) {

                    this.publish[i] = Mechansim.sanitize(this.getOrig()[i], 1.0d / this.epsilon_per_sample);
                    this.query.set(i);
                    correctKF(i, predct);

                    // Line 3 'if sampling point and numSamples < M'
                } else if ((i == nextQuery) && (this.query.cardinality() < M)) {
                    // Line 4 'perturb'
                    this.publish[i] = Mechansim.sanitize(this.getOrig()[i], 1.0d / this.epsilon_per_sample);
                    this.query.set(i);
                    // line 6: obtain posterior estimate from correction
                    correctKF(i, predct);

                    // line 8: adjust sampling rate by adaptive sampling =>
                    // begin: Algo. 9
                    double ratio = PID(i); // PID error
                    int deltaI = (int) (this.theta * (1.0D - Math.exp((ratio - this.xi) / this.xi))); // (32) - max fehlt
                    this.interval += deltaI;
                    if (this.interval < this.minIntvl) {
                        this.interval = this.minIntvl;
                    }
                    nextQuery += this.interval;
                    // end: Algo 9
                } else {
                    // line 10
                    this.publish[i] = ((int) predct);
                }
            }
            // end: Algorithm 3

        }
    }

    @SuppressWarnings("unused")
	private void setR(double d) {
        this.R = d;
    }

    @SuppressWarnings("unused")
	private void setQ(double d) {
        this.Q = d;
    }

    public void setCp(double d) {
        this.Cp = d;
    }

    public void setCi(double d) {
        this.Ci = d;
    }

    public void setCd(double d) {
        this.Cd = d;
    }

    private double predictKF(int curr) {
        double lastValue = getLastQuery(curr);

        this.P += this.Q;

        return lastValue;
    }

    private void correctKF(int curr, double predict) {
        this.K = (this.P / (this.P + this.R));
        double correct = predict + this.K * (this.publish[curr] - predict);

        this.publish[curr] = Mechansim.truncate(correct);
        this.P *= (1.0D - this.K);
    }

    private double getLastQuery(int curr) {
        int i;
        for (i = curr - 1; i >= 0; i--) {
            if (this.query.get(i)) {
                break;
            }
        }
        return this.publish[i];
    }

    private double PID(int curr) { //  eq. (29)
        double ratio = 0.0D;

        double sum = 0.0D;
        double lastValue = 0.0D;
        double change = 0.0D;
        int timeDiff = 0;
        int next = curr;
        for (int j = this.windowPID - 1; j >= 0; j--) {
            int index;
            for (index = next; index >= 0; index--) {
                if (this.query.get(index)) {
                    next = index - 1;
                    break;
                }
            }
            if (j == this.windowPID - 1) {
                // Feedback error (cf. Def. 4)
                lastValue = Math.abs(this.publish[index] - this.predict[index])
                        / (1.0D * Math.max(this.publish[index], 1));
                change = Math.abs(this.publish[index] - this.predict[index])
                        / (1.0D * Math.max(this.publish[index], 1));
                timeDiff = index;
            }
            if (j == this.windowPID - 2) {
                change -= Math.abs(this.publish[index] - this.predict[index])
                        / (1.0D * Math.max(this.publish[index], 1));
                timeDiff -= index;
            }
            sum += Math.abs(this.publish[index] - this.predict[index]) / (1.0D * Math.max(this.publish[index], 1));
        }
        // Eq. (29) . last value = e_k_n = feedback error
        ratio = this.Cp * lastValue + this.Ci * sum + this.Cd * change / timeDiff;

        return ratio;
    }

    @Override
    public ArrayList<double[]> run(ArrayList<double[]> org_stream) {
        ArrayList<double[]> san_stream = new ArrayList<double[]>();

        // init san stream
        int number_dim = org_stream.get(0).length;
        for (int t = 0; t < org_stream.size(); t++) {
            // init san_stream
            san_stream.add(new double[number_dim]);
        }


        for (int t = 0; t < org_stream.size(); t += this.w + 1) { //split stream in fixed windows
            // for each dimension
            double[] stream_t = org_stream.get(t);
            FAST_w[] fastPerdim = new FAST_w[stream_t.length];
            int end_index = Math.min(t + this.w, org_stream.size() - 1) + 1;

            for (int d = 0; d < stream_t.length; d++) {

                FAST_w fast = new FAST_w(); //we could exchange fast mechanism here
                fastPerdim[d] = fast;
                double[] substream_of_dimension = new double[Math.abs(t - end_index + 1)];

                // read out substream
                for (int i = t; i < end_index - 1; i++) {
                    substream_of_dimension[i - t] = org_stream.get(i)[d];
                }
                fast.orig = substream_of_dimension;
                fast.total_budget = this.epsilon / 2.0;
                fast.publish();

                // store window result
                for (int i = 0; i < this.w && t + i < org_stream.size() && i < fast.publish.length; i++) {
                    san_stream.get(t + i)[d] = fast.publish[i];
                }
            }
        }
        return san_stream;
    }

    @Override
    public String name() {
        return "Fastw";
    }
}
