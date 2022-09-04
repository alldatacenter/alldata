package com.alibaba.tesla.authproxy.component.cookie;

import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import lombok.Builder;
import lombok.Data;
import org.springframework.util.StringUtils;

/**
 * @author cdx
 * @date 2020/3/6 2:28
 */
@Data
@Builder
public class ResponseCookie {
    private final String name;
    private final String value;
    private final Duration maxAge;
    private final String domain;
    private final String path;
    private final boolean secure;
    private final boolean httpOnly;
    private final String sameSite;

    private static final DecimalFormatSymbols DECIMAL_FORMAT_SYMBOLS;
    private static final ZoneId GMT;
    private static final DateTimeFormatter[] DATE_FORMATTERS;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getName()).append('=').append(this.getValue());
        if (StringUtils.hasText(this.getPath())) {
            sb.append("; Path=").append(this.getPath());
        }

        if (StringUtils.hasText(this.domain)) {
            sb.append("; Domain=").append(this.domain);
        }

        if (!this.maxAge.isNegative()) {
            sb.append("; Max-Age=").append(this.maxAge.getSeconds());
            sb.append("; Expires=");
            long millis = this.maxAge.getSeconds() > 0L ? System.currentTimeMillis() + this.maxAge.toMillis() : 0L;
            sb.append(formatDate(millis));
        }

        if (this.secure) {
            sb.append("; Secure");
        }

        if (this.httpOnly) {
            sb.append("; HttpOnly");
        }

        if (StringUtils.hasText(this.sameSite)) {
            sb.append("; SameSite=").append(this.sameSite);
        }

        return sb.toString();
    }

    static {
        DECIMAL_FORMAT_SYMBOLS = new DecimalFormatSymbols(Locale.ENGLISH);
        GMT = ZoneId.of("GMT");
        DATE_FORMATTERS = new DateTimeFormatter[] {DateTimeFormatter.RFC_1123_DATE_TIME, DateTimeFormatter.ofPattern(
            "EEEE, dd-MMM-yy HH:mm:ss zz", Locale.US), DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss yyyy",
            Locale.US).withZone(GMT)};
    }

    static String formatDate(long date) {
        Instant instant = Instant.ofEpochMilli(date);
        ZonedDateTime time = ZonedDateTime.ofInstant(instant, GMT);
        return DATE_FORMATTERS[0].format(time);
    }

}
