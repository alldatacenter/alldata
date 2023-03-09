/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

package org.apache.griffin.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@SuppressWarnings("serial")
public abstract class GriffinException extends RuntimeException {

    GriffinException(String message) {
        super(message);
    }

    GriffinException(String message, Throwable cause) {
        super(message, cause);
    }

    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    public static class NotFoundException extends GriffinException {
        public NotFoundException(GriffinExceptionMessage message) {
            super(message.toString());
        }
    }

    @ResponseStatus(value = HttpStatus.CONFLICT)
    public static class ConflictException extends GriffinException {
        public ConflictException(GriffinExceptionMessage message) {
            super(message.toString());
        }
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public static class BadRequestException extends GriffinException {
        public BadRequestException(GriffinExceptionMessage message) {
            super(message.toString());
        }
    }

    public static class ServiceException extends GriffinException {
        public ServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class UnImplementedException extends GriffinException {
        public UnImplementedException(String message) {
            super(message);
        }
    }
}
