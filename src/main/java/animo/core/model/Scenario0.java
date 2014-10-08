package animo.core.model;

public class Scenario0 extends Scenario {
	private static final double DEFAULT_K = 0.004;

	public Scenario0() {
		super();
		// TODO: set defaultK as parameter: parameters.put(key, value)
		parameters.put(Model.Properties.SCENARIO_PARAMETER_K, DEFAULT_K);
	}

	@Override
	public double computeRate(int r1Level, int nLevelsR1, boolean activeR1, int r2Level, int nLevelsR2, boolean activeR2) {
		double par = parameters.get(Model.Properties.SCENARIO_PARAMETER_K);
		double e;
		if (activeR1) { // If we depend on active R1, the level of activity is the value of E
			e = r1Level;
		} else { // otherwise we find the inactivity level via the total number of levels
			e = nLevelsR1 - r1Level;
		}
		return par * e;
	}

	@Override
	public String[] getReactantNames() {
		return new String[] { "E" };
	}

	@Override
	public String[] listVariableParameters() {
		return new String[] { Model.Properties.SCENARIO_PARAMETER_K };
	}

	@Override
	public String toString() {
		return "Scenario 1: k * [E]";
	}
}
