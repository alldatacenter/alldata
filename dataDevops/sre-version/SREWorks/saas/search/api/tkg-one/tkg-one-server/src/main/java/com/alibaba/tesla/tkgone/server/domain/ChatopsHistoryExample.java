package com.alibaba.tesla.tkgone.server.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChatopsHistoryExample {
    protected String orderByClause;

    protected boolean distinct;

    protected List<Criteria> oredCriteria;

    public ChatopsHistoryExample() {
        oredCriteria = new ArrayList<Criteria>();
    }

    public void setOrderByClause(String orderByClause) {
        this.orderByClause = orderByClause;
    }

    public String getOrderByClause() {
        return orderByClause;
    }

    public void setDistinct(boolean distinct) {
        this.distinct = distinct;
    }

    public boolean isDistinct() {
        return distinct;
    }

    public List<Criteria> getOredCriteria() {
        return oredCriteria;
    }

    public void or(Criteria criteria) {
        oredCriteria.add(criteria);
    }

    public Criteria or() {
        Criteria criteria = createCriteriaInternal();
        oredCriteria.add(criteria);
        return criteria;
    }

    public Criteria createCriteria() {
        Criteria criteria = createCriteriaInternal();
        if (oredCriteria.size() == 0) {
            oredCriteria.add(criteria);
        }
        return criteria;
    }

    protected Criteria createCriteriaInternal() {
        Criteria criteria = new Criteria();
        return criteria;
    }

    public void clear() {
        oredCriteria.clear();
        orderByClause = null;
        distinct = false;
    }

    protected abstract static class GeneratedCriteria {
        protected List<Criterion> criteria;

        protected GeneratedCriteria() {
            super();
            criteria = new ArrayList<Criterion>();
        }

        public boolean isValid() {
            return criteria.size() > 0;
        }

        public List<Criterion> getAllCriteria() {
            return criteria;
        }

        public List<Criterion> getCriteria() {
            return criteria;
        }

        protected void addCriterion(String condition) {
            if (condition == null) {
                throw new RuntimeException("Value for condition cannot be null");
            }
            criteria.add(new Criterion(condition));
        }

        protected void addCriterion(String condition, Object value, String property) {
            if (value == null) {
                throw new RuntimeException("Value for " + property + " cannot be null");
            }
            criteria.add(new Criterion(condition, value));
        }

        protected void addCriterion(String condition, Object value1, Object value2, String property) {
            if (value1 == null || value2 == null) {
                throw new RuntimeException("Between values for " + property + " cannot be null");
            }
            criteria.add(new Criterion(condition, value1, value2));
        }

        public Criteria andIdIsNull() {
            addCriterion("id is null");
            return (Criteria) this;
        }

        public Criteria andIdIsNotNull() {
            addCriterion("id is not null");
            return (Criteria) this;
        }

        public Criteria andIdEqualTo(Long value) {
            addCriterion("id =", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdNotEqualTo(Long value) {
            addCriterion("id <>", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdGreaterThan(Long value) {
            addCriterion("id >", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdGreaterThanOrEqualTo(Long value) {
            addCriterion("id >=", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdLessThan(Long value) {
            addCriterion("id <", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdLessThanOrEqualTo(Long value) {
            addCriterion("id <=", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdIn(List<Long> values) {
            addCriterion("id in", values, "id");
            return (Criteria) this;
        }

        public Criteria andIdNotIn(List<Long> values) {
            addCriterion("id not in", values, "id");
            return (Criteria) this;
        }

        public Criteria andIdBetween(Long value1, Long value2) {
            addCriterion("id between", value1, value2, "id");
            return (Criteria) this;
        }

        public Criteria andIdNotBetween(Long value1, Long value2) {
            addCriterion("id not between", value1, value2, "id");
            return (Criteria) this;
        }

        public Criteria andGmtCreateIsNull() {
            addCriterion("gmt_create is null");
            return (Criteria) this;
        }

        public Criteria andGmtCreateIsNotNull() {
            addCriterion("gmt_create is not null");
            return (Criteria) this;
        }

        public Criteria andGmtCreateEqualTo(Date value) {
            addCriterion("gmt_create =", value, "gmtCreate");
            return (Criteria) this;
        }

        public Criteria andGmtCreateNotEqualTo(Date value) {
            addCriterion("gmt_create <>", value, "gmtCreate");
            return (Criteria) this;
        }

        public Criteria andGmtCreateGreaterThan(Date value) {
            addCriterion("gmt_create >", value, "gmtCreate");
            return (Criteria) this;
        }

        public Criteria andGmtCreateGreaterThanOrEqualTo(Date value) {
            addCriterion("gmt_create >=", value, "gmtCreate");
            return (Criteria) this;
        }

        public Criteria andGmtCreateLessThan(Date value) {
            addCriterion("gmt_create <", value, "gmtCreate");
            return (Criteria) this;
        }

        public Criteria andGmtCreateLessThanOrEqualTo(Date value) {
            addCriterion("gmt_create <=", value, "gmtCreate");
            return (Criteria) this;
        }

        public Criteria andGmtCreateIn(List<Date> values) {
            addCriterion("gmt_create in", values, "gmtCreate");
            return (Criteria) this;
        }

        public Criteria andGmtCreateNotIn(List<Date> values) {
            addCriterion("gmt_create not in", values, "gmtCreate");
            return (Criteria) this;
        }

        public Criteria andGmtCreateBetween(Date value1, Date value2) {
            addCriterion("gmt_create between", value1, value2, "gmtCreate");
            return (Criteria) this;
        }

        public Criteria andGmtCreateNotBetween(Date value1, Date value2) {
            addCriterion("gmt_create not between", value1, value2, "gmtCreate");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedIsNull() {
            addCriterion("gmt_modified is null");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedIsNotNull() {
            addCriterion("gmt_modified is not null");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedEqualTo(Date value) {
            addCriterion("gmt_modified =", value, "gmtModified");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedNotEqualTo(Date value) {
            addCriterion("gmt_modified <>", value, "gmtModified");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedGreaterThan(Date value) {
            addCriterion("gmt_modified >", value, "gmtModified");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedGreaterThanOrEqualTo(Date value) {
            addCriterion("gmt_modified >=", value, "gmtModified");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedLessThan(Date value) {
            addCriterion("gmt_modified <", value, "gmtModified");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedLessThanOrEqualTo(Date value) {
            addCriterion("gmt_modified <=", value, "gmtModified");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedIn(List<Date> values) {
            addCriterion("gmt_modified in", values, "gmtModified");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedNotIn(List<Date> values) {
            addCriterion("gmt_modified not in", values, "gmtModified");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedBetween(Date value1, Date value2) {
            addCriterion("gmt_modified between", value1, value2, "gmtModified");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedNotBetween(Date value1, Date value2) {
            addCriterion("gmt_modified not between", value1, value2, "gmtModified");
            return (Criteria) this;
        }

        public Criteria andCategoryIsNull() {
            addCriterion("category is null");
            return (Criteria) this;
        }

        public Criteria andCategoryIsNotNull() {
            addCriterion("category is not null");
            return (Criteria) this;
        }

        public Criteria andCategoryEqualTo(String value) {
            addCriterion("category =", value, "category");
            return (Criteria) this;
        }

        public Criteria andCategoryNotEqualTo(String value) {
            addCriterion("category <>", value, "category");
            return (Criteria) this;
        }

        public Criteria andCategoryGreaterThan(String value) {
            addCriterion("category >", value, "category");
            return (Criteria) this;
        }

        public Criteria andCategoryGreaterThanOrEqualTo(String value) {
            addCriterion("category >=", value, "category");
            return (Criteria) this;
        }

        public Criteria andCategoryLessThan(String value) {
            addCriterion("category <", value, "category");
            return (Criteria) this;
        }

        public Criteria andCategoryLessThanOrEqualTo(String value) {
            addCriterion("category <=", value, "category");
            return (Criteria) this;
        }

        public Criteria andCategoryLike(String value) {
            addCriterion("category like", value, "category");
            return (Criteria) this;
        }

        public Criteria andCategoryNotLike(String value) {
            addCriterion("category not like", value, "category");
            return (Criteria) this;
        }

        public Criteria andCategoryIn(List<String> values) {
            addCriterion("category in", values, "category");
            return (Criteria) this;
        }

        public Criteria andCategoryNotIn(List<String> values) {
            addCriterion("category not in", values, "category");
            return (Criteria) this;
        }

        public Criteria andCategoryBetween(String value1, String value2) {
            addCriterion("category between", value1, value2, "category");
            return (Criteria) this;
        }

        public Criteria andCategoryNotBetween(String value1, String value2) {
            addCriterion("category not between", value1, value2, "category");
            return (Criteria) this;
        }

        public Criteria andSenderNickIsNull() {
            addCriterion("sender_nick is null");
            return (Criteria) this;
        }

        public Criteria andSenderNickIsNotNull() {
            addCriterion("sender_nick is not null");
            return (Criteria) this;
        }

        public Criteria andSenderNickEqualTo(String value) {
            addCriterion("sender_nick =", value, "senderNick");
            return (Criteria) this;
        }

        public Criteria andSenderNickNotEqualTo(String value) {
            addCriterion("sender_nick <>", value, "senderNick");
            return (Criteria) this;
        }

        public Criteria andSenderNickGreaterThan(String value) {
            addCriterion("sender_nick >", value, "senderNick");
            return (Criteria) this;
        }

        public Criteria andSenderNickGreaterThanOrEqualTo(String value) {
            addCriterion("sender_nick >=", value, "senderNick");
            return (Criteria) this;
        }

        public Criteria andSenderNickLessThan(String value) {
            addCriterion("sender_nick <", value, "senderNick");
            return (Criteria) this;
        }

        public Criteria andSenderNickLessThanOrEqualTo(String value) {
            addCriterion("sender_nick <=", value, "senderNick");
            return (Criteria) this;
        }

        public Criteria andSenderNickLike(String value) {
            addCriterion("sender_nick like", value, "senderNick");
            return (Criteria) this;
        }

        public Criteria andSenderNickNotLike(String value) {
            addCriterion("sender_nick not like", value, "senderNick");
            return (Criteria) this;
        }

        public Criteria andSenderNickIn(List<String> values) {
            addCriterion("sender_nick in", values, "senderNick");
            return (Criteria) this;
        }

        public Criteria andSenderNickNotIn(List<String> values) {
            addCriterion("sender_nick not in", values, "senderNick");
            return (Criteria) this;
        }

        public Criteria andSenderNickBetween(String value1, String value2) {
            addCriterion("sender_nick between", value1, value2, "senderNick");
            return (Criteria) this;
        }

        public Criteria andSenderNickNotBetween(String value1, String value2) {
            addCriterion("sender_nick not between", value1, value2, "senderNick");
            return (Criteria) this;
        }

        public Criteria andSenderIdIsNull() {
            addCriterion("sender_id is null");
            return (Criteria) this;
        }

        public Criteria andSenderIdIsNotNull() {
            addCriterion("sender_id is not null");
            return (Criteria) this;
        }

        public Criteria andSenderIdEqualTo(String value) {
            addCriterion("sender_id =", value, "senderId");
            return (Criteria) this;
        }

        public Criteria andSenderIdNotEqualTo(String value) {
            addCriterion("sender_id <>", value, "senderId");
            return (Criteria) this;
        }

        public Criteria andSenderIdGreaterThan(String value) {
            addCriterion("sender_id >", value, "senderId");
            return (Criteria) this;
        }

        public Criteria andSenderIdGreaterThanOrEqualTo(String value) {
            addCriterion("sender_id >=", value, "senderId");
            return (Criteria) this;
        }

        public Criteria andSenderIdLessThan(String value) {
            addCriterion("sender_id <", value, "senderId");
            return (Criteria) this;
        }

        public Criteria andSenderIdLessThanOrEqualTo(String value) {
            addCriterion("sender_id <=", value, "senderId");
            return (Criteria) this;
        }

        public Criteria andSenderIdLike(String value) {
            addCriterion("sender_id like", value, "senderId");
            return (Criteria) this;
        }

        public Criteria andSenderIdNotLike(String value) {
            addCriterion("sender_id not like", value, "senderId");
            return (Criteria) this;
        }

        public Criteria andSenderIdIn(List<String> values) {
            addCriterion("sender_id in", values, "senderId");
            return (Criteria) this;
        }

        public Criteria andSenderIdNotIn(List<String> values) {
            addCriterion("sender_id not in", values, "senderId");
            return (Criteria) this;
        }

        public Criteria andSenderIdBetween(String value1, String value2) {
            addCriterion("sender_id between", value1, value2, "senderId");
            return (Criteria) this;
        }

        public Criteria andSenderIdNotBetween(String value1, String value2) {
            addCriterion("sender_id not between", value1, value2, "senderId");
            return (Criteria) this;
        }

        public Criteria andSenderEmpidIsNull() {
            addCriterion("sender_empid is null");
            return (Criteria) this;
        }

        public Criteria andSenderEmpidIsNotNull() {
            addCriterion("sender_empid is not null");
            return (Criteria) this;
        }

        public Criteria andSenderEmpidEqualTo(String value) {
            addCriterion("sender_empid =", value, "senderEmpid");
            return (Criteria) this;
        }

        public Criteria andSenderEmpidNotEqualTo(String value) {
            addCriterion("sender_empid <>", value, "senderEmpid");
            return (Criteria) this;
        }

        public Criteria andSenderEmpidGreaterThan(String value) {
            addCriterion("sender_empid >", value, "senderEmpid");
            return (Criteria) this;
        }

        public Criteria andSenderEmpidGreaterThanOrEqualTo(String value) {
            addCriterion("sender_empid >=", value, "senderEmpid");
            return (Criteria) this;
        }

        public Criteria andSenderEmpidLessThan(String value) {
            addCriterion("sender_empid <", value, "senderEmpid");
            return (Criteria) this;
        }

        public Criteria andSenderEmpidLessThanOrEqualTo(String value) {
            addCriterion("sender_empid <=", value, "senderEmpid");
            return (Criteria) this;
        }

        public Criteria andSenderEmpidLike(String value) {
            addCriterion("sender_empid like", value, "senderEmpid");
            return (Criteria) this;
        }

        public Criteria andSenderEmpidNotLike(String value) {
            addCriterion("sender_empid not like", value, "senderEmpid");
            return (Criteria) this;
        }

        public Criteria andSenderEmpidIn(List<String> values) {
            addCriterion("sender_empid in", values, "senderEmpid");
            return (Criteria) this;
        }

        public Criteria andSenderEmpidNotIn(List<String> values) {
            addCriterion("sender_empid not in", values, "senderEmpid");
            return (Criteria) this;
        }

        public Criteria andSenderEmpidBetween(String value1, String value2) {
            addCriterion("sender_empid between", value1, value2, "senderEmpid");
            return (Criteria) this;
        }

        public Criteria andSenderEmpidNotBetween(String value1, String value2) {
            addCriterion("sender_empid not between", value1, value2, "senderEmpid");
            return (Criteria) this;
        }

        public Criteria andIsConversationIsNull() {
            addCriterion("is_conversation is null");
            return (Criteria) this;
        }

        public Criteria andIsConversationIsNotNull() {
            addCriterion("is_conversation is not null");
            return (Criteria) this;
        }

        public Criteria andIsConversationEqualTo(Integer value) {
            addCriterion("is_conversation =", value, "isConversation");
            return (Criteria) this;
        }

        public Criteria andIsConversationNotEqualTo(Integer value) {
            addCriterion("is_conversation <>", value, "isConversation");
            return (Criteria) this;
        }

        public Criteria andIsConversationGreaterThan(Integer value) {
            addCriterion("is_conversation >", value, "isConversation");
            return (Criteria) this;
        }

        public Criteria andIsConversationGreaterThanOrEqualTo(Integer value) {
            addCriterion("is_conversation >=", value, "isConversation");
            return (Criteria) this;
        }

        public Criteria andIsConversationLessThan(Integer value) {
            addCriterion("is_conversation <", value, "isConversation");
            return (Criteria) this;
        }

        public Criteria andIsConversationLessThanOrEqualTo(Integer value) {
            addCriterion("is_conversation <=", value, "isConversation");
            return (Criteria) this;
        }

        public Criteria andIsConversationIn(List<Integer> values) {
            addCriterion("is_conversation in", values, "isConversation");
            return (Criteria) this;
        }

        public Criteria andIsConversationNotIn(List<Integer> values) {
            addCriterion("is_conversation not in", values, "isConversation");
            return (Criteria) this;
        }

        public Criteria andIsConversationBetween(Integer value1, Integer value2) {
            addCriterion("is_conversation between", value1, value2, "isConversation");
            return (Criteria) this;
        }

        public Criteria andIsConversationNotBetween(Integer value1, Integer value2) {
            addCriterion("is_conversation not between", value1, value2, "isConversation");
            return (Criteria) this;
        }

        public Criteria andConversationTitleIsNull() {
            addCriterion("conversation_title is null");
            return (Criteria) this;
        }

        public Criteria andConversationTitleIsNotNull() {
            addCriterion("conversation_title is not null");
            return (Criteria) this;
        }

        public Criteria andConversationTitleEqualTo(String value) {
            addCriterion("conversation_title =", value, "conversationTitle");
            return (Criteria) this;
        }

        public Criteria andConversationTitleNotEqualTo(String value) {
            addCriterion("conversation_title <>", value, "conversationTitle");
            return (Criteria) this;
        }

        public Criteria andConversationTitleGreaterThan(String value) {
            addCriterion("conversation_title >", value, "conversationTitle");
            return (Criteria) this;
        }

        public Criteria andConversationTitleGreaterThanOrEqualTo(String value) {
            addCriterion("conversation_title >=", value, "conversationTitle");
            return (Criteria) this;
        }

        public Criteria andConversationTitleLessThan(String value) {
            addCriterion("conversation_title <", value, "conversationTitle");
            return (Criteria) this;
        }

        public Criteria andConversationTitleLessThanOrEqualTo(String value) {
            addCriterion("conversation_title <=", value, "conversationTitle");
            return (Criteria) this;
        }

        public Criteria andConversationTitleLike(String value) {
            addCriterion("conversation_title like", value, "conversationTitle");
            return (Criteria) this;
        }

        public Criteria andConversationTitleNotLike(String value) {
            addCriterion("conversation_title not like", value, "conversationTitle");
            return (Criteria) this;
        }

        public Criteria andConversationTitleIn(List<String> values) {
            addCriterion("conversation_title in", values, "conversationTitle");
            return (Criteria) this;
        }

        public Criteria andConversationTitleNotIn(List<String> values) {
            addCriterion("conversation_title not in", values, "conversationTitle");
            return (Criteria) this;
        }

        public Criteria andConversationTitleBetween(String value1, String value2) {
            addCriterion("conversation_title between", value1, value2, "conversationTitle");
            return (Criteria) this;
        }

        public Criteria andConversationTitleNotBetween(String value1, String value2) {
            addCriterion("conversation_title not between", value1, value2, "conversationTitle");
            return (Criteria) this;
        }

        public Criteria andIsContentHelpIsNull() {
            addCriterion("is_content_help is null");
            return (Criteria) this;
        }

        public Criteria andIsContentHelpIsNotNull() {
            addCriterion("is_content_help is not null");
            return (Criteria) this;
        }

        public Criteria andIsContentHelpEqualTo(Integer value) {
            addCriterion("is_content_help =", value, "isContentHelp");
            return (Criteria) this;
        }

        public Criteria andIsContentHelpNotEqualTo(Integer value) {
            addCriterion("is_content_help <>", value, "isContentHelp");
            return (Criteria) this;
        }

        public Criteria andIsContentHelpGreaterThan(Integer value) {
            addCriterion("is_content_help >", value, "isContentHelp");
            return (Criteria) this;
        }

        public Criteria andIsContentHelpGreaterThanOrEqualTo(Integer value) {
            addCriterion("is_content_help >=", value, "isContentHelp");
            return (Criteria) this;
        }

        public Criteria andIsContentHelpLessThan(Integer value) {
            addCriterion("is_content_help <", value, "isContentHelp");
            return (Criteria) this;
        }

        public Criteria andIsContentHelpLessThanOrEqualTo(Integer value) {
            addCriterion("is_content_help <=", value, "isContentHelp");
            return (Criteria) this;
        }

        public Criteria andIsContentHelpIn(List<Integer> values) {
            addCriterion("is_content_help in", values, "isContentHelp");
            return (Criteria) this;
        }

        public Criteria andIsContentHelpNotIn(List<Integer> values) {
            addCriterion("is_content_help not in", values, "isContentHelp");
            return (Criteria) this;
        }

        public Criteria andIsContentHelpBetween(Integer value1, Integer value2) {
            addCriterion("is_content_help between", value1, value2, "isContentHelp");
            return (Criteria) this;
        }

        public Criteria andIsContentHelpNotBetween(Integer value1, Integer value2) {
            addCriterion("is_content_help not between", value1, value2, "isContentHelp");
            return (Criteria) this;
        }

        public Criteria andSendContentIsNull() {
            addCriterion("send_content is null");
            return (Criteria) this;
        }

        public Criteria andSendContentIsNotNull() {
            addCriterion("send_content is not null");
            return (Criteria) this;
        }

        public Criteria andSendContentEqualTo(String value) {
            addCriterion("send_content =", value, "sendContent");
            return (Criteria) this;
        }

        public Criteria andSendContentNotEqualTo(String value) {
            addCriterion("send_content <>", value, "sendContent");
            return (Criteria) this;
        }

        public Criteria andSendContentGreaterThan(String value) {
            addCriterion("send_content >", value, "sendContent");
            return (Criteria) this;
        }

        public Criteria andSendContentGreaterThanOrEqualTo(String value) {
            addCriterion("send_content >=", value, "sendContent");
            return (Criteria) this;
        }

        public Criteria andSendContentLessThan(String value) {
            addCriterion("send_content <", value, "sendContent");
            return (Criteria) this;
        }

        public Criteria andSendContentLessThanOrEqualTo(String value) {
            addCriterion("send_content <=", value, "sendContent");
            return (Criteria) this;
        }

        public Criteria andSendContentLike(String value) {
            addCriterion("send_content like", value, "sendContent");
            return (Criteria) this;
        }

        public Criteria andSendContentNotLike(String value) {
            addCriterion("send_content not like", value, "sendContent");
            return (Criteria) this;
        }

        public Criteria andSendContentIn(List<String> values) {
            addCriterion("send_content in", values, "sendContent");
            return (Criteria) this;
        }

        public Criteria andSendContentNotIn(List<String> values) {
            addCriterion("send_content not in", values, "sendContent");
            return (Criteria) this;
        }

        public Criteria andSendContentBetween(String value1, String value2) {
            addCriterion("send_content between", value1, value2, "sendContent");
            return (Criteria) this;
        }

        public Criteria andSendContentNotBetween(String value1, String value2) {
            addCriterion("send_content not between", value1, value2, "sendContent");
            return (Criteria) this;
        }

        public Criteria andBackContentIsNull() {
            addCriterion("back_content is null");
            return (Criteria) this;
        }

        public Criteria andBackContentIsNotNull() {
            addCriterion("back_content is not null");
            return (Criteria) this;
        }

        public Criteria andBackContentEqualTo(String value) {
            addCriterion("back_content =", value, "backContent");
            return (Criteria) this;
        }

        public Criteria andBackContentNotEqualTo(String value) {
            addCriterion("back_content <>", value, "backContent");
            return (Criteria) this;
        }

        public Criteria andBackContentGreaterThan(String value) {
            addCriterion("back_content >", value, "backContent");
            return (Criteria) this;
        }

        public Criteria andBackContentGreaterThanOrEqualTo(String value) {
            addCriterion("back_content >=", value, "backContent");
            return (Criteria) this;
        }

        public Criteria andBackContentLessThan(String value) {
            addCriterion("back_content <", value, "backContent");
            return (Criteria) this;
        }

        public Criteria andBackContentLessThanOrEqualTo(String value) {
            addCriterion("back_content <=", value, "backContent");
            return (Criteria) this;
        }

        public Criteria andBackContentLike(String value) {
            addCriterion("back_content like", value, "backContent");
            return (Criteria) this;
        }

        public Criteria andBackContentNotLike(String value) {
            addCriterion("back_content not like", value, "backContent");
            return (Criteria) this;
        }

        public Criteria andBackContentIn(List<String> values) {
            addCriterion("back_content in", values, "backContent");
            return (Criteria) this;
        }

        public Criteria andBackContentNotIn(List<String> values) {
            addCriterion("back_content not in", values, "backContent");
            return (Criteria) this;
        }

        public Criteria andBackContentBetween(String value1, String value2) {
            addCriterion("back_content between", value1, value2, "backContent");
            return (Criteria) this;
        }

        public Criteria andBackContentNotBetween(String value1, String value2) {
            addCriterion("back_content not between", value1, value2, "backContent");
            return (Criteria) this;
        }

        public Criteria andRateIsNull() {
            addCriterion("rate is null");
            return (Criteria) this;
        }

        public Criteria andRateIsNotNull() {
            addCriterion("rate is not null");
            return (Criteria) this;
        }

        public Criteria andRateEqualTo(Integer value) {
            addCriterion("rate =", value, "rate");
            return (Criteria) this;
        }

        public Criteria andRateNotEqualTo(Integer value) {
            addCriterion("rate <>", value, "rate");
            return (Criteria) this;
        }

        public Criteria andRateGreaterThan(Integer value) {
            addCriterion("rate >", value, "rate");
            return (Criteria) this;
        }

        public Criteria andRateGreaterThanOrEqualTo(Integer value) {
            addCriterion("rate >=", value, "rate");
            return (Criteria) this;
        }

        public Criteria andRateLessThan(Integer value) {
            addCriterion("rate <", value, "rate");
            return (Criteria) this;
        }

        public Criteria andRateLessThanOrEqualTo(Integer value) {
            addCriterion("rate <=", value, "rate");
            return (Criteria) this;
        }

        public Criteria andRateIn(List<Integer> values) {
            addCriterion("rate in", values, "rate");
            return (Criteria) this;
        }

        public Criteria andRateNotIn(List<Integer> values) {
            addCriterion("rate not in", values, "rate");
            return (Criteria) this;
        }

        public Criteria andRateBetween(Integer value1, Integer value2) {
            addCriterion("rate between", value1, value2, "rate");
            return (Criteria) this;
        }

        public Criteria andRateNotBetween(Integer value1, Integer value2) {
            addCriterion("rate not between", value1, value2, "rate");
            return (Criteria) this;
        }

        public Criteria andSuggestIsNull() {
            addCriterion("suggest is null");
            return (Criteria) this;
        }

        public Criteria andSuggestIsNotNull() {
            addCriterion("suggest is not null");
            return (Criteria) this;
        }

        public Criteria andSuggestEqualTo(String value) {
            addCriterion("suggest =", value, "suggest");
            return (Criteria) this;
        }

        public Criteria andSuggestNotEqualTo(String value) {
            addCriterion("suggest <>", value, "suggest");
            return (Criteria) this;
        }

        public Criteria andSuggestGreaterThan(String value) {
            addCriterion("suggest >", value, "suggest");
            return (Criteria) this;
        }

        public Criteria andSuggestGreaterThanOrEqualTo(String value) {
            addCriterion("suggest >=", value, "suggest");
            return (Criteria) this;
        }

        public Criteria andSuggestLessThan(String value) {
            addCriterion("suggest <", value, "suggest");
            return (Criteria) this;
        }

        public Criteria andSuggestLessThanOrEqualTo(String value) {
            addCriterion("suggest <=", value, "suggest");
            return (Criteria) this;
        }

        public Criteria andSuggestLike(String value) {
            addCriterion("suggest like", value, "suggest");
            return (Criteria) this;
        }

        public Criteria andSuggestNotLike(String value) {
            addCriterion("suggest not like", value, "suggest");
            return (Criteria) this;
        }

        public Criteria andSuggestIn(List<String> values) {
            addCriterion("suggest in", values, "suggest");
            return (Criteria) this;
        }

        public Criteria andSuggestNotIn(List<String> values) {
            addCriterion("suggest not in", values, "suggest");
            return (Criteria) this;
        }

        public Criteria andSuggestBetween(String value1, String value2) {
            addCriterion("suggest between", value1, value2, "suggest");
            return (Criteria) this;
        }

        public Criteria andSuggestNotBetween(String value1, String value2) {
            addCriterion("suggest not between", value1, value2, "suggest");
            return (Criteria) this;
        }

        public Criteria andPatternIdIsNull() {
            addCriterion("pattern_id is null");
            return (Criteria) this;
        }

        public Criteria andPatternIdIsNotNull() {
            addCriterion("pattern_id is not null");
            return (Criteria) this;
        }

        public Criteria andPatternIdEqualTo(Long value) {
            addCriterion("pattern_id =", value, "patternId");
            return (Criteria) this;
        }

        public Criteria andPatternIdNotEqualTo(Long value) {
            addCriterion("pattern_id <>", value, "patternId");
            return (Criteria) this;
        }

        public Criteria andPatternIdGreaterThan(Long value) {
            addCriterion("pattern_id >", value, "patternId");
            return (Criteria) this;
        }

        public Criteria andPatternIdGreaterThanOrEqualTo(Long value) {
            addCriterion("pattern_id >=", value, "patternId");
            return (Criteria) this;
        }

        public Criteria andPatternIdLessThan(Long value) {
            addCriterion("pattern_id <", value, "patternId");
            return (Criteria) this;
        }

        public Criteria andPatternIdLessThanOrEqualTo(Long value) {
            addCriterion("pattern_id <=", value, "patternId");
            return (Criteria) this;
        }

        public Criteria andPatternIdIn(List<Long> values) {
            addCriterion("pattern_id in", values, "patternId");
            return (Criteria) this;
        }

        public Criteria andPatternIdNotIn(List<Long> values) {
            addCriterion("pattern_id not in", values, "patternId");
            return (Criteria) this;
        }

        public Criteria andPatternIdBetween(Long value1, Long value2) {
            addCriterion("pattern_id between", value1, value2, "patternId");
            return (Criteria) this;
        }

        public Criteria andPatternIdNotBetween(Long value1, Long value2) {
            addCriterion("pattern_id not between", value1, value2, "patternId");
            return (Criteria) this;
        }

        public Criteria andServiceIdIsNull() {
            addCriterion("service_id is null");
            return (Criteria) this;
        }

        public Criteria andServiceIdIsNotNull() {
            addCriterion("service_id is not null");
            return (Criteria) this;
        }

        public Criteria andServiceIdEqualTo(Long value) {
            addCriterion("service_id =", value, "serviceId");
            return (Criteria) this;
        }

        public Criteria andServiceIdNotEqualTo(Long value) {
            addCriterion("service_id <>", value, "serviceId");
            return (Criteria) this;
        }

        public Criteria andServiceIdGreaterThan(Long value) {
            addCriterion("service_id >", value, "serviceId");
            return (Criteria) this;
        }

        public Criteria andServiceIdGreaterThanOrEqualTo(Long value) {
            addCriterion("service_id >=", value, "serviceId");
            return (Criteria) this;
        }

        public Criteria andServiceIdLessThan(Long value) {
            addCriterion("service_id <", value, "serviceId");
            return (Criteria) this;
        }

        public Criteria andServiceIdLessThanOrEqualTo(Long value) {
            addCriterion("service_id <=", value, "serviceId");
            return (Criteria) this;
        }

        public Criteria andServiceIdIn(List<Long> values) {
            addCriterion("service_id in", values, "serviceId");
            return (Criteria) this;
        }

        public Criteria andServiceIdNotIn(List<Long> values) {
            addCriterion("service_id not in", values, "serviceId");
            return (Criteria) this;
        }

        public Criteria andServiceIdBetween(Long value1, Long value2) {
            addCriterion("service_id between", value1, value2, "serviceId");
            return (Criteria) this;
        }

        public Criteria andServiceIdNotBetween(Long value1, Long value2) {
            addCriterion("service_id not between", value1, value2, "serviceId");
            return (Criteria) this;
        }

        public Criteria andFeedbackIdIsNull() {
            addCriterion("feedback_id is null");
            return (Criteria) this;
        }

        public Criteria andFeedbackIdIsNotNull() {
            addCriterion("feedback_id is not null");
            return (Criteria) this;
        }

        public Criteria andFeedbackIdEqualTo(String value) {
            addCriterion("feedback_id =", value, "feedbackId");
            return (Criteria) this;
        }

        public Criteria andFeedbackIdNotEqualTo(String value) {
            addCriterion("feedback_id <>", value, "feedbackId");
            return (Criteria) this;
        }

        public Criteria andFeedbackIdGreaterThan(String value) {
            addCriterion("feedback_id >", value, "feedbackId");
            return (Criteria) this;
        }

        public Criteria andFeedbackIdGreaterThanOrEqualTo(String value) {
            addCriterion("feedback_id >=", value, "feedbackId");
            return (Criteria) this;
        }

        public Criteria andFeedbackIdLessThan(String value) {
            addCriterion("feedback_id <", value, "feedbackId");
            return (Criteria) this;
        }

        public Criteria andFeedbackIdLessThanOrEqualTo(String value) {
            addCriterion("feedback_id <=", value, "feedbackId");
            return (Criteria) this;
        }

        public Criteria andFeedbackIdLike(String value) {
            addCriterion("feedback_id like", value, "feedbackId");
            return (Criteria) this;
        }

        public Criteria andFeedbackIdNotLike(String value) {
            addCriterion("feedback_id not like", value, "feedbackId");
            return (Criteria) this;
        }

        public Criteria andFeedbackIdIn(List<String> values) {
            addCriterion("feedback_id in", values, "feedbackId");
            return (Criteria) this;
        }

        public Criteria andFeedbackIdNotIn(List<String> values) {
            addCriterion("feedback_id not in", values, "feedbackId");
            return (Criteria) this;
        }

        public Criteria andFeedbackIdBetween(String value1, String value2) {
            addCriterion("feedback_id between", value1, value2, "feedbackId");
            return (Criteria) this;
        }

        public Criteria andFeedbackIdNotBetween(String value1, String value2) {
            addCriterion("feedback_id not between", value1, value2, "feedbackId");
            return (Criteria) this;
        }

        public Criteria andFeedbackEmpidIsNull() {
            addCriterion("feedback_empid is null");
            return (Criteria) this;
        }

        public Criteria andFeedbackEmpidIsNotNull() {
            addCriterion("feedback_empid is not null");
            return (Criteria) this;
        }

        public Criteria andFeedbackEmpidEqualTo(String value) {
            addCriterion("feedback_empid =", value, "feedbackEmpid");
            return (Criteria) this;
        }

        public Criteria andFeedbackEmpidNotEqualTo(String value) {
            addCriterion("feedback_empid <>", value, "feedbackEmpid");
            return (Criteria) this;
        }

        public Criteria andFeedbackEmpidGreaterThan(String value) {
            addCriterion("feedback_empid >", value, "feedbackEmpid");
            return (Criteria) this;
        }

        public Criteria andFeedbackEmpidGreaterThanOrEqualTo(String value) {
            addCriterion("feedback_empid >=", value, "feedbackEmpid");
            return (Criteria) this;
        }

        public Criteria andFeedbackEmpidLessThan(String value) {
            addCriterion("feedback_empid <", value, "feedbackEmpid");
            return (Criteria) this;
        }

        public Criteria andFeedbackEmpidLessThanOrEqualTo(String value) {
            addCriterion("feedback_empid <=", value, "feedbackEmpid");
            return (Criteria) this;
        }

        public Criteria andFeedbackEmpidLike(String value) {
            addCriterion("feedback_empid like", value, "feedbackEmpid");
            return (Criteria) this;
        }

        public Criteria andFeedbackEmpidNotLike(String value) {
            addCriterion("feedback_empid not like", value, "feedbackEmpid");
            return (Criteria) this;
        }

        public Criteria andFeedbackEmpidIn(List<String> values) {
            addCriterion("feedback_empid in", values, "feedbackEmpid");
            return (Criteria) this;
        }

        public Criteria andFeedbackEmpidNotIn(List<String> values) {
            addCriterion("feedback_empid not in", values, "feedbackEmpid");
            return (Criteria) this;
        }

        public Criteria andFeedbackEmpidBetween(String value1, String value2) {
            addCriterion("feedback_empid between", value1, value2, "feedbackEmpid");
            return (Criteria) this;
        }

        public Criteria andFeedbackEmpidNotBetween(String value1, String value2) {
            addCriterion("feedback_empid not between", value1, value2, "feedbackEmpid");
            return (Criteria) this;
        }

        public Criteria andConversationIdIsNull() {
            addCriterion("conversation_id is null");
            return (Criteria) this;
        }

        public Criteria andConversationIdIsNotNull() {
            addCriterion("conversation_id is not null");
            return (Criteria) this;
        }

        public Criteria andConversationIdEqualTo(String value) {
            addCriterion("conversation_id =", value, "conversationId");
            return (Criteria) this;
        }

        public Criteria andConversationIdNotEqualTo(String value) {
            addCriterion("conversation_id <>", value, "conversationId");
            return (Criteria) this;
        }

        public Criteria andConversationIdGreaterThan(String value) {
            addCriterion("conversation_id >", value, "conversationId");
            return (Criteria) this;
        }

        public Criteria andConversationIdGreaterThanOrEqualTo(String value) {
            addCriterion("conversation_id >=", value, "conversationId");
            return (Criteria) this;
        }

        public Criteria andConversationIdLessThan(String value) {
            addCriterion("conversation_id <", value, "conversationId");
            return (Criteria) this;
        }

        public Criteria andConversationIdLessThanOrEqualTo(String value) {
            addCriterion("conversation_id <=", value, "conversationId");
            return (Criteria) this;
        }

        public Criteria andConversationIdLike(String value) {
            addCriterion("conversation_id like", value, "conversationId");
            return (Criteria) this;
        }

        public Criteria andConversationIdNotLike(String value) {
            addCriterion("conversation_id not like", value, "conversationId");
            return (Criteria) this;
        }

        public Criteria andConversationIdIn(List<String> values) {
            addCriterion("conversation_id in", values, "conversationId");
            return (Criteria) this;
        }

        public Criteria andConversationIdNotIn(List<String> values) {
            addCriterion("conversation_id not in", values, "conversationId");
            return (Criteria) this;
        }

        public Criteria andConversationIdBetween(String value1, String value2) {
            addCriterion("conversation_id between", value1, value2, "conversationId");
            return (Criteria) this;
        }

        public Criteria andConversationIdNotBetween(String value1, String value2) {
            addCriterion("conversation_id not between", value1, value2, "conversationId");
            return (Criteria) this;
        }

        public Criteria andCategoryLikeInsensitive(String value) {
            addCriterion("upper(category) like", value.toUpperCase(), "category");
            return (Criteria) this;
        }

        public Criteria andSenderNickLikeInsensitive(String value) {
            addCriterion("upper(sender_nick) like", value.toUpperCase(), "senderNick");
            return (Criteria) this;
        }

        public Criteria andSenderIdLikeInsensitive(String value) {
            addCriterion("upper(sender_id) like", value.toUpperCase(), "senderId");
            return (Criteria) this;
        }

        public Criteria andSenderEmpidLikeInsensitive(String value) {
            addCriterion("upper(sender_empid) like", value.toUpperCase(), "senderEmpid");
            return (Criteria) this;
        }

        public Criteria andConversationTitleLikeInsensitive(String value) {
            addCriterion("upper(conversation_title) like", value.toUpperCase(), "conversationTitle");
            return (Criteria) this;
        }

        public Criteria andSendContentLikeInsensitive(String value) {
            addCriterion("upper(send_content) like", value.toUpperCase(), "sendContent");
            return (Criteria) this;
        }

        public Criteria andBackContentLikeInsensitive(String value) {
            addCriterion("upper(back_content) like", value.toUpperCase(), "backContent");
            return (Criteria) this;
        }

        public Criteria andSuggestLikeInsensitive(String value) {
            addCriterion("upper(suggest) like", value.toUpperCase(), "suggest");
            return (Criteria) this;
        }

        public Criteria andFeedbackIdLikeInsensitive(String value) {
            addCriterion("upper(feedback_id) like", value.toUpperCase(), "feedbackId");
            return (Criteria) this;
        }

        public Criteria andFeedbackEmpidLikeInsensitive(String value) {
            addCriterion("upper(feedback_empid) like", value.toUpperCase(), "feedbackEmpid");
            return (Criteria) this;
        }

        public Criteria andConversationIdLikeInsensitive(String value) {
            addCriterion("upper(conversation_id) like", value.toUpperCase(), "conversationId");
            return (Criteria) this;
        }
    }

    public static class Criteria extends GeneratedCriteria {

        protected Criteria() {
            super();
        }
    }

    public static class Criterion {
        private String condition;

        private Object value;

        private Object secondValue;

        private boolean noValue;

        private boolean singleValue;

        private boolean betweenValue;

        private boolean listValue;

        private String typeHandler;

        public String getCondition() {
            return condition;
        }

        public Object getValue() {
            return value;
        }

        public Object getSecondValue() {
            return secondValue;
        }

        public boolean isNoValue() {
            return noValue;
        }

        public boolean isSingleValue() {
            return singleValue;
        }

        public boolean isBetweenValue() {
            return betweenValue;
        }

        public boolean isListValue() {
            return listValue;
        }

        public String getTypeHandler() {
            return typeHandler;
        }

        protected Criterion(String condition) {
            super();
            this.condition = condition;
            this.typeHandler = null;
            this.noValue = true;
        }

        protected Criterion(String condition, Object value, String typeHandler) {
            super();
            this.condition = condition;
            this.value = value;
            this.typeHandler = typeHandler;
            if (value instanceof List<?>) {
                this.listValue = true;
            } else {
                this.singleValue = true;
            }
        }

        protected Criterion(String condition, Object value) {
            this(condition, value, null);
        }

        protected Criterion(String condition, Object value, Object secondValue, String typeHandler) {
            super();
            this.condition = condition;
            this.value = value;
            this.secondValue = secondValue;
            this.typeHandler = typeHandler;
            this.betweenValue = true;
        }

        protected Criterion(String condition, Object value, Object secondValue) {
            this(condition, value, secondValue, null);
        }
    }
}