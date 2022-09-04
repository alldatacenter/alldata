package com.alibaba.sreworks.common.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author jinghua.yjh
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NameAlias {

    private String name;

    private String alias;

}
