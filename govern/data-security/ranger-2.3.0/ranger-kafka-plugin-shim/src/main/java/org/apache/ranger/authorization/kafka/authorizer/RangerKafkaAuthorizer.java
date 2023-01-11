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

package org.apache.ranger.authorization.kafka.authorizer;

import java.util.Map;

import org.apache.ranger.plugin.classloader.RangerPluginClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scala.collection.immutable.Set;
import kafka.network.RequestChannel.Session;
import kafka.security.auth.Acl;
import kafka.security.auth.Authorizer;
import org.apache.kafka.common.security.auth.KafkaPrincipal;
import kafka.security.auth.Operation;
import kafka.security.auth.Resource;


//public class RangerKafkaAuthorizer extends Authorizer {
public class RangerKafkaAuthorizer implements Authorizer {
	private static final Logger LOG  = LoggerFactory.getLogger(RangerKafkaAuthorizer.class);

	private static final String   RANGER_PLUGIN_TYPE                      = "kafka";
	private static final String   RANGER_KAFKA_AUTHORIZER_IMPL_CLASSNAME  = "org.apache.ranger.authorization.kafka.authorizer.RangerKafkaAuthorizer";

	private Authorizer              rangerKakfaAuthorizerImpl = null;
	private RangerPluginClassLoader rangerPluginClassLoader   = null;
	
	public RangerKafkaAuthorizer() {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerKafkaAuthorizer.RangerKafkaAuthorizer()");
		}

		this.init();

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerKafkaAuthorizer.RangerKafkaAuthorizer()");
		}
	}
	
	private void init(){
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerKafkaAuthorizer.init()");
		}

		try {
			
			rangerPluginClassLoader = RangerPluginClassLoader.getInstance(RANGER_PLUGIN_TYPE, this.getClass());
			
			@SuppressWarnings("unchecked")
			Class<Authorizer> cls = (Class<Authorizer>) Class.forName(RANGER_KAFKA_AUTHORIZER_IMPL_CLASSNAME, true, rangerPluginClassLoader);

			activatePluginClassLoader();

			rangerKakfaAuthorizerImpl = cls.newInstance();
		} catch (Exception e) {
			// check what need to be done
			LOG.error("Error Enabling RangerKafkaPlugin", e);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerKafkaAuthorizer.init()");
		}
	}

	@Override
	public void configure(Map<String, ?> configs) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerKafkaAuthorizer.configure(Map<String, ?>)");
		}

		try {
			activatePluginClassLoader();

			rangerKakfaAuthorizerImpl.configure(configs);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerKafkaAuthorizer.configure(Map<String, ?>)");
		}
	}

	@Override
	public void close() {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerKafkaAuthorizer.close()");
		}

		try {
			activatePluginClassLoader();
			
			rangerKakfaAuthorizerImpl.close();
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerKafkaAuthorizer.close()");
		}
		
	}

	@Override
	public boolean authorize(Session session, Operation operation,Resource resource) {	
		if(LOG.isDebugEnabled()) {
			LOG.debug(String.format("==> RangerKafkaAuthorizer.authorize(Session=%s, Operation=%s, Resource=%s)", session, operation, resource));
		}

		boolean ret = false;
		
		try {
			activatePluginClassLoader();

			ret = rangerKakfaAuthorizerImpl.authorize(session, operation, resource);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerKafkaAuthorizer.authorize: " + ret);
		}
		
		return ret;
	}

	@Override
	public void addAcls(Set<Acl> acls, Resource resource) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerKafkaAuthorizer.addAcls(Set<Acl>, Resource)");
		}

		try {
			activatePluginClassLoader();

			rangerKakfaAuthorizerImpl.addAcls(acls, resource);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerKafkaAuthorizer.addAcls(Set<Acl>, Resource)");
		}
	}

	@Override
	public boolean removeAcls(Set<Acl> acls, Resource resource) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerKafkaAuthorizer.removeAcls(Set<Acl>, Resource)");
		}
		boolean ret = false;
		try {
			activatePluginClassLoader();

			ret = rangerKakfaAuthorizerImpl.removeAcls(acls, resource);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerKafkaAuthorizer.removeAcls(Set<Acl>, Resource)");
		}
		
		return ret;
	}

	@Override
	public boolean removeAcls(Resource resource) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerKafkaAuthorizer.removeAcls(Resource)");
		}
		boolean ret = false;
		try {
			activatePluginClassLoader();

			ret = rangerKakfaAuthorizerImpl.removeAcls(resource);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerKafkaAuthorizer.removeAcls(Resource)");
		}

		return ret;
	}

	@Override
	public Set<Acl> getAcls(Resource resource) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerKafkaAuthorizer.getAcls(Resource)");
		}
		
		Set<Acl> ret = null;
		
		try {
			activatePluginClassLoader();

			ret = rangerKakfaAuthorizerImpl.getAcls(resource);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerKafkaAuthorizer.getAcls(Resource)");
		}

		return ret;
	}

	@Override
	public scala.collection.immutable.Map<Resource, Set<Acl>> getAcls(KafkaPrincipal principal) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerKafkaAuthorizer.getAcls(KafkaPrincipal)");
		}

		scala.collection.immutable.Map<Resource, Set<Acl>> ret = null;

		try {
			activatePluginClassLoader();

			ret = rangerKakfaAuthorizerImpl.getAcls(principal);
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerKafkaAuthorizer.getAcls(KafkaPrincipal)");
		}

		return ret;
	}

	@Override
	public scala.collection.immutable.Map<Resource, Set<Acl>> getAcls() {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerKafkaAuthorizer.getAcls()");
		}

		scala.collection.immutable.Map<Resource, Set<Acl>> ret = null;

		try {
			activatePluginClassLoader();

			ret = rangerKakfaAuthorizerImpl.getAcls();
		} finally {
			deactivatePluginClassLoader();
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerKafkaAuthorizer.getAcls()");
		}

		return ret;
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
