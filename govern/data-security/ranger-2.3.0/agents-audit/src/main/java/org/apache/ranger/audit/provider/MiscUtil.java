/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ranger.audit.provider;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.rmi.dgc.VMID;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.security.auth.Subject;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.authentication.util.KerberosName;
import org.apache.hadoop.security.authentication.util.KerberosUtil;
import org.apache.ranger.authorization.hadoop.utils.RangerCredentialProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import static org.apache.hadoop.util.PlatformName.IBM_JAVA;

public class MiscUtil {
	private static final Logger logger = LoggerFactory.getLogger(MiscUtil.class);

	public static final String TOKEN_START = "%";
	public static final String TOKEN_END = "%";
	public static final String TOKEN_HOSTNAME = "hostname";
	public static final String TOKEN_APP_TYPE = "app-type";
	public static final String TOKEN_JVM_INSTANCE = "jvm-instance";
	public static final String TOKEN_TIME = "time:";
	public static final String TOKEN_PROPERTY = "property:";
	public static final String TOKEN_ENV = "env:";
	public static final String ESCAPE_STR = "\\";

	private static final VMID         sJvmID        = new VMID();

	public static String LINE_SEPARATOR = System.getProperty("line.separator");

	private static Gson sGsonBuilder = null;
	private static String sApplicationType = null;
	private static UserGroupInformation ugiLoginUser = null;
	private static Subject subjectLoginUser = null;
	private static String local_hostname = null;

	private static Map<String, LogHistory> logHistoryList = new Hashtable<String, LogHistory>();
	private static int logInterval = 30000; // 30 seconds

	static {
		try {
			sGsonBuilder = new GsonBuilder().setDateFormat(
					"yyyy-MM-dd HH:mm:ss.SSS").create();
		} catch (Throwable excp) {
			logger.warn(
					"failed to create GsonBuilder object. stringify() will return obj.toString(), instead of Json",
					excp);
		}

		initLocalHost();
	}

	public static String replaceTokens(String str, long time) {
		if (str == null) {
			return str;
		}

		if (time <= 0) {
			time = System.currentTimeMillis();
		}

		for (int startPos = 0; startPos < str.length();) {
			int tagStartPos = str.indexOf(TOKEN_START, startPos);

			if (tagStartPos == -1) {
				break;
			}

			int tagEndPos = str.indexOf(TOKEN_END,
					tagStartPos + TOKEN_START.length());

			if (tagEndPos == -1) {
				break;
			}

			String tag = str.substring(tagStartPos,
					tagEndPos + TOKEN_END.length());
			String token = tag.substring(TOKEN_START.length(),
					tag.lastIndexOf(TOKEN_END));
			String val = "";

			if (token != null) {
				if (token.equals(TOKEN_HOSTNAME)) {
					val = getHostname();
				} else if (token.equals(TOKEN_APP_TYPE)) {
					val = getApplicationType();
				} else if (token.equals(TOKEN_JVM_INSTANCE)) {
					val = getJvmInstanceId();
				} else if (token.startsWith(TOKEN_PROPERTY)) {
					String propertyName = token.substring(TOKEN_PROPERTY
							.length());

					val = getSystemProperty(propertyName);
				} else if (token.startsWith(TOKEN_ENV)) {
					String envName = token.substring(TOKEN_ENV.length());

					val = getEnv(envName);
				} else if (token.startsWith(TOKEN_TIME)) {
					String dtFormat = token.substring(TOKEN_TIME.length());

					val = getFormattedTime(time, dtFormat);
				}
			}

			if (val == null) {
				val = "";
			}

			str = str.substring(0, tagStartPos) + val
					+ str.substring(tagEndPos + TOKEN_END.length());
			startPos = tagStartPos + val.length();
		}

		return str;
	}

	public static String getHostname() {
		String ret = local_hostname;

		if  (ret == null) {
			initLocalHost();

			ret = local_hostname;

			if (ret == null) {
				ret = "unknown";
			}
		}

		return ret;
	}

	public static void setApplicationType(String applicationType) {
		sApplicationType = applicationType;
	}

	public static String getApplicationType() {
		return sApplicationType;
	}

	public static String getJvmInstanceId() {
		Integer val  = Integer.valueOf(sJvmID.toString().hashCode());
		long longVal = val.longValue();
		String ret 	 = Long.toString(Math.abs(longVal));

		return ret;
	}

	public static String getSystemProperty(String propertyName) {
		String ret = null;

		try {
			ret = propertyName != null ? System.getProperty(propertyName)
					: null;
		} catch (Exception excp) {
			logger.warn("getSystemProperty(" + propertyName + ") failed", excp);
		}

		return ret;
	}

	public static String getEnv(String envName) {
		String ret = null;

		try {
			ret = envName != null ? System.getenv(envName) : null;
		} catch (Exception excp) {
			logger.warn("getenv(" + envName + ") failed", excp);
		}

		return ret;
	}

	public static String getFormattedTime(long time, String format) {
		String ret = null;

		try {
			SimpleDateFormat sdf = new SimpleDateFormat(format);

			ret = sdf.format(time);
		} catch (Exception excp) {
			logger.warn("SimpleDateFormat.format() failed: " + format, excp);
		}

		return ret;
	}

	public static void createParents(File file) {
		if (file != null) {
			String parentName = file.getParent();

			if (parentName != null) {
				File parentDir = new File(parentName);

				if (!parentDir.exists()) {
					if (!parentDir.mkdirs()) {
						logger.warn("createParents(): failed to create "
								+ parentDir.getAbsolutePath());
					}
				}
			}
		}
	}

	public static long getNextRolloverTime(long lastRolloverTime, long interval) {
		long now = System.currentTimeMillis() / 1000 * 1000; // round to second

		if (lastRolloverTime <= 0) {
			// should this be set to the next multiple-of-the-interval from
			// start of the day?
			return now + interval;
		} else if (lastRolloverTime <= now) {
			long nextRolloverTime = now + interval;

			// keep it at 'interval' boundary
			long trimInterval = (nextRolloverTime - lastRolloverTime)
					% interval;

			return nextRolloverTime - trimInterval;
		} else {
			return lastRolloverTime;
		}
	}

	public static long getRolloverStartTime(long nextRolloverTime, long interval) {
		return (nextRolloverTime <= interval) ? System.currentTimeMillis()
				: nextRolloverTime - interval;
	}

	public static int parseInteger(String str, int defValue) {
		int ret = defValue;

		if (str != null) {
			try {
				ret = Integer.parseInt(str);
			} catch (Exception excp) {
				// ignore
			}
		}

		return ret;
	}

	public static String generateUniqueId() {
		return UUID.randomUUID().toString();
	}

	// UUID.randomUUID() uses SecureRandom, which is seen to be slow in some environments; this method uses Random
	public static String generateGuid() {
		byte[] randomBytes = new byte[16];

		RandomHolder.random.nextBytes(randomBytes);

		UUID uuid = UUID.nameUUIDFromBytes(randomBytes);

		return uuid.toString();
	}

	public static <T> String stringify(T log) {
		String ret = null;

		if (log != null) {
			if (log instanceof String) {
				ret = (String) log;
			} else if (MiscUtil.sGsonBuilder != null) {
				ret = MiscUtil.sGsonBuilder.toJson(log);
			} else {
				ret = log.toString();
			}
		}

		return ret;
	}

	static public <T> T fromJson(String jsonStr, Class<T> clazz) {
		return sGsonBuilder.fromJson(jsonStr, clazz);
	}

	public static String getStringProperty(Properties props, String propName) {
		String ret = null;

		if (props != null && propName != null) {
			String val = props.getProperty(propName);
			if (val != null) {
				ret = val;
			}
		}

		return ret;
	}

	public static String getStringProperty(Properties props, String propName, String defValue) {
		String ret = defValue;

		if (props != null && propName != null) {
			String val = props.getProperty(propName);
			if (val != null) {
				ret = val;
			}
		}

		return ret;
	}

	public static boolean getBooleanProperty(Properties props, String propName,
			boolean defValue) {
		boolean ret = defValue;

		if (props != null && propName != null) {
			String val = props.getProperty(propName);

			if (val != null) {
				ret = Boolean.valueOf(val);
			}
		}

		return ret;
	}

	public static int getIntProperty(Properties props, String propName,
			int defValue) {
		int ret = defValue;

		if (props != null && propName != null) {
			String val = props.getProperty(propName);
			if (val != null) {
				try {
					ret = Integer.parseInt(val);
				} catch (NumberFormatException excp) {
					ret = defValue;
				}
			}
		}

		return ret;
	}

	public static long getLongProperty(Properties props, String propName,
			long defValue) {
		long ret = defValue;

		if (props != null && propName != null) {
			String val = props.getProperty(propName);
			if (val != null) {
				try {
					ret = Long.parseLong(val);
				} catch (NumberFormatException excp) {
					ret = defValue;
				}
			}
		}

		return ret;
	}

	public static Map<String, String> getPropertiesWithPrefix(Properties props,
			String prefix) {
		Map<String, String> prefixedProperties = new HashMap<String, String>();

		if (props != null && prefix != null) {
			for (String key : props.stringPropertyNames()) {
				if (key == null) {
					continue;
				}

				String val = props.getProperty(key);

				if (key.startsWith(prefix)) {
					key = key.substring(prefix.length());

					if (key == null) {
						continue;
					}

					prefixedProperties.put(key, val);
				}
			}
		}

		return prefixedProperties;
	}

	/**
	 * @param destListStr
	 * @param delim
	 * @return
	 */
	public static List<String> toArray(String destListStr, String delim) {
		List<String> list = new ArrayList<String>();
		if (destListStr != null && !destListStr.isEmpty()) {
			StringTokenizer tokenizer = new StringTokenizer(destListStr,
					delim.trim());
			while (tokenizer.hasMoreTokens()) {
				list.add(tokenizer.nextToken());
			}
		}
		return list;
	}

	public static String getCredentialString(String url, String alias) {
		if (url != null && alias != null) {
			return RangerCredentialProvider.getInstance()
					.getCredentialString(url, alias);
		}
		return null;
	}

	public static UserGroupInformation createUGIFromSubject(Subject subject)
			throws IOException {
		logger.info("SUBJECT " + (subject == null ? "not found" : "found"));
		UserGroupInformation ugi = null;
		if (subject != null) {
			logger.info("SUBJECT.PRINCIPALS.size()="
					+ subject.getPrincipals().size());
			Set<Principal> principals = subject.getPrincipals();
			for (Principal principal : principals) {
				logger.info("SUBJECT.PRINCIPAL.NAME=" + principal.getName());
			}
			try {
				// Do not remove the below statement. The default
				// getLoginUser does some initialization which is needed
				// for getUGIFromSubject() to work.
				UserGroupInformation.getLoginUser();
				logger.info("Default UGI before using new Subject:"
						+ UserGroupInformation.getLoginUser());
			} catch (Throwable t) {
				logger.error("", t);
			}
			ugi = UserGroupInformation.getUGIFromSubject(subject);
			logger.info("SUBJECT.UGI.NAME=" + ugi.getUserName() + ", ugi="
					+ ugi);
		} else {
			logger.info("Server username is not available");
		}
		return ugi;
	}

	/**
	 * @param newUGI
	 * @param newSubject
	 */
	public static void setUGILoginUser(UserGroupInformation newUGI,
			Subject newSubject) {
		if (newUGI != null) {
			UserGroupInformation.setLoginUser(newUGI);
			ugiLoginUser = newUGI;
			logger.info("Setting UGI=" + newUGI);
		} else {
			logger.error("UGI is null. Not setting it.");
		}
		if (newSubject != null) {
			logger.info("Setting SUBJECT");
			subjectLoginUser = newSubject;
		}
	}

	public static UserGroupInformation getUGILoginUser() {
		UserGroupInformation ret = ugiLoginUser;

		if (ret == null) {
			try {
				// Do not cache ugiLoginUser if it is not explicitly set with
				// setUGILoginUser.
				// It appears that the user represented by
				// the returned object is periodically logged out and logged back
				// in when the token is scheduled to expire. So it is better
				// to get the user object every time from UserGroupInformation class and
				// not cache it
				ret = getLoginUser();
			} catch (IOException e) {
				logger.error("Error getting UGI.", e);
			}
		}

		if(ret != null) {
			try {
			ret.checkTGTAndReloginFromKeytab();
			} catch(IOException ioe) {
				logger.error("Error renewing TGT and relogin. Ignoring Exception, and continuing with the old TGT", ioe);
			}
		}

		return ret;
	}

	/**
	 * Execute the {@link PrivilegedExceptionAction} on the {@link UserGroupInformation} if it's set, otherwise call it directly
	 */
	public static <X> X executePrivilegedAction(final PrivilegedExceptionAction<X> action) throws Exception {
		final UserGroupInformation ugi = getUGILoginUser();
		if (ugi != null) {
			return ugi.doAs(action);
		} else {
			return action.run();
		}
	}

	/**
	 * Execute the {@link PrivilegedAction} on the {@link UserGroupInformation} if it's set, otherwise call it directly.
	 */
	public static <X> X executePrivilegedAction(final PrivilegedAction<X> action) {
		final UserGroupInformation ugi = getUGILoginUser();
		if (ugi != null) {
			return ugi.doAs(action);
		} else {
			return action.run();
		}
	}

	public static Subject getSubjectLoginUser() {
		return subjectLoginUser;
	}

	public static String getKerberosNamesRules() {
		return KerberosName.getRules();
	}
	/**
	 *
	 * @param principal
	 *            This could be in the format abc/host@domain.com
	 * @return
	 */
	static public String getShortNameFromPrincipalName(String principal) {
		if (principal == null) {
			return null;
		}
		try {
			// Assuming it is kerberos name for now
			KerberosName kerbrosName = new KerberosName(principal);
			String userName = kerbrosName.getShortName();
			userName = StringUtils.substringBefore(userName, "/");
			userName = StringUtils.substringBefore(userName, "@");
			return userName;
		} catch (Throwable t) {
			logger.error("Error converting kerberos name. principal="
					+ principal + ", KerberosName.rules=" + KerberosName.getRules());
		}
		return principal;
	}

	/**
	 * @param userName
	 * @return
	 */
	static public Set<String> getGroupsForRequestUser(String userName) {
		if (userName != null) {
			try {
				UserGroupInformation ugi = UserGroupInformation
						.createRemoteUser(userName);
				String[] groups = ugi.getGroupNames();
				if (groups != null && groups.length > 0) {
					Set<String> groupsSet = new java.util.HashSet<String>();
					for (String group : groups) {
						groupsSet.add(group);
					}
					return groupsSet;
				}
			} catch (Throwable e) {
				logErrorMessageByInterval(logger,
						"Error getting groups for users. userName=" + userName, e);
			}
		}
		return Collections.emptySet();
	}

	static public boolean logErrorMessageByInterval(Logger useLogger,
			String message) {
		return logErrorMessageByInterval(useLogger, message, null);
	}

	/**
	 * @param useLogger
	 * @param message
	 * @param e
	 */
	static public boolean logErrorMessageByInterval(Logger useLogger,
			String message, Throwable e) {
        if (message == null) {
            return false;
        }

		LogHistory log = logHistoryList.get(message);
		if (log == null) {
			log = new LogHistory();
			logHistoryList.put(message, log);
		}
		if ((System.currentTimeMillis() - log.lastLogTime) > logInterval) {
			log.lastLogTime = System.currentTimeMillis();
			int counter = log.counter;
			log.counter = 0;
			if (counter > 0) {
				message += ". Messages suppressed before: " + counter;
			}
			if (e == null) {
				useLogger.error(message);
			} else {
				useLogger.error(message, e);
			}

			return true;
		} else {
			log.counter++;
		}
		return false;

	}

	public static void setUGIFromJAASConfig(String jaasConfigAppName) throws Exception {
		String keytabFile 			= null;
		String principal  			= null;
		UserGroupInformation ugi 	= null;
		if (logger.isDebugEnabled()){
			logger.debug("===> MiscUtil.setUGIFromJAASConfig() jaasConfigAppName: " + jaasConfigAppName);
		}
		try {
			AppConfigurationEntry entries[] = Configuration.getConfiguration().getAppConfigurationEntry(jaasConfigAppName);
			if(!ArrayUtils.isEmpty(entries)) {
				for (AppConfigurationEntry entry : entries) {
					if (entry.getOptions().get("keyTab") != null) {
						keytabFile = (String) entry.getOptions().get("keyTab");
					}
					if (entry.getOptions().get("principal") != null) {
						principal = (String) entry.getOptions().get("principal");
					}
					if (!StringUtils.isEmpty(principal) && !StringUtils.isEmpty(keytabFile)) {
						break;
					}
				}
				if (!StringUtils.isEmpty(principal) && !StringUtils.isEmpty(keytabFile)) {
					// This will login and set the UGI
					UserGroupInformation.loginUserFromKeytab(principal, keytabFile);
					ugi = UserGroupInformation.getLoginUser();
				} else {
					String error_mesage = "Unable to get the principal/keytab from jaasConfigAppName: " + jaasConfigAppName;
					logger.error(error_mesage);
					throw new Exception(error_mesage);
				}
				logger.info("MiscUtil.setUGIFromJAASConfig() UGI: " + ugi + " principal: " + principal + " keytab: " + keytabFile);
			} else {
				logger.warn("JAASConfig file not found! Ranger Plugin will not working in a Secure Cluster...");
			}
		} catch ( Exception e) {
			logger.error("Unable to set UGI for Principal: " + principal + " keytab: " + keytabFile );
			throw e;
		}
		if (logger.isDebugEnabled()) {
			logger.debug("<=== MiscUtil.setUGIFromJAASConfig() jaasConfigAppName: " + jaasConfigAppName + " UGI: " + ugi + " principal: " + principal + " keytab: " + keytabFile);
		}
	}

	public static void authWithKerberos(String keytab, String principal,
			String nameRules) {

		if (keytab == null || principal == null) {
			return;
		}
		Subject serverSubject = new Subject();
		int successLoginCount = 0;
		String[] spnegoPrincipals = null;

		try {
			if (principal.equals("*")) {
				spnegoPrincipals = KerberosUtil.getPrincipalNames(keytab,
						Pattern.compile("HTTP/.*"));
				if (spnegoPrincipals.length == 0) {
					logger.error("No principals found in keytab=" + keytab);
				}
			} else {
				spnegoPrincipals = new String[] { principal };
			}

			if (nameRules != null) {
				KerberosName.setRules(nameRules);
			}

			boolean useKeytab = true;
			if (!useKeytab) {
				logger.info("Creating UGI with subject");
                                LoginContext loginContext = null;
				List<LoginContext> loginContexts = new ArrayList<LoginContext>();
				for (String spnegoPrincipal : spnegoPrincipals) {
					try {
						logger.info("Login using keytab " + keytab
								+ ", for principal " + spnegoPrincipal);
						final KerberosConfiguration kerberosConfiguration = new KerberosConfiguration(
								keytab, spnegoPrincipal);
                                                loginContext = new LoginContext("",
								serverSubject, null, kerberosConfiguration);
						loginContext.login();
						successLoginCount++;
						logger.info("Login success keytab " + keytab
								+ ", for principal " + spnegoPrincipal);
						loginContexts.add(loginContext);
					} catch (Throwable t) {
						logger.error("Login failed keytab " + keytab
								+ ", for principal " + spnegoPrincipal, t);
					}
					if (successLoginCount > 0) {
						logger.info("Total login success count="
								+ successLoginCount);
						try {
							UserGroupInformation
									.loginUserFromSubject(serverSubject);
							// UserGroupInformation ugi =
							// createUGIFromSubject(serverSubject);
							// if (ugi != null) {
							// setUGILoginUser(ugi, serverSubject);
							// }
						} catch (Throwable e) {
							logger.error("Error creating UGI from subject. subject="
									+ serverSubject);
                                                } finally {
                                                        if (loginContext != null) {
                                                                loginContext.logout();
                                                        }
						}
					} else {
						logger.error("Total logins were successfull from keytab="
								+ keytab + ", principal=" + principal);
					}
				}
			} else {
				logger.info("Creating UGI from keytab directly. keytab="
						+ keytab + ", principal=" + spnegoPrincipals[0]);
				UserGroupInformation ugi = UserGroupInformation
						.loginUserFromKeytabAndReturnUGI(spnegoPrincipals[0],
								keytab);
				MiscUtil.setUGILoginUser(ugi, null);
			}

		} catch (Throwable t) {
			logger.error("Failed to login with given keytab and principal", t);
		}

	}

	static class LogHistory {
		long lastLogTime = 0;
		int counter = 0;
	}

	/**
	 * Kerberos context configuration for the JDK GSS library.
	 */
	private static class KerberosConfiguration extends Configuration {
		private String keytab;
		private String principal;

		public KerberosConfiguration(String keytab, String principal) {
			this.keytab = keytab;
			this.principal = principal;
		}

		@Override
		public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
			Map<String, String> options = new HashMap<String, String>();
			if (IBM_JAVA) {
				options.put("useKeytab", keytab.startsWith("file://") ? keytab
						: "file://" + keytab);
				options.put("principal", principal);
				options.put("credsType", "acceptor");
			} else {
				options.put("keyTab", keytab);
				options.put("principal", principal);
				options.put("useKeyTab", "true");
				options.put("storeKey", "true");
				options.put("doNotPrompt", "true");
				options.put("useTicketCache", "true");
				options.put("renewTGT", "true");
				options.put("isInitiator", "false");
			}
			options.put("refreshKrb5Config", "true");
			String ticketCache = System.getenv("KRB5CCNAME");
			if (ticketCache != null) {
				if (IBM_JAVA) {
					options.put("useDefaultCcache", "true");
					// The first value searched when "useDefaultCcache" is used.
					System.setProperty("KRB5CCNAME", ticketCache);
					options.put("renewTGT", "true");
					options.put("credsType", "both");
				} else {
					options.put("ticketCache", ticketCache);
				}
			}
			if (logger.isDebugEnabled()) {
				options.put("debug", "true");
			}

			return new AppConfigurationEntry[] { new AppConfigurationEntry(
					KerberosUtil.getKrb5LoginModuleName(),
					AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
					options), };
		}
	}

	public static UserGroupInformation getLoginUser() throws IOException {
		return UserGroupInformation.getLoginUser();
	}

	private static void initLocalHost() {
		if ( logger.isDebugEnabled() ) {
			logger.debug("==> MiscUtil.initLocalHost()");
		}

		try {
			local_hostname = InetAddress.getLocalHost().getHostName();
		} catch (Throwable excp) {
			logger.warn("getHostname()", excp);
		}
		if ( logger.isDebugEnabled() ) {
			logger.debug("<== MiscUtil.initLocalHost()");
		}
	}
	public static Date getUTCDateForLocalDate(Date date) {
		TimeZone gmtTimeZone = TimeZone.getTimeZone("GMT+0");
		Calendar local  = Calendar.getInstance();
		int      offset = local.getTimeZone().getOffset(local.getTimeInMillis());
		GregorianCalendar utc = new GregorianCalendar(gmtTimeZone);
		utc.setTimeInMillis(date.getTime());
		utc.add(Calendar.MILLISECOND, -offset);
		return utc.getTime();
	}
	public static Date getUTCDate() {
		TimeZone gmtTimeZone = TimeZone.getTimeZone("GMT+0");
	    Calendar local  = Calendar.getInstance();
	    int      offset = local.getTimeZone().getOffset(local.getTimeInMillis());
	    GregorianCalendar utc = new GregorianCalendar(gmtTimeZone);
	    utc.setTimeInMillis(local.getTimeInMillis());
	    utc.add(Calendar.MILLISECOND, -offset);
	    return utc.getTime();
	}

	// use Holder class to defer initialization until needed
	private static class RandomHolder {
		static final Random random = new Random();
	}

	// Utility methods
	public static int toInt(Object value) {
		if (value == null) {
			return 0;
		}
		if (value instanceof Integer) {
			return (Integer) value;
		}
		if (value.toString().isEmpty()) {
			return 0;
		}
		try {
			return Integer.valueOf(value.toString());
		} catch (Throwable t) {
			logger.error("Error converting value to integer. Value = " + value, t);
		}
		return 0;
	}

	public static long toLong(Object value) {
		if (value == null) {
			return 0;
		}
		if (value instanceof Long) {
			return (Long) value;
		}
		if (value.toString().isEmpty()) {
			return 0;
		}
		try {
			return Long.valueOf(value.toString());
		} catch (Throwable t) {
			logger.error("Error converting value to long. Value = " + value, t);
		}
		return 0;
	}

	public static Date toDate(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Date) {
			return (Date) value;
		}
		try {
			// TODO: Do proper parsing based on Solr response value
			return new Date(value.toString());
		} catch (Throwable t) {
			logger.error("Error converting value to date. Value = " + value, t);
		}
		return null;
	}

	public static Date toLocalDate(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Date) {
			return (Date) value;
		}
		try {
			LocalDateTime localDateTime = LocalDateTime.parse(value.toString(), DateTimeFormatter.ISO_DATE_TIME);
			return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
		} catch (Throwable t) {
			logger.error("Error converting value to date. Value = " + value, t);
		}
		return null;
	}

}
