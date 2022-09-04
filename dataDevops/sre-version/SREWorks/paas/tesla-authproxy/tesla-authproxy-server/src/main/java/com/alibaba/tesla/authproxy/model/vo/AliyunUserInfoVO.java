package com.alibaba.tesla.authproxy.model.vo;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 存储 Aliyun User Info 的对象
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AliyunUserInfoVO {

    private String sub;
    private String name;
    private String upn;
    @SerializedName("login_name")
    private String loginName;
    private String aid;
    private String uid;
}
