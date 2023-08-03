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

 package org.apache.ranger.authorization.hive.authorizer;

import java.util.List;

import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.conf.HiveConf.ConfVars;
import org.apache.hadoop.hive.ql.security.HiveAuthenticationProvider;
import org.apache.hadoop.hive.ql.security.authorization.plugin.AbstractHiveAuthorizer;
import org.apache.hadoop.hive.ql.security.authorization.plugin.DisallowTransformHook;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveAccessControlException;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveAuthzPluginException;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveAuthzSessionContext;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveAuthzSessionContext.CLIENT_TYPE;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveMetastoreClientFactory;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HivePolicyProvider;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HivePrincipal;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HivePrivilegeInfo;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HivePrivilegeObject;
import org.apache.hadoop.hive.ql.security.authorization.plugin.SettableConfigUpdater;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.ranger.authorization.utils.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RangerHiveAuthorizerBase extends AbstractHiveAuthorizer {

	private static final Logger LOG = LoggerFactory.getLogger(RangerHiveAuthorizerBase.class);

	private HiveMetastoreClientFactory mMetastoreClientFactory;
	private HiveConf                   mHiveConf;
	private HiveAuthenticationProvider mHiveAuthenticator;
	private HiveAuthzSessionContext    mSessionContext;
	private UserGroupInformation       mUgi;

	public RangerHiveAuthorizerBase(HiveMetastoreClientFactory metastoreClientFactory,
									  HiveConf                   hiveConf,
									  HiveAuthenticationProvider hiveAuthenticator,
									  HiveAuthzSessionContext    context) {
		mMetastoreClientFactory = metastoreClientFactory;
		mHiveConf               = hiveConf;
		mHiveAuthenticator      = hiveAuthenticator;
		mSessionContext         = context;

		String userName = mHiveAuthenticator == null ? null : mHiveAuthenticator.getUserName();

		mUgi = userName == null ? null : UserGroupInformation.createRemoteUser(userName);

		if(mHiveAuthenticator == null) {
			LOG.warn("RangerHiveAuthorizerBase.RangerHiveAuthorizerBase(): hiveAuthenticator is null");
		} else if(StringUtil.isEmpty(userName)) {
			LOG.warn("RangerHiveAuthorizerBase.RangerHiveAuthorizerBase(): hiveAuthenticator.getUserName() returned null/empty");
		} else if(mUgi == null) {
			LOG.warn(String.format("RangerHiveAuthorizerBase.RangerHiveAuthorizerBase(): UserGroupInformation.createRemoteUser(%s) returned null", userName));
		}
	}

	public HiveMetastoreClientFactory getMetastoreClientFactory() {
		return mMetastoreClientFactory;
	}

	public HiveConf getHiveConf() {
		return mHiveConf;
	}

	public HiveAuthenticationProvider getHiveAuthenticator() {
		return mHiveAuthenticator;
	}

	public HiveAuthzSessionContext getHiveAuthzSessionContext() {
		return mSessionContext;
	}

	public UserGroupInformation getCurrentUserGroupInfo() {
		return mUgi;
	}

	@Override
	public void applyAuthorizationConfigPolicy(HiveConf hiveConf) throws HiveAuthzPluginException {
		LOG.debug("RangerHiveAuthorizerBase.applyAuthorizationConfigPolicy()");

		// from SQLStdHiveAccessController.applyAuthorizationConfigPolicy()
		if (mSessionContext != null && mSessionContext.getClientType() == CLIENT_TYPE.HIVESERVER2) {
			// Configure PREEXECHOOKS with DisallowTransformHook to disallow transform queries
			String hooks = hiveConf.getVar(ConfVars.PREEXECHOOKS).trim();
			if (hooks.isEmpty()) {
				hooks = DisallowTransformHook.class.getName();
			} else {
				hooks = hooks + "," + DisallowTransformHook.class.getName();
			}

			hiveConf.setVar(ConfVars.PREEXECHOOKS, hooks);

			SettableConfigUpdater.setHiveConfWhiteList(hiveConf);
		}
	}

	/**
	 * Show privileges for given principal on given object
	 * @param principal
	 * @param privObj
	 * @return
	 * @throws HiveAuthzPluginException
	 * @throws HiveAccessControlException
	 */
	@Override
	public List<HivePrivilegeInfo> showPrivileges(HivePrincipal principal, HivePrivilegeObject privObj)
			throws HiveAuthzPluginException, HiveAccessControlException {
		LOG.debug("RangerHiveAuthorizerBase.showPrivileges()");

		throwNotImplementedException("showPrivileges");

		return null;
	}

	@Override
	public VERSION getVersion() {
		return VERSION.V1;
	}

	private void throwNotImplementedException(String method) throws HiveAuthzPluginException {
		throw new HiveAuthzPluginException(method + "() not implemented in Ranger AbstractHiveAuthorizer");
	}

	@Override
	public HivePolicyProvider getHivePolicyProvider() throws HiveAuthzPluginException {
	    // TODO Auto-generated method stub
	    return null;
	}

}
