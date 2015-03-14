package parser.douban;

import java.util.HashMap;
import java.util.LinkedList;

import data.Weibo;
import BasicOps.Config;
import BasicOps.FileOps;
import DBConnector.DoubanDB;
import DBConnector.WeiboDB;

public class WeiboLink {
	public void run(){
		Weibo weibo=new Weibo();
		weibo.loadUser();
		weibo.loadUmap();
		
		DoubanDB conn=new DoubanDB();
		LinkedList<String[]> link=conn.getWeiboLink();
		conn.close();
		LinkedList<String> outdata=new LinkedList<String>();
		
		for (String[] row:link){
			String wid=weibo.queryWeiboId(row[1]);
			if (wid.length()>0) 
				outdata.add(row[0]+"\t"+wid);
		}

		FileOps.SaveFile(Config.getValue("DataDir")+"douban_weibo", outdata);
	}
	
}
