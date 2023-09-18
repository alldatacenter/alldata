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

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


public class CustomOAuth2AuthorizationRequestRedirectFilter extends OncePerRequestFilter {

    private final ClientRegistrationRepository clientRegistrationRepository;

    private final AntPathRequestMatcher authorizationRequestMatcher = new AntPathRequestMatcher(
            OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI + "/{" + CustomOauth2Client.REGISTRATION_ID + "}");

    public CustomOAuth2AuthorizationRequestRedirectFilter(ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        ClientRegistration registration = getCustomOauth2Registration(request);
        if (registration != null) {
            if (DingTalkOauth2Client.REGISTRATION_ID.equals(registration.getRegistrationId())) {
                DingTalkOauth2Client oauth2Client = new DingTalkOauth2Client(registration);
                oauth2Client.authorizationRequest(request, response);
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private String resolveRegistrationId(HttpServletRequest request) {
        if (this.authorizationRequestMatcher.matches(request)) {
            return this.authorizationRequestMatcher.matcher(request).getVariables()
                    .get(CustomOauth2Client.REGISTRATION_ID);
        }
        return null;
    }

    private ClientRegistration getCustomOauth2Registration(HttpServletRequest request) {
        String registrationId = resolveRegistrationId(request);
        if (registrationId != null && CustomOauth2Client.CUSTOM_OAUTH2_CLIENTS.contains(registrationId)) {
            return clientRegistrationRepository.findByRegistrationId(registrationId);
        }
        return null;
    }
}
