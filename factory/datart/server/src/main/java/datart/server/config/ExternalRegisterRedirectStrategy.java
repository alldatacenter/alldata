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

import datart.core.base.consts.Const;
import datart.core.base.exception.Exceptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class ExternalRegisterRedirectStrategy {

    private static final String redirectUrl = "/authorization";

    public void redirect(HttpServletRequest request, HttpServletResponse response, String token) throws Exception {
        String target = redirectUrl + "?authorization_token=" + URLEncoder.encode(token, StandardCharsets.UTF_8.name());
        response.setHeader(Const.TOKEN, token);
        response.sendRedirect(target);
    }

    public void redirectError(HttpServletRequest request, HttpServletResponse response, String message) {
        try {
            String target = redirectUrl + "?error_message=" + URLEncoder.encode(message, StandardCharsets.UTF_8.name());
            response.sendRedirect(target);
        } catch (IOException e) {
            Exceptions.e(e);
        }
    }
}
