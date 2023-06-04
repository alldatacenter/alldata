package datart.server.service.impl;

import datart.core.common.UUIDGenerator;
import datart.core.entity.AccessLog;
import datart.core.log.AccessType;
import datart.core.mappers.ext.AccessLogMapperExt;
import datart.security.base.ResourceType;
import datart.server.service.AsyncAccessLogService;
import datart.server.service.BaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;

@Service
@Slf4j
public class AccessLogServiceImpl extends BaseService implements AsyncAccessLogService {

    private final LinkedBlockingQueue<AccessLog> logQueue = new LinkedBlockingQueue<>();

    private final Thread logThread;

    private boolean stop = false;

    public AccessLogServiceImpl(AccessLogMapperExt logMapper) {
        logThread = new Thread(() -> {
            while (!stop) {
                try {
                    AccessLog accessLog = logQueue.take();
//                    logMapper.insert(accessLog);
                } catch (Exception e) {
//                    log.error("access log insert error", e);
                }
            }
        });
        logThread.start();
    }

    @Override
    public void start() {
        logThread.start();
    }

    @Override
    public AccessLog log(AccessLog log) {
        log.setId(UUIDGenerator.generate());
        logQueue.add(log);
        return log;
    }

    @Override
    public AccessLog log(AccessType accessType, ResourceType resourceType, String resourceId) {
        return log(accessType, resourceType, resourceId, new Date());
    }

    @Override
    public AccessLog log(AccessType accessType, ResourceType resourceType, String resourceId, Date accessTime) {
        return log(accessType, resourceType, resourceId, accessTime, null);
    }


    @Override
    public AccessLog log(AccessType accessType, ResourceType resourceType, String resourceId, Date accessTime, Integer duration) {
        AccessLog log = new AccessLog();
        log.setUser(getCurrentUser().getId());
        log.setAccessType(accessType.name());
        log.setResourceType(resourceType != null ? resourceType.name() : null);
        log.setResourceId(resourceId);
        log.setAccessTime(accessTime);
        log.setDuration(duration);
        return log(log);
    }

    @Override
    public void stop() {
        stop = true;
    }


}
