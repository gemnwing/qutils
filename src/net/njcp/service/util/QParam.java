package net.njcp.service.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;

import org.apache.commons.lang.StringEscapeUtils;

import net.njcp.service.util.QLog.Level;

public class QParam extends Properties {

	/**
		 * 
		 */
	private static final long serialVersionUID = 5239929831942370035L;
	// private Properties prop = new Properties();

	private static final String subParamStartSymbol = "[";
	private static final String subParamEndSymbol = "]";

	private static Charset defaultCharset = Charset.forName("GB18030");

	private Object source;
	private boolean multiSource;
	private List<QParam> subParams = new ArrayList<QParam>();
	private StackTraceElement caller;

	private Object secretlyPut(Object key, Object value) {
		return super.put(key, value);
	}

	/**
	 * Put &lt;key = value&gt; to sub-parameter set specified by subType.
	 * @see {@link #put(Object, Object)}
	 * @see {@link #updateAllMatchedKey(Object, Object)}
	 */
	public Object put(String subType, Object key, Object value) {
		return getSubParam(subType).put(key, value);
	}

	/**
	 * Put &lt;key = value&gt; to root level, if there's a sub-parameter set has the same key, it won't be changed.
	 * @see {@link #updateAllMatchedKey(Object, Object)}
	 * @see {@link #put(String, Object, Object)}
	 */
	@Override
	public Object put(Object key, Object value) {
		if ( key != null ) {
			String act = I18N.tr("Putting into");
			Object orig = null;
			if ( super.containsKey(key) ) {
				act = I18N.tr("Updating");
				orig = super.get(key);
				if ( (value != null && value.equals(orig)) || (value == null && orig == null) ) {
					return value;
				}
			}
			QLog.innerCall(Level.DEBUG, I18N.tr("{0} prop map, key: {1}, value: {2}{3}", act, key, value, (orig != null ? I18N.tr(", origin: ") + orig : "")), null, getCaller());
			try {
				super.put(key, value);
			} catch ( Throwable e ) {
				QLog.debug(I18N.tr("Failed to add a property:<{0} = {1}>", key, value));
			}
		}
		return value;
	}

	/**
	 * Put &lt;key = value&gt; to root level and all the sub-parameter sets which contains the same key.
	 * @see {@link #put(Object, Object)}
	 * @see {@link #put(String, Object, Object)}
	 */
	public void updateAllMatchedKey(Object key, Object value) {
		put(key, value);
		if ( !this.subParams.isEmpty() ) {
			for ( QParam subParam : this.subParams ) {
				subParam.put(key, value);
			}
		}
	}

	@Override
	public void putAll(Map<? extends Object, ? extends Object> map) {
		if ( map != null && !map.isEmpty() ) {
			for ( Object key : map.keySet() ) {
				put(key, map.get(key));
			}
		}
	}

	@Override
	public String get(Object key) {
		return get(key, null);
	}

	public String get(Object key, Object defaultValue) {
		Object value = super.get(key);
		if ( value == null ) {
			value = defaultValue;
		}
		return (value == null) ? null : value.toString();
	}

	public String getString(Object key, Object defaultValue) {
		return get(key, defaultValue);
	}

	public <T> T getAs(Class<T> clazz, Object key, T defaultValue) {
		T retVal = QStringUtil.castTo(clazz, super.get(key));
		return (retVal == null ? defaultValue : retVal);
	}

	public Double getDouble(Object key, Double defaultValue) {
		return getAs(Double.class, key, defaultValue);
	}

	public Boolean getBoolean(Object key, Boolean defaultValue) {
		return getAs(Integer.class, key, (defaultValue == null ? 0 : (defaultValue ? 1 : 0))) > 0;
	}

	public Long getLong(Object key, Long defaultValue) {
		return getAs(Long.class, key, defaultValue);
	}

	public Integer getInteger(Object key, Integer defaultValue) {
		return getAs(Integer.class, key, defaultValue);
	}

	public Timestamp getTimestamp(Object key, Timestamp defaultValue) {
		return getAs(Timestamp.class, key, defaultValue);
	}

	public QTimestamp getQTimestamp(Object key, QTimestamp defaultValue) {
		return getAs(QTimestamp.class, key, defaultValue);
	}

	public List<Integer> getIntegerCSVList(Object key) {
		return getCSVList(Integer.class, key);
	}

	public List<Long> getLongCSVList(Object key) {
		return getCSVList(Long.class, key);
	}

	public List<Double> getDoubleCSVList(Object key) {
		return getCSVList(Double.class, key);
	}

	public List<String> getStringCSVList(Object key) {
		return getCSVList(String.class, key);
	}

	public <T> List<T> getCSVList(Class<T> clazz, Object key) {
		List<T> retList = new ArrayList<T>();
		String value = getString(key, null);
		if ( value != null && !value.trim().isEmpty() ) {
			String[] argArr = value.split(",");
			if ( argArr.length > 0 ) {
				for ( String arg : argArr ) {
					arg = arg.trim().replaceAll("(^\"|\"$)", "");
					if ( !arg.isEmpty() ) {
						retList.add(QStringUtil.castTo(clazz, arg));
					}
				}
			}
		}
		return retList;
	}

	public List<String> getList(Object keyword) {
		List<String> retList = new ArrayList<String>();
		keyword = keyword.toString();
		if ( !keyword.toString().endsWith("_") ) {
			keyword = keyword.toString() + "_";
		}
		TreeSet<String> keySet = new TreeSet<String>();
		for ( Object key : keySet() ) {
			if ( key.toString().startsWith(keyword.toString()) ) {
				keySet.add(key.toString());
			}
		}
		for ( String key : keySet ) {
			retList.add(get(key));
		}
		return retList;
	}

	public QParam getSubParam(Object key) {
		QParam param = null;
		try {
			if ( !key.toString().matches("^\\[.*\\]$") ) {
				key = subParamStartSymbol + key + subParamEndSymbol;
			}
			param = (QParam) getObject(key);
		} catch ( Exception e ) {
		}
		if ( param == null ) {
			param = new QParam();
		}
		return param;
	}

	public Object getObject(Object key) {
		try {
			return super.get(key);
		} catch ( Exception e ) {
			return null;
		}
	}

	private static InputStream getStreamFromFile(String confFile, StackTraceElement caller) throws Exception {
		InputStream stream = null;
		// Alarm.param("Loading configuration file: " + confFile);
		if ( confFile.matches("^([a-zA-Z]+\\.){2,}[a-zA-Z]+$") ) {
			// Alarm.debug("File <" + confFile + "> might be in classpath, convert it to url");
			String path = confFile.replaceAll("[a-zA-Z]+\\.[a-zA-Z]+$", "");
			confFile = path.replace('.', '/') + confFile.replace(path, "");
		}
		stream = Class.forName(caller.getClassName()).getResourceAsStream(confFile);
		if ( stream == null ) {
			stream = ClassLoader.getSystemResourceAsStream(confFile);
		}
		if ( stream == null ) {
			stream = new FileInputStream(confFile);
		}

		return stream;
	}

	public QParam load(File confFile) {
		return load(confFile.getAbsolutePath());
	}

	public QParam load(String confFile) {
		if ( this.source == null ) {
			this.source = confFile;
		} else if ( !this.source.toString().equals(confFile) ) {
			this.multiSource = true;
		}
		InputStream stream = null;
		try {
			stream = getStreamFromFile(confFile, getCaller());
			if ( confFile.toLowerCase().endsWith(".properties") ) {
				Properties tmpProp = new Properties();
				tmpProp.load(stream);
				putAll(tmpProp);
			} else {
				loadAEMSStructuredConfFile(stream);
			}
		} catch ( FileNotFoundException e1 ) {
			QLog.innerCall(Level.ERROR, I18N.tr("Trying to load parameters but file \"{0}\" was not found.", confFile), e1, getCaller());
		} catch ( Exception e2 ) {
			QLog.innerCall(Level.ERROR, I18N.tr("Failed to load parameters from file \"{0}\".", confFile), e2, getCaller());
		} finally {
			if ( stream != null ) {
				try {
					stream.close();
				} catch ( IOException e ) {
					QLog.innerCall(Level.ERROR, I18N.tr("Failed to close file stream."), e, getCaller());
				}
			}
		}
		return this;
	}

	public void save() {
		if ( this.multiSource ) {
			QLog.innerCall(Level.ERROR, I18N.tr("This parameter set seems from multiple sources, please use function <{0}.save(String confFile)> to try again.", this.getClass().getName()), null,
					getCaller());
			return;
		}
		save(this.source.toString());
	}

	public void save(Object key, Object value) {
		updateAllMatchedKey(key, value);
		save();
	}

	public void save(String confFile, Object key, Object value) {
		updateAllMatchedKey(key, value);
		save(confFile);
	}

	public void save(String confFile, String subType, Object key, Object value) {
		put(subType, key, value);
		save(confFile);
	}

	public void save(String confFile) {
		BufferedWriter bw = null;
		try {
			InputStream is = getStreamFromFile(confFile, getCaller());
			InputStreamReader isr = new InputStreamReader(is, defaultCharset);
			BufferedReader br = new BufferedReader(isr);
			String line = null;
			StringBuilder newLine = null;
			String type = null;
			QParam tmpParam = null;
			StringBuilder newContent = new StringBuilder();
			while ( (line = br.readLine()) != null ) {
				newLine = new StringBuilder(line);
				if ( line.matches("^\\[.*\\].*$") ) {
					type = subParamStartSymbol + line.split("[\\[\\]]")[1] + subParamEndSymbol;
					if ( containsKey(type) ) {
						tmpParam = getSubParam(type);
					} else {
						tmpParam = null;
					}
				} else if ( !line.isEmpty() && line.contains("=") ) {
					String[] prop = parseAProp(line, true);
					String key = prop[0];
					String oldValue = prop[1];
					String newValue = null;
					String comment = prop[2];
					if ( tmpParam != null && tmpParam.containsKey(key) ) {
						newValue = tmpParam.get(key);
					} else {
						newValue = get(key);
					}
					if ( (newValue != null && !newValue.equals(oldValue)) || (newValue == null && oldValue != null) ) {
						newLine.setLength(0);
						newValue = newValue == null ? "" : newValue;
						oldValue = oldValue == null ? "" : oldValue;
						QLog.innerCall(Level.DEBUG, I18N.tr("Update file, key:{0}, value:{1}, origin:{2}", key, newValue, oldValue), null, getCaller());
						newLine.append(key).append('=').append(newValue);
						if ( comment != null ) {
							newLine.append("\t#").append(comment);
						}
					}
				}
				newContent.append(newLine).append('\n');
			}
			File oldFile = new File(confFile);
			oldFile.renameTo(new File(I18N.tr("{0}.bak", oldFile.getAbsolutePath())));
			File newFile = new File(confFile);
			if ( newFile.createNewFile() && newFile.canWrite() ) {
				FileOutputStream fos = new FileOutputStream(newFile);
				OutputStreamWriter osw = new OutputStreamWriter(fos, defaultCharset);
				bw = new BufferedWriter(osw);
				bw.write(newContent.toString());
			}
		} catch ( Exception e ) {
			QLog.error(e, e);
		} finally {
			if ( bw != null ) {
				try {
					bw.close();
				} catch ( IOException e ) {
				}
			}
		}
	}

	private void loadAEMSStructuredConfFile(InputStream stream) throws Exception {
		try {
			InputStreamReader isr = new InputStreamReader(stream, defaultCharset);
			// InputStreamReader isr = new InputStreamReader(stream);
			BufferedReader br = new BufferedReader(isr);
			String line = null;
			String type = null;
			QParam tmpParam = null;
			while ( (line = br.readLine()) != null ) {
				// line = line.replaceAll("#.*$", "");
				// line = line.replaceAll("//.*$", "");
				if ( line.matches("^[ \t]*#.*$") ) {
					continue;
				}
				if ( line.matches("^\\[.*\\].*$") ) {
					type = subParamStartSymbol + line.split("[\\[\\]]")[1] + subParamEndSymbol;
					tmpParam = getSubParam(type);
					this.subParams.add(tmpParam);
					secretlyPut(type, tmpParam);
				} else {
					if ( !line.isEmpty() && line.contains("=") ) {
						String[] prop = parseAProp(line, true);
						String key = prop[0];
						String value = prop[1] == null ? "" : prop[1];
						if ( tmpParam != null ) {
							tmpParam.secretlyPut(key, value);
						}
						put(key, value);
					}
				}
			}
		} catch ( Exception e ) {
			throw e;
		}
	}

	protected static String[] parseAProp(String line, boolean escape) {
		StringBuilder keySb = new StringBuilder();
		StringBuilder valueSb = null;
		StringBuilder commentSb = null;
		char[] charArray = line.toCharArray();
		char lastChar = '\0';
		boolean equalSignFound = false;
		boolean quoted = false;
		boolean commentFound = false;
		int escaped = 0;
		for ( char curChar : charArray ) {
			if ( !quoted && curChar == '#' ) {
				commentFound = true;
				continue;
			}
			if ( commentFound ) {
				if ( commentSb == null ) {
					commentSb = new StringBuilder();
				}
				commentSb.append(curChar);
				continue;
			}
			if ( quoted ) {
				if ( escaped == 0 && curChar == '\\' ) {
					escaped++;
				} else if ( escaped == 1 ) {
					escaped++;
				} else if ( escaped >= 2 ) {
					escaped = 0;
				}
			}
			if ( curChar == '"' ) {
				if ( (quoted && escaped == 0) || (!quoted && lastChar != '\'') ) {
					quoted = !quoted;
				}
			}
			if ( !equalSignFound && !quoted && curChar == '=' ) {
				equalSignFound = true;
				continue;
			}
			if ( !equalSignFound ) {
				keySb.append(curChar);
			} else {
				if ( valueSb == null ) {
					valueSb = new StringBuilder();
				}
				valueSb.append(curChar);
			}
			lastChar = curChar;
		}
		String key = keySb.toString().trim();
		String value = valueSb == null ? null : valueSb.toString().trim();
		String comment = commentSb == null ? null : commentSb.toString().trim();

		if ( escape ) {
			key = key.replaceAll("(^\"|\"$)", "");
			key = StringEscapeUtils.unescapeJava(key);
			if ( value != null ) {
				value = value.replaceAll("(^\"|\"$)", "");
				value = StringEscapeUtils.unescapeJava(value);
			}
		}
		return new String[] { key, value, comment };
	}

	public QParam() {
		super();
	}

	public String getString(String string, Object object) {
		return get(string, object);
	}

	public static Charset getDefaultCharset() {
		return defaultCharset;
	}

	public static void setDefaultCharset(Charset defaultCharset) {
		QParam.defaultCharset = defaultCharset;
	}

	public StackTraceElement getCaller() {
		if ( this.caller == null ) {
			this.caller = new Throwable().getStackTrace()[2];
		}
		return this.caller;
	}

	public void setCaller(StackTraceElement caller) {
		this.caller = caller;
	}

	public static void main(String[] args) {
		String confFile = "/Users/Dominic/Downloads/NOBKP/a.conf";
		QParam param = new QParam().load(confFile);
		// QLog.println(param.get("test1"));
		// param.put("sub", "test2", param.getInteger("test2", 1) + 1);
		// QLog.println(param);
		// for ( Object key : param.keySet() ) {
		// QLog.println(key.getClass().getSimpleName() + " " + key + " = " + param.getString(key.toString(), "xx"));
		// }
		// // QLog.println(param.getObject("test1"));
		// QLog.println(param.getSubParam("sub"));
		// QLog.println(param.get("test1"));
		// QLog.println(param.getInteger("test1", 1));
		// param.save();
		System.out.println(param.getStringCSVList("date"));
		System.out.println(param.getDouble("failureRate", 20D));
	}

}
