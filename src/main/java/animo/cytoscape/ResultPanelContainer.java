package animo.cytoscape;

import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JPanel;

import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;

public class ResultPanelContainer extends JPanel implements CytoPanelComponent {
	private static final long serialVersionUID = -255471556831642543L;
	private AnimoResultPanel resultPanel;

	public ResultPanelContainer(AnimoResultPanel resultPanel) {
		this.resultPanel = resultPanel;
	}

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public CytoPanelName getCytoPanelName() {
		return CytoPanelName.EAST;
	}

	@Override
	public Icon getIcon() {
		return null;
	}

	@Override
	public String getTitle() {
		return this.getName();
	}

	//Make sure that the proper ANIMO visual style is selected
	public void ensureCorrectVisualStyle() {
		resultPanel.ensureCorrectVisualStyle();
	}

}
