package experiment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
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
		//create_and_print_fig_3(all_results);
		//create_and_print_fig_5(all_results);
		create_and_print_tab_6();
	}
	
	static void run() {
		List<String> all_results = get_all_file_names(Experiment.RESLT_DIR);
		
		create_and_print_fig_2a(all_results);
		create_and_print_fig_2b(all_results);
		create_and_print_fig_3(all_results);
		create_and_print_fig_5(all_results);
		create_and_print_tab_6();
	}
	
	private static void create_and_print_tab_6() {
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
		System.out.println("Stream s\tdim\tLength p\tQuery result distribution");
		System.out.println("\t\t\tq_min\tq_max\t90% quantile");
		for(int data_set=0;data_set<data_sets_md.length;data_set++) {
			ArrayList<double[]> stream = streams.get(data_set);
			System.out.print(data_set_names[data_set]+"\t"+stream.get(0).length+"\t"+stream.size()+"\t");
			ArrayList<Double> all_values = new ArrayList<Double>(stream.size()*stream.get(0).length);
			for(double[] arr : stream) {
				for(double d : arr) {
					all_values.add(d);
				}
			}
			Collections.sort(all_values);
			System.out.print(all_values.get(0)+"\t");//min
			System.out.print(all_values.get(all_values.size()-1)+"\t");//max
			double quantile_index = all_values.size();
			quantile_index *= 0.9d;
			System.out.println(all_values.get((int)quantile_index));
		}
		
		//for each data set output dim etc.
	}

	private static void create_and_print_fig_5(List<String> all_results) {
		final int[] data_sets_md = {Experiment.STAT_FLU, Experiment.TDRIVE_EXTENDED, Experiment.ADAPUB_RETAIL, Experiment.WORLD_CUP, Experiment.WANG_TAXI_ECPK};
		final String[] dataset_names = {"State Flu (51 dim.)", "TDrive (100 dim.)", "Retail (1,289 dim.)", "World Cup (1,289 dim.)", "Taxi Porto (1,289 dim.)"};
		
		System.out.println("Fig 5 (a)");
		for(int i=0;i<data_sets_md.length;i++) {
			System.out.println(dataset_names[i]);	
			print_all_mechanism_w_results_one_file(all_results, data_sets_md[i]);
			System.out.println();
		}
		
		System.out.println("Fig 5 (b)");
		for(int i=0;i<data_sets_md.length;i++) {
			System.out.println(dataset_names[i]);	
			print_all_mechanism_e_results_one_file(all_results, data_sets_md[i]);
			System.out.println();
		};
		
	}
	
	private static void create_and_print_fig_3(List<String> all_results) {
		final int[] data_sets_1d = {Experiment.FAST_FLU_OUTPATIENT_EXTENDED, Experiment.FLU_NUM_DEATH, Experiment.UNEMPLOY};
		
		System.out.println("Fig 3 (a)");
		System.out.println("Flu Outpatient");
		print_all_mechanism_w_results_one_file(all_results, data_sets_1d[0]);
		System.out.println();
		System.out.println("Flu Death");
		print_all_mechanism_w_results_one_file(all_results, data_sets_1d[1]);
		System.out.println();
		System.out.println("Unemployment");
		print_all_mechanism_w_results_one_file(all_results, data_sets_1d[2]);
		System.out.println();
		
		System.out.println("Fig 3 (b)");
		System.out.println("Flu Outpatient");
		print_all_mechanism_e_results_one_file(all_results, data_sets_1d[0]);
		System.out.println();
		System.out.println("Flu Death");
		print_all_mechanism_e_results_one_file(all_results, data_sets_1d[1]);
		System.out.println();
		System.out.println("Unemployment");
		print_all_mechanism_e_results_one_file(all_results, data_sets_1d[2]);
		System.out.println();
		
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
		for(String s : vary_e_results) {
			//System.out.println(s);
		}
		
		String[] mechanism_names = {"Uniform", "Sample", "Fast", "BD", "BA", "RescueDP", "PeGaSus", "AdaPub", "DSAT"};
		for(String m : mechanism_names) {
			for(String f : vary_e_results) {
				if(f.contains(m)) {
					double[] results = get_aggregated_file_content(f);
					//System.out.println(m);
					System.out.println(m+"\t"+to_tsv(results));
				}
			}
		}
	}
	
	private static void print_all_mechanism_e_results_one_file(List<String> all_results, int file_num) {
		String file_prefix = "eps-0-"+file_num;
		List<String> vary_e_results = get_files_starting_with(file_prefix, all_results);
		for(String s : vary_e_results) {
			//System.out.println(s);
		}
		
		String[] mechanism_names = {"Uniform", "Sample", "Fast", "BD", "BA", "RescueDP", "PeGaSus", "AdaPub", "DSAT"};
		for(String m : mechanism_names) {
			for(String f : vary_e_results) {
				if(f.contains(m)) {
					double[] results = get_aggregated_file_content(f);
					//System.out.println(m);
					System.out.println(m+"\t"+to_tsv(results));
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
	}

	private static void create_and_print_fig_2a(List<String> all_results) {
		List<String> artificial_vary_e_results = get_artificial_vary_e_results(all_results);
		for(String s : artificial_vary_e_results) {
			//System.out.println(s);
		}
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

	private static void out_delta_mae(double[][] min_mae, String[] mechanisms, double[][][] results_line) {
		DecimalFormat df = new DecimalFormat("#,###.##", new DecimalFormatSymbols(Locale.US));
		
		System.out.print("\t");
		for(String m : mechanisms) {
			System.out.print(m+"\t\t\t\t\t");
		}
		System.out.println();
		int num_mechs = results_line.length;
		int num_lines = results_line[0].length;
		int num_columns = results_line[0][0].length;
		
		String[] all_eps = {"0.1","0.3","0.5","0.7","0.9","1"};
		
		System.out.println("a/e\t10\t100\t1000\t10000\t\t10\t100\t1000\t10000\t\t10\t100\t1000\t10000");
		for(int line = 0;line<num_lines;line++) {
			System.out.print(all_eps[line]+"\t");
			for(int m=0;m<num_mechs;m++) {
				double[][] m_r = results_line[m];
				for(int column=0;column<num_columns;column++) {
					double delta_mae = m_r[line][column] / min_mae[line][column];
					System.out.print(df.format(delta_mae)+"\t");
				}
				System.out.print("\t");
			}
			System.out.println();
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
