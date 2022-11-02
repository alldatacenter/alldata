package com.alibaba.tesla.appmanager.server.service.appoption;

public interface AppOptionType {

    String encode(Object value);

    Object decode(String value);

    String defaultValue();
}
