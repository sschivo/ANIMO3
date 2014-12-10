package animo.cytoscape;

import java.awt.event.ActionEvent;

import org.cytoscape.application.swing.AbstractCyAction;

public class AnimoActionTask extends AbstractCyAction {

	private static final long serialVersionUID = 7601367319473988438L;

	public static String timeDifferenceFormat(long diffInSeconds) {
		long[] diff = new long[] { 0, 0, 0, 0 };
		/* sec */diff[3] = diffInSeconds >= 60 ? diffInSeconds % 60 : diffInSeconds;
		long diffInMinutes = diffInSeconds / 60;
		/* min */diff[2] = diffInMinutes >= 60 ? diffInMinutes % 60 : diffInMinutes;
		long diffInHours = diffInMinutes / 60;
		/* hours */diff[1] = diffInHours >= 24 ? diffInHours % 24 : diffInHours;
		long diffInDays = diffInHours / 24;
		/* days */diff[0] = diffInDays;

		return String.format("%d day%s, %d hour%s, %d minute%s, %d second%s", diff[0], diff[0] != 1 ? "s" : "",
				diff[1], diff[1] != 1 ? "s" : "", diff[2], diff[2] != 1 ? "s" : "", diff[3], diff[3] != 1 ? "s" : "");
	}
	
	public static String timeDifferenceShortFormat(long diffInSeconds) {
		long[] diff = new long[] { 0, 0, 0, 0 };
		/* sec */diff[3] = diffInSeconds >= 60 ? diffInSeconds % 60 : diffInSeconds;
		long diffInMinutes = diffInSeconds / 60;
		/* min */diff[2] = diffInMinutes >= 60 ? diffInMinutes % 60 : diffInMinutes;
		long diffInHours = diffInMinutes / 60;
		/* hours */diff[1] = diffInHours >= 24 ? diffInHours % 24 : diffInHours;
		long diffInDays = diffInHours / 24;
		/* days */diff[0] = diffInDays;
		String labels[] = new String[] { "day", "hour", "minute", "second" }; //It's simple in this case: we just can add an "s" to get the plural (otherwise we should have another array with the plurals)
		
		int startIndex = 0;
		for (int i = 0; i < diff.length; i++) { //Skip the "0 days, 0 hours, ..." until the first != 0
			if (diff[i] > 0) {
				startIndex = i;
				break;
			}
		}
		StringBuilder builder = new StringBuilder();
		for (int i = startIndex; i < diff.length - 1; i++) {
			builder.append(diff[i] + " " + labels[i]);
			if (diff[i] != 1) {
				builder.append("s");
			}
			builder.append(", ");
		}
		builder.append(diff[diff.length - 1] + " " + labels[labels.length - 1]);
		if (diff[diff.length - 1] != 1) {
			builder.append("s");
		}
		
		return builder.toString();
	}

	public static String timeDifferenceFormat(long startTime, long endTime) {
		long diffInSeconds = (endTime - startTime) / 1000;
		return timeDifferenceFormat(diffInSeconds);
	}
	
	public static String timeDifferenceShortFormat(long startTime, long endTime) {
		long diffInSeconds = (endTime - startTime) / 1000;
		return timeDifferenceShortFormat(diffInSeconds);
	}

	protected boolean needToStop = false; // Whether the user has pressed the Cancel button on the TaskMonitor while we were running an analysis process

	public AnimoActionTask(String init) {
		super(init);
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {

	}

	public boolean needToStop() {
		return this.needToStop;
	}

}
