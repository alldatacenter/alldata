package com.alibaba.tesla.authproxy.outbound.aas;

import com.alibaba.tesla.authproxy.Constants;
import com.alibaba.tesla.authproxy.model.UserDO;
import com.alibaba.tesla.authproxy.util.UserUtil;

import java.util.Date;

/**
 * <p>Description: AAS返回的用户信息JSON对一个的Java对象，便于本地进行转换处理 <／p>
 * <p>Copyright: Copyright (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017年5月3日
 */
public class AasUserInfo {

    private String createTime;
    private String aliyunID;
    private String trueName;
    private Integer certType;
    private Integer rsaAkLimit;
    private Integer forceUserLoginMfa;
    private Integer isChannelUser;
    private boolean isMfaChecked;
    private String nationalityCode;
    private String nickName;
    private String taobaoNick;
    private Integer shadowAccount;
    private Integer cookieSkipMfaNotAllowed;
    private String lastLoginTime;
    private String updateTime;
    private String partnerPk;
    private String isCertified;
    private String virtualMfaLimit;
    private String accountAttribute;
    private String IDNumber;
    private String havanaId;
    private String accountFrozen;
    private String email;
    private String accountStructure;
    private String ownerBid;
    private String accountType;
    private String accountCertifyType;
    private String parentPk;
    private String multiBid;
    private String securityMobile;
    private String aliyunPK;

    /**
     * 将当前返回值创建一个新的 UserDO 对象
     */
    public UserDO toTeslaUserDo(String accessKeyId, String accessKeySecret) {
        Date now = new Date();
        UserDO teslaUser = new UserDO();
        teslaUser.setTenantId(Constants.DEFAULT_TENANT_ID);
        teslaUser.setBid("26842");
        teslaUser.setEmail(getEmail());
        teslaUser.setPhone("");
        teslaUser.setAliyunPk(getAliyunPK());
        teslaUser.setLoginName(getAliyunID());
        // if havanaId is null set havanaId equal aliyunId
        teslaUser.setEmpId(getAliyunPK());
        teslaUser.setNickName(null == getNickName() || getNickName().length() == 0 ? getAliyunID() : getNickName());
        teslaUser.setIsFirstLogin((byte) 1);
        teslaUser.setIsLocked((byte) 0);
        teslaUser.setAccessKeyId(accessKeyId);
        teslaUser.setAccessKeySecret(accessKeySecret);
        teslaUser.setStatus(0);
        teslaUser.setGmtCreate(now);
        teslaUser.setGmtModified(now);
        teslaUser.setLastLoginTime(now);
        teslaUser.setUserId(UserUtil.getUserId(teslaUser));
        return teslaUser;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getAliyunID() {
        return aliyunID;
    }

    public void setAliyunID(String aliyunID) {
        this.aliyunID = aliyunID;
    }

    public String getTrueName() {
        return trueName;
    }

    public void setTrueName(String trueName) {
        this.trueName = trueName;
    }

    public Integer getCertType() {
        return certType;
    }

    public void setCertType(Integer certType) {
        this.certType = certType;
    }

    public Integer getRsaAkLimit() {
        return rsaAkLimit;
    }

    public void setRsaAkLimit(Integer rsaAkLimit) {
        this.rsaAkLimit = rsaAkLimit;
    }

    public Integer getForceUserLoginMfa() {
        return forceUserLoginMfa;
    }

    public void setForceUserLoginMfa(Integer forceUserLoginMfa) {
        this.forceUserLoginMfa = forceUserLoginMfa;
    }

    public Integer getIsChannelUser() {
        return isChannelUser;
    }

    public void setIsChannelUser(Integer isChannelUser) {
        this.isChannelUser = isChannelUser;
    }

    public boolean isMfaChecked() {
        return isMfaChecked;
    }

    public void setMfaChecked(boolean isMfaChecked) {
        this.isMfaChecked = isMfaChecked;
    }

    public String getNationalityCode() {
        return nationalityCode;
    }

    public void setNationalityCode(String nationalityCode) {
        this.nationalityCode = nationalityCode;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getTaobaoNick() {
        return taobaoNick;
    }

    public void setTaobaoNick(String taobaoNick) {
        this.taobaoNick = taobaoNick;
    }

    public Integer getShadowAccount() {
        return shadowAccount;
    }

    public void setShadowAccount(Integer shadowAccount) {
        this.shadowAccount = shadowAccount;
    }

    public Integer getCookieSkipMfaNotAllowed() {
        return cookieSkipMfaNotAllowed;
    }

    public void setCookieSkipMfaNotAllowed(Integer cookieSkipMfaNotAllowed) {
        this.cookieSkipMfaNotAllowed = cookieSkipMfaNotAllowed;
    }

    public String getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(String lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public String getPartnerPk() {
        return partnerPk;
    }

    public void setPartnerPk(String partnerPk) {
        this.partnerPk = partnerPk;
    }

    public String getIsCertified() {
        return isCertified;
    }

    public void setIsCertified(String isCertified) {
        this.isCertified = isCertified;
    }

    public String getVirtualMfaLimit() {
        return virtualMfaLimit;
    }

    public void setVirtualMfaLimit(String virtualMfaLimit) {
        this.virtualMfaLimit = virtualMfaLimit;
    }

    public String getAccountAttribute() {
        return accountAttribute;
    }

    public void setAccountAttribute(String accountAttribute) {
        this.accountAttribute = accountAttribute;
    }

    public String getIDNumber() {
        return IDNumber;
    }

    public void setIDNumber(String iDNumber) {
        IDNumber = iDNumber;
    }

    public String getHavanaId() {
        return havanaId;
    }

    public void setHavanaId(String havanaId) {
        this.havanaId = havanaId;
    }

    public String getAccountFrozen() {
        return accountFrozen;
    }

    public void setAccountFrozen(String accountFrozen) {
        this.accountFrozen = accountFrozen;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAccountStructure() {
        return accountStructure;
    }

    public void setAccountStructure(String accountStructure) {
        this.accountStructure = accountStructure;
    }

    public String getOwnerBid() {
        return ownerBid;
    }

    public void setOwnerBid(String ownerBid) {
        this.ownerBid = ownerBid;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getAccountCertifyType() {
        return accountCertifyType;
    }

    public void setAccountCertifyType(String accountCertifyType) {
        this.accountCertifyType = accountCertifyType;
    }

    public String getParentPk() {
        return parentPk;
    }

    public void setParentPk(String parentPk) {
        this.parentPk = parentPk;
    }

    public String getMultiBid() {
        return multiBid;
    }

    public void setMultiBid(String multiBid) {
        this.multiBid = multiBid;
    }

    public String getSecurityMobile() {
        return securityMobile;
    }

    public void setSecurityMobile(String securityMobile) {
        this.securityMobile = securityMobile;
    }

    public String getAliyunPK() {
        return aliyunPK;
    }

    public void setAliyunPK(String aliyunPK) {
        this.aliyunPK = aliyunPK;
    }
}
