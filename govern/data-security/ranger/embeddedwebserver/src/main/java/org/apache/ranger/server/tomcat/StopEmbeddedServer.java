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

 package org.apache.ranger.server.tomcat;

import java.io.PrintWriter;
import java.net.Socket;

public class StopEmbeddedServer extends EmbeddedServer {

	private static final String SHUTDOWN_HOSTNAME = "localhost";
	
	public static void main(String[] args) {
		new StopEmbeddedServer(args).stop();
	}

	public StopEmbeddedServer(String[] args) {
		super(args);
	}
	
	public void stop() {
		
		try {

			int shutdownPort = EmbeddedServerUtil.getIntConfig("ranger.service.shutdown.port", DEFAULT_SHUTDOWN_PORT );
			String shutdownCommand = EmbeddedServerUtil.getConfig("ranger.service.shutdown.command", DEFAULT_SHUTDOWN_COMMAND );

			Socket sock = new Socket(SHUTDOWN_HOSTNAME,shutdownPort);
			
			PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
			
			out.println(shutdownCommand);
			
			out.flush();
			
			out.close();
		}
		catch(Throwable t) {
			System.err.println("Server could not be shutdown due to exception:" +  t);
			System.exit(1);
		}
	}
	
}
