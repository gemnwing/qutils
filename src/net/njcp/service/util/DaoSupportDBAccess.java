package net.njcp.service.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

public class DaoSupportDBAccess extends HibernateDaoSupport implements QDBAccess {
	public DaoSupportDBAccess() {
	};

	public DaoSupportDBAccess(SessionFactory sessionFactory) {
		super.setSessionFactory(sessionFactory);
	}

	public HibernateTemplate getService() {
		return getHibernateTemplate();
	}

	public void save(Object entity) {
		getHibernateTemplate().save(entity);
	}

	public void saveOrUpdate(Object entity) {
		getHibernateTemplate().saveOrUpdate(entity);
	}

	@Override
	public void setCoreSource(Object configuration) {
		if ( configuration instanceof SessionFactory ) {
			super.setSessionFactory((SessionFactory) configuration);
		}
	}

	@Override
	public List<Object[]> executeSQLQuery(String sql, List<?> params) {
		return executeSQLQuery(sql, params.toArray());
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Object[]> executeSQLQuery(String sql, Object... params) {
		List<Object[]> retList = new ArrayList<Object[]>();
		try {
			retList.addAll(getHibernateTemplate().find(sql, params));
		} catch ( Throwable t ) {
			QLog.error("Failed to query by sql:" + sql + (params != null ? (ArrayUtils.toString(params)) : ""), t);
		}
		return retList;
	}

	public List<?> query(String sql, Object... params) {
		return query(sql, null, null, params);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List<?> query(final String sql, final String paramName, final Collection<?> paramList, final Object... params) {
		List<?> retList = null;
		try {
			retList = (List) getHibernateTemplate().execute(new HibernateCallback() {
				@Override
				public Object doInHibernate(Session session) throws HibernateException {
					Query query = session.createQuery(sql);
					if ( params != null && params.length != 0 ) {
						for ( int i = 0; i < params.length; ++i ) {
							query.setParameter(i, params[i]);
						}
					}
					if ( paramName != null && paramList != null ) {
						query.setParameterList(paramName, paramList);
					}
					return query.list();
				}
			});
		} catch ( Throwable t ) {
			QLog.error("Failed to query by sql:" + sql, t);
		}
		return retList;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T> List<T> executeDynamicSQLQuery(final String sql, final Class<T> entityClass) {
		List<T> retList = null;
		try {
			retList = (List<T>) getHibernateTemplate().execute(new HibernateCallback() {
				@Override
				public Object doInHibernate(Session session) throws HibernateException {
					SQLQuery query = session.createSQLQuery(sql);
					query.addEntity(entityClass);
					return query.list();
				}
			});
		} catch ( Throwable t ) {
			QLog.error("Failed to query by sql:" + sql + ", with class<" + entityClass.getName() + ">", t);
		}
		return retList;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public int executeSQL(final String sql, final Object... params) throws RuntimeException {
		int retInt = -1;
		try {
			retInt = (Integer) getHibernateTemplate().execute(new HibernateCallback() {
				@Override
				public Object doInHibernate(Session session) throws HibernateException {
					Transaction tx = session.beginTransaction();
					SQLQuery query = session.createSQLQuery(sql);
					for ( int i = 0; i < params.length; i++ ) {
						query.setParameter(i, params[i]);
					}
					int ret = query.executeUpdate();
					tx.commit();
					return ret;
				}
			});
		} catch ( Throwable t ) {
			QLog.error("Failed to execute sql:" + sql);
		}
		return retInt;
	}

	public static void main(String[] args) {
	}

}
