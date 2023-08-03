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

package org.apache.ranger.security.web.filter;

import com.google.inject.Inject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;

import org.apache.ranger.util.RestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.ranger.biz.UserMgr;
import org.apache.ranger.common.PropertiesUtil;
import org.apache.ranger.common.RangerConstants;
import org.apache.ranger.common.UserSessionBase;
import org.apache.ranger.security.context.RangerContextHolder;
import org.apache.ranger.security.context.RangerSecurityContext;
import org.apache.ranger.security.handler.RangerAuthenticationProvider;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class RangerSSOAuthenticationFilter implements Filter {
	private static final Logger LOG = LoggerFactory.getLogger(RangerSSOAuthenticationFilter.class);

	public static final String BROWSER_USERAGENT = "ranger.sso.browser.useragent";
	public static final String JWT_AUTH_PROVIDER_URL = "ranger.sso.providerurl";
	public static final String JWT_PUBLIC_KEY = "ranger.sso.publicKey";
	public static final String JWT_COOKIE_NAME = "ranger.sso.cookiename";
	public static final String JWT_AUDIENCES = "ranger.sso.audiences";
	public static final String JWT_ORIGINAL_URL_QUERY_PARAM = "ranger.sso.query.param.originalurl";
	public static final String JWT_COOKIE_NAME_DEFAULT = "hadoop-jwt";
	public static final String JWT_ORIGINAL_URL_QUERY_PARAM_DEFAULT = "originalUrl";
	 /**
     * If specified, this configuration property refers to the signature algorithm which a received
     * token must match. Otherwise, the default value "RS256" is used
     */
    public static final String JWT_EXPECTED_SIGALG = "ranger.sso.expected.sigalg";
    public static final String JWT_DEFAULT_SIGALG = "RS256";

	public static final String DEFAULT_BROWSER_USERAGENT = "ranger.default.browser-useragents";


	private SSOAuthenticationProperties jwtProperties;

	private String originalUrlQueryParam = "originalUrl";
        private String authenticationProviderUrl = null;
	private RSAPublicKey publicKey = null;
	private String cookieName = "hadoop-jwt";

	@Autowired
	UserMgr userMgr;

	@Inject
	public RangerSSOAuthenticationFilter(){
		jwtProperties = getJwtProperties();
		loadJwtProperties();
	}

	public RangerSSOAuthenticationFilter(
			SSOAuthenticationProperties jwtProperties){
		this.jwtProperties = jwtProperties;
		loadJwtProperties();
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	/*
	 * doFilter of RangerSSOAuthenticationFilter is the first in the filter list so in this it check for the request
	 * if the request is from browser, doesn't contain local login and sso is enabled then it process the request against knox sso
	 * else if it's ssoenable and the request is with local login string then it show's the appropriate msg
	 * else if ssoenable is false then it contiunes with further filters as it was before sso
	 */
	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)throws IOException, ServletException {

		HttpServletRequest httpRequest = (HttpServletRequest)servletRequest;

                String xForwardedURL = RestUtil.constructForwardableURL(httpRequest);

		if (httpRequest.getRequestedSessionId() != null && !httpRequest.isRequestedSessionIdValid()){
			synchronized(httpRequest.getServletContext()){
				if(httpRequest.getServletContext().getAttribute(httpRequest.getRequestedSessionId()) != null && "locallogin".equals(httpRequest.getServletContext().getAttribute(httpRequest.getRequestedSessionId()).toString())){
					httpRequest.getSession().setAttribute("locallogin","true");
					httpRequest.getServletContext().removeAttribute(httpRequest.getRequestedSessionId());
				}
			}
		}

		RangerSecurityContext context = RangerContextHolder.getSecurityContext();
		UserSessionBase session = context != null ? context.getUserSession() : null;
		boolean ssoEnabled = session != null ? session.isSSOEnabled() : PropertiesUtil.getBooleanProperty("ranger.sso.enabled", false);

		String userAgent = httpRequest.getHeader("User-Agent");
		if(httpRequest.getSession() != null){
			if(httpRequest.getSession().getAttribute("locallogin") != null){
				servletRequest.setAttribute("ssoEnabled", false);
				filterChain.doFilter(servletRequest, servletResponse);
				return;
			}
		}
		//If sso is enable and request is not for local login and is from browser then it will go inside and try for knox sso authentication
		if (ssoEnabled && !httpRequest.getRequestURI().contains(RestUtil.LOCAL_LOGIN_URL)) {
			//if jwt properties are loaded and is current not authenticated then it will go for sso authentication
			//Note : Need to remove !isAuthenticated() after knoxsso solve the bug from cross-origin script
			if (jwtProperties != null && !isAuthenticated()) {
				HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
				String serializedJWT = getJWTFromCookie(httpRequest);
				// if we get the hadoop-jwt token from the cookies then will process it further
				if (serializedJWT != null) {
					SignedJWT jwtToken = null;
					try {
						jwtToken = SignedJWT.parse(serializedJWT);
						boolean valid = validateToken(jwtToken);
						//if the public key provide is correct and also token is not expired the process token
						if (valid) {
							String userName = jwtToken.getJWTClaimsSet().getSubject();
							LOG.info("SSO login user : "+userName);

							String rangerLdapDefaultRole = PropertiesUtil.getProperty("ranger.ldap.default.role", "ROLE_USER");
							//if we get the userName from the token then log into ranger using the same user
							if (userName != null && !userName.trim().isEmpty()) {
								final List<GrantedAuthority> grantedAuths = new ArrayList<>();
								grantedAuths.add(new SimpleGrantedAuthority(rangerLdapDefaultRole));
								final UserDetails principal = new User(userName, "",grantedAuths);
								final Authentication finalAuthentication = new UsernamePasswordAuthenticationToken(principal, "", grantedAuths);
								WebAuthenticationDetails webDetails = new WebAuthenticationDetails(httpRequest);
								((AbstractAuthenticationToken) finalAuthentication).setDetails(webDetails);
								RangerAuthenticationProvider authenticationProvider = new RangerAuthenticationProvider();
								authenticationProvider.setSsoEnabled(ssoEnabled);
								Authentication authentication = authenticationProvider.authenticate(finalAuthentication);
								authentication = getGrantedAuthority(authentication);
								SecurityContextHolder.getContext().setAuthentication(authentication);
							}

							filterChain.doFilter(servletRequest,httpServletResponse);
						}
						// if the token is not valid then redirect to knox sso
						else {
							if (isWebUserAgent(userAgent)) {
								String ssourl = null;
								String ajaxRequestHeader = httpRequest.getHeader("X-Requested-With");
								if ("XMLHttpRequest".equals(ajaxRequestHeader)) {
									ssourl = constructLoginURLForApi(httpRequest, xForwardedURL);
									if (LOG.isDebugEnabled()) {
										LOG.debug("ajaxRequestHeader redirectUrl = " + ssourl);
									}
									httpServletResponse.setHeader("X-Frame-Options", "DENY");
									httpServletResponse.setStatus(RangerConstants.SC_AUTHENTICATION_TIMEOUT);
									httpServletResponse.setHeader("X-Rngr-Redirect-Url", ssourl);
								} else {
									ssourl = RestUtil.constructRedirectURL(httpRequest, authenticationProviderUrl, xForwardedURL, originalUrlQueryParam);
									if (LOG.isDebugEnabled()) {
										LOG.debug("SSO URL = " + ssourl);
									}
									httpServletResponse.sendRedirect(ssourl);
								}
							} else {
								filterChain.doFilter(servletRequest,httpServletResponse);
							}
						}
					} catch (ParseException e) {
						LOG.warn("Unable to parse the JWT token", e);
					}
				}
				// if the jwt token is not available then redirect it to knox sso
				else {
					if (isWebUserAgent(userAgent)) {
						String ssourl = null;
						String ajaxRequestHeader = httpRequest.getHeader("X-Requested-With");
						if ("XMLHttpRequest".equals(ajaxRequestHeader)) {
							ssourl = constructLoginURLForApi(httpRequest, xForwardedURL);
							if (LOG.isDebugEnabled()) {
								LOG.debug("ajaxRequestHeader redirectUrl = " + ssourl);
							}
							httpServletResponse.setHeader("X-Frame-Options", "DENY");
							httpServletResponse.setStatus(RangerConstants.SC_AUTHENTICATION_TIMEOUT);
							httpServletResponse.setHeader("X-Rngr-Redirect-Url", ssourl);
						} else {
							ssourl = RestUtil.constructRedirectURL(httpRequest, authenticationProviderUrl, xForwardedURL, originalUrlQueryParam);
							if (LOG.isDebugEnabled()) {
								LOG.debug("SSO URL = " + ssourl);
							}
							httpServletResponse.sendRedirect(ssourl);
						}
					} else {
						filterChain.doFilter(servletRequest,httpServletResponse);
					}
				}
			}
			//if property is not loaded or is already authenticated then proceed further with next filter
			else {
				filterChain.doFilter(servletRequest, servletResponse);
			}
		} else if(ssoEnabled && ((HttpServletRequest) servletRequest).getRequestURI().contains(RestUtil.LOCAL_LOGIN_URL) && isWebUserAgent(userAgent) && isAuthenticated()){
				//If already there's an active session with sso and user want's to switch to local login(i.e without sso) then it won't be navigated to local login
				// In this scenario the user as to use separate browser
				String url = ((HttpServletRequest) servletRequest).getRequestURI().replace(RestUtil.LOCAL_LOGIN_URL+"/", "");
				url = url.replace(RestUtil.LOCAL_LOGIN_URL, "");
				LOG.warn("There is an active session and if you want local login to ranger, try this on a separate browser");
				((HttpServletResponse)servletResponse).sendRedirect(url);
        } else if (!ssoEnabled && ((HttpServletRequest) servletRequest).getRequestURI().contains(RestUtil.LOCAL_LOGIN_URL) && !isAuthenticated() &&
				( isWebUserAgent(userAgent) || isBrowserAgent(userAgent))) {
			// if sso is not enabled and request has locallogin then need to redirect user to the login page.
			String url = ((HttpServletRequest) servletRequest).getRequestURI().replace(RestUtil.LOCAL_LOGIN_URL+"/", "");
			url = url.replace(RestUtil.LOCAL_LOGIN_URL, "login.jsp");
			// invalidating session
			if (LOG.isDebugEnabled()) {
				LOG.debug("Request does not have any authentication and contains local login url redirecting to login page.");
			}
			((HttpServletRequest) servletRequest).getSession().invalidate();

			((HttpServletResponse)servletResponse).sendRedirect(url);
		} else if (!ssoEnabled && !((HttpServletRequest) servletRequest).getRequestURI().contains(RestUtil.LOCAL_LOGIN_URL) && !isAuthenticated() &&
				( isWebUserAgent(userAgent) || isBrowserAgent(userAgent)) && !isKerberosAuthEnabled()) {
			// if sso is not enabled and request has is from browser and user is not authenticated and browser kerberos auth is not enabled
			// then need to redirect user to the login page.
			String url = ((HttpServletRequest) servletRequest).getRequestURI() ;
			if (!url.contains("login.jsp")) {
				url = url + "login.jsp";
			}
			// invalidating session
			if (LOG.isDebugEnabled()) {
				LOG.debug("Request does not have any authentication, redirecting to login page.");
			}
			((HttpServletRequest) servletRequest).getSession().invalidate();
			((HttpServletResponse)servletResponse).sendRedirect(url);
		}
		//if sso is not enable or the request is not from browser then proceed further with next filter
		else {
			filterChain.doFilter(servletRequest, servletResponse);
		}
	}

	private Authentication getGrantedAuthority(Authentication authentication) {
		UsernamePasswordAuthenticationToken result=null;
		if(authentication!=null && authentication.isAuthenticated()){
			final List<GrantedAuthority> grantedAuths=getAuthorities(authentication.getName().toString());
			final UserDetails userDetails = new User(authentication.getName().toString(), authentication.getCredentials().toString(),grantedAuths);
			result = new UsernamePasswordAuthenticationToken(userDetails,authentication.getCredentials(),grantedAuths);
			result.setDetails(authentication.getDetails());
			return result;
		}
		return authentication;
	}

	private List<GrantedAuthority> getAuthorities(String username) {
		Collection<String> roleList=userMgr.getRolesByLoginId(username);
		final List<GrantedAuthority> grantedAuths = new ArrayList<>();
		for(String role:roleList){
			grantedAuths.add(new SimpleGrantedAuthority(role));
		}
		return grantedAuths;
	}

	private boolean isWebUserAgent(String userAgent) {
		boolean isWeb = false;
		if (jwtProperties != null) {
			String userAgentList[] = jwtProperties.getUserAgentList();
			if(userAgentList != null && userAgentList.length > 0){
				for(String ua : userAgentList){
					if(userAgent.toLowerCase().startsWith(ua.toLowerCase())){
						isWeb = true;
						break;
					}
				}
			}
		}
		return isWeb;
	}

	private void loadJwtProperties() {
		if (jwtProperties != null) {
			authenticationProviderUrl = jwtProperties.getAuthenticationProviderUrl();
			publicKey = jwtProperties.getPublicKey();
			cookieName = jwtProperties.getCookieName();
			originalUrlQueryParam = jwtProperties.getOriginalUrlQueryParam();
		}
	}

	/**
	 * Do not try to validate JWT if user already authenticated via other
	 * provider
	 *
	 * @return true, if JWT validation required
	 */
	private boolean isAuthenticated() {
		Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();
		return !(!(existingAuth != null && existingAuth.isAuthenticated()) || existingAuth instanceof SSOAuthentication);
	}

	/**
	 * Encapsulate the acquisition of the JWT token from HTTP cookies within the
	 * request.
	 *
	 * @param req
	 *            servlet request to get the JWT token from
	 * @return serialized JWT token
	 */
	protected String getJWTFromCookie(HttpServletRequest req) {
		String serializedJWT = null;
		Cookie[] cookies = req.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (cookieName != null && cookieName.equals(cookie.getName())) {
					if (LOG.isDebugEnabled()) {
						LOG.debug(cookieName + " cookie has been found and is being processed");
					}
					serializedJWT = cookie.getValue();
					break;
				}
			}
		}
		return serializedJWT;
	}

	/**
	 * This method provides a single method for validating the JWT for use in
	 * request processing. It provides for the override of specific aspects of
	 * this implementation through submethods used within but also allows for
	 * the override of the entire token validation algorithm.
	 *
	 * @param jwtToken
	 *            the token to validate
	 * @return true if valid
	 */
	protected boolean validateToken(SignedJWT jwtToken) {
		boolean sigValid = validateSignature(jwtToken);
		if (!sigValid) {
			LOG.warn("Signature of JWT token could not be verified. Please check the public key");
			return false;
		}

		boolean expValid = validateExpiration(jwtToken);
		if (!expValid) {
			LOG.warn("Expiration time validation of JWT token failed.");
			return false;
		}

		boolean audiencesValid = validateAudiences(jwtToken);
        if (!audiencesValid) {
            LOG.warn("Audience validation of JWT token failed.");
            return false;
        }

		return true;
	}

	/**
	 * Verify the signature of the JWT token in this method. This method depends
	 * on the public key that was established during init based upon the
	 * provisioned public key. Override this method in subclasses in order to
	 * customize the signature verification behavior.
	 *
	 * @param jwtToken
	 *            the token that contains the signature to be validated
	 * @return valid true if signature verifies successfully; false otherwise
	 */
	protected boolean validateSignature(SignedJWT jwtToken) {
		boolean valid = false;
		if (JWSObject.State.SIGNED == jwtToken.getState()) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("SSO token is in a SIGNED state");
			}
			if (jwtToken.getSignature() != null) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("SSO token signature is not null");
				}
				try {
					JWSVerifier verifier = new RSASSAVerifier(publicKey);
					if (jwtToken.verify(verifier)) {
						valid = true;
						if (LOG.isDebugEnabled()) {
							LOG.debug("SSO token has been successfully verified");
						}
					} else {
						LOG.warn("SSO signature verification failed.Please check the public key");
					}
				} catch (JOSEException je) {
					LOG.warn("Error while validating signature", je);
				}catch(Exception e){
					LOG.warn("Error while validating signature", e);
				}
			}

			// Now check that the signature algorithm was as expected
			if (valid) {
			  String receivedSigAlg = jwtToken.getHeader().getAlgorithm().getName();
			  if (!receivedSigAlg.equals(jwtProperties.getExpectedSigAlg())) {
			    valid = false;
			  }
			}
		}
		return valid;
	}

	/**
	 * Validate that the expiration time of the JWT token has not been violated.
	 * If it has then throw an AuthenticationException. Override this method in
	 * subclasses in order to customize the expiration validation behavior.
	 *
	 * @param jwtToken
	 *            the token that contains the expiration date to validate
	 * @return valid true if the token has not expired; false otherwise
	 */
	protected boolean validateExpiration(SignedJWT jwtToken) {
		boolean valid = false;
		try {
			Date expires = jwtToken.getJWTClaimsSet().getExpirationTime();
			if (expires == null || new Date().before(expires)) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("SSO token expiration date has been " + "successfully validated");
				}
				valid = true;
			} else {
				LOG.warn("SSO expiration date validation failed.");
			}
		} catch (ParseException pe) {
			LOG.warn("SSO expiration date validation failed.", pe);
		}
		return valid;
	}

	protected boolean validateAudiences(SignedJWT jwtToken) {
	    boolean valid = false;

	    if (jwtProperties.getAudiences().isEmpty()) {
	        // if there were no expected audiences configured then just
	        // consider any audience acceptable
	        valid = true;
	    } else {
	        try {
	            List<String> tokenAudienceList = jwtToken.getJWTClaimsSet().getAudience();
	            // if any of the configured audiences is found then consider it acceptable
	            if (tokenAudienceList != null) {
	                for (String aud : tokenAudienceList) {
	                    if (jwtProperties.getAudiences().contains(aud)) {
	                        LOG.debug("Audience claim has been validated.");
	                        valid = true;
	                        break;
	                    }
	                }
	            }
	        } catch (ParseException pe) {
	            LOG.warn("Audience validation failed.", pe);
	        }
	    }

	    return valid;
	}

	@Override
	public void destroy() {
	}

	public SSOAuthenticationProperties getJwtProperties() {
		String providerUrl = PropertiesUtil.getProperty(JWT_AUTH_PROVIDER_URL);
		if (providerUrl != null && PropertiesUtil.getBooleanProperty("ranger.sso.enabled", false)) {
			SSOAuthenticationProperties jwtProperties = new SSOAuthenticationProperties();
			String publicKeyPath = PropertiesUtil.getProperty(JWT_PUBLIC_KEY);
			if (publicKeyPath == null) {
				LOG.error("Public key pem not specified for SSO auth provider {}. SSO auth will be disabled.",providerUrl);
				return null;
			}
			jwtProperties.setAuthenticationProviderUrl(providerUrl);
			jwtProperties.setCookieName(PropertiesUtil.getProperty(JWT_COOKIE_NAME, JWT_COOKIE_NAME_DEFAULT));
			jwtProperties.setOriginalUrlQueryParam(PropertiesUtil.getProperty(JWT_ORIGINAL_URL_QUERY_PARAM, JWT_ORIGINAL_URL_QUERY_PARAM_DEFAULT));
			String defaultUserAgent = PropertiesUtil.getProperty(DEFAULT_BROWSER_USERAGENT);
			String userAgent = PropertiesUtil.getProperty(BROWSER_USERAGENT);
			if (userAgent != null && !userAgent.isEmpty()) {
				jwtProperties.setUserAgentList(userAgent.split(","));
			} else if (defaultUserAgent != null && !defaultUserAgent.isEmpty()) {
				userAgent = defaultUserAgent;
				jwtProperties.setUserAgentList(userAgent.split(","));
			}
			String audiences = PropertiesUtil.getProperty(JWT_AUDIENCES);
            if (audiences != null && !audiences.isEmpty()) {
                jwtProperties.setAudiences(Arrays.asList(audiences.split(",")));
            }
            jwtProperties.setExpectedSigAlg(PropertiesUtil.getProperty(JWT_EXPECTED_SIGALG, JWT_DEFAULT_SIGALG));
			try {
				RSAPublicKey publicKey = parseRSAPublicKey(publicKeyPath);
				jwtProperties.setPublicKey(publicKey);
			} catch (IOException e) {
				LOG.error("Unable to read public certificate file. JWT auth will be disabled.",e);
			} catch (CertificateException e) {
				LOG.error("Unable to parse public certificate file. JWT auth will be disabled.",e);
			} catch (ServletException e) {
				LOG.error("ServletException while processing the properties",e);
			}
			return jwtProperties;
		} else {
			return null;
		}
	}

	/*
	 * public static RSAPublicKey getPublicKeyFromFile(String filePath) throws
	 * IOException, CertificateException {
	 * FileUtils.readFileToString(new File(filePath));
	 * getPublicKeyFromString(pemString); }
	 */

	public static RSAPublicKey parseRSAPublicKey(String pem)
			throws CertificateException, UnsupportedEncodingException,
			ServletException {
		String PEM_HEADER = "-----BEGIN CERTIFICATE-----\n";
		String PEM_FOOTER = "\n-----END CERTIFICATE-----";
		String fullPem = PEM_HEADER + pem + PEM_FOOTER;
		PublicKey key = null;
		try {
			CertificateFactory fact = CertificateFactory.getInstance("X.509");
			ByteArrayInputStream is = new ByteArrayInputStream(fullPem.getBytes("UTF8"));
			X509Certificate cer = (X509Certificate) fact.generateCertificate(is);
			key = cer.getPublicKey();
		} catch (CertificateException ce) {
			String message = null;
			if (pem.startsWith(PEM_HEADER)) {
				message = "CertificateException - be sure not to include PEM header " + "and footer in the PEM configuration element.";
			} else {
				message = "CertificateException - PEM may be corrupt";
			}
			throw new ServletException(message, ce);
		} catch (UnsupportedEncodingException uee) {
			throw new ServletException(uee);
		}
		return (RSAPublicKey) key;
	}
        /**
         * Create the redirect URL to be used for authentication of the user in the absence
         * of a JWT token within the incoming request.
         *
         * @param request
         *            for getting the original request URL
         * @return url to use as login url for redirect
         */
        protected String constructLoginURLForApi(HttpServletRequest request, String xForwardedURL) {
                String delimiter = "?";
                if (authenticationProviderUrl.contains("?")) {
                        delimiter = "&";
                }
                String loginURL = authenticationProviderUrl + delimiter + originalUrlQueryParam + "=";
                if (StringUtils.trimToNull(xForwardedURL) != null) {
                        loginURL += xForwardedURL;
                } else {
                        loginURL += request.getRequestURL();
                }
                if (StringUtils.isNotEmpty(request.getRequestURI()) && request.getRequestURI().length() > 1) {
                        loginURL = loginURL.replace(request.getRequestURI(), "/");
                }
                return loginURL;
        }


    protected boolean isBrowserAgent(String userAgent) {
        boolean isWeb = false;
        String agents = PropertiesUtil.getProperty("ranger.krb.browser-useragents-regex", RangerCSRFPreventionFilter.BROWSER_USER_AGENTS_DEFAULT);
        if (agents == null) {
            agents = RangerCSRFPreventionFilter.BROWSER_USER_AGENTS_DEFAULT;
        }
        String[] browserUserAgents = agents.split(",");
        if (browserUserAgents.length > 0 && userAgent != null) {
            for (String ua : browserUserAgents) {
                if (userAgent.toLowerCase().startsWith(ua.toLowerCase())) {
                    isWeb = true;
                    break;
                }
            }
        }
        return isWeb;
    }

	protected boolean isKerberosAuthEnabled() {
		return PropertiesUtil.getBooleanProperty("ranger.allow.kerberos.auth.login.browser", false);
	}
}
