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

package datart.server.config;

import datart.server.service.ExternalRegisterService;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class Oauth2AuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final ExternalRegisterService externalRegisterService;

    private final ExternalRegisterRedirectStrategy registerRedirectStrategy;

    public Oauth2AuthenticationSuccessHandler(ExternalRegisterService externalRegisterService,
                                              ExternalRegisterRedirectStrategy registerRedirectStrategy) {
        this.externalRegisterService = externalRegisterService;
        this.registerRedirectStrategy = registerRedirectStrategy;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws ServletException, IOException {
        try {
            String token = externalRegisterService.oauth2Register((OAuth2AuthenticationToken) authentication);
            registerRedirectStrategy.redirect(request, response, token);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            registerRedirectStrategy.redirectError(request, response, e.getMessage());
        }
    }

}
