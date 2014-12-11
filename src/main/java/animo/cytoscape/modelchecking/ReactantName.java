package animo.cytoscape.modelchecking;

import animo.core.model.Model;

public class ReactantName {
	private Long cytoscapeID;
	private String canonicalName, modelID;

	public ReactantName(Long cytoscapeID, String canonicalName) {
		this.cytoscapeID = cytoscapeID;
		this.canonicalName = canonicalName;
	}

	public void setReactantID(Model m) {
		this.modelID = m.getReactantByCytoscapeID(cytoscapeID).getId();
	}

	public String toHumanReadable() {
		return canonicalName;
	}

	@Override
	public String toString() {
		if (modelID == null) {
			return "" + StateFormula.REACTANT_NAME_DELIMITER + cytoscapeID + StateFormula.REACTANT_NAME_DELIMITER;
		} else {
			return modelID;
		}
	}
}
