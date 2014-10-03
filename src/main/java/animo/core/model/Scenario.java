package animo.core.model;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import animo.util.Pair;

/**
 * Represents a scenario for a reaction in the model.
 * There are 3 predefined scenarios, each one with its set of parameters.
 */
public abstract class Scenario
{
    protected double k;

    protected static double defaultK = 0.004;
    /**
     * Parameter name -> value
     */
    protected Map<String, Double> parameters = new HashMap<String, Double>();
    /**
     * The defaul values of parameters
     */
    protected Map<String, Double> defaultParameterValues = new HashMap<String, Double>();
    /**
     * Reactant name (i.e., E, S, E1, E2) --> <reactant ID, active/inactive>
     */
    protected Map<String, Pair<String, Boolean>> reactants = new HashMap<String, Pair<String, Boolean>>();
    /**
     * The constant to mean that the reaction will not happen
     */
    public static final int INFINITE_TIME = -1;

    /**
     * The default predefined scenarios
     */
    public static final Scenario[] SIX_SCENARIOS = { new Scenario0(), new Scenario1(), new Scenario2() };

    public Scenario()
    {
        k = defaultK;
    }


    public Scenario(Scenario source)
    {
        k = source.k;
        this.parameters = new HashMap<String, Double>(source.parameters);
    }

    public Double computeFormula(int r1Level, int nLevelsR1, boolean activeR1, int r2Level, int nLevelsR2, boolean activeR2)
    {
        double rate = computeRate(r1Level, nLevelsR1, activeR1, r2Level, nLevelsR2, activeR2);
        if (rate > 1e-8)
        {
            //return Math.max(0, (int)Math.round(1 / rate)); //We need to put at least 1 because otherwise the reaction will keep happening forever (it is not very nice not to let time pass..)
            return 1.0 / rate;
        }
        else
        {
            //return INFINITE_TIME;
            return Double.POSITIVE_INFINITY;
        }
    }

    public abstract double computeRate(int r1Level, int nLevelsR1, boolean activeR1, int r2Level, int nLevelsR2, boolean activeR2);

    /**
     * Generate the times table based on the scenario formula and parameters.
     * @param nLevelsReactant1 The total number of levels of reactant1 (the enzyme or catalyst) + 1 (!! Note the +1! Basically, this is the number of columns of the time table, so when using it as the number of activity levels of the enzyme we must subtract 1)
     * @param activeR1 true if the R1 input to the formula is the concentration of active reactant 1
     * @param reactant1IsDownstream true when the reactant 1 is a downstream reactant (the reaction time depends on its activity, but we treat limit cases as their nearest neighbours to ensure that boolean networks behave correctly) 
     * @param nLevelsReactant2 The total number of levels of reactant2 (the substrate) + 1 (!! Note the +1)
     * @param activeR2 true if the R2 input to the formula is the concentration of active reactant 2
     * @param reactant2IsDownstream see reactant1IsDownstream
     * @return Output is a list for no particular reason anymore. It used to be
     * set as a Cytoscape property, but these tables can become clumsy to be stored
     * inside the model, slowing down the loading/saving processes in the Cytoscape
     * interface. We now compute the tables on the fly and output them directly as
     * reference constants in the UPPPAAL models, saving also memory.
     */
    public List<Double> generateTimes(int nLevelsReactant1, boolean activeR1, boolean reactant1IsDownstream, int nLevelsReactant2, boolean activeR2,
            boolean reactant2IsDownstream)
    {
        List<Double> times = new LinkedList<Double>();
        int i, limitI;
        if (!activeR2)
        { //We depend on the inactivity of R2, which is completely inactive (first row --> R2 = 0), so the first row will have the smallest values
            i = 0;
            limitI = nLevelsReactant2 - 1; //the last row will be all infinite
        }
        else
        { //We depend on the activity of R2, which is completely inactive, so the first row should be all infinite
            if (reactant2IsDownstream)
            {
                for (int j = 0; j < nLevelsReactant1; j++)
                {
                    times.add(computeFormula(j, nLevelsReactant1 - 1, activeR1, 1, nLevelsReactant2 - 1, activeR2));
                }
            }
            else
            {
                for (int j = 0; j < nLevelsReactant1; j++)
                {
                    times.add(Double.POSITIVE_INFINITY); //all reactant2 already reacted (inactive) = no reaction
                }
            }
            i = 1; //the first row was already done here, with all infinites
            limitI = nLevelsReactant2; //the last row will have the smallest values
        }
        for (; i < limitI; i++)
        {
            int j, limitJ;
            if (!activeR1)
            { //We depend on the inactivity of R1, which when j = 0 is completely inactive (first column --> R1 = 0), so the first column will have the smallest values
                j = 0;
                limitJ = nLevelsReactant1 - 1; //the last column will be all infinite
            }
            else
            {
                if (reactant1IsDownstream)
                {
                    times.add(computeFormula(1, nLevelsReactant1 - 1, activeR1, i, nLevelsReactant2 - 1, activeR2));
                }
                else
                {
                    times.add(Double.POSITIVE_INFINITY); //no reactant1 = no reaction
                }
                j = 1;
                limitJ = nLevelsReactant1; //the last column will have the smallest values
            }
            for (; j < limitJ; j++)
            {
                times.add(computeFormula(j, nLevelsReactant1 - 1, activeR1, i, nLevelsReactant2 - 1, activeR2));
            }
            if (!activeR1)
            { //We depend on the inactivity of R1, which in the last column is completely active. So the last column has all infinite
                if (reactant1IsDownstream)
                {
                    times.add(computeFormula(nLevelsReactant1 - 2, nLevelsReactant1 - 1, activeR1, i, nLevelsReactant2 - 1, activeR2));
                }
                else
                {
                    times.add(Double.POSITIVE_INFINITY);
                }
            }
        }
        if (!activeR2)
        { //We depend on the inactivity of R2, which in the last row is completely active. So the last row has all infinite
            for (int j = 0; j < nLevelsReactant1; j++)
            {
                if (reactant2IsDownstream)
                {
                    times.add(computeFormula(j, nLevelsReactant1 - 1, activeR1, nLevelsReactant2 - 2, nLevelsReactant2 - 1, activeR2));
                }
                else
                {
                    times.add(Double.POSITIVE_INFINITY); //all reactant2 already reacted (active) = no reaction
                }
            }
        }
        return times;
    }

    public Double getDefaultParameterValue(String name)
    {
        return defaultParameterValues.get(name);
    }

    public Map<String, Double> getDefaultParameterValues()
    {
        return new HashMap<String, Double>(defaultParameterValues);
    }

    public Double getParameter(String name)
    {
        return parameters.get(name);
    }

    public Map<String, Double> getParameters()
    {
        return new HashMap<String, Double>(parameters);
    }

    public abstract String[] getReactantNames();

    public Map<String, Pair<String, Boolean>> getReactants()
    {
        return reactants; //No clone! We want to set them!
    }

    public abstract String[] listVariableParameters();

    public void setDefaultParameterValue(String name, Double value)
    {
        defaultParameterValues.put(name, value);
        parameters.put(name, value);
    }

    public void setParameter(String name, Double value)
    {
        parameters.put(name, value);
    }

    /**
     * 
     * @param reactantName
     * @param linkedTo contains <Cytoscape reactant ID, whether we consider the active or inactive part of that reactant as parameter for this scenario>
     */
    public void setReactant(String reactantName, Pair<String, Boolean> linkedTo)
    {
        reactants.put(reactantName, linkedTo);
    }

    @Override
    public abstract String toString();
}