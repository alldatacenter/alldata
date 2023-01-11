/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

 package org.apache.ranger.common.db;



import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.Table;
import javax.persistence.TypedQuery;

import org.apache.ranger.biz.RangerBizUtil;
import org.apache.ranger.common.AppConstants;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.db.RangerDaoManagerBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseDao<T> {
	private static final Logger logger = LoggerFactory.getLogger(BaseDao.class);

	protected RangerDaoManager daoManager;

	EntityManager em;

	protected Class<T> tClass;

	public BaseDao(RangerDaoManagerBase daoManager) {
		this.daoManager = (RangerDaoManager) daoManager;
		this.init(daoManager.getEntityManager());
	}

	public BaseDao(RangerDaoManagerBase daoManager, String persistenceContextUnit) {
		this.daoManager = (RangerDaoManager) daoManager;

		EntityManager em = this.daoManager.getEntityManager(persistenceContextUnit);

		this.init(em);
	}

	@SuppressWarnings("unchecked")
	private void init(EntityManager em) {
		this.em = em;

		ParameterizedType genericSuperclass = (ParameterizedType) getClass()
				.getGenericSuperclass();

		Type type = genericSuperclass.getActualTypeArguments()[0];

		if (type instanceof ParameterizedType) {
			this.tClass = (Class<T>) ((ParameterizedType) type).getRawType();
		} else {
			this.tClass = (Class<T>) type;
		}
	}

	public EntityManager getEntityManager() {
		return this.em;
	}

	public T create(T obj) {
		T ret = null;

		em.persist(obj);
		if (!RangerBizUtil.isBulkMode()) {
			em.flush();
		}
		ret = obj;
		return ret;
	}

	public List<T> batchCreate(List<T> obj) {
		List<T> ret = null;

		for (int n = 0; n < obj.size(); ++n) {
			em.persist(obj.get(n));
			if (!RangerBizUtil.isBulkMode() && (n % RangerBizUtil.batchPersistSize == 0)) {
				em.flush();
			}
		}
		if (!RangerBizUtil.isBulkMode()) {
			em.flush();
		}

		ret = obj;
		return ret;
	}

	public T update(T obj) {
		em.merge(obj);
		if (!RangerBizUtil.isBulkMode()) {
			em.flush();
		}
		return obj;
	}

	public boolean remove(Long id) {
		return remove(getById(id));
	}

	public boolean remove(T obj) {
		if (obj == null) {
			return true;
		}
		if (!em.contains(obj)) {
			obj = em.merge(obj);
		}
		em.remove(obj);
		if (!RangerBizUtil.isBulkMode()) {
			em.flush();
		}
		return true;
	}

	public void flush() {
		em.flush();
	}

	public void clear() {
		em.clear();
	}
	public T create(T obj, boolean flush) {
		T ret = null;
		em.persist(obj);
		if(flush) {
			em.flush();
		}
		ret = obj;
		return ret;
	}

	public T update(T obj, boolean flush) {
		em.merge(obj);
		if(flush) {
			em.flush();
		}
		return obj;
	}

	public boolean remove(T obj, boolean flush) {
		if (obj == null) {
			return true;
		}
		em.remove(obj);
		if(flush) {
			em.flush();
		}
		return true;
	}

	public T getById(Long id) {
		if (id == null) {
			return null;
		}
		T ret = null;
		try {
			ret = em.find(tClass, id);
		} catch (NoResultException e) {
			return null;
		}
		return ret;
	}

	public List<T> findByNamedQuery(String namedQuery, String paramName,
			Object refId) {
		List<T> ret = new ArrayList<T>();

		if (namedQuery == null) {
			return ret;
		}
		try {
			TypedQuery<T> qry = em.createNamedQuery(namedQuery, tClass);
			qry.setParameter(paramName, refId);
			ret = qry.getResultList();
		} catch (NoResultException e) {
			// ignore
		}
		return ret;
	}

	public List<T> findByParentId(Long parentId) {
		String namedQuery = tClass.getSimpleName() + ".findByParentId";
		return findByNamedQuery(namedQuery, "parentId", parentId);
	}

	
	public List<T> executeQueryInSecurityContext(Class<T> clazz, Query query) {
		return executeQueryInSecurityContext(clazz, query, true);
	}

	@SuppressWarnings("unchecked")
	public List<T> executeQueryInSecurityContext(Class<T> clazz, Query query,
			boolean userPrefFilter) {
		// boolean filterEnabled = false;
		List<T> rtrnList = null;
		// filterEnabled = enableVisiblityFilters(clazz, userPrefFilter);

		rtrnList = query.getResultList();

		return rtrnList;
	}

	public Long executeCountQueryInSecurityContext(Class<T> clazz, Query query) { //NOPMD
		return (Long) query.getSingleResult();
	}
	
	public List<T> getAll() {
		List<T> ret = null;
		TypedQuery<T> qry = em.createQuery(
				"SELECT t FROM " + tClass.getSimpleName() + " t", tClass);
		ret = qry.getResultList();
		return ret;
	}

	public Long getAllCount() {
		Long ret = null;
		TypedQuery<Long> qry = em.createQuery(
				"SELECT count(t) FROM " + tClass.getSimpleName() + " t",
				Long.class);
		ret = qry.getSingleResult();
		return ret;
	}

	public void updateSequence(String seqName, long nextValue) {
		if(RangerBizUtil.getDBFlavor() == AppConstants.DB_FLAVOR_ORACLE) {
			String[] queries = {
					"ALTER SEQUENCE " + seqName + " INCREMENT BY " + (nextValue - 1),
					"select " + seqName + ".nextval from dual",
					"ALTER SEQUENCE " + seqName + " INCREMENT BY 1 NOCACHE NOCYCLE"
			};

			for(String query : queries) {
				getEntityManager().createNativeQuery(query).executeUpdate();
			}
		} else if(RangerBizUtil.getDBFlavor() == AppConstants.DB_FLAVOR_POSTGRES) {
			String query = "SELECT setval('" + seqName + "', " + nextValue + ")";

			getEntityManager().createNativeQuery(query).getSingleResult();
		}

	}

	public void setIdentityInsert(boolean identityInsert) {
		if (RangerBizUtil.getDBFlavor() != AppConstants.DB_FLAVOR_SQLSERVER) {
			logger.debug("Ignoring BaseDao.setIdentityInsert(). This should be executed if DB flavor is sqlserver.");
			return;
		}

		EntityManager entityMgr = getEntityManager();

		String identityInsertStr;
		if (identityInsert) {
			identityInsertStr = "ON";
		} else {
			identityInsertStr = "OFF";
		}

		Table table = tClass.getAnnotation(Table.class);

		if(table == null) {
			throw new NullPointerException("Required annotation `Table` not found");
		}

		String tableName = table.name();

		try {
			entityMgr.unwrap(Connection.class).createStatement().execute("SET IDENTITY_INSERT " + tableName + " " + identityInsertStr);
		} catch (SQLException e) {
			logger.error("Error while settion identity_insert " + identityInsertStr, e);
		}
	}

	public void updateUserIDReference(String paramName,long oldID) {
		Table table = tClass.getAnnotation(Table.class);
		if(table != null) {
			String tableName = table.name();
			String query = "update " + tableName + " set " + paramName+"=null"
					+ " where " +paramName+"=" + oldID;
			int count=getEntityManager().createNativeQuery(query).executeUpdate();
			if(count>0){
				logger.warn(count + " records updated in table '" + tableName + "' with: set " + paramName + "=null where " + paramName + "=" + oldID);
			}
		}else{
			logger.warn("Required annotation `Table` not found");
		}
	}

	public String getDBVersion(){
		String dbVersion="Not Available";
		String query ="SELECT 1";
		try{
			if(RangerBizUtil.getDBFlavor() == AppConstants.DB_FLAVOR_MYSQL) {
				query="SELECT version()";
				dbVersion=(String) getEntityManager().createNativeQuery(query).getSingleResult();
			}else if(RangerBizUtil.getDBFlavor() == AppConstants.DB_FLAVOR_ORACLE){
				query="SELECT * from v$version where rownum<2";
				dbVersion=(String) getEntityManager().createNativeQuery(query).getSingleResult();
			}else if(RangerBizUtil.getDBFlavor() == AppConstants.DB_FLAVOR_POSTGRES){
				query="SELECT version()";
				dbVersion=(String) getEntityManager().createNativeQuery(query).getSingleResult();
			}else if(RangerBizUtil.getDBFlavor() == AppConstants.DB_FLAVOR_SQLSERVER){
				query="SELECT @@version";
				dbVersion=(String) getEntityManager().createNativeQuery(query).getSingleResult();
			}else if(RangerBizUtil.getDBFlavor() == AppConstants.DB_FLAVOR_SQLANYWHERE){
				query="SELECT @@version";
				dbVersion=(String) getEntityManager().createNativeQuery(query).getSingleResult();
			}
		}catch(Exception ex){}
		return dbVersion;
	}
}
