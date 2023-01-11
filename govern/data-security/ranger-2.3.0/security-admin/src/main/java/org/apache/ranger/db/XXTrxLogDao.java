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

 package org.apache.ranger.db;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.NoResultException;

import org.apache.ranger.common.db.BaseDao;
import org.apache.ranger.entity.XXTrxLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class XXTrxLogDao extends BaseDao<XXTrxLog> {
	private static final Logger logger = LoggerFactory.getLogger(XXTrxLogDao.class);
	
    public XXTrxLogDao( RangerDaoManagerBase daoManager ) {
		super(daoManager);
    }

    public List<XXTrxLog> findByTransactionId(String transactionId){
    	if(transactionId == null){
    		return null;
    	}
    	
		List<XXTrxLog> xTrxLogList = new ArrayList<XXTrxLog>();
		try {
			xTrxLogList = getEntityManager()
					.createNamedQuery("XXTrxLog.findByTrxId", XXTrxLog.class)
					.setParameter("transactionId", transactionId)
					.getResultList();
		} catch (NoResultException e) {
			logger.debug(e.getMessage());
		}
		
		return xTrxLogList;
	}

	public Long findMaxObjIdOfClassType(int classType) {
		
		try {
			return (Long) getEntityManager().createNamedQuery("XXTrxLog.findLogForMaxIdOfClassType")
					.setParameter("classType", classType)
					.getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}
	public Long getMaxIdOfXXTrxLog(){
		Long maxXTrxLogID=Long.valueOf(0);
		try {
			maxXTrxLogID = (Long) getEntityManager()
					.createNamedQuery("XXTrxLog.getMaxIdOfXXTrxLog", Long.class)
					.getSingleResult();
		} catch (NoResultException e) {
			logger.debug(e.getMessage());
		}finally{
			if(maxXTrxLogID==null){
				maxXTrxLogID=Long.valueOf(0);
			}
		}
		return maxXTrxLogID;
	}

	public int updateXTrxLog(long idFrom,long idTo,int objClassType,String attrName,String newValue){
		int rowAffected=-1;
		if(objClassType == 0 ||attrName==null || newValue==null){
			return rowAffected;
		}
		try {
		//idFrom and idTo both exclusive
		rowAffected=getEntityManager().createNamedQuery("XXTrxLog.updateLogAttr", tClass)
	        .setParameter("idFrom", idFrom)
	        .setParameter("idTo", idTo)
	        .setParameter("objClassType", objClassType)
	        .setParameter("attrName", attrName)
	        .setParameter("newValue", newValue)
	        .executeUpdate();
		}catch (NoResultException e) {
			logger.debug(e.getMessage());
		}
		return rowAffected;
	}

}

