package task.usermap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import basic.Config;
import basic.DataOps;
import basic.FileOps;
import basic.format.Pair;

public class WeiboTopics extends UserMapTask {
	private static int NTopic = 30;
	private static int NWords = 1000;

	private ArrayList<ArrayList<Integer>> w, z;
	
	private ArrayList<ArrayList<Double>> phi;
	private ArrayList<ArrayList<Double>> theta;
	private ArrayList<Double> alpha;
	HashMap<String, Integer> dict;

	private ArrayList<Double> beta;
	private ArrayList<String> words;

		private Random random;

	private void insertUser(int wid) {
		ArrayList<Integer> curw = new ArrayList<Integer>();
		ArrayList<Integer> curz = new ArrayList<Integer>();
		for (String s : data.getWeibo_weibo(wid))
			for (String seg : s.split("\t"))
				if (dict.containsKey(seg)) {
					curw.add(dict.get(seg));
					curz.add(random.nextInt(NTopic));
				}

		w.add(curw);
		z.add(curz);
	}

	private void initParams() {
		random = new Random();
		alpha = new ArrayList<Double>();
		for (int i = 0; i < NTopic; i++)
			alpha.add(1.0);
		beta = new ArrayList<Double>();
		for (int i = 0; i < NWords; i++)
			beta.add(1.0);

		dict = new HashMap<String, Integer>();
		for (int i = 0; i < data.getSizeWeibo(); i++)
			for (String s : data.getWeibo_weibo(i))
				for (String seg : s.split("\t")) {
					if (dict.containsKey(seg))
						dict.put(seg, dict.get(seg) + 1);
					else
						dict.put(seg, 1);
				}
		for (String c : new LinkedList<String>(dict.keySet()))
			if (dict.get(c) > 50000)
				dict.remove(c);

		LinkedList<Pair<String, Integer>> topwords = basic.DataOps.selectTopN(
				dict, new Comparator<Pair<String, Integer>>() {
					@Override
					public int compare(Pair<String, Integer> o1,
							Pair<String, Integer> o2) {
						return -o1.getSecond().compareTo(o2.getSecond());
					}
				}, NWords);
		dict.clear();
		words = new ArrayList<String>();
		for (Pair<String, Integer> word : topwords) {
			dict.put(word.getFirst(), dict.size());
			words.add(word.getFirst());
		}
		FileOps.SaveList(Config.getValue("WorkDir") + "topwords", topwords);
		
		w = new ArrayList<ArrayList<Integer>>();
		z = new ArrayList<ArrayList<Integer>>();
		
		for (int i=0;i<data.getSizeWeibo();i++)
			insertUser(i);
		System.out.println("Parameters Initialized");
	}

	@Override
	public void run() {
		initParams();

		for (int itr = 0; itr < 100; itr++) {
			System.out.println("Iteration # " + itr);
			updatePhi();
			updateTheta();
			updateZ();
			savePhi();
			saveTheta();
		}
	}

	private void updatePhi() {
		ArrayList<ArrayList<Integer>> wordcnt = new ArrayList<ArrayList<Integer>>();
		ArrayList<Integer> topiccnt = new ArrayList<Integer>();
		for (int i = 0; i < NTopic; i++) {
			topiccnt.add(0);
			ArrayList<Integer> curcnt = new ArrayList<Integer>();
			for (int q = 0; q < NWords; q++)
				curcnt.add(0);
			wordcnt.add(curcnt);
		}
		for (int i = 0; i < w.size(); i++)
				for (int j = 0; j < w.get(i).size(); j++) {
					int tw = w.get(i).get(j);
					int tz = z.get(i).get(j);
					topiccnt.set(tz, topiccnt.get(tz) + 1);
					wordcnt.get(tz).set(tw, wordcnt.get(tz).get(tw) + 1);
				}
		phi = new ArrayList<ArrayList<Double>>();
		for (int i = 0; i < NTopic; i++) {
			ArrayList<Double> phi_i = new ArrayList<Double>();
			double s = DataOps.sum(beta);
			for (int q = 0; q < NWords; q++)
				phi_i.add((wordcnt.get(i).get(q) + beta.get(q))
						/ (topiccnt.get(i) + s));
			phi.add(phi_i);
		}
	}


	private void updateTheta() {
		theta = new ArrayList<ArrayList<Double>>();
		for (int i = 0; i < w.size(); i++) {
			ArrayList<Double> curtheta = new ArrayList<Double>(alpha);
			for (int tz : z.get(i))
				curtheta.set(tz, curtheta.get(tz) + 1);
			double s = DataOps.sum(curtheta);
			for (int q = 0; q < curtheta.size(); q++)
				curtheta.set(q, curtheta.get(q) / s);
			theta.add(curtheta);
		}
	}

	private void updateZ() {
		final LinkedList<Integer> Q = new LinkedList<Integer>();
		for (int i = 0; i < w.size(); i++)
			Q.add(i);

		Thread[] workers = new Thread[Integer.valueOf(Config
				.getValue("#Thread"))];
		for (int i = 0; i < workers.length; i++) {
			workers[i] = new Thread() {
				@Override
				public void run() {
					for (;;) {
						int i;
						synchronized (Q) {
							if (Q.isEmpty())
								return;
							i = Q.removeFirst();
						}
						for (int j = 0; j < z.get(i).size(); j++) {
							int tw = w.get(i).get(j);
							ArrayList<Double> posi = new ArrayList<Double>();
							for (int q = 0; q < NTopic; q++)
								posi.add(phi.get(q).get(tw)
										* theta.get(i).get(q));
							double s = DataOps.sum(posi);
							for (int q = 0; q < NTopic; q++)
								posi.set(q, posi.get(q) / s);
							double t = random.nextDouble();
							int res = 0;
							for (; res < NTopic; res++) {
								t -= posi.get(res);
								if (t <= 0)
									break;
							}
							z.get(i).set(j, res);
						}
					}
				}
			};
			workers[i].start();
		}
		for (Thread worker : workers)
			try {
				worker.join();
			} catch (Exception e) {
			}
	}

	private void savePhi() {
		LinkedList<String> outdata = new LinkedList<String>();
		for (int ti = 0; ti < NTopic; ti++) {
			outdata.add("Topic # " + ti + ":");
			ArrayList<Double> phi_i = phi.get(ti);
			LinkedList<Pair<String, Double>> tmp = new LinkedList<Pair<String, Double>>();
			for (int i = 0; i < NWords; i++)
				tmp.add(new Pair<String, Double>(words.get(i), phi_i.get(i)));
			Collections.sort(tmp, new Comparator<Pair<String, Double>>() {
				@Override
				public int compare(Pair<String, Double> o1,
						Pair<String, Double> o2) {
					return -o1.getSecond().compareTo(o2.getSecond());
				}
			});
			StringBuilder sb = new StringBuilder();
			StringBuilder sbf = new StringBuilder();
			for (int q = 0; q < 100; q++)
				sb.append(tmp.get(q).getFirst() + "\t");
			outdata.add(sb.toString());
		}
		FileOps.SaveFile(Config.getValue("WorkDir") + "weibo.topics.phi", outdata);
	}
	
	private void saveTheta(){
		LinkedList<String> outdata=new LinkedList<String>();
		for (ArrayList<Double> cur:theta){
			StringBuilder sb=new StringBuilder();
			for (Double v:cur)
				sb.append(v+"\t");
			outdata.add(sb.toString());
		}
		FileOps.SaveFile(Config.getValue("WorkDir")+"weibo.topics.theta", outdata);
	}

	private double getSimilarity(int i, int j) {
		return basic.Vector.dot(theta.get(i), theta.get(j));
	}

	@Override
	public void evaluate() {
	}
}
