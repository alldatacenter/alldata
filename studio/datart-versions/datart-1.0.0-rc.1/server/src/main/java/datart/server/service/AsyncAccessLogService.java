package datart.server.service;

import datart.core.entity.AccessLog;
import datart.core.log.AccessType;
import datart.security.base.ResourceType;

import java.util.Date;

public interface AsyncAccessLogService {

    void start();

    AccessLog log(AccessLog log);

    AccessLog log(AccessType accessType, ResourceType resourceType, String resourceId);

    AccessLog log(AccessType accessType, ResourceType resourceType, String resourceId, Date accessTime);

    AccessLog log(AccessType accessType, ResourceType resourceType, String resourceId, Date accessTime, Integer duration);

    void stop();

}