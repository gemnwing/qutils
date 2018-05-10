package net.njcp.service.util;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import net.njcp.service.util.QLog.Level;

public class QTimer {
	private static ThreadLocal<HashMap<String, Long>> markMap = new ThreadLocal<HashMap<String, Long>>();
	private static final String LAST_OPERATION = I18N.tr("Operation since last timer mark");
	private static ThreadLocal<Long> threadLocalAlarmThreshold = new ThreadLocal<Long>();
	private static long globalAlarmThreshold = -1;

	private static HashMap<String, Long> getMarkMap() {
		if ( markMap.get() == null ) {
			markMap.set(new HashMap<String, Long>());
		}
		return markMap.get();
	}

	public static void setTimeMark() {
		getMarkMap().put(LAST_OPERATION, System.currentTimeMillis());
		QLog.timer(Level.DEBUG, I18N.tr("New timer mark sets here."), Thread.currentThread().getStackTrace()[2]);
	}

	public static void setTimeMark(String mark) {
		getMarkMap().put(mark, System.currentTimeMillis());
		QLog.timer(Level.DEBUG, I18N.tr("{0} begins.", mark), Thread.currentThread().getStackTrace()[2]);
	}

	public static Long getTimeElapsedInMillis() {
		return getTimeElapsedInMillis(LAST_OPERATION);
	}

	public static Long getTimeElapsedInMillis(String mark) {
		Long tiemElapsed = 0L;
		if ( !getMarkMap().isEmpty() ) {
			Long lastTimeMark = getMarkMap().get(mark);
			if ( lastTimeMark == null ) {
				lastTimeMark = (Long) getMarkMap().values().toArray()[getMarkMap().size() - 1];
			}
			tiemElapsed = System.currentTimeMillis() - lastTimeMark;
		}
		return tiemElapsed;
	}

	public static String getTimeElapsed() {
		return getTimeElapsed(LAST_OPERATION);
	}

	public static String getTimeElapsed(String mark) {
		Long timeElapsed = getTimeElapsedInMillis(mark);
		return format(timeElapsed);
	}

	public static String format(long timeElapsed) {
		return I18N.tr("{0} ms", new DecimalFormat("#,###").format(timeElapsed));
	}

	public static void showTimeElapsed() {
		genATimerLog(LAST_OPERATION, getAlarmThreshold());
	}

	public static void showTimeElapsed(String mark) {
		genATimerLog(mark, getAlarmThreshold());
	}

	public static void showTimeElapsed(String mark, long alarmThresholdInMillis) {
		genATimerLog(mark, alarmThresholdInMillis);
	}

	private static void genATimerLog(String mark, long threshold) {
		StackTraceElement caller = Thread.currentThread().getStackTrace()[3];
		Level level = Level.DEBUG;
		long timeElapsed = getTimeElapsedInMillis(mark);
		String log = I18N.tr("{0} takes {1}", mark, format(timeElapsed));
		if ( threshold > 0 && timeElapsed >= threshold ) {
			level = Level.WARN;
			log += I18N.tr(",over the upper-limit({0})", format(threshold));
		}
		log += ".";
		QLog.timer(level, log, caller);
	}

	private static long getThreadLocalAlarmThreshold() {
		if ( threadLocalAlarmThreshold.get() == null ) {
			threadLocalAlarmThreshold.set(-1L);
		}
		return threadLocalAlarmThreshold.get();
	}

	private static long getAlarmThreshold() {
		return getThreadLocalAlarmThreshold() == -1 ? getGlobalAlarmThreshold() : getThreadLocalAlarmThreshold();
	}

	public static void setThreadLocalAlarmThreshold(long alarmThresholdInMillis) {
		QTimer.threadLocalAlarmThreshold.set(alarmThresholdInMillis);
	}

	private static long getGlobalAlarmThreshold() {
		return globalAlarmThreshold;
	}

	public static void setGlobalAlarmThreshold(long alarmThresholdInMillis) {
		QTimer.globalAlarmThreshold = alarmThresholdInMillis;
	}

	/**
	 * Description will be loaded as (name, action)
	 * @param time
	 * @param unit
	 * @param descs
	 */
	public static void countdown(int time, TimeUnit unit, Object... descs) {
		StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
		Level level = Level.INFO;
		String name = "";
		String action = "";
		if ( descs.length > 0 ) {
			name = descs[0] == null ? "" : descs[0].toString().toLowerCase();
			if ( descs.length > 1 ) {
				action = descs[1] == null ? "" : descs[1].toString().toLowerCase();
			}
		}
		String actionDesc = (name.isEmpty() ? "" : name + " ") + action;
		QLog.timer(level, "This is " + (actionDesc.isEmpty() ? "" : actionDesc) + "launch control, " + (actionDesc.isEmpty() ? "launching " : actionDesc + " ") + "starts in " + time + " "
				+ unit.name().toLowerCase() + ".", caller);
		try {
			long millis = unit.toMillis(time);
			while ( millis > 0 ) {
				long now = System.currentTimeMillis();
				if ( millis % (5 * 60 * 1000) == 0 || (millis <= 5 * 60 * 1000 && millis % (60 * 1000) == 0) ) {
					QLog.timer(level, "T minus " + millis / (60 * 1000) + " minutes and counting.", caller);
				} else if ( millis < 60 * 1000 && millis % 10000 == 0 && millis != 10000 ) {
					QLog.timer(level, "T minus " + millis / 1000 + " seconds and counting.", caller);
				} else if ( millis <= 10 * 1000 ) {
					if ( millis == 10 * 1000 ) {
						QLog.timer(level, "T minus 10 seconds and counting.", caller);
					} else {
						QLog.timer(level, millis / 1000, caller);
					}
				}
				long elapsed = System.currentTimeMillis() - now;
				long sleepTime = elapsed >= 1000 ? 0 : (1000 - elapsed);
				Thread.sleep(sleepTime);
				millis -= 1000;
			}
		} catch ( InterruptedException e ) {
			QLog.timer(level, QStringUtil.titleCase(name) + " " + action + time + " " + unit.name().toLowerCase() + " process interrupted", caller);
		}
	}

	public static void main(String[] args) {
		countdown(21, TimeUnit.SECONDS, "Load forecast service");
		// QLog.println(QStringUtil.repeat("*", 10));
	}
}
