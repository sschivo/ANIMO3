package animo.cytoscape;

import java.awt.Dialog;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

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
import animo.core.analyser.uppaal.UppaalModelAnalyserSMC;
import animo.core.model.Model;
import animo.cytoscape.modelchecking.PathFormula;
import animo.exceptions.AnimoException;
import animo.util.Utilities;
import animo.util.XmlConfiguration;

public class ModelCheckAction extends AnimoActionTask {
	private class RunTask extends AbstractTask {

		/**
		 * Send the (already translated) formula to the analyser. Show the result in a message window.
		 * 
		 * @param model
		 * @throws AnalysisException
		 */
		private void performModelChecking(final Model model, final String formula, final String humanFormula,
				TaskMonitor monitor) throws AnalysisException {
			monitor.setStatusMessage("Analyzing model with UPPAAL");
			monitor.setProgress(-1);

			// analyse model
			final SMCResult result;

			/*
			 * if (remoteUppaal.isSelected()) { UPPAALClient client = new UPPAALClient(serverName.getText(), Integer.parseInt(serverPort.getText())); result =
			 * client.analyzeSMC(model, probabilisticFormula); } else {
			 */// this was mestesso
			result = new UppaalModelAnalyserSMC(monitor, modelCheckAction).analyzeSMC(model, formula);
			// }

			EventQueue.invokeLater(new Runnable() {
				@Override
				public void run() {
					JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(), "Property \"" + humanFormula + "\""
							+ System.getProperty("line.separator") + "is " + result.toString(), "Result",
							JOptionPane.INFORMATION_MESSAGE);
					if (result.getResultType() == SMCResult.RESULT_TYPE_TRACE) { // We have a trace to show
						double scale = model.getProperties().get(Model.Properties.SECONDS_PER_POINT).as(Double.class) / 60.0;
						final AnimoResultPanel resultViewer = new AnimoResultPanel(model, result.getLevelResult(),
								scale, Animo.getCytoscapeApp().getCyApplicationManager().getCurrentNetwork());
						resultViewer.addToPanel(Animo.getCytoscape().getCytoPanel(CytoPanelName.EAST));
						resultViewer.setTitle(humanFormula + " (" + (result.getBooleanResult() ? "True" : "False")
								+ ")");
						Animo.selectAnimoControlPanel();
					}
				}
			});

		}

		@Override
		public void run(final TaskMonitor monitor) throws AnalysisException, AnimoException {
//			EventQueue.invokeLater(new Runnable() {
//				@Override
//				public void run() {
					needToStop = false;
					
					monitor.setTitle(Animo.APP_NAME + " - UPPAAL model checking");
					
					monitor.setStatusMessage("Creating model representation");
					monitor.setProgress(0);

					boolean generateTables = false;
					XmlConfiguration configuration = AnimoBackend.get().configuration();
					String modelType = configuration.get(XmlConfiguration.MODEL_TYPE_KEY, null);
					if (modelType.equals(XmlConfiguration.MODEL_TYPE_REACTION_CENTERED_TABLES) || modelType.equals(XmlConfiguration.MODEL_TYPE_REACTION_CENTERED_TABLES_OLD)) {
						generateTables = true;
					}
					Model model = null;
					try {
						model = Model.generateModelFromCurrentNetwork(monitor, null, generateTables);
					} catch (AnimoException e) {
						e.printStackTrace(System.err);
					}
					formulaToCheck.setReactantIDs(model); // This is enough to set the proper model IDs to all reactants in the formula
					model.getProperties().let(Model.Properties.MODEL_CHECKING_TYPE)
							.be(Model.Properties.NORMAL_MODEL_CHECKING); //We don't use statistical model checking here (e.g. queries about probability etc), especially because with uncertainty = 0 the models are deterministic
					try {
						performModelChecking(model, formulaToCheck.toString(), formulaToCheck.toHumanReadable(),
								monitor);
					} catch (AnalysisException e) {
						e.printStackTrace(System.err);
					}
//				}
//			});
		}

	}

	private static final long serialVersionUID = 1147435660037202034L;
	private PathFormula formulaToCheck;

//	private PrintStream logStream;
	private ModelCheckAction modelCheckAction;

	public ModelCheckAction() {
		super("<html>Model <br/>checking...</html>");
		this.modelCheckAction = this;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		final RunTask task = new RunTask();

		final long startTime = System.currentTimeMillis();
		Date now = new Date(startTime);
//		Calendar nowCal = Calendar.getInstance();
		File logFile = null;
		final PrintStream oldErr = System.err;
		final PrintStream logStream;
		PrintStream tmpLogStream = null;
		try {
//			if (UppaalModelAnalyserSMC.areWeUnderWindows()) {
//				logFile = File.createTempFile(
//						Animo.APP_NAME + "_run_" + nowCal.get(Calendar.YEAR) + "-" + nowCal.get(Calendar.MONTH) + "-"
//								+ nowCal.get(Calendar.DAY_OF_MONTH) + "_" + nowCal.get(Calendar.HOUR_OF_DAY) + "-"
//								+ nowCal.get(Calendar.MINUTE) + "-" + nowCal.get(Calendar.SECOND), ".log"); // windows doesn't like long file names..
//			} else {
//				logFile = File.createTempFile(Animo.APP_NAME + " run " + now.toString(), ".log");
//			}
			logFile = File.createTempFile(Animo.APP_NAME + "_run_" + new SimpleDateFormat("dd-MMM-yyyy_HH.mm.ss_").format(now), ".log");
			logFile.deleteOnExit();
			tmpLogStream = new PrintStream(new FileOutputStream(logFile));
			System.setErr(tmpLogStream);
		} catch (IOException ex) {
			// We have no log file, bad luck: we will have to use System.err.
		}
		logStream = tmpLogStream;
		
		//Check to see if the uncertainty is currently set to 0. If it is not, suggest to the user that 0 is a good idea to get model checking answers
		final XmlConfiguration configuration = AnimoBackend.get().configuration();
		String uncertaintyStr = configuration.get(XmlConfiguration.UNCERTAINTY_KEY);
		double uncertainty = -1;
		boolean saveUncertainty = false;
		if (uncertaintyStr != null) {
			try {
				uncertainty = Double.parseDouble(uncertaintyStr);
			} catch (NumberFormatException ex) {
				System.err.println("Couldn't read uncertainty setting \"" + uncertaintyStr + "\"!");
				ex.printStackTrace(System.err);
			}
		}
		if (uncertainty < 0) { //Either it was not set or it was not read correctly (wrong format/whatever)
			uncertainty = Double.parseDouble(XmlConfiguration.DEFAULT_UNCERTAINTY);
			configuration.set(XmlConfiguration.UNCERTAINTY_KEY, XmlConfiguration.DEFAULT_UNCERTAINTY);
			saveUncertainty = true;
		}
		if (uncertainty > 0) {
			int response = JOptionPane.NO_OPTION;
			response = JOptionPane.showConfirmDialog(Animo.getCytoscape().getJFrame(), "The uncertainty level for interaction durations is currently " + uncertainty + "%\n(i.e., an interaction can take between " + (100 - uncertainty) + "% and " + (100 + uncertainty) + "% of its\nexact time defined by the scenario choice and parameter k)\nHowever, we recommend setting it to 0 in order to obtain\nmodel checking results in reasonable times.\n\nDo you want to set uncertainty to 0?", "Suggestion: set uncertainty to 0?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			if (response == JOptionPane.YES_OPTION) {
				configuration.set(XmlConfiguration.UNCERTAINTY_KEY, "0");
				saveUncertainty = true;
			}
		}
		if (saveUncertainty) {
			try {
				configuration.writeConfigFile();
			} catch (Exception ex) {
				System.err.println("Couldn't save the uncertainty setting");
				ex.printStackTrace(System.err);
			}
		}
		

		formulaToCheck = null;
		final JDialog dialog = new JDialog(Animo.getCytoscape().getJFrame(), "Model checking templates",
				Dialog.ModalityType.APPLICATION_MODAL);
		Box boxContent = new Box(BoxLayout.Y_AXIS);
		final PathFormula pathFormula = new PathFormula();
		boxContent.add(pathFormula);
		Box buttonsBox = new Box(BoxLayout.X_AXIS);
		buttonsBox.add(Box.createGlue());
		JButton doWork = new JButton("Start model checking");
		doWork.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				formulaToCheck = pathFormula.getSelectedFormula();
				dialog.dispose();
			}
		});
		buttonsBox.add(doWork);
		buttonsBox.add(Box.createGlue());
		boxContent.add(buttonsBox);
		dialog.getContentPane().add(boxContent);
		dialog.pack();
		dialog.setLocationRelativeTo(Animo.getCytoscape().getJFrame());
		dialog.setVisible(true);

		if (formulaToCheck == null)
			return; // If the user simply closed the window, the analysis is cancelled
		System.err.println("Checking formula " + formulaToCheck.toHumanReadable());

		TaskObserver finalizer = new TaskObserver() {

			@Override
			public void allFinished(FinishStatus s) {
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
				//see allFinished
			}
			
		};
		
		// Execute Task in New Thread; pops open JTask Dialog Box.
//		TaskManager<?, ?> taskManager = Animo.getCytoscapeApp().getTaskManager();
//		taskManager.execute(new TaskIterator(task), taskObserver);
		Animo.getCytoscapeApp().getTaskManager().execute(new TaskIterator(task), finalizer);
	}
}
