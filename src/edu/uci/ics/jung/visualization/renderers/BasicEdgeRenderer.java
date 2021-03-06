/*
 * Copyright (c) 2005, The JUNG Authors
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either "license.txt"
 * or https://github.com/jrtom/jung/blob/master/LICENSE for a description.
 *
 * Created on Aug 23, 2005
 */
package edu.uci.ics.jung.visualization.renderers;

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Context;
import edu.uci.ics.jung.graph.util.EdgeIndexFunction;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.graph.util.Pair;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.RenderContext;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.decorators.ParallelEdgeShapeTransformer;
import edu.uci.ics.jung.visualization.transform.LensTransformer;
import edu.uci.ics.jung.visualization.transform.MutableTransformer;
import edu.uci.ics.jung.visualization.transform.shape.GraphicsDecorator;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class BasicEdgeRenderer<V,E> implements Renderer.Edge<V,E> {
	
	protected EdgeArrowRenderingSupport<V,E> edgeArrowRenderingSupport =
		new BasicEdgeArrowRenderingSupport<V,E>();

    public void paintEdge(RenderContext<V,E> rc, Layout<V, E> layout, E e) {
        GraphicsDecorator g2d = rc.getGraphicsContext();
        Graph<V,E> graph = layout.getGraph();
        if (!rc.getEdgeIncludePredicate().apply(Context.getInstance(graph,e)))
            return;
        
        // don't draw edge if either incident vertex is not drawn
        Pair<V> endpoints = graph.getEndpoints(e);
        V v1 = endpoints.getFirst();
        V v2 = endpoints.getSecond();
        if (!rc.getVertexIncludePredicate().apply(Context.getInstance(graph,v1)) ||
            !rc.getVertexIncludePredicate().apply(Context.getInstance(graph,v2)))
            return;
        
        Stroke new_stroke = rc.getEdgeStrokeTransformer().apply(e);
        Stroke old_stroke = g2d.getStroke();
        if (new_stroke != null)
            g2d.setStroke(new_stroke);
        
        drawSimpleEdge(rc, layout, e);

        // restore paint and stroke
        if (new_stroke != null)
            g2d.setStroke(old_stroke);

    }

	protected Shape prepareFinalEdgeShape(RenderContext<V,E> rc, Layout<V, E> layout, E e, int[] coords, boolean[] loop) {
        Graph<V,E> graph = layout.getGraph();
        Pair<V> endpoints = graph.getEndpoints(e);
        V v1 = endpoints.getFirst();
        V v2 = endpoints.getSecond();
        
        Point2D p1 = layout.apply(v1);
        Point2D p2 = layout.apply(v2);
        p1 = rc.getMultiLayerTransformer().transform(Layer.LAYOUT, p1);
        p2 = rc.getMultiLayerTransformer().transform(Layer.LAYOUT, p2);
        float x1 = (float) p1.getX();
        float y1 = (float) p1.getY();
        float x2 = (float) p2.getX();
        float y2 = (float) p2.getY();
        coords[0] = (int)x1;
        coords[1] = (int)y1;
        coords[2] = (int)x2;
        coords[3] = (int)y2;
        
        boolean isLoop = loop[0] = v1.equals(v2);
        Shape s2 = rc.getVertexShapeTransformer().apply(v2);
        Shape edgeShape = rc.getEdgeShapeTransformer().apply(e);
        
        AffineTransform xform = AffineTransform.getTranslateInstance(x1, y1);
        
        if(isLoop) {
            // this is a self-loop. scale it is larger than the vertex
            // it decorates and translate it so that its nadir is
            // at the center of the vertex.
            Rectangle2D s2Bounds = s2.getBounds2D();
            xform.scale(s2Bounds.getWidth(),s2Bounds.getHeight());
            xform.translate(0, -edgeShape.getBounds2D().getWidth()/2);
        } else if(rc.getEdgeShapeTransformer() instanceof EdgeShape.Orthogonal) {
            float dx = x2-x1;
            float dy = y2-y1;
            int index = 0;
            if(rc.getEdgeShapeTransformer() instanceof ParallelEdgeShapeTransformer) {
				@SuppressWarnings("unchecked")
				EdgeIndexFunction<V,E> peif =
					((ParallelEdgeShapeTransformer<V,E>)rc.getEdgeShapeTransformer())
						.getEdgeIndexFunction();
            	index = peif.getIndex(null, e);
            	index *= 20;
            }
            GeneralPath gp = new GeneralPath();
            gp.moveTo(0,0);// the xform will do the translation to x1,y1
            if(x1 > x2) {
            	if(y1 > y2) {
            		gp.lineTo(0, index);
            		gp.lineTo(dx-index, index);
            		gp.lineTo(dx-index, dy);
            		gp.lineTo(dx, dy);
            	} else {
            		gp.lineTo(0, -index);
            		gp.lineTo(dx-index, -index);
            		gp.lineTo(dx-index, dy);
            		gp.lineTo(dx, dy);
            	}

            } else {
            	if(y1 > y2) {
            		gp.lineTo(0, index);
            		gp.lineTo(dx+index, index);
            		gp.lineTo(dx+index, dy);
            		gp.lineTo(dx, dy);
            		
            	} else {
            		gp.lineTo(0, -index);
            		gp.lineTo(dx+index, -index);
            		gp.lineTo(dx+index, dy);
            		gp.lineTo(dx, dy);
            		
            	}
            	
            }

            edgeShape = gp;
        	
        } else {
            // this is a normal edge. Rotate it to the angle between
            // vertex endpoints, then scale it to the distance between
            // the vertices
            float dx = x2-x1;
            float dy = y2-y1;
            float thetaRadians = (float) Math.atan2(dy, dx);
            xform.rotate(thetaRadians);
            float dist = (float) Math.sqrt(dx*dx + dy*dy);
            xform.scale(dist, 1.0);
        }
        
        edgeShape = xform.createTransformedShape(edgeShape);
        
        return edgeShape;
	}

	/**
     * Draws the edge <code>e</code>, whose endpoints are at <code>(x1,y1)</code>
     * and <code>(x2,y2)</code>, on the graphics context <code>g</code>.
     * The <code>Shape</code> provided by the <code>EdgeShapeFunction</code> instance
     * is scaled in the x-direction so that its width is equal to the distance between
     * <code>(x1,y1)</code> and <code>(x2,y2)</code>.
     * @param rc the render context used for rendering the edge
     * @param layout the layout instance which provides the edge's endpoints' coordinates
     * @param e the edge to be drawn
     */
    protected void drawSimpleEdge(RenderContext<V,E> rc, Layout<V,E> layout, E e) {
    	
    	int[] coords = new int[4];
    	boolean[] loop = new boolean[1];
    	Shape edgeShape = prepareFinalEdgeShape(rc, layout, e, coords, loop);
    	
    	int x1 = coords[0];
    	int y1 = coords[1];
    	int x2 = coords[2];
    	int y2 = coords[3];
    	boolean isLoop = loop[0];
        
        GraphicsDecorator g = rc.getGraphicsContext();
        Graph<V,E> graph = layout.getGraph();
        boolean edgeHit = true;
        boolean arrowHit = true;
        Rectangle deviceRectangle = null;
        JComponent vv = rc.getScreenDevice();
        if(vv != null) {
            Dimension d = vv.getSize();
            deviceRectangle = new Rectangle(0,0,d.width,d.height);
        }
        
        MutableTransformer vt = rc.getMultiLayerTransformer().getTransformer(Layer.VIEW);
        if(vt instanceof LensTransformer) {
        	vt = ((LensTransformer)vt).getDelegate();
        }
        edgeHit = vt.transform(edgeShape).intersects(deviceRectangle);

        if(edgeHit == true) {
            
            Paint oldPaint = g.getPaint();
            
            // get Paints for filling and drawing
            // (filling is done first so that drawing and segment use same Paint)
            Paint fill_paint = rc.getEdgeFillPaintTransformer().apply(e); 
            if (fill_paint != null)
            {
                g.setPaint(fill_paint);
                g.fill(edgeShape);
            }
            Paint draw_paint = rc.getEdgeDrawPaintTransformer().apply(e);
            if (draw_paint != null)
            {
                g.setPaint(draw_paint);
                g.draw(edgeShape);
            }
            
            float scalex = (float)g.getTransform().getScaleX();
            float scaley = (float)g.getTransform().getScaleY();
            // see if arrows are too small to bother drawing
            if(scalex < .3 || scaley < .3) return;
            
            if (rc.getEdgeArrowPredicate().apply(Context.getInstance(graph, e))) {
            	
                Stroke new_stroke = rc.getEdgeArrowStrokeTransformer().apply(e);
                Stroke old_stroke = g.getStroke();
                if (new_stroke != null)
                    g.setStroke(new_stroke);

                
                Shape destVertexShape = 
                    rc.getVertexShapeTransformer().apply(graph.getEndpoints(e).getSecond());

                AffineTransform xf = AffineTransform.getTranslateInstance(x2, y2);
                destVertexShape = xf.createTransformedShape(destVertexShape);
                
                arrowHit = rc.getMultiLayerTransformer().getTransformer(Layer.VIEW).transform(destVertexShape).intersects(deviceRectangle);
                if(arrowHit) {
                    
                    AffineTransform at = 
                        edgeArrowRenderingSupport.getArrowTransform(rc, edgeShape, destVertexShape);
                    if(at == null) return;
                    Shape arrow = rc.getEdgeArrowTransformer().apply(Context.getInstance(graph, e));
                    arrow = at.createTransformedShape(arrow);
                    g.setPaint(rc.getArrowFillPaintTransformer().apply(e));
                    g.fill(arrow);
                    g.setPaint(rc.getArrowDrawPaintTransformer().apply(e));
                    g.draw(arrow);
                }
                if (graph.getEdgeType(e) == EdgeType.UNDIRECTED) {
                    Shape vertexShape = 
                        rc.getVertexShapeTransformer().apply(graph.getEndpoints(e).getFirst());
                    xf = AffineTransform.getTranslateInstance(x1, y1);
                    vertexShape = xf.createTransformedShape(vertexShape);
                    
                    arrowHit = rc.getMultiLayerTransformer().getTransformer(Layer.VIEW).transform(vertexShape).intersects(deviceRectangle);
                    
                    if(arrowHit) {
                        AffineTransform at = edgeArrowRenderingSupport.getReverseArrowTransform(rc, edgeShape, vertexShape, !isLoop);
                        if(at == null) return;
                        Shape arrow = rc.getEdgeArrowTransformer().apply(Context.getInstance(graph, e));
                        arrow = at.createTransformedShape(arrow);
                        g.setPaint(rc.getArrowFillPaintTransformer().apply(e));
                        g.fill(arrow);
                        g.setPaint(rc.getArrowDrawPaintTransformer().apply(e));
                        g.draw(arrow);
                    }
                }
                // restore paint and stroke
                if (new_stroke != null)
                    g.setStroke(old_stroke);

            }
            
            // restore old paint
            g.setPaint(oldPaint);
        }
    }

	public EdgeArrowRenderingSupport<V, E> getEdgeArrowRenderingSupport() {
		return edgeArrowRenderingSupport;
	}

	public void setEdgeArrowRenderingSupport(
			EdgeArrowRenderingSupport<V, E> edgeArrowRenderingSupport) {
		this.edgeArrowRenderingSupport = edgeArrowRenderingSupport;
	}
}
