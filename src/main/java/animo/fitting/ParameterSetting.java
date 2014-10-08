package animo.fitting;

public class ParameterSetting {
	private String name = null;
	private boolean fixed = false;
	// This is the log-base for the logarithmic variable scale
	private boolean logarithmic = false;
	private Double fixedValue = null;
	private Double min = null;
	private Double max = null;
	private Double increase = null;

	public ParameterSetting(String name, Double fixedValue) {
		this.name = name;
		this.fixedValue = fixedValue;
		this.fixed = true;
	}

	public ParameterSetting(String name, Double min, Double max, Double increase) {
		this.name = name;
		this.min = min;
		this.max = max;
		this.increase = increase;
		this.fixed = false;
	}

	public Double getFixedValue() {
		return this.fixedValue;
	}

	public Double getIncrease() {
		return this.increase;
	}

	public Double getMax() {
		return this.max;
	}

	public Double getMin() {
		return this.min;
	}

	public String getName() {
		return this.name;
	}

	public boolean isFixed() {
		return fixed;
	}

	public boolean isLogarithmic() {
		return this.logarithmic;
	}

	public void setFixed(Double fixedValue) {
		this.fixedValue = fixedValue;
		this.fixed = true;
	}

	public void setVariable(Double min, Double max, Double increase, boolean logarithmic) {
		this.min = min;
		this.max = max;
		this.increase = increase;
		this.fixed = false;
		this.logarithmic = logarithmic;
	}

	@Override
	public String toString() {
		if (isFixed()) {
			return this.name + " fixed: value = " + fixedValue;
		} else {
			return this.name + " variable: min = " + min + ", max = " + max + ", inc = " + increase
					+ (logarithmic ? " (log)" : " (linear)");
		}
	}
}