package net.njcp.service.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.njcp.service.util.I18N;

public class QLog {

	private static final String BRACE_L = "[";
	private static final String BRACE_R = "]";
	private static final String SPACE = " ";
	private static final String EMPTY = "";
	private static final String NEW_LINE = "\n";

	private enum LogType {
		UNKNOWN, LOG4J, LOG4J2, LOG4J2_JCL
	}

	private static LogType logType = LogType.UNKNOWN;
	private static File debugFile;
	private static File timerFile;

	private static ThreadLocal<String> customPrefixTl = new ThreadLocal<String>();
	private static ThreadLocal<Boolean> debugFlagTl = new ThreadLocal<Boolean>();
	private static ThreadLocal<Boolean> timerFlagTl = new ThreadLocal<Boolean>();
	private static ThreadLocal<Boolean> bufferFlagTl = new ThreadLocal<Boolean>();
	private static ThreadLocal<Boolean> lastLogEndsWithoutANewlineTl = new ThreadLocal<Boolean>();
	private static ThreadLocal<List<String>> logBufferTl = new ThreadLocal<List<String>>();
	private static ThreadLocal<List<String>> warnNErrorBufferTl = new ThreadLocal<List<String>>();
	private static ThreadLocal<LogProfile> logProfileTl = new ThreadLocal<LogProfile>();
	private static ThreadLocal<String> logNameTl = new ThreadLocal<String>();

	private static boolean usingSimpleClassName = true;
	private static boolean saveDebugAsInfo = false;
	private static boolean saveStackTraceInLog = false;
	private static boolean saveDebugLogExclusively = false;
	private static PrintStream debugOut;
	private static Object rootLogger;
	// private static String DEFAULT_PATTERN = "%-d{yyyy-MM-dd HH:mm:ss,SSS} [%c]-[%p] %m%n";

	private static ConcurrentHashMap<String, LogProfile> logMap = new ConcurrentHashMap<String, LogProfile>();
	private static HashSet<String> dbgKeywords = new HashSet<String>();
	private static Long lastModifiedTime = 0L;
	private static Long debugModeActivateTime;
	private static Long overrunThreshold = 24 * 60 * 60 * 1000L;

	private static Class<?> loggerClazz;
	private static Method getLoggerMethod;
	private static Method getAppendersMethod;
	private static boolean rootLoggerHasConsoleAppender;
	private static Class<?> consoleAppenderClazz;
	private static String rootLoggerFileName;
	private static Object loggerContext;

	private static class LogProfile {
		public String name;
		public Log log;
		public Boolean hasConsoleAppender;
		public Level level;

		public LogProfile(Log log, String name) {
			this.log = log;
			this.name = name;
			this.level = getLevel(log);
			this.hasConsoleAppender = hasConsoleAppender();
		}

		public static Level getLevel(Log log) {
			if ( log.isTraceEnabled() ) {
				return Level.TRACE;
			} else if ( log.isDebugEnabled() ) {
				return Level.DEBUG;
			} else if ( log.isInfoEnabled() ) {
				return Level.INFO;
			} else if ( log.isWarnEnabled() ) {
				return Level.WARN;
			} else if ( log.isErrorEnabled() ) {
				return Level.ERROR;
			} else if ( log.isFatalEnabled() ) {
				return Level.FATAL;
			}
			return Level.FATAL;
		}

		private boolean hasConsoleAppender() {
			boolean hasConsoleAppender = false;
			try {
				Object logger = null;
				try {
					if ( logType == LogType.LOG4J2_JCL ) {
						logger = getLoggerMethod.invoke(loggerContext, this.name);
					} else {
						logger = getLoggerMethod.invoke((this.log));
					}
				} catch ( Throwable t ) {
					logger = null;
				}
				if ( logger == null ) {
					logger = this.log;
				}

				switch ( getLogType() ) {
				case LOG4J:
					Enumeration<?> appenders = (Enumeration<?>) getAppendersMethod.invoke(logger);
					while ( appenders.hasMoreElements() ) {
						if ( consoleAppenderClazz.isInstance(appenders.nextElement()) ) {
							hasConsoleAppender = true;
						}
					}
					break;
				case LOG4J2:
					Map<?, ?> appenderMap = (Map<?, ?>) getAppendersMethod.invoke(logger);
					for ( Object appender : appenderMap.values() ) {
						if ( consoleAppenderClazz.isInstance(appender) ) {
							hasConsoleAppender = true;
						}
					}
					break;
				case UNKNOWN:
					break;
				default:
					break;
				}
				hasConsoleAppender |= rootLoggerHasConsoleAppender;
			} catch ( Throwable t ) {
				t.printStackTrace();
			}
			return hasConsoleAppender;
		}
	}

	public static enum Level {
		PRINT, TRACE, DEBUG, INFO, WARN, ERROR, FATAL;

		private static Map<Integer, String> nameMap = new HashMap<Integer, String>();

		public void replaceDesc(String newName) {
			nameMap.put(ordinal(), newName);
		}

		public String getDesc() {
			if ( nameMap.containsKey(ordinal()) ) {
				return nameMap.get(ordinal());
			}
			return name();
		}
	}

	static {
		Log log = LogFactory.getLog("");
		rootLoggerHasConsoleAppender = false;
		try {
			Object logger = null;
			Class<?> log4JLoggerClazz = null;
			Class<?> log4JLogClazz = null;
			try {
				log4JLoggerClazz = Class.forName("org.apache.commons.logging.impl.Log4JLogger");
			} catch ( Throwable t ) {
			}
			try {
				log4JLogClazz = Class.forName("org.apache.logging.log4j.jcl.Log4jLog");
			} catch ( Throwable t ) {
			}
			if ( log4JLoggerClazz != null && log4JLoggerClazz.isInstance(log) ) {
				getLoggerMethod = log4JLoggerClazz.getMethod("getLogger");
				logger = getLoggerMethod.invoke((log));

			} else if ( log4JLogClazz != null && log4JLogClazz.isInstance(log) ) {
				setLogType(LogType.LOG4J2_JCL);
				Class<?> loggerContextClazz = null;
				try {
					loggerContextClazz = Class.forName("org.apache.logging.log4j.core.LoggerContext");
				} catch ( Throwable t ) {
				}
				if ( loggerContextClazz != null ) {
					Method m = loggerContextClazz.getMethod("getContext", boolean.class);
					loggerContext = m.invoke(loggerContextClazz, false);
					getLoggerMethod = loggerContextClazz.getMethod("getLogger", String.class);
					logger = getLoggerMethod.invoke(loggerContext, "");
				}
			}

			// org.apache.logging.log4j.jcl.Log4jLog
			if ( logger == null ) {
				logger = log;
			}

			Class<?> log4j1loggerClazz = null;
			Class<?> log4j2loggerClazz = null;
			try {
				log4j1loggerClazz = Class.forName("org.apache.log4j.Logger");
			} catch ( Throwable t ) {
			}
			try {
				log4j2loggerClazz = Class.forName("org.apache.logging.log4j.core.Logger");
			} catch ( Throwable t ) {
			}
			Class<?> fileAppenderClazz;
			if ( log4j1loggerClazz != null && log4j1loggerClazz.isInstance(logger) ) {
				loggerClazz = log4j1loggerClazz;
				setLogType(LogType.LOG4J);
				getAppendersMethod = log4j1loggerClazz.getMethod("getAllAppenders");
				Enumeration<?> appenders = (Enumeration<?>) getAppendersMethod.invoke(getRootLogger());
				consoleAppenderClazz = Class.forName("org.apache.log4j.ConsoleAppender");
				fileAppenderClazz = Class.forName("org.apache.log4j.FileAppender");
				while ( appenders.hasMoreElements() ) {
					Object appender = appenders.nextElement();
					if ( fileAppenderClazz.isInstance(appender) ) {
						Method m = fileAppenderClazz.getMethod("getFile");
						rootLoggerFileName = String.valueOf(m.invoke(appender));
					} else if ( consoleAppenderClazz.isInstance(appender) ) {
						rootLoggerHasConsoleAppender = true;
					}
				}
			} else if ( log4j2loggerClazz != null && log4j2loggerClazz.isInstance(logger) ) {
				loggerClazz = log4j2loggerClazz;
				setLogType(LogType.LOG4J2);
				getAppendersMethod = log4j2loggerClazz.getMethod("getAppenders");
				consoleAppenderClazz = Class.forName("org.apache.logging.log4j.core.appender.ConsoleAppender");
				fileAppenderClazz = Class.forName("org.apache.logging.log4j.core.appender.FileAppender");
				Class<?> rollingFileAppenderClazz = Class.forName("org.apache.logging.log4j.core.appender.RollingFileAppender");
				Map<?, ?> appenderMap = (Map<?, ?>) getAppendersMethod.invoke(getRootLogger());
				for ( Object appender : appenderMap.values() ) {
					if ( fileAppenderClazz.isInstance(appender) ) {
						Method m = fileAppenderClazz.getMethod("getFileName");
						rootLoggerFileName = String.valueOf(m.invoke(appender));
					} else if ( rollingFileAppenderClazz.isInstance(appender) ) {
						Method m = rollingFileAppenderClazz.getMethod("getFileName");
						rootLoggerFileName = String.valueOf(m.invoke(appender));
					} else if ( consoleAppenderClazz.isInstance(appender) ) {
						rootLoggerHasConsoleAppender = true;
					}
				}
			} else {
				rootLoggerHasConsoleAppender = true;
			}

		} catch ( Throwable t ) {
			t.printStackTrace();
		}

		setDebugFile("debug");
	}

	private static String genFakePrefix(Level level, StackTraceElement caller) {
		return new StringBuilder().append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Timestamp(System.currentTimeMillis()))).append(" ").append(BRACE_L)
				.append((getLogName() == null ? getClassName(caller) : getLogName())).append(BRACE_R).append("-").append(BRACE_L).append(level.name()).append(BRACE_R).append(SPACE).toString();
	}

	public static LogProfile getLogProfile() {
		return logProfileTl.get();
	}

	public static LogProfile getLogProfile(StackTraceElement caller) {
		String logName = getClassName(caller);
		if ( !logMap.containsKey(logName) ) {
			logMap.put(logName, new LogProfile(LogFactory.getLog(logName), logName));
		}
		return logMap.get(logName);
	}

	public static void setLogProfile(LogProfile logProfile) {
		QLog.logProfileTl.set(logProfile);
	}

	public static Log getLog(StackTraceElement caller) {
		if ( getLog() == null ) {
			return getLogProfile(caller).log;
		} else {
			return getLog();
		}
	}

	public static Log getLog() {
		if ( logProfileTl.get() == null ) {
			return null;
		}
		return logProfileTl.get().log;
	}

	// public static void setLog(Log log) {
	// logProfile.set(new LogProfile(log));
	// }

	public static void setLog(Class<?> clazz) {
		String logName = getClassName(clazz);
		setLog(logName);
	}

	public static void setLog(String logName) {
		setLogName(logName);
		Log log = LogFactory.getLog(logName);
		LogProfile logProfile = new LogProfile(log, logName);
		setLogProfile(logProfile);
		logMap.put(logName, logProfile);
	}

	public static String getLogName() {
		return logNameTl.get();
	}

	public static void setLogName(String logName) {
		QLog.logNameTl.set(logName);
	}

	private static String getThreadIcon() {
		// .getId()
		// in case formatter combine this line
		return new StringBuilder().append(BRACE_L).append(Thread.currentThread().getName()).append(BRACE_R).toString();
	}

	public static String getClassName(Object o) {
		String className = "";
		if ( o instanceof StackTraceElement ) {
			StackTraceElement caller = (StackTraceElement) o;
			if ( usingSimpleClassName ) {
				className = caller.getClassName().replaceAll(".*\\.", "");
			} else {
				className = caller.getClassName();
			}
		} else if ( o instanceof Class<?> ) {
			Class<?> clazz = (Class<?>) o;
			if ( usingSimpleClassName ) {
				className = clazz.getSimpleName();
			} else {
				className = clazz.getName();
			}
		}
		return className;
	}

	public static String getCustomPrefix() {
		if ( customPrefixTl.get() == null ) {
			return EMPTY;
		} else {
			return customPrefixTl.get();
		}
	}

	public static void setCustomPrefix(Object prefix) {
		customPrefixTl.set(prefixFormat(prefix));
		checkDebugFlag();
	}

	public static File getDebugFile() {
		return debugFile;
	}

	public static void setDebugFile(File file) {
		info(I18N.tr("Debug mode switch file has been set to <{0}>", file.getAbsolutePath()));
		QLog.debugFile = file;
		File path = file.getAbsoluteFile().getParentFile();
		setTimerFile(new File(path.getAbsolutePath() + "/timer"));
		checkDebugFlag();
	}

	public static void setDebugFile(String fileName) {
		setDebugFile(new File(fileName));
	}

	public static File getTimerFile() {
		return timerFile;
	}

	private static void setTimerFile(File file) {
		info(I18N.tr("Timer mode switch file has been set to <{0}>", file.getAbsolutePath()));
		QLog.timerFile = file;
	}

	public static boolean getDebugFlag() {
		if ( debugFlagTl.get() == null ) {
			checkDebugFlag();
		}
		return debugFlagTl.get();
	}

	public static void setDebugFlag(boolean debugFlag) {
		QLog.debugFlagTl.set(debugFlag);
	}

	public static LogType getLogType() {
		return logType;
	}

	public static void setLogType(LogType logType) {
		if ( QLog.logType != logType && (QLog.logType == LogType.UNKNOWN || logType == LogType.UNKNOWN) ) {
			QLog.logType = logType;
		}
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

	public static boolean checkDebugFlag() {
		setDebugFlag(false);
		setTimerFlag(false);
		if ( debugFile != null && debugFile.isFile() ) {
			if ( lastModifiedTime != debugFile.lastModified() ) {
				debugModeActivateTime = System.currentTimeMillis();
				synchronized ( lastModifiedTime ) {
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
								line = line.trim();
								if ( line.matches("^[^#].*=.*$") ) {
									String[] keyValue = QParam.parseAProp(line, false);
									String key = keyValue[0];
									String value = keyValue[1];
									if ( !key.trim().isEmpty() && value.matches("^[0-9]+$") ) {
										if ( "OverrunThreshold".equalsIgnoreCase(key) ) {
											setOverrunThreshold(Integer.valueOf(value), TimeUnit.SECONDS);
										} else {
											Method m = null;
											try {
												m = QLog.class.getMethod("set" + QStringUtil.sentenceCase(key), boolean.class);
											} catch ( Throwable t ) {
											}
											if ( m != null ) {
												m.invoke(QLog.class, Integer.valueOf(value) > 0);
											}
										}
									}
								} else if ( !line.matches("^[ \t]*$") ) {
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
			} else if ( overrunThreshold > 0 && debugModeActivateTime != null ) {
				// overrun process
				long timeOverrun = System.currentTimeMillis() - debugModeActivateTime;
				synchronized ( overrunThreshold ) {
					if ( debugModeActivateTime != null && timeOverrun >= overrunThreshold ) {
						File debugStop = new File(I18N.tr("{0}.stop", debugFile.getAbsolutePath()));
						info(I18N.tr("Debug mode overrun for {0}, about to stop.", QTimestamp.niceDisplay(timeOverrun)));
						debugFile.renameTo(debugStop);
						debugModeActivateTime = null;
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
		}
		if ( debugFile == null || !debugFile.isFile() ) {
			debugModeActivateTime = null;
			lastModifiedTime = 0L;
			setDebugFlag(false);
			if ( timerFile != null && timerFile.isFile() ) {
				setTimerFlag(true);
			}
		}
		return debugFlagTl.get();
	}

	private static synchronized void createDebugOutput() {
		if ( debugOut != null ) {
			return;
		}

		File debugOutputPath = null;
		if ( rootLoggerFileName != null ) {
			debugOutputPath = new File(rootLoggerFileName).getAbsoluteFile().getParentFile();
		} else {
			debugOutputPath = debugFile.getAbsoluteFile().getParentFile();
		}
		File debugOutputFile = new File(debugOutputPath.getAbsolutePath() + "/debug.out");
		try {
			debugOut = new PrintStream(debugOutputFile);
		} catch ( FileNotFoundException e ) {
			debugOut = null;
		}
	}

	private static String appendExceptionAndCause(Object str, Throwable e) {
		StringBuilder retSb = new StringBuilder();
		if ( e == null || getDebugFlag() || isSaveStackTraceInLog() ) {
			retSb.append(str);
		} else {
			retSb.append(String.valueOf(str).replaceAll("[\\.,:;。，：；][ ]*$", EMPTY));
			String exception = e.toString().replaceAll("[\\.,:;。，：；][ ]*$", EMPTY);
			retSb.append(", ").append(exception);
			if ( e.getCause() == null ) {
				retSb.append(".");
			} else {
				retSb.append(I18N.tr(", caused by: ")).append(e.getCause());
			}
		}
		return retSb.toString();
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
	private static String logHandler(Level level, Object rawStr, Throwable e, StackTraceElement caller) {
		String logStr = appendExceptionAndCause(rawStr, e);
		if ( getBufferFlag() && !rawStr.equals(getLogBuffer()) ) {
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
			caller = new Throwable().getStackTrace()[2];
		}
		StringBuilder logSb = new StringBuilder();
		if ( !getCustomPrefix().isEmpty() ) {
			logSb.append(BRACE_L).append(getCustomPrefix()).append(BRACE_R);
		}
		if ( getDebugFlag() ) {
			logSb.append(getThreadIcon());
			logSb.append(BRACE_L).append(Level.DEBUG.getDesc()).append(" ");
			logSb.append(getClassName(caller)).append(".");
			logSb.append(caller.getMethodName()).append("(").append(caller.getFileName()).append(":").append(caller.getLineNumber()).append(")").append(BRACE_R).append(SPACE);
			logSb.append(logStr);
		} else {
			if ( logSb.length() != 0 ) {
				logSb.append(SPACE);
			}
			logSb.append(logStr);
		}
		logStr = logSb.toString();
		logALog(level, logStr, e, caller);
		return logStr;
	}

	private static boolean hasAConsoleAppenderAndLevelEnabled(LogProfile logProfile, Level level) {
		return logProfile.hasConsoleAppender && logProfile.level.ordinal() <= ((isSaveDebugAsInfo() && level == Level.DEBUG) ? Level.INFO : level).ordinal();
	}

	private static void logALog(Level level, String logStr, Throwable e, StackTraceElement caller) {
		if ( level != Level.PRINT ) {
			LogProfile logProfile = getLogProfile() == null ? getLogProfile(caller) : getLogProfile();
			if ( logProfile != null ) {
				String methodName = level.name().toLowerCase();
				if ( getDebugFlag() && isSaveDebugAsInfo() && level == Level.DEBUG ) {
					methodName = "info";
				}
				boolean success = true;
				try {
					Method m = null;
					if ( e != null && (getDebugFlag() || isSaveStackTraceInLog()) ) {
						m = Log.class.getMethod(level.name().toLowerCase(), Object.class, Throwable.class);
						m.invoke(logProfile.log, logStr, e);
					} else {
						m = Log.class.getMethod(methodName, Object.class);
						m.invoke(logProfile.log, logStr);
					}
				} catch ( Throwable e1 ) {
					success = false;
				}
				if ( success && hasAConsoleAppenderAndLevelEnabled(logProfile, level) ) {
					logADebug(logStr, e);
					return;
				}
			}
		}
		if ( level == Level.PRINT || getDebugFlag() || level.ordinal() >= Level.INFO.ordinal() ) {
			if ( !getDebugFlag() ) {
				level = Level.PRINT;
			}
			logStr = genFakePrefix(level, caller) + logStr;
			System.out.println(logStr);
			if ( e != null ) {
				e.printStackTrace();
			}
			logADebug(logStr, e);
		}
	}

	private static void logADebug(String logStr, Throwable e) {
		if ( getDebugFlag() && isSaveDebugLogExclusively() ) {
			if ( debugOut == null ) {
				createDebugOutput();
			}
			if ( debugOut != null ) {
				debugOut.println(logStr);
				if ( e != null ) {
					e.printStackTrace(debugOut);
				}
			}
		}
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
		QLog.timerFlagTl.set(timerFlag);
	}

	private static boolean getTimerFlag() {
		if ( timerFlagTl.get() == null ) {
			checkDebugFlag();
		}
		return timerFlagTl.get();
	}

	protected static void innerCall(Level level, Object logStr, Throwable e, StackTraceElement caller) {
		logHandler(Level.DEBUG, logStr, e, caller);
	}

	public static void dbUtil(Level level, String logStr, Throwable e) {
		StackTraceElement caller = new Throwable().getStackTrace()[2];
		logHandler(level, logStr, e, caller);
	}

	/******************** LOG BUFFER ********************/
	public static Boolean getBufferFlag() {
		if ( bufferFlagTl.get() == null ) {
			return false;
		}
		return bufferFlagTl.get();
	}

	public static void setBufferFlag(boolean bufferFlag) {
		QLog.bufferFlagTl.set(bufferFlag);
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
		if ( logBufferTl.get() == null ) {
			setLogBuffer(new ArrayList<String>());
		}
		return logBufferTl.get();
	}

	private static void setLogBuffer(List<String> logBuffer) {
		QLog.logBufferTl.set(logBuffer);
	}

	private static List<String> getWarnNErrorBuffer() {
		if ( warnNErrorBufferTl.get() == null ) {
			warnNErrorBufferTl.set(new ArrayList<String>());
		}
		return warnNErrorBufferTl.get();
	}

	private static void setWarnNErrorBuffer(List<String> warnNErrorBuffer) {
		QLog.warnNErrorBufferTl.set(warnNErrorBuffer);
	}

	private static Boolean isLastLogEndstWithoutANewline() {
		if ( lastLogEndsWithoutANewlineTl.get() == null ) {
			return false;
		}
		return lastLogEndsWithoutANewlineTl.get();
	}

	private static void setLastLogEndsWithoutNewline(boolean lastLogEndsWithoutANewline) {
		QLog.lastLogEndsWithoutANewlineTl.set(lastLogEndsWithoutANewline);
	}

	/** TO MAKE THINGS EASY */
	/**
	 * Same as {@link #getDebugFlag()}
	 * @return
	 */
	public static boolean isDebugMode() {
		return getDebugFlag();
	}

	/** UNIVERSAL TOGGLES */
	public static boolean isUsingSimpleClassName() {
		return usingSimpleClassName;
	}

	public static synchronized void setUsingSimpleClassName(boolean usingSimpleClassName) {
		if ( QLog.usingSimpleClassName != usingSimpleClassName ) {
			QLog.usingSimpleClassName = usingSimpleClassName;
			info(I18N.tr("Using simple class name has been set to <{0}>", QLog.usingSimpleClassName));
		}
	}

	public static boolean isSaveDebugAsInfo() {
		return saveDebugAsInfo;
	}

	public static synchronized void setSaveDebugAsInfo(boolean saveDebugAsInfo) {
		if ( QLog.saveDebugAsInfo != saveDebugAsInfo ) {
			QLog.saveDebugAsInfo = saveDebugAsInfo;
			info(I18N.tr("Save DEBUG as INFO has been set to <{0}>", QLog.saveDebugAsInfo));
		}
	}

	public static boolean isSaveStackTraceInLog() {
		return saveStackTraceInLog;
	}

	public static synchronized void setSaveStackTraceInLog(boolean saveStackTraceInLog) {
		if ( QLog.saveStackTraceInLog != saveStackTraceInLog ) {
			QLog.saveStackTraceInLog = saveStackTraceInLog;
			info(I18N.tr("Save stack trace in log has been set to <{0}>", QLog.saveStackTraceInLog));
		}
	}

	public static boolean isSaveDebugLogExclusively() {
		return saveDebugLogExclusively;
	}

	public static synchronized void setSaveDebugLogExclusively(boolean saveDebugLogExclusively) {
		if ( QLog.saveDebugLogExclusively != saveDebugLogExclusively ) {
			QLog.saveDebugLogExclusively = saveDebugLogExclusively;
			info(I18N.tr("Save debug log exclusively has been set to <{0}>", QLog.saveDebugLogExclusively));
		}
	}

	public static long getOverrunThreshold() {
		return overrunThreshold;
	}

	public static synchronized void setOverrunThreshold(long overrunThreshold, TimeUnit unit) {
		long time = unit.toMillis(overrunThreshold);
		if ( QLog.overrunThreshold != time ) {
			QLog.overrunThreshold = time;
			info(I18N.tr("Overrun threshold has been set to <{0}>", QTimestamp.niceDisplay(QLog.overrunThreshold)));
		}
	}

	public static Object getRootLogger() {
		if ( rootLogger == null ) {
			switch ( getLogType() ) {
			case LOG4J:
				try {
					Method getRootLogger = loggerClazz.getMethod("getRootLogger");
					rootLogger = getRootLogger.invoke(loggerClazz);
				} catch ( Throwable t ) {
					t.printStackTrace();
				}
				break;
			case LOG4J2:
			case LOG4J2_JCL:
				try {
					Method getRootLogger = loggerContext.getClass().getMethod("getRootLogger");
					rootLogger = getRootLogger.invoke(loggerContext);
				} catch ( Throwable t ) {
					t.printStackTrace();
				}
				break;
			default:
				break;
			}
		}
		if ( rootLogger == null ) {
			rootLogger = new Object();
		}
		return rootLogger;
	}

}
