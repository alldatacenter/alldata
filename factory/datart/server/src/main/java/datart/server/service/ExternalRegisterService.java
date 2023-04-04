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
package datart.server.service;

import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

import javax.mail.MessagingException;
import java.io.UnsupportedEncodingException;

public interface ExternalRegisterService {

    String ldapRegister(String filter, String password) throws MessagingException, UnsupportedEncodingException;

    String oauth2Register(OAuth2AuthenticationToken oauthAuthToken) throws MessagingException, UnsupportedEncodingException;

}