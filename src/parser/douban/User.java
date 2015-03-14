package parser.douban;

import java.io.File;
import java.nio.channels.NetworkChannel;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import Util.NetworkConnect;
import data.DoubanUser;
import BasicOps.Config;
import BasicOps.FileOps;
import Crawler.Client;
import Crawler.ProxyBank;

public class User {
	public void run(){
		String dir=Config.getValue("RawDataDir")+"douban/userhomepage/";
		String [] files=(new File(dir)).list();
		LinkedList<DoubanUser> users=new LinkedList<DoubanUser>();
		int cnt=0;
		for (String uid:files){
			cnt++;if (cnt%1000==0) System.out.println("Parsing Douban User : "+cnt);
			
			DoubanUser cur=new DoubanUser();
			cur.uid=uid;
			String content=FileOps.LoadFile(dir+uid,"UTF-8");
			if (!content.contains("返回他/她的豆瓣"))
				content=FileOps.LoadFile(dir+uid,"GBK");
			if (!content.contains("返回他/她的豆瓣")){
				System.out.println(content);
				System.out.println(uid);
				if (content.length()>100) return;
			}
			
			Matcher matcher=Pattern.compile("class=\"founder\">(.*?)</a>").matcher(content);
			if (!matcher.find()) continue;
			cur.display=matcher.group(1);
			matcher=Pattern.compile("<span>常居地：</span>(.*?)<br />").matcher(content);
			if (matcher.find())
				cur.location=matcher.group(1).trim();
			else cur.location="";
			matcher=Pattern.compile("<div class=\"intro\">([\\S\\s]*?)</div>").matcher(content);
			if (matcher.find())
				cur.description=matcher.group(1).trim();
			else cur.description="";
			users.add(cur);
		}
		
		LinkedList<String> outdata=new LinkedList<String>();
		for (DoubanUser user:users)
			outdata.add(user.toString());
		FileOps.SaveFile(Config.getValue("DataDir")+"douban_user", outdata);
	}
}
