package com.platform.website.transformer.model.dim.base;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * 表dimension_inbound对应的model类<br/>
 * 由于我们在代码中不需要进行插入操作(往数据库插入新的数据)，所有最终的mapper端和reducer端通信的时候，只需要传递id即可。
 * 
 * 
 * @author wulinhao
 *
 */

/**
 * @author wulinhao
 *
 */
public class InboundDimension extends BaseDimension {
    private int id;
    private int parentId;
    private String name;
    private String hostRegex;
    private int type;

    public InboundDimension() {
        super();
    }

    public InboundDimension(int id, String name, String hostRegex) {
        super();
        this.id = id;
        this.name = name;
        this.hostRegex = hostRegex;
    }

    public InboundDimension(int id, int parentId, String name, String hostRegex, int type) {
        super();
        this.id = id;
        this.parentId = parentId;
        this.name = name;
        this.hostRegex = hostRegex;
        this.type = type;
    }

    public InboundDimension(int id, String name, String hostRegex, int type) {
        super();
        this.id = id;
        this.name = name;
        this.hostRegex = hostRegex;
        this.type = type;
    }

    public InboundDimension(InboundDimension inbound) {
        super();
        this.id = inbound.id;
        this.name = inbound.name;
        this.hostRegex = inbound.name;
        this.type = inbound.type;
        this.parentId = inbound.parentId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getParentId() {
        return parentId;
    }

    public void setParentId(int parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHostRegex() {
        return hostRegex;
    }

    public void setHostRegex(String hostRegex) {
        this.hostRegex = hostRegex;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeInt(this.id);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        this.id = in.readInt();
    }

    @Override
    public int compareTo(BaseDimension o) {
        return Integer.compare(this.id, ((InboundDimension) o).id);
    }

}
