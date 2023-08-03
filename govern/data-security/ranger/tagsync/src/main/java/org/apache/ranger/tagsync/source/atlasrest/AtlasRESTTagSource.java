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

package org.apache.ranger.tagsync.source.atlasrest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.atlas.AtlasClientV2;
import org.apache.atlas.AtlasServiceException;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.SearchFilter;
import org.apache.atlas.model.TimeBoundary;
import org.apache.atlas.model.discovery.AtlasSearchResult;
import org.apache.atlas.model.discovery.SearchParameters;
import org.apache.atlas.model.instance.AtlasClassification;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.model.typedef.AtlasBaseTypeDef;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.type.AtlasBuiltInTypes;
import org.apache.atlas.type.AtlasClassificationType;
import org.apache.atlas.type.AtlasStructType;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import org.apache.hadoop.security.UserGroupInformation;
import org.apache.ranger.plugin.model.RangerValiditySchedule;
import org.apache.ranger.plugin.util.ServiceTags;
import org.apache.ranger.tagsync.model.AbstractTagSource;
import org.apache.ranger.tagsync.model.TagSink;
import org.apache.ranger.tagsync.process.TagSyncConfig;
import org.apache.ranger.tagsync.process.TagSynchronizer;
import org.apache.ranger.tagsync.source.atlas.AtlasNotificationMapper;
import org.apache.ranger.tagsync.source.atlas.AtlasResourceMapperUtil;
import org.apache.ranger.tagsync.source.atlas.EntityNotificationWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;

public class AtlasRESTTagSource extends AbstractTagSource implements Runnable {
	private static final Logger LOG = LoggerFactory.getLogger(AtlasRESTTagSource.class);


    	private static final ThreadLocal<DateFormat> DATE_FORMATTER = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			SimpleDateFormat dateFormat = new SimpleDateFormat(AtlasBaseTypeDef.SERIALIZED_DATE_FORMAT_STR);

			dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

			return dateFormat;
		}
	};

	private long sleepTimeBetweenCycleInMillis;
	private String[] restUrls         = null;
	private boolean  isKerberized     = false;
	private String[] userNamePassword = null;
	private int      entitiesBatchSize = TagSyncConfig.DEFAULT_TAGSYNC_ATLASREST_SOURCE_ENTITIES_BATCH_SIZE;

	private Thread myThread = null;

	public static void main(String[] args) {

		AtlasRESTTagSource atlasRESTTagSource = new AtlasRESTTagSource();

		TagSyncConfig config = TagSyncConfig.getInstance();

		Properties props = config.getProperties();

		TagSynchronizer.printConfigurationProperties(props);

		boolean ret = TagSynchronizer.initializeKerberosIdentity(props);

		if (ret) {

			TagSink tagSink = TagSynchronizer.initializeTagSink(props);

			if (tagSink != null) {

				if (atlasRESTTagSource.initialize(props)) {
					try {
						tagSink.start();
						atlasRESTTagSource.setTagSink(tagSink);
						atlasRESTTagSource.synchUp();
					} catch (Exception exception) {
						LOG.error("ServiceTags upload failed : ", exception);
						System.exit(1);
					}
				} else {
					LOG.error("AtlasRESTTagSource initialization failed, exiting.");
					System.exit(1);
				}

			} else {
				LOG.error("TagSink initialization failed, exiting.");
				System.exit(1);
			}
		} else {
			LOG.error("Error initializing kerberos identity");
			System.exit(1);
		}

	}
	@Override
	public boolean initialize(Properties properties) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> AtlasRESTTagSource.initialize()");
		}

		boolean ret = AtlasResourceMapperUtil.initializeAtlasResourceMappers(properties);

		sleepTimeBetweenCycleInMillis = TagSyncConfig.getTagSourceAtlasDownloadIntervalInMillis(properties);
		isKerberized = TagSyncConfig.getTagsyncKerberosIdentity(properties) != null;
		entitiesBatchSize = TagSyncConfig.getAtlasRestSourceEntitiesBatchSize(properties);

		String restEndpoint       = TagSyncConfig.getAtlasRESTEndpoint(properties);
		String sslConfigFile = TagSyncConfig.getAtlasRESTSslConfigFile(properties);
        this.userNamePassword = new String[] { TagSyncConfig.getAtlasRESTUserName(properties), TagSyncConfig.getAtlasRESTPassword(properties) };

		if (LOG.isDebugEnabled()) {
			LOG.debug("restUrl=" + restEndpoint);
			LOG.debug("sslConfigFile=" + sslConfigFile);
			LOG.debug("userName=" + userNamePassword[0]);
			LOG.debug("kerberized=" + isKerberized);
		}
        if (StringUtils.isNotEmpty(restEndpoint)) {
            this.restUrls = restEndpoint.split(",");

            for (int i = 0; i < restUrls.length; i++) {
                if (!restUrls[i].endsWith("/")) {
                    restUrls[i] += "/";
                }
            }
		} else {
			LOG.info("AtlasEndpoint not specified, Initial download of Atlas-entities cannot be done.");
			ret = false;
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== AtlasRESTTagSource.initialize(), result=" + ret);
		}

		return ret;
	}

	@Override
	public boolean start() {

		myThread = new Thread(this);
		myThread.setDaemon(true);
		myThread.start();

		return true;
	}

	@Override
	public void stop() {
		if (myThread != null && myThread.isAlive()) {
			myThread.interrupt();
		}
	}

	@Override
    public void run() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasRESTTagSource.run()");
        }
        while (true) {
            try {
                synchUp();

                LOG.debug("Sleeping for [" + sleepTimeBetweenCycleInMillis + "] milliSeconds");

                Thread.sleep(sleepTimeBetweenCycleInMillis);

            } catch (InterruptedException exception) {
                LOG.error("Interrupted..: ", exception);
                return;
            } catch (Exception e) {
                LOG.error("Caught exception", e);
                return;
            }
        }
    }

	public void synchUp() throws Exception {

		List<RangerAtlasEntityWithTags> rangerAtlasEntities = getAtlasActiveEntities();

		if (CollectionUtils.isNotEmpty(rangerAtlasEntities)) {
			if (LOG.isDebugEnabled()) {
				for (RangerAtlasEntityWithTags element : rangerAtlasEntities) {
					LOG.debug("", element);
				}
			}
			Map<String, ServiceTags> serviceTagsMap = AtlasNotificationMapper.processAtlasEntities(rangerAtlasEntities);

			if (MapUtils.isNotEmpty(serviceTagsMap)) {
				for (Map.Entry<String, ServiceTags> entry : serviceTagsMap.entrySet()) {
					if (LOG.isDebugEnabled()) {
						Gson gsonBuilder = new GsonBuilder().setDateFormat("yyyyMMdd-HH:mm:ss.SSS-Z")
								.setPrettyPrinting()
								.create();
						String serviceTagsString = gsonBuilder.toJson(entry.getValue());

						LOG.debug("serviceTags=" + serviceTagsString);
					}
					updateSink(entry.getValue());
				}
			}
		}

	}

    private List<RangerAtlasEntityWithTags> getAtlasActiveEntities() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> getAtlasActiveEntities()");
        }
        List<RangerAtlasEntityWithTags> ret         = new ArrayList<>();

        AtlasClientV2                   atlasClient = null;
        try {
            atlasClient = getAtlasClient();
        } catch (IOException exception) {
            LOG.error("Failed to get Atlas client.", exception);
        }

        if (atlasClient != null) {

            SearchParameters searchParams = new SearchParameters();

            searchParams.setExcludeDeletedEntities(true);
            searchParams.setClassification("*");
            //searchParams.setIncludeSubClassifications(true);
            //searchParams.setIncludeSubTypes(true);
            searchParams.setIncludeClassificationAttributes(true);
            searchParams.setLimit(entitiesBatchSize);

            boolean isMoreData;
            int     nextStartIndex = 0;

            do {
                AtlasTypeRegistry                            typeRegistry  = new AtlasTypeRegistry();
                AtlasTypeRegistry.AtlasTransientTypeRegistry tty           = null;
                AtlasSearchResult                            searchResult  = null;
                boolean                                      commitUpdates = false;

                searchParams.setOffset(nextStartIndex);
                isMoreData = false;

                try {
                    searchResult = atlasClient.facetedSearch(searchParams);
                    AtlasTypesDef typesDef = atlasClient.getAllTypeDefs(new SearchFilter());
                    tty = typeRegistry.lockTypeRegistryForUpdate();
                    tty.addTypes(typesDef);
                    commitUpdates = true;
                } catch (AtlasServiceException | AtlasBaseException excp) {
                    LOG.error("failed to download tags from Atlas", excp);
                    ret = null;
                } catch (Exception unexpectedException) {
                    LOG.error("Failed to download tags from Atlas due to unexpected exception", unexpectedException);
                    ret = null;
                } finally {
                    if (tty != null) {
                        typeRegistry.releaseTypeRegistryForUpdate(tty, commitUpdates);
                    }
                }

                if (commitUpdates && searchResult != null) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(AtlasType.toJson(searchResult));
                    }

                    List<AtlasEntityHeader> entityHeaders = searchResult.getEntities();

                    if (CollectionUtils.isNotEmpty(entityHeaders)) {

                        nextStartIndex += entityHeaders.size();
                        isMoreData = true;

                        for (AtlasEntityHeader header : entityHeaders) {
                            if (!header.getStatus().equals(AtlasEntity.Status.ACTIVE)) {
                                if (LOG.isDebugEnabled()) {
                                    LOG.debug("Skipping entity because it is not ACTIVE, header:[" + header + "]");
                                }
                                continue;
                            }

                            String typeName = header.getTypeName();
                            if (!AtlasResourceMapperUtil.isEntityTypeHandled(typeName)) {
                                if (LOG.isDebugEnabled()) {
                                    LOG.debug("Not fetching Atlas entities of type:[" + typeName + "]");
                                }
                                continue;
                            }

                            List<EntityNotificationWrapper.RangerAtlasClassification> allTagsForEntity = new ArrayList<>();

                            for (AtlasClassification classification : header.getClassifications()) {
                                List<EntityNotificationWrapper.RangerAtlasClassification> tags = resolveTag(typeRegistry, classification);
                                if (tags != null) {
                                    allTagsForEntity.addAll(tags);
                                }
                            }

                            if (CollectionUtils.isNotEmpty(allTagsForEntity)) {
                                RangerAtlasEntity entity = new RangerAtlasEntity(typeName, header.getGuid(), header.getAttributes());
                                RangerAtlasEntityWithTags entityWithTags = new RangerAtlasEntityWithTags(entity, allTagsForEntity, typeRegistry);

                                ret.add(entityWithTags);
                            }
                        }
                    }
                }
            } while (isMoreData);

        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== getAtlasActiveEntities()");
        }

        return ret;
    }

    /*
     * Returns a list of <EntityNotificationWrapper.RangerAtlasClassification>
     */
    private List<EntityNotificationWrapper.RangerAtlasClassification> resolveTag(AtlasTypeRegistry typeRegistry, AtlasClassification classification) {
        List<EntityNotificationWrapper.RangerAtlasClassification>   ret         = new ArrayList<>();
        String                                                      typeName    = classification.getTypeName();
        Map<String, Object>                                         attributes  = classification.getAttributes();

        try {
            AtlasClassificationType classificationType = typeRegistry.getClassificationTypeByName(typeName);

            if (classificationType != null) {

                Map<String, String> allAttributes = new HashMap<>();

                if (MapUtils.isNotEmpty(attributes) && MapUtils.isNotEmpty(classificationType.getAllAttributes())) {
                    for (Map.Entry<String, Object> attribute : attributes.entrySet()) {

                        String name     = attribute.getKey();
                        Object value    = attribute.getValue();

                        if (value != null) {

                            String stringValue                              = value.toString();
                            AtlasStructType.AtlasAttribute atlasAttribute   = classificationType.getAttribute(name);

                            if (atlasAttribute != null) {
                                if (value instanceof Number) {
                                    if (atlasAttribute.getAttributeType() instanceof AtlasBuiltInTypes.AtlasDateType) {
                                        stringValue = DATE_FORMATTER.get().format(value);
                                    }
                                }
                                allAttributes.put(name, stringValue);
                            }
                        }
                    }
                }
                List<TimeBoundary> validityPeriods              = classification.getValidityPeriods();
                List<RangerValiditySchedule> validitySchedules  = null;

                if (CollectionUtils.isNotEmpty(validityPeriods)) {
                    validitySchedules = EntityNotificationWrapper.convertTimeSpecFromAtlasToRanger(validityPeriods);
                }

                // Add most derived classificationType with all attributes
                ret.add(new EntityNotificationWrapper.RangerAtlasClassification(typeName, allAttributes, validitySchedules));

                // Find base classification types
                Set<String> superTypeNames = classificationType.getAllSuperTypes();
                for (String superTypeName : superTypeNames) {

                    AtlasClassificationType superType = typeRegistry.getClassificationTypeByName(superTypeName);

                    if (superType != null) {

                        Map<String, String> attributeMap = new HashMap<>();

                        if (MapUtils.isNotEmpty(attributes) && MapUtils.isNotEmpty(superType.getAllAttributes())) {
                            for (String name : superType.getAllAttributes().keySet()) {

                                String stringValue = allAttributes.get(name);

                                if (stringValue != null) {
                                    attributeMap.put(name, stringValue);
                                }
                            }
                        }
                        validityPeriods     = classification.getValidityPeriods();
                        validitySchedules   = null;

                        if (CollectionUtils.isNotEmpty(validityPeriods)) {
                            validitySchedules = EntityNotificationWrapper.convertTimeSpecFromAtlasToRanger(validityPeriods);
                        }
                        ret.add(new EntityNotificationWrapper.RangerAtlasClassification(superTypeName, attributeMap, validitySchedules));
                    }
                }
            }
        } catch (Exception exception) {
            LOG.error("Error in resolving tags for type:[" + typeName + "]", exception);
        }
        return ret;
    }

	private AtlasClientV2 getAtlasClient() throws IOException {
		final AtlasClientV2 ret;

		if (isKerberized) {
			UserGroupInformation ugi = UserGroupInformation.getLoginUser();

			ugi.checkTGTAndReloginFromKeytab();

			ret = new AtlasClientV2(ugi, ugi.getShortUserName(), restUrls);
		} else {
			ret = new AtlasClientV2(restUrls, userNamePassword);
		}

		return ret;
	}
}

