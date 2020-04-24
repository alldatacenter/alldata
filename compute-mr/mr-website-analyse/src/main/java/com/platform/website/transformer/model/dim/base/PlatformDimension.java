package com.platform.website.transformer.model.dim.base;

import com.platform.website.common.GlobalConstants;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;

/**
 * 平台维度类
 * 
 * @author wulinhao
 *
 */
public class PlatformDimension extends BaseDimension {
    private int id;
    private String platformName;

    public PlatformDimension() {
        super();
    }

    public PlatformDimension(String platformName) {
        super();
        this.platformName = platformName;
    }

    public PlatformDimension(int id, String platformName) {
        super();
        this.id = id;
        this.platformName = platformName;
    }

    public static List<PlatformDimension> buildList(String platformName) {
        if (StringUtils.isBlank(platformName)) {
            platformName = GlobalConstants.DEFAULT_VALUE;
        }
        List<PlatformDimension> list = new ArrayList<PlatformDimension>();
        list.add(new PlatformDimension(GlobalConstants.VALUE_OF_ALL));
        list.add(new PlatformDimension(platformName));
        return list;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeInt(this.id);
        out.writeUTF(this.platformName);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        this.id = in.readInt();
        this.platformName = in.readUTF();
    }

    @Override
    public int compareTo(BaseDimension o) {
        if (this == o) {
            return 0;
        }

        PlatformDimension other = (PlatformDimension) o;
        int tmp = Integer.compare(this.id, other.id);
        if (tmp != 0) {
            return tmp;
        }
        tmp = this.platformName.compareTo(other.platformName);
        return tmp;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        result = prime * result + ((platformName == null) ? 0 : platformName.hashCode());
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
        PlatformDimension other = (PlatformDimension) obj;
        if (id != other.id)
            return false;
        if (platformName == null) {
            if (other.platformName != null)
                return false;
        } else if (!platformName.equals(other.platformName))
            return false;
        return true;
    }

}
