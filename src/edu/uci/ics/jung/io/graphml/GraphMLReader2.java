/*
 * Copyright (c) 2008, The JUNG Authors
 *
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * https://github.com/jrtom/jung/blob/master/LICENSE for a description.
 */

package edu.uci.ics.jung.io.graphml;

import com.google.common.base.Function;
import edu.uci.ics.jung.graph.Hypergraph;
import edu.uci.ics.jung.io.GraphIOException;
import edu.uci.ics.jung.io.GraphReader;
import edu.uci.ics.jung.io.graphml.parser.ElementParserRegistry;
import edu.uci.ics.jung.io.graphml.parser.GraphMLEventFilter;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.IOException;
import java.io.Reader;

/**
 * Reads in data from a GraphML-formatted file and generates graphs based on
 * that data. Does not currently support nested graphs.
 * 
 * <p>Note that the user is responsible for supplying a graph
 * <code>Transformer</code> that will create graphs capable of supporting the
 * edge types in the supplied GraphML file. If the graph generated by the
 * <code>Factory</code> is not compatible (for example: if the graph does not
 * accept directed edges, and the GraphML file contains a directed edge) then
 * the results are graph-implementation-dependent.
 *
 * @author Nathan Mittler - nathan.mittler@gmail.com
 * 
 * @param <G>
 * The graph type to be read from the GraphML file
 * @param <V> the vertex type
 * The vertex type used by the graph
 * @param <V> the edge type
 * The edge type used by the graph
 * @see "http://graphml.graphdrawing.org/specification.html"
 */
public class GraphMLReader2<G extends Hypergraph<V, E>, V, E> implements
        GraphReader<G, V, E> {

    protected XMLEventReader xmlEventReader;
    protected Reader fileReader;
    protected Function<GraphMetadata, G> graphTransformer;
    protected Function<NodeMetadata, V> vertexTransformer;
    protected Function<EdgeMetadata, E> edgeTransformer;
    protected Function<HyperEdgeMetadata, E> hyperEdgeTransformer;
    protected boolean initialized;
    final protected GraphMLDocument document = new GraphMLDocument();
    final protected ElementParserRegistry<G,V,E> parserRegistry;

    /**
     * Constructs a GraphML reader around the given reader. This constructor
     * requires the user to supply transformation functions to convert from the
     * GraphML metadata to Graph, Vertex, Edge instances. These Function
     * functions can be used as purely factories (i.e. the metadata is
     * disregarded) or can use the metadata to set particular fields in the
     * objects.
     *
     * @param fileReader           the reader for the input GraphML document.
     * @param graphTransformer     Transformation function to convert from GraphML GraphMetadata
     *                             to graph objects. This must be non-null.
     * @param vertexTransformer    Transformation function to convert from GraphML NodeMetadata
     *                             to vertex objects. This must be non-null.
     * @param edgeTransformer      Transformation function to convert from GraphML EdgeMetadata
     *                             to edge objects. This must be non-null.
     * @param hyperEdgeTransformer Transformation function to convert from GraphML
     *                             HyperEdgeMetadata to edge objects. This must be non-null.
     * @throws IllegalArgumentException thrown if any of the arguments are null.
     */
    public GraphMLReader2(Reader fileReader,
                          Function<GraphMetadata, G> graphTransformer,
                          Function<NodeMetadata, V> vertexTransformer,
                          Function<EdgeMetadata, E> edgeTransformer,
                          Function<HyperEdgeMetadata, E> hyperEdgeTransformer) {

        if (fileReader == null) {
            throw new IllegalArgumentException(
                    "Argument fileReader must be non-null");
        }        
        
        if (graphTransformer == null) {
            throw new IllegalArgumentException(
            "Argument graphTransformer must be non-null");
        }        

        if (vertexTransformer == null) {
            throw new IllegalArgumentException(
                    "Argument vertexTransformer must be non-null");
        }        
        
        if (edgeTransformer == null) {
            throw new IllegalArgumentException(
                    "Argument edgeTransformer must be non-null");
        }        
        
        if (hyperEdgeTransformer == null) {
            throw new IllegalArgumentException(
                    "Argument hyperEdgeTransformer must be non-null");
        }
        
        this.fileReader = fileReader;
        this.graphTransformer = graphTransformer;
        this.vertexTransformer = vertexTransformer;
        this.edgeTransformer = edgeTransformer;
        this.hyperEdgeTransformer = hyperEdgeTransformer;
        
        // Create the parser registry.
        this.parserRegistry = new ElementParserRegistry<G,V,E>(document.getKeyMap(), 
                graphTransformer, vertexTransformer, edgeTransformer, hyperEdgeTransformer);
    }

    /**
     * Gets the current Function that is being used for graph objects.
     *
     * @return the current Function.
     */
    public Function<GraphMetadata, G> getGraphTransformer() {
        return graphTransformer;
    }

    /**
     * Gets the current Function that is being used for vertex objects.
     *
     * @return the current Function.
     */
    public Function<NodeMetadata, V> getVertexTransformer() {
        return vertexTransformer;
    }

    /**
     * Gets the current Function that is being used for edge objects.
     *
     * @return the current Function.
     */
    public Function<EdgeMetadata, E> getEdgeTransformer() {
        return edgeTransformer;
    }

    /**
     * Gets the current Function that is being used for hyperedge objects.
     *
     * @return the current Function.
     */
    public Function<HyperEdgeMetadata, E> getHyperEdgeTransformer() {
        return hyperEdgeTransformer;
    }

    /**
     * Verifies the object state and initializes this reader. All Function
     * properties must be set and be non-null or a <code>GraphReaderException
     * </code> will be thrown. This method may be called more than once.
     * Successive calls will have no effect.
     *
     * @throws edu.uci.ics.jung.io.GraphIOException thrown if an error occurred.
     */
    public void init() throws GraphIOException {

        try {

            if (!initialized) {

                // Create the event reader.
                XMLInputFactory Supplier = XMLInputFactory.newInstance();
                xmlEventReader = Supplier.createXMLEventReader(fileReader);
                xmlEventReader = Supplier.createFilteredReader(xmlEventReader,
                        new GraphMLEventFilter());

                initialized = true;
            }

        } catch( Exception e ) {
            ExceptionConverter.convert(e);
        }
    }

    /**
     * Closes the GraphML reader and disposes of any resources.
     *
     * @throws edu.uci.ics.jung.io.GraphIOException thrown if an error occurs.
     */
    public void close() throws GraphIOException {
        try {

            // Clear the contents of the document.
            document.clear();

            if (xmlEventReader != null) {
                xmlEventReader.close();
            }

            if (fileReader != null) {
                fileReader.close();
            }

        } catch (IOException e) {
            throw new GraphIOException(e);
        } catch (XMLStreamException e) {
            throw new GraphIOException(e);
        } finally {
            fileReader = null;
            xmlEventReader = null;
            graphTransformer = null;
            vertexTransformer = null;
            edgeTransformer = null;
            hyperEdgeTransformer = null;
        }
    }

    /**
     * Returns the object that contains the metadata read in from the GraphML
     * document
     *
     * @return the GraphML document
     */
    public GraphMLDocument getGraphMLDocument() {
        return document;
    }

    /**
     * Reads a single graph object from the GraphML document. Automatically
     * calls <code>init</code> to initialize the state of the reader.
     *
     * @return the graph that was read if one was found, otherwise null.
     */
    @SuppressWarnings("unchecked")
    public G readGraph() throws GraphIOException {

        try {

            // Initialize if not already.
            init();

            while (xmlEventReader.hasNext()) {

                XMLEvent event = xmlEventReader.nextEvent();
                if (event.isStartElement()) {
                    StartElement element = (StartElement) event;
                    String name = element.getName().getLocalPart();

                    // The element should be one of: key, graph, graphml
                    if (GraphMLConstants.KEY_NAME.equals(name)) {

                        // Parse the key object.
                        Key key = (Key) parserRegistry.getParser(name).parse(
                                xmlEventReader, element);

                        // Add the key to the key map.
                        document.getKeyMap().addKey(key);

                    } else if (GraphMLConstants.GRAPH_NAME.equals(name)) {

                        // Parse the graph.
                        GraphMetadata graph = (GraphMetadata) parserRegistry
                                .getParser(name).parse(xmlEventReader, element);

                        // Add it to the graph metadata list.
                        document.getGraphMetadata().add(graph);
                        
                        // Return the graph object.
                        return (G)graph.getGraph();

                    } else if (GraphMLConstants.GRAPHML_NAME.equals(name)) {
                        // Ignore the graphML object.
                    } else {

                        // Encounted an unknown element - just skip by it.
                        parserRegistry.getUnknownElementParser().parse(
                                xmlEventReader, element);
                    }

                } else if (event.isEndDocument()) {
                    break;
                }
            }

        } catch (Exception e) {
            ExceptionConverter.convert(e);
        }

        // We didn't read anything from the document.
        throw new GraphIOException("Unable to read Graph from document - the document could be empty");
    }    
}