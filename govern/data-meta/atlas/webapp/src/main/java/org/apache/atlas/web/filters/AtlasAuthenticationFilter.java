/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.web.filters;

import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasConfiguration;
import org.apache.atlas.security.SecurityProperties;
import org.apache.atlas.utils.AuthenticationUtil;
import org.apache.atlas.web.security.AtlasAuthenticationProvider;
import org.apache.atlas.web.util.Servlets;
import org.apache.commons.collections.iterators.IteratorEnumeration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationConverter;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.security.SecurityUtil;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.authentication.client.AuthenticatedURL;
import org.apache.hadoop.security.authentication.client.AuthenticationException;
import org.apache.hadoop.security.authentication.client.KerberosAuthenticator;
import org.apache.hadoop.security.authentication.server.AuthenticationFilter;
import org.apache.hadoop.security.authentication.server.AuthenticationHandler;
import org.apache.hadoop.security.authentication.server.AuthenticationToken;
import org.apache.hadoop.security.authentication.server.KerberosAuthenticationHandler;
import org.apache.hadoop.security.authentication.util.Signer;
import org.apache.hadoop.security.authentication.util.SignerException;
import org.apache.hadoop.security.authentication.util.SignerSecretProvider;
import org.apache.hadoop.security.authorize.AuthorizationException;
import org.apache.hadoop.security.authorize.ProxyUsers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.atlas.web.filters.RestUtil.constructForwardableURL;


/**
 * This enforces authentication as part of the filter before processing the request.
 * todo: Subclass of {@link AuthenticationFilter}.
 */

@Component
public class AtlasAuthenticationFilter extends AuthenticationFilter {
    private static final Logger LOG = LoggerFactory.getLogger(AtlasAuthenticationFilter.class);

    private   static final int            SESSION_TIMEOUT_DISABLED_VALUE = -1;
    private   static final String         CONFIG_KERBEROS_TOKEN_VALIDITY = "atlas.authentication.method.kerberos.token.validity";
    private   static final String         CONFIG_PROXY_USERS    = "atlas.proxyusers";
    private   static final String         PREFIX                = "atlas.authentication.method";
    private   static final String[]       DEFAULT_PROXY_USERS   = new String[] { "knox" };
    private   static final String         CONF_PROXYUSER_PREFIX = "atlas.proxyuser";
    protected static final ServletContext nullContext           = new NullServletContext();
    private   static final String         ORIGINAL_URL_QUERY_PARAM = "originalUrl";

    private Signer               signer;
    private SignerSecretProvider secretProvider;
    private final boolean        isKerberos = AuthenticationUtil.isKerberosAuthenticationEnabled();
    private boolean              isInitializedByTomcat;
    private Set<Pattern>         browserUserAgents;
    private boolean              supportKeyTabBrowserLogin = false;
    private Configuration        configuration;
    private Properties           headerProperties;
    private Set<String>          atlasProxyUsers = new HashSet<>();
    private HttpServlet          optionsServlet;
    private boolean              supportTrustedProxy = false;
    private int                  sessionTimeout;

    private SecurityContextLogoutHandler logoutHandler;

    public AtlasAuthenticationFilter() {
        LOG.info("==> AtlasAuthenticationFilter()");

        try {
            init(null);
        } catch (ServletException e) {
            LOG.error("Error while initializing AtlasAuthenticationFilter", e);
        }

        LOG.info("<== AtlasAuthenticationFilter()");
    }

    /**
     * Initialize the filter.
     *
     * @param filterConfig filter configuration.
     * @throws ServletException thrown if the filter could not be initialized.
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        LOG.info("==> AtlasAuthenticationFilter.init");

        final FilterConfig        globalConf = filterConfig;
        final Map<String, String> params     = new HashMap<>();
        try {
            configuration = ApplicationProperties.get();
        } catch (Exception e) {
            throw new ServletException(e);
        }

        if (configuration != null) {
            headerProperties = ConfigurationConverter.getProperties(configuration.subset("atlas.headers"));
        }

        String tokenValidityStr = null;

        if(configuration != null) {
            tokenValidityStr = configuration.getString(CONFIG_KERBEROS_TOKEN_VALIDITY);
        }

        if (StringUtils.isNotBlank(tokenValidityStr)) {
            try {
                Long tokenValidity = Long.parseLong(tokenValidityStr);

                if (tokenValidity > 0) {
                    params.put(AuthenticationFilter.AUTH_TOKEN_VALIDITY, tokenValidity.toString());
                } else {
                    throw new ServletException(tokenValidity + ": invalid value for property '" + CONFIG_KERBEROS_TOKEN_VALIDITY + "'. Must be a positive integer");
                }
            } catch (NumberFormatException e) {
                throw new ServletException(tokenValidityStr + ": invalid value for property '" + CONFIG_KERBEROS_TOKEN_VALIDITY + "'. Must be a positive integer", e);
            }
        }

        FilterConfig filterConfig1 = new FilterConfig() {
            @Override
            public ServletContext getServletContext() {
                if (globalConf != null) {
                    return globalConf.getServletContext();
                } else {
                    return nullContext;
                }
            }

            @SuppressWarnings("unchecked")
            @Override
            public Enumeration<String> getInitParameterNames() {
                return new IteratorEnumeration(params.keySet().iterator());
            }

            @Override
            public String getInitParameter(String param) {
                return params.get(param);
            }

            @Override
            public String getFilterName() {
                return "AtlasAuthenticationFilter";
            }
        };

        super.init(filterConfig1);

        ProxyUsers.refreshSuperUserGroupsConfiguration(getProxyuserConfiguration(), CONF_PROXYUSER_PREFIX);

        optionsServlet = new HttpServlet() {
        };
        optionsServlet.init();

        if (sessionTimeout != -1) {
            logoutHandler = new SecurityContextLogoutHandler();
        }

        LOG.info("<== AtlasAuthenticationFilter.init(filterConfig={})", filterConfig);

    }


    @Override
    public void initializeSecretProvider(FilterConfig filterConfig) throws ServletException {
        LOG.info("==> AtlasAuthenticationFilter.initializeSecretProvider");

        secretProvider = (SignerSecretProvider) filterConfig.getServletContext().getAttribute(AuthenticationFilter.SIGNER_SECRET_PROVIDER_ATTRIBUTE);

        if (secretProvider == null) {
            // As tomcat cannot specify the provider object in the configuration.
            // It'll go into this path
            String configPrefix = filterConfig.getInitParameter(CONFIG_PREFIX);

            configPrefix = (configPrefix != null) ? configPrefix + "." : "";

            try {
                secretProvider = AuthenticationFilter.constructSecretProvider(filterConfig.getServletContext(), super.getConfiguration(configPrefix, filterConfig), false);

                this.isInitializedByTomcat = true;
            } catch (Exception ex) {
                throw new ServletException(ex);
            }
        }

        signer = new Signer(secretProvider);

        LOG.info("<== AtlasAuthenticationFilter.initializeSecretProvider(filterConfig={})", filterConfig);
    }

    @Override
    protected Properties getConfiguration(String configPrefix, FilterConfig filterConfig) throws ServletException {
        LOG.info("==> AtlasAuthenticationFilter.getConfiguration()");

        try {
            configuration = ApplicationProperties.get();
        } catch (Exception e) {
            throw new ServletException(e);
        }

        Properties ret = new Properties();

        String kerberosAuthEnabled = configuration != null ? configuration.getString("atlas.authentication.method.kerberos") : null;

        final String authMethod;

        if (kerberosAuthEnabled == null || kerberosAuthEnabled.equalsIgnoreCase("false")) {
            LOG.info("No authentication method configured.  Defaulting to simple authentication");

            authMethod = "simple";
        } else if (kerberosAuthEnabled.equalsIgnoreCase("true")) {
            authMethod = "kerberos";

            if (configuration.getString("atlas.authentication.method.kerberos.name.rules") != null) {
                ret.put("kerberos.name.rules", configuration.getString("atlas.authentication.method.kerberos.name.rules"));
            }

            if (configuration.getString("atlas.authentication.method.kerberos.keytab") != null) {
                ret.put("kerberos.keytab", configuration.getString("atlas.authentication.method.kerberos.keytab"));
            }

            if (configuration.getString("atlas.authentication.method.kerberos.principal") != null) {
                ret.put("kerberos.principal", configuration.getString("atlas.authentication.method.kerberos.principal"));
            }
        } else {
            authMethod = "";
        }

        ret.put(AuthenticationFilter.AUTH_TYPE, authMethod);
        ret.put(AuthenticationFilter.COOKIE_PATH, "/");

        // add any config passed in as init parameters
        Enumeration<String> enumeration = filterConfig.getInitParameterNames();
        while (enumeration.hasMoreElements()) {
            String name = enumeration.nextElement();

            ret.put(name, filterConfig.getInitParameter(name));
        }

        //Resolve _HOST into bind address
        String bindAddress = configuration.getString(SecurityProperties.BIND_ADDRESS);
        if (bindAddress == null) {
            LOG.info("No host name configured. Defaulting to local host name.");

            try {
                bindAddress = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                throw new ServletException("Unable to obtain host name", e);
            }
        }

        String principal = ret.getProperty(KerberosAuthenticationHandler.PRINCIPAL);
        if (principal != null) {
            try {
                principal = SecurityUtil.getServerPrincipal(principal, bindAddress);
            } catch (IOException ex) {
                throw new RuntimeException("Could not resolve Kerberos principal name: " + ex.toString(), ex);
            }

            ret.put(KerberosAuthenticationHandler.PRINCIPAL, principal);
        }


        LOG.debug(" AuthenticationFilterConfig: {}", ret);
        sessionTimeout = AtlasConfiguration.SESSION_TIMEOUT_SECS.getInt();
        LOG.info("AtlasAuthenticationFilter: {} = {}: {}",
                    AtlasConfiguration.SESSION_TIMEOUT_SECS.getPropertyName(), sessionTimeout, 
                    (sessionTimeout == SESSION_TIMEOUT_DISABLED_VALUE) ? "Disabled" : "Enabled");

        supportKeyTabBrowserLogin = configuration.getBoolean("atlas.authentication.method.kerberos.support.keytab.browser.login", false);
        supportTrustedProxy = configuration.getBoolean("atlas.authentication.method.trustedproxy", true);
        String agents = configuration.getString(AtlasCSRFPreventionFilter.BROWSER_USER_AGENT_PARAM, AtlasCSRFPreventionFilter.BROWSER_USER_AGENTS_DEFAULT);

        if (agents == null) {
            agents = AtlasCSRFPreventionFilter.BROWSER_USER_AGENTS_DEFAULT;
        }

        String[] proxyUsers = configuration.getStringArray(CONFIG_PROXY_USERS);

        if (proxyUsers == null || proxyUsers.length == 0) {
            proxyUsers = DEFAULT_PROXY_USERS;
        }

        atlasProxyUsers = new HashSet<>(Arrays.asList(proxyUsers));

        parseBrowserUserAgents(agents);

        LOG.info("<== AtlasAuthenticationFilter.getConfiguration(configPrefix={}, filterConfig={}): {}", configPrefix, filterConfig, ret);

        return ret;
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain filterChain) throws IOException, ServletException {
        final HttpServletRequest httpRequest = (HttpServletRequest) request;

        try {
            Authentication              existingAuth    = SecurityContextHolder.getContext().getAuthentication();
            HttpServletResponse         httpResponse    = (HttpServletResponse) response;
            AtlasResponseRequestWrapper responseWrapper = new AtlasResponseRequestWrapper(httpResponse);
            String action = httpRequest.getParameter("action");
            String doAsUser = request.getParameter("doAs");

            HeadersUtil.setHeaderMapAttributes(responseWrapper, HeadersUtil.X_FRAME_OPTIONS_KEY);
            HeadersUtil.setHeaderMapAttributes(responseWrapper, HeadersUtil.X_CONTENT_TYPE_OPTIONS_KEY);
            HeadersUtil.setHeaderMapAttributes(responseWrapper, HeadersUtil.X_XSS_PROTECTION_KEY);
            HeadersUtil.setHeaderMapAttributes(responseWrapper, HeadersUtil.STRICT_TRANSPORT_SEC_KEY);

            if (headerProperties != null) {
                for (String headerKey : headerProperties.stringPropertyNames()) {
                    responseWrapper.setHeader(headerKey, headerProperties.getProperty(headerKey));
                }
            }

            if (logoutHandler != null && supportTrustedProxy && StringUtils.isNotEmpty(doAsUser) && StringUtils.equals(action, RestUtil.TIMEOUT_ACTION)) {
                if (existingAuth != null) {
                    logoutHandler.logout(httpRequest, httpResponse, existingAuth);
                }
                redirectTimeoutReqeust(httpRequest, httpResponse);
                return;
            }

            if (existingAuth == null) {
                String authHeader = httpRequest.getHeader("Authorization");

                if (authHeader != null && authHeader.startsWith("Basic")) {
                    filterChain.doFilter(request, response);
                } else if (isKerberos) {
                    doKerberosAuth(request, response, filterChain);
                } else {
                    filterChain.doFilter(request, response);
                }
            } else {
                filterChain.doFilter(request, response);
            }
        } catch (NullPointerException e) {
            LOG.error("Exception in AtlasAuthenticationFilter ", e);
            //PseudoAuthenticationHandler.getUserName() from hadoop-auth throws NPE if user name is not specified
            ((HttpServletResponse) response).sendError(Response.Status.BAD_REQUEST.getStatusCode(),
                    "Authentication is enabled and user is not specified. Specify user.name parameter");
        }
    }

    private void redirectTimeoutReqeust(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException{
        String logoutUrl = httpRequest.getRequestURL().toString();

        logoutUrl =  StringUtils.replace(logoutUrl, httpRequest.getRequestURI(), RestUtil.LOGOUT_URL);
        if (LOG.isDebugEnabled()) {
            LOG.debug("logoutUrl value is " + logoutUrl);
        }
        String xForwardedURL = constructForwardableURL(httpRequest);


        if (LOG.isDebugEnabled()) {
            LOG.debug("xForwardedURL = " + xForwardedURL);
        }
        String redirectUrl = RestUtil.constructRedirectURL(httpRequest, logoutUrl, xForwardedURL, ORIGINAL_URL_QUERY_PARAM);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Redirect URL = " + redirectUrl);
            LOG.debug("session id = " + httpRequest.getRequestedSessionId());
        }

        httpResponse.sendRedirect(redirectUrl);
    }


    /**
     * This method is copied from hadoop auth lib, code added for error handling and fallback to other auth methods
     *
     * If the request has a valid authentication token it allows the request to continue to the target resource,
     * otherwise it triggers an authentication sequence using the configured {@link AuthenticationHandler}.
     *
     * @param request     the request object.
     * @param response    the response object.
     * @param filterChain the filter chain object.
     *
     * @throws IOException      thrown if an IO error occurred.
     * @throws ServletException thrown if a processing error occurred.
     */
    private void doKerberosAuth(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        KerberosFilterChainWrapper filterChainWrapper   = new KerberosFilterChainWrapper(request, response, filterChain);
        boolean                    unauthorizedResponse = true;
        int                        errCode              = HttpServletResponse.SC_UNAUTHORIZED;
        AuthenticationException    authenticationEx     = null;
        HttpServletRequest         httpRequest          = (HttpServletRequest) request;
        HttpServletResponse        httpResponse         = (HttpServletResponse) response;
        boolean                    isHttps              = "https".equals(httpRequest.getScheme());
        AuthenticationHandler      authHandler          = getAuthenticationHandler();
        String doAsUser = supportTrustedProxy ? Servlets.getDoAsUser(httpRequest) : null;

        try {
            boolean             newToken = false;
            AuthenticationToken token;

            try {
                token = getToken(httpRequest);
            } catch (AuthenticationException ex) {
                LOG.warn("AuthenticationToken ignored: {}", ex);
                // will be sent back in a 401 unless filter authenticates
                authenticationEx = ex;
                token            = null;
            }

            if (authHandler.managementOperation(token, httpRequest, httpResponse)) {
                if (token == null) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Request [{}] triggering authentication", getRequestURL(httpRequest));
                    }

                    token = authHandler.authenticate(httpRequest, httpResponse);

                    if (token != null && token.getExpires() != 0 && token != AuthenticationToken.ANONYMOUS) {
                        token.setExpires(System.currentTimeMillis() + getValidity() * 1000);
                    }

                    newToken = true;
                }

                if (token != null) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Request [{}] user [{}] authenticated", getRequestURL(httpRequest), token.getUserName());
                    }

                    unauthorizedResponse = false;

                    final AuthenticationToken authToken = token;

                    httpRequest = new HttpServletRequestWrapper(httpRequest) {
                        @Override
                        public String getAuthType() {
                            return authToken.getType();
                        }

                        @Override
                        public String getRemoteUser() {
                            return authToken.getUserName();
                        }

                        @Override
                        public Principal getUserPrincipal() {
                            return (authToken != AuthenticationToken.ANONYMOUS) ? authToken : null;
                        }
                    };

                    // Create the proxy user if doAsUser exists

                    if (supportTrustedProxy && doAsUser != null && !doAsUser.equals(httpRequest.getRemoteUser())) {
                        LOG.debug("doAsUser is {}", doAsUser);

                        UserGroupInformation requestUgi = (token != null) ? UserGroupInformation.createRemoteUser(token.getUserName()) : null;

                        if (requestUgi != null) {
                            requestUgi = UserGroupInformation.createProxyUser(doAsUser, requestUgi);

                            try {
                                ProxyUsers.authorize(requestUgi, request.getRemoteAddr());
                                request.setAttribute("proxyUser", doAsUser);
                            } catch (AuthorizationException ex) {
                                LOG.warn("Proxy user AuthorizationException", ex);

                                httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                filterChain.doFilter(request, response);

                                return;

                            }
                        }
                    } else if(StringUtils.isNotBlank(httpRequest.getRemoteUser()) && atlasProxyUsers.contains(httpRequest.getRemoteUser())){
                        LOG.info("Ignoring kerberos login from proxy user "+ httpRequest.getRemoteUser());

                        httpResponse.setHeader(KerberosAuthenticator.WWW_AUTHENTICATE, "");
                        httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        filterChain.doFilter(request, response);

                        return;
                    }

                    if (newToken && !token.isExpired() && token != AuthenticationToken.ANONYMOUS) {
                        String signedToken = signer.sign(token.toString());

                        createAtlasAuthCookie(httpResponse, signedToken, getCookieDomain(), getCookiePath(), token.getExpires(), isHttps);
                    }

                    filterChainWrapper.doFilter(httpRequest, httpResponse);
                }
            } else {
                unauthorizedResponse = false;
            }
        } catch (AuthenticationException ex) {
            LOG.warn("Authentication exception: {}", ex.getMessage(), ex);

            // exception from the filter itself is fatal
            errCode          = HttpServletResponse.SC_FORBIDDEN;
            authenticationEx = ex;
        }

        if (unauthorizedResponse) {
            if (!httpResponse.isCommitted()) {
                createAtlasAuthCookie(httpResponse, "", getCookieDomain(), getCookiePath(), 0, isHttps);

                // If response code is 401. Then WWW-Authenticate Header should be
                // present.. reset to 403 if not found..
                if (errCode == HttpServletResponse.SC_UNAUTHORIZED && !httpResponse.containsHeader(KerberosAuthenticator.WWW_AUTHENTICATE)) {
                    errCode = HttpServletResponse.SC_FORBIDDEN;
                }

                boolean isKerberosOnBrowser = supportKeyTabBrowserLogin || doAsUser != null;

                if (authenticationEx == null) { // added this code for atlas error handling and fallback
                    if (!isKerberosOnBrowser && isBrowser(httpRequest.getHeader("User-Agent"))) {
                        filterChain.doFilter(request, response);
                    } else {
                        boolean            chk         = true;
                        Collection<String> headerNames = httpResponse.getHeaderNames();

                        for (String headerName : headerNames) {
                            String value = httpResponse.getHeader(headerName);

                            if (headerName.equalsIgnoreCase("Set-Cookie") && value.startsWith("ATLASSESSIONID")) {
                                chk = false;
                                break;
                            }
                        }

                        String authHeader = httpRequest.getHeader("Authorization");

                        if (authHeader == null && chk) {
                            filterChain.doFilter(request, response);
                        } else if (authHeader != null && authHeader.startsWith("Basic")) {
                            filterChain.doFilter(request, response);
                        }
                    }
                } else {
                    httpResponse.sendError(errCode, authenticationEx.getMessage());
                }
            }
        }
    }


    @Override
    public void destroy() {

        if ((this.secretProvider != null) && (this.isInitializedByTomcat)) {
            this.secretProvider.destroy();
            this.secretProvider = null;
        }
        optionsServlet.destroy();
        super.destroy();
    }


    private static String readUserFromCookie(HttpServletResponse response1) {
        String  userName    = null;
        boolean isCookieSet = response1.containsHeader("Set-Cookie");

        if (isCookieSet) {
            Collection<String> authUserName = response1.getHeaders("Set-Cookie");

            if (authUserName != null) {
                for (String cookie : authUserName) {
                    if (!StringUtils.isEmpty(cookie)) {
                        if (cookie.toLowerCase().startsWith(AuthenticatedURL.AUTH_COOKIE.toLowerCase()) && cookie.contains("u=")) {
                            String[] split = cookie.split(";");

                            if (split != null) {
                                for (String s : split) {
                                    if (!StringUtils.isEmpty(s) && s.toLowerCase().startsWith(AuthenticatedURL.AUTH_COOKIE.toLowerCase())) {
                                        int ustr = s.indexOf("u=");

                                        if (ustr != -1) {
                                            int andStr = s.indexOf("&", ustr);

                                            if (andStr != -1) {
                                                try {
                                                    userName = s.substring(ustr + 2, andStr);
                                                    break;
                                                } catch (Exception e) {
                                                    userName = null;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return userName;
    }

    private void createAtlasAuthCookie(HttpServletResponse resp, String token, String domain, String path, long expires, boolean isSecure) {
        StringBuilder sb = (new StringBuilder(AuthenticatedURL.AUTH_COOKIE)).append("=");

        if (token != null && token.length() > 0) {
            sb.append("\"").append(token).append("\"");
        }

        sb.append("; Version=1");

        if (path != null) {
            sb.append("; Path=").append(path);
        }

        if (domain != null) {
            sb.append("; Domain=").append(domain);
        }

        if (expires >= 0L) {
            SimpleDateFormat df = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss zzz");
            df.setTimeZone(TimeZone.getTimeZone("GMT"));
            sb.append("; Expires=").append(df.format(new Date(expires)));
        }

        if (isSecure) {
            sb.append("; Secure");
        }

        sb.append("; HttpOnly");
        resp.addHeader("Set-Cookie", sb.toString());
    }

    @Override
    protected AuthenticationToken getToken(HttpServletRequest request)
            throws IOException, AuthenticationException {
        AuthenticationToken token = null;
        String tokenStr = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(AuthenticatedURL.AUTH_COOKIE)) {
                    tokenStr = cookie.getValue();
                    try {
                        tokenStr = this.signer.verifyAndExtract(tokenStr);
                    } catch (SignerException ex) {
                        throw new AuthenticationException(ex);
                    }
                }
            }
        }

        if (tokenStr != null) {
            token = AuthenticationToken.parse(tokenStr);
            if (token != null) {
                AuthenticationHandler authHandler = getAuthenticationHandler();
                if (!token.getType().equals(authHandler.getType())) {
                    throw new AuthenticationException("Invalid AuthenticationToken type");
                }
                if (token.isExpired()) {
                    throw new AuthenticationException("AuthenticationToken expired");
                }
            }
        }
        return token;
    }

    void parseBrowserUserAgents(String userAgents) {
        String[] agentsArray = userAgents.split(",");
        browserUserAgents = new HashSet<>();
        for (String patternString : agentsArray) {
            browserUserAgents.add(Pattern.compile(patternString));
        }
    }

    boolean isBrowser(String userAgent) {
        if (userAgent != null) {
            for (Pattern pattern : browserUserAgents) {
                Matcher matcher = pattern.matcher(userAgent);

                if (matcher.matches()) {
                    return true;
                }
            }
        }

        return false;
    }


    private class KerberosFilterChainWrapper implements FilterChain {
        private final ServletRequest  request;
        private final ServletResponse response;
        private final FilterChain     filterChain;

        KerberosFilterChainWrapper(ServletRequest request, ServletResponse response, FilterChain filterChain) {
            this.request     = request;
            this.response    = response;
            this.filterChain = filterChain;
        }

        @Override
        public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
            final HttpServletRequest  httpRequest  = (HttpServletRequest) servletRequest;
            final HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
            final Authentication      existingAuth = SecurityContextHolder.getContext().getAuthentication();
            String                    loggedInUser = readUserFromCookie(httpResponse);
            String                    userName     = loggedInUser;

            if (!StringUtils.isEmpty((String) httpRequest.getAttribute("proxyUser"))) {
                userName = (String) httpRequest.getAttribute("proxyUser");
            } else if (StringUtils.isEmpty(userName) && !StringUtils.isEmpty(httpRequest.getRemoteUser())) {
                userName = httpRequest.getRemoteUser();
            }

            if ((existingAuth == null || !existingAuth.isAuthenticated()) && !StringUtils.isEmpty(userName)) {
                final List<GrantedAuthority>   grantedAuths        = AtlasAuthenticationProvider.getAuthoritiesFromUGI(userName);
                final UserDetails              principal           = new User(userName, "", grantedAuths);
                final Authentication           finalAuthentication = new UsernamePasswordAuthenticationToken(principal, "", grantedAuths);
                final WebAuthenticationDetails webDetails          = new WebAuthenticationDetails(httpRequest);

                ((AbstractAuthenticationToken) finalAuthentication).setDetails(webDetails);

                SecurityContextHolder.getContext().setAuthentication(finalAuthentication);
                if (sessionTimeout != SESSION_TIMEOUT_DISABLED_VALUE) {
                    httpRequest.getSession().setMaxInactiveInterval(sessionTimeout);
                }

                request.setAttribute("atlas.http.authentication.type", true);

                if (!StringUtils.equals(loggedInUser, userName)) {
                    LOG.info("Logged into Atlas as = {}, by proxyUser = {}", userName, loggedInUser);
                } else {
                    LOG.info("Logged into Atlas as = {}", userName);
                }
            }

            // OPTIONS method is sent from quick start jersey atlas client
            if (httpRequest.getMethod().equals("OPTIONS")) {
                optionsServlet.service(request, response);
            } else {
                String requestUser = httpRequest.getRemoteUser();

                LOG.info("Request from authenticated user: {}, URL={}", requestUser, Servlets.getRequestURI(httpRequest));

                filterChain.doFilter(servletRequest, servletResponse);
            }
        }
    }

    private org.apache.hadoop.conf.Configuration getProxyuserConfiguration() {
        org.apache.hadoop.conf.Configuration ret  = new org.apache.hadoop.conf.Configuration(false);

        if(configuration!=null) {
            Properties props = ConfigurationConverter.getProperties(configuration.subset(CONF_PROXYUSER_PREFIX));

            for (String key : props.stringPropertyNames()) {
                ret.set(CONF_PROXYUSER_PREFIX + "." + key, props.getProperty(key));
            }
        }

        return ret;
    }
}
