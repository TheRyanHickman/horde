/*
 * Created on Oct 18, 2005
 *
 * Copyright (c) 2005, The JUNG Authors 
 *
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * https://github.com/jrtom/jung/blob/master/LICENSE for a description.
 */
package edu.uci.ics.jung.graph;

import com.google.common.base.Supplier;
import com.google.common.collect.Ordering;
import edu.uci.ics.jung.graph.util.Pair;

import java.util.*;

/**
 * An implementation of <code>Graph</code> that is suitable for sparse graphs,
 * orders its vertex and edge collections according to either specified <code>Comparator</code>
 * instances or the natural ordering of their elements, and permits directed, undirected,
 * and parallel edges. 
 * 
 * @author Joshua O'Madadhain
 */
@SuppressWarnings("serial")
public class SortedSparseMultigraph<V,E> 
    extends OrderedSparseMultigraph<V,E>
    implements MultiGraph<V,E> 
{
    /**
     * @param <V> the vertex type for the graph Supplier
     * @param <E> the edge type for the graph Supplier
     * @return a {@code Supplier} that creates an instance of this graph type.
     */
	public static <V,E> Supplier<Graph<V,E>> getFactory() 
	{ 
		return new Supplier<Graph<V,E>> () 
		{
			public Graph<V,E> get() 
			{
				return new SortedSparseMultigraph<V,E>();
			}
		};
	}
    
    /**
     * <code>Comparator</code> used in ordering vertices.  Defaults to <code>util.ComparableComparator</code>
     * if no comparators are specified in the constructor.
     */
    protected Comparator<V> vertex_comparator;

    /**
     * <code>Comparator</code> used in ordering edges.  Defaults to <code>util.ComparableComparator</code>
     * if no comparators are specified in the constructor.
     */
    protected Comparator<E> edge_comparator;
    
    /**
     * Creates a new instance which sorts its vertices and edges according to the 
     * specified {@code Comparator}s.
     * @param vertex_comparator specifies how the vertices are to be compared 
     * @param edge_comparator specifies how the edges are to be compared
     */
    public SortedSparseMultigraph(Comparator<V> vertex_comparator, Comparator<E> edge_comparator)
    {
        this.vertex_comparator = vertex_comparator;
        this.edge_comparator = edge_comparator;
        vertices = new TreeMap<V, Pair<Set<E>>>(vertex_comparator);
        edges = new TreeMap<E, Pair<V>>(edge_comparator);
        directedEdges = new TreeSet<E>(edge_comparator);
    }
    
    /**
     * Creates a new instance which sorts its vertices and edges according to 
     * their natural ordering.
     */
    public SortedSparseMultigraph()
    {
        this(new Ordering<V>(){
			@SuppressWarnings("unchecked")
			public int compare(V v1, V v2) {
				return ((Comparable<V>) v1).compareTo(v2);
			}},
        		new Ordering<E>(){
					@SuppressWarnings("unchecked")
					public int compare(E e1, E e2) {
						return ((Comparable<E>) e1).compareTo(e2);
					}});
    }

    /**
     * Provides a new {@code Comparator} to be used in sorting the vertices.
     * @param vertex_comparator the comparator that defines the new ordering
     */
    public void setVertexComparator(Comparator<V> vertex_comparator)
    {
        this.vertex_comparator = vertex_comparator;
        Map<V, Pair<Set<E>>> tmp_vertices = new TreeMap<V, Pair<Set<E>>>(vertex_comparator);
        for (Map.Entry<V, Pair<Set<E>>> entry : vertices.entrySet())
            tmp_vertices.put(entry.getKey(), entry.getValue());
        this.vertices = tmp_vertices;
    }
    
    @Override
    public boolean addVertex(V vertex) {
        if(vertex == null) {
            throw new IllegalArgumentException("vertex may not be null");
        }
        if (!containsVertex(vertex)) 
        {
            vertices.put(vertex, new Pair<Set<E>>(new TreeSet<E>(edge_comparator), 
                new TreeSet<E>(edge_comparator)));
            return true;
        } 
        else 
        {
        	return false;
        }
    }
}
