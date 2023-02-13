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

package org.apache.inlong.sort.formats.common;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import static org.apache.inlong.sort.formats.common.Constants.DATE_AND_TIME_STANDARD_ISO_8601;
import static org.apache.inlong.sort.formats.common.Constants.DATE_AND_TIME_STANDARD_SQL;

/**
 * The format information for {@link Timestamp}s.
 */
public class TimestampFormatInfo implements BasicFormatInfo<Timestamp> {

    private static final long serialVersionUID = 1L;

    private static final String FIELD_FORMAT = "format";
    // to support avro format, precision must be less than 3
    private static final int DEFAULT_PRECISION_FOR_TIMESTAMP = 2;

    @JsonProperty(FIELD_FORMAT)
    @Nonnull
    private final String format;

    @JsonIgnore
    @Nullable
    private final SimpleDateFormat simpleDateFormat;

    @JsonProperty("precision")
    private int precision;

    @JsonCreator
    public TimestampFormatInfo(
            @JsonProperty(FIELD_FORMAT) @Nonnull String format,
            @JsonProperty("precision") int precision) {
        this.format = format;
        this.precision = precision;
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

    public TimestampFormatInfo(@JsonProperty(FIELD_FORMAT) @Nonnull String format) {
        this(format, DEFAULT_PRECISION_FOR_TIMESTAMP);
    }

    public TimestampFormatInfo() {
        this("yyyy-MM-dd HH:mm:ss", DEFAULT_PRECISION_FOR_TIMESTAMP);
    }

    public TimestampFormatInfo(@JsonProperty("precision") int precision) {
        this("yyyy-MM-dd HH:mm:ss", precision);
    }

    @Nonnull
    public String getFormat() {
        return format;
    }

    @Override
    public TimestampTypeInfo getTypeInfo() {
        return TimestampTypeInfo.INSTANCE;
    }

    @Override
    public String serialize(Timestamp timestamp) {

        switch (format) {
            case "MICROS": {
                long millis = timestamp.getTime();
                long micros = TimeUnit.MILLISECONDS.toMicros(millis);
                return Long.toString(micros);
            }
            case "MILLIS": {
                long millis = timestamp.getTime();
                return Long.toString(millis);
            }
            case "SECONDS": {
                long millis = timestamp.getTime();
                long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
                return Long.toString(seconds);
            }
            default: {
                if (simpleDateFormat == null) {
                    throw new IllegalStateException();
                }

                return simpleDateFormat.format(timestamp);
            }
        }
    }

    @Override
    public Timestamp deserialize(String text) throws ParseException {
        switch (format) {
            case "MICROS": {
                long micros = Long.parseLong(text);
                long millis = TimeUnit.MICROSECONDS.toMillis(micros);
                return new Timestamp(millis);
            }
            case "MILLIS": {
                long millis = Long.parseLong(text);
                return new Timestamp(millis);
            }
            case "SECONDS": {
                long seconds = Long.parseLong(text);
                long millis = TimeUnit.SECONDS.toMillis(seconds);
                return new Timestamp(millis);
            }
            default: {
                if (simpleDateFormat == null) {
                    throw new IllegalStateException();
                }

                Date date = simpleDateFormat.parse(text);
                return new Timestamp(date.getTime());
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

        TimestampFormatInfo that = (TimestampFormatInfo) o;
        return format.equals(that.format);
    }

    @Override
    public int hashCode() {
        return Objects.hash(format);
    }

    @Override
    public String toString() {
        return "TimestampFormatInfo{" + "format='" + format + '\'' + '}';
    }

    public int getPrecision() {
        return precision;
    }

    public void setPrecision(int precision) {
        this.precision = precision;
    }
}
