package task.movierec;

import java.text.Format;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;

import basic.Config;
import basic.DataOps;
import basic.FileOps;
import basic.Formats;
import basic.Vector;
import basic.algorithm.MatrixFactorization;
import basic.format.Pair;

public class Topics extends RankingTask {
	private ArrayList<ArrayList<Double>> phi;
	private ArrayList<ArrayList<Double>> theta;

	Random random;

	public Topics() {
		random = new Random(0);
	}

	@Override
	public void train() {
		theta = new ArrayList<ArrayList<Double>>();
		for (String line : FileOps.LoadFilebyLine(Config.getValue("WorkDir")
				+ "jum.theta"))
			theta.add(Formats.doubleArrayLoader(line, "\t"));
		HashMap<String, Integer> movieId = new HashMap<String, Integer>();
		for (String line : FileOps.LoadFilebyLine(Config.getValue("SelectDir")
				+ "douban_movie")) {
			String[] sep = line.split("\t");
			movieId.put(sep[2], Integer.valueOf(sep[0]));
		}
		ArrayList<String> phis = new ArrayList<String>(
				FileOps.LoadFilebyLine(Config.getValue("WorkDir")
						+ "jum.topic_phi.full"));
		phi = new ArrayList<ArrayList<Double>>();
		for (int i = 1; i < phis.size(); i += 2) {
			ArrayList<Double> curPhi = new ArrayList<Double>();
			for (int q = 0; q < data.getSizeMovie(); q++)
				curPhi.add(0.0);
			for (String s : phis.get(i).split("\t")) {
				String[] sep = s.split(":");
				if (sep.length == 2 && movieId.containsKey(sep[0]))
					curPhi.set(movieId.get(sep[0]), Double.valueOf(sep[1]));
			}
			phi.add(curPhi);
		}
	}

	@Override
	public double predict(int uid, int mid) {
		double s = 0;
		for (int i = 0; i < theta.get(uid).size(); i++)
			s += theta.get(uid).get(i) * phi.get(i).get(mid);
		return s;
	}
}
