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
package org.apache.hadoop.security;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.hadoop.security.UserGroupInformation.AuthenticationMethod;
import org.apache.hadoop.security.authentication.util.KerberosUtil;
import org.apache.hadoop.security.authentication.util.KerberosName;
import org.apache.hadoop.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecureClientLogin {
	private static final Logger LOG = LoggerFactory.getLogger(SecureClientLogin.class);
	public static final String HOSTNAME_PATTERN = "_HOST";

	public synchronized static Subject loginUserFromKeytab(String user, String path) throws IOException {
		try {
			Subject subject = new Subject();
			SecureClientLoginConfiguration loginConf = new SecureClientLoginConfiguration(true, user, path);
			LoginContext login = new LoginContext("hadoop-keytab-kerberos", subject, null, loginConf);
			subject.getPrincipals().add(new User(user, AuthenticationMethod.KERBEROS, login));
			login.login();
			return login.getSubject();
		} catch (LoginException le) {
			throw new IOException("Login failure for " + user + " from keytab " + path, le);
		}
	}
	
	public synchronized static Subject loginUserFromKeytab(String user, String path, String nameRules) throws IOException {
		try {
			Subject subject = new Subject();
			SecureClientLoginConfiguration loginConf = new SecureClientLoginConfiguration(true, user, path);
			LoginContext login = new LoginContext("hadoop-keytab-kerberos", subject, null, loginConf);
			KerberosName.setRules(nameRules);
			subject.getPrincipals().add(new User(user, AuthenticationMethod.KERBEROS, login));
			login.login();
			return login.getSubject();
		} catch (LoginException le) {
			throw new IOException("Login failure for " + user + " from keytab " + path, le);
		}
	}

	public synchronized static Subject loginUserWithPassword(String user, String password) throws IOException {
		try {
			Subject subject = new Subject();
			SecureClientLoginConfiguration loginConf = new SecureClientLoginConfiguration(false, user, password);
			LoginContext login = new LoginContext("hadoop-keytab-kerberos", subject, null, loginConf);
			subject.getPrincipals().add(new User(user, AuthenticationMethod.KERBEROS, login));
			login.login();
			return login.getSubject();
		} catch (LoginException le) {
			throw new IOException("Login failure for " + user + " using password ****", le);
		}
	}

	public synchronized static Subject login(String user) throws IOException {
		Subject subject = new Subject();
		subject.getPrincipals().add(new User(user));
		return subject;
	}

	public static Set<Principal> getUserPrincipals(Subject aSubject) {
		if (aSubject != null) {
			Set<User> list = aSubject.getPrincipals(User.class);
			if (list != null) {
				Set<Principal> ret = new HashSet<>();
				ret.addAll(list);
				return ret;
			} else {
				return null;
			}
		} else {
			return null;
		}
	}
	
	public static Principal createUserPrincipal(String aLoginName) {
		return new User(aLoginName);
	}
	
	public static boolean isKerberosCredentialExists(String principal, String keytabPath){
		boolean isValid = false;
		if (keytabPath != null && !keytabPath.isEmpty()) {			
			File keytabFile = new File(keytabPath);
			if (!keytabFile.exists()) {
				LOG.warn(keytabPath + " doesn't exist.");
			} else if (!keytabFile.canRead()) {
				LOG.warn("Unable to read " + keytabPath + ". Please check the file access permissions for user");
			}else{
				isValid = true;
			}
		} else {
			LOG.warn("Can't find keyTab Path : "+keytabPath);
		}
		if (!(principal != null && !principal.isEmpty() && isValid)) {
			isValid = false;
			LOG.warn("Can't find principal : "+principal);
		}
		return isValid;
	}
	
	public static String getPrincipal(String principalConfig, String hostName) throws IOException {
		String[] components = getComponents(principalConfig);
		if (components == null || components.length != 3 || !HOSTNAME_PATTERN.equals(components[1])) {
			return principalConfig;
		} else {
			if (hostName == null) {
				throw new IOException("Can't replace " + HOSTNAME_PATTERN + " pattern since client ranger.service.host is null");
			}
			return replacePattern(components, hostName);
		}
	}
		
	private static String[] getComponents(String principalConfig) {
		if (principalConfig == null)
			return null;
		return principalConfig.split("[/@]");
	}
		
	private static String replacePattern(String[] components, String hostname)
			throws IOException {
		String fqdn = hostname;
		if (fqdn == null || fqdn.isEmpty() || "0.0.0.0".equals(fqdn)) {
			fqdn = java.net.InetAddress.getLocalHost().getCanonicalHostName();
		}
		return components[0] + "/" + StringUtils.toLowerCase(fqdn) + "@" + components[2];
	}
}

class SecureClientLoginConfiguration extends javax.security.auth.login.Configuration {
	private Map<String, String> kerberosOptions = new HashMap<>();
	private boolean usePassword;

	public SecureClientLoginConfiguration(boolean useKeyTab, String principal, String credential) {
		kerberosOptions.put("principal", principal);
		kerberosOptions.put("debug", "false");
		if (useKeyTab) {
			kerberosOptions.put("useKeyTab", "true");
			kerberosOptions.put("keyTab", credential);
			kerberosOptions.put("doNotPrompt", "true");
		} else {
			usePassword = true;
			kerberosOptions.put("useKeyTab", "false");
			kerberosOptions.put(KrbPasswordSaverLoginModule.USERNAME_PARAM, principal);
			kerberosOptions.put(KrbPasswordSaverLoginModule.PASSWORD_PARAM, credential);
			kerberosOptions.put("doNotPrompt", "false");
			kerberosOptions.put("useFirstPass", "true");
			kerberosOptions.put("tryFirstPass","false");
		}
		kerberosOptions.put("storeKey", "true");
		kerberosOptions.put("refreshKrb5Config", "true");
	}

	@Override
	public AppConfigurationEntry[] getAppConfigurationEntry(String appName) {
		AppConfigurationEntry KEYTAB_KERBEROS_LOGIN = new AppConfigurationEntry(KerberosUtil.getKrb5LoginModuleName(), LoginModuleControlFlag.REQUIRED, kerberosOptions);
		if (usePassword) {
			AppConfigurationEntry KERBEROS_PWD_SAVER = new AppConfigurationEntry(KrbPasswordSaverLoginModule.class.getName(), LoginModuleControlFlag.REQUIRED, kerberosOptions);
			return new AppConfigurationEntry[] { KERBEROS_PWD_SAVER, KEYTAB_KERBEROS_LOGIN };
		}
		else {
			return new AppConfigurationEntry[] { KEYTAB_KERBEROS_LOGIN };
		}
	}
	

}
