package util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import datagenerator.OutlierStream;

/**
 * Count of Dodgers .data file
 * Extracts the desired info of one line (i.e., time stamp) of the file. 
 * @author b1074672
 *
 */
public class Count {
	public final LocalDateTime my_time;
	public int count;
	public double outlier_label = OutlierStream.NO_OUTLIER;
	
	public Count(String line) {
		String[] tokens = line.split(","); 
		DateTimeFormatter formatter_1 = DateTimeFormatter.ofPattern("M/d/yyyy k:mm");
		LocalDateTime temp;
		temp = LocalDateTime.parse(tokens[0], formatter_1);
		this.my_time = temp;
		this.count = Math.max(0, Integer.parseInt(tokens[1]));//Replace -1 values by 0
	}
	
	public Count(LocalDateTime my_time, int count, double outlier_label) {
		this.my_time = my_time;
		this.count = count;
		this.outlier_label = outlier_label;
	}

	public String toString() {
		return my_time+"\tcount=\t"+count+"\toutlier_label=\t"+outlier_label;
	}
}
