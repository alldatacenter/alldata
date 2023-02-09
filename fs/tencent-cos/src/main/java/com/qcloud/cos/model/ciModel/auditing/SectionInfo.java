package com.qcloud.cos.model.ciModel.auditing;

/**
 * 具体文本分片的审核结果信息，只返回带有违规结果的分片
 */
public class SectionInfo {
    private String startByte;
    private PornInfo pornInfo;
    private TerroristInfo terroristInfo;
    private PoliticsInfo politicsInfo;
    private AdsInfo adsInfo;
    private AbuseInfo abuseInfo;
    private IllegalInfo illegalInfo;
    private String text;
    private String url;
    private String duration;
    private String offsetTime;

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

    public String getStartByte() {
        return startByte;
    }

    public void setStartByte(String startByte) {
        this.startByte = startByte;
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

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getOffsetTime() {
        return offsetTime;
    }

    public void setOffsetTime(String offsetTime) {
        this.offsetTime = offsetTime;
    }

    public void setIllegalInfo(IllegalInfo illegalInfo) {
        this.illegalInfo = illegalInfo;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("SectionInfo{");
        sb.append("startByte='").append(startByte).append('\'');
        sb.append(", pornInfo=").append(pornInfo);
        sb.append(", terroristInfo=").append(terroristInfo);
        sb.append(", politicsInfo=").append(politicsInfo);
        sb.append(", adsInfo=").append(adsInfo);
        sb.append(", abuseInfo=").append(abuseInfo);
        sb.append(", illegalInfo=").append(illegalInfo);
        sb.append(", text='").append(text).append('\'');
        sb.append(", url='").append(url).append('\'');
        sb.append(", duration='").append(duration).append('\'');
        sb.append(", offsetTime='").append(offsetTime).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
