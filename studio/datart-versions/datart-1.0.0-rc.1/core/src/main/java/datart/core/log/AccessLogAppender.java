package datart.core.log;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import datart.core.mappers.ext.AccessLogMapperExt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AccessLogAppender extends UnsynchronizedAppenderBase<LoggingEvent> {

    private static AccessLogMapperExt logMapper;

    @Override
    protected void append(LoggingEvent log) {
        System.out.println("------------------>" + log);
    }

    @Autowired
    public void setLogMapper(AccessLogMapperExt logMapper) {
        AccessLogAppender.logMapper = logMapper;
    }


}