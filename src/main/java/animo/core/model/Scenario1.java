package animo.core.model;

public class Scenario1 extends Scenario
{
    private static final double DEFAULT_K = 0.004;

    @Override
    public double computeRate(int r1Level, int nLevelsR1, boolean activeR1, int r2Level, int nLevelsR2, boolean activeR2)
    {
        double par = parameters.get(Model.Properties.SCENARIO_PARAMETER_K);
        double s;
        double e;
        if (activeR1)
        { //If we depend on active R1, the activity level is the value of the parameter
            e = r1Level;
        }
        else
        { //If we depend on inactive R1, we must find the inactive part based on the total amount (n. of levels)
            e = nLevelsR1 - r1Level;
        }
        if (activeR2)
        { //Same reasoning here
            s = r2Level;
        }
        else
        {
            s = nLevelsR2 - r2Level;
        }
        return par * e * s;
    }

    @Override
    public String[] getReactantNames()
    {
        return new String[] { "E", "S" };
    }

    @Override
    public String[] listVariableParameters()
    {
        return new String[] { Model.Properties.SCENARIO_PARAMETER_K };
    }

    @Override
    public String toString()
    {
        return "Scenario 2: k * [E] * [S]";
    }
}
