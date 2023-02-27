/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.exception;

import org.apache.atlas.AtlasErrorCode;

import java.util.List;

/**
 * Base Exception class for Atlas API.
 */
public class AtlasBaseException extends Exception {

    private AtlasErrorCode atlasErrorCode;

    public AtlasBaseException(AtlasErrorCode errorCode, String ... params) {
        super(errorCode.getFormattedErrorMessage(params));
        this.atlasErrorCode = errorCode;
    }

    public AtlasBaseException(final AtlasErrorCode errorCode, final List<String> params) {
        super(errorCode.getFormattedErrorMessage(params.toArray(new String[params.size()])));
        this.atlasErrorCode = errorCode;
    }

    public AtlasBaseException() {
        this(AtlasErrorCode.INTERNAL_ERROR);
    }

    public AtlasBaseException(String message) {
        super(message);
        this.atlasErrorCode = AtlasErrorCode.INTERNAL_ERROR;
    }

    public AtlasBaseException(AtlasErrorCode errorCode, Throwable cause, String... params) {
        super(errorCode.getFormattedErrorMessage(params), cause);
        this.atlasErrorCode = errorCode;
    }

    public AtlasBaseException(String message, Throwable cause) {
        super(message, cause);
        this.atlasErrorCode = AtlasErrorCode.INTERNAL_ERROR;
    }

    public AtlasBaseException(Throwable cause) {
        super(cause);
        this.atlasErrorCode = AtlasErrorCode.INTERNAL_ERROR;
    }

    public AtlasBaseException(AtlasErrorCode errorCode, Throwable cause, boolean enableSuppression,
                              boolean writableStackTrace, String ... params) {
        super(errorCode.getFormattedErrorMessage(params), cause, enableSuppression, writableStackTrace);
        this.atlasErrorCode = AtlasErrorCode.INTERNAL_ERROR;
    }

    public AtlasBaseException(String message, Throwable cause, boolean enableSuppression,
                              boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.atlasErrorCode = AtlasErrorCode.INTERNAL_ERROR;
    }

    public AtlasBaseException(final AtlasErrorCode errorCode, Throwable cause, final List<String> params) {
        super(errorCode.getFormattedErrorMessage(params.toArray(new String[params.size()])), cause);
        this.atlasErrorCode = errorCode;
    }

    public AtlasErrorCode getAtlasErrorCode() {
        return atlasErrorCode;
    }
}
