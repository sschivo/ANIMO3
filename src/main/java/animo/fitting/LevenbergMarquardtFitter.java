package animo.fitting;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import org.ejml.data.DenseMatrix64F;

import animo.core.graph.Graph;
import animo.core.model.Model;
import animo.core.model.Reactant;
import animo.core.model.Reaction;
import animo.cytoscape.Animo;
import animo.cytoscape.RunAction;
import animo.util.LevenbergMarquardt;
import animo.util.LevenbergMarquardt.Function;

public class LevenbergMarquardtFitter {
	
	private Model model = null;
	private List<Reaction> reactionsToBeOptimized = null;
	private String referenceDataFile = null;
	private Map<Reactant, String> reactantToDataCorrespondence = null;
	private int timeTo = -1;
	private Properties parameters = null;
	public static final String MIN_COST_KEY = "MIN_COST",
							   DELTA_KEY = "DELTA";
	private final double MIN_COST_DEFAULT = 0.5;
	private final double DELTA_DEFAULT = 0.0001;
	
	public LevenbergMarquardtFitter() {
		
	}
	
	/**
	 * Create a parameter fitter based on the Levenberg-Marquardt method
	 * @param model The ANIMO model to be optimized
	 * @param reactionsToBeOptimized Which reactions we are allowed to change
	 * @param referenceDataFile The experimental data file (.csv) against which the selected nodes will be compared
	 * @param reactantToDataCorrespondence Which reactants (in the ANIMO model) correspond to which data series in the reference file.
	 * 		  The comparison of the model results on those nodes against the corresponding series in the file will give the model fitness to data. 
	 * @param timeTo Time (in minutes) until which the simulations have to run (should be <= last time point in the reference data file)
	 */
	public LevenbergMarquardtFitter(Model model,
									List<Reaction> reactionsToBeOptimized,
									String referenceDataFile,
									Map<Reactant, String> reactantToDataCorrespondence,
									int timeTo,
									Properties parameters) {
		this.model = model;
		this.reactionsToBeOptimized = reactionsToBeOptimized;
		this.referenceDataFile = referenceDataFile;
		this.reactantToDataCorrespondence = reactantToDataCorrespondence;
		this.timeTo = timeTo;
		this.parameters = parameters;
	}
	
	public void performParameterFitting() {
		DenseMatrix64F experimentalData = null;
		try {
			experimentalData = LevenbergMarquardt.readCSVtoMatrix(referenceDataFile, reactantToDataCorrespondence.values(), timeTo);
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
		
		final Graph graph = new Graph();
    	final JFrame frame = new JFrame();
    	frame.getContentPane().add(graph, BorderLayout.CENTER);
    	final JProgressBar progressBar = new JProgressBar(0, 100),
    					   bestProgressBar = new JProgressBar(0, 100);
    	progressBar.setString("Current fitness");
    	progressBar.setStringPainted(true);
    	bestProgressBar.setString("Best fitness up to now");
    	bestProgressBar.setStringPainted(true);
    	Box progressBox = new Box(BoxLayout.Y_AXIS);
    	progressBox.add(progressBar);
    	progressBox.add(bestProgressBar);
    	frame.getContentPane().add(progressBox, BorderLayout.SOUTH);
    	JFrame cytoscapeFrame = Animo.getCytoscape().getJFrame();
    	frame.setBounds((int) (cytoscapeFrame.getWidth() * 0.2), (int) (cytoscapeFrame.getHeight() * 0.2), (int) (cytoscapeFrame.getWidth() * 0.6), (int) (cytoscapeFrame.getHeight() * 0.6));
    	//100, 100, 600, 400);
    	frame.setVisible(true);
    	final LevenbergMarquardtFunction function = new LevenbergMarquardtFunction(graph, model, reactionsToBeOptimized, referenceDataFile, reactantToDataCorrespondence, timeTo);
    	final LevenbergMarquardt lm = new LevenbergMarquardt(function);
    	lm.MIN_COST = MIN_COST_DEFAULT;
    	lm.DELTA = DELTA_DEFAULT;
    	if (parameters.containsKey(MIN_COST_KEY)) {
    		double val = 0;
    		try {
    			val = Double.parseDouble(parameters.getProperty(MIN_COST_KEY));
    			lm.MIN_COST = val;
    		} catch (NumberFormatException ex) {
    			ex.printStackTrace(System.err);
    		}
    	}
    	if (parameters.containsKey(DELTA_KEY)) {
    		double val = 0;
    		try {
    			val = Double.parseDouble(parameters.getProperty(DELTA_KEY));
    			lm.DELTA = val;
    		} catch (NumberFormatException ex) {
    			ex.printStackTrace(System.err);
    		}
    	}
    	final DenseMatrix64F initParam, X, Y;
    	initParam = function.getInitialParameters(); //Automatically gets the parameters from the model's reactions we are going to optimize (and also establishes an ordering that will remain the same for the whole experiment)
    	double dataX[][] = new double[experimentalData.getData().length][1];
    	for (int i = 0; i < dataX.length; i++) {
    		for (int j = 0; j < dataX[i].length; j++) {
    			dataX[i][j] = 0;
    		}
    	}
    	X = new DenseMatrix64F(dataX);
    	Y = experimentalData;
    	
    	final LMSwingWorker worker = new LMSwingWorker(lm, X, Y, initParam);
    	JButton chiudi = new JButton("Chiudi");
    	chiudi.addActionListener(new ActionListener() {
    		public void actionPerformed(ActionEvent e) {
    			worker.setMustTerminate(true); //Just push the button to cancel the job
    			//For some reason, listening for the window close does not work o_O
    		}
    	});
    	frame.add(chiudi, BorderLayout.EAST);
    	final long startTime = System.currentTimeMillis();
    	worker.addPropertyChangeListener(new PropertyChangeListener() {
    		
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				//The other default property is "progress", i.e. the progress, and its event is generated every time the setProgress is called in the worker
				if (!evt.getPropertyName().equals("state") || !worker.getState().equals(SwingWorker.StateValue.DONE)) {
					if (evt.getPropertyName().equals("progress")) {
						progressBar.setValue(worker.getProgress());
						if (worker.getProgress() > bestProgressBar.getValue()) { //We show the best progress up to now
							bestProgressBar.setValue(worker.getProgress());
						}
					}
					return;
				}
				boolean success = false;
	        	try {
					success = worker.get();
				} catch (InterruptedException e) {
					e.printStackTrace(System.err);
				} catch (ExecutionException e) {
					e.printStackTrace(System.err);
				}
	        	long endTime = System.currentTimeMillis();

	        	JFrame newFrame = new JFrame("Resulting graph (cost " + lm.getFinalCost() + ")");
	        	graph.reset();
	        	function.compute(lm.getParameters(), X, Y);
	        	newFrame.getContentPane().add(graph, BorderLayout.CENTER);
	        	newFrame.setBounds(frame.getBounds());
	        	frame.setVisible(false);
	        	frame.dispose();
	        	
	        	if (success) {
	        		JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(), "Done in " + RunAction.timeDifferenceShortFormat(startTime, endTime) + ".\nFinal cost: " + lm.getFinalCost() + "\nParameters: " + lm.getParameters(), "Good result found!", JOptionPane.INFORMATION_MESSAGE);
	        	} else {
	        		JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(), "Done in " + RunAction.timeDifferenceShortFormat(startTime, endTime) + ".\nMinimum cost found: " + lm.getFinalCost() + "\nParameters with that cost: " + lm.getParameters(), "No good result found!", JOptionPane.ERROR_MESSAGE);
	        	}
	        	
	        	newFrame.setVisible(true);
			}
    		
    	});
    	worker.execute();
	}
	
	public void vai() {
		DenseMatrix64F experimentalData = null;
		try {
			experimentalData = LevenbergMarquardt.readCSVtoMatrix("/Users/stefano/Documents/Lavoro/Prometheus/Data_Wnt_0-240_erk-frzld.csv", Arrays.asList("ERK data", "Frizzled data"), 240);
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
		
    	final Graph graph = new Graph();
    	final JFrame frame = new JFrame();
    	frame.getContentPane().add(graph, BorderLayout.CENTER);
    	final JProgressBar progressBar = new JProgressBar(0, 100),
    					   bestProgressBar = new JProgressBar(0, 100);
    	progressBar.setString("Current fitness");
    	progressBar.setStringPainted(true);
    	bestProgressBar.setString("Best fitness up to now");
    	bestProgressBar.setStringPainted(true);
    	Box progressBox = new Box(BoxLayout.Y_AXIS);
    	progressBox.add(progressBar);
    	progressBox.add(bestProgressBar);
    	frame.getContentPane().add(progressBox, BorderLayout.SOUTH);
    	frame.setBounds(100, 100, 600, 400);
    	frame.setVisible(true);
    	final Function function = new LevenbergMarquardtFunction(graph);
		final LevenbergMarquardt lm = new LevenbergMarquardt(function);
    	lm.MIN_COST = 0.5;
    	lm.DELTA = 0.0001;
    	final DenseMatrix64F initParam, X, Y;
    	
    	initParam = new DenseMatrix64F(new double[][]{{0.0005}, {0.0002}, {0.0007}, {0.04}, {0.015}}); //{0.0004}, {0.0004}, {0.0032}, {0.008}, {0.002}}); //{0.0004}, {0.0001}, {0.0008}, {0.04}, {0.015}});
    	X = new DenseMatrix64F(new double[][]{{0}, {0}, {0}, {0}, {0}, {0}, {0}, {0}, {0}, {0}}); //{0}, {0}, {30}, {0}, {60}, {0}, {120}, {0}
    	Y = experimentalData;
    	
    	final LMSwingWorker worker = new LMSwingWorker(lm, X, Y, initParam);
    	JButton chiudi = new JButton("Chiudi");
    	chiudi.addActionListener(new ActionListener() {
    		public void actionPerformed(ActionEvent e) {
    			worker.setMustTerminate(true); //Just push the button to cancel the job
    			//For some reason, listening for the window close does not work o_O
    		}
    	});
    	frame.add(chiudi, BorderLayout.EAST);
    	final long startTime = System.currentTimeMillis();
    	worker.addPropertyChangeListener(new PropertyChangeListener() {
    		
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				//The other default property is "progress", i.e. the progress, and its event is generated every time the setProgress is called in the worker
				if (!evt.getPropertyName().equals("state") || !worker.getState().equals(SwingWorker.StateValue.DONE)) {
					if (evt.getPropertyName().equals("progress")) {
						progressBar.setValue(worker.getProgress());
						if (worker.getProgress() > bestProgressBar.getValue()) { //We show the best progress up to now
							bestProgressBar.setValue(worker.getProgress());
						}
					}
					return;
				}
				boolean success = false;
	        	try {
					success = worker.get();
				} catch (InterruptedException e) {
					e.printStackTrace(System.err);
				} catch (ExecutionException e) {
					e.printStackTrace(System.err);
				}
	        	long endTime = System.currentTimeMillis();

	        	JFrame newFrame = new JFrame("Resulting graph (cost " + lm.getFinalCost() + ")");
	        	graph.reset();
	        	function.compute(lm.getParameters(), X, Y);
	        	newFrame.getContentPane().add(graph, BorderLayout.CENTER);
	        	newFrame.setBounds(frame.getBounds());
	        	frame.setVisible(false);
	        	frame.dispose();
	        	
	        	if (success) {
	        		JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(), "Done in " + RunAction.timeDifferenceShortFormat(startTime, endTime) + ".\nFinal cost: " + lm.getFinalCost() + "\nParameters: " + lm.getParameters(), "Good result found!", JOptionPane.INFORMATION_MESSAGE);
	        	} else {
	        		JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(), "Done in " + RunAction.timeDifferenceShortFormat(startTime, endTime) + ".\nMinimum cost found: " + lm.getFinalCost() + "\nParameters with that cost: " + lm.getParameters(), "No good result found!", JOptionPane.ERROR_MESSAGE);
	        	}
	        	
	        	newFrame.setVisible(true);
			}
    		
    	});
    	worker.execute();
	}
	
	//We use this to perform the L&M computations on a different task than the UI,
	//so we still have the possibility to interact with the UI (to move/resize
	//the output window and cancel the computation)
	public class LMSwingWorker extends SwingWorker<Boolean, Void> {
		private boolean success = false;
		private boolean mustTerminate = false;
		private LevenbergMarquardt lm;
		private DenseMatrix64F X, Y, initParam;
		
		public LMSwingWorker(LevenbergMarquardt lm, DenseMatrix64F X, DenseMatrix64F Y, DenseMatrix64F initParam) {
			this.lm = lm;
			this.X = X;
			this.Y = Y;
			this.initParam = initParam;
		}
		
		public boolean getMustTerminate() {
			return mustTerminate;
		}
		
		public void setMustTerminate(boolean mustTerminate) {
			this.mustTerminate = mustTerminate;
		}
		
		public void setProgresso(Integer progress) {
			this.setProgress(progress);
		}
		
		@Override
		protected Boolean doInBackground() throws Exception {
			this.setProgress(0);
			lm.setSwingWorker(this);
			success = lm.optimize(initParam, X, Y);
			this.setProgress(100);
			return success;
		}
		
		@Override
		protected void done() {
			
		}
		
	};
}
