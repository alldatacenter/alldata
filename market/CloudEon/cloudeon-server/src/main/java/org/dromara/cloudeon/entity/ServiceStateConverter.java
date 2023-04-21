package org.dromara.cloudeon.entity;

import org.dromara.cloudeon.enums.ServiceState;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class ServiceStateConverter implements AttributeConverter<ServiceState, Integer> {


    @Override
    public Integer convertToDatabaseColumn(ServiceState serviceState) {
        return serviceState.getValue();
    }

    @Override
    public ServiceState convertToEntityAttribute(Integer integer) {
        for (ServiceState serviceState : ServiceState.values()) {
            if (serviceState.getValue() == integer) {
                return serviceState;
            }
        }
        return null;
    }
}