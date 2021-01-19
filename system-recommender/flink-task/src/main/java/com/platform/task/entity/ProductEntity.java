package com.platform.task.entity;

public class ProductEntity {

    private Integer productId;
    private String name;
    private String price;
    private String brand;
    private String imageUrl;
    private String categories;
    private String tags;

    public ProductEntity() {

    }

    public ProductEntity(Integer productId, String name, String price, String brand, String imageUrl, String categories, String tags) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.brand = brand;
        this.imageUrl = imageUrl;
        this.categories = categories;
        this.tags = tags;
    }

    public Integer getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getCategories() {
        return categories;
    }

    public String getTags() {
        return tags;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setCategories(String categories) {
        this.categories = categories;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    @Override
    public String toString() {
        return "ProductEntity{" +
                "productId=" + productId +
                ", name='" + name + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", categories='" + categories + '\'' +
                ", tags='" + tags + '\'' +
                '}';
    }
}
