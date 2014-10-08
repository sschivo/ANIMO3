package animo.cytoscape;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class AnimoPropertyChangeListener {

	public class ColorsListener implements ChangeListener {
		@Override
		public void stateChanged(ChangeEvent e) {
			legendColors.updateFromSettings();
		}
	}

	public class ShapesListener implements ChangeListener {
		@Override
		public void stateChanged(ChangeEvent arg0) {
			legendShapes.updateFromSettings();
		}
	}

	// private int currentEdgeNumber = -1, currentNodeNumber = -1;
	// private Object[] edgesArray = null;
	private ColorsLegend legendColors;

	private ShapesLegend legendShapes;

	private ColorsListener colorsListener = null;
	private ShapesListener shapesListener = null;

	public AnimoPropertyChangeListener(ColorsLegend legendColors, ShapesLegend legendShapes) {
		this.legendColors = legendColors;
		this.legendShapes = legendShapes;
	}

	public ColorsListener getColorsListener() {
		if (colorsListener == null) {
			colorsListener = new ColorsListener();
		}
		return this.colorsListener;
	}

	public ShapesListener getShapesListener() {
		if (shapesListener == null) {
			shapesListener = new ShapesListener();
		}
		return this.shapesListener;
	}
}
