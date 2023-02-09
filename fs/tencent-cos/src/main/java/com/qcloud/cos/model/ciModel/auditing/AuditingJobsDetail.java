package com.qcloud.cos.model.ciModel.auditing;

import java.util.ArrayList;
import java.util.List;

public class AuditingJobsDetail {
    /**
     * 新创建任务的 ID
     */
    private String jobId;
    /**
     * 任务的状态，为 Submitted、Snapshoting、Success、Failed、Auditing 其中一个
     */
    private String state;
    /**
     * 任务的创建时间
     */
    private String creationTime;

    /**
     * 错误码，只有 State 为 Failed 时有意义
     */
    private String code;
    /**
     * 错误描述，只有 State 为 Failed 时有意义
     */
    private String message;

    /**
     * 任务对象
     */
    private String object;

    /**
     * 任务内容
     */
    private String content;

    /**
     * 截图数量
     */
    private String snapshotCount;

    /**
     * 文本分片数量
     */
    private String sectionCount;

    /**
     * 供参考的识别结果，0表示确认正常，1表示确认敏感，2表示疑似敏感
     */
    private String result;

    /**
     * 审核文本
     */
    private String audioText;

    /**
     * 内容url地址
     */
    private String url;

    /**
     * 数据id
     */
    private String dataId;
    /**
     * 标签
     */
    private String label;

    /**
     * 审核结果 鉴黄信息
     */
    private PornInfo pornInfo;

    /**
     * 审核结果 暴恐信息
     */
    private TerroristInfo terroristInfo;

    /**
     * 审核结果 政治敏感信息
     */
    private PoliticsInfo politicsInfo;

    /**
     * 审核结果 广告信息
     */
    private AdsInfo adsInfo;

    /**
     * 审核结果 谩骂信息
     */
    private AbuseInfo abuseInfo;

    /**
     * 审核结果 违法信息
     */
    private IllegalInfo illegalInfo;

    private List<SnapshotInfo> snapshotList;

    /**
     * 具体文本分片的审核结果信息，只返回带有违规结果的分片
     */
    private List<SectionInfo> sectionList;

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(String creationTime) {
        this.creationTime = creationTime;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public String getSnapshotCount() {
        return snapshotCount;
    }

    public void setSnapshotCount(String snapshotCount) {
        this.snapshotCount = snapshotCount;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public PornInfo getPornInfo() {
        if (pornInfo == null) {
            pornInfo = new PornInfo();
        }
        return pornInfo;
    }

    public void setPornInfo(PornInfo pornInfo) {
        this.pornInfo = pornInfo;
    }

    public TerroristInfo getTerroristInfo() {
        if (terroristInfo == null) {
            terroristInfo = new TerroristInfo();
        }
        return terroristInfo;
    }

    public void setTerroristInfo(TerroristInfo terroristInfo) {
        this.terroristInfo = terroristInfo;
    }

    public PoliticsInfo getPoliticsInfo() {
        if (politicsInfo == null) {
            politicsInfo = new PoliticsInfo();
        }
        return politicsInfo;
    }

    public void setPoliticsInfo(PoliticsInfo politicsInfo) {
        this.politicsInfo = politicsInfo;
    }

    public AdsInfo getAdsInfo() {
        if (adsInfo == null) {
            adsInfo = new AdsInfo();
        }
        return adsInfo;
    }

    public void setAdsInfo(AdsInfo adsInfo) {
        this.adsInfo = adsInfo;
    }

    public List<SnapshotInfo> getSnapshotList() {
        if (snapshotList == null) {
            snapshotList = new ArrayList<>();
        }
        return snapshotList;
    }

    public void setSnapshotList(List<SnapshotInfo> snapshotList) {
        this.snapshotList = snapshotList;
    }

    public List<SectionInfo> getSectionList() {
        if (sectionList == null) {
            sectionList = new ArrayList<>();
        }
        return sectionList;
    }

    public void setSectionList(List<SectionInfo> sectionList) {
        this.sectionList = sectionList;
    }

    public String getSectionCount() {
        return sectionCount;
    }

    public void setSectionCount(String sectionCount) {
        this.sectionCount = sectionCount;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public AbuseInfo getAbuseInfo() {
        if (abuseInfo == null) {
            abuseInfo = new AbuseInfo();
        }
        return abuseInfo;
    }

    public void setAbuseInfo(AbuseInfo abuseInfo) {
        this.abuseInfo = abuseInfo;
    }

    public IllegalInfo getIllegalInfo() {
        if (illegalInfo == null) {
            illegalInfo = new IllegalInfo();
        }
        return illegalInfo;
    }

    public void setIllegalInfo(IllegalInfo illegalInfo) {
        this.illegalInfo = illegalInfo;
    }

    public String getAudioText() {
        return audioText;
    }

    public void setAudioText(String audioText) {
        this.audioText = audioText;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDataId() {
        return dataId;
    }

    public void setDataId(String dataId) {
        this.dataId = dataId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("AuditingJobsDetail{");
        sb.append("jobId='").append(jobId).append('\'');
        sb.append(", state='").append(state).append('\'');
        sb.append(", creationTime='").append(creationTime).append('\'');
        sb.append(", code='").append(code).append('\'');
        sb.append(", message='").append(message).append('\'');
        sb.append(", object='").append(object).append('\'');
        sb.append(", content='").append(content).append('\'');
        sb.append(", snapshotCount='").append(snapshotCount).append('\'');
        sb.append(", sectionCount='").append(sectionCount).append('\'');
        sb.append(", result='").append(result).append('\'');
        sb.append(", audioText='").append(audioText).append('\'');
        sb.append(", url='").append(url).append('\'');
        sb.append(", dataId='").append(dataId).append('\'');
        sb.append(", label='").append(label).append('\'');
        sb.append(", pornInfo=").append(pornInfo);
        sb.append(", terroristInfo=").append(terroristInfo);
        sb.append(", politicsInfo=").append(politicsInfo);
        sb.append(", adsInfo=").append(adsInfo);
        sb.append(", abuseInfo=").append(abuseInfo);
        sb.append(", illegalInfo=").append(illegalInfo);
        sb.append(", snapshotList=").append(snapshotList);
        sb.append(", sectionList=").append(sectionList);
        sb.append('}');
        return sb.toString();
    }
}
