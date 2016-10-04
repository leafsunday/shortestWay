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

	private List<SitePath> errorPathList = new ArrayList<SitePath>();

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

	private boolean checkAvoidPath(String path){
		if(path != null){
			for(String avoidPath : avoidPathSet){
				if(path.indexOf(avoidPath) > -1){
					return false;
				}
			}
		}
		return true;
	}

	private List<String> getAvoidPath(String path){
		List<String> avoidPaths = new ArrayList<String>();
		if(path != null){
			for(String avoidPath : avoidPathSet){
				if(path.indexOf(avoidPath) > -1){
					String[] sites = avoidPath.split(",");
					//组装边
					for(int i=0;i<sites.length-1;i++){
						avoidPaths.add(sites[i]+","+sites[i+1]);
					}
				}
			}
		}
		return avoidPaths;
	}

	private boolean checkSubSitePath(List<Site> subSiteList, Map<Integer, Site> siteMapOfId, String path){
		if(path != null){
			for(Site subSite : subSiteList){
				String subSiteNo = subSite.getSiteNo();
				String parnetNo = siteMapOfId.get(subSite.getParentId()).getSiteNo();
				int parentIndex = path.indexOf(parnetNo);
				int subSiteIndex = path.indexOf(subSiteNo);
				if(subSiteIndex > -1 && parentIndex > -1 && subSiteIndex < parentIndex){
					return false;
				}
			}
		}
		return true;
	}

	private boolean needRecordOfiSubSitePath(List<Site> subSiteList, Map<Integer, Site> siteMapOfId, 
			String path, int from, int to){
		boolean flag = false;
		if(path != null){
			for(Site subSite : subSiteList){
				String subSiteNo = subSite.getSiteNo();
				String parnetNo = siteMapOfId.get(subSite.getParentId()).getSiteNo();
				int parentIndex = path.indexOf(parnetNo);
				int subSiteIndex = path.indexOf(subSiteNo);
				if(subSiteIndex > -1 && parentIndex > -1 && subSiteIndex < parentIndex){
					if(subSite.getId() == from && subSite.getParentId() == to){
						return false;
					}
					flag = true;
				}
			}
		}
		return flag;
	}

	private List<String> getSubSite(List<Site> subSiteList, Map<Integer, Site> siteMapOfId, 
			String path, int from, int to){
		List<String> subSites = new ArrayList<String>();
		if(path != null){
			for(Site subSite : subSiteList){
				String subSiteNo = subSite.getSiteNo();
				String parnetNo = siteMapOfId.get(subSite.getParentId()).getSiteNo();
				int parentIndex = path.indexOf(parnetNo);
				int subSiteIndex = path.indexOf(subSiteNo);
				if(subSiteIndex > -1 && parentIndex > -1 && subSiteIndex < parentIndex){
					if(subSite.getId() == from && subSite.getParentId() == to){
					}else if(subSite.getId() == from){
						subSites.add(parnetNo);
					}else if(subSite.getParentId() == to){
						subSites.add(subSiteNo);
					}else{
						subSites.add(subSiteNo);
						subSites.add(parnetNo);
					}
				}
			}
		}
		return subSites;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public boolean check(String path){
		List list = initSite();
		Map<Integer, Site> siteMapOfId = (Map<Integer, Site>) list.get(1);
		List<Site> subSiteList = (List<Site>) list.get(2);
		return checkAvoidPath(path) && checkSubSitePath(subSiteList, siteMapOfId, path);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void exec() {
		/*
		 * 初始化数据
		 */
		List list = initSite();
		final Map<Integer, Site> siteMapOfNo = (Map<Integer, Site>) list.get(0);
		final Map<Integer, Site> siteMapOfId = (Map<Integer, Site>) list.get(1);
		final List<Site> subSiteList = (List<Site>) list.get(2);
		final EdgeWeightedDigraph G = initSiteGraph(siteMapOfId.size());

		/*
		 * 开始计算最短路径
		 */
		ExecutorService executor = Executors.newFixedThreadPool(3);
		Iterator<Integer> siteIt = siteMapOfId.keySet().iterator();
		while(siteIt.hasNext()){
			final Integer vId = siteIt.next();
			executor.execute(new Runnable(){
				public void run() {
					Site sourceSite = siteMapOfId.get(vId);
					DijkstraSP DSP = new DijkstraSP(G, vId, siteMapOfId);
					for (int t = 0; t < G.V(); t++) {
						if (DSP.hasPathTo(t) && vId != t) {
							StringBuffer path = DSP.pathTo(t);
							double pathWeight = DSP.distTo(t);
							SitePath errorPath = null;
							//回避路径/主付站校验
							if(!checkAvoidPath(path.toString())
									|| !checkSubSitePath(subSiteList, siteMapOfId, path.toString())){
								errorPath = new SitePath(vId, t, pathWeight, path);
							}

							if(errorPath == null){
								path.insert(0,sourceSite.getSiteNo()+" to "+siteMapOfId.get(t).getSiteNo()
										+"["+String.format("%.3f", pathWeight)+"] : ");
								logger.info(path.toString());
							}else{
								errorPathList.add(errorPath);
							}
						}
					}
				}
			});
		}
		executor.shutdown();
		while(!executor.isTerminated()){}
		//错误路径处理
		for(int i=0;i<errorPathList.size();i++){
			SitePath errorPath = errorPathList.get(i);
			errorPath.setWeight(Double.POSITIVE_INFINITY);
			reCompute(errorPath, subSiteList, siteMapOfId, errorPath.getWeight(), errorPath.getPath(), G, siteMapOfNo);
			if(errorPath.getWeight() < Double.POSITIVE_INFINITY){
				errorPath.getPath().insert(0, siteMapOfId.get(errorPath.getFrom()).getSiteNo()
						+" to "+siteMapOfId.get(errorPath.getTo()).getSiteNo()
						+"["+String.format("%.3f", errorPath.getWeight())+"] : ");
				logger.info(errorPath.getPath().toString());
			}
		}
	}

	private void reCompute(SitePath errorPath, List<Site> subSiteList, Map<Integer, Site> siteMapOfId,
			double weight, StringBuffer path, EdgeWeightedDigraph G, Map<Integer, Site> siteMapOfNo){
		if(needRecordOfiSubSitePath(subSiteList, siteMapOfId, path.toString(), errorPath.getFrom(), errorPath.getTo())
				|| !checkAvoidPath(path.toString())){
			List<String> subSites = getSubSite(subSiteList, siteMapOfId, path.toString(), errorPath.getFrom(), errorPath.getTo());
			List<String> edges = getAvoidPath(path.toString());

			if(subSites.size() > 0){
				for(int i=0;i<subSites.size();i++){
					EdgeWeightedDigraph g = new EdgeWeightedDigraph(G);
					String s = subSites.get(i);
					//去掉这个点重新计算最短路径
					g.removeSite(siteMapOfNo.get(s).getId());
					DijkstraSP DSP = new DijkstraSP(g, errorPath.getFrom(), siteMapOfId);
					int w = errorPath.getTo();
					if(DSP.hasPathTo(w)){
						reCompute(errorPath, subSiteList, siteMapOfId, DSP.distTo(w), DSP.pathTo(w), g, siteMapOfNo);
					}
				}
			}
			
			if(edges.size() > 0){
				for(int i=0;i<edges.size();i++){
					EdgeWeightedDigraph g = new EdgeWeightedDigraph(G);
					String e = edges.get(i);
					String[] siteNos = e.split(",");
					String from = siteNos[0];
					String to = siteNos[1];
					//去掉这条边重新计算最短路径
					g.removeEdge(siteMapOfNo.get(from).getId(), siteMapOfNo.get(to).getId());
					DijkstraSP DSP = new DijkstraSP(g, errorPath.getFrom(), siteMapOfId);
					int w = errorPath.getTo();
					if(DSP.hasPathTo(w)){
						reCompute(errorPath, subSiteList, siteMapOfId, DSP.distTo(w), DSP.pathTo(w), g, siteMapOfNo);
					}
				}
			}
		}else{
			if(errorPath.getWeight() > weight){
				errorPath.setWeight(weight);
				errorPath.setPath(path);
			}
		}
	}
}
