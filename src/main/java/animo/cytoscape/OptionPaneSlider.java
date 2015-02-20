package animo.cytoscape;

import java.awt.Component;
import java.util.Dictionary;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class OptionPaneSlider extends JOptionPane {
	private static final long serialVersionUID = -2948188980884500970L;
	
	@SuppressWarnings("unchecked")
	private static JSlider getSlider(int init, int min, int max, final JLabel currentValueLabel, final String valueLabel) {
		final JSlider slider = new JSlider(min, max);
		slider.setMajorTickSpacing(20);
		slider.setMinorTickSpacing(10);
		slider.setValue(init);
		slider.setPaintTicks(true);
		slider.setPaintLabels(true);
		if (slider.getMaximum() == 100) {
			Dictionary<Integer, JLabel> labelTable = slider.getLabelTable();
			labelTable.put(slider.getMaximum(), new JLabel("" + slider.getMaximum()));
			slider.setLabelTable(labelTable);
		}
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent changeEvent) {
				currentValueLabel.setText(valueLabel + ": " + slider.getValue());
			}
		});
		return slider;
	}

	
	public static Integer showInputSliderDialog(Component parent, Object message, int initialValue, int min, int max, String valueLabel){
		JLabel nLevelsLabel = new JLabel(valueLabel + ": " + initialValue);
		JSlider slider = getSlider(initialValue, min, max, nLevelsLabel, valueLabel);
		int answer = JOptionPane.showConfirmDialog(parent, new Object[] { message, slider, nLevelsLabel }, "Input", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
		if (answer == JOptionPane.CANCEL_OPTION) {
			return null;
		} else {
			return slider.getValue();
		}
	}
}
