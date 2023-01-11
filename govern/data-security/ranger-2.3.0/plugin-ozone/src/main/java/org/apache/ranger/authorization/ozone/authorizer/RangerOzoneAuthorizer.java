
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

package org.apache.ranger.authorization.ozone.authorizer;

import com.google.common.collect.Sets;
import org.apache.hadoop.ozone.security.acl.IAccessAuthorizer;
import org.apache.hadoop.ozone.security.acl.IOzoneObj;
import org.apache.hadoop.ozone.security.acl.OzoneObj;
import org.apache.hadoop.ozone.security.acl.RequestContext;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.ranger.audit.provider.MiscUtil;
import org.apache.ranger.plugin.audit.RangerDefaultAuditHandler;
import org.apache.ranger.plugin.policyengine.RangerAccessRequestImpl;
import org.apache.ranger.plugin.policyengine.RangerAccessResourceImpl;
import org.apache.ranger.plugin.policyengine.RangerAccessResult;
import org.apache.ranger.plugin.service.RangerBasePlugin;
import org.apache.ranger.plugin.util.RangerPerfTracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class RangerOzoneAuthorizer implements IAccessAuthorizer {
	public static final String ACCESS_TYPE_READ = "read";
	public static final String ACCESS_TYPE_WRITE = "write";
	public static final String ACCESS_TYPE_CREATE = "create";
	public static final String ACCESS_TYPE_LIST = "list";
	public static final String ACCESS_TYPE_DELETE = "delete";
	public static final String ACCESS_TYPE_READ_ACL = "read_acl";
	public static final String ACCESS_TYPE_WRITE_ACL = "write_acl";


    public static final String KEY_RESOURCE_VOLUME = "volume";
	public static final String KEY_RESOURCE_BUCKET = "bucket";
	public static final String KEY_RESOURCE_KEY = "key";

	private static final Logger PERF_OZONEAUTH_REQUEST_LOG = RangerPerfTracer.getPerfLogger("ozoneauth.request");

    private static final Logger LOG = LoggerFactory.getLogger(RangerOzoneAuthorizer.class);

	private static volatile RangerBasePlugin rangerPlugin = null;
	RangerDefaultAuditHandler auditHandler = null;

	public RangerOzoneAuthorizer() {
		rangerPlugin = new RangerBasePlugin("ozone", "ozone");

		rangerPlugin.init(); // this will initialize policy engine and policy refresher
		auditHandler = new RangerDefaultAuditHandler();
		rangerPlugin.setResultProcessor(auditHandler);
	}

	@Override
	public boolean checkAccess(IOzoneObj ozoneObject, RequestContext context) {
		boolean returnValue = false;
		if (ozoneObject == null) {
			LOG.error("Ozone object is null!!");
			return returnValue;
		}
		OzoneObj ozoneObj = (OzoneObj) ozoneObject;
		UserGroupInformation ugi = context.getClientUgi();
		ACLType operation = context.getAclRights();
		String resource = ozoneObj.getPath();

		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerOzoneAuthorizer.checkAccess with operation = " + operation + ", resource = " +
					resource + ", store type = " + OzoneObj.StoreType.values() + ", ugi = " + ugi + ", ip = " +
					context.getIp() + ", resourceType = " + ozoneObj.getResourceType() + ")");
		}

		if (rangerPlugin == null) {
			MiscUtil.logErrorMessageByInterval(LOG,
					"Authorizer is still not initialized");
			return returnValue;
		}

		//TODO: If sorce type is S3 and resource is volume, then allow it by default
		if (ozoneObj.getStoreType() == OzoneObj.StoreType.S3 && ozoneObj.getResourceType() == OzoneObj.ResourceType.VOLUME) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("If store type is s3 and resource is volume, then we allow it by default!  Returning true");
			}
			LOG.warn("Allowing access by default since source type is S3 and resource type is Volume!!");
			return true;
		}

		RangerPerfTracer perf = null;

		if (RangerPerfTracer.isPerfTraceEnabled(PERF_OZONEAUTH_REQUEST_LOG)) {
			perf = RangerPerfTracer.getPerfTracer(PERF_OZONEAUTH_REQUEST_LOG, "RangerOzoneAuthorizer.authorize(resource=" + resource + ")");
		}

		Date eventTime = new Date();
		String accessType = mapToRangerAccessType(operation);
		if (accessType == null) {
			MiscUtil.logErrorMessageByInterval(LOG,
					"Unsupported access type. operation=" + operation) ;
			LOG.error("Unsupported access type. operation=" + operation + ", resource=" + resource);
			return returnValue;
		}
		String action = accessType;
		String clusterName = rangerPlugin.getClusterName();

		RangerAccessRequestImpl rangerRequest = new RangerAccessRequestImpl();
		rangerRequest.setUser(ugi.getShortUserName());
		rangerRequest.setUserGroups(Sets.newHashSet(ugi.getGroupNames()));
		rangerRequest.setClientIPAddress(context.getIp().getHostAddress());
		rangerRequest.setRemoteIPAddress(context.getIp().getHostAddress());
		rangerRequest.setAccessTime(eventTime);

		RangerAccessResourceImpl rangerResource = new RangerAccessResourceImpl();
		rangerRequest.setResource(rangerResource);
		rangerRequest.setAccessType(accessType);
		rangerRequest.setAction(action);
		rangerRequest.setRequestData(resource);
		rangerRequest.setClusterName(clusterName);

		if (ozoneObj.getResourceType() == OzoneObj.ResourceType.VOLUME) {
			rangerResource.setValue(KEY_RESOURCE_VOLUME, ozoneObj.getVolumeName());
		} else if (ozoneObj.getResourceType() == OzoneObj.ResourceType.BUCKET || ozoneObj.getResourceType() == OzoneObj.ResourceType.KEY) {
			if (ozoneObj.getStoreType() == OzoneObj.StoreType.S3) {
				rangerResource.setValue(KEY_RESOURCE_VOLUME, "s3Vol");
			} else {
				rangerResource.setValue(KEY_RESOURCE_VOLUME, ozoneObj.getVolumeName());
			}
			rangerResource.setValue(KEY_RESOURCE_BUCKET, ozoneObj.getBucketName());
			if (ozoneObj.getResourceType() == OzoneObj.ResourceType.KEY) {
				rangerResource.setValue(KEY_RESOURCE_KEY, ozoneObj.getKeyName());
			}
		} else {
			LOG.error("Unsupported resource = " + resource);
			MiscUtil.logErrorMessageByInterval(LOG, "Unsupported resource type " + ozoneObj.getResourceType() + " for resource = " + resource
					+ ", request=" + rangerRequest);
			return returnValue;
		}

		try {
			RangerAccessResult result = rangerPlugin
					.isAccessAllowed(rangerRequest);
			if (result == null) {
				LOG.error("Ranger Plugin returned null. Returning false");
			} else {
				returnValue = result.getIsAllowed();
			}
		} catch (Throwable t) {
			LOG.error("Error while calling isAccessAllowed(). request="
					+ rangerRequest, t);
		}
		RangerPerfTracer.log(perf);

		if (LOG.isDebugEnabled()) {
			LOG.debug("rangerRequest=" + rangerRequest + ", return="
					+ returnValue);
		}
		return returnValue;
	}

	private String mapToRangerAccessType(ACLType operation) {
		String rangerAccessType = null;
		switch (operation) {
			case READ:
				rangerAccessType = ACCESS_TYPE_READ;
				break;
			case WRITE:
				rangerAccessType = ACCESS_TYPE_WRITE;
				break;
			case CREATE:
				rangerAccessType = ACCESS_TYPE_CREATE;
				break;
			case DELETE:
				rangerAccessType = ACCESS_TYPE_DELETE;
				break;
			case LIST:
				rangerAccessType = ACCESS_TYPE_LIST;
				break;
			case READ_ACL:
				rangerAccessType = ACCESS_TYPE_READ_ACL;
				break;
			case WRITE_ACL:
				rangerAccessType = ACCESS_TYPE_WRITE_ACL;
				break;
		}
		return rangerAccessType;
	}
}

