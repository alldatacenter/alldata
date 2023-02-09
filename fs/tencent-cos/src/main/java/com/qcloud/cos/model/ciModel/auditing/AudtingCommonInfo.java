package com.qcloud.cos.model.ciModel.auditing;

import java.util.ArrayList;
import java.util.List;

/**
 * 审核信息公共实体类 https://cloud.tencent.com/document/product/460/37318
 */
public class AudtingCommonInfo {
    /**
     * 错误码，0为正确，其他数字对应相应错误。详情请参见 https://cloud.tencent.com/document/product/460/8523
     */
    private String code;
    /**
     * 具体错误信息，如正常则为 OK
     */
    private String msg;
    /**
     * 是否命中该审核分类，0表示未命中，1表示命中，2表示疑似
     */
    private String hitFlag;
    /**
     * 审核分值。0 - 60分表示图片正常，60 - 90分表示图片疑似敏感，90 - 100分表示图片确定敏感
     */
    private String score;
    /**
     * 识别出的图片标签
     */
    private String label;

    /**
     * 次数
     */
    private String count;

    /**
     * 该字段表示审核命中的具体子标签，例如：Porn 下的 SexBehavior 子标签。
     * 注意：该字段可能返回空，表示未命中具体的子标签。
     */
    private String subLabel;

    private OcrResults ocrResults;

    private List<ObjectResults> objectResults = new ArrayList<>();

    public AudtingCommonInfo() {
    }

    public AudtingCommonInfo(String code, String msg, String hitFlag, String score, String label, String count) {
        this.code = code;
        this.msg = msg;
        this.hitFlag = hitFlag;
        this.score = score;
        this.label = label;
        this.count = count;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getHitFlag() {
        return hitFlag;
    }

    public void setHitFlag(String hitFlag) {
        this.hitFlag = hitFlag;
    }

    public String getScore() {
        return score;
    }

    public void setScore(String score) {
        this.score = score;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getCount() {
        return count;
    }

    public void setCount(String count) {
        this.count = count;
    }

    public OcrResults getOcrResults() {
        if (ocrResults == null) {
            ocrResults = new OcrResults();
        }
        return ocrResults;
    }

    public void setOcrResults(OcrResults ocrResults) {
        this.ocrResults = ocrResults;
    }

    public List<ObjectResults> getObjectResults() {
        return objectResults;
    }

    public void setObjectResults(List<ObjectResults> objectResults) {
        this.objectResults = objectResults;
    }

    public String getSubLabel() {
        return subLabel;
    }

    public void setSubLabel(String subLabel) {
        this.subLabel = subLabel;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("AudtingCommonInfo{");
        sb.append("code='").append(code).append('\'');
        sb.append(", msg='").append(msg).append('\'');
        sb.append(", hitFlag='").append(hitFlag).append('\'');
        sb.append(", score='").append(score).append('\'');
        sb.append(", label='").append(label).append('\'');
        sb.append(", count='").append(count).append('\'');
        sb.append(", subLabel='").append(subLabel).append('\'');
        sb.append(", ocrResults=").append(ocrResults);
        sb.append(", objectResults=").append(objectResults);
        sb.append('}');
        return sb.toString();
    }
}
