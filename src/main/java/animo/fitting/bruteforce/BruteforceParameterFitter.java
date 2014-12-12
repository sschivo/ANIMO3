package animo.fitting.bruteforce;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;

import animo.core.AnimoBackend;
import animo.core.analyser.LevelResult;
import animo.core.analyser.uppaal.UppaalModelAnalyserSMC;
import animo.core.analyser.uppaal.VariablesModel;
import animo.core.graph.FileUtils;
import animo.core.graph.Graph;
import animo.core.model.Model;
import animo.core.model.Reactant;
import animo.core.model.Reaction;
import animo.core.model.Scenario;
import animo.cytoscape.Animo;
import animo.cytoscape.AnimoActionTask;
import animo.cytoscape.ComponentTitledBorder;
import animo.cytoscape.LabelledField;
import animo.exceptions.AnimoException;
import animo.fitting.AcceptableConfiguration;
import animo.fitting.BoxAutoenabler;
import animo.fitting.GridLayout2;
import animo.fitting.ParameterSetting;
import animo.fitting.ReactantComparison;
import animo.fitting.ScenarioCfg;
import animo.fitting.ScenarioFitting;
import animo.fitting.multithread.ThreadPool;
import animo.util.Pair;
import animo.util.Table;
import animo.util.XmlConfiguration;

public class BruteforceParameterFitter extends WindowAdapter {
	private JFrame window = null;
	private static final String DECIMAL_FORMAT_STRING = "#.####", BUTTON_START = "Start", BUTTON_CANCEL = "Cancel";
	private HashMap<Reaction, ScenarioFitting> scenarioFittingParameters = null;
	private HashMap<Reactant, ReactantComparison> reactantComparisons = null;
	private Scenario[] scenarios = null;
	private JButton startExecution = null;
	private boolean terminate = false;
	private long nComputations, totalComputations;
	// Please notice that currentSettingIndex counts the computation index as we start it (so that we can keep the results ordered), while nComputations counts the number of
	// computations we have finished
	private int currentSettingIndex;
	private List<AcceptableConfiguration> acceptableConfigurations = null;
	private List<AcceptableConfiguration> allConfigurations = null;
	private int timeTo = 120;
	private double scale = 1.0;
	private Model model = null;
	private boolean generateTables = false;
	private String csvFileName = ""; // /local/schivos/Data_0-120_TNF100_semplificato_con_dichiarazione_solo_MK2_e_JNK1.csva";
	private JProgressBar progress = null;
	private long startTime = 0;
	private JFormattedTextField numberOfParallelExecutions = null;
	private JSlider parallelExecs = null;
	private ThreadPool pool = null;
	private static final String WINDOW_TITLE = "Acceptable configurations";
	private JFrame acceptableGraphsWindow = new JFrame(WINDOW_TITLE);

	public BruteforceParameterFitter() {

		this.scenarioFittingParameters = new HashMap<Reaction, ScenarioFitting>();
		this.reactantComparisons = new HashMap<Reactant, ReactantComparison>();
		this.scenarios = Scenario.THREE_SCENARIOS;

	}

	public void addReaction(Reaction reaction) {
		ScenarioCfg cfg = reaction.getScenarioCfg();
		scenarioFittingParameters.put(reaction, new ScenarioFitting(cfg));
		for (String parName : cfg.getParameters().keySet()) {
			scenarioFittingParameters.get(reaction).setParameterSetting(parName,
					new ParameterSetting(parName, cfg.getParameters().get(parName)));
			scenarioFittingParameters.get(reaction).setScenarioCfg(reaction.getScenarioCfg());
		}
	}

	private void addReactionFixedParameters(JPanel panel, JCheckBox check, final Reaction reaction) {
		final Box parametersBox = new BoxAutoenabler(BoxLayout.Y_AXIS);
		parametersBox.add(Box.createGlue());
		ScenarioCfg cfg = reaction.getScenarioCfg();
		if (cfg == null)
			return;
		HashMap<String, Double> parameters = cfg.getParameters();
		DecimalFormat format = new DecimalFormat(DECIMAL_FORMAT_STRING);
		format.setMinimumFractionDigits(5);
		for (final String parName : parameters.keySet()) {
			final JFormattedTextField paramValue = new JFormattedTextField(format);
			paramValue.setValue(parameters.get(parName));
			Dimension prefSize = paramValue.getPreferredSize();
			prefSize.width *= 1.5;
			paramValue.setPreferredSize(prefSize);
			paramValue.getDocument().addDocumentListener(new ParameterDocumentListener(reaction, parName, paramValue));
			setFixedParameter(reaction, parName, new Double(paramValue.getValue().toString()));
			updateTotalComputations();
			window.validate();
			parametersBox.add(new LabelledField(parName, paramValue));
		}

		parametersBox.add(Box.createGlue());
		Box boxParameter = new Box(BoxLayout.X_AXIS);
		boxParameter.add(Box.createHorizontalStrut(check.getPreferredSize().width / 2));
		boxParameter.add(parametersBox);
		boxParameter.add(Box.createHorizontalStrut(check.getPreferredSize().width / 2));
		boxParameter.setBorder(new ComponentTitledBorder(check, boxParameter, BorderFactory.createEtchedBorder()));
		panel.add(boxParameter);
		parametersBox.setEnabled(true);
	}

	private void addReactionVariableParameters(JPanel panel, JCheckBox check, final Reaction reaction) {
		final Box parametersBox = new BoxAutoenabler(BoxLayout.Y_AXIS);
		parametersBox.add(Box.createGlue());
		ScenarioCfg cfg = reaction.getScenarioCfg();
		if (cfg == null)
			return;
		HashMap<String, Double> parameters = cfg.getParameters();
		DecimalFormat format = new DecimalFormat(DECIMAL_FORMAT_STRING);
		format.setMinimumFractionDigits(5);
		for (final String parName : parameters.keySet()) {
			Box parameterBox = new Box(BoxLayout.X_AXIS);
			parameterBox.add(Box.createGlue());
			final JFormattedTextField minValue = new JFormattedTextField(format);
			final JFormattedTextField maxValue = new JFormattedTextField(format);
			final JFormattedTextField incrementValue = new JFormattedTextField(format);
			final JRadioButton linearScale = new JRadioButton("Linear"), logarithmicScale = new JRadioButton(
					"Logarithmic");
			Box boxIncrement = new Box(BoxLayout.Y_AXIS);
			Box boxScale = new Box(BoxLayout.X_AXIS);
			boxScale.add(linearScale);
			boxScale.add(logarithmicScale);
			boxIncrement.add(incrementValue);
			boxIncrement.add(boxScale);
			final LabelledField incrementField = new LabelledField("Increment", boxIncrement);
			ButtonGroup scaleGroup = new ButtonGroup();
			scaleGroup.add(linearScale);
			scaleGroup.add(logarithmicScale);
			ActionListener l = new ScaleActionListener(linearScale, incrementField);
			linearScale.addActionListener(l);
			logarithmicScale.addActionListener(l);
			logarithmicScale.setSelected(true);
			linearScale.setSelected(false);
			minValue.setValue(0.001); // parameters.get(parName)/10.0);
			Dimension prefSize = minValue.getPreferredSize();
			prefSize.width *= 1.5;
			minValue.setPreferredSize(prefSize);
			minValue.getDocument().addDocumentListener(
					new MinValueDocumentListener(reaction, parName, maxValue, minValue, incrementValue,
							logarithmicScale));
			parameterBox.add(new LabelledField("Min", minValue));
			maxValue.setValue(0.016); // parameters.get(parName));
			prefSize = maxValue.getPreferredSize();
			prefSize.width *= 1.5;
			maxValue.setPreferredSize(prefSize);
			maxValue.getDocument().addDocumentListener(new DocumentListener() {
				@Override
				public void changedUpdate(DocumentEvent e) {
					setVariableParameter(reaction, parName, new Double(minValue.getValue().toString()), new Double(
							maxValue.getValue().toString()), new Double(incrementValue.getValue().toString()),
							logarithmicScale.isSelected());
					updateTotalComputations();
				}

				@Override
				public void insertUpdate(DocumentEvent e) {
					changedUpdate(e);
				}

				@Override
				public void removeUpdate(DocumentEvent e) {
					changedUpdate(e);
				}
			});
			parameterBox.add(new LabelledField("Max", maxValue));
			incrementValue.setValue(2); // parameters.get(parName)/10.0);
			prefSize = incrementValue.getPreferredSize();
			prefSize.width *= 1.5;
			incrementValue.setPreferredSize(prefSize);
			incrementValue.getDocument().addDocumentListener(new DocumentListener() {
				@Override
				public void changedUpdate(DocumentEvent e) {
					setVariableParameter(reaction, parName, new Double(minValue.getValue().toString()), new Double(
							maxValue.getValue().toString()), new Double(incrementValue.getValue().toString()),
							logarithmicScale.isSelected());
					updateTotalComputations();
				}

				@Override
				public void insertUpdate(DocumentEvent e) {
					changedUpdate(e);
				}

				@Override
				public void removeUpdate(DocumentEvent e) {
					changedUpdate(e);
				}
			});
			parameterBox.add(incrementField);
			parameterBox.add(Box.createGlue());
			parametersBox.add(new LabelledField(parName, parameterBox));
			setVariableParameter(reaction, parName, new Double(minValue.getValue().toString()), new Double(maxValue
					.getValue().toString()), new Double(incrementValue.getValue().toString()),
					logarithmicScale.isSelected());
			updateTotalComputations();
		}
		parametersBox.add(Box.createGlue());
		Box boxParameter = new Box(BoxLayout.Y_AXIS);
		boxParameter.add(parametersBox);
		boxParameter.setBorder(new ComponentTitledBorder(check, boxParameter, BorderFactory.createEtchedBorder()));
		panel.add(boxParameter);

	}

	@SuppressWarnings("unused")
	private boolean compareResults(File myCSV) throws IOException {
		for (Reactant reactant : reactantComparisons.keySet()) {
			ReactantComparison compare = reactantComparisons.get(reactant);
			double allowedError = compare.getMaxError();
			String myColumn = reactant.getName();
			File hisCSV = new File(compare.getCsvFile());
			String hisColumn = compare.getSeriesNames().get(0);
			int myColumnIndex = 0, hisColumnIndex = 0;

			BufferedReader myIS = new BufferedReader(new FileReader(myCSV)), hisIS = new BufferedReader(new FileReader(
					hisCSV));
			String myFirstLine = myIS.readLine(), hisFirstLine = hisIS.readLine();
			if (myFirstLine == null) {
				System.err.println("ERROR: file " + myCSV + ": is empty!");
				myIS.close();
				hisIS.close();
				return false;
			}
			if (hisFirstLine == null) {
				System.err.println("ERROR file " + hisCSV + ": is empty!");
				myIS.close();
				hisIS.close();
				return false;
			}
			StringTokenizer myTritatutto = new StringTokenizer(myFirstLine, ","), hisTritatutto = new StringTokenizer(
					hisFirstLine, ",");
			while (myTritatutto.hasMoreElements()) {
				String colName = myTritatutto.nextToken().replace('\"', ' ');
				if (colName.equals(myColumn)) {
					break;
				}
				myColumnIndex++;
			}
			while (hisTritatutto.hasMoreElements()) {
				String colName = hisTritatutto.nextToken().replace('\"', ' ');
				if (colName.equals(hisColumn)) {
					break;
				}
				hisColumnIndex++;
			}

			String myLine = myIS.readLine();
			String hisLine = hisIS.readLine();
			double maxDifference = 0;
			while (hisLine != null) {
				hisTritatutto = new StringTokenizer(hisLine, ",");
				double hisTime = Double.parseDouble(hisTritatutto.nextToken());
				for (int i = 1; i < hisColumnIndex; i++)
					hisTritatutto.nextToken();
				double hisValue = Double.parseDouble(hisTritatutto.nextToken());
				while (myLine != null) {
					myTritatutto = new StringTokenizer(myLine, ",");
					double myTime = Double.parseDouble(myTritatutto.nextToken());
					if (myTime < hisTime)
						continue;
					for (int i = 1; i < myColumnIndex; i++)
						myTritatutto.nextToken();
					double myValue = Double.parseDouble(myTritatutto.nextToken());
					double difference = Math.abs(myValue - hisValue) / hisValue;
					if (difference > maxDifference) {
						maxDifference = difference;
					}
					if (difference > allowedError) { // TODO: interessante notare che la differenza calcolata cos� mi esclude paradossalmente i grafici pi� belli... o_O
						myIS.close();
						hisIS.close();
						return false;
					}
					myLine = myIS.readLine();
				}
				hisLine = hisIS.readLine();
			}
			myIS.close();
			hisIS.close();
		}
		return true;
	}

	private Pair<Boolean, Double> compareResults(LevelResult myResult) throws IOException {
		double maxDiff = 0;
		for (Reactant reactant : reactantComparisons.keySet()) {
			ReactantComparison compare = reactantComparisons.get(reactant);
			File hisCSV = new File(compare.getCsvFile());
			String hisColumn = compare.getSeriesNames().get(0), hisNLevelsColumn = Graph.MAX_Y_STRING.toLowerCase();
			int hisColumnIndex = 0, hisNLevelsColumnIndex = 0;
			double hisNLevels = 15, myNLevels = model.getProperties().get(Model.Properties.NUMBER_OF_LEVELS)
					.as(Integer.class);

			BufferedReader hisIS = new BufferedReader(new FileReader(hisCSV));
			String hisFirstLine = hisIS.readLine();
			if (hisFirstLine == null) {
				System.err.println("ERROR file " + hisCSV + ": is empty!");
				hisIS.close();
				return new Pair<Boolean, Double>(false, 101.0);
			}
			StringTokenizer hisTritatutto = new StringTokenizer(hisFirstLine, ",");
			boolean foundColumn = false, foundNLevels = false;
			while (hisTritatutto.hasMoreElements()) {
				String colName = hisTritatutto.nextToken().replace('\"', ' ');
				if (!colName.equals(hisColumn) && !foundColumn) {
					hisColumnIndex++;
				} else if (colName.equals(hisColumn)) {
					foundColumn = true;
				}
				if (colName.toLowerCase().equals(hisNLevelsColumn)) {
					foundNLevels = true;
				} else {
					hisNLevelsColumnIndex++;
				}
				if (foundColumn && foundNLevels)
					break;
			}

			String hisLine = hisIS.readLine();
			double maxDifference = 0;
			foundColumn = false;
			while (hisLine != null) {
				hisTritatutto = new StringTokenizer(hisLine, ",");
				double hisTime = Double.parseDouble(hisTritatutto.nextToken());
				int i = 1;
				for (; i < hisColumnIndex - 1; i++) {
					hisTritatutto.nextToken();
				}
				double hisValue = Double.parseDouble(hisTritatutto.nextToken());
				if (foundNLevels && !foundColumn) {
					for (; i < hisNLevelsColumnIndex - 1; i++) {
						hisTritatutto.nextToken();
					}
					hisNLevels = Double.parseDouble(hisTritatutto.nextToken());
					foundColumn = true;
					// System.err.println("Lui ha " + hisNLevels + " livelli");
				}
				hisValue = hisValue / hisNLevels;
				double myValue = myResult.getConcentration(reactant.getId(), hisTime / scale);
				double difference = Math.abs(myValue / myNLevels - hisValue);
				// System.err.println("IO (" + (hisTime / scale) + "): " + myValue + "/" + myNLevels + ", LUI (" + hisTime + "): " + (hisValue / myNLevels * hisNLevels) + "/" +
				// hisNLevels + ". Diff: " + difference);
				if (difference > maxDifference) {
					maxDifference = difference;
				}
				hisLine = hisIS.readLine();
			}
			if (maxDifference > maxDiff) {
				maxDiff = maxDifference;
			}
			hisIS.close();
		}
		return new Pair<Boolean, Double>(true, maxDiff);
	}

	public JComponent createReactionComparisonPanel(final Model model) {

		final JPanel comparisonPanel = new JPanel();
		comparisonPanel.setLayout(new BorderLayout());
		final Box comparisonBox = new Box(BoxLayout.Y_AXIS);
		comparisonBox.add(Box.createGlue());
		numberOfParallelExecutions = new JFormattedTextField(2 * (Runtime.getRuntime().availableProcessors() - 1));
		final JTextField csvFileNameTextField = new JTextField(15);
		csvFileNameTextField.setText(csvFileName);
		try {
			updateComparisonBox(comparisonBox, csvFileName);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		csvFileNameTextField.getDocument().addDocumentListener(
				new CSVDocumentListener(comparisonBox, csvFileNameTextField, comparisonPanel));
		comparisonBox.add(Box.createGlue());
		JScrollPane scrollComparison = new JScrollPane(comparisonBox);
		scrollComparison.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollComparison.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		comparisonPanel.add(scrollComparison, BorderLayout.CENTER);
		// Box vTextBox = new Box(BoxLayout.Y_AXIS);
		Box csvBox = new Box(BoxLayout.X_AXIS);
		csvBox.add(csvFileNameTextField);
		csvBox.add(Box.createGlue());
		JButton openCSV = new JButton("Browse...");
		openCSV.addActionListener(new OpenCSVActionListener(csvFileNameTextField));
		csvBox.add(openCSV);
		comparisonPanel.add(new LabelledField("CSV file name", csvBox), BorderLayout.NORTH);
		parallelExecs = new JSlider();
		parallelExecs.setMinimum(1);
		parallelExecs.setMaximum(2 * Runtime.getRuntime().availableProcessors());
		parallelExecs.addChangeListener(new ParallelExecsChangeListener());
		Box parallelBox = new Box(BoxLayout.X_AXIS);
		parallelBox.add(parallelExecs);
		parallelBox.add(Box.createGlue());
		parallelBox.add(numberOfParallelExecutions);
		int nExecs = parallelExecs.getMaximum();
		try {
			nExecs = Integer.parseInt(numberOfParallelExecutions.getValue().toString());
		} catch (NumberFormatException ex) {
			nExecs = parallelExecs.getMaximum();
		}
		parallelExecs.setValue(nExecs);
		comparisonPanel.add(new LabelledField("Number of parallel executions", parallelBox), BorderLayout.SOUTH);

		return comparisonPanel; // scrollComparison;

	}

	public void doCompute(ThreadPool pool) {
		final HashMap<String, ScenarioCfg> currentConfiguration = new HashMap<String, ScenarioCfg>();
		double secStepFactor = model.getProperties().get(Model.Properties.SECS_POINT_SCALE_FACTOR).as(Double.class);
		for (Reaction r : model.getReactionCollection()) {
			if (!scenarioFittingParameters.keySet().contains(r))
				continue;
			ScenarioCfg cfg = r.getScenarioCfg();
			Scenario scenario = scenarios[cfg.getIndex()];
			// TODO: riferito al TODO di sopra. Questo tratta solo Reaction2, niente Reaction1!!
			for (String parName : scenario.getParameters().keySet()) {
				scenario.setParameter(parName, cfg.getParameters().get(parName));
			}
			// We don't want to use uncertainty in the parameter sweeping!! Otherwise you would need many simulations for each configuration! unc;
			double uncertainty = 0;
			int scenarioIdx = cfg.getIndex();
			int nLevelsR1, nLevelsR2;
			double levelsScaleFactor = r.get(Model.Properties.LEVELS_SCALE_FACTOR + "_reaction").as(Double.class);

			String cata, reac;
			cata = r.get(Model.Properties.CATALYST).as(String.class);
			reac = r.get(Model.Properties.REACTANT).as(String.class);
			if (!model.getReactant(cata).get(Model.Properties.NUMBER_OF_LEVELS).isNull()) {
				nLevelsR1 = model.getReactant(cata).get(Model.Properties.NUMBER_OF_LEVELS).as(Integer.class);
			} else {
				nLevelsR1 = model.getProperties().get(Model.Properties.NUMBER_OF_LEVELS).as(Integer.class);
			}
			if (!model.getReactant(reac).get(Model.Properties.NUMBER_OF_LEVELS).isNull()) {
				nLevelsR2 = model.getReactant(reac).get(Model.Properties.NUMBER_OF_LEVELS).as(Integer.class);
			} else {
				nLevelsR2 = model.getProperties().get(Model.Properties.NUMBER_OF_LEVELS).as(Integer.class);
			}

			boolean activeR1 = true, activeR2 = false;
			boolean reactant1IsDownstream = false, reactant2IsDownstream = true;

			if (scenarioIdx == 0 || scenarioIdx == 1) {
				activeR1 = true;
				if (r.get(Model.Properties.INCREMENT).as(Integer.class) >= 0) {
					activeR2 = false;
				} else {
					activeR2 = true;
				}
			} else if (scenarioIdx == 2) {
				activeR1 = r.get(Model.Properties.REACTANT_IS_ACTIVE_INPUT_E1).as(Boolean.class);
				activeR2 = r.get(Model.Properties.REACTANT_IS_ACTIVE_INPUT_E2).as(Boolean.class);
				reactant1IsDownstream = r.get(Model.Properties.CATALYST).as(String.class)
						.equals(r.get(Model.Properties.OUTPUT_REACTANT).as(String.class));
				reactant2IsDownstream = r.get(Model.Properties.REACTANT).as(String.class)
						.equals(r.get(Model.Properties.OUTPUT_REACTANT).as(String.class));
			} else {
				// TODO: this should never happen, because we have already made these checks
				activeR1 = activeR2 = true;
			}

			if (generateTables) {
				List<Double> times = scenario.generateTimes(1 + nLevelsR1, activeR1, reactant1IsDownstream,
						1 + nLevelsR2, activeR2, reactant2IsDownstream);
				Table timesLTable = new Table(nLevelsR2 + 1, nLevelsR1 + 1);
				Table timesUTable = new Table(nLevelsR2 + 1, nLevelsR1 + 1);

				for (int j = 0; j < nLevelsR2 + 1; j++) {
					for (int k = 0; k < nLevelsR1 + 1; k++) {
						Double t = times.get(j * (nLevelsR1 + 1) + k);
						if (Double.isInfinite(t)) {
							timesLTable.set(j, k, VariablesModel.INFINITE_TIME);
							timesUTable.set(j, k, VariablesModel.INFINITE_TIME);
						} else if (uncertainty == 0) {
							timesLTable.set(j, k, (int) Math.round(secStepFactor * levelsScaleFactor * t));
							timesUTable.set(j, k, (int) Math.round(secStepFactor * levelsScaleFactor * t));
						} else {
							timesLTable.set(
									j,
									k,
									Math.max(
											0,
											(int) Math.round(secStepFactor * levelsScaleFactor * t
													* (1 - uncertainty / 100.0))));
							timesUTable.set(
									j,
									k,
									Math.max(
											0,
											(int) Math.round(secStepFactor * levelsScaleFactor * t
													* (1 + uncertainty / 100.0))));
						}
					}
				}
				r.let(Model.Properties.TIMES_LOWER).be(timesLTable);
				r.let(Model.Properties.TIMES_UPPER).be(timesUTable);
			}

			currentConfiguration.put(r.getId(), new ScenarioCfg(cfg));
		}
		synchronized (model) {
			final Model analyzedModel = model.copy();
			pool.addTask(new TaskThread(analyzedModel, currentConfiguration, currentSettingIndex));
		}

	}

	public List<Pair<ScenarioCfg, ParameterSetting>> enumerateVariableParameters() {
		List<Pair<ScenarioCfg, ParameterSetting>> list = new ArrayList<Pair<ScenarioCfg, ParameterSetting>>();
		for (ScenarioFitting scnFitting : scenarioFittingParameters.values()) {
			for (ParameterSetting parSetting : scnFitting.getParameterSettings().values()) {
				if (!parSetting.isFixed()) {
					list.add(new Pair<ScenarioCfg, ParameterSetting>(scnFitting.getScenarioCfg(), parSetting));
				}
			}
		}
		return list;
	}

	public ReactantComparison getReactantComparison(Reactant reactant) {
		return reactantComparisons.get(reactant);
	}

	public void parameterSweep(ThreadPool pool) {
		{
			for (Reaction reaction : scenarioFittingParameters.keySet()) {
				ScenarioFitting scnFitting = scenarioFittingParameters.get(reaction);
				if (scnFitting == null)
					continue;
				for (String parName : reaction.getScenarioCfg().getParameters().keySet()) {
					if (scnFitting.getParameterSetting(parName).isFixed()
							&& !scnFitting.getParameterSetting(parName).getFixedValue()
									.equals(reaction.getScenarioCfg().getParameters().get(parName))) {
						reaction.getScenarioCfg().getParameters()
								.put(parName, scnFitting.getParameterSetting(parName).getFixedValue());
					}
				}
			}
			List<Pair<ScenarioCfg, ParameterSetting>> parameterList = enumerateVariableParameters();
			updateTotalComputations();
			nComputations = 0;
			// Please notice that nSettings counts the computation index as we start it (so that we can keep the results ordered), while nComputations counts the number of
			// computations we have finished
			currentSettingIndex = -1;
			visitParameterSettings(parameterList, 0, pool);
			do {
				try {
					Thread.sleep(200);
				} catch (InterruptedException ex) {
					ex.printStackTrace();
				}
			} while (!pool.isEmpty());
			pool.terminateAll();
		}
	}

	public void setComparisonCSV(String csvFileName) {
		this.csvFileName = csvFileName;
	}

	public void setFixedParameter(Reaction reaction, String parName, Double fixedValue) {
		ParameterSetting parSetting = scenarioFittingParameters.get(reaction).getParameterSetting(parName);
		if (parSetting != null) {
			parSetting.setFixed(fixedValue);
		} else {
			parSetting = new ParameterSetting(parName, fixedValue);
			scenarioFittingParameters.get(reaction).setParameterSetting(parName, parSetting);
		}
	}

	public void setReactantComparison(Reactant reactant, ReactantComparison comparison) {
		reactantComparisons.put(reactant, comparison);
	}

	public void setTimeParameters(Integer timeLimit, Double timeScale) {
		this.timeTo = timeLimit;
	}

	public void setVariableParameter(Reaction reaction, String parName, Double min, Double max, Double increase,
			boolean logarithmic) {
		ParameterSetting parSetting = scenarioFittingParameters.get(reaction).getParameterSetting(parName);
		if (parSetting != null) {
			parSetting.setVariable(min, max, increase, logarithmic);
		} else {
			parSetting = new ParameterSetting(parName, min, max, increase);
			scenarioFittingParameters.get(reaction).setParameterSetting(parName, parSetting);
		}
	}

	public void showWindow(boolean exitOnClose, int nMinutesToSimulate) {
		window = new JFrame("Parameter fitter");
		model = null;
		generateTables = false;
		XmlConfiguration configuration = AnimoBackend.get().configuration();
		String modelType = configuration.get(XmlConfiguration.MODEL_TYPE_KEY, null);
		if (modelType.equals(XmlConfiguration.MODEL_TYPE_REACTION_CENTERED_TABLES)) {
			generateTables = true;
		}
		try {
			model = Model.generateModelFromCurrentNetwork(null, Math.round(nMinutesToSimulate), generateTables);
			timeTo = (int) Math.round(nMinutesToSimulate * 60.0
					/ model.getProperties().get(Model.Properties.SECONDS_PER_POINT).as(Double.class));
			scale = (double) nMinutesToSimulate / timeTo;

		} catch (AnimoException e1) {
			e1.printStackTrace();
		}
		final JPanel panelReactions = new JPanel(new GridLayout2(
				(int) Math.ceil(model.getReactionCollection().size() / 2.0), 2, 30, 30));
		for (Reaction r : model.getReactionCollection()) {
			if (!r.getEnabled())
				continue;
			if (r.getScenarioCfg() == null) {
				continue;
			}
			addReaction(r);
			final JCheckBox checkReaction = new JCheckBox(r.getName());
			final Reaction reaction = r;
			final JPanel placeForParameters = new JPanel();
			checkReaction.setSelected(false);
			checkReaction.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					placeForParameters.removeAll();
					if (checkReaction.isSelected()) {
						addReactionVariableParameters(placeForParameters, checkReaction, reaction);
						updateTotalComputations();
					} else {
						addReactionFixedParameters(placeForParameters, checkReaction, reaction);
						updateTotalComputations();
					}
					window.validate();
					window.repaint();
				}
			});
			addReactionFixedParameters(placeForParameters, checkReaction, reaction);
			panelReactions.add(placeForParameters);
		}
		JScrollPane scroll = new JScrollPane(panelReactions);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		JComponent comparisonPanel = createReactionComparisonPanel(model);
		JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scroll, comparisonPanel);
		mainSplit.setDividerLocation(0.7);
		mainSplit.setResizeWeight(0.7);
		window.getContentPane().add(mainSplit, BorderLayout.CENTER);
		startExecution = new JButton(BUTTON_START);
		startExecution.addActionListener(new StartActionListener());

		progress = new JProgressBar(0, 100);
		progress.setStringPainted(true);
		progress.setString("Messages");
		Box buttonsBox = new Box(BoxLayout.X_AXIS);
		buttonsBox.add(Box.createGlue());
		buttonsBox.add(progress);
		buttonsBox.add(Box.createGlue());
		buttonsBox.add(startExecution);
		window.getContentPane().add(buttonsBox, BorderLayout.SOUTH);
		window.setBounds((int) (Animo.getCytoscape().getJFrame().getWidth() * 0.2), (int) (Animo.getCytoscape()
				.getJFrame().getHeight() * 0.2), (int) (Animo.getCytoscape().getJFrame().getWidth() * 0.6),
				(int) (Animo.getCytoscape().getJFrame().getHeight() * 0.6));
		if (exitOnClose) {
			window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		} else {
			// TODO: are we sure we dont need to implement the other methods / extend WindowAdapter instead?
			window.addWindowListener(new FrameWindowListener());
		}
		window.setVisible(true);
	}

	private void sortConfigurations(List<AcceptableConfiguration> candidateConfigurations) {
		DecimalFormat decimalFormat = new DecimalFormat("##0.####");
		try {
			for (AcceptableConfiguration tryConfiguration : candidateConfigurations) {
				if (Double.isNaN(tryConfiguration.getErrorValue())) {
					Pair<Boolean, Double> comparisonResult = compareResults(tryConfiguration.getResult());
					tryConfiguration.setErrorValue(comparisonResult.second);
					tryConfiguration.setErrorEstimation("Max abs diff: "
							+ decimalFormat.format(comparisonResult.second));
				}
			}
			Collections.sort(candidateConfigurations);
		} catch (IOException ex) {

		}
	}

	public void stampaRapporto(double a, double b) {
		if (a < b) {
			long estimation = (long) ((System.currentTimeMillis() - startTime) / (a + 1) * (b - a - 1));
			progress.setToolTipText("Estimated remaining time: "
					+ AnimoActionTask.timeDifferenceFormat(estimation / 1000));
		} else {
			long duration = System.currentTimeMillis() - startTime;
			progress.setToolTipText("Process completed in " + AnimoActionTask.timeDifferenceFormat(duration / 1000));
		}
		progress.setValue(progress.getMinimum() + (int) (a / b * (progress.getMaximum() - progress.getMinimum())));
		NumberFormat formatter = new DecimalFormat("#,###");
		progress.setString(formatter.format((int) a) + " / " + formatter.format((int) b));
	}

	public void stampaRapporto(double a, double b, PrintStream out) {
		int rap = (int) (a / b * 10.0);
		int i = 0;
		out.print("[");
		while (i < rap) {
			out.print("#");
			i++;
		}
		while (i < 10) {
			out.print("-");
			i++;
		}
		out.print("]\r");
	}

	private void updateComparisonBox(Box comparisonBox, String csvFileName) throws IOException {
		File f = new File(csvFileName);
		String firstLine;
		if (!f.exists())
			return;
		BufferedReader is = new BufferedReader(new FileReader(f));
		firstLine = is.readLine();
		StringTokenizer tritatutto = new StringTokenizer(firstLine, ",");
		int nColonne = tritatutto.countTokens();
		final String[] graphNames = new String[nColonne - 1];
		tritatutto.nextToken(); // il primo e' la X (tempo)
		for (int i = 0; i < graphNames.length; i++) {
			graphNames[i] = tritatutto.nextToken().replace('\"', ' ');
		}
		comparisonBox.removeAll();
		comparisonBox.add(Box.createGlue());
		for (final Reactant reactant : model.getReactantCollection()) {
			final Box reagentBox = new Box(BoxLayout.X_AXIS);
			List<String> selectedSeries = new ArrayList<String>();
			selectedSeries.add(graphNames[0]);
			final ReactantComparison comparison = new ReactantComparison(csvFileName, selectedSeries, 0.2);
			final JCheckBox checkBox = new JCheckBox(reactant.getName());
			final JComboBox<String> comboBox = new JComboBox<String>(graphNames);
			checkBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					comboBox.setEnabled(checkBox.isSelected());
					if (checkBox.isEnabled()) {
						reactantComparisons.put(reactant, comparison);
					} else {
						reactantComparisons.remove(reactant);
					}
				}
			});
			comboBox.setEnabled(false);
			comboBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					List<String> seriesNames = new ArrayList<String>();
					seriesNames.add(comboBox.getSelectedItem().toString());
					comparison.setSeriesNames(seriesNames);
				}
			});
			reagentBox.add(checkBox);
			reagentBox.add(Box.createGlue());
			reagentBox.add(comboBox);
			reagentBox.add(Box.createGlue());
			comparisonBox.add(reagentBox);
		}
		comparisonBox.add(Box.createGlue());
		is.close();
	}

	private void updateTotalComputations() {
		if (scenarioFittingParameters == null || progress == null)
			return;
		totalComputations = 1;
		for (ScenarioFitting fitting : scenarioFittingParameters.values()) {
			for (ParameterSetting setting : fitting.getParameterSettings().values()) {
				if (!setting.isFixed()) {
					Double min = setting.getMin(), max = setting.getMax(), inc = setting.getIncrease();
					long nSteps = 1;
					if (setting.isLogarithmic()) {
						nSteps = 1 + Math.round(Math.log(max / min) / Math.log(inc));
					} else {
						nSteps = Math.round((max - min) / inc + 1);
					}
					totalComputations *= nSteps;
				}
			}
		}
		NumberFormat formatter = new DecimalFormat("#,###");
		progress.setValue(progress.getMinimum());
		progress.setString(formatter.format(totalComputations) + " total runs needed");
	}

	public void visitParameterSettings(List<Pair<ScenarioCfg, ParameterSetting>> parSettings, int startIndex,
			ThreadPool pool) {
		if (terminate) {
			return;
		}
		if (startIndex > parSettings.size() - 1) {
			// siamo alla fine: dobbiamo solo fare il conto e casomai ritornare
			currentSettingIndex++;
			doCompute(pool);
		} else {
			Pair<ScenarioCfg, ParameterSetting> pair = parSettings.get(startIndex);
			ScenarioCfg cfg = pair.first;
			ParameterSetting parSetting = pair.second;
			boolean isLogarithmicScale = parSetting.isLogarithmic();
			if (isLogarithmicScale) {
				for (Double val = parSetting.getMin(); val <= parSetting.getMax(); val *= parSetting.getIncrease()) {
					// parSetting.getIncrease() gives us the log base if we are on a logarithmic scale
					HashMap<String, Double> params = cfg.getParameters();
					if (params == null) {
						params = new HashMap<String, Double>();
					}
					params.put(parSetting.getName(), val);
					cfg.setParameters(params);
					visitParameterSettings(parSettings, startIndex + 1, pool);
				}
			} else {
				long computationsAtMyLevel = Math.round((parSetting.getMax() - parSetting.getMin())
						/ parSetting.getIncrease() + 1);
				long mySteps = 0;
				for (Double val = parSetting.getMin(); mySteps < computationsAtMyLevel; val += parSetting.getIncrease()) {
					// the termination condition on the for is correct: we don't want to stop too early just because of some rounding problems
					HashMap<String, Double> params = cfg.getParameters();
					if (params == null) {
						params = new HashMap<String, Double>();
					}
					params.put(parSetting.getName(), val);
					cfg.setParameters(params);
					visitParameterSettings(parSettings, startIndex + 1, pool);
					mySteps++;
				}
			}
		}
	}

	class StartActionListener implements ActionListener {

		private int numberOfShownResults = -1;

		@Override
		public void actionPerformed(ActionEvent e) {
			if (startExecution.getText().equals(BUTTON_START)) {
				new StarterThread(numberOfShownResults).start();
			} else {
				startExecution.setText(BUTTON_START);
				terminate = true;
				pool.terminateAll();
			}
		}

	}

	class StarterThread extends Thread {
		private int numberOfShownResults;

		public StarterThread(int numberOfShownResults) {
			this.numberOfShownResults = numberOfShownResults;
		}

		@Override
		public void run() {
			startExecution.setText(BUTTON_CANCEL);
			progress.setValue(progress.getMinimum());
			startTime = System.currentTimeMillis();
			terminate = false;
			acceptableConfigurations = new ArrayList<AcceptableConfiguration>();
			allConfigurations = new ArrayList<AcceptableConfiguration>();
			int nParallelExecutions = 2 * (Runtime.getRuntime().availableProcessors() - 1);
			if (numberOfParallelExecutions.getText() != null) {
				try {
					nParallelExecutions = Integer.parseInt(numberOfParallelExecutions.getText());
				} catch (NumberFormatException e) {
					// nothing to do
				}
				if (nParallelExecutions < 1) {
					nParallelExecutions = 1;
				}
			}
			int valueForSlider = Math.max(nParallelExecutions, parallelExecs.getMinimum());
			valueForSlider = Math.min(valueForSlider, parallelExecs.getMaximum());
			parallelExecs.setValue(valueForSlider);
			numberOfParallelExecutions.setValue(nParallelExecutions);
			pool = new ThreadPool(nParallelExecutions);
			parameterSweep(pool);
			sortConfigurations(allConfigurations);
			showAcceptableGraphsWindow(numberOfShownResults);
			startExecution.setText(BUTTON_START);
		}

		public void showAcceptableGraphsWindow() {
			if (numberOfShownResults == -1) {
				numberOfShownResults = 10;
			}
			showAcceptableGraphsWindow(numberOfShownResults);
		}

		public void showAcceptableGraphsWindow(int numberOfResultsToShow) {
			acceptableGraphsWindow.getContentPane().setLayout(new BorderLayout());
			acceptableGraphsWindow.getContentPane().removeAll();
			final int itemsPerPage = 6;
			final List<Container> pages = new ArrayList<Container>();
			final JLabel showPageNumber = new JLabel("Page 1/1");
			int countItems = 0;
			int countPages = 0;
			DecimalFormat decimalFormat = new DecimalFormat("0.##E0");
			Component lastAdded = null;

			acceptableConfigurations.clear();
			numberOfResultsToShow = Math.min(numberOfResultsToShow, allConfigurations.size());
			for (int i = 0; i < numberOfResultsToShow; i++) {
				acceptableConfigurations.add(allConfigurations.get(i));
			}

			for (final AcceptableConfiguration acceptableConfiguration : acceptableConfigurations) {
				if (acceptableConfiguration == null)
					continue;
				try {
					Graph g = new Graph();

					Map<String, String> seriesNameMapping = new HashMap<String, String>();
					for (String r : acceptableConfiguration.getResult().getReactantIds()) {
						String name = null;
						if (model.getReactant(r) != null) {
							// we can also refer to a name not present in the reactant collection
							name = model.getReactant(r).getName(); // if an alias (canonical name) is set, we prefer it
							if (name == null) {
								name = model.getReactant(r).get(Model.Properties.REACTANT_NAME).as(String.class); //Otherwise, also the cytoscape name may make sense
							}
						}
						seriesNameMapping.put(r, name);
					}

					List<String> shownReactants = new ArrayList<String>();
					List<String> filteredReactants = new ArrayList<String>();
					LevelResult res = acceptableConfiguration.getResult();
					for (Reactant r : reactantComparisons.keySet()) {
						shownReactants.add(r.getName());
					}
					for (Reactant r : model.getReactantCollection()) {
						if (shownReactants.contains(r.getName())) {
							// I comment this so I show all and only the reactants selected for comparison. If you remove this comment, you can also get those reactants marked as
							// plotted, and they will be hidden if they are not in a comparison || r.get(Model.Properties.PLOTTED).as(Boolean.class)) {
							filteredReactants.add(r.getId());
						}
					}
					res = res.filter(filteredReactants);
					g.parseLevelResult(res, seriesNameMapping, scale, shownReactants);
					g.setXSeriesName("Time (min)");
					g.setYLabel("Protein activity (a. u.)");
					if (!model.getProperties().get(Model.Properties.NUMBER_OF_LEVELS).isNull()) {
						// if we find a maximum value for activity levels, we declare it to the graph, so that other added graphs (such as experimental data) will be automatically
						// rescaled to match us
						int nLevels = model.getProperties().get(Model.Properties.NUMBER_OF_LEVELS).as(Integer.class);
						g.declareMaxYValue(nLevels);
					}

					List<String> namesToCompare = new ArrayList<String>();
					String csvFile = null;
					for (Reactant r : reactantComparisons.keySet()) {
						ReactantComparison compare = reactantComparisons.get(r);
						if (compare.getCsvFile() != null && csvFile == null) {
							csvFile = compare.getCsvFile();
						}
						namesToCompare.addAll(compare.getSeriesNames());
						namesToCompare.add(r.getName());
					}
					g.parseCSV(csvFile, namesToCompare);

					StringBuilder builder = new StringBuilder();
					for (Reaction r : scenarioFittingParameters.keySet()) {
						boolean onlyFixed = true;
						for (ParameterSetting parSetting : scenarioFittingParameters.get(r).getParameterSettings()
								.values()) {
							if (!parSetting.isFixed()) {
								onlyFixed = false;
								break;
							}
						}
						if (onlyFixed)
							continue;
						builder.append(r.getName() + " [");
						ScenarioFitting scnFitting = scenarioFittingParameters.get(r);
						for (String parName : scnFitting.getParameterSettings().keySet()) {
							ParameterSetting parSetting = scnFitting.getParameterSettings().get(parName);
							if (!parSetting.isFixed()) {
								builder.append(parName
										+ "="
										+ decimalFormat.format(acceptableConfiguration.getScenarioConfigurations()
												.get(r.getId()).getParameters().get(parName)) + ",");
							}
						}
						builder.append("]; ");
					}
					final String title = builder.toString();
					Box graphBox = new Box(BoxLayout.Y_AXIS);
					graphBox.add(g);
					JButton accept = new JButton("I want this");
					accept.setToolTipText("Set the model parameters to obtain this result");
					accept.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							if (JOptionPane.showConfirmDialog(window, "Are you sure that you want this graph?\n"
									+ title, "Confirm parameter setting choice", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
								return;
							CyNetwork network = Animo.getCytoscapeApp().getCyApplicationManager().getCurrentNetwork();

							for (Reaction r : scenarioFittingParameters.keySet()) {
								ScenarioFitting scnFitting = scenarioFittingParameters.get(r);
								for (String parName : scnFitting.getParameterSettings().keySet()) {
									ParameterSetting parSetting = scnFitting.getParameterSettings().get(parName);
									if (parSetting.isFixed()) {
										CyEdge edge = network.getEdge(Long.parseLong(r.getId()));
										network.getRow(edge).set(parName, parSetting.getFixedValue());
									} else {
										CyEdge edge = network.getEdge(Long.parseLong(r.getId()));
										network.getRow(edge).set(
												parName,
												acceptableConfiguration.getScenarioConfigurations().get(r.getId())
														.getParameters().get(parName));
									}
								}
							}

							acceptableGraphsWindow.dispose();
							window.dispose();
							JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(),
									"The selected parameters have been correctly set in the network");
						}
					});
					Box acceptBox = new Box(BoxLayout.X_AXIS);
					JLabel errorLabel = new JLabel("" + acceptableConfiguration.getErrorValue()) {
						private static final long serialVersionUID = -8694494127364601604L;

						@Override
						public void paint(Graphics g1) {
							Graphics2D g = (Graphics2D) g1;
							double v = -1;
							try {
								v = Double.parseDouble(this.getText());
							} catch (NumberFormatException ex) {
								v = 0.0;
							}
							v = 1 - v;
							g.setPaint(Color.RED);
							g.fillRect(0, 0, this.getWidth(), this.getHeight());
							g.setPaint(Color.GREEN);
							g.fillRect(0, 0, (int) Math.round(v * this.getWidth()), this.getHeight());
						}
					};
					errorLabel.setToolTipText(acceptableConfiguration.getErrorEstimation());
					Dimension labelSize = new Dimension(75, 15);
					errorLabel.setPreferredSize(labelSize);
					errorLabel.setMaximumSize(labelSize);
					acceptBox.add(new JLabel("Fitness: "));
					acceptBox.add(errorLabel);
					acceptBox.add(new JLabel("   "));
					acceptBox.add(accept);
					graphBox.add(acceptBox);
					if (countPages >= pages.size()) {
						pages.add(new JPanel());
						pages.get(countPages).setLayout(new GridLayout(2, itemsPerPage / 2));
					}
					lastAdded = new LabelledField(title, graphBox, title);
					pages.get(countPages).add(lastAdded);
					countItems++;
					if (countItems % itemsPerPage == 0) {
						countPages++;
					}
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
			if (lastAdded != null && countItems < itemsPerPage * (countPages + 1)) {
				// The last page needs "empty" elements to make everybody behave correctly
				int diff = itemsPerPage * (countPages + 1) - countItems;
				for (int i = 0; i < diff; i++) {
					pages.get(countPages).add(Box.createRigidArea(lastAdded.getPreferredSize()));
				}
			}
			if (!pages.isEmpty()) {
				acceptableGraphsWindow.getContentPane().add(pages.get(0), BorderLayout.CENTER);
				JButton prev = new JButton("<--");
				prev.addActionListener(new PrevActionListener(pages, showPageNumber));
				JButton next = new JButton("-->");
				next.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						for (Component c : acceptableGraphsWindow.getContentPane().getComponents()) {
							if (pages.contains(c)) {
								int idx = pages.indexOf(c);
								idx++;
								if (idx >= pages.size()) {
									idx = 0;
								}
								showPageNumber.setText("Page " + (idx + 1) + "/" + pages.size());
								acceptableGraphsWindow.getContentPane().remove(c);
								acceptableGraphsWindow.getContentPane().add(pages.get(idx), BorderLayout.CENTER);
								acceptableGraphsWindow.validate();
								acceptableGraphsWindow.repaint();
								break;
							}
						}
					}
				});
				Box boxButtons = new Box(BoxLayout.X_AXIS);
				boxButtons.add(prev);
				boxButtons.add(Box.createGlue());
				showPageNumber.setText("Page 1/" + pages.size());
				boxButtons.add(showPageNumber);
				boxButtons.add(Box.createGlue());
				boxButtons.add(next);
				acceptableGraphsWindow.getContentPane().add(boxButtons, BorderLayout.SOUTH);
			}
			final Box filterButtonsBox = new Box(BoxLayout.X_AXIS);
			JButton filter = new JButton("Filter");
			final JFormattedTextField resultsToShow = new JFormattedTextField(numberOfResultsToShow);
			Dimension prefSize = resultsToShow.getPreferredSize();
			prefSize.width *= 1.5;
			resultsToShow.setPreferredSize(prefSize);
			resultsToShow.setMaximumSize(prefSize);
			JLabel labelShow = new JLabel("Show the best "), labelResults = new JLabel(" results  ");
			filterButtonsBox.add(labelShow);
			filterButtonsBox.add(resultsToShow);
			filterButtonsBox.add(labelResults);
			filter.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {

					int nResults = 0;
					try {
						nResults = Integer.parseInt(resultsToShow.getValue().toString());
					} catch (NumberFormatException ex) {
						nResults = 0;
					}
					showAcceptableGraphsWindow(nResults);
				}
			});
			filterButtonsBox.add(filter);
			JButton showAll = new JButton("Show all");
			showAll.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					showAcceptableGraphsWindow(allConfigurations.size());
				}
			});
			filterButtonsBox.add(showAll);
			acceptableGraphsWindow.getContentPane().add(filterButtonsBox, BorderLayout.NORTH);
			acceptableGraphsWindow.setBounds(window.getBounds());
			int actuallyAccepted = 0;
			for (int i = 0; i < acceptableConfigurations.size(); i++) {
				if (acceptableConfigurations.get(i) != null)
					actuallyAccepted++;
			}
			acceptableGraphsWindow.setTitle(WINDOW_TITLE + ": " + allConfigurations.size() + " configurations tried, "
					+ actuallyAccepted + " shown.");
			acceptableGraphsWindow.validate();
			acceptableGraphsWindow.getContentPane().validate();
			acceptableGraphsWindow.setVisible(true);
		}
	}

	class PrevActionListener implements ActionListener {
		private List<Container> pages;
		private JLabel showPageNumber;

		public PrevActionListener(List<Container> pages, JLabel showPageNumber) {
			this.pages = pages;
			this.showPageNumber = showPageNumber;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			for (Component c : acceptableGraphsWindow.getContentPane().getComponents()) {
				if (pages.contains(c)) {
					int idx = pages.indexOf(c);
					idx--;
					if (idx < 0) {
						idx = pages.size() - 1;
					}
					showPageNumber.setText("Page " + (idx + 1) + "/" + pages.size());
					acceptableGraphsWindow.getContentPane().remove(c);
					acceptableGraphsWindow.getContentPane().add(pages.get(idx), BorderLayout.CENTER);
					acceptableGraphsWindow.validate();
					acceptableGraphsWindow.repaint();
					break;
				}
			}
		}
	}

	class FrameWindowListener implements WindowListener {

		@Override
		public void windowActivated(WindowEvent e) {
		}

		@Override
		public void windowClosed(WindowEvent e) {
		}

		@Override
		public void windowClosing(WindowEvent e) {
			acceptableGraphsWindow.dispose();
			e.getWindow().dispose();
		}

		@Override
		public void windowDeactivated(WindowEvent e) {
		}

		@Override
		public void windowDeiconified(WindowEvent e) {
		}

		@Override
		public void windowIconified(WindowEvent e) {
		}

		@Override
		public void windowOpened(WindowEvent e) {
		}

	}

	class ParameterDocumentListener implements DocumentListener {

		private Reaction reaction;
		private String parName;
		private JFormattedTextField paramValue;

		public ParameterDocumentListener(Reaction reaction, String parName, JFormattedTextField paramValue) {
			this.reaction = reaction;
			this.parName = parName;
			this.paramValue = paramValue;
		}

		@Override
		public void changedUpdate(DocumentEvent e) {
			setFixedParameter(reaction, parName, new Double(paramValue.getValue().toString()));
			updateTotalComputations();
		}

		@Override
		public void insertUpdate(DocumentEvent e) {
			changedUpdate(e);
		}

		@Override
		public void removeUpdate(DocumentEvent e) {
			changedUpdate(e);
		}
	}

	class ScaleActionListener implements ActionListener {

		private JRadioButton linearScale;
		private LabelledField incrementField;

		public ScaleActionListener(JRadioButton linearScale, LabelledField incrementField) {
			this.linearScale = linearScale;
			this.incrementField = incrementField;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (linearScale.isSelected()) {
				incrementField.setTitle("Increment");
			} else {
				incrementField.setTitle("Log base");
			}

		}

	}

	class MinValueDocumentListener implements DocumentListener {
		private Reaction reaction;
		private String parName;
		private JFormattedTextField maxValue;
		private JFormattedTextField minValue;
		private JFormattedTextField incrementValue;
		private JRadioButton logarithmicScale;

		public MinValueDocumentListener(Reaction reaction, String parName, JFormattedTextField maxValue,
				JFormattedTextField minValue, JFormattedTextField incrementValue, JRadioButton logarithmicScale) {
			this.reaction = reaction;
			this.parName = parName;
			this.maxValue = maxValue;
			this.minValue = minValue;
			this.incrementValue = incrementValue;
			this.logarithmicScale = logarithmicScale;
		}

		@Override
		public void changedUpdate(DocumentEvent e) {
			setVariableParameter(reaction, parName, new Double(minValue.getValue().toString()), new Double(maxValue
					.getValue().toString()), new Double(incrementValue.getValue().toString()),
					logarithmicScale.isSelected());
			updateTotalComputations();
		}

		@Override
		public void insertUpdate(DocumentEvent e) {
			changedUpdate(e);
		}

		@Override
		public void removeUpdate(DocumentEvent e) {
			changedUpdate(e);
		}
	}

	class CSVDocumentListener implements DocumentListener {
		private Box comparisonBox;
		private JTextField csvFileNameTextField;
		private JPanel comparisonPanel;

		public CSVDocumentListener(Box comparisonBox, JTextField csvFileNameTextField, JPanel comparisonPanel) {
			this.comparisonBox = comparisonBox;
			this.csvFileNameTextField = csvFileNameTextField;
			this.comparisonPanel = comparisonPanel;
		}

		@Override
		public void changedUpdate(DocumentEvent e) {
			try {
				updateComparisonBox(comparisonBox, csvFileNameTextField.getText());
			} catch (IOException ex) {
			}
			comparisonPanel.validate();
		}

		@Override
		public void insertUpdate(DocumentEvent e) {
			changedUpdate(e);
		}

		@Override
		public void removeUpdate(DocumentEvent e) {
			changedUpdate(e);
		}
	}

	class OpenCSVActionListener implements ActionListener {
		private JTextField csvFileNameTextField;

		public OpenCSVActionListener(JTextField csvFileNameTextField) {
			this.csvFileNameTextField = csvFileNameTextField;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			String oldFile = csvFileNameTextField.getText();
			String file = FileUtils.open(".csv", "CSV file", Animo.getCytoscape().getJFrame());
			if (file != null && !file.equals(oldFile)) {
				csvFileNameTextField.setText(file);
			}
		}
	}

	class ParallelExecsChangeListener implements ChangeListener {
		@Override
		public void stateChanged(ChangeEvent arg0) {
			numberOfParallelExecutions.setValue(Integer.valueOf(parallelExecs.getValue()));
		}
	}

	class TaskThread extends Thread {
		private Model analyzedModel;
		private HashMap<String, ScenarioCfg> currentConfiguration;
		private int actualIndexToRemember;
		private final DecimalFormat decimalFormat = new DecimalFormat("##0.####");

		public TaskThread(Model analyzedModel, HashMap<String, ScenarioCfg> currentConfiguration,
				int actualIndexToRemember) {
			this.analyzedModel = analyzedModel;
			this.currentConfiguration = currentConfiguration;
			this.actualIndexToRemember = actualIndexToRemember;
		}

		@Override
		public void run() {
			try {
				for (Reaction r : analyzedModel.getReactionCollection()) {
					ScenarioCfg cfg = currentConfiguration.get(r.getId());
					for (String pn : cfg.getParameters().keySet()) {
						r.let(pn).be(cfg.getParameters().get(pn));
					}
				}
				LevelResult result = new UppaalModelAnalyserSMC(null, null).analyze(analyzedModel, timeTo);
				Pair<Boolean, Double> comparisonResult = compareResults(result);
				allConfigurations.set(actualIndexToRemember, new AcceptableConfiguration(currentConfiguration, result,
						"Max abs diff: " + decimalFormat.format(comparisonResult.second)));
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			synchronized (System.out) {
				nComputations++;
				stampaRapporto(nComputations, totalComputations);
			}
		}
	}

}
