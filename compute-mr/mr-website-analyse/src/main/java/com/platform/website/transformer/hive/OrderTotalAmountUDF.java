package com.platform.website.transformer.hive;

import com.platform.website.common.DateEnum;
import com.platform.website.common.GlobalConstants;
import com.platform.website.transformer.model.dim.base.DateDimension;
import com.platform.website.util.JdbcManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.ql.exec.UDF;

/**
 * 计算总金额的udf类
 * 
 * @author wulinhao
 *
 */
public class OrderTotalAmountUDF extends UDF {
    private Connection conn = null;
    private Map<String, Integer> cache = new LinkedHashMap<String, Integer>() {
        private static final long serialVersionUID = 2498482880678491162L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest) {
            return this.size() > 100;
        }
    };

    public OrderTotalAmountUDF() {
        Configuration conf = new Configuration();
        conf.addResource("transformer-env.xml");
        try {
            this.conn = JdbcManager.getConnection(conf, GlobalConstants.WAREHOUSE_OF_WEBSITE);
        } catch (SQLException e) {
            throw new RuntimeException("创建mysql连接异常", e);
        }

        // 添加一个钩子进行关闭操作
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    JdbcManager.close(conn, null, null);
                } catch (Throwable e) {
                    // nothing
                }
            }
        }));
    }

    /**
     * 根据给定的flag参数决定获取当前维度下的total
     * amount值，如果flag给定为revenue，那么获取总的成功支付订单金额，如果给定位refund，获取退款订单总金额
     * 
     * @param platformDimensionId
     * @param dateDimensionId
     * @param currencyTypeDimensionId
     * @param paymentTypeDimensionId
     * @param amount
     * @param flag
     * @return
     */
    public int evaluate(int platformDimensionId, int dateDimensionId, int currencyTypeDimensionId, int paymentTypeDimensionId, int amount, String flag) {
        if ("revenue".equals(flag)) {
            return this.evaluateTotalRevenueAmount(platformDimensionId, dateDimensionId, currencyTypeDimensionId, paymentTypeDimensionId, amount);
        } else if ("refund".equals(flag)) {
            return this.evaluateTotalRefundAmount(platformDimensionId, dateDimensionId, currencyTypeDimensionId, paymentTypeDimensionId, amount);
        } else {
            throw new IllegalArgumentException("参数异常，flag必须为(revenue,refund)，先给定为:" + flag);
        }
    }

    // 计算总的成功订单金额
    private int evaluateTotalRevenueAmount(int platformDimensionId, int dateDimensionId, int currencyTypeDimensionId, int paymentTypeDimensionId, int amount) {
        String key = platformDimensionId + "_" + dateDimensionId + "_" + currencyTypeDimensionId + "_" + paymentTypeDimensionId + "_revenue";
        Integer lastTotal = this.cache.get(key);

        if (lastTotal == null) {
            PreparedStatement pstmt = null;
            ResultSet rs = null;

            try {
                // 1. 获取前一个维度的日期id
                int lastDateDimensionId = this.fetchLastDateDimensionId(dateDimensionId);

                // 2. 前一个日期维度不是-1，那么获取对应的退款订单金额
                int lastTotalRevenueAmount = 0;
                if (lastDateDimensionId != -1) {
                    pstmt = this.conn.prepareStatement("SELECT total_revenue_amount FROM stats_order WHERE platform_dimension_id=? AND date_dimension_id=? AND currency_type_dimension_id=? AND payment_type_dimension_id=?");
                    int i = 0;
                    pstmt.setInt(++i, platformDimensionId);
                    pstmt.setInt(++i, lastDateDimensionId);
                    pstmt.setInt(++i, currencyTypeDimensionId);
                    pstmt.setInt(++i, paymentTypeDimensionId);
                    rs = pstmt.executeQuery();
                    if (rs.next()) {
                        lastTotalRevenueAmount = rs.getInt("total_revenue_amount");
                    }
                }
                lastTotal = lastTotalRevenueAmount;
                this.cache.put(key, lastTotal);
            } catch (Exception e) {
                throw new RuntimeException("获取总支付成功订单金额出现异常", e);
            }
        }
        return lastTotal == null ? amount : lastTotal + amount;
    }

    // 计算总的退款订单金额
    private int evaluateTotalRefundAmount(int platformDimensionId, int dateDimensionId, int currencyTypeDimensionId, int paymentTypeDimensionId, int amount) {
        String key = platformDimensionId + "_" + dateDimensionId + "_" + currencyTypeDimensionId + "_" + paymentTypeDimensionId + "_refund";
        Integer lastTotal = this.cache.get(key);

        if (lastTotal == null) {
            PreparedStatement pstmt = null;
            ResultSet rs = null;

            try {
                // 1. 获取前一个维度的日期id
                int lastDateDimensionId = this.fetchLastDateDimensionId(dateDimensionId);

                // 2. 前一个日期维度不是-1，那么获取对应的退款订单金额
                int lastTotalRefundAmount = 0;
                if (lastDateDimensionId != -1) {
                    pstmt = this.conn.prepareStatement("SELECT total_refund_amount FROM stats_order WHERE platform_dimension_id=? AND date_dimension_id=? AND currency_type_dimension_id=? AND payment_type_dimension_id=?");
                    int i = 0;
                    pstmt.setInt(++i, platformDimensionId);
                    pstmt.setInt(++i, lastDateDimensionId);
                    pstmt.setInt(++i, currencyTypeDimensionId);
                    pstmt.setInt(++i, paymentTypeDimensionId);
                    rs = pstmt.executeQuery();
                    if (rs.next()) {
                        lastTotalRefundAmount = rs.getInt("total_refund_amount");
                    }
                }
                lastTotal = lastTotalRefundAmount;
                this.cache.put(key, lastTotal);
            } catch (Exception e) {
                throw new RuntimeException("获取总的退款金额出现异常", e);
            }
        }
        // 当前总=前总+amount
        return lastTotal == null ? amount : lastTotal + amount;
    }

    // 获取前一个维度的日期id
    private int fetchLastDateDimensionId(int dateDimensionId) throws SQLException {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Date calendar = null;
        DateEnum de = null;

        // 获取指定id的日志和维度类型
        try {
            pstmt = this.conn.prepareStatement("select calendar,type from dimension_date where id=?");
            pstmt.setInt(1, dateDimensionId);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                calendar = rs.getDate("calendar");
                String type = rs.getString("type");
                de = DateEnum.valueOfName(type);
            }
        } finally {
            JdbcManager.close(null, pstmt, rs);
        }

        if (calendar == null || de == null) {
            throw new RuntimeException("无法从数据库中获取对应id的日期维度,id:" + dateDimensionId);
        }

        // 获取前一个维度的id值
        long diff = 0; // 间隔毫秒
        switch (de) {
        case DAY:
            diff = GlobalConstants.DAY_OF_MILLISECONDS;
            break;
        default:
            throw new IllegalArgumentException("时间维度必须为day，当前为:" + de.name);
        }

        try {
            DateDimension lastDateDimension = DateDimension.buildDate(calendar.getTime() - diff, de);
            pstmt = conn.prepareStatement("SELECT `id` FROM `dimension_date` WHERE `year` = ? AND `season` = ? AND `month` = ? AND `week` = ? AND `day` = ? AND `type` = ? AND `calendar` = ?");
            int i = 0;
            pstmt.setInt(++i, lastDateDimension.getYear());
            pstmt.setInt(++i, lastDateDimension.getSeason());
            pstmt.setInt(++i, lastDateDimension.getMonth());
            pstmt.setInt(++i, lastDateDimension.getWeek());
            pstmt.setInt(++i, lastDateDimension.getDay());
            pstmt.setString(++i, lastDateDimension.getType());
            pstmt.setDate(++i, new java.sql.Date(lastDateDimension.getCalendar().getTime()));
            rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
            return -1;
        } finally {
            JdbcManager.close(null, pstmt, rs);
        }
    }

}
