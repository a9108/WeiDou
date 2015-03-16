package task.usermap;

import java.util.HashMap;
import java.util.LinkedList;

import basic.Config;

public class ExactMatch extends UserMapTask {
	
//	Precision = 0.9944581280788177
//	Recall = 0.20012391573729862
//	F1-Score = 0.33319579121105836

	@Override
	public void run() {
		result = new HashMap<Integer, Integer>();
		
		final LinkedList<Integer> Q=new LinkedList<Integer>();
		for (int i=0;i<data.getSizeDouban();i++)
			Q.add(i);
		
		Thread [] workers=new Thread[Integer.valueOf(Config.getValue("#Thread"))];
		for (int i=0;i<workers.length;i++){
			workers[i]=new Thread(){
				public void run() {
					for (;;){
						int i;
						synchronized (Q) {
							if (Q.isEmpty()) return;
							i=Q.removeFirst();
						}
						for (int j = 0; j < data.getSizeWeibo(); j++)
							if (data.getDouban_username(i)
									.equals(data.getWeibo_username(j))){
								synchronized (result) {
									result.put(i, j);
									break;
								}
							}
					}
				};
			};
			workers[i].start();
		}
		for (Thread worker:workers)
			try{
				worker.join();
			}catch (Exception e) {
			}
	}
}
