package com.qcloud.cos.model.ciModel.template;

import com.qcloud.cos.internal.CIServiceRequest;

import java.io.Serializable;

public class GenerateSnapshotRequest extends CIServiceRequest implements Serializable {
    /**
     * 截取哪个时间点的内容，单位为秒
     */
    private String time;
    /**
     * 文件位置
     */
    private Input input;
    /**
     * 截图保存的位置信息
     */
    private Output output;

    /**
     * 截图的宽。默认为0
     */
    private Integer width;
    /**
     * 截图的高。默认为0。
     * Width 和 Height 都为0时，表示使用视频的宽高。如果单个为0，则以另外一个值按视频宽高比例自动适应。
     */
    private Integer height;
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

    public class Input {
        private String object;

        public String getObject() {
            return object;
        }

        public void setObject(String object) {
            this.object = object;
        }
    }

    public class Output {
        private String region;
        private String bucket;
        private String object;

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getObject() {
            return object;
        }

        public void setObject(String object) {
            this.object = object;
        }
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public Input getInput() {
        return input;
    }

    public void setInput(Input input) {
        this.input = input;
    }

    public Output getOutput() {
        return output;
    }

    public void setOutput(Output output) {
        this.output = output;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
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
        return "GenerateSnapshotRequest{" +
                "time='" + time + '\'' +
                ", input=" + input +
                ", output=" + output +
                ", width=" + width +
                ", height=" + height +
                ", format='" + format + '\'' +
                ", mode='" + mode + '\'' +
                ", rotate='" + rotate + '\'' +
                '}';
    }
}
