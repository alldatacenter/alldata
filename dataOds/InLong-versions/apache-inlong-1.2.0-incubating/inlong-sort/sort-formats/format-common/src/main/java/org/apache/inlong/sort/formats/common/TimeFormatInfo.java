/*
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

package org.apache.inlong.sort.formats.common;

import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;

import static org.apache.inlong.sort.formats.common.Constants.DATE_AND_TIME_STANDARD_ISO_8601;
import static org.apache.inlong.sort.formats.common.Constants.DATE_AND_TIME_STANDARD_SQL;

/**
 * The format information for {@link Time}s.
 */
public class TimeFormatInfo implements BasicFormatInfo<Time> {

    private static final long serialVersionUID = 1L;

    private static final String FIELD_FORMAT = "format";

    @JsonProperty(FIELD_FORMAT)
    @Nonnull
    private final String format;

    @JsonIgnore
    @Nullable
    private final SimpleDateFormat simpleDateFormat;

    @JsonCreator
    public TimeFormatInfo(
            @JsonProperty(FIELD_FORMAT) @Nonnull String format
    ) {
        this.format = format;

        if (!format.equals("MICROS")
                && !format.equals("MILLIS")
                && !format.equals("SECONDS")
                && !DATE_AND_TIME_STANDARD_SQL.equals(format)
                && !DATE_AND_TIME_STANDARD_ISO_8601.equals(format)) {
            this.simpleDateFormat = new SimpleDateFormat(format);
        } else {
            this.simpleDateFormat = null;
        }
    }

    public TimeFormatInfo() {
        this("HH:mm:ss");
    }

    @Nonnull
    public String getFormat() {
        return format;
    }

    @Override
    public TimeTypeInfo getTypeInfo() {
        return TimeTypeInfo.INSTANCE;
    }

    @Override
    public String serialize(Time time) {
        switch (format) {
            case "MICROS": {
                long millis = time.getTime();
                long micros = TimeUnit.MILLISECONDS.toMicros(millis);
                return Long.toString(micros);
            }
            case "MILLIS": {
                long millis = time.getTime();
                return Long.toString(millis);
            }
            case "SECONDS": {
                long millis = time.getTime();
                long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
                return Long.toString(seconds);
            }
            default: {
                if (simpleDateFormat == null) {
                    throw new IllegalStateException();
                }

                return simpleDateFormat.format(time);
            }
        }
    }

    @Override
    public Time deserialize(String text) throws ParseException {
        switch (format) {
            case "MICROS": {
                long micros = Long.parseLong(text);
                long millis = TimeUnit.MICROSECONDS.toMillis(micros);
                return new Time(millis);
            }
            case "MILLIS": {
                long millis = Long.parseLong(text);
                return new Time(millis);
            }
            case "SECONDS": {
                long seconds = Long.parseLong(text);
                long millis = TimeUnit.SECONDS.toMillis(seconds);
                return new Time(millis);
            }
            default: {
                if (simpleDateFormat == null) {
                    throw new IllegalStateException();
                }

                Date date = simpleDateFormat.parse(text);
                return new Time(date.getTime());
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TimeFormatInfo that = (TimeFormatInfo) o;
        return format.equals(that.format);
    }

    @Override
    public int hashCode() {
        return Objects.hash(format);
    }

    @Override
    public String toString() {
        return "TimeFormatInfo{" + "format='" + format + '\'' + '}';
    }
}
