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
import java.util.Vector;
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

	private List<SitePath> errorPathList = new Vector<SitePath>();

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
	
	private boolean hasAvoidSite(String path){
		if(path != null){
			for(String avoidPath : avoidPathSet){
				if(path.indexOf(avoidPath) > -1){
					return true;
				}
			}
		}
		return false;
	}

	private boolean checkSubSitePath(List<Site> subSiteList, Map<Integer, Site> siteMapOfId, String path){
		if(path != null && path.length() > 9){
			for(Site subSite : subSiteList){
				String subSiteNo = subSite.getSiteNo();
				String parnetNo = siteMapOfId.get(subSite.getParentId()).getSiteNo();
				if(path.indexOf(parnetNo) > -1 && path.indexOf(subSiteNo) > -1){
					String[] sites = path.split(",");
					for(int i=0;i<sites.length-2;i++){
						if((subSiteNo.equals(sites[i])&&parnetNo.equals(sites[i+2]))
								||(parnetNo.equals(sites[i])&&subSiteNo.equals(sites[i+2]))){
							return false;
						}
					}
				}
			}
			
			String[] ss = path.split(",");
			for(int j=0;j<ss.length-2;j++){
				if(ss[j].equals(ss[j+2]))
					return false;
			}
		}
		return true;
	}

	private Set<String> getSubSiteEdge(List<Site> subSiteList, Map<Integer, Site> siteMapOfId,String path){
		Set<String> edges = new TreeSet<String>();;
		if(path != null && path.length() > 9){
			for(Site subSite : subSiteList){
				String subSiteNo = subSite.getSiteNo();
				String parnetNo = siteMapOfId.get(subSite.getParentId()).getSiteNo();
				if(path.indexOf(parnetNo) > -1 && path.indexOf(subSiteNo) > -1){
					String[] sites = path.split(",");
					for(int i=0;i<sites.length-2;i++){
						if((subSiteNo.equals(sites[i])&&parnetNo.equals(sites[i+2]))
								||(parnetNo.equals(sites[i])&&subSiteNo.equals(sites[i+2]))){
							edges.add(sites[i]+","+sites[i+1]);
							edges.add(sites[i+1]+","+sites[i+2]);
						}
					}
				}
			}
		}
		return edges;
	}

	private Set<String> getSubSite(List<Site> subSiteList, Map<Integer, Site> siteMapOfId, String path){
		Set<String> siteSet = new TreeSet<String>();;
		if(path != null && path.length() > 9){
			for(Site subSite : subSiteList){
				String subSiteNo = subSite.getSiteNo();
				String parnetNo = siteMapOfId.get(subSite.getParentId()).getSiteNo();
				if(path.indexOf(parnetNo) > -1 && path.indexOf(subSiteNo) > -1){
					String[] sites = path.split(",");
					for(int i=0;i<sites.length-2;i++){
						if((subSiteNo.equals(sites[i])&&parnetNo.equals(sites[i+2]))
								||(parnetNo.equals(sites[i])&&subSiteNo.equals(sites[i+2]))){
							siteSet.add(sites[i+1]);
						}
					}
				}
			}
		}
		return siteSet;
	}

	private boolean hasSubSite(List<Site> subSiteList, int id, int parentId){
		for(Site subSite : subSiteList){
			if((subSite.getId() == id && subSite.getParentId() == parentId)
					||(subSite.getId() == parentId && subSite.getParentId() == id))
				return true;
		}
		return false;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public boolean check(String path){
		boolean flag = true;
		List list = initSite();
		Map<Integer, Site> siteMapOfId = (Map<Integer, Site>) list.get(1);
		List<Site> subSiteList = (List<Site>) list.get(2);
		
		if(path != null){
			for(String avoidPath : avoidPathSet){
				if(path.indexOf(avoidPath) > -1){
					flag = false;
					System.out.println("回避路段："+avoidPath);
				}
			}
		}
		
		if(path != null && path.length() > 9){
			for(Site subSite : subSiteList){
				String subSiteNo = subSite.getSiteNo();
				String parnetNo = siteMapOfId.get(subSite.getParentId()).getSiteNo();
				if(path.indexOf(parnetNo) > -1 && path.indexOf(subSiteNo) > -1){
					String[] sites = path.split(",");
					for(int i=0;i<sites.length-2;i++){
						if((subSiteNo.equals(sites[i])&&parnetNo.equals(sites[i+2]))
								||(parnetNo.equals(sites[i])&&subSiteNo.equals(sites[i+2]))){
							System.out.println("主付站："+sites[i]+","+sites[i+1]+","+sites[i+2]);
							flag=false;
						}
					}
				}
			}
		}
		
		//连通性校验
		double weight = 0.0d;
		Map<Integer, Site> siteMapOfNo = (Map<Integer, Site>) list.get(0);
		EdgeWeightedDigraph G = initSiteGraph(siteMapOfId.size());
		String[] sites = path.split(",");
		for(int i=0;i<sites.length-1;i++){
			SiteEdge edge = G.getEdge(siteMapOfNo.get(sites[i]).getId(), 
					siteMapOfNo.get(sites[i+1]).getId());
			if(edge == null){
				System.out.println("连通性："+sites[i]+","+sites[i+1]);
				flag = false;
			}else{
				weight = weight + edge.weight();
			}
		}
		
		System.out.println(weight);
		
		//是否环路
		Set<String> set = new TreeSet<String>();
		for(int i=0;i<sites.length;i++){
			set.add(sites[i]);
		}
		System.out.println("环路个数:"+(sites.length - set.size()));
		return flag;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void exec(String from, String to) {
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
		int vId = siteMapOfNo.get(from).getId();
		int t = siteMapOfNo.get(to).getId();
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
			String path = errorPath.getPath().toString();
			errorPath.setWeight(Double.POSITIVE_INFINITY);
			reComputeOfEdge(errorPath, subSiteList, siteMapOfId, weight, path, G, siteMapOfNo);
			//替换环路 进行 最短路径矫正
//			reComputeOfCycle(errorPath, subSiteList, siteMapOfId, weight, path, G, siteMapOfNo, 0);
			if(errorPath.getWeight() < Double.POSITIVE_INFINITY){
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
								path.insert(0,sourceSite.getSiteNo()+""+siteMapOfId.get(t).getSiteNo()
										+" "+String.format("%.3f", pathWeight)+" ");
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
					String path = errorPath.getPath().toString();
					errorPath.setWeight(Double.POSITIVE_INFINITY);
					reComputeOfEdge(errorPath, subSiteList, siteMapOfId, weight, path, G, siteMapOfNo);
					//替换环路 进行 最短路径矫正
//					reComputeOfCycle(errorPath, subSiteList, siteMapOfId, weight, path, G, siteMapOfNo, 0);
					if(errorPath.getWeight() < Double.POSITIVE_INFINITY){
						errorPath.getPath().insert(0, siteMapOfId.get(errorPath.getFrom()).getSiteNo()
								+""+siteMapOfId.get(errorPath.getTo()).getSiteNo()
								+" "+String.format("%.3f", errorPath.getWeight())+" ");
						logger.info(errorPath.getPath().toString());
					}
				}
			});
		}
		executor.shutdown();
	}

	private void reComputeOfEdge(SitePath errorPath, List<Site> subSiteList, Map<Integer, Site> siteMapOfId,
			double weight, String path, EdgeWeightedDigraph G, Map<Integer, Site> siteMapOfNo){
		if(!checkSubSitePath(subSiteList, siteMapOfId, path)
				|| !checkAvoidPath(path)){
			//去边
			Set<String> edges = getAvoidEdge(path);
			edges.addAll(getSubSiteEdge(subSiteList, siteMapOfId, path));
			for(String e : edges){
				String[] siteNos = e.split(",");
				String from = siteNos[0];
				String to = siteNos[1];
				DijkstraSP dsp = removeEdgeSP(G, siteMapOfNo.get(from).getId(), siteMapOfNo.get(to).getId(), 
						errorPath.getFrom(), siteMapOfId);
				int w = errorPath.getTo();
				if(dsp.hasPathTo(w) && dsp.distTo(w) > 0 && errorPath.getWeight() > dsp.distTo(w)){
					reComputeOfEdge(errorPath, subSiteList, siteMapOfId, dsp.distTo(w), dsp.pathTo(w).toString(), dsp.getG(), 
							siteMapOfNo);
				}
			}
			//加环矫正
			reComputeOfCycle(errorPath, subSiteList, siteMapOfId, weight, path, G, siteMapOfNo, 0);
		}else{
			if(errorPath.getWeight() > weight){
				errorPath.setWeight(weight);
				errorPath.setPath(new StringBuffer(path));
			}
		}
	}
	
	private void reComputeOfCycle(SitePath errorPath, List<Site> subSiteList, Map<Integer, Site> siteMapOfId,
			double weight, String path, EdgeWeightedDigraph G, Map<Integer, Site> siteMapOfNo, int max){
		Set<String> sites = getAvoidSite(path);
		sites.addAll(getSubSite(subSiteList, siteMapOfId, path));
		if(sites.size() == 1){
			for(String s:sites){
				int sIndex = path.indexOf(s);
				int lIndex = path.lastIndexOf(s);
				if(sIndex == lIndex){
					String beforeSiteNo = path.substring(sIndex-5, sIndex-1);
					String afterSiteNo = path.substring(sIndex+5, sIndex+9);
					int v = siteMapOfNo.get(s).getId();
					for(SiteEdge e:G.adj(v)){
						DijkstraSP DSP = new DijkstraSP(G, e.to(), siteMapOfId);
						if(DSP.hasPathTo(v) && DSP.distTo(v) > 0){
							if(max != 0){
								addCycleWithEdgeSP(subSiteList, siteMapOfId, G, siteMapOfNo, errorPath, e, 
										DSP.pathTo(v).toString(), 0.0d, path, weight, s, beforeSiteNo, 
										afterSiteNo, max, new SitePath());
							}else{
								SitePath comparePath = new SitePath();
								for(int j=1;j<17;j++){
									SitePath flagPath = new SitePath();
									addCycleWithEdgeSP(subSiteList, siteMapOfId, G, siteMapOfNo, errorPath, e, 
											DSP.pathTo(v).toString(), 0.0d, path, weight, s, beforeSiteNo, 
											afterSiteNo, j, flagPath);
									if(j == 1){
										comparePath.setPath(errorPath.getPath());
									}else{
										if(comparePath.getPath().toString().equals(errorPath.getPath().toString())
												&& comparePath.getPath().toString().equals(flagPath.getPath().toString())){
											return;
										}else{
											comparePath.setPath(errorPath.getPath());
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private void addCycleWithEdgeSP(List<Site> subSiteList, Map<Integer, Site> siteMapOfId, EdgeWeightedDigraph G,
			Map<Integer, Site> siteMapOfNo, SitePath errorPath, SiteEdge e,
			String cyclePath, double cycleWeight, String originalPath, double originalWeight,
			String cycleSiteNo, String beforeSiteNo, String afterSiteNo, int cycleSize, SitePath flagPath) {
		if(cycleWeight+e.weight()+originalWeight < errorPath.getWeight()){
			if(cyclePath.length() == 9){
				if(cycleSize == 1){
					DijkstraSP dsp = removeEdgeSP(G, e.to(), e.from(), e.to(), siteMapOfId);
					if(dsp.hasPathTo(e.from()) && dsp.distTo(e.from()) > 0){
						addCycleWithEdgeSP(subSiteList, siteMapOfId, dsp.getG(), siteMapOfNo, errorPath, e, 
								dsp.pathTo(e.from()).toString(), dsp.distTo(e.from()), originalPath, originalWeight, 
								cycleSiteNo, beforeSiteNo, afterSiteNo, 0, flagPath);
					}
				}else{
					String[] sites = cyclePath.toString().split(",");
					int v = siteMapOfNo.get(sites[0]).getId();
					for(SiteEdge se:G.adj(v)){
						if(originalPath.indexOf(siteMapOfId.get(se.to()).getSiteNo()) == -1){
							DijkstraSP dsp = new DijkstraSP(G, se.to(), siteMapOfId);
							if(dsp.hasPathTo(v) && dsp.distTo(v) > 0){
								addCycleWithEdgeSP(subSiteList, siteMapOfId, dsp.getG(), siteMapOfNo, errorPath, se, 
										dsp.pathTo(se.from()).toString(), dsp.distTo(se.from()), 
										originalPath.replace(cycleSiteNo, cycleSiteNo+","+cyclePath), 
										originalWeight+e.weight()*2, 
										sites[0], cycleSiteNo, cycleSiteNo, cycleSize-1, flagPath);
							}
						}
					}
				}
			}else if(cyclePath.length() > 9){
				String[] ss = cyclePath.toString().split(",");
				if(beforeSiteNo.equals(ss[0]) || hasSubSite(subSiteList, siteMapOfNo.get(beforeSiteNo).getId(), 
						siteMapOfNo.get(ss[0]).getId())
						|| hasAvoidSite(beforeSiteNo+","+cycleSiteNo+","+ss[0])){
					//放弃这条路
				}else if(afterSiteNo.equals(ss[ss.length-2]) || hasSubSite(subSiteList, siteMapOfNo.get(afterSiteNo).getId(), 
						siteMapOfNo.get(ss[ss.length-2]).getId())
						|| hasAvoidSite(ss[ss.length-2]+","+cycleSiteNo+","+afterSiteNo)){
					//去掉这条边再计算
					String from = ss[ss.length-2];
					String to = cycleSiteNo;
					DijkstraSP dsp = removeEdgeSP(G, siteMapOfNo.get(from).getId(), siteMapOfNo.get(to).getId(), 
							e.to(), siteMapOfId);
					if(dsp.hasPathTo(e.from()) && dsp.distTo(e.from()) > 0){
						addCycleWithEdgeSP(subSiteList, siteMapOfId, dsp.getG(), siteMapOfNo, errorPath, e, 
								dsp.pathTo(e.from()).toString(), dsp.distTo(e.from()), originalPath, originalWeight, cycleSiteNo, 
								beforeSiteNo, afterSiteNo, 0, flagPath);
					}
				}else{
					String originalTemp = originalPath.replace(cycleSiteNo, cycleSiteNo+","+cyclePath);
					if(originalTemp.length() < 360){
						Set<String> edges = new TreeSet<String>();
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
								if(dsp.hasPathTo(e.from()) && dsp.distTo(e.from()) > 0){
									addCycleWithEdgeSP(subSiteList, siteMapOfId, dsp.getG(), siteMapOfNo, errorPath, e, 
											dsp.pathTo(e.from()).toString(), dsp.distTo(e.from()), originalPath, originalWeight, cycleSiteNo, 
											beforeSiteNo, afterSiteNo, 0, flagPath);
								}
							}
							
							reComputeOfCycle(errorPath, subSiteList, siteMapOfId, 
									originalWeight+cycleWeight+e.weight(), originalTemp, 
									G, siteMapOfNo, 1);
							reComputeOfCycle(errorPath, subSiteList, siteMapOfId, 
									originalWeight+cycleWeight+e.weight(), originalTemp, 
									G, siteMapOfNo, 16);
						}else{
							if(checkAvoidPath(originalTemp) && checkSubSitePath(subSiteList, siteMapOfId, originalTemp)){
								if(errorPath.getWeight() > originalWeight+cycleWeight+e.weight()){
									errorPath.setWeight(originalWeight+cycleWeight+e.weight());
									errorPath.setPath(new StringBuffer(originalTemp));
									flagPath.setPath(new StringBuffer(originalTemp));
								}
							}
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
