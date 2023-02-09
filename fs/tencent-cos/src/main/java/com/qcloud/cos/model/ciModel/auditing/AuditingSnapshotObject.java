package com.qcloud.cos.model.ciModel.auditing;

/**
 * 截帧配置实体类 https://cloud.tencent.com/document/product/460/46427
 */
public class AuditingSnapshotObject {
    /**
     * 截帧模式。Interval 表示间隔模式；Average 表示平均模式；Fps 表示固定帧率模式。
     * Interval 模式：TimeInterval，Count 参数生效。当设置 Count，未设置 TimeInterval 时，表示截取所有帧，共 Count 张图片
     * Average 模式：Count 参数生效。表示整个视频，按平均间隔截取共 Count 张图片
     * Fps 模式：TimeInterval 表示每秒截取多少帧，Count 表示共截取多少帧
     */
    private String mode;

    /**
     * 截图数量，范围为(0,10000]
     */
    private String count;

    /**
     * 截图频率，范围为(0,60]，单位为秒，支持 float 格式，执行精度精确到毫秒
     */
    private String timeInterval;

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getCount() {
        return count;
    }

    public void setCount(String count) {
        this.count = count;
    }

    public String getTimeInterval() {
        return timeInterval;
    }

    public void setTimeInterval(String timeInterval) {
        this.timeInterval = timeInterval;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("AuditingSnapshotObject{");
        sb.append("mode='").append(mode).append('\'');
        sb.append(", count='").append(count).append('\'');
        sb.append(", timeInterval='").append(timeInterval).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
