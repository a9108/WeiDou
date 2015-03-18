package data.selector;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import Crawler.Worker;
import basic.Config;
import basic.FileOps;
import data.Douban;
import data.DoubanUser;
import data.MovieLog;
import data.Weibo;

public class WeiboSelector {
	static nlp.ChineseParser parser;
	public static void main(String[] args) {
		parser = new nlp.ChineseParser();
		parser.init("D:\\cxz\\Workspace\\seg\\data");
		Config.load("config.txt");
		Weibo weibo = new Weibo();
		weibo.loadUser();
		weibo.loadWeibo();

		System.out.println("Weibo Loaded");

		LinkedList<String> users = FileOps.LoadFilebyLine(Config
				.getValue("SelectDir") + "users");
		System.out.println("User Loaded");

		LinkedList<String> username = new LinkedList<String>();
		final LinkedList<String> userweibo = new LinkedList<String>();

		for (String user : users) {
			String[] sep = user.split("\t");
			if (sep.length != 3)
				continue;
			final String uid = sep[2];
			username.add(uid + "\t" + weibo.getUsername(uid));
			System.out.println("Parsing "+uid+"\t # = "+weibo.getWeibo(uid).size());
			
			final LinkedList<String> Q=weibo.getWeibo(uid);
			
			Thread [] workers=new Thread[Integer.valueOf(Config.getValue("#Thread"))];
			for (int i=0;i<workers.length;i++){
				workers[i]=new Thread(){
					public void run() {
						for (;;){
							String w;
							synchronized (Q) {
								if (Q.isEmpty()) return;
								w=Q.removeFirst();
							}
							List<String> seg = parser.parse(w);
//							System.out.println(seg);
							StringBuilder sb = new StringBuilder();
							sb.append(uid + ":");
							for (String s : seg)
								sb.append(s + "\t");
							synchronized (userweibo) {
								userweibo.add(sb.toString());
							}
						}
					};
				};
				workers[i].start();
			}
			for (Thread worker:workers)
				try {
					worker.join();
				} catch (Exception e) {
				}
//			for (String w : weibo.getWeibo(uid)) {
//				List<String> seg = parser.parse(w);
//				System.out.println(seg);
//				StringBuilder sb = new StringBuilder();
//				sb.append(uid + ":");
//				for (String s : seg)
//					sb.append(s + "\t");
//				userweibo.add(sb.toString());
//			}
		}

		FileOps.SaveFile(Config.getValue("SelectDir") + "weibo_username",
				username);
		FileOps.SaveFile(Config.getValue("SelectDir") + "weibo_weibo",
				userweibo);

	}
}
