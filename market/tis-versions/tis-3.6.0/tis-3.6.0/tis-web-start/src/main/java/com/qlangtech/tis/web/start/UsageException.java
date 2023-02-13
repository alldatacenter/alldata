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

/**
 * A Usage Error has occurred. Print the usage and exit with the appropriate exit code.
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/09/25
 */
@SuppressWarnings("serial")
public class UsageException extends RuntimeException {

    public static final int ERR_LOGGING = -1;

    public static final int ERR_INVOKE_MAIN = -2;

    public static final int ERR_NOT_STOPPED = -4;

    public static final int ERR_BAD_ARG = -5;

    public static final int ERR_BAD_GRAPH = -6;

    public static final int ERR_BAD_STOP_PROPS = -7;

    public static final int ERR_UNKNOWN = -9;

    private int exitCode;

    public UsageException(int exitCode, String message) {
        super(message);
        this.exitCode = exitCode;
    }

    public UsageException(int exitCode, String format, Object... objs) {
        super(String.format(format, objs));
        this.exitCode = exitCode;
    }

    public UsageException(String format, Object... objs) {
        this(ERR_UNKNOWN, format, objs);
    }

    public UsageException(int exitCode, Throwable cause) {
        super(cause);
        this.exitCode = exitCode;
    }

    public int getExitCode() {
        return exitCode;
    }
}
