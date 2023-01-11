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

package org.apache.ranger.authorization.kms.authorizer;

import org.apache.hadoop.crypto.key.kms.server.KMS.KMSOp;
import org.apache.hadoop.crypto.key.kms.server.KMSACLsType.Type;
import org.apache.hadoop.crypto.key.kms.server.KeyAuthorizationKeyProvider.KeyACLs;
import org.apache.hadoop.crypto.key.kms.server.KeyAuthorizationKeyProvider.KeyOpType;
import org.apache.hadoop.security.AccessControlException;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.ranger.plugin.classloader.RangerPluginClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RangerKmsAuthorizer implements Runnable, KeyACLs {

	private static final Logger LOG  = LoggerFactory.getLogger(RangerKmsAuthorizer.class);

	private static final String   RANGER_PLUGIN_TYPE                      = "kms";
	private static final String   RANGER_KMS_AUTHORIZER_IMPL_CLASSNAME  = "org.apache.ranger.authorization.kms.authorizer.RangerKmsAuthorizer";

	private Object                  impl                    = null;
	private Runnable                implRunnable            = null;
	private KeyACLs                 implKeyACLs             = null;
	private RangerPluginClassLoader rangerPluginClassLoader = null;


	public RangerKmsAuthorizer() {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerKmsAuthorizer.RangerKmsAuthorizer()");
		}

		this.init();

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerKmsAuthorizer.RangerKmsAuthorizer()");
		}
	}

	private void init(){
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerKmsAuthorizer.init()");
		}

		try {

			rangerPluginClassLoader = RangerPluginClassLoader.getInstance(RANGER_PLUGIN_TYPE, this.getClass());

			Class<?> cls = Class.forName(RANGER_KMS_AUTHORIZER_IMPL_CLASSNAME, true, rangerPluginClassLoader);

			activatePluginClassLoader();

			impl 			   = cls.newInstance();
			implRunnable       = (Runnable)impl;
			implKeyACLs 	   = (KeyACLs)impl;
		} catch (Exception e) {
			// check what need to be done
			LOG.error("Error Enabling RangerKMSPlugin", e);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerKmsAuthorizer.init()");
		}
	}

	@Override
	public boolean hasAccessToKey(String keyName, UserGroupInformation ugi, KeyOpType opType) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerKmsAuthorizer.hasAccessToKey(" + keyName + ", " + ugi +", " + opType + ")");
		}

		boolean ret = false;

		try {
			activatePluginClassLoader();

			ret = implKeyACLs.hasAccessToKey(keyName,ugi,opType);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerKmsAuthorizer.hasAccessToKey(" + keyName + ", " + ugi +", " + opType + ")");
		}

		return ret;
	}

	@Override
	public boolean isACLPresent(String aclName, KeyOpType opType) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerKmsAuthorizer.isACLPresent(" + aclName + ", " + opType + ")");
		}

		boolean ret = false;

		try {
			activatePluginClassLoader();

			ret = implKeyACLs.isACLPresent(aclName,opType);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerKmsAuthorizer.isACLPresent(" + aclName + ", " + opType + ")");
		}

		return ret;
	}


	@Override
	public boolean hasAccess(Type aclType, UserGroupInformation ugi, String clientIp) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerKmsAuthorizer.hasAccess(" + aclType + ", " + ugi + ")");
		}

		boolean ret = false;

		try {
			activatePluginClassLoader();

			ret = implKeyACLs.hasAccess(aclType,ugi,clientIp);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerKmsAuthorizer.hasAccess(" + aclType + ", " + ugi + ")");
		}

		return ret;
	}

	@Override
	public void assertAccess(Type aclType, UserGroupInformation ugi,KMSOp operation, String key, String clientIp) throws AccessControlException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerKmsAuthorizer.assertAccess(" + key + ", " + ugi +", " + aclType + ")");
		}

		try {
			activatePluginClassLoader();

			implKeyACLs.assertAccess(aclType,ugi,operation,key,clientIp);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerKmsAuthorizer.assertAccess(" + key + ", " + ugi +", " + aclType + ")");
		}

	}

	@Override
	public void startReloader() {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerKmsAuthorizer.startReloader()");
		}

		try {
			activatePluginClassLoader();

			implKeyACLs.startReloader();
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerKmsAuthorizer.startReloader()");
		}
	}

	@Override
	public void stopReloader() {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerKmsAuthorizer.stopReloader()");
		}

		try {
			activatePluginClassLoader();

			implKeyACLs.stopReloader();
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerKmsAuthorizer.stopReloader()");
		}
	}

	@Override
	public void run() {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerKmsAuthorizer.run()");
		}

		try {
			activatePluginClassLoader();
			implRunnable.run();
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerKmsAuthorizer.run()");
		}

	}

	private void activatePluginClassLoader() {
		if(rangerPluginClassLoader != null) {
			rangerPluginClassLoader.activate();
		}
	}

	private void deactivatePluginClassLoader() {
		if(rangerPluginClassLoader != null) {
			rangerPluginClassLoader.deactivate();
		}
	}
}
