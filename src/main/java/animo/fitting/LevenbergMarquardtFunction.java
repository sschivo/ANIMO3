package animo.fitting;

import java.awt.Color;
import java.awt.Component;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.swing.JFrame;

import org.ejml.data.DenseMatrix64F;

import animo.core.analyser.uppaal.SimpleLevelResult;
import animo.core.analyser.uppaal.UppaalModelAnalyserSMC;
import animo.core.analyser.uppaal.VariablesModel;
import animo.core.graph.Graph;
import animo.core.model.Model;
import animo.core.model.Reactant;
import animo.core.model.Reaction;
import animo.core.model.Scenario;
import animo.util.LevenbergMarquardt;
import animo.util.LevenbergMarquardt.Function;

//Computes the X * params = Y for the L&M method,
//where X is the initial state of the ANIMO model
//Y is the time series originated from that model
//when considering the given parameters params
//All three are single-column matrices (as ugly as this can be..)
public class LevenbergMarquardtFunction implements Function {
	private Model model;
	private Map<Reaction, Map<String, Integer>> reactionParameterIndices; //Associate a Map to each reaction.
																		  //The Map gives the integer corresponding to the index of each parameter of this reaction inside the params DenseMatrix64F, which contains all the reaction parameters in one (column)matrix
	int minTimeModel;
	int maxTimeModel;
	private Graph graph;
	
	private int contaTentativi = 0;
	
	public LevenbergMarquardtFunction(Graph graph, Model model) {
		this.graph = graph;
		this.model = model;
	}
	
	public DenseMatrix64F getInitialParameters() {
		DenseMatrix64F initialParameters = null;
		//We must set this ourself.
		Vector<Double> params = new Vector<Double>();
		//Also, we are supposed to fill the reactionParameterIndices with the same indices we use when creating the parameters matrix
		reactionParameterIndices = new HashMap<Reaction, Map<String, Integer>>();
		int idx = 0;
		for (Reaction r : model.getReactionCollection()) {
			Map<String, Integer> reactionParameters = new HashMap<String, Integer>();
			Scenario scenario = Scenario.THREE_SCENARIOS[r.get(Model.Properties.SCENARIO).as(Integer.class)];
			for (String paramName : scenario.getReactantNames()) {
				reactionParameters.put(paramName, idx++);
				double paramValue = r.get(paramName).as(Double.class);
				params.add(paramValue);
			}
			reactionParameterIndices.put(r, reactionParameters);
		}
		double[][] paramsData = new double[params.size()][1];
		for (int i = 0; i < params.size(); i++) {
			paramsData[i][0] = params.get(i);
		}
		initialParameters = new DenseMatrix64F(paramsData);
		return initialParameters;
	}
	
	public LevenbergMarquardtFunction(Graph graph) {
		this();
		this.graph = graph;
	}
	
	public LevenbergMarquardtFunction() {
		minTimeModel = Integer.MAX_VALUE;
		maxTimeModel = Integer.MIN_VALUE;
		model = new Model();
		model.getProperties().let(Model.Properties.NUMBER_OF_LEVELS).be(100);
		model.getProperties().let(Model.Properties.SECONDS_PER_POINT).be(0.01);
		model.getProperties().let(Model.Properties.SECS_POINT_SCALE_FACTOR).be(100.0);
		
		Reactant reactant = new Reactant("R0");
		reactant.let(Model.Properties.REACTANT_NAME).be("Wnt");
		reactant.setName("Wnt");
		reactant.let(Model.Properties.NUMBER_OF_LEVELS).be(100);
		reactant.let(Model.Properties.LEVELS_SCALE_FACTOR).be(6.6666);
		reactant.let(Model.Properties.ENABLED).be(true);
		reactant.let(Model.Properties.PLOTTED).be(false);
		reactant.let(Model.Properties.INITIAL_LEVEL).be(100);
		model.add(reactant);
		
		reactant = new Reactant("R1");
		reactant.let(Model.Properties.REACTANT_NAME).be("FRZLD");
		reactant.setName("FRZLD");
		reactant.let(Model.Properties.NUMBER_OF_LEVELS).be(100);
		reactant.let(Model.Properties.LEVELS_SCALE_FACTOR).be(6.6666);
		reactant.let(Model.Properties.ENABLED).be(true);
		reactant.let(Model.Properties.PLOTTED).be(false);
		reactant.let(Model.Properties.INITIAL_LEVEL).be(0);
		model.add(reactant);
		
		reactant = new Reactant("R2");
		reactant.let(Model.Properties.REACTANT_NAME).be("FRZLD_Int");
		reactant.setName("FRZLD_Int");
		reactant.let(Model.Properties.NUMBER_OF_LEVELS).be(100);
		reactant.let(Model.Properties.LEVELS_SCALE_FACTOR).be(6.6666);
		reactant.let(Model.Properties.ENABLED).be(true);
		reactant.let(Model.Properties.PLOTTED).be(false);
		reactant.let(Model.Properties.INITIAL_LEVEL).be(0);
		model.add(reactant);
		
		reactant = new Reactant("R3");
		reactant.let(Model.Properties.REACTANT_NAME).be("ERK");
		reactant.setName("ERK");
		reactant.let(Model.Properties.NUMBER_OF_LEVELS).be(100);
		reactant.let(Model.Properties.LEVELS_SCALE_FACTOR).be(6.6666);
		reactant.let(Model.Properties.ENABLED).be(true);
		reactant.let(Model.Properties.PLOTTED).be(true);
		reactant.let(Model.Properties.INITIAL_LEVEL).be(0);
		model.add(reactant);
		
		reactant = new Reactant("R4");
		reactant.let(Model.Properties.REACTANT_NAME).be("ERK P");
		reactant.setName("ERK P");
		reactant.let(Model.Properties.NUMBER_OF_LEVELS).be(100);
		reactant.let(Model.Properties.LEVELS_SCALE_FACTOR).be(6.6666);
		reactant.let(Model.Properties.ENABLED).be(true);
		reactant.let(Model.Properties.PLOTTED).be(false);
		reactant.let(Model.Properties.INITIAL_LEVEL).be(100);
		model.add(reactant);
		
		Reaction wnt_frzld,
				 frzld_frzldInt,
				 frzldInt_frzld,
				 frzld_erk,
				 erkP_erk;
		
		wnt_frzld = new Reaction("Wnt -> FRZLD");
		wnt_frzld.let(Model.Properties.ENABLED).be(true);
		wnt_frzld.let(Model.Properties.INCREMENT).be(1);
		wnt_frzld.let(Model.Properties.SCENARIO).be(1);
		wnt_frzld.let(Model.Properties.CYTOSCAPE_ID).be("E0");
		wnt_frzld.let(Model.Properties.REACTION_TYPE).be(Model.Properties.BI_REACTION);
		wnt_frzld.let(Model.Properties.REACTANT).be("R1"); //target
		wnt_frzld.let(Model.Properties.CATALYST).be("R0"); //source
		setParameters(model, wnt_frzld, 0.01);
		model.add(wnt_frzld);
		
		frzld_frzldInt = new Reaction("FRZLD -> FRZLD Int");
		frzld_frzldInt.let(Model.Properties.ENABLED).be(true);
		frzld_frzldInt.let(Model.Properties.INCREMENT).be(1);
		frzld_frzldInt.let(Model.Properties.SCENARIO).be(1);
		frzld_frzldInt.let(Model.Properties.CYTOSCAPE_ID).be("E1");
		frzld_frzldInt.let(Model.Properties.REACTION_TYPE).be(Model.Properties.BI_REACTION);
		frzld_frzldInt.let(Model.Properties.REACTANT).be("R2"); //target
		frzld_frzldInt.let(Model.Properties.CATALYST).be("R1"); //source
		setParameters(model, frzld_frzldInt, 0.01);
		model.add(frzld_frzldInt);
		
		frzldInt_frzld = new Reaction("FRZLD Int -| FRZLD");
		frzldInt_frzld.let(Model.Properties.ENABLED).be(true);
		frzldInt_frzld.let(Model.Properties.INCREMENT).be(-1);
		frzldInt_frzld.let(Model.Properties.SCENARIO).be(0);
		frzldInt_frzld.let(Model.Properties.CYTOSCAPE_ID).be("E2");
		frzldInt_frzld.let(Model.Properties.REACTION_TYPE).be(Model.Properties.BI_REACTION);
		frzldInt_frzld.let(Model.Properties.REACTANT).be("R1"); //target
		frzldInt_frzld.let(Model.Properties.CATALYST).be("R2"); //source
		setParameters(model, frzldInt_frzld, 0.01);
		model.add(frzldInt_frzld);
		
		frzld_erk = new Reaction("FRZLD -> ERK");
		frzld_erk.let(Model.Properties.ENABLED).be(true);
		frzld_erk.let(Model.Properties.INCREMENT).be(1);
		frzld_erk.let(Model.Properties.SCENARIO).be(1);
		frzld_erk.let(Model.Properties.CYTOSCAPE_ID).be("E3");
		frzld_erk.let(Model.Properties.REACTION_TYPE).be(Model.Properties.BI_REACTION);
		frzld_erk.let(Model.Properties.REACTANT).be("R3"); //target
		frzld_erk.let(Model.Properties.CATALYST).be("R1"); //source
		setParameters(model, frzld_erk, 0.01);
		model.add(frzld_erk);
		
		erkP_erk = new Reaction("ERK P -| ERK");
		erkP_erk.let(Model.Properties.ENABLED).be(true);
		erkP_erk.let(Model.Properties.INCREMENT).be(-1);
		erkP_erk.let(Model.Properties.SCENARIO).be(0);
		erkP_erk.let(Model.Properties.CYTOSCAPE_ID).be("E4");
		erkP_erk.let(Model.Properties.REACTION_TYPE).be(Model.Properties.BI_REACTION);
		erkP_erk.let(Model.Properties.REACTANT).be("R3"); //target
		erkP_erk.let(Model.Properties.CATALYST).be("R4"); //source
		setParameters(model, erkP_erk, 0.01);
		model.add(erkP_erk);
		
		reactionParameterIndices = new HashMap<Reaction, Map<String, Integer>>();
		//reactionParameterIndices.put(wnt_frzld, 0);
		String kScenario0 = Scenario.THREE_SCENARIOS[0].listVariableParameters()[0],
			   kScenario1 = Scenario.THREE_SCENARIOS[1].listVariableParameters()[0];
		
		Map<String, Integer> wnt_frzldMap = new HashMap<String, Integer>();
		wnt_frzldMap.put(kScenario1, 0);
		reactionParameterIndices.put(wnt_frzld, wnt_frzldMap);
		
		Map<String, Integer> frzld_frzldIntMap = new HashMap<String, Integer>();
		frzld_frzldIntMap.put(kScenario1, 1);
		reactionParameterIndices.put(frzld_frzldInt, frzld_frzldIntMap);
		
		Map<String, Integer> frzldInt_frzldMap = new HashMap<String, Integer>();
		frzldInt_frzldMap.put(kScenario0, 2);
		reactionParameterIndices.put(frzldInt_frzld, frzldInt_frzldMap);
		
		Map<String, Integer> frzld_erkMap = new HashMap<String, Integer>();
		frzld_erkMap.put(kScenario1, 3);
		reactionParameterIndices.put(frzld_erk, frzld_erkMap);
		
		Map<String, Integer> erkP_erkMap = new HashMap<String, Integer>();
		erkP_erkMap.put(kScenario0, 4);
		reactionParameterIndices.put(erkP_erk, erkP_erkMap);
		
		if (minTimeModel == Integer.MAX_VALUE) {
			minTimeModel = VariablesModel.INFINITE_TIME;
		}
		model.getProperties().let(Model.Properties.MINIMUM_DURATION).be(minTimeModel);
		if (maxTimeModel == Integer.MIN_VALUE) {
			maxTimeModel = VariablesModel.INFINITE_TIME;
		}
		model.getProperties().let(Model.Properties.MAXIMUM_DURATION).be(maxTimeModel);
		
	}
	
	private void updateParameters(DenseMatrix64F params) {
		for (Reaction r : reactionParameterIndices.keySet()) {
			Map<String, Integer> paramIndices = reactionParameterIndices.get(r);
			for (String paramName : paramIndices.keySet()) {
				double paramValue = params.get(paramIndices.get(paramName));
				setParameter(r, paramName, paramValue);
			}
		}
	}
	
	private void setParameter(Reaction reaction, String parName, double parValue) {
		reaction.let(parName).be(parValue);
	}
	
	private void setParameters(Model model, Reaction reaction, double parameter) {
		Integer scenarioIdx = reaction.get(Model.Properties.SCENARIO).as(Integer.class);
		final int increment = reaction.get(Model.Properties.INCREMENT).as(Integer.class);
		double secStepFactor = model.getProperties().get(Model.Properties.SECS_POINT_SCALE_FACTOR).as(Double.class);
		int nLevelsR1 = 100, nLevelsR2 = 100;
		Scenario[] scenarios = Scenario.THREE_SCENARIOS;
		Scenario scenario;
		double levelsScaleFactor = 1.0;
		String reactant = reaction.get(Model.Properties.REACTANT).as(String.class),
			   catalyst = reaction.get(Model.Properties.CATALYST).as(String.class);
		scenario = scenarios[scenarioIdx];
		switch (scenarioIdx) {
			case 0:
				levelsScaleFactor = 1.0
						/ model.getReactant(reactant).get(Model.Properties.NUMBER_OF_LEVELS).as(Integer.class)
						* model.getReactant(catalyst).get(Model.Properties.NUMBER_OF_LEVELS).as(Integer.class);
				break;
			case 1:
				levelsScaleFactor = 1.0 * model.getReactant(catalyst).get(Model.Properties.NUMBER_OF_LEVELS)
						.as(Integer.class);
				break;
			case 2:
				//Assuming no scenario 3 here in the small example, for simplicity's sake...
				break;
			default:
				levelsScaleFactor = 1.0;
				break;
		}
		reaction.let(Model.Properties.SCENARIO).be(scenarioIdx);
		reaction.let(Model.Properties.LEVELS_SCALE_FACTOR + "_reaction").be(levelsScaleFactor);
		reaction.let(Model.Properties.SCENARIO_PARAMETER_K).be(parameter);

		String[] parameters = scenario.listVariableParameters();
		HashMap<String, Double> scenarioParameterValues = new HashMap<String, Double>();
		for (int j = 0; j < parameters.length; j++) {
			Double parVal = parameter; //I ASSUME THAT THE PARAMETER IS ONLY ONE: k
			scenario.setParameter(parameters[j], parVal);
			scenarioParameterValues.put(parameters[j], parVal);
		}
		reaction.let(Model.Properties.SCENARIO_CFG).be(new ScenarioCfg(scenarioIdx, scenarioParameterValues));
		reaction.let(Model.Properties.OUTPUT_REACTANT).be(reactant);
		
		String reactionAlias;
		reactionAlias = model.getReactant(reaction.get(Model.Properties.CATALYST).as(String.class)).getName();
		if (scenarioIdx == 2) {
			reactionAlias += " AND "
					+ model.getReactant(reaction.get(Model.Properties.REACTANT).as(String.class)).getName();
		}
		if (reaction.get(Model.Properties.INCREMENT).as(Integer.class) >= 0) {
			reactionAlias += " --> ";
		} else {
			reactionAlias += " --| ";
		}
		if (scenarioIdx == 2) {
			reactionAlias += model.getReactant(reaction.get(Model.Properties.OUTPUT_REACTANT).as(String.class)).getName();
		} else {
			reactionAlias += model.getReactant(reaction.get(Model.Properties.REACTANT).as(String.class)).getName();
		}
		reaction.setName(reactionAlias);

		boolean activeR1 = true, activeR2 = false;
		boolean reactant1IsDownstream = false, reactant2IsDownstream = true;

		if (scenarioIdx == 0 || scenarioIdx == 1) {
			activeR1 = true;
			if (increment >= 0) {
				activeR2 = false;
			} else {
				activeR2 = true;
			}
		} else {
			// This should never happen, because we have already made these checks
			activeR1 = activeR2 = true;
		}
		reaction.let(Model.Properties.R1_IS_DOWNSTREAM).be(reactant1IsDownstream);
		reaction.let(Model.Properties.R2_IS_DOWNSTREAM).be(reactant2IsDownstream);

		double uncertainty = 0.0;
		Double maxValueFormula = Double.POSITIVE_INFINITY, minValueFormula;
		int maxValueInTables, minValueInTables;
		int colMax, rowMax, incrementColMax, incrementRowMax, colMin, rowMin;
		if (activeR1 && !activeR2) {
			colMax = 0;
			rowMax = nLevelsR2; // The largest number should be in the lower-left corner (the first not to be considered INFINITE_TIME)
			incrementColMax = 1;
			incrementRowMax = -1;
			colMin = nLevelsR1;
			rowMin = 0; // The smallest number should be in the top-right corner
		} else if (activeR1 && activeR2) {
			colMax = 0;
			rowMax = 0; // The largest number should be in the top-left corner (the first != INF)
			incrementColMax = 1;
			incrementRowMax = 1;
			colMin = nLevelsR1;
			rowMin = nLevelsR2; // The smallest number should be in the lower right corner
		} else if (!activeR1 && !activeR2) {
			colMax = nLevelsR1;
			rowMax = nLevelsR2; // The largest number should be in the lower right corner (the first != INF)
			incrementColMax = -1;
			incrementRowMax = -1;
			colMin = 0;
			rowMin = 0; // The smallest number should be in the top-left corner
		} else if (!activeR1 && activeR2) {
			colMax = nLevelsR1;
			rowMax = 0; // The largest number should be in the top-right corner (the first != INF)
			incrementColMax = -1;
			incrementRowMax = 1;
			colMin = 0;
			rowMin = nLevelsR2; // The smallest number should be in the lower-left corner
		} else {
			// This should never happen, as we have already considered all 4 possibilities for activeR1 and activeR2
			colMax = rowMax = colMin = rowMin = incrementColMax = incrementRowMax = 1;
		}
		minValueFormula = scenario.computeFormula(colMin, nLevelsR1, activeR1, rowMin, nLevelsR2, activeR2);
		while (Double.isInfinite(maxValueFormula) && colMax >= 0 && colMax <= nLevelsR1 && rowMax >= 0
				&& rowMax <= nLevelsR2) {
			colMax = colMax + incrementColMax;
			rowMax = rowMax + incrementRowMax;
			maxValueFormula = scenario.computeFormula(colMax, nLevelsR1, activeR1, rowMax, nLevelsR2, activeR2);
		}

		if (Double.isInfinite(minValueFormula)) {
			minValueInTables = VariablesModel.INFINITE_TIME;
		} else {
			if (uncertainty == 0) {
				minValueInTables = Math.max(0,
						(int) Math.round(secStepFactor * levelsScaleFactor * minValueFormula));
			} else {
				minValueInTables = Math.max(
						0,
						(int) Math.round(secStepFactor * levelsScaleFactor * minValueFormula
								* (1 - uncertainty / 100.0)));
			}
		}
		if (Double.isInfinite(maxValueFormula)) {
			maxValueInTables = VariablesModel.INFINITE_TIME;
		} else {
			if (uncertainty == 0) {
				maxValueInTables = Math.max(0,
						(int) Math.round(secStepFactor * levelsScaleFactor * maxValueFormula));
			} else {
				maxValueInTables = Math.max(
						0,
						(int) Math.round(secStepFactor * levelsScaleFactor * maxValueFormula
								* (1 + uncertainty / 100.0)));
			}
		}
		reaction.let(Model.Properties.MINIMUM_DURATION).be(minValueInTables);
		reaction.let(Model.Properties.MAXIMUM_DURATION).be(maxValueInTables);
		if (minValueInTables != VariablesModel.INFINITE_TIME
				&& (minTimeModel == Integer.MAX_VALUE || minValueInTables < minTimeModel)) {
			minTimeModel = minValueInTables;
		}
		if (maxValueInTables != VariablesModel.INFINITE_TIME
				&& (maxTimeModel == Integer.MIN_VALUE || maxValueInTables > maxTimeModel)) {
			maxTimeModel = maxValueInTables;
		}

		String r1Id = reaction.get(Model.Properties.CATALYST).as(String.class);
		String r2Id = reaction.get(Model.Properties.REACTANT).as(String.class);
		String rOutput = reaction.get(Model.Properties.OUTPUT_REACTANT).as(String.class);
		reaction.setId(r1Id + "_" + r2Id + ((rOutput.equals(r2Id)) ? "" : "_" + rOutput));

	}
	
	@Override
	public void compute(DenseMatrix64F param, DenseMatrix64F x, DenseMatrix64F y) {
		contaTentativi++;
		updateParameters(param);
		int nMinutesToSimulate = 240;
		int timeTo = (int) (nMinutesToSimulate * 60.0 / model.getProperties().get(Model.Properties.SECONDS_PER_POINT).as(Double.class));
		double scale = (double) nMinutesToSimulate / timeTo;
		SimpleLevelResult result = null;
		try {
			PrintStream sysErr = System.err;
			System.setErr(new PrintStream(new OutputStream() {
			    public void write(int b) {
			    	//Not interested in UPPAAL analysis output here
			    }
			}));
			result = (SimpleLevelResult)new UppaalModelAnalyserSMC(null, null).analyze(model, timeTo).filter(Arrays.asList("R3", "R1"));
			System.setErr(sysErr);
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
		//graph.reset();
		Map<String, String> seriesNameMapping = new HashMap<String, String>();
		seriesNameMapping.put("R3", "ERK");
		seriesNameMapping.put("R1", "Frizzled");
		graph.reset();
		graph.parseLevelResult(result, seriesNameMapping, scale);
		double maxTime = scale * result.getTimeIndices().get(result.getTimeIndices().size() - 1);
		try {
			graph.parseCSV("/Users/stefano/Documents/Lavoro/Prometheus/Data_Wnt_0-240_erk-frzld.csv");
			Vector<String> seriesNames = graph.getSeriesNames();
			for (int i = 0; i < seriesNames.size(); i++) {
				String name = seriesNames.get(i);
				if (name.equals("ERK")) {
					graph.setSeriesColor(i, Color.BLUE.brighter());
				} else if (name.startsWith("ERK")) {
					graph.setSeriesColor(i, Color.BLUE.darker());
				} else if (name.equals("Frizzled")) {
					graph.setSeriesColor(i, Color.RED.brighter());
				} else if (name.startsWith("Frizzled")) {
					graph.setSeriesColor(i, Color.RED.darker());
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace(System.err);
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
		graph.declareMaxYValue(100);
		graph.setDrawArea(0, maxTime, 0, 100);
		Component w = graph.getParent();
		while (w != null) {
			if (w instanceof JFrame) {
				JFrame f = (JFrame)w;
				f.setTitle("Attempt nr. " + contaTentativi);
				break;
			}
			w = w.getParent();
		}
		graph.repaint();
		y.set(LevenbergMarquardt.levelResultToMatrix(result, scale, Arrays.asList(0.0, 30.0, 60.0, 120.0, 240.0)));
	}
}