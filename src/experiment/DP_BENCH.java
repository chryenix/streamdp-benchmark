package experiment;

import java.io.File;

import datagenerator.OutlierStream;

/**
 * Controller for starting experiments etc
 * @author b1074672
 *
 */
public class DP_BENCH {
	public static void main(String args[]) {
		if(args.length==0) {
			String[] temp = {"h"};//if no experiment specified run the bible experiment 
			args = temp;
		}
		if(contains(args, "h")) {
			System.out.println("DP Bench Experiment Framework");
			System.out.println("Usage");
			System.out.println("DP_BENCH h: print this manual");
			System.out.println("a: run artificial data experiments");
			System.out.println("r: run real-world-data experiments");
			System.out.println("p: print aggregated results to console and figures folder. Note, results need to be created first using a and r arguments.");
			System.out.println("g: geneate sanitized streams for outlier detetion. Storing data under "+OutlierStream.CSV_DIR);
			System.out.println("o: print anomaly-detection results to console and figures folder. Note, results need to be created first using g argument. In addtion, you need to run the anomaly detection framework");
		}
		if(contains(args, "a")) {
			File RESLT_DIR = new File(Experiment.RESLT_DIR);
			if(!RESLT_DIR.exists()) {
				RESLT_DIR.mkdir();
			}
			System.out.println("Running artificial experiments. Storing result data under "+Experiment.RESLT_DIR);
			Experiment.runArtificalExperiments();
		}
		if(contains(args, "r")) {
			File RESLT_DIR = new File(Experiment.RESLT_DIR);
			if(!RESLT_DIR.exists()) {
				RESLT_DIR.mkdir();
			}
			System.out.println("Running real-word-data experiments. Storing result data under "+Experiment.RESLT_DIR);
			Experiment.runRealWorldExperiments();
		}
		if(contains(args, "p")) {
			System.out.println("Creating aggregated results for Figures 2,3 and 5 and Table 6");
			Eval.run();
		}
		if(contains(args, "g")) {
			File CSV_DIR = new File(OutlierStream.CSV_DIR);
			if(!CSV_DIR.exists()) {
				CSV_DIR.mkdir();
			}
			System.out.println("Geneate sanitized streams for outlier detetion. Storing data under "+OutlierStream.CSV_DIR);
			OutlierStream.main(null);
			System.err.println();
		}
		if(contains(args, "o")) {
			System.out.println("Creating aggregated results for Figure 6");
			Eval.run_anomaly_detection();
		}
	}
	private static boolean contains(String[] array, String to_match) {
		for(String s : array) {
			if(s.equals(to_match)) {
				return true;
			}
		}
		return false;
	}
}
