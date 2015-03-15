package parser.douban;

import java.io.File;
import java.nio.channels.NetworkChannel;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import Util.NetworkConnect;
import data.Douban;
import data.DoubanUser;
import data.MovieLog;
import BasicOps.Config;
import BasicOps.FileOps;
import Crawler.Client;
import Crawler.ProxyBank;

public class UserMovie {
	public static LinkedList<String> Q = new LinkedList<String>();
	public static int cnt;
	private static HashMap<Integer, Integer> moviecnt = new HashMap<Integer, Integer>();

	public static void main(String[] args) {
		Config.load("config.txt");
		final String dir = Config.getValue("RawDataDir") + "douban/userwatched/";
		Douban douban = new Douban();
		douban.loadUser();
		for (DoubanUser user : douban.users)
			Q.add(user.uid);
		cnt = 0;

		Thread[] workers = new Thread[10];
		for (int i = 0; i < workers.length; i++) {
			workers[i] = new Thread() {
				public void run() {
					for (;;) {
						String curid = "";
						synchronized (Q) {
							if (Q.size() == 0)
								break;
							curid = Q.getFirst();
							Q.removeFirst();
							cnt++;
						}
						LinkedList<String> outdata = new LinkedList<String>();
						String curdir = dir + curid + "/";
						if (!FileOps.exist(curdir))
							continue;

						String c1 = loadUserMovieDoc(curid, 1);
						Matcher matcher = Pattern.compile(
								"<span> 1/(\\d*) </span>").matcher(c1);
						int page = 1;
						if (matcher.find())
							page = Integer.valueOf(matcher.group(1));

						for (int i = page + 1; i <= 1000; i++) {
							if (FileOps.exist(curdir + i + ".html")) {
								(new File(curdir + i + ".html")).delete();
							} else
								break;
						}

						int curcnt = 0;

						for (int i = 1; i <= page; i++) {
							String content = loadUserMovieDoc(curid, i);
							matcher = Pattern.compile(
									"<div class=\"item\">([\\s\\S]*?)</div>")
									.matcher(content);
							for (; matcher.find();) {
								MovieLog cur = new MovieLog();
								if (cur.parse(curid, matcher.group(1))) {
									outdata.add(cur.toString());
									curcnt++;
									int mid = cur.mid;
									synchronized (moviecnt) {
										if (moviecnt.containsKey(mid))
											moviecnt.put(mid, moviecnt.get(mid) + 1);
										else
											moviecnt.put(mid, 1);
									}
								}
							}
						}
						System.out.println("cnt : " + cnt + "\tuid : " + curid
								+ "\t watched : " + curcnt);
						 FileOps.SaveFile(Config.getValue("DataDir")
						 + "douban_usermovie\\" + curid, outdata);
					}
				};
			};
			workers[i].start();
		}
		for (int i = 0; i < workers.length; i++)
			try {
				workers[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
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
		for (int i = 0; i < movies.size(); i++)
			outdata.add(i + "\t" + movies.get(i)[0] + "\t" + movies.get(i)[1]);

		FileOps.SaveFile(Config.getValue("DataDir") + "douban_movie", outdata);

	}

	private static String loadUserMovieDoc(String id, int i) {
		String dir = Config.getValue("RawDataDir") + "douban/userwatched/" + id
				+ "/" + i + ".html";
		String content = FileOps.LoadFile(dir, "UTF-8");
		if (!content.contains("看过的电影"))
			content = FileOps.LoadFile(dir, "GBK");
		if (!content.contains("看过的电影")) {
			System.out.println(content);
		}
		return content;
	}
}
