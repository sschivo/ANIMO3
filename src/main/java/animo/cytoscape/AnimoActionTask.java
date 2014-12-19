package animo.cytoscape;

import java.awt.event.ActionEvent;

import org.cytoscape.application.swing.AbstractCyAction;

public class AnimoActionTask extends AbstractCyAction {

	private static final long serialVersionUID = 7601367319473988438L;

	protected boolean needToStop = false; // Whether the user has pressed the Cancel button on the TaskMonitor while we were running an analysis process

	public AnimoActionTask(String init) {
		super(init);
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {

	}

	public boolean needToStop() {
		return this.needToStop;
	}

}
