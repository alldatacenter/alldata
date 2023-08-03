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

 package org.apache.ranger.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.SingularAttribute;

import org.apache.commons.lang.StringUtils;
import org.apache.ranger.common.AppConstants;
import org.apache.ranger.common.ContextUtil;
import org.apache.ranger.common.SearchCriteria;
import org.apache.ranger.common.SearchField;
import org.apache.ranger.common.SortField;
import org.apache.ranger.common.SortField.SORT_ORDER;
import org.apache.ranger.common.UserSessionBase;
import org.apache.ranger.entity.XXPortalUser;
import org.apache.ranger.entity.XXPortalUserRole;
import org.apache.ranger.entity.XXServiceDef;
import org.apache.ranger.entity.XXTrxLog;
import org.apache.ranger.entity.view.VXXTrxLog;
import org.apache.ranger.plugin.store.EmbeddedServiceDefsUtil;
import org.apache.ranger.view.VXTrxLog;
import org.apache.ranger.view.VXTrxLogList;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@Scope("singleton")
public class XTrxLogService extends XTrxLogServiceBase<XXTrxLog, VXTrxLog> {
	Long keyadminCount = 0L;

	public XTrxLogService(){
		searchFields.add(new SearchField("attributeName", "obj.attributeName",
				SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.PARTIAL));
		searchFields.add(new SearchField("action", "obj.action",
				SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.PARTIAL));
		searchFields.add(new SearchField("sessionId", "obj.sessionId",
				SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.FULL));
		searchFields.add(new SearchField("startDate", "obj.createTime",
				SearchField.DATA_TYPE.DATE, SearchField.SEARCH_TYPE.GREATER_EQUAL_THAN));	
		searchFields.add(new SearchField("endDate", "obj.createTime",
				SearchField.DATA_TYPE.DATE, SearchField.SEARCH_TYPE.LESS_EQUAL_THAN));	
		searchFields.add(new SearchField("owner", "obj.addedByUserId",
				SearchField.DATA_TYPE.INT_LIST, SearchField.SEARCH_TYPE.FULL));
		searchFields.add(new SearchField("objectClassType", "obj.objectClassType",
				SearchField.DATA_TYPE.INT_LIST, SearchField.SEARCH_TYPE.FULL));
		
		sortFields.add(new SortField("createDate", "obj.createTime", true, SORT_ORDER.DESC));
		}

	@Override
	protected void validateForCreate(VXTrxLog vObj) {}

	@Override
	protected void validateForUpdate(VXTrxLog vObj, XXTrxLog mObj) {}

	@Override
	public VXTrxLogList searchXTrxLogs(SearchCriteria searchCriteria) {
		EntityManager em = daoManager.getEntityManager();
		CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
		CriteriaQuery<VXXTrxLog> selectCQ = criteriaBuilder.createQuery(VXXTrxLog.class);
		Root<VXXTrxLog> rootEntityType = selectCQ.from(VXXTrxLog.class);
		Predicate predicate = generatePredicate(searchCriteria, em, criteriaBuilder, rootEntityType);

		selectCQ.where(predicate);
		if ("asc".equalsIgnoreCase(searchCriteria.getSortType())) {
			selectCQ.orderBy(criteriaBuilder.asc(rootEntityType.get("id")));
		} else {
			selectCQ.orderBy(criteriaBuilder.desc(rootEntityType.get("id")));
		}
		int startIndex = searchCriteria.getStartIndex();
		int pageSize = searchCriteria.getMaxRows();
		List<VXXTrxLog> resultList = em.createQuery(selectCQ).setFirstResult(startIndex).setMaxResults(pageSize)
				.getResultList();

		int maxRowSize = Integer.MAX_VALUE;
		int minRowSize = 0;
		XXServiceDef xxServiceDef = daoManager.getXXServiceDef()
				.findByName(EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_KMS_NAME);
		UserSessionBase session = ContextUtil.getCurrentUserSession();
		if (session != null && session.isKeyAdmin()) {
			resultList = em.createQuery(selectCQ).setFirstResult(minRowSize).setMaxResults(maxRowSize).getResultList();
		}
                if (session != null && session.isAuditKeyAdmin()) {
                        resultList = em.createQuery(selectCQ).setFirstResult(minRowSize).setMaxResults(maxRowSize).getResultList();
                }

		List<VXTrxLog> trxLogList = new ArrayList<VXTrxLog>();
		for (VXXTrxLog xTrxLog : resultList) {
			VXTrxLog trxLog = mapCustomViewToViewObj(xTrxLog);

			if (trxLog.getUpdatedBy() != null) {
				XXPortalUser xXPortalUser = daoManager.getXXPortalUser().getById(Long.parseLong(trxLog.getUpdatedBy()));
				if (xXPortalUser != null) {
					trxLog.setOwner(xXPortalUser.getLoginId());
				}
			}

			trxLogList.add(trxLog);
		}

		List<VXTrxLog> keyAdminTrxLogList = new ArrayList<VXTrxLog>();
                if (session != null && xxServiceDef != null && (session.isKeyAdmin()|| session.isAuditKeyAdmin())) {
			List<VXTrxLog> vXTrxLogs = new ArrayList<VXTrxLog>();
			for (VXTrxLog xTrxLog : trxLogList) {
				int parentObjectClassType = xTrxLog.getParentObjectClassType();
				Long parentObjectId = xTrxLog.getParentObjectId();
				if (parentObjectClassType == AppConstants.CLASS_TYPE_XA_SERVICE_DEF
                                                && parentObjectId.equals(xxServiceDef.getId())) {
					vXTrxLogs.add(xTrxLog);
				} else if (parentObjectClassType == AppConstants.CLASS_TYPE_XA_SERVICE
                                                && !(parentObjectId.equals(xxServiceDef.getId()))) {
					for (VXTrxLog vxTrxLog : trxLogList) {
						if (parentObjectClassType == vxTrxLog.getObjectClassType()
                                                                && parentObjectId.equals(vxTrxLog.getObjectId())
                                                                && vxTrxLog.getParentObjectId().equals(xxServiceDef.getId())) {
							vXTrxLogs.add(xTrxLog);
							break;
						}
					}
				} else if (xTrxLog.getObjectClassType() == AppConstants.CLASS_TYPE_XA_USER
						|| xTrxLog.getObjectClassType() == AppConstants.CLASS_TYPE_RANGER_POLICY
						|| xTrxLog.getObjectClassType() == AppConstants.HIST_OBJ_STATUS_UPDATED) {
					XXPortalUser xxPortalUser = null;
					if (xTrxLog.getUpdatedBy() != null) {
						xxPortalUser = daoManager.getXXPortalUser()
								.getById(Long.parseLong(xTrxLog.getUpdatedBy()));
					}
					if (xxPortalUser != null && xxPortalUser.getId() != null) {
						List<XXPortalUserRole> xxPortalUserRole = daoManager.getXXPortalUserRole()
								.findByUserId(xxPortalUser.getId());
						if (xxPortalUserRole != null
                                                                && (xxPortalUserRole.get(0).getUserRole().equalsIgnoreCase("ROLE_KEY_ADMIN") || xxPortalUserRole.get(0).getUserRole().equalsIgnoreCase("ROLE_KEY_ADMIN_AUDITOR"))) {
							vXTrxLogs.add(xTrxLog);
						}
					}
				}
			}
			keyadminCount = (long) vXTrxLogs.size();
			if (vXTrxLogs != null && !vXTrxLogs.isEmpty()) {
				for (int k = startIndex; k <= pageSize; k++) {
					if (k < vXTrxLogs.size()) {
						keyAdminTrxLogList.add(vXTrxLogs.get(k));
					}
				}
			}
		}

		VXTrxLogList vxTrxLogList = new VXTrxLogList();
		vxTrxLogList.setStartIndex(startIndex);
		vxTrxLogList.setPageSize(pageSize);
                vxTrxLogList.setSortBy(searchCriteria.getSortBy());
                vxTrxLogList.setSortType(searchCriteria.getSortType());
                if (session != null && (session.isKeyAdmin() || session.isAuditKeyAdmin()) ) {
			vxTrxLogList.setVXTrxLogs(keyAdminTrxLogList);
		} else {
			vxTrxLogList.setVXTrxLogs(trxLogList);
		}
		return vxTrxLogList;
	}

	public Long searchXTrxLogsCount(SearchCriteria searchCriteria) {
		EntityManager em = daoManager.getEntityManager();
		CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
		CriteriaQuery<VXXTrxLog> selectCQ = criteriaBuilder.createQuery(VXXTrxLog.class);
		Root<VXXTrxLog> rootEntityType = selectCQ.from(VXXTrxLog.class);
		Predicate predicate = generatePredicate(searchCriteria, em, criteriaBuilder, rootEntityType);

		CriteriaQuery<Long> countCQ = criteriaBuilder.createQuery(Long.class);
		countCQ.select(criteriaBuilder.count(rootEntityType)).where(predicate);
		List<Long> countList = em.createQuery(countCQ).getResultList();
		Long count = 0L;
		if(!CollectionUtils.isEmpty(countList)) {
			count = countList.get(0);
			if(count == null) {
				count = 0L;
			}
		}
		UserSessionBase session = ContextUtil.getCurrentUserSession();
		if (session != null && session.isKeyAdmin()) {
			count = keyadminCount;
		}
                if (session != null && session.isAuditKeyAdmin()) {
                        count = keyadminCount;
                }
		return count;
	}

	private Predicate generatePredicate(SearchCriteria searchCriteria, EntityManager em,
			CriteriaBuilder criteriaBuilder, Root<VXXTrxLog> rootEntityType) {
		Predicate predicate = criteriaBuilder.conjunction();
		Map<String, Object> paramList = searchCriteria.getParamList();
		if (CollectionUtils.isEmpty(paramList)) {
			return predicate;
		}

		Metamodel entityMetaModel = em.getMetamodel();
		EntityType<VXXTrxLog> entityType = entityMetaModel.entity(VXXTrxLog.class);

		for (Map.Entry<String, Object> entry : paramList.entrySet()) {
			String key=entry.getKey();
			for (SearchField searchField : searchFields) {
				if (!key.equalsIgnoreCase(searchField.getClientFieldName())) {
					continue;
				}

				String fieldName = searchField.getFieldName();
				if (!StringUtils.isEmpty(fieldName)) {
					fieldName = fieldName.contains(".") ? fieldName.substring(fieldName.indexOf(".") + 1) : fieldName;
				}

				Object paramValue = entry.getValue();
				boolean isListValue = false;
				if (paramValue != null && paramValue instanceof Collection) {
					isListValue = true;
				}

				// build where clause depending upon given parameters
				if (SearchField.DATA_TYPE.STRING.equals(searchField.getDataType())) {
					// build where clause for String datatypes
					SingularAttribute attr = entityType.getSingularAttribute(fieldName);
					if (attr != null) {
						Predicate stringPredicate = null;
						if (SearchField.SEARCH_TYPE.PARTIAL.equals(searchField.getSearchType())) {
							String val = "%" + paramValue + "%";
							stringPredicate = criteriaBuilder.like(rootEntityType.get(attr), val);
						} else {
							stringPredicate = criteriaBuilder.equal(rootEntityType.get(attr), paramValue);
						}
						predicate = criteriaBuilder.and(predicate, stringPredicate);
					}

				} else if (SearchField.DATA_TYPE.INT_LIST.equals(searchField.getDataType()) || isListValue
						&& SearchField.DATA_TYPE.INTEGER.equals(searchField.getDataType())) {
					// build where clause for integer lists or integers datatypes
					Collection<Number> intValueList = null;
					if (paramValue != null && (paramValue instanceof Integer || paramValue instanceof Long)) {
						intValueList = new ArrayList<Number>();
						intValueList.add((Number) paramValue);
					} else {
						intValueList = (Collection<Number>) paramValue;
					}
					for (Number value : intValueList) {
						SingularAttribute attr = entityType.getSingularAttribute(fieldName);
						if (attr != null) {
							Predicate intPredicate = criteriaBuilder.equal(rootEntityType.get(attr), value);
							predicate = criteriaBuilder.and(predicate, intPredicate);
						}
					}

				} else if (SearchField.DATA_TYPE.DATE.equals(searchField.getDataType())) {
					// build where clause for date datatypes
					Date fieldValue = (Date) paramList.get(searchField.getClientFieldName());
					if (fieldValue != null && searchField.getCustomCondition() == null) {
						SingularAttribute attr = entityType.getSingularAttribute(fieldName);
						Predicate datePredicate = null;
						if (SearchField.SEARCH_TYPE.LESS_THAN.equals(searchField.getSearchType())) {
							datePredicate = criteriaBuilder.lessThan(rootEntityType.get(attr), fieldValue);
						} else if (SearchField.SEARCH_TYPE.LESS_EQUAL_THAN.equals(searchField.getSearchType())) {
							datePredicate = criteriaBuilder.lessThanOrEqualTo(rootEntityType.get(attr), fieldValue);
						} else if (SearchField.SEARCH_TYPE.GREATER_THAN.equals(searchField.getSearchType())) {
							datePredicate = criteriaBuilder.greaterThan(rootEntityType.get(attr), fieldValue);
						} else if (SearchField.SEARCH_TYPE.GREATER_EQUAL_THAN.equals(searchField.getSearchType())) {
							datePredicate = criteriaBuilder.greaterThanOrEqualTo(rootEntityType.get(attr), fieldValue);
						} else {
							datePredicate = criteriaBuilder.equal(rootEntityType.get(attr), fieldValue);
						}
						predicate = criteriaBuilder.and(predicate, datePredicate);
					}
				}
			}
		}
		return predicate;
	}
	
	private VXTrxLog mapCustomViewToViewObj(VXXTrxLog vXXTrxLog){
		VXTrxLog vXTrxLog = new VXTrxLog();
		vXTrxLog.setId(vXXTrxLog.getId());
		vXTrxLog.setAction(vXXTrxLog.getAction());
		vXTrxLog.setAttributeName(vXXTrxLog.getAttributeName());
		vXTrxLog.setCreateDate(vXXTrxLog.getCreateTime());
		vXTrxLog.setNewValue(vXXTrxLog.getNewValue());
		vXTrxLog.setPreviousValue(vXXTrxLog.getPreviousValue());
		vXTrxLog.setSessionId(vXXTrxLog.getSessionId());
		if(vXXTrxLog.getUpdatedByUserId()==null || vXXTrxLog.getUpdatedByUserId()==0){
			vXTrxLog.setUpdatedBy(null);
		}else{
			vXTrxLog.setUpdatedBy(String.valueOf(vXXTrxLog.getUpdatedByUserId()));
		}
		//We will have to get this from XXUser
		//vXTrxLog.setOwner(vXXTrxLog.getAddedByUserName());
		vXTrxLog.setParentObjectId(vXXTrxLog.getParentObjectId());
		vXTrxLog.setParentObjectClassType(vXXTrxLog.getParentObjectClassType());
		vXTrxLog.setParentObjectName(vXXTrxLog.getParentObjectName());
		vXTrxLog.setObjectClassType(vXXTrxLog.getObjectClassType());
		vXTrxLog.setObjectId(vXXTrxLog.getObjectId());
		vXTrxLog.setObjectName(vXXTrxLog.getObjectName());
		vXTrxLog.setTransactionId(vXXTrxLog.getTransactionId());
		return vXTrxLog;
	}
	
	@Override
	protected XXTrxLog mapViewToEntityBean(VXTrxLog vObj, XXTrxLog mObj, int OPERATION_CONTEXT) {
	    XXTrxLog ret = null;
		if(vObj!=null && mObj!=null){
			ret = super.mapViewToEntityBean(vObj, mObj, OPERATION_CONTEXT);
			XXPortalUser xXPortalUser=null;
			if(ret.getAddedByUserId()==null || ret.getAddedByUserId()==0){
				if(!stringUtil.isEmpty(vObj.getOwner())){
					xXPortalUser=daoManager.getXXPortalUser().findByLoginId(vObj.getOwner());
					if(xXPortalUser!=null){
						ret.setAddedByUserId(xXPortalUser.getId());
					}
				}
			}
			if(ret.getUpdatedByUserId()==null || ret.getUpdatedByUserId()==0){
				if(!stringUtil.isEmpty(vObj.getUpdatedBy())){
					xXPortalUser= daoManager.getXXPortalUser().findByLoginId(vObj.getUpdatedBy());
					if(xXPortalUser!=null){
						ret.setUpdatedByUserId(xXPortalUser.getId());
					}		
				}
			}
		}
		return ret;
	}

	@Override
	protected VXTrxLog mapEntityToViewBean(VXTrxLog vObj, XXTrxLog mObj) {
	    VXTrxLog ret = null;
        if(mObj!=null && vObj!=null){
            ret = super.mapEntityToViewBean(vObj, mObj);
            XXPortalUser xXPortalUser=null;
            if(stringUtil.isEmpty(ret.getOwner())){
                xXPortalUser= daoManager.getXXPortalUser().getById(mObj.getAddedByUserId());
                if(xXPortalUser!=null){
                    ret.setOwner(xXPortalUser.getLoginId());
                }
            }
            if(stringUtil.isEmpty(ret.getUpdatedBy())){
                xXPortalUser= daoManager.getXXPortalUser().getById(mObj.getUpdatedByUserId());
                if(xXPortalUser!=null){
                    ret.setUpdatedBy(xXPortalUser.getLoginId());
                }
            }
        }
        return ret;
	}
}
