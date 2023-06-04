package org.dromara.cloudeon.entity;

import org.dromara.cloudeon.enums.AlertLevel;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class AlertLevelConverter implements AttributeConverter<AlertLevel, Integer> {


    @Override
    public Integer convertToDatabaseColumn(AlertLevel alertLevel) {
        return alertLevel.getValue();
    }

    @Override
    public AlertLevel convertToEntityAttribute(Integer integer) {
        for (AlertLevel alertLevel : AlertLevel.values()) {
            if (alertLevel.getValue() == integer) {
                return alertLevel;
            }
        }
        return null;
    }
}