package task.usermap;

import java.awt.AlphaComposite;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import basic.Config;
import basic.format.Pair;
import data.DoubanUser;

public class Cai extends UserMapTask {
	private static int TOPN = 100;
	private static int K = 100;
	private static double lambda = 0;
	private double rate = 1e-2;
	private ArrayList<Integer> wid, did;
	private double[][] embed;
	private Random random;
	private ArrayList<Integer[]> pairs;
	private ArrayList<ArrayList<Pair<Integer, Double>>> cands;

	private void learn(int value, int i, int j) {
		double cur = predict(i, j);
		double err = cur - value;
		double g = err * cur * (1 - cur);
		for (int k = 0; k < K; k++) {
			double t = embed[i][k];
			embed[i][k] += rate * (-embed[j][k] * g - lambda * t);
			embed[j][k] += rate * (-t * g - lambda * embed[j][k]);
		}
	}

	private void learn() {
		Collections.shuffle(pairs);
		for (int i = 0; i < pairs.size(); i++) {
			learn(1, pairs.get(i)[0], pairs.get(i)[1]);
			learn(0, random.nextInt(embed.length), random.nextInt(embed.length));
			// learn(0, random.nextInt(embed.length), pairs.get(i)[1]);
		}
	}

	private double predict(int i, int j) {
		return basic.Functions.sigmoid(basic.Vector.dot(embed[i], embed[j]));
	}

	private double getCost() {
		double err = 0, norm = 0;
		for (int i = 0; i < embed.length; i++)
			for (int j = 0; j < K; j++)
				norm += embed[i][j] * embed[i][j];
		for (int i = 0; i < pairs.size(); i++) {
			err += Math.pow(predict(pairs.get(i)[0], pairs.get(i)[1]) - 1, 2);
			err += Math.pow(
					predict(random.nextInt(embed.length),
							random.nextInt(embed.length)), 2);
		}
		System.out.println(err+"\t"+norm);
		return err + norm * lambda;
	}

	public void run() {
		System.out.println("Cai Method Start");
		random = new Random();
		int tcnt = 0;
		wid = new ArrayList<Integer>();
		did = new ArrayList<Integer>();
		for (int i = 0; i < data.getSizeWeibo(); i++)
			wid.add(tcnt++);
		for (int i = 0; i < data.getSizeDouban(); i++)
			if (data.getTrain().containsKey(i))
				did.add(wid.get(data.getTrain().get(i)));
			else
				did.add(tcnt++);
		embed = new double[tcnt][K];

		for (int i = 0; i < tcnt; i++)
			for (int j = 0; j < K; j++)
				embed[i][j] = (random.nextDouble() * 2 - 1) / Math.sqrt(K);
		System.out.println("Initial Finished");

		pairs = new ArrayList<Integer[]>();
		for (int i = 0; i < data.getSizeDouban(); i++)
			for (int j : data.getDouban_friends(i))
				if (i < j){
					pairs.add(new Integer[] { did.get(i), did.get(j) });
//					pairs.add(new Integer[] { wid.get(data.getTruth().get(i)), wid.get(data.getTruth().get(j)) });
				}
		for (int i = 0; i < data.getSizeWeibo(); i++) {
			for (int j : data.getWeibo_friends(i))
				if (i < j)
					pairs.add(new Integer[] { wid.get(i), wid.get(j) });
		}
		System.out.println("Pairs Generated : " + pairs.size());

		double lacost = getCost();
		System.out.println("Initial Cost : " + lacost);
		for (int i = 0; i < 20 && rate > 1e-8; i++) {
			learn();
			double cost = getCost();
			if (cost < lacost)
				rate *= 1.2;
			else
				rate /= 2;
			System.out.println("Iteration " + i + " : Cost = " + cost
					+ "\tRate = " + rate);
			for (int q = 0; q < 10; q++) {
//				 System.out.println(getScore(q, data.getTruth().get(q)));
				// System.out.println(predict(pairs.get(q)[0],pairs.get(q)[1]));
				// System.out.println(predict(pairs.get(q)[0],random.nextInt(embed.length)));
			}
			lacost = cost;
		}

		cands = new ArrayList<ArrayList<Pair<Integer, Double>>>();
		result = new HashMap<Integer, Integer>();

		final LinkedList<Integer> Q = new LinkedList<Integer>();
		for (int i = 0; i < data.getSizeDouban(); i++) {
			cands.add(new ArrayList<Pair<Integer, Double>>());
			if (!data.getTrain().containsKey(i))
				Q.add(i);
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

	};

	private double getScore(int i, int j) {
		i = did.get(i);
		j = wid.get(j);
		// return basic.Functions.sigmoid(basic.Vector.dot(embed[i], embed[j]));
		return basic.Vector.CosineSimilarity(embed[i], embed[j]);
	}

	@Override
	public void evaluate() {
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
			System.out.println("Precision @ " + i + " : " + hit / (tcnt + 0.0));
		}
		super.evaluate();
	}
}
