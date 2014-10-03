package animo.inat.model;

public class Scenario2 extends Scenario
{
    private static final double DEFAULT_K = 0.004;

    @Override
    public double computeRate(int r1Level, int nLevelsR1, boolean activeR1, int r2Level, int nLevelsR2, boolean activeR2)
    {
        double par = parameters.get(Model.Properties.SCENARIO_PARAMETER_K);
        double e1;
        double e2;
        if (activeR1)
        {
            // If we depend on active R1, the activity level is the value of the parameter
            e1 = r1Level;
        }
        else
        {
            // If we depend on inactive R1, we must find the inactive part based on the total amount (n. of levels)
            e1 = nLevelsR1 - r1Level;
        }
        if (activeR2)
        {
            // Same reasoning here
            e2 = r2Level;
        }
        else
        {
            e2 = nLevelsR2 - r2Level;
        }
        return par * e1 * e2;
    }

    @Override
    public String[] getReactantNames()
    {
        return new String[] { "E1", "E2" };
    }

    @Override
    public String[] listVariableParameters()
    {
        return new String[] { Model.Properties.SCENARIO_PARAMETER_K };
    }

    @Override
    public String toString()
    {
        return "Scenario 3: k * [E1] * [E2]";
    }

}
