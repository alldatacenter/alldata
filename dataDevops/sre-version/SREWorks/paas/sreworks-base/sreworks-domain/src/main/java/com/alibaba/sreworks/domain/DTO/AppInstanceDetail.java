package com.alibaba.sreworks.domain.DTO;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class AppInstanceDetail extends AppDetail {

    private List<Long> clusterResourceIdList;

    public List<Long> clusterResourceIdList() {
        return clusterResourceIdList == null ? new ArrayList<>() : clusterResourceIdList;
    }

}
