package com.platform.mall.dto.admin;

import com.platform.mall.entity.admin.OmsCompanyAddress;
import com.platform.mall.entity.admin.OmsOrderReturnApply;

/**
 * 申请信息封装
 * @author AllDataDC
 */
public class OmsOrderReturnApplyResult extends OmsOrderReturnApply {
    private OmsCompanyAddress companyAddress;

    public OmsCompanyAddress getCompanyAddress() {
        return companyAddress;
    }

    public void setCompanyAddress(OmsCompanyAddress companyAddress) {
        this.companyAddress = companyAddress;
    }
}
