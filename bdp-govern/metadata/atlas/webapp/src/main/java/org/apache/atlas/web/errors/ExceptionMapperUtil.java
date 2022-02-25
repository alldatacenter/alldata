/**
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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.web.errors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExceptionMapperUtil {
    protected static final Logger LOGGER = LoggerFactory.getLogger(ExceptionMapperUtil.class);

    @SuppressWarnings("UnusedParameters")
    protected static String formatErrorMessage(long id, Exception exception) {
        return String.format("There was an error processing your request. It has been logged (ID %016x).", id);
    }

    protected static void logException(long id, Exception exception) {
        LOGGER.error(formatLogMessage(id, exception), exception);
    }

    @SuppressWarnings("UnusedParameters")
    protected static String formatLogMessage(long id, Throwable exception) {
        return String.format("Error handling a request: %016x", id);
    }

}
