package edu.uci.ics.jung.visualization;

import edu.uci.ics.jung.visualization.transform.BidirectionalTransformer;
import edu.uci.ics.jung.visualization.transform.MutableTransformer;
import edu.uci.ics.jung.visualization.transform.shape.ShapeTransformer;
import edu.uci.ics.jung.visualization.util.ChangeEventSupport;

import java.awt.*;
import java.awt.geom.Point2D;

public interface MultiLayerTransformer extends BidirectionalTransformer, ShapeTransformer, ChangeEventSupport {

	
	void setTransformer(Layer layer, MutableTransformer Function);

	MutableTransformer getTransformer(Layer layer);

	Point2D inverseTransform(Layer layer, Point2D p);

	Point2D transform(Layer layer, Point2D p);

	Shape transform(Layer layer, Shape shape);
	
	Shape inverseTransform(Layer layer, Shape shape);

	void setToIdentity();

}