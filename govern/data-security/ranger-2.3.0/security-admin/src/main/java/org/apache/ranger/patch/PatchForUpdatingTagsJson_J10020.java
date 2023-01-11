/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Consolidates Ranger policy details into a JSON string and stores it into a
 * column in x_policy table After running this patch Ranger policy can be
 * completely read/saved into x_policy table and some related Ref tables (which
 * maintain ID->String mapping for each policy).
 *
 */

package org.apache.ranger.patch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.authorization.utils.JsonUtils;
import org.apache.ranger.authorization.utils.StringUtil;
import org.apache.ranger.biz.ServiceDBStore;
import org.apache.ranger.biz.TagDBStore;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.db.XXServiceResourceDao;
import org.apache.ranger.db.XXTagDao;
import org.apache.ranger.db.XXTagDefDao;
import org.apache.ranger.entity.XXPortalUser;
import org.apache.ranger.entity.XXResourceDef;
import org.apache.ranger.entity.XXService;
import org.apache.ranger.entity.XXServiceResource;
import org.apache.ranger.entity.XXServiceResourceElement;
import org.apache.ranger.entity.XXServiceResourceElementValue;
import org.apache.ranger.entity.XXTag;
import org.apache.ranger.entity.XXTagAttribute;
import org.apache.ranger.entity.XXTagAttributeDef;
import org.apache.ranger.entity.XXTagDef;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.plugin.model.RangerServiceResource;
import org.apache.ranger.plugin.model.RangerTag;
import org.apache.ranger.plugin.model.RangerTagDef;
import org.apache.ranger.plugin.model.RangerValiditySchedule;
import org.apache.ranger.plugin.util.SearchFilter;
import org.apache.ranger.service.RangerServiceResourceService;
import org.apache.ranger.service.RangerTagDefService;
import org.apache.ranger.service.RangerTagService;
import org.apache.ranger.util.CLIUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class PatchForUpdatingTagsJson_J10020 extends BaseLoader {

    private static final Logger logger = LoggerFactory.getLogger(PatchForUpdatingTagsJson_J10020.class);

    @Autowired
    RangerDaoManager daoMgr;

    @Autowired
    ServiceDBStore svcStore;

    @Autowired
    TagDBStore tagStore;

    @Autowired
    @Qualifier(value = "transactionManager")
    PlatformTransactionManager txManager;

    @Autowired
    RangerTagDefService tagDefService;

    @Autowired
    RangerTagService tagService;

    @Autowired
    RangerServiceResourceService serviceResourceService;

    public static void main(String[] args) {
        logger.info("main()");
        try {
            PatchForUpdatingTagsJson_J10020 loader = (PatchForUpdatingTagsJson_J10020) CLIUtil
                    .getBean(PatchForUpdatingTagsJson_J10020.class);

            loader.init();

            while (loader.isMoreToProcess()) {
                loader.load();
            }

            logger.info("Load complete. Exiting!!!");

            System.exit(0);
        } catch (Exception e) {
            logger.error("Error loading", e);
            System.exit(1);
        }
    }

    @Override
    public void init() throws Exception {
        // Do Nothing
    }

    @Override
    public void execLoad() {
        logger.info("==> PatchForUpdatingTagsJson.execLoad()");

        try {
            updateRangerTagsTablesWithTagsJson();
        } catch (Exception e) {
            logger.error("Error while UpdateRangerTagsTablesWithTagsJson()", e);
            System.exit(1);
        }

        logger.info("<== PatchForUpdatingTagsJson.execLoad()");
    }

    @Override
    public void printStats() {
        logger.info("Update Ranger Tags Tables with Json data ");
    }

    private void updateRangerTagsTablesWithTagsJson() throws Exception {
        logger.info("==> updateRangerTagsTablesWithTagsJson() ");

        List<RangerService> allServices = svcStore.getServices(new SearchFilter());

        if (CollectionUtils.isNotEmpty(allServices)) {
            TransactionTemplate txTemplate = new TransactionTemplate(txManager);

            for (RangerService service : allServices) {
                XXService                   dbService        = daoMgr.getXXService().getById(service.getId());
                RangerTagDBRetriever        tagsRetriever    = new RangerTagDBRetriever(daoMgr, txManager, dbService);
                Map<Long, RangerTagDef>     tagDefs          = tagsRetriever.getTagDefs();
                Map<Long, RangerTag>        tags             = tagsRetriever.getTags();
                List<RangerServiceResource> serviceResources = tagsRetriever.getServiceResources();

                XXTagDefDao          tagDefDao          = daoMgr.getXXTagDef();
                XXTagDao             tagDao             = daoMgr.getXXTag();
                XXServiceResourceDao serviceResourceDao = daoMgr.getXXServiceResource();

                if (MapUtils.isNotEmpty(tagDefs)) {
                    logger.info("==> Port " + tagDefs.size() + " Tag Definitions for service(name=" + dbService.getName() + ")");

                    for (Map.Entry<Long, RangerTagDef> entry : tagDefs.entrySet()) {
                        RangerTagDef tagDef = entry.getValue();
                        XXTagDef xTagDef = tagDefDao.getById(tagDef.getId());

                        if (xTagDef != null && StringUtils.isEmpty(xTagDef.getTagAttrDefs())) {

                            TagsUpdaterThread updaterThread = new TagsUpdaterThread(txTemplate, null, null, tagDef);
                            String errorMsg = runThread(updaterThread);
                            if (StringUtils.isNotEmpty(errorMsg)) {
                                throw new Exception(errorMsg);
                            }
                        }
                    }
                }

                if (MapUtils.isNotEmpty(tags)) {
                    logger.info("==> Port " + tags.size() + " Tags for service(name=" + dbService.getName() + ")");

                    for (Map.Entry<Long, RangerTag> entry : tags.entrySet()) {
                        RangerTag tag = entry.getValue();
                        XXTag xTag = tagDao.getById(tag.getId());

                        if (xTag != null && StringUtils.isEmpty(xTag.getTagAttrs())) {
                            TagsUpdaterThread updaterThread = new TagsUpdaterThread(txTemplate, null, tag, null);
                            String errorMsg = runThread(updaterThread);
                            if (StringUtils.isNotEmpty(errorMsg)) {
                                throw new Exception(errorMsg);
                            }
                        }
                    }
                }

                if (CollectionUtils.isNotEmpty(serviceResources)) {
                    logger.info("==> Port " + serviceResources.size() + " Service Resources for service(name=" + dbService.getName() + ")");

                    for (RangerServiceResource serviceResource : serviceResources) {

                        XXServiceResource xServiceResource = serviceResourceDao.getById(serviceResource.getId());

                        if (xServiceResource != null && StringUtils.isEmpty(xServiceResource.getServiceResourceElements())) {
                            TagsUpdaterThread updaterThread = new TagsUpdaterThread(txTemplate, serviceResource, null, null);
                            String errorMsg = runThread(updaterThread);
                            if (StringUtils.isNotEmpty(errorMsg)) {
                                throw new Exception(errorMsg);
                            }
                        }
                    }
                }
            }
        }

        logger.info("<== updateRangerTagsTablesWithTagsJson() ");
    }

    private String runThread(TagsUpdaterThread updaterThread) throws Exception {
        updaterThread.setDaemon(true);
        updaterThread.start();
        updaterThread.join();
        return updaterThread.getErrorMsg();
    }

    private class TagsUpdaterThread extends Thread {
        final TransactionTemplate   txTemplate;
        final RangerServiceResource serviceResource;
        final RangerTag             tag;
        final RangerTagDef          tagDef;
        String                      errorMsg;

        TagsUpdaterThread(TransactionTemplate txTemplate, final RangerServiceResource serviceResource, final RangerTag tag, final RangerTagDef tagDef) {
            this.txTemplate        = txTemplate;
            this.serviceResource   = serviceResource;
            this.tag               = tag;
            this.tagDef            = tagDef;
            this.errorMsg          = null;
        }

        public String getErrorMsg() {
            return errorMsg;
        }

        @Override
        public void run() {
            errorMsg = txTemplate.execute(new TransactionCallback<String>() {
                @Override
                public String doInTransaction(TransactionStatus status) {
                    String ret = null;
                    try {
                        if (serviceResource != null) {
                            portServiceResource(serviceResource);
                        }
                        if (tag != null) {
                            portTag(tag);
                        }
                        if (tagDef != null) {
                            portTagDef(tagDef);
                        }
                    } catch (Throwable e) {
                        logger.error("Port failed :[serviceResource=" + serviceResource + ", tag=" + tag + ", tagDef=" + tagDef +"]", e);
                        ret = e.toString();
                    }
                    return ret;
                }
            });
        }
    }
    private void portTagDef(RangerTagDef tagDef) {
        tagDefService.update(tagDef);
    }

    private void portTag(RangerTag tag) {
        tagService.update(tag);
    }

    private void portServiceResource(RangerServiceResource serviceResource) throws Exception {
        serviceResourceService.update(serviceResource);
        tagStore.refreshServiceResource(serviceResource.getId());
    }

    private class RangerTagDBRetriever {
        final Logger LOG = LoggerFactory.getLogger(RangerTagDBRetriever.class);

        private final RangerDaoManager                 daoMgr;
        private final XXService                        xService;
        private final RangerTagDBRetriever.LookupCache lookupCache;
        private final PlatformTransactionManager       txManager;
        private final TransactionTemplate              txTemplate;
        private       List<RangerServiceResource>      serviceResources;
        private       Map<Long, RangerTagDef>          tagDefs;
        private       Map<Long, RangerTag>             tags;

        RangerTagDBRetriever(final RangerDaoManager daoMgr, final PlatformTransactionManager txManager, final XXService xService) throws InterruptedException {
            this.daoMgr      = daoMgr;
            this.xService    = xService;
            this.lookupCache = new RangerTagDBRetriever.LookupCache();
            this.txManager   = txManager;

            if (this.txManager != null) {
                this.txTemplate = new TransactionTemplate(this.txManager);
                this.txTemplate.setReadOnly(true);
            } else {
                this.txTemplate = null;
            }

            if (this.daoMgr != null && this.xService != null) {
                if (this.txTemplate == null) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Load Tags in the same thread and using an existing transaction");
                    }

                    if (!initializeTagCache(xService)) {
                        LOG.error("Failed to get tags for service:[" + xService.getName() + "] in the same thread and using an existing transaction");
                    }
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Load Tags in a separate thread and using a new transaction");
                    }

                    RangerTagDBRetriever.TagLoaderThread t = new RangerTagDBRetriever.TagLoaderThread(txTemplate, xService);
                    t.setDaemon(true);
                    t.start();
                    t.join();
                }

            }
        }

        List<RangerServiceResource> getServiceResources() {
            return serviceResources;
        }

        Map<Long, RangerTagDef> getTagDefs() {
            return tagDefs;
        }

        Map<Long, RangerTag> getTags() {
            return tags;
        }

        private boolean initializeTagCache(XXService xService) {
            boolean ret;
            RangerTagDBRetriever.TagRetrieverServiceResourceContext serviceResourceContext  = new RangerTagDBRetriever.TagRetrieverServiceResourceContext(xService);
            RangerTagDBRetriever.TagRetrieverTagDefContext          tagDefContext           = new RangerTagDBRetriever.TagRetrieverTagDefContext(xService);
            RangerTagDBRetriever.TagRetrieverTagContext             tagContext              = new RangerTagDBRetriever.TagRetrieverTagContext(xService);

            serviceResources = serviceResourceContext.getAllServiceResources();
            tagDefs          = tagDefContext.getAllTagDefs();
            tags             = tagContext.getAllTags();

            ret = true;
            return ret;
        }

        private <T> List<T> asList(T obj) {
            List<T> ret = new ArrayList<>();

            if (obj != null) {
                ret.add(obj);
            }

            return ret;
        }

        private class LookupCache {
            final Map<Long, String> userScreenNames = new HashMap<>();
            final Map<Long, String> resourceDefs    = new HashMap<>();

            String getUserScreenName(Long userId) {
                String ret = null;

                if (userId != null) {
                    ret = userScreenNames.get(userId);

                    if (ret == null) {
                        XXPortalUser user = daoMgr.getXXPortalUser().getById(userId);

                        if (user != null) {
                            ret = user.getPublicScreenName();

                            if (StringUtil.isEmpty(ret)) {
                                ret = user.getFirstName();

                                if (StringUtil.isEmpty(ret)) {
                                    ret = user.getLoginId();
                                } else {
                                    if (!StringUtil.isEmpty(user.getLastName())) {
                                        ret += (" " + user.getLastName());
                                    }
                                }
                            }

                            if (ret != null) {
                                userScreenNames.put(userId, ret);
                            }
                        }
                    }
                }

                return ret;
            }

            String getResourceName(Long resourceDefId) {
                String ret = null;

                if (resourceDefId != null) {
                    ret = resourceDefs.get(resourceDefId);

                    if (ret == null) {
                        XXResourceDef xResourceDef = daoMgr.getXXResourceDef().getById(resourceDefId);

                        if (xResourceDef != null) {
                            ret = xResourceDef.getName();

                            resourceDefs.put(resourceDefId, ret);
                        }
                    }
                }

                return ret;
            }
        }

        private class TagLoaderThread extends Thread {
            final TransactionTemplate txTemplate;
            final XXService           xService;

            TagLoaderThread(TransactionTemplate txTemplate, final XXService xService) {
                this.txTemplate = txTemplate;
                this.xService   = xService;
            }

            @Override
            public void run() {
                txTemplate.setReadOnly(true);
                Boolean result = txTemplate.execute(new TransactionCallback<Boolean>() {
                    @Override
                    public Boolean doInTransaction(TransactionStatus status) {
                        boolean ret = initializeTagCache(xService);

                        if (!ret) {
                            status.setRollbackOnly();
                            LOG.error("Failed to get tags for service:[" + xService.getName() + "] in a new transaction");
                        }
                        return ret;
                    }
                });

                if (LOG.isDebugEnabled()) {
                    LOG.debug("transaction result:[" + result +"]");
                }
            }
        }

        private class TagRetrieverServiceResourceContext {
            final XXService                                   service;
            final ListIterator<XXServiceResource>             iterServiceResource;
            final ListIterator<XXServiceResourceElement>      iterServiceResourceElement;
            final ListIterator<XXServiceResourceElementValue> iterServiceResourceElementValue;

            TagRetrieverServiceResourceContext(XXService xService) {
                Long                                serviceId                     = xService == null ? null : xService.getId();
                List<XXServiceResource>             xServiceResources             = daoMgr.getXXServiceResource().findByServiceId(serviceId);
                List<XXServiceResourceElement>      xServiceResourceElements      = daoMgr.getXXServiceResourceElement().findTaggedResourcesInServiceId(serviceId);
                List<XXServiceResourceElementValue> xServiceResourceElementValues = daoMgr.getXXServiceResourceElementValue().findTaggedResourcesInServiceId(serviceId);

                this.service                         = xService;
                this.iterServiceResource             = xServiceResources.listIterator();
                this.iterServiceResourceElement      = xServiceResourceElements.listIterator();
                this.iterServiceResourceElementValue = xServiceResourceElementValues.listIterator();

            }

            TagRetrieverServiceResourceContext(XXServiceResource xServiceResource, XXService xService) {
                Long                                resourceId                    = xServiceResource == null ? null : xServiceResource.getId();
                List<XXServiceResource>             xServiceResources             = asList(xServiceResource);
                List<XXServiceResourceElement>      xServiceResourceElements      = daoMgr.getXXServiceResourceElement().findByResourceId(resourceId);
                List<XXServiceResourceElementValue> xServiceResourceElementValues = daoMgr.getXXServiceResourceElementValue().findByResourceId(resourceId);

                this.service                         = xService;
                this.iterServiceResource             = xServiceResources.listIterator();
                this.iterServiceResourceElement      = xServiceResourceElements.listIterator();
                this.iterServiceResourceElementValue = xServiceResourceElementValues.listIterator();
            }

            List<RangerServiceResource> getAllServiceResources() {
                List<RangerServiceResource> ret = new ArrayList<>();

                while (iterServiceResource.hasNext()) {
                    RangerServiceResource serviceResource = getNextServiceResource();

                    if (serviceResource != null) {
                        ret.add(serviceResource);
                    }
                }

                if (!hasProcessedAll()) {
                    LOG.warn("getAllServiceResources(): perhaps one or more serviceResources got updated during retrieval. Using fallback ... ");

                    ret = getServiceResourcesBySecondary();
                }

                return ret;
            }

            RangerServiceResource getNextServiceResource() {
                RangerServiceResource ret = null;

                if (iterServiceResource.hasNext()) {
                    XXServiceResource xServiceResource = iterServiceResource.next();

                    if (xServiceResource != null) {
                        ret = new RangerServiceResource();

                        ret.setId(xServiceResource.getId());
                        ret.setGuid(xServiceResource.getGuid());
                        ret.setIsEnabled(xServiceResource.getIsEnabled());
                        ret.setCreatedBy(lookupCache.getUserScreenName(xServiceResource.getAddedByUserId()));
                        ret.setUpdatedBy(lookupCache.getUserScreenName(xServiceResource.getUpdatedByUserId()));
                        ret.setCreateTime(xServiceResource.getCreateTime());
                        ret.setUpdateTime(xServiceResource.getUpdateTime());
                        ret.setVersion(xServiceResource.getVersion());
                        ret.setResourceSignature(xServiceResource.getResourceSignature());
                        ret.setServiceName(xService.getName());

                        getServiceResourceElements(ret);
                    }
                }

                return ret;
            }

            void getServiceResourceElements(RangerServiceResource serviceResource) {
                while (iterServiceResourceElement.hasNext()) {
                    XXServiceResourceElement xServiceResourceElement = iterServiceResourceElement.next();

                    if (xServiceResourceElement.getResourceId().equals(serviceResource.getId())) {
                        RangerPolicy.RangerPolicyResource resource = new RangerPolicy.RangerPolicyResource();

                        resource.setIsExcludes(xServiceResourceElement.getIsExcludes());
                        resource.setIsRecursive(xServiceResourceElement.getIsRecursive());

                        while (iterServiceResourceElementValue.hasNext()) {
                            XXServiceResourceElementValue xServiceResourceElementValue = iterServiceResourceElementValue.next();

                            if (xServiceResourceElementValue.getResElementId().equals(xServiceResourceElement.getId())) {
                                resource.getValues().add(xServiceResourceElementValue.getValue());
                            } else {
                                if (iterServiceResourceElementValue.hasPrevious()) {
                                    iterServiceResourceElementValue.previous();
                                }

                                break;
                            }
                        }

                        serviceResource.getResourceElements().put(lookupCache.getResourceName(xServiceResourceElement.getResDefId()), resource);
                    } else if (xServiceResourceElement.getResourceId().compareTo(serviceResource.getId()) > 0) {
                        if (iterServiceResourceElement.hasPrevious()) {
                            iterServiceResourceElement.previous();
                        }

                        break;
                    }
                }
            }

            boolean hasProcessedAll() {
                boolean moreToProcess = iterServiceResource.hasNext()
                        || iterServiceResourceElement.hasNext()
                        || iterServiceResourceElementValue.hasNext();

                return !moreToProcess;
            }

            List<RangerServiceResource> getServiceResourcesBySecondary() {
                List<RangerServiceResource> ret = null;

                if (service != null) {
                    List<XXServiceResource> xServiceResources = daoMgr.getXXServiceResource().findTaggedResourcesInServiceId(service.getId());

                    if (CollectionUtils.isNotEmpty(xServiceResources)) {
                        ret = new ArrayList<>(xServiceResources.size());

                        for (XXServiceResource xServiceResource : xServiceResources) {
                            RangerTagDBRetriever.TagRetrieverServiceResourceContext ctx = new RangerTagDBRetriever.TagRetrieverServiceResourceContext(xServiceResource, service);

                            RangerServiceResource serviceResource = ctx.getNextServiceResource();

                            if (serviceResource != null) {
                                ret.add(serviceResource);
                            }
                        }
                    }
                }
                return ret;
            }
        }

        private class TagRetrieverTagDefContext {
            final XXService                       service;
            final ListIterator<XXTagDef>          iterTagDef;
            final ListIterator<XXTagAttributeDef> iterTagAttributeDef;


            TagRetrieverTagDefContext(XXService xService) {
                Long                    serviceId         = xService == null ? null : xService.getId();
                List<XXTagDef>          xTagDefs          = daoMgr.getXXTagDef().findByServiceId(serviceId);
                List<XXTagAttributeDef> xTagAttributeDefs = daoMgr.getXXTagAttributeDef().findByServiceId(serviceId);

                this.service             = xService;
                this.iterTagDef          = xTagDefs.listIterator();
                this.iterTagAttributeDef = xTagAttributeDefs.listIterator();
            }

            TagRetrieverTagDefContext(XXTagDef xTagDef, XXService xService) {
                Long                    tagDefId          = xTagDef == null ? null : xTagDef.getId();
                List<XXTagDef>          xTagDefs          = asList(xTagDef);
                List<XXTagAttributeDef> xTagAttributeDefs = daoMgr.getXXTagAttributeDef().findByTagDefId(tagDefId);

                this.service             = xService;
                this.iterTagDef          = xTagDefs.listIterator();
                this.iterTagAttributeDef = xTagAttributeDefs.listIterator();
            }

            Map<Long, RangerTagDef> getAllTagDefs() {
                Map<Long, RangerTagDef> ret = new HashMap<>();

                while (iterTagDef.hasNext()) {
                    RangerTagDef tagDef = getNextTagDef();

                    if (tagDef != null) {
                        ret.put(tagDef.getId(), tagDef);
                    }
                }

                if (!hasProcessedAllTagDefs()) {
                    LOG.warn("getAllTagDefs(): perhaps one or more tag-definitions got updated during retrieval.  Using fallback ... ");

                    ret = getTagDefsBySecondary();
                }

                return ret;
            }

            RangerTagDef getNextTagDef() {
                RangerTagDef ret = null;

                if (iterTagDef.hasNext()) {
                    XXTagDef xTagDef = iterTagDef.next();

                    if (xTagDef != null) {
                        ret = new RangerTagDef();

                        ret.setId(xTagDef.getId());
                        ret.setGuid(xTagDef.getGuid());
                        ret.setIsEnabled(xTagDef.getIsEnabled());
                        ret.setCreatedBy(lookupCache.getUserScreenName(xTagDef.getAddedByUserId()));
                        ret.setUpdatedBy(lookupCache.getUserScreenName(xTagDef.getUpdatedByUserId()));
                        ret.setCreateTime(xTagDef.getCreateTime());
                        ret.setUpdateTime(xTagDef.getUpdateTime());
                        ret.setVersion(xTagDef.getVersion());
                        ret.setName(xTagDef.getName());
                        ret.setSource(xTagDef.getSource());

                        getTagAttributeDefs(ret);
                    }
                }

                return ret;
            }

            void getTagAttributeDefs(RangerTagDef tagDef) {
                while (iterTagAttributeDef.hasNext()) {
                    XXTagAttributeDef xTagAttributeDef = iterTagAttributeDef.next();

                    if (xTagAttributeDef.getTagDefId().equals(tagDef.getId())) {
                        RangerTagDef.RangerTagAttributeDef tagAttributeDef = new RangerTagDef.RangerTagAttributeDef();

                        tagAttributeDef.setName(xTagAttributeDef.getName());
                        tagAttributeDef.setType(xTagAttributeDef.getType());

                        tagDef.getAttributeDefs().add(tagAttributeDef);
                    } else if (xTagAttributeDef.getTagDefId().compareTo(tagDef.getId()) > 0) {
                        if (iterTagAttributeDef.hasPrevious()) {
                            iterTagAttributeDef.previous();
                        }
                        break;
                    }
                }
            }

            boolean hasProcessedAllTagDefs() {
                boolean moreToProcess = iterTagAttributeDef.hasNext();

                return !moreToProcess;
            }

            Map<Long, RangerTagDef> getTagDefsBySecondary() {
                Map<Long, RangerTagDef> ret = null;

                if (service != null) {
                    List<XXTagDef> xTagDefs = daoMgr.getXXTagDef().findByServiceId(service.getId());

                    if (CollectionUtils.isNotEmpty(xTagDefs)) {
                        ret = new HashMap<>(xTagDefs.size());

                        for (XXTagDef xTagDef : xTagDefs) {
                            TagRetrieverTagDefContext ctx = new TagRetrieverTagDefContext(xTagDef, service);

                            RangerTagDef tagDef = ctx.getNextTagDef();

                            if (tagDef != null) {
                                ret.put(tagDef.getId(), tagDef);
                            }
                        }
                    }
                }
                return ret;
            }
        }

        private class TagRetrieverTagContext {
            final XXService                    service;
            final ListIterator<XXTag>          iterTag;
            final ListIterator<XXTagAttribute> iterTagAttribute;


            TagRetrieverTagContext(XXService xService) {
                Long                 serviceId      = xService == null ? null : xService.getId();
                List<XXTag>          xTags          = daoMgr.getXXTag().findByServiceId(serviceId);
                List<XXTagAttribute> xTagAttributes = daoMgr.getXXTagAttribute().findByServiceId(serviceId);

                this.service          = xService;
                this.iterTag          = xTags.listIterator();
                this.iterTagAttribute = xTagAttributes.listIterator();

            }

            TagRetrieverTagContext(XXTag xTag, XXService xService) {
                Long                 tagId          = xTag == null ? null : xTag.getId();
                List<XXTag>          xTags          = asList(xTag);
                List<XXTagAttribute> xTagAttributes = daoMgr.getXXTagAttribute().findByTagId(tagId);

                this.service          = xService;
                this.iterTag          = xTags.listIterator();
                this.iterTagAttribute = xTagAttributes.listIterator();
            }


            Map<Long, RangerTag> getAllTags() {
                Map<Long, RangerTag> ret = new HashMap<>();

                while (iterTag.hasNext()) {
                    RangerTag tag = getNextTag();

                    if (tag != null) {
                        ret.put(tag.getId(), tag);
                    }
                }

                if (!hasProcessedAllTags()) {
                    LOG.warn("getAllTags(): perhaps one or more tags got updated during retrieval. Using fallback ... ");

                    ret = getTagsBySecondary();
                }

                return ret;
            }

            RangerTag getNextTag() {
                RangerTag ret = null;

                if (iterTag.hasNext()) {
                    XXTag xTag = iterTag.next();

                    if (xTag != null) {
                        ret = new RangerTag();

                        ret.setId(xTag.getId());
                        ret.setGuid(xTag.getGuid());
                        ret.setOwner(xTag.getOwner());
                        ret.setCreatedBy(lookupCache.getUserScreenName(xTag.getAddedByUserId()));
                        ret.setUpdatedBy(lookupCache.getUserScreenName(xTag.getUpdatedByUserId()));
                        ret.setCreateTime(xTag.getCreateTime());
                        ret.setUpdateTime(xTag.getUpdateTime());
                        ret.setVersion(xTag.getVersion());

                        Map<String, String> mapOfOptions = JsonUtils.jsonToMapStringString(xTag.getOptions());

                        if (MapUtils.isNotEmpty(mapOfOptions)) {
                            String validityPeriodsStr = mapOfOptions.get(RangerTag.OPTION_TAG_VALIDITY_PERIODS);

                            if (StringUtils.isNotEmpty(validityPeriodsStr)) {
                                List<RangerValiditySchedule> validityPeriods = JsonUtils.jsonToRangerValiditySchedule(validityPeriodsStr);

                                ret.setValidityPeriods(validityPeriods);
                            }
                        }

                        Map<Long, RangerTagDef> tagDefs = getTagDefs();
                        if (tagDefs != null) {
                            RangerTagDef tagDef = tagDefs.get(xTag.getType());
                            if (tagDef != null) {
                                ret.setType(tagDef.getName());
                            }
                        }

                        getTagAttributes(ret);
                    }
                }

                return ret;
            }

            void getTagAttributes(RangerTag tag) {
                while (iterTagAttribute.hasNext()) {
                    XXTagAttribute xTagAttribute = iterTagAttribute.next();

                    if (xTagAttribute.getTagId().equals(tag.getId())) {
                        String attributeName = xTagAttribute.getName();
                        String attributeValue = xTagAttribute.getValue();

                        tag.getAttributes().put(attributeName, attributeValue);
                    } else if (xTagAttribute.getTagId().compareTo(tag.getId()) > 0) {
                        if (iterTagAttribute.hasPrevious()) {
                            iterTagAttribute.previous();
                        }
                        break;
                    }
                }
            }

            boolean hasProcessedAllTags() {
                boolean moreToProcess = iterTagAttribute.hasNext();

                return !moreToProcess;
            }

            Map<Long, RangerTag> getTagsBySecondary() {
                Map<Long, RangerTag> ret = null;

                if (service != null) {
                    List<XXTag> xTags = daoMgr.getXXTag().findByServiceId(service.getId());

                    if (CollectionUtils.isNotEmpty(xTags)) {
                        ret = new HashMap<>(xTags.size());

                        for (XXTag xTag : xTags) {
                            TagRetrieverTagContext ctx = new TagRetrieverTagContext(xTag, service);

                            RangerTag tag = ctx.getNextTag();

                            if (tag != null) {
                                ret.put(tag.getId(), tag);
                            }
                        }
                    }
                }
                return ret;
            }
        }
    }
}

