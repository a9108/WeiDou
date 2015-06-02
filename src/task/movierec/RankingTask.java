package task.movierec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;

import basic.DataOps;
import basic.format.Pair;

public abstract class RankingTask {
	public static DataSet data;
	public static String expname="0.9, JUM";

	public abstract void train();

	public abstract double predict(int uid, int mid);

	private ArrayList<LinkedList<Integer>> train, test;

	public void genTrain(double ratio) {
		Random random = new Random(1);
		train = new ArrayList<LinkedList<Integer>>();
		test = new ArrayList<LinkedList<Integer>>();

		for (int i = 0; i < data.getSizeDouban(); i++) {

			int select = random.nextInt(100);
			if (random.nextDouble()<0.3)
				select=random.nextInt(10);
			// select = 100;

			double curR = select
					/ (0.0 + data.getDouban_usermovie_set(i).size());// random.nextDouble()<0.3?.8:ratio;
			// if (random.nextDouble() < ratio)
			// curR = 0;
//			curR = 0.3;
//			curR=0.4;
//			curR*=0.4;
//			curR=0.9;
			if (random.nextDouble()<0.3) curR=2;
			
			LinkedList<Integer> curTest = new LinkedList<Integer>();
			LinkedList<Integer> curTrain = new LinkedList<Integer>();

			for (int item : data.getDouban_usermovie_set(i)) {
				if (random.nextDouble() < curR)// && curTrain.size() < select)
					curTrain.add(item);
				else
					curTest.add(item);
			}
			// System.out.println(curTrain.size()+"\t"+curTest.size());
			train.add(curTrain);
			test.add(curTest);
		}
	}

	public ArrayList<LinkedList<Integer>> getTrain() {
		return train;
	}

	public ArrayList<LinkedList<Integer>> getTest() {
		return test;
	}

	private LinkedList<Pair<Integer, Double>> getRank(int id,
			Collection<Integer> cands) {
		LinkedList<Pair<Integer, Double>> res = new LinkedList<Pair<Integer, Double>>();
		for (int item : cands)
			res.add(new Pair<Integer, Double>(item, predict(id, item)));
		Collections.sort(res, new Comparator<Pair<Integer, Double>>() {
			public int compare(Pair<Integer, Double> o1,
					Pair<Integer, Double> o2) {
				return -o1.getSecond().compareTo(o2.getSecond());
			};
		});
		return res;
	}

	private double calcMAP(int id, LinkedList<Integer> truth,
			LinkedList<Integer> holdout, int pos) {
		LinkedList<Double> AP = new LinkedList<Double>();
		HashSet<Integer> items = new HashSet<Integer>();
		for (int i = 0; i < data.getSizeMovie(); i++)
			items.add(i);
		items.removeAll(holdout);

		LinkedList<Pair<Integer, Double>> rank = getRank(id, items);

		HashSet<Integer> ts = new HashSet<Integer>(truth);
		int cnt = 0, hit = 0;
		for (Pair<Integer, Double> item : rank) {
			cnt++;
			if (ts.contains(item.getFirst())) {
				hit++;
				AP.add(hit / (0.0 + cnt));
			}
			if (cnt == pos)
				break;
		}
		if (AP.size() == 0)
			return 0;
		return DataOps.average(AP);
	}

	private double calcAUC(int id, LinkedList<Integer> truth,
			LinkedList<Integer> holdout, int pos) {
		LinkedList<Double> AP = new LinkedList<Double>();
		HashSet<Integer> items = new HashSet<Integer>();
		for (int i = 0; i < data.getSizeMovie(); i++)
			items.add(i);
		items.removeAll(holdout);

		LinkedList<Pair<Integer, Double>> rank = getRank(id, items);

		HashSet<Integer> ts = new HashSet<Integer>(truth);
		int cnt = 0, hit = 0;
		Pair<Double, Double> la = new Pair<Double, Double>(0., 0.);
		double res = 0;
		for (Pair<Integer, Double> item : rank) {
			cnt++;
			if (ts.contains(item.getFirst()))
				hit++;
			double tp = hit / (ts.size() + 0.0);
			double fp = (cnt - hit) / (items.size() - ts.size() + 0.0);
			res += (la.getFirst() + tp) / 2 * (fp - la.getSecond());
			la = new Pair<Double, Double>(tp, fp);
		}
		// System.out.println(res);
		return res;
	}

	public void evaluate() {
		int POS = 0;
		ArrayList<Integer> traincnt = new ArrayList<Integer>();
		for (int i = 0; i < data.getSizeDouban(); i++)
			traincnt.add(0);
		LinkedList<Double> trainMAE = new LinkedList<Double>();

		for (int i = 0; i < data.getSizeDouban(); i++) {
			traincnt.add(train.get(i).size());
			LinkedList<Integer> curTruth = new LinkedList<Integer>(train.get(i));
			curTruth.addAll(test.get(i));
			trainMAE.add(calcAUC(i, curTruth, new LinkedList<Integer>(), POS));
		}

		System.out.println(expname);
		System.out.println("MAP @ Train: " + DataOps.average(trainMAE));
		LinkedList<Double> testMAP = new LinkedList<Double>();
		int[] coldThres = new int[] { 0, 1, 2, 5, 10, 20, 30, 40, 50, 60, 70,
				80, 90, 100, 10000 };
		ArrayList<LinkedList<Double>> coldMAE = new ArrayList<LinkedList<Double>>();
		for (int i = 0; i < coldThres.length; i++)
			coldMAE.add(new LinkedList<Double>());

		for (int i = 0; i < data.getSizeDouban(); i++) {
			if (test.get(i).size() == 0)
				continue;
			double MAP = calcAUC(i, test.get(i), train.get(i), POS);
			testMAP.add(MAP);
			int size = train.get(i).size();
			for (int q = 0; q < coldThres.length; q++)
				if (size <= coldThres[q]) {
					coldMAE.get(q).add(MAP);
					break;
				}
		}

		System.out.println("MAP @ Test: " + DataOps.calcRMSE(testMAP));
		for (int i = 0; i < coldThres.length; i++) {
			System.out.println("MAP @ Cold<" + coldThres[i] + " : "
					+ DataOps.calcRMSE(coldMAE.get(i)) + "\t"
					+ coldMAE.get(i).size());
		}
	}
}
