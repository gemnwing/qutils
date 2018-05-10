package net.njcp.service.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class QLog {

	private static File debugFile = new File("debug");
	private static File timerFile = new File("timer");
	private static final String BRACE_L = "[";
	private static final String BRACE_R = "]";
	private static final String SPACE = " ";
	private static final String EMPTY = "";
	private static final String NEW_LINE = "\n";
	// private static final String NEW_LINE = "\n";
	// private static final String DEBUG_SYMBOL = BRACE_L + "DEBUG" + BRACE_R;
	// private static final String TRACE_SYMBOL = BRACE_L + "TRACE" + BRACE_R;
	// private static final String INFO_SYMBOL = BRACE_L + "INFO" + BRACE_R;
	// private static final String WARN_SYMBOL = BRACE_L + "WARN" + BRACE_R;
	// private static final String ERROR_SYMBOL = BRACE_L + "ERROR" + BRACE_R;
	// private static final String TIMER_SYMBOL = BRACE_L + "TIMER" + BRACE_R;
	// private static final String PARAM_SYMBOL = BRACE_L + "PARAM" + BRACE_R;
	// private static final String NOT_LOG_SYMBOL = BRACE_L + "NOTLOG" + BRACE_R;

	protected static enum Level {
		PRINT, TRACE, DEBUG, INFO, WARN, ERROR, FATAL
	};

	private static ThreadLocal<String> customPrefix = new ThreadLocal<String>();
	private static ThreadLocal<Boolean> debugFlag = new ThreadLocal<Boolean>();
	private static ThreadLocal<Boolean> timerFlag = new ThreadLocal<Boolean>();
	private static ThreadLocal<Boolean> bufferFlag = new ThreadLocal<Boolean>();
	private static ThreadLocal<Boolean> lastLogEndsWithoutANewline = new ThreadLocal<Boolean>();
	private static ThreadLocal<List<String>> logBuffer = new ThreadLocal<List<String>>();
	private static ThreadLocal<List<String>> warnNErrorBuffer = new ThreadLocal<List<String>>();
	private static ThreadLocal<Log> log = new ThreadLocal<Log>();

	private static HashMap<String, Log> logMap = new HashMap<String, Log>();
	private static HashSet<String> dbgKeywords = new HashSet<String>();
	private static Long lastModifiedTime = 0L;

	static {
		checkDebugFlag();
	}

	private static String genFakePrefix(Level level, StackTraceElement caller) {
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Timestamp(System.currentTimeMillis())) + " " + BRACE_L + getSimpleClassName(caller) + BRACE_R + "-"
				+ BRACE_L + level.name() + BRACE_R + SPACE;
	}

	public static Log getLog(StackTraceElement caller) {
		if ( getLog() == null ) {
			String logName = getSimpleClassName(caller);
			Log log = logMap.get(logName);
			if ( log == null ) {
				synchronized ( logMap ) {
					log = logMap.get(logName);
					if ( log == null ) {
						log = LogFactory.getLog(logName);
						logMap.put(logName, log);
					}
				}
			}
			return log;
		} else {
			return getLog();
		}
	}

	public static Log getLog() {
		return log.get();
	}

	public static void setLog(Log log) {
		QLog.log.set(log);
	}

	public static void setLog(Class<?> clazz) {
		String logName = clazz.getSimpleName();
		setLog(logName);
	}

	public static void setLog(String logName) {
		Log log = LogFactory.getLog(logName);
		QLog.log.set(log);
		logMap.put(logName, log);
	}

	private static String getThreadIcon() {
		return BRACE_L + Thread.currentThread()
				// .getId()
				.getName()
		// in case formatter combine this line
				+ BRACE_R;
	}

	public static String getSimpleClassName(StackTraceElement ste) {
		return ste.getClassName().replaceAll(".*\\.", "");
	}

	public static String getCustomPrefix() {
		if ( customPrefix.get() == null ) {
			return EMPTY;
		} else {
			return customPrefix.get();
		}
	}

	public static void setCustomPrefix(Object prefix) {
		customPrefix.set(prefixFormat(prefix));
		checkDebugFlag();
	}

	public static File getDebugFile() {
		return debugFile;
	}

	public static void setDebugFile(File debugFile) {
		QLog.debugFile = debugFile;
		checkDebugFlag();
	}

	public static File getTimerFile() {
		return timerFile;
	}

	public static void setTimerFile(File timerFile) {
		QLog.timerFile = timerFile;
		checkDebugFlag();
	}

	public static boolean getDebugFlag() {
		if ( debugFlag.get() == null ) {
			checkDebugFlag();
		}
		return debugFlag.get();
	}

	public static void setDebugFlag(boolean debugFlag) {
		QLog.debugFlag.set(debugFlag);
	}

	private static String prefixFormat(Object prefix) {
		if ( prefix != null ) {
			prefix = prefix.toString().replaceAll("^[\\[]", EMPTY);
			prefix = prefix.toString().replaceAll("[\\] ]*$", EMPTY);
		}
		if ( prefix != null && !prefix.toString().trim().isEmpty() ) {
			return prefix.toString();
		} else {
			return EMPTY;
		}
	}

	public static void checkDebugFlag() {
		setDebugFlag(false);
		setTimerFlag(false);
		if ( debugFile.isFile() ) {
			if ( lastModifiedTime != debugFile.lastModified() ) {
				synchronized ( debugFile ) {
					if ( lastModifiedTime != debugFile.lastModified() ) {
						// if ( lastModifiedTime == 0 ) {
						// info("File \"debug\" detected, debug mode on.");
						// } else {
						// info("File \"debug\" modified.");
						// }
						try {
							FileReader fr = new FileReader(debugFile);
							BufferedReader br = new BufferedReader(fr);
							dbgKeywords.clear();
							String line = null;
							while ( (line = br.readLine()) != null ) {
								if ( !line.matches("^[ \t]*$") ) {
									dbgKeywords.add(line);
									// info("Adding \EMPTY + line + "\" to debug keywords set.");
								}
							}
							br.close();
						} catch ( Exception e ) {
							error(I18N.tr("Processing debug keywords failed."), e);
						}
						lastModifiedTime = debugFile.lastModified();
					}
				}
			}
			if ( dbgKeywords.isEmpty() || getCustomPrefix().isEmpty() ) {
				setDebugFlag(true);
			} else {
				for ( String keyword : dbgKeywords ) {
					if ( getCustomPrefix().toLowerCase().contains(keyword.toLowerCase()) ) {
						setDebugFlag(true);
						break;
					}
				}
			}
		} else {
			if ( lastModifiedTime != 0 ) {
				// info("File \"debug\" removed, debug mode off.");
				lastModifiedTime = 0L;
			}
			setDebugFlag(false);
			if ( timerFile.isFile() ) {
				setTimerFlag(true);
			}
		}
	}

	private static String appendExceptionCause(Object logStr, Throwable e) {
		String retStr = null;
		if ( e == null ) {
			retStr = EMPTY + logStr;
		} else {
			retStr = (EMPTY + logStr).replaceAll("[\\.,:;。，：；][ ]*$", EMPTY);
			String exception = e.toString().replaceAll("[\\.,:;。，：；][ ]*$", EMPTY);
			retStr = retStr + ", " + exception + ((e.getCause() == null) ? "." : I18N.tr(", caused by: ") + e.getCause());
		}
		return retStr;
	}

	/******************** PRINT ********************/
	public static String print(Object logStr) {
		return logHandler(Level.PRINT, logStr, null, null);
	}

	public static String printf(String format, Object... args) {
		String logStr = String.format(format, args);
		return logHandler(Level.PRINT, logStr, null, null);
	}

	public static String printf(Locale locale, String format, Object... args) {
		String logStr = String.format(locale, format, args);
		return logHandler(Level.PRINT, logStr, null, null);
	}

	public static String println(Object logStr) {
		return logHandler(Level.PRINT, logStr + NEW_LINE, null, null);
	}

	public static void println() {
		logHandler(Level.PRINT, NEW_LINE, null, null);
	}

	/******************** TRACE ********************/
	public static String trace(Object logStr) {
		return logHandler(Level.TRACE, logStr, null, null);
	}

	/******************** DEBUG ********************/
	@Deprecated
	public static String debug(Object logStr, Throwable... e) {
		Throwable e1 = e.length == 0 ? null : e[0];
		return logHandler(Level.DEBUG, logStr, e1, null);
	}

	public static String debug() {
		String logStr = I18N.tr("Debug mark sets here.");
		return logHandler(Level.DEBUG, logStr, null, null);
	}

	public static String debug(Object logStr) {
		return logHandler(Level.DEBUG, logStr, null, null);
	}

	public static String debug(Object logStr, Throwable e) {
		return logHandler(Level.DEBUG, logStr, e, null);
	}

	/******************** INFO ********************/
	@Deprecated
	public static String info(Object logStr, Throwable... e) {
		Throwable e1 = e.length == 0 ? null : e[0];
		return logHandler(Level.INFO, logStr, e1, null);
	}

	public static String info(Object logStr) {
		return logHandler(Level.INFO, logStr, null, null);
	}

	public static String info(Object logStr, Throwable e) {
		return logHandler(Level.INFO, logStr, e, null);
	}

	/******************** WARN ********************/
	@Deprecated
	public static String warn(Object logStr, Throwable... e) {
		Throwable e1 = e.length == 0 ? null : e[0];
		return logHandler(Level.WARN, logStr, e1, null);
	}

	public static String warn(Object logStr) {
		return logHandler(Level.WARN, logStr, null, null);
	}

	public static String warn(Object logStr, Throwable e) {
		return logHandler(Level.WARN, logStr, e, null);
	}

	/******************** ERROR ********************/
	@Deprecated
	public static String error(Object logStr, Throwable... e) {
		Throwable e1 = e.length == 0 ? null : e[0];
		return logHandler(Level.ERROR, logStr, e1, null);
	}

	public static String error(Object logStr) {
		return logHandler(Level.ERROR, logStr, null, null);
	}

	public static String error(Object logStr, Throwable e) {
		return logHandler(Level.ERROR, logStr, e, null);
	}

	/******************** FATAL ********************/
	public static String fatal(Object logStr) {
		return logHandler(Level.FATAL, logStr, null, null);
	}

	public static String fatal(Object logStr, Throwable e) {
		return logHandler(Level.FATAL, logStr, e, null);
	}

	/******************** HANDLER ********************/
	private static String logHandler(Level level, Object rowStr, Throwable e, StackTraceElement caller) {
		String logStr = appendExceptionCause(rowStr, e);
		if ( getBufferFlag() && !rowStr.equals(getLogBuffer()) ) {
			if ( level == Level.PRINT ) {
				String toStuffIn = logStr.replaceFirst("\n$", "");	// replaceAll will replace all the "\n" at the end, no f**king idea why...
				if ( isLastLogEndstWithoutANewline() && getLogBuffer().size() > 0 ) {
					toStuffIn = getLogBuffer().get(getLogBuffer().size() - 1) + toStuffIn;
					getLogBuffer().set(getLogBuffer().size() - 1, toStuffIn);
				} else {
					getLogBuffer().add(toStuffIn);
				}
				setLastLogEndsWithoutNewline(!logStr.endsWith("\n"));
			} else {
				getLogBuffer().add(logStr);
				if ( level.ordinal() >= Level.WARN.ordinal() ) {
					getWarnNErrorBuffer().add(logStr);
				}
			}
		}
		if ( level == Level.PRINT ) {
			System.out.print(logStr);
			return logStr;
		}
		if ( caller == null ) {
			caller = Thread.currentThread().getStackTrace()[3];
		}
		if ( getDebugFlag() ) {
			return debugLogHandler(level, logStr, e, caller);
		}
		logStr = (getCustomPrefix().isEmpty() ? EMPTY : BRACE_L + getCustomPrefix() + BRACE_R + SPACE) + logStr;
		Log log = getLog(caller);
		if ( log != null ) {
			switch ( level ) {
			case TRACE:
				log.trace(logStr);
				break;
			case DEBUG:
				log.debug(logStr);
				break;
			case INFO:
				log.info(logStr);
				break;
			case WARN:
				log.warn(logStr);
				break;
			case ERROR:
				log.error(logStr);
				break;
			case FATAL:
				log.fatal(logStr);
				break;
			default:
				break;
			}
		} else {
			System.out.println(genFakePrefix(level, caller) + logStr);
			if ( e != null ) {
				e.printStackTrace();
			}
		}
		return logStr;
	}

	private static String debugLogHandler(Level level, String logStr, Throwable e, StackTraceElement caller) {
		String symbol = null;
		if ( level == Level.TRACE ) {
			symbol = BRACE_L + Level.TRACE.name() + BRACE_R;
		} else {
			symbol = BRACE_L + Level.DEBUG.name() + BRACE_R;
		}
		logStr = (getCustomPrefix().isEmpty() ? EMPTY : BRACE_L + getCustomPrefix() + BRACE_R) + getThreadIcon() + symbol.replace("]", " ") + getSimpleClassName(caller) + "." + caller.getMethodName()
				+ "(" + caller.getFileName() + ":"
				+ caller.getLineNumber() + ")" + BRACE_R + SPACE + logStr;
		Log log = getLog(caller);
		if ( log != null && level != Level.TRACE ) {
			switch ( level ) {
			case WARN:
				if ( e != null ) {
					log.warn(logStr, e);
				} else {
					log.warn(logStr);
				}
				break;
			case ERROR:
				if ( e != null ) {
					log.error(logStr, e);
				} else {
					log.error(logStr);
				}
				break;
			case FATAL:
				if ( e != null ) {
					log.fatal(logStr, e);
				} else {
					log.fatal(logStr);
				}
				break;
			default:
				if ( e != null ) {
					log.info(logStr, e);
				} else {
					log.info(logStr);
				}
				break;
			}
		} else {
			System.out.println(genFakePrefix(level, caller) + logStr);
			if ( e != null ) {
				e.printStackTrace();
			}
		}
		return logStr;
	}

	/******************** INNER CALLER ********************/

	protected static void timer(Level level, Object logStr, StackTraceElement caller) {
		if ( level == Level.DEBUG ) {
			if ( getTimerFlag() || getDebugFlag() ) {
				logHandler(Level.INFO, logStr, null, caller);
			}
		} else {
			logHandler(level, logStr, null, caller);
		}
	}

	private static void setTimerFlag(boolean timerFlag) {
		QLog.timerFlag.set(timerFlag);
	}

	private static boolean getTimerFlag() {
		if ( timerFlag.get() == null ) {
			checkDebugFlag();
		}
		return timerFlag.get();
	}

	protected static void innerCall(Level level, Object logStr, Throwable e, StackTraceElement caller) {
		logHandler(Level.DEBUG, logStr, e, caller);
	}

	public static void dbUtil(Level level, String logStr, Throwable e) {
		StackTraceElement caller = Thread.currentThread().getStackTrace()[3];
		logHandler(level, logStr, e, caller);
	}

	/******************** LOG BUFFER ********************/
	public static Boolean getBufferFlag() {
		if ( bufferFlag.get() == null ) {
			return false;
		}
		return bufferFlag.get();
	}

	public static void setBufferFlag(boolean bufferFlag) {
		QLog.bufferFlag.set(bufferFlag);
		clearLogBuffer();
	}

	public static void clearLogBuffer() {
		setLogBuffer(null);
		setWarnNErrorBuffer(null);
	}

	public static List<String> getLogsFromBuffer() {
		List<String> retList = new ArrayList<String>();
		retList.addAll(getLogBuffer());
		setLogBuffer(null);
		return retList;
	}

	public static List<String> getWarnNErrorsFromBuffer() {
		List<String> retList = new ArrayList<String>();
		retList.addAll(getWarnNErrorBuffer());
		setWarnNErrorBuffer(null);
		return retList;
	}

	private static List<String> getLogBuffer() {
		if ( logBuffer.get() == null ) {
			setLogBuffer(new ArrayList<String>());
		}
		return logBuffer.get();
	}

	private static void setLogBuffer(List<String> logBuffer) {
		QLog.logBuffer.set(logBuffer);
	}

	private static List<String> getWarnNErrorBuffer() {
		if ( warnNErrorBuffer.get() == null ) {
			warnNErrorBuffer.set(new ArrayList<String>());
		}
		return warnNErrorBuffer.get();
	}

	private static void setWarnNErrorBuffer(List<String> warnNErrorBuffer) {
		QLog.warnNErrorBuffer.set(warnNErrorBuffer);
	}

	private static Boolean isLastLogEndstWithoutANewline() {
		if ( lastLogEndsWithoutANewline.get() == null ) {
			return false;
		}
		return lastLogEndsWithoutANewline.get();
	}

	private static void setLastLogEndsWithoutNewline(boolean lastLogEndsWithoutANewline) {
		QLog.lastLogEndsWithoutANewline.set(lastLogEndsWithoutANewline);
		;
	}

}
