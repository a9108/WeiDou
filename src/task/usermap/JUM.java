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

public class JUM extends UserMapTask {
	private static int TOPN = 100;
	private ArrayList<ArrayList<Pair<Integer, Double>>> cands;

	private static int NTopic = 20;
	private static int NWords = 1000;
	private static int NMovie;

	private ArrayList<ArrayList<Integer>> w, z;
	private ArrayList<ArrayList<Integer>> w_M, z_M;
	private ArrayList<Integer> wid, did;
	private HashMap<Integer, Integer> wrid, drid;

	private ArrayList<ArrayList<Double>> phi;
	private ArrayList<ArrayList<Double>> phi_M;
	private ArrayList<ArrayList<Double>> theta;
	private ArrayList<Double> alpha;
	HashMap<String, Integer> dict;

	private ArrayList<Double> beta;
	private ArrayList<String> words;

	private ArrayList<Double> beta_M;
	private ArrayList<String> words_M;
	private Random random;

	private void insertUser(int did, int wid) {
		ArrayList<Integer> curw = new ArrayList<Integer>();
		ArrayList<Integer> curz = new ArrayList<Integer>();
		if (wid != -1)
			for (String s : data.getWeibo_weibo(wid)){
				for (String seg : s.split("\t"))
					if (dict.containsKey(seg)) {
						curw.add(dict.get(seg));
						curz.add(random.nextInt(NTopic));
					}
			}

		ArrayList<Integer> curw_M = new ArrayList<Integer>();
		ArrayList<Integer> curz_M = new ArrayList<Integer>();
		if (did != -1)
			for (Integer[] mid : data.getDouban_usermovie(did)) {
				curw_M.add(mid[0]);
				curz_M.add(random.nextInt(NTopic));
			}
		w.add(curw);
		z.add(curz);
		w_M.add(curw_M);
		z_M.add(curz_M);
		if (wid != -1)
			wrid.put(wid, this.wid.size());
		if (did != -1)
			drid.put(did, this.did.size());
		this.wid.add(wid);
		this.did.add(did);

	}

	private void initParams() {
		wrid = new HashMap<Integer, Integer>();
		drid = new HashMap<Integer, Integer>();

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

		NMovie = data.getSizeMovie();
		words_M = new ArrayList<String>();
		beta_M = new ArrayList<Double>();
		for (int i = 0; i < NMovie; i++) {
			beta_M.add(1.0);
			words_M.add(data.getMovieName(i));
		}

		w = new ArrayList<ArrayList<Integer>>();
		z = new ArrayList<ArrayList<Integer>>();
		w_M = new ArrayList<ArrayList<Integer>>();
		z_M = new ArrayList<ArrayList<Integer>>();
		wid = new ArrayList<Integer>();
		did = new ArrayList<Integer>();

		for (int i : data.getTruth().keySet())
			insertUser(i, data.getTruth().get(i));
		// for (int i = 0; i < data.getSizeDouban(); i++)
		// insertUser(i, -1);
		// for (int i = 0; i < data.getSizeWeibo(); i++)
		// insertUser(-1, i);
		System.out.println("Parameters Initialized");
	}

	@Override
	public void run() {
		initParams();

		for (int itr = 0; itr < 100; itr++) {
			System.out.println("Iteration # " + itr);
			updatePhi();
			updatePhi_M();
			updateTheta();
			updateZ();
			updateZ_M();
			if (itr % 10 == 0) {
				// evaluate();
				savePhi();
				saveTheta();
			}
		}

		savePhi();
		saveTheta();
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
			if (wid.get(i) != -1 && did.get(i) != -1)
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

	private void updatePhi_M() {
		ArrayList<ArrayList<Integer>> wordcnt = new ArrayList<ArrayList<Integer>>();
		ArrayList<Integer> topiccnt = new ArrayList<Integer>();
		for (int i = 0; i < NTopic; i++) {
			topiccnt.add(0);
			ArrayList<Integer> curcnt = new ArrayList<Integer>();
			for (int q = 0; q < NMovie; q++)
				curcnt.add(0);
			wordcnt.add(curcnt);
		}
		for (int i = 0; i < w.size(); i++)
			if (wid.get(i) != -1 && did.get(i) != -1)
				for (int j = 0; j < w_M.get(i).size(); j++) {
					int tw = w_M.get(i).get(j);
					int tz = z_M.get(i).get(j);
					topiccnt.set(tz, topiccnt.get(tz) + 1);
					wordcnt.get(tz).set(tw, wordcnt.get(tz).get(tw) + 1);
				}
		phi_M = new ArrayList<ArrayList<Double>>();
		for (int i = 0; i < NTopic; i++) {
			ArrayList<Double> phi_i = new ArrayList<Double>();
			double s = DataOps.sum(beta_M);
			for (int q = 0; q < NMovie; q++)
				phi_i.add((wordcnt.get(i).get(q) + beta_M.get(q))
						/ (topiccnt.get(i) + s));
			phi_M.add(phi_i);
		}
	}

	private void updateTheta() {
		theta = new ArrayList<ArrayList<Double>>();
		for (int i = 0; i < w.size(); i++) {
			ArrayList<Double> curtheta = new ArrayList<Double>(alpha);
			for (int tz : z.get(i))
				curtheta.set(tz, curtheta.get(tz) + 1);
			for (int tz : z_M.get(i))
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

	private void updateZ_M() {
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
						for (int j = 0; j < z_M.get(i).size(); j++) {
							int tw = w_M.get(i).get(j);
							ArrayList<Double> posi = new ArrayList<Double>();
							for (int q = 0; q < NTopic; q++)
								posi.add(phi_M.get(q).get(tw)
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
							z_M.get(i).set(j, res);
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

	private void saveTheta() {
		LinkedList<String> outdata = new LinkedList<String>();
		for (ArrayList<Double> cur : theta) {
			StringBuilder sb = new StringBuilder();
			for (Double v : cur)
				sb.append(v + "\t");
			outdata.add(sb.toString());
		}
		FileOps.SaveFile(Config.getValue("WorkDir") + "jum.theta", outdata);
	}

	private void savePhi() {
		LinkedList<String> outdata = new LinkedList<String>();
		LinkedList<String> fulldata = new LinkedList<String>();
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
			for (int q = 0; q < 100; q++)
				sb.append(tmp.get(q).getFirst() + "\t");
			StringBuilder sbf = new StringBuilder();
			for (Pair<String, Double> word : tmp)
				sbf.append(word.getFirst() + ":" + word.getSecond() + "\t");
			outdata.add(sb.toString());
			fulldata.add(sbf.toString());
			phi_i = phi_M.get(ti);
			tmp = new LinkedList<Pair<String, Double>>();
			for (int i = 0; i < NMovie; i++)
				tmp.add(new Pair<String, Double>(words_M.get(i), phi_i.get(i)));
			Collections.sort(tmp, new Comparator<Pair<String, Double>>() {
				@Override
				public int compare(Pair<String, Double> o1,
						Pair<String, Double> o2) {
					return -o1.getSecond().compareTo(o2.getSecond());
				}
			});
			sb = new StringBuilder();
			sbf = new StringBuilder();
			for (int q = 0; q < 100; q++)
				sb.append(tmp.get(q).getFirst() + "\t");
			for (Pair<String, Double> word : tmp)
				sbf.append(word.getFirst() + ":" + word.getSecond() + "\t");
			outdata.add(sb.toString());
			fulldata.add(sbf.toString());

		}
		FileOps.SaveFile(Config.getValue("WorkDir") + "jum.topic_phi", outdata);
		FileOps.SaveFile(Config.getValue("WorkDir") + "jum.topic_phi.full",
				fulldata);
	}

	private double getSimilarity(int i, int j) {
		ArrayList<Double> ctheta;
		ArrayList<Integer> cw = w.get(j);
		ArrayList<Integer> cw_M = w_M.get(i);
		ArrayList<Integer> cz = z.get(j);
		ArrayList<Integer> cz_M = z_M.get(i);
		double res = 0;
		for (int q = 0; q < 1; q++) {
			res = 0;
			ctheta = new ArrayList<Double>(alpha);
			for (int z : cz)
				ctheta.set(z, ctheta.get(z) + 1);
			for (int z : cz_M)
				ctheta.set(z, ctheta.get(z) + 1);
			double s = DataOps.sum(ctheta);
			for (int k = 0; k < NTopic; k++)
				ctheta.set(k, ctheta.get(k) / s);

			for (int k = 0; k < cw.size(); k++) {
				int curw = cw.get(k);
				int curz = cz.get(k);
				res += Math.log(ctheta.get(curz) * phi.get(curz).get(curw));
			}
			for (int k = 0; k < cw_M.size(); k++) {
				int curw = cw_M.get(k);
				int curz = cz_M.get(k);
				res += Math.log(ctheta.get(curz) * phi_M.get(curz).get(curw));
			}
			res /= Math.pow(cw.size() + cw_M.size(), 1);
		}
		// System.out.println(res);
		return res;
		// return basic.Vector.dot(theta.get(i), theta.get(j));
	}

	public double getScore(int i, int j) {
		return getSimilarity(drid.get(i), wrid.get(j));
	}

	@Override
	public void evaluate() {

		for (int i = 0; i < 10; i++) {
			int rid = random.nextInt(data.getSizeWeibo());
			// System.out.println(getScore(i,
			// data.getTruth().get(i))+"\t"+getScore(i, rid));
			int did = drid.get(i), wid = wrid.get(data.getTruth().get(i));
			// System.out.println(did+"\t"+theta.get(did));
			// System.out.println(wid+"\t"+theta.get(wid));
			// System.out.println(rid+"\t"+theta.get(wrid.get(rid)));
		}

		result = new HashMap<Integer, Integer>();
		final LinkedList<Integer> Q = new LinkedList<Integer>();
		cands = new ArrayList<ArrayList<Pair<Integer, Double>>>();
		for (int i = 0; i < data.getSizeDouban(); i++) {
			if (!data.getTrain().containsKey(i))
				Q.add(i);
			cands.add(new ArrayList<Pair<Integer, Double>>());
		}

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

						ArrayList<Pair<Integer, Double>> cur = new ArrayList<Pair<Integer, Double>>();
						for (int j = 0; j < data.getSizeWeibo(); j++)
							cur.add(new Pair<Integer, Double>(j, getScore(i, j)));
						Collections.sort(cur,
								new Comparator<Pair<Integer, Double>>() {
									@Override
									public int compare(
											Pair<Integer, Double> o1,
											Pair<Integer, Double> o2) {
										return -o1.getSecond().compareTo(
												o2.getSecond());
									}
								});

						int pos = -1;
						int truth = data.getTruth().get(i);
						for (int j = 0; j < data.getSizeWeibo(); j++)
							if (cur.get(j).getFirst() == truth)
								pos = j;

						int res = -1;
						String dname = data.getDouban_username(i);
						for (int j = 0; res == -1
								&& j < data.getSizeWeibo() / 2; j++) {
							int k = cur.get(j).getFirst();
							if (dname.equals(data.getWeibo_username(k)))
								res = k;
						}
						for (int j = 0; res == -1
								&& j < data.getSizeWeibo() / 4; j++) {
							int k = cur.get(j).getFirst();
							if (dname.contains(data.getWeibo_username(k))
									|| data.getWeibo_username(k)
											.contains(dname))
								res = k;
						}
						for (int j = 0; res == -1
								&& j < data.getSizeWeibo() / 50; j++) {
							int k = cur.get(j).getFirst();
							if (basic.algorithm.StringAlg.EditDistance(dname,
									data.getWeibo_username(k)) < 4)
								res = k;
						}
						if (res == -1)
							res = cur.get(0).getFirst();
						System.out.println(i + "\t"
								+ getScore(i, data.getTruth().get(i)) + "\t"
								+ pos + "\t" + data.getTruth().get(i) + "\t"
								+ res + "\t" + cur.get(0).getSecond());

						ArrayList<Pair<Integer, Double>> tcur = new ArrayList<Pair<Integer, Double>>();
						for (int q = 0; q < TOPN; q++)
							tcur.add(cur.get(q));
						synchronized (cands) {
							cands.set(i, cur);
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

		for (int i = 1; i <= TOPN; i++) {
			int tcnt = 0, hit = 0;
			for (int j = 0; j < data.getSizeDouban(); j++)
				if (!data.getTrain().containsKey(j)) {
					tcnt++;
					boolean f = false;
					int truth = data.getTruth().get(j);
					for (int q = 0; q < i; q++)
						if (cands.get(j).get(q).getFirst() == truth)
							f = true;
					if (f)
						hit++;
				}
		}
		super.evaluate();
	}
}
