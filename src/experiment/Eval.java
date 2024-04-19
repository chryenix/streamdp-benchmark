package experiment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Eval {
	static final DecimalFormat df = new DecimalFormat("####.##", new DecimalFormatSymbols(Locale.US));
	static final String figures_dir = "./figures";
	static BufferedWriter output;
	static final String new_line = "\n";
	
	static List<String> get_all_file_names(String dir){
		File directory = new File(dir);
		if(!directory.exists()) {
			System.err.println(dir+" does not exist");
		}
		
	    try (Stream<Path> stream = Files.list(Paths.get(dir))) {
	        return stream
	          //.filter(file -> Files.is(file))
	          .map(Path::getFileName)
	          .map(Path::toString)
	          .collect(Collectors.toList());
	    }catch (Exception e) {
			System.err.println(e);
		}
	    return null;
	}
	
	static String[] get_all_directory_names(String dir){
		File directory = new File(dir);
		if(!directory.exists()) {
			System.err.println(dir+" does not exist");
		}
		
		String[] directories = directory.list(new FilenameFilter() {
			@Override
			public boolean accept(File current, String name) {
				return new File(current, name).isDirectory();
			}
		});
		return directories;
	}
	
	static List<String> get_artificial_vary_e_results(List<String> all_results){
		//length = 80
		ArrayList<String> ret = new ArrayList<String>(all_results.size());
		for(String s : all_results) {
			if(s.startsWith("eps-0-period_length80")) {//0=MAE
				ret.add(s);
			}
		}
		return ret;
	}
	
	static List<String> get_artificial_vary_w_results(List<String> all_results){
		//length = 80
		ArrayList<String> ret = new ArrayList<String>(all_results.size());
		for(String s : all_results) {
			if(s.startsWith("w-0-period_length80")) {//0=MAE
				ret.add(s);
			}
		}
		return ret;
	}
	
	public static void main(String[] args) {
		List<String> all_results = get_all_file_names(Experiment.RESLT_DIR);
		/*for(String s : all_results) {
			System.out.println(s);
		}*/
		
		//create_and_print_fig_2a(all_results);
		//create_and_print_fig_2b(all_results);
		create_and_print_fig_3(all_results);
		create_and_print_fig_5(all_results);
		//create_and_print_tab_6();
		//Eval.run();
	}
	
	public static void run() {
		List<String> all_results = get_all_file_names(Experiment.RESLT_DIR);
		if(all_results.isEmpty()) {
			System.err.println("Eval.run() - result dir is empty or does not exist at "+Experiment.RESLT_DIR);
			return;
		}
		File figures = new File(figures_dir);
		if(!figures.exists()) {
			figures.mkdir();
		}
		
		create_and_print_fig_2a(all_results);
		create_and_print_fig_2b(all_results);
		create_and_print_fig_3(all_results);
		create_and_print_fig_5(all_results);
		create_and_print_tab_6();
	}
	
	public static void run_anomaly_detection() {
		
		String[] directories = get_all_directory_names(Experiment.RESLT_DIR);
		if(directories.length!=1) {
			System.err.println("Eval.run_anomaly_detection() - result dir appears to contain no or multiple anomaly-detection results. Expecting one in "+Experiment.RESLT_DIR);
			System.err.println(Arrays.toString(directories));
			return;
		}
		File figures = new File(figures_dir);
		if(!figures.exists()) {
			figures.mkdir();
		}
		File result_file = new File(Experiment.RESLT_DIR+"/"+directories[0]+"/results.csv");

		
		//Read all results and store them
		try {
			if(!result_file.exists()) {
				System.err.println(result_file.getCanonicalPath()+" Does not exist");
				return;
			}
			System.out.println("Reading anomaly detection results from "+result_file.getCanonicalPath());
			BufferedReader reader = new BufferedReader(new FileReader(result_file));
			String line;
			ArrayList<String[]> all_lines = new ArrayList<String[]>(20000);
			reader.readLine();//skip heading
			
			int num_lines = 0;
			while((line = reader.readLine()) != null) {
				num_lines++;
				String[] line_values = line.split(",");
				String[] short_line = {line_values[0],line_values[2],line_values[line_values.length-1]};//e.g. [knn], [eps=0.1-dodgers-12-Sample-7.csv,UNSUPERVISED], [0.5254466079247907]
				all_lines.add(short_line);
				if(num_lines%100==0) {
					System.out.println(Arrays.toString(short_line));
				}
			}
			reader.close();
			
			String[] mechanism_names = {"Uniform", "Sample", "Fast", "BD", "BA", "RescueDP", "Pegasus", "AdaPub", "DSAT"};
			String[] data_set_names = {"Dodgers MLAED","Pattern Anomalies LOF","Extrema Anomalies LOF"};
			
			open_result_file("fig_6.tsv");
			// Fig 6 (a) vary-w experiments
			out("Fig 6 (a)");
			for(String data_set : data_set_names) {
				out(new_line);
				out(data_set);
				out(new_line);
				String[] w_s = {"1","2","4","8","16","32"};
				for(String w : w_s) {
					out("\tw="+w);
				}
				out(new_line);
				for(String m : mechanism_names) {
					out(m);//TODO results
					print_results(data_set, m, "w=", all_lines);
					out(new_line);
				}
				out(new_line);
				out(new_line);
			}
			
			out(new_line);
			// Fig 6 (b) vary-e experiments
			out("Fig 6 (b)");
			for(String data_set : data_set_names) {
				out(new_line);
				out(data_set);
				out(new_line);
				String[] e_s = {"0.1","0.3","0.5","0.7","0.9","1.0"};
				for(String e : e_s) {
					out("\te="+e);
				}
				out(new_line);
				for(String m : mechanism_names) {
					out(m);//TODO results
					print_results(data_set, m, "eps=", all_lines);
					out(new_line);
				}
				out(new_line);
				out(new_line);
			}
			
			close_result_file();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void print_results(String data_set, String mechanism, String experiment, ArrayList<String[]> all_lines) {
		ArrayList<String[]> relevant_lines = get_relevant_lines(data_set, mechanism, experiment, all_lines);
		/*for(String[] line : relevant_lines) {
			System.out.println(Arrays.toString(line));	
		}*/
		String[] params = null; 
		if(experiment.equals("w=")) {
			String[] temp = {"1","2","4","8","16","32"};
			params = temp;
		}else if(experiment.equals("eps=")){
			String[] temp = {"0.1","0.3","0.5","0.7","0.9","1.0"};
			params = temp;
		}else{
			System.err.println("Eval.print_results() unknown experiment type "+experiment);
			return;
		}
		for(String w : params) {
			double counter = 0;
			double sum = 0;
			for(String[] line : relevant_lines) {
				if(line[1].startsWith(experiment+w+"-")) {
					counter++;
					double val = Double.parseDouble(line[2]);
					sum += val;
				}
			}
			if(counter!=10) {
				System.err.println("Eval.print_results() should always be 10 repitions, but got "+counter);
			}
			double agg = sum / counter;
			out("\t"+agg);
		}
	}

	private static ArrayList<String[]> get_relevant_lines(String data_set, String mechanism, String experiment, ArrayList<String[]> all_lines) {
		ArrayList<String[]> relevant_lines = new ArrayList<String[]>();
		if(data_set.equals("Dodgers MLAED")) {
			for(String[] line : all_lines) {
				if(line[0].equals("dwt_mlead") && line[1].contains("dodgers") && line[1].contains(experiment) && line[1].contains(mechanism)) {
					relevant_lines.add(line);
				}
			}
		}else if(data_set.equals("Pattern Anomalies LOF")) {
			for(String[] line : all_lines) {
				if(line[0].equals("lof") && line[1].contains("pattern") && line[1].contains(experiment) && line[1].contains(mechanism)) {
					relevant_lines.add(line);
				}
			}
		}else if(data_set.equals("Extrema Anomalies LOF")) {
			for(String[] line : all_lines) {
				if(line[0].equals("lof") && line[1].contains("extrema") && line[1].contains(experiment) && line[1].contains(mechanism)) {
					relevant_lines.add(line);
				}
			}
		}else{
			System.err.println("Eval.get_relevant_lines(): unknown data set "+data_set);
		}
		return relevant_lines;
	}

	private static void create_and_print_tab_6() {
		open_result_file("tab_6.tsv");
		// TODO Auto-generated method stub
		//Load all md data
		final int[] data_sets_md = {
			Experiment.STAT_FLU
			, Experiment.TDRIVE_EXTENDED
			, Experiment.ADAPUB_RETAIL
			, Experiment.WANG_TAXI_ECPK
			, Experiment.WORLD_CUP
		};
		final String[] data_set_names = {
			"StateFlu"
			, "TDrive"
			, "Retail"
			, "TaxiPorto"
			, "WorldCup"
		};
		
		ArrayList<ArrayList<double[]>> streams = new ArrayList<ArrayList<double[]>>(data_sets_md.length);
		for(int data_set=0;data_set<data_sets_md.length;data_set++) {
			streams.add(Experiment.load(data_sets_md[data_set]));
		}
		
		System.out.println("Printing Table 6");
		out("Stream s\tdim\tLength p\tQuery result distribution"+new_line);
		out("\t\t\tq_min\tq_max\t90% quantile"+new_line);
		for(int data_set=0;data_set<data_sets_md.length;data_set++) {
			ArrayList<double[]> stream = streams.get(data_set);
			out(data_set_names[data_set]+"\t"+stream.get(0).length+"\t"+stream.size()+"\t");
			ArrayList<Double> all_values = new ArrayList<Double>(stream.size()*stream.get(0).length);
			for(double[] arr : stream) {
				for(double d : arr) {
					all_values.add(d);
				}
			}
			Collections.sort(all_values);
			out(all_values.get(0)+"\t");//min
			out(all_values.get(all_values.size()-1)+"\t");//max
			double quantile_index = all_values.size();
			quantile_index *= 0.9d;
			out(all_values.get((int)quantile_index)+new_line);
		}
		
		close_result_file();
	}

	private static void create_and_print_fig_5(List<String> all_results) {
		open_result_file("fig_5.tsv");
		final int[] data_sets_md = {Experiment.STAT_FLU, Experiment.TDRIVE_EXTENDED, Experiment.ADAPUB_RETAIL, Experiment.WORLD_CUP, Experiment.WANG_TAXI_ECPK};
		final String[] dataset_names = {"State Flu (51 dim.)", "TDrive (100 dim.)", "Retail (1,289 dim.)", "World Cup (1,289 dim.)", "Taxi Porto (1,289 dim.)"};
		
		out("Fig 5 (a)"+new_line);
		for(int i=0;i<data_sets_md.length;i++) {
			out(dataset_names[i]+new_line);	
			print_all_mechanism_w_results_one_file(all_results, data_sets_md[i]);
			out(new_line);
		}
		
		out("Fig 5 (b)"+new_line);
		for(int i=0;i<data_sets_md.length;i++) {
			out(dataset_names[i]+new_line);	
			print_all_mechanism_e_results_one_file(all_results, data_sets_md[i]);
			out(new_line);
		};
		close_result_file();
	}
	
	private static void create_and_print_fig_3(List<String> all_results) {
		open_result_file("fig_3.tsv");
		final int[] data_sets_1d = {Experiment.FAST_FLU_OUTPATIENT_EXTENDED, Experiment.FLU_NUM_DEATH, Experiment.UNEMPLOY};
		
		out("Fig 3 (a)"+new_line);
		out("Flu Outpatient"+new_line);
		print_all_mechanism_w_results_one_file(all_results, data_sets_1d[0]);
		out(new_line);
		out("Flu Death"+new_line);
		print_all_mechanism_w_results_one_file(all_results, data_sets_1d[1]);
		out(new_line);
		out("Unemployment"+new_line);
		print_all_mechanism_w_results_one_file(all_results, data_sets_1d[2]);
		out(new_line);
		
		out("Fig 3 (b)"+new_line);
		out("Flu Outpatient"+new_line);
		print_all_mechanism_e_results_one_file(all_results, data_sets_1d[0]);
		out(new_line);
		out("Flu Death"+new_line);
		print_all_mechanism_e_results_one_file(all_results, data_sets_1d[1]);
		out(new_line);
		out("Unemployment"+new_line);
		print_all_mechanism_e_results_one_file(all_results, data_sets_1d[2]);
		out(new_line);
		close_result_file();
		
	}
	
	static List<String> get_files_starting_with(String file_prefix, List<String> all_results) {
		ArrayList<String> ret = new ArrayList<String>(all_results.size());
		for(String s : all_results) {
			if(s.startsWith(file_prefix)) {
				ret.add(s);
			}
		}
		return ret;
	}

	private static void print_all_mechanism_w_results_one_file(List<String> all_results, int file_num) {
		String file_prefix = "w-0-"+file_num;
		List<String> vary_e_results = get_files_starting_with(file_prefix, all_results);
		
		String[] mechanism_names = {"Uniform", "Sample", "Fast", "BD", "BA", "RescueDP", "PeGaSus", "AdaPub", "DSAT"};
		for(String m : mechanism_names) {
			for(String f : vary_e_results) {
				if(f.contains(m)) {
					double[] results = get_aggregated_file_content(f);
					//System.out.println(m);
					out(m+"\t"+to_tsv(results)+new_line);
				}
			}
		}
	}
	
	private static void print_all_mechanism_e_results_one_file(List<String> all_results, int file_num) {
		String file_prefix = "eps-0-"+file_num;
		List<String> vary_e_results = get_files_starting_with(file_prefix, all_results);
		
		String[] mechanism_names = {"Uniform", "Sample", "Fast", "BD", "BA", "RescueDP", "PeGaSus", "AdaPub", "DSAT"};
		for(String m : mechanism_names) {
			for(String f : vary_e_results) {
				if(f.contains(m)) {
					double[] results = get_aggregated_file_content(f);
					//System.out.println(m);
					out(m+"\t"+to_tsv(results)+new_line);
				}
			}
		}
	}
	
	static String to_tsv(double[] arr) {
		String ret = df.format(arr[0])+"";
		for(int i=1;i<arr.length;i++) {
			ret+="\t"+df.format(arr[i]);
		}
		return ret;
	}

	static double[][] get_mechanism_results_e(String mechanism_name, List<String> artificial_vary_e_results){
		double[][] results = new double[6][4];//6 eps values, 4 amplitudes
		System.out.println(mechanism_name);
		for(String s : artificial_vary_e_results) {
			if(s.contains(mechanism_name)) {
				System.out.println(s);
				int column = get_column_by_amplitude(s);
				fill(results, column,s);
			}
		}
		return results;
	}
	
	static double[][] get_mechanism_results_w(String mechanism_name, List<String> artificial_vary_e_results){
		double[][] results = new double[5][4];//6 eps values, 4 amplitudes
		System.out.println(mechanism_name);
		for(String s : artificial_vary_e_results) {
			if(s.contains(mechanism_name)) {
				System.out.println(s);
				int column = get_column_by_amplitude(s);
				fill(results, column,s);
			}
		}
		return results;
	}
	
	private static void create_and_print_fig_2b(List<String> all_results) {
		open_result_file("fig_2b.tsv");
		List<String> artificial_vary_w_results = get_artificial_vary_w_results(all_results);
		for(String s : artificial_vary_w_results) {
			//System.out.println(s);
		}
		String mechanism_name = "Uniform";
		//Uniform
		double[][] uniform_results = get_mechanism_results_w(mechanism_name,artificial_vary_w_results); 
		out(mechanism_name, uniform_results);
		
		double[][] min_mae = copy(uniform_results);
		
		//Sample
		mechanism_name = "Sample";
		double[][] sample_results = get_mechanism_results_w(mechanism_name,artificial_vary_w_results); 
		out(mechanism_name, sample_results);
		min(min_mae, sample_results);
		
		//AdaPub
		mechanism_name = "AdaPub";
		double[][] adapub_results = get_mechanism_results_w(mechanism_name,artificial_vary_w_results); 
		out(mechanism_name, adapub_results);
		min(min_mae, adapub_results);
		
		//BA
		mechanism_name = "BA";
		double[][] ba_results = get_mechanism_results_w(mechanism_name,artificial_vary_w_results); 
		out(mechanism_name, ba_results);
		min(min_mae, ba_results);
		
		//BD
		mechanism_name = "BD";
		double[][] bd_results = get_mechanism_results_w(mechanism_name,artificial_vary_w_results); 
		out(mechanism_name, bd_results);
		min(min_mae, bd_results);
		
		//DSAT
		mechanism_name = "DSAT";
		double[][] dast_results = get_mechanism_results_w(mechanism_name,artificial_vary_w_results); 
		out(mechanism_name, dast_results);
		min(min_mae, dast_results);
		
		//FAST
		mechanism_name = "Fast";
		double[][] fast_results = get_mechanism_results_w(mechanism_name,artificial_vary_w_results); 
		out(mechanism_name, fast_results);
		min(min_mae, fast_results);
		
		//BD
		mechanism_name = "Pegasus";
		double[][] pegasus_results = get_mechanism_results_w(mechanism_name,artificial_vary_w_results); 
		out(mechanism_name, pegasus_results);
		min(min_mae, pegasus_results);
		
		//DSAT
		mechanism_name = "RescueDP";
		double[][] rescuedp_results = get_mechanism_results_w(mechanism_name,artificial_vary_w_results); 
		out(mechanism_name, rescuedp_results);
		min(min_mae, rescuedp_results);
		
		out("Min MAE", min_mae);
		
		{
			String[] m = {"Uniform", "Sample", "AdaPub"};
			double[][][] results_line = {uniform_results, sample_results,adapub_results};
			out_delta_mae(min_mae,m,results_line);
		}
		{
			String[] m = {"BA", "BD", "DSAT"};
			double[][][] results_line = {ba_results, bd_results, dast_results};
			out_delta_mae(min_mae,m,results_line);
		}
		{
			String[] m = {"FAST_w", "PeGaSuS", "RescueDP"};
			double[][][] results_line = {fast_results, pegasus_results, rescuedp_results};
			out_delta_mae(min_mae,m,results_line);
		}
		close_result_file();
	}

	static void open_result_file(String figure_name) {
		try {
			output = new BufferedWriter(new FileWriter(figures_dir+File.separator+figure_name));
		} catch (IOException e) {
			output = null;
			e.printStackTrace();
		}
	}
	
	static void close_result_file(){
		try {
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	 
	private static void create_and_print_fig_2a(List<String> all_results) {
		open_result_file("fig_2a.tsv");
		List<String> artificial_vary_e_results = get_artificial_vary_e_results(all_results);

		String mechanism_name = "Uniform";
		//Uniform
		double[][] uniform_results = get_mechanism_results_e(mechanism_name,artificial_vary_e_results); 
		out(mechanism_name, uniform_results);
		
		double[][] min_mae = copy(uniform_results);
		
		//Sample
		mechanism_name = "Sample";
		double[][] sample_results = get_mechanism_results_e(mechanism_name,artificial_vary_e_results); 
		out(mechanism_name, sample_results);
		min(min_mae, sample_results);
		
		//AdaPub
		mechanism_name = "AdaPub";
		double[][] adapub_results = get_mechanism_results_e(mechanism_name,artificial_vary_e_results); 
		out(mechanism_name, adapub_results);
		min(min_mae, adapub_results);
		
		//BA
		mechanism_name = "BA";
		double[][] ba_results = get_mechanism_results_e(mechanism_name,artificial_vary_e_results); 
		out(mechanism_name, ba_results);
		min(min_mae, ba_results);
		
		//BD
		mechanism_name = "BD";
		double[][] bd_results = get_mechanism_results_e(mechanism_name,artificial_vary_e_results); 
		out(mechanism_name, bd_results);
		min(min_mae, bd_results);
		
		//DSAT
		mechanism_name = "DSAT";
		double[][] dast_results = get_mechanism_results_e(mechanism_name,artificial_vary_e_results); 
		out(mechanism_name, dast_results);
		min(min_mae, dast_results);
		
		//FAST
		mechanism_name = "Fast";
		double[][] fast_results = get_mechanism_results_e(mechanism_name,artificial_vary_e_results); 
		out(mechanism_name, fast_results);
		min(min_mae, fast_results);
		
		//BD
		mechanism_name = "Pegasus";
		double[][] pegasus_results = get_mechanism_results_e(mechanism_name,artificial_vary_e_results); 
		out(mechanism_name, pegasus_results);
		min(min_mae, pegasus_results);
		
		//DSAT
		mechanism_name = "RescueDP";
		double[][] rescuedp_results = get_mechanism_results_e(mechanism_name,artificial_vary_e_results); 
		out(mechanism_name, rescuedp_results);
		min(min_mae, rescuedp_results);
		
		out("Min MAE", min_mae);
		
		{
			String[] m = {"Uniform", "Sample", "AdaPub"};
			double[][][] results_line = {uniform_results, sample_results,adapub_results};
			out_delta_mae(min_mae,m,results_line);
		}
		{
			String[] m = {"BA", "BD", "DSAT"};
			double[][][] results_line = {ba_results, bd_results, dast_results};
			out_delta_mae(min_mae,m,results_line);
		}
		{
			String[] m = {"FAST_w", "PeGaSuS", "RescueDP"};
			double[][][] results_line = {fast_results, pegasus_results, rescuedp_results};
			out_delta_mae(min_mae,m,results_line);
		}
		close_result_file();
	}

	private static double[][] copy(double[][] arr) {
		double[][] ret = new double[arr.length][arr[0].length];
		for(int l=0;l<arr.length;l++) {
			for(int c=0;c<arr[0].length;c++) {
				ret[l][c] = arr[l][c];
			}
		}
		return ret;
	}
	
	static void out(String s){
		System.out.print(s);
		try {
			output.write(s);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void out_delta_mae(double[][] min_mae, String[] mechanisms, double[][][] results_line) {
		DecimalFormat df = new DecimalFormat("#,###.##", new DecimalFormatSymbols(Locale.US));
		
		out("\t"); 
		for(String m : mechanisms) {
			out(m+"\t\t\t\t\t");
		}
		out(new_line);
		int num_mechs = results_line.length;
		int num_lines = results_line[0].length;
		int num_columns = results_line[0][0].length;
		
		String[] all_eps = {"0.1","0.3","0.5","0.7","0.9","1"};
		
		out("a/e\t10\t100\t1000\t10000\t\t10\t100\t1000\t10000\t\t10\t100\t1000\t10000"+new_line);
		for(int line = 0;line<num_lines;line++) {
			out(all_eps[line]+"\t");
			for(int m=0;m<num_mechs;m++) {
				double[][] m_r = results_line[m];
				for(int column=0;column<num_columns;column++) {
					double delta_mae = m_r[line][column] / min_mae[line][column];
					out(df.format(delta_mae)+"\t");
				}
				out("\t");
			}
			out(new_line);
		}
	}

	private static void min(double[][] min_mae, double[][] results) {
		for(int line=0;line<min_mae.length;line++) {
			for(int column=0;column<min_mae[0].length;column++) {
				if(results[line][column]<min_mae[line][column]) {
					min_mae[line][column] = results[line][column];
				}
			}
		}
	}

	private static void out(String name, double[][] results) {
		System.out.println(name);
		for(double[] arr : results) {
			System.out.println(Arrays.toString(arr));
		}
		
	}

	/**
	 * Fills one column in result array
	 * 
	 * @param uniform_results
	 * @param column
	 * @param path
	 */
	private static void fill(double[][] uniform_results, int column, String path) {
		File f = new File(Experiment.RESLT_DIR+File.separator+path);
		if(!f.exists()) {
			System.err.println("!f.exists()");//TODO
		}
		double[] agg = new double[uniform_results.length];
		double num_lines = 0;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(f));
			String line;
			reader.readLine();//skip heading
			
			while((line = reader.readLine()) != null) {
				num_lines++;
				String[] line_values = line.split("\t");
				if(line_values.length!=agg.length) {
					System.err.println("line_values.length!=temp.length");
				}
				for(int i=0;i<line_values.length;i++) {
					String s = line_values[i];
					double d = Double.parseDouble(s);
					agg[i] += d;
				}
			}
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		for(int i=0;i<agg.length;i++) {
			uniform_results[i][column] = agg[i]/num_lines;
		}
	}
	
	private static double[] get_aggregated_file_content(String path) {
		File f = new File(Experiment.RESLT_DIR+File.separator+path);
		if(!f.exists()) {
			System.err.println("!f.exists()");//TODO
		}
		double[] agg = null;
		double num_lines = 0;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(f));
			String line;
			reader.readLine();//skip heading
			
			while((line = reader.readLine()) != null) {
				num_lines++;
				String[] line_values = line.split("\t");
				if(agg==null) {
					agg = new double[line_values.length];
				}
				if(agg.length!=line_values.length) {
					System.err.println("agg.length!=line_values.length");
				}
				
				for(int i=0;i<line_values.length;i++) {
					String s = line_values[i];
					double d = Double.parseDouble(s);
					agg[i] += d;
				}
			}
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		for(int i=0;i<agg.length;i++) {
			agg[i] /= num_lines;
		}
		return agg;
	}

	private static int get_column_by_amplitude(String s) {
		if(s.contains("_a10000_")) {
			return 3;
		}else if(s.contains("_a1000_")) {
			return 2;
		}else if(s.contains("_a100_")) {
			return 1;
		}else if(s.contains("_a10_")) {
			return 0;
		}
		System.err.println("Unknown amplitude for "+s);
		return -1;
	}
}
