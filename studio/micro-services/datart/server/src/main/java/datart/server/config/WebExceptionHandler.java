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

import datart.core.common.MessageResolver;
import datart.core.common.RequestContext;
import datart.core.data.provider.Dataframe;
import datart.security.exception.AuthException;
import datart.server.base.dto.ResponseData;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class WebExceptionHandler {

    @ResponseBody
    @ResponseStatus(code = HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(JwtException.class)
    public ResponseData<String> exceptionHandler(JwtException e) {
        ResponseData.ResponseDataBuilder<String> builder = ResponseData.builder();
        return builder.success(false)
                .message(MessageResolver.getMessage("login.session.timeout"))
                .build();
    }

    @ResponseBody
    @ResponseStatus(code = HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(AuthException.class)
    public ResponseData<String> exceptionHandler(AuthException e) {
        ResponseData.ResponseDataBuilder<String> builder = ResponseData.builder();
        return builder.success(false)
                .message(e.getMessage())
                .errCode(e.getErrCode())
                .build();
    }

    @ResponseBody
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BindException.class)
    public ResponseData<String> validateException(BindException e) {
        ResponseData.ResponseDataBuilder<String> builder = ResponseData.builder();
        return builder.success(false)
                .message(e.getBindingResult().getFieldErrors().stream()
                        .map(error -> error.getField() + ":" + error.getDefaultMessage())
                        .collect(Collectors.toList()).toString())
                .build();
    }

    @ResponseBody
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(Exception.class)
    public ResponseData<Object> exceptionHandler(Exception e) {
        Object data = null;
        if (RequestContext.getScriptPermission() != null) {
            Dataframe df = Dataframe.empty();
            if (RequestContext.getScriptPermission()) {
                df.setScript(RequestContext.getSql());
            }
            data = df;
        }

        String msg = null;
        msg = e.getMessage();
        if (msg == null) {
            Throwable cause = e.getCause();
            if (cause != null) {
                msg = cause.getMessage();
            }
        }
        log.error(msg, e);
        ResponseData.ResponseDataBuilder<Object> builder = ResponseData.builder();
        return builder.success(false)
                .message(msg)
                .data(data)
                .build();
    }

}
