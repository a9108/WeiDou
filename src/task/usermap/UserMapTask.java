package task.usermap;

import java.util.HashMap;
import java.util.HashSet;

import BasicOps.Config;

public abstract class UserMapTask {
	public static DataSet data;
	protected HashMap<Integer, Integer> result;
	public abstract void run();
	public void evaluate(){
		int tp=0,fp=0,fn=0;
		for (Integer uid:result.keySet())
		if (!data.getTrain().containsKey(uid)){
			if (!data.getTruth().containsKey(uid)||
				!data.getTruth().get(uid).equals(result.get(uid)))
				fp++;
			else tp++;
		}
		fn=data.getTruth().size()-data.getTrain().size()-tp;
		double prec=tp/(tp+fp+0.0);
		double recall=tp/(tp+fn+0.0);
		double fscore=(2*prec*recall)/(prec+recall);
		System.out.println("Precision = "+prec);
		System.out.println("Recall = "+recall);
		System.out.println("F1-Score = "+fscore);		
	}
}
