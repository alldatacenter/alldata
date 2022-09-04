package com.alibaba.sreworks.appdev.server.params;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.common.util.JsonUtil;
import com.alibaba.sreworks.domain.DTO.CpuMemResource;
import com.alibaba.sreworks.domain.DTO.Resource;

import lombok.Data;

/**
 * @author jinghua.yjh
 */
@Data
public class AppParam {

    private String cpu;

    private String memory;

    private String cpu() {
        switch (getCpu()) {
            case "0":
                return "1";
            case "1":
                return "2";
            case "2":
                return "4";
            case "3":
                return "8";
            case "4":
                return "16";
            case "5":
                return "32";
            case "6":
                return "64";
            case "7":
                return "96";
            case "8":
                return "128";
            default:
                return "0";
        }
    }

    private String memory() {
        switch (getMemory()) {
            case "0":
                return "1G";
            case "1":
                return "2G";
            case "2":
                return "4G";
            case "3":
                return "8G";
            case "4":
                return "16G";
            case "5":
                return "32G";
            case "6":
                return "64G";
            case "7":
                return "96G";
            case "8":
                return "128G";
            default:
                return "0";
        }
    }

    String toDetail() {
        return JSONObject.toJSONString(JsonUtil.map(
            "resource", Resource.builder()
                .limits(CpuMemResource.builder()
                    .cpu(cpu())
                    .memory(memory())
                    .build())
                .requests(CpuMemResource.builder()
                    .cpu(cpu())
                    .memory(memory())
                    .build())
                .build(),
            "cpu", cpu,
            "memory", memory
        ));
    }
}
