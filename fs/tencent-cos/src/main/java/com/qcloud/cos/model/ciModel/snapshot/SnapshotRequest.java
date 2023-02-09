package com.qcloud.cos.model.ciModel.snapshot;

import com.qcloud.cos.internal.CIServiceRequest;
import com.qcloud.cos.model.ciModel.common.MediaInputObject;
import com.qcloud.cos.model.ciModel.common.MediaOutputObject;

import java.io.Serializable;

/**
 * 媒体截图请求实体  详情见：https://cloud.tencent.com/document/product/460/38934
 */
public class SnapshotRequest extends CIServiceRequest implements Serializable {
    /**
     * 媒体文件的位置信息
     */
    private MediaInputObject input;
    /**
     * 截图保存的位置信息
     */
    private MediaOutputObject output;
    /**
     * 截取哪个时间点的内容，单位为秒
     */
    private String time;
    /**
     * 截图的宽。默认为0
     */
    private String Width;

    /**
     * 截图的高。默认为0。
     * Width 和 Height 都为0时，表示使用视频的宽高。如果单个为0，则以另外一个值按视频宽高比例自动适应
     */
    private String height;

    /**
     * 截图的格式，支持 jpg 和 png，默认 jpg
     */
    private String format;

    /**
     * 截帧方式：
     * keyframe：截取指定时间点之前的最近的一个关键帧
     * exactframe：截取指定时间点的帧
     * 默认值为 exactframe
     */
    private String mode;

    /**
     * 图片旋转方式。
     * auto：按视频旋转信息进行自动旋转
     * off：不旋转
     * 默认值为 auto
     */
    private String rotate;

    public MediaInputObject getInput() {
        if (input==null)
            this.input = new MediaInputObject();
        return input;
    }

    public void setInput(MediaInputObject input) {
        this.input = input;
    }

    public MediaOutputObject getOutput() {
        if (output==null)
            this.output = new MediaOutputObject();
        return output;
    }

    public void setOutput(MediaOutputObject output) {
        this.output = output;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getWidth() {
        return Width;
    }

    public void setWidth(String width) {
        Width = width;
    }

    public String getHeight() {
        return height;
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getRotate() {
        return rotate;
    }

    public void setRotate(String rotate) {
        this.rotate = rotate;
    }

    @Override
    public String toString() {
        return "SnapshotRequest{" +
                "input=" + input +
                ", output=" + output +
                ", time='" + time + '\'' +
                ", Width='" + Width + '\'' +
                ", height='" + height + '\'' +
                ", format='" + format + '\'' +
                ", mode='" + mode + '\'' +
                ", rotate='" + rotate + '\'' +
                '}';
    }
}
