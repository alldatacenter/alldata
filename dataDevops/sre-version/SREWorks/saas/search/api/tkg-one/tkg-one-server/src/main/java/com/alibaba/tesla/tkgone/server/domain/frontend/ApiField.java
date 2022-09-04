package com.alibaba.tesla.tkgone.server.domain.frontend;

import lombok.Data;
import lombok.NonNull;

/**
* 
*@author: fangzong.lyj@alibaba-inc.com
*@date: 2022/05/30 20:02
*/
@Data
public class ApiField {
    @NonNull
    private String field;

    @NonNull
    private String type;

    @NonNull
    private String alias;
}
