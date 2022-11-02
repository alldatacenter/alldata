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

package org.apache.atlas.web.params;

import org.apache.atlas.exception.AtlasBaseException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * A parameter encapsulating date/time values. All non-parsable values will return a {@code 400 Bad
 * Request} response. All values returned are in UTC.
 */
public class DateTimeParam extends AbstractParam<DateTime> {

    public DateTimeParam(String input) {
        super(input);
    }

    @Override
    protected DateTime parse(String input) throws AtlasBaseException {
            return new DateTime(input, DateTimeZone.UTC);
    }
}