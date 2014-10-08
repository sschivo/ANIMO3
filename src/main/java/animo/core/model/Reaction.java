/**
 * 
 */
package animo.core.model;

import java.io.Serializable;

import animo.fitting.ScenarioCfg;

/**
 * A connection between two components in the network.
 * 
 * @author B. Wanders
 */
public class Reaction extends Entity implements Serializable {
	private static final long serialVersionUID = 6737480973051526963L;

	public Reaction() {
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 *            the identifier for this edge
	 */
	public Reaction(String id) {
		super(id);
	}

	/**
	 * returns a (deep) copy of this
	 * 
	 * @return
	 */
	public Reaction copy() {
		Reaction e = new Reaction(this.id);
		e.setModel(this.getModel());
		e.properties = this.properties.copy();
		return e;
	}

	public boolean getEnabled() {
		return this.get(Model.Properties.ENABLED).as(Boolean.class);
	}

	public String getName() {
		return this.get(Model.Properties.ALIAS).as(String.class);
	}

	public ScenarioCfg getScenarioCfg() {
		return this.get(Model.Properties.SCENARIO_CFG).as(ScenarioCfg.class);
	}

	public void setName(String name) {
		this.let(Model.Properties.ALIAS).be(name);
	}

	public void setScenarioCfg(ScenarioCfg cfg) {
		this.let(Model.Properties.SCENARIO_CFG).be(cfg);
	}

	@Override
	public String toString() {
		return "Reaction '" + this.getId() + "'";
	}
}
