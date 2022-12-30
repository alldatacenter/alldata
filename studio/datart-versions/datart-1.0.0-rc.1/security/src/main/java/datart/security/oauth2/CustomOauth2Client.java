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

import com.google.common.collect.Sets;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Set;

public interface CustomOauth2Client {

    String NAME = "username";

    String EMAIL = "email";

    String AVATAR = "avatar";

    String REGISTRATION_ID = "registrationId";

    Set<String> CUSTOM_OAUTH2_CLIENTS = Sets.newHashSet(DingTalkOauth2Client.REGISTRATION_ID
            , WeChartOauth2Client.REGISTRATION_ID);

    void authorizationRequest(HttpServletRequest request, HttpServletResponse response);

    OAuth2AuthenticationToken getUserInfo(HttpServletRequest request, HttpServletResponse response);

}
