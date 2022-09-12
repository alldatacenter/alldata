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

import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
 * The format information for {@link Date}s.
 */
public class DateFormatInfo implements BasicFormatInfo<Date> {

    private static final long serialVersionUID = 1L;

    private static final String FIELD_FORMAT = "format";

    @JsonProperty(FIELD_FORMAT)
    @Nonnull
    private final String format;

    @JsonIgnore
    @Nullable
    private final SimpleDateFormat simpleDateFormat;

    @JsonCreator
    public DateFormatInfo(
            @JsonProperty(FIELD_FORMAT) @Nonnull String format
    ) {
        this.format = format;

        if (!format.equals("SECONDS")
                && !format.equals("MILLIS")
                && !format.equals("MICROS")
                && !DATE_AND_TIME_STANDARD_SQL.equals(format)
                && !DATE_AND_TIME_STANDARD_ISO_8601.equals(format)) {
            this.simpleDateFormat = new SimpleDateFormat(format);
        } else {
            this.simpleDateFormat = null;
        }
    }

    public DateFormatInfo() {
        this("yyyy-MM-dd");
    }

    @Nonnull
    public String getFormat() {
        return format;
    }

    @Override
    public DateTypeInfo getTypeInfo() {
        return DateTypeInfo.INSTANCE;
    }

    @Override
    public String serialize(Date date) {
        switch (format) {
            case "MICROS": {
                long millis = date.getTime();
                long micros = TimeUnit.MILLISECONDS.toMicros(millis);
                return Long.toString(micros);
            }
            case "MILLIS": {
                long millis = date.getTime();
                return Long.toString(millis);
            }
            case "SECONDS": {
                long millis = date.getTime();
                long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
                return Long.toString(seconds);
            }
            default: {
                if (simpleDateFormat == null) {
                    throw new IllegalStateException();
                }

                return simpleDateFormat.format(date);
            }
        }
    }

    @Override
    public Date deserialize(String text) throws ParseException {

        switch (format) {
            case "MICROS": {
                long micros = Long.parseLong(text.trim());
                long millis = TimeUnit.MICROSECONDS.toMillis(micros);
                return new Date(millis);
            }
            case "MILLIS": {
                long millis = Long.parseLong(text.trim());
                return new Date(millis);
            }
            case "SECONDS": {
                long seconds = Long.parseLong(text.trim());
                long millis = TimeUnit.SECONDS.toMillis(seconds);
                return new Date(millis);
            }
            default: {
                if (simpleDateFormat == null) {
                    throw new IllegalStateException();
                }

                java.util.Date jDate = simpleDateFormat.parse(text.trim());
                return new Date(jDate.getTime());
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

        DateFormatInfo that = (DateFormatInfo) o;
        return format.equals(that.format);
    }

    @Override
    public int hashCode() {
        return Objects.hash(format);
    }

    @Override
    public String toString() {
        return "DateFormatInfo{" + "format='" + format + '\'' + '}';
    }
}
