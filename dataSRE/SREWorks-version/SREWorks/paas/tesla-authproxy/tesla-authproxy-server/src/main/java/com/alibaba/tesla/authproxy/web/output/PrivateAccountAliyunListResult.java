package com.alibaba.tesla.authproxy.web.output;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrivateAccountAliyunListResult {

    private List<Item> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String aliyunId;
        private String aliyunPk;
    }
}
