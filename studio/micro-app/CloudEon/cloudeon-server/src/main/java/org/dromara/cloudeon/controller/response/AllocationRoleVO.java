package org.dromara.cloudeon.controller.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AllocationRoleVO {
    private String stackRoleName;
    private Integer minNum;
    private Integer fixedNum;
    private boolean needOdd;
    private List<Integer> nodeIds;
}
