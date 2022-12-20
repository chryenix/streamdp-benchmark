package util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

/**
 * Baseball Game at Dodgers arena
 * @author b1074672
 *
 */
public class Event{
	public final LocalDateTime begin;
	public final LocalDateTime end;
	/**
	 * 
	 * @param line e.g., "04/12/05,13:10:00,16:23:00,55892,San Francisco,W 9-8"
	 */
	public Event(String line){
		String[] tokens = line.split(","); 
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yykk:mm:ss");
		
		this.begin = LocalDateTime.parse(tokens[0]+tokens[1], formatter);
		this.end   = LocalDateTime.parse(tokens[0]+tokens[2], formatter);
	}
	
	public static boolean is_outlier(LocalDateTime time, ArrayList<Event> outlier_events){
		for(Event e : outlier_events) {
			if(e.is_during_event(time)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean is_during_event(LocalDateTime time) {
		if(this.begin.isBefore(time)) {
			return false;//event started after time
		}
		if(this.end.isAfter(time)) {
			return false;//event finished before time
		}
		return true;
	}
	
	public String toString(){
		return "From "+begin+" to "+end; 
	}
}
