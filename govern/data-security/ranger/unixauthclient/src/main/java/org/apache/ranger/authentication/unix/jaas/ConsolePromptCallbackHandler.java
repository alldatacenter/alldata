/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

 package org.apache.ranger.authentication.unix.jaas;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

public class ConsolePromptCallbackHandler implements CallbackHandler {


	@Override
	public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		
		for(Callback cb : callbacks) {
			if (cb instanceof NameCallback) {
		          NameCallback nc = (NameCallback)cb;
		          System.out.print(nc.getPrompt());
		          System.out.flush();
                  String line = null;

                  while (line == null) {
                    line = reader.readLine();
                  }
		          nc.setName(line);
			}
			else if (cb instanceof PasswordCallback) {
		          PasswordCallback pc = (PasswordCallback)cb;
		          System.out.print(pc.getPrompt());
		          System.out.flush();

                  String line = null;
                  while (line == null) {
                    line = reader.readLine();
                  }
		          pc.setPassword(line.toCharArray());
			}
			else {
				System.out.println("Unknown callback [" + cb.getClass().getName() + "]");
			}
		}
	}

}
