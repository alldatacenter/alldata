/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.web.start;

import static com.qlangtech.tis.web.start.UsageException.ERR_BAD_STOP_PROPS;
import static com.qlangtech.tis.web.start.UsageException.ERR_NOT_STOPPED;
import static com.qlangtech.tis.web.start.UsageException.ERR_UNKNOWN;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/09/25
 */
public class TriggerStop {

    private static final Logger logger = LoggerFactory.getLogger(TriggerStop.class);

    /**
     * 是否是终止命令
     *
     * @param args
     * @return
     */
    public static boolean isStopCommand(String[] args) {
        for (String arg : args) {
            if ("--stop".equals(arg)) {
                return true;
            }
        }
        return false;
    }

    public static void stop(String host, int port, String key, int timeout) {
        if (host == null || host.length() == 0) {
            host = "127.0.0.1";
        }
        try {
            if ((port <= 0) || (port > 65535)) {
                System.err.println("STOP.PORT property must be specified with a valid port number");
                usageExit(ERR_BAD_STOP_PROPS);
            }
            if (key == null) {
                key = "";
                System.err.println("STOP.KEY property must be specified");
                System.err.println("Using empty key");
                usageExit(ERR_BAD_STOP_PROPS);
            }
            try (Socket s = new Socket(InetAddress.getByName(host), port)) {
                if (timeout > 0) {
                    s.setSoTimeout(timeout * 1000);
                }
                try (OutputStream out = s.getOutputStream()) {
                    out.write((key + "\r\nstop\r\n").getBytes());
                    out.flush();
                    if (timeout > 0) {
                        logger.info("Waiting %,d seconds for jetty to stop%n", timeout);
                        LineNumberReader lin = new LineNumberReader(new InputStreamReader(s.getInputStream()));
                        String response;
                        while ((response = lin.readLine()) != null) {
                            logger.info("Received \"%s\"", response);
                            if ("Stopped".equals(response)) {
                                logger.warn("Server reports itself as Stopped");
                            }
                        }
                    }
                }
            }
        } catch (SocketTimeoutException e) {
            logger.warn("Timed out waiting for stop confirmation");
            System.exit(ERR_UNKNOWN);
        } catch (ConnectException e) {
            usageExit(e, ERR_NOT_STOPPED, false);
        } catch (Exception e) {
            usageExit(e, ERR_UNKNOWN, false);
        }
    }

    static void usageExit(int exit) {
        usageExit(null, exit, false);
    }

    static void usageExit(Throwable t, int exit, boolean test) {
        if (t != null) {
            t.printStackTrace(System.err);
        }
        System.err.println();
        System.err.println("Usage: java -jar $JETTY_HOME/start.jar [options] [properties] [configs]");
        System.err.println("       java -jar $JETTY_HOME/start.jar --help  # for more information");
        if (test)
            System.err.println("EXIT: " + exit);
        else
            System.exit(exit);
    }

    public static void main(String[] args) {
    }
}
