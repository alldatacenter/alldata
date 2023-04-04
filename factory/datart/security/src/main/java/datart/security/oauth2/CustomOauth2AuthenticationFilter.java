/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package datart.security.oauth2;

import datart.core.base.exception.Exceptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
public class CustomOauth2AuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    private final static String processUrl = "/login/oauth2/code/{" + CustomOauth2Client.REGISTRATION_ID + "}";

    private final ClientRegistrationRepository clientRegistrationRepository;

    private final AntPathRequestMatcher authorizationRequestMatcher = new AntPathRequestMatcher(processUrl);

    public CustomOauth2AuthenticationFilter(ClientRegistrationRepository clientRegistrationRepository, AuthenticationSuccessHandler authenticationSuccessHandler) {
        super(processUrl);
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.setAuthenticationSuccessHandler(authenticationSuccessHandler);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, IOException, ServletException {
        String registrationId = resolveRegistrationId(request);
        if (registrationId != null && CustomOauth2Client.CUSTOM_OAUTH2_CLIENTS.contains(registrationId)) {
            if (DingTalkOauth2Client.REGISTRATION_ID.equals(registrationId)) {
                ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(registrationId);
                DingTalkOauth2Client oauth2Client = new DingTalkOauth2Client(clientRegistration);
                return oauth2Client.getUserInfo(request, response);
            }
        }
        Exceptions.msg("oauth2 authentication error");
        return null;
    }

    @Override
    protected boolean requiresAuthentication(HttpServletRequest request, HttpServletResponse response) {
        return CustomOauth2Client.CUSTOM_OAUTH2_CLIENTS.contains(resolveRegistrationId(request));
    }

    private String resolveRegistrationId(HttpServletRequest request) {
        if (this.authorizationRequestMatcher.matches(request)) {
            return this.authorizationRequestMatcher.matcher(request).getVariables()
                    .get(CustomOauth2Client.REGISTRATION_ID);
        }
        return null;
    }
}
