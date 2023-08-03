/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ranger.authentication.unix.jaas;

import javax.security.auth.callback.*;
import java.io.IOException;

public class UsernamePasswordCallbackHandler implements CallbackHandler {
    private String _user;
    private String _password;

    public UsernamePasswordCallbackHandler(String user, String password) {
        super();
        _user = user;
        _password = password;
    }

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (Callback callback : callbacks) {
            if (callback instanceof NameCallback) {
                handleName((NameCallback) callback);
            } else if (callback instanceof PasswordCallback) {
                handlePassword((PasswordCallback) callback);
            } else {
                throw new UnsupportedCallbackException(callback);
            }
        }
    }

    private void handleName(NameCallback callback) {
        callback.setName(_user);
    }

    private void handlePassword(PasswordCallback callback) {
        char[] passwordChars = _password.toCharArray();
        callback.setPassword(passwordChars);
    }
}

