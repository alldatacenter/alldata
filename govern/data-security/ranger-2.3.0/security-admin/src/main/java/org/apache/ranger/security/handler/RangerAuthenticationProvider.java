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

package org.apache.ranger.security.handler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import javax.security.auth.login.Configuration;

import org.apache.ranger.authentication.unix.jaas.RoleUserAuthorityGranter;
import org.apache.ranger.authorization.hadoop.config.RangerAdminConfig;
import org.apache.ranger.authorization.utils.StringUtil;
import org.apache.ranger.common.PropertiesUtil;
import org.apache.ranger.util.Pbkdf2PasswordEncoderCust;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.ldap.core.support.DefaultTlsDirContextAuthenticationStrategy;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.jaas.DefaultJaasAuthenticationProvider;
import org.springframework.security.authentication.jaas.memory.InMemoryConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.LdapAuthenticator;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.apache.ranger.biz.UserMgr;
import org.apache.ranger.biz.SessionMgr;



public class RangerAuthenticationProvider implements AuthenticationProvider {

	@Autowired
	@Qualifier("userService")
	private JdbcUserDetailsManager userDetailsService;

	@Autowired
	UserMgr userMgr;

	@Autowired
	SessionMgr sessionMgr;

	private static final Logger logger = LoggerFactory.getLogger(RangerAuthenticationProvider.class);

	private String rangerAuthenticationMethod;

	private LdapAuthenticator authenticator;

	private boolean ssoEnabled = false;
	private final boolean isFipsEnabled;
	protected MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();

	public RangerAuthenticationProvider() {
		this.isFipsEnabled = RangerAdminConfig.getInstance().isFipsEnabled();

	}

	@Override
	public Authentication authenticate(Authentication authentication)
			throws AuthenticationException {
		if (isSsoEnabled()) {
			if (authentication != null) {
				authentication = getSSOAuthentication(authentication);
				if (authentication != null && authentication.isAuthenticated()) {
					return authentication;
				}
			}
		} else {
		String sha256PasswordUpdateDisable = PropertiesUtil.getProperty("ranger.sha256Password.update.disable", "false");
		if (rangerAuthenticationMethod==null) {
			rangerAuthenticationMethod="NONE";
		}
		if (authentication != null && rangerAuthenticationMethod != null) {
			if ("LDAP".equalsIgnoreCase(rangerAuthenticationMethod)) {
				authentication = getLdapAuthentication(authentication);
				if (authentication!=null && authentication.isAuthenticated()) {
					return authentication;
				} else {
					authentication=getLdapBindAuthentication(authentication);
					if (authentication != null && authentication.isAuthenticated()) {
						return authentication;
					}
				}
			}
			if ("ACTIVE_DIRECTORY".equalsIgnoreCase(rangerAuthenticationMethod)) {
				authentication = getADBindAuthentication(authentication);
				if (authentication != null && authentication.isAuthenticated()) {
					return authentication;
				} else {
					authentication = getADAuthentication(authentication);
					if (authentication != null && authentication.isAuthenticated()) {
						return authentication;
					}
				}
			}
			if ("UNIX".equalsIgnoreCase(rangerAuthenticationMethod)) {
                boolean isPAMAuthEnabled = PropertiesUtil.getBooleanProperty("ranger.pam.authentication.enabled", false);
                authentication= (isPAMAuthEnabled ? getPamAuthentication(authentication) : getUnixAuthentication(authentication));
				if (authentication != null && authentication.isAuthenticated()) {
					return authentication;
				}
			}
			if ("PAM".equalsIgnoreCase(rangerAuthenticationMethod)) {
				authentication = getPamAuthentication(authentication);
				if (authentication != null && authentication.isAuthenticated()) {
					return authentication;
				}
			}

			// Following are JDBC
			if (authentication != null && authentication.getName() != null && sessionMgr.isLoginIdLocked(authentication.getName())) {
				logger.debug("Failed to authenticate since user account is locked");

				throw new LockedException(messages.getMessage("AbstractUserDetailsAuthenticationProvider.locked", "User account is locked"));
			}

			if (this.isFipsEnabled) {
				try {
					authentication = getJDBCAuthentication(authentication,"");
				} catch (Exception e) {
					logger.error("JDBC Authentication failure: ", e);
					throw e;
				}
				return authentication;
			}
			String encoder="SHA256";
			try {
				authentication = getJDBCAuthentication(authentication,encoder);
			} catch (Exception e) {
				logger.debug("JDBC Authentication failure: ", e);
			}
			if (authentication !=null && authentication.isAuthenticated()) {
				return authentication;
			}
			if (authentication != null && !authentication.isAuthenticated()) {
				logger.info("Authentication with SHA-256 failed. Now trying with MD5.");
				encoder="MD5";
				String userName = authentication.getName();
				String userPassword = null;
				if (authentication.getCredentials() != null) {
					userPassword = authentication.getCredentials().toString();
				}
				try {
					authentication = getJDBCAuthentication(authentication,encoder);
				} catch (Exception e) {
					throw e;
				}
				if (authentication != null && authentication.isAuthenticated()) {
					if ("false".equalsIgnoreCase(sha256PasswordUpdateDisable)) {
                                                userMgr.updatePasswordInSHA256(userName,userPassword,false);
					}
					return authentication;
				}else{
					return authentication;
				}
			}
			return authentication;
		}
		}
		return authentication;
	}

	private Authentication getLdapAuthentication(Authentication authentication) {

		try {
			// getting ldap settings
			String rangerLdapURL = PropertiesUtil.getProperty(
					"ranger.ldap.url", "");
			String rangerLdapUserDNPattern = PropertiesUtil.getProperty(
					"ranger.ldap.user.dnpattern", "");
			String rangerLdapGroupSearchBase = PropertiesUtil.getProperty(
					"ranger.ldap.group.searchbase", "");
			String rangerLdapGroupSearchFilter = PropertiesUtil.getProperty(
					"ranger.ldap.group.searchfilter", "");
			String rangerLdapGroupRoleAttribute = PropertiesUtil.getProperty(
					"ranger.ldap.group.roleattribute", "");
			String rangerLdapDefaultRole = PropertiesUtil.getProperty(
					"ranger.ldap.default.role", "ROLE_USER");
			boolean rangerIsStartTlsEnabled = Boolean.valueOf(PropertiesUtil.getProperty(
					"ranger.ldap.starttls", "false"));

			// taking the user-name and password from the authentication
			// object.
			String userName = authentication.getName();
			String userPassword = "";
			if (authentication.getCredentials() != null) {
				userPassword = authentication.getCredentials().toString();
			}

			// populating LDAP context source with LDAP URL and user-DN-pattern
			LdapContextSource ldapContextSource = new DefaultSpringSecurityContextSource(
					rangerLdapURL);
			if (rangerIsStartTlsEnabled) {
				ldapContextSource.setPooled(false);
				ldapContextSource.setAuthenticationStrategy(new DefaultTlsDirContextAuthenticationStrategy());
			}

			ldapContextSource.setCacheEnvironmentProperties(false);
			ldapContextSource.setAnonymousReadOnly(true);

			// Creating BindAuthenticator using Ldap Context Source.
			BindAuthenticator bindAuthenticator = new BindAuthenticator(
					ldapContextSource);
			//String[] userDnPatterns = new String[] { rangerLdapUserDNPattern };
			String[] userDnPatterns = rangerLdapUserDNPattern.split(";");
			bindAuthenticator.setUserDnPatterns(userDnPatterns);

			LdapAuthenticationProvider ldapAuthenticationProvider = null;

			if (!StringUtil.isEmpty(rangerLdapGroupSearchBase) && !StringUtil.isEmpty(rangerLdapGroupSearchFilter)) {
				// Creating LDAP authorities populator using Ldap context source and
				// Ldap group search base.
				// populating LDAP authorities populator with group search
				// base,group role attribute, group search filter.
				DefaultLdapAuthoritiesPopulator defaultLdapAuthoritiesPopulator = new DefaultLdapAuthoritiesPopulator(
						ldapContextSource, rangerLdapGroupSearchBase);
				defaultLdapAuthoritiesPopulator.setGroupRoleAttribute(rangerLdapGroupRoleAttribute);
				defaultLdapAuthoritiesPopulator.setGroupSearchFilter(rangerLdapGroupSearchFilter);
				defaultLdapAuthoritiesPopulator.setIgnorePartialResultException(true);

				// Creating Ldap authentication provider using BindAuthenticator and Ldap authentication populator
				ldapAuthenticationProvider = new LdapAuthenticationProvider(
						bindAuthenticator, defaultLdapAuthoritiesPopulator);
			} else {
				ldapAuthenticationProvider = new LdapAuthenticationProvider(bindAuthenticator);
			}

			// getting user authenticated
			if (userName != null && userPassword != null
					&& !userName.trim().isEmpty()
					&& !userPassword.trim().isEmpty()) {
				final List<GrantedAuthority> grantedAuths = new ArrayList<>();
				grantedAuths.add(new SimpleGrantedAuthority(
						rangerLdapDefaultRole));

				final UserDetails principal = new User(userName, userPassword,
						grantedAuths);

				final Authentication finalAuthentication = new UsernamePasswordAuthenticationToken(
						principal, userPassword, grantedAuths);

				authentication = ldapAuthenticationProvider
						.authenticate(finalAuthentication);
				authentication=getAuthenticationWithGrantedAuthority(authentication);
				return authentication;
			} else {
				return authentication;
			}
		} catch (Exception e) {
			logger.debug("LDAP Authentication Failed:", e);
		}
		return authentication;
	}

	public Authentication getADAuthentication(Authentication authentication) {
		try{
			String rangerADURL = PropertiesUtil.getProperty("ranger.ldap.ad.url",
					"");
			String rangerADDomain = PropertiesUtil.getProperty(
					"ranger.ldap.ad.domain", "");
			String rangerLdapDefaultRole = PropertiesUtil.getProperty(
					"ranger.ldap.default.role", "ROLE_USER");
			String rangerLdapUserSearchFilter = PropertiesUtil.getProperty(
                                       "ranger.ldap.ad.user.searchfilter", "(sAMAccountName={0})");

			ActiveDirectoryLdapAuthenticationProvider adAuthenticationProvider = new ActiveDirectoryLdapAuthenticationProvider(
					rangerADDomain, rangerADURL);
			adAuthenticationProvider.setConvertSubErrorCodesToExceptions(true);
			adAuthenticationProvider.setUseAuthenticationRequestCredentials(true);
			adAuthenticationProvider.setSearchFilter(rangerLdapUserSearchFilter);

			// Grab the user-name and password out of the authentication object.
			String userName = authentication.getName();
			String userPassword = "";
			if (authentication.getCredentials() != null) {
				userPassword = authentication.getCredentials().toString();
			}

			// getting user authenticated
			if (userName != null && userPassword != null
					&& !userName.trim().isEmpty() && !userPassword.trim().isEmpty()) {
				final List<GrantedAuthority> grantedAuths = new ArrayList<>();
				grantedAuths.add(new SimpleGrantedAuthority(rangerLdapDefaultRole));
				final UserDetails principal = new User(userName, userPassword,
						grantedAuths);
				final Authentication finalAuthentication = new UsernamePasswordAuthenticationToken(
						principal, userPassword, grantedAuths);
				authentication = adAuthenticationProvider
						.authenticate(finalAuthentication);
				return authentication;
			} else {
				return authentication;
			}
		}catch (Exception e) {
			logger.debug("AD Authentication Failed:", e);
		}
		return authentication;
	}

	public Authentication getPamAuthentication(Authentication authentication) {
		try {
			String rangerLdapDefaultRole = PropertiesUtil.getProperty(
					"ranger.ldap.default.role", "ROLE_USER");
			DefaultJaasAuthenticationProvider jaasAuthenticationProvider = new DefaultJaasAuthenticationProvider();
			String loginModuleName = "org.apache.ranger.authentication.unix.jaas.PamLoginModule";
			LoginModuleControlFlag controlFlag = LoginModuleControlFlag.REQUIRED;
			Map<String, String> options = PropertiesUtil.getPropertiesMap();

			if (!options.containsKey("ranger.pam.service"))
				options.put("ranger.pam.service", "ranger-admin");

			AppConfigurationEntry appConfigurationEntry = new AppConfigurationEntry(
					loginModuleName, controlFlag, options);
			AppConfigurationEntry[] appConfigurationEntries = new AppConfigurationEntry[] { appConfigurationEntry };
			Map<String, AppConfigurationEntry[]> appConfigurationEntriesOptions = new HashMap<String, AppConfigurationEntry[]>();
			appConfigurationEntriesOptions.put("SPRINGSECURITY",
					appConfigurationEntries);
			Configuration configuration = new InMemoryConfiguration(
					appConfigurationEntriesOptions);
			jaasAuthenticationProvider.setConfiguration(configuration);
			RoleUserAuthorityGranter authorityGranter = new RoleUserAuthorityGranter();
			RoleUserAuthorityGranter[] authorityGranters = new RoleUserAuthorityGranter[] { authorityGranter };
			jaasAuthenticationProvider.setAuthorityGranters(authorityGranters);
			jaasAuthenticationProvider.afterPropertiesSet();
			String userName = authentication.getName();
			String userPassword = "";
			if (authentication.getCredentials() != null) {
				userPassword = authentication.getCredentials().toString();
			}

			// getting user authenticated
			if (userName != null && userPassword != null
					&& !userName.trim().isEmpty()
					&& !userPassword.trim().isEmpty()) {
				final List<GrantedAuthority> grantedAuths = new ArrayList<>();
				grantedAuths.add(new SimpleGrantedAuthority(
						rangerLdapDefaultRole));
				final UserDetails principal = new User(userName, userPassword,
						grantedAuths);
				final Authentication finalAuthentication = new UsernamePasswordAuthenticationToken(
						principal, userPassword, grantedAuths);
				authentication = jaasAuthenticationProvider
						.authenticate(finalAuthentication);
				authentication=getAuthenticationWithGrantedAuthority(authentication);
				return authentication;
			} else {
				return authentication;
			}
		} catch (Exception e) {
			logger.debug("Pam Authentication Failed:", e);
		}
		return authentication;
	}

	public Authentication getUnixAuthentication(Authentication authentication) {

		try {
			String rangerLdapDefaultRole = PropertiesUtil.getProperty(
					"ranger.ldap.default.role", "ROLE_USER");
			DefaultJaasAuthenticationProvider jaasAuthenticationProvider = new DefaultJaasAuthenticationProvider();
			String loginModuleName = "org.apache.ranger.authentication.unix.jaas.RemoteUnixLoginModule";
			LoginModuleControlFlag controlFlag = LoginModuleControlFlag.REQUIRED;
			Map<String, String> options = PropertiesUtil.getPropertiesMap();
			AppConfigurationEntry appConfigurationEntry = new AppConfigurationEntry(
					loginModuleName, controlFlag, options);
			AppConfigurationEntry[] appConfigurationEntries = new AppConfigurationEntry[] { appConfigurationEntry };
			Map<String, AppConfigurationEntry[]> appConfigurationEntriesOptions = new HashMap<String, AppConfigurationEntry[]>();
			appConfigurationEntriesOptions.put("SPRINGSECURITY",
					appConfigurationEntries);
			Configuration configuration = new InMemoryConfiguration(
					appConfigurationEntriesOptions);
			jaasAuthenticationProvider.setConfiguration(configuration);
			RoleUserAuthorityGranter authorityGranter = new RoleUserAuthorityGranter();
			RoleUserAuthorityGranter[] authorityGranters = new RoleUserAuthorityGranter[] { authorityGranter };
			jaasAuthenticationProvider.setAuthorityGranters(authorityGranters);
			jaasAuthenticationProvider.afterPropertiesSet();
			String userName = authentication.getName();
			String userPassword = "";
			if (authentication.getCredentials() != null) {
				userPassword = authentication.getCredentials().toString();
			}

			// getting user authenticated
			if (userName != null && userPassword != null
					&& !userName.trim().isEmpty()
					&& !userPassword.trim().isEmpty()) {
				final List<GrantedAuthority> grantedAuths = new ArrayList<>();
				grantedAuths.add(new SimpleGrantedAuthority(
						rangerLdapDefaultRole));
				final UserDetails principal = new User(userName, userPassword,
						grantedAuths);
				final Authentication finalAuthentication = new UsernamePasswordAuthenticationToken(
						principal, userPassword, grantedAuths);
				authentication = jaasAuthenticationProvider
						.authenticate(finalAuthentication);
				authentication=getAuthenticationWithGrantedAuthority(authentication);
				return authentication;
			} else {
				return authentication;
			}
		} catch (Exception e) {
			logger.debug("Unix Authentication Failed:", e);
		}

		return authentication;
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return authentication.equals(UsernamePasswordAuthenticationToken.class);
	}

	public String getRangerAuthenticationMethod() {
		return rangerAuthenticationMethod;
	}

	public void setRangerAuthenticationMethod(String rangerAuthenticationMethod) {
		this.rangerAuthenticationMethod = rangerAuthenticationMethod;
	}

	public LdapAuthenticator getAuthenticator() {
		return authenticator;
	}

	public void setAuthenticator(LdapAuthenticator authenticator) {
		this.authenticator = authenticator;
	}

	private Authentication getADBindAuthentication(Authentication authentication) {
		try {
			String rangerADURL = PropertiesUtil.getProperty("ranger.ldap.ad.url", "");
			String rangerLdapADBase = PropertiesUtil.getProperty("ranger.ldap.ad.base.dn", "");
			String rangerADBindDN = PropertiesUtil.getProperty("ranger.ldap.ad.bind.dn", "");
			String rangerADBindPassword = PropertiesUtil.getProperty("ranger.ldap.ad.bind.password", "");
			String rangerLdapDefaultRole = PropertiesUtil.getProperty("ranger.ldap.default.role", "ROLE_USER");
			String rangerLdapReferral = PropertiesUtil.getProperty("ranger.ldap.ad.referral", "follow");
			String rangerLdapUserSearchFilter = PropertiesUtil.getProperty("ranger.ldap.ad.user.searchfilter", "(sAMAccountName={0})");
			boolean rangerIsStartTlsEnabled = Boolean.valueOf(PropertiesUtil.getProperty(
					"ranger.ldap.starttls", "false"));
			String userName = authentication.getName();
			String userPassword = "";
			if (authentication.getCredentials() != null) {
				userPassword = authentication.getCredentials().toString();
			}

			LdapContextSource ldapContextSource = new DefaultSpringSecurityContextSource(rangerADURL);
			ldapContextSource.setUserDn(rangerADBindDN);
			ldapContextSource.setPassword(rangerADBindPassword);
			ldapContextSource.setReferral(rangerLdapReferral);
			ldapContextSource.setCacheEnvironmentProperties(true);
			ldapContextSource.setAnonymousReadOnly(false);
			ldapContextSource.setPooled(true);
			if (rangerIsStartTlsEnabled) {
				ldapContextSource.setPooled(false);
				ldapContextSource.setAuthenticationStrategy(new DefaultTlsDirContextAuthenticationStrategy());
			}
			ldapContextSource.afterPropertiesSet();

			//String searchFilter="(sAMAccountName={0})";
			if (rangerLdapUserSearchFilter==null || rangerLdapUserSearchFilter.trim().isEmpty()) {
				rangerLdapUserSearchFilter="(sAMAccountName={0})";
			}
			FilterBasedLdapUserSearch userSearch=new FilterBasedLdapUserSearch(rangerLdapADBase, rangerLdapUserSearchFilter,ldapContextSource);
			userSearch.setSearchSubtree(true);

			BindAuthenticator bindAuthenticator = new BindAuthenticator(ldapContextSource);
			bindAuthenticator.setUserSearch(userSearch);
			bindAuthenticator.afterPropertiesSet();

			LdapAuthenticationProvider ldapAuthenticationProvider = new LdapAuthenticationProvider(bindAuthenticator);

			if (userName != null && userPassword != null && !userName.trim().isEmpty() && !userPassword.trim().isEmpty()) {
				final List<GrantedAuthority> grantedAuths = new ArrayList<>();
				grantedAuths.add(new SimpleGrantedAuthority(rangerLdapDefaultRole));
				final UserDetails principal = new User(userName, userPassword,grantedAuths);
				final Authentication finalAuthentication = new UsernamePasswordAuthenticationToken(principal, userPassword, grantedAuths);

				authentication = ldapAuthenticationProvider.authenticate(finalAuthentication);
				authentication=getAuthenticationWithGrantedAuthority(authentication);
				return authentication;
			} else {
				return authentication;
			}
		} catch (Exception e) {
			logger.debug("AD Authentication Failed:", e);
		}
		return authentication;
	}

	private Authentication getLdapBindAuthentication(Authentication authentication) {
		try {
			String rangerLdapURL = PropertiesUtil.getProperty("ranger.ldap.url", "");
			String rangerLdapUserDNPattern = PropertiesUtil.getProperty("ranger.ldap.user.dnpattern", "");
			String rangerLdapGroupSearchBase = PropertiesUtil.getProperty("ranger.ldap.group.searchbase", "");
			String rangerLdapGroupSearchFilter = PropertiesUtil.getProperty("ranger.ldap.group.searchfilter", "");
			String rangerLdapGroupRoleAttribute = PropertiesUtil.getProperty("ranger.ldap.group.roleattribute", "");
			String rangerLdapDefaultRole = PropertiesUtil.getProperty("ranger.ldap.default.role", "ROLE_USER");
			String rangerLdapBase = PropertiesUtil.getProperty("ranger.ldap.base.dn", "");
			String rangerLdapBindDN = PropertiesUtil.getProperty("ranger.ldap.bind.dn", "");
			String rangerLdapBindPassword = PropertiesUtil.getProperty("ranger.ldap.bind.password", "");
			String rangerLdapReferral = PropertiesUtil.getProperty("ranger.ldap.referral", "follow");
			String rangerLdapUserSearchFilter = PropertiesUtil.getProperty("ranger.ldap.user.searchfilter", "(uid={0})");
			boolean rangerIsStartTlsEnabled = Boolean.valueOf(PropertiesUtil.getProperty(
					"ranger.ldap.starttls", "false"));
			String userName = authentication.getName();
			String userPassword = "";
			if (authentication.getCredentials() != null) {
				userPassword = authentication.getCredentials().toString();
			}

			LdapContextSource ldapContextSource = new DefaultSpringSecurityContextSource(rangerLdapURL);
			ldapContextSource.setUserDn(rangerLdapBindDN);
			ldapContextSource.setPassword(rangerLdapBindPassword);
			ldapContextSource.setReferral(rangerLdapReferral);
			ldapContextSource.setCacheEnvironmentProperties(false);
			ldapContextSource.setAnonymousReadOnly(false);
			ldapContextSource.setPooled(true);
			if (rangerIsStartTlsEnabled) {
				ldapContextSource.setPooled(false);
				ldapContextSource.setAuthenticationStrategy(new DefaultTlsDirContextAuthenticationStrategy());
			}
			ldapContextSource.afterPropertiesSet();

			DefaultLdapAuthoritiesPopulator defaultLdapAuthoritiesPopulator = new DefaultLdapAuthoritiesPopulator(ldapContextSource, rangerLdapGroupSearchBase);
			defaultLdapAuthoritiesPopulator.setGroupRoleAttribute(rangerLdapGroupRoleAttribute);
			defaultLdapAuthoritiesPopulator.setGroupSearchFilter(rangerLdapGroupSearchFilter);
			defaultLdapAuthoritiesPopulator.setIgnorePartialResultException(true);

			//String searchFilter="(uid={0})";
			if (rangerLdapUserSearchFilter==null||rangerLdapUserSearchFilter.trim().isEmpty()) {
				rangerLdapUserSearchFilter="(uid={0})";
			}
			FilterBasedLdapUserSearch userSearch=new FilterBasedLdapUserSearch(rangerLdapBase, rangerLdapUserSearchFilter,ldapContextSource);
			userSearch.setSearchSubtree(true);

			BindAuthenticator bindAuthenticator = new BindAuthenticator(ldapContextSource);
			bindAuthenticator.setUserSearch(userSearch);
			String[] userDnPatterns = new String[] { rangerLdapUserDNPattern };
			bindAuthenticator.setUserDnPatterns(userDnPatterns);
			bindAuthenticator.afterPropertiesSet();

			LdapAuthenticationProvider ldapAuthenticationProvider = new LdapAuthenticationProvider(bindAuthenticator,defaultLdapAuthoritiesPopulator);

			if (userName != null && userPassword != null && !userName.trim().isEmpty()&& !userPassword.trim().isEmpty()) {
				final List<GrantedAuthority> grantedAuths = new ArrayList<>();
				grantedAuths.add(new SimpleGrantedAuthority(rangerLdapDefaultRole));
				final UserDetails principal = new User(userName, userPassword,grantedAuths);
				final Authentication finalAuthentication = new UsernamePasswordAuthenticationToken(principal, userPassword, grantedAuths);

				authentication = ldapAuthenticationProvider.authenticate(finalAuthentication);
				authentication=getAuthenticationWithGrantedAuthority(authentication);
				return authentication;
			} else {
				return authentication;
			}
		} catch (Exception e) {
			logger.debug("LDAP Authentication Failed:", e);
		}
		return authentication;
	}

	private Authentication getJDBCAuthentication(Authentication authentication,String encoder) throws AuthenticationException{
		try {
			DaoAuthenticationProvider authenticator = new DaoAuthenticationProvider();
			authenticator.setUserDetailsService(userDetailsService);
			if (this.isFipsEnabled) {
				if (authentication != null && authentication.getCredentials() != null && !authentication.isAuthenticated()) {
					Pbkdf2PasswordEncoderCust passwordEncoder = new Pbkdf2PasswordEncoderCust(authentication.getName());
					passwordEncoder.setEncodeHashAsBase64(true);
					authenticator.setPasswordEncoder(passwordEncoder);
				}
			} else {
				if (encoder != null && "SHA256".equalsIgnoreCase(encoder) && authentication != null) {
					authenticator.setPasswordEncoder(new RangerCustomPasswordEncoder(authentication.getName(),"SHA-256"));

				} else if (encoder != null && "MD5".equalsIgnoreCase(encoder)  && authentication != null) {
					authenticator.setPasswordEncoder(new RangerCustomPasswordEncoder(authentication.getName(),"MD5"));
				}
			}

			String userName ="";
			String userPassword = "";
			if (authentication!=null) {
				userName = authentication.getName();
				if (authentication.getCredentials() != null) {
					userPassword = authentication.getCredentials().toString();
				}
			}

			String rangerLdapDefaultRole = PropertiesUtil.getProperty("ranger.ldap.default.role", "ROLE_USER");
			if (userName != null && userPassword != null && !userName.trim().isEmpty()&& !userPassword.trim().isEmpty()) {
				final List<GrantedAuthority> grantedAuths = new ArrayList<>();
				grantedAuths.add(new SimpleGrantedAuthority(rangerLdapDefaultRole));
				final UserDetails principal = new User(userName, userPassword,grantedAuths);
				final Authentication finalAuthentication = new UsernamePasswordAuthenticationToken(principal, userPassword, grantedAuths);
				authentication= authenticator.authenticate(finalAuthentication);
				return authentication;
			} else {
				if (authentication !=null && !authentication.isAuthenticated()) {
					throw new BadCredentialsException("Bad credentials");
				}
			}
		} catch (BadCredentialsException e) {
			throw e;
		}catch (AuthenticationServiceException e) {
			throw e;
		}catch (AuthenticationException e) {
			throw e;
		}catch (Exception e) {
			throw e;
		} catch (Throwable t) {
			throw new BadCredentialsException("Bad credentials", t);
		}
		return authentication;
	}
	
	private List<GrantedAuthority> getAuthorities(String username) {
		Collection<String> roleList=userMgr.getRolesByLoginId(username);
		final List<GrantedAuthority> grantedAuths = new ArrayList<>();
		for (String role : roleList) {
			grantedAuths.add(new SimpleGrantedAuthority(role));
		}
		return grantedAuths;
	}

	public Authentication getAuthenticationWithGrantedAuthority(Authentication authentication){
		UsernamePasswordAuthenticationToken result = null;
		if (authentication != null && authentication.isAuthenticated()) {
			final List<GrantedAuthority> grantedAuths=getAuthorities(authentication.getName().toString());
			final UserDetails userDetails = new User(authentication.getName().toString(), authentication.getCredentials().toString(),grantedAuths);
			result = new UsernamePasswordAuthenticationToken(userDetails,authentication.getCredentials(),grantedAuths);
			result.setDetails(authentication.getDetails());
			return result;
		}
		return authentication;
	}
	
	private Authentication getSSOAuthentication(Authentication authentication) throws AuthenticationException{
		return authentication;
	}

	/**
	 * @return the ssoEnabled
	 */
	public boolean isSsoEnabled() {
		return ssoEnabled;
	}

	/**
	 * @param ssoEnabled the ssoEnabled to set
	 */
	public void setSsoEnabled(boolean ssoEnabled) {
		this.ssoEnabled = ssoEnabled;
	}
}
