package net.njcp.service.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;

import net.njcp.service.util.I18N;

public class I18N21 {

	private static final String TR_METHOD_NAME = I18N.tr("I18N.tr");
	private static final String DROPPED_FLAG = I18N.tr("# Dropped at ");
	private static final String ADDED_FLAG = I18N.tr("# Added at ");
	private static final String UPDATED_FLAG = I18N.tr("# Updated at ");
	private static final String NON_SYMBOL = "[^0-9!@#$%^&*_=+;:'`~,./\\|?\\[\\]()<>{}\"\\- \n\t\r\b\f\\\\]";
	private static final String ONLY_SYMBOL = "[0-9!@#$%^&*_=+;:'`~,./\\|?\\[\\]()<>{}\"\\- \n\t\r\b\f\\\\]";
	private static final String RETURN_PATTERN = "^[ \t]*return[ \t]+.*";
	private static final String ASSIGN_PATTERN = "^[ \t]*([a-z]+[ \t]+)*([a-zA-Z\\.]+[ \t]+)*[a-zA-Z0-9_\\.]+[ \t]*[+]?=.*";
	private static final String CHINESE_CHARACTORS = "[\u3400-\u4DB5\u4E00-\u9FA5\u9FA6-\u9FBB\uF900-\uFA2D\uFA30-\uFA6A\uFA70-\uFAD9]";
	private static final String COMBINE_PATTERN = ".*(\"[^\"]*" + NON_SYMBOL + "{1,}[^\"]*\"[ \t]*\\+[ \t]*[^0-9 \t]+|[^0-9 \t]+[ \t]*\\+[ \t]*\"[^\"]*" + NON_SYMBOL + "{1,}[^\"]*\").*";
	private static final int PATTERN_OPTION = Pattern.DOTALL | Pattern.MULTILINE;

	// private static final String STRING_FORMAT_SPECIFIER = "%(\\d+\\$)?([-#+ 0,(\\<]*)?(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z%])";
	private static ConcurrentHashMap<String, Bundle> bundleMap = new ConcurrentHashMap<String, Bundle>();
	private static String charset;

	private static ConcurrentHashMap<String, Pattern> patterns = new ConcurrentHashMap<String, Pattern>();

	private static final String DEFAULT_DECIMAL_FORMAT = "0.0#####";
	private static final String DEFAULT_INTEGER_FORMAT = "#";
	private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

	private static final String QQTT = "";
	private static final String[] DIGIT_TRANS = new String[] { I18N.tr("零"), I18N.tr("一"), I18N.tr("二"), I18N.tr("三"), I18N.tr("四"), I18N.tr("五"), I18N.tr("六"), I18N.tr("七"), I18N.tr("八"), I18N.tr("九") };
	private static final String[] DIGIT_SCALE = new String[] { "", I18N.tr("十"), I18N.tr("百"), I18N.tr("千") };

	private static final int LEADING_LENGTH = 18;
	private static final int QUOTED_LENGTH = 25;

	@SuppressWarnings("serial")
	public static final List<String> NON_PATCH_METHODS = new ArrayList<String>() {
		{
			add("System\\.get.*");	// system to get env
			add("System\\.set.*");	// system to set env
			add(".*\\.get");	// to get from some map
			add(".*\\.*put");	// to put in some map
			add(".*\\.getTaskManager");	// to get task manager
			add(".*\\.getBean");	// to get task manager

			/* string methods */
			add(".*\\.startsWith");
			add(".*\\.endsWith");
			add(".*\\.matches");
			add(".*\\.replace");
			add(".*\\.replaceAll");
			add(".*\\.replaceFirst");
			add(".*\\.contains");
			add(".*\\.equals");
			add(".*\\.split");

			/* valueOf */
			add(".*\\.valueOf");

			/* parametes methods */
			add(".*\\.load");
			add(".*\\.loadParam");
			add(".*\\.getBoolean");
			add(".*\\.getDouble");
			add(".*\\.getInteger");
			add(".*\\.getList");
			add(".*\\.getLongCSVList");
			add(".*\\.getObject");
			add(".*\\.getProperty");
			add(".*\\.getString");
			add(".*\\.getSubParam");
			add(".*\\.getTimestamp");

			/* assignmeng */
			add(".*[Ss]ql[+]?=");
			add(".*[Cc]har[Ss]et[+]?=");

			/* sql */
			add(".*[Qq]uery");
			add(".*[HhSs][Qq][Ll].*");
			add(".*\\.getTables");
		}
	};
	@SuppressWarnings("serial")
	public static final List<String> FORCE_PATCH_METHODS = new ArrayList<String>() {
		{
			add("System\\.out\\.print.*");
			add(".*\\.info");
			add(".*\\.warn");
			add(".*\\.error");
			add(".*\\.trace");
			add(".*\\.debug");
		}
	};

	@SuppressWarnings("serial")
	public static class Bundle extends LinkedHashMap<String, Message> {

		public void addAMessage(String source, String translation, String comment, LinkedHashSet<String> locations) {
			Message message = null;
			if ( super.containsKey(source) ) {
				message = super.get(source);
				message.setSource(source);
				message.setTranslation(translation);
				message.setComment(comment);
				message.addLocations(locations);
			} else {
				message = new Message(source, translation, comment, locations.toArray(new String[] {}));
			}
			super.put(source, message);

		}

		public void addAMessage(String source, String javaName, int lineNum) {
			String location = "# " + javaName + ":" + lineNum;
			Message message = null;
			if ( super.containsKey(source) ) {
				message = super.get(source);
				message.addLocations(location);
			} else {
				message = new Message(source, "", "", location);
			}
			super.put(source, message);

		}

		public String getTranslation(String source) {
			if ( super.containsKey(source) ) {
				return super.get(source).getTranslation();
			}
			return null;
		}

		@Override
		public String toString() {
			String retStr = "";
			for ( Message message : this.values() ) {
				retStr += message.toString();
			}
			return retStr;
		}

	}

	private static class Message {
		private String source;
		private String translation;
		private LinkedHashSet<String> locations;
		private String comment;

		public Message(String source, String translation, String comment, String... locations) {
			this.source = source;
			this.translation = translation;
			this.comment = comment;
			addLocations(locations);
		}

		public void setTranslation(String translation) {
			this.translation = translation;
		}

		public String getTranslation() {
			return translation;
		}

		public LinkedHashSet<String> getLocations() {
			if ( this.locations == null ) {
				this.locations = new LinkedHashSet<String>();
			}
			return locations;
		}

		public void addLocations(String... locations) {
			for ( String s : locations ) {
				if ( s != null && !s.trim().isEmpty() && s.trim().startsWith("#") ) {
					getLocations().add(s.trim());
				}
			}
		}

		public void addLocations(LinkedHashSet<String> locations) {
			addLocations(locations.toArray(new String[] {}));
		}

		public String getComment() {
			return comment;
		}

		public void setComment(String comment) {
			this.comment = comment;
		}

		public String getSource() {
			return source;
		}

		public void setSource(String source) {
			this.source = source;
		}

		@Override
		public String toString() {
			String retStr = "\n";
			for ( String location : this.locations ) {
				retStr += location + "\n";
			}
			retStr += this.comment + "\n";
			retStr += this.source + " = " + this.translation + "\n";
			return retStr;
		}

		@Override
		public boolean equals(Object o) {
			if ( o == null ) {
				return false;
			}
			if ( !(o instanceof Message) ) {
				return false;
			}
			Message m = (Message) o;
			if ( !getSource().equals(m.getSource()) ) {
				return false;
			}
			if ( !getTranslation().equals(m.getTranslation()) ) {
				return false;
			}
			if ( !getLocations().equals(m.getLocations()) ) {
				return false;
			}
			return true;
		}

	}

	public static String getCharset() {
		if ( charset == null ) {
			charset = "UTF-8";
		}
		return charset;
	}

	public static void setCharset(String charset) {
		I18N21.charset = charset;
	}

	private static Bundle getBundle() {
		String className = Thread.currentThread().getStackTrace()[3].getClassName().replaceAll("^.*\\.", "");
		if ( className.contains("$") ) {
			className = className.replaceAll("\\$.*$", "");
		}
		if ( !bundleMap.containsKey(className) ) {
			synchronized ( bundleMap ) {
				if ( !bundleMap.containsKey(className) ) {
					Class<?> clazz = null;
					try {
						clazz = Class.forName(Thread.currentThread().getStackTrace()[3].getClassName());
					} catch ( ClassNotFoundException e ) {
						e.printStackTrace();
					}
					Bundle tmpBundle = loadFromClass(clazz, getCharset(), true);
					QLog.debug(I18N.tr("Putting loaded bundle(size:{0}) for class \"{1}\"", tmpBundle.size(), className));
					bundleMap.put(className, tmpBundle);
				}
			}
		}
		return bundleMap.get(className);
	}

	public static Bundle loadFromClass(Class<?> clazz, String charset, boolean escapeFlag) {
		InputStream is = null;

		String fileNameWOSuffix = clazz.getName().replace('.', '/');
		String fileName = fileNameWOSuffix + "_" + Locale.getDefault() + ".properties";
		QLog.info(I18N.tr("Loading bundle from file \"{0}\"", fileName.replaceAll(".*/", "")));
		try {
			is = clazz.getClassLoader().getResourceAsStream(fileName);
			if ( is == null ) {
				QLog.warn(I18N.tr("File \"{0}_{1}.properties\" was not found, \"{2}.properties\" will be loaded by default.", clazz.getSimpleName(), Locale.getDefault(), clazz.getSimpleName()));
				fileName = fileNameWOSuffix + ".properties";
				is = clazz.getClassLoader().getResourceAsStream(fileName);
			}
		} catch ( Exception e ) {
			is = null;
		}
		if ( is != null ) {
			return loadFromStream(is, charset, escapeFlag);
		} else {
			QLog.warn(I18N.tr("File \"{0}\" was not found.", fileName.replaceAll("^.*/", "")));
		}
		return new Bundle();
	}

	public static Bundle loadFromFile(File msgFile, String charset, boolean escapeFlag) {
		if ( msgFile != null ) {
			if ( !msgFile.isFile() ) {
				QLog.error(I18N.tr("Message file \"{0}\" is either not a file or not exist.", msgFile.getAbsolutePath()));
			}
			QLog.info(I18N.tr("Loading bundle from file \"{0}\"", msgFile.getName()));
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(msgFile);
			} catch ( Exception e ) {
				fis = null;
			}
			if ( fis != null ) {
				return loadFromStream(fis, charset, escapeFlag);
			}
		} else {
			QLog.error(I18N.tr("Message file is null."));
		}
		return new Bundle();
	}

	public static Bundle loadFromStream(InputStream is, String charset, boolean escapeFlag) {
		Bundle bundle = new Bundle();
		if ( is == null ) {
			QLog.error(I18N.tr("Message file is null."));
			return bundle;
		}
		InputStreamReader isr = null;
		BufferedReader br = null;
		try {
			isr = new InputStreamReader(is, Charset.forName(charset));
			br = new BufferedReader(isr);
			String line = null;
			LinkedHashSet<String> locations = new LinkedHashSet<String>();
			String comment = "";
			boolean dropped = false;
			int lineNum = 0;
			while ( (line = br.readLine()) != null ) {
				lineNum++;
				line = line.trim();
				if ( line.startsWith("#") ) {
					if ( line.startsWith(DROPPED_FLAG) ) {
						dropped = true;
						comment = line;
						continue;
					} else if ( line.startsWith(ADDED_FLAG) || line.startsWith(UPDATED_FLAG) ) {
						comment = line;
						continue;
					} else if ( line.toLowerCase().matches(".*\\.java:[0-9]+$") ) {
						locations.add(line);
						continue;
					}
				} else if ( line.isEmpty() ) {
					continue;
				}
				if ( !dropped || !escapeFlag ) {
					char[] charArray = line.toCharArray();
					char lastChar = '\0';
					String source = "";
					String translation = null;
					boolean equalSignFound = false;
					boolean quoted = false;
					boolean escaped = false;
					for ( char curChar : charArray ) {
						if ( escaped ) {
							escaped = false;
						} else if ( lastChar != '\\' && curChar == '\\' ) {
							escaped = true;
						}
						if ( curChar == '"' ) {
							if ( (quoted && !escaped) || (!quoted && lastChar != '\'') ) {
								quoted = !quoted;
							}
						}
						if ( !equalSignFound && !quoted && curChar == '=' ) {
							equalSignFound = true;
							continue;
						}
						if ( !equalSignFound ) {
							source += curChar;
						} else {
							translation = (translation == null ? "" : translation) + curChar;
						}
						lastChar = curChar;
					}
					source = source.trim();
					if ( translation != null ) {
						translation = translation.trim();
					}
					if ( escapeFlag ) {
						source = source.replaceAll("(^\"|\"$)", "");
						source = StringEscapeUtils.unescapeJava(source);
						if ( translation != null ) {
							translation = translation.replaceAll("(^\"|\"$)", "");
							translation = StringEscapeUtils.unescapeJava(translation);
						}
					}
					if ( !placeholderDiff(source, translation) ) {
						QLog.warn(I18N.tr("Placeholder inconsistent: line<{0}> source<{1}> translation<{2}>", lineNum, source, translation));
					}
					bundle.addAMessage(source, translation, comment, locations);
				}
				locations.clear();
				comment = "";
				dropped = false;
			}
		} catch ( Exception e ) {
			QLog.error(I18N.tr("Failed to parse message file due to unexpected exception."), e);
		} finally {
			try {
				if ( is != null ) {
					is.close();
				}
				if ( isr != null ) {
					isr.close();
				}
				if ( br != null ) {
					br.close();
				}
			} catch ( Exception e ) {
				e.printStackTrace();
			}
		}
		return bundle;
	}

	private static boolean placeholderDiff(String source, String translation) {
		if ( translation == null ) {
			return true;
		}
		HashSet<String> sourcePlaceholders = new HashSet<String>();
		HashSet<String> transPlaceholders = new HashSet<String>();
		char[] charArray;
		HashSet<String> placeholders;
		for ( int i = 1; i <= 2; i++ ) {
			if ( i == 1 ) {
				charArray = source.toCharArray();
				placeholders = sourcePlaceholders;
			} else {
				charArray = translation.toCharArray();
				placeholders = transPlaceholders;
			}
			String placeholder = "";
			boolean placeholderFound = false;
			for ( char c : charArray ) {
				if ( c == '{' ) {
					placeholderFound = true;
				}
				if ( placeholderFound && c == '}' ) {
					if ( !placeholder.isEmpty() ) {
						placeholder += c;
						placeholders.add(placeholder);
					}
					placeholderFound = false;
					placeholder = "";
				}
				if ( placeholderFound ) {
					placeholder += c;
					if ( placeholder.replaceAll("[ \t]", "").length() >= 2 && !placeholder.matches("\\{[ \t]*[0-9]+(,[^{}]*)*") ) {
						placeholderFound = false;
						placeholder = "";
					}
				}
			}
		}
		return sourcePlaceholders.equals(transPlaceholders);
	}

	public static void saveOrMerge(Bundle newUnescapedBundle, File msgFile) {
		FileOutputStream fos = null;
		List<Message> sortedFinalBundle = new ArrayList<Message>();
		try {
			if ( !msgFile.isFile() ) {
				if ( !msgFile.createNewFile() ) {
					throw new Exception(I18N.tr("Creating file \"{0}\" failed.", msgFile.getAbsolutePath()));
				}
			}
			Bundle oldUnescapedBundle = loadFromFile(msgFile, getCharset(), false);
			LinkedHashSet<String> keySet = new LinkedHashSet<String>();
			keySet.addAll(oldUnescapedBundle.keySet());
			keySet.addAll(newUnescapedBundle.keySet());
			for ( String source : keySet ) {
				Message message = newUnescapedBundle.get(source);
				Message oldMessage = oldUnescapedBundle.get(source);
				boolean updated = false;
				String oldSource = source;
				String newSource = source;
				if ( message != null && oldMessage != null ) {
					updated = true;
				} else {
					String refStr = StringEscapeUtils.unescapeJava(source).replaceAll(ONLY_SYMBOL, "").toLowerCase();
					if ( message != null ) {	// new message: not null, old message: null
						for ( String origin : oldUnescapedBundle.keySet() ) {
							if ( StringEscapeUtils.unescapeJava(origin).replaceAll(ONLY_SYMBOL, "").toLowerCase().equals(refStr) ) {
								oldSource = origin;
								updated = true;
								break;
							}
						}
					} else if ( oldMessage != null ) {	// new message: null, old message: not null
						for ( String origin : newUnescapedBundle.keySet() ) {
							if ( StringEscapeUtils.unescapeJava(origin).replaceAll(ONLY_SYMBOL, "").toLowerCase().equals(refStr) ) {
								newSource = origin;
								updated = true;
								break;
							}
						}
					} else {
						continue;
					}
				}

				if ( updated ) {
					message = newUnescapedBundle.get(newSource);
					oldMessage = oldUnescapedBundle.get(oldSource);
					if ( source.equals(newSource) && source.equals(oldSource) && !oldMessage.getComment().startsWith(DROPPED_FLAG) ) {
						message.setComment(oldMessage.getComment());
					} else {
						message.setComment(UPDATED_FLAG + new QTimestamp().toSimpleString());
					}
					message.setTranslation(oldMessage.getTranslation());
				} else {
					if ( message != null ) {
						message.setComment(ADDED_FLAG + new QTimestamp().toSimpleString());
					} else {
						message = oldMessage;
						if ( !message.getComment().startsWith(DROPPED_FLAG) ) {
							message.setComment(DROPPED_FLAG + new QTimestamp().toSimpleString());
						}
						if ( !message.getSource().startsWith("#") ) {
							message.setSource("# " + message.getSource());
						}
					}
				}
				sortedFinalBundle.add(message);
				for ( int i = sortedFinalBundle.size() - 1; i > 0; i-- ) {
					Message current = sortedFinalBundle.get(i);
					Message next = sortedFinalBundle.get(i - 1);
					if ( Integer.valueOf(current.getLocations().iterator().next().split(":")[1]) < Integer.valueOf((next.getLocations().iterator().next().split(":")[1])) ) {
						sortedFinalBundle.set(i, next);
						sortedFinalBundle.set(i - 1, current);
					}
				}
				oldUnescapedBundle.remove(oldSource);
				newUnescapedBundle.remove(newSource);
			}

			fos = new FileOutputStream(msgFile);
			for ( Message msg : sortedFinalBundle ) {
				for ( String location : msg.getLocations() ) {
					fos.write((location + "\n").getBytes(getCharset()));
				}
				fos.write((msg.getComment() + "\n").getBytes(getCharset()));
				fos.write((msg.getSource() + " = " + (msg.getTranslation() == null ? "" : msg.getTranslation()) + "\n\n").getBytes(getCharset()));
			}
		} catch ( Exception e ) {
			e.printStackTrace();
		} finally {
			if ( fos != null ) {
				try {
					fos.close();
				} catch ( IOException e ) {
					e.printStackTrace();
				}
			}
		}
		QLog.info(I18N.tr("Update message file \"{0}\" succeed.", msgFile.getAbsolutePath()));
	}

	public static String tr(String source, Object... args) {
		String message = null;
		try {
			if ( getBundle().containsKey(source) ) {
				message = getBundle().get(source).getTranslation();
			}
		} catch ( Exception e ) {
			message = null;
		}
		if ( message == null ) {
			message = source;
		}
		try {
			MessageFormat messageFormat = new MessageFormat(message);
			if ( args.length != 0 ) {
				for ( int i = 0; i < Math.min(messageFormat.getFormatsByArgumentIndex().length, args.length); i++ ) {
					if ( args[i] != null && messageFormat.getFormatsByArgumentIndex()[i] == null ) {
						if ( args[i] instanceof Number ) {
							if ( args[i] instanceof Double || args[i] instanceof Float ) {
								messageFormat.setFormatByArgumentIndex(i, new DecimalFormat(DEFAULT_DECIMAL_FORMAT));
							} else {
								messageFormat.setFormatByArgumentIndex(i, new DecimalFormat(DEFAULT_INTEGER_FORMAT));
							}
						} else if ( args[i] instanceof Date ) {
							messageFormat.setFormatByArgumentIndex(i, new SimpleDateFormat(DEFAULT_DATE_FORMAT));
						}
					}
				}
				message = messageFormat.format(args);
			}
		} catch ( Exception e ) {
			QLog.error(I18N.tr("Failed to parse \"{0}\".", message), e);
		}
		return message;
	}

	private static void patchWithTrMethod(File javaFile) {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(javaFile);
		} catch ( FileNotFoundException e ) {
			e.printStackTrace();
		}
		int printLength = (I18N.tr("{0}:xxxx", javaFile.getName())).length();
		InputStreamReader isr = new InputStreamReader(fis);
		BufferedReader br = new BufferedReader(isr);
		boolean lineCommented = false;
		boolean blockCommented = false;
		boolean quoted = false;
		int escaped = 0;
		boolean imported = false;
		String curLine = null;
		String wholeLine = null;
		String newLine = null;
		String quotedContent = "";
		int lineNum = 0;
		List<String> newContent = new ArrayList<String>();
		int patchedCount = 0;
		int bracketLevel = 0;

		// String methodName = null;
		HashMap<Integer, String> methodLevelMap = new HashMap<Integer, String>();
		try {
			while ( (curLine = br.readLine()) != null ) {
				lineNum++;
				lineCommented = false;
				if ( curLine.trim().startsWith("/*") ) {
					blockCommented = true;
				}
				if ( !blockCommented && (curLine.trim().startsWith("//") || curLine.trim().startsWith("@") || match(curLine.trim(), "/\\*.*\\*/")) ) {
					lineCommented = true;
				}
				if ( !lineCommented ) {
					if ( curLine.trim().contains(I18N21.class.getName()) ) {
						imported = true;
					}
					String unCommentedLine = null;
					if ( blockCommented ) {
						unCommentedLine = "";
					} else {
						unCommentedLine = unComment(curLine);
					}
					if ( !blockCommented && !unCommentedLine.trim().isEmpty() && !match(unCommentedLine.trim(), ".*[;{}:]$") ) {
						wholeLine = (wholeLine == null) ? unCommentedLine : wholeLine + " " + unCommentedLine.trim();
						continue;
					}
					wholeLine = (wholeLine == null) ? curLine : wholeLine + " " + curLine.trim();
					// line = line.trim();
					// System.out.println(String.format("%10d:\t", count) + line);
					// line = line + "\n";
					char[] charArray = null;
					if ( !blockCommented && !wholeLine.contains(TR_METHOD_NAME) && match(wholeLine.replace("\\\"", "").trim(), COMBINE_PATTERN) ) {
						String combinedContent = combineToMessageFormat(wholeLine);
						if ( !wholeLine.replaceAll("[ \t]", "I18N.tr(").equals(combinedContent.replaceAll(")[ \t]", "")) ) {
							patchedCount++;
							wholeLine = combinedContent;
						}
					}
					charArray = wholeLine.trim().toCharArray();
					char lastChar = '\0';
					String leadingString = "";
					for ( char curChar : charArray ) {
						if ( !quoted ) {
							if ( bracketLevel == 0 && (leadingString.endsWith("returnI18N.tr(") || leadingString.endsWith(")=")) ) {
								methodLevelMap.put(bracketLevel, getMethodNameFromLeadingString(leadingString));
							}
							if ( curChar == '(' ) {
								bracketLevel++;
								methodLevelMap.put(bracketLevel, getMethodNameFromLeadingString(leadingString + "("));
							} else if ( curChar == ')' ) {
								methodLevelMap.remove(bracketLevel);
								bracketLevel--;
							}
							if ( curChar == ';' ) {
								if ( methodLevelMap.containsKey(0) ) {
									methodLevelMap.remove(0);
								}
							}
							if ( lastChar == '/' && curChar == '*' ) {
								blockCommented = true;
								continue;
							}
							if ( lastChar == '*' && curChar == '/' ) {
								blockCommented = false;
								continue;
							}
							if ( !blockCommented ) {
								if ( (lastChar == '/' && curChar == '/') ) {
									lineCommented = true;
									continue;
								}
							}
						} else {
							if ( escaped == 0 && curChar == '\\' ) {
								escaped++;
							} else if ( escaped == 1 ) {
								escaped++;
							}
						}
						if ( curChar == '"' ) {
							if ( (quoted && escaped == 0) || (!quoted && lastChar != '\'') ) {
								quoted = !quoted;
							}
						}
						if ( !blockCommented && !lineCommented && quoted ) {
							quotedContent += curChar;
						} else {
							leadingString += curChar;
						}
						if ( !quoted && !quotedContent.isEmpty() ) {
							quotedContent += curChar;
							System.out.print(String.format("%" + printLength + "s\t%-" + (LEADING_LENGTH + 10) + "s %-" + (QUOTED_LENGTH + 9) + "s", "↓", I18N.tr("LEADING<{0}>", ((leadingString.length() >= LEADING_LENGTH) ? "..." + leadingString.substring(leadingString.length() - LEADING_LENGTH + 3): leadingString)), I18N.tr("QUOTED<{0}>", ((quotedContent.length() >= QUOTED_LENGTH) ? quotedContent.substring(0, (QUOTED_LENGTH - 3) / 2) + "..." + quotedContent.substring(quotedContent.length() - (QUOTED_LENGTH - 3) / 2): quotedContent))));
							String methodName = methodLevelMap.get(bracketLevel);
							if ( methodName == null ) {
								for ( int i = bracketLevel - 1; i >= 0; i-- ) {
									methodName = methodLevelMap.get(i);
									if ( methodName != null && !methodName.equals(TR_METHOD_NAME) ) {
										break;
									}
								}
							}
							System.out.print(I18N.tr(" METHOD<{0}>", methodName));
							if ( shouldIPatchThis(methodName, quotedContent) ) {
								String converted = TR_METHOD_NAME + "(" + quotedContent.replace("\"", QQTT) + ")";
								wholeLine = wholeLine.replace(quotedContent, converted);
								patchedCount++;
							}
							System.out.println();
							quotedContent = "";
							leadingString = "";
						}
						lastChar = curChar;
						if ( escaped == 2 ) {
							escaped = 0;
						}
						// char loop ends
					}
					newLine = wholeLine.replace(QQTT, "\"");
					wholeLine = null;
				} else {
					newLine = curLine;
				}
				System.out.println(String.format("%" + printLength + "s\t", javaFile.getName() + ":" + lineNum) + newLine.trim());
				if ( !imported && newLine.trim().toLowerCase().matches("^public[ \t]+(abstract[ \t]+)*class.*") ) {
					newContent.add(I18N.tr("import {0};\n", I18N21.class.getName()));
					imported = true;
				}
				newContent.add(newLine);
				methodLevelMap.clear();
				newLine = null;
				// line loop ends
			}
		} catch (  IOException e ) {
			QLog.error(I18N.tr("Failed to patch file \"{0}\".", javaFile.getAbsolutePath()), e);
		}

		if ( patchedCount > 0 ) {
			File newFile;
			FileOutputStream newFos = null;
			try {
				newFile = File.createTempFile(javaFile.getName(), null);
				newFos = new FileOutputStream(newFile);
				for ( String line : newContent ) {
					newFos.write((line + "\n").getBytes(getCharset()));
				}
				String bkSuffix = I18N.tr(".{0}.trbak", new QTimestamp().format("yyyy_MM_dd_HH_mm_ss"));
				if ( javaFile.renameTo(new File(javaFile.getAbsolutePath() + bkSuffix)) ) {
					newFile.renameTo(javaFile);
				} else {
					throw new Exception(I18N.tr("Rename failed..."));
				}
			} catch ( Exception e ) {
				QLog.error(I18N.tr("Backup failed, due to"), e);
			} finally {
				try {
					if ( newFos != null ) {
						newFos.close();
					}
				} catch ( IOException e ) {
					QLog.error(I18N.tr("Failed to close file output stream."), e);
				}
			}
			QLog.info(I18N.tr("Patching file \"{0}\" succeed.", javaFile.getAbsolutePath()));
		} else {
			QLog.info(I18N.tr("Nothing patched for file \"{0}\"", javaFile.getAbsolutePath()));
		}
	}

	private static String combineToMessageFormat(String line) {
		// block commented contente already removed
		String unCommentedLine = unComment(line).replace("++", QQTT);
		String comment = extractComment(line);
		String combined = combineToMessageFormat(unCommentedLine, 0).replace(QQTT, "++");
		return combined + (comment.isEmpty() ? "" : "\t" + comment);
	}

	private static String combineToMessageFormat(String line, int callCount) {
		String leading = line.replaceAll("[^ \t,].*$", "");
		String lineContent = null;
		if ( ++callCount == 1 ) {
			if ( match(line, RETURN_PATTERN) ) {
				leading = line.replaceAll("return.*$", "") + "return";
				lineContent = line.replaceAll("^" + leading + "[ \t]*", "").trim().replaceAll(";$", "");
				return leading + " " + combineToMessageFormat(lineContent, callCount) + ";";
			} else if ( match(line, ASSIGN_PATTERN) ) {
				leading = line.replaceAll("=.*$", "") + "=";
				lineContent = line.substring(leading.length()).trim().replaceAll(";$", "");
				if ( containsNonPatchMethod(getMethodNameFromLeadingString(leading)) ) {
					return line;
				}
				return leading + " " + combineToMessageFormat(lineContent, callCount) + ";";
			}
		}
		lineContent = line.replaceAll("^" + leading, "");

		// System.out.println(I18N.tr("a = {0}{1}{2}", a, String.valueOf("\nb = " + (1 + 2), b);
		char[] charArray = lineContent.toCharArray();
		char lastChar = '\0';
		boolean quoted = false;
		boolean escaped = false;
		boolean bracketedContentHasAPlusSign = false;
		// boolean bracketed = false;
		// int bracketLevel = -1;
		int bracketLevel = 0;
		String bracketedContent = "";
		// boolean beforeBracket = true;
		String beforeBracketContent = "";
		// String afterBracketContent = "";
		String plusSepratedContent = "";
		List<String> plusSepratedContents = new ArrayList<String>();
		int ternaryOperatorLevel = 0;
		// String processedContent ="";
		for ( char curChar : charArray ) {
			if ( escaped ) {
				escaped = false;
			} else if ( lastChar != '\\' && curChar == '\\' ) {
				escaped = true;
			}
			if ( curChar == '"' ) {
				if ( (quoted && !escaped) || (!quoted && lastChar != '\'') ) {
					quoted = !quoted;
				}
			}
			if ( !quoted ) {
				if ( curChar == '(' || curChar == '?' ) {
					// System.out.println("unbracketed →" + unBracketedContent + "←");
					if ( curChar == '?' ) {
						ternaryOperatorLevel++;
					}
					bracketLevel++;
					if ( bracketLevel == 1 ) {
						plusSepratedContent += curChar;
						beforeBracketContent += curChar;
						continue;
					}
					// beforeBracket = false;
				}
				if ( curChar == ')' || curChar == ',' || (curChar == ':' && ternaryOperatorLevel > 0) ) {
					if ( curChar == ')' || curChar == ':' ) {
						bracketLevel--;
						if ( curChar == ':' ) {
							ternaryOperatorLevel--;
						}
					}
					if ( (bracketLevel == 0 || (curChar == ',' && bracketLevel == 1)) && !bracketedContent.isEmpty() ) {
						// System.out.println(bracketedContent);
						String methodName = getMethodNameFromLeadingString(beforeBracketContent);
						if ( !containsNonPatchMethod(methodName) || containsForcePatchMethod(methodName) ) {
							String processedContent = combineToMessageFormat(bracketedContent, callCount);
							plusSepratedContent = plusSepratedContent.replace(bracketedContent, processedContent);
						}
						bracketedContent = "";
						if ( bracketLevel == 0 ) {
							beforeBracketContent = "";
						}
					}
				}
				if ( bracketLevel == 0 && !bracketedContentHasAPlusSign && (curChar == '+' && (lastChar == '"' || String.valueOf(lastChar).matches("[^0-9]")) || (lastChar == '+' && (curChar == '"' || String.valueOf(curChar).matches("[^0-9 \t]")))) ) {
					bracketedContentHasAPlusSign = true;
				}
			}
			if ( bracketLevel == 0 ) {
				// if ( beforeBracket ) {
				beforeBracketContent += curChar;
				// } else {
				// afterBracketContent += curChar;
				// }
			} else {
				bracketedContent += curChar;
			}
			if ( !quoted && bracketLevel == 0 && curChar == '+' ) {
				plusSepratedContents.add(plusSepratedContent.trim());
				plusSepratedContent = "";
			} else {
				plusSepratedContent += curChar;
			}
			if ( quoted || (curChar != ' ' && curChar != '\t') ) {
				lastChar = curChar;
			}
		}
		plusSepratedContents.add(plusSepratedContent.trim());
		boolean patched = false;
		if ( bracketedContentHasAPlusSign ) {
			int quotedParamCount = 0;
			lineContent = TR_METHOD_NAME + "(\"";
			String params = "";
			int idx = 0;
			for ( String content : plusSepratedContents ) {
				if ( content.startsWith("\"") && content.endsWith("\"") ) {
					lineContent += content.replaceAll("(^\"|\"$)", "");
					if ( shouldIPatchThis(null, content) ) {
						quotedParamCount++;
					}
				} else {
					lineContent += "{" + idx++ + "}";
					params += ", " + content;
				}
			}
			if ( quotedParamCount > 0 ) {
				lineContent += "\"";
				if ( !params.isEmpty() ) {
					lineContent += params;
				}
				lineContent += ")";
				patched = true;
			}
		}
		if ( !patched ) {
			lineContent = "";
			for ( String content : plusSepratedContents ) {
				lineContent += content + " + ";
			}
			lineContent = lineContent.replaceAll(" \\+ $", "");
		} else {
			QLog.debug(I18N.tr("calling for the {0} time, content: {1}", callCount, lineContent));
		}
		return leading + lineContent;
	}

	private static String extractComment(String line) {
		String comment = "";
		Character lastChar = null;
		boolean quoted = false;
		boolean escaped = false;
		boolean blockCommented = false;
		boolean lineCommented = false;
		char[] charArray = line.toCharArray();
		for ( int i = 0; i < charArray.length; i++ ) {
			char curChar = charArray[i];
			if ( lastChar != null ) {
				if ( escaped ) {
					escaped = false;
				} else if ( lastChar != '\\' && curChar == '\\' ) {
					escaped = true;
				}
				if ( curChar == '"' ) {
					if ( (quoted && !escaped) || (!quoted && lastChar != '\'') ) {
						quoted = !quoted;
					}
				}
				if ( !quoted ) {
					if ( !blockCommented && lastChar == '/' && curChar == '/' ) {
						lineCommented = true;
					}
					if ( lastChar == '/' && curChar == '*' ) {
						blockCommented = true;
					}
					if ( !lineCommented && lastChar == '*' && curChar == '/' ) {
						comment += "*/ ";
						blockCommented = false;
					}
				}
				if ( lineCommented || blockCommented ) {
					comment += lastChar;
				}
			}
			lastChar = curChar;
			if ( i == charArray.length - 1 && lineCommented ) {
				comment += curChar;
			}
		}
		return comment;
	}

	private static String unComment(String line) {
		String lineWOComment = "";
		Character lastChar = null;
		boolean quoted = false;
		boolean escaped = false;
		boolean blockCommented = false;
		char[] charArray = line.toCharArray();
		for ( int i = 0; i < charArray.length; i++ ) {
			char curChar = charArray[i];
			if ( lastChar != null ) {
				if ( escaped ) {
					escaped = false;
				} else if ( lastChar != '\\' && curChar == '\\' ) {
					escaped = true;
				}
				if ( curChar == '"' ) {
					if ( (quoted && !escaped) || (!quoted && lastChar != '\'') ) {
						quoted = !quoted;
					}
				}
				if ( !quoted ) {
					if ( !blockCommented && lastChar == '/' && curChar == '/' ) {
						break;
					}
					if ( lastChar == '/' && curChar == '*' ) {
						blockCommented = true;
						continue;
					}
					if ( lastChar == '*' && curChar == '/' ) {
						blockCommented = false;
						lastChar = null;
						continue;
					}
				}
				if ( !blockCommented ) {
					lineWOComment += lastChar;
				}
			}
			lastChar = curChar;
			if ( i == charArray.length - 1 ) {
				lineWOComment += curChar;
			}
		}
		return lineWOComment;
	}

	private static boolean containsNonPatchMethod(String methodName) {
		if ( methodName != null ) {
			for ( String pattern : NON_PATCH_METHODS ) {
				if ( match(methodName, pattern) ) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean containsForcePatchMethod(String methodName) {
		if ( methodName != null ) {
			for ( String pattern : FORCE_PATCH_METHODS ) {
				if ( match(methodName, pattern) ) {
					return true;
				}
			}
		}
		return false;
	}

	private static void print(boolean printFlag, String string) {
		if ( printFlag ) {
			System.out.print(string);
		}
	}

	static void shouldIPatchThis(String quotedContent) {
		System.out.print(I18N.tr("should I patch this? {0}", quotedContent));
		shouldIPatchThis("", quotedContent);
		System.out.println();
	}

	private static boolean shouldIPatchThis(String methodName, String quotedContent) {
		boolean printFlag = (methodName != null);
		String desc = I18N.tr(" PATCH?<");
		/* empty string */
		if ( quotedContent == null || quotedContent.replaceAll("(^\"|\"$)", "").trim().isEmpty() ) {
			print(printFlag, I18N.tr("{0}NO> CAUSE<NO> CAUSE<empty>", desc));
			return false;
		}

		/* check previous method */
		if ( methodName != null ) {
			if ( methodName.contains(TR_METHOD_NAME) ) {
				print(printFlag, I18N.tr("{0}DONE>", desc));
				return false;
			}
			if ( containsNonPatchMethod(methodName) ) {
				print(printFlag, I18N.tr("{0}NO> CAUSE<avoid-patching-method>", desc));
				return false;
			}
		}

		if ( match(quotedContent, "[\"]?.*" + CHINESE_CHARACTORS + ".*[\"]?") ) {
			print(printFlag, I18N.tr("{0}READY> CAUSE<Chinese characters included>", desc));
			return true;
		}

		/* potential string format */
		// if ( matches(quotedContent, ".*" + STRING_FORMAT_SPECIFIER + ".*") ) {
		// System.out.println(desc + "NO> CAUSE<it might be a string formatter pattern.");
		// return false;
		// }
		// keep it patched due to there can be some words within a string format pattern.

		String unEscapedQuotedContent = StringEscapeUtils.unescapeJava(quotedContent);
		/* at least 2 non-symbol characters */
		if ( !match(unEscapedQuotedContent, ".*" + NON_SYMBOL + "{2,}.*") ) {
			print(printFlag, I18N.tr("{0}NO> CAUSE<no more than 2 continuous letters>", desc));
			return false;
		}

		if ( containsForcePatchMethod(methodName) ) {
			print(printFlag, I18N.tr("{0}READY> CAUSE<force-patching-method>", desc));
			return true;
		}

		/* single word */
		if ( match(quotedContent, "[\"]?[A-Z]?[a-z]+[\"]?") ) {
			print(printFlag, I18N.tr("{0}NO> CAUSE<single word>", desc));
			return false;
		}

		/* potential regex */
		if ( match(quotedContent, ".*(\\.|[\\[\\(]+.+[\\]\\)]+)+([\\+\\*]|\\{[0-9,]+\\})+.*") || match(quotedContent, ".*\\[.*\\\\[a-zA-Z]+.*\\].*") ) {
			print(printFlag, I18N.tr("{0}NO> CAUSE<might be a regex pattern>", desc));
			return false;
		}

		/* HEX string */
		if ( match(quotedContent.toLowerCase(), "[\"]?[#]?[0-9a-f]+[\"]?") ) {
			if ( match(quotedContent.toLowerCase(), "[\"]?#[0-9a-f]{6}[\"]?") ) {
				print(printFlag, desc + "NO> CAUSE<might stands for a color>");
			} else {
				print(printFlag, I18N.tr("{0}NO> CAUSE<might be a number in HEX format>", desc));
			}
			return false;
		}

		/* potential argument */
		if ( match(quotedContent, "[\"]?[a-z]+([A-Z]+[a-z]+)+[\"]?") || match(quotedContent.toLowerCase(), "[\"]?[a-z]_[a-zA-Z0-9]+[\"]?") ) {
			print(printFlag, I18N.tr("{0}NO> CAUSE<seems like a variable name>", desc));
			return false;
		}
		if ( match(quotedContent, "[\"' \t]*\\(.+(,.+)+\\)[\"' \t]*") ) {
			print(printFlag, I18N.tr("{0}NO> CAUSE<probably a series of arguments>", desc));
			return false;
		}

		/* all capitals */
		if ( match(quotedContent, "[^a-z]+") ) {
			if ( match(quotedContent, "[\"]?[A-Z_0-9]+[\"]?") ) {
				print(printFlag, desc + "NO> CAUSE<might be a marco or code or something>");
				return false;
			} else if ( unEscapedQuotedContent.replaceAll(ONLY_SYMBOL, "").matches("[^AEIOU]+") ) {
				print(printFlag, I18N.tr("{0}NO> CAUSE<might be some kind of abbreviation>", desc));
				return false;
			}
		}

		/* special format */
		if ( match(quotedContent, "[\"']?[a-zA-Z_\\-%?]+[\"']?") ) {
			print(printFlag, I18N.tr("{0}NO> CAUSE<might be a SQL like pattern>", desc));
			return false;
		}

		/* time format */
		if ( match(quotedContent, "[^a-zA-Z]*([\"' \t]*[Yy]+|[\\-_ \t./]*[Mm]+|[\\-_ \t./]*[Dd]+){2,3}[\\-_ \t./]*([Hh]+(24)?[\\-_ \t:]*[m]+[i]?[\\-_ \t:]*[s]+[\\-_ \t.]*[S]*)*[\"' \t]*[^a-zA-Z]*") ) {
			print(printFlag, I18N.tr("{0}NO> CAUSE<might be a time pattern>", desc));
			return false;
		}
		/* sqls */
		if ( match(quotedContent.toLowerCase(), ".*(update|from)*.*where[ \t]+.+([ \t]+(in|like|is)[ \t]+|[ \t]*[>=<]).*") || match(quotedContent.toLowerCase(), ".*insert[ \t]+into.*") || match(quotedContent.toLowerCase(), ".*(select|delete).*from.*") || match(quotedContent.toLowerCase(), ".*(truncate|drop|create)[ \t]+table.*") || match(quotedContent.toLowerCase(), ".*(group|order)[ \t]+by[ \t]+.*") || match(quotedContent.toLowerCase(), ".*(in[ \t]+\\(.*\\)|like[ \t]+'.*').*") || match(quotedContent.toLowerCase(), ".*(where|and|or)+[ \t]+.+([ \t]+is[ \t]+(not[ \t]+)?null|[ \t]*[>=<][ \t]*.+).*") || match(quotedContent.toLowerCase(), ".*(to_date|to_char|sysdate).*") ) {
			print(printFlag, I18N.tr("{0}NO> CAUSE<might be within a SQL statement>", desc));
			return false;
		}
		/* files */
		if ( match(quotedContent.toLowerCase(), "[\"']*([a-zA-Z0-9\\-_]*\\.)*(xml|java|txt|properties|conf|sys|jpg|jpeg|png|gif|bmp|doc[x]*|xls[x]*|ppt[x]*|pages|numbers|key)[\"']*") ) {
			if ( match(quotedContent.toLowerCase(), "[\"']*[^\"']+\\.[^\\.]+[\"']*$") ) {
				print(printFlag, I18N.tr("{0}NO> CAUSE<seems like a file name>", desc));
			} else {
				print(printFlag, I18N.tr("{0}NO> CAUSE<seems like a file suffix>", desc));
			}
			return false;
		}
		/* folders */
		if ( match(quotedContent.toLowerCase(), "[\"']*[a-zA-Z0-9\\-_\\. /]*/[a-zA-Z0-9\\-_\\. /]*[\"']*") ) {
			if ( match(quotedContent.toLowerCase(), ".*\\.[^/]+[\"']*$") ) {
				print(printFlag, I18N.tr("{0}NO> CAUSE<seems like a file name>", desc));
			} else {
				print(printFlag, I18N.tr("{0}NO> CAUSE<seems like a folder name>", desc));
			}
			return false;
		}

		/* className */
		if ( match(quotedContent, "[\"']*([a-zA-Z0-9]+\\.)+[A-Z][a-zA-Z0-9]+[\"']*") ) {
			print(printFlag, I18N.tr("{0}NO> CAUSE<seems like a class name>", desc));
			return false;
		}

		print(printFlag, I18N.tr("{0}READY>", desc));
		return true;
	}

	private static boolean match(Object stringObject, String paramString) {
		if ( stringObject != null ) {
			Pattern pattern = patterns.get(paramString);
			if ( pattern == null ) {
				pattern = Pattern.compile(paramString, PATTERN_OPTION);
				patterns.put(paramString, pattern);
			}
			return pattern.matcher(stringObject.toString()).matches();
		}
		return false;
	}

	private static String getMethodNameFromLeadingString(String leadingString) {
		if ( leadingString == null || leadingString.trim().isEmpty() ) {
			return null;
		}
		StringBuffer method = new StringBuffer();
		StringBuffer sb = new StringBuffer(leadingString.replaceAll("[ \t]", ""));
		sb = sb.reverse();
		char curChar = '\0';
		char lastChar = '\0';
		boolean methodFlag = false;
		int rightBracketCount = 0;
		// QLog.info(abc.getTaskDesc()+(resumeFlag ? "
		for ( int idx = 0; idx < sb.length(); idx++ ) {
			curChar = sb.charAt(idx);
			// System.out.println(idx + ":" + curChar + " method: " + new StringBuffer(method).reverse());
			if ( curChar == ')' ) {
				rightBracketCount++;
			} else if ( curChar == '(' && rightBracketCount > 0 ) {
				rightBracketCount--;
				continue;
			}
			if ( rightBracketCount == 0 && lastChar == '(' && String.valueOf(curChar).matches("[0-9a-zA-Z]") ) {
				methodFlag = true;
			} else if ( methodFlag && !String.valueOf(curChar).matches("[0-9a-zA-Z.]") ) {
				methodFlag = false;
				if ( method.length() != 0 ) {
					break;
				}
			}
			if ( methodFlag ) {
				method.append(curChar);
			}
			lastChar = curChar;
		}
		if ( method.length() == 0 ) {
			if ( match(leadingString, RETURN_PATTERN) ) {
				return "return";
			} else if ( match(leadingString, ASSIGN_PATTERN) ) {
				return leadingString.split("=")[0].trim() + "=";
			} else {
				return null;
			}
		}
		return method.reverse().toString();
	}

	public static void update(String javaFileName) {
		updatePropFile(new File(javaFileName), null);
	}

	public static void updatePropFile(File javaFile) {
		updatePropFile(javaFile, null);
	}

	public static void updatePropFile(File javaFile, File msgFile) {
		Bundle bundleUnescaped = new Bundle();
		if ( !javaFile.isFile() ) {
			QLog.error(I18N.tr("Java file \"{0}\" is not a file or simply not exist.", javaFile.getAbsolutePath()));
			return;
		}
		FileInputStream fis = null;
		InputStreamReader isr = null;
		BufferedReader br = null;
		try {
			fis = new FileInputStream(javaFile);
			isr = new InputStreamReader(fis);
			br = new BufferedReader(isr);
			String line = null;
			int lineNum = 0;
			while ( (line = br.readLine()) != null ) {
				lineNum++;
				line = unComment(line);
				if ( line.contains(TR_METHOD_NAME) ) {
					String[] a = line.split(TR_METHOD_NAME);
					if ( a.length > 1 ) {
						for ( int i = 1; i < a.length; i++ ) {
							char[] charArray = a[i].trim().toCharArray();
							String source = "";
							char lastChar = '\0';
							boolean quoted = false;
							boolean escaped = false;
							for ( char curChar : charArray ) {
								if ( escaped ) {
									escaped = false;
								} else if ( lastChar != '\\' && curChar == '\\' ) {
									escaped = true;
								}
								if ( curChar == '"' ) {
									if ( (quoted && !escaped) || (!quoted && lastChar != '\'') ) {
										quoted = !quoted;
									}
								}
								if ( quoted ) {
									source += curChar;
								} else if ( !source.isEmpty() ) {
									source += curChar;
									break;
								}
								lastChar = curChar;
							}
							if ( !source.isEmpty() ) {
								bundleUnescaped.addAMessage(source, javaFile.getName(), lineNum);
							}
						}
					}
				}
			}
			if ( msgFile == null ) {
				String msgFileName = javaFile.getAbsolutePath().replaceAll("\\.java$", "") + "_" + Locale.getDefault() + ".properties";
				msgFile = new File(msgFileName);
			}
			if ( !bundleUnescaped.isEmpty() || msgFile.isFile() ) {
				saveOrMerge(bundleUnescaped, msgFile);
			}
		} catch ( Exception e ) {
			e.printStackTrace();
		} finally {
			try {
				if ( fis != null ) {
					fis.close();
				}
				if ( isr != null ) {
					isr.close();
				}
				if ( br != null ) {
					br.close();
				}
			} catch ( Exception e ) {
				e.printStackTrace();
			}
		}
	}

	private static void patchOneFile(File javaFile) {
		if ( !javaFile.isFile() || !javaFile.getName().matches(".*\\.(JAVA|java)") ) {
			QLog.error(I18N.tr("File \"{0}\" is not a valid java file or simply dose not exist.", javaFile.getAbsolutePath()));
			return;
		}
		QLog.debug(I18N.tr("Patching java file \"{0}\".", javaFile.getAbsolutePath()));
		patchWithTrMethod(javaFile);
		updatePropFile(javaFile);
	}

	private static void patchFolders(File folder) {
		if ( !folder.isDirectory() ) {
			QLog.error(I18N.tr("Folder \"{0}\" dose not exist.", folder.getAbsolutePath()));
			return;
		}
		QLog.debug(I18N.tr("Patching directory \"{0}\".", folder.getAbsolutePath()));
		for ( File file : folder.listFiles() ) {
			if ( file.isDirectory() ) {
				patchFolders(file);
			} else if ( file.isFile() && file.getName().matches(".*\\.(JAVA|java)") ) {
				patchOneFile(file);
			}
		}
	}

	public static void patch(String fileName) {
		File file = new File(fileName);
		patch(file);
	}

	public static void patch(File file) {
		if ( !file.exists() ) {
			QLog.error(I18N.tr("File or directory \"{0}\" dost not exist.", file.getAbsolutePath()));
		} else if ( file.isDirectory() ) {
			patchFolders(file);
		} else if ( file.isFile() ) {
			patchOneFile(file);
		} else {
			QLog.warn(I18N.tr("What the what now? \"{0}\" is neither file nor directory.", file.getAbsolutePath()));
		}
	}

	public static String transNumberToChn(Object number) {
		if ( number instanceof Number ) {
			if ( number instanceof Integer || number instanceof Long || number instanceof BigInteger ) {
				return transNumberToChn(number.toString(), "");
			} else {
				return transNumberToChn(new DecimalFormat("#.#########").format(number).replaceAll("\\.$", ""), "");
			}
		} else {
			return transNumberToChn(number.toString().replaceAll("[^\\-\\.0-9]", ""), "");
		}
	}

	private static String transNumberToChn(String number, String base) {
		if ( !number.matches("^[-]?[0-9]+(\\.[0-9]+)?$") ) {
			return number;
		}
		if ( number.equals("0") ) {
			return I18N.tr("零");
		}
		StringBuffer intSb = new StringBuffer();
		StringBuffer decSb = new StringBuffer();
		String retStr = "";
		boolean negative = false;
		if ( number.contains(".") ) {
			intSb = new StringBuffer(number.split("\\.")[0]);
			decSb = new StringBuffer(number.split("\\.")[1]);
		} else {
			intSb = new StringBuffer(number);
		}
		if ( number.startsWith("-") ) {
			negative = true;
			intSb.deleteCharAt(0);
		}
		intSb = intSb.reverse();
		for ( int pos = 0; pos < intSb.length(); pos++ ) {
			if ( pos == 4 ) {
				retStr = transNumberToChn(intSb.reverse().substring(0, intSb.length() - pos), base.equals(I18N.tr("万")) ? I18N.tr("亿") : I18N.tr("万")) + retStr;
				break;
			}
			int digit = Integer.valueOf(intSb.substring(pos, pos + 1));
			retStr = DIGIT_TRANS[digit] + (digit == 0 ? "" : DIGIT_SCALE[pos]) + retStr;
		}
		if ( base.isEmpty() ) {
			retStr = retStr.replaceAll("^一十", "十");
		}
		retStr = retStr.replaceAll("零+", "零");
		for ( int pos = 0; pos < decSb.length(); pos++ ) {
			int digit = Integer.valueOf(decSb.substring(pos, pos + 1));
			retStr += ((pos == 0) ? I18N.tr("点") : "") + DIGIT_TRANS[digit];
		}
		retStr = retStr.replaceAll("零$", "");
		return (negative ? I18N.tr("负") : "") + retStr + base;
	}

	public static void installBundles(String sourceName) {
		installBundles(new File(sourceName));
	}

	public static void installBundles(File source) {
		if ( !source.exists() ) {
			QLog.error(I18N.tr("Folder or file \"{0}\" can not be accessed or simply not exist.", source.getAbsolutePath()));
			return;
		}
		if ( source.isDirectory() ) {
			QLog.info(I18N.tr("Loading property files within directory \"{0}\".", source.getAbsolutePath()));
			for ( File msgFile : source.listFiles() ) {
				if ( msgFile.getName().endsWith(".properties") ) {
					installBundles(msgFile);
				}
			}
		} else if ( source.isFile() ) {
			if ( !source.getName().endsWith(".properties") ) {
				QLog.error(I18N.tr("Message file can only be loaded as a property file."));
				return;
			}
			String className = source.getName().replaceAll("(_" + Locale.getDefault() + "|\\.properties$)", "");
			Bundle tmpBundle = loadFromFile(source, getCharset(), true);
			QLog.debug(I18N.tr("Putting loaded bundle(size:{0}) for class \"{1}\"", tmpBundle.size(), className));
			bundleMap.put(className, tmpBundle);
		} else {
			QLog.warn(I18N.tr("What the what now? \"{0}\" is neither file nor directory.", source.getAbsolutePath()));
		}
	}

	public static void main(String[] args) {
		// Timer.setTimeMark("I18N Test");
		// patch(new File("/Users/Dominic/Developer/EnergySVN/bgservice/BalanceCompute/test/net/njcp/service/balance/compute/test/LossComputeTest.java"));
		// patch("/Users/Dominic/Developer/EnergySVN/bgservice/BalanceCompute/src/net/njcp/service/balance/compute/utils/LossCompute.java");

		// System.out.println(placeholderDiff("a{0}b{1}c{2}d{3,b}e{1{2{3}}}", "4321432{5}{2}fdsa{3}fdsa{4}"));
		// installBundles("/Users/Dominic/Developer/EnergySVN/bgservice/BalanceCompute/src/net/njcp/service/balance/compute/utils/");
		// patch("/Users/Dominic/Developer/EnergySVN/bgservice/EmsEnergyRetriever/src/net/njcp/service/ems/EmsEnergyRetriever.java");
		// update("/Users/Dominic/Developer/EnergySVN/bgservice/Utils-ext/src/net/njcp/service/util/QLog.java");
		// patch("/Users/Dominic/Developer/EnergySVN/bgservice/Utils-ext/src/net/njcp/service/util/I18N21.java");
		// patch("/Users/Dominic/Developer/EnergySVN/bgservice/Utils-ext/src/net/njcp/service/util/RunTimeCheck.java");

		String testStr;
		testStr = I18N.tr("System.out.print(\"this is the \" + time + \" time test.\" + String.format(\"%\" + printLength /* afds//aaaafd */ + \"s a test \t%-\" + (LEADING_LENGTH + 10) + \"s %-\" + (QUOTED_LENGTH + 9) + \"s\", \"↓\", fdsafd)) // /* fdasd */ fdasfdas ");
		// testStr = "qqtt(\"abc \" + def + \" ghi \")";
		// testStr = "\"The \\\"last refresh time\\\" will be: \\\"\" + now + \"\\\"\"";
		// // testStr = " QLog.debug(\"Skipped for \" + meterDesc + \" due to incomplete data\" + ((startDayView == null) ? \", view at <\" + beginTime + \"> is null\" : \"\") + ((endDayView == null) ? \", view at <\" + endTime + \"> is null\" : \"\"));";
		// // testStr = "abc(\"a\" + (2+4), \"b\" + (3+4))";

		//
		System.out.println(I18N.tr("origin:   {0}", testStr));
				//
		System.out.println(I18N.tr("combined: {0}", combineToMessageFormat( testStr)));

		//
		System.out.println(I18N.tr("match?  : ") + match(testStr.replace("\\\"", ""), COMBINE_PATTERN) );

		//
		System.out.println(I18N.tr("uncommented: {0}", unComment(testStr)));

		//
		System.out.println(I18N.tr("comment : {0}", extractComment(testStr)));

		shouldIPatchThis(testStr);
		// System.out.println(match("throw new Exception(\".\" + tabId + \".\" + devId);".trim(),
		// "^.*(\".*" + NON_SYMBOL + "{1,}.*\"[ \t]*\\+[ \t]*[^0-9]+|[^0-9]+[ \t]*\\+[ \t]*\".*" + NON_SYMBOL + "{1,}.*\").*;$"));
		// System.out.println("throw new Exception(\".a\" + tabId + \".\" + devId);".trim()
		// .matches(".*(\"[^\"]*" + NON_SYMBOL + "{1,}[^\"]*\"[ \t]*\\+[ \t]*[^0-9 \t]+|[^0-9 \t]+[ \t]*\\+[ \t]*\"[^\"]*" + NON_SYMBOL + "{1,}[^\"]*\").*"));

		// String testStr = "\"abc\nd\nef\\nbc=1\"";
		// System.out.println(testStr);
		// String unescaped = StringEscapeUtils.unescapeJava(testStr);
		// System.out.println(testStr.replaceAll(ONLY_SYMBOL, ""));
		// System.out.println(unescaped.replaceAll(ONLY_SYMBOL, ""));
		// patch("/Users/Dominic/Developer/EnergySVN/bgservice/Utils-ext/src/net/njcp/service/util/Timer.java");
		// System.out.println("Save meter, parameters changed event for {0}".replaceAll(NOTHING_BUT_SYMBOL, ""));
		// patch("/Users/Dominic/Developer/EnergySVN/bgservice/Utils-ext/src/net/njcp/service/util/Parameters.java");
		// updatePropFile("/Users/Dominic/Developer/EnergySVN/bgservice/BalanceCompute/src/net/njcp/service/balance/task/LossComputeTask.java");
		// System.out.println("public abstract class LossComputeTaskManager<TL, TC> extends AbstractScheduledTaskManager<Long, LossComputeTask> implements Runnable {".trim().toLowerCase().matches("^public (abstract )*class.*"));
		// patch("/Users/Dominic/Developer/EnergySVN/bgservice/BalanceCompute/src/net/njcp/service/balance/task/LossComputeTaskManager.java");
		// System.out.println("from AclineLossConfig where id.aclnsegId = ? ".toLowerCase().matches(".*(update|from)*.*where[ \t]+[a-zA-Z_.]+([ \t]+(in|like|is)[ \t]+|[ \t]*[>=<]).*"));
		// System.out.println(getMethodNameFromLeadingString(
		// " public static final Parameters param = new Parameters();"
		//// "aram = new Parameters();"
		// //
		// ));
		// System.out.println(getMethodNameFromPreviousString("QLog.info(I18N.tr(\""));
		// System.out.println(Pattern.compile("[a-z]").matcher("c").matches());
		// Timer.showTimeElapsed("I18N Test");
	}

}
