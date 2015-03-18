package data.selector;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;

import basic.Config;
import basic.FileOps;
import data.Douban;
import data.DoubanUser;
import data.MovieLog;

public class MovieSelector {
	public static void main(String[] args) {
		Config.load("config.txt");

		HashMap<Integer, Integer> moviecnt = new HashMap<Integer, Integer>();
		HashMap<Integer, String> movieName = new HashMap<Integer, String>();

		LinkedList<String> Q = new LinkedList<String>();
		for (String line : FileOps.LoadFilebyLine(Config.getValue("SelectDir")
				+ "users"))
			Q.add(line.split("\t")[1]);

		int cnt = 0;
		for (String uid : Q) {
			System.out.println("Loading " + (++cnt));
			for (String s : FileOps.LoadFilebyLine(Config.getValue("DataDir")
					+ "douban_usermovie\\" + uid)) {
				MovieLog cur = new MovieLog(s);
				movieName.put(cur.mid, cur.name);
				int mid = cur.mid;
				if (moviecnt.containsKey(mid))
					moviecnt.put(mid, moviecnt.get(mid) + 1);
				else
					moviecnt.put(mid, 1);
			}
		}
		LinkedList<Integer[]> movies = new LinkedList<Integer[]>();
		for (int mid : moviecnt.keySet())
			movies.add(new Integer[] { mid, moviecnt.get(mid) });
		Collections.sort(movies, new Comparator<Integer[]>() {
			public int compare(Integer[] o1, Integer[] o2) {
				return -o1[1].compareTo(o2[1]);
			};
		});

		LinkedList<String> outdata = new LinkedList<String>();
		for (int i = 0; i < movies.size() && i < 1000; i++)
			outdata.add(i + "\t" + movies.get(i)[0] + "\t"
					+ movieName.get(movies.get(i)[0]) + "\t" + movies.get(i)[1]);

		FileOps.SaveFile(Config.getValue("SelectDir") + "douban_movie",
				outdata);
	}
}
