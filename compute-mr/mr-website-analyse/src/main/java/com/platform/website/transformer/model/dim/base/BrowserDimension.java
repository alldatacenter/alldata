package com.platform.website.transformer.model.dim.base;

import com.platform.website.common.GlobalConstants;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;

/**
 * 浏览器维度
 * 
 * @author wulinhao
 *
 */
public class BrowserDimension extends BaseDimension {
    private int id; // id
    private String browserName; // 名称
    private String browserVersion; // 版本

    public BrowserDimension() {
        super();
    }

    public BrowserDimension(String browserName, String browserVersion) {
        super();
        this.browserName = browserName;
        this.browserVersion = browserVersion;
    }

    public void clean() {
        this.id = 0;
        this.browserName = "";
        this.browserVersion = "";
    }

    public static BrowserDimension newInstance(String browserName, String browserVersion) {
        BrowserDimension browserDimension = new BrowserDimension();
        browserDimension.browserName = browserName;
        browserDimension.browserVersion = browserVersion;
        return browserDimension;
    }

    /**
     * 构建多个浏览器维度信息对象集合
     * 
     * @param browserName
     * @param browserVersion
     * @return
     */
    public static List<BrowserDimension> buildList(String browserName, String browserVersion) {
        List<BrowserDimension> list = new ArrayList<BrowserDimension>();
        if (StringUtils.isBlank(browserName)) {
            // 浏览器名称为空，那么设置为unknown
            browserName = GlobalConstants.DEFAULT_VALUE;
            browserVersion = GlobalConstants.DEFAULT_VALUE;
        }
        if (StringUtils.isEmpty(browserVersion)) {
            browserVersion = GlobalConstants.DEFAULT_VALUE;
        }
        // list.add(BrowserDimension.newInstance(GlobalConstants.VALUE_OF_ALL,
        // GlobalConstants.VALUE_OF_ALL));
        list.add(BrowserDimension.newInstance(browserName, GlobalConstants.VALUE_OF_ALL));
        list.add(BrowserDimension.newInstance(browserName, browserVersion));
        return list;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getBrowserName() {
        return browserName;
    }

    public void setBrowserName(String browserName) {
        this.browserName = browserName;
    }

    public String getBrowserVersion() {
        return browserVersion;
    }

    public void setBrowserVersion(String browserVersion) {
        this.browserVersion = browserVersion;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeInt(this.id);
        out.writeUTF(this.browserName);
        out.writeUTF(this.browserVersion);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        this.id = in.readInt();
        this.browserName = in.readUTF();
        this.browserVersion = in.readUTF();
    }

    @Override
    public int compareTo(BaseDimension o) {
        if (this == o) {
            return 0;
        }

        BrowserDimension other = (BrowserDimension) o;
        int tmp = Integer.compare(this.id, other.id);
        if (tmp != 0) {
            return tmp;
        }
        tmp = this.browserName.compareTo(other.browserName);
        if (tmp != 0) {
            return tmp;
        }
        tmp = this.browserVersion.compareTo(other.browserVersion);
        return tmp;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((browserName == null) ? 0 : browserName.hashCode());
        result = prime * result + ((browserVersion == null) ? 0 : browserVersion.hashCode());
        result = prime * result + id;
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
        BrowserDimension other = (BrowserDimension) obj;
        if (browserName == null) {
            if (other.browserName != null)
                return false;
        } else if (!browserName.equals(other.browserName))
            return false;
        if (browserVersion == null) {
            if (other.browserVersion != null)
                return false;
        } else if (!browserVersion.equals(other.browserVersion))
            return false;
        if (id != other.id)
            return false;
        return true;
    }
}
