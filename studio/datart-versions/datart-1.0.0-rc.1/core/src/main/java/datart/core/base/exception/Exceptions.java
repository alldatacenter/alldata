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

package datart.core.base.exception;

import datart.core.common.MessageResolver;

import java.lang.reflect.Constructor;

public class Exceptions {

    public static void msg(String msg, String... code) {
        tr(BaseException.class, msg, code);
    }

    public static void base(String msg) {
        throw new BaseException(msg);
    }

    public static void notFound(String... msg) {
        tr(NotFoundException.class, "base.not.exists", msg);
    }

    public static void exists(String... msg) {
        tr(ParamException.class, "base.not.exists", msg);
    }

    public static void e(Exception e) {
        throw new BaseException(e);
    }

    public static void tr(Class<? extends BaseException> clz, String messageCode, String... codes) throws RuntimeException {
        BaseException throwable;
        try {
            String message = MessageResolver.getMessages(messageCode, (Object[]) codes);
            Constructor<? extends BaseException> constructor = clz.getConstructor(String.class);
            throwable = constructor.newInstance(message);
        } catch (Exception e) {
            throwable = new BaseException(messageCode);
        }
        throw throwable;
    }


}
