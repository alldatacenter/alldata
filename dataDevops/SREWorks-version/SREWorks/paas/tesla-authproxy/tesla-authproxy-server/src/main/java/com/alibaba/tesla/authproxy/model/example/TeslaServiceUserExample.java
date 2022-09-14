package com.alibaba.tesla.authproxy.model.example;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author cdx
 * @date 2019/10/10
 */
public class TeslaServiceUserExample {
    protected String orderByClause;

    protected boolean distinct;

    protected List<Criteria> oredCriteria;

    public TeslaServiceUserExample() {
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

        public Criteria andUseridIsNull() {
            addCriterion("userid is null");
            return (Criteria) this;
        }

        public Criteria andUseridIsNotNull() {
            addCriterion("userid is not null");
            return (Criteria) this;
        }

        public Criteria andUseridEqualTo(Integer value) {
            addCriterion("userid =", value, "userid");
            return (Criteria) this;
        }

        public Criteria andUseridNotEqualTo(Integer value) {
            addCriterion("userid <>", value, "userid");
            return (Criteria) this;
        }

        public Criteria andUseridGreaterThan(Integer value) {
            addCriterion("userid >", value, "userid");
            return (Criteria) this;
        }

        public Criteria andUseridGreaterThanOrEqualTo(Integer value) {
            addCriterion("userid >=", value, "userid");
            return (Criteria) this;
        }

        public Criteria andUseridLessThan(Integer value) {
            addCriterion("userid <", value, "userid");
            return (Criteria) this;
        }

        public Criteria andUseridLessThanOrEqualTo(Integer value) {
            addCriterion("userid <=", value, "userid");
            return (Criteria) this;
        }

        public Criteria andUseridIn(List<Integer> values) {
            addCriterion("userid in", values, "userid");
            return (Criteria) this;
        }

        public Criteria andUseridNotIn(List<Integer> values) {
            addCriterion("userid not in", values, "userid");
            return (Criteria) this;
        }

        public Criteria andUseridBetween(Integer value1, Integer value2) {
            addCriterion("userid between", value1, value2, "userid");
            return (Criteria) this;
        }

        public Criteria andUseridNotBetween(Integer value1, Integer value2) {
            addCriterion("userid not between", value1, value2, "userid");
            return (Criteria) this;
        }

        public Criteria andUsernameIsNull() {
            addCriterion("username is null");
            return (Criteria) this;
        }

        public Criteria andUsernameIsNotNull() {
            addCriterion("username is not null");
            return (Criteria) this;
        }

        public Criteria andUsernameEqualTo(String value) {
            addCriterion("username =", value, "username");
            return (Criteria) this;
        }

        public Criteria andUsernameNotEqualTo(String value) {
            addCriterion("username <>", value, "username");
            return (Criteria) this;
        }

        public Criteria andUsernameGreaterThan(String value) {
            addCriterion("username >", value, "username");
            return (Criteria) this;
        }

        public Criteria andUsernameGreaterThanOrEqualTo(String value) {
            addCriterion("username >=", value, "username");
            return (Criteria) this;
        }

        public Criteria andUsernameLessThan(String value) {
            addCriterion("username <", value, "username");
            return (Criteria) this;
        }

        public Criteria andUsernameLessThanOrEqualTo(String value) {
            addCriterion("username <=", value, "username");
            return (Criteria) this;
        }

        public Criteria andUsernameLike(String value) {
            addCriterion("username like", value, "username");
            return (Criteria) this;
        }

        public Criteria andUsernameNotLike(String value) {
            addCriterion("username not like", value, "username");
            return (Criteria) this;
        }

        public Criteria andUsernameIn(List<String> values) {
            addCriterion("username in", values, "username");
            return (Criteria) this;
        }

        public Criteria andUsernameNotIn(List<String> values) {
            addCriterion("username not in", values, "username");
            return (Criteria) this;
        }

        public Criteria andUsernameBetween(String value1, String value2) {
            addCriterion("username between", value1, value2, "username");
            return (Criteria) this;
        }

        public Criteria andUsernameNotBetween(String value1, String value2) {
            addCriterion("username not between", value1, value2, "username");
            return (Criteria) this;
        }

        public Criteria andEmployeeIdIsNull() {
            addCriterion("employee_id is null");
            return (Criteria) this;
        }

        public Criteria andEmployeeIdIsNotNull() {
            addCriterion("employee_id is not null");
            return (Criteria) this;
        }

        public Criteria andEmployeeIdEqualTo(String value) {
            addCriterion("employee_id =", value, "employeeId");
            return (Criteria) this;
        }

        public Criteria andEmployeeIdNotEqualTo(String value) {
            addCriterion("employee_id <>", value, "employeeId");
            return (Criteria) this;
        }

        public Criteria andEmployeeIdGreaterThan(String value) {
            addCriterion("employee_id >", value, "employeeId");
            return (Criteria) this;
        }

        public Criteria andEmployeeIdGreaterThanOrEqualTo(String value) {
            addCriterion("employee_id >=", value, "employeeId");
            return (Criteria) this;
        }

        public Criteria andEmployeeIdLessThan(String value) {
            addCriterion("employee_id <", value, "employeeId");
            return (Criteria) this;
        }

        public Criteria andEmployeeIdLessThanOrEqualTo(String value) {
            addCriterion("employee_id <=", value, "employeeId");
            return (Criteria) this;
        }

        public Criteria andEmployeeIdLike(String value) {
            addCriterion("employee_id like", value, "employeeId");
            return (Criteria) this;
        }

        public Criteria andEmployeeIdNotLike(String value) {
            addCriterion("employee_id not like", value, "employeeId");
            return (Criteria) this;
        }

        public Criteria andEmployeeIdIn(List<String> values) {
            addCriterion("employee_id in", values, "employeeId");
            return (Criteria) this;
        }

        public Criteria andEmployeeIdNotIn(List<String> values) {
            addCriterion("employee_id not in", values, "employeeId");
            return (Criteria) this;
        }

        public Criteria andEmployeeIdBetween(String value1, String value2) {
            addCriterion("employee_id between", value1, value2, "employeeId");
            return (Criteria) this;
        }

        public Criteria andEmployeeIdNotBetween(String value1, String value2) {
            addCriterion("employee_id not between", value1, value2, "employeeId");
            return (Criteria) this;
        }

        public Criteria andBucUserIdIsNull() {
            addCriterion("buc_user_id is null");
            return (Criteria) this;
        }

        public Criteria andBucUserIdIsNotNull() {
            addCriterion("buc_user_id is not null");
            return (Criteria) this;
        }

        public Criteria andBucUserIdEqualTo(String value) {
            addCriterion("buc_user_id =", value, "bucUserId");
            return (Criteria) this;
        }

        public Criteria andBucUserIdNotEqualTo(String value) {
            addCriterion("buc_user_id <>", value, "bucUserId");
            return (Criteria) this;
        }

        public Criteria andBucUserIdGreaterThan(String value) {
            addCriterion("buc_user_id >", value, "bucUserId");
            return (Criteria) this;
        }

        public Criteria andBucUserIdGreaterThanOrEqualTo(String value) {
            addCriterion("buc_user_id >=", value, "bucUserId");
            return (Criteria) this;
        }

        public Criteria andBucUserIdLessThan(String value) {
            addCriterion("buc_user_id <", value, "bucUserId");
            return (Criteria) this;
        }

        public Criteria andBucUserIdLessThanOrEqualTo(String value) {
            addCriterion("buc_user_id <=", value, "bucUserId");
            return (Criteria) this;
        }

        public Criteria andBucUserIdLike(String value) {
            addCriterion("buc_user_id like", value, "bucUserId");
            return (Criteria) this;
        }

        public Criteria andBucUserIdNotLike(String value) {
            addCriterion("buc_user_id not like", value, "bucUserId");
            return (Criteria) this;
        }

        public Criteria andBucUserIdIn(List<String> values) {
            addCriterion("buc_user_id in", values, "bucUserId");
            return (Criteria) this;
        }

        public Criteria andBucUserIdNotIn(List<String> values) {
            addCriterion("buc_user_id not in", values, "bucUserId");
            return (Criteria) this;
        }

        public Criteria andBucUserIdBetween(String value1, String value2) {
            addCriterion("buc_user_id between", value1, value2, "bucUserId");
            return (Criteria) this;
        }

        public Criteria andBucUserIdNotBetween(String value1, String value2) {
            addCriterion("buc_user_id not between", value1, value2, "bucUserId");
            return (Criteria) this;
        }

        public Criteria andNicknameIsNull() {
            addCriterion("nickname is null");
            return (Criteria) this;
        }

        public Criteria andNicknameIsNotNull() {
            addCriterion("nickname is not null");
            return (Criteria) this;
        }

        public Criteria andNicknameEqualTo(String value) {
            addCriterion("nickname =", value, "nickname");
            return (Criteria) this;
        }

        public Criteria andNicknameNotEqualTo(String value) {
            addCriterion("nickname <>", value, "nickname");
            return (Criteria) this;
        }

        public Criteria andNicknameGreaterThan(String value) {
            addCriterion("nickname >", value, "nickname");
            return (Criteria) this;
        }

        public Criteria andNicknameGreaterThanOrEqualTo(String value) {
            addCriterion("nickname >=", value, "nickname");
            return (Criteria) this;
        }

        public Criteria andNicknameLessThan(String value) {
            addCriterion("nickname <", value, "nickname");
            return (Criteria) this;
        }

        public Criteria andNicknameLessThanOrEqualTo(String value) {
            addCriterion("nickname <=", value, "nickname");
            return (Criteria) this;
        }

        public Criteria andNicknameLike(String value) {
            addCriterion("nickname like", value, "nickname");
            return (Criteria) this;
        }

        public Criteria andNicknameNotLike(String value) {
            addCriterion("nickname not like", value, "nickname");
            return (Criteria) this;
        }

        public Criteria andNicknameIn(List<String> values) {
            addCriterion("nickname in", values, "nickname");
            return (Criteria) this;
        }

        public Criteria andNicknameNotIn(List<String> values) {
            addCriterion("nickname not in", values, "nickname");
            return (Criteria) this;
        }

        public Criteria andNicknameBetween(String value1, String value2) {
            addCriterion("nickname between", value1, value2, "nickname");
            return (Criteria) this;
        }

        public Criteria andNicknameNotBetween(String value1, String value2) {
            addCriterion("nickname not between", value1, value2, "nickname");
            return (Criteria) this;
        }

        public Criteria andNicknamePinyinIsNull() {
            addCriterion("nickname_pinyin is null");
            return (Criteria) this;
        }

        public Criteria andNicknamePinyinIsNotNull() {
            addCriterion("nickname_pinyin is not null");
            return (Criteria) this;
        }

        public Criteria andNicknamePinyinEqualTo(String value) {
            addCriterion("nickname_pinyin =", value, "nicknamePinyin");
            return (Criteria) this;
        }

        public Criteria andNicknamePinyinNotEqualTo(String value) {
            addCriterion("nickname_pinyin <>", value, "nicknamePinyin");
            return (Criteria) this;
        }

        public Criteria andNicknamePinyinGreaterThan(String value) {
            addCriterion("nickname_pinyin >", value, "nicknamePinyin");
            return (Criteria) this;
        }

        public Criteria andNicknamePinyinGreaterThanOrEqualTo(String value) {
            addCriterion("nickname_pinyin >=", value, "nicknamePinyin");
            return (Criteria) this;
        }

        public Criteria andNicknamePinyinLessThan(String value) {
            addCriterion("nickname_pinyin <", value, "nicknamePinyin");
            return (Criteria) this;
        }

        public Criteria andNicknamePinyinLessThanOrEqualTo(String value) {
            addCriterion("nickname_pinyin <=", value, "nicknamePinyin");
            return (Criteria) this;
        }

        public Criteria andNicknamePinyinLike(String value) {
            addCriterion("nickname_pinyin like", value, "nicknamePinyin");
            return (Criteria) this;
        }

        public Criteria andNicknamePinyinNotLike(String value) {
            addCriterion("nickname_pinyin not like", value, "nicknamePinyin");
            return (Criteria) this;
        }

        public Criteria andNicknamePinyinIn(List<String> values) {
            addCriterion("nickname_pinyin in", values, "nicknamePinyin");
            return (Criteria) this;
        }

        public Criteria andNicknamePinyinNotIn(List<String> values) {
            addCriterion("nickname_pinyin not in", values, "nicknamePinyin");
            return (Criteria) this;
        }

        public Criteria andNicknamePinyinBetween(String value1, String value2) {
            addCriterion("nickname_pinyin between", value1, value2, "nicknamePinyin");
            return (Criteria) this;
        }

        public Criteria andNicknamePinyinNotBetween(String value1, String value2) {
            addCriterion("nickname_pinyin not between", value1, value2, "nicknamePinyin");
            return (Criteria) this;
        }

        public Criteria andNameIsNull() {
            addCriterion("name is null");
            return (Criteria) this;
        }

        public Criteria andNameIsNotNull() {
            addCriterion("name is not null");
            return (Criteria) this;
        }

        public Criteria andNameEqualTo(String value) {
            addCriterion("name =", value, "name");
            return (Criteria) this;
        }

        public Criteria andNameNotEqualTo(String value) {
            addCriterion("name <>", value, "name");
            return (Criteria) this;
        }

        public Criteria andNameGreaterThan(String value) {
            addCriterion("name >", value, "name");
            return (Criteria) this;
        }

        public Criteria andNameGreaterThanOrEqualTo(String value) {
            addCriterion("name >=", value, "name");
            return (Criteria) this;
        }

        public Criteria andNameLessThan(String value) {
            addCriterion("name <", value, "name");
            return (Criteria) this;
        }

        public Criteria andNameLessThanOrEqualTo(String value) {
            addCriterion("name <=", value, "name");
            return (Criteria) this;
        }

        public Criteria andNameLike(String value) {
            addCriterion("name like", value, "name");
            return (Criteria) this;
        }

        public Criteria andNameNotLike(String value) {
            addCriterion("name not like", value, "name");
            return (Criteria) this;
        }

        public Criteria andNameIn(List<String> values) {
            addCriterion("name in", values, "name");
            return (Criteria) this;
        }

        public Criteria andNameNotIn(List<String> values) {
            addCriterion("name not in", values, "name");
            return (Criteria) this;
        }

        public Criteria andNameBetween(String value1, String value2) {
            addCriterion("name between", value1, value2, "name");
            return (Criteria) this;
        }

        public Criteria andNameNotBetween(String value1, String value2) {
            addCriterion("name not between", value1, value2, "name");
            return (Criteria) this;
        }

        public Criteria andSecretkeyIsNull() {
            addCriterion("secretkey is null");
            return (Criteria) this;
        }

        public Criteria andSecretkeyIsNotNull() {
            addCriterion("secretkey is not null");
            return (Criteria) this;
        }

        public Criteria andSecretkeyEqualTo(String value) {
            addCriterion("secretkey =", value, "secretkey");
            return (Criteria) this;
        }

        public Criteria andSecretkeyNotEqualTo(String value) {
            addCriterion("secretkey <>", value, "secretkey");
            return (Criteria) this;
        }

        public Criteria andSecretkeyGreaterThan(String value) {
            addCriterion("secretkey >", value, "secretkey");
            return (Criteria) this;
        }

        public Criteria andSecretkeyGreaterThanOrEqualTo(String value) {
            addCriterion("secretkey >=", value, "secretkey");
            return (Criteria) this;
        }

        public Criteria andSecretkeyLessThan(String value) {
            addCriterion("secretkey <", value, "secretkey");
            return (Criteria) this;
        }

        public Criteria andSecretkeyLessThanOrEqualTo(String value) {
            addCriterion("secretkey <=", value, "secretkey");
            return (Criteria) this;
        }

        public Criteria andSecretkeyLike(String value) {
            addCriterion("secretkey like", value, "secretkey");
            return (Criteria) this;
        }

        public Criteria andSecretkeyNotLike(String value) {
            addCriterion("secretkey not like", value, "secretkey");
            return (Criteria) this;
        }

        public Criteria andSecretkeyIn(List<String> values) {
            addCriterion("secretkey in", values, "secretkey");
            return (Criteria) this;
        }

        public Criteria andSecretkeyNotIn(List<String> values) {
            addCriterion("secretkey not in", values, "secretkey");
            return (Criteria) this;
        }

        public Criteria andSecretkeyBetween(String value1, String value2) {
            addCriterion("secretkey between", value1, value2, "secretkey");
            return (Criteria) this;
        }

        public Criteria andSecretkeyNotBetween(String value1, String value2) {
            addCriterion("secretkey not between", value1, value2, "secretkey");
            return (Criteria) this;
        }

        public Criteria andEmailIsNull() {
            addCriterion("email is null");
            return (Criteria) this;
        }

        public Criteria andEmailIsNotNull() {
            addCriterion("email is not null");
            return (Criteria) this;
        }

        public Criteria andEmailEqualTo(String value) {
            addCriterion("email =", value, "email");
            return (Criteria) this;
        }

        public Criteria andEmailNotEqualTo(String value) {
            addCriterion("email <>", value, "email");
            return (Criteria) this;
        }

        public Criteria andEmailGreaterThan(String value) {
            addCriterion("email >", value, "email");
            return (Criteria) this;
        }

        public Criteria andEmailGreaterThanOrEqualTo(String value) {
            addCriterion("email >=", value, "email");
            return (Criteria) this;
        }

        public Criteria andEmailLessThan(String value) {
            addCriterion("email <", value, "email");
            return (Criteria) this;
        }

        public Criteria andEmailLessThanOrEqualTo(String value) {
            addCriterion("email <=", value, "email");
            return (Criteria) this;
        }

        public Criteria andEmailLike(String value) {
            addCriterion("email like", value, "email");
            return (Criteria) this;
        }

        public Criteria andEmailNotLike(String value) {
            addCriterion("email not like", value, "email");
            return (Criteria) this;
        }

        public Criteria andEmailIn(List<String> values) {
            addCriterion("email in", values, "email");
            return (Criteria) this;
        }

        public Criteria andEmailNotIn(List<String> values) {
            addCriterion("email not in", values, "email");
            return (Criteria) this;
        }

        public Criteria andEmailBetween(String value1, String value2) {
            addCriterion("email between", value1, value2, "email");
            return (Criteria) this;
        }

        public Criteria andEmailNotBetween(String value1, String value2) {
            addCriterion("email not between", value1, value2, "email");
            return (Criteria) this;
        }

        public Criteria andTelephoneIsNull() {
            addCriterion("telephone is null");
            return (Criteria) this;
        }

        public Criteria andTelephoneIsNotNull() {
            addCriterion("telephone is not null");
            return (Criteria) this;
        }

        public Criteria andTelephoneEqualTo(String value) {
            addCriterion("telephone =", value, "telephone");
            return (Criteria) this;
        }

        public Criteria andTelephoneNotEqualTo(String value) {
            addCriterion("telephone <>", value, "telephone");
            return (Criteria) this;
        }

        public Criteria andTelephoneGreaterThan(String value) {
            addCriterion("telephone >", value, "telephone");
            return (Criteria) this;
        }

        public Criteria andTelephoneGreaterThanOrEqualTo(String value) {
            addCriterion("telephone >=", value, "telephone");
            return (Criteria) this;
        }

        public Criteria andTelephoneLessThan(String value) {
            addCriterion("telephone <", value, "telephone");
            return (Criteria) this;
        }

        public Criteria andTelephoneLessThanOrEqualTo(String value) {
            addCriterion("telephone <=", value, "telephone");
            return (Criteria) this;
        }

        public Criteria andTelephoneLike(String value) {
            addCriterion("telephone like", value, "telephone");
            return (Criteria) this;
        }

        public Criteria andTelephoneNotLike(String value) {
            addCriterion("telephone not like", value, "telephone");
            return (Criteria) this;
        }

        public Criteria andTelephoneIn(List<String> values) {
            addCriterion("telephone in", values, "telephone");
            return (Criteria) this;
        }

        public Criteria andTelephoneNotIn(List<String> values) {
            addCriterion("telephone not in", values, "telephone");
            return (Criteria) this;
        }

        public Criteria andTelephoneBetween(String value1, String value2) {
            addCriterion("telephone between", value1, value2, "telephone");
            return (Criteria) this;
        }

        public Criteria andTelephoneNotBetween(String value1, String value2) {
            addCriterion("telephone not between", value1, value2, "telephone");
            return (Criteria) this;
        }

        public Criteria andMobilephoneIsNull() {
            addCriterion("mobilephone is null");
            return (Criteria) this;
        }

        public Criteria andMobilephoneIsNotNull() {
            addCriterion("mobilephone is not null");
            return (Criteria) this;
        }

        public Criteria andMobilephoneEqualTo(String value) {
            addCriterion("mobilephone =", value, "mobilephone");
            return (Criteria) this;
        }

        public Criteria andMobilephoneNotEqualTo(String value) {
            addCriterion("mobilephone <>", value, "mobilephone");
            return (Criteria) this;
        }

        public Criteria andMobilephoneGreaterThan(String value) {
            addCriterion("mobilephone >", value, "mobilephone");
            return (Criteria) this;
        }

        public Criteria andMobilephoneGreaterThanOrEqualTo(String value) {
            addCriterion("mobilephone >=", value, "mobilephone");
            return (Criteria) this;
        }

        public Criteria andMobilephoneLessThan(String value) {
            addCriterion("mobilephone <", value, "mobilephone");
            return (Criteria) this;
        }

        public Criteria andMobilephoneLessThanOrEqualTo(String value) {
            addCriterion("mobilephone <=", value, "mobilephone");
            return (Criteria) this;
        }

        public Criteria andMobilephoneLike(String value) {
            addCriterion("mobilephone like", value, "mobilephone");
            return (Criteria) this;
        }

        public Criteria andMobilephoneNotLike(String value) {
            addCriterion("mobilephone not like", value, "mobilephone");
            return (Criteria) this;
        }

        public Criteria andMobilephoneIn(List<String> values) {
            addCriterion("mobilephone in", values, "mobilephone");
            return (Criteria) this;
        }

        public Criteria andMobilephoneNotIn(List<String> values) {
            addCriterion("mobilephone not in", values, "mobilephone");
            return (Criteria) this;
        }

        public Criteria andMobilephoneBetween(String value1, String value2) {
            addCriterion("mobilephone between", value1, value2, "mobilephone");
            return (Criteria) this;
        }

        public Criteria andMobilephoneNotBetween(String value1, String value2) {
            addCriterion("mobilephone not between", value1, value2, "mobilephone");
            return (Criteria) this;
        }

        public Criteria andAliwwIsNull() {
            addCriterion("aliww is null");
            return (Criteria) this;
        }

        public Criteria andAliwwIsNotNull() {
            addCriterion("aliww is not null");
            return (Criteria) this;
        }

        public Criteria andAliwwEqualTo(String value) {
            addCriterion("aliww =", value, "aliww");
            return (Criteria) this;
        }

        public Criteria andAliwwNotEqualTo(String value) {
            addCriterion("aliww <>", value, "aliww");
            return (Criteria) this;
        }

        public Criteria andAliwwGreaterThan(String value) {
            addCriterion("aliww >", value, "aliww");
            return (Criteria) this;
        }

        public Criteria andAliwwGreaterThanOrEqualTo(String value) {
            addCriterion("aliww >=", value, "aliww");
            return (Criteria) this;
        }

        public Criteria andAliwwLessThan(String value) {
            addCriterion("aliww <", value, "aliww");
            return (Criteria) this;
        }

        public Criteria andAliwwLessThanOrEqualTo(String value) {
            addCriterion("aliww <=", value, "aliww");
            return (Criteria) this;
        }

        public Criteria andAliwwLike(String value) {
            addCriterion("aliww like", value, "aliww");
            return (Criteria) this;
        }

        public Criteria andAliwwNotLike(String value) {
            addCriterion("aliww not like", value, "aliww");
            return (Criteria) this;
        }

        public Criteria andAliwwIn(List<String> values) {
            addCriterion("aliww in", values, "aliww");
            return (Criteria) this;
        }

        public Criteria andAliwwNotIn(List<String> values) {
            addCriterion("aliww not in", values, "aliww");
            return (Criteria) this;
        }

        public Criteria andAliwwBetween(String value1, String value2) {
            addCriterion("aliww between", value1, value2, "aliww");
            return (Criteria) this;
        }

        public Criteria andAliwwNotBetween(String value1, String value2) {
            addCriterion("aliww not between", value1, value2, "aliww");
            return (Criteria) this;
        }

        public Criteria andIssuperadminIsNull() {
            addCriterion("issuperadmin is null");
            return (Criteria) this;
        }

        public Criteria andIssuperadminIsNotNull() {
            addCriterion("issuperadmin is not null");
            return (Criteria) this;
        }

        public Criteria andIssuperadminEqualTo(Byte value) {
            addCriterion("issuperadmin =", value, "issuperadmin");
            return (Criteria) this;
        }

        public Criteria andIssuperadminNotEqualTo(Byte value) {
            addCriterion("issuperadmin <>", value, "issuperadmin");
            return (Criteria) this;
        }

        public Criteria andIssuperadminGreaterThan(Byte value) {
            addCriterion("issuperadmin >", value, "issuperadmin");
            return (Criteria) this;
        }

        public Criteria andIssuperadminGreaterThanOrEqualTo(Byte value) {
            addCriterion("issuperadmin >=", value, "issuperadmin");
            return (Criteria) this;
        }

        public Criteria andIssuperadminLessThan(Byte value) {
            addCriterion("issuperadmin <", value, "issuperadmin");
            return (Criteria) this;
        }

        public Criteria andIssuperadminLessThanOrEqualTo(Byte value) {
            addCriterion("issuperadmin <=", value, "issuperadmin");
            return (Criteria) this;
        }

        public Criteria andIssuperadminIn(List<Byte> values) {
            addCriterion("issuperadmin in", values, "issuperadmin");
            return (Criteria) this;
        }

        public Criteria andIssuperadminNotIn(List<Byte> values) {
            addCriterion("issuperadmin not in", values, "issuperadmin");
            return (Criteria) this;
        }

        public Criteria andIssuperadminBetween(Byte value1, Byte value2) {
            addCriterion("issuperadmin between", value1, value2, "issuperadmin");
            return (Criteria) this;
        }

        public Criteria andIssuperadminNotBetween(Byte value1, Byte value2) {
            addCriterion("issuperadmin not between", value1, value2, "issuperadmin");
            return (Criteria) this;
        }

        public Criteria andCreatetimeIsNull() {
            addCriterion("createtime is null");
            return (Criteria) this;
        }

        public Criteria andCreatetimeIsNotNull() {
            addCriterion("createtime is not null");
            return (Criteria) this;
        }

        public Criteria andCreatetimeEqualTo(Date value) {
            addCriterion("createtime =", value, "createtime");
            return (Criteria) this;
        }

        public Criteria andCreatetimeNotEqualTo(Date value) {
            addCriterion("createtime <>", value, "createtime");
            return (Criteria) this;
        }

        public Criteria andCreatetimeGreaterThan(Date value) {
            addCriterion("createtime >", value, "createtime");
            return (Criteria) this;
        }

        public Criteria andCreatetimeGreaterThanOrEqualTo(Date value) {
            addCriterion("createtime >=", value, "createtime");
            return (Criteria) this;
        }

        public Criteria andCreatetimeLessThan(Date value) {
            addCriterion("createtime <", value, "createtime");
            return (Criteria) this;
        }

        public Criteria andCreatetimeLessThanOrEqualTo(Date value) {
            addCriterion("createtime <=", value, "createtime");
            return (Criteria) this;
        }

        public Criteria andCreatetimeIn(List<Date> values) {
            addCriterion("createtime in", values, "createtime");
            return (Criteria) this;
        }

        public Criteria andCreatetimeNotIn(List<Date> values) {
            addCriterion("createtime not in", values, "createtime");
            return (Criteria) this;
        }

        public Criteria andCreatetimeBetween(Date value1, Date value2) {
            addCriterion("createtime between", value1, value2, "createtime");
            return (Criteria) this;
        }

        public Criteria andCreatetimeNotBetween(Date value1, Date value2) {
            addCriterion("createtime not between", value1, value2, "createtime");
            return (Criteria) this;
        }

        public Criteria andLogintimeIsNull() {
            addCriterion("logintime is null");
            return (Criteria) this;
        }

        public Criteria andLogintimeIsNotNull() {
            addCriterion("logintime is not null");
            return (Criteria) this;
        }

        public Criteria andLogintimeEqualTo(Date value) {
            addCriterion("logintime =", value, "logintime");
            return (Criteria) this;
        }

        public Criteria andLogintimeNotEqualTo(Date value) {
            addCriterion("logintime <>", value, "logintime");
            return (Criteria) this;
        }

        public Criteria andLogintimeGreaterThan(Date value) {
            addCriterion("logintime >", value, "logintime");
            return (Criteria) this;
        }

        public Criteria andLogintimeGreaterThanOrEqualTo(Date value) {
            addCriterion("logintime >=", value, "logintime");
            return (Criteria) this;
        }

        public Criteria andLogintimeLessThan(Date value) {
            addCriterion("logintime <", value, "logintime");
            return (Criteria) this;
        }

        public Criteria andLogintimeLessThanOrEqualTo(Date value) {
            addCriterion("logintime <=", value, "logintime");
            return (Criteria) this;
        }

        public Criteria andLogintimeIn(List<Date> values) {
            addCriterion("logintime in", values, "logintime");
            return (Criteria) this;
        }

        public Criteria andLogintimeNotIn(List<Date> values) {
            addCriterion("logintime not in", values, "logintime");
            return (Criteria) this;
        }

        public Criteria andLogintimeBetween(Date value1, Date value2) {
            addCriterion("logintime between", value1, value2, "logintime");
            return (Criteria) this;
        }

        public Criteria andLogintimeNotBetween(Date value1, Date value2) {
            addCriterion("logintime not between", value1, value2, "logintime");
            return (Criteria) this;
        }

        public Criteria andValidflagIsNull() {
            addCriterion("validflag is null");
            return (Criteria) this;
        }

        public Criteria andValidflagIsNotNull() {
            addCriterion("validflag is not null");
            return (Criteria) this;
        }

        public Criteria andValidflagEqualTo(Byte value) {
            addCriterion("validflag =", value, "validflag");
            return (Criteria) this;
        }

        public Criteria andValidflagNotEqualTo(Byte value) {
            addCriterion("validflag <>", value, "validflag");
            return (Criteria) this;
        }

        public Criteria andValidflagGreaterThan(Byte value) {
            addCriterion("validflag >", value, "validflag");
            return (Criteria) this;
        }

        public Criteria andValidflagGreaterThanOrEqualTo(Byte value) {
            addCriterion("validflag >=", value, "validflag");
            return (Criteria) this;
        }

        public Criteria andValidflagLessThan(Byte value) {
            addCriterion("validflag <", value, "validflag");
            return (Criteria) this;
        }

        public Criteria andValidflagLessThanOrEqualTo(Byte value) {
            addCriterion("validflag <=", value, "validflag");
            return (Criteria) this;
        }

        public Criteria andValidflagIn(List<Byte> values) {
            addCriterion("validflag in", values, "validflag");
            return (Criteria) this;
        }

        public Criteria andValidflagNotIn(List<Byte> values) {
            addCriterion("validflag not in", values, "validflag");
            return (Criteria) this;
        }

        public Criteria andValidflagBetween(Byte value1, Byte value2) {
            addCriterion("validflag between", value1, value2, "validflag");
            return (Criteria) this;
        }

        public Criteria andValidflagNotBetween(Byte value1, Byte value2) {
            addCriterion("validflag not between", value1, value2, "validflag");
            return (Criteria) this;
        }

        public Criteria andIsPublicAccountIsNull() {
            addCriterion("is_public_account is null");
            return (Criteria) this;
        }

        public Criteria andIsPublicAccountIsNotNull() {
            addCriterion("is_public_account is not null");
            return (Criteria) this;
        }

        public Criteria andIsPublicAccountEqualTo(Byte value) {
            addCriterion("is_public_account =", value, "isPublicAccount");
            return (Criteria) this;
        }

        public Criteria andIsPublicAccountNotEqualTo(Byte value) {
            addCriterion("is_public_account <>", value, "isPublicAccount");
            return (Criteria) this;
        }

        public Criteria andIsPublicAccountGreaterThan(Byte value) {
            addCriterion("is_public_account >", value, "isPublicAccount");
            return (Criteria) this;
        }

        public Criteria andIsPublicAccountGreaterThanOrEqualTo(Byte value) {
            addCriterion("is_public_account >=", value, "isPublicAccount");
            return (Criteria) this;
        }

        public Criteria andIsPublicAccountLessThan(Byte value) {
            addCriterion("is_public_account <", value, "isPublicAccount");
            return (Criteria) this;
        }

        public Criteria andIsPublicAccountLessThanOrEqualTo(Byte value) {
            addCriterion("is_public_account <=", value, "isPublicAccount");
            return (Criteria) this;
        }

        public Criteria andIsPublicAccountIn(List<Byte> values) {
            addCriterion("is_public_account in", values, "isPublicAccount");
            return (Criteria) this;
        }

        public Criteria andIsPublicAccountNotIn(List<Byte> values) {
            addCriterion("is_public_account not in", values, "isPublicAccount");
            return (Criteria) this;
        }

        public Criteria andIsPublicAccountBetween(Byte value1, Byte value2) {
            addCriterion("is_public_account between", value1, value2, "isPublicAccount");
            return (Criteria) this;
        }

        public Criteria andIsPublicAccountNotBetween(Byte value1, Byte value2) {
            addCriterion("is_public_account not between", value1, value2, "isPublicAccount");
            return (Criteria) this;
        }

        public Criteria andAccountSafeguardIsNull() {
            addCriterion("account_safeguard is null");
            return (Criteria) this;
        }

        public Criteria andAccountSafeguardIsNotNull() {
            addCriterion("account_safeguard is not null");
            return (Criteria) this;
        }

        public Criteria andAccountSafeguardEqualTo(String value) {
            addCriterion("account_safeguard =", value, "accountSafeguard");
            return (Criteria) this;
        }

        public Criteria andAccountSafeguardNotEqualTo(String value) {
            addCriterion("account_safeguard <>", value, "accountSafeguard");
            return (Criteria) this;
        }

        public Criteria andAccountSafeguardGreaterThan(String value) {
            addCriterion("account_safeguard >", value, "accountSafeguard");
            return (Criteria) this;
        }

        public Criteria andAccountSafeguardGreaterThanOrEqualTo(String value) {
            addCriterion("account_safeguard >=", value, "accountSafeguard");
            return (Criteria) this;
        }

        public Criteria andAccountSafeguardLessThan(String value) {
            addCriterion("account_safeguard <", value, "accountSafeguard");
            return (Criteria) this;
        }

        public Criteria andAccountSafeguardLessThanOrEqualTo(String value) {
            addCriterion("account_safeguard <=", value, "accountSafeguard");
            return (Criteria) this;
        }

        public Criteria andAccountSafeguardLike(String value) {
            addCriterion("account_safeguard like", value, "accountSafeguard");
            return (Criteria) this;
        }

        public Criteria andAccountSafeguardNotLike(String value) {
            addCriterion("account_safeguard not like", value, "accountSafeguard");
            return (Criteria) this;
        }

        public Criteria andAccountSafeguardIn(List<String> values) {
            addCriterion("account_safeguard in", values, "accountSafeguard");
            return (Criteria) this;
        }

        public Criteria andAccountSafeguardNotIn(List<String> values) {
            addCriterion("account_safeguard not in", values, "accountSafeguard");
            return (Criteria) this;
        }

        public Criteria andAccountSafeguardBetween(String value1, String value2) {
            addCriterion("account_safeguard between", value1, value2, "accountSafeguard");
            return (Criteria) this;
        }

        public Criteria andAccountSafeguardNotBetween(String value1, String value2) {
            addCriterion("account_safeguard not between", value1, value2, "accountSafeguard");
            return (Criteria) this;
        }

        public Criteria andUsernameLikeInsensitive(String value) {
            addCriterion("upper(username) like", value.toUpperCase(), "username");
            return (Criteria) this;
        }

        public Criteria andEmployeeIdLikeInsensitive(String value) {
            addCriterion("upper(employee_id) like", value.toUpperCase(), "employeeId");
            return (Criteria) this;
        }

        public Criteria andBucUserIdLikeInsensitive(String value) {
            addCriterion("upper(buc_user_id) like", value.toUpperCase(), "bucUserId");
            return (Criteria) this;
        }

        public Criteria andNicknameLikeInsensitive(String value) {
            addCriterion("upper(nickname) like", value.toUpperCase(), "nickname");
            return (Criteria) this;
        }

        public Criteria andNicknamePinyinLikeInsensitive(String value) {
            addCriterion("upper(nickname_pinyin) like", value.toUpperCase(), "nicknamePinyin");
            return (Criteria) this;
        }

        public Criteria andNameLikeInsensitive(String value) {
            addCriterion("upper(name) like", value.toUpperCase(), "name");
            return (Criteria) this;
        }

        public Criteria andSecretkeyLikeInsensitive(String value) {
            addCriterion("upper(secretkey) like", value.toUpperCase(), "secretkey");
            return (Criteria) this;
        }

        public Criteria andEmailLikeInsensitive(String value) {
            addCriterion("upper(email) like", value.toUpperCase(), "email");
            return (Criteria) this;
        }

        public Criteria andTelephoneLikeInsensitive(String value) {
            addCriterion("upper(telephone) like", value.toUpperCase(), "telephone");
            return (Criteria) this;
        }

        public Criteria andMobilephoneLikeInsensitive(String value) {
            addCriterion("upper(mobilephone) like", value.toUpperCase(), "mobilephone");
            return (Criteria) this;
        }

        public Criteria andAliwwLikeInsensitive(String value) {
            addCriterion("upper(aliww) like", value.toUpperCase(), "aliww");
            return (Criteria) this;
        }

        public Criteria andAccountSafeguardLikeInsensitive(String value) {
            addCriterion("upper(account_safeguard) like", value.toUpperCase(), "accountSafeguard");
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
