package com.platform.website.transformer.model.dim.base;

import com.platform.website.common.GlobalConstants;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;

/**
 * dimension_location表对应的model类
 * 
 * @author wulinhao
 *
 */
public class LocationDimension extends BaseDimension {
    private int id;
    private String country;
    private String province;
    private String city;

    public LocationDimension() {
        super();
        this.clean();
    }

    public LocationDimension(int id, String country, String province, String city) {
        super();
        this.id = id;
        this.country = country;
        this.province = province;
        this.city = city;
    }

    public void clean() {
        this.id = 0;
        this.country = "";
        this.province = "";
        this.city = "";
    }

    /**
     * 创建location dimension类
     * 
     * @param country
     * @param province
     * @param city
     * @return
     */
    public static LocationDimension newInstance(String country, String province, String city) {
        LocationDimension location = new LocationDimension();
        location.country = country;
        location.province = province;
        location.city = city;
        return location;
    }

    /**
     * 构造对维度的地域维度对象
     * 
     * @param country
     * @param province
     * @param city
     * @return
     */
    public static List<LocationDimension> buildList(String country, String province, String city) {
        List<LocationDimension> list = new ArrayList<LocationDimension>();
        if (StringUtils.isBlank(country) || GlobalConstants.DEFAULT_VALUE.equals(country)) {
            // 国家名称为空，那么将所有的设置为default value；或者国家名称为default
            // value，那么也需要将所有设置为default value
            country = province = city = GlobalConstants.DEFAULT_VALUE;
        }
        if (StringUtils.isBlank(province) || GlobalConstants.DEFAULT_VALUE.equals(province)) {
            // 省份名称为空，那么将city 和province设置为default value
            province = city = GlobalConstants.DEFAULT_VALUE;
        }
        if (StringUtils.isBlank(city)) {
            // 城市名称为空，设置为default value
            city = GlobalConstants.DEFAULT_VALUE;
        }
        list.add(LocationDimension.newInstance(country, GlobalConstants.VALUE_OF_ALL, GlobalConstants.VALUE_OF_ALL));
        list.add(LocationDimension.newInstance(country, province, GlobalConstants.VALUE_OF_ALL));
        list.add(LocationDimension.newInstance(country, province, city));
        return list;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeInt(this.id);
        out.writeUTF(this.country);
        out.writeUTF(this.province);
        out.writeUTF(this.city);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        this.id = in.readInt();
        this.country = in.readUTF();
        this.province = in.readUTF();
        this.city = in.readUTF();
    }

    @Override
    public int compareTo(BaseDimension o) {
        LocationDimension other = (LocationDimension) o;
        int tmp = Integer.compare(this.id, other.id);
        if (tmp != 0) {
            return tmp;
        }

        tmp = this.country.compareTo(other.country);
        if (tmp != 0) {
            return tmp;
        }

        tmp = this.province.compareTo(other.province);
        if (tmp != 0) {
            return tmp;
        }

        tmp = this.city.compareTo(other.city);
        return tmp;
    }

}
