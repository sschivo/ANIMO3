package animo.cytoscape.contextmenu;

import javax.swing.SwingUtilities;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.task.NodeViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

import animo.cytoscape.NodeDialog;

public class NodeDoubleClickTaskFactory implements NodeViewTaskFactory {
	@Override
	public TaskIterator createTaskIterator(View<CyNode> nodeView, CyNetworkView networkView) {
		return new TaskIterator(new NodeDoubleClickTask(nodeView, networkView));
	}

	@Override
	public boolean isReady(View<CyNode> nodeView, CyNetworkView networkView) {
		return true;
	}
	
	class NodeDoubleClickTask extends AbstractTask {
		private CyNode node;
		private CyNetwork network;
		
		public NodeDoubleClickTask(View<CyNode> nodeView, CyNetworkView networkView) {
			this.node = nodeView.getModel();
			this.network = networkView.getModel();
		}

		@Override
		public void run(TaskMonitor tm) throws Exception {
			tm.setTitle("Edit reactant...");
			tm.setProgress(1.0);
			SwingUtilities.invokeLater(new Thread() {
				public void run() {
					NodeDialog dialog = new NodeDialog(network, node);
					dialog.showYourself();
				}
			});
			
		}
	}

}
