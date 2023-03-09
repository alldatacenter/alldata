package com.linkedin.feathr.common.configObj.configbuilder;

import com.linkedin.feathr.common.configObj.DateTimeConfig;
import com.typesafe.config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.TimeZone;

/**
 * Build DateTimeConfig from config
 */
public class DateTimeConfigBuilder {

    private final static Logger logger = LogManager.getLogger(DateTimeConfigBuilder.class);
    private static final String DEFAULT_TIME_ZONE = "America/Los_Angeles";
    private static final String END_TIME = "endTime";
    private static final String END_TIME_FORMAT = "endTimeFormat";
    private static final String TIME_RESOLUTION = "resolution";
    private static final String OFFSET = "offset";
    private static final String LENGTH = "length";
    private static final String TIME_ZONE = "timeZone";

    private DateTimeConfigBuilder() {
    }

    /**
     * build time information object
     * default values are: length = 0 and offset = 0 and timeZone = PDT/PST
     */
    public static DateTimeConfig build(Config config) {
        String endTIme = config.getString(END_TIME);
        String endTimeFormat = config.getString(END_TIME_FORMAT);
        String timeResolutionStr = config.getString(TIME_RESOLUTION);
        ChronoUnit timeResolution = ConfigUtils.getChronoUnit(timeResolutionStr);
        long length = ConfigUtils.getLongWithDefault(config, LENGTH, 0);
        Duration offset = ConfigUtils.getDurationWithDefault(config, OFFSET, Duration.ofSeconds(0));
        String timeZoneStr = ConfigUtils.getStringWithDefault(config, TIME_ZONE, DEFAULT_TIME_ZONE);
        TimeZone timeZone = TimeZone.getTimeZone(timeZoneStr);
        DateTimeConfig dateTimeConfig = new DateTimeConfig(endTIme, endTimeFormat, timeResolution, length, offset, timeZone);
        logger.trace("Built DateTimeConfig object");
        return dateTimeConfig;
    }
}
