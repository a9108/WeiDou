package task.movierec;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Properties;

import basic.Config;
import basic.SystemOps;

public class RankingEntry {
	
	public static void main(String[] args) throws Exception {
		
		Config.load("config.txt");
		RankingTask.data=new DataSet();
		RankingTask.data.load(Config.getValue("SelectDir"));
		
		RankingTask task=new JUM();
		task.genTrain(0.3);
		task.train();
		task.evaluate();
	}
}
