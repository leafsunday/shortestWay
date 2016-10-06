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
import java.util.Set;
import java.util.TreeSet;
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
			ps = conn.prepareStatement("select from_id,to_id,distance from tb_site_edge");
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

	private Set<String> getAvoidEdge(String path){
		Set<String> edges = new TreeSet<String>();
		if(path != null){
			for(String avoidPath : avoidPathSet){
				if(path.indexOf(avoidPath) > -1){
					String[] sites = avoidPath.split(",");
					//组装边
					for(int i=0;i<sites.length-1;i++){
						edges.add(sites[i]+","+sites[i+1]);
					}
				}
			}
		}
		return edges;
	}

	private Set<String> getAvoidSite(String path){
		Set<String> sites = new TreeSet<String>();
		if(path != null){
			for(String avoidPath : avoidPathSet){
				if(path.indexOf(avoidPath) > -1){
					String[] ss = avoidPath.split(",");
					//获取中间的站点
					for(int i=1;i<ss.length-1;i++){
						sites.add(ss[i]);
					}
				}
			}
		}
		return sites;
	}

	private boolean checkSubSitePath(List<Site> subSiteList, Map<Integer, Site> siteMapOfId, String path){
		if(path != null){
			for(Site subSite : subSiteList){
				String subSiteNo = subSite.getSiteNo();
				String parnetNo = siteMapOfId.get(subSite.getParentId()).getSiteNo();
				int parentIndex = path.indexOf(parnetNo);
				int subSiteIndex = path.indexOf(subSiteNo);
				if(subSiteIndex > -1 && parentIndex > -1 && (parentIndex - subSiteIndex)==10){
					return false;
				}
			}
		}
		return true;
	}

	private Set<String> getSubSiteEdge(List<Site> subSiteList, Map<Integer, Site> siteMapOfId,String path){
		Set<String> edges = new TreeSet<String>();;
		if(path != null){
			for(Site subSite : subSiteList){
				String subSiteNo = subSite.getSiteNo();
				String parnetNo = siteMapOfId.get(subSite.getParentId()).getSiteNo();
				int parentIndex = path.indexOf(parnetNo);
				int subSiteIndex = path.indexOf(subSiteNo);
				if(subSiteIndex > -1 && parentIndex > -1 && (parentIndex - subSiteIndex)==10){
					//截取中间的站点
					String midNo = path.substring(subSiteIndex+5, parentIndex-1);
					edges.add(subSiteNo+","+midNo);
					edges.add(midNo+","+parnetNo);
				}
			}
		}
		return edges;
	}

	private Set<String> getSubSite(List<Site> subSiteList, Map<Integer, Site> siteMapOfId, String path){
		Set<String> sites = new TreeSet<String>();;
		if(path != null){
			for(Site subSite : subSiteList){
				String subSiteNo = subSite.getSiteNo();
				String parnetNo = siteMapOfId.get(subSite.getParentId()).getSiteNo();
				int parentIndex = path.indexOf(parnetNo);
				int subSiteIndex = path.indexOf(subSiteNo);
				if(subSiteIndex > -1 && parentIndex > -1 && (parentIndex - subSiteIndex)==10){
					//截取中间的站点
					sites.add(path.substring(subSiteIndex+5, parentIndex-1));
				}
			}
		}
		return sites;
	}

	private boolean hasSubSite(List<Site> subSiteList, int id, int parentId){
		for(Site subSite : subSiteList){
			if(subSite.getId() == id && subSite.getParentId() == parentId)
				return true;
		}
		return false;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public boolean check(String path){
		List list = initSite();
		Map<Integer, Site> siteMapOfId = (Map<Integer, Site>) list.get(1);
		List<Site> subSiteList = (List<Site>) list.get(2);
		return checkAvoidPath(path) && checkSubSitePath(subSiteList, siteMapOfId, path);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void exec(int vId, int t) {
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
		Site sourceSite = siteMapOfId.get(vId);
		DijkstraSP DSP = new DijkstraSP(G, vId, siteMapOfId);
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
				System.out.println(path);
			}else{
				errorPathList.add(errorPath);
			}
		}

		//错误路径处理
		for(int i=0;i<errorPathList.size();i++){
			final SitePath errorPath = errorPathList.get(i);
			double weight = errorPath.getWeight();
			StringBuffer path = errorPath.getPath();
			errorPath.setWeight(Double.POSITIVE_INFINITY);
			reComputeOfEdge(errorPath, subSiteList, siteMapOfId, weight, path, G, siteMapOfNo);
			if(errorPath.getWeight() < Double.POSITIVE_INFINITY){
				//替换环路 进行 最短路径矫正
				reComputeOfCycle(errorPath, subSiteList, siteMapOfId, weight, path, G, siteMapOfNo);
				
				errorPath.getPath().insert(0, siteMapOfId.get(errorPath.getFrom()).getSiteNo()
						+" to "+siteMapOfId.get(errorPath.getTo()).getSiteNo()
						+"["+String.format("%.3f", errorPath.getWeight())+"] : ");
				System.out.println(errorPath.getPath());
			}
		}
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
		ExecutorService executor = Executors.newFixedThreadPool(5);
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
		executor = Executors.newFixedThreadPool(5);
		for(int i=0;i<errorPathList.size();i++){
			final SitePath errorPath = errorPathList.get(i);
			executor.execute(new Runnable(){
				public void run() {
					double weight = errorPath.getWeight();
					StringBuffer path = errorPath.getPath();
					errorPath.setWeight(Double.POSITIVE_INFINITY);
					reComputeOfEdge(errorPath, subSiteList, siteMapOfId, weight, path, G, siteMapOfNo);
					if(errorPath.getWeight() < Double.POSITIVE_INFINITY){
						//替换环路 进行 最短路径矫正
						reComputeOfCycle(errorPath, subSiteList, siteMapOfId, weight, path, G, siteMapOfNo);
						
						errorPath.getPath().insert(0, siteMapOfId.get(errorPath.getFrom()).getSiteNo()
								+" to "+siteMapOfId.get(errorPath.getTo()).getSiteNo()
								+"["+String.format("%.3f", errorPath.getWeight())+"] : ");
						logger.info(errorPath.getPath().toString());
					}
				}
			});
		}
		executor.shutdown();
	}

	private void reComputeOfEdge(SitePath errorPath, List<Site> subSiteList, Map<Integer, Site> siteMapOfId,
			double weight, StringBuffer path, EdgeWeightedDigraph G, Map<Integer, Site> siteMapOfNo){
		if(!checkSubSitePath(subSiteList, siteMapOfId, path.toString())
				|| !checkAvoidPath(path.toString())){
			//去边
			Set<String> edges = getAvoidEdge(path.toString());
			edges.addAll(getSubSiteEdge(subSiteList, siteMapOfId, path.toString()));
			for(String e : edges){
				String[] siteNos = e.split(",");
				String from = siteNos[0];
				String to = siteNos[1];
				DijkstraSP dsp = removeEdgeSP(G, siteMapOfNo.get(from).getId(), siteMapOfNo.get(to).getId(), 
						errorPath.getFrom(), siteMapOfId);
				int w = errorPath.getTo();
				if(dsp.hasPathTo(w) && dsp.distTo(w) > 0 && errorPath.getWeight() > dsp.distTo(w)){
					reComputeOfEdge(errorPath, subSiteList, siteMapOfId, dsp.distTo(w), dsp.pathTo(w), dsp.getG(), 
							siteMapOfNo);
				}
			}
		}else{
			if(errorPath.getWeight() > weight){
				errorPath.setWeight(weight);
				errorPath.setPath(path);
			}
		}
	}
	
	private void reComputeOfCycle(SitePath errorPath, List<Site> subSiteList, Map<Integer, Site> siteMapOfId,
			double weight, StringBuffer path, EdgeWeightedDigraph G, Map<Integer, Site> siteMapOfNo){
		Set<String> sites = getAvoidSite(path.toString());
		sites.addAll(getSubSite(subSiteList, siteMapOfId, path.toString()));
		if(sites.size() == 1){
			//替换环路
			for(String s:sites){
				int sIndex = path.indexOf(s);
				String beforeSiteNo = path.substring(sIndex-5, sIndex-1);
				String afterSiteNo = path.substring(sIndex+5, sIndex+9);
				int v = siteMapOfNo.get(s).getId();
				SitePath initPath = new SitePath(v, v, Double.POSITIVE_INFINITY, null);
				for(SiteEdge e:G.adj(v)){
					DijkstraSP DSP = new DijkstraSP(G, e.to(), siteMapOfId);
					if(DSP.hasPathTo(v) && DSP.distTo(v) > 0){
						addCycleSP(subSiteList, siteMapOfId, G, siteMapOfNo, initPath, errorPath, e, 
								DSP.pathTo(v), 0.0d, path.toString(), DSP.distTo(v), s, beforeSiteNo, 
								afterSiteNo);
					}
				}
				if(initPath.getPath() != null && weight+initPath.getWeight() < errorPath.getWeight()){
					StringBuffer clonePath = new StringBuffer(path);
					clonePath.replace(clonePath.indexOf(s), clonePath.indexOf(s)+4, initPath.getPath().toString());
					if(checkSubSitePath(subSiteList, siteMapOfId, clonePath.toString()) && checkAvoidPath(clonePath.toString())){
						errorPath.setWeight(weight+initPath.getWeight());
						errorPath.setPath(clonePath);
					}
				}
			}
		}
	}

	private void addCycleSP(List<Site> subSiteList, Map<Integer, Site> siteMapOfId, EdgeWeightedDigraph G,
			Map<Integer, Site> siteMapOfNo, SitePath initPath, SitePath errorPath, SiteEdge e,
			StringBuffer cyclePath, double cycleWeight, String originalPath, double originalWeight,
			String cycleSiteNo, String beforeSiteNo, String afterSiteNo) {
		if(cycleWeight+e.weight()+originalWeight < errorPath.getWeight()){
			if(cyclePath.length() == 9){
				DijkstraSP dsp = removeEdgeSP(G, e.to(), e.from(), e.to(), siteMapOfId);
				if(dsp.hasPathTo(e.from()) && dsp.distTo(e.from()) > 0
						&& dsp.distTo(e.from())+e.weight() < initPath.getWeight()){
					addCycleSP(subSiteList, siteMapOfId, dsp.getG(), siteMapOfNo, initPath, errorPath, e, 
							dsp.pathTo(e.from()), dsp.distTo(e.from()), originalPath, originalWeight, 
							cycleSiteNo, beforeSiteNo, afterSiteNo);
				}
			}else if(cyclePath.length() > 9){
				String[] ss = cyclePath.toString().split(",");
				if(beforeSiteNo.equals(ss[0]) || hasSubSite(subSiteList, siteMapOfNo.get(beforeSiteNo).getId(), 
						siteMapOfNo.get(ss[0]).getId())){
					//放弃这条路
				}else{
					Set<String> edges = new TreeSet<String>();
					if(afterSiteNo.equals(ss[ss.length-2])){
						edges.add(ss[ss.length-2]+","+cycleSiteNo);
					}
					String originalTemp = originalPath.replace(cycleSiteNo, cycleSiteNo+","+cyclePath);
					Set<String> tempSet = getAvoidEdge(originalTemp);
					tempSet.addAll(getSubSiteEdge(subSiteList, siteMapOfId, originalTemp));
					for(String temp : tempSet){
						if(cyclePath.indexOf(temp)>-1) edges.add(temp);
					}
					
					if(edges.size() > 0){
						for(String edge : edges){
							String[] siteNos = edge.split(",");
							String from = siteNos[0];
							String to = siteNos[1];
							DijkstraSP dsp = removeEdgeSP(G, siteMapOfNo.get(from).getId(), siteMapOfNo.get(to).getId(), 
									e.to(), siteMapOfId);
							if(dsp.hasPathTo(e.from()) && dsp.distTo(e.from()) > 0
									&& dsp.distTo(e.from())+e.weight() < initPath.getWeight()){
								addCycleSP(subSiteList, siteMapOfId, dsp.getG(), siteMapOfNo, initPath, errorPath, e, 
										dsp.pathTo(e.from()), dsp.distTo(e.from()), originalPath, originalWeight, cycleSiteNo, 
										beforeSiteNo, afterSiteNo);
							}
						}
					}else{
						if(initPath.getWeight() > cycleWeight+e.weight()){
							initPath.setWeight(cycleWeight+e.weight());
							initPath.setPath(cyclePath.insert(0, cycleSiteNo+","));
						}
					}
				}
			}
		}
	}
	
	private DijkstraSP removeEdgeSP(EdgeWeightedDigraph G, int edgeFrom, int edgeTo, 
			int pathFrom, Map<Integer, Site> siteMapOfId){
		EdgeWeightedDigraph g = new EdgeWeightedDigraph(G);
		g.removeEdge(edgeFrom, edgeTo);
		return new DijkstraSP(g, pathFrom, siteMapOfId);
	}
}
