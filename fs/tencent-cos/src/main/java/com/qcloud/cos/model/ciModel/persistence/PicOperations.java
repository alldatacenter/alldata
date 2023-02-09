package com.qcloud.cos.model.ciModel.persistence;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class PicOperations {
    @JsonProperty("is_pic_info")
    private int isPicInfo = 0;
    private List<Rule> rules;
    public static class Rule {
        private String bucket;
        @JsonProperty("fileid")
        private String fileId;
        private String rule;
        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getFileId() {
            return fileId;
        }

        public void setFileId(String fileId) {
            this.fileId = fileId;
        }

        public String getRule() {
            return rule;
        }

        public void setRule(String rule) {
            this.rule = rule;
        }
    }
    public int getIsPicInfo() {
        return isPicInfo;
    }

    public void setIsPicInfo(int isPicInfo) {
        this.isPicInfo = isPicInfo;
    }

    public List<Rule> getRules() {
        return rules;
    }

    public void setRules(List<Rule> rules) {
        this.rules = rules;
    }
}
