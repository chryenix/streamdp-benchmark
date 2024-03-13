package experiment;

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
			System.out.println("p: print aggregated results to console. Note, results need to be created first using a and r arguments.");
			System.out.println("g: geneate sanitized streams for outlier detetion. Storing data under "+OutlierStream.CSV_DIR);
		}
		if(contains(args, "a")) {
			System.out.println("Running artificial experiments. Storing result data under "+Experiment.RESLT_DIR);
			Experiment.runArtificalExperiments();
		}
		if(contains(args, "r")) {
			System.out.println("Running real-word-data experiments. Storing result data under "+Experiment.RESLT_DIR);
			Experiment.runRealWorldExperiments();
		}
		if(contains(args, "p")) {
			System.out.println("Creating aggregated results for Figures 2,3 and 5");
			Eval.run();
		}
		if(contains(args, "g")) {
			System.out.println("Geneate sanitized streams for outlier detetion. Storing data under "+OutlierStream.CSV_DIR);
			OutlierStream.main(null);
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
