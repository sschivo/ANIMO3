package animo.cytoscape;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

import animo.core.graph.Graph;
import animo.core.graph.Series;

public class ChooseVSGraphDialog extends JDialog {
	private static final long serialVersionUID = 1997420406956304989L;
	private Graph graph = null;
	private List<Series> seriesData = null;
	private Map<ButtonModel, Series> mapSeriesComponent = null;
	
	public ChooseVSGraphDialog(Graph graph, List<Series> seriesData) {
		this(Animo.getCytoscape().getJFrame(), graph, seriesData);
	}
	
	public ChooseVSGraphDialog(Window owner, Graph graph, List<Series> seriesData) {
		super(owner, "Choose two series to put on the VS graph", Dialog.ModalityType.APPLICATION_MODAL);
		this.graph = graph;
		this.seriesData = seriesData;
		this.mapSeriesComponent = new HashMap<ButtonModel, Series>();
		prepareWindow();
		this.pack();
	}

	private void prepareWindow() {
		Box xCandidates = new Box(BoxLayout.Y_AXIS),
			yCandidates = new Box(BoxLayout.Y_AXIS);
		final ButtonGroup xGroup = new ButtonGroup(),
						  yGroup = new ButtonGroup();
		fillBox(xCandidates, xGroup);
		fillBox(yCandidates, yGroup);
		Box allCandidates = new Box(BoxLayout.X_AXIS);
		allCandidates.add(new LabelledField("X", new JScrollPane(xCandidates, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER)));
		allCandidates.add(Box.createHorizontalStrut(5));
		allCandidates.add(new LabelledField("Y", new JScrollPane(yCandidates, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER)));
		this.getContentPane().add(allCandidates, BorderLayout.CENTER);
		JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton accept = new JButton(new AbstractAction("OK") {
			private static final long serialVersionUID = 8631051016372664335L;

			@Override
			public void actionPerformed(ActionEvent ev) {
				ButtonModel xCandidate = xGroup.getSelection(),
							yCandidate = yGroup.getSelection();
				if (xCandidate != null && yCandidate != null) {
					Series xSeries = mapSeriesComponent.get(xCandidate),
						   ySeries = mapSeriesComponent.get(yCandidate);
					graph.setXvsY(xSeries, ySeries);
				}
				ChooseVSGraphDialog.this.dispose();
			}
		});
		JButton cancel = new JButton(new AbstractAction("Cancel") {
			private static final long serialVersionUID = 5185840022638748933L;

			@Override
			public void actionPerformed(ActionEvent ev) {
				ChooseVSGraphDialog.this.dispose();
			}
		});
		this.getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "CANCEL");
		this.getRootPane().getActionMap().put("CANCEL", cancel.getAction());
		controls.add(accept);
		controls.add(cancel);
		this.getContentPane().add(controls, BorderLayout.SOUTH);
	}
	
	private void fillBox(Box candidates, ButtonGroup group) {
		for (Series series : seriesData) {
			if (series.isSlave()) continue;
			JRadioButton candidate = new JRadioButton(series.getName(), false);
			mapSeriesComponent.put(candidate.getModel(), series);
			group.add(candidate);
			candidates.add(candidate);
		}
	}
	
	public void showYourself() {
		this.setLocationRelativeTo(graph); //Animo.getCytoscape().getJFrame());
		this.setVisible(true);
	}
}
