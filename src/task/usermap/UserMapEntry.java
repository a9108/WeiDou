package task.usermap;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Properties;

import basic.Config;
import basic.SystemOps;

public class UserMapEntry {
	
	public static void main(String[] args) throws Exception {
		
		Config.load("config.txt");
		UserMapTask.data=new DataSet();
		UserMapTask.data.load(Config.getValue("SelectDir"));
		UserMapTask.data.genTrain(0.3);
		
		UserMapTask task=new JUM();
		task.run();
		task.evaluate();
	}
}
