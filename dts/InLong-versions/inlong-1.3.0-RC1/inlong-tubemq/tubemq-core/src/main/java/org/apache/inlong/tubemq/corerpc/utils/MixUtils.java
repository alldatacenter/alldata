/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.corerpc.utils;

import java.lang.reflect.Constructor;
import org.apache.inlong.tubemq.corebase.TokenConstants;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.corerpc.exception.RemoteException;
import org.apache.inlong.tubemq.corerpc.protocol.RpcProtocol;

public class MixUtils {

    /**
     * Substitute class name prefix
     *
     * After TubeMQ was donated to Apache, the package prefix name was not migrated to
     * the name of apache for a long time, and some errors on the server side were returned
     * to the client by throwing exceptions.
     * In order to maintain the compatibility between the previous and previous versions,
     * the the class name in exception information is replaced through this function
     *
     * @param className        the class name
     * @param toOldVersion     whether the client is old version
     * @param toProtocolVer     the client's protocol version
     * @return                 the translated class name
     */
    public static String replaceClassNamePrefix(String className,
                                                boolean toOldVersion,
                                                int toProtocolVer) {

        if (toProtocolVer == RpcProtocol.RPC_PROTOCOL_VERSION_OLD_1) {
            if (toOldVersion) {
                return className.replace("org.apache.inlong.tubemq.", "com.tencent.tubemq.");
            } else {
                return className.replace("com.tencent.tubemq.", "org.apache.inlong.tubemq.");
            }
        } else if (toProtocolVer == RpcProtocol.RPC_PROTOCOL_VERSION_TUBEMQ) {
            if (toOldVersion) {
                return className.replace("org.apache.inlong.tubemq.", "com.apache.tubemq.");
            } else {
                return className.replace("com.apache.tubemq.", "org.apache.inlong.tubemq.");
            }
        } else {
            return className;
        }
    }

    /**
     * Construct the corresponding exception object according to the exception text
     *
     * @param exceptionMsg     the exception text
     * @return                 the exception object
     */
    public static Throwable unwrapException(String exceptionMsg) {
        // Perform string to exception conversion processing
        try {
            String[] strExceptionMsgSet =
                    exceptionMsg.split(TokenConstants.SEGMENT_SEP);
            if (strExceptionMsgSet.length > 0) {
                if (!TStringUtils.isBlank(strExceptionMsgSet[0])) {
                    Class clazz = Class.forName(strExceptionMsgSet[0]);
                    if (clazz != null) {
                        Constructor<?> ctor = clazz.getConstructor(String.class);
                        if (ctor != null) {
                            if (strExceptionMsgSet.length == 1) {
                                return (Throwable) ctor.newInstance();
                            } else {
                                if (strExceptionMsgSet[0]
                                        .equalsIgnoreCase("java.lang.NullPointerException")) {
                                    return new NullPointerException("remote return null");
                                } else {
                                    if ((strExceptionMsgSet[1] == null)
                                            || (TStringUtils.isBlank(strExceptionMsgSet[1]))
                                            || (strExceptionMsgSet[1].equalsIgnoreCase("null"))) {
                                        return (Throwable) ctor.newInstance(
                                                "Exception with null StackTrace content");
                                    } else {
                                        return (Throwable) ctor.newInstance(strExceptionMsgSet[1]);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable e) {
            //
        }
        return new RemoteException(exceptionMsg);
    }

}
