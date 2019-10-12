package com.platform.manage.dto;

import com.platform.manage.model.OmsCompanyAddress;
import com.platform.manage.model.OmsOrderReturnApply;
import lombok.Getter;
import lombok.Setter;

/**
 * 申请信息封装
 * Created by wulinhao on 2019/9/18.
 */
public class OmsOrderReturnApplyResult extends OmsOrderReturnApply {
    @Getter
    @Setter
    private OmsCompanyAddress companyAddress;
}
