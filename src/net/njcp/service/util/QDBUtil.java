package net.njcp.service.util;

import javax.persistence.Table;

public class QDBUtil {
	Object configuration;

	public QDBAccess getDBAccess() {
		Class<?> clazz = null;
		try {
			clazz = Class.forName("org.hibernate.SessionFactory");
		} catch ( ClassNotFoundException e ) {
			e.printStackTrace();
		}
		if ( clazz != null ) {
			if ( clazz.isInstance(this.configuration) ) {

			}
		}
		return null;
	}

	public static String getQualifiedTableName(Class<?> clazz) {
		if ( clazz == null ) {
			return null;
		}
		Table table = clazz.getAnnotation(Table.class);
		String catalog = table.catalog();
		String schema = table.schema();
		String tabName = table.name();
		return (catalog.trim().isEmpty() ? "" : catalog + ".") + (schema.trim().isEmpty() ? "" : schema + ".") + tabName;
	}
}
