package com.alibaba.sreworks.domain.DTO;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.JSONObject;

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
public class AppComponentDetail {

    private Long replicas;

    private Resource resource;

    private List<Env> envs;

    private List<Config> configs;

    private List<Port> ports;

    private List<Volume> volumes;

    private Boolean metricOn;

    private String values;

    public List<Env> envs() {
        return envs == null ? new ArrayList<>() : envs;
    }

    public List<Port> ports() {
        return ports == null ? new ArrayList<>() : ports;
    }

    public List<Config> configs() {
        return configs == null ? new ArrayList<>() : configs;
    }

    public List<Volume> volumes() {
        return volumes == null ? new ArrayList<>() : volumes;
    }

    public boolean metricOn() {
        return metricOn != null && metricOn;
    }

    public String values() {
        return values == null ? "" : values;
    }
}
