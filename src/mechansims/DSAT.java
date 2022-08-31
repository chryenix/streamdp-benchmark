package mechansims;

import experiment.LaplaceStream;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Re-implementation of DSAT mechanism without grouping from 
 * H. Li et al. 2015. Differentially private histogram publication for dynamic datasets: 
 * an adaptive sampling approach. In Proc. Int’l Conf. on Information and Knowledge
 * Management (CIKM). ACM, 1001–1010.
 * 
 * @author b1074672
 *
 */
public class DSAT extends Mechansim {

    // repair function for 'number of tuples'
    static boolean USE_PERTURBED_NUMBER_TUPLES=true;
    double SHARE_EPS_NO_TUPLES = 0.1; // the budget at first time stamp for threshold sanitizing is automatically reduced by this amount
    static int NO_TUPELS_n = 500000;

    boolean DEBUG = false;
    static boolean OWN_EPS_1 = false; //works better; maybe something is wrong with k? However, if we use own, we can give upon "number tuples".
    static double OWN_EPS_1_FRACTION = 0.5;

    int dimensionality_U;
    static double THRESHOLD_T = 0.025;//paper: 0.025; //10
    static double TOLERANCE_DELTA = 0.05;
    static double PROP_GAIN_THETA = 0.5;

   // static double CUTOFFPOINT_RATION = 0.01; // paper
    static double CUTOFFPOINT_RATION = 0.15; // --  due to algorithm repair --  2* of FAST

    int skipping_points_M = 2; // not given in paper (?)

    public DSAT() {
        super();
    }

    public DSAT(int w, double epsilon) {
        super(w, epsilon);
    }

    public ArrayList<double[]> run(ArrayList<double[]> org_stream) {
        // Algorithm 2F
        final int length_N = org_stream.size();
        this.dimensionality_U = org_stream.get(0).length;                                                                // each time stamp in window
        ArrayList<double[]> sanitized_stream = new ArrayList<double[]>(length_N);
        double delta = 2d / dimensionality_U;///sensitivity L1 distance

        double cuttoff_point_c = Math.ceil(w * CUTOFFPOINT_RATION);  //in paper for user-level: length_N * 0.01 in paper

        double[] used_budgets_eps_2 = new double[length_N];
        double[] used_budgets_eps_1 = new double[length_N];
        Boolean[] samplingpoint = new Boolean[length_N];

        // repair function
        double no_tuples_n = 0;
        double eps_for_determining_n = 0;
        double reducedEpsFirstWindow = 0;
        if (USE_PERTURBED_NUMBER_TUPLES) {
            eps_for_determining_n = epsilon*SHARE_EPS_NO_TUPLES;
            reducedEpsFirstWindow =eps_for_determining_n;
            double lambda = 1d/ eps_for_determining_n;
            double sum = Arrays.stream(org_stream.get(0)).sum();
            double san_sum = sanitize(sum, lambda);
            no_tuples_n = san_sum;
            if (no_tuples_n == 0) {
                no_tuples_n = NO_TUPELS_n;
            }
        } else {
            no_tuples_n = NO_TUPELS_n;
        }

        double k = this.computeK(cuttoff_point_c, this.dimensionality_U, delta, this.w, no_tuples_n); //before: length N instead of k


        double eps_1_per_window_paper  = this.epsilon * k;
        double eps_1_per_window_paper_first  = (this.epsilon - reducedEpsFirstWindow) * k;

        double eps_1_per_window_own = this.epsilon * OWN_EPS_1_FRACTION;
        double eps_1_per_window_own_frist = (this.epsilon - reducedEpsFirstWindow) * OWN_EPS_1_FRACTION;

        double eps_1_per_window=0;
        double noisy_threshold=0;
        double eps_1_per_window_first=0;
        @SuppressWarnings("unused")
		double noisy_threshold_first=0;
        if (OWN_EPS_1) {
            eps_1_per_window  =eps_1_per_window_own;
            eps_1_per_window_first=eps_1_per_window_own_frist;
            noisy_threshold = THRESHOLD_T + LaplaceStream.nextNumber() * 2d * delta / (eps_1_per_window_first/cuttoff_point_c); 
            used_budgets_eps_1[0] = eps_1_per_window_first/cuttoff_point_c - eps_for_determining_n;
        } else {
            eps_1_per_window = eps_1_per_window_paper;
            eps_1_per_window_first=eps_1_per_window_paper_first;
            noisy_threshold = THRESHOLD_T + LaplaceStream.nextNumber() * 2d * delta / (eps_1_per_window_first/cuttoff_point_c) ; 
            used_budgets_eps_1[0] = eps_1_per_window_first/cuttoff_point_c - eps_for_determining_n ; 
        }

        final   double eps_2_per_window = this.epsilon - eps_1_per_window ; //for sanitizing
        final   double eps_2_per_window_first = this.epsilon - eps_1_per_window_first ; //for sanitizing
        double total_budget_spent_eps_2 = 0; //eps_1;

        int no_sampling_points_window = 0;
        double[] last_realize = null;
        for (int t = 0; t < length_N; t++) {
            double[] org_stream_t = org_stream.get(t);

            if (t <= this.w) {
            //Algorithm 2
            if (t == 0) {
                double scale = this.lambda(eps_2_per_window_first / cuttoff_point_c);
                last_realize = sanitize(org_stream_t, scale);
                sanitized_stream.add(last_realize);
                total_budget_spent_eps_2 += eps_2_per_window_first / cuttoff_point_c;
                used_budgets_eps_2[t] = eps_2_per_window_first / cuttoff_point_c;
                samplingpoint[t] = true;
                no_sampling_points_window++;
            } else {
                if (t <= skipping_points_M) {// Line 2
                    sanitized_stream.add(last_realize);
                    samplingpoint[t] = false;

                } else {
                    // Line 4
                    if (no_sampling_points_window >= cuttoff_point_c) { // ">=" is important due to line 47
                        sanitized_stream.add(last_realize);
                        samplingpoint[t] = false;
                    } else { // cf. Algo 1 ("continue")
                        //Line 5
                        double noisy_dist = avg_dissimilarity(org_stream_t, last_realize,
                                2d * cuttoff_point_c * delta / eps_1_per_window_first ); // L1 distance
                        used_budgets_eps_1[t] = eps_1_per_window_first /cuttoff_point_c;

                        // Line 6
                        double feedback_error_E = Math.abs(no_sampling_points_window / t - cuttoff_point_c / w); //use here already w instead of N
                        double prop_e = Math.abs(feedback_error_E - TOLERANCE_DELTA) / TOLERANCE_DELTA;
                        double prop_part_u = prop_e * PROP_GAIN_THETA;
                        // Line 7+8: adapt threshold
                        if ((no_sampling_points_window / t - cuttoff_point_c / w) <= 0) { //use here already w instead of N
                            noisy_threshold = Math.max(0, noisy_threshold - prop_part_u);
                        } else {
                            noisy_threshold = Math.min(2, noisy_threshold + prop_part_u);
                        }
                        // Line 9-12: decide whether to sample
                        if (noisy_dist >= noisy_threshold) { // wir verschenken hier budget im ersten window, wenn wir nicht samplen, wegen zeile 94. man sollte hier auch mit eps_rm arbeiten?
                            // sample
                            last_realize = sanitize(org_stream_t, this.lambda(eps_2_per_window_first / cuttoff_point_c));
                            sanitized_stream.add(last_realize);
                            total_budget_spent_eps_2 += eps_2_per_window_first / cuttoff_point_c;
                            no_sampling_points_window += 1;
                            used_budgets_eps_2[t] = eps_2_per_window_first / cuttoff_point_c;
                            samplingpoint[t] = true;

                        } else {
                            sanitized_stream.add(last_realize);
                            samplingpoint[t] = false;
                        }
                        // Line 14+15 -- should not happen in w event variant
                        if (t == length_N && no_sampling_points_window < cuttoff_point_c) { // end of the time series
                            double remaining_budget = eps_2_per_window_first - total_budget_spent_eps_2;
                            last_realize = sanitize(org_stream_t, this.lambda(remaining_budget));
                            sanitized_stream.add(last_realize);
                            no_sampling_points_window++;
                            used_budgets_eps_2[t] = remaining_budget;
                            total_budget_spent_eps_2 = this.epsilon;
                            samplingpoint[t] = true;

                        }
                    }
                }
            }}
        else {//second window begins
            if (DEBUG) {
                System.out.println(t-w-1 + " budget " + used_budgets_eps_2[t-w-1]);
                System.out.println(Arrays.toString(Arrays.copyOfRange(used_budgets_eps_2, t-w, t-1)));
            }
            double eps_spent = Arrays.stream(Arrays.copyOfRange(used_budgets_eps_2, Math.max(0, t-w), t-1)).sum();// total_budget_spent_eps_2 - used_budgets_eps_2[t-w-1];
            double eps_rm = eps_2_per_window - eps_spent;
            //System.err.println("Use wrong eps_rm check!");
            if (eps_rm <= 0.00001) { //rounding issues
                sanitized_stream.add(last_realize);
                samplingpoint[t]=false;}
            else {
                //line 7
                no_sampling_points_window = (int) Arrays.stream(Arrays.copyOfRange(samplingpoint, Math.max(0, t-w), t-1)).filter(x -> x.booleanValue()).count(); // paper: (int) (total_budget_spent_eps_2/(eps_2_per_window / cuttoff_point_c)); //number publications current window
                //Line 8-11: -- analogous to above
                double noisy_dist = avg_dissimilarity(org_stream_t, last_realize,
                        2d * cuttoff_point_c * delta / eps_1_per_window ); // L1 distance
                // Line 6
                double feedback_error_E = Math.abs(no_sampling_points_window / t - cuttoff_point_c / w); //between target sampling rate and actual one
                double prop_e = Math.abs(feedback_error_E - TOLERANCE_DELTA) / TOLERANCE_DELTA;
                double prop_part_u = prop_e * PROP_GAIN_THETA;
                // Line 7+8: adapt threshold
                if ((no_sampling_points_window / t - cuttoff_point_c / w) <= 0) {
                    noisy_threshold = Math.max(0, noisy_threshold - prop_part_u);
                } else {
                    noisy_threshold = Math.min(2, noisy_threshold + prop_part_u);
                }
                // Line 9-12: decide whether to sample
                if (noisy_dist >= noisy_threshold) {
                    // sample
                    last_realize = sanitize(org_stream_t, this.lambda(eps_2_per_window / cuttoff_point_c));
                    sanitized_stream.add(last_realize);
                    total_budget_spent_eps_2 += eps_2_per_window / cuttoff_point_c;
                    used_budgets_eps_2[t] = eps_2_per_window / cuttoff_point_c;
                    samplingpoint[t] = true;

                } else {
                    sanitized_stream.add(last_realize);
                    samplingpoint[t] = false;

                }
            }

        }

    }
        //System.out.println(Arrays.toString(last_realize));
        if (DEBUG) {
            System.out.println(Mechansim.outTSV(used_budgets_eps_1));
            System.out.println(Mechansim.outTSV(used_budgets_eps_2));
            System.out.println(Mechansim.outTSV(samplingpoint));
            System.out.println(Mechansim.outTSVFirstDimension(org_stream));
            System.out.println(Mechansim.outTSVFirstDimension(sanitized_stream));

        }
        return sanitized_stream;
    }

    double computeK(double cuttoff_point_c, int dimensionality_U, double delta, double no_time_points_N, double no_tuples_n) {
        // Theorem 5.4
        double root1 = (double) no_tuples_n * (double) no_tuples_n;
        double root2 = (8d * delta * delta
                + 32d * cuttoff_point_c * cuttoff_point_c * delta * delta)
                / ((double) dimensionality_U * cuttoff_point_c * cuttoff_point_c);
        double root = Math.round(Math.pow(root1 * root2, 1.0 / 3.0));
        return Math.min(root, 1d - cuttoff_point_c / (double) no_time_points_N);

    }

    @Override
    public String name() {
        return "DSAT-WEvent";
    }
}
