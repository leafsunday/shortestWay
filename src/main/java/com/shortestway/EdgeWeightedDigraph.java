package com.shortestway;

import java.util.Stack;

public class EdgeWeightedDigraph {
    private final int V;
    private int E;
    private Bag<SiteEdge>[] adj;
    
    /**
     * Initializes an empty edge-weighted digraph with <tt>V</tt> vertices and 0 edges.
     * param V the number of vertices
     * @throws java.lang.IllegalArgumentException if <tt>V</tt> < 0
     */
    @SuppressWarnings("unchecked")
	public EdgeWeightedDigraph(int V) {
        if (V < 0) throw new IllegalArgumentException("Number of vertices in a Digraph must be nonnegative");
        this.V = V;
        this.E = 0;
        adj = (Bag<SiteEdge>[]) new Bag[V];
        for (int v = 0; v < V; v++)
            adj[v] = new Bag<SiteEdge>();
    }

    /**
     * Initializes a new edge-weighted digraph that is a deep copy of <tt>G</tt>.
     * @param G the edge-weighted graph to copy
     */
    public EdgeWeightedDigraph(EdgeWeightedDigraph G) {
        this(G.V());
        this.E = G.E();
        for (int v = 0; v < G.V(); v++) {
            // reverse so that adjacency list is in same order as original
            Stack<SiteEdge> reverse = new Stack<SiteEdge>();
            for (SiteEdge e : G.adj[v]) {
                reverse.push(e);
            }
            for (SiteEdge e : reverse) {
                adj[v].add(e);
            }
        }
    }

    /**
     * Returns the number of vertices in the edge-weighted digraph.
     * @return the number of vertices in the edge-weighted digraph
     */
    public int V() {
        return V;
    }

    /**
     * Returns the number of edges in the edge-weighted digraph.
     * @return the number of edges in the edge-weighted digraph
     */
    public int E() {
        return E;
    }

    /**
     * Adds the directed edge <tt>e</tt> to the edge-weighted digraph.
     * @param e the edge
     */
    public void addEdge(SiteEdge e) {
        int v = e.from();
        adj[v].add(e);
        E++;
    }
    
    public void removeEdge(int v, int w){
    	for(SiteEdge e : adj[v]){
    		if(e.to() == w){
    			adj[v].remove(e);
    			E--;
    			break;
    		}
    	}
    }
    
    public void removeSite(int v){
    	//去除以v开始的边
    	for(SiteEdge e : adj[v]){
    		adj[v].remove(e);
    		E--;
    	}
    	//去除以v结尾的边
    	for(int i=0;i<adj.length;i++){
    		for(SiteEdge e : adj[i]){
        		if(e.to() == v){
        			adj[v].remove(e);
        			E--;
        			break;
        		}
        	}
    	}
    }
    
    public SiteEdge getEdge(int v, int w){
    	for(SiteEdge e : adj[v]){
    		if(e.to() == w){
    			return e;
    		}
    	}
    	return null;
    }

    /**
     * Returns the directed edges incident from vertex <tt>v</tt>.
     * @return the directed edges incident from vertex <tt>v</tt> as an Iterable
     * @param v the vertex
     * @throws java.lang.IndexOutOfBoundsException unless 0 <= v < V
     */
    public Iterable<SiteEdge> adj(int v) {
        if (v < 0 || v >= V) throw new IndexOutOfBoundsException("vertex " + v + " is not between 0 and " + (V-1));
        return adj[v];
    }

    /**
     * Returns all directed edges in the edge-weighted digraph.
     * To iterate over the edges in the edge-weighted graph, use foreach notation:
     * <tt>for (DirectedEdge e : G.edges())</tt>.
     * @return all edges in the edge-weighted graph as an Iterable.
     */
    public Iterable<SiteEdge> edges() {
        Bag<SiteEdge> list = new Bag<SiteEdge>();
        for (int v = 0; v < V; v++) {
            for (SiteEdge e : adj(v)) {
                list.add(e);
            }
        }
        return list;
    } 

    /**
     * Returns the number of directed edges incident from vertex <tt>v</tt>.
     * This is known as the <em>outdegree</em> of vertex <tt>v</tt>.
     * @return the number of directed edges incident from vertex <tt>v</tt>
     * @param v the vertex
     * @throws java.lang.IndexOutOfBoundsException unless 0 <= v < V
     */
    public int outdegree(int v) {
        if (v < 0 || v >= V) throw new IndexOutOfBoundsException("vertex " + v + " is not between 0 and " + (V-1));
        return adj[v].size();
    }

    /**
     * Returns a string representation of the edge-weighted digraph.
     * This method takes time proportional to <em>E</em> + <em>V</em>.
     * @return the number of vertices <em>V</em>, followed by the number of edges <em>E</em>,
     *   followed by the <em>V</em> adjacency lists of edges
     */
    public String toString() {
        String NEWLINE = System.getProperty("line.separator");
        StringBuilder s = new StringBuilder();
        s.append(V + " " + E + NEWLINE);
        for (int v = 0; v < V; v++) {
            s.append(v + ": ");
            for (SiteEdge e : adj[v]) {
                s.append(e + "  ");
            }
            s.append(NEWLINE);
        }
        return s.toString();
    }
}
