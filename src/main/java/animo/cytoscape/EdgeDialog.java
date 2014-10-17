package animo.cytoscape;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;

import animo.core.AnimoBackend;
import animo.core.model.Model;
import animo.core.model.Scenario;
import animo.util.XmlConfiguration;

/**
 * The edge dialog contains the settings of a edge.
 * 
 * @author Brend Wanders
 * 
 */
public class EdgeDialog extends JDialog {
	private static final long serialVersionUID = 6630154220142970079L;
	private static final String DECIMAL_FORMAT_STRING = "##.####", SAVE = "Save", CANCEL = "Cancel",
			SCENARIO = Model.Properties.SCENARIO, CANONICAL_NAME = Model.Properties.CANONICAL_NAME,
			INCREMENT = Model.Properties.INCREMENT, UNCERTAINTY = Model.Properties.UNCERTAINTY;

	private Scenario[] scenarios = Scenario.SIX_SCENARIOS;
	private String[] reactantAliases;
	private String[] reactantIdentifiers; // The identifiers and aliases of all reactants in the network
	// private int previouslySelectedScenario = -1;

	private boolean wasNewlyCreated = false;

	private final CyNetwork network;

	public EdgeDialog(CyNetwork network, CyEdge edge) {
		this(Animo.getCytoscape().getJFrame(), network, edge);
	}

	/**
	 * Constructor.
	 * 
	 * @param edge
	 *            the edge to display for.
	 */
	@SuppressWarnings("unchecked")
	public EdgeDialog(final Window owner, final CyNetwork network, final CyEdge edge) {
		super(owner, "Reaction '" + edge.getSUID() + "'", Dialog.ModalityType.APPLICATION_MODAL);
		this.network = network;
		// super("Reaction " + Cytoscape.getNodeAttributes().getAttribute(edge.getSource().getSUID(), "canonicalName") +
		// ((Integer.parseInt(Cytoscape.getEdgeAttributes().getAttribute(edge.getSUID(), "increment").toString()) >= 0)?" --> ":" --| ") +
		// Cytoscape.getNodeAttributes().getAttribute(edge.getTarget().getSUID(), "canonicalName"));
		StringBuilder title = new StringBuilder();
		title.append("Reaction ");
		CyRow sourceNodeRow = network.getRow(edge.getSource());
		CyRow targetNodeRow = network.getRow(edge.getTarget());
		CyRow edgeRow = network.getRow(edge);

		// CyTable nodeAttrib = edge.getTarget().getNetworkPointer().getDefaultNodeTable();
		// final CyTable edgeAttrib = edge.getTarget().getNetworkPointer().getDefaultEdgeTable();
		final Integer increment;
		Integer tempInteger = edgeRow.get(INCREMENT, Integer.class);
		if (tempInteger == null) {
			increment = 1;
		} else {
			increment = tempInteger;
		}

		String res = sourceNodeRow.get(CANONICAL_NAME, String.class);
		if (res != null) {
			title.append(res);

			if (increment >= 0) {
				title.append(" --> ");
			} else {
				title.append(" --| ");
			}
			res = targetNodeRow.get(CANONICAL_NAME, String.class);
			if (res != null) {
				title.append(res);
				this.setTitle(title.toString());
			}
		}

		// Read the list of reactant identifiers and aliases from the nodes in the current network
		// CyNetwork network = edge.getSource().getNetworkPointer();

		reactantAliases = new String[network.getNodeCount()];
		reactantIdentifiers = new String[network.getNodeCount()];
		Iterator<CyNode> nodes = network.getNodeList().iterator();
		for (int i = 0; nodes.hasNext(); i++) {
			CyNode node = nodes.next();
			reactantIdentifiers[i] = node.getSUID().toString();

			String canonical = network.getRow(node).get(Model.Properties.CANONICAL_NAME, String.class);
			if (canonical != null) {
				reactantAliases[i] = canonical;
			} else {
				reactantAliases[i] = reactantIdentifiers[i];
			}
			/*
			 * if (edge.getSource().getSUID().equals(reactantIdentifiers[i])) { //Highlight the nodes involved in the current reaction reactantAliases[i] +=
			 * " (the Upstream reactant)"; } else if (edge.getTarget().getSUID().equals(reactantIdentifiers[i])) { reactantAliases[i] += " (the Downstream reactant)"; }
			 */
		}

		this.setLayout(new BorderLayout(2, 2));

		final JTextPane description = new JTextPane();
		JScrollPane descriptionScrollPane = new JScrollPane(description);
		descriptionScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		descriptionScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		description.setPreferredSize(new Dimension(400, 100));
		description.setMinimumSize(new Dimension(150, 50));

		String desc = edgeRow.get(Model.Properties.DESCRIPTION, String.class);
		if (desc != null) {
			description.setText(desc);
		}

		JPanel values = new JPanel(new GridLayout(1, 2, 2, 2));
		final Box boxScenario = new Box(BoxLayout.X_AXIS);
		JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		final Box boxScenarioParameters = new Box(BoxLayout.Y_AXIS);
		final JComboBox<Scenario> comboScenario = new JComboBox<Scenario>(scenarios);
		comboScenario.setMaximumSize(new Dimension(200, 20));
		comboScenario.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				updateParametersBox(edge, boxScenarioParameters, (Scenario) comboScenario.getSelectedItem(), increment);
				EdgeDialog.this.validate();
				EdgeDialog.this.pack();
			}
		});

		final int scenarioIdx;
		tempInteger = edgeRow.get(SCENARIO, Integer.class);
		if (tempInteger == null) {
			scenarioIdx = new Integer(0);
		} else {
			scenarioIdx = tempInteger;
		}

		comboScenario.setSelectedIndex(scenarioIdx);
		Box boxComboScenario = new Box(BoxLayout.Y_AXIS);
		boxComboScenario.add(comboScenario);
		boxComboScenario.add(Box.createGlue());

		Box boxScenarioAllParameters = new Box(BoxLayout.Y_AXIS);

		Integer value = edgeRow.get(UNCERTAINTY, Integer.class);
		if (value == null) {
			value = 0;
		}
		final JSlider uncertainty = new JSlider(0, 100, value);
		uncertainty.setPaintTicks(true);
		uncertainty.setMinorTickSpacing(5);
		uncertainty.setMajorTickSpacing(10);
		uncertainty.setPaintLabels(true);
		final LabelledField uncertaintyField = new LabelledField("Uncertainty = " + uncertainty.getValue() + "%",
				uncertainty);
		uncertainty.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				uncertaintyField.setTitle("Uncertainty = " + uncertainty.getValue() + "%");
				repaint();
			}
		});
		// boxScenarioAllParameters.add(uncertaintyField);
		final JRadioButton positiveIncrement = new JRadioButton("Activation"), negativeIncrement = new JRadioButton(
				"Inhibition");
		ButtonGroup incrementGroup = new ButtonGroup();
		incrementGroup.add(positiveIncrement);
		incrementGroup.add(negativeIncrement);
		if (increment >= 0) {
			positiveIncrement.setSelected(true);
			negativeIncrement.setSelected(false);
		} else {
			positiveIncrement.setSelected(false);
			negativeIncrement.setSelected(true);
		}
		ActionListener incrementListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int incr = 0;
				if (positiveIncrement.isSelected()) {
					incr = 1;
				} else if (negativeIncrement.isSelected()) {
					incr = -1;
				}
				if (incr != 0) {
					// save the parameters before refreshing the layout
					// TODO: HORRIBLE THING!!! This is copied from below, when we save!! HORRIBLE!
					Scenario selectedScenario = (Scenario) comboScenario.getSelectedItem();
					Component[] paramFields = boxScenarioParameters.getComponents();
					for (int i = 0; i < paramFields.length; i++) {
						if (paramFields[i] instanceof LabelledField) {
							LabelledField paramField = (LabelledField) paramFields[i];
							String paramName = paramField.getTitle();
							Double paramValue;
							if (paramField.getField() instanceof JFormattedTextField) {
								paramValue = new Double(((JFormattedTextField) (paramField).getField()).getValue()
										.toString());
							} else if (paramField.getField() instanceof Box) {
								paramValue = new Double(
										((JFormattedTextField) (((Box) paramField.getField()).getComponents()[0]))
												.getValue().toString());
							} else {
								paramValue = selectedScenario.getParameter(paramName);
							}
							selectedScenario.setParameter(paramName, paramValue);
						}
					}
					updateParametersBox(edge, boxScenarioParameters, (Scenario) comboScenario.getSelectedItem(), incr);
					EdgeDialog.this.validate();
					EdgeDialog.this.pack();
				}
			}
		};
		positiveIncrement.addActionListener(incrementListener);
		negativeIncrement.addActionListener(incrementListener);
		Box incrementBox = new Box(BoxLayout.X_AXIS);
		incrementBox.add(positiveIncrement);
		incrementBox.add(Box.createHorizontalStrut(50));
		incrementBox.add(negativeIncrement);
		boxScenarioAllParameters.add(new LabelledField("Influence", incrementBox));
		boxScenarioAllParameters.add(new LabelledField("Reaction kinetics", boxComboScenario));
		boxScenarioAllParameters.add(boxScenarioParameters);
		boxScenario.add(boxScenarioAllParameters);

		boxScenario.add(Box.createGlue());

		controls.add(new JButton(new AbstractAction(SAVE) {
			private static final long serialVersionUID = -6920908627164931058L;

			@Override
			public void actionPerformed(ActionEvent e) {
				Scenario selectedScenario = (Scenario) comboScenario.getSelectedItem();
				Component[] paramFields = boxScenarioParameters.getComponents();
				for (int i = 0; i < paramFields.length; i++) {
					if (paramFields[i] instanceof LabelledField) {
						LabelledField paramField = (LabelledField) paramFields[i];
						String paramName = paramField.getTitle();
						Double paramValue;
						if (paramField.getField() instanceof JFormattedTextField) {
							paramValue = new Double(((JFormattedTextField) (paramField).getField()).getValue()
									.toString());
						} else if (paramField.getField() instanceof Box) {
							paramValue = new Double(((JFormattedTextField) (((Box) paramField.getField())
									.getComponents()[0])).getValue().toString());
						} else {
							paramValue = selectedScenario.getParameter(paramName);
						}
						selectedScenario.setParameter(paramName, paramValue);
						Animo.setRowValue(network.getRow(edge), paramName, Double.class, paramValue);
						// edgeAttrib.getRow(edge).set(paramName, paramValue);
					} else if (paramFields[i] instanceof JLabel) { // Scenario 1/2 reactants

					} else if (paramFields[i] instanceof Box) { // Scenario 3 reactants
						Box e1Box = (Box) paramFields[i++], // Two consecutive boxes
						e2Box = (Box) paramFields[i];
						JComboBox<String> listE1act = (JComboBox<String>) (e1Box.getComponent(1)), listE1 = (JComboBox<String>) (e1Box
								.getComponent(2)), listE2act = (JComboBox<String>) (e2Box.getComponent(1)), listE2 = (JComboBox<String>) (e2Box
								.getComponent(2));

						// edgeAttrib.getRow(edge).set(Model.Properties.REACTANT_ID + "E1", reactantIdentifiers[listE1.getSelectedIndex()]);
						Animo.setRowValue(network.getRow(edge), Model.Properties.REACTANT_ID + "E1", String.class,
								reactantIdentifiers[listE1.getSelectedIndex()]);

						if (listE1act.getSelectedIndex() == 0) {
							// edgeAttrib.getRow(edge).set(Model.Properties.REACTANT_IS_ACTIVE_INPUT + "E1", true);
							Animo.setRowValue(network.getRow(edge), Model.Properties.REACTANT_IS_ACTIVE_INPUT + "E1",
									Boolean.class, true);
						} else {
							// edgeAttrib.getRow(edge).set(Model.Properties.REACTANT_IS_ACTIVE_INPUT + "E1", false);
							Animo.setRowValue(network.getRow(edge), Model.Properties.REACTANT_IS_ACTIVE_INPUT + "E1",
									Boolean.class, false);
						}
						// edgeAttrib.getRow(edge).set(Model.Properties.REACTANT_ID + "E2", reactantIdentifiers[listE1.getSelectedIndex()]);
						Animo.setRowValue(network.getRow(edge), Model.Properties.REACTANT_ID + "E2", String.class,
								reactantIdentifiers[listE1.getSelectedIndex()]);
						if (listE2act.getSelectedIndex() == 0) {
							// edgeAttrib.getRow(edge).set(Model.Properties.REACTANT_IS_ACTIVE_INPUT + "E2", true);
							Animo.setRowValue(network.getRow(edge), Model.Properties.REACTANT_IS_ACTIVE_INPUT + "E2",
									Boolean.class, true);
						} else {
							// edgeAttrib.getRow(edge).set(Model.Properties.REACTANT_IS_ACTIVE_INPUT + "E2", false);
							Animo.setRowValue(network.getRow(edge), Model.Properties.REACTANT_IS_ACTIVE_INPUT + "E2",
									Boolean.class, false);
						}
					}
				}

				int uncertVal = 5;
				XmlConfiguration configuration = AnimoBackend.get().configuration();
				String uncert = configuration.get(XmlConfiguration.UNCERTAINTY_KEY, null);
				if (uncert != null) {
					try {
						uncertVal = Integer.parseInt(uncert);
					} catch (NumberFormatException ex) {
						uncertVal = 5;
					}
				} else {
					uncertVal = 5;
				}

				// edgeAttrib.getRow(edge).set(UNCERTAINTY, uncertVal);
				Animo.setRowValue(network.getRow(edge), UNCERTAINTY, Integer.class, uncertVal);
				// edgeAttrib.getRow(edge).set(SCENARIO, comboScenario.getSelectedIndex());
				Animo.setRowValue(network.getRow(edge), SCENARIO, Integer.class, comboScenario.getSelectedIndex());
				// edgeAttrib.getRow(edge).set(INCREMENT, ((positiveIncrement.isSelected()) ? 1 : -1));
				Animo.setRowValue(network.getRow(edge), INCREMENT, Integer.class, ((positiveIncrement.isSelected()) ? 1
						: -1));
				// edgeAttrib.getRow(edge).set(Model.Properties.DESCRIPTION, description.getText());
				Animo.setRowValue(network.getRow(edge), Model.Properties.DESCRIPTION, String.class,
						description.getText());
				EdgeDialog.this.dispose();
			}
		}));

		Box boxScenarioDescription = new Box(BoxLayout.Y_AXIS);
		boxScenarioDescription.add(boxScenario);
		boxScenarioDescription.add(new LabelledField("Description", descriptionScrollPane));
		values.add(boxScenarioDescription);

		JButton cancelButton = new JButton(new AbstractAction(CANCEL) {
			private static final long serialVersionUID = 3103827646050457714L;

			@Override
			public void actionPerformed(ActionEvent e) {
				// discard changes
				if (wasNewlyCreated) {
					network.removeEdges(Collections.singletonList(edge));
				}
				EdgeDialog.this.dispose();
			}
		});
		controls.add(cancelButton);

		// Associate the "Cancel" button with the Esc key
		getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "CANCEL");
		getRootPane().getActionMap().put("CANCEL", cancelButton.getAction());

		this.add(values, BorderLayout.CENTER);
		this.add(controls, BorderLayout.SOUTH);
		
		this.pack();
	}

	public void setCreatedNewEdge() {
		wasNewlyCreated = true;
	}

	/**
	 * Update the list of parameters shown when a different scenario is chosen
	 * 
	 * @param edge
	 *            The edge representing the reaction of which we display the parameter
	 * @param parametersBox
	 *            The box in which to put the list of parameters for the new scenario
	 * @param selectedScenario
	 *            The newly selected scenario
	 * @param increment
	 *            the increment of the reaction (usually +1 or -1)
	 */
	@SuppressWarnings("unchecked")
	private void updateParametersBox(CyEdge edge, Box parametersBox, Scenario selectedScenario, int increment) {
		parametersBox.removeAll();
		// TODO: refactor
		final CyNetwork n = network;

		// Look for the index of the currently selected scenario, so that we can compare it with the previously selected one, and thus correctly convert the parameters between the
		// two
		int currentlySelectedScenario = 0;
		for (int i = 0; i < scenarios.length; i++) {
			if (scenarios[i].equals(selectedScenario)) {
				currentlySelectedScenario = i;
				break;
			}
		}

		String[] parameters = selectedScenario.listVariableParameters();
		// CyTable edgeAttrib = network.getDefaultEdgeTable();
		// CyTable nodeAttrib = network.getDefaultNodeTable();

		final View<CyNode>[] nodeViews;
		final CyNetworkView networkView = Animo.getCytoscapeApp().getCyApplicationManager().getCurrentNetworkView();
		nodeViews = new View[n.getNodeCount()];
		Iterator<CyNode> nodes = n.getNodeList().iterator();
		for (int i = 0; nodes.hasNext(); i++) {
			CyNode node = nodes.next();
			nodeViews[i] = networkView.getNodeView(node);
		}
		if (currentlySelectedScenario == 0) {
			String upstreamReactantName = n.getRow(edge).get(Model.Properties.CANONICAL_NAME, String.class);
			JLabel label = new JLabel("E = active " + upstreamReactantName);
			parametersBox.add(label);
		} else if (currentlySelectedScenario == 1) {
			String upstreamReactantName = n.getRow(edge.getSource()).get(Model.Properties.CANONICAL_NAME, String.class);
			String downstreamReactantName = n.getRow(edge.getTarget()).get(Model.Properties.CANONICAL_NAME,
					String.class);
			JLabel label1 = new JLabel("E = active " + upstreamReactantName), label2 = new JLabel("S = "
					+ ((increment >= 0) ? "inactive " : "active ") + downstreamReactantName);
			parametersBox.add(label1);
			parametersBox.add(label2);
		} else if (currentlySelectedScenario == 2) {
			String[] activeInactive = new String[] { "active", "inactive" };
			final JComboBox<String> listE1 = new JComboBox<String>(reactantAliases), listE2 = new JComboBox<String>(
					reactantAliases), listE1act = new JComboBox<String>(activeInactive), listE2act = new JComboBox<String>(
					activeInactive);
			int selectedReactant = -1;
			String id = n.getRow(edge).get(Model.Properties.REACTANT_ID + "E1", String.class);
			if (id != null) {
				Boolean active = n.getRow(edge).get(Model.Properties.REACTANT_IS_ACTIVE_INPUT + "E1", Boolean.class);
				for (int i = 0; i < reactantIdentifiers.length; i++) {
					if (reactantIdentifiers[i].equals(id)) {
						selectedReactant = i;
						break;
					}
				}
				if (selectedReactant != -1) {
					listE1.setSelectedIndex(selectedReactant);
					if (active) {
						listE1act.setSelectedIndex(0);
					} else {
						listE1act.setSelectedIndex(1);
					}
				}
			}
			if (selectedReactant == -1) {
				for (int i = 0; i < reactantIdentifiers.length; i++) {
					if (reactantIdentifiers[i].equals(edge.getSource().getSUID())) {
						selectedReactant = i;
						break;
					}
				}
				if (selectedReactant != -1) {
					listE1.setSelectedIndex(selectedReactant);
					listE1act.setSelectedIndex(0);
				}
			}

			selectedReactant = -1;
			id = n.getRow(edge).get(Model.Properties.REACTANT_ID + "E2", String.class);
			if (id != null) {
				Boolean active = n.getRow(edge).get(Model.Properties.REACTANT_IS_ACTIVE_INPUT + "E2", Boolean.class);
				for (int i = 0; i < reactantIdentifiers.length; i++) {
					if (reactantIdentifiers[i].equals(id)) {
						selectedReactant = i;
						break;
					}
				}
				if (selectedReactant != -1) {
					listE2.setSelectedIndex(selectedReactant);
					if (active) {
						listE2act.setSelectedIndex(0);
					} else {
						listE2act.setSelectedIndex(1);
					}
				}
			}
			if (selectedReactant == -1) {
				for (int i = 0; i < reactantIdentifiers.length; i++) {
					if (reactantIdentifiers[i].equals(edge.getTarget().getSUID())) {
						selectedReactant = i;
						break;
					}
				}
				if (selectedReactant != -1) {
					listE2.setSelectedIndex(selectedReactant);
					listE2act.setSelectedIndex(0);
				}
			}
			listE1.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) { // Show the selected node in the network, so that the user can be sure of which it is.
					for (View<CyNode> v : nodeViews) {
						// CyTable networkAttr = network.getDefaultNetworkTable();
						if (v.getModel().getSUID().equals(reactantIdentifiers[listE1.getSelectedIndex()])) {
							// network.getDefaultNodeTable().getRow(v.getModel()).set(CyNetwork.SELECTED, true);
							Animo.setRowValue(network.getRow(v.getModel()), CyNetwork.SELECTED, Boolean.class, true);
						} else {
							// network.getDefaultNodeTable().getRow(v.getModel()).set(CyNetwork.SELECTED, false);
							Animo.setRowValue(network.getRow(v.getModel()), CyNetwork.SELECTED, Boolean.class, false);
						}
					}
					networkView.updateView();
				}
			});
			listE2.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) { // Show the selected node in the network, so that the user can be sure of which it is.
					for (View<CyNode> v : nodeViews) {
						/*
						 * if (v.getModel().getSUID().equals(reactantIdentifiers[listE2.getSelectedIndex()])) {
						 * network.getDefaultNodeTable().getRow(v.getModel()).set(CyNetwork.SELECTED, true); } else {
						 * network.getDefaultNodeTable().getRow(v.getModel()).set(CyNetwork.SELECTED, false); }
						 */
						boolean selected = v.getModel().getSUID()
								.equals(reactantIdentifiers[listE2.getSelectedIndex()]);
						Animo.setRowValue(n.getRow(v.getModel()), CyNetwork.SELECTED, Boolean.class, selected);
					}
					networkView.updateView();
				}
			});
			Box e1Box = new Box(BoxLayout.X_AXIS), e2Box = new Box(BoxLayout.X_AXIS);
			e1Box.add(new JLabel("E1 = "));
			e1Box.add(listE1act);
			e1Box.add(listE1);
			parametersBox.add(e1Box);
			e2Box.add(new JLabel("E2 = "));
			e2Box.add(listE2act);
			e2Box.add(listE2);
			parametersBox.add(e2Box);
		}

		for (int i = 0; i < parameters.length; i++) {
			DecimalFormat format = new DecimalFormat(DECIMAL_FORMAT_STRING);
			format.setMinimumFractionDigits(8);
			final JFormattedTextField param = new JFormattedTextField(format);
			// if (previouslySelectedScenario == -1 || previouslySelectedScenario == currentlySelectedScenario) {

			Double value = n.getRow(edge).get(parameters[i], Double.class);
			if (value != null) {
				param.setValue(value);
				scenarios[currentlySelectedScenario].setParameter(parameters[i], value);
			} else {
				Double val = selectedScenario.getParameter(parameters[i]);
				if (val == null) {
					val = 0d;
				}
				param.setValue(val);
			}
			Dimension prefSize = param.getPreferredSize();
			prefSize.width *= 1.5;
			param.setPreferredSize(prefSize);
			if (parameters.length == 1) // If we have only one parameter, we show a slider for the parameter
			{
				final JSlider parSlider = new JSlider(1, 5, 3);
				Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
				labelTable.put(Integer.valueOf(1), new JLabel("v. slow"));
				labelTable.put(Integer.valueOf(2), new JLabel("slow"));
				labelTable.put(Integer.valueOf(3), new JLabel("medium"));
				labelTable.put(Integer.valueOf(4), new JLabel("fast"));
				labelTable.put(Integer.valueOf(5), new JLabel("v. fast"));
				parSlider.setLabelTable(labelTable);
				parSlider.setPaintLabels(true);
				parSlider.setMajorTickSpacing(1);
				parSlider.setPaintTicks(true);
				parSlider.setSnapToTicks(true);
				prefSize = parSlider.getPreferredSize();
				prefSize.width *= 1.5;
				parSlider.setPreferredSize(prefSize);
				Box parSliBox = new Box(BoxLayout.Y_AXIS);
				param.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent e) {
						param.setEnabled(true);
						parSlider.setEnabled(false);
					}
				});
				parSlider.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent e) {
						parSlider.setEnabled(true);
						param.setEnabled(false);
						double multiplicator = (Double) (param.getValue()) / 0.004;
						double exp = Math.log(multiplicator) / Math.log(2);
						int sliderValue = (int) (exp + 3);
						if (sliderValue < 1) {
							parSlider.setValue(1);
						} else if (sliderValue > 5) {
							parSlider.setValue(5);
						} else {
							parSlider.setValue(sliderValue);
						}
					}
				});
				parSlider.addChangeListener(new ChangeListener() {
					@Override
					public void stateChanged(ChangeEvent e) {
						int exp = parSlider.getValue() - 3;
						double multiplicator = Math.pow(2.0, exp);
						param.setValue(0.004 * multiplicator);
					}
				});
				parSliBox.add(param);
				parSliBox.add(parSlider);
				param.setEnabled(true);
				double savedPreciseValue = (Double) (param.getValue());
				double multiplicator = (Double) (param.getValue()) / 0.004;
				double exp = Math.log(multiplicator) / Math.log(2);
				int sliderValue = (int) (exp + 3);
				if (sliderValue >= 1 && sliderValue <= 5) {
					parSlider.setValue(sliderValue);
				}
				parSlider.setEnabled(false);
				param.setValue(savedPreciseValue);
				parametersBox.add(new LabelledField(parameters[i], parSliBox));
			} else {
				parametersBox.add(new LabelledField(parameters[i], param));
			}
		}

		parametersBox.validate();

		// Update the index of the currently selected scenario, so that if the user changes scenario we will be able to know which we were using before, and thus correctly convert
		// the parameters between the two
		// previouslySelectedScenario = -1;
		// for (int i=0;i<scenarios.length;i++) {
		// if (scenarios[i].equals(selectedScenario)) {
		// previouslySelectedScenario = i;
		// break;
		// }
		// }
	}
	
	public void showYourself() {
		this.setLocationRelativeTo(Animo.getCytoscape().getJFrame());
		this.setVisible(true);
	}
}
