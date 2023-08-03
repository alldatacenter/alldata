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

 package org.apache.ranger.kms.dao;


import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class BaseDao<T> {
    private static final Logger logger = LoggerFactory.getLogger(BaseDao.class);

    protected final DaoManager daoManager;
    protected final Class<T>   tClass;

    @SuppressWarnings("unchecked")
    protected BaseDao(DaoManagerBase daoManager) {
        this.daoManager = (DaoManager) daoManager;

        ParameterizedType genericSuperclass = (ParameterizedType) getClass().getGenericSuperclass();
        Type              type              = genericSuperclass.getActualTypeArguments()[0];

        if (type instanceof ParameterizedType) {
            this.tClass = (Class<T>) ((ParameterizedType) type).getRawType();
        } else {
            this.tClass = (Class<T>) type;
        }
    }

    public EntityManager getEntityManager() {
        return daoManager.getEntityManager();
    }

    public boolean beginTransaction() {
        boolean ret = false;

        EntityManager em = getEntityManager();

        if(em != null) {
            EntityTransaction et = em.getTransaction();

            // check the transaction is not already active
            if(et != null && !et.isActive()) {
                et.begin();

                ret = true;
            }
        }

        return ret;
    }

    public void commitTransaction() {
        EntityManager em = getEntityManager();

        if(em != null) {
            em.flush();

            EntityTransaction et = em.getTransaction();

            if(et != null) {
                et.commit();
            }
        }
    }

    public void rollbackTransaction() {
        EntityManager em = getEntityManager();

        if(em != null) {
            EntityTransaction et = em.getTransaction();

            if(et != null) {
                et.rollback();
            }
        }
    }

    public T create(T obj) {
        T       ret      = null;
        boolean trxBegan = beginTransaction();

        try {
            getEntityManager().persist(obj);

            if(trxBegan) {
                commitTransaction();
            }

            ret = obj;
        }catch(Exception e){
            logger.error("create({}) failed", tClass.getSimpleName(), e);

            rollbackTransaction();
        }

        return ret;
    }

    public T update(T obj) {
        boolean trxBegan = beginTransaction();

        getEntityManager().merge(obj);

        if(trxBegan) {
            commitTransaction();
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

        boolean trxBegan = beginTransaction();

        getEntityManager().remove(obj);

        if(trxBegan) {
            commitTransaction();
        }

        return true;
    }

    public T getById(Long id) {
        if (id == null) {
            return null;
        }

        T ret;

        try {
            ret = getEntityManager().find(tClass, id);
        } catch (NoResultException e) {
            return null;
        }

        return ret;
    }

    public List<T> getAll() {
        List<T>       ret;
        TypedQuery<T> qry = getEntityManager().createQuery("SELECT t FROM " + tClass.getSimpleName() + " t", tClass);

        qry.setHint("eclipselink.refresh", "true");

        ret = qry.getResultList();

        return ret;
    }

    public Long getAllCount() {
        TypedQuery<Long> qry = getEntityManager().createQuery("SELECT count(t) FROM " + tClass.getSimpleName() + " t", Long.class);

        qry.setHint("eclipselink.refresh", "true");

        Long ret = qry.getSingleResult();

        return ret;
    }

    public T getUniqueResult(TypedQuery<T> qry) {
        T ret = null;

        try {
            ret = qry.getSingleResult();
        } catch (NoResultException e) {
            // ignore
        }

        return ret;
    }

    public List<T> executeQuery(TypedQuery<T> qry) {
        List<T> ret = qry.getResultList();

        return ret;
    }

    public List<T> findByNamedQuery(String namedQuery, String paramName, Object refId) {
        List<T> ret = new ArrayList<>();

        if (namedQuery != null) {
            try {
                TypedQuery<T> qry = getEntityManager().createNamedQuery(namedQuery, tClass);

                qry.setParameter(paramName, refId);

                ret = qry.getResultList();
            } catch (NoResultException e) {
                // ignore
            }
        }

        return ret;
    }

    public T findByAlias(String namedQuery, String alias) {
        try {
            return getEntityManager().createNamedQuery(namedQuery, tClass)
                                     .setParameter("alias", alias)
                                     .getSingleResult();
        } catch (NoResultException e) {
            // ignore
        }

        return null;
    }

    public int deleteByAlias(String namedQuery, String alias) {
        int     ret      = 0;
        boolean trxBegan = beginTransaction();

        try {
            ret = getEntityManager().createNamedQuery(namedQuery, tClass)
                                    .setParameter("alias", alias).executeUpdate();

            if(trxBegan) {
                commitTransaction();
            }
        } catch (NoResultException e) {
            logger.error("deleteByAlias({}) failed", alias, e);

            rollbackTransaction();
        }

        return ret;
    }
}
