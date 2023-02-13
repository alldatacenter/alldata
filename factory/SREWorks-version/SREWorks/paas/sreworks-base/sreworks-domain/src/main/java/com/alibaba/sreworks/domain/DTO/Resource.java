package com.alibaba.sreworks.domain.DTO;

import java.util.HashMap;

import com.alibaba.fastjson.JSONObject;

import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.models.V1ResourceQuotaSpec;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Resource {

    private CpuMemResource limits;

    private CpuMemResource requests;

    public JSONObject toJsonObject() {
        return JSONObject.parseObject(JSONObject.toJSONString(this));
    }

    public V1ResourceQuotaSpec toV1ResourceQuotaSpec() {
        V1ResourceQuotaSpec spec = new V1ResourceQuotaSpec().hard(new HashMap<>());
        if (limits != null) {
            if (limits.getCpu() != null) {
                spec.putHardItem("limits.cpu", Quantity.fromString(limits.getCpu()));
            }
            if (limits.getMemory() != null) {
                spec.putHardItem("limits.memory", Quantity.fromString(limits.getMemory()));
            }
        }
        if (requests != null) {
            if (requests.getCpu() != null) {
                spec.putHardItem("requests.cpu", Quantity.fromString(requests.getCpu()));
            }
            if (requests.getMemory() != null) {
                spec.putHardItem("requests.memory", Quantity.fromString(requests.getMemory()));
            }
        }
        return spec;
    }

}
