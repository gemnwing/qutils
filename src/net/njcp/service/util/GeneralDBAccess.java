package net.njcp.service.util;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;

public class GeneralDBAccess implements QDBAccess {
	private DataSource dataSource;
	private String configLocation;

	public GeneralDBAccess() {
	}

	public GeneralDBAccess(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public GeneralDBAccess(String driver, String url, String user, String passwd) {
		this.dataSource = getDefaultBasicDataSource(driver, url, user, passwd);
	}

	@Override
	public void setCoreSource(Object source) {
		if ( source instanceof DataSource ) {
			this.dataSource = ((DataSource) source);
		}
	}

	private static DataSource getDefaultBasicDataSource(String driver, String url, String user, String passwd) {
		BasicDataSource dataSource = new BasicDataSource();
		if ( driver == null || driver.isEmpty() ) {
			dataSource.setDriverClassName(guessDriverName(url));
		} else {
			dataSource.setDriverClassName(driver);
		}
		dataSource.setUrl(url);
		dataSource.setUsername(user);
		dataSource.setPassword(passwd);

		int DEFAULT_MAX_ACTIVE = 10;
		long DEFAULT_MAX_WAIT = 60 * 1000;
		int DEFAULT_MAX_IDLE = 5;
		int DEFAULT_MIN_IDLE = 1;
		int DEFAULT_INITIALSIZE = 1;
		int DEFAULT_REMOVE_ABANDONED_TIMEOUT = 60;
		long DEFAULT_TIME_BETWEEN_EVICTABLE_RUNS = 30 * 1000;
		long MIN_EVICTABLE_IDLE_TIME = 30 * 1000;

		dataSource.setInitialSize(DEFAULT_INITIALSIZE);
		dataSource.setMaxActive(DEFAULT_MAX_ACTIVE);
		dataSource.setMaxWait(DEFAULT_MAX_WAIT);
		dataSource.setMaxIdle(DEFAULT_MAX_IDLE);
		dataSource.setMinIdle(DEFAULT_MIN_IDLE);

		dataSource.setRemoveAbandoned(true);
		dataSource.setRemoveAbandonedTimeout(DEFAULT_REMOVE_ABANDONED_TIMEOUT);
		dataSource.setLogAbandoned(true);

		dataSource.setTimeBetweenEvictionRunsMillis(DEFAULT_TIME_BETWEEN_EVICTABLE_RUNS);
		dataSource.setMinEvictableIdleTimeMillis(MIN_EVICTABLE_IDLE_TIME);

		dataSource.setTestWhileIdle(true);
		dataSource.setTestOnBorrow(true);
		dataSource.setTestOnReturn(true);
		// dataSource.setDefaultReadOnly(true);

		if ( dataSource.getDriverClassName() != null && dataSource.getDriverClassName().toLowerCase().contains("oracle") ) {
			dataSource.setValidationQuery("select 1 from dual");
		} else {
			dataSource.setValidationQuery("select 1");
		}
		return dataSource;
	}

	public List<Object[]> query(String sql) {
		return executeSQLQuery(sql, new Object[0]);
	}

	@Override
	public List<Object[]> executeSQLQuery(String sql, List<?> params) {
		return executeSQLQuery(sql, params.toArray());
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Object[]> executeSQLQuery(String sql, Object... params) {
		return (List<Object[]>) executeSQLQuery(false, sql, params);
	}

	public List<?> executeSQLQuery(boolean retWithColName, String sql, Object... params) {
		List<?> retList;
		QLog.dbUtil(QLog.Level.DEBUG, "SQL:" + sql, null);
		String queryMark = I18N.tr("Querying SQL");
		String getConnMark = I18N.tr("Getting connection");
		List<Object[]> objList = null;
		List<Map<String, Object>> mapList = null;
		if ( retWithColName ) {
			mapList = new ArrayList<Map<String, Object>>();
		} else {
			objList = new ArrayList<Object[]>();
		}
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		Connection conn = null;
		try {
			QTimer.setTimeMark(getConnMark);
			conn = this.dataSource.getConnection();
			QTimer.showTimeElapsed(getConnMark);
			if ( conn != null ) {
				pstmt = conn.prepareStatement(sql);
				// .createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
				// ResultSet rs = stmt.executeQuery(sql);
				if ( params != null && params.length > 0 ) {
					for ( int i = 0; i < params.length; i++ ) {
						pstmt.setObject(i + 1, params[i]);
					}
				}
				QTimer.setTimeMark(queryMark);
				rs = pstmt.executeQuery();
				ResultSetMetaData metaData = rs.getMetaData();
				int columnCount = metaData.getColumnCount();
				while ( rs.next() ) {
					if ( retWithColName ) {
						Map<String, Object> tmpMap = new HashMap<String, Object>();
						for ( int i = 1; i <= columnCount; i++ ) {
							tmpMap.put(metaData.getColumnName(i), rs.getObject(i));
						}
						mapList.add(tmpMap);
					} else {
						Object[] row = new Object[columnCount];
						for ( int i = 1; i <= columnCount; i++ ) {
							row[i - 1] = rs.getObject(i);
						}
						objList.add(row);
					}
				}
			}
		} catch ( Throwable t ) {
			QLog.dbUtil(QLog.Level.ERROR, I18N.tr("Query failed for SQL: {0}", sql), t);
		} finally {
			try {
				if ( pstmt != null ) {
					pstmt.close();
				}
			} catch ( Throwable t ) {
			}
			try {
				if ( rs != null ) {
					rs.close();
				}
			} catch ( Throwable t ) {
			}
			try {
				if ( conn != null ) {
					conn.close();
				}
			} catch ( Throwable t ) {
			}
		}
		long timeElapsed = QTimer.getTimeElapsedInMillis(queryMark);
		if ( retWithColName ) {
			retList = mapList;
		} else {
			retList = objList;
		}
		String logStr = I18N.tr("SQL Execution finised, result {0}, time elapsed {1}", (retList == null ? "NULL" : retList.size()), QTimer.format(timeElapsed));
		QLog.Level level;
		long limit = 300 * 1000;
		if ( timeElapsed >= limit ) {
			level = QLog.Level.WARN;
			logStr += I18N.tr(", over upper-limit({0}), SQL: {1};", QTimer.format(limit), sql);
		} else {
			level = QLog.Level.DEBUG;
			logStr += ".";
		}
		QLog.dbUtil(level, logStr, null);
		return retList;
	}

	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> findResult(String sql, List<?> params) {
		return (List<Map<String, Object>>) executeSQLQuery(true, sql, params);
	}

	@Override
	public int executeSQL(String sql, Object... params) {
		QLog.dbUtil(QLog.Level.DEBUG, "SQL:" + sql, null);
		String executeMark = I18N.tr("Executing SQL");
		String getConnMark = I18N.tr("Getting connection");
		PreparedStatement pstmt = null;
		int retVal = -1;
		Connection conn = null;
		try {
			QTimer.setTimeMark(getConnMark);
			conn = this.dataSource.getConnection();
			QTimer.showTimeElapsed(getConnMark);
			if ( conn != null ) {
				pstmt = conn.prepareStatement(sql);
				if ( params != null && params.length > 0 ) {
					for ( int i = 0; i < params.length; i++ ) {
						pstmt.setObject(i + 1, params[i]);
					}
				}
				QTimer.setTimeMark(executeMark);
				retVal = pstmt.executeUpdate();
				conn.commit();
			}
		} catch ( Exception e ) {
			QLog.dbUtil(QLog.Level.ERROR, I18N.tr("Failed to execute SQL: {0}", sql), e);
		} finally {
			try {
				if ( pstmt != null ) {
					pstmt.close();
				}
			} catch ( Throwable t ) {
			}
			try {
				if ( conn != null ) {
					conn.close();
				}
			} catch ( Throwable t ) {
			}
		}
		long timeElapsed = QTimer.getTimeElapsedInMillis(executeMark);
		String logStr = I18N.tr("SQL Execution finised, infected rows {0}, time elapsed {1}", retVal, QTimer.format(timeElapsed));
		QLog.Level level;
		long limit = 300 * 1000;
		if ( timeElapsed >= limit ) {
			level = QLog.Level.WARN;
			logStr += I18N.tr(", over upper-limit({0}), SQL: {1};", QTimer.format(limit), sql);
		} else {
			level = QLog.Level.DEBUG;
			logStr += ".";
		}
		QLog.dbUtil(level, logStr, null);
		return retVal;
	}

	public static String guessDriverName(String url) {
		String driverName = null;
		String dbType = url.split(":")[1];
		if ( dbType.toLowerCase().equals("oracle") ) {
			driverName = "oracle.jdbc.driver.OracleDriver";
		} else if ( dbType.toLowerCase().equals("dm") || dbType.toLowerCase().equals("dm7") ) {
			driverName = "dm.jdbc.driver.DmDriver";
		} else if ( dbType.toLowerCase().equals("dm6") ) {
			driverName = "dm6.jdbc.driver.DmDriver";
		} else if ( dbType.toLowerCase().equals("kingbase") ) {
			driverName = "com.kingbase.Driver";
		} else if ( dbType.toLowerCase().equals("db2") ) {
			driverName = "com.ibm.db2.jcc.DB2Driver";
		} else {
			driverName = null;
			QLog.error(I18N.tr("Unsupported database type: {0}", dbType));
		}
		return driverName;
	}

	public DataSource getDataSource() {
		return this.dataSource;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public String getConfigLocation() {
		return this.configLocation;
	}

	public void setConfigLocation(String configLocation) throws Exception {
		this.configLocation = configLocation;
		if ( this.dataSource == null ) {
			QParam param = new QParam().load(configLocation);
			String urlKey = "url";
			String url = param.getString(urlKey, null);
			if ( url == null ) {
				throw new QDBUException("Failed to get parameter \"" + urlKey + "\" from configuration file \"" + configLocation + "\".");
			}
			String userKey = "user";
			String user = param.getString(userKey, null);
			if ( user == null ) {
				throw new QDBUException("Failed to get parameter \"" + userKey + "\" from configuration file \"" + configLocation + "\".");
			}
			String passwd = param.getString("passwd", null);
			String driver = param.getString("driver", null);
			this.dataSource = getDefaultBasicDataSource(driver, url, user, passwd);
		}
	}

	public void close() {
		try {
			Method m = this.dataSource.getClass().getMethod("close");
			if ( m != null ) {
				m.invoke(this.dataSource);
			}
		} catch ( Throwable t ) {
			QLog.error("Failed to close database connection pool.", t);
		}
	}

	public static void main(String[] args) throws Exception {
		// ClassPathXmlApplicationContext a = new ClassPathXmlApplicationContext("qutils.workspace.applicationcontext.xml");
		// GeneralDBAccess g = a.getBean(GeneralDBAccess.class);
		// // g.setConfigLocation("dbaccess.properties");
		// System.out.println(
		// //
		// g.executeSQLQuery("select sysdate from dual;").get(0)[0]
		// //
		// );
		// g.close();
		// a.close();
	}

}
