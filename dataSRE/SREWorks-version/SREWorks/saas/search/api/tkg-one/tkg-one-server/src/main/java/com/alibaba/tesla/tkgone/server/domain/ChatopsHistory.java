package com.alibaba.tesla.tkgone.server.domain;

import java.io.Serializable;
import java.util.Date;

public class ChatopsHistory implements Serializable {
    private Long id;

    private Date gmtCreate;

    private Date gmtModified;

    private String category;

    private String senderNick;

    private String senderId;

    private String senderEmpid;

    private Integer isConversation;

    private String conversationTitle;

    private Integer isContentHelp;

    private String sendContent;

    private String backContent;

    private Integer rate;

    private String suggest;

    private Long patternId;

    private Long serviceId;

    private String feedbackId;

    private String feedbackEmpid;

    private String conversationId;

    private static final long serialVersionUID = 1L;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(Date gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    public Date getGmtModified() {
        return gmtModified;
    }

    public void setGmtModified(Date gmtModified) {
        this.gmtModified = gmtModified;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category == null ? null : category.trim();
    }

    public String getSenderNick() {
        return senderNick;
    }

    public void setSenderNick(String senderNick) {
        this.senderNick = senderNick == null ? null : senderNick.trim();
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId == null ? null : senderId.trim();
    }

    public String getSenderEmpid() {
        return senderEmpid;
    }

    public void setSenderEmpid(String senderEmpid) {
        this.senderEmpid = senderEmpid == null ? null : senderEmpid.trim();
    }

    public Integer getIsConversation() {
        return isConversation;
    }

    public void setIsConversation(Integer isConversation) {
        this.isConversation = isConversation;
    }

    public String getConversationTitle() {
        return conversationTitle;
    }

    public void setConversationTitle(String conversationTitle) {
        this.conversationTitle = conversationTitle == null ? null : conversationTitle.trim();
    }

    public Integer getIsContentHelp() {
        return isContentHelp;
    }

    public void setIsContentHelp(Integer isContentHelp) {
        this.isContentHelp = isContentHelp;
    }

    public String getSendContent() {
        return sendContent;
    }

    public void setSendContent(String sendContent) {
        this.sendContent = sendContent == null ? null : sendContent.trim();
    }

    public String getBackContent() {
        return backContent;
    }

    public void setBackContent(String backContent) {
        this.backContent = backContent == null ? null : backContent.trim();
    }

    public Integer getRate() {
        return rate;
    }

    public void setRate(Integer rate) {
        this.rate = rate;
    }

    public String getSuggest() {
        return suggest;
    }

    public void setSuggest(String suggest) {
        this.suggest = suggest == null ? null : suggest.trim();
    }

    public Long getPatternId() {
        return patternId;
    }

    public void setPatternId(Long patternId) {
        this.patternId = patternId;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }

    public String getFeedbackId() {
        return feedbackId;
    }

    public void setFeedbackId(String feedbackId) {
        this.feedbackId = feedbackId == null ? null : feedbackId.trim();
    }

    public String getFeedbackEmpid() {
        return feedbackEmpid;
    }

    public void setFeedbackEmpid(String feedbackEmpid) {
        this.feedbackEmpid = feedbackEmpid == null ? null : feedbackEmpid.trim();
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId == null ? null : conversationId.trim();
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        ChatopsHistory other = (ChatopsHistory) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getGmtCreate() == null ? other.getGmtCreate() == null : this.getGmtCreate().equals(other.getGmtCreate()))
            && (this.getGmtModified() == null ? other.getGmtModified() == null : this.getGmtModified().equals(other.getGmtModified()))
            && (this.getCategory() == null ? other.getCategory() == null : this.getCategory().equals(other.getCategory()))
            && (this.getSenderNick() == null ? other.getSenderNick() == null : this.getSenderNick().equals(other.getSenderNick()))
            && (this.getSenderId() == null ? other.getSenderId() == null : this.getSenderId().equals(other.getSenderId()))
            && (this.getSenderEmpid() == null ? other.getSenderEmpid() == null : this.getSenderEmpid().equals(other.getSenderEmpid()))
            && (this.getIsConversation() == null ? other.getIsConversation() == null : this.getIsConversation().equals(other.getIsConversation()))
            && (this.getConversationTitle() == null ? other.getConversationTitle() == null : this.getConversationTitle().equals(other.getConversationTitle()))
            && (this.getIsContentHelp() == null ? other.getIsContentHelp() == null : this.getIsContentHelp().equals(other.getIsContentHelp()))
            && (this.getSendContent() == null ? other.getSendContent() == null : this.getSendContent().equals(other.getSendContent()))
            && (this.getBackContent() == null ? other.getBackContent() == null : this.getBackContent().equals(other.getBackContent()))
            && (this.getRate() == null ? other.getRate() == null : this.getRate().equals(other.getRate()))
            && (this.getSuggest() == null ? other.getSuggest() == null : this.getSuggest().equals(other.getSuggest()))
            && (this.getPatternId() == null ? other.getPatternId() == null : this.getPatternId().equals(other.getPatternId()))
            && (this.getServiceId() == null ? other.getServiceId() == null : this.getServiceId().equals(other.getServiceId()))
            && (this.getFeedbackId() == null ? other.getFeedbackId() == null : this.getFeedbackId().equals(other.getFeedbackId()))
            && (this.getFeedbackEmpid() == null ? other.getFeedbackEmpid() == null : this.getFeedbackEmpid().equals(other.getFeedbackEmpid()))
            && (this.getConversationId() == null ? other.getConversationId() == null : this.getConversationId().equals(other.getConversationId()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getGmtCreate() == null) ? 0 : getGmtCreate().hashCode());
        result = prime * result + ((getGmtModified() == null) ? 0 : getGmtModified().hashCode());
        result = prime * result + ((getCategory() == null) ? 0 : getCategory().hashCode());
        result = prime * result + ((getSenderNick() == null) ? 0 : getSenderNick().hashCode());
        result = prime * result + ((getSenderId() == null) ? 0 : getSenderId().hashCode());
        result = prime * result + ((getSenderEmpid() == null) ? 0 : getSenderEmpid().hashCode());
        result = prime * result + ((getIsConversation() == null) ? 0 : getIsConversation().hashCode());
        result = prime * result + ((getConversationTitle() == null) ? 0 : getConversationTitle().hashCode());
        result = prime * result + ((getIsContentHelp() == null) ? 0 : getIsContentHelp().hashCode());
        result = prime * result + ((getSendContent() == null) ? 0 : getSendContent().hashCode());
        result = prime * result + ((getBackContent() == null) ? 0 : getBackContent().hashCode());
        result = prime * result + ((getRate() == null) ? 0 : getRate().hashCode());
        result = prime * result + ((getSuggest() == null) ? 0 : getSuggest().hashCode());
        result = prime * result + ((getPatternId() == null) ? 0 : getPatternId().hashCode());
        result = prime * result + ((getServiceId() == null) ? 0 : getServiceId().hashCode());
        result = prime * result + ((getFeedbackId() == null) ? 0 : getFeedbackId().hashCode());
        result = prime * result + ((getFeedbackEmpid() == null) ? 0 : getFeedbackEmpid().hashCode());
        result = prime * result + ((getConversationId() == null) ? 0 : getConversationId().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", gmtCreate=").append(gmtCreate);
        sb.append(", gmtModified=").append(gmtModified);
        sb.append(", category=").append(category);
        sb.append(", senderNick=").append(senderNick);
        sb.append(", senderId=").append(senderId);
        sb.append(", senderEmpid=").append(senderEmpid);
        sb.append(", isConversation=").append(isConversation);
        sb.append(", conversationTitle=").append(conversationTitle);
        sb.append(", isContentHelp=").append(isContentHelp);
        sb.append(", sendContent=").append(sendContent);
        sb.append(", backContent=").append(backContent);
        sb.append(", rate=").append(rate);
        sb.append(", suggest=").append(suggest);
        sb.append(", patternId=").append(patternId);
        sb.append(", serviceId=").append(serviceId);
        sb.append(", feedbackId=").append(feedbackId);
        sb.append(", feedbackEmpid=").append(feedbackEmpid);
        sb.append(", conversationId=").append(conversationId);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}