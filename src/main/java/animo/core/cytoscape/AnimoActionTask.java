package animo.core.cytoscape;

import java.awt.event.ActionEvent;

import org.cytoscape.application.swing.AbstractCyAction;

public class AnimoActionTask extends AbstractCyAction
{

    private static final long serialVersionUID = 7601367319473988438L;

    public static String timeDifferenceFormat(long diffInSeconds)
    {
        long[] diff = new long[] { 0, 0, 0, 0 };
        /* sec */diff[3] = diffInSeconds >= 60 ? diffInSeconds % 60 : diffInSeconds;
        long diffInMinutes = diffInSeconds / 60;
        /* min */diff[2] = diffInMinutes >= 60 ? diffInMinutes % 60 : diffInMinutes;
        long diffInHours = diffInMinutes / 60;
        /* hours */diff[1] = diffInHours >= 24 ? diffInHours % 24 : diffInHours;
        long diffInDays = diffInHours / 24;
        /* days */diff[0] = diffInDays;

        return String.format("%d day%s, %d hour%s, %d minute%s, %d second%s", diff[0], diff[0] != 1 ? "s" : "", diff[1], diff[1] != 1 ? "s" : "", diff[2],
                diff[2] != 1 ? "s" : "", diff[3], diff[3] != 1 ? "s" : "");
    }

    public static String timeDifferenceFormat(long startTime, long endTime)
    {
        long diffInSeconds = (endTime - startTime) / 1000;
        return timeDifferenceFormat(diffInSeconds);
    }


    protected boolean needToStop = false; //Whether the user has pressed the Cancel button on the TaskMonitor while we were running an analysis process

    public AnimoActionTask(String init)
    {
        super(init);
    }

    @Override
    public void actionPerformed(ActionEvent arg0)
    {

    }

    public boolean needToStop()
    {
        return this.needToStop;
    }


}
