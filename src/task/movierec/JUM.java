package task.movierec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import javax.management.loading.MLet;

import basic.Config;
import basic.DataOps;
import basic.FileOps;
import basic.Functions;
import basic.Matrix;
import basic.RandomOps;
import basic.Vector;
import basic.format.Pair;

public class JUM extends RankingTask {

	private static int NTopic = 20;
	private static int NWords = 1000;
	private static int KG = 20;
	private static int KM = 10;

	private ArrayList<ArrayList<Integer>> w, z;

	private double[][] phi;
	private double[][] theta;
	private double[][] pref;
	private double[][] U;
	private double[][] movies;
	private double[] mbias;
	HashMap<String, Integer> dict;

	private ArrayList<Double> beta;
	private double var_Movie = 1;
	private double var_Pref = 0.1;
	private double var_U = 1;
	private double var_Trans = 1;

	private double topicPriorMulti = 5;

	private boolean linkDouban = true;
	private boolean linkWeibo = true;

	private double[][] transWeibo, transDouban;

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
		phi = new double[NTopic][NWords];
		for (int i = 0; i < NTopic; i++) {
			double s = DataOps.sum(beta);
			for (int q = 0; q < NWords; q++)
				phi[i][q] = (wordcnt.get(i).get(q) + beta.get(q))
						/ (topiccnt.get(i) + s);
		}
	}

	private void updateTheta() {
		theta = new double[w.size()][NTopic];
		double[][] theta_mean = Matrix.matmult(U, transWeibo);
		for (int i = 0; i < w.size(); i++) {
			for (int j = 0; j < NTopic; j++)
				theta[i][j] = theta_mean[i][j] * topicPriorMulti;

			for (int tz : z.get(i))
				theta[i][tz]++;
			double s = DataOps.sum(theta[i]);
			for (int q = 0; q < NTopic; q++)
				theta[i][q] /= s;
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
								posi.add(phi[q][tw] * theta[i][q]);
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
			LinkedList<Pair<String, Double>> tmp = new LinkedList<Pair<String, Double>>();
			for (int i = 0; i < NWords; i++)
				tmp.add(new Pair<String, Double>(words.get(i), phi[ti][i]));
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
		FileOps.SaveFile(Config.getValue("WorkDir") + "jum.phi", outdata);
	}

	private void initTopicModel() {
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

		for (int i = 0; i < data.getSizeWeibo(); i++)
			insertUser(i);

		transWeibo = new double[KG][NTopic];
		for (int i = 0; i < KG; i++)
			for (int j = 0; j < NTopic; j++)
				transWeibo[i][j] = RandomOps.genNormal(0, var_Trans);
		System.out.println("Topic Model Parameters Initialized");
	}

	private void updateU() {
		final LinkedList<Integer> Q = new LinkedList<Integer>();
		for (int i = 0; i < data.getSizeDouban(); i++)
			Q.add(i);
		Thread[] workers = new Thread[Integer.valueOf(Config
				.getValue("#Thread"))];
		for (int i = 0; i < workers.length; i++) {
			workers[i] = new Thread() {
				@Override
				public void run() {
					int id;
					for (;;) {
						synchronized (Q) {
							if (Q.isEmpty())
								break;
							id = Q.removeFirst();
						}
						double[] curU = U[id];
						double[] curP = pref[id];

						for (int q = 0; q < 10; q++) {
							for (int i = 0; i < KG; i++) {
								double left = 0, right = 0;
								if (linkDouban) {
									left += 1;
									double multi = var_U / var_Pref;
									for (int j = 0; j < KM; j++) {

										right += transDouban[i][j] * curP[j]
												* multi;
										for (int k = 0; k < KG; k++)
											if (k != i)
												right -= transDouban[i][j]
														* curU[k]
														* transDouban[k][j]
														* multi;
										left += transDouban[i][j]
												* transDouban[i][j] * multi;
									}
								}

								if (linkWeibo) {
									double[] curTheta = theta[id];
									left += 1;
									double multi = var_U / var_Pref;
									for (int j = 0; j < NTopic; j++) {

										right += transWeibo[i][j] * curTheta[j]
												* multi;
										for (int k = 0; k < KG; k++)
											if (k != i)
												right -= transWeibo[i][j]
														* curU[k]
														* transWeibo[k][j]
														* multi;
										left += transWeibo[i][j]
												* transWeibo[i][j] * multi;
									}
								}
								double w = 0;
								curU[i] = (curU[i] * w + right / left)
										/ (1.0 + w);
							}

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

	private void updateMF() {
		LinkedList<Integer[]> train = new LinkedList<Integer[]>();
		for (int i = 0; i < data.getSizeDouban(); i++)
			for (int m : getTrain().get(i)) {
				train.add(new Integer[] { i, m, 1 });
				train.add(new Integer[] { i,
						random.nextInt(data.getSizeMovie()), 0 });
			}
		Collections.shuffle(train, random);

		double rate = 1e-2;

		double mse = 0;

		double[][] pref_mean = Matrix.matmult(U, transDouban);

		for (Integer[] entry : train) {
			int uid = entry[0];
			int mid = entry[1];
			int v = entry[2];
			double pred = predict(uid, mid);

			// System.out.println(pred + "\t" + v);
			StringBuilder sb = new StringBuilder();
			for (double q : pref[uid])
				sb.append(q + "\t");
			// System.out.println(sb.toString());
			double err = pred - v;
			mse += err * err;
			double g = 2 * err * pred * (1 - pred);

			for (int k = 0; k < KM; k++) {
				double uk = pref[uid][k];
				if (linkDouban)
					pref[uid][k] -= rate
							* (g * movies[mid][k] + (pref[uid][k] - pref_mean[uid][k])
									/ var_Pref / getTrain().get(uid).size());
				else
					pref[uid][k] -= rate
							* (g * movies[mid][k] + (pref[uid][k]) / var_Pref
									/ getTrain().get(uid).size());
				movies[mid][k] -= rate
						* (g * uk + movies[mid][k] / var_Movie
								/ data.getSizeDouban() * 10);
			}
			mbias[mid] -= rate * g;
		}
		System.out.println(train.size() + "\t" + mse + "\t" + mse
				/ train.size());
	}

	private void updateTransWeibo() {
		for (int r = 0; r < 10; r++) {
			final double rate = 1e-4;
			Thread[] workers = new Thread[NTopic];
			for (int i = 0; i < workers.length; i++) {
				final int workerId = i;
				workers[i] = new Thread() {
					public void run() {
						for (int z = 0; z < 10; z++) {
							double[] trans = new double[KG];
							for (int i = 0; i < KG; i++)
								trans[i] = transWeibo[i][workerId];
							for (int q = 0; q < data.getSizeDouban(); q++) {
								int i = random.nextInt(data.getSizeDouban());
								double res = Vector.dot(U[i], trans);
								double g = res - theta[i][workerId];
								for (int j = 0; j < KG; j++)
									trans[j] -= rate
											* (g * U[i][j] + trans[j]
													/ data.getSizeDouban()
													/ var_Trans);
							}
							for (int i = 0; i < KG; i++)
								transWeibo[i][workerId] = trans[i];
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

	private void updateTransDouban() {
		for (int r = 0; r < 10; r++) {
			final double rate = 1e-2;
			Thread[] workers = new Thread[KM];
			for (int i = 0; i < workers.length; i++) {
				final int workerId = i;
				workers[i] = new Thread() {
					public void run() {
						for (int z = 0; z < 10; z++) {
							double[] trans = new double[KG];
							for (int i = 0; i < KG; i++)
								trans[i] = transDouban[i][workerId];
							for (int q = 0; q < data.getSizeDouban(); q++) {
								int i = random.nextInt(data.getSizeDouban());
								double res = Vector.dot(U[i], trans);
								double g = res - pref[i][workerId];
								for (int j = 0; j < KG; j++)
									trans[j] -= rate
											* (g * U[i][j] + trans[j]
													/ data.getSizeDouban()
													/ var_Trans);
							}
							for (int i = 0; i < KG; i++)
								transDouban[i][workerId] = trans[i];
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

	private void initMF() {
		movies = new double[data.getSizeMovie()][KM];
		for (int i = 0; i < data.getSizeMovie(); i++)
			for (int j = 0; j < KM; j++)
				movies[i][j] = RandomOps.genNormal(0, var_Movie);
		mbias = new double[data.getSizeMovie()];
		for (int i = 0; i < data.getSizeMovie(); i++)
			mbias[i] = RandomOps.genNormal(0, var_Movie);

		pref = new double[data.getSizeDouban()][KM];
		for (int i = 0; i < data.getSizeDouban(); i++)
			for (int j = 0; j < KM; j++)
				pref[i][j] = RandomOps.genNormal(0, var_Pref);
		transDouban = new double[KG][KM];
		for (int i = 0; i < KG; i++)
			for (int j = 0; j < KM; j++)
				transDouban[i][j] = RandomOps.genNormal(0, var_Trans);
	}

	private void saveModel() {
		FileOps.SaveFile(Config.getValue("WorkDir") + "jum.U", U);
		FileOps.SaveFile(Config.getValue("WorkDir") + "jum.TransDouban",
				transDouban);
		FileOps.SaveFile(Config.getValue("WorkDir") + "jum.TransWeibo",
				transWeibo);
		FileOps.SaveFile(Config.getValue("WorkDir") + "jum.Pref", pref);
		FileOps.SaveFile(Config.getValue("WorkDir") + "jum.Movies", movies);
		FileOps.SaveFile(Config.getValue("WorkDir") + "jum.Pref.Mean",
				Matrix.matmult(U, transDouban));
		FileOps.SaveFile(Config.getValue("WorkDir") + "jum.theta", theta);
		savePhi();

	}

	private void analyzeModel() {
		{
			LinkedList<Double> userLen = new LinkedList<Double>();
			for (int i = 0; i < U.length; i++)
				userLen.add(Vector.norm(U[i]));
			System.out.println("Average U-Norm : " + DataOps.average(userLen));
		}

		{
			double[][] pref_mean = Matrix.matmult(U, transDouban);
			double err = 0;
			LinkedList<Double> prefMeanNorm = new LinkedList<Double>();
			LinkedList<Double> prefNorm = new LinkedList<Double>();
			for (int i = 0; i < data.getSizeDouban(); i++) {
				prefMeanNorm.add(Vector.norm(pref_mean[i]));
				prefNorm.add(Vector.norm(pref[i]));
				for (int j = 0; j < KM; j++)
					err += Math.abs(pref_mean[i][j] - pref[i][j]);
			}
			System.out.println("Average Pref-Mean-Norm : "
					+ DataOps.average(prefMeanNorm));
			System.out.println("Average Pref-Norm : "
					+ DataOps.average(prefNorm));
			System.out.println("Pref Distance : " + err + "\t" + err
					/ data.getSizeDouban() / KM);
		}
		{
			double[][] theta_mean = Matrix.matmult(U, transWeibo);
			double err = 0;
			LinkedList<Double> thetaMeanNorm = new LinkedList<Double>();
			LinkedList<Double> thetaNorm = new LinkedList<Double>();
			for (int i = 0; i < data.getSizeDouban(); i++) {
				thetaMeanNorm.add(Vector.norm(theta_mean[i]));
				thetaNorm.add(Vector.norm(pref[i]));
				for (int j = 0; j < NTopic; j++)
					err += Math.abs(theta_mean[i][j] - theta[i][j]);
			}
			System.out.println("Average Theta-Mean-Norm : "
					+ DataOps.average(thetaMeanNorm));
			System.out.println("Average Theta-Norm : "
					+ DataOps.average(thetaNorm));
			System.out.println("Theta Distance : " + err + "\t" + err
					/ data.getSizeDouban() / NTopic);

		}
	}

	@Override
	public void train() {
		random = new Random(0);

		U = new double[data.getSizeDouban()][KG];
		for (int i = 0; i < data.getSizeDouban(); i++)
			for (int j = 0; j < KG; j++)
				U[i][j] = RandomOps.genNormal(0, var_U);

		if (linkWeibo)
			initTopicModel();
		initMF();
		for (int i = 0; i < 1000; i++) {
			if (linkWeibo) {
				updatePhi();
				updateTheta();
				updateZ();
			}
			updateMF();

			if (linkWeibo) {
				updateTransWeibo();
			}

			if (linkDouban) {
				updateTransDouban();
			}

			if (linkDouban || linkWeibo)
				updateU();

			if (i % 10 == 0) {
				System.out.println("Current Iteration : # " + i);
				evaluate();
			}

			// analyzeModel();
			// saveModel();
		}
	}

	@Override
	public double predict(int uid, int mid) {
		return Functions.sigmoid(Vector.dot(pref[uid], movies[mid])
				+ mbias[mid])
				+ random.nextDouble() * 1e-8;
	}

}
