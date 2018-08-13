package net.njcp.service.util;

import java.util.List;

public interface QDBAccess {

	public List<Object[]> executeSQLQuery(String sql, List<?> params);

	public List<Object[]> executeSQLQuery(String sql, Object... params);

	public int executeSQL(String sql, Object... params);

	void setCoreSource(Object configuration);
}
