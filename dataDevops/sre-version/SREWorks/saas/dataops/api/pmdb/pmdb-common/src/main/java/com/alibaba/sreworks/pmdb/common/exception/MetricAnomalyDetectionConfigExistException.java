package com.alibaba.sreworks.pmdb.common.exception;

/**
 * 指标检测配置存在异常
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/14 14:42
 */
public class MetricAnomalyDetectionConfigExistException extends Exception {
    public MetricAnomalyDetectionConfigExistException(String message) {
        super(message);
    }

    public MetricAnomalyDetectionConfigExistException(Throwable cause) {
        super(cause);
    }

    public MetricAnomalyDetectionConfigExistException(String message, Throwable cause) {
        super(message, cause);
    }
}
