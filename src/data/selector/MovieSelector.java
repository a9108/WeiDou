package data.selector;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;

import data.Douban;
import data.DoubanUser;
import data.MovieLog;
import BasicOps.Config;
import BasicOps.FileOps;

public class MovieSelector {
	public static void main(String[] args) {
		Config.load("config.txt");

		HashMap<Integer, Integer> moviecnt = new HashMap<Integer, Integer>();
		
		try {
			int cnt = 0;
			BufferedReader fin = new BufferedReader(new FileReader(
					Config.getValue("DataDir") + "douban_usermovie"));
			for (;;) {
				cnt++;
				if (cnt % 10000 == 0)
					System.out.println("Loading Movie # : " + cnt);
				String s = fin.readLine();
				if (s == null)
					break;
				MovieLog cur = new MovieLog(s);
				int mid = cur.mid;
				if (moviecnt.containsKey(mid))
					moviecnt.put(mid, moviecnt.get(mid) + 1);
				else
					moviecnt.put(mid, 1);
			}
			fin.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		LinkedList<Integer[]> movies = new LinkedList<Integer[]>();
		for (int mid : moviecnt.keySet())
			movies.add(new Integer[] { mid, moviecnt.get(mid) });
		Collections.sort(movies, new Comparator<Integer[]>() {
			public int compare(Integer[] o1, Integer[] o2) {
				return -o1[1].compareTo(o2[1]);
			};
		});

		LinkedList<String> outdata=new LinkedList<String>();
		for (int i = 0; i < movies.size(); i++)
			outdata.add(i+"\t"+movies.get(i)[0] + "\t" + movies.get(i)[1]);
		
		FileOps.SaveFile(Config.getValue("SelectDir")+"douban_movie", outdata);
	}
}
