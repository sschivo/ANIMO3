package animo.cytoscape;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;

import animo.core.model.Model;

/**
 * The node dialog contains the settings of a node.
 * 
 * @author Brend Wanders
 * 
 */
public class NodeDialog extends JDialog {

	private static final long serialVersionUID = 1498730989498413815L;
	private boolean wasNewlyCreated = false;

	public NodeDialog(CyNetwork network, CyNode node) {
		this(Animo.getCytoscape().getJFrame(), network, node);
	}
	
	/**
	 * Constructor.
	 * 
	 * @param node
	 *            the node to display for.
	 */
	@SuppressWarnings("unchecked")
	public NodeDialog(final Window owner, final CyNetwork network, final CyNode node) {

		super(owner, "Reactant '" + node.getSUID() + "'", Dialog.ModalityType.APPLICATION_MODAL);

		CyRow networkAttributesRow = network.getRow(network);
		// CyTable nodeAttributes = network.getDefaultNodeTable();
		this.setTitle("Edit reactant");
		CyRow nodeAttributesRow = network.getRow(node);

		Object res = nodeAttributesRow.get(Model.Properties.CANONICAL_NAME, Object.class);
		String name;
		if (res != null) {
			// this.setTitle("Reactant " + res.toString());
			name = res.toString();
		} else {
			name = null;
		}
		Integer integer = nodeAttributesRow.get(Model.Properties.INITIAL_LEVEL, Integer.class);
		if (integer == null) {
			Animo.setRowValue(nodeAttributesRow, Model.Properties.INITIAL_LEVEL, Integer.class, 0);

			integer = 0;
		}

		this.setLayout(new GridBagLayout()); // BorderLayout(2, 2));

		// JPanel values = new JPanel(new GridLayout(3, 2, 2, 2));
		// JPanel values = new JPanel(new GridBagLayout()); //You REALLY don't want to know how GridBagLayout works...
		// Box values = new Box(BoxLayout.Y_AXIS);

		Integer levels = nodeAttributesRow.get(Model.Properties.NUMBER_OF_LEVELS, Integer.class);
		if (levels == null) {
			levels = networkAttributesRow.get(Model.Properties.NUMBER_OF_LEVELS, Integer.class);
			if (levels == null) {
				levels = Integer.valueOf(15);
			}
		}

		// JLabel nameLabel = new JLabel("Reactant name:");
		final JTextField nameField = new JTextField(name);
		// values.add(nameLabel, new GridBagConstraints(0, 0, 1, 1, 0.3, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		// values.add(nameField, new GridBagConstraints(1, 0, 1, 1, 1, 0.0, GridBagConstraints.LINE_END, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		// values.add(new LabelledField("Name", nameField));
		this.add(new LabelledField("Name", nameField), new GridBagConstraints(0, 0, 1, 1, 0.5, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));

		// final JLabel totalLevelsLabel = new JLabel("Total activity levels: " + levels);
		// values.add(totalLevelsLabel, new GridBagConstraints(0, 1, 1, 1, 0.3, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		final JSlider totalLevels = new JSlider(1, 100);
		totalLevels.setValue(levels);
		totalLevels.setMajorTickSpacing(20);
		totalLevels.setMinorTickSpacing(10);

		totalLevels.setPaintLabels(true);
		totalLevels.setPaintTicks(true);
		if (totalLevels.getMaximum() == 100) {
			Dictionary<Integer, JLabel> labelTable = totalLevels.getLabelTable();
			labelTable.put(totalLevels.getMaximum(), new JLabel("" + totalLevels.getMaximum()));
			totalLevels.setLabelTable(labelTable);
		}
		// //values.add(totalLevels, new GridBagConstraints(1, 1, 1, 1, 1, 0.0, GridBagConstraints.LINE_END, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		final LabelledField totalLevelsField = new LabelledField("Total activity levels: " + levels, totalLevels);
		// values.add(totalLevelsField);

		final JSlider initialConcentration = new JSlider(0, levels);
		initialConcentration.setValue(nodeAttributesRow.get(Model.Properties.INITIAL_LEVEL, Integer.class));

		// //final JLabel initialConcentrationLabel = new JLabel("Initial activity level: " + initialConcentration.getValue());
		// //values.add(initialConcentrationLabel, new GridBagConstraints(0, 2, 1, 1, 0.3, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0,
		// 0));
		final LabelledField initialLevelField = new LabelledField("Initial activity level: "
				+ initialConcentration.getValue(), initialConcentration);

		initialConcentration.setMajorTickSpacing(levels / 5);
		initialConcentration.setMinorTickSpacing(levels / 10);

		initialConcentration.setPaintLabels(true);
		initialConcentration.setPaintTicks(true);

		// //values.add(initialConcentration, new GridBagConstraints(1, 2, 1, 1, 1, 0.0, GridBagConstraints.LINE_END, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		// values.add(initialLevelField);

		// this.add(values, BorderLayout.CENTER);

		// When the user changes the total number of levels, we automatically update the "current activity level" slider, adapting maximum and current values in a sensible way
		totalLevels.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				totalLevelsField.setTitle("Total activity levels: " + totalLevels.getValue());
				if (totalLevels.getValueIsAdjusting())
					return;
				double prevMax = initialConcentration.getMaximum(), currMax = totalLevels.getValue();
				int currValue = (int) ((initialConcentration.getValue()) / prevMax * currMax);
				initialConcentration.setMaximum(totalLevels.getValue());
				initialConcentration.setValue(currValue);
				int space = (initialConcentration.getMaximum() - initialConcentration.getMinimum() + 1) / 5;
				if (space < 1)
					space = 1;
				initialConcentration.setMajorTickSpacing(space);
				initialConcentration.setMinorTickSpacing(space / 2);
				Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
				for (int i = initialConcentration.getMinimum(); i <= initialConcentration.getMaximum(); i += space) {
					labelTable.put(i, new JLabel("" + i));
				}
				initialConcentration.setLabelTable(labelTable);
				initialLevelField.setTitle("Initial activity level: " + initialConcentration.getValue());
				initialConcentration.setValue(currValue);
			}

		});

		initialConcentration.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				initialLevelField.setTitle("Initial activity level: " + initialConcentration.getValue());
			}

		});

		// Box optionBoxes = new Box(BoxLayout.Y_AXIS);
		String[] moleculeTypes = new String[] { Model.Properties.TYPE_CYTOKINE, Model.Properties.TYPE_RECEPTOR,
				Model.Properties.TYPE_KINASE, Model.Properties.TYPE_PHOSPHATASE,
				Model.Properties.TYPE_TRANSCRIPTION_FACTOR, Model.Properties.TYPE_GENE, Model.Properties.TYPE_MRNA,
				Model.Properties.TYPE_DUMMY, Model.Properties.TYPE_OTHER };
		final JComboBox<String> moleculeType = new JComboBox<String>(moleculeTypes);
		String type = nodeAttributesRow.get(Model.Properties.MOLECULE_TYPE, String.class);
		if (type != null) {
			boolean notContained = true;
			for (String s : moleculeTypes) {
				if (s.equals(type)) {
					notContained = false;
				}
			}
			if (notContained) {
				moleculeType.setSelectedItem(Model.Properties.TYPE_OTHER);
			} else {
				moleculeType.setSelectedItem(type);
			}
		} else {
			moleculeType.setSelectedItem(Model.Properties.TYPE_KINASE);
		}
		// optionBoxes.add(new LabelledField("Molecule type", moleculeType));
		// values.add(new LabelledField("Molecule type", moleculeType));
		this.add(new LabelledField("Molecule type", moleculeType), new GridBagConstraints(0, 1, 1, 1, 0.5, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		// optionBoxes.add(totalLevelsField);
		// optionBoxes.add(initialLevelField);
		this.add(totalLevelsField, new GridBagConstraints(1, 0, 1, 1, 0.5, 0.0, GridBagConstraints.CENTER,
				GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		this.add(initialLevelField, new GridBagConstraints(1, 1, 1, 1, 0.5, 0.0, GridBagConstraints.CENTER,
				GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

		final JRadioButton enabledNode = new JRadioButton("Enabled"), disabledNode = new JRadioButton("Disabled"), plottedNode = new JRadioButton(
				"Plotted"), hiddenNode = new JRadioButton("Hidden");
		ButtonGroup enabledGroup = new ButtonGroup(), plottedGroup = new ButtonGroup();
		enabledGroup.add(enabledNode);
		enabledGroup.add(disabledNode);
		plottedGroup.add(plottedNode);
		plottedGroup.add(hiddenNode);

		Boolean enabled = nodeAttributesRow.get(Model.Properties.ENABLED, Boolean.class);
		if (enabled != null) {
			enabledNode.setSelected(enabled);
		} else {
			enabledNode.setSelected(true);
		}
		disabledNode.setSelected(!enabledNode.isSelected());
		Boolean plotted = nodeAttributesRow.get(Model.Properties.PLOTTED, Boolean.class);
		if (plotted != null) {
			plottedNode.setSelected(plotted);
		} else {
			plottedNode.setSelected(true);
		}
		hiddenNode.setSelected(!plottedNode.isSelected());
		Box enabledBox = new Box(BoxLayout.X_AXIS);
		enabledBox.add(enabledNode);
		enabledBox.add(Box.createGlue());
		enabledBox.add(disabledNode);
		// optionBoxes.add(enabledBox);
		this.add(enabledBox, new GridBagConstraints(1, 2, 1, 1, 0.5, 0.0, GridBagConstraints.LINE_END,
				GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		Box plottedBox = new Box(BoxLayout.X_AXIS);
		plottedBox.add(plottedNode);
		plottedBox.add(Box.createGlue());
		plottedBox.add(hiddenNode);
		// optionBoxes.add(plottedBox);
		// optionBoxes.add(Box.createVerticalStrut(150));
		this.add(plottedBox, new GridBagConstraints(1, 3, 1, 1, 0.5, 0.0, GridBagConstraints.LINE_END,
				GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		// this.add(optionBoxes, BorderLayout.EAST);

		final JTextPane description = new JTextPane();
		JScrollPane descriptionScrollPane = new JScrollPane(description);
		descriptionScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		descriptionScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		description.setPreferredSize(new Dimension(400, 100));
		description.setMinimumSize(new Dimension(150, 50));

		String desc = nodeAttributesRow.get(Model.Properties.DESCRIPTION, String.class);
		if (desc != null) {
			description.setText(desc);
		}
		this.add(new LabelledField("Description", descriptionScrollPane), new GridBagConstraints(0, 4, 2, 1, 1.0, 1.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

		JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		controls.add(new JButton(new AbstractAction("Save") {
			private static final long serialVersionUID = -6179643943409321939L;

			@Override
			public void actionPerformed(ActionEvent e) {
				// CyTable nodeAttributes = network.getDefaultNodeTable();
				CyRow nodeAttributesRow = network.getRow(node);
				// nodeAttributesRow.set(Model.Properties.INITIAL_LEVEL, initialConcentration.getValue());
				Animo.setRowValue(nodeAttributesRow, Model.Properties.INITIAL_LEVEL, Integer.class,
						initialConcentration.getValue());
				// nodeAttributesRow.set(Model.Properties.NUMBER_OF_LEVELS, totalLevels.getValue());
				Animo.setRowValue(nodeAttributesRow, Model.Properties.NUMBER_OF_LEVELS, Integer.class,
						totalLevels.getValue());

				double activityRatio = (double) initialConcentration.getValue() / totalLevels.getValue();

				// nodeAttributesRow.set(Model.Properties.SHOWN_LEVEL, activityRatio);
				Animo.setRowValue(nodeAttributesRow, Model.Properties.SHOWN_LEVEL, Double.class, activityRatio);

				if (nameField.getText() != null && nameField.getText().length() > 0) {
					// nodeAttributesRow.set(Model.Properties.CANONICAL_NAME, nameField.getText());
					Animo.setRowValue(nodeAttributesRow, Model.Properties.CANONICAL_NAME, String.class,
							nameField.getText());

				}
				// nodeAttributesRow.set(Model.Properties.MOLECULE_TYPE, moleculeType.getSelectedItem().toString());
				Animo.setRowValue(nodeAttributesRow, Model.Properties.MOLECULE_TYPE, String.class, moleculeType
						.getSelectedItem().toString());
				// nodeAttributesRow.set(Model.Properties.ENABLED, enabledNode.isSelected());
				Animo.setRowValue(nodeAttributesRow, Model.Properties.ENABLED, Boolean.class, enabledNode.isSelected());

				// nodeAttributesRow.set(Model.Properties.PLOTTED, plottedNode.isSelected());
				Animo.setRowValue(nodeAttributesRow, Model.Properties.PLOTTED, Boolean.class, plottedNode.isSelected());

				// nodeAttributesRow.set(Model.Properties.DESCRIPTION, description.getText());
				Animo.setRowValue(nodeAttributesRow, Model.Properties.DESCRIPTION, String.class, description.getText());

				NodeDialog.this.dispose();
				
				tryNetworkViewUpdate();
			}
		}));

		JButton cancelButton = new JButton(new AbstractAction("Cancel") {
			private static final long serialVersionUID = -2038333013177775241L;

			@Override
			public void actionPerformed(ActionEvent e) {
				// discard changes
				if (wasNewlyCreated) {
					network.removeNodes(Collections.singletonList(node));
				}
				NodeDialog.this.dispose();
			}
		});
		controls.add(cancelButton);

		// Associate the "Cancel" button with the Esc key
		getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "CANCEL");
		getRootPane().getActionMap().put("CANCEL", cancelButton.getAction());

		this.add(controls, new GridBagConstraints(1, 5, 1, 2, 1.0, 0.0, GridBagConstraints.LINE_END,
				GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0)); // BorderLayout.SOUTH);
		
		this.pack();
	}
	
	public void showYourself() {
		this.setLocationRelativeTo(Animo.getCytoscape().getJFrame());
		this.setVisible(true);
	}

	public void setCreatedNewNode() {
		wasNewlyCreated = true;
	}
	
	

	private static Thread networkViewUpdater = null;
	
	public static void tryNetworkViewUpdate() {
		if (networkViewUpdater == null) {
			networkViewUpdater = new Thread() {
				final VisualMappingManager vmm = Animo.getCytoscapeApp().getVisualMappingManager();
				final CyNetworkView networkView = Animo.getCytoscapeApp().getCyApplicationManager().getCurrentNetworkView();
				final VisualStyle visualStyle = vmm.getCurrentVisualStyle();
				
				public void run() {
					//System.err.println("Inizia il threado");
					while (true) { //Try to do the update each time you are notified to do it. Before doing the update, wait X seconds: if you are interrupted before that time, do nothing and go back waiting for the next request
						boolean doUpdate = true;
						try {
							Thread.sleep(200);
						} catch (InterruptedException ex) {
							//System.err.println("Hai premuto il bottone troppo presto: non ho fatto in tempo ad aggiornare");
							doUpdate = false;
						}
						if (doUpdate) {
							//System.err.println("Ho atteso abbastanza: aggiorno");
							visualStyle.apply(networkView); //TODO: problem, this can raise a concurrent modification exception because we may be updating the view concurrently with cytoscape (who I truly wish would mind its own business..)
							networkView.updateView();
							//This one below is even less useful: it makes a dialog appear to ask for which visual style you want to apply...
//							TaskIterator ti = Animo.getCyServiceRegistrar().getService(ApplyVisualStyleTaskFactory.class).createTaskIterator(Arrays.asList(networkView));
//							Animo.getCytoscapeApp().getTaskManager().execute(ti);
						}
						synchronized (this) {
							try {
								//System.err.println("Mi metto in attesa di poter fare un aggiornamento");
								this.wait();
							} catch (InterruptedException ex) {
								//System.err.println("Mi dicono che devo provare a fare l'aggiornamento");
							}
						}
					}
				}
			};
			networkViewUpdater.start();
		}
		synchronized (networkViewUpdater) {
			networkViewUpdater.notify();
		}
	}
	
	public static void dontUpdateNetworkView() {
		if (networkViewUpdater != null) {
			if (networkViewUpdater.getState().equals(Thread.State.TIMED_WAITING)) { //We break the sleep only: if it is already doing the "wait", we let it stay there because we don't want to try a new update
				networkViewUpdater.interrupt();
			}
		}
	}
}
