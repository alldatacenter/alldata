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

package org.apache.ranger.biz;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import javax.annotation.PostConstruct;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.ranger.audit.provider.MiscUtil;
import org.apache.ranger.authorization.hadoop.config.RangerAdminConfig;
import org.apache.ranger.authorization.utils.JsonUtils;
import org.apache.ranger.common.AppConstants;
import org.apache.ranger.common.ContextUtil;
import org.apache.ranger.common.GUIDUtil;
import org.apache.ranger.common.MessageEnums;
import org.apache.ranger.common.RangerCommonEnums;
import org.apache.ranger.common.db.RangerTransactionSynchronizationAdapter;
import org.apache.ranger.db.XXGlobalStateDao;
import org.apache.ranger.db.XXPolicyDao;
import org.apache.ranger.entity.XXTagChangeLog;
import org.apache.ranger.plugin.model.RangerSecurityZone;
import org.apache.ranger.plugin.util.RangerCommonConstants;
import org.apache.ranger.plugin.util.ServiceTags;
import org.apache.ranger.plugin.model.validation.RangerServiceDefValidator;
import org.apache.ranger.plugin.model.validation.RangerValidator;
import org.apache.ranger.plugin.model.validation.ValidationFailureDetails;
import org.apache.ranger.plugin.model.RangerPolicyDelta;
import org.apache.ranger.plugin.policyengine.RangerPolicyEngine;
import org.apache.ranger.plugin.policyresourcematcher.RangerDefaultPolicyResourceMatcher;
import org.apache.ranger.plugin.policyresourcematcher.RangerPolicyResourceMatcher;
import org.apache.ranger.plugin.service.RangerBaseService;
import org.apache.ranger.plugin.store.ServiceStore;
import org.apache.ranger.plugin.util.PasswordUtils;
import org.apache.ranger.common.DateUtil;
import org.apache.ranger.common.JSONUtil;
import org.apache.ranger.common.PropertiesUtil;
import org.apache.ranger.common.RESTErrorUtil;
import org.apache.ranger.common.RangerConstants;
import org.apache.ranger.common.RangerFactory;
import org.apache.ranger.common.RangerServicePoliciesCache;
import org.apache.ranger.common.RangerVersionInfo;
import org.apache.ranger.common.SearchCriteria;
import org.apache.ranger.common.StringUtil;
import org.apache.ranger.common.UserSessionBase;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.db.XXAccessTypeDefDao;
import org.apache.ranger.db.XXAccessTypeDefGrantsDao;
import org.apache.ranger.db.XXContextEnricherDefDao;
import org.apache.ranger.db.XXDataMaskTypeDefDao;
import org.apache.ranger.db.XXEnumDefDao;
import org.apache.ranger.db.XXEnumElementDefDao;
import org.apache.ranger.db.XXPolicyConditionDefDao;
import org.apache.ranger.db.XXPolicyLabelMapDao;
import org.apache.ranger.db.XXResourceDefDao;
import org.apache.ranger.db.XXServiceConfigDefDao;
import org.apache.ranger.db.XXServiceConfigMapDao;
import org.apache.ranger.db.XXServiceDao;
import org.apache.ranger.db.XXServiceVersionInfoDao;
import org.apache.ranger.entity.XXAccessTypeDef;
import org.apache.ranger.entity.XXAccessTypeDefGrants;
import org.apache.ranger.entity.XXContextEnricherDef;
import org.apache.ranger.entity.XXDataHist;
import org.apache.ranger.entity.XXDataMaskTypeDef;
import org.apache.ranger.entity.XXEnumDef;
import org.apache.ranger.entity.XXEnumElementDef;
import org.apache.ranger.entity.XXGroup;
import org.apache.ranger.entity.XXPolicy;
import org.apache.ranger.entity.XXPolicyChangeLog;
import org.apache.ranger.entity.XXPolicyConditionDef;
import org.apache.ranger.entity.XXPolicyLabel;
import org.apache.ranger.entity.XXPolicyLabelMap;
import org.apache.ranger.entity.XXPolicyRefAccessType;
import org.apache.ranger.entity.XXPolicyRefCondition;
import org.apache.ranger.entity.XXPolicyRefResource;
import org.apache.ranger.entity.XXPortalUser;
import org.apache.ranger.entity.XXResourceDef;
import org.apache.ranger.entity.XXSecurityZone;
import org.apache.ranger.entity.XXService;
import org.apache.ranger.entity.XXServiceConfigDef;
import org.apache.ranger.entity.XXServiceConfigMap;
import org.apache.ranger.entity.XXServiceDef;
import org.apache.ranger.entity.XXServiceVersionInfo;
import org.apache.ranger.entity.XXTrxLog;
import org.apache.ranger.entity.XXUser;
import org.apache.ranger.plugin.model.AuditFilter;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicy.RangerDataMaskPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemAccess;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemCondition;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyResource;
import org.apache.ranger.plugin.model.RangerPolicy.RangerRowFilterPolicyItem;
import org.apache.ranger.plugin.model.RangerSecurityZone.RangerSecurityZoneService;
import org.apache.ranger.plugin.model.RangerPolicyResourceSignature;
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerAccessTypeDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerContextEnricherDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerDataMaskDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerDataMaskTypeDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerEnumDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerEnumElementDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerPolicyConditionDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerResourceDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerRowFilterDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerServiceConfigDef;
import org.apache.ranger.plugin.model.validation.RangerServiceDefHelper;
import org.apache.ranger.plugin.store.AbstractServiceStore;
import org.apache.ranger.plugin.store.EmbeddedServiceDefsUtil;
import org.apache.ranger.plugin.store.PList;
import org.apache.ranger.plugin.store.ServicePredicateUtil;
import org.apache.ranger.plugin.util.RangerPolicyDeltaUtil;
import org.apache.ranger.plugin.util.SearchFilter;
import org.apache.ranger.plugin.util.ServicePolicies;
import org.apache.ranger.rest.ServiceREST;
import org.apache.ranger.rest.TagREST;
import org.apache.ranger.service.RangerAuditFields;
import org.apache.ranger.service.RangerDataHistService;
import org.apache.ranger.service.RangerPolicyLabelsService;
import org.apache.ranger.service.RangerPolicyService;
import org.apache.ranger.service.RangerPolicyWithAssignedIdService;
import org.apache.ranger.service.RangerRoleService;
import org.apache.ranger.service.RangerSecurityZoneServiceService;
import org.apache.ranger.service.RangerServiceDefService;
import org.apache.ranger.service.RangerServiceDefWithAssignedIdService;
import org.apache.ranger.service.RangerServiceService;
import org.apache.ranger.service.RangerServiceWithAssignedIdService;
import org.apache.ranger.service.XGroupService;
import org.apache.ranger.service.XUserService;
import org.apache.ranger.util.RestUtil;
import org.apache.ranger.view.RangerExportPolicyList;
import org.apache.ranger.view.RangerPolicyList;
import org.apache.ranger.view.RangerServiceDefList;
import org.apache.ranger.view.RangerServiceList;
import org.apache.ranger.view.VXAccessAuditList;
import org.apache.ranger.view.VXGroup;
import org.apache.ranger.view.VXGroupList;
import org.apache.ranger.view.VXMetricAuditDetailsCount;
import org.apache.ranger.view.VXMetricContextEnricher;
import org.apache.ranger.view.VXMetricPolicyWithServiceNameCount;
import org.apache.ranger.view.VXMetricServiceCount;
import org.apache.ranger.view.VXMetricServiceNameCount;
import org.apache.ranger.view.VXMetricUserGroupCount;
import org.apache.ranger.view.VXPolicyLabelList;
import org.apache.ranger.view.VXPortalUser;
import org.apache.ranger.view.VXString;
import org.apache.ranger.view.VXUser;
import org.apache.ranger.view.VXUserList;
import org.codehaus.jettison.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Component
public class ServiceDBStore extends AbstractServiceStore {
	private static final Logger LOG = LoggerFactory.getLogger(ServiceDBStore.class);

	private static final String POLICY_ALLOW_EXCLUDE = "Policy Allow:Exclude";

	private static final String POLICY_ALLOW_INCLUDE = "Policy Allow:Include";
	private static final String POLICY_DENY_EXCLUDE  = "Policy Deny:Exclude";
	private static final String POLICY_DENY_INCLUDE  = "Policy Deny:Include";

    private static final String POLICY_TYPE_ACCESS = "Access";
    private static final String POLICY_TYPE_DATAMASK  = "Masking";
    private static final String POLICY_TYPE_ROWFILTER = "Row Level Filter";

	private static       String LOCAL_HOSTNAME;
	private static final String HOSTNAME       = "Host name";
	private static final String USER_NAME      = "Exported by";
	private static final String RANGER_VERSION = "Ranger apache version";
	private static final String TIMESTAMP      = "Export time";

    private static final String SERVICE_CHECK_USER = "service.check.user";
    private static final String AMBARI_SERVICE_CHECK_USER = "ambari.service.check.user";
	public static final String SERVICE_ADMIN_USERS     = "service.admin.users";

	private static boolean isRolesDownloadedByService = false;

	public static final String  CRYPT_ALGO      = PropertiesUtil.getProperty("ranger.password.encryption.algorithm", PasswordUtils.DEFAULT_CRYPT_ALGO);
	public static final String  ENCRYPT_KEY     = PropertiesUtil.getProperty("ranger.password.encryption.key", PasswordUtils.DEFAULT_ENCRYPT_KEY);
	public static final String  SALT            = PropertiesUtil.getProperty("ranger.password.salt", PasswordUtils.DEFAULT_SALT);
	public static final Integer ITERATION_COUNT = PropertiesUtil.getIntProperty("ranger.password.iteration.count", PasswordUtils.DEFAULT_ITERATION_COUNT);
	public static boolean SUPPORTS_POLICY_DELTAS = false;
	public static boolean SUPPORTS_IN_PLACE_POLICY_UPDATES = false;
	public static Integer RETENTION_PERIOD_IN_DAYS = 7;
	public static Integer TAG_RETENTION_PERIOD_IN_DAYS = 3;

	private static final String RANGER_PLUGIN_CONFIG_PREFIX = "ranger.plugin.";
	public static final String RANGER_PLUGIN_AUDIT_FILTERS  = "ranger.plugin.audit.filters";

	static {
		try {
			LOCAL_HOSTNAME = java.net.InetAddress.getLocalHost().getCanonicalHostName();
		} catch (UnknownHostException e) {
			LOCAL_HOSTNAME = "unknown";
		}
	}

	@Autowired
	RangerServiceDefService serviceDefService;

	@Autowired
	RangerDaoManager daoMgr;

	@Autowired
	RESTErrorUtil restErrorUtil;
	
	@Autowired
	RangerServiceService svcService;
	
	@Autowired
	StringUtil stringUtil;
	
	@Autowired
	RangerAuditFields<?> rangerAuditFields;
	
	@Autowired
	RangerPolicyService policyService;
	
	@Autowired
        RangerPolicyLabelsService<XXPolicyLabel, ?> policyLabelsService;

	@Autowired
	XUserService xUserService;

	@Autowired
	XUserMgr xUserMgr;

	@Autowired
	XGroupService xGroupService;

	@Autowired
	PolicyRefUpdater policyRefUpdater;

	@Autowired
	RangerDataHistService dataHistService;

	@Autowired
	@Qualifier(value = "transactionManager")
	PlatformTransactionManager txManager;

	@Autowired
	RangerBizUtil bizUtil;

	@Autowired
	RangerPolicyWithAssignedIdService assignedIdPolicyService;

	@Autowired
	RangerServiceWithAssignedIdService svcServiceWithAssignedId;

	@Autowired
	RangerServiceDefWithAssignedIdService svcDefServiceWithAssignedId;

	@Autowired
	RangerFactory factory;

	@Autowired
	JSONUtil jsonUtil;

	@Autowired
	ServiceMgr serviceMgr;

	@Autowired
	AssetMgr assetMgr;

	@Autowired
	RangerTransactionSynchronizationAdapter transactionSynchronizationAdapter;

	@Autowired
	RangerSecurityZoneServiceService securityZoneService;

	@Autowired
	RoleDBStore roleStore;

	@Autowired
	RangerRoleService roleService;

	@Autowired
	UserMgr userMgr;

	@Autowired
	SecurityZoneDBStore securityZoneStore;

	@Autowired
	GUIDUtil guidUtil;

	private static volatile boolean legacyServiceDefsInitDone = false;
	private Boolean populateExistingBaseFields = false;

	public static final String HIDDEN_PASSWORD_STR = "*****";
	public static final String CONFIG_KEY_PASSWORD = "password";
	public static final String ACCESS_TYPE_DECRYPT_EEK    = "decrypteek";
	public static final String ACCESS_TYPE_GENERATE_EEK   = "generateeek";
	public static final String ACCESS_TYPE_GET_METADATA   = "getmetadata";

	private ServicePredicateUtil predicateUtil = null;
	private RangerAdminConfig    config = null;


	@Override
	public void init() throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceDBStore.init()");
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== ServiceDBStore.init()");
		}
	}

	@PostConstruct
	public void initStore() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceDBStore.initStore()");
		}

		config = RangerAdminConfig.getInstance();

		if(! legacyServiceDefsInitDone) {
			synchronized(ServiceDBStore.class) {
				if(!legacyServiceDefsInitDone) {

					SUPPORTS_POLICY_DELTAS       = config.getBoolean("ranger.admin" + RangerCommonConstants.RANGER_ADMIN_SUFFIX_POLICY_DELTA, RangerCommonConstants.RANGER_ADMIN_SUFFIX_POLICY_DELTA_DEFAULT);
					RETENTION_PERIOD_IN_DAYS     = config.getInt("ranger.admin.delta.retention.time.in.days", 7);
					TAG_RETENTION_PERIOD_IN_DAYS = config.getInt("ranger.admin.tag.delta.retention.time.in.days", 3);
					isRolesDownloadedByService   = config.getBoolean("ranger.support.for.service.specific.role.download", false);
					SUPPORTS_IN_PLACE_POLICY_UPDATES    = SUPPORTS_POLICY_DELTAS && config.getBoolean("ranger.admin" + RangerCommonConstants.RANGER_ADMIN_SUFFIX_IN_PLACE_POLICY_UPDATES, RangerCommonConstants.RANGER_ADMIN_SUFFIX_IN_PLACE_POLICY_UPDATES_DEFAULT);

					TransactionTemplate txTemplate = new TransactionTemplate(txManager);

					final ServiceDBStore dbStore = this;
					predicateUtil = new ServicePredicateUtil(dbStore);

					try {
						txTemplate.execute(new TransactionCallback<Object>() {
							@Override
							public Object doInTransaction(TransactionStatus status) {
								EmbeddedServiceDefsUtil.instance().init(dbStore);
								getServiceUpgraded();
								createGenericUsers();
								resetPolicyUpdateLog(RETENTION_PERIOD_IN_DAYS, RangerPolicyDelta.CHANGE_TYPE_RANGER_ADMIN_START);
								resetTagUpdateLog(TAG_RETENTION_PERIOD_IN_DAYS, ServiceTags.TagsChangeType.RANGER_ADMIN_START);
								//createUnzonedSecurityZone();
								initRMSDaos();
								return null;
							}
						});
					} catch (Throwable ex) {
						LOG.error("ServiceDBStore.initStore(): Failed to update DB: " + ex);
					}

					legacyServiceDefsInitDone = true;
				}
			}
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== ServiceDBStore.initStore()");
		}
	}

	@Override
	public RangerServiceDef createServiceDef(RangerServiceDef serviceDef) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceDBStore.createServiceDef(" + serviceDef + ")");
		}

		XXServiceDef xServiceDef = daoMgr.getXXServiceDef().findByName(
				serviceDef.getName());
		if (xServiceDef != null) {
			throw restErrorUtil.createRESTException("service-def with name: "
					+ serviceDef.getName() + " already exists",
					MessageEnums.ERROR_DUPLICATE_OBJECT);
		}

		List<RangerServiceConfigDef> configs = serviceDef.getConfigs();
		List<RangerResourceDef> resources = serviceDef.getResources();

		if (CollectionUtils.isNotEmpty(resources)) {
			RangerServiceDefValidator validator = new RangerServiceDefValidator(this);
			List<ValidationFailureDetails> failures = new ArrayList<>();
			boolean isValidResources = validator.isValidResources(serviceDef, failures, RangerValidator.Action.CREATE);
			if (!isValidResources) {
				throw restErrorUtil.createRESTException("service-def with name: "
								+ serviceDef.getName() + " has invalid resources:[" + failures.toString() + "]",
						MessageEnums.INVALID_INPUT_DATA);
			}
		}

		List<RangerAccessTypeDef> accessTypes = serviceDef.getAccessTypes();
		List<RangerPolicyConditionDef> policyConditions = serviceDef.getPolicyConditions();
		List<RangerContextEnricherDef> contextEnrichers = serviceDef.getContextEnrichers();
		List<RangerEnumDef> enums = serviceDef.getEnums();
		RangerDataMaskDef           dataMaskDef          = serviceDef.getDataMaskDef();
		RangerRowFilterDef          rowFilterDef         = serviceDef.getRowFilterDef();
		List<RangerDataMaskTypeDef> dataMaskTypes        = dataMaskDef == null || dataMaskDef.getMaskTypes() == null ? new ArrayList<RangerDataMaskTypeDef>() : dataMaskDef.getMaskTypes();
		List<RangerAccessTypeDef>   dataMaskAccessTypes  = dataMaskDef == null || dataMaskDef.getAccessTypes() == null ? new ArrayList<RangerAccessTypeDef>() : dataMaskDef.getAccessTypes();
		List<RangerResourceDef>     dataMaskResources    = dataMaskDef == null || dataMaskDef.getResources() == null ? new ArrayList<RangerResourceDef>() : dataMaskDef.getResources();
		List<RangerAccessTypeDef>   rowFilterAccessTypes = rowFilterDef == null || rowFilterDef.getAccessTypes() == null ? new ArrayList<RangerAccessTypeDef>() : rowFilterDef.getAccessTypes();
		List<RangerResourceDef>     rowFilterResources   = rowFilterDef == null || rowFilterDef.getResources() == null ? new ArrayList<RangerResourceDef>() : rowFilterDef.getResources();

		RangerServiceDefHelper defHelper = new RangerServiceDefHelper(serviceDef, false);
		defHelper.patchServiceDefWithDefaultValues();

		// While creating, value of version should be 1.
		serviceDef.setVersion(Long.valueOf(1));
		
		if (populateExistingBaseFields) {
			svcDefServiceWithAssignedId.setPopulateExistingBaseFields(true);
			daoMgr.getXXServiceDef().setIdentityInsert(true);

			svcDefServiceWithAssignedId.create(serviceDef);

			svcDefServiceWithAssignedId.setPopulateExistingBaseFields(false);
			daoMgr.getXXServiceDef().updateSequence();
			daoMgr.getXXServiceDef().setIdentityInsert(false);
		} else {
			// following fields will be auto populated
			serviceDef.setId(null);
			serviceDef.setCreateTime(null);
			serviceDef.setUpdateTime(null);
			serviceDef = serviceDefService.create(serviceDef);
		}
		Long serviceDefId = serviceDef.getId();
		XXServiceDef createdSvcDef = daoMgr.getXXServiceDef().getById(serviceDefId);
		
		XXServiceConfigDefDao xxServiceConfigDao = daoMgr.getXXServiceConfigDef();
		for(int i = 0; i < configs.size(); i++) {
			RangerServiceConfigDef config = configs.get(i);

			XXServiceConfigDef xConfig = new XXServiceConfigDef();
			xConfig = serviceDefService.populateRangerServiceConfigDefToXX(config, xConfig, createdSvcDef,
					RangerServiceDefService.OPERATION_CREATE_CONTEXT);
			xConfig.setOrder(i);
			xConfig = xxServiceConfigDao.create(xConfig);
		}
		
		XXResourceDefDao xxResDefDao = daoMgr.getXXResourceDef();
		for(int i = 0; i < resources.size(); i++) {
			RangerResourceDef resource = resources.get(i);

			XXResourceDef parent = xxResDefDao.findByNameAndServiceDefId(resource.getParent(), serviceDefId);
			Long parentId = (parent != null) ? parent.getId() : null;
			
			XXResourceDef xResource = new XXResourceDef();
			xResource = serviceDefService.populateRangerResourceDefToXX(resource, xResource, createdSvcDef,
					RangerServiceDefService.OPERATION_CREATE_CONTEXT);
			xResource.setOrder(i);
			xResource.setParent(parentId);
			xResource = xxResDefDao.create(xResource);
		}
		
		XXAccessTypeDefDao xxATDDao = daoMgr.getXXAccessTypeDef();
		for(int i = 0; i < accessTypes.size(); i++) {
			RangerAccessTypeDef accessType = accessTypes.get(i);

			XXAccessTypeDef xAccessType = new XXAccessTypeDef();
			xAccessType = serviceDefService.populateRangerAccessTypeDefToXX(accessType, xAccessType, createdSvcDef,
					RangerServiceDefService.OPERATION_CREATE_CONTEXT);
			xAccessType.setOrder(i);
			xAccessType = xxATDDao.create(xAccessType);
			
			Collection<String> impliedGrants = accessType.getImpliedGrants();
			XXAccessTypeDefGrantsDao xxATDGrantDao = daoMgr.getXXAccessTypeDefGrants();
			for(String impliedGrant : impliedGrants) {
				XXAccessTypeDefGrants xImpliedGrant = new XXAccessTypeDefGrants();
				xImpliedGrant.setAtdId(xAccessType.getId());
				xImpliedGrant.setImpliedGrant(impliedGrant);
				xImpliedGrant = xxATDGrantDao.create(xImpliedGrant);
			}
		}
		
		XXPolicyConditionDefDao xxPolCondDao = daoMgr.getXXPolicyConditionDef();
		for (int i = 0; i < policyConditions.size(); i++) {
			RangerPolicyConditionDef policyCondition = policyConditions.get(i);

			XXPolicyConditionDef xPolicyCondition = new XXPolicyConditionDef();
			xPolicyCondition = serviceDefService
					.populateRangerPolicyConditionDefToXX(policyCondition,
							xPolicyCondition, createdSvcDef, RangerServiceDefService.OPERATION_CREATE_CONTEXT);
			xPolicyCondition.setOrder(i);
			xPolicyCondition = xxPolCondDao.create(xPolicyCondition);
		}
		
		XXContextEnricherDefDao xxContextEnricherDao = daoMgr.getXXContextEnricherDef();
		for (int i = 0; i < contextEnrichers.size(); i++) {
			RangerContextEnricherDef contextEnricher = contextEnrichers.get(i);

			XXContextEnricherDef xContextEnricher = new XXContextEnricherDef();
			xContextEnricher = serviceDefService
					.populateRangerContextEnricherDefToXX(contextEnricher,
							xContextEnricher, createdSvcDef, RangerServiceDefService.OPERATION_CREATE_CONTEXT);
			xContextEnricher.setOrder(i);
			xContextEnricher = xxContextEnricherDao.create(xContextEnricher);
		}
		
		XXEnumDefDao xxEnumDefDao = daoMgr.getXXEnumDef();
		for(RangerEnumDef vEnum : enums) {
			XXEnumDef xEnum = new XXEnumDef();
			xEnum = serviceDefService.populateRangerEnumDefToXX(vEnum, xEnum, createdSvcDef, RangerServiceDefService.OPERATION_CREATE_CONTEXT);
			xEnum = xxEnumDefDao.create(xEnum);
			
			List<RangerEnumElementDef> elements = vEnum.getElements();
			XXEnumElementDefDao xxEnumEleDefDao = daoMgr.getXXEnumElementDef();
			for(int i = 0; i < elements.size(); i++) {
				RangerEnumElementDef element = elements.get(i);

				XXEnumElementDef xElement = new XXEnumElementDef();
				xElement = serviceDefService.populateRangerEnumElementDefToXX(element, xElement, xEnum, RangerServiceDefService.OPERATION_CREATE_CONTEXT);
				xElement.setOrder(i);
				xElement = xxEnumEleDefDao.create(xElement);
			}
		}

		XXDataMaskTypeDefDao xxDataMaskDefDao = daoMgr.getXXDataMaskTypeDef();
		for (int i = 0; i < dataMaskTypes.size(); i++) {
			RangerDataMaskTypeDef dataMask = dataMaskTypes.get(i);

			XXDataMaskTypeDef xDataMaskDef = new XXDataMaskTypeDef();
			xDataMaskDef = serviceDefService.populateRangerDataMaskDefToXX(dataMask, xDataMaskDef, createdSvcDef,
					RangerServiceDefService.OPERATION_CREATE_CONTEXT);
			xDataMaskDef.setOrder(i);
			xDataMaskDef = xxDataMaskDefDao.create(xDataMaskDef);
		}

		List<XXAccessTypeDef> xxAccessTypeDefs = xxATDDao.findByServiceDefId(createdSvcDef.getId());

		for(RangerAccessTypeDef accessType : dataMaskAccessTypes) {
			if(! isAccessTypeInList(accessType.getName(), xxAccessTypeDefs)) {
				throw restErrorUtil.createRESTException("accessType with name: "
								+ accessType.getName() + " does not exists", MessageEnums.DATA_NOT_FOUND);
			}
		}

		for(RangerAccessTypeDef accessType : rowFilterAccessTypes) {
			if(! isAccessTypeInList(accessType.getName(), xxAccessTypeDefs)) {
				throw restErrorUtil.createRESTException("accessType with name: "
						+ accessType.getName() + " does not exists", MessageEnums.DATA_NOT_FOUND);
			}
		}

		for(XXAccessTypeDef xxAccessTypeDef : xxAccessTypeDefs) {
			String dataMaskOptions  = null;
			String rowFilterOptions = null;

			for(RangerAccessTypeDef accessTypeDef : dataMaskAccessTypes) {
				if(StringUtils.equals(accessTypeDef.getName(), xxAccessTypeDef.getName())) {
					dataMaskOptions = svcDefServiceWithAssignedId.objectToJson(accessTypeDef);
					break;
				}
			}

			for(RangerAccessTypeDef accessTypeDef : rowFilterAccessTypes) {
				if(StringUtils.equals(accessTypeDef.getName(), xxAccessTypeDef.getName())) {
					rowFilterOptions = svcDefServiceWithAssignedId.objectToJson(accessTypeDef);
					break;
				}
			}

			if(!StringUtils.equals(dataMaskOptions, xxAccessTypeDef.getDataMaskOptions()) ||
			   !StringUtils.equals(rowFilterOptions, xxAccessTypeDef.getRowFilterOptions())) {
				xxAccessTypeDef.setDataMaskOptions(dataMaskOptions);
				xxAccessTypeDef.setRowFilterOptions(rowFilterOptions);

				xxATDDao.update(xxAccessTypeDef);
			}
		}

		List<XXResourceDef> xxResourceDefs = xxResDefDao.findByServiceDefId(createdSvcDef.getId());

		for(RangerResourceDef resource : dataMaskResources) {
			if(! isResourceInList(resource.getName(), xxResourceDefs)) {
				throw restErrorUtil.createRESTException("resource with name: "
						+ resource.getName() + " does not exists", MessageEnums.DATA_NOT_FOUND);
			}
		}

		for(RangerResourceDef resource : rowFilterResources) {
			if(! isResourceInList(resource.getName(), xxResourceDefs)) {
				throw restErrorUtil.createRESTException("resource with name: "
						+ resource.getName() + " does not exists", MessageEnums.DATA_NOT_FOUND);
			}
		}

		for(XXResourceDef xxResourceDef : xxResourceDefs) {
			String dataMaskOptions  = null;
			String rowFilterOptions = null;

			for(RangerResourceDef resource : dataMaskResources) {
				if(StringUtils.equals(resource.getName(), xxResourceDef.getName())) {
					dataMaskOptions = svcDefServiceWithAssignedId.objectToJson(resource);
					break;
				}
			}

			for(RangerResourceDef resource : rowFilterResources) {
				if(StringUtils.equals(resource.getName(), xxResourceDef.getName())) {
					rowFilterOptions = svcDefServiceWithAssignedId.objectToJson(resource);
					break;
				}
			}

			if(!StringUtils.equals(dataMaskOptions, xxResourceDef.getDataMaskOptions()) ||
			   !StringUtils.equals(rowFilterOptions, xxResourceDef.getRowFilterOptions())) {
				xxResourceDef.setDataMaskOptions(dataMaskOptions);
				xxResourceDef.setRowFilterOptions(rowFilterOptions);

				xxResDefDao.update(xxResourceDef);
			}
		}

		RangerServiceDef createdServiceDef = serviceDefService.getPopulatedViewObject(createdSvcDef);
		dataHistService.createObjectDataHistory(createdServiceDef, RangerDataHistService.ACTION_CREATE);

		postCreate(createdServiceDef);

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== ServiceDBStore.createServiceDef(" + serviceDef + "): " + createdServiceDef);
		}

		return createdServiceDef;
	}

	@Override
	public RangerServiceDef updateServiceDef(RangerServiceDef serviceDef) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceDBStore.updateServiceDef(" + serviceDef + ")");
		}

		Long serviceDefId = serviceDef.getId();

		XXServiceDef existing = daoMgr.getXXServiceDef().getById(serviceDefId);
		if (existing == null) {
			throw restErrorUtil.createRESTException("no service-def exists with ID=" + serviceDef.getId(),
					MessageEnums.DATA_NOT_FOUND);
		}

		String existingName = existing.getName();

		boolean renamed = !StringUtils.equalsIgnoreCase(serviceDef.getName(), existingName);

		if (renamed) {
			XXServiceDef renamedSVCDef = daoMgr.getXXServiceDef().findByName(serviceDef.getName());

			if (renamedSVCDef != null) {
				throw restErrorUtil.createRESTException(
						"another service-def already exists with name '" + serviceDef.getName() + "'. ID="
								+ renamedSVCDef.getId(), MessageEnums.DATA_NOT_UPDATABLE);
			}
		}

		List<RangerServiceConfigDef> configs 			= serviceDef.getConfigs() != null 			? serviceDef.getConfigs()   		  : new ArrayList<RangerServiceConfigDef>();
		List<RangerResourceDef> resources 				= serviceDef.getResources() != null  		? serviceDef.getResources() 		  : new ArrayList<RangerResourceDef>();
		List<RangerAccessTypeDef> accessTypes 			= serviceDef.getAccessTypes() != null 		? serviceDef.getAccessTypes() 	  	  : new ArrayList<RangerAccessTypeDef>();
		List<RangerPolicyConditionDef> policyConditions = serviceDef.getPolicyConditions() != null 	? serviceDef.getPolicyConditions() 	  : new ArrayList<RangerPolicyConditionDef>();
		List<RangerContextEnricherDef> contextEnrichers = serviceDef.getContextEnrichers() != null 	? serviceDef.getContextEnrichers() 	  : new ArrayList<RangerContextEnricherDef>();
		List<RangerEnumDef> enums 						= serviceDef.getEnums() != null 			? serviceDef.getEnums() 			  : new ArrayList<RangerEnumDef>();
		RangerDataMaskDef dataMaskDef                   = serviceDef.getDataMaskDef();
		RangerRowFilterDef rowFilterDef                 = serviceDef.getRowFilterDef();

		RangerServiceDefHelper defHelper = new RangerServiceDefHelper(serviceDef, false);
		defHelper.patchServiceDefWithDefaultValues();

		serviceDef.setCreateTime(existing.getCreateTime());
		serviceDef.setGuid(existing.getGuid());
		serviceDef.setVersion(existing.getVersion());

		serviceDef = serviceDefService.update(serviceDef);
		XXServiceDef createdSvcDef = daoMgr.getXXServiceDef().getById(serviceDefId);

		updateChildObjectsOfServiceDef(createdSvcDef, configs, resources, accessTypes, policyConditions, contextEnrichers, enums, dataMaskDef, rowFilterDef);

		RangerServiceDef updatedSvcDef = getServiceDef(serviceDefId);
		dataHistService.createObjectDataHistory(updatedSvcDef, RangerDataHistService.ACTION_UPDATE);

		postUpdate(updatedSvcDef);


		if (LOG.isDebugEnabled()) {
			LOG.debug("<== ServiceDBStore.updateServiceDef(" + serviceDef + "): " + serviceDef);
		}

		return updatedSvcDef;
	}

	private void updateChildObjectsOfServiceDef(XXServiceDef createdSvcDef, List<RangerServiceConfigDef> configs,
			List<RangerResourceDef> resources, List<RangerAccessTypeDef> accessTypes,
			List<RangerPolicyConditionDef> policyConditions, List<RangerContextEnricherDef> contextEnrichers,
			List<RangerEnumDef> enums, RangerDataMaskDef dataMaskDef, RangerRowFilterDef rowFilterDef) {

		Long serviceDefId = createdSvcDef.getId();

		List<XXServiceConfigDef> xxConfigs = daoMgr.getXXServiceConfigDef().findByServiceDefId(serviceDefId);
		List<XXResourceDef> xxResources = daoMgr.getXXResourceDef().findByServiceDefId(serviceDefId);
		List<XXAccessTypeDef> xxAccessTypes = daoMgr.getXXAccessTypeDef().findByServiceDefId(serviceDefId);
		List<XXPolicyConditionDef> xxPolicyConditions = daoMgr.getXXPolicyConditionDef().findByServiceDefId(
				serviceDefId);
		List<XXContextEnricherDef> xxContextEnrichers = daoMgr.getXXContextEnricherDef().findByServiceDefId(
				serviceDefId);
		List<XXEnumDef> xxEnums = daoMgr.getXXEnumDef().findByServiceDefId(serviceDefId);

		XXServiceConfigDefDao xxServiceConfigDao = daoMgr.getXXServiceConfigDef();
		for (int i = 0; i < configs.size(); i++) {
			RangerServiceConfigDef config = configs.get(i);
			boolean found = false;
			for (XXServiceConfigDef xConfig : xxConfigs) {
				if (config.getItemId() != null && config.getItemId().equals(xConfig.getItemId())) {
					found = true;
					xConfig = serviceDefService.populateRangerServiceConfigDefToXX(config, xConfig, createdSvcDef,
							RangerServiceDefService.OPERATION_UPDATE_CONTEXT);
					xConfig.setOrder(i);
					xConfig = xxServiceConfigDao.update(xConfig);
					config = serviceDefService.populateXXToRangerServiceConfigDef(xConfig);
					break;
				}
			}
			if (!found) {
				XXServiceConfigDef xConfig = new XXServiceConfigDef();
				xConfig = serviceDefService.populateRangerServiceConfigDefToXX(config, xConfig, createdSvcDef,
						RangerServiceDefService.OPERATION_CREATE_CONTEXT);
				xConfig.setOrder(i);
				xConfig = xxServiceConfigDao.create(xConfig);
				config = serviceDefService.populateXXToRangerServiceConfigDef(xConfig);
			}
		}
		for (XXServiceConfigDef xConfig : xxConfigs) {
			boolean found = false;
			for (RangerServiceConfigDef config : configs) {
				if (xConfig.getItemId() != null && xConfig.getItemId().equals(config.getItemId())) {
					found = true;
					break;
				}
			}
			if (!found) {
				xxServiceConfigDao.remove(xConfig);
			}
		}

		XXResourceDefDao xxResDefDao = daoMgr.getXXResourceDef();
		for (RangerResourceDef resource : resources) {
			boolean found = false;
			for (XXResourceDef xRes : xxResources) {
				if (resource.getItemId() != null && resource.getItemId().equals(xRes.getItemId())) {
					found = true;
					xRes = serviceDefService.populateRangerResourceDefToXX(resource, xRes, createdSvcDef,
							RangerServiceDefService.OPERATION_UPDATE_CONTEXT);
					xxResDefDao.update(xRes);
					resource = serviceDefService.populateXXToRangerResourceDef(xRes);
					break;
				}
			}
			if (!found) {
				XXResourceDef parent = xxResDefDao.findByNameAndServiceDefId(resource.getParent(), serviceDefId);
				Long parentId = (parent != null) ? parent.getId() : null;

				XXResourceDef xResource = new XXResourceDef();
				xResource = serviceDefService.populateRangerResourceDefToXX(resource, xResource, createdSvcDef,
						RangerServiceDefService.OPERATION_CREATE_CONTEXT);
				xResource.setParent(parentId);
				xResource = xxResDefDao.create(xResource);
			}
		}
		for (XXResourceDef xRes : xxResources) {
			boolean found = false;
			for (RangerResourceDef resource : resources) {
				if (xRes.getItemId() != null && xRes.getItemId().equals(resource.getItemId())) {
					found = true;
					break;
				}
			}
			if (!found) {
				List<XXPolicyRefResource> xxPolicyRefResource = daoMgr.getXXPolicyRefResource().findByResourceDefID(xRes.getId());
				if (!stringUtil.isEmpty(xxPolicyRefResource)) {
					throw restErrorUtil.createRESTException("Policy/Policies are referring to this resource: "
							+ xRes.getName() + ". Please remove such references from policy before updating service-def.",
							MessageEnums.DATA_NOT_UPDATABLE);
				}
				deleteXXResourceDef(xRes);
			}
		}

		XXAccessTypeDefDao xxATDDao = daoMgr.getXXAccessTypeDef();
		for(int i = 0; i < accessTypes.size(); i++) {
			RangerAccessTypeDef access = accessTypes.get(i);
			boolean found = false;
			for (XXAccessTypeDef xAccess : xxAccessTypes) {
				if (access.getItemId() != null && access.getItemId().equals(xAccess.getItemId())) {
					found = true;
					xAccess = serviceDefService.populateRangerAccessTypeDefToXX(access, xAccess, createdSvcDef,
							RangerServiceDefService.OPERATION_UPDATE_CONTEXT);
					xAccess.setOrder(i);
					xAccess = xxATDDao.update(xAccess);

					Collection<String> impliedGrants = access.getImpliedGrants();
					XXAccessTypeDefGrantsDao xxATDGrantDao = daoMgr.getXXAccessTypeDefGrants();
					List<String> xxImpliedGrants = xxATDGrantDao.findImpliedGrantsByATDId(xAccess.getId());
					for (String impliedGrant : impliedGrants) {
						boolean foundGrant = false;
						for (String xImpliedGrant : xxImpliedGrants) {
							if (StringUtils.equalsIgnoreCase(impliedGrant, xImpliedGrant)) {
								foundGrant = true;
								break;
							}
						}
						if (!foundGrant) {
							XXAccessTypeDefGrants xImpliedGrant = new XXAccessTypeDefGrants();
							xImpliedGrant.setAtdId(xAccess.getId());
							xImpliedGrant.setImpliedGrant(impliedGrant);
							xImpliedGrant = xxATDGrantDao.create(xImpliedGrant);
						}
					}
					for (String xImpliedGrant : xxImpliedGrants) {
						boolean foundGrant = false;
						for (String impliedGrant : impliedGrants) {
							if (StringUtils.equalsIgnoreCase(xImpliedGrant, impliedGrant)) {
								foundGrant = true;
								break;
							}
						}
						if (!foundGrant) {
							XXAccessTypeDefGrants xATDGrant = xxATDGrantDao.findByNameAndATDId(xAccess.getId(),
									xImpliedGrant);
							xxATDGrantDao.remove(xATDGrant);

						}
					}
					access = serviceDefService.populateXXToRangerAccessTypeDef(xAccess);
					break;
				}
			}
			if (!found) {
				XXAccessTypeDef xAccessType = new XXAccessTypeDef();
				xAccessType = serviceDefService.populateRangerAccessTypeDefToXX(access, xAccessType, createdSvcDef,
						RangerServiceDefService.OPERATION_CREATE_CONTEXT);
				xAccessType.setOrder(i);
				xAccessType = xxATDDao.create(xAccessType);

				Collection<String> impliedGrants = access.getImpliedGrants();
				XXAccessTypeDefGrantsDao xxATDGrantDao = daoMgr.getXXAccessTypeDefGrants();
				for (String impliedGrant : impliedGrants) {
					XXAccessTypeDefGrants xImpliedGrant = new XXAccessTypeDefGrants();
					xImpliedGrant.setAtdId(xAccessType.getId());
					xImpliedGrant.setImpliedGrant(impliedGrant);
					xImpliedGrant = xxATDGrantDao.create(xImpliedGrant);
				}
				access = serviceDefService.populateXXToRangerAccessTypeDef(xAccessType);
			}
		}

		for (XXAccessTypeDef xAccess : xxAccessTypes) {
			boolean found = false;
			for (RangerAccessTypeDef access : accessTypes) {
				if (xAccess.getItemId() != null && xAccess.getItemId().equals(access.getItemId())) {
					found = true;
					break;
				}
			}
			if (!found) {
				List<XXPolicyRefAccessType> policyRefAccessTypeList = daoMgr.getXXPolicyRefAccessType().findByAccessTypeDefId(xAccess.getId());
				if(!stringUtil.isEmpty(policyRefAccessTypeList)) {
					throw restErrorUtil.createRESTException("Policy/Policies are referring to this access-type: "
							+ xAccess.getName() + ". Please remove such references from policy before updating service-def.",
							MessageEnums.DATA_NOT_UPDATABLE);
				}
				deleteXXAccessTypeDef(xAccess);
			}
		}

		XXPolicyConditionDefDao xxPolCondDao = daoMgr.getXXPolicyConditionDef();
		for (int i = 0; i < policyConditions.size(); i++) {
			RangerPolicyConditionDef condition = policyConditions.get(i);
			boolean found = false;
			for (XXPolicyConditionDef xCondition : xxPolicyConditions) {
				if (condition.getItemId() != null && condition.getItemId().equals(xCondition.getItemId())) {
					found = true;
					xCondition = serviceDefService.populateRangerPolicyConditionDefToXX(condition, xCondition,
							createdSvcDef, RangerServiceDefService.OPERATION_UPDATE_CONTEXT);
					xCondition.setOrder(i);
					xCondition = xxPolCondDao.update(xCondition);
					condition = serviceDefService.populateXXToRangerPolicyConditionDef(xCondition);
					break;
				}
			}
			if (!found) {
				XXPolicyConditionDef xCondition = new XXPolicyConditionDef();
				xCondition = serviceDefService.populateRangerPolicyConditionDefToXX(condition, xCondition,
						createdSvcDef, RangerServiceDefService.OPERATION_CREATE_CONTEXT);
				xCondition.setOrder(i);
				xCondition = xxPolCondDao.create(xCondition);
				condition = serviceDefService.populateXXToRangerPolicyConditionDef(xCondition);
			}
		}
		for(XXPolicyConditionDef xCondition : xxPolicyConditions) {
			boolean found = false;
			for(RangerPolicyConditionDef condition : policyConditions) {
				if(xCondition.getItemId() != null && xCondition.getItemId().equals(condition.getItemId())) {
					found = true;
					break;
				}
			}
			if(!found) {
				List<XXPolicyRefCondition> xxPolicyRefConditions = daoMgr.getXXPolicyRefCondition().findByConditionDefId(xCondition.getId());
				if(!stringUtil.isEmpty(xxPolicyRefConditions)) {
					throw restErrorUtil.createRESTException("Policy/Policies are referring to this policy-condition: "
							+ xCondition.getName() + ". Please remove such references from policy before updating service-def.",
							MessageEnums.DATA_NOT_UPDATABLE);
				}
				for(XXPolicyRefCondition xxPolicyRefCondition : xxPolicyRefConditions) {
					daoMgr.getXXPolicyRefCondition().remove(xxPolicyRefCondition);
				}
				xxPolCondDao.remove(xCondition);
			}
		}

		XXContextEnricherDefDao xxContextEnricherDao = daoMgr.getXXContextEnricherDef();
		for (int i = 0; i < contextEnrichers.size(); i++) {
			RangerContextEnricherDef context = contextEnrichers.get(i);
			boolean found = false;
			for (XXContextEnricherDef xContext : xxContextEnrichers) {
				if (context.getItemId() != null && context.getItemId().equals(xContext.getItemId())) {
					found = true;
					xContext = serviceDefService.populateRangerContextEnricherDefToXX(context, xContext, createdSvcDef,
							RangerServiceDefService.OPERATION_UPDATE_CONTEXT);
					xContext.setOrder(i);
					xContext = xxContextEnricherDao.update(xContext);
					context = serviceDefService.populateXXToRangerContextEnricherDef(xContext);
					break;
				}
			}
			if (!found) {
				XXContextEnricherDef xContext = new XXContextEnricherDef();
				xContext = serviceDefService.populateRangerContextEnricherDefToXX(context, xContext, createdSvcDef,
						RangerServiceDefService.OPERATION_UPDATE_CONTEXT);
				xContext.setOrder(i);
				xContext = xxContextEnricherDao.create(xContext);
				context = serviceDefService.populateXXToRangerContextEnricherDef(xContext);
			}
		}
		for (XXContextEnricherDef xContext : xxContextEnrichers) {
			boolean found = false;
			for (RangerContextEnricherDef context : contextEnrichers) {
				if (xContext.getItemId() != null && xContext.getItemId().equals(context.getItemId())) {
					found = true;
					break;
				}
			}
			if (!found) {
				daoMgr.getXXContextEnricherDef().remove(xContext);
			}
		}

		XXEnumDefDao xxEnumDefDao = daoMgr.getXXEnumDef();
		for (RangerEnumDef enumDef : enums) {
			boolean found = false;
			for (XXEnumDef xEnumDef : xxEnums) {
				if (enumDef.getItemId() != null && enumDef.getItemId().equals(xEnumDef.getItemId())) {
					found = true;
					xEnumDef = serviceDefService.populateRangerEnumDefToXX(enumDef, xEnumDef, createdSvcDef,
							RangerServiceDefService.OPERATION_UPDATE_CONTEXT);
					xEnumDef = xxEnumDefDao.update(xEnumDef);

					XXEnumElementDefDao xEnumEleDao = daoMgr.getXXEnumElementDef();
					List<XXEnumElementDef> xxEnumEleDefs = xEnumEleDao.findByEnumDefId(xEnumDef.getId());
					List<RangerEnumElementDef> enumEleDefs = enumDef.getElements();

					for (int i = 0; i < enumEleDefs.size(); i++) {
						RangerEnumElementDef eleDef = enumEleDefs.get(i);
						boolean foundEle = false;
						for (XXEnumElementDef xEleDef : xxEnumEleDefs) {
							if (eleDef.getItemId() != null && eleDef.getItemId().equals(xEleDef.getItemId())) {
								foundEle = true;
								xEleDef = serviceDefService.populateRangerEnumElementDefToXX(eleDef, xEleDef, xEnumDef,
										RangerServiceDefService.OPERATION_UPDATE_CONTEXT);
								xEleDef.setOrder(i);
								xEleDef = xEnumEleDao.update(xEleDef);
								break;
							}
						}
						if (!foundEle) {
							XXEnumElementDef xElement = new XXEnumElementDef();
							xElement = serviceDefService.populateRangerEnumElementDefToXX(eleDef, xElement, xEnumDef,
									RangerServiceDefService.OPERATION_CREATE_CONTEXT);
							xElement.setOrder(i);
							xElement = xEnumEleDao.create(xElement);
						}
					}
					for (XXEnumElementDef xxEleDef : xxEnumEleDefs) {
						boolean foundEle = false;
						for (RangerEnumElementDef enumEle : enumEleDefs) {
							if (xxEleDef.getItemId() != null && xxEleDef.getItemId().equals(enumEle.getItemId())) {
								foundEle = true;
								break;
							}
						}
						if (!foundEle) {
							xEnumEleDao.remove(xxEleDef);
						}
					}
					enumDef = serviceDefService.populateXXToRangerEnumDef(xEnumDef);
					break;
				}
			}
			if (!found) {
				XXEnumDef xEnum = new XXEnumDef();
				xEnum = serviceDefService.populateRangerEnumDefToXX(enumDef, xEnum, createdSvcDef,
						RangerServiceDefService.OPERATION_CREATE_CONTEXT);
				xEnum = xxEnumDefDao.create(xEnum);

				List<RangerEnumElementDef> elements = enumDef.getElements();
				XXEnumElementDefDao xxEnumEleDefDao = daoMgr.getXXEnumElementDef();
				for (RangerEnumElementDef element : elements) {
					XXEnumElementDef xElement = new XXEnumElementDef();
					xElement = serviceDefService.populateRangerEnumElementDefToXX(element, xElement, xEnum,
							RangerServiceDefService.OPERATION_CREATE_CONTEXT);
					xElement = xxEnumEleDefDao.create(xElement);
				}
				enumDef = serviceDefService.populateXXToRangerEnumDef(xEnum);
			}
		}
		for (XXEnumDef xEnumDef : xxEnums) {
			boolean found = false;
			for (RangerEnumDef enumDef : enums) {
				if (xEnumDef.getItemId() != null && xEnumDef.getItemId().equals(enumDef.getItemId())) {
					found = true;
					break;
				}
			}
			if (!found) {
				List<XXEnumElementDef> enumEleDefList = daoMgr.getXXEnumElementDef().findByEnumDefId(xEnumDef.getId());
				for (XXEnumElementDef eleDef : enumEleDefList) {
					daoMgr.getXXEnumElementDef().remove(eleDef);
				}
				xxEnumDefDao.remove(xEnumDef);
			}
		}

		List<RangerDataMaskTypeDef> dataMasks            = dataMaskDef == null || dataMaskDef.getMaskTypes() == null ? new ArrayList<RangerDataMaskTypeDef>() : dataMaskDef.getMaskTypes();
		List<RangerAccessTypeDef>   dataMaskAccessTypes  = dataMaskDef == null || dataMaskDef.getAccessTypes() == null ? new ArrayList<RangerAccessTypeDef>() : dataMaskDef.getAccessTypes();
		List<RangerResourceDef>     dataMaskResources    = dataMaskDef == null || dataMaskDef.getResources() == null ? new ArrayList<RangerResourceDef>() : dataMaskDef.getResources();
		List<RangerAccessTypeDef>   rowFilterAccessTypes = rowFilterDef == null || rowFilterDef.getAccessTypes() == null ? new ArrayList<RangerAccessTypeDef>() : rowFilterDef.getAccessTypes();
		List<RangerResourceDef>     rowFilterResources   = rowFilterDef == null || rowFilterDef.getResources() == null ? new ArrayList<RangerResourceDef>() : rowFilterDef.getResources();
		XXDataMaskTypeDefDao        dataMaskTypeDao      = daoMgr.getXXDataMaskTypeDef();
		List<XXDataMaskTypeDef>     xxDataMaskTypes      = dataMaskTypeDao.findByServiceDefId(serviceDefId);
		List<XXAccessTypeDef>       xxAccessTypeDefs     = xxATDDao.findByServiceDefId(serviceDefId);
		List<XXResourceDef>         xxResourceDefs       = xxResDefDao.findByServiceDefId(serviceDefId);

		// create or update dataMasks
		for(int i = 0; i < dataMasks.size(); i++) {
			RangerDataMaskTypeDef dataMask = dataMasks.get(i);
			boolean found = false;
			for (XXDataMaskTypeDef xxDataMask : xxDataMaskTypes) {
				if (xxDataMask.getItemId() != null && xxDataMask.getItemId().equals(dataMask.getItemId())) {
					if (LOG.isDebugEnabled()) {
						LOG.debug("Updating existing dataMask with itemId=" + dataMask.getItemId());
					}

					found = true;
					xxDataMask = serviceDefService.populateRangerDataMaskDefToXX(dataMask, xxDataMask, createdSvcDef,
							RangerServiceDefService.OPERATION_UPDATE_CONTEXT);
					xxDataMask.setOrder(i);
					xxDataMask = dataMaskTypeDao.update(xxDataMask);
					dataMask = serviceDefService.populateXXToRangerDataMaskTypeDef(xxDataMask);
					break;
				}
			}

			if (!found) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("Creating dataMask with itemId=" + dataMask.getItemId() + "");
				}

				XXDataMaskTypeDef xxDataMask = new XXDataMaskTypeDef();
				xxDataMask = serviceDefService.populateRangerDataMaskDefToXX(dataMask, xxDataMask, createdSvcDef, RangerServiceDefService.OPERATION_CREATE_CONTEXT);
				xxDataMask.setOrder(i);
				xxDataMask = dataMaskTypeDao.create(xxDataMask);
			}
		}

		// remove dataMasks
		for (XXDataMaskTypeDef xxDataMask : xxDataMaskTypes) {
			boolean found = false;
			for (RangerDataMaskTypeDef dataMask : dataMasks) {
				if (xxDataMask.getItemId() != null && xxDataMask.getItemId().equals(dataMask.getItemId())) {
					found = true;
					break;
				}
			}
			if (!found) {
				if(LOG.isDebugEnabled()) {
					LOG.debug("Deleting dataMask with itemId=" + xxDataMask.getItemId());
				}

				dataMaskTypeDao.remove(xxDataMask);
			}
		}

		for(RangerAccessTypeDef accessType : dataMaskAccessTypes) {
			if(! isAccessTypeInList(accessType.getName(), xxAccessTypeDefs)) {
				throw restErrorUtil.createRESTException("accessType with name: "
						+ accessType.getName() + " does not exist", MessageEnums.DATA_NOT_FOUND);
			}
		}

		for(RangerAccessTypeDef accessType : rowFilterAccessTypes) {
			if(! isAccessTypeInList(accessType.getName(), xxAccessTypeDefs)) {
				throw restErrorUtil.createRESTException("accessType with name: "
						+ accessType.getName() + " does not exists", MessageEnums.DATA_NOT_FOUND);
			}
		}

		for(XXAccessTypeDef xxAccessTypeDef : xxAccessTypeDefs) {
			String dataMaskOptions = null;
			String rowFilterOptions = null;

			for(RangerAccessTypeDef accessTypeDef : dataMaskAccessTypes) {
				if(StringUtils.equals(accessTypeDef.getName(), xxAccessTypeDef.getName())) {
					dataMaskOptions = svcDefServiceWithAssignedId.objectToJson(accessTypeDef);
					break;
				}
			}

			for(RangerAccessTypeDef accessTypeDef : rowFilterAccessTypes) {
				if(StringUtils.equals(accessTypeDef.getName(), xxAccessTypeDef.getName())) {
					rowFilterOptions = svcDefServiceWithAssignedId.objectToJson(accessTypeDef);
					break;
				}
			}

			if(!StringUtils.equals(dataMaskOptions, xxAccessTypeDef.getDataMaskOptions()) ||
			   !StringUtils.equals(rowFilterOptions, xxAccessTypeDef.getRowFilterOptions())) {
				xxAccessTypeDef.setDataMaskOptions(dataMaskOptions);
				xxAccessTypeDef.setRowFilterOptions(rowFilterOptions);
				xxATDDao.update(xxAccessTypeDef);
			}
		}

		for(RangerResourceDef resource : dataMaskResources) {
			if(! isResourceInList(resource.getName(), xxResourceDefs)) {
				throw restErrorUtil.createRESTException("resource with name: "
						+ resource.getName() + " does not exists", MessageEnums.DATA_NOT_FOUND);
			}
		}

		for(RangerResourceDef resource : rowFilterResources) {
			if(! isResourceInList(resource.getName(), xxResourceDefs)) {
				throw restErrorUtil.createRESTException("resource with name: "
						+ resource.getName() + " does not exists", MessageEnums.DATA_NOT_FOUND);
			}
		}

		for(XXResourceDef xxResourceDef : xxResourceDefs) {
			String dataMaskOptions  = null;
			String rowFilterOptions = null;

			for(RangerResourceDef resource : dataMaskResources) {
				if(StringUtils.equals(resource.getName(), xxResourceDef.getName())) {
					dataMaskOptions = svcDefServiceWithAssignedId.objectToJson(resource);
					break;
				}
			}

			for(RangerResourceDef resource : rowFilterResources) {
				if(StringUtils.equals(resource.getName(), xxResourceDef.getName())) {
					rowFilterOptions = svcDefServiceWithAssignedId.objectToJson(resource);
					break;
				}
			}

			if(!StringUtils.equals(dataMaskOptions, xxResourceDef.getDataMaskOptions()) ||
			   !StringUtils.equals(rowFilterOptions, xxResourceDef.getRowFilterOptions())) {
				xxResourceDef.setDataMaskOptions(dataMaskOptions);
				xxResourceDef.setRowFilterOptions(rowFilterOptions);
				xxResDefDao.update(xxResourceDef);
			}
		}
	}

	public void deleteServiceDef(Long serviceDefId, Boolean forceDelete) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceDBStore.deleteServiceDef(" + serviceDefId + ", " + forceDelete + ")");
		}
		bizUtil.blockAuditorRoleUser();
		UserSessionBase session = ContextUtil.getCurrentUserSession();
		if (session == null) {
			throw restErrorUtil.createRESTException(
					"UserSession cannot be null, only Admin can update service-def",
					MessageEnums.OPER_NO_PERMISSION);
		}

		if (!session.isKeyAdmin() && !session.isUserAdmin()) {
			throw restErrorUtil.createRESTException(
					"User is not allowed to update service-def, only Admin can update service-def",
					MessageEnums.OPER_NO_PERMISSION);
		}

		RangerServiceDef serviceDef = getServiceDef(serviceDefId);
		if(serviceDef == null) {
			throw restErrorUtil.createRESTException("No Service Definiton found for Id: " + serviceDefId,
					MessageEnums.DATA_NOT_FOUND);
		}

		List<XXService> serviceList = daoMgr.getXXService().findByServiceDefId(serviceDefId);
		if (!forceDelete) {
			if(CollectionUtils.isNotEmpty(serviceList)) {
				throw restErrorUtil.createRESTException(
						"Services exists under given service definition, can't delete Service-Def: "
								+ serviceDef.getName(), MessageEnums.OPER_NOT_ALLOWED_FOR_ENTITY);
			}
		}

		if(CollectionUtils.isNotEmpty(serviceList)) {
			for(XXService service : serviceList) {
				deleteService(service.getId());
			}
		}

		XXDataMaskTypeDefDao dataMaskDao = daoMgr.getXXDataMaskTypeDef();
		List<XXDataMaskTypeDef> dataMaskDefs = dataMaskDao.findByServiceDefId(serviceDefId);
		for(XXDataMaskTypeDef dataMaskDef : dataMaskDefs) {
			dataMaskDao.remove(dataMaskDef);
		}

		List<XXAccessTypeDef> accTypeDefs = daoMgr.getXXAccessTypeDef().findByServiceDefId(serviceDefId);
		for(XXAccessTypeDef accessType : accTypeDefs) {
			deleteXXAccessTypeDef(accessType);
		}

		XXContextEnricherDefDao xContextEnricherDao = daoMgr.getXXContextEnricherDef();
		List<XXContextEnricherDef> contextEnrichers = xContextEnricherDao.findByServiceDefId(serviceDefId);
		for(XXContextEnricherDef context : contextEnrichers) {
			xContextEnricherDao.remove(context);
		}

		XXEnumDefDao enumDefDao = daoMgr.getXXEnumDef();
		List<XXEnumDef> enumDefList = enumDefDao.findByServiceDefId(serviceDefId);
		for (XXEnumDef enumDef : enumDefList) {
			List<XXEnumElementDef> enumEleDefList = daoMgr.getXXEnumElementDef().findByEnumDefId(enumDef.getId());
			for (XXEnumElementDef eleDef : enumEleDefList) {
				daoMgr.getXXEnumElementDef().remove(eleDef);
			}
			enumDefDao.remove(enumDef);
		}

		XXPolicyConditionDefDao policyCondDao = daoMgr.getXXPolicyConditionDef();
		List<XXPolicyConditionDef> policyCondList = policyCondDao.findByServiceDefId(serviceDefId);

		for (XXPolicyConditionDef policyCond : policyCondList) {
			List<XXPolicyRefCondition> xxPolicyRefConditions = daoMgr.getXXPolicyRefCondition().findByConditionDefId(policyCond.getId());
			for (XXPolicyRefCondition XXPolicyRefCondition : xxPolicyRefConditions) {
				daoMgr.getXXPolicyRefCondition().remove(XXPolicyRefCondition);
			}
			policyCondDao.remove(policyCond);
		}

		List<XXResourceDef> resDefList = daoMgr.getXXResourceDef().findByServiceDefId(serviceDefId);
		for(XXResourceDef resDef : resDefList) {
			deleteXXResourceDef(resDef);
		}

		XXServiceConfigDefDao configDefDao = daoMgr.getXXServiceConfigDef();
		List<XXServiceConfigDef> configDefList = configDefDao.findByServiceDefId(serviceDefId);
		for(XXServiceConfigDef configDef : configDefList) {
			configDefDao.remove(configDef);
		}

		Long version = serviceDef.getVersion();
		if(version == null) {
			version = Long.valueOf(1);
			LOG.info("Found Version Value: `null`, so setting value of version to 1, While updating object, version should not be null.");
		} else {
			version = Long.valueOf(version.longValue() + 1);
		}
		serviceDef.setVersion(version);

		serviceDefService.delete(serviceDef);
		LOG.info("ServiceDefinition has been deleted successfully. Service-Def Name: " + serviceDef.getName());

		dataHistService.createObjectDataHistory(serviceDef, RangerDataHistService.ACTION_DELETE);

		postDelete(serviceDef);

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== ServiceDBStore.deleteServiceDef(" + serviceDefId + ", " + forceDelete + ")");
		}
	}

	public void deleteXXAccessTypeDef(XXAccessTypeDef xAccess) {
		List<XXAccessTypeDefGrants> atdGrantsList = daoMgr.getXXAccessTypeDefGrants().findByATDId(xAccess.getId());

		for (XXAccessTypeDefGrants atdGrant : atdGrantsList) {
			daoMgr.getXXAccessTypeDefGrants().remove(atdGrant);
		}

		List<XXPolicyRefAccessType> policyRefAccessTypeList = daoMgr.getXXPolicyRefAccessType().findByAccessTypeDefId(xAccess.getId());
		for (XXPolicyRefAccessType xxPolicyRefAccessType : policyRefAccessTypeList) {
			daoMgr.getXXPolicyRefAccessType().remove(xxPolicyRefAccessType);
		}
		daoMgr.getXXAccessTypeDef().remove(xAccess);
	}

	public void deleteXXResourceDef(XXResourceDef xRes) {
		List<XXResourceDef> xChildObjs = daoMgr.getXXResourceDef().findByParentResId(xRes.getId());
		for(XXResourceDef childRes : xChildObjs) {
			deleteXXResourceDef(childRes);
		}
		List<XXPolicyRefResource> xxPolicyRefResources = daoMgr.getXXPolicyRefResource().findByResourceDefID(xRes.getId());
		for (XXPolicyRefResource xPolRefRes : xxPolicyRefResources) {
			daoMgr.getXXPolicyRefResource().remove(xPolRefRes);
		}
		daoMgr.getXXResourceDef().remove(xRes);
	}

	@Override
	public RangerServiceDef getServiceDef(Long id) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceDBStore.getServiceDef(" + id + ")");
		}

		RangerServiceDef ret = serviceDefService.read(id);
		if (LOG.isDebugEnabled()) {
			LOG.debug("<== ServiceDBStore.getServiceDef(" + id + "): " + ret);
		}

		return ret;
	}

	@Override
	public RangerServiceDef getServiceDefByName(String name) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceDBStore.getServiceDefByName(" + name + ")");
		}

		RangerServiceDef ret = null;

		XXServiceDef xServiceDef = daoMgr.getXXServiceDef().findByName(name);

		if(xServiceDef != null) {
			ret = serviceDefService.getPopulatedViewObject(xServiceDef);
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("== ServiceDBStore.getServiceDefByName(" + name + "): " );
		}

		return  ret;
	}

	/**
	 * @param displayName
	 * @return {@link RangerServiceDef} - service using display name if present in DB, <code>null</code> otherwise.
	 */
	@Override
	public RangerServiceDef getServiceDefByDisplayName(String displayName) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceDBStore.getServiceDefByDisplayName(" + displayName + ")");
		}

		RangerServiceDef ret = null;

		XXServiceDef xServiceDef = daoMgr.getXXServiceDef().findByDisplayName(displayName);

		if(xServiceDef != null) {
			ret = serviceDefService.getPopulatedViewObject(xServiceDef);
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("== ServiceDBStore.getServiceDefByName(" + displayName + "): " + ret);
		}

		return  ret;
	}

	@Override
	public List<RangerServiceDef> getServiceDefs(SearchFilter filter) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceDBStore.getServiceDefs(" + filter + ")");
		}

		RangerServiceDefList svcDefList = serviceDefService.searchRangerServiceDefs(filter);

		predicateUtil.applyFilter(svcDefList.getServiceDefs(), filter);

		List<RangerServiceDef> ret = svcDefList.getServiceDefs();

		if (LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceDBStore.getServiceDefs(" + filter + "): " + ret);
		}

		return ret;
	}

	@Override

	public PList<RangerServiceDef> getPaginatedServiceDefs(SearchFilter filter) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceDBStore.getPaginatedServiceDefs(" + filter + ")");
		}

		RangerServiceDefList svcDefList = serviceDefService.searchRangerServiceDefs(filter);

		predicateUtil.applyFilter(svcDefList.getServiceDefs(), filter);

		if (LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceDBStore.getPaginatedServiceDefs(" + filter + ")");
		}

		return new PList<RangerServiceDef>(svcDefList.getServiceDefs(), svcDefList.getStartIndex(), svcDefList.getPageSize(), svcDefList.getTotalCount(),
				svcDefList.getResultSize(), svcDefList.getSortType(), svcDefList.getSortBy());

	}

	@Override
	public RangerService createService(RangerService service) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceDBStore.createService(" + service + ")");
		}

		if (service == null) {
			throw restErrorUtil.createRESTException("Service object cannot be null.",
					MessageEnums.ERROR_CREATING_OBJECT);
		}

		boolean createDefaultPolicy = true;
		Map<String, String> configs = service.getConfigs();
		Map<String, String> validConfigs = validateRequiredConfigParams(service, configs);
		if (validConfigs == null) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("==> ConfigParams cannot be null, ServiceDBStore.createService(" + service + ")");
			}
			throw restErrorUtil.createRESTException("ConfigParams cannot be null.", MessageEnums.ERROR_CREATING_OBJECT);
		}

		// While creating, value of version should be 1.
		service.setVersion(Long.valueOf(1));
		service.setTagVersion(Long.valueOf(1));

		if (populateExistingBaseFields) {
			svcServiceWithAssignedId.setPopulateExistingBaseFields(true);
			daoMgr.getXXService().setIdentityInsert(true);

			service = svcServiceWithAssignedId.create(service);

			daoMgr.getXXService().setIdentityInsert(false);
			daoMgr.getXXService().updateSequence();
			svcServiceWithAssignedId.setPopulateExistingBaseFields(false);
			createDefaultPolicy = false;
		} else {
			service = svcService.create(service);
		}
		XXService xCreatedService = daoMgr.getXXService().getById(service.getId());
		VXUser vXUser = null;

		XXServiceConfigMapDao xConfMapDao = daoMgr.getXXServiceConfigMap();
		for (Entry<String, String> configMap : validConfigs.entrySet()) {
			String configKey = configMap.getKey();
			String configValue = configMap.getValue();

			if (StringUtils.equalsIgnoreCase(configKey, "username")) {
				String userName = stringUtil.getValidUserName(configValue);
				XXUser xxUser = daoMgr.getXXUser().findByUserName(userName);
				if (xxUser != null) {
					vXUser = xUserService.populateViewBean(xxUser);
				} else {
					UserSessionBase usb = ContextUtil.getCurrentUserSession();
					if (usb != null && !usb.isUserAdmin() && !usb.isSpnegoEnabled()) {
						throw restErrorUtil.createRESTException("User does not exist with given username: ["
								+ userName + "] please use existing user", MessageEnums.OPER_NO_PERMISSION);
					}
					vXUser = xUserMgr.createServiceConfigUser(userName);
				}
			}

			if (StringUtils.equalsIgnoreCase(configKey, CONFIG_KEY_PASSWORD)) {
				Joiner joiner = Joiner.on(",").skipNulls();
				String iv = PasswordUtils.generateIvIfNeeded(CRYPT_ALGO);

				String cryptConfigString = joiner.join(CRYPT_ALGO, ENCRYPT_KEY, SALT, ITERATION_COUNT, iv, configValue);
				String encryptedPwd = PasswordUtils.encryptPassword(cryptConfigString);

				String paddedEncryptedPwd = joiner.join(CRYPT_ALGO, ENCRYPT_KEY, SALT, ITERATION_COUNT, iv,
						encryptedPwd);
				String decryptedPwd = PasswordUtils.decryptPassword(paddedEncryptedPwd);
				if (StringUtils.equals(decryptedPwd, configValue)) {
					configValue = paddedEncryptedPwd;
				}
			}
			XXServiceConfigMap xConfMap = new XXServiceConfigMap();
			xConfMap = rangerAuditFields.populateAuditFields(xConfMap, xCreatedService);
			xConfMap.setServiceId(xCreatedService.getId());
			xConfMap.setConfigkey(configKey);
                        if (StringUtils.equalsIgnoreCase(configKey, "username")) {
                                configValue = stringUtil.getValidUserName(configValue);
                        }
			xConfMap.setConfigvalue(configValue);
			xConfMap = xConfMapDao.create(xConfMap);
		}
		updateTabPermissions(service.getType(), validConfigs);
		if (LOG.isDebugEnabled()) {
			LOG.debug("vXUser:[" + vXUser + "]");
		}
		RangerService createdService = svcService.getPopulatedViewObject(xCreatedService);

		if (createdService == null) {
			throw restErrorUtil.createRESTException("Could not create service - Internal error ", MessageEnums.ERROR_CREATING_OBJECT);
		}

		dataHistService.createObjectDataHistory(createdService, RangerDataHistService.ACTION_CREATE);

		List<XXTrxLog> trxLogList = svcService.getTransactionLog(createdService,
				RangerServiceService.OPERATION_CREATE_CONTEXT);
		bizUtil.createTrxLog(trxLogList);

		if (createDefaultPolicy) {
			createDefaultPolicies(createdService);
		}

		return createdService;

	}

	@Override
	public RangerService updateService(RangerService service, Map<String, Object> options) throws Exception {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceDBStore.updateService()");
		}

		XXService existing = daoMgr.getXXService().getById(service.getId());

		if(existing == null) {
			throw restErrorUtil.createRESTException(
					"no service exists with ID=" + service.getId(),
					MessageEnums.DATA_NOT_FOUND);
		}

		String existingName = existing.getName();

		boolean renamed = !StringUtils.equalsIgnoreCase(service.getName(), existingName);

		if(renamed) {
            XXService newNameService = daoMgr.getXXService().findByName(service.getName());

            if (newNameService != null) {
                throw restErrorUtil.createRESTException("another service already exists with name '"
                        + service.getName() + "'. ID=" + newNameService.getId(), MessageEnums.DATA_NOT_UPDATABLE);
            }

            long countOfTaggedResources = daoMgr.getXXServiceResource().countTaggedResourcesInServiceId(existing.getId());

            Boolean isForceRename =  options != null && options.get(ServiceStore.OPTION_FORCE_RENAME) != null ? (Boolean) options.get(ServiceStore.OPTION_FORCE_RENAME) : Boolean.FALSE;

            if (countOfTaggedResources != 0L) {
                if (isForceRename) {
                    LOG.warn("Forcing the renaming of service from " + existingName + " to " + service.getName() + " although it is associated with " + countOfTaggedResources
                    + " service-resources!");
                } else {
                    throw restErrorUtil.createRESTException("Service " + existingName + " cannot be renamed, as it has associated service-resources", MessageEnums.DATA_NOT_UPDATABLE);
                }
            }
        }

		Map<String, String> configs = service.getConfigs();
		Map<String, String> validConfigs = validateRequiredConfigParams(service, configs);
		if (validConfigs == null) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("==> ConfigParams cannot be null, ServiceDBStore.createService(" + service + ")");
			}
			throw restErrorUtil.createRESTException("ConfigParams cannot be null.", MessageEnums.ERROR_CREATING_OBJECT);
		}

		boolean hasTagServiceValueChanged = false;
		Long    existingTagServiceId      = existing.getTagService();
		String  newTagServiceName         = service.getTagService(); // null for old clients; empty string to remove existing association
		Long    newTagServiceId           = null;

		if(newTagServiceName == null) { // old client; don't update existing tagService
			if(existingTagServiceId != null) {
				newTagServiceName = getServiceName(existingTagServiceId);

				service.setTagService(newTagServiceName);

				LOG.info("ServiceDBStore.updateService(id=" + service.getId() + "; name=" + service.getName() + "): tagService is null; using existing tagService '" + newTagServiceName + "'");
			}
		}

		if (StringUtils.isNotBlank(newTagServiceName)) {
			RangerService tmp = getServiceByName(newTagServiceName);

			if (tmp == null || !EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_TAG_NAME.equals(tmp.getType())) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("ServiceDBStore.updateService() - " + newTagServiceName + " does not refer to a valid tag service.(" + service + ")");
				}
				throw restErrorUtil.createRESTException("Invalid tag service name " + newTagServiceName, MessageEnums.ERROR_CREATING_OBJECT);

			} else {
				newTagServiceId = tmp.getId();
			}
		}

		if (existingTagServiceId == null) {
			if (newTagServiceId != null) {
				hasTagServiceValueChanged = true;
			}
		} else if (!existingTagServiceId.equals(newTagServiceId)) {
			hasTagServiceValueChanged = true;
		}

		boolean hasIsEnabledChanged = !existing.getIsenabled().equals(service.getIsEnabled());

		List<XXServiceConfigMap> dbConfigMaps = daoMgr.getXXServiceConfigMap().findByServiceId(service.getId());
		boolean hasServiceConfigForPluginChanged = hasServiceConfigForPluginChanged(dbConfigMaps, validConfigs);

		List<XXTrxLog> trxLogList = svcService.getTransactionLog(service, existing, RangerServiceService.OPERATION_UPDATE_CONTEXT);

		if(populateExistingBaseFields) {
			svcServiceWithAssignedId.setPopulateExistingBaseFields(true);
			service = svcServiceWithAssignedId.update(service);
			svcServiceWithAssignedId.setPopulateExistingBaseFields(false);
		} else {
			service.setCreateTime(existing.getCreateTime());
			service.setGuid(existing.getGuid());
			service.setVersion(existing.getVersion());
			service = svcService.update(service);

			if (hasTagServiceValueChanged || hasIsEnabledChanged || hasServiceConfigForPluginChanged) {
				updatePolicyVersion(service, RangerPolicyDelta.CHANGE_TYPE_SERVICE_CHANGE, null, false);
			}
		}

		XXService xUpdService = daoMgr.getXXService().getById(service.getId());

		String oldPassword = null;

		for(XXServiceConfigMap dbConfigMap : dbConfigMaps) {
			if(StringUtils.equalsIgnoreCase(dbConfigMap.getConfigkey(), CONFIG_KEY_PASSWORD)) {
				oldPassword = dbConfigMap.getConfigvalue();
			}
			daoMgr.getXXServiceConfigMap().remove(dbConfigMap);
		}

		VXUser vXUser = null;
		XXServiceConfigMapDao xConfMapDao = daoMgr.getXXServiceConfigMap();
		for (Entry<String, String> configMap : validConfigs.entrySet()) {
			String configKey = configMap.getKey();
			String configValue = configMap.getValue();

			if(StringUtils.equalsIgnoreCase(configKey, "username")) {
				String userName = stringUtil.getValidUserName(configValue);
				XXUser xxUser = daoMgr.getXXUser().findByUserName(userName);
				if (xxUser != null) {
					vXUser = xUserService.populateViewBean(xxUser);
				} else {
					UserSessionBase usb = ContextUtil.getCurrentUserSession();
					if (usb != null && !usb.isUserAdmin()) {
						throw restErrorUtil.createRESTException("User does not exist with given username: ["
								+ userName + "] please use existing user", MessageEnums.OPER_NO_PERMISSION);
					}
					vXUser = xUserMgr.createServiceConfigUser(userName);
				}
			}

			if (StringUtils.equalsIgnoreCase(configKey, CONFIG_KEY_PASSWORD)) {
				if (StringUtils.equalsIgnoreCase(configValue, HIDDEN_PASSWORD_STR)) {
					if (oldPassword != null && oldPassword.contains(",")) {
						PasswordUtils util = PasswordUtils.build(oldPassword);
						if (!util.getCryptAlgo().equalsIgnoreCase(CRYPT_ALGO)) {
							String decryptedPwd = PasswordUtils.decryptPassword(oldPassword);
							String paddingString = Joiner.on(",").skipNulls().join(CRYPT_ALGO,
									new String(util.getEncryptKey()), new String(util.getSalt()),
									util.getIterationCount(), PasswordUtils.generateIvIfNeeded(CRYPT_ALGO));
							String encryptedPwd = PasswordUtils.encryptPassword(paddingString + "," + decryptedPwd);
							String newDecryptedPwd = PasswordUtils.decryptPassword(paddingString + "," + encryptedPwd);
							if (StringUtils.equals(newDecryptedPwd, decryptedPwd)) {
								configValue = paddingString + "," + encryptedPwd;
							}
						} else {
							configValue = oldPassword;
						}
					} else {
												configValue = oldPassword;
											}
				} else {
					String paddingString = Joiner.on(",").skipNulls().join(CRYPT_ALGO, ENCRYPT_KEY, SALT,
							ITERATION_COUNT, PasswordUtils.generateIvIfNeeded(CRYPT_ALGO));
					String encryptedPwd = PasswordUtils.encryptPassword(paddingString + "," + configValue);
					String decryptedPwd = PasswordUtils.decryptPassword(paddingString + "," + encryptedPwd);
					if (StringUtils.equals(decryptedPwd, configValue)) {
						configValue = paddingString + "," + encryptedPwd;
					}
				}
			}
			XXServiceConfigMap xConfMap = new XXServiceConfigMap();
			xConfMap = (XXServiceConfigMap) rangerAuditFields.populateAuditFields(xConfMap, xUpdService);
			xConfMap.setServiceId(service.getId());
			xConfMap.setConfigkey(configKey);
			xConfMap.setConfigvalue(configValue);
			xConfMapDao.create(xConfMap);
		}
		updateTabPermissions(service.getType(), validConfigs);
		if (LOG.isDebugEnabled()) {
			LOG.debug("vXUser:[" + vXUser + "]");
		}
		RangerService updService = svcService.getPopulatedViewObject(xUpdService);
		dataHistService.createObjectDataHistory(updService, RangerDataHistService.ACTION_UPDATE);
		bizUtil.createTrxLog(trxLogList);

		return updService;
	}

	@Override
	public void deleteService(Long id) throws Exception {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceDBStore.deleteService(" + id + ")");
		}

		RangerService service = getService(id);

		if(service == null) {
			throw new Exception("no service exists with ID=" + id);
		}

		// Manage zone
		disassociateZonesForService(service); //RANGER-3016

		List<Long> policyIds = daoMgr.getXXPolicy().findPolicyIdsByServiceId(service.getId());
		if (CollectionUtils.isNotEmpty(policyIds)) {
			long totalDeletedPolicies = 0;
			for (Long policyID : policyIds) {
				RangerPolicy rangerPolicy = getPolicy(policyID);
				deletePolicy(rangerPolicy, service);
				totalDeletedPolicies = totalDeletedPolicies + 1;
				// its a bulk policy delete call flush and clear
				if (totalDeletedPolicies % RangerBizUtil.policyBatchSize == 0) {
					bizUtil.bulkModeOnlyFlushAndClear();
				}
			}
			bizUtil.bulkModeOnlyFlushAndClear();
		}

		XXServiceConfigMapDao configDao = daoMgr.getXXServiceConfigMap();
		List<XXServiceConfigMap> configs = configDao.findByServiceId(service.getId());
		for (XXServiceConfigMap configMap : configs) {
			configDao.remove(configMap);
		}

		// Purge x_rms data
		daoMgr.getXXRMSServiceResource().purge(service.getId());

		Long version = service.getVersion();
		if(version == null) {
			version = Long.valueOf(1);
			LOG.info("Found Version Value: `null`, so setting value of version to 1, While updating object, version should not be null.");
		} else {
			version = Long.valueOf(version.longValue() + 1);
		}
		service.setVersion(version);

		svcService.delete(service);

		dataHistService.createObjectDataHistory(service, RangerDataHistService.ACTION_DELETE);

		List<XXTrxLog> trxLogList = svcService.getTransactionLog(service, RangerServiceService.OPERATION_DELETE_CONTEXT);
		bizUtil.createTrxLog(trxLogList);
	}

	private void updateTabPermissions(String svcType, Map<String, String> svcConfig) {
		if (StringUtils.equalsIgnoreCase(svcType, EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_TAG_NAME)) {
			String svcAdminUsers = svcConfig.get(SERVICE_ADMIN_USERS);
			if (StringUtils.isNotEmpty(svcAdminUsers)) {
				for (String user : svcAdminUsers.split(",")) {
					validateUserAndProvideTabTagBasedPolicyPermission(user.trim());
				}
			}
		}

	}

	private void validateUserAndProvideTabTagBasedPolicyPermission(String username){
		XXPortalUser xxPortalUser = daoMgr.getXXPortalUser().findByLoginId(username);
		if (xxPortalUser == null) {
			throw restErrorUtil
					.createRESTException(
							"Username : "
									+ username
									+ " does not exist. Please provide valid user as service admin for tag service .",
							MessageEnums.ERROR_CREATING_OBJECT);
		} else {
			VXPortalUser vXPortalUser = userMgr
					.mapXXPortalUserToVXPortalUserForDefaultAccount(xxPortalUser);
			if (CollectionUtils.isNotEmpty(vXPortalUser.getUserRoleList())
					&& vXPortalUser.getUserRoleList().size() == 1) {
				for (String userRole : vXPortalUser.getUserRoleList()) {
					if (userRole.equals(RangerConstants.ROLE_USER)) {
						HashMap<String, Long> moduleNameId = xUserMgr
								.getAllModuleNameAndIdMap();
						xUserMgr.createOrUpdateUserPermisson(
								vXPortalUser,
								moduleNameId.get(RangerConstants.MODULE_TAG_BASED_POLICIES),true);
					}
				}
			}
		}
	}

	@Override
	public List<RangerPolicy> getPoliciesByResourceSignature(String serviceName, String policySignature, Boolean isPolicyEnabled) throws Exception {

		List<XXPolicy> xxPolicies = daoMgr.getXXPolicy().findByResourceSignatureByPolicyStatus(serviceName, policySignature, isPolicyEnabled);
		List<RangerPolicy> policies = new ArrayList<RangerPolicy>(xxPolicies.size());
		for (XXPolicy xxPolicy : xxPolicies) {
			RangerPolicy policy = policyService.getPopulatedViewObject(xxPolicy);
			policies.add(policy);
		}
		
		return policies;
	}

	@Override
	public RangerService getService(Long id) throws Exception {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceDBStore.getService()");
		}

		UserSessionBase session = ContextUtil.getCurrentUserSession();
		if (session == null) {
			throw restErrorUtil.createRESTException("UserSession cannot be null.",
					MessageEnums.OPER_NOT_ALLOWED_FOR_STATE);
		}

		XXService xService = daoMgr.getXXService().getById(id);

		// TODO: As of now we are allowing SYS_ADMIN to read all the
		// services including KMS

		if (!bizUtil.hasAccess(xService, null)) {
			throw restErrorUtil.createRESTException("Logged in user is not allowed to read service, id: " + id,
					MessageEnums.OPER_NO_PERMISSION);
		}

		return svcService.getPopulatedViewObject(xService);
	}

	@Override
	public RangerService getServiceByName(String name) throws Exception {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceDBStore.getServiceByName()");
		}
		XXService xService = daoMgr.getXXService().findByName(name);

		// TODO: As of now we are allowing SYS_ADMIN to read all the
		// services including KMS

		if (ContextUtil.getCurrentUserSession() != null) {
			if (xService == null) {
				return null;
			}
			if (!bizUtil.hasAccess(xService, null)) {
				throw restErrorUtil.createRESTException("Logged in user is not allowed to read service, name: " + name,
						MessageEnums.OPER_NO_PERMISSION);
			}
		}
		return xService == null ? null : svcService.getPopulatedViewObject(xService);
	}

	@Override
	public RangerService getServiceByDisplayName(String displayName) throws Exception {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceDBStore.getServiceByName()");
		}
		XXService xService = daoMgr.getXXService().findByDisplayName(displayName);

		if (ContextUtil.getCurrentUserSession() != null) {
			if (xService == null) {
				return null;
			}
			if (!bizUtil.hasAccess(xService, null)) {
				throw restErrorUtil.createRESTException("Logged in user is not allowed to read service, name: " + displayName,
						MessageEnums.OPER_NO_PERMISSION);
			}
		}
		return xService == null ? null : svcService.getPopulatedViewObject(xService);
	}

	public RangerService getServiceByNameForDP(String name) throws Exception {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceDBStore.getServiceByNameForDP()");
		}
		XXService xService = daoMgr.getXXService().findByName(name);
		if (ContextUtil.getCurrentUserSession() != null) {
			if (xService == null) {
				return null;
			}
		}
		return xService == null ? null : svcService.getPopulatedViewObject(xService);
	}

	@Override
	public List<RangerService> getServices(SearchFilter filter) throws Exception {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceDBStore.getServices()");
		}

		RangerServiceList serviceList = svcService.searchRangerServices(filter);
		predicateUtil.applyFilter(serviceList.getServices(), filter);
		List<RangerService> ret = serviceList.getServices();

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== ServiceDBStore.getServices()");
		}

		return ret;
	}

	public PList<RangerService> getPaginatedServices(SearchFilter filter) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceDBStore.getPaginatedServices()");
		}

		RangerServiceList serviceList = svcService.searchRangerServices(filter);
		if (StringUtils.isEmpty(filter.getParam("serviceNamePartial"))){

		predicateUtil.applyFilter(serviceList.getServices(), filter);

		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("<== ServiceDBStore.getPaginatedServices()");
		}

		return new PList<RangerService>(serviceList.getServices(), serviceList.getStartIndex(), serviceList.getPageSize(), serviceList.getTotalCount(),
				serviceList.getResultSize(), serviceList.getSortType(), serviceList.getSortBy());

	}

	@Override
	public RangerPolicy createPolicy(RangerPolicy policy) throws Exception {

		RangerService service = getServiceByName(policy.getService());

		if(service == null) {
			throw new Exception("service does not exist - name=" + policy.getService());
		}

		XXServiceDef xServiceDef = daoMgr.getXXServiceDef().findByName(service.getType());

		if(xServiceDef == null) {
			throw new Exception("service-def does not exist - name=" + service.getType());
		}

		Long   zoneId   = RangerSecurityZone.RANGER_UNZONED_SECURITY_ZONE_ID;
		String zoneName = policy.getZoneName();
		if (StringUtils.isNotEmpty(zoneName)) {
			RangerSecurityZone zone = getSecurityZone(zoneName);
			if (zone == null) {
				throw new Exception("zone does not exist - name=" + zoneName);
			} else {
				zoneId = zone.getId();
			}
		}
		XXPolicy existing = daoMgr.getXXPolicy().findByNameAndServiceIdAndZoneId(policy.getName(), service.getId(), zoneId);

		if(existing != null) {
			throw new Exception("policy already exists: ServiceName=" + policy.getService() + "; PolicyName=" + policy.getName() + ". ID=" + existing.getId());
		}

		List<String> policyLabels = policy.getPolicyLabels();
		Set<String> uniquePolicyLabels = new TreeSet<>(policyLabels);
		policy.setVersion(Long.valueOf(1));
		updatePolicySignature(policy);

		if(populateExistingBaseFields) {
			assignedIdPolicyService.setPopulateExistingBaseFields(true);
			daoMgr.getXXPolicy().setIdentityInsert(true);

			policy = assignedIdPolicyService.create(policy, true);

			daoMgr.getXXPolicy().setIdentityInsert(false);
			daoMgr.getXXPolicy().updateSequence();
			assignedIdPolicyService.setPopulateExistingBaseFields(false);
		} else {
			policy = policyService.create(policy, true);
		}

		XXPolicy xCreatedPolicy = daoMgr.getXXPolicy().getById(policy.getId());
		policyRefUpdater.createNewPolMappingForRefTable(policy, xCreatedPolicy, xServiceDef);
		createOrMapLabels(xCreatedPolicy, uniquePolicyLabels);
		RangerPolicy createdPolicy = policyService.getPopulatedViewObject(xCreatedPolicy);

		boolean updateServiceInfoRoleVersion = false;
		if (isSupportsRolesDownloadByService()) {
			updateServiceInfoRoleVersion = isRoleDownloadRequired(createdPolicy, service);
		}
		handlePolicyUpdate(service, RangerPolicyDelta.CHANGE_TYPE_POLICY_CREATE, createdPolicy, updateServiceInfoRoleVersion);
		dataHistService.createObjectDataHistory(createdPolicy, RangerDataHistService.ACTION_CREATE);

		List<XXTrxLog> trxLogList = getTransactionLogList(createdPolicy,
				RangerPolicyService.OPERATION_IMPORT_CREATE_CONTEXT, RangerPolicyService.OPERATION_CREATE_CONTEXT);
		bizUtil.createTrxLog(trxLogList);

		return createdPolicy;
	}

	public void createOrMapLabels(XXPolicy xPolicy, Set<String> uniquePolicyLabels) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceDBStore.createOrMapLabels()");
		}

		for (String policyLabel : uniquePolicyLabels) {
			//check and create new label If does not exist
			if (StringUtils.isNotEmpty(policyLabel)) {
				transactionSynchronizationAdapter.executeOnTransactionCommit(new AssociatePolicyLabel(policyLabel, xPolicy));
			}
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== ServiceDBStore.createOrMapLabels()");
		}
	}

	private class AssociatePolicyLabel implements Runnable {
		private String   policyLabel;
		private XXPolicy xPolicy;

		AssociatePolicyLabel(String policyLabel, XXPolicy xPolicy) {
			this.policyLabel = policyLabel;
			this.xPolicy     = xPolicy;
		}

		@Override
		public void run() {
			getOrCreateLabel();
		}

		private void getOrCreateLabel() {
			if (LOG.isDebugEnabled()) {
				LOG.debug("==> AssociatePolicyLabel.getOrCreateLabel(policyId=" + xPolicy.getId() + ", label=" + policyLabel + ")");
			}

			XXPolicyLabel xxPolicyLabel = daoMgr.getXXPolicyLabels().findByName(policyLabel);

			if (xxPolicyLabel == null) {
				xxPolicyLabel = daoMgr.getXXPolicyLabels().findByName(policyLabel);

				if (xxPolicyLabel == null) {
					xxPolicyLabel = new XXPolicyLabel();
					xxPolicyLabel.setPolicyLabel(policyLabel);
					xxPolicyLabel = rangerAuditFields.populateAuditFieldsForCreate(xxPolicyLabel);
					xxPolicyLabel = daoMgr.getXXPolicyLabels().create(xxPolicyLabel);
				}
			}

			if (xxPolicyLabel != null) {
				XXPolicyLabelMap xxPolicyLabelMap = new XXPolicyLabelMap();
				xxPolicyLabelMap.setPolicyId(xPolicy.getId());
				xxPolicyLabelMap.setPolicyLabelId(xxPolicyLabel.getId());
				xxPolicyLabelMap = rangerAuditFields.populateAuditFieldsForCreate(xxPolicyLabelMap);
				daoMgr.getXXPolicyLabelMap().create(xxPolicyLabelMap);
			}
			if (LOG.isDebugEnabled()) {
				LOG.debug("<== AssociatePolicyLabel.getOrCreateLabel(policyId=" + xPolicy.getId() + ", label=" + policyLabel + ")");
			}
		}
	}

	private boolean validatePolicyItems(List<? extends RangerPolicyItem> policyItems) {

		if (CollectionUtils.isNotEmpty(policyItems)) {
			for (RangerPolicyItem policyItem : policyItems) {
				if (policyItem == null) {
					return false;
				}

				if (CollectionUtils.isEmpty(policyItem.getUsers()) && CollectionUtils.isEmpty(policyItem.getGroups()) && CollectionUtils.isEmpty(policyItem.getRoles())) {
					return false;
				}

				if (policyItem.getUsers() != null && (policyItem.getUsers().contains(null) || policyItem.getUsers().contains(""))) {
					return false;
				}

				if (policyItem.getGroups() != null && (policyItem.getGroups().contains(null) || policyItem.getGroups().contains(""))) {
					return false;
				}

				if (policyItem.getRoles() != null && (policyItem.getRoles().contains(null) || policyItem.getRoles().contains(""))) {
					return false;
				}

				if (CollectionUtils.isEmpty(policyItem.getAccesses()) || policyItem.getAccesses().contains(null)) {
					return false;
				}
				for (RangerPolicyItemAccess itemAccesses : policyItem.getAccesses()) {
					if (itemAccesses.getType() == null || itemAccesses.getIsAllowed() == null) {
						return false;
					}
				}
			}
		}

		return true;
	}

	@Override
	public RangerPolicy updatePolicy(RangerPolicy policy) throws Exception {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceDBStore.updatePolicy(" + policy + ")");
		}

		XXPolicy xxExisting = daoMgr.getXXPolicy().getById(policy.getId());
		RangerPolicy existing = policyService.getPopulatedViewObject(xxExisting);

		if(existing == null) {
			throw new Exception("no policy exists with ID=" + policy.getId());
		}

		RangerService service = getServiceByName(policy.getService());

		if(service == null) {
			throw new Exception("service does not exist - name=" + policy.getService());
		}

		XXServiceDef xServiceDef = daoMgr.getXXServiceDef().findByName(service.getType());

		if(xServiceDef == null) {
			throw new Exception("service-def does not exist - name=" + service.getType());
		}

		if(! StringUtils.equalsIgnoreCase(existing.getService(), policy.getService())) {
			throw new Exception("policy id=" + policy.getId() + " already exists in service " + existing.getService() + ". It can not be moved to service " + policy.getService());
		}
		boolean renamed = !StringUtils.equalsIgnoreCase(policy.getName(), existing.getName());

		if(renamed) {
			XXPolicy newNamePolicy = daoMgr.getXXPolicy().findByNameAndServiceIdAndZoneId(policy.getName(), service.getId(), xxExisting.getZoneId());

			if(newNamePolicy != null) {
				throw new Exception("another policy already exists with name '" + policy.getName() + "'. ID=" + newNamePolicy.getId());
			}
		}
		List<String> policyLabels = policy.getPolicyLabels();
		Set<String> uniquePolicyLabels = new TreeSet<>(policyLabels);
		policy.setCreateTime(xxExisting.getCreateTime());
		if (StringUtils.isEmpty(policy.getGuid())) {
			policy.setGuid(xxExisting.getGuid());
		}
		policy.setVersion(xxExisting.getVersion());

		List<XXTrxLog> trxLogList = policyService.getTransactionLog(policy, xxExisting, existing, RangerPolicyService.OPERATION_UPDATE_CONTEXT);

		updatePolicySignature(policy);

		policy = policyService.update(policy);
		XXPolicy newUpdPolicy = daoMgr.getXXPolicy().getById(policy.getId());

		policyRefUpdater.cleanupRefTables(policy);
		deleteExistingPolicyLabel(policy);

		policyRefUpdater.createNewPolMappingForRefTable(policy, newUpdPolicy, xServiceDef);
		createOrMapLabels(newUpdPolicy, uniquePolicyLabels);
		RangerPolicy updPolicy = policyService.getPopulatedViewObject(newUpdPolicy);

		boolean updateServiceInfoRoleVersion = false;
		if (isSupportsRolesDownloadByService()) {
			updateServiceInfoRoleVersion = isRoleDownloadRequired(updPolicy, service);
		}
		handlePolicyUpdate(service, RangerPolicyDelta.CHANGE_TYPE_POLICY_UPDATE, updPolicy, updateServiceInfoRoleVersion);
		dataHistService.createObjectDataHistory(updPolicy, RangerDataHistService.ACTION_UPDATE);

		bizUtil.createTrxLog(trxLogList);

		return updPolicy;
	}

	@Override
	public void deletePolicy(RangerPolicy policy) throws Exception {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceDBStore.deletePolicy(" + policy + ")");
		}

		if(policy == null) {
			throw new Exception("No such policy exists");
		}

		String policyName = policy.getName();
		RangerService service = getServiceByName(policy.getService());

		if(service == null) {
			throw new Exception("service does not exist - name='" + policy.getService());
		}

		Long version = policy.getVersion();
		if(version == null) {
			version = Long.valueOf(1);
			LOG.info("Found Version Value: `null`, so setting value of version to 1, While updating object, version should not be null.");
		} else {
			version = Long.valueOf(version.longValue() + 1);
		}

		policy.setVersion(version);

		List<XXTrxLog> trxLogList = getTransactionLogList(policy, RangerPolicyService.OPERATION_IMPORT_DELETE_CONTEXT,
				RangerPolicyService.OPERATION_DELETE_CONTEXT);

		policyRefUpdater.cleanupRefTables(policy);
		deleteExistingPolicyLabel(policy);
		policyService.delete(policy);

		handlePolicyUpdate(service, RangerPolicyDelta.CHANGE_TYPE_POLICY_DELETE, policy, false);

		dataHistService.createObjectDataHistory(policy, RangerDataHistService.ACTION_DELETE);

		bizUtil.createTrxLog(trxLogList);

		LOG.info("Policy Deleted Successfully. PolicyName : " + policyName);
	}

	@Override
	public void deletePolicy(RangerPolicy policy, RangerService service) throws Exception {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceDBStore.deletePolicy()");
		}
		if(policy != null) {
			if(service==null) {
				service = getServiceByName(policy.getService());
			}
			if(service != null) {
				String policyName = policy.getName();
				if(LOG.isDebugEnabled()) {
					LOG.debug("Deleting Policy, policyName: " + policyName);
				}
				Long version = policy.getVersion();
				if(version == null) {
					version = Long.valueOf(1);
					LOG.info("Found Version Value: `null`, so setting value of version to 1, While updating object, version should not be null.");
				} else {
					version = Long.valueOf(version.longValue() + 1);
				}
				policy.setVersion(version);
				policyRefUpdater.cleanupRefTables(policy);
				deleteExistingPolicyLabel(policy);
				policyService.delete(policy);
				List<XXTrxLog> trxLogList = getTransactionLogList(policy, RangerPolicyService.OPERATION_IMPORT_DELETE_CONTEXT, RangerPolicyService.OPERATION_DELETE_CONTEXT);
				handlePolicyUpdate(service, RangerPolicyDelta.CHANGE_TYPE_POLICY_DELETE, policy, false);
				dataHistService.createObjectDataHistory(policy, RangerDataHistService.ACTION_DELETE);
				bizUtil.createTrxLog(trxLogList);
			}
		}
		if(LOG.isDebugEnabled()) {
			LOG.debug("<== ServiceDBStore.deletePolicy()");
		}
	}

	List<XXTrxLog> getTransactionLogList(RangerPolicy policy, int operationImportContext, int operationContext) {
		List<XXTrxLog> trxLogList;
		StackTraceElement[] trace = Thread.currentThread().getStackTrace();
		if (trace.length > 3 && (StringUtils.contains(trace[4].getMethodName(), "import") ||
    StringUtils.contains(trace[5].getMethodName(), "import"))) {
      trxLogList = policyService.getTransactionLog(policy, operationImportContext);
		} else {
			trxLogList = policyService.getTransactionLog(policy, operationContext);
		}
		return trxLogList;
	}

	@Override
	public boolean policyExists(Long id) throws Exception {
		return daoMgr.getXXPolicy().getCountById(id) > 0;
	}

	@Override
	public RangerPolicy getPolicy(Long id) throws Exception {
		return policyService.read(id);
	}

	public RangerPolicy getPolicy(String guid, String serviceName, String zoneName) throws Exception {
		RangerPolicy ret = null;
		if (StringUtils.isNotBlank(guid) && StringUtils.isNotBlank(serviceName)) {
			XXPolicy xPolicy = daoMgr.getXXPolicy().findPolicyByGUIDAndServiceNameAndZoneName(guid, serviceName, zoneName);
			if (xPolicy != null) {
				ret = policyService.getPopulatedViewObject(xPolicy);
			}
		}
		return ret;
	}

	@Override
	public List<RangerPolicy> getPolicies(SearchFilter filter) throws Exception {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceDBStore.getPolicies()");
		}
		Boolean fetchTagPolicies     = Boolean.valueOf(filter.getParam(SearchFilter.FETCH_TAG_POLICIES));
		Boolean fetchAllZonePolicies = Boolean.valueOf(filter.getParam(SearchFilter.FETCH_ZONE_UNZONE_POLICIES));
		String  zoneName             = filter.getParam(SearchFilter.ZONE_NAME);

		List<RangerPolicy> ret = new ArrayList<RangerPolicy>();
		RangerPolicyList policyList = searchRangerPolicies(filter);
		List<RangerPolicy> resourcePolicies = policyList.getPolicies();
		List<RangerPolicy> tagPolicies = new ArrayList<RangerPolicy>();

		if(fetchTagPolicies) {
			tagPolicies = searchRangerTagPoliciesOnBasisOfServiceName(resourcePolicies);
			Iterator<RangerPolicy> itr = tagPolicies.iterator();
			while (itr.hasNext()) {
				RangerPolicy pol = (RangerPolicy) itr.next();
				if(!fetchAllZonePolicies) {
					if(StringUtils.isNotEmpty(zoneName)) {
						if(!zoneName.equals(pol.getZoneName())){
							itr.remove();
						}
					} else {
						if(StringUtils.isNotEmpty(pol.getZoneName())) {
							itr.remove();
						}
					}
				}
			}
		}
		if(LOG.isDebugEnabled()) {
			LOG.debug("<== ServiceDBStore.getPolicies()");
		}
		ret.addAll(resourcePolicies);
		ret.addAll(tagPolicies);
		return ret;
	}

	private List<RangerPolicy> searchRangerTagPoliciesOnBasisOfServiceName(List<RangerPolicy> allExceptTagPolicies) throws Exception {
		Set<String> rangerServiceNames = new HashSet<String>();
		for(RangerPolicy pol : allExceptTagPolicies) {
			rangerServiceNames.add(pol.getService());
		}
		List<RangerPolicy> retPolicies = new ArrayList<RangerPolicy>();
		for(String eachRangerService : rangerServiceNames) {
			List<RangerPolicy> policies = new ArrayList<RangerPolicy>();
				RangerService rangerServiceObj = getServiceByName(eachRangerService);
				RangerService rangerTagService = getServiceByName(rangerServiceObj.getTagService());
				if(rangerTagService != null) {
					ServicePolicies servicePolicies = RangerServicePoliciesCache.getInstance().getServicePolicies(rangerTagService.getName(),rangerTagService.getId(), -1L, true, this);
					policies = servicePolicies != null ? servicePolicies.getPolicies() : null;
					retPolicies.addAll(policies);
				}
			}
		return retPolicies;
	}

	@Override
	public Long getPolicyId(final Long serviceId, final String policyName, final Long zoneId) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceDBStore.getPolicyId()");
		}
		Long ret = null;
		XXPolicy xxPolicy = daoMgr.getXXPolicy().findByNameAndServiceIdAndZoneId(policyName, serviceId, zoneId);
		if (xxPolicy != null) {
			ret = xxPolicy.getId();
		}
		if(LOG.isDebugEnabled()) {
			LOG.debug("<== ServiceDBStore.getPolicyId()");
		}
		return ret;
	}


	public void getPoliciesInExcel(List<RangerPolicy> policies, HttpServletResponse response) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceDBStore.getPoliciesInExcel()");
		}
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		String excelFileName = "Ranger_Policies_"+timeStamp+".xls";
		writeExcel(policies, excelFileName, response);
	}

	public void getPoliciesInCSV(List<RangerPolicy> policies,
			HttpServletResponse response) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceDBStore.getPoliciesInCSV()");
		}
		ServletOutputStream out = null;
		String CSVFileName = null;
		try {
			String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
			CSVFileName = "Ranger_Policies_" + timeStamp + ".csv";
			out = response.getOutputStream();
			StringBuilder sb = writeCSV(policies, CSVFileName, response);
			IOUtils.write(sb.toString(), out, "UTF-8");
		} catch (Exception e) {
			LOG.error("Error while generating report file " + CSVFileName, e);
			e.printStackTrace();
		} finally {
			try {
				if (out != null) {
					out.flush();
					out.close();
				}
			} catch (Exception ex) {
			}
		}
	}
	
	public void getPoliciesInJson(List<RangerPolicy> policies,
			HttpServletResponse response) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceDBStore.getPoliciesInJson()");
		}
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		String jsonFileName = "Ranger_Policies_" + timeStamp + ".json";
		writeJson(policies, jsonFileName, response);
	}

	public PList<RangerPolicy> getPaginatedPolicies(SearchFilter filter) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceDBStore.getPaginatedPolicies(+ " + filter + ")");
		}

		RangerPolicyList policyList = searchRangerPolicies(filter);

		if (LOG.isDebugEnabled()) {
			LOG.debug("before filter: count=" + policyList.getListSize());
		}
		predicateUtil.applyFilter(policyList.getPolicies(), filter);
		if (LOG.isDebugEnabled()) {
			LOG.debug("after filter: count=" + policyList.getListSize());
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== ServiceDBStore.getPaginatedPolicies(" + filter + "): count=" + policyList.getListSize());
		}


		return new PList<RangerPolicy>(policyList.getPolicies(), policyList.getStartIndex(), policyList.getPageSize(), policyList.getTotalCount(),
				policyList.getResultSize(), policyList.getSortType(), policyList.getSortBy());

	}

	@Override
	public List<RangerPolicy> getServicePolicies(Long serviceId, SearchFilter filter) throws Exception {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceDBStore.getServicePolicies(" + serviceId + ")");
		}
		String zoneName = filter.getParam(SearchFilter.FETCH_ZONE_NAME);
		String denyCondition = filter.getParam(SearchFilter.FETCH_DENY_CONDITION);
		XXService service = daoMgr.getXXService().getById(serviceId);

		if (service == null) {
			throw new Exception("service does not exist - id='" + serviceId);
		}

		List<RangerPolicy> ret = getServicePolicies(service, filter);
		if(!"true".equalsIgnoreCase(filter.getParam(SearchFilter.FETCH_ZONE_UNZONE_POLICIES))) {
			if(StringUtils.isBlank(zoneName) && StringUtils.isBlank(denyCondition)) {
				ret = noZoneFilter(ret);
			}
		}
		if(LOG.isDebugEnabled()) {
			LOG.debug("<== ServiceDBStore.getServicePolicies(" + serviceId + ") : policy-count=" + (ret == null ? 0 : ret.size()));
		}
		return ret;

	}

	public List<RangerPolicy> noZoneFilter(List<RangerPolicy> servicePolicies) {
		List<RangerPolicy> noZonePolicies = new ArrayList<RangerPolicy>();
		if (CollectionUtils.isNotEmpty(servicePolicies)) {
			for (RangerPolicy policy : servicePolicies) {
				if (StringUtils.isBlank(policy.getZoneName())) {
					noZonePolicies.add(policy);
				}
			}
		}
		return noZonePolicies;
	}

	public PList<RangerPolicy> getPaginatedServicePolicies(Long serviceId, SearchFilter filter) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceDBStore.getPaginatedServicePolicies(" + serviceId + ")");
		}

		XXService service = daoMgr.getXXService().getById(serviceId);

		if (service == null) {
			throw new Exception("service does not exist - id='" + serviceId);
		}

		PList<RangerPolicy> ret = getPaginatedServicePolicies(service.getName(), filter);

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== ServiceDBStore.getPaginatedServicePolicies(" + serviceId + ")");
		}
		return ret;
	}

	@Override
	public List<RangerPolicy> getServicePolicies(String serviceName, SearchFilter filter) throws Exception {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceDBStore.getServicePolicies(" + serviceName + ")");
		}

		List<RangerPolicy> ret = null;
		String zoneName = filter.getParam("zoneName");
		XXService service = daoMgr.getXXService().findByName(serviceName);

		if (service == null) {
			throw new Exception("service does not exist - name='" + serviceName);
		}

		ret = getServicePolicies(service, filter);
		if(StringUtils.isBlank(zoneName)) {
			ret = noZoneFilter(ret);
		}
		if(LOG.isDebugEnabled()) {
			LOG.debug("<== ServiceDBStore.getServicePolicies(" + serviceName + "): count=" + ((ret == null) ? 0 : ret.size()));
		}

		return ret;
	}

	private List<RangerPolicy> getServicePolicies(XXService service, SearchFilter filter) throws Exception {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceDBStore.getServicePolicies()");
		}

		if (service == null) {
			throw new Exception("service does not exist");
		}

		List<RangerPolicy> ret = null;

		ServicePolicies servicePolicies = RangerServicePoliciesCache.getInstance().getServicePolicies(service.getName(), service.getId(), -1L, true, this);
		final List<RangerPolicy> policies = servicePolicies != null ? servicePolicies.getPolicies() : null;

		if(policies != null && filter != null && MapUtils.isNotEmpty(filter.getParams())) {
			Map<String, String> filterResources = filter.getParamsWithPrefix(SearchFilter.RESOURCE_PREFIX, true);
			String resourceMatchScope = filter.getParam(SearchFilter.RESOURCE_MATCH_SCOPE);

			boolean useLegacyResourceSearch = true;

			Map<String, String> paramsCopy  = new HashMap<>(filter.getParams());
			SearchFilter       searchFilter = new SearchFilter(paramsCopy);

			if (MapUtils.isNotEmpty(filterResources) && resourceMatchScope != null) {
				useLegacyResourceSearch = false;
				for (Map.Entry<String, String> entry : filterResources.entrySet()) {
					searchFilter.removeParam(SearchFilter.RESOURCE_PREFIX + entry.getKey());
				}
			}

			if (LOG.isDebugEnabled()) {
				LOG.debug("Using" + (useLegacyResourceSearch ? " old " : " new ") + "way of filtering service-policies");
			}

			ret = new ArrayList<>(policies);
			predicateUtil.applyFilter(ret, searchFilter);

			if (!useLegacyResourceSearch && CollectionUtils.isNotEmpty(ret)) {
				RangerPolicyResourceMatcher.MatchScope scope;

				if (StringUtils.equalsIgnoreCase(resourceMatchScope, "self")) {
					scope = RangerPolicyResourceMatcher.MatchScope.SELF;
				} else if (StringUtils.equalsIgnoreCase(resourceMatchScope, "ancestor")) {
					scope = RangerPolicyResourceMatcher.MatchScope.ANCESTOR;
				} else if (StringUtils.equalsIgnoreCase(resourceMatchScope, "self_or_ancestor")) {
					scope = RangerPolicyResourceMatcher.MatchScope.SELF_OR_ANCESTOR;
				} else {
					// DESCENDANT match will never happen
					scope = RangerPolicyResourceMatcher.MatchScope.SELF_OR_ANCESTOR;
				}

				RangerServiceDef serviceDef = servicePolicies.getServiceDef();

				switch (scope) {
					case SELF : {
						serviceDef = RangerServiceDefHelper.getServiceDefForPolicyFiltering(serviceDef);
						break;
					}
					case ANCESTOR : {
						Map<String, String> updatedFilterResources = RangerServiceDefHelper.getFilterResourcesForAncestorPolicyFiltering(serviceDef, filterResources);
						if (MapUtils.isNotEmpty(updatedFilterResources)) {
							for (Map.Entry<String, String> entry : updatedFilterResources.entrySet()) {
								filterResources.put(entry.getKey(), entry.getValue());
							}
							scope = RangerPolicyResourceMatcher.MatchScope.SELF_OR_ANCESTOR;
						}
						break;
					}
					default:
						break;
				}

				ret = applyResourceFilter(serviceDef, ret, filterResources, searchFilter, scope);
			}
		} else {
			ret = policies;
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== ServiceDBStore.getServicePolicies(): count=" + ((ret == null) ? 0 : ret.size()));
		}

		return ret;
	}

	List<RangerPolicy> applyResourceFilter(RangerServiceDef serviceDef, List<RangerPolicy> policies, Map<String, String> filterResources, SearchFilter filter, RangerPolicyResourceMatcher.MatchScope scope) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceDBStore.applyResourceFilter(policies-size=" + policies.size() + ", filterResources=" + filterResources + ", " + scope + ")");
		}

		List<RangerPolicy> ret = new ArrayList<RangerPolicy>();

		List<RangerPolicyResourceMatcher> matchers = getMatchers(serviceDef, filterResources, filter);

		if (CollectionUtils.isNotEmpty(matchers)) {

			for (RangerPolicy policy : policies) {

				for (RangerPolicyResourceMatcher matcher : matchers) {

					if (LOG.isDebugEnabled()) {
						LOG.debug("Trying to match for policy:[" + policy + "] using RangerDefaultPolicyResourceMatcher:[" + matcher + "]");
					}

					if (matcher.isMatch(policy, scope, null)) {
						if (LOG.isDebugEnabled()) {
							LOG.debug("matched policy:[" + policy + "]");
						}
						ret.add(policy);
						break;
					}
				}
			}
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== ServiceDBStore.applyResourceFilter(policies-size=" + ret.size() + ", filterResources=" + filterResources + ", " + scope + ")");
		}

		return ret;
	}

	List<RangerPolicyResourceMatcher> getMatchers(RangerServiceDef serviceDef, Map<String, String> filterResources, SearchFilter filter) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceDBStore.getMatchers(filterResources=" + filterResources + ")");
		}

		List<RangerPolicyResourceMatcher> ret = new ArrayList<RangerPolicyResourceMatcher>();

		RangerServiceDefHelper serviceDefHelper = new RangerServiceDefHelper(serviceDef);

		String policyTypeStr = filter.getParam(SearchFilter.POLICY_TYPE);

		int[] policyTypes = RangerPolicy.POLICY_TYPES;

		if (StringUtils.isNotBlank(policyTypeStr)) {
			policyTypes = new int[1];
			policyTypes[0] = Integer.parseInt(policyTypeStr);
		}

		for (Integer policyType : policyTypes) {
			Set<List<RangerResourceDef>> validResourceHierarchies = serviceDefHelper.getResourceHierarchies(policyType, filterResources.keySet());

			if (LOG.isDebugEnabled()) {
				LOG.debug("Found " + validResourceHierarchies.size() + " valid resource hierarchies for key-set " + filterResources.keySet());
			}

			List<List<RangerResourceDef>> resourceHierarchies = new ArrayList<List<RangerResourceDef>>(validResourceHierarchies);

			for (List<RangerResourceDef> validResourceHierarchy : resourceHierarchies) {

				if (LOG.isDebugEnabled()) {
					LOG.debug("validResourceHierarchy:[" + validResourceHierarchy + "]");
				}

				Map<String, RangerPolicyResource> policyResources = new HashMap<String, RangerPolicyResource>();

				for (RangerResourceDef resourceDef : validResourceHierarchy) {
					policyResources.put(resourceDef.getName(), new RangerPolicyResource(filterResources.get(resourceDef.getName()), false, resourceDef.getRecursiveSupported()));
				}

				RangerDefaultPolicyResourceMatcher matcher = new RangerDefaultPolicyResourceMatcher();
				matcher.setServiceDef(serviceDef);
				matcher.setPolicyResources(policyResources, policyType);
				matcher.init();

				ret.add(matcher);

				if (LOG.isDebugEnabled()) {
					LOG.debug("Added matcher:[" + matcher + "]");
				}
			}
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== ServiceDBStore.getMatchers(filterResources=" + filterResources + ", " + ", count=" + ret.size() + ")");
		}

		return ret;
	}

	private List<RangerPolicy> getServicePoliciesFromDb(XXService service) throws Exception {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceDBStore.getServicePoliciesFromDb(" + service.getName() + ")");
		}

		RangerPolicyRetriever policyRetriever = new RangerPolicyRetriever(daoMgr, txManager);

		List<RangerPolicy> ret = policyRetriever.getServicePolicies(service);

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== ServiceDBStore.getServicePoliciesFromDb(" + service.getName() + "): count=" + ((ret == null) ? 0 : ret.size()));
		}

		return ret;
	}

	public PList<RangerPolicy> getPaginatedServicePolicies(String serviceName, SearchFilter filter) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceDBStore.getPaginatedServicePolicies(" + serviceName + ")");
		}

		if (filter == null) {
			filter = new SearchFilter();
		}

		filter.setParam(SearchFilter.SERVICE_NAME, serviceName);

		PList<RangerPolicy> ret = getPaginatedPolicies(filter);

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== ServiceDBStore.getPaginatedServicePolicies(" + serviceName + "): count="
					+ ((ret == null) ? 0 : ret.getListSize()));
		}

		return ret;
	}

	@Override
	public ServicePolicies getServicePoliciesIfUpdated(String serviceName, Long lastKnownVersion, boolean needsBackwardCompatibility) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceDBStore.getServicePoliciesIfUpdated(" + serviceName + ", " + lastKnownVersion + ", " + needsBackwardCompatibility + ")");
		}

		ServicePolicies ret = null;

		XXService serviceDbObj = daoMgr.getXXService().findByName(serviceName);

		if (serviceDbObj == null) {
			throw new Exception("service does not exist. name=" + serviceName);
		}

		XXServiceVersionInfo serviceVersionInfoDbObj = daoMgr.getXXServiceVersionInfo().findByServiceName(serviceName);

		if (serviceVersionInfoDbObj == null) {
			LOG.warn("serviceVersionInfo does not exist. name=" + serviceName);
		}

		if (lastKnownVersion == null || serviceVersionInfoDbObj == null || serviceVersionInfoDbObj.getPolicyVersion() == null || !lastKnownVersion.equals(serviceVersionInfoDbObj.getPolicyVersion())) {
			ret = RangerServicePoliciesCache.getInstance().getServicePolicies(serviceName, serviceDbObj.getId(), lastKnownVersion, needsBackwardCompatibility, this);
		}

		if (LOG.isDebugEnabled()) {
			RangerServicePoliciesCache.getInstance().dump();
		}

		if (ret != null && lastKnownVersion != null && lastKnownVersion.equals(ret.getPolicyVersion())) {
			// ServicePolicies are not changed
			ret = null;
		}

		if (ret != null) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Checking if resource-service:[" + ret.getServiceName() + "] is disabled");
			}
			if (!serviceDbObj.getIsenabled()) {
				ret = ServicePolicies.copyHeader(ret);
				ret.setTagPolicies(null);
			} else {
				boolean isTagServiceActive = true;

				if (ret.getTagPolicies() != null) {
					if (LOG.isDebugEnabled()) {
						LOG.debug("Checking if tag-service:[" + ret.getTagPolicies().getServiceName() + "] is disabled");
					}
					String tagServiceName = ret.getTagPolicies().getServiceName();

					if (StringUtils.isNotEmpty(tagServiceName)) {
						XXService tagService = daoMgr.getXXService().findByName(tagServiceName);
						if (tagService == null || !tagService.getIsenabled()) {
							if (LOG.isDebugEnabled()) {
								LOG.debug("tag-service:[" + tagServiceName + "] is disabled");
							}
							isTagServiceActive = false;
						}
					} else {
						isTagServiceActive = false;
					}
				} else {
					isTagServiceActive = false;
				}

				if (!isTagServiceActive) {
					ServicePolicies copy = ServicePolicies.copyHeader(ret);
					copy.setTagPolicies(null);
					List<RangerPolicy> copyPolicies = ret.getPolicies() != null ? new ArrayList<>(ret.getPolicies()) : null;
					List<RangerPolicyDelta> copyPolicyDeltas = ret.getPolicyDeltas() != null ? new ArrayList<>(ret.getPolicyDeltas()) : null;
					copy.setPolicies(copyPolicies);
					copy.setPolicyDeltas(copyPolicyDeltas);
					ret = copy;
				}
			}

			Map<String, RangerSecurityZone.RangerSecurityZoneService> securityZones = securityZoneStore.getSecurityZonesForService(serviceName);
			ServicePolicies updatedServicePolicies = ret;
			if (MapUtils.isNotEmpty(securityZones)) {
				updatedServicePolicies = getUpdatedServicePoliciesForZones(ret, securityZones);
				patchAssociatedTagServiceInSecurityZoneInfos(updatedServicePolicies);
			}

			if (lastKnownVersion == null || lastKnownVersion == -1L || needsBackwardCompatibility) {
				ret = filterServicePolicies(updatedServicePolicies);
			} else {
				ret = updatedServicePolicies;
			}

			ret.setServiceConfig(getServiceConfigForPlugin(ret.getServiceId()));

			if (ret.getTagPolicies() != null && ret.getTagPolicies().getServiceId() != null) {
				ret.getTagPolicies().setServiceConfig(getServiceConfigForPlugin(ret.getTagPolicies().getServiceId()));
			}

		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== ServiceDBStore.getServicePoliciesIfUpdated(" + serviceName + ", " + lastKnownVersion + ", " + needsBackwardCompatibility + "): count=" + ((ret == null || ret.getPolicies() == null) ? 0 : ret.getPolicies().size()));
		}

		return ret;
	}

	@Override
	public Long getServicePolicyVersion(String serviceName) {

		XXServiceVersionInfo serviceVersionInfoDbObj = daoMgr.getXXServiceVersionInfo().findByServiceName(serviceName);

		return serviceVersionInfoDbObj != null ? serviceVersionInfoDbObj.getPolicyVersion() : null;
	}

	@Override
	public ServicePolicies getServicePolicyDeltasOrPolicies(String serviceName, Long lastKnownVersion) throws Exception {
		boolean getOnlyDeltas = false;
		if (LOG.isDebugEnabled()) {
			LOG.debug("Support for incremental policy updates enabled using \"ranger.admin" + RangerCommonConstants.RANGER_ADMIN_SUFFIX_POLICY_DELTA + "\" configuation parameter :[" + SUPPORTS_POLICY_DELTAS +"]");
		}
		return getServicePolicies(serviceName, lastKnownVersion, getOnlyDeltas, SUPPORTS_POLICY_DELTAS);
	}

	@Override
	public ServicePolicies getServicePolicyDeltas(String serviceName, Long lastKnownVersion) throws Exception {
		boolean getOnlyDeltas = true;
		if (LOG.isDebugEnabled()) {
			LOG.debug("Support for incremental policy updates enabled using \"ranger.admin" + RangerCommonConstants.RANGER_ADMIN_SUFFIX_POLICY_DELTA + "\" configuation parameter :[" + SUPPORTS_POLICY_DELTAS +"]");
		}
		return getServicePolicies(serviceName, lastKnownVersion, getOnlyDeltas, SUPPORTS_POLICY_DELTAS);
	}

	@Override
	public ServicePolicies getServicePolicies(String serviceName, Long lastKnownVersion) throws Exception {
		boolean getOnlyDeltas = false;
		return getServicePolicies(serviceName, lastKnownVersion, getOnlyDeltas, false);
	}

	private ServicePolicies getServicePolicies(String serviceName, Long lastKnownVersion, boolean getOnlyDeltas, boolean isDeltaEnabled) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceDBStore.getServicePolicies(" + serviceName  + ", " + lastKnownVersion + ")");
		}

		ServicePolicies ret = null;

		XXService serviceDbObj = daoMgr.getXXService().findByName(serviceName);

		if (serviceDbObj == null) {
			throw new Exception("service does not exist. name=" + serviceName);
		}

		XXServiceVersionInfo serviceVersionInfoDbObj = daoMgr.getXXServiceVersionInfo().findByServiceName(serviceName);

		if (serviceVersionInfoDbObj == null) {
			LOG.warn("serviceVersionInfo does not exist. name=" + serviceName);
		}

		RangerServiceDef serviceDef = getServiceDef(serviceDbObj.getType());

		if (serviceDef == null) {
			throw new Exception("service-def does not exist. id=" + serviceDbObj.getType());
		}
		String serviceType = serviceDef.getName();

		String auditMode = getAuditMode(serviceType, serviceName);

		XXService tagServiceDbObj = null;
		RangerServiceDef tagServiceDef = null;
		XXServiceVersionInfo tagServiceVersionInfoDbObj= null;

		if (serviceDbObj.getTagService() != null) {
			tagServiceDbObj = daoMgr.getXXService().getById(serviceDbObj.getTagService());
		}

		if (tagServiceDbObj != null) {
			tagServiceDef = getServiceDef(tagServiceDbObj.getType());

			if (tagServiceDef == null) {
				throw new Exception("service-def does not exist. id=" + tagServiceDbObj.getType());
			}

			tagServiceVersionInfoDbObj = daoMgr.getXXServiceVersionInfo().findByServiceId(serviceDbObj.getTagService());

			if (tagServiceVersionInfoDbObj == null) {
				LOG.warn("serviceVersionInfo does not exist. name=" + tagServiceDbObj.getName());
			}
		}

		if (isDeltaEnabled) {
			ret = getServicePoliciesWithDeltas(serviceDef, serviceDbObj, tagServiceDef, tagServiceDbObj, lastKnownVersion);
		}

		if (ret != null) {
			ret.setPolicyUpdateTime(serviceVersionInfoDbObj == null ? null : serviceVersionInfoDbObj.getPolicyUpdateTime());
			ret.setAuditMode(auditMode);
			if (ret.getTagPolicies() != null) {
				ret.getTagPolicies().setPolicyUpdateTime(tagServiceVersionInfoDbObj == null ? null : tagServiceVersionInfoDbObj.getPolicyUpdateTime());
				ret.getTagPolicies().setAuditMode(auditMode);
			}
		} else if (!getOnlyDeltas) {
			ServicePolicies.TagPolicies tagPolicies = null;

			if (tagServiceDbObj != null) {

				tagPolicies = new ServicePolicies.TagPolicies();

				tagPolicies.setServiceId(tagServiceDbObj.getId());
				tagPolicies.setServiceName(tagServiceDbObj.getName());
				tagPolicies.setPolicyVersion(tagServiceVersionInfoDbObj == null ? null : tagServiceVersionInfoDbObj.getPolicyVersion());
				tagPolicies.setPolicyUpdateTime(tagServiceVersionInfoDbObj == null ? null : tagServiceVersionInfoDbObj.getPolicyUpdateTime());
				tagPolicies.setPolicies(getServicePoliciesFromDb(tagServiceDbObj));
				tagPolicies.setServiceDef(tagServiceDef);
				tagPolicies.setAuditMode(auditMode);
			}
			List<RangerPolicy> policies = getServicePoliciesFromDb(serviceDbObj);

			ret = new ServicePolicies();

			ret.setServiceId(serviceDbObj.getId());
			ret.setServiceName(serviceDbObj.getName());
			ret.setPolicyVersion(serviceVersionInfoDbObj == null ? null : serviceVersionInfoDbObj.getPolicyVersion());
			ret.setPolicyUpdateTime(serviceVersionInfoDbObj == null ? null : serviceVersionInfoDbObj.getPolicyUpdateTime());
			ret.setPolicies(policies);
			ret.setServiceDef(serviceDef);
			ret.setAuditMode(auditMode);
			ret.setTagPolicies(tagPolicies);
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== ServiceDBStore.getServicePolicies(" + serviceName + ", " + lastKnownVersion + "): count=" + ((ret == null || ret.getPolicies() == null) ? 0 : ret.getPolicies().size()) + ", delta-count=" + ((ret == null || ret.getPolicyDeltas() == null) ? 0 : ret.getPolicyDeltas().size()));
		}

		return ret;
	}

	private static class RangerPolicyDeltaComparator implements Comparator<RangerPolicyDelta>, java.io.Serializable {
		@Override
		public int compare(RangerPolicyDelta me, RangerPolicyDelta other) {
			return Long.compare(me.getId(), other.getId());
		}
	}

	private static final Comparator<RangerPolicyDelta> POLICY_DELTA_ID_COMPARATOR = new RangerPolicyDeltaComparator();

	private static List<RangerPolicyDelta> compressDeltas(List<RangerPolicyDelta> deltas) {
		List<RangerPolicyDelta>                  ret               = new ArrayList<>();

		final Map<Long, List<RangerPolicyDelta>> policyDeltaMap    = new HashMap<>();

		for (RangerPolicyDelta delta : deltas) {
			Long                    policyId        = delta.getPolicyId();
			List<RangerPolicyDelta> oldPolicyDeltas = policyDeltaMap.get(policyId);

			if (oldPolicyDeltas == null) {
				oldPolicyDeltas = new ArrayList<>();
				policyDeltaMap.put(policyId, oldPolicyDeltas);
			}
			oldPolicyDeltas.add(delta);
		}

		for (Map.Entry<Long, List<RangerPolicyDelta>> entry : policyDeltaMap.entrySet()) {
			List<RangerPolicyDelta> policyDeltas = entry.getValue();

			if (policyDeltas.size() == 1) {
				ret.addAll(policyDeltas);
			} else { // Will always be greater than 1
				List<RangerPolicyDelta> policyDeltasForPolicy = new ArrayList<>();
				RangerPolicyDelta       first                 = policyDeltas.get(0);

				policyDeltasForPolicy.add(first);

				int index = 1;

				switch (first.getChangeType()) {
					case RangerPolicyDelta.CHANGE_TYPE_POLICY_CREATE:
						while (index < policyDeltas.size()) {
							RangerPolicyDelta policyDelta = policyDeltas.get(index);
							switch (policyDelta.getChangeType()) {
								case RangerPolicyDelta.CHANGE_TYPE_POLICY_CREATE:
									LOG.error("Multiple policy creates!! [" + policyDelta + "]");
									policyDeltasForPolicy = null;
									break;
								case RangerPolicyDelta.CHANGE_TYPE_POLICY_UPDATE:
									for (int i = index + 1; i < policyDeltas.size(); i++) {
										RangerPolicyDelta next = policyDeltas.get(i);
										if (next.getChangeType() == RangerPolicyDelta.CHANGE_TYPE_POLICY_UPDATE) {
											index = i;
										} else {
											break;
										}
									}
									index++;
									break;
								case RangerPolicyDelta.CHANGE_TYPE_POLICY_DELETE:
									if (policyDeltas.size() == index + 1) {
										// Last one
										policyDeltasForPolicy.clear();
										index++;
									} else {
										LOG.error("CHANGE_TYPE_POLICY_DELETE should be the last policyDelta, found:[" + policyDeltas.get(index+1) +"]");
										policyDeltasForPolicy = null;
									}
									break;
								default:
									break;
							}
							if (policyDeltasForPolicy == null) {
								break;
							}
						}
						break;
					case RangerPolicyDelta.CHANGE_TYPE_POLICY_UPDATE:
						while (index < policyDeltas.size()) {
							RangerPolicyDelta policyDelta = policyDeltas.get(index);

							switch (policyDelta.getChangeType()) {
								case RangerPolicyDelta.CHANGE_TYPE_POLICY_CREATE:
									LOG.error("Should not get here! policy is created after it is updated!! policy-delta:[" + policyDelta + "]");
									policyDeltasForPolicy = null;
									break;
								case RangerPolicyDelta.CHANGE_TYPE_POLICY_UPDATE:
									for (int i = index + 1; i < policyDeltas.size(); i++) {
										RangerPolicyDelta next = policyDeltas.get(i);
										if (next.getChangeType() == RangerPolicyDelta.CHANGE_TYPE_POLICY_UPDATE) {
											index = i;
										} else {
											break;
										}
									}
									policyDeltasForPolicy.clear();
									policyDeltasForPolicy.add(policyDeltas.get(index));
									index++;
									break;
								case RangerPolicyDelta.CHANGE_TYPE_POLICY_DELETE:
									if (policyDeltas.size() == index + 1) {
										// Last one
										policyDeltasForPolicy.clear();
										policyDeltasForPolicy.add(policyDeltas.get(index));
										index++;
									} else {
										LOG.error("CHANGE_TYPE_POLICY_DELETE should be the last policyDelta, found:[" + policyDeltas.get(index+1) +"]");
										policyDeltasForPolicy = null;
									}
									break;
								default:
									break;
							}
							if (policyDeltasForPolicy == null) {
								break;
							}
						}
						break;
					case RangerPolicyDelta.CHANGE_TYPE_POLICY_DELETE:
						LOG.error("CHANGE_TYPE_POLICY_DELETE should be the last policyDelta, found:[" + policyDeltas.get(index) +"]");
						policyDeltasForPolicy = null;
						break;
					default:
						LOG.error("Should not get here for valid policy-delta:[" + first + "]");
						break;
				}
				if (policyDeltasForPolicy != null) {
					ret.addAll(policyDeltasForPolicy);
				} else {
					ret = null;
					break;
				}
			}
		}

		if (ret != null) {
			ret.sort(POLICY_DELTA_ID_COMPARATOR);
		}

		return ret;

	}

	ServicePolicies getServicePoliciesWithDeltas(RangerServiceDef serviceDef, XXService service, RangerServiceDef tagServiceDef, XXService tagService, Long lastKnownVersion) {
		ServicePolicies ret = null;

		// if lastKnownVersion != -1L : try and get deltas. Get delta for serviceName first. Find id of the delta
		// returned first in the list. and then find all ids greater than that for corresponding tag service.
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceDBStore.getServicePoliciesWithDeltas(serviceType=" + serviceDef.getName() + ", serviceId=" + service.getId()
					+", tagServiceId=" + (tagService != null ? tagService.getId() : null) + ", lastKnownVersion=" + lastKnownVersion + ")");
		}
		if (lastKnownVersion != -1L) {

			List<RangerPolicyDelta> resourcePolicyDeltas;
			List<RangerPolicyDelta> tagPolicyDeltas = null;
			Long                    retrievedPolicyVersion = null;
			Long                    retrievedTagPolicyVersion = null;

			String componentServiceType = serviceDef.getName();

			boolean isValid;

			resourcePolicyDeltas = daoMgr.getXXPolicyChangeLog().findLaterThan(lastKnownVersion, service.getId());
			if (CollectionUtils.isNotEmpty(resourcePolicyDeltas)) {
				isValid = RangerPolicyDeltaUtil.isValidDeltas(resourcePolicyDeltas, componentServiceType);

				if (isValid) {
					retrievedPolicyVersion = resourcePolicyDeltas.get(resourcePolicyDeltas.size() - 1).getPoliciesVersion();
				} else {
					LOG.warn("Resource policy-Deltas :[" + resourcePolicyDeltas + "] from version :[" + lastKnownVersion + "] are not valid");
				}

				if (isValid && tagService != null) {
					Long id = resourcePolicyDeltas.get(0).getId();
					tagPolicyDeltas = daoMgr.getXXPolicyChangeLog().findGreaterThan(id, tagService.getId());


					if (CollectionUtils.isNotEmpty(tagPolicyDeltas)) {
						String tagServiceType = tagServiceDef.getName();

						isValid = RangerPolicyDeltaUtil.isValidDeltas(tagPolicyDeltas, tagServiceType);

						if (isValid) {
							retrievedTagPolicyVersion = tagPolicyDeltas.get(tagPolicyDeltas.size() - 1).getPoliciesVersion();
						} else {
							LOG.warn("Tag policy-Deltas :[" + tagPolicyDeltas + "] for service-version :[" + lastKnownVersion + "] and delta-id :[" + id + "] are not valid");
						}
					}
				}

				if (isValid) {
					if (CollectionUtils.isNotEmpty(tagPolicyDeltas)) {
						// To ensure that resource-policy-deltas with service-type of 'tag' are ignored after validation
						resourcePolicyDeltas.removeIf(rangerPolicyDelta -> StringUtils.equals(EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_TAG_NAME, rangerPolicyDelta.getServiceType()));

						resourcePolicyDeltas.addAll(tagPolicyDeltas);
					}

					List<RangerPolicyDelta> compressedDeltas = compressDeltas(resourcePolicyDeltas);

					if (compressedDeltas != null) {
						ret = new ServicePolicies();
						ret.setServiceId(service.getId());
						ret.setServiceName(service.getName());
						ret.setServiceDef(serviceDef);
						ret.setPolicies(null);
						ret.setPolicyDeltas(compressedDeltas);
						ret.setPolicyVersion(retrievedPolicyVersion);

						if (tagServiceDef != null && tagService != null) {
							ServicePolicies.TagPolicies tagPolicies = new ServicePolicies.TagPolicies();
							tagPolicies.setServiceDef(tagServiceDef);
							tagPolicies.setServiceId(tagService.getId());
							tagPolicies.setServiceName(tagService.getName());
							tagPolicies.setPolicies(null);
							tagPolicies.setPolicyVersion(retrievedTagPolicyVersion);
							ret.setTagPolicies(tagPolicies);
						}
					} else {
						LOG.warn("Deltas :[" + resourcePolicyDeltas + "] from version :[" + lastKnownVersion + "] after compressing are null!");
					}
				}
			} else {
				LOG.warn("No policy-deltas found for serviceId=" + service.getId() + ", tagServiceId=" + (tagService != null ? tagService.getId() : null) + ", lastKnownVersion=" + lastKnownVersion + ")");
			}
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== ServiceDBStore.getServicePoliciesWithDeltas(serviceType=" + serviceDef.getName() + ", serviceId=" + service.getId()
					+", tagServiceId=" + (tagService != null ? tagService.getId() : null) + ", lastKnownVersion=" + lastKnownVersion + ") : deltasSize=" + (ret != null && CollectionUtils.isNotEmpty(ret.getPolicyDeltas()) ? ret.getPolicyDeltas().size() : 0));
		}
		return ret;
	}

	void createDefaultPolicies(RangerService createdService) throws Exception {

		List<RangerPolicy> defaultPolicies = populateDefaultPolicies(createdService);

		if (CollectionUtils.isNotEmpty(defaultPolicies)) {

			for (RangerPolicy defaultPolicy : defaultPolicies) {
				createPolicy(defaultPolicy);
			}
		}

	}

	public void createZoneDefaultPolicies(Collection<String> serviceNames, RangerSecurityZone zone) throws Exception {

		if (CollectionUtils.isNotEmpty(serviceNames)) {

			for (String serviceName : serviceNames) {
				RangerService service = getServiceByName(serviceName);

				if (service != null) {

					List<RangerPolicy> defaultPolicies = populateDefaultPolicies(service);

					if (CollectionUtils.isNotEmpty(defaultPolicies)) {

						String zoneName = zone.getName();

						for (RangerPolicy defaultPolicy : defaultPolicies) {

							defaultPolicy.setZoneName(zoneName);

							createPolicy(defaultPolicy);
						}
					}
				}
			}
		}
	}

	public void deleteZonePolicies(Collection<String> serviceNames, Long zoneId) throws Exception {
		if (CollectionUtils.isNotEmpty(serviceNames)) {
			XXPolicyDao policyDao = daoMgr.getXXPolicy();
			for (String serviceName : serviceNames) {
				RangerService service = getServiceByName(serviceName);
				List<Long> policyIds = policyDao.findPolicyIdsByServiceNameAndZoneId(serviceName, zoneId);
				if (CollectionUtils.isNotEmpty(policyIds)) {
					List<RangerPolicy> rangerPolicyList = new ArrayList<RangerPolicy>();
					for (Long id : policyIds) {
						rangerPolicyList.add(getPolicy(id));
					}
					long totalDeletedPolicies = 0;
					for (RangerPolicy rangerPolicy : rangerPolicyList) {
						deletePolicy(rangerPolicy, service);
						totalDeletedPolicies = totalDeletedPolicies + 1;
						if (totalDeletedPolicies % RangerBizUtil.policyBatchSize == 0) {
							bizUtil.bulkModeOnlyFlushAndClear();
						}
					}
					bizUtil.bulkModeOnlyFlushAndClear();
				}
			}
		}
	}

	List<RangerPolicy> populateDefaultPolicies(RangerService service) throws Exception {

		List<RangerPolicy> ret = null;

		RangerBaseService svc = serviceMgr.getRangerServiceByService(service, this);

		if (svc != null) {

			List<String> serviceCheckUsers = getServiceCheckUsers(service);
                        List<String> users = new ArrayList<String>();

                        /*Need to create ambari service check user before initiating policy creation. */
                        if(serviceCheckUsers != null){
                                for (String userName : serviceCheckUsers) {
                                        if(!StringUtils.isEmpty(userName)){
                                                VXUser vXUser = null;
                                                XXUser xxUser = daoMgr.getXXUser().findByUserName(userName);
                                                if (xxUser != null) {
                                                        vXUser = xUserService.populateViewBean(xxUser);
                                                } else {
                                                        vXUser = xUserMgr.createServiceConfigUser(userName);
                                                        LOG.info("Creating Ambari Service Check User : "+vXUser.getName());
                                                }
                                                if(vXUser != null){
                                                        users.add(vXUser.getName());
                                                }
                                        }
                                }
                        }

			List<RangerPolicy> defaultPolicies = svc.getDefaultRangerPolicies();

			if (CollectionUtils.isNotEmpty(defaultPolicies)) {

				createDefaultPolicyUsersAndGroups(defaultPolicies);

				for (RangerPolicy defaultPolicy : defaultPolicies) {
                                        if (CollectionUtils.isNotEmpty(users) && StringUtils.equalsIgnoreCase(defaultPolicy.getService(), service.getName())) {
						RangerPolicyItem defaultAllowPolicyItem = CollectionUtils.isNotEmpty(defaultPolicy.getPolicyItems()) ? defaultPolicy.getPolicyItems().get(0) : null;

						if (defaultAllowPolicyItem == null) {
							LOG.error("There is no allow-policy-item in the default-policy:[" + defaultPolicy + "]");
						} else {
							RangerPolicy.RangerPolicyItem policyItem = new RangerPolicy.RangerPolicyItem();

                                                        policyItem.setUsers(users);
							policyItem.setAccesses(defaultAllowPolicyItem.getAccesses());
							policyItem.setDelegateAdmin(true);

							defaultPolicy.getPolicyItems().add(policyItem);
						}
					}

					boolean isPolicyItemValid = validatePolicyItems(defaultPolicy.getPolicyItems())
							&& validatePolicyItems(defaultPolicy.getDenyPolicyItems())
							&& validatePolicyItems(defaultPolicy.getAllowExceptions())
							&& validatePolicyItems(defaultPolicy.getDenyExceptions())
							&& validatePolicyItems(defaultPolicy.getDataMaskPolicyItems())
							&& validatePolicyItems(defaultPolicy.getRowFilterPolicyItems());

					if (isPolicyItemValid) {
						if (ret == null) {
							ret = new ArrayList<>();
						}
						ret.add(defaultPolicy);
					} else {
						LOG.warn("Default policy won't be created,since policyItems not valid-either users/groups not present or access not present in policy.");
					}
				}
			}
		}
		return ret;
	}

	void createDefaultPolicyUsersAndGroups(List<RangerPolicy> defaultPolicies) {
		Set<String> defaultPolicyUsers = new HashSet<String>();
		Set<String> defaultPolicyGroups = new HashSet<String>();

		for (RangerPolicy defaultPolicy : defaultPolicies) {

			for (RangerPolicyItem defaultPolicyItem : defaultPolicy.getPolicyItems()) {
				defaultPolicyUsers.addAll(defaultPolicyItem.getUsers());
				defaultPolicyGroups.addAll(defaultPolicyItem.getGroups());
			}
			for (RangerPolicyItem defaultPolicyItem : defaultPolicy.getAllowExceptions()) {
				defaultPolicyUsers.addAll(defaultPolicyItem.getUsers());
				defaultPolicyGroups.addAll(defaultPolicyItem.getGroups());
			}
			for (RangerPolicyItem defaultPolicyItem : defaultPolicy.getDenyPolicyItems()) {
				defaultPolicyUsers.addAll(defaultPolicyItem.getUsers());
				defaultPolicyGroups.addAll(defaultPolicyItem.getGroups());
			}
			for (RangerPolicyItem defaultPolicyItem : defaultPolicy.getDenyExceptions()) {
				defaultPolicyUsers.addAll(defaultPolicyItem.getUsers());
				defaultPolicyGroups.addAll(defaultPolicyItem.getGroups());
			}
			for (RangerPolicyItem defaultPolicyItem : defaultPolicy.getDataMaskPolicyItems()) {
				defaultPolicyUsers.addAll(defaultPolicyItem.getUsers());
				defaultPolicyGroups.addAll(defaultPolicyItem.getGroups());
			}
			for (RangerPolicyItem defaultPolicyItem : defaultPolicy.getRowFilterPolicyItems()) {
				defaultPolicyUsers.addAll(defaultPolicyItem.getUsers());
				defaultPolicyGroups.addAll(defaultPolicyItem.getGroups());
			}
		}
		for (String policyUser : defaultPolicyUsers) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Checking policyUser:[" + policyUser + "] for existence");
			}
			if (StringUtils.isNotBlank(policyUser) && !StringUtils.equals(policyUser, RangerPolicyEngine.USER_CURRENT)
					&& !StringUtils.equals(policyUser, RangerPolicyEngine.RESOURCE_OWNER)) {
                                String userName = stringUtil.getValidUserName(policyUser);
                                XXUser xxUser = daoMgr.getXXUser().findByUserName(userName);
				if (xxUser == null) {
					UserSessionBase usb = ContextUtil.getCurrentUserSession();
					if (usb != null && !usb.isKeyAdmin() && !usb.isUserAdmin() && !usb.isSpnegoEnabled()) {
						throw restErrorUtil.createRESTException("User does not exist with given username: ["
								+ policyUser + "] please use existing user", MessageEnums.OPER_NO_PERMISSION);
					}
                                        xUserMgr.createServiceConfigUser(userName);
				}
			}
		}
		for (String policyGroup : defaultPolicyGroups) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Checking policyGroup:[" + policyGroup + "] for existence");
			}
			if (StringUtils.isNotBlank(policyGroup)) {
				XXGroup xxGroup = daoMgr.getXXGroup().findByGroupName(policyGroup);
				if (xxGroup == null) {
					UserSessionBase usb = ContextUtil.getCurrentUserSession();
					if (usb != null && !usb.isKeyAdmin() && !usb.isUserAdmin() && !usb.isSpnegoEnabled()) {
						throw restErrorUtil.createRESTException("Group does not exist with given groupname: ["
								+ policyGroup + "] please use existing group", MessageEnums.OPER_NO_PERMISSION);
					}
					VXGroup vXGroup = new VXGroup();
					vXGroup.setName(policyGroup);
					vXGroup.setDescription(policyGroup);
					vXGroup.setGroupSource(RangerCommonEnums.GROUP_INTERNAL);
					vXGroup.setIsVisible(RangerCommonEnums.IS_VISIBLE);
					VXGroup createdVXGrp = xGroupService.createResource(vXGroup);
					List<XXTrxLog> trxLogList = xGroupService.getTransactionLog(createdVXGrp, "create");
					bizUtil.createTrxLog(trxLogList);
				}
			}
		}
	}

	List<String> getServiceCheckUsers(RangerService createdService) {
		List<String> ret = new ArrayList<String>();
		String userNames ="";

		Map<String, String> serviceConfig = createdService.getConfigs();

		if (serviceConfig.containsKey(SERVICE_CHECK_USER)) {
			userNames = serviceConfig.get(SERVICE_CHECK_USER);
		} else if (serviceConfig.containsKey(AMBARI_SERVICE_CHECK_USER)) {
			userNames = serviceConfig.get(AMBARI_SERVICE_CHECK_USER);
		}

		if (!StringUtils.isEmpty(userNames)) {
			String[] userList = userNames.split(",");
			for (String userName : userList) {
				if (!StringUtils.isEmpty(userName)) {
					ret.add(userName.trim());
				}
			}
		}
		return ret;
	}

	private Map<String, String> validateRequiredConfigParams(RangerService service, Map<String, String> configs) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceDBStore.validateRequiredConfigParams()");
		}
		if(configs == null) {
			return null;
		}
		
		List<XXServiceConfigDef> svcConfDefList = daoMgr.getXXServiceConfigDef()
				.findByServiceDefName(service.getType());
		for(XXServiceConfigDef svcConfDef : svcConfDefList ) {
			String confField = configs.get(svcConfDef.getName());
			
			if(svcConfDef.getIsMandatory() && stringUtil.isEmpty(confField)) {
				throw restErrorUtil.createRESTException(
						"Please provide value of mandatory: "+ svcConfDef.getName(),
						MessageEnums.INVALID_INPUT_DATA);
			}

			if (StringUtils.equals(svcConfDef.getName(), RANGER_PLUGIN_AUDIT_FILTERS) && !configs.containsKey(RANGER_PLUGIN_AUDIT_FILTERS)) {
				if (svcConfDef.getDefaultvalue() != null) {
					configs.put(RANGER_PLUGIN_AUDIT_FILTERS, svcConfDef.getDefaultvalue());
				}
			}
		}
		Map<String, String> validConfigs = new HashMap<String, String>();
		for(Entry<String, String> config : configs.entrySet()) {
			if(!stringUtil.isEmpty(config.getValue())) {
				validConfigs.put(config.getKey(), config.getValue());
			}
		}
		return validConfigs;
	}

	private void handlePolicyUpdate(RangerService service, Integer policyDeltaType, RangerPolicy policy, boolean updateServiceInfoRoleVersion) throws Exception {
		updatePolicyVersion(service, policyDeltaType, policy, updateServiceInfoRoleVersion);
	}

	public enum VERSION_TYPE { POLICY_VERSION, TAG_VERSION, ROLE_VERSION }

	private void updatePolicyVersion(RangerService service, Integer policyDeltaType, RangerPolicy policy, boolean updateServiceInfoRoleVersion) throws Exception {
		if(service == null || service.getId() == null) {
			return;
		}

		XXServiceDao serviceDao = daoMgr.getXXService();

		final XXService serviceDbObj = serviceDao.getById(service.getId());
		if(serviceDbObj == null) {
			LOG.warn("updatePolicyVersion(serviceId=" + service.getId() + "): service not found");

			return;
		}

		final RangerDaoManager daoManager  = daoMgr;
		final Long 			   serviceId   = serviceDbObj.getId();

		// if this is a tag service, update all services that refer to this tag service
		// so that next policy-download from plugins will get updated tag policies
		boolean isTagService = serviceDbObj.getType() == EmbeddedServiceDefsUtil.instance().getTagServiceDefId();
		if(isTagService) {
			List<XXService> referringServices = serviceDao.findByTagServiceId(serviceId);

			if(CollectionUtils.isNotEmpty(referringServices)) {
				for(XXService referringService : referringServices) {
					final Long 		    referringServiceId 	  = referringService.getId();
					final VERSION_TYPE  tagServiceversionType = VERSION_TYPE.POLICY_VERSION;

					Runnable tagServiceVersionUpdater = new ServiceVersionUpdater(daoManager, referringServiceId, tagServiceversionType, policy != null ? policy.getZoneName() : null, policyDeltaType, policy);
					transactionSynchronizationAdapter.executeOnTransactionCommit(tagServiceVersionUpdater);

					Runnable roleVersionUpdater = new ServiceVersionUpdater(daoManager, referringServiceId, VERSION_TYPE.ROLE_VERSION, policy != null ? policy.getZoneName() : null, policyDeltaType, policy);
					transactionSynchronizationAdapter.executeOnTransactionCommit(roleVersionUpdater);
				}
			}
		}
		final VERSION_TYPE     versionType = VERSION_TYPE.POLICY_VERSION;

		Runnable serviceVersionUpdater = new ServiceVersionUpdater(daoManager, serviceId, versionType, policy != null ? policy.getZoneName() : null, policyDeltaType, policy);
		transactionSynchronizationAdapter.executeOnTransactionCommit(serviceVersionUpdater);

		if (updateServiceInfoRoleVersion) {
			Runnable roleVersionUpdater = new ServiceVersionUpdater(daoManager, serviceId, VERSION_TYPE.ROLE_VERSION, policy != null ? policy.getZoneName() : null, policyDeltaType, policy);
			transactionSynchronizationAdapter.executeOnTransactionCommit(roleVersionUpdater);
		}
	}

	public static void persistVersionChange(ServiceVersionUpdater serviceVersionUpdater) {
		RangerDaoManager daoMgr = serviceVersionUpdater.daoManager;
		Long id = serviceVersionUpdater.serviceId;
		VERSION_TYPE versionType = serviceVersionUpdater.versionType;

		XXServiceVersionInfoDao serviceVersionInfoDao = daoMgr.getXXServiceVersionInfo();

		XXServiceVersionInfo serviceVersionInfoDbObj = serviceVersionInfoDao.findByServiceId(id);
		XXService service = daoMgr.getXXService().getById(id);

		Long nextVersion = 1L;
		Date now = new Date();

		if (serviceVersionInfoDbObj != null) {
			if (versionType == VERSION_TYPE.POLICY_VERSION) {
				nextVersion = getNextVersion(serviceVersionInfoDbObj.getPolicyVersion());
				serviceVersionInfoDbObj.setPolicyVersion(nextVersion);
				serviceVersionInfoDbObj.setPolicyUpdateTime(now);
			} else if (versionType == VERSION_TYPE.TAG_VERSION) {
				nextVersion = getNextVersion(serviceVersionInfoDbObj.getTagVersion());
				serviceVersionInfoDbObj.setTagVersion(nextVersion);
				serviceVersionInfoDbObj.setTagUpdateTime(now);
			} else if(versionType == VERSION_TYPE.ROLE_VERSION) {
				// get the LatestRoleVersion from the GlobalTable and update ServiceInfo for a service
				XXGlobalStateDao xxGlobalStateDao = daoMgr.getXXGlobalState();
				if (xxGlobalStateDao != null) {
					Long roleVersion = xxGlobalStateDao.getAppDataVersion("RangerRole");
					if (roleVersion != null) {
						nextVersion = roleVersion;
					} else {
						LOG.error("No Global state for 'RoleVersion'. Cannot execute this object:[" + serviceVersionUpdater + "]");
					}
					serviceVersionInfoDbObj.setRoleVersion(nextVersion);
					serviceVersionInfoDbObj.setRoleUpdateTime(now);
				} else {
					LOG.error("No Global state DAO. Cannot execute this object:[" + serviceVersionUpdater + "]");
					return;
				}
			} else {
				LOG.error("Unknown VERSION_TYPE:" + versionType + ". Cannot execute this object:[" + serviceVersionUpdater + "]");
				return;
			}
			serviceVersionUpdater.version = nextVersion;
			serviceVersionInfoDao.update(serviceVersionInfoDbObj);

		} else {
			if (service != null) {
				serviceVersionInfoDbObj = new XXServiceVersionInfo();
				serviceVersionInfoDbObj.setServiceId(service.getId());
				serviceVersionInfoDbObj.setPolicyVersion(nextVersion);
				serviceVersionInfoDbObj.setPolicyUpdateTime(new Date());
				serviceVersionInfoDbObj.setTagVersion(nextVersion);
				serviceVersionInfoDbObj.setTagUpdateTime(new Date());
				serviceVersionInfoDbObj.setRoleVersion(nextVersion);
				serviceVersionInfoDbObj.setRoleUpdateTime(new Date());

				serviceVersionUpdater.version = nextVersion;
				serviceVersionInfoDao.create(serviceVersionInfoDbObj);
			}
		}

		if (service != null) {
			if (versionType == VERSION_TYPE.POLICY_VERSION) {
				persistChangeLog(service, versionType, serviceVersionInfoDbObj.getPolicyVersion(), serviceVersionUpdater);
			} else if (versionType == VERSION_TYPE.TAG_VERSION) {
				persistChangeLog(service, versionType, serviceVersionInfoDbObj.getTagVersion(), serviceVersionUpdater);
			}
		}
	}

	private boolean isRoleDownloadRequired(RangerPolicy policy, RangerService service) {
		// Role Download to plugin is required if some role in the policy created/updated is not present in any other
		// policy for that service.
		boolean ret = false;

		if (policy != null) {
			Set<String> roleNames = getAllPolicyItemRoleNames(policy);
			if (CollectionUtils.isNotEmpty(roleNames)) {
				Long serviceId = service.getId();
				checkAndFilterRoleNames(roleNames, service);
				if (CollectionUtils.isNotEmpty(roleNames)) {
					for (String roleName : roleNames) {
						long roleRefPolicyCount = daoMgr.getXXPolicy().findRoleRefPolicyCount(roleName, serviceId);
						if (roleRefPolicyCount == 0) {
							ret = true;
							break;
						}
					}
				}
			}
		}

		return ret;
	}

	private void checkAndFilterRoleNames(Set<String> roleNames, RangerService service) {
		//remove all roles which are already in DB for this serviceId, so we just download roles if there are new roles added.
		Set<String>  rolesToRemove = new HashSet<>();
		Long 		 serviceId     = service.getId();
		List<String> rolesFromDb   = daoMgr.getXXRole().findRoleNamesByServiceId(serviceId);
		if(CollectionUtils.isNotEmpty(rolesFromDb)) {
			rolesToRemove.addAll(rolesFromDb);
		}

		String    tagService   = service.getTagService();
		XXService serviceDbObj = daoMgr.getXXService().findByName(tagService);
		if (serviceDbObj != null) {
			List<String> rolesFromServiceTag = daoMgr.getXXRole().findRoleNamesByServiceId(serviceDbObj.getId());
			if (CollectionUtils.isNotEmpty(rolesFromServiceTag)) {
				rolesToRemove.addAll(rolesFromServiceTag);
			}
		}

		roleNames.removeAll(rolesToRemove);
	}

	private Set<String> getAllPolicyItemRoleNames(RangerPolicy policy) {
		Set<String> ret = new HashSet<>();

		List<? extends RangerPolicy.RangerPolicyItem> policyItems = policy.getPolicyItems();
		if (CollectionUtils.isNotEmpty(policyItems)) {
			collectRolesFromPolicyItems(policyItems, ret);
		}

		policyItems = policy.getDenyPolicyItems();
		if (CollectionUtils.isNotEmpty(policyItems)) {
			collectRolesFromPolicyItems(policyItems, ret);
		}

		policyItems = policy.getAllowExceptions();
		if (CollectionUtils.isNotEmpty(policyItems)) {
			collectRolesFromPolicyItems(policyItems, ret);
		}

		policyItems = policy.getDenyExceptions();
		if (CollectionUtils.isNotEmpty(policyItems)) {
			collectRolesFromPolicyItems(policyItems, ret);
		}

		policyItems = policy.getDataMaskPolicyItems();
		if (CollectionUtils.isNotEmpty(policyItems)) {
			collectRolesFromPolicyItems(policyItems, ret);
		}

		policyItems = policy.getRowFilterPolicyItems();
		if (CollectionUtils.isNotEmpty(policyItems)) {
			collectRolesFromPolicyItems(policyItems, ret);
		}

		return ret;
	}

	private void collectRolesFromPolicyItems(List<? extends RangerPolicyItem> rangerPolicyItems, Set<String> roleNames) {
		for (RangerPolicyItem rangerPolicyItem : rangerPolicyItems) {
			List<String> rangerPolicyItemRoles = rangerPolicyItem.getRoles();
			if (CollectionUtils.isNotEmpty(rangerPolicyItemRoles)) {
				roleNames.addAll(rangerPolicyItemRoles);
			}
		}
	}

	private void persistChangeLog(ServiceVersionUpdater serviceVersionUpdater) {
		XXServiceVersionInfoDao serviceVersionInfoDao = serviceVersionUpdater.daoManager.getXXServiceVersionInfo();

		XXServiceVersionInfo serviceVersionInfoDbObj = serviceVersionInfoDao.findByServiceId(serviceVersionUpdater.serviceId);
		XXService service = serviceVersionUpdater.daoManager.getXXService().getById(serviceVersionUpdater.serviceId);

		if (service != null && serviceVersionInfoDao != null) {
			Long version = serviceVersionUpdater.versionType == VERSION_TYPE.TAG_VERSION ? serviceVersionInfoDbObj.getTagVersion() : serviceVersionInfoDbObj.getPolicyVersion();
			persistChangeLog(service, serviceVersionUpdater.versionType, version, serviceVersionUpdater);
		}
	}

	private static void persistChangeLog(XXService service, VERSION_TYPE versionType, Long version, ServiceVersionUpdater serviceVersionUpdater) {
		Date now = new Date();

		if (versionType == VERSION_TYPE.TAG_VERSION) {
			ServiceTags.TagsChangeType tagChangeType = serviceVersionUpdater.tagChangeType;
			if (tagChangeType == ServiceTags.TagsChangeType.RANGER_ADMIN_START || TagDBStore.isSupportsTagDeltas()) {
				// Build and save TagChangeLog
				XXTagChangeLog tagChangeLog = new XXTagChangeLog();

				Long serviceResourceId = serviceVersionUpdater.resourceId;
				Long tagId = serviceVersionUpdater.tagId;

				tagChangeLog.setCreateTime(now);
				tagChangeLog.setServiceId(service.getId());
				tagChangeLog.setChangeType(tagChangeType.ordinal());
				tagChangeLog.setServiceTagsVersion(version);
				tagChangeLog.setServiceResourceId(serviceResourceId);
				tagChangeLog.setTagId(tagId);

				serviceVersionUpdater.daoManager.getXXTagChangeLog().create(tagChangeLog);
			}

		} else {
			Integer policyDeltaChange = serviceVersionUpdater.policyDeltaChange;

			if (policyDeltaChange == RangerPolicyDelta.CHANGE_TYPE_RANGER_ADMIN_START || isSupportsPolicyDeltas()) {
				// Build and save PolicyChangeLog
				XXPolicyChangeLog policyChangeLog = new XXPolicyChangeLog();

				policyChangeLog.setCreateTime(now);
				policyChangeLog.setServiceId(service.getId());
				policyChangeLog.setChangeType(serviceVersionUpdater.policyDeltaChange);
				policyChangeLog.setPolicyVersion(version);
				policyChangeLog.setZoneName(serviceVersionUpdater.zoneName);

				RangerPolicy policy = serviceVersionUpdater.policy;
				if (policy != null) {
					policyChangeLog.setServiceType(policy.getServiceType());
					policyChangeLog.setPolicyType(policy.getPolicyType());
					policyChangeLog.setPolicyId(policy.getId());
					policyChangeLog.setPolicyGuid(policy.getGuid());
				}

				serviceVersionUpdater.daoManager.getXXPolicyChangeLog().create(policyChangeLog);
			}
		}

	}


	private Boolean deleteExistingPolicyLabel(RangerPolicy policy) {
		if (policy == null) {
			return false;
		}

		List<XXPolicyLabelMap> xxPolicyLabelMaps = daoMgr.getXXPolicyLabelMap().findByPolicyId(policy.getId());
		XXPolicyLabelMapDao policyLabelMapDao = daoMgr.getXXPolicyLabelMap();
		for (XXPolicyLabelMap xxPolicyLabelMap : xxPolicyLabelMaps) {
			policyLabelMapDao.remove(xxPolicyLabelMap);
		}
		return true;
	}

	@Override
	public Boolean getPopulateExistingBaseFields() {
		return populateExistingBaseFields;
	}

	@Override
	public void setPopulateExistingBaseFields(Boolean populateExistingBaseFields) {
		this.populateExistingBaseFields = populateExistingBaseFields;
	}

	public RangerPolicy getPolicyFromEventTime(String eventTime, Long policyId) {

		XXDataHist xDataHist = daoMgr.getXXDataHist().findObjByEventTimeClassTypeAndId(eventTime,
				AppConstants.CLASS_TYPE_RANGER_POLICY, policyId);

		if (xDataHist == null) {
			String errMsg = "No policy history found for given policy ID: " + policyId + " and event time: " + eventTime;
			LOG.error(errMsg);
			throw restErrorUtil.createRESTException(errMsg, MessageEnums.DATA_NOT_FOUND);
		}

		String content = xDataHist.getContent();
		RangerPolicy policy = (RangerPolicy) jsonUtil.writeJsonToJavaObject(content, RangerPolicy.class);

		return policy;
	}

	public VXString getPolicyVersionList(Long policyId) {
		List<Integer> versionList = daoMgr.getXXDataHist().getVersionListOfObject(policyId,
				AppConstants.CLASS_TYPE_RANGER_POLICY);

		VXString vXString = new VXString();
		vXString.setValue(StringUtils.join(versionList, ","));

		return vXString;
	}

	public RangerPolicy getPolicyForVersionNumber(Long policyId, int versionNo) {
		XXDataHist xDataHist = daoMgr.getXXDataHist().findObjectByVersionNumber(policyId,
				AppConstants.CLASS_TYPE_RANGER_POLICY, versionNo);

		if (xDataHist == null) {
			throw restErrorUtil.createRESTException("No Policy found for given version.", MessageEnums.DATA_NOT_FOUND);
		}

		String content = xDataHist.getContent();
		RangerPolicy policy = (RangerPolicy) jsonUtil.writeJsonToJavaObject(content, RangerPolicy.class);

		return policy;
	}

	void updatePolicySignature(RangerPolicy policy) {
		String guid = policy.getGuid();
		if (StringUtils.isEmpty(guid)) {
			guid = guidUtil.genGUID();
			policy.setGuid(guid);
		}
		RangerPolicyResourceSignature policySignature = factory.createPolicyResourceSignature(policy);
		String signature = policySignature.getSignature();
		policy.setResourceSignature(signature);
		if (LOG.isDebugEnabled()) {
			String message = String.format("Setting signature on policy id=%d, name=%s to [%s]", policy.getId(), policy.getName(), signature);
			LOG.debug(message);
		}
	}

	// when a service-def is updated, the updated service-def should be made available to plugins
	//   this is achieved by incrementing policyVersion of all services of this service-def
	protected void updateServicesForServiceDefUpdate(RangerServiceDef serviceDef) throws Exception {
		if(serviceDef == null) {
			return;
		}

		final RangerDaoManager daoManager = daoMgr;

		boolean isTagServiceDef = StringUtils.equals(serviceDef.getName(), EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_TAG_NAME);

		XXServiceDao serviceDao = daoMgr.getXXService();

		List<XXService> services = serviceDao.findByServiceDefId(serviceDef.getId());

		if(CollectionUtils.isNotEmpty(services)) {
			for(XXService service : services) {

				if(isTagServiceDef) {
					List<XXService> referringServices = serviceDao.findByTagServiceId(service.getId());

					if(CollectionUtils.isNotEmpty(referringServices)) {
						for(XXService referringService : referringServices) {

							final Long 		    referringServiceId    = referringService.getId();
							final VERSION_TYPE  tagServiceVersionType = VERSION_TYPE.POLICY_VERSION;

							Runnable tagServiceVersionUpdater = new ServiceVersionUpdater(daoManager, referringServiceId, tagServiceVersionType, RangerPolicyDelta.CHANGE_TYPE_SERVICE_DEF_CHANGE);
							transactionSynchronizationAdapter.executeOnTransactionCommit(tagServiceVersionUpdater);
						}
					}
				}

				final Long 		    serviceId 	= service.getId();
				final VERSION_TYPE  versionType = VERSION_TYPE.POLICY_VERSION;

				Runnable serviceVersionUpdater = new ServiceVersionUpdater(daoManager, serviceId, versionType, RangerPolicyDelta.CHANGE_TYPE_SERVICE_DEF_CHANGE);
				transactionSynchronizationAdapter.executeOnTransactionCommit(serviceVersionUpdater);
			}
		}
	}

	@Override
	public RangerSecurityZone getSecurityZone(Long id) throws Exception {
		return securityZoneService.read(id);
	}

	@Override
	public RangerSecurityZone getSecurityZone(String name) throws Exception {
		XXSecurityZone xxSecurityZone = daoMgr.getXXSecurityZoneDao().findByZoneName(name);
		if (xxSecurityZone != null) {
			return getSecurityZone(xxSecurityZone.getId());
		}
		return null;
	}

	@Override
	public long getPoliciesCount(final String serviceName) {
		final long ret;
		if (StringUtils.isNotBlank(serviceName)) {
			ret = daoMgr.getXXPolicy().getPoliciesCount(serviceName);
		} else {
			ret = 0L;
		}
		return ret;
	}


	private String getServiceName(Long serviceId) {
		String ret = null;

		if(serviceId != null) {
			XXService service = daoMgr.getXXService().getById(serviceId);

			if(service != null) {
				ret = service.getName();
			}
		}

		return ret;
	}

	private boolean isAccessTypeInList(String accessType, List<XXAccessTypeDef> xAccessTypeDefs) {
		for(XXAccessTypeDef xxAccessTypeDef : xAccessTypeDefs) {
			if(StringUtils.equals(xxAccessTypeDef.getName(), accessType)) {
				return true;
			}
		}

		return false;
	}

	private boolean isResourceInList(String resource, List<XXResourceDef> xResourceDefs) {
		for(XXResourceDef xResourceDef : xResourceDefs) {
			if(StringUtils.equals(xResourceDef.getName(), resource)) {
				return true;
			}
		}

		return false;
	}

        private void writeExcel(List<RangerPolicy> policies, String excelFileName,
                        HttpServletResponse response) throws IOException {
		Workbook workbook = null;
		OutputStream outStream = null;
		try {
			workbook = new HSSFWorkbook();
			Sheet sheet = workbook.createSheet();
			createHeaderRow(sheet);
			int rowCount = 0;
			if (!CollectionUtils.isEmpty(policies)) {
				for (RangerPolicy policy : policies) {

                                        List<RangerPolicyItem> policyItems = policy
                                                        .getPolicyItems();
                                        List<RangerRowFilterPolicyItem> rowFilterPolicyItems = policy
                                                        .getRowFilterPolicyItems();
                                        List<RangerDataMaskPolicyItem> dataMaskPolicyItems = policy
                                                        .getDataMaskPolicyItems();
                                        List<RangerPolicyItem> allowExceptions = policy
                                                        .getAllowExceptions();
                                        List<RangerPolicyItem> denyExceptions = policy
                                                        .getDenyExceptions();
                                        List<RangerPolicyItem> denyPolicyItems = policy
                                                        .getDenyPolicyItems();
                                        XXService xxservice = daoMgr.getXXService().findByName(
                                                        policy.getService());
                                        String serviceType = "";
                                        if (xxservice != null) {
                                                Long ServiceId = xxservice.getType();
                                                XXServiceDef xxservDef = daoMgr.getXXServiceDef()
                                                                .getById(ServiceId);
                                                if (xxservDef != null) {
                                                        serviceType = xxservDef.getName();
                                                }
                                        }
					if (CollectionUtils.isNotEmpty(policyItems)) {
						for (RangerPolicyItem policyItem : policyItems) {
							Row row = sheet.createRow(++rowCount);
                                                        writeBookForPolicyItems(policy, policyItem, null,
                                                                        null, row, POLICY_ALLOW_INCLUDE);
						}
					} else if (CollectionUtils.isNotEmpty(dataMaskPolicyItems)) {
						for (RangerDataMaskPolicyItem dataMaskPolicyItem : dataMaskPolicyItems) {
							Row row = sheet.createRow(++rowCount);
                                                        writeBookForPolicyItems(policy, null,
                                                                        dataMaskPolicyItem, null, row,
                                                                        null);
						}
					} else if (CollectionUtils.isNotEmpty(rowFilterPolicyItems)) {
						for (RangerRowFilterPolicyItem rowFilterPolicyItem : rowFilterPolicyItems) {
							Row row = sheet.createRow(++rowCount);
                                                        writeBookForPolicyItems(policy, null, null,
                                                                        rowFilterPolicyItem, row,
                                                                        null);
						}
                                        } else if (serviceType
                                                        .equalsIgnoreCase(EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_TAG_NAME)) {
						if (CollectionUtils.isEmpty(policyItems)) {
							Row row = sheet.createRow(++rowCount);
							RangerPolicyItem policyItem = new RangerPolicyItem();
                                                        writeBookForPolicyItems(policy, policyItem, null,
                                                                        null, row, POLICY_ALLOW_INCLUDE);
						}
					} else if (CollectionUtils.isEmpty(policyItems)) {
						Row row = sheet.createRow(++rowCount);
						RangerPolicyItem policyItem = new RangerPolicyItem();
                                                writeBookForPolicyItems(policy, policyItem, null, null,
                                                                row, POLICY_ALLOW_INCLUDE);
					}
					if (CollectionUtils.isNotEmpty(allowExceptions)) {
						for (RangerPolicyItem policyItem : allowExceptions) {
							Row row = sheet.createRow(++rowCount);
                                                        writeBookForPolicyItems(policy, policyItem, null,
                                                                        null, row, POLICY_ALLOW_EXCLUDE);
						}
					}
					if (CollectionUtils.isNotEmpty(denyExceptions)) {
						for (RangerPolicyItem policyItem : denyExceptions) {
							Row row = sheet.createRow(++rowCount);
                                                        writeBookForPolicyItems(policy, policyItem, null,
                                                                        null, row, POLICY_DENY_EXCLUDE);
						}
					}
					if (CollectionUtils.isNotEmpty(denyPolicyItems)) {
						for (RangerPolicyItem policyItem : denyPolicyItems) {
							Row row = sheet.createRow(++rowCount);
                                                        writeBookForPolicyItems(policy, policyItem, null,
                                                                        null, row, POLICY_DENY_INCLUDE);
						}
                                        }
				}
			}
			ByteArrayOutputStream outByteStream = new ByteArrayOutputStream();
			workbook.write(outByteStream);
			byte[] outArray = outByteStream.toByteArray();
			response.setContentType("application/ms-excel");
			response.setContentLength(outArray.length);
			response.setHeader("Expires:", "0");
                        response.setHeader("Content-Disposition", "attachment; filename="
                                        + excelFileName);
			response.setStatus(HttpServletResponse.SC_OK);
			outStream = response.getOutputStream();
			outStream.write(outArray);
			outStream.flush();
		} catch (IOException ex) {
			LOG.error("Failed to create report file " + excelFileName, ex);
		} catch (Exception ex) {
			LOG.error("Error while generating report file " + excelFileName, ex);
		} finally {
			if (outStream != null) {
				outStream.close();
			}
			if (workbook != null) {
				workbook.close();
			}
		}
	}

        private StringBuilder writeCSV(List<RangerPolicy> policies,
                        String cSVFileName, HttpServletResponse response) {
		response.setContentType("text/csv");

		final String LINE_SEPARATOR = "\n";
                final String FILE_HEADER = "ID|Name|Resources|Roles|Groups|Users|Accesses|Service Type|Status|Policy Type|Delegate Admin|isRecursive|"
                                + "isExcludes|Service Name|Description|isAuditEnabled|Policy Conditions|Policy Condition Type|Masking Options|Row Filter Expr|Policy Label Name";
		StringBuilder csvBuffer = new StringBuilder();
		csvBuffer.append(FILE_HEADER);
		csvBuffer.append(LINE_SEPARATOR);
                if (!CollectionUtils.isEmpty(policies)) {
                        for (RangerPolicy policy : policies) {
                                List<RangerPolicyItem> policyItems = policy.getPolicyItems();
                                List<RangerRowFilterPolicyItem> rowFilterPolicyItems = policy
                                                .getRowFilterPolicyItems();
                                List<RangerDataMaskPolicyItem> dataMaskPolicyItems = policy
                                                .getDataMaskPolicyItems();
                                List<RangerPolicyItem> allowExceptions = policy
                                                .getAllowExceptions();
                                List<RangerPolicyItem> denyExceptions = policy
                                                .getDenyExceptions();
                                List<RangerPolicyItem> denyPolicyItems = policy
                                                .getDenyPolicyItems();
                                XXService xxservice = daoMgr.getXXService().findByName(
                                                policy.getService());
                                String serviceType = "";
                                if (xxservice != null) {
                                        Long ServiceId = xxservice.getType();
                                        XXServiceDef xxservDef = daoMgr.getXXServiceDef().getById(
                                                        ServiceId);
                                        if (xxservDef != null) {
                                                serviceType = xxservDef.getName();
					}
				}
                                if (CollectionUtils.isNotEmpty(policyItems)) {
					for (RangerPolicyItem policyItem : policyItems) {
                                                writeCSVForPolicyItems(policy, policyItem, null, null,
                                                                csvBuffer, POLICY_ALLOW_INCLUDE);
					}
                                } else if (CollectionUtils.isNotEmpty(dataMaskPolicyItems)) {
                                        for (RangerDataMaskPolicyItem dataMaskPolicyItem : dataMaskPolicyItems) {
                                                writeCSVForPolicyItems(policy, null,
                                                                dataMaskPolicyItem, null, csvBuffer,
                                                                null);
                                        }
                                } else if (CollectionUtils.isNotEmpty(rowFilterPolicyItems)) {
                                        for (RangerRowFilterPolicyItem rowFilterPolicyItem : rowFilterPolicyItems) {
                                                writeCSVForPolicyItems(policy, null, null,
                                                                rowFilterPolicyItem, csvBuffer,
                                                                null);
                                        }
                                } else if (serviceType
                                                .equalsIgnoreCase(EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_TAG_NAME)) {
                                        if (CollectionUtils.isEmpty(policyItems)) {
                                                RangerPolicyItem policyItem = new RangerPolicyItem();
                                                writeCSVForPolicyItems(policy, policyItem, null, null,
                                                                csvBuffer, POLICY_ALLOW_INCLUDE);
                                        }
                                } else if (CollectionUtils.isEmpty(policyItems)) {
                                        RangerPolicyItem policyItem = new RangerPolicyItem();
                                        writeCSVForPolicyItems(policy, policyItem, null, null,
                                                        csvBuffer, POLICY_ALLOW_INCLUDE);
				}
                                if (CollectionUtils.isNotEmpty(allowExceptions)) {
                                        for (RangerPolicyItem policyItem : allowExceptions) {
                                                writeCSVForPolicyItems(policy, policyItem, null, null,
                                                                csvBuffer, POLICY_ALLOW_EXCLUDE);
					}
				}
                                if (CollectionUtils.isNotEmpty(denyExceptions)) {
                                        for (RangerPolicyItem policyItem : denyExceptions) {
                                                writeCSVForPolicyItems(policy, policyItem, null, null,
                                                                csvBuffer, POLICY_DENY_EXCLUDE);
					}
                                }
                                if (CollectionUtils.isNotEmpty(denyPolicyItems)) {
                                        for (RangerPolicyItem policyItem : denyPolicyItems) {
                                                writeCSVForPolicyItems(policy, policyItem, null, null,
                                                                csvBuffer, POLICY_DENY_INCLUDE);
                                        }
                                }
                        }
                }
                response.setHeader("Content-Disposition", "attachment; filename="
                                + cSVFileName);
		response.setStatus(HttpServletResponse.SC_OK);
		return csvBuffer;
	}

        private void writeCSVForPolicyItems(RangerPolicy policy,
                        RangerPolicyItem policyItem,
                        RangerDataMaskPolicyItem dataMaskPolicyItem,
                        RangerRowFilterPolicyItem rowFilterPolicyItem,
                        StringBuilder csvBuffer, String policyConditionType) {
                if (LOG.isDebugEnabled()) {
                        // To avoid PMD violation
                        LOG.debug("policyConditionType:[" + policyConditionType + "]");
                }
                final String COMMA_DELIMITER = "|";
                final String LINE_SEPARATOR = "\n";
                List<String> roles = new ArrayList<String>();
                List<String> groups = new ArrayList<String>();
                List<String> users = new ArrayList<String>();
                String roleNames = "";
                String groupNames = "";
                String userNames = "";
                String policyLabelName = "";
                String accessType = "";
                String policyStatus = "";
                String policyType = "";
                Boolean delegateAdmin = false;
                String isRecursive = "";
                String isExcludes = "";
                String serviceName = "";
                String description = "";
                Boolean isAuditEnabled = true;
                String isExcludesValue = "";
                String maskingInfo = "";
                List<RangerPolicyItemAccess> accesses = new ArrayList<RangerPolicyItemAccess>();
                List<RangerPolicyItemCondition> conditionsList = new ArrayList<RangerPolicyItemCondition>();
                String conditionKeyValue = "";
                String resValue = "";
                String resourceKeyVal = "";
                String isRecursiveValue = "";
                String resKey = "";
                String ServiceType = "";
                String filterExpr = "";
                String policyName = "";
                List<String> policyLabels = new ArrayList<String>();
                String policyConditionTypeValue = "";
                serviceName = policy.getService();
                description = policy.getDescription();
                isAuditEnabled = policy.getIsAuditEnabled();
                policyLabels = policy.getPolicyLabels();
                StringBuffer sb = new StringBuffer();
                StringBuffer sbIsRecursive = new StringBuffer();
                StringBuffer sbIsExcludes = new StringBuffer();
                Map<String, RangerPolicyResource> resources = policy.getResources();
                RangerPolicy.RangerPolicyItemDataMaskInfo dataMaskInfo = new RangerPolicy.RangerPolicyItemDataMaskInfo();
                RangerPolicy.RangerPolicyItemRowFilterInfo filterInfo = new RangerPolicy.RangerPolicyItemRowFilterInfo();
                policyName = policy.getName();
                policyName = policyName.replace("|", "");
                if (resources != null) {
                        for (Entry<String, RangerPolicyResource> resource : resources
                                        .entrySet()) {
                                resKey = resource.getKey();
                                RangerPolicyResource policyResource = resource.getValue();
                                List<String> resvalueList = policyResource.getValues();
                                isExcludes = policyResource.getIsExcludes().toString();
                                isRecursive = policyResource.getIsRecursive().toString();
                                resValue = resvalueList.toString();
                                sb = sb.append(resourceKeyVal).append(" ").append(resKey)
                                                .append("=").append(resValue);
                                sbIsExcludes = sbIsExcludes.append(resourceKeyVal).append(" ")
                                                .append(resKey).append("=[").append(isExcludes)
                                                .append("]");
                                sbIsRecursive = sbIsRecursive.append(resourceKeyVal)
                                                .append(" ").append(resKey).append("=[")
                                                .append(isRecursive).append("]");
                        }
                        isExcludesValue = sbIsExcludes.toString();
                        isExcludesValue = isExcludesValue.substring(1);
                        isRecursiveValue = sbIsRecursive.toString();
                        isRecursiveValue = isRecursiveValue.substring(1);
                        resourceKeyVal = sb.toString();
                        resourceKeyVal = resourceKeyVal.substring(1);
                        if (policyItem != null && dataMaskPolicyItem == null
                                        && rowFilterPolicyItem == null) {
                                roles = policyItem.getRoles();
                                groups = policyItem.getGroups();
                                users = policyItem.getUsers();
                                accesses = policyItem.getAccesses();
                                delegateAdmin = policyItem.getDelegateAdmin();
                                conditionsList = policyItem.getConditions();
                        } else if (dataMaskPolicyItem != null && policyItem == null
                                        && rowFilterPolicyItem == null) {
                                roles = dataMaskPolicyItem.getRoles();
                                groups = dataMaskPolicyItem.getGroups();
                                users = dataMaskPolicyItem.getUsers();
                                accesses = dataMaskPolicyItem.getAccesses();
                                delegateAdmin = dataMaskPolicyItem.getDelegateAdmin();
                                conditionsList = dataMaskPolicyItem.getConditions();
                                dataMaskInfo = dataMaskPolicyItem.getDataMaskInfo();
                                String dataMaskType = dataMaskInfo.getDataMaskType();
                                String conditionExpr = dataMaskInfo.getConditionExpr();
                                String valueExpr = dataMaskInfo.getValueExpr();
                                maskingInfo = "dataMasktype=[" + dataMaskType + "]";
                                if (conditionExpr != null && !conditionExpr.isEmpty()
                                                && valueExpr != null && !valueExpr.isEmpty()) {
                                        maskingInfo = maskingInfo + "; conditionExpr=["
                                                        + conditionExpr + "]";
                                }
                        } else if (rowFilterPolicyItem != null && policyItem == null
                                        && dataMaskPolicyItem == null) {
                                roles = rowFilterPolicyItem.getRoles();
                                groups = rowFilterPolicyItem.getGroups();
                                users = rowFilterPolicyItem.getUsers();
                                accesses = rowFilterPolicyItem.getAccesses();
                                delegateAdmin = rowFilterPolicyItem.getDelegateAdmin();
                                conditionsList = rowFilterPolicyItem.getConditions();
                                filterInfo = rowFilterPolicyItem.getRowFilterInfo();
                                filterExpr = filterInfo.getFilterExpr();
                        }
                        if (CollectionUtils.isNotEmpty(accesses)) {
                                for (RangerPolicyItemAccess access : accesses) {
                                        accessType = accessType
                                                        + access.getType().replace("#", "")
                                                                        .replace("|", "") + "#";
                                }
                                accessType = accessType.substring(0,
                                                accessType.lastIndexOf("#"));
                        }
                        if (CollectionUtils.isNotEmpty(roles)) {
                            for (String role : roles) {
                                role = role.replace("|", "");
                                role = role.replace("#", "");
                                roleNames = roleNames + role + "#";
                            }
                            roleNames = roleNames.substring(0, roleNames.lastIndexOf("#"));
                        }
                        if (CollectionUtils.isNotEmpty(groups)) {
                                for (String group : groups) {
                                        group = group.replace("|", "");
                                        group = group.replace("#", "");
                                        groupNames = groupNames + group + "#";
                                }
                                groupNames = groupNames.substring(0,
                                                groupNames.lastIndexOf("#"));
                        }
                        if (CollectionUtils.isNotEmpty(users)) {
                                for (String user : users) {
                                        user = user.replace("|", "");
                                        user = user.replace("#", "");
                                        userNames = userNames + user + "#";
                                }
                                userNames = userNames.substring(0, userNames.lastIndexOf("#"));
                        }
                        String conditionValue = "";
                        for (RangerPolicyItemCondition conditions : conditionsList) {
                                String conditionType = conditions.getType();
                                List<String> conditionList = conditions.getValues();
                                conditionValue = conditionList.toString();
                                conditionKeyValue = conditionType + "=" + conditionValue;
                        }
                        XXService xxservice = daoMgr.getXXService().findByName(
                                        policy.getService());
                        if (xxservice != null) {
                                Long ServiceId = xxservice.getType();
                                XXServiceDef xxservDef = daoMgr.getXXServiceDef().getById(
                                                ServiceId);
                                if (xxservDef != null) {
                                        ServiceType = xxservDef.getName();
                                }
                        }
                }
                if (policyConditionType != null) {
                        policyConditionTypeValue = policyConditionType;
                }
                if (policyConditionType == null && ServiceType.equalsIgnoreCase("tag")) {
                        policyConditionTypeValue = POLICY_ALLOW_INCLUDE;
                } else if (policyConditionType == null) {
                        policyConditionTypeValue = "";
                }
                if (policy.getIsEnabled()) {
                        policyStatus = "Enabled";
                } else {
                        policyStatus = "Disabled";
                }
                int policyTypeInt = policy.getPolicyType();
                switch (policyTypeInt) {
                case RangerPolicy.POLICY_TYPE_ACCESS:
                        policyType = POLICY_TYPE_ACCESS;
                        break;
                case RangerPolicy.POLICY_TYPE_DATAMASK:
                        policyType = POLICY_TYPE_DATAMASK;
                        break;
                case RangerPolicy.POLICY_TYPE_ROWFILTER:
                        policyType = POLICY_TYPE_ROWFILTER;
                        break;
                }
                if (CollectionUtils.isNotEmpty(policyLabels)) {
                        for (String policyLabel : policyLabels) {
                                policyLabel = policyLabel.replace("|", "");
                                policyLabel = policyLabel.replace("#", "");
                                policyLabelName = policyLabelName + policyLabel + "#";
                        }
                        policyLabelName = policyLabelName.substring(0,
                                        policyLabelName.lastIndexOf("#"));
                }

                csvBuffer.append(policy.getId());
                csvBuffer.append(COMMA_DELIMITER);
                csvBuffer.append(policyName);
                csvBuffer.append(COMMA_DELIMITER);
                csvBuffer.append(resourceKeyVal);
                csvBuffer.append(COMMA_DELIMITER);
                csvBuffer.append(roleNames);
                csvBuffer.append(COMMA_DELIMITER);
                csvBuffer.append(groupNames);
                csvBuffer.append(COMMA_DELIMITER);
                csvBuffer.append(userNames);
                csvBuffer.append(COMMA_DELIMITER);
                csvBuffer.append(accessType.trim());
                csvBuffer.append(COMMA_DELIMITER);
                csvBuffer.append(ServiceType);
                csvBuffer.append(COMMA_DELIMITER);
                csvBuffer.append(policyStatus);
                csvBuffer.append(COMMA_DELIMITER);
                csvBuffer.append(policyType);
                csvBuffer.append(COMMA_DELIMITER);
                csvBuffer.append(delegateAdmin.toString().toUpperCase());
                csvBuffer.append(COMMA_DELIMITER);
                csvBuffer.append(isRecursiveValue);
                csvBuffer.append(COMMA_DELIMITER);
                csvBuffer.append(isExcludesValue);
                csvBuffer.append(COMMA_DELIMITER);
                csvBuffer.append(serviceName);
                csvBuffer.append(COMMA_DELIMITER);
                csvBuffer.append(description);
                csvBuffer.append(COMMA_DELIMITER);
                csvBuffer.append(isAuditEnabled.toString().toUpperCase());
                csvBuffer.append(COMMA_DELIMITER);
                csvBuffer.append(conditionKeyValue.trim());
                csvBuffer.append(COMMA_DELIMITER);
                csvBuffer.append(policyConditionTypeValue);
                csvBuffer.append(COMMA_DELIMITER);
                csvBuffer.append(maskingInfo);
                csvBuffer.append(COMMA_DELIMITER);
                csvBuffer.append(filterExpr);
                csvBuffer.append(COMMA_DELIMITER);
                csvBuffer.append(policyLabelName);
                csvBuffer.append(COMMA_DELIMITER);
                csvBuffer.append(LINE_SEPARATOR);
        }
	
	public void putMetaDataInfo(RangerExportPolicyList rangerExportPolicyList){
		Map<String, Object> metaDataInfo = new LinkedHashMap<String, Object>();
		UserSessionBase usb = ContextUtil.getCurrentUserSession();
		String userId = usb!=null ? usb.getLoginId() : null;
		
		metaDataInfo.put(HOSTNAME, LOCAL_HOSTNAME);
		metaDataInfo.put(USER_NAME, userId);
		metaDataInfo.put(TIMESTAMP, MiscUtil.getUTCDateForLocalDate(new Date()));
		metaDataInfo.put(RANGER_VERSION, RangerVersionInfo.getVersion());
		
		rangerExportPolicyList.setMetaDataInfo(metaDataInfo);
	}
	
	private void writeJson(List<RangerPolicy> policies, String jsonFileName,
			HttpServletResponse response) throws JSONException, IOException {
		response.setContentType("text/json");
		response.setHeader("Content-Disposition", "attachment; filename="+ jsonFileName);
		ServletOutputStream out = null;
		RangerExportPolicyList rangerExportPolicyList = new RangerExportPolicyList();
		putMetaDataInfo(rangerExportPolicyList);
		rangerExportPolicyList.setPolicies(policies);
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json = gson.toJson(rangerExportPolicyList, RangerExportPolicyList.class);
		try {
			out = response.getOutputStream();
			response.setStatus(HttpServletResponse.SC_OK);
			IOUtils.write(json, out, "UTF-8");
		} catch (Exception e) {
			LOG.error("Error while exporting json file " + jsonFileName, e);
		} finally {
			try {
				if (out != null) {
					out.flush();
					out.close();
				}
			} catch (Exception ex) {
			}
		}
	}

	public Map<String, String> getMapFromInputStream(InputStream mapStream) throws IOException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceDBStore.getMapFromInputStream()");
		}
		Map<String, String> inputMap = new LinkedHashMap<String, String>();
		String inputMapString = IOUtils.toString(mapStream);
		if (StringUtils.isNotEmpty(inputMapString)) {
			inputMap = jsonUtil.jsonToMap(inputMapString);
		}
		if (!CollectionUtils.sizeIsEmpty(inputMap)) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("<== ServiceDBStore.getMapFromInputStream()");
			}
			return inputMap;
		} else {
			LOG.error("Provided zone/service input map is empty!!");
			throw restErrorUtil.createRESTException("Provided zone/service map is empty!!");
		}
	}
	
	public Map<String, RangerPolicy> setPolicyMapKeyValue(Map<String, RangerPolicy> policiesMap, RangerPolicy policy){
		if (StringUtils.isNotEmpty(policy.getName().trim())
				&& StringUtils.isNotEmpty(policy.getService().trim())
				&& StringUtils.isNotEmpty(policy.getResources().toString().trim())) {
			policiesMap.put(policy.getName().trim() + " " + policy.getService().trim() + " "
					+ policy.getResources().toString().trim() + " " + policy.getZoneName(), policy);
		}else if (StringUtils.isEmpty(policy.getName().trim()) && StringUtils.isNotEmpty(policy.getService().trim())){
			LOG.error("Policy Name is not provided for service : " + policy.getService().trim());
			throw restErrorUtil.createRESTException("Policy Name is not provided for service : " + policy.getService().trim());
		}else if (StringUtils.isNotEmpty(policy.getName().trim()) && StringUtils.isEmpty(policy.getService().trim())){
			LOG.error("Service Name is not provided for policy : " + policy.getName().trim());
			throw restErrorUtil.createRESTException("Service Name is not provided for policy : " + policy.getName().trim());
		}else{
			LOG.error("Service Name or Policy Name is not provided!!");
			throw restErrorUtil.createRESTException("Service Name or Policy Name is not provided!!");
		}
		return policiesMap;
	}
	
	public Map<String, RangerPolicy> createPolicyMap(Map<String, String> zoneMappingMap, List<String> sourceZones,
			String destinationZoneName, Map<String, String> servicesMappingMap, List<String> sourceServices,
			List<String> destinationServices, RangerPolicy policy, Map<String, RangerPolicy> policiesMap) {

		if (!CollectionUtils.sizeIsEmpty(zoneMappingMap)) {
			policy.setZoneName(destinationZoneName);// set destination zone name in policy.
		}
		if (!CollectionUtils.sizeIsEmpty(servicesMappingMap)) {
			if (!StringUtils.isEmpty(policy.getService().trim())){
				if (sourceServices.contains(policy.getService().trim())) {
					int index = sourceServices.indexOf(policy.getService().trim());
					policy.setService(destinationServices.get(index));
					policiesMap = setPolicyMapKeyValue(policiesMap, policy);
				}
			}else{
				LOG.error("Service Name or Policy Name is not provided!!");
				throw restErrorUtil.createRESTException("Service Name or Policy Name is not provided!!");
			}
		} else if (CollectionUtils.sizeIsEmpty(servicesMappingMap)) {
			policiesMap = setPolicyMapKeyValue(policiesMap, policy);
		}
		return policiesMap;
	}

	private void writeBookForPolicyItems(RangerPolicy policy, RangerPolicyItem policyItem,
                        RangerDataMaskPolicyItem dataMaskPolicyItem, RangerRowFilterPolicyItem rowFilterPolicyItem, Row row, String policyConditionType) {
		if (LOG.isDebugEnabled()) {
			// To avoid PMD violation
                        LOG.debug("policyConditionType:[" + policyConditionType + "]");
		}
		List<String> groups = new ArrayList<String>();
		List<String> users = new ArrayList<String>();
		List<String> roles = new ArrayList<String>();
		String roleNames = "";
		String groupNames = "";
                String policyConditionTypeValue = "";
		String userNames = "";
                String policyLabelNames = "";
		String accessType = "";
		String policyStatus = "";
                String policyType = "";
                Boolean delegateAdmin = false;
                String isRecursive  = "";
                String isExcludes  = "";
                String serviceName = "";

                String description = "";
                Boolean isAuditEnabled =  true;
                isAuditEnabled = policy.getIsAuditEnabled();
                String isExcludesValue = "";
		Cell cell = row.createCell(0);
		cell.setCellValue(policy.getId());
		List<RangerPolicyItemAccess> accesses = new ArrayList<RangerPolicyItemAccess>();
                List<RangerPolicyItemCondition> conditionsList = new ArrayList<RangerPolicyItemCondition>();
                String conditionKeyValue = "";
                List<String> policyLabels = new ArrayList<String>();
		String resValue = "";
		String resourceKeyVal = "";
                String isRecursiveValue = "";
		String resKey = "";
		StringBuffer sb = new StringBuffer();
                StringBuffer sbIsRecursive = new StringBuffer();
                StringBuffer sbIsExcludes = new StringBuffer();
		Map<String, RangerPolicyResource> resources = policy.getResources();
                RangerPolicy.RangerPolicyItemDataMaskInfo dataMaskInfo = new RangerPolicy.RangerPolicyItemDataMaskInfo();
                RangerPolicy.RangerPolicyItemRowFilterInfo filterInfo = new RangerPolicy.RangerPolicyItemRowFilterInfo();
                cell = row.createCell(1);
                cell.setCellValue(policy.getName());
                cell = row.createCell(2);
		if (resources != null) {
			for (Entry<String, RangerPolicyResource> resource : resources.entrySet()) {
				resKey = resource.getKey();
				RangerPolicyResource policyResource = resource.getValue();
				List<String> resvalueList = policyResource.getValues();
                                isExcludes = policyResource.getIsExcludes().toString();
                                isRecursive = policyResource.getIsRecursive().toString();
				resValue = resvalueList.toString();
                                sb = sb.append(resourceKeyVal).append("; ").append(resKey).append("=").append(resValue);
                                sbIsExcludes = sbIsExcludes.append(resourceKeyVal).append("; ").append(resKey).append("=[").append(isExcludes).append("]");
                                sbIsRecursive = sbIsRecursive.append(resourceKeyVal).append("; ").append(resKey).append("=[").append(isRecursive).append("]");
                        }
                        isExcludesValue = sbIsExcludes.toString();
                        isExcludesValue = isExcludesValue.substring(1);
                        isRecursiveValue = sbIsRecursive.toString();
                        isRecursiveValue = isRecursiveValue.substring(1);
			resourceKeyVal = sb.toString();
			resourceKeyVal = resourceKeyVal.substring(1);
			cell.setCellValue(resourceKeyVal);
			if (policyItem != null && dataMaskPolicyItem == null && rowFilterPolicyItem == null) {
				roles = policyItem.getRoles();
				groups = policyItem.getGroups();
				users = policyItem.getUsers();
				accesses = policyItem.getAccesses();
                                delegateAdmin = policyItem.getDelegateAdmin();
                                conditionsList = policyItem.getConditions();
			} else if (dataMaskPolicyItem != null && policyItem == null && rowFilterPolicyItem == null) {
				roles = dataMaskPolicyItem.getRoles();
				groups = dataMaskPolicyItem.getGroups();
				users = dataMaskPolicyItem.getUsers();
				accesses = dataMaskPolicyItem.getAccesses();
                                delegateAdmin = dataMaskPolicyItem.getDelegateAdmin();
                                conditionsList = dataMaskPolicyItem.getConditions();
                                dataMaskInfo = dataMaskPolicyItem.getDataMaskInfo();
                                String dataMaskType = dataMaskInfo.getDataMaskType();
                                String conditionExpr = dataMaskInfo.getConditionExpr();
                                String valueExpr = dataMaskInfo.getValueExpr();
                                String maskingInfo = "dataMasktype=[" + dataMaskType + "]";
                                if (conditionExpr != null && !conditionExpr.isEmpty() && valueExpr != null && !valueExpr.isEmpty()) {
                                        maskingInfo = maskingInfo + "; conditionExpr=[" + conditionExpr + "]";
                                }
                                cell = row.createCell(18);
                                cell.setCellValue(maskingInfo);
			} else if (rowFilterPolicyItem != null && policyItem == null && dataMaskPolicyItem == null) {
				roles = rowFilterPolicyItem.getRoles();
				groups = rowFilterPolicyItem.getGroups();
				users = rowFilterPolicyItem.getUsers();
				accesses = rowFilterPolicyItem.getAccesses();
                                delegateAdmin = rowFilterPolicyItem.getDelegateAdmin();
                                conditionsList = rowFilterPolicyItem.getConditions();
                                filterInfo = rowFilterPolicyItem.getRowFilterInfo();
                                String filterExpr = filterInfo.getFilterExpr();
                                cell = row.createCell(19);
                                cell.setCellValue(filterExpr);
			}
			if (CollectionUtils.isNotEmpty(accesses)) {
				for (RangerPolicyItemAccess access : accesses) {
					accessType = accessType + access.getType();
					accessType = accessType + " ,";
				}
				accessType = accessType.substring(0, accessType.lastIndexOf(","));
			}
			if (CollectionUtils.isNotEmpty(roles)) {
				roleNames = roleNames + roles.toString();
				StringTokenizer roleToken = new StringTokenizer(roleNames, "[]");
				while(roleToken.hasMoreTokens()) {
					roleNames = roleToken.nextToken().toString();
				}
			}
			if (CollectionUtils.isNotEmpty(groups)) {
				groupNames = groupNames + groups.toString();
				StringTokenizer groupToken = new StringTokenizer(groupNames, "[]");
				while(groupToken.hasMoreTokens()) {
					groupNames = groupToken.nextToken().toString();
				}
			}
			if (CollectionUtils.isNotEmpty(users)) {
				userNames = userNames + users.toString();
				StringTokenizer userToken = new StringTokenizer(userNames, "[]");
				while(userToken.hasMoreTokens()) {
					userNames = userToken.nextToken().toString();
				}
			}
                        String conditionValue = "";
                        for(RangerPolicyItemCondition conditions : conditionsList ){
                                String conditionType = conditions.getType();
                                List<String> conditionList = conditions.getValues();
                                conditionValue = conditionList.toString();
                                conditionKeyValue = conditionType + "=" + conditionValue;
                        }
			cell = row.createCell(3);
			cell.setCellValue(roleNames);
			cell = row.createCell(4);
			cell.setCellValue(groupNames);
			cell = row.createCell(5);
			cell.setCellValue(userNames);
			cell = row.createCell(6);
			cell.setCellValue(accessType.trim());
			cell = row.createCell(7);
			XXService xxservice = daoMgr.getXXService().findByName(policy.getService());
			String ServiceType = "";
			if (xxservice != null) {
				Long ServiceId = xxservice.getType();
				XXServiceDef xxservDef = daoMgr.getXXServiceDef().getById(ServiceId);
				if (xxservDef != null) {
					ServiceType = xxservDef.getName();
				}
			}
                        if(policyConditionType != null) {
                                policyConditionTypeValue = policyConditionType;
                        }
                        if (policyConditionType == null && ServiceType.equalsIgnoreCase("tag")) {
                                policyConditionTypeValue = POLICY_ALLOW_INCLUDE;
                        }else if (policyConditionType == null) {
                                policyConditionTypeValue = "";
                        }

			cell.setCellValue(ServiceType);
			cell = row.createCell(8);

		}
		if (policy.getIsEnabled()) {
			policyStatus = "Enabled";
		} else {
			policyStatus = "Disabled";
		}
                policyLabels = policy.getPolicyLabels();
                if (CollectionUtils.isNotEmpty(policyLabels)) {
                        policyLabelNames = policyLabelNames + policyLabels.toString();
                        StringTokenizer policyLabelToken = new StringTokenizer(policyLabelNames, "[]");
                        while(policyLabelToken.hasMoreTokens()) {
                        	policyLabelNames = policyLabelToken.nextToken().toString();
                        }
                }
		cell.setCellValue(policyStatus);
                cell = row.createCell(9);
                int policyTypeInt = policy.getPolicyType();
                switch (policyTypeInt) {
                        case RangerPolicy.POLICY_TYPE_ACCESS:
                                policyType = POLICY_TYPE_ACCESS;
                                break;

                        case RangerPolicy.POLICY_TYPE_DATAMASK:
                                policyType = POLICY_TYPE_DATAMASK;
                                break;

                        case RangerPolicy.POLICY_TYPE_ROWFILTER:
                                policyType = POLICY_TYPE_ROWFILTER;
                                break;
                }
                cell.setCellValue(policyType);
                cell = row.createCell(10);
                cell.setCellValue(delegateAdmin.toString().toUpperCase());
                cell = row.createCell(11);
                cell.setCellValue(isRecursiveValue);
                cell = row.createCell(12);
                cell.setCellValue(isExcludesValue);
                cell = row.createCell(13);
                serviceName = policy.getService();
                cell.setCellValue(serviceName);
                cell = row.createCell(14);
                description = policy.getDescription();
                cell.setCellValue(description);
                cell = row.createCell(15);
                cell.setCellValue(isAuditEnabled.toString().toUpperCase());
                cell = row.createCell(16);
                cell.setCellValue(conditionKeyValue.trim());
                cell = row.createCell(17);
                cell.setCellValue(policyConditionTypeValue);
                cell = row.createCell(20);
                cell.setCellValue(policyLabelNames);

        }
	private void createHeaderRow(Sheet sheet) {
		CellStyle cellStyle = sheet.getWorkbook().createCellStyle();
		Font font = sheet.getWorkbook().createFont();
		font.setBold(true);
		font.setFontHeightInPoints((short) 12);
		cellStyle.setFont(font);

		Row row = sheet.createRow(0);

		Cell cellID = row.createCell(0);
		cellID.setCellStyle(cellStyle);
		cellID.setCellValue("ID");

		Cell cellNAME = row.createCell(1);
		cellNAME.setCellStyle(cellStyle);
		cellNAME.setCellValue("Name");

		Cell cellResources = row.createCell(2);
		cellResources.setCellStyle(cellStyle);
		cellResources.setCellValue("Resources");

		Cell cellRoles = row.createCell(3);
		cellRoles.setCellStyle(cellStyle);
		cellRoles.setCellValue("Roles");

		Cell cellGroups = row.createCell(4);
		cellGroups.setCellStyle(cellStyle);
		cellGroups.setCellValue("Groups");

		Cell cellUsers = row.createCell(5);
		cellUsers.setCellStyle(cellStyle);
		cellUsers.setCellValue("Users");

		Cell cellAccesses = row.createCell(6);
		cellAccesses.setCellStyle(cellStyle);
		cellAccesses.setCellValue("Accesses");

		Cell cellServiceType = row.createCell(7);
		cellServiceType.setCellStyle(cellStyle);
		cellServiceType.setCellValue("Service Type");

		Cell cellStatus = row.createCell(8);
		cellStatus.setCellStyle(cellStyle);
		cellStatus.setCellValue("Status");

                Cell cellPolicyType = row.createCell(9);
                cellPolicyType.setCellStyle(cellStyle);
                cellPolicyType.setCellValue("Policy Type");

                Cell cellDelegateAdmin = row.createCell(10);
                cellDelegateAdmin.setCellStyle(cellStyle);
                cellDelegateAdmin.setCellValue("Delegate Admin");

                Cell cellIsRecursive = row.createCell(11);
                cellIsRecursive.setCellStyle(cellStyle);
                cellIsRecursive.setCellValue("isRecursive");

                Cell cellIsExcludes = row.createCell(12);
                cellIsExcludes.setCellStyle(cellStyle);
                cellIsExcludes.setCellValue("isExcludes");

                Cell cellServiceName = row.createCell(13);
                cellServiceName.setCellStyle(cellStyle);
                cellServiceName.setCellValue("Service Name");

                Cell cellDescription = row.createCell(14);
                cellDescription.setCellStyle(cellStyle);
                cellDescription.setCellValue("Description");

                Cell cellisAuditEnabled = row.createCell(15);
                cellisAuditEnabled.setCellStyle(cellStyle);
                cellisAuditEnabled.setCellValue("isAuditEnabled");

                Cell cellPolicyConditions = row.createCell(16);
                cellPolicyConditions.setCellStyle(cellStyle);
                cellPolicyConditions.setCellValue("Policy Conditions");

                Cell cellPolicyConditionType = row.createCell(17);
                cellPolicyConditionType.setCellStyle(cellStyle);
                cellPolicyConditionType.setCellValue("Policy Condition Type");

                Cell cellMaskingOptions = row.createCell(18);
                cellMaskingOptions.setCellStyle(cellStyle);
                cellMaskingOptions.setCellValue("Masking Options");

                Cell cellRowFilterExpr = row.createCell(19);
                cellRowFilterExpr.setCellStyle(cellStyle);
                cellRowFilterExpr.setCellValue("Row Filter Expr");

                Cell cellPolicyLabelName = row.createCell(20);
                cellPolicyLabelName.setCellStyle(cellStyle);
                cellPolicyLabelName.setCellValue("Policy Labels Name");
        }

	private RangerPolicyList searchRangerPolicies(SearchFilter searchFilter) {
		List<RangerPolicy> policyList = new ArrayList<RangerPolicy>();
		RangerPolicyList retList = new RangerPolicyList();
		Map<Long,RangerPolicy> policyMap=new HashMap<Long,RangerPolicy>();
		Set<Long> processedServices=new HashSet<Long>();
		Set<Long> processedPolicies=new HashSet<Long>();
		Comparator<RangerPolicy> comparator = new Comparator<RangerPolicy>() {
			public int compare(RangerPolicy c1, RangerPolicy c2) {
				return (int) ((c1.getId()).compareTo(c2.getId()));
			}
		};

		List<XXPolicy> xPolList    = null;
		Long           serviceId   = null;
		String         serviceName = searchFilter.getParam(ServiceREST.PARAM_SERVICE_NAME);

		if (StringUtils.isNotBlank(serviceName)) {
			serviceId = getRangerServiceByName(serviceName.trim());
			if (serviceId != null) {
				loadRangerPolicies(serviceId, processedServices, policyMap, searchFilter);
			}
		} else {
			xPolList = policyService.searchResources(searchFilter, policyService.searchFields, policyService.sortFields, retList);
			if (!CollectionUtils.isEmpty(xPolList)) {
				for (XXPolicy xXPolicy : xPolList) {
					if (!processedServices.contains(xXPolicy.getService())) {
						loadRangerPolicies(xXPolicy.getService(), processedServices, policyMap, searchFilter);
					}
				}
			}
		}

		String userName = searchFilter.getParam("user");
		if (!StringUtils.isEmpty(userName)) {
			searchFilter.setParam("user", RangerPolicyEngine.USER_CURRENT);
			List<XXPolicy> xPolListForMacroUser = policyService.searchResources(searchFilter,policyService.searchFields, policyService.sortFields, retList);
			Set<Long> processedSvcIdsForMacroUser = new HashSet<Long>();
			if (!CollectionUtils.isEmpty(xPolListForMacroUser)) {
				for (XXPolicy xXPolicy : xPolListForMacroUser) {
					if (!processedPolicies.contains(xXPolicy.getId())) {
						if (!processedSvcIdsForMacroUser.contains(xXPolicy.getService())) {
							loadRangerPolicies(xXPolicy.getService(), processedSvcIdsForMacroUser, policyMap, searchFilter);
						}
						if (policyMap.get(xXPolicy.getId()) != null) {
							policyList.add(policyMap.get(xXPolicy.getId()));
							processedPolicies.add(xXPolicy.getId());
						}
					}
				}
			}

            searchFilter.removeParam("user");
			Set<String> groupNames = daoMgr.getXXGroupUser().findGroupNamesByUserName(userName);
			groupNames.add(RangerConstants.GROUP_PUBLIC);
			Set<Long> processedSvcIdsForGroup = new HashSet<Long>();
			Set<String> processedGroupsName = new HashSet<String>();
			List<XXPolicy> xPolList2;
				for (String groupName : groupNames) {
					searchFilter.setParam("group", groupName);
					xPolList2 = policyService.searchResources(searchFilter, policyService.searchFields, policyService.sortFields, retList);
					if (!CollectionUtils.isEmpty(xPolList2)) {
						for (XXPolicy xPol2 : xPolList2) {
							if(xPol2!=null){
								if (!processedPolicies.contains(xPol2.getId())) {
									if (!processedSvcIdsForGroup.contains(xPol2.getService())
											|| !processedGroupsName.contains(groupName)) {
										loadRangerPolicies(xPol2.getService(), processedSvcIdsForGroup, policyMap, searchFilter);
										processedGroupsName.add(groupName);
									}
									if (policyMap.containsKey(xPol2.getId())) {
										policyList.add(policyMap.get(xPol2.getId()));
										processedPolicies.add(xPol2.getId());
									}
								}
							}
						}
					}
				}
		}
		if (!CollectionUtils.isEmpty(xPolList)) {
			if (isSearchQuerybyResource(searchFilter)) {
				if (MapUtils.isNotEmpty(policyMap)) {
					for(Entry<Long,RangerPolicy> entry:policyMap.entrySet()) {
						if (!processedPolicies.contains(entry.getKey())) {
							policyList.add(entry.getValue());
							processedPolicies.add(entry.getKey());
						}
					}
				}
			} else {
				for (XXPolicy xPol : xPolList) {
					if (xPol != null) {
						if (!processedPolicies.contains(xPol.getId())) {
							if (!processedServices.contains(xPol.getService())) {
								loadRangerPolicies(xPol.getService(), processedServices, policyMap, searchFilter);
							}
							if (policyMap.containsKey(xPol.getId())) {
								policyList.add(policyMap.get(xPol.getId()));
								processedPolicies.add(xPol.getId());
							}
						}
					}
				}
			}
		} else {
			if (MapUtils.isNotEmpty(policyMap)) {
				for(Entry<Long,RangerPolicy> entry:policyMap.entrySet()) {
					if (!processedPolicies.contains(entry.getKey())) {
						policyList.add(entry.getValue());
						processedPolicies.add(entry.getKey());
					}
				}
			}
		}

		if (CollectionUtils.isNotEmpty(policyList)) {
			Collections.sort(policyList, comparator);
		}
		retList.setPolicies(policyList);
		return retList;
	}

	private boolean isSearchQuerybyResource(SearchFilter searchFilter) {
		boolean ret = false;
		Map<String, String> filterResourcesPrefix = searchFilter.getParamsWithPrefix(SearchFilter.RESOURCE_PREFIX, true);
		if(MapUtils.isNotEmpty(filterResourcesPrefix)) {
			ret = true;
		}
		if(!ret) {
			Map<String, String> filterResourcesPolResource = searchFilter.getParamsWithPrefix(SearchFilter.POL_RESOURCE, true);
			if (MapUtils.isNotEmpty(filterResourcesPolResource)) {
				ret = true;
			}
		}
		return ret;
	}

	private  Long getRangerServiceByName(String name) {
		XXService    xxService    = null;
		XXServiceDao xxServiceDao = daoMgr.getXXService();
		if (xxServiceDao != null ) {
			xxService = xxServiceDao.findByName(name);
		}
		return xxService == null ? null : xxService.getId();
	}

	private void loadRangerPolicies(Long serviceId,Set<Long> processedServices,Map<Long,RangerPolicy> policyMap,SearchFilter searchFilter){
		try {
			List<RangerPolicy> tempPolicyList = getServicePolicies(serviceId,searchFilter);
			if(!CollectionUtils.isEmpty(tempPolicyList)){
				for (RangerPolicy rangerPolicy : tempPolicyList) {
					if(!policyMap.containsKey(rangerPolicy.getId())){
						policyMap.put(rangerPolicy.getId(), rangerPolicy);
					}
				}
			}
			processedServices.add(serviceId);
		} catch (Exception e) {
		}
	}
	public void getServiceUpgraded(){
		LOG.info("==> ServiceDBStore.getServiceUpgraded()");
		updateServiceWithCustomProperty();
		LOG.info("<== ServiceDBStore.getServiceUpgraded()");
	}
	private void updateServiceWithCustomProperty() {		
			LOG.info("Adding custom properties to services");
			SearchFilter filter = new SearchFilter();
			try {
				List<RangerService> lstRangerService = getServices(filter);
				for(RangerService rangerService : lstRangerService){
					String serviceUser = PropertiesUtil.getProperty("ranger.plugins."+rangerService.getType()+".serviceuser");
					if(!StringUtils.isEmpty(serviceUser)){
						boolean chkServiceUpdate = false;
						LOG.debug("customproperty = " + rangerService.getConfigs().get(ServiceREST.Allowed_User_List_For_Download) + " for service = " + rangerService.getName());
						if(!rangerService.getConfigs().containsKey(ServiceREST.Allowed_User_List_For_Download)){
							rangerService.getConfigs().put(ServiceREST.Allowed_User_List_For_Download, serviceUser);
							chkServiceUpdate = true;
		                }
		                if((!rangerService.getConfigs().containsKey(ServiceREST.Allowed_User_List_For_Grant_Revoke)) && ("hbase".equalsIgnoreCase(rangerService.getType()) || "hive".equalsIgnoreCase(rangerService.getType()))){
							rangerService.getConfigs().put(ServiceREST.Allowed_User_List_For_Grant_Revoke, serviceUser);
							chkServiceUpdate = true;
		                }
		                if(!rangerService.getConfigs().containsKey(TagREST.Allowed_User_List_For_Tag_Download)){
							rangerService.getConfigs().put(TagREST.Allowed_User_List_For_Tag_Download, serviceUser);
							chkServiceUpdate = true;
		                }
		                if(chkServiceUpdate){
		                	updateService(rangerService, null);
							if(LOG.isDebugEnabled()){
								LOG.debug("Updated service "+rangerService.getName()+" with custom properties in secure environment");
							}
		                }
					}
				}
			} catch (Throwable e) {
				LOG.error("updateServiceWithCustomProperty failed with exception : "+e.getMessage());
			}
	}

	private String  getAuditMode(String serviceTypeName, String serviceName) {
		String ret = config.get("ranger.audit.global.mode");
		if (StringUtils.isNotBlank(ret)) {
			return ret;
		}
		ret = config.get("ranger.audit.servicedef." + serviceTypeName + ".mode");
		if (StringUtils.isNotBlank(ret)) {
			return ret;
		}
		ret = config.get("ranger.audit.service." + serviceName + ".mode");
		if (StringUtils.isNotBlank(ret)) {
			return ret;
		}
		return RangerPolicyEngine.AUDIT_DEFAULT;
	}

	private void createGenericUsers() {
		VXUser genericUser = new VXUser();

		genericUser.setName(RangerPolicyEngine.USER_CURRENT);
		genericUser.setDescription(RangerPolicyEngine.USER_CURRENT);
		xUserService.createXUserWithOutLogin(genericUser);

		genericUser.setName(RangerPolicyEngine.RESOURCE_OWNER);
		genericUser.setDescription(RangerPolicyEngine.RESOURCE_OWNER);
		xUserService.createXUserWithOutLogin(genericUser);
	}

	private void initRMSDaos() {
		daoMgr.getXXService();
		daoMgr.getXXRMSMappingProvider();
		daoMgr.getXXRMSNotification();
		daoMgr.getXXRMSServiceResource();
		daoMgr.getXXRMSResourceMapping();
	}

	public void resetPolicyUpdateLog(int retentionInDays, Integer policyChangeType) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> resetPolicyUpdateLog(" + retentionInDays + ", " + policyChangeType + ")");
		}

		daoMgr.getXXPolicyChangeLog().deleteOlderThan(retentionInDays);

		List<Long> allServiceIds = daoMgr.getXXService().getAllServiceIds();
		if (CollectionUtils.isNotEmpty(allServiceIds)) {
			for (Long serviceId : allServiceIds) {
				ServiceVersionUpdater updater = new ServiceVersionUpdater(daoMgr, serviceId, VERSION_TYPE.POLICY_VERSION, null, policyChangeType, null);
				persistVersionChange(updater);
			}
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== resetPolicyUpdateLog(" + retentionInDays + ", " + policyChangeType + ")");

		}
	}
	public void resetTagUpdateLog(int retentionInDays, ServiceTags.TagsChangeType tagChangeType) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> resetTagUpdateLog(" + retentionInDays + ", " + tagChangeType + ")");
		}

		daoMgr.getXXTagChangeLog().deleteOlderThan(retentionInDays);

		List<Long> allServiceIds = daoMgr.getXXService().getAllServiceIds();
		if (CollectionUtils.isNotEmpty(allServiceIds)) {
			for (Long serviceId : allServiceIds) {
				ServiceVersionUpdater updater = new ServiceVersionUpdater(daoMgr, serviceId, VERSION_TYPE.TAG_VERSION, tagChangeType, null, null);
				persistVersionChange(updater);
			}
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== resetTagUpdateLog(" + retentionInDays + ", " + tagChangeType + ")");

		}
	}

    public List<String> getPolicyLabels(SearchFilter searchFilter) {
        if (LOG.isDebugEnabled()) {
                LOG.debug("==> ServiceDBStore.getPolicyLabels()");
        }
        VXPolicyLabelList vxPolicyLabelList = new VXPolicyLabelList();
        @SuppressWarnings("unchecked")
                List<XXPolicyLabel> xPolList = (List<XXPolicyLabel>) policyLabelsService.searchResources(searchFilter,
                        policyLabelsService.searchFields, policyLabelsService.sortFields, vxPolicyLabelList);
        List<String> result = new ArrayList<String>();
        for (XXPolicyLabel xPolicyLabel : xPolList) {
                result.add(xPolicyLabel.getPolicyLabel());
        }
        if (LOG.isDebugEnabled()) {
                LOG.debug("<== ServiceDBStore.getPolicyLabels()");
        }
        return result;
    }

    public enum METRIC_TYPE {
        USER_GROUP {
            @Override
            public String getMetric(ServiceDBStore ref, SearchCriteria searchCriteria) {
                return ref.getMetricOfTypeUserGroup(searchCriteria);
            }},
        AUDITS {
            @Override
            public String getMetric(ServiceDBStore ref, SearchCriteria searchCriteria) {
                return ref.getMetricOfTypeAudits(searchCriteria);
            }},
        SERVICES {
            @Override
            public String getMetric(ServiceDBStore ref, SearchCriteria searchCriteria) {
                return ref.getMetricOfTypeServices(searchCriteria);
            }},
        POLICIES {
            @Override
            public String getMetric(ServiceDBStore ref, SearchCriteria searchCriteria) {
                return ref.getMetricOfTypePolicies(searchCriteria);
            }},
        DATABASE {
            @Override
            public String getMetric(ServiceDBStore ref, SearchCriteria searchCriteria) {
                return ref.getMetricOfTypeDatabase(searchCriteria);
            }},
        CONTEXT_ENRICHERS {
            @Override
            public String getMetric(ServiceDBStore ref, SearchCriteria searchCriteria) {
                return ref.getMetricOfTypeContextEnrichers(searchCriteria);
            }},
        DENY_CONDITIONS {
            @Override
            public String getMetric(ServiceDBStore ref, SearchCriteria searchCriteria) {
                return ref.getMetricOfTypeDenyConditions(searchCriteria);
            }};

        abstract String getMetric(final ServiceDBStore ref, final SearchCriteria searchCriteria);

        public static METRIC_TYPE getMetricTypeByName(final String metricTypeName) {
            METRIC_TYPE ret = null;

            if (metricTypeName != null) {
                switch(metricTypeName) {
                    case "usergroup":
                        ret = METRIC_TYPE.USER_GROUP;
                        break;
                    case "audits":
                        ret = METRIC_TYPE.AUDITS;
                        break;
                    case "services":
                        ret = METRIC_TYPE.SERVICES;
                        break;
                    case "policies":
                        ret = METRIC_TYPE.POLICIES;
                        break;
                    case "database":
                        ret = METRIC_TYPE.DATABASE;
                        break;
                    case "contextenrichers":
                        ret = METRIC_TYPE.CONTEXT_ENRICHERS;
                        break;
                    case "denyconditions":
                        ret = METRIC_TYPE.DENY_CONDITIONS;
                        break;
                }
            }

            return ret;
        }
    }

    private String getMetricOfTypeUserGroup(final SearchCriteria searchCriteria) {
        String ret = null;

        try {
            VXGroupList       vxGroupList        = xUserMgr.searchXGroups(searchCriteria);
            long              groupCount         = vxGroupList.getTotalCount();
            ArrayList<String> userAdminRoleCount = new ArrayList<String>();
            userAdminRoleCount.add(RangerConstants.ROLE_SYS_ADMIN);
            long              userSysAdminCount         = getUserCountBasedOnUserRole(userAdminRoleCount);
            ArrayList<String> userAdminAuditorRoleCount = new ArrayList<String>();
            userAdminAuditorRoleCount.add(RangerConstants.ROLE_ADMIN_AUDITOR);
            long              userSysAdminAuditorCount = getUserCountBasedOnUserRole(userAdminAuditorRoleCount);
            ArrayList<String> userRoleListKeyRoleAdmin = new ArrayList<String>();
            userRoleListKeyRoleAdmin.add(RangerConstants.ROLE_KEY_ADMIN);
            long              userKeyAdminCount               = getUserCountBasedOnUserRole(userRoleListKeyRoleAdmin);
            ArrayList<String> userRoleListKeyadminAduitorRole = new ArrayList<String>();
            userRoleListKeyadminAduitorRole.add(RangerConstants.ROLE_KEY_ADMIN_AUDITOR);
            long              userKeyadminAuditorCount = getUserCountBasedOnUserRole(userRoleListKeyadminAduitorRole);
            ArrayList<String> userRoleListUser         = new ArrayList<String>();
            userRoleListUser.add(RangerConstants.ROLE_USER);
            long                   userRoleCount        = getUserCountBasedOnUserRole(userRoleListUser);
            long                   userTotalCount       = userSysAdminCount + userKeyAdminCount + userRoleCount + userKeyadminAuditorCount + userSysAdminAuditorCount;
            VXMetricUserGroupCount metricUserGroupCount = new VXMetricUserGroupCount();
            metricUserGroupCount.setUserCountOfUserRole(userRoleCount);
            metricUserGroupCount.setUserCountOfKeyAdminRole(userKeyAdminCount);
            metricUserGroupCount.setUserCountOfSysAdminRole(userSysAdminCount);
            metricUserGroupCount.setUserCountOfKeyadminAuditorRole(userKeyadminAuditorCount);
            metricUserGroupCount.setUserCountOfSysAdminAuditorRole(userSysAdminAuditorCount);
            metricUserGroupCount.setUserTotalCount(userTotalCount);
            metricUserGroupCount.setGroupCount(groupCount);
            Gson         gson               = new GsonBuilder().create();
            final String jsonUserGroupCount = gson.toJson(metricUserGroupCount);
            ret = jsonUserGroupCount;
        } catch (Exception e) {
            LOG.error("ServiceDBStore.getMetricByType(usergroup): Error calculating Metric for usergroup : " + e.getMessage());
        }

        return ret;
    }

    private String getMetricOfTypeAudits(final SearchCriteria searchCriteria) {
        String ret = null;
        try {
            int        clientTimeOffsetInMinute = RestUtil.getClientTimeOffset();
            String     defaultDateFormat        = "MM/dd/yyyy";
            DateFormat formatter                = new SimpleDateFormat(defaultDateFormat);

            VXMetricAuditDetailsCount auditObj             = new VXMetricAuditDetailsCount();
            DateUtil                  dateUtilTwoDays      = new DateUtil();
            Date                      startDateUtilTwoDays = dateUtilTwoDays.getDateFromNow(-2);
            Date                      dStart2              = restErrorUtil.parseDate(formatter.format(startDateUtilTwoDays), "Invalid value for startDate", MessageEnums.INVALID_INPUT_DATA, null,
            "startDate", defaultDateFormat);

            Date endDateTwoDays = MiscUtil.getUTCDate();
            Date dEnd2          = restErrorUtil.parseDate(formatter.format(endDateTwoDays), "Invalid value for endDate", MessageEnums.INVALID_INPUT_DATA, null, "endDate", defaultDateFormat);
            dEnd2 = dateUtilTwoDays.getDateFromGivenDate(dEnd2, 0, 23, 59, 59);
            dEnd2 = dateUtilTwoDays.addTimeOffset(dEnd2, clientTimeOffsetInMinute);
            VXMetricServiceCount deniedCountObj = getAuditsCount(0, dStart2, dEnd2);
            auditObj.setDenialEventsCountTwoDays(deniedCountObj);

            VXMetricServiceCount allowedCountObj = getAuditsCount(1, dStart2, dEnd2);
            auditObj.setAccessEventsCountTwoDays(allowedCountObj);

            long totalAuditsCountTwoDays = deniedCountObj.getTotalCount() + allowedCountObj.getTotalCount();
            auditObj.setSolrIndexCountTwoDays(totalAuditsCountTwoDays);

            DateUtil dateUtilWeek      = new DateUtil();
            Date     startDateUtilWeek = dateUtilWeek.getDateFromNow(-7);
            Date     dStart7           = restErrorUtil.parseDate(formatter.format(startDateUtilWeek), "Invalid value for startDate", MessageEnums.INVALID_INPUT_DATA, null, "startDate",
            defaultDateFormat);

            Date     endDateWeek  = MiscUtil.getUTCDate();
            DateUtil dateUtilweek = new DateUtil();
            Date     dEnd7        = restErrorUtil.parseDate(formatter.format(endDateWeek), "Invalid value for endDate", MessageEnums.INVALID_INPUT_DATA, null, "endDate", defaultDateFormat);
            dEnd7 = dateUtilweek.getDateFromGivenDate(dEnd7, 0, 23, 59, 59);
            dEnd7 = dateUtilweek.addTimeOffset(dEnd7, clientTimeOffsetInMinute);
            VXMetricServiceCount deniedCountObjWeek = getAuditsCount(0, dStart7, dEnd7);
            auditObj.setDenialEventsCountWeek(deniedCountObjWeek);

            VXMetricServiceCount allowedCountObjWeek = getAuditsCount(1, dStart7, dEnd7);
            auditObj.setAccessEventsCountWeek(allowedCountObjWeek);

            long totalAuditsCountWeek = deniedCountObjWeek.getTotalCount() + allowedCountObjWeek.getTotalCount();
            auditObj.setSolrIndexCountWeek(totalAuditsCountWeek);

            Gson         gson      = new GsonBuilder().create();
            final String jsonAudit = gson.toJson(auditObj);
            ret = jsonAudit;
        } catch (Exception e) {
            LOG.error("ServiceDBStore.getMetricByType(audits): Error calculating Metric for audits : " + e.getMessage());
        }

        return ret;
    }

    private String getMetricOfTypeServices(final SearchCriteria searchCriteria) {
        String ret = null;
        try {
            SearchFilter serviceFilter = new SearchFilter();
            serviceFilter.setMaxRows(200);
            serviceFilter.setStartIndex(0);
            serviceFilter.setGetCount(true);
            serviceFilter.setSortBy("serviceId");
            serviceFilter.setSortType("asc");
            VXMetricServiceCount vXMetricServiceCount = new VXMetricServiceCount();
            PList<RangerService> paginatedSvcs        = getPaginatedServices(serviceFilter);
            long                 totalServiceCount    = paginatedSvcs.getTotalCount();
            List<RangerService>  rangerServiceList    = paginatedSvcs.getList();
            Map<String, Long>    services             = new HashMap<String, Long>();
            for (Object rangerService : rangerServiceList) {
                RangerService RangerServiceObj = (RangerService) rangerService;
                String        serviceName      = RangerServiceObj.getType();
                if (!(services.containsKey(serviceName))) {
                    serviceFilter.setParam("serviceType", serviceName);
                    PList<RangerService> paginatedSvcscount = getPaginatedServices(serviceFilter);
                    services.put(serviceName, paginatedSvcscount.getTotalCount());
                }
            }
            vXMetricServiceCount.setServiceBasedCountList(services);
            vXMetricServiceCount.setTotalCount(totalServiceCount);
            Gson         gson         = new GsonBuilder().create();
            final String jsonServices = gson.toJson(vXMetricServiceCount);
            ret = jsonServices;
        } catch (Exception e) {
            LOG.error("ServiceDBStore.getMetricByType(services): Error calculating Metric for services : " + e.getMessage());
        }
        return ret;
    }

    private String getMetricOfTypePolicies(final SearchCriteria searchCriteria) {
        String ret = null;
        try {
            SearchFilter policyFilter = new SearchFilter();
            policyFilter.setMaxRows(200);
            policyFilter.setStartIndex(0);
            policyFilter.setGetCount(true);
            policyFilter.setSortBy("serviceId");
            policyFilter.setSortType("asc");
            VXMetricPolicyWithServiceNameCount vXMetricPolicyWithServiceNameCount = new VXMetricPolicyWithServiceNameCount();
            PList<RangerPolicy>                paginatedSvcsList                  = getPaginatedPolicies(policyFilter);
            vXMetricPolicyWithServiceNameCount.setTotalCount(paginatedSvcsList.getTotalCount());
            Map<String, VXMetricServiceNameCount> servicesWithPolicy = new HashMap<String, VXMetricServiceNameCount>();
            for (int k = 2; k >= 0; k--) {
                String                   policyType               = String.valueOf(k);
                VXMetricServiceNameCount vXMetricServiceNameCount = getVXMetricServiceCount(policyType);
                if (k == 2) {
                    servicesWithPolicy.put("rowFilteringPolicies", vXMetricServiceNameCount);
                } else if (k == 1) {
                    servicesWithPolicy.put("maskingPolicies", vXMetricServiceNameCount);
                } else if (k == 0) {
                    servicesWithPolicy.put("resourceAccessPolicies", vXMetricServiceNameCount);
                }
            }
            Map<String, Map<String, Long>> tagMap                     = new HashMap<String, Map<String, Long>>();
            Map<String, Long>              ServiceNameWithPolicyCount = new HashMap<String, Long>();
            boolean                        tagFlag                    = false;
            if (tagFlag == false) {
                policyFilter.setParam("serviceType", "tag");
                PList<RangerPolicy> policiestype = getPaginatedPolicies(policyFilter);
                List<RangerPolicy>  policies     = policiestype.getList();
                for (RangerPolicy rangerPolicy : policies) {
                    if (ServiceNameWithPolicyCount.containsKey(rangerPolicy.getService())) {
                        Long tagServicePolicyCount = ServiceNameWithPolicyCount.get(rangerPolicy.getService()) + 1l;
                        ServiceNameWithPolicyCount.put(rangerPolicy.getService(), tagServicePolicyCount);
                    } else if (!rangerPolicy.getName().isEmpty()) {
                        ServiceNameWithPolicyCount.put(rangerPolicy.getService(), 1l);
                    }
                }
                tagMap.put("tag", ServiceNameWithPolicyCount);
                long                     tagCount                 = policiestype.getTotalCount();
                VXMetricServiceNameCount vXMetricServiceNameCount = new VXMetricServiceNameCount();
                vXMetricServiceNameCount.setServiceBasedCountList(tagMap);
                vXMetricServiceNameCount.setTotalCount(tagCount);
                servicesWithPolicy.put("tagAccessPolicies", vXMetricServiceNameCount);
                tagFlag = true;
            }
            vXMetricPolicyWithServiceNameCount.setPolicyCountList(servicesWithPolicy);
            Gson         gson         = new GsonBuilder().create();
            final String jsonPolicies = gson.toJson(vXMetricPolicyWithServiceNameCount);
            ret = jsonPolicies;
        } catch (Exception e) {
            LOG.error("ServiceDBStore.getMetricByType(policies): Error calculating Metric for policies : " + e.getMessage());
        }
        return ret;
    }

    private String getMetricOfTypeDatabase(final SearchCriteria searchCriteria) {
        String ret = null;
        try {
            int    dbFlavor      = RangerBizUtil.getDBFlavor();
            String dbFlavourType = "Unknow ";
            if (dbFlavor == AppConstants.DB_FLAVOR_MYSQL) {
                dbFlavourType = "MYSQL ";
            } else if (dbFlavor == AppConstants.DB_FLAVOR_ORACLE) {
                dbFlavourType = "ORACLE ";
            } else if (dbFlavor == AppConstants.DB_FLAVOR_POSTGRES) {
                dbFlavourType = "POSTGRES ";
            } else if (dbFlavor == AppConstants.DB_FLAVOR_SQLANYWHERE) {
                dbFlavourType = "SQLANYWHERE ";
            } else if (dbFlavor == AppConstants.DB_FLAVOR_SQLSERVER) {
                dbFlavourType = "SQLSERVER ";
            }
            String       dbDetail     = dbFlavourType + bizUtil.getDBVersion();
            Gson         gson         = new GsonBuilder().create();
            final String jsonDBDetail = gson.toJson(dbDetail);
            ret = jsonDBDetail;
        } catch (Exception e) {
            LOG.error("ServiceDBStore.getMetricByType(database): Error calculating Metric for database : " + e.getMessage());
        }
        return ret;
    }

    private String getMetricOfTypeContextEnrichers(final SearchCriteria searchCriteria) {
        String ret = null;
        try {
            SearchFilter filter = new SearchFilter();
            filter.setStartIndex(0);
            VXMetricContextEnricher serviceWithContextEnrichers = new VXMetricContextEnricher();
            PList<RangerServiceDef> paginatedSvcDefs            = getPaginatedServiceDefs(filter);
            List<RangerServiceDef>  repoTypeList                = paginatedSvcDefs.getList();
            if (repoTypeList != null) {
                for (RangerServiceDef repoType : repoTypeList) {
                    RangerServiceDef               rangerServiceDefObj = (RangerServiceDef) repoType;
                    String                         name                = rangerServiceDefObj.getName();
                    List<RangerContextEnricherDef> contextEnrichers    = rangerServiceDefObj.getContextEnrichers();
                    if (contextEnrichers != null && !contextEnrichers.isEmpty()) {
                        serviceWithContextEnrichers.setServiceName(name);
                        serviceWithContextEnrichers.setTotalCount(contextEnrichers.size());
                    }
                }
            }
            Gson         gson                 = new GsonBuilder().create();
            final String jsonContextEnrichers = gson.toJson(serviceWithContextEnrichers);
            ret = jsonContextEnrichers;
        } catch (Exception e) {
            LOG.error("ServiceDBStore.getMetricByType(contextenrichers): Error calculating Metric for contextenrichers : " + e.getMessage());
        }
        return ret;
    }

    private String getMetricOfTypeDenyConditions(final SearchCriteria searchCriteria) {
        String ret = null;
        try {
            SearchFilter policyFilter1 = new SearchFilter();
            policyFilter1.setMaxRows(200);
            policyFilter1.setStartIndex(0);
            policyFilter1.setGetCount(true);
            policyFilter1.setSortBy("serviceId");
            policyFilter1.setSortType("asc");
            policyFilter1.setParam("denyCondition", "true");
            int                     denyCount           = 0;
            Map<String, Integer>    denyconditionsonMap = new HashMap<String, Integer>();
            PList<RangerServiceDef> paginatedSvcDefs    = getPaginatedServiceDefs(policyFilter1);
            if (paginatedSvcDefs != null) {
                List<RangerServiceDef> rangerServiceDefs = paginatedSvcDefs.getList();
                if (rangerServiceDefs != null && !rangerServiceDefs.isEmpty()) {
                    for (RangerServiceDef rangerServiceDef : rangerServiceDefs) {
                        if (rangerServiceDef != null) {
                            String serviceDef = rangerServiceDef.getName();
                            if (!StringUtils.isEmpty(serviceDef)) {
                                policyFilter1.setParam("serviceType", serviceDef);
                                PList<RangerPolicy> policiesList = getPaginatedPolicies(policyFilter1);
                                if (policiesList != null && policiesList.getListSize() > 0) {
                                    int policyListCount = policiesList.getListSize();
                                    if (policyListCount > 0 && policiesList.getList() != null) {
                                        List<RangerPolicy> policies = policiesList.getList();
                                        for (RangerPolicy policy : policies) {
                                            if (policy != null) {
                                                List<RangerPolicyItem> policyItem = policy.getDenyPolicyItems();
                                                if (policyItem != null && !policyItem.isEmpty()) {
                                                    if (denyconditionsonMap.get(serviceDef) != null) {
                                                        denyCount = denyconditionsonMap.get(serviceDef) + denyCount + policyItem.size();
                                                    } else {
                                                        denyCount = denyCount + policyItem.size();
                                                    }
                                                }
                                                List<RangerPolicyItem> policyItemExclude = policy.getDenyExceptions();
                                                if (policyItemExclude != null && !policyItemExclude.isEmpty()) {
                                                    if (denyconditionsonMap.get(serviceDef) != null) {
                                                        denyCount = denyconditionsonMap.get(serviceDef) + denyCount + policyItemExclude.size();
                                                    } else {
                                                        denyCount = denyCount + policyItemExclude.size();
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                policyFilter1.removeParam("serviceType");
                            }
                            denyconditionsonMap.put(serviceDef, denyCount);
                            denyCount = 0;
                        }
                    }
                }
            }
            Gson   gson                      = new GsonBuilder().create();
            String jsonContextDenyCondtionOn = gson.toJson(denyconditionsonMap);
            ret = jsonContextDenyCondtionOn;
        } catch (Exception e) {
            LOG.error("ServiceDBStore.getMetricByType(denyconditions): Error calculating Metric for denyconditions : " + e.getMessage());
        }
        return ret;
    }

    public String getMetricByType(final METRIC_TYPE metricType) throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> ServiceDBStore.getMetricByType(" + metricType + ")");
        }
        String ret = null;
        try {
            SearchCriteria searchCriteria = new SearchCriteria();
            searchCriteria.setStartIndex(0);
            searchCriteria.setMaxRows(100);
            searchCriteria.setGetCount(true);
            searchCriteria.setSortType("asc");

            ret = metricType.getMetric(this, searchCriteria);
        } catch (Exception e) {
            LOG.error("ServiceDBStore.getMetricByType(" + metricType + "): Error calculating Metric : " + e.getMessage());
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("== ServiceDBStore.getMetricByType(" + metricType + "): " + ret);
        }
        return ret;
    }

    private VXMetricServiceNameCount getVXMetricServiceCount(String policyType) throws Exception {
            SearchFilter policyFilter1 = new SearchFilter();
            policyFilter1.setMaxRows(200);
            policyFilter1.setStartIndex(0);
            policyFilter1.setGetCount(true);
            policyFilter1.setSortBy("serviceId");
            policyFilter1.setSortType("asc");
            policyFilter1.setParam("policyType", policyType);
            PList<RangerPolicy> policies = getPaginatedPolicies(policyFilter1);
            PList<RangerService> paginatedSvcsSevice = getPaginatedServices(policyFilter1);
            List<RangerService> rangerServiceList = paginatedSvcsSevice.getList();
            Map<String, Map<String, Long> > servicesforPolicyType = new HashMap<String, Map<String, Long> >();

            long tagCount = 0;
            for (Object rangerService : rangerServiceList) {
                RangerService rangerServiceObj = (RangerService) rangerService;
                String servicetype = rangerServiceObj.getType();
                String serviceName =rangerServiceObj.getName();
                policyFilter1.setParam("serviceName", serviceName);
                Map<String, Long> servicesNamewithPolicyCount = new HashMap<String, Long>();
                    PList<RangerPolicy> policiestype = getPaginatedPolicies(policyFilter1);
                    long count = policiestype.getTotalCount();
                    if (count != 0) {
                        if (!"tag".equalsIgnoreCase(servicetype)) {
                            if (!(servicesforPolicyType.containsKey(servicetype))) {
                                servicesNamewithPolicyCount.put(serviceName, count);
                                servicesforPolicyType.put(servicetype, servicesNamewithPolicyCount);
                            }
                            else if (servicesforPolicyType.containsKey(servicetype)) {
                                Map<String, Long> previousPolicyCount = servicesforPolicyType.get(servicetype);
                                if(!previousPolicyCount.containsKey(serviceName)) {
                                    previousPolicyCount.put(serviceName, count);
                                    servicesforPolicyType.put(servicetype, previousPolicyCount);
                                }
                            }
                        } else {
                                tagCount = tagCount + count;
                        }
                    }
            }
            VXMetricServiceNameCount vXMetricServiceNameCount = new VXMetricServiceNameCount();
            vXMetricServiceNameCount.setServiceBasedCountList(servicesforPolicyType);
            long totalCountOfPolicyType =  0;
            totalCountOfPolicyType = policies.getTotalCount() - tagCount;
            vXMetricServiceNameCount.setTotalCount(totalCountOfPolicyType);
            return vXMetricServiceNameCount;
    }

    private VXMetricServiceCount getAuditsCount(int accessResult,Date startDate, Date endDate) throws Exception {
            long totalCountOfAudits = 0;
            SearchFilter filter = new SearchFilter();
            filter.setStartIndex(0);
            Map<String, Long> servicesRepoType = new HashMap<String, Long>();
            VXMetricServiceCount vXMetricServiceCount = new VXMetricServiceCount();
            PList<RangerServiceDef> paginatedSvcDefs = getPaginatedServiceDefs(filter);
            Iterable<RangerServiceDef> repoTypeGet = paginatedSvcDefs.getList();
            for (Object repo : repoTypeGet) {
                RangerServiceDef rangerServiceDefObj = (RangerServiceDef) repo;
                long id = rangerServiceDefObj.getId();
                String serviceRepoName = rangerServiceDefObj.getName();
                SearchCriteria searchCriteriaWithType = new SearchCriteria();
                searchCriteriaWithType.getParamList().put("repoType", id);
                searchCriteriaWithType.getParamList().put("accessResult", accessResult);
                searchCriteriaWithType.addParam("startDate", startDate);
                searchCriteriaWithType.addParam("endDate", endDate);
                searchCriteriaWithType.setMaxRows(0);
                searchCriteriaWithType.setGetCount(true);
                VXAccessAuditList vXAccessAuditListwithType = assetMgr.getAccessLogs(searchCriteriaWithType);
                long toltalCountOfRepo = vXAccessAuditListwithType.getTotalCount();
                if (toltalCountOfRepo != 0) {
                    servicesRepoType.put(serviceRepoName, toltalCountOfRepo);
                    totalCountOfAudits += toltalCountOfRepo;
                }
            }
            vXMetricServiceCount.setServiceBasedCountList(servicesRepoType);
            vXMetricServiceCount.setTotalCount(totalCountOfAudits);
            return vXMetricServiceCount;
    }

    private Long getUserCountBasedOnUserRole(@SuppressWarnings("rawtypes") List userRoleList) {
            SearchCriteria searchCriteria = new SearchCriteria();
            searchCriteria.setStartIndex(0);
            searchCriteria.setMaxRows(100);
            searchCriteria.setGetCount(true);
            searchCriteria.setSortType("asc");
            searchCriteria.addParam("userRoleList", userRoleList);
            VXUserList VXUserListKeyAdmin = xUserMgr.searchXUsers(searchCriteria);
            long userCount = VXUserListKeyAdmin.getTotalCount();
            return userCount;
    }

    public boolean isServiceAdminUser(String serviceName, String userName) {
		boolean ret=false;
		XXServiceConfigMap cfgSvcAdminUsers = daoMgr.getXXServiceConfigMap().findByServiceNameAndConfigKey(serviceName, SERVICE_ADMIN_USERS);
		String svcAdminUsers = cfgSvcAdminUsers != null ? cfgSvcAdminUsers.getConfigvalue() : null;
		if (svcAdminUsers != null) {
			for (String svcAdminUser : svcAdminUsers.split(",")) {
				if (userName.equals(svcAdminUser)) {
					ret=true;
					break;
				}
			}
		}
		return ret;
	}

	public static boolean isSupportsPolicyDeltas() {
		return SUPPORTS_POLICY_DELTAS;
	}

	public static boolean isSupportsRolesDownloadByService() {
		return isRolesDownloadedByService;
	}

	public static class ServiceVersionUpdater implements Runnable {
		final Long 			   serviceId;
		final RangerDaoManager daoManager;
		final VERSION_TYPE     versionType;
		final String           zoneName;
		final Integer          policyDeltaChange;
		final RangerPolicy     policy;
		      long             version = -1;

		final ServiceTags.TagsChangeType tagChangeType;
		final Long             resourceId;
		final Long             tagId;

		public ServiceVersionUpdater(RangerDaoManager daoManager, Long serviceId, VERSION_TYPE versionType, Integer policyDeltaType) {
			this(daoManager, serviceId, versionType, null, policyDeltaType, null);
		}

		public ServiceVersionUpdater(RangerDaoManager daoManager, Long serviceId, VERSION_TYPE versionType, String zoneName, Integer policyDeltaType, RangerPolicy policy ) {
			this.serviceId   = serviceId;
			this.daoManager  = daoManager;
			this.versionType = versionType;
			this.policyDeltaChange = policyDeltaType;
			this.zoneName    = zoneName;
			this.policy      = policy;
			this.tagChangeType = ServiceTags.TagsChangeType.NONE;
			this.resourceId    = null;
			this.tagId         = null;
		}

		public ServiceVersionUpdater(RangerDaoManager daoManager, Long serviceId, VERSION_TYPE versionType, ServiceTags.TagsChangeType tagChangeType, Long resourceId, Long tagId ) {
			this.serviceId   = serviceId;
			this.daoManager  = daoManager;
			this.versionType = versionType;
			this.zoneName    = null;
			this.policyDeltaChange = null;
			this.policy            = null;
			this.tagChangeType = tagChangeType;
			this.resourceId    = resourceId;
			this.tagId         = tagId;
		}

		@Override
		public void run() {
			ServiceDBStore.persistVersionChange(this);
		}

		@Override
		public String toString() {
			return "ServiceVersionUpdater:[ " +
					"serviceId="           + serviceId +
					", versionType="         + versionType +
					", version="             + version +
					", zoneName="            + zoneName +
					", policyDeltaChange="   + policyDeltaChange +
					", policy="              + policy +
					", tagChangeType="       + tagChangeType +
					", resourceId="          + resourceId +
					", tagId="               + tagId +
					" ]";
		}
	}

	@Override
	public Map<String, String> getServiceConfigForPlugin(Long serviceId) {
		Map<String, String> configs = new HashMap<String, String>();
		List<XXServiceConfigMap> xxServiceConfigMaps = daoMgr.getXXServiceConfigMap().findByServiceId(serviceId);
		if (CollectionUtils.isNotEmpty(xxServiceConfigMaps)) {
			for (XXServiceConfigMap svcConfMap : xxServiceConfigMaps) {
				if (StringUtils.startsWith(svcConfMap.getConfigkey(), RANGER_PLUGIN_CONFIG_PREFIX)) {
					configs.put(svcConfMap.getConfigkey(), svcConfMap.getConfigvalue());
				}
			}
		}
		return configs;
	}

	boolean hasServiceConfigForPluginChanged(List<XXServiceConfigMap> dbConfigMaps, Map<String, String> validConfigs) {
		boolean ret = false;
		Map<String, String> configs = new HashMap<String, String>();
		if (CollectionUtils.isNotEmpty(dbConfigMaps)) {
			for (XXServiceConfigMap dbConfigMap : dbConfigMaps) {
				if (StringUtils.startsWith(dbConfigMap.getConfigkey(), RANGER_PLUGIN_CONFIG_PREFIX)) {
					configs.put(dbConfigMap.getConfigkey(), dbConfigMap.getConfigvalue());
				}
			}
		}
		if (MapUtils.isNotEmpty(validConfigs)) {
			for (String key : validConfigs.keySet()) {
				if (StringUtils.startsWith(key, RANGER_PLUGIN_CONFIG_PREFIX)) {
					if (!StringUtils.equals(configs.get(key), validConfigs.get(key))) {
						return true;
					} else {
						configs.remove(key);
					}
				}
			}
		}
		if (configs.size() > 0) {
			return true;
		}

		return ret;
	}

	/**
	 * Removes given service from security zones.
	 * And if given service is the only service
	 * associated with security zone, remove zone.
	 * @param service
	 * @throws Exception
	 */
	private void disassociateZonesForService(RangerService service) throws Exception {
		String serviceName = service.getName();
		List<String> zonesNameList = daoMgr.getXXSecurityZoneDao().findZonesByServiceName(serviceName);
		if (CollectionUtils.isNotEmpty(zonesNameList)) {
			for (String zoneName : zonesNameList) {
				RangerSecurityZone securityZone = securityZoneStore.getSecurityZoneByName(zoneName);
				Map<String, RangerSecurityZoneService> zoneServices = securityZone.getServices();

				if (zoneServices != null && !zoneServices.isEmpty()) {
					zoneServices.remove(serviceName);
					securityZone.setServices(zoneServices);
					securityZoneStore.updateSecurityZoneById(securityZone);

					if (zoneServices.isEmpty()) {
						securityZoneStore.deleteSecurityZoneByName(zoneName);
					}
				}
			}
		}
	}

	private static ServicePolicies getUpdatedServicePoliciesForZones(ServicePolicies servicePolicies, Map<String, RangerSecurityZone.RangerSecurityZoneService> securityZones) {
		final ServicePolicies ret;

		if (MapUtils.isNotEmpty(securityZones)) {
			ret = new ServicePolicies();

			ret.setServiceDef(servicePolicies.getServiceDef());
			ret.setServiceId(servicePolicies.getServiceId());
			ret.setServiceName(servicePolicies.getServiceName());
			ret.setAuditMode(servicePolicies.getAuditMode());
			ret.setPolicyVersion(servicePolicies.getPolicyVersion());
			ret.setPolicyUpdateTime(servicePolicies.getPolicyUpdateTime());
			ret.setTagPolicies(servicePolicies.getTagPolicies());

			Map<String, ServicePolicies.SecurityZoneInfo> securityZonesInfo = new HashMap<>();

			if (CollectionUtils.isEmpty(servicePolicies.getPolicyDeltas())) {
				List<RangerPolicy> allPolicies = new ArrayList<>(servicePolicies.getPolicies());

				for (Map.Entry<String, RangerSecurityZone.RangerSecurityZoneService> entry : securityZones.entrySet()) {
					List<RangerPolicy> zonePolicies = extractZonePolicies(allPolicies, entry.getKey());

					if (CollectionUtils.isNotEmpty(zonePolicies)) {
						allPolicies.removeAll(zonePolicies);
					}

					ServicePolicies.SecurityZoneInfo securityZoneInfo = new ServicePolicies.SecurityZoneInfo();

					securityZoneInfo.setZoneName(entry.getKey());
					securityZoneInfo.setPolicies(zonePolicies);
					securityZoneInfo.setResources(entry.getValue().getResources());
					securityZoneInfo.setContainsAssociatedTagService(false);
					securityZonesInfo.put(entry.getKey(), securityZoneInfo);
				}

				ret.setPolicies(allPolicies);
			} else {
				List<RangerPolicyDelta> allPolicyDeltas = new ArrayList<>(servicePolicies.getPolicyDeltas());

				for (Map.Entry<String, RangerSecurityZone.RangerSecurityZoneService> entry : securityZones.entrySet()) {
					List<RangerPolicyDelta> zonePolicyDeltas = extractZonePolicyDeltas(allPolicyDeltas, entry.getKey());

					if (CollectionUtils.isNotEmpty(zonePolicyDeltas)) {
						allPolicyDeltas.removeAll(zonePolicyDeltas);
					}

					ServicePolicies.SecurityZoneInfo securityZoneInfo = new ServicePolicies.SecurityZoneInfo();

					securityZoneInfo.setZoneName(entry.getKey());
					securityZoneInfo.setPolicyDeltas(zonePolicyDeltas);
					securityZoneInfo.setResources(entry.getValue().getResources());
					securityZoneInfo.setContainsAssociatedTagService(false);
					securityZonesInfo.put(entry.getKey(), securityZoneInfo);
				}

				ret.setPolicyDeltas(allPolicyDeltas);
			}

			ret.setSecurityZones(securityZonesInfo);
		} else {
			ret = servicePolicies;
		}

		return ret;
	}

	private void patchAssociatedTagServiceInSecurityZoneInfos(ServicePolicies servicePolicies) {
		if (servicePolicies != null && MapUtils.isNotEmpty(servicePolicies.getSecurityZones())) {

			// Get list of zones that associated tag-service (if any) is associated with
			List<String> zonesInAssociatedTagService = new ArrayList<>();

			String tagServiceName = servicePolicies.getTagPolicies() != null ? servicePolicies.getTagPolicies().getServiceName() : null;

			if (StringUtils.isNotEmpty(tagServiceName)) {
				try {
					RangerService tagService = getServiceByName(tagServiceName);
					if (tagService != null && tagService.getIsEnabled()) {
						zonesInAssociatedTagService = daoMgr.getXXSecurityZoneDao().findZonesByTagServiceName(tagServiceName);
					}
				} catch (Exception exception) {
					LOG.warn("Could not get service associated with [" + tagServiceName + "]", exception);
				}
			}

			if (CollectionUtils.isNotEmpty(zonesInAssociatedTagService)) {
				for (Map.Entry<String, ServicePolicies.SecurityZoneInfo> entry : servicePolicies.getSecurityZones().entrySet()) {
					String zoneName = entry.getKey();
					ServicePolicies.SecurityZoneInfo securityZoneInfo = entry.getValue();

					securityZoneInfo.setContainsAssociatedTagService(zonesInAssociatedTagService.contains(zoneName));
				}
			}
		}
	}

	private static List<RangerPolicy> extractZonePolicies(final List<RangerPolicy> allPolicies, final String zoneName) {
		final List<RangerPolicy> ret = new ArrayList<>();

		for (RangerPolicy policy : allPolicies) {
			if (policy.getIsEnabled() && StringUtils.equals(policy.getZoneName(), zoneName)) {
				ret.add(policy);
			}
		}

		return ret;
	}

	private static List<RangerPolicyDelta> extractZonePolicyDeltas(final List<RangerPolicyDelta> allPolicyDeltas, final String zoneName) {
		final List<RangerPolicyDelta> ret = new ArrayList<>();

		for (RangerPolicyDelta delta : allPolicyDeltas) {
			if (StringUtils.equals(delta.getZoneName(), zoneName) && !StringUtils.equals(delta.getServiceType(), EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_TAG_NAME)) {
				ret.add(delta);
			}
		}

		return ret;
	}

	private ServicePolicies filterServicePolicies(ServicePolicies servicePolicies) {
		ServicePolicies ret = null;
		boolean containsDisabledResourcePolicies = false;
		boolean containsDisabledTagPolicies = false;

		if (servicePolicies != null) {
			List<RangerPolicy> policies = null;

			policies = servicePolicies.getPolicies();
			if (CollectionUtils.isNotEmpty(policies)) {
				for (RangerPolicy policy : policies) {
					if (!policy.getIsEnabled()) {
						containsDisabledResourcePolicies = true;
						break;
					}
				}
			}

			if (servicePolicies.getTagPolicies() != null) {
				policies = servicePolicies.getTagPolicies().getPolicies();
				if (CollectionUtils.isNotEmpty(policies)) {
					for (RangerPolicy policy : policies) {
						if (!policy.getIsEnabled()) {
							containsDisabledTagPolicies = true;
							break;
						}
					}
				}
			}

			if (!containsDisabledResourcePolicies && !containsDisabledTagPolicies) {
				ret = servicePolicies;
			} else {
				ret = new ServicePolicies();

				ret.setServiceDef(servicePolicies.getServiceDef());
				ret.setServiceId(servicePolicies.getServiceId());
				ret.setServiceName(servicePolicies.getServiceName());
				ret.setPolicyVersion(servicePolicies.getPolicyVersion());
				ret.setPolicyUpdateTime(servicePolicies.getPolicyUpdateTime());
				ret.setPolicies(servicePolicies.getPolicies());
				ret.setTagPolicies(servicePolicies.getTagPolicies());
				ret.setSecurityZones(servicePolicies.getSecurityZones());

				if (containsDisabledResourcePolicies) {
					List<RangerPolicy> filteredPolicies = new ArrayList<RangerPolicy>();
					for (RangerPolicy policy : servicePolicies.getPolicies()) {
						if (policy.getIsEnabled()) {
							filteredPolicies.add(policy);
						}
					}
					ret.setPolicies(filteredPolicies);
				}

				if (containsDisabledTagPolicies) {
					ServicePolicies.TagPolicies tagPolicies = new ServicePolicies.TagPolicies();

					tagPolicies.setServiceDef(servicePolicies.getTagPolicies().getServiceDef());
					tagPolicies.setServiceId(servicePolicies.getTagPolicies().getServiceId());
					tagPolicies.setServiceName(servicePolicies.getTagPolicies().getServiceName());
					tagPolicies.setPolicyVersion(servicePolicies.getTagPolicies().getPolicyVersion());
					tagPolicies.setPolicyUpdateTime(servicePolicies.getTagPolicies().getPolicyUpdateTime());

					List<RangerPolicy> filteredPolicies = new ArrayList<RangerPolicy>();
					for (RangerPolicy policy : servicePolicies.getTagPolicies().getPolicies()) {
						if (policy.getIsEnabled()) {
							filteredPolicies.add(policy);
						}
					}
					tagPolicies.setPolicies(filteredPolicies);

					ret.setTagPolicies(tagPolicies);
				}
			}
		}

		return ret;
	}

	private List<XXServiceConfigMap> getAuditFiltersServiceConfigByName(String searchUsrGrpRoleName) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("===> ServiceDBStore.getAuditFiltersServiceConfigByName( searchUsrGrpRoleName : "
					+ searchUsrGrpRoleName + ")");
		}
		List<XXServiceConfigMap> configMapToBeModified = null;

		if (StringUtils.isNotBlank(searchUsrGrpRoleName)) {
			configMapToBeModified = new ArrayList<XXServiceConfigMap>();
			XXServiceConfigMapDao configDao = daoMgr.getXXServiceConfigMap();
			List<XXServiceConfigMap> configs = configDao.findByConfigKey(ServiceDBStore.RANGER_PLUGIN_AUDIT_FILTERS);
			for (XXServiceConfigMap configMap : configs) {
				if (StringUtils.contains(configMap.getConfigvalue(), searchUsrGrpRoleName)) {
					configMapToBeModified.add(configMap);
				}
			}
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<=== ServiceDBStore.getAuditFiltersServiceConfigByName( searchUsrGrpRoleName : "
					+ searchUsrGrpRoleName + ") configMapToBeModified : " + configMapToBeModified);
		}
		return configMapToBeModified;
	}

	public enum REMOVE_REF_TYPE { USER, GROUP, ROLE }

	public void updateServiceAuditConfig(String searchUsrGrpRoleName, REMOVE_REF_TYPE removeRefType) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("===> ServiceDBStore.updateServiceAuditConfig( searchUsrGrpRoleName : " + searchUsrGrpRoleName + " removeRefType : "
					+ removeRefType + ")");
		}
		List<XXServiceConfigMap> configMapToBeModified = getAuditFiltersServiceConfigByName(searchUsrGrpRoleName);
		if (CollectionUtils.isNotEmpty(configMapToBeModified)) {
			for (XXServiceConfigMap xConfigMap : configMapToBeModified) {
				String jsonStr = xConfigMap.getConfigvalue() != null ? xConfigMap.getConfigvalue() : null;
				if (StringUtils.isNotBlank(jsonStr)) {
					List<AuditFilter> auditFilters = JsonUtils.jsonToAuditFilterList(jsonStr);
					int filterCount = auditFilters != null ? auditFilters.size() : 0;
					RangerService rangerService = null;
					if (filterCount > 0) {
						String userName = null;
						String groupName = null;
						String roleName = null;
						if (removeRefType == REMOVE_REF_TYPE.USER) {
							userName = searchUsrGrpRoleName;
						} else if (removeRefType == REMOVE_REF_TYPE.GROUP) {
							groupName = searchUsrGrpRoleName;
						} else if (removeRefType == REMOVE_REF_TYPE.ROLE) {
							roleName = searchUsrGrpRoleName;
						}
						removeUserGroupRoleReferences(auditFilters, userName, groupName, roleName);
						String updatedJsonStr = JsonUtils.listToJson(auditFilters);
						XXService xService = daoMgr.getXXService().getById(xConfigMap.getServiceId());
						rangerService = svcService.getPopulatedViewObject(xService);
						Map<String, String> configs = rangerService.getConfigs();
						if (configs.containsKey(ServiceDBStore.RANGER_PLUGIN_AUDIT_FILTERS)) {
							updatedJsonStr = StringUtils.isBlank(updatedJsonStr) ? ""
									: updatedJsonStr.replaceAll("\"", "'");

							configs.put(ServiceDBStore.RANGER_PLUGIN_AUDIT_FILTERS, updatedJsonStr);

							try {
								LOG.info("==>ServiceDBStore.updateServiceAuditConfig updating audit-filter of service : "+rangerService.getName()  +" as part of delete request for : " + searchUsrGrpRoleName);
								updateService(rangerService, null);
							} catch (Throwable excp) {
								LOG.error("updateService(" + rangerService + ") failed", excp);

								throw restErrorUtil.createRESTException(excp.getMessage());
							}
						}
					} else {
						if (LOG.isDebugEnabled()) {
							LOG.debug("ServiceDBStore.updateServiceAuditConfig audit filter count is zero ");
						}
					}
				}
			}
		} else {
			if (LOG.isDebugEnabled()) {
				LOG.info("ServiceDBStore.updateServiceAuditConfig no service audit filter Config map found for : "
					+ searchUsrGrpRoleName);
			}
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("<=== ServiceDBStore.updateServiceAuditConfig( searchUsrGrpRoleName : " + searchUsrGrpRoleName + " removeRefType : "
					+ removeRefType  + ")");
		}
	}

	private void removeUserGroupRoleReferences(List<AuditFilter> auditFilters, String user, String group, String role) {
		List<AuditFilter> itemsToRemove = null;
		if (LOG.isDebugEnabled()) {
			LOG.debug("===> ServiceDBStore.removeUserGroupRoleReferences( user : "+ user + " group : "+ group + " role : " + role + " auditFilters : " + auditFilters +")");
		}
		for (AuditFilter auditFilter : auditFilters) {
			boolean isAuditFilterModified = false;
			if (StringUtils.isNotEmpty(user) && CollectionUtils.isNotEmpty(auditFilter.getUsers())) {
				auditFilter.getUsers().remove(user);
				isAuditFilterModified = true;
			}
			if (StringUtils.isNotEmpty(group) && CollectionUtils.isNotEmpty(auditFilter.getGroups())) {
				auditFilter.getGroups().remove(group);
				isAuditFilterModified = true;
			}
			if (StringUtils.isNotEmpty(role) && CollectionUtils.isNotEmpty(auditFilter.getRoles())) {
				auditFilter.getRoles().remove(role);
				isAuditFilterModified = true;
			}
			if (isAuditFilterModified && CollectionUtils.isEmpty(auditFilter.getUsers())
					&& CollectionUtils.isEmpty(auditFilter.getGroups())
					&& CollectionUtils.isEmpty(auditFilter.getRoles())) {
				if (itemsToRemove == null) {
					itemsToRemove = new ArrayList<AuditFilter>();
				}
				itemsToRemove.add(auditFilter);
			}
		}
		if (CollectionUtils.isNotEmpty(itemsToRemove)) {
			auditFilters.removeAll(itemsToRemove);
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("<=== ServiceDBStore.removeUserGroupRoleReferences( user : "+ user + " group : "+ group + " role : " + role + " auditFilters : " + auditFilters +")");
		}
	}
}
