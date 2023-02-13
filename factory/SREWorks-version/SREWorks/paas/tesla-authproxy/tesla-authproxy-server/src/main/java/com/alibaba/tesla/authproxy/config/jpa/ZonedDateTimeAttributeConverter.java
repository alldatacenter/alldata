package com.alibaba.tesla.authproxy.config.jpa;

import lombok.extern.slf4j.Slf4j;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Converter(autoApply = true)
@Slf4j
public class ZonedDateTimeAttributeConverter implements AttributeConverter<ZonedDateTime, Timestamp> {

    @Override
    public Timestamp convertToDatabaseColumn(ZonedDateTime zonedDateTime) {
        return (zonedDateTime == null ? null : Timestamp.valueOf(zonedDateTime.toLocalDateTime()));
    }

    @Override
    public ZonedDateTime convertToEntityAttribute(Timestamp sqlTimestamp) {
        return (sqlTimestamp == null ? null : sqlTimestamp.toLocalDateTime().atZone(ZoneId.of("UTC")));
    }
}
