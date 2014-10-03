package animo.fitting;

import java.util.HashMap;

public class ScenarioFitting
{
    private ScenarioCfg cfg = null;
    private HashMap<String, ParameterSetting> parameterSettings = null;

    public ScenarioFitting(ScenarioCfg cfg)
    {
        parameterSettings = new HashMap<String, ParameterSetting>();
    }

    public ScenarioFitting(ScenarioCfg cfg, HashMap<String, ParameterSetting> pararmeterSettings)
    {
        this.cfg = cfg;
        this.parameterSettings = pararmeterSettings;
    }

    public ParameterSetting getParameterSetting(String parName)
    {
        return parameterSettings.get(parName);
    }

    public HashMap<String, ParameterSetting> getParameterSettings()
    {
        return parameterSettings;
    }

    public ScenarioCfg getScenarioCfg()
    {
        return this.cfg;
    }

    public void setParameterSetting(String parName, ParameterSetting setting)
    {
        parameterSettings.put(parName, setting);
    }

    public void setScenarioCfg(ScenarioCfg cfg)
    {
        this.cfg = cfg;
    }
}
