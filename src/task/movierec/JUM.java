package task.movierec;

import java.text.Format;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;

import javax.swing.plaf.multi.MultiButtonUI;

import basic.Config;
import basic.DataOps;
import basic.FileOps;
import basic.Formats;
import basic.Vector;
import basic.algorithm.MatrixFactorization;
import basic.format.Pair;

public class JUM extends RankingTask {
	private MatrixFactorization model;

	private static int NTopic = 20;
	private static int NWords = 1000;

	private ArrayList<ArrayList<Integer>> w, z;

	private ArrayList<ArrayList<Double>> phi;
	private ArrayList<ArrayList<Double>> theta;
	private ArrayList<Double> alpha;
	HashMap<String, Integer> dict;

	private ArrayList<Double> beta;
	private ArrayList<String> words;

	Random random;

	public JUM() {
		random = new Random(0);
	}

	@Override
	public void train() {

		// trainLDA(1);
		theta = new ArrayList<ArrayList<Double>>();
		for (String line : FileOps.LoadFilebyLine(Config.getValue("WorkDir")
				+ "jum.theta"))
			theta.add(Formats.doubleArrayLoader(line, "\t"));

		model = new MatrixFactorization(data.getSizeDouban(),
				data.getSizeMovie(), 10, 50, 1e-2, 1e-1,
				MatrixFactorization.SIGMOID);

//		showSimAnalyze_Jacc();

		for (int i = 0; i < data.getSizeDouban(); i++) {
			ArrayList<Double> topics = theta.get(data.getLinks().get(i));
			for (int j = 0; j < topics.size(); j++)
				model.addUserFeature(i, j, topics.get(j) * 10);
		}

		for (int i = 0; i < data.getSizeDouban(); i++)
			for (int item : getTrain().get(i)) {
				model.addTrain(i, item, 1);
				model.addTrain(i, random.nextInt(data.getSizeMovie()), 0);
			}
		for (int i = 0; i < 100; i++) {
			model.train(10, false);
			evaluate();
			saveModel();
			showSimAnalyze();
		}
	}

	private void saveModel() {
		LinkedList<String> outdata = new LinkedList<String>();
		for (int i = 0; i < data.getSizeDouban(); i++) {
			double[] vec = model.getEmbedding_User(i);
			StringBuilder sb = new StringBuilder();
			for (double v : vec)
				sb.append(v + "\t");
			outdata.add(sb.toString());
		}
		FileOps.SaveFile(Config.getValue("WorkDir") + "mf.model", outdata);

		outdata = new LinkedList<String>();
		for (int i = 0; i < data.getSizeDouban(); i++) {
			Double[] vec = (Double[]) (theta.get(data.getLinks().get(i))
					.toArray(new Double[0]));
			StringBuilder sb = new StringBuilder();
			for (double v : vec)
				sb.append(v + "\t");
			outdata.add(sb.toString());
		}
		FileOps.SaveFile(Config.getValue("WorkDir") + "topics.model", outdata);
	}

	private void showSimAnalyze() {
		LinkedList<String> lda = new LinkedList<String>();
		LinkedList<String> mf = new LinkedList<String>();
		for (int i : data.getLinks().keySet())
			for (int j : data.getLinks().keySet())
				if (i < j && random.nextDouble() < 0.01 && lda.size() < 100000) {
					double sim11 = Vector.CosineSimilarity(
							model.getEmbedding_User(i),
							model.getEmbedding_User(j));
					double sim12 = Vector.dot(model.getEmbedding_User(i),
							model.getEmbedding_User(j));
					double sim13 = Vector.dist(model.getEmbedding_User(i),
							model.getEmbedding_User(j));
					mf.add(sim11 + "\t" + sim12 + "\t" + sim13);
					double sim21 = Vector.CosineSimilarity(
							theta.get(data.getLinks().get(i)),
							theta.get(data.getLinks().get(j)));
					double sim22 = Vector.dot(
							theta.get(data.getLinks().get(i)),
							theta.get(data.getLinks().get(j)));
					double sim23 = Vector.dist(
							theta.get(data.getLinks().get(i)),
							theta.get(data.getLinks().get(j)));
					lda.add(sim21 + "\t" + sim22 + "\t" + sim23);
				}
		FileOps.SaveFile(Config.getValue("WorkDir") + "lda.usersim", lda);
		FileOps.SaveFile(Config.getValue("WorkDir") + "mf.usersim", mf);
	}

	private void showSimAnalyze_Jacc() {
		LinkedList<String> outdata = new LinkedList<String>();
		for (int q = 0; q < 1000000; q++) {
			int i = random.nextInt(data.getSizeDouban());
			int j = random.nextInt(data.getSizeDouban());
			double lda = Vector.CosineSimilarity(
					theta.get(data.getLinks().get(i)),
					theta.get(data.getLinks().get(j)));
			HashSet<Integer> mi = data.getDouban_usermovie_set(i);
			HashSet<Integer> mj = data.getDouban_usermovie_set(j);
			int si = mi.size();
			mi.retainAll(mj);
			
			double jacc = mi.size() / (si + mj.size() - mi.size() + 0.0);
			outdata.add(lda + "\t" + jacc);
		}
		FileOps.SaveFile(Config.getValue("WorkDir") + "usersim", outdata);
	}

	private void trainLDA(int round) {
		initLDA();
		for (int i = 0; i < round; i++) {
			System.out.println("LDA Round #" + i);
			updatePhi();
			updateTheta();
			updateZ();
			savePhi();
		}
	}

	@Override
	public double predict(int uid, int mid) {
		return model.predict(uid, mid);
	}

	private void initLDA() {
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
		for (int i = 0; i < data.getSizeWeibo(); i++) {
			ArrayList<Integer> curw = new ArrayList<Integer>();
			ArrayList<Integer> curz = new ArrayList<Integer>();
			for (String s : data.getWeibo_weibo(i))
				for (String seg : s.split("\t"))
					if (dict.containsKey(seg)) {
						curw.add(dict.get(seg));
						curz.add(random.nextInt(NTopic));
					}
			w.add(curw);
			z.add(curz);
		}
		System.out.println("LDA Parameters Initialized");
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
			for (int q = 0; q < 100; q++)
				sb.append(tmp.get(q).getFirst() + "\t");
			outdata.add(sb.toString());
		}
		FileOps.SaveFile(Config.getValue("WorkDir") + "topic_phi", outdata);
	}
}
