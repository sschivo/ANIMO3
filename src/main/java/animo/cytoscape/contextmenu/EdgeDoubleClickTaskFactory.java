package animo.cytoscape.contextmenu;

import javax.swing.SwingUtilities;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.task.EdgeViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

import animo.cytoscape.EdgeDialog;

public class EdgeDoubleClickTaskFactory implements EdgeViewTaskFactory {

	@Override
	public TaskIterator createTaskIterator(View<CyEdge> edgeView, CyNetworkView networkView) {
		return new TaskIterator(new EdgeDoubleClickTask(edgeView, networkView));
	}

	@Override
	public boolean isReady(View<CyEdge> edgeView, CyNetworkView networkView) {
		return true;
	}

	class EdgeDoubleClickTask extends AbstractTask {
		private CyEdge edge;
		private CyNetwork network;
		
		public EdgeDoubleClickTask(View<CyEdge> edgeView, CyNetworkView networkView) {
			this.edge = edgeView.getModel();
			this.network = networkView.getModel();
		}

		@Override
		public void run(TaskMonitor tm) throws Exception {
			tm.setTitle("Edit reaction...");
			tm.setProgress(1.0);
			SwingUtilities.invokeLater(new Thread() {
				public void run() {
					EdgeDialog dialog = new EdgeDialog(network, edge);
					dialog.showYourself();
				}
			});
		}
	}
}
