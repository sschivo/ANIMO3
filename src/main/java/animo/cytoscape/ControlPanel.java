package animo.cytoscape;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.text.DecimalFormat;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.model.CyNetwork;

import animo.core.AnimoBackend;
import animo.core.analyser.uppaal.UppaalModelAnalyserSMC;
import animo.core.graph.FileUtils;
import animo.fitting.bruteforce.BruteforceParameterFitter;
import animo.util.XmlConfiguration;

public class ControlPanel extends JPanel implements CytoPanelComponent {
	private static final long serialVersionUID = 5342179061558497910L;

	final JButton changeSecondsPerPointbutton;
	private ColorsLegend colorsLegend;
	private ShapesLegend shapesLegend;

	private class ParameterFitterListener implements ActionListener {
		private JFormattedTextField timeTo;

		public ParameterFitterListener(JFormattedTextField timeTo) {
			this.timeTo = timeTo;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			BruteforceParameterFitter fitter = new BruteforceParameterFitter();
			fitter.showWindow(false, Integer.parseInt(timeTo.getValue().toString()));
		}
	}
	
	public ColorsLegend getColorsLegend() {
		return colorsLegend;
	}
	
	public ShapesLegend getShapesLegend() {
		return shapesLegend;
	}

	public ControlPanel() {
		final XmlConfiguration configuration = AnimoBackend.get().configuration();
		final boolean areWeTheDeveloper = Animo.areWeTheDeveloper();
		
//		final JPanel panel = this;
//		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		// the button container
//		JPanel buttons = new JPanel();
		JPanel buttons = this;
		buttons.setLayout(new GridBagLayout());
		int yPositionCounter = 0; // The Y position of the various elements to be put in the GridBagLayout. TODO: remember to increment it each time you add an element to the
									// buttons JPanel, and to use it as index for the y position

		// This part allows the user to choose whether to perform the computations on the local machine or on a remote machine.
//		Box uppaalBox = new Box(BoxLayout.Y_AXIS);
		final JCheckBox remoteUppaal = new JCheckBox("Remote");
		final Box serverBox = new Box(BoxLayout.Y_AXIS);
		final JTextField serverName = new JTextField("my.server.com"), serverPort = new JFormattedTextField("1234");
		remoteUppaal.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				boolean sel = remoteUppaal.isSelected();
				serverName.setEnabled(sel);
				serverPort.setEnabled(sel);
			}
		});
		remoteUppaal.setSelected(false);
		serverName.setEnabled(false);
		serverPort.setEnabled(false);
		serverBox.add(serverName);
		serverBox.add(serverPort);
		Box sBox = new Box(BoxLayout.X_AXIS);
		sBox.add(new JLabel("Server"));
		sBox.add(serverName);
		serverBox.add(Box.createVerticalStrut(10));
		serverBox.add(sBox);
		Box pBox = new Box(BoxLayout.X_AXIS);
		pBox.add(new JLabel("Port"));
		pBox.add(serverPort);
		serverBox.add(Box.createVerticalStrut(10));
		serverBox.add(pBox);
		serverBox.add(Box.createVerticalStrut(10));

		remoteUppaal.setOpaque(true);
		
//		final ComponentTitledBorder border = new ComponentTitledBorder(remoteUppaal, serverBox,
//				BorderFactory.createEtchedBorder());
//		serverBox.setBorder(border);
		CollapsiblePanel serverCollapsible = new CollapsiblePanel(remoteUppaal);
		serverCollapsible.getContentPane().add(serverBox);
		
//		uppaalBox.add(serverBox);
		// buttons.add(uppaalBox);
		if (areWeTheDeveloper) {
			buttons.add(serverCollapsible, new GridBagConstraints(0, yPositionCounter++, 1, 1, 1, 0, GridBagConstraints.CENTER,
					GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		}

		// This part allows the user to choose between simulation run(s) and Statistical Model Checking
		final JRadioButton normalUppaal = new JRadioButton("Simulation"), smcUppaal = new JRadioButton("SMC");
		ButtonGroup modelCheckingGroup = new ButtonGroup();
		modelCheckingGroup.add(normalUppaal);
		modelCheckingGroup.add(smcUppaal);

		final JCheckBox multipleRuns = new JCheckBox("Compute");
		final JRadioButton overlayPlot = new JRadioButton("Overlay plot"), computeAvgStdDev = new JRadioButton(
				"Average and std deviation"); // "Show standard deviation as error bars");
		ButtonGroup multipleRunsGroup = new ButtonGroup();
		multipleRunsGroup.add(overlayPlot);
		multipleRunsGroup.add(computeAvgStdDev);
		computeAvgStdDev.setSelected(true);
		computeAvgStdDev.setToolTipText(computeAvgStdDev.getText());
		overlayPlot.setToolTipText("Plot all run results one above the other");
		final JFormattedTextField timeTo = new JFormattedTextField(240);
		final JFormattedTextField nSimulationRuns = new JFormattedTextField(10);
		final JTextField smcFormula = new JTextField("Pr[<=50](<> MK2 > 50)");
		timeTo.setToolTipText("Plot activity levels up to this time point (real-life MINUTES).");
		nSimulationRuns
				.setToolTipText("Number of simulations of which to show the average. NO statistical guarantees!");
		smcFormula.setToolTipText("Give an answer to this probabilistic query (times in real-life MINUTES).");
		normalUppaal.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (normalUppaal.isSelected()) {
					timeTo.setEnabled(true);
					multipleRuns.setEnabled(true);
					nSimulationRuns.setEnabled(multipleRuns.isSelected());
					computeAvgStdDev.setEnabled(multipleRuns.isSelected());
					overlayPlot.setEnabled(multipleRuns.isSelected());
					smcFormula.setEnabled(false);
				} else {
					timeTo.setEnabled(false);
					multipleRuns.setEnabled(false);
					nSimulationRuns.setEnabled(false);
					computeAvgStdDev.setEnabled(false);
					overlayPlot.setEnabled(false);
					smcFormula.setEnabled(true);
				}
			}
		});
		multipleRuns.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (multipleRuns.isSelected() && normalUppaal.isSelected()) {
					nSimulationRuns.setEnabled(true);
					computeAvgStdDev.setEnabled(true);
					overlayPlot.setEnabled(true);
				} else {
					nSimulationRuns.setEnabled(false);
					computeAvgStdDev.setEnabled(false);
					overlayPlot.setEnabled(false);
				}
			}
		});
		normalUppaal.setSelected(true);
		smcUppaal.setSelected(false);
		timeTo.setEnabled(true);
		multipleRuns.setEnabled(true);
		multipleRuns.setSelected(false);
		computeAvgStdDev.setEnabled(false);
		computeAvgStdDev.setSelected(true);
		overlayPlot.setEnabled(false);
		nSimulationRuns.setEnabled(false);
		smcFormula.setEnabled(false);
		Box modelCheckingBox = new Box(BoxLayout.Y_AXIS);
		final Box normalBox = new Box(BoxLayout.Y_AXIS);
		Box smcBox = new Box(BoxLayout.X_AXIS);
		Box timeToBox = new Box(BoxLayout.X_AXIS);
		timeToBox.add(new JLabel("until"));
		timeToBox.add(timeTo);
		timeToBox.add(new JLabel("minutes"));
		normalBox.add(timeToBox);
		Box averageBox = new Box(BoxLayout.X_AXIS);
		averageBox.add(multipleRuns);
		averageBox.add(nSimulationRuns);
		averageBox.add(new JLabel("runs"));
		normalBox.add(averageBox);
		Box stdDevBox = new Box(BoxLayout.X_AXIS);
		stdDevBox.add(computeAvgStdDev);
		stdDevBox.add(Box.createGlue());
		normalBox.add(stdDevBox);
		Box overlayPlotBox = new Box(BoxLayout.X_AXIS);
		overlayPlotBox.add(overlayPlot);
		overlayPlotBox.add(Box.createGlue());
		normalBox.add(overlayPlotBox);
		smcBox.add(smcUppaal);
		smcBox.add(smcFormula);
		normalUppaal.setOpaque(true);

		JButton loadSimulationDataButton = new JButton(new AbstractAction("Load simulation data...") {
			private static final long serialVersionUID = -998176729911500957L;

			@Override
			public void actionPerformed(ActionEvent e) {
				CyNetwork net = Animo.getCytoscapeApp().getCyApplicationManager().getCurrentNetwork();
				if (net != null) {
					String inputFileName = FileUtils.open(".sim", Animo.APP_NAME + " simulation", null, Animo.getCytoscape().getJFrame());
					if (inputFileName == null)
						return;
					final AnimoResultPanel resultPanel = new AnimoResultPanel(new File(inputFileName));
					resultPanel.addToPanel(Animo.getCytoscape().getCytoPanel(CytoPanelName.EAST));

				} else {
					JOptionPane
							.showMessageDialog(
									Animo.getCytoscape().getJFrame(),
									"There is no current network to which to associate the simulation data.\nPlease load a network first.",
									"No current network", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		Box loadSimulationBox = new Box(BoxLayout.X_AXIS);
		loadSimulationBox.add(Box.createGlue());
		loadSimulationBox.add(loadSimulationDataButton);
		loadSimulationBox.add(Box.createGlue());
		// buttonsBox.add(loadSimulationBox);
		modelCheckingBox.add(loadSimulationBox);

		modelCheckingBox.add(new LabelledField("Simulation", normalBox));
		buttons.add(modelCheckingBox, new GridBagConstraints(0, yPositionCounter++, 1, 1, 1, 0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		Box buttonsBox = new Box(BoxLayout.Y_AXIS);

		changeSecondsPerPointbutton = new JButton();
		changeSecondsPerPointbutton
				.setToolTipText("Click here to change the number of seconds to which a single simulation step corresponds");
		new ChangeSecondsAction(Animo.getCytoscapeApp().getCyApplicationManager(), changeSecondsPerPointbutton); // This manages the button for changing the number of seconds per
																													// UPPAAL time unit
		Box changeSecondsPerPointbuttonBox = new Box(BoxLayout.X_AXIS);
		changeSecondsPerPointbuttonBox.add(Box.createGlue());
		changeSecondsPerPointbuttonBox.add(changeSecondsPerPointbutton);
		changeSecondsPerPointbuttonBox.add(Box.createGlue());
		//buttonsBox.add(changeSecondsPerPointbuttonBox); Don't want the user to mess with a parameter that is all but intuitive

		// The "Parameter Fitter"
		// if (areWeTheDeveloper) {
		JButton parameterFit = new JButton("Parameter fitter...");
		parameterFit.addActionListener(new ParameterFitterListener(timeTo));
		Box parameterFitBox = new Box(BoxLayout.X_AXIS);
		parameterFitBox.add(Box.createGlue());
		parameterFitBox.add(parameterFit);
		parameterFitBox.add(Box.createGlue());
		buttonsBox.add(parameterFitBox);
		// }

		// The "Analyse network" button: perform the requested analysis on the current network with the given parameters
		final JButton runButton = new JButton(new RunAction(remoteUppaal, serverName, serverPort, smcUppaal, timeTo,
				nSimulationRuns, computeAvgStdDev, overlayPlot, smcFormula));
		Box runButtonBox = new Box(BoxLayout.X_AXIS);
		runButtonBox.add(Box.createGlue());
		runButtonBox.add(runButton);
		runButtonBox.add(Box.createGlue());
		JButton mcButton = new JButton(new ModelCheckAction());
		runButtonBox.add(mcButton);
		runButtonBox.add(Box.createGlue());
		buttonsBox.add(runButtonBox);

		buttons.add(buttonsBox, new GridBagConstraints(0, yPositionCounter++, 1, 1, 0, 0, GridBagConstraints.CENTER,
				GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		

		timeTo.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
				if (e.getKeyChar() == KeyEvent.VK_ENTER) { //Pressing enter when editing the "time to" text box makes the simulation start
					runButton.doClick();
				}
			}

			@Override
			public void keyPressed(KeyEvent e) {
			}

			@Override
			public void keyReleased(KeyEvent e) {
			}
		});
		
		colorsLegend = new ColorsLegend();
		shapesLegend = new ShapesLegend();
		JPanel panelLegends = new JPanel();
		panelLegends.setBackground(Color.WHITE);
		panelLegends.setLayout(new GridBagLayout());
		panelLegends.add(colorsLegend, new GridBagConstraints(0, 0, 1, 1, 1, 0.5, GridBagConstraints.CENTER,
				GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		panelLegends.add(shapesLegend, new GridBagConstraints(0, 1, 1, 1, 1, 0.5, GridBagConstraints.CENTER,
				GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		panelLegends.setPreferredSize(new Dimension(200, 400));
		buttons.add(new LabelledField("Legend",
				// panelLegends),
				new JScrollPane(panelLegends, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED)),
				new GridBagConstraints(0, yPositionCounter++, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(30, 5, 5, 5), 0, 0));
		
		// The options dialog
		JButton options = new JButton("Options...");
		options.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final JDialog optionsDialog = new JDialog(Animo.getCytoscape().getJFrame(), Animo.APP_NAME + " Options",
						Dialog.ModalityType.APPLICATION_MODAL);
				optionsDialog.getContentPane().setLayout(new BorderLayout());
				JPanel content = new JPanel(new GridBagLayout());
				String location = configuration.get(XmlConfiguration.VERIFY_KEY);
				final JLabel verifytaLocation = new JLabel(location);
				verifytaLocation.setToolTipText(location);
				JPanel verifytaPanel = new JPanel(new BorderLayout());
				verifytaPanel.add(verifytaLocation, BorderLayout.CENTER);
				final JButton changeLocation = new JButton("Change...");
				changeLocation.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent ev) {
						String verifytaFileName = "verifyta";
						if (UppaalModelAnalyserSMC.areWeUnderWindows()) {
							verifytaFileName += ".exe";
						}
						JOptionPane
								.showMessageDialog(
										Animo.getCytoscape().getJFrame(),
										"Please, find and select the \"verifyta\" tool.\nIt is usually located in the \"bin\" directory of UPPAAL.",
										"Verifyta", JOptionPane.QUESTION_MESSAGE);
						String verifytaFileLocation = FileUtils.open(verifytaFileName, "Verifyta Executable",
								"Open UPPAAL verifyta program",
								optionsDialog);
						if (verifytaFileLocation != null) {
							verifytaLocation.setText(verifytaFileLocation);
							verifytaLocation.setToolTipText(verifytaFileName);
						}
					}
				});
				verifytaPanel.add(changeLocation, BorderLayout.EAST);
				content.add(new LabelledField("UPPAAL verifyta location", verifytaPanel), new GridBagConstraints(0, 0,
						1, 1, 1.0, 0.5, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0,
						0));

				final String useUncertaintyTitle = "Use uncertainty value (in %): ", noUncertaintyTitle = "Uncertainty is 0";
				final JCheckBox useUncertainty = new JCheckBox(useUncertaintyTitle);
				Integer unc = 5;
				try {
					unc = Integer.valueOf(configuration.get(XmlConfiguration.UNCERTAINTY_KEY));
				} catch (NumberFormatException ex) {
					unc = 5;
				} catch (NullPointerException ex) { // If the property wasn't there, we shouldn't assume that the user knows of its existence: so we make its effects null
					unc = 0;
				}
				final JFormattedTextField uncertainty = new JFormattedTextField(unc);
				Dimension dim = uncertainty.getPreferredSize();
				dim.setSize(dim.getWidth() * 1.5, dim.getHeight());
				uncertainty.setPreferredSize(dim);
				useUncertainty.setSelected(true);
				useUncertainty.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent ev) {
						uncertainty.setVisible(useUncertainty.isSelected());
						if (useUncertainty.isSelected()) {
							useUncertainty.setText(useUncertaintyTitle);
						} else {
							useUncertainty.setText(noUncertaintyTitle);
						}
					}
				});
				if (unc == 0) {
					useUncertainty.setSelected(false);
					useUncertainty.setText(noUncertaintyTitle);
					uncertainty.setVisible(useUncertainty.isSelected());
				}
				JPanel uncertaintyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
				uncertaintyPanel.add(useUncertainty);
				uncertaintyPanel.add(uncertainty);
				content.add(new LabelledField("Reaction parameters uncertainty", uncertaintyPanel),
						new GridBagConstraints(0, 1, 1, 1, 1.0, 0.5, GridBagConstraints.CENTER,
								GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

				final String reactionCenteredTitle = "Reaction-centered model without pre-computed tables",
							 reactionCenteredTablesTitle = "Reaction-centered model with pre-computed tables",
							 reactionCenteredTablesOldTitle = "Reaction-centered model with pre-computed tables, old version",
							 reactantCenteredTitle = "Reactant-centered model (less precise)",
							 reactantCenteredMorePreciseTitle = "Reactant-centered model (recommended)", // (model B)
							 reactantCenteredMorePreciseNewTitle = "Reactant-centered model, more precise  (model Q)",
							 reactantCenteredOpaalTitle = "Reactant-centered for multi-core analysis",
							 ODEmodelTitle = "Ordinary Differential Equations (ODEs) for UPPAAL";
				final JRadioButton useReactionCentered = new JRadioButton(reactionCenteredTitle),
								   useReactionCenteredTables = new JRadioButton(reactionCenteredTablesTitle),
								   useReactionCenteredTablesOld = new JRadioButton(reactionCenteredTablesOldTitle),
								   useReactantCentered = new JRadioButton(reactantCenteredTitle),
								   useReactantCenteredMorePrecise = new JRadioButton(reactantCenteredMorePreciseTitle),
								   useReactantCenteredMorePreciseNew = new JRadioButton(reactantCenteredMorePreciseNewTitle),
								   useReactantCenteredOpaal = new JRadioButton(reactantCenteredOpaalTitle),
								   useODEmodel = new JRadioButton(ODEmodelTitle);
//				useReactionCentered.setToolTipText("Advised when the network is not reaction-heavy. (generally NOT recommended)");
//				useReactionCenteredTables.setToolTipText("Advised when the network is not reaction-heavy. It is slower and uses more memory. (NOT recommended)");
//				useReactantCentered.setToolTipText("Faster method, especially with reaction-heavy networks (recommended)");
				useReactionCentered.setToolTipText("Proof of concept, NOT recommended");
				useReactionCenteredTables.setToolTipText("Slow and uses more memory than reactant-centered. NOT recommended");
				useReactionCenteredTablesOld.setToolTipText("Even worse than the one with plain tables. Definitely NOT recommended");
				useReactantCentered.setToolTipText("Fast and uses little memory. Experimental");
				useReactantCenteredMorePrecise.setToolTipText("Fast and uses little memory. More precise, recommended");
				useReactantCenteredMorePreciseNew.setToolTipText("Fast and uses little memory. More precise (different approach), experimental");
				useReactantCenteredOpaal
						.setToolTipText("Reactant-centered model for use the generated model with opaal and ltsmin");
				useODEmodel.setToolTipText("Ordinary Differential Equations model (always deterministic!) to be analyzed with UPPAAL's solver");
				final ButtonGroup reactionCenteredGroup = new ButtonGroup();
				reactionCenteredGroup.add(useReactionCentered);
				reactionCenteredGroup.add(useReactionCenteredTables);
				reactionCenteredGroup.add(useReactionCenteredTablesOld);
				reactionCenteredGroup.add(useReactantCentered);
				reactionCenteredGroup.add(useReactantCenteredMorePrecise);
				reactionCenteredGroup.add(useReactantCenteredMorePreciseNew);
				reactionCenteredGroup.add(useReactantCenteredOpaal);
				reactionCenteredGroup.add(useODEmodel);
				String modelType = null;
				modelType = configuration.get(XmlConfiguration.MODEL_TYPE_KEY);
				useReactionCenteredTablesOld.setSelected(false);
				useReactantCenteredMorePrecise.setSelected(false);
				useReactantCenteredMorePreciseNew.setSelected(false);
				useODEmodel.setSelected(false);
				if (modelType.equals(XmlConfiguration.MODEL_TYPE_REACTION_CENTERED)) {
					useReactionCentered.setSelected(true);
					useReactionCenteredTables.setSelected(false);
					useReactantCentered.setSelected(false);
					useReactantCenteredOpaal.setSelected(false);
				} else if (modelType.equals(XmlConfiguration.MODEL_TYPE_REACTION_CENTERED_TABLES)) {
					useReactionCentered.setSelected(false);
					useReactionCenteredTables.setSelected(true);
					useReactantCentered.setSelected(false);
					useReactantCenteredOpaal.setSelected(false);
				}  else if (modelType.equals(XmlConfiguration.MODEL_TYPE_REACTION_CENTERED_TABLES_OLD)) {
					useReactionCentered.setSelected(false);
					useReactionCenteredTables.setSelected(false);
					useReactionCenteredTablesOld.setSelected(true);
					useReactantCentered.setSelected(false);
					useReactantCenteredOpaal.setSelected(false);
				} else if (modelType.equals(XmlConfiguration.MODEL_TYPE_REACTANT_CENTERED)) {
					useReactionCentered.setSelected(false);
					useReactionCenteredTables.setSelected(false);
					useReactantCentered.setSelected(true);
					useReactantCenteredOpaal.setSelected(false);
				} else if (modelType.equals(XmlConfiguration.MODEL_TYPE_REACTANT_CENTERED_MORE_PRECISE)) {
					useReactionCentered.setSelected(false);
					useReactionCenteredTables.setSelected(false);
					useReactantCentered.setSelected(false);
					useReactantCenteredMorePrecise.setSelected(true);
					useReactantCenteredOpaal.setSelected(false);
				} else if (modelType.equals(XmlConfiguration.MODEL_TYPE_REACTANT_CENTERED_MORE_PRECISE_NEW)) {
					useReactionCentered.setSelected(false);
					useReactionCenteredTables.setSelected(false);
					useReactantCentered.setSelected(false);
					useReactantCenteredMorePreciseNew.setSelected(true);
					useReactantCenteredOpaal.setSelected(false);
				} else if (modelType.equals(XmlConfiguration.MODEL_TYPE_REACTANT_CENTERED_OPAAL)) {
					useReactionCentered.setSelected(false);
					useReactionCenteredTables.setSelected(false);
					useReactantCentered.setSelected(false);
					useReactantCenteredOpaal.setSelected(true);
				} else if (modelType.equals(XmlConfiguration.MODEL_TYPE_ODE)) {
					useReactionCentered.setSelected(false);
					useReactionCenteredTables.setSelected(false);
					useReactantCentered.setSelected(false);
					useReactantCenteredOpaal.setSelected(false);
					useODEmodel.setSelected(true);
				} else {
					useReactionCentered.setSelected(false);
					useReactionCenteredTables.setSelected(false);
					useReactantCentered.setSelected(true);
					useReactantCenteredOpaal.setSelected(false);
				}
				Box modelTypePanel = new Box(BoxLayout.Y_AXIS);//new JPanel(new FlowLayout(FlowLayout.LEFT));
				modelTypePanel.add(useReactantCenteredMorePrecise);
				if (areWeTheDeveloper) {
					modelTypePanel.add(useReactantCentered);
					modelTypePanel.add(useReactantCenteredMorePreciseNew);
					modelTypePanel.add(useReactantCenteredOpaal);
				}
				modelTypePanel.add(useReactionCentered);
//				if (areWeTheDeveloper) {
					modelTypePanel.add(useReactionCenteredTables);
//				}
				if (areWeTheDeveloper) {
					modelTypePanel.add(useReactionCenteredTablesOld);
					modelTypePanel.add(useODEmodel);
				}
				content.add(new LabelledField("Model type", modelTypePanel), new GridBagConstraints(0, 2, 1, 1, 1.0,
						0.5, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

				JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
				JButton okButton = new JButton("OK"), cancelButton = new JButton("Cancel");
				okButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent ev) {
						String verifyta = verifytaLocation.getText();
						configuration.set(XmlConfiguration.VERIFY_KEY, verifyta);
						String uncertaintyValue = "5";
						if (useUncertainty.isSelected()) {
							uncertaintyValue = uncertainty.getText();
							try {
								uncertaintyValue = "" + Integer.valueOf(uncertaintyValue);
							} catch (NumberFormatException ex) {
								JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(), "\"" + uncertaintyValue
										+ "\" is not a number.", "Error", JOptionPane.ERROR_MESSAGE);
								return;
							}
						} else {
							uncertaintyValue = "0";
						}
						configuration.set(XmlConfiguration.UNCERTAINTY_KEY, uncertaintyValue);
						String modelTypeValue = XmlConfiguration.DEFAULT_MODEL_TYPE;
						if (useReactionCentered.isSelected()) {
							modelTypeValue = XmlConfiguration.MODEL_TYPE_REACTION_CENTERED;
						} else if (useReactionCenteredTables.isSelected()) {
							modelTypeValue = XmlConfiguration.MODEL_TYPE_REACTION_CENTERED_TABLES;
						} else if (useReactionCenteredTablesOld.isSelected()) {
							modelTypeValue = XmlConfiguration.MODEL_TYPE_REACTION_CENTERED_TABLES_OLD;
						} else if (useReactantCentered.isSelected()) {
							modelTypeValue = XmlConfiguration.MODEL_TYPE_REACTANT_CENTERED;
						} else if (useReactantCenteredMorePrecise.isSelected()) {
							modelTypeValue = XmlConfiguration.MODEL_TYPE_REACTANT_CENTERED_MORE_PRECISE;
						} else if (useReactantCenteredMorePreciseNew.isSelected()) {
							modelTypeValue = XmlConfiguration.MODEL_TYPE_REACTANT_CENTERED_MORE_PRECISE_NEW;
						} else if (useReactantCenteredOpaal.isSelected()) {
							modelTypeValue = XmlConfiguration.MODEL_TYPE_REACTANT_CENTERED_OPAAL;
						} else if (useODEmodel.isSelected()) {
							modelTypeValue = XmlConfiguration.MODEL_TYPE_ODE;
						} else {
							modelTypeValue = XmlConfiguration.DEFAULT_MODEL_TYPE;
						}
						configuration.set(XmlConfiguration.MODEL_TYPE_KEY, modelTypeValue);
						try {
							configuration.writeConfigFile();
						} catch (Exception ex) {
							JOptionPane
									.showMessageDialog(Animo.getCytoscape().getJFrame(), "Unexpected problem: " + ex);
							ex.printStackTrace();
						}
						optionsDialog.dispose();
					}
				});
				cancelButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent ev) {
						optionsDialog.dispose();
					}
				});
				buttonsPanel.add(okButton);
				buttonsPanel.add(cancelButton);
				optionsDialog.getContentPane().add(content, BorderLayout.CENTER);
				optionsDialog.getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
				optionsDialog.pack();
				optionsDialog.setLocationRelativeTo(null);
				optionsDialog.setVisible(true);
			}
		});
		buttons.add(options, new GridBagConstraints(0, yPositionCounter++, 1, 1, 1, 0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		
//		if (areWeTheDeveloper) {
//			JButton bottoneDeiParametri = new JButton("Test Levenberg-Marquardt method");
//			bottoneDeiParametri.addActionListener(new ActionListener() {
//				@Override
//				public void actionPerformed(ActionEvent e) {
//					//new LevenbergMarquardtFitter().vai();
//					CyNetwork currentNetwork = Animo.getCytoscapeApp().getCyApplicationManager().getCurrentNetwork();
//					if (currentNetwork == null) {
//						return;
//					}
//					int timeTo = 240;
//					Model model = null;
//					try {
//						model = Model.generateModelFromCurrentNetwork(null, timeTo, false);
//					} catch (Exception ex) {
//						ex.printStackTrace(System.err);
//						return;
//					}
//					List<Reaction> reactionsToBeOptimized = new Vector<Reaction>(model.getReactionCollection());
//					String referenceDataFile = "/Users/stefano/Documents/Lavoro/Prometheus/Data_Wnt_0-240_erk-frzld.csv";
//					SortedMap<Reactant, String> reactantToDataCorrespondence = new TreeMap<Reactant, String>();
//					for (Reactant r : model.getReactantCollection()) {
//						if (r.getName().equals("ERK")) {
//							reactantToDataCorrespondence.put(r, "ERK data");
//						}
//						if (r.getName().equals("Frzld")) {
//							reactantToDataCorrespondence.put(r, "Frizzled data");
//						}
//					}
//					Properties parameters = new Properties();
//					parameters.setProperty(LevenbergMarquardtFitter.MIN_COST_KEY, "0.5");
//					parameters.setProperty(LevenbergMarquardtFitter.DELTA_KEY, "0.0001");
//					LevenbergMarquardtFitter fitter = new LevenbergMarquardtFitter(model, reactionsToBeOptimized, referenceDataFile, reactantToDataCorrespondence, timeTo, parameters);
//					fitter.performParameterFitting();
//				}
//			});
//			buttons.add(bottoneDeiParametri, new GridBagConstraints(0, yPositionCounter++, 1, 1, 1, 0, GridBagConstraints.CENTER,
//					GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
//		}
		
		this.setVisible(true);
		this.repaint();
	}

	public void setChangePerSeconds(double d) {
		DecimalFormat df = new DecimalFormat("#.########");
		changeSecondsPerPointbutton.setText(df.format(d) + " seconds/step");
	}

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public CytoPanelName getCytoPanelName() {
		return CytoPanelName.WEST;
	}

	@Override
	public Icon getIcon() {
		return null;
	}

	@Override
	public String getTitle() {
		return Animo.APP_NAME;
	}

}
