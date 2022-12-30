package com.platform.website.module;

public class LocationDimension {
    private int id;
    private String country;
    private String province;
    private String city;

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

    public LocationDimension() {
        super();
    }

    public LocationDimension(int id) {
        super();
        this.id = id;
    }

    public LocationDimension(String country, String province, String city) {
        super();
        this.country = country;
        this.province = province;
        this.city = city;
    }

    public LocationDimension(int id, String country, String province, String city) {
        super();
        this.id = id;
        this.country = country;
        this.province = province;
        this.city = city;
    }

}
