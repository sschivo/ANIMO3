package animo.cytoscape;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;

/**
 * Based on code from
 * Definitive Guide to Swing for Java 2, Second Edition
 * Original author John Zukowski
 */
public class DualListBox<Type> extends JPanel {
	private static final long serialVersionUID = -1371691055025926130L;
	private static final Insets EMPTY_INSETS = new Insets(0, 0, 0, 0);
	private static final String ADD_BUTTON_LABEL = "Add >>";
	private static final String REMOVE_BUTTON_LABEL = "<< Remove";
	private static final String DEFAULT_SOURCE_CHOICE_LABEL = "Available Choices";
	private static final String DEFAULT_DEST_CHOICE_LABEL = "Your Choices";
	private JLabel sourceLabel;
	private JList<Type> sourceList;
	private SortedListModel<Type> sourceListModel;
	private JList<Type> destList;
	private SortedListModel<Type> destListModel;
	private JLabel destLabel;
	private JButton addButton;
	private JButton removeButton;

	public DualListBox() {
		initScreen();
	}

	public String getSourceChoicesTitle() {
		return sourceLabel.getText();
	}

	public void setSourceChoicesTitle(String newValue) {
		sourceLabel.setText(newValue);
	}

	public String getDestinationChoicesTitle() {
		return destLabel.getText();
	}

	public void setDestinationChoicesTitle(String newValue) {
		destLabel.setText(newValue);
	}

	public void clearSourceListModel() {
		sourceListModel.clear();
	}

	public void clearDestinationListModel() {
		destListModel.clear();
	}

	public void addSourceElements(ListModel<Type> newValue) {
		fillListModel(sourceListModel, newValue);
	}

	public void setSourceElements(ListModel<Type> newValue) {
		clearSourceListModel();
		addSourceElements(newValue);
	}

	public void addDestinationElements(ListModel<Type> newValue) {
		fillListModel(destListModel, newValue);
	}

	private void fillListModel(SortedListModel<Type> model, ListModel<Type> newValues) {
		int size = newValues.getSize();
		for (int i = 0; i < size; i++) {
			model.add(newValues.getElementAt(i));
		}
	}

	public void addSourceElements(List<Type> newValues) {
		fillListModel(sourceListModel, newValues);
	}
	
	public void addSourceElements(Type newValue[]) {
		fillListModel(sourceListModel, newValue);
	}

	public void setSourceElements(Type newValue[]) {
		clearSourceListModel();
		addSourceElements(newValue);
	}
	
	public void setSourceElements(List<Type> newValues) {
		clearSourceListModel();
		addSourceElements(newValues);
	}

	public void addDestinationElements(Type newValue[]) {
		fillListModel(destListModel, newValue);
	}
	
	public void addDestinationElements(List<Type> newValues) {
		fillListModel(destListModel, newValues);
	}

	private void fillListModel(SortedListModel<Type> model, Type newValues[]) {
		model.addAll(newValues);
	}
	
	private void fillListModel(SortedListModel<Type> model, Collection<Type> newValues) {
		model.addAll(newValues);
	}

	public Iterator<Type> sourceIterator() {
		return sourceListModel.iterator();
	}
	
	public List<Type> getSourceList() {
		return sourceListModel.getElementList();
	}

	public Iterator<Type> destinationIterator() {
		return destListModel.iterator();
	}
	
	public List<Type> getDestinationList() {
		return destListModel.getElementList();
	}

	public void setSourceCellRenderer(ListCellRenderer<? super Type> newValue) {
		sourceList.setCellRenderer(newValue);
	}

	public ListCellRenderer<? super Type> getSourceCellRenderer() {
		return sourceList.getCellRenderer();
	}

	public void setDestinationCellRenderer(ListCellRenderer<? super Type> newValue) {
		destList.setCellRenderer(newValue);
	}

	public ListCellRenderer<? super Type> getDestinationCellRenderer() {
		return destList.getCellRenderer();
	}

	public void setVisibleRowCount(int newValue) {
		sourceList.setVisibleRowCount(newValue);
		destList.setVisibleRowCount(newValue);
	}

	public int getVisibleRowCount() {
		return sourceList.getVisibleRowCount();
	}

	public void setSelectionBackground(Color newValue) {
		sourceList.setSelectionBackground(newValue);
		destList.setSelectionBackground(newValue);
	}

	public Color getSelectionBackground() {
		return sourceList.getSelectionBackground();
	}

	public void setSelectionForeground(Color newValue) {
		sourceList.setSelectionForeground(newValue);
		destList.setSelectionForeground(newValue);
	}

	public Color getSelectionForeground() {
		return sourceList.getSelectionForeground();
	}

	@SuppressWarnings("unchecked")
	private void clearSourceSelected() {
		Object selected[] = sourceList.getSelectedValuesList().toArray();
		for (int i = selected.length - 1; i >= 0; --i) {
			sourceListModel.removeElement((Type)selected[i]);
		}
		sourceList.getSelectionModel().clearSelection();
	}

	@SuppressWarnings("unchecked")
	private void clearDestinationSelected() {
		Object selected[] = destList.getSelectedValuesList().toArray();
		for (int i = selected.length - 1; i >= 0; --i) {
			destListModel.removeElement((Type)selected[i]);
		}
		destList.getSelectionModel().clearSelection();
	}

	private void initScreen() {
		setBorder(BorderFactory.createEtchedBorder());
		setLayout(new GridBagLayout());
		sourceLabel = new JLabel(DEFAULT_SOURCE_CHOICE_LABEL);
		sourceListModel = new SortedListModel<Type>();
		sourceList = new JList<Type>(sourceListModel);
		add(sourceLabel, new GridBagConstraints(0, 0, 1, 1, 0, 0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				EMPTY_INSETS, 0, 0));
		add(new JScrollPane(sourceList), new GridBagConstraints(0, 1, 1, 5, .5,
				1, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				EMPTY_INSETS, 0, 0));

		addButton = new JButton(ADD_BUTTON_LABEL);
		add(addButton, new GridBagConstraints(1, 2, 1, 2, 0, .25,
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				EMPTY_INSETS, 0, 0));
		addButton.addActionListener(new AddListener());
		removeButton = new JButton(REMOVE_BUTTON_LABEL);
		add(removeButton, new GridBagConstraints(1, 4, 1, 2, 0, .25,
				GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(
						0, 5, 0, 5), 0, 0));
		removeButton.addActionListener(new RemoveListener());

		destLabel = new JLabel(DEFAULT_DEST_CHOICE_LABEL);
		destListModel = new SortedListModel<Type>();
		destList = new JList<Type>(destListModel);
		add(destLabel, new GridBagConstraints(2, 0, 1, 1, 0, 0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				EMPTY_INSETS, 0, 0));
		add(new JScrollPane(destList), new GridBagConstraints(2, 1, 1, 5, .5,
				1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				EMPTY_INSETS, 0, 0));
		
		sourceList.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
		        @SuppressWarnings("unchecked")
				JList<Type> list = (JList<Type>)e.getSource();
		        if (e.getClickCount() == 2) {
		            // Double-click detected
		            int index = list.locationToIndex(e.getPoint());
		            sourceList.setSelectedIndex(index);
		            addButton.doClick();
		        }/* else if (evt.getClickCount() == 3) {
		            // Triple-click detected
		            int index = list.locationToIndex(evt.getPoint());
		        }*/
		    }
		});
		
		destList.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
		        @SuppressWarnings("unchecked")
				JList<Type> list = (JList<Type>)e.getSource();
		        if (e.getClickCount() == 2) {
		            // Double-click detected
		            int index = list.locationToIndex(e.getPoint());
		            destList.setSelectedIndex(index);
		            removeButton.doClick();
		        }/* else if (evt.getClickCount() == 3) {
		            // Triple-click detected
		            int index = list.locationToIndex(evt.getPoint());
		        }*/
		    }
		});
		
	}

	public static void main(String args[]) {
		JFrame f = new JFrame("Dual List Box Tester");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		DualListBox<String> dual = new DualListBox<String>();
		dual.addSourceElements(new String[] { "One", "Two", "Three" });
		dual.addSourceElements(new String[] { "Four", "Five", "Six" });
		dual.addSourceElements(new String[] { "Seven", "Eight", "Nine" });
		dual.addSourceElements(new String[] { "Ten", "Eleven", "Twelve" });
		dual
		.addSourceElements(new String[] { "Thirteen", "Fourteen",
		"Fifteen" });
		dual.addSourceElements(new String[] { "Sixteen", "Seventeen",
		"Eighteen" });
		dual.addSourceElements(new String[] { "Nineteen", "Twenty", "Thirty" });
		f.getContentPane().add(dual, BorderLayout.CENTER);
		f.setSize(400, 300);
		f.setVisible(true);
	}

	private class AddListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			List<Type> selected = sourceList.getSelectedValuesList();
			addDestinationElements(selected);
			clearSourceSelected();
		}
	}

	private class RemoveListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			List<Type> selected = destList.getSelectedValuesList();
			addSourceElements(selected);
			clearDestinationSelected();
		}
	}
}

class SortedListModel<Type> extends AbstractListModel<Type> {
	private static final long serialVersionUID = -279404378358330310L;
	SortedSet<Type> model;

	public SortedListModel() {
		model = new TreeSet<Type>();
	}

	public int getSize() {
		return model.size();
	}

	@SuppressWarnings("unchecked")
	public Type getElementAt(int index) {
		return (Type)model.toArray()[index];
	}

	public void add(Type element) {
		if (model.add(element)) {
			fireContentsChanged(this, 0, getSize());
		}
	}
	
	public void addAll(Collection<Type> elements) {
		model.addAll(elements);
		fireContentsChanged(this, 0, getSize());
	}

	public void addAll(Type elements[]) {
		Collection<Type> c = Arrays.asList(elements);
		addAll(c);
	}

	public void clear() {
		model.clear();
		fireContentsChanged(this, 0, getSize());
	}

	public boolean contains(Type element) {
		return model.contains(element);
	}

	public Type firstElement() {
		return model.first();
	}

	public Iterator<Type> iterator() {
		return model.iterator();
	}

	public Type lastElement() {
		return model.last();
	}
	
	public List<Type> getElementList() {
		List<Type> result = new Vector<Type>(model.size());
		result.addAll(model);
		return result;
	}

	public boolean removeElement(Type element) {
		boolean removed = model.remove(element);
		if (removed) {
			fireContentsChanged(this, 0, getSize());
		}
		return removed;
	}
}