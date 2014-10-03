package animo.fitting;

import java.util.HashMap;

import animo.core.analyser.LevelResult;

public class AcceptableConfiguration implements Comparable<AcceptableConfiguration>
{
    private HashMap<String, ScenarioCfg> scenarioConfigurations = null;
    private LevelResult result = null;
    private String errorEstimation = null;
    private double errorValue = Double.NaN;

    public AcceptableConfiguration(HashMap<String, ScenarioCfg> scenarioConfigurations, LevelResult result, String errorEstimation)
    {
        this.scenarioConfigurations = scenarioConfigurations;
        this.result = result;
        this.errorEstimation = errorEstimation;
    }

    @Override
    public int compareTo(AcceptableConfiguration other)
    {
        if (this.errorValue < other.errorValue)
        {
            return -1;
        }
        else if (this.errorValue == other.errorValue)
        {
            return 0;
        }
        else
            return 1;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;

        AcceptableConfiguration ac = (AcceptableConfiguration) obj;
        return Double.valueOf(errorValue).equals(ac.errorValue);
    }

    public String getErrorEstimation()
    {
        return this.errorEstimation;
    }

    public double getErrorValue()
    {
        return this.errorValue;
    }

    public LevelResult getResult()
    {
        return this.result;
    }

    public HashMap<String, ScenarioCfg> getScenarioConfigurations()
    {
        return this.scenarioConfigurations;
    }

    @Override
    public int hashCode()
    {
        return Double.valueOf(errorValue).hashCode();
    }

    public void setErrorEstimation(String errorEstimation)
    {
        this.errorEstimation = errorEstimation;
    }

    public void setErrorValue(double errorValue)
    {
        this.errorValue = errorValue;
    }

    public void setResult(LevelResult result)
    {
        this.result = result;
    }

    public void setScenarioConfigurations(HashMap<String, ScenarioCfg> scenarioConfigurations)
    {
        this.scenarioConfigurations = scenarioConfigurations;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        for (String r : scenarioConfigurations.keySet())
        {
            builder.append(r + " uses ");
            ScenarioCfg cfg = scenarioConfigurations.get(r);
            for (String parName : cfg.getParameters().keySet())
            {
                builder.append(parName + "=" + cfg.getParameters().get(parName) + ", ");
            }
            builder.append(System.getProperty("line.separator"));
        }
        builder.append(". Error estimation: " + errorEstimation);
        return builder.toString();
    }
}
