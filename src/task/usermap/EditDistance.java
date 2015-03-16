package task.usermap;

import java.util.HashMap;
import java.util.LinkedList;

import basic.Config;

public class EditDistance extends UserMapTask {

	// Precision = 0.31901074194354234
	// Recall = 0.31901074194354234
	// F1-Score = 0.31901074194354234
	// ===== THRES : 10 =====
	// Precision = 0.3194340095166541
	// Recall = 0.31863602298276295
	// F1-Score = 0.3190345172586293
	// ===== THRES : 9 =====
	// Precision = 0.31975432439207824
	// Recall = 0.31863602298276295
	// F1-Score = 0.31919419419419426
	// ===== THRES : 8 =====
	// Precision = 0.3205886052068922
	// Recall = 0.3183862103422433
	// F1-Score = 0.31948361220780847
	// ===== THRES : 7 =====
	// Precision = 0.3219888663967611
	// Recall = 0.3178865850612041
	// F1-Score = 0.3199245757385292
	// ===== THRES : 6 =====
	// Precision = 0.32531661762824615
	// Recall = 0.3176367724206845
	// F1-Score = 0.3214308285407319
	// ===== THRES : 5 =====
	// Precision = 0.33315824031516744
	// Recall = 0.31688733449912565
	// F1-Score = 0.3248191537033481
	// ===== THRES : 4 =====
	// Precision = 0.3514309764309764
	// Recall = 0.31289033225081186
	// F1-Score = 0.33104268534425796
	// ===== THRES : 3 =====
	// Precision = 0.3757378067722895
	// Recall = 0.30214838870846866
	// F1-Score = 0.33494876765438936
	// ===== THRES : 2 =====
	// Precision = 0.4424778761061947
	// Recall = 0.2872845365975518
	// F1-Score = 0.3483792790063617
	// ===== THRES : 1 =====
	// Precision = 0.6842804918577601
	// Recall = 0.2571821134149388
	// F1-Score = 0.3738538356786201
	// ===== THRES : 0 =====
	// Precision = 0.9962940086473132
	// Recall = 0.2014738945790657
	// F1-Score = 0.3351688311688312

	private HashMap<Integer, Integer> ed = new HashMap<Integer, Integer>();

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
						int res = -1, mi = 100;
						String curname = data.getDouban_username(i);
						for (int j = 0; j < data.getSizeWeibo(); j++) {
							int dist = basic.algorithm.StringAlg.EditDistance(
									curname, data.getWeibo_username(j));
							if (dist < mi) {
								res = j;
								mi = dist;
							}
						}
						// System.out.println(i + "\t" + res + "\t" + mi);
						if (res != -1)
							synchronized (result) {
								result.put(i, res);
								ed.put(i, mi);
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

	@Override
	public void evaluate() {
		super.evaluate();
		for (int thres = 10; thres >= 0; thres--) {
			System.out.println(" ===== THRES : " + thres + " =====");
			for (int id : ed.keySet())
				if (ed.get(id) > thres)
					result.remove(id);
			super.evaluate();
		}
	}

}
