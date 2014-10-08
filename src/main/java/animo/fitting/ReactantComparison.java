package animo.fitting;

import java.util.ArrayList;
import java.util.List;

public class ReactantComparison {
	private String csvFile = null;
	private List<String> seriesNames = null;
	private Double maxError = null;

	public ReactantComparison() {
		this.csvFile = "";
		this.seriesNames = new ArrayList<String>();
		this.maxError = 0.0;
	}

	public ReactantComparison(String csvFile, List<String> seriesNames, Double maxError) {
		this.csvFile = csvFile;
		this.seriesNames = seriesNames;
		this.maxError = maxError;
	}

	public String getCsvFile() {
		return this.csvFile;
	}

	public Double getMaxError() {
		return this.maxError;
	}

	public List<String> getSeriesNames() {
		return this.seriesNames;
	}

	public void setCsvFile(String csvFile) {
		this.csvFile = csvFile;
	}

	public void setMaxError(Double maxError) {
		this.maxError = maxError;
	}

	public void setSeriesNames(List<String> seriesNames) {
		this.seriesNames = seriesNames;
	}
}