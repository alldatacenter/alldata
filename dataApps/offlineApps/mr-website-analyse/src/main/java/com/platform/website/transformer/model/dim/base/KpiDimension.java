package com.platform.website.transformer.model.dim.base;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class KpiDimension extends BaseDimension {
    private int id;
    private String kpiName;

    public KpiDimension() {
        super();
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

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeInt(this.id);
        out.writeUTF(this.kpiName);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        this.id = in.readInt();
        this.kpiName = in.readUTF();
    }

    @Override
    public int compareTo(BaseDimension o) {
        if (this == o) {
            return 0;
        }

        KpiDimension other = (KpiDimension) o;
        int tmp = Integer.compare(this.id, other.id);
        if (tmp != 0) {
            return tmp;
        }
        tmp = this.kpiName.compareTo(other.kpiName);
        return tmp;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        result = prime * result + ((kpiName == null) ? 0 : kpiName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        KpiDimension other = (KpiDimension) obj;
        if (id != other.id)
            return false;
        if (kpiName == null) {
            if (other.kpiName != null)
                return false;
        } else if (!kpiName.equals(other.kpiName))
            return false;
        return true;
    }

}
