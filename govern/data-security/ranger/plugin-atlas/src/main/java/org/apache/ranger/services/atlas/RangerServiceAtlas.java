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
package org.apache.ranger.services.atlas;

import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.atlas.model.discovery.AtlasSearchResult;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.client.BaseClient;
import org.apache.ranger.plugin.client.HadoopException;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemAccess;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyResource;
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.policyengine.RangerPolicyEngine;
import org.apache.ranger.plugin.service.RangerBaseService;
import org.apache.ranger.plugin.service.ResourceLookupContext;
import org.apache.ranger.plugin.util.PasswordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.security.auth.Subject;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;

public class RangerServiceAtlas extends RangerBaseService {
	private static final Logger LOG = LoggerFactory.getLogger(RangerServiceAtlas.class);

	public static final String RESOURCE_SERVICE                       = "atlas-service";
	public static final String RESOURCE_TYPE_CATEGORY                 = "type-category";
	public static final String RESOURCE_TYPE_NAME                     = "type";
	public static final String RESOURCE_ENTITY_TYPE                   = "entity-type";
	public static final String RESOURCE_ENTITY_CLASSIFICATION         = "entity-classification";
	public static final String RESOURCE_CLASSIFICATION                = "classification";
	public static final String RESOURCE_ENTITY_ID                     = "entity";
	public static final String RESOURCE_ENTITY_LABEL                  = "entity-label";
	public static final String RESOURCE_ENTITY_BUSINESS_METADATA      = "entity-business-metadata";
	public static final String RESOURCE_ENTITY_OWNER                  = "owner";
	public static final String RESOURCE_RELATIONSHIP_TYPE             = "relationship-type";
	public static final String RESOURCE_END_ONE_ENTITY_TYPE           = "end-one-entity-type";
	public static final String RESOURCE_END_ONE_ENTITY_CLASSIFICATION = "end-one-entity-classification";
	public static final String RESOURCE_END_ONE_ENTITY_ID             = "end-one-entity";
	public static final String RESOURCE_END_TWO_ENTITY_TYPE           =  "end-two-entity-type";
	public static final String RESOURCE_END_TWO_ENTITY_CLASSIFICATION = "end-two-entity-classification";
	public static final String RESOURCE_END_TWO_ENTITY_ID             = "end-two-entity";
	public static final String SEARCH_FEATURE_POLICY_NAME             = "Allow users to manage favorite searches";

	public static final String ACCESS_TYPE_ENTITY_READ  = "entity-read";
	public static final String ACCESS_TYPE_TYPE_READ = "type-read";
	public static final String ACCESS_TYPE_ENTITY_CREATE  = "entity-create";
	public static final String ACCESS_TYPE_ENTITY_UPDATE = "entity-update";
	public static final String ACCESS_TYPE_ENTITY_DELETE = "entity-delete";
	public static final String ADMIN_USERNAME_DEFAULT   = "admin";
	public static final String TAGSYNC_USERNAME_DEFAULT = "rangertagsync";
	public static final String ENTITY_TYPE_USER_PROFILE = "__AtlasUserProfile";
	public static final String ENTITY_TYPE_SAVED_SEARCH = "__AtlasUserSavedSearch";
	public static final String ENTITY_ID_USER_PROFILE = RangerPolicyEngine.USER_CURRENT;
	public static final String ENTITY_ID_USER_SAVED_SEARCH= RangerPolicyEngine.USER_CURRENT + ":*";


	public static final String CONFIG_REST_ADDRESS            = "atlas.rest.address";
	public static final String CONFIG_USERNAME                = "username";
	public static final String CONFIG_PASSWORD                = "password";
	public static final String ENTITY_NOT_CLASSIFIED          = "_NOT_CLASSIFIED";

	private static final String TYPE_ENTITY             = "entity";
	private static final String TYPE_CLASSIFICATION     = "classification";
	private static final String TYPE_STRUCT             = "struct";
	private static final String TYPE_ENUM               = "enum";
	private static final String TYPE_RELATIONSHIP       = "relationship";
	private static final String TYPE_BUSINESS_METADATA  = "business_metadata";

	private static final String URL_LOGIN                = "/j_spring_security_check";
	private static final String URL_GET_TYPESDEF_HEADERS = "/api/atlas/v2/types/typedefs/headers";
	private static final String URl_ENTITY_SEARCH        = "v2/search/attribute?attrName=qualifiedName";

	private static final String WEB_RESOURCE_CONTENT_TYPE = "application/x-www-form-urlencoded";
	private static final String CONNECTION_ERROR_MSG      =   " You can still save the repository and start creating"
	                                                        + " policies, but you would not be able to use autocomplete for"
	                                                        + " resource names. Check ranger_admin.log for more info.";

	public RangerServiceAtlas() {
		super();
	}

	@Override
	public void init(RangerServiceDef serviceDef, RangerService service) {
		super.init(serviceDef, service);
	}

	@Override
	public Map<String, Object> validateConfig() throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerServiceAtlas.validateConfig()");
		}

		AtlasServiceClient  client = new AtlasServiceClient(getServiceName(), configs);
		Map<String, Object> ret    = client.validateConfig();

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerServiceAtlas.validateConfig(): " + ret );
		}

		return ret;
	}

	@Override
	public List<String> lookupResource(ResourceLookupContext context)throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerServiceAtlas.lookupResource(" + context + ")");
		}

		AtlasServiceClient client = new AtlasServiceClient(getServiceName(), configs);
		List<String>       ret    = client.lookupResource(context);

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerServiceAtlas.lookupResource("+ context + "): " + ret);
		}

		return ret;
	}

    @Override
    public List<RangerPolicy> getDefaultRangerPolicies() throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> RangerServiceAtlas.getDefaultRangerPolicies()");
        }

        List<RangerPolicy> ret                         = super.getDefaultRangerPolicies();
        String             adminUser                   = getStringConfig("atlas.admin.user", ADMIN_USERNAME_DEFAULT);
        String             tagSyncUser                 = getStringConfig("atlas.rangertagsync.user", TAGSYNC_USERNAME_DEFAULT);

        boolean            relationshipTypeAllowPublic = getBooleanConfig("atlas.default-policy.relationship-type.allow.public", true);


        for (RangerPolicy defaultPolicy : ret) {
            final Map<String, RangerPolicyResource> policyResources = defaultPolicy.getResources();

            // 1. add adminUser to every policyItem
            for (RangerPolicyItem defaultPolicyItem : defaultPolicy.getPolicyItems()) {
                defaultPolicyItem.getUsers().add(adminUser);
            }

            // 2. add a policy-item for rangertagsync user with 'entity-read' permission in the policy for 'entity-type'
            if (policyResources.containsKey(RESOURCE_ENTITY_TYPE) && !policyResources.containsKey(RESOURCE_CLASSIFICATION)) {
                RangerPolicyItem policyItemForTagSyncUser = new RangerPolicyItem();

                policyItemForTagSyncUser.setUsers(Collections.singletonList(tagSyncUser));
                policyItemForTagSyncUser.setGroups(Collections.singletonList(RangerPolicyEngine.GROUP_PUBLIC));
                policyItemForTagSyncUser.setAccesses(Collections.singletonList(new RangerPolicyItemAccess(ACCESS_TYPE_ENTITY_READ)));

                defaultPolicy.getPolicyItems().add(policyItemForTagSyncUser);
            }

            if (relationshipTypeAllowPublic) {
                // 3. add 'public' group in the policy for 'relationship-type',
                if (policyResources.containsKey(RangerServiceAtlas.RESOURCE_RELATIONSHIP_TYPE)) {
                    for (RangerPolicyItem defaultPolicyItem : defaultPolicy.getPolicyItems()) {
                        defaultPolicyItem.getGroups().add(RangerPolicyEngine.GROUP_PUBLIC);
                    }
                }
            }

			if (defaultPolicy.getName().contains("all")
					&& policyResources.containsKey(RangerServiceAtlas.RESOURCE_ENTITY_TYPE)
					&& StringUtils.isNotBlank(lookUpUser) && !policyResources.containsKey(RESOURCE_CLASSIFICATION)) {
				RangerPolicyItem policyItemForLookupUser = new RangerPolicyItem();
				policyItemForLookupUser.setUsers(Collections.singletonList(lookUpUser));
				policyItemForLookupUser.setAccesses(Collections.singletonList(new RangerPolicyItemAccess(ACCESS_TYPE_ENTITY_READ)));
				policyItemForLookupUser.setDelegateAdmin(false);
				defaultPolicy.getPolicyItems().add(policyItemForLookupUser);
			}

			//  add a policy-item for rangertagsync user with 'type-read' permission in the policy for 'type-category'
			if (policyResources.containsKey(RangerServiceAtlas.RESOURCE_TYPE_CATEGORY)) {
				RangerPolicyItem policyItemTypeReadForAll = new RangerPolicyItem();
				policyItemTypeReadForAll.setGroups(Collections.singletonList(RangerPolicyEngine.GROUP_PUBLIC));
				policyItemTypeReadForAll.setAccesses(Collections.singletonList(new RangerPolicyItemAccess(ACCESS_TYPE_TYPE_READ)));
				defaultPolicy.getPolicyItems().add(policyItemTypeReadForAll);
			}
        }

        //4.add new policy for public group with entity-read, entity-create, entity-update, entity-delete for  __AtlasUserProfile, __AtlasUserSavedSearch entity type
        RangerPolicy searchFeaturePolicy = getSearchFeaturePolicy();
        ret.add(searchFeaturePolicy);
        if (LOG.isDebugEnabled()) {
            LOG.debug("<== RangerServiceAtlas.getDefaultRangerPolicies()");
        }

        return ret;
    }

	private RangerPolicy getSearchFeaturePolicy() {
		RangerPolicy searchFeaturePolicy = new RangerPolicy();

		searchFeaturePolicy.setName(SEARCH_FEATURE_POLICY_NAME);
		searchFeaturePolicy.setService(serviceName);
		searchFeaturePolicy.setResources(getSearchFeaturePolicyResource());
		searchFeaturePolicy.setPolicyItems(getSearchFeaturePolicyItem());

		return searchFeaturePolicy;
	}

	private List<RangerPolicyItem> getSearchFeaturePolicyItem() {
		List<RangerPolicyItemAccess> accesses = new ArrayList<RangerPolicyItemAccess>();

		accesses.add(new RangerPolicyItemAccess(ACCESS_TYPE_ENTITY_READ));
		accesses.add(new RangerPolicyItemAccess(ACCESS_TYPE_ENTITY_CREATE));
		accesses.add(new RangerPolicyItemAccess(ACCESS_TYPE_ENTITY_UPDATE));
		accesses.add(new RangerPolicyItemAccess(ACCESS_TYPE_ENTITY_DELETE));

		RangerPolicyItem item = new RangerPolicyItem(accesses, Arrays.asList(RangerPolicyEngine.USER_CURRENT), null, null, null, false);

		return Collections.singletonList(item);
	}

	private Map<String, RangerPolicyResource> getSearchFeaturePolicyResource() {
		Map<String, RangerPolicyResource> resources = new HashMap<>();

		resources.put(RESOURCE_ENTITY_TYPE, new RangerPolicyResource(Arrays.asList(ENTITY_TYPE_USER_PROFILE, ENTITY_TYPE_SAVED_SEARCH), false, false));
		resources.put(RESOURCE_ENTITY_CLASSIFICATION, new RangerPolicyResource("*"));
		resources.put(RESOURCE_ENTITY_ID, new RangerPolicyResource(Arrays.asList(ENTITY_ID_USER_PROFILE, ENTITY_ID_USER_SAVED_SEARCH), false, false));

		return resources;
	}

	private static class AtlasServiceClient extends BaseClient {
		private static final String[] TYPE_CATEGORIES = new String[] { "classification", "enum", "entity", "relationship", "struct" ,"business_metadata" };

		Map<String, List<String>> typesDef = new HashMap<>();

		public AtlasServiceClient(String serviceName, Map<String, String> serviceConfig) {
			super(serviceName, serviceConfig);
		}

		public Map<String, Object> validateConfig() {
			Map<String, Object> ret = new HashMap<>();

			loginToAtlas(Client.create());

			BaseClient.generateResponseDataMap(true, "ConnectionTest Successful", "ConnectionTest Successful", null, null, ret);

			return ret;
		}

		public List<String> lookupResource(ResourceLookupContext lookupContext) {
			final List<String> ret           = new ArrayList<>();
			final String       userInput     = lookupContext.getUserInput();
			final List<String> currentValues = lookupContext.getResources().get(lookupContext.getResourceName());

			switch(lookupContext.getResourceName()) {
				case RESOURCE_TYPE_CATEGORY: {
					for (String typeCategory : TYPE_CATEGORIES) {
						addIfStartsWithAndNotExcluded(ret, typeCategory, userInput, currentValues);
					}
				}
				break;

				case RESOURCE_TYPE_NAME: {
					refreshTypesDefs();

					final List<String> typeCategories = lookupContext.getResources().get(RESOURCE_TYPE_CATEGORY);

					if (emptyOrContainsMatch(typeCategories, TYPE_CLASSIFICATION)) {
						addIfStartsWithAndNotExcluded(ret, typesDef.get(TYPE_CLASSIFICATION), userInput, currentValues);
					}

					if (emptyOrContainsMatch(typeCategories, TYPE_ENTITY)) {
						addIfStartsWithAndNotExcluded(ret, typesDef.get(TYPE_ENTITY), userInput, currentValues);
					}

					if (emptyOrContainsMatch(typeCategories, TYPE_ENUM)) {
						addIfStartsWithAndNotExcluded(ret, typesDef.get(TYPE_ENUM), userInput, currentValues);
					}

					if (emptyOrContainsMatch(typeCategories, TYPE_STRUCT)) {
						addIfStartsWithAndNotExcluded(ret, typesDef.get(TYPE_STRUCT), userInput, currentValues);
					}

					if (emptyOrContainsMatch(typeCategories, TYPE_RELATIONSHIP)) {
						addIfStartsWithAndNotExcluded(ret, typesDef.get(TYPE_RELATIONSHIP), userInput, currentValues);
					}

					if (emptyOrContainsMatch(typeCategories, TYPE_BUSINESS_METADATA)) {
						addIfStartsWithAndNotExcluded(ret, typesDef.get(TYPE_BUSINESS_METADATA), userInput, currentValues);
					}
				}
				break;

				case RESOURCE_END_ONE_ENTITY_TYPE:
				case RESOURCE_END_TWO_ENTITY_TYPE:
				case RESOURCE_ENTITY_TYPE: {
					refreshTypesDefs();

					addIfStartsWithAndNotExcluded(ret, typesDef.get(TYPE_ENTITY), userInput, currentValues);
				}
				break;

				case RESOURCE_END_ONE_ENTITY_CLASSIFICATION:
				case RESOURCE_END_TWO_ENTITY_CLASSIFICATION:
				case RESOURCE_ENTITY_CLASSIFICATION: {
					refreshTypesDefs();

					addIfStartsWithAndNotExcluded(ret, typesDef.get(TYPE_CLASSIFICATION), userInput, currentValues);
				}
                break;

				case RESOURCE_ENTITY_ID: {
					List<String> searchTypes = lookupContext.getResources().get("entity-type");

					if (searchTypes != null && searchTypes.size() == 1) {
						List<String> values = searchEntities(userInput, searchTypes.get(0));

						addIfStartsWithAndNotExcluded(ret, values, userInput, currentValues);
					}
				}
				break;

				case RESOURCE_RELATIONSHIP_TYPE: {
					refreshTypesDefs();
					addIfStartsWithAndNotExcluded(ret, typesDef.get(TYPE_RELATIONSHIP), userInput, currentValues);

				}
				break;

				case RESOURCE_END_ONE_ENTITY_ID: {

					List<String> searchTypes = lookupContext.getResources().get(RESOURCE_END_ONE_ENTITY_TYPE);

					if (searchTypes != null && searchTypes.size() == 1) {
						List<String> values = searchEntities(userInput, searchTypes.get(0));

						addIfStartsWithAndNotExcluded(ret, values, userInput, currentValues);
					}

				}
				break;

				case RESOURCE_END_TWO_ENTITY_ID: {
					List<String> searchTypes = lookupContext.getResources().get(RESOURCE_END_TWO_ENTITY_TYPE);

					if (searchTypes != null && searchTypes.size() == 1) {
						List<String> values = searchEntities(userInput, searchTypes.get(0));

						addIfStartsWithAndNotExcluded(ret, values, userInput, currentValues);
					}
				}
				break;

				default: {
					ret.add(lookupContext.getResourceName());
				}
			}

			return ret;
		}

		private ClientResponse loginToAtlas(Client client) {
			ClientResponse ret      = null;
			HadoopException excp     = null;
			String          loginUrl = null;

			for (String atlasUrl : getAtlasUrls()) {
				try {
					loginUrl = atlasUrl + URL_LOGIN;

					WebResource                    webResource = client.resource(loginUrl);
					MultivaluedMap<String, String> formData    = new MultivaluedMapImpl();
					String                         password    = null;

					try {
						password = PasswordUtils.decryptPassword(getPassword());
					} catch (Exception ex) {
						LOG.info("Password decryption failed; trying Atlas connection with received password string");
					}

					if (password == null) {
						password = getPassword();
					}

					formData.add("j_username", getUserName());
					formData.add("j_password", password);

					try {
						ret = webResource.type(WEB_RESOURCE_CONTENT_TYPE).post(ClientResponse.class, formData);
					} catch (Exception e) {
						LOG.error("failed to login to Atlas at " + loginUrl, e);
					}

					if (ret != null) {
						break;
					}
				} catch (Throwable t) {
					String msgDesc = "Exception while login to Atlas at : " + loginUrl;

					LOG.error(msgDesc, t);

					excp = new HadoopException(msgDesc, t);

					excp.generateResponseDataMap(false, BaseClient.getMessage(t), msgDesc + CONNECTION_ERROR_MSG, null, null);
				}
			}

			if (ret == null) {
				if (excp == null) {
					String msgDesc = "Exception while login to Atlas at : " + loginUrl;

					excp = new HadoopException(msgDesc);

					excp.generateResponseDataMap(false, "", msgDesc + CONNECTION_ERROR_MSG, null, null);
				}

				throw excp;
			}

			return ret;
		}

		private boolean refreshTypesDefs() {
			boolean ret  = false;
			Subject subj = getLoginSubject();

			if (subj == null) {
				return ret;
			}

			Map<String, List<String>> typesDef = Subject.doAs(subj, new PrivilegedAction<Map<String, List<String>>>() {
				@Override
				public Map<String, List<String>> run() {
					Map<String, List<String>> ret  = null;

					for (String atlasUrl : getAtlasUrls()) {
						Client client = null;

						try {
							client = Client.create();

							ClientResponse      loginResponse = loginToAtlas(client);
							WebResource         webResource   = client.resource(atlasUrl + URL_GET_TYPESDEF_HEADERS);
							WebResource.Builder builder       = webResource.getRequestBuilder();

							for (NewCookie cook : loginResponse.getCookies()) {
								builder = builder.cookie(cook);
							}

							ClientResponse response = builder.get(ClientResponse.class);

							if (response != null) {
								String jsonString = response.getEntity(String.class);
								Gson   gson       = new Gson();
								List   types      = gson.fromJson(jsonString, List.class);

								ret = new HashMap<>();

								for (Object type : types) {
									if (type instanceof Map) {
										Map    typeDef  = (Map) type;
										Object name     = typeDef.get("name");
										Object category = typeDef.get("category");

										if (name != null && category != null) {
											String       strCategory  = category.toString().toLowerCase();
											List<String> categoryList = ret.get(strCategory);

											if (categoryList == null) {
												categoryList = new ArrayList<>();

												ret.put(strCategory, categoryList);
											}

											categoryList.add(name.toString());
										}
									}
								}

								break;
							}
						} catch (Throwable t) {
							String msgDesc = "Exception while getting Atlas Resource List.";

							LOG.error(msgDesc, t);
						} finally {
							if (client != null) {
								client.destroy();
							}
						}
					}

					return ret;
				}
			});

			if (typesDef != null) {
				this.typesDef = typesDef;

				ret = true;
			}

			return ret;
		}

		private List<String> searchEntities(String userInput, String entityType) {
			if( LOG.isDebugEnabled()) {
				LOG.debug("==> RangerServiceAtlas.searchEntities(userInput=" + userInput + ", entityType=" + entityType + ")");
			}

			Subject subj = getLoginSubject();

			if (subj == null) {
				return null;
			}

			List<String> list = Subject.doAs(subj, new PrivilegedAction<List<String>>() {
				@Override
				public List<String> run() {
					List<String> ret = null;

					for (String atlasUrl : getAtlasUrls()) {
						Client client = null;

						try {
							client = Client.create();

							ClientResponse loginResponse     = loginToAtlas(client);
							String         entitySearcApiUrl = atlasUrl + "/api/atlas/" + URl_ENTITY_SEARCH;
							StringBuilder  searchUrl         = new StringBuilder();

							searchUrl.append(entitySearcApiUrl)
									 .append("&typeName=")
									 .append(entityType)
									 .append("&attrValuePrefix=" + userInput + "&limit=25");


							WebResource         webResource = client.resource(searchUrl.toString());
							WebResource.Builder builder     = webResource.getRequestBuilder();

							for (NewCookie cook : loginResponse.getCookies()) {
								builder = builder.cookie(cook);
							}

							ClientResponse response = builder.get(ClientResponse.class);

							if (response != null) {
								String            jsonString   = response.getEntity(String.class);
								Gson              gson         = new Gson();
								AtlasSearchResult searchResult = gson.fromJson(jsonString, AtlasSearchResult.class);

								ret = new ArrayList<>();

								if (searchResult != null) {
									List<AtlasEntityHeader> entityHeaderList = searchResult.getEntities();

									for (AtlasEntityHeader entity : entityHeaderList) {
										ret.add((String) entity.getAttribute("qualifiedName"));
									}
								}
							}
						} catch (Throwable t) {
							String msgDesc = "Exception while getting Atlas Entity Resource List.";

							LOG.error(msgDesc, t);
						} finally {
							if (client != null) {
								client.destroy();
							}
						}
					}

					return ret;
				}
			});

			if (LOG.isDebugEnabled()) {
				LOG.debug("<== RangerServiceAtlas.searchEntities(userInput=" + userInput + ", entityType=" + entityType + "): " + list);
			}

			return list;
		}

		String[] getAtlasUrls() {
			String   urlString = connectionProperties.get(CONFIG_REST_ADDRESS);
			String[] ret       = urlString == null ? new String[0] : urlString.split(",");

			// remove separator at the end
			for (int i = 0; i < ret.length; i++) {
				String url = ret[i];

				while (url.length() > 0 && url.charAt(url.length() - 1) == '/') {
					url = url.substring(0, url.length() - 1);
				}

				ret[i] = url;
			}

			return ret;
		}

		String getUserName() {
			return connectionProperties.get(CONFIG_USERNAME);
		}

		String getPassword() {
			return connectionProperties.get(CONFIG_PASSWORD);
		}

		boolean emptyOrContainsMatch(List<String> list, String value) {
			if (list == null || list.isEmpty()) {
				return true;
			}

			for (String item : list) {
				if (StringUtils.equalsIgnoreCase(item, value) || FilenameUtils.wildcardMatch(value, item, IOCase.INSENSITIVE)) {
					return true;
				}
			}

			return false;
		}

		void addIfStartsWithAndNotExcluded(List<String> list, List<String> values, String prefix, List<String> excludeList) {
			if (list == null) {
				return;
			}

			if (values == null) {
				addIfStartsWithAndNotExcluded(list, ENTITY_NOT_CLASSIFIED, prefix, excludeList);
			} else {
				for (String value : values) {
					addIfStartsWithAndNotExcluded(list, value, prefix, excludeList);
				}
			}
		}

		void addIfStartsWithAndNotExcluded(List<String> list, String value, String prefix, List<String> excludeList) {
			if (value == null || list == null) {
				return;
			}

			if (prefix != null && !value.startsWith(prefix)) {
				return;
			}

			if (excludeList != null && excludeList.contains(value)) {
				return;
			}

			list.add(value);
		}
	}

	String getStringConfig(String configName, String defaultValue) {
		String val = service.getConfigs().get(configName);

		return StringUtils.isBlank(val) ? defaultValue : val;
	}

	boolean getBooleanConfig(String configName, boolean defaultValue) {
		String val = service.getConfigs().get(configName);

		return StringUtils.isBlank(val) ? defaultValue : Boolean.parseBoolean(val);
	}
}
