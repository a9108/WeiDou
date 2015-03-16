package parser.weibo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import basic.Config;
import basic.FileOps;

public class Friends {
	public static void main(String[] args) {
		Config.load("config.txt");
		final HashMap<String, String> username=FileOps.LoadDictionSS(Config.getValue("SelectDir")+"weibo_username");
		final HashMap<Long,LinkedList<Long> > links=new HashMap<Long, LinkedList<Long>>();
		
		final LinkedList<String> Q=new LinkedList<String>();
		Q.addAll(username.keySet());
		
		Thread[]workers=new Thread[10];
		for (int i=0;i<10;i++){
			workers[i]=new Thread(){
				public void run() {
					Long uid;
					for (;;){
						synchronized (Q) {
							if (Q.isEmpty()) return;
							uid=Long.valueOf(Q.removeFirst());
							System.out.println("Loading Friends : "+Q.size()+" / "+username.size()+"\t"+links.size());
						}
						for (int i=1;;i++){
							String content=loadFriendFile(Config.getValue("RawDataDir")+"weibo\\friends\\"+uid+"\\"+i);
							if (content.length()==0) break;
							Matcher matcher=Pattern.compile("attention/add\\?uid=(\\d*)").matcher(content);
							int cnt=0;
							for (;matcher.find();cnt++){
								Long uidb=Long.valueOf(matcher.group(1));
								if (uidb.equals(uid)) continue;
								if (!username.containsKey(matcher.group(1))) continue;
								synchronized (links) {
									if (!links.containsKey(uid)) links.put(uid, new LinkedList<Long>());
									if (!links.containsKey(uidb)) links.put(uidb, new LinkedList<Long>());
									links.get(uid).add(uidb);
									links.get(uidb).add(uid);
								}
							}
							if (cnt==0) System.out.println(content);
						}
					}
				};
			};
			workers[i].start();
		}
		for (int i=0;i<10;i++)
			try{
				workers[i].join();
			}catch (Exception e) {
			}
		
		for (String uid:username.keySet()){
			
		}
		LinkedList<String> outdata=new LinkedList<String>();
		for (Long uida:links.keySet()){
//			System.out.println(uida+"\t"+new HashSet<Long>(links.get(uida)).size());
			for (Long uidb:new HashSet<Long>(links.get(uida)))
				outdata.add(uida+"\t"+uidb);
		}
		FileOps.SaveFile(Config.getValue("SelectDir")+"weibo_friend",outdata);
	}
	
	private static String loadFriendFile(String dir){
		String content=FileOps.LoadFile(dir,"UTF-8");
		if (content.contains("关注的人")) return content;
		content=FileOps.LoadFile(dir,"GBK");
		if (content.contains("关注的人")) return content;
		return "";
	}
}
