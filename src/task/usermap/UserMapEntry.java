package task.usermap;

import BasicOps.Config;

public class UserMapEntry {
	
	public static void main(String[] args) {
		Config.load("config.txt");
		UserMapTask.data=new DataSet();
		UserMapTask.data.load(Config.getValue("SelectDir"));
		UserMapTask.data.genTrain(0.3);
		
		UserMapTask task=new EditDistance();
		task.run();
		task.evaluate();
	}
}
