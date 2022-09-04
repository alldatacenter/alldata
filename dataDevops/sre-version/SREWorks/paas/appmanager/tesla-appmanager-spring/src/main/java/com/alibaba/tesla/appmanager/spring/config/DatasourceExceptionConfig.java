package com.alibaba.tesla.appmanager.spring.config;

import com.zaxxer.hikari.SQLExceptionOverride;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;

@Slf4j
public class DatasourceExceptionConfig implements SQLExceptionOverride {

    @java.lang.Override
    public Override adjudicate(SQLException sqlException) {
        return Override.CONTINUE_EVICT;
    }
}