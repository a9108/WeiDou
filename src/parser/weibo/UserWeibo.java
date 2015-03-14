package parser.weibo;

import java.io.File;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import BasicOps.Config;
import BasicOps.FileOps;
import BasicOps.StringOps;

public class UserWeibo {
	private LinkedList<String> Q = new LinkedList<String>();
	private LinkedList<String> outdata = new LinkedList<String>();
	private LinkedList<String> uids=new LinkedList<String>();
	public void run() {
		String dir = Config.getValue("RawDataDir") + "weibo\\user\\";
		
		for (String uid : (new File(dir)).list())
			Q.add(uid);
		uids=new LinkedList<String>(Q);

		Thread[] worker = new Thread[10];

		for (int i = 0; i < worker.length; i++) {
			worker[i] = new Thread() {
				public void run() {
					for (;;){
						String curid="";
						synchronized (Q) {
							if (Q.isEmpty()) break;
							curid=Q.removeFirst();
						}
						
						LinkedList<String> curout=new LinkedList<String>();
						for (String page:(new File(dir+curid)).list()){
							String content=loadWeiboPage(dir+curid+"\\"+page);
							Matcher matcher=Pattern.compile("<span class=\"ctt\">(.*?)</span>").matcher(content);
							for (;matcher.find();)
								curout.add(curid+"\t"+StringOps.simplifyHTML(matcher.group(1)).replace("\t", ""));
						}
						System.out.println(Q.size()+"\tuid : "+curid+"\t"+" #weibo : "+curout.size());
						synchronized (outdata) {
							outdata.addAll(curout);
						}
					}
				};
			};
			worker[i].start();
		}

		for (int i = 0; i < worker.length; i++)
			try {
				worker[i].join();
			} catch (Exception e) {
			}
		
		FileOps.SaveFile(Config.getValue("DataDir")+"weibo_weibo", outdata);
		FileOps.SaveFile(Config.getValue("DataDir")+"weibo_user", uids);
	}
	
	public static String loadWeiboPage(String dir){
		String content = FileOps.LoadFile(dir, "UTF-8");
		if (!content.contains("的微博"))
			content = FileOps.LoadFile(dir, "GBK");
		if (!content.contains("的微博")) {
			System.out.println(content);
		}
		return content;
	}
	
}
