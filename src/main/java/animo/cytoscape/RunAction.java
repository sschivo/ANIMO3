package animo.cytoscape;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.rmi.NotBoundException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskObserver;

import animo.core.AnimoBackend;
import animo.core.analyser.AnalysisException;
import animo.core.analyser.SMCResult;
import animo.core.analyser.uppaal.ResultAverager;
import animo.core.analyser.uppaal.SimpleLevelResult;
import animo.core.analyser.uppaal.UppaalModelAnalyserSMC;
import animo.core.model.Model;
import animo.core.model.Reactant;
import animo.exceptions.AnimoException;
import animo.network.UPPAALClient;
import animo.util.Utilities;
import animo.util.XmlConfiguration;

/**
 * The run action runs the network through the ANIMO analyser.
 * 
 * @author Brend Wanders
 * 
 */
public class RunAction extends AnimoActionTask {
	private class RunTask extends AbstractTask implements ObservableTask {

		private Model model;
		
		public RunTask() {
			
		}
		
		

		// private TaskMonitor monitor;

		/**
		 * Perform a simulation analysis. Translate the user-set number of real-life minutes for the length of the simulation, and obtain all input data for the model engine, based
		 * on the control the user has set (average, N simulation, StdDev, etc). When the analysis is done, display the obtained SimpleLevelResult on a ResultPanel
		 * 
		 * @param model
		 * @throws NotBoundException
		 * @throws IOException
		 * @throws AnalysisException
		 */
		private void performNormalAnalysis(TaskMonitor monitor) throws NumberFormatException, NotBoundException,
				AnalysisException, IOException {

			int nMinutesToSimulate = 0;
			try {
				nMinutesToSimulate = Integer.parseInt(timeToFormula.getValue().toString());
			} catch (NumberFormatException ex) {
				throw new NumberFormatException(
						"Unable to understand the number of minutes requested for the simulation.");
			}
			/*
			 * (int)(timeTo * model.getProperties().get(SECONDS_PER_POINT).as(Double.class) / 60); String inputTime = JOptionPane.showInputDialog(Cytoscape.getDesktop(),
			 * "Up to which time (in real-life MINUTES)?", nMinutesToSimulate); if (inputTime != null) { try { nMinutesToSimulate = Integer.parseInt(inputTime); } catch (Exception
			 * ex) { //the default value is still there, so nothing to change } } else { return; }
			 */
			monitor.setTitle(Animo.APP_NAME + " - UPPAAL model analysis");

			timeTo = (int) (nMinutesToSimulate * 60.0 / model.getProperties().get(SECONDS_PER_POINT).as(Double.class));
			scale = (double) nMinutesToSimulate / timeTo;

			// this.monitor.setStatus("Analyzing model with UPPAAL");
//			monitor.setStatusMessage("Analyzing model with UPPAAL");
//			monitor.setProgress(-1);

			// composite the analyser (this should be done from
			// configuration)
			// ModelAnalyser<LevelResult> analyzer = new UppaalModelAnalyser(new VariablesInterpreter(), new VariablesModel());

			// analyse model
			final SimpleLevelResult result;

			if (remoteUppaal.isSelected()) {
				UPPAALClient client = new UPPAALClient(serverName.getText(), Integer.parseInt(serverPort.getText()));
				int nSims = 1;
				if (nSimulationRuns.isEnabled()) {
					try {
						nSims = Integer.parseInt(nSimulationRuns.getValue().toString());
					} catch (NumberFormatException e) {
						throw new NumberFormatException("Unable to understand the number of requested simulations.");
					}
				} else {
					nSims = 1;
				}
				monitor.setStatusMessage("Forwarding the request to the server " + serverName.getText() + ":"
						+ serverPort.getText());
				result = client.analyze(model, timeTo, nSims, computeAvgStdDev.isSelected(), overlayPlot.isSelected());
			} else {
				// ModelAnalyser<LevelResult> analyzer = new UppaalModelAnalyser(new VariablesInterpreter(), new VariablesModel());
				// result = analyzer.analyze(model, timeTo);
				if (nSimulationRuns.isEnabled()) {
					int nSims = 0;
					try {
						nSims = Integer.parseInt(nSimulationRuns.getValue().toString());
					} catch (NumberFormatException e) {
						throw new NumberFormatException("Unable to understand the number of requested simulations.");
					}
					if (computeAvgStdDev.isSelected()) {
						result = new ResultAverager(monitor, meStesso).analyzeAverage(model, timeTo, nSims);
					} else if (overlayPlot.isSelected()) {
						result = new ResultAverager(monitor, meStesso).analyzeOverlay(model, timeTo, nSims);
					} else {
						result = null;
					}
				} else {
					result = new UppaalModelAnalyserSMC/* FasterSymbolicConcretizedFaster */(monitor, meStesso)
							.analyze(model, timeTo);
				}
			}

			/*
			 * CsvWriter csvWriter = new CsvWriter(); csvWriter.writeCsv("/tmp/test.csv", model, result);
			 */
			if (result == null) {
				throw new AnalysisException("No result was obtained.");
			}
			if (result.getReactantIds().isEmpty()) {
				throw new AnalysisException("No reactants selected for plot, or no reactants present in the result");
			} else {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						CyApplicationManager app = Animo.getCytoscapeApp().getCyApplicationManager();
						final AnimoResultPanel resultPanel = new AnimoResultPanel(model, result, scale, app
								.getCurrentNetwork());
						resultPanel.addToPanel(Animo.getCytoscape().getCytoPanel(CytoPanelName.EAST));
						Animo.selectAnimoControlPanel();
					}
				});
			}
		}

		/**
		 * Translate the SMC formula into UPPAAL time units, reactant names and give it to the analyser. Show the result in a message window.
		 * 
		 * @param model
		 * @throws NotBoundException
		 * @throws NumberFormatException
		 * @throws IOException
		 * @throws AnalysisException
		 */
		private void performSMCAnalysis(TaskMonitor monitor) throws NumberFormatException, NotBoundException,
				AnalysisException, IOException {
			// TODO: "understand" the formula and correctly change time values and reagent names
			String probabilisticFormula = smcFormula.getText();
			for (Reactant r : model.getReactantCollection()) {
				String name = r.getName();
				if (probabilisticFormula.contains(name)) {
					probabilisticFormula = probabilisticFormula.replace(name, r.getId());
				}
			}
			if (probabilisticFormula.contains("Pr[<")) {
				String[] parts = probabilisticFormula.split("Pr\\[<");
				StringBuilder sb = new StringBuilder();
				for (String p : parts) {
					if (p.length() < 1)
						continue;
					String timeS;
					if (p.startsWith("=")) {
						timeS = p.substring(1, p.indexOf("]"));
					} else {
						timeS = p.substring(0, p.indexOf("]"));
					}
					int time;
					try {
						time = Integer.parseInt(timeS);
					} catch (NumberFormatException ex) {
						throw new NumberFormatException("Problems with the identification of time string \"" + timeS
								+ "\"");
					}
					time = (int) (time * 60.0 / model.getProperties().get(SECONDS_PER_POINT).as(Double.class));
					sb.append("Pr[<");
					if (p.startsWith("=")) {
						sb.append("=");
					}
					sb.append(time);
					sb.append(p.substring(p.indexOf("]")));
				}
				probabilisticFormula = sb.toString();
			}

//			monitor.setStatusMessage("Analyzing model with UPPAAL");
//			monitor.setProgress(-1);

			// analyse model
			final SMCResult result;

			if (remoteUppaal.isSelected()) {
				UPPAALClient client = new UPPAALClient(serverName.getText(), Integer.parseInt(serverPort.getText()));
				result = client.analyzeSMC(model, probabilisticFormula);
			} else {
				result = new UppaalModelAnalyserSMC/* FasterConcrete */(monitor, meStesso).analyzeSMC(model,
						probabilisticFormula);
			}

			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(), result.toString(), "Result",
							JOptionPane.INFORMATION_MESSAGE);
				}
			});

		}

		@Override
		public void run(TaskMonitor monitor) throws AnimoException, NumberFormatException, NotBoundException,
				IOException {
			needToStop = false;
			monitor.setStatusMessage("Creating model representation");
			monitor.setProgress(0);
			
			// Just check that we understand how many minutes to run the simulation
			int nMinutesToSimulate = 0;
			try {
				nMinutesToSimulate = Integer.parseInt(timeToFormula.getValue().toString());
			} catch (NumberFormatException ex) {
				if (!smcUppaal.isSelected()) {
					throw new AnimoException("Unable to understand the number of minutes requested for the simulation.");
				}
			}

			final boolean generateTables;
			XmlConfiguration configuration = AnimoBackend.get().configuration();
			String modelType = configuration.get(XmlConfiguration.MODEL_TYPE_KEY, null);
			if (modelType.equals(XmlConfiguration.MODEL_TYPE_REACTION_CENTERED_TABLES)) {
				generateTables = true;
			} else {
				generateTables = false;
			}

			final int nMinutesToSimulate2 = nMinutesToSimulate;
			Model model = null;
			try {
				if (smcUppaal.isSelected()) {
					model = Model.generateModelFromCurrentNetwork(null, null, generateTables);
				} else {
					model = Model.generateModelFromCurrentNetwork(null, nMinutesToSimulate2, generateTables);
				}
			} catch (Exception ex) {
				ex.printStackTrace(System.err);
				return;
			}
			this.model = model;

			model.getProperties().let(Model.Properties.MODEL_CHECKING_TYPE)
					.be(Model.Properties.STATISTICAL_MODEL_CHECKING);

			boolean noReactantsPlotted = true;
			for (Reactant r : model.getReactantCollection()) {
				if (r.get(Model.Properties.ENABLED).as(Boolean.class)
						&& r.get(Model.Properties.PLOTTED).as(Boolean.class)) {
					noReactantsPlotted = false;
					break;
				}
			}
			if (noReactantsPlotted && !smcUppaal.isSelected()) {
				//This does work, but throwing the AnimoException has the same effect:
				//the task ends and the error is displayed to the user.
//				EventQueue.invokeLater(new Runnable() {
//					@Override
//					public void run() {
//						JOptionPane
//								.showMessageDialog(
//										Animo.getCytoscape().getJFrame(),/* (Component) monitor, */
//										"No reactants selected for plot: select at least one reactant to be plotted in the graph.",
//										"Error", JOptionPane.ERROR_MESSAGE);
//					}
//				});
				throw new AnimoException(
						"No reactants selected for plot: select at least one reactant to be plotted in the graph.");
			}

			if (smcUppaal.isSelected()) {
				performSMCAnalysis(monitor);
			} else {
				performNormalAnalysis(monitor);
			}

		}


		//TODO: this is not used at the moment: we are just content that the task finishes.
		//In case we change our mind and want to get some results from the task, here is the function
		//to go to
		@Override
		public <R> R getResults(Class<? extends R> arg0) {
			return null;
		}



	}

	private static final String SECONDS_PER_POINT = Model.Properties.SECONDS_PER_POINT; // The number of real-life seconds represented by a single UPPAAL time unit
	private static final long serialVersionUID = -5018057013811632477L;
	private int timeTo = 1200; // The default number of UPPAAL time units until which a simulation will run
	private double scale = 0.2; // The time scale representing the number of real-life minutes represented by a single UPPAAL time unit
	private JCheckBox remoteUppaal; // The RadioButtons telling us whether we use a local or a remote engine, and whether we use the Statistical Model Checking or the "normal"
									// engine
	private JRadioButton smcUppaal;
	private JRadioButton computeAvgStdDev, // Whether to compute the standard deviation when computing the average of a series of runs (if average of N runs is requested)
			overlayPlot; // Whether to show all plots as a series each
	private JFormattedTextField timeToFormula, nSimulationRuns; // Up to which point in time (real-life minutes) the simulation(s) will run, and the number of simulations (if
																// average of N runs is requested)
	private JTextField serverName, serverPort, smcFormula; // The name of the server, and the corresponding port, in the case we use a remote engine. The text inserted by the user
															// for the SMC formula. Notice that this formula will need to be changed so that it will be compliant with the UPPAAL
															// time scale, and reactant names
	private boolean needToStop; // Whether the user has pressed the Cancel button on the TaskMonitor while we were running an analysis process

	private AnimoActionTask meStesso; // Myself

	/**
	 * Constructor.
	 * 
	 * @param plugin
	 *            the plugin we should use
	 */
	public RunAction(/* TODO: moet hier iets mee? InatPlugin plugin, */JCheckBox remoteUppaal, JTextField serverName,
			JTextField serverPort, JRadioButton smcUppaal, JFormattedTextField timeToFormula,
			JFormattedTextField nSimulationRuns, JRadioButton computeAvgStdDev, JRadioButton overlayPlot,
			JTextField smcFormula) {
		super("<html>Analyze <br/>network</html>");
		this.remoteUppaal = remoteUppaal;
		this.serverName = serverName;
		this.serverPort = serverPort;
		this.smcUppaal = smcUppaal;
		this.timeToFormula = timeToFormula;
		this.nSimulationRuns = nSimulationRuns;
		this.computeAvgStdDev = computeAvgStdDev;
		this.overlayPlot = overlayPlot;
		this.smcFormula = smcFormula;
		this.meStesso = this;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		final RunTask task = new RunTask();

		// TODO: Nog geen oplossing voor gevonden voor cytoscape 3

		/*
		 * // Configure JTask Dialog Pop-Up Box JTaskConfig jTaskConfig = new JTaskConfig(); jTaskConfig.setOwner(Cytoscape.getDesktop()); // jTaskConfig.displayCloseButton(true);
		 * // jTaskConfig.displayCancelButton(true);
		 * 
		 * jTaskConfig.displayStatus(true); jTaskConfig.setAutoDispose(true); jTaskConfig.displayCancelButton(true); jTaskConfig.displayTimeElapsed(true);
		 * jTaskConfig.setModal(true); //if (nSimulationRuns.isEnabled()) { jTaskConfig.displayTimeRemaining(true); //}
		 */

		final long startTime = System.currentTimeMillis();
		Date now = new Date(startTime);
//		Calendar nowCal = Calendar.getInstance();
		File logFile = null;
		final PrintStream logStream;
		PrintStream tmpLogStream = null;
		final PrintStream oldErr = System.err;
		try {
//			if (UppaalModelAnalyserSMC/* FasterConcrete */.areWeUnderWindows()) {
//				logFile = File.createTempFile(
//						Animo.APP_NAME + "_run_" + nowCal.get(Calendar.YEAR) + "-" + nowCal.get(Calendar.MONTH) + "-"
//								+ nowCal.get(Calendar.DAY_OF_MONTH) + "_" + nowCal.get(Calendar.HOUR_OF_DAY) + "-"
//								+ nowCal.get(Calendar.MINUTE) + "-" + nowCal.get(Calendar.SECOND), ".log"); // windows doesn't like long file names..
//			} else {
				//Let's just do this for everybody
				logFile = File.createTempFile(Animo.APP_NAME + "_run_" + new SimpleDateFormat("dd-MMM-yyyy_HH.mm.ss_").format(now)/*Animo.APP_NAME + " run " + now.toString()*/, ".log");
//			}
			logFile.deleteOnExit();
			tmpLogStream = new PrintStream(new FileOutputStream(logFile));
			System.setErr(tmpLogStream);
		} catch (IOException ex) {
			// We have no log file, bad luck: we will have to use System.err.
		}
		logStream = tmpLogStream; //Just to have it final (from now on, so we can use it in the thread we create below) and still be able to initialize it as null

		TaskObserver finalizer = new TaskObserver() {
			@Override
			public void allFinished(FinishStatus status) {
				long endTime = System.currentTimeMillis();

				System.err.println("Time taken: " + Utilities.timeDifferenceFormat(startTime, endTime));
				System.err.flush();
				System.setErr(oldErr);
				if (logStream != null) {
					logStream.close();
				}
			}

			@Override
			public void taskFinished(ObservableTask t) {
				//We just use allFinished, even if the TaskIterator contains only one task
			}
		};
		
		// Execute Task in New Thread; pops open JTask Dialog Box.
		Animo.getCytoscapeApp().getTaskManager().execute(new TaskIterator(task), finalizer);
		
		//Execute task and WAIT FOR IT (!!) otherwise we just close the log file before the task has started..
		//Animo.getCyServiceRegistrar().getService(SynchronousTaskManager.class).execute(new TaskIterator(task)); <-- This does not create a task monitor, but we do want a task monitor, so we must end up waiting until the task finishes...)
		//So in order to wait for the task to finish, we give it the TaskObserver object that gets called when the task is done.
		
	}

	@Override
	public boolean needToStop() {
		return this.needToStop;
	}
}
