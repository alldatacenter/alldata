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

package datart.core.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class MessageResolver {

    private static MessageSource messageSource;

    public MessageResolver() {
    }

    @Autowired
    public void setMessageSource(MessageSource messageSource) {
        MessageResolver.messageSource = messageSource;
    }

    public static String getMessage(Object code) {
        return messageSource.getMessage(code.toString(), null, code.toString(), LocaleContextHolder.getLocale());
    }

//    public static String getMessage(String code, Object... args) {
//        return messageSource.getMessage(code, args, code, LocaleContextHolder.getLocale());
//    }

    public static String getMessages(Object code, Object... messageCodes) {
        Object[] objs = Arrays.stream(messageCodes).map(MessageResolver::getMessage).toArray();
        return messageSource.getMessage(code.toString(), objs, code.toString(), LocaleContextHolder.getLocale());
//        return getMessage(code, objs);
    }
}
