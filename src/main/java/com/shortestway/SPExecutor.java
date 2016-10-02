/**
 * 版权所有：恒生电子股份有限公司
 * 项目名称:shortestway
 * 创建者: yexh
 * 创建日期: 2016年10月2日
 * 文件说明: 
 */
package com.shortestway;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.shortestway.utils.DBUtils;

/**
 * @author yexh
 *
 */
public class SPExecutor {
	
	private Logger logger = LogManager.getLogger("SPExcutor.info");
	
	private String[] avoidPathSet = {
			"1004,1005,2602",
			"1004,2602,2629",
			"1006,1901,1923",
			"1006,1923,1901",
			"1023,1005,2602",
			"1023,2602,2629",
			"1029,1001,2643",
			"1055,2604,2630",
			"1533,1531,1543",
			"1543,1531,1533",
			"1803,3533,3535",
			"1901,1006,1923",
			"1901,1923,1006",
			"1902,2702,3103",
			"1902,3103,2702",
			"1913,1902,3103,2711",
			"1913,1902,3123",
			"1923,1006,1901",
			"1923,1901,1006",
			"2321,2302,2323",
			"2323,2302,2321",
			"2602,1005,1004",
			"2602,1005,1023",
			"2607,2637,2639",
			"2628,2602,1004",
			"2628,2602,1023",
			"2628,2632,3102",
			"2629,2632,3102",
			"2630,2604,1055",
			"2639,2637,2607",
			"2643,1001,1029",
			"2702,1902,3103",
			"2702,3103,1902",
			"2711,3103,1902,1913",
			"2921,2923,2925",
			"2925,2923,2921",
			"3102,2632,2628",
			"3102,2632,2629",
			"3103,1902,2702",
			"3103,2702,1902",
			"3123,1902,1913",
			"3535,3533,1803",
			"3545,3549,3547",
			"3547,3549,3546"
	};

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private List initSite(){
		Connection conn = DBUtils.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		List list = new ArrayList();
		Map<Integer, Site> siteMapOfId = new HashMap<Integer, Site>();
		Map<String, Site> siteMapOfNo = new HashMap<String, Site>();
		List<Site> subSiteList = new ArrayList<Site>();
		try {
			ps = conn.prepareStatement("select id,parent_id,site_no,site_name from tb_site_info ");
			rs = ps.executeQuery();
			while(rs.next()){
				Site site = new Site();
				site.setId(rs.getInt(1));
				site.setParentId(rs.getInt(2));
				site.setSiteNo(rs.getString(3));
				site.setSiteName(rs.getString(4));
				siteMapOfId.put(site.getId(), site);
				siteMapOfNo.put(site.getSiteNo(), site);
				if(site.getId() != site.getParentId()){
					subSiteList.add(site);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			DBUtils.close(rs, ps, conn);
		}
		list.add(siteMapOfNo);
		list.add(siteMapOfId);
		list.add(subSiteList);
		return list;
	}

	private EdgeWeightedDigraph initSiteGraph(int siteSize){
		EdgeWeightedDigraph G = new EdgeWeightedDigraph(siteSize);
		Connection conn = DBUtils.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = conn.prepareStatement("select from_id,to_id,distance from tb_site_relation");
			rs = ps.executeQuery();
			while(rs.next()){
				SiteEdge siteEdge = new SiteEdge(rs.getInt(1), rs.getInt(2), rs.getDouble(3));
				G.addEdge(siteEdge);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			DBUtils.close(rs, ps, conn);
		}
		return G;
	}
	
	private DijkstraSP reCompute(EdgeWeightedDigraph G, String vNo, String wNo, 
			Map<Integer, Site> siteMapOfNo,
			int vId, Map<Integer, Site> siteMapOfId){
		DijkstraSP DSP = null;
		//去掉beforeLastNo->lastNo这条边重新计算最短路径
		EdgeWeightedDigraph g = new EdgeWeightedDigraph(G);
		g.removeEdge(siteMapOfNo.get(vNo).getId(), siteMapOfNo.get(wNo).getId());
		DSP = new DijkstraSP(g, vId, siteMapOfId);
		return DSP;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private List reCheck(String[] siteNos, StringBuffer pathInfo, double pathWeight, int t, int vId,
			Map<Integer, Site> siteMapOfId, Map<Integer, Site> siteMapOfNo, EdgeWeightedDigraph G){
		List list = new ArrayList();
		for(int i=siteNos.length-1;i>0;i--){
			String lastNo = siteNos[i];
			String beforeLastNo = siteNos[i-1];
			//去掉beforeLastNo->lastNo这条边重新计算最短路径
			DijkstraSP avoidDSP = reCompute(G, beforeLastNo, lastNo, siteMapOfNo, vId, siteMapOfId);
			if(avoidDSP.hasPathTo(t)){
				pathInfo = avoidDSP.pathTo(t);
				pathWeight = avoidDSP.distTo(t);
				break;
			}else if(i==1){
				pathInfo = null;
			}
		}
		list.add(pathInfo);
		list.add(pathWeight);
		return list;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public boolean checkPath(String path){
		boolean passed = true;
		List list = initSite();
		Map<Integer, Site> siteMapOfNo = (Map<Integer, Site>) list.get(0);
		Map<Integer, Site> siteMapOfId = (Map<Integer, Site>) list.get(1);
		List<Site> subSiteList = (List<Site>) list.get(2);//副站点
		EdgeWeightedDigraph g = initSiteGraph(siteMapOfId.size());
		//联通性校验
		String[] siteNos = path.split(",");
		for(int i=0;i+1<siteNos.length;i++){
			if(g.getEdge(siteMapOfNo.get(siteNos[i]).getId(), 
					siteMapOfNo.get(siteNos[i+1]).getId()) == null){
				System.out.println("存在不联通的边:"+siteNos[i]+","+siteNos[i+1]);
			}
		}
		//回头路校验
		Map<String, Object> siteNoMap = new HashMap<String, Object>();
		for(int i=0;i<siteNos.length;i++){
			siteNoMap.put(siteNos[i], null);
		}
		if(siteNos.length > siteNoMap.size()){
			System.out.println("存在回头路");
		}
		
		
		//回避路径校验
		for(String avoidPath : avoidPathSet){
			if(path.indexOf(avoidPath) > -1){
				System.out.println("存在回避路径:"+avoidPath);
				passed = false;
			}
		}
		//主付站校验
		for(Site site : subSiteList){
			String siteNo = site.getSiteNo();
			String parnetNo = siteMapOfId.get(site.getParentId()).getSiteNo();
			int parentIndex = path.indexOf(parnetNo);
			int siteIndex = path.indexOf(siteNo);
			if(siteIndex > -1 && parentIndex > -1 && siteIndex < parentIndex){
				System.out.println("存在主付站:"+siteNo+","+parnetNo);
			}
		}
		
		return passed;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void exec() {
		/*
		 * 初始化数据
		 */
		List list = initSite();
		final Map<Integer, Site> siteMapOfNo = (Map<Integer, Site>) list.get(0);
		final Map<Integer, Site> siteMapOfId = (Map<Integer, Site>) list.get(1);
		final List<Site> subSiteList = (List<Site>) list.get(2);//副站点
		final EdgeWeightedDigraph G = initSiteGraph(siteMapOfId.size());
		
		/*
		 * 开始计算最短路径
		 */
		ExecutorService executor = Executors.newFixedThreadPool(3);//多线程
		Iterator<Integer> siteIt = siteMapOfId.keySet().iterator();
		while(siteIt.hasNext()){
			final Integer vId = siteIt.next();
			executor.execute(new Runnable(){
				public void run() {
					Site sourceSite = siteMapOfId.get(vId);
					DijkstraSP DSP = new DijkstraSP(G, vId, siteMapOfId);
					for (int t = 0; t < G.V(); t++) {
						if (DSP.hasPathTo(t) && vId != t) {
							StringBuffer pathInfo = DSP.pathTo(t);
							double pathWeight = DSP.distTo(t);
							//回避路径校验
							for(String avoidPath : avoidPathSet){
								if(pathInfo != null && pathInfo.indexOf(avoidPath) > -1){
									String[] siteNos = avoidPath.split(",");
									List checkedList = reCheck(siteNos, pathInfo, pathWeight, t, vId, siteMapOfId, siteMapOfNo, G);
									pathInfo = (StringBuffer) checkedList.get(0);
									pathWeight = (Double) checkedList.get(1);
								}
							}
							//主付站校验
							if(pathInfo != null){
								for(Site subSite : subSiteList){
									if(pathInfo != null){
										String subSiteNo = subSite.getSiteNo();
										String parnetNo = siteMapOfId.get(subSite.getParentId()).getSiteNo();
										int parentIndex = pathInfo.indexOf(parnetNo);
										int subSiteIndex = pathInfo.indexOf(subSiteNo);
										if(subSiteIndex > -1 && parentIndex > -1 && subSiteIndex < parentIndex){
											//截取subSiteNo-->parnetNo
											String subPath = pathInfo.substring(subSiteIndex, parentIndex+4);
											String[] siteNos = subPath.split(",");
											List checkedList = reCheck(siteNos, pathInfo, pathWeight, t, vId, siteMapOfId, siteMapOfNo, G);
											pathInfo = (StringBuffer) checkedList.get(0);
											pathWeight = (Double) checkedList.get(1);
										}
									}
								}
							}
							
							if(pathInfo != null){
								pathInfo.insert(0,sourceSite.getSiteNo()+" to "+siteMapOfId.get(t).getSiteNo()
										+"["+String.format("%.3f", pathWeight)+"] : ");
								pathInfo.append(siteMapOfId.get(t).getSiteNo());
								logger.info(pathInfo.toString());
							}
						}
					}
				}
			});
		}
		executor.shutdown();
	}
}
