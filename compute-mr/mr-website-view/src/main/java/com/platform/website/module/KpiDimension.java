package com.platform.website.module;

import lombok.Data;

@Data
public class KpiDimension {
    private int id;
    private String kpiName;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getKpiName() {
        return kpiName;
    }

    public void setKpiName(String kpiName) {
        this.kpiName = kpiName;
    }

    public KpiDimension() {
        super();
    }

    public KpiDimension(int id) {
        super();
        this.id = id;
    }

    public KpiDimension(String kpiName) {
        super();
        this.kpiName = kpiName;
    }

    public KpiDimension(int id, String kpiName) {
        super();
        this.id = id;
        this.kpiName = kpiName;
    }
}