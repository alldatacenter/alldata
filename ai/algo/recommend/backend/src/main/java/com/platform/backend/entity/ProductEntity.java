package com.platform.backend.entity;

import org.codehaus.jackson.annotate.JsonIgnore;
import javax.persistence.*;

@Entity
@Table(name="product")
public class ProductEntity {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    @Column
    @JsonIgnore
    private int id;
    @Column(name = "productid")
    private int productId;
    @Column(name = "name")
    private String name;
    @Column(name = "imageurl")
    private String imageUrl;
    @Column(name = "categories")
    private String categories;
    @Column(name = "tags")
    private String tags;

    private double score;

    public ProductEntity() {
    }

    public ProductEntity(int productId, String name, String imageUrl, String categories, String tags) {
        this.productId = productId;
        this.name = name;
        this.imageUrl = imageUrl;
        this.categories = categories;
        this.tags = tags;
    }


    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getCategories() {
        return categories;
    }

    public void setCategories(String categories) {
        this.categories = categories;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    @Override
    public String toString() {
        return "ProductEntity{" +
                "id=" + id +
                ", productId=" + productId +
                ", name='" + name + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", categories='" + categories + '\'' +
                ", tags='" + tags + '\'' +
                '}';
    }
}
