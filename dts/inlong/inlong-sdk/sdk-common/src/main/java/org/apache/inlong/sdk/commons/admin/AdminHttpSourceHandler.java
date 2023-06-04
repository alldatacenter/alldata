/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sdk.commons.admin;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.flume.Event;
import org.apache.flume.conf.Configurable;
import org.apache.flume.source.http.HTTPBadRequestException;

/**
 * 
 * IAdminHttpSourceHandler
 */
public interface AdminHttpSourceHandler extends Configurable {

    /**
     * Takes an {@linkplain HttpServletRequest} and returns a list of Flume Events. If this request cannot be parsed
     * into Flume events based on the format this method will throw an exception. This method may also throw an
     * exception if there is some sort of other error.
     * <p>
     *
     * @param  request                 The request to be parsed into Flume events.
     * @param  response                The response to be parsed into Flume events.
     * @return                         List of Flume events generated from the request.
     * @throws HTTPBadRequestException If the was not parsed correctly into an event because the request was not in the
     *                                 expected format.
     * @throws Exception               If there was an unexpected error.
     */
    List<Event> getEvents(HttpServletRequest request, HttpServletResponse response)
            throws HTTPBadRequestException, Exception;
}
