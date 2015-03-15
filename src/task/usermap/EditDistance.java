package task.usermap;

import java.util.HashMap;
import java.util.LinkedList;

import BasicOps.Config;

public class EditDistance extends UserMapTask {

	@Override
	public void run() {
		result = new HashMap<Integer, Integer>();

		final LinkedList<Integer> Q = new LinkedList<Integer>();
		for (int i = 0; i < data.getSizeDouban(); i++)
			Q.add(i);

		Thread[] workers = new Thread[Integer.valueOf(Config
				.getValue("#Thread"))];
		for (int i = 0; i < workers.length; i++) {
			workers[i] = new Thread() {
				public void run() {
					for (;;) {
						int i;
						synchronized (Q) {
							if (Q.isEmpty())
								return;
							i = Q.removeFirst();
						}
						int res=-1,mi=100;
						String curname = data.getDouban_username(i);
						for (int j = 0; j < data.getSizeWeibo(); j++){
							int dist=	basic.algorithm.StringAlg.EditDistance(
											curname, data.getWeibo_username(j));
							if (dist<mi){
								res=j;
								mi=dist;
							}
						}
						System.out.println(i+"\t"+res+"\t"+mi);
						if (res!=-1) synchronized (result) {
							result.put(i, res);
						}
					}
				};
			};
			workers[i].start();
		}
		for (Thread worker : workers)
			try {
				worker.join();
			} catch (Exception e) {
			}
	}
	
}
