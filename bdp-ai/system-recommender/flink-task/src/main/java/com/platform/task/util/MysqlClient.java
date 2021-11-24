package com.platform.schedule.entity;

import com.platform.schedule.entity.ProductEntity;
import com.platform.schedule.entity.RatingEntity;

import java.sql.*;

public class MysqlClient {
    private static String url = Property.getStrValue("mysql.url");
    private static String username = Property.getStrValue("mysql.username");
    private static String password = Property.getStrValue("mysql.password");
    private static Statement stmt;

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(url, username, password);
            stmt = conn.createStatement();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static boolean putData(Object object) throws Exception {
        String sql = "";
        if (object instanceof ProductEntity) {
            ProductEntity product = (ProductEntity) object;
            sql = String.format("insert into product(productId, name, imageUrl, categories, tags) values (%d, '%s', '%s', '%s', '%s')",
                    product.getProductId(), format(product.getName()), format(product.getImageUrl()),
                    format(product.getCategories()), format(product.getTags()));
            System.out.println(sql);
        } else if (object instanceof RatingEntity) {
            RatingEntity rating = (RatingEntity) object;
            sql = String.format("insert into rating (userId, productId, score, timestamp) values (%d, %d, %f, %d)",
                    rating.getUserId(), rating.getProductId(),
                    rating.getScore(), rating.getTimestamp());
        }
        return !stmt.execute(sql);
    }

    public static String format(String str) {
        str.replaceAll("\"", "\\\"");
        if (str.startsWith("\"")) {
            str = "\\\"" + str.substring(1, str.length() - 1) + "\\\"";
        }
        return str;
    }

}
