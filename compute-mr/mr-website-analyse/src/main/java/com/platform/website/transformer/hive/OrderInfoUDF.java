package com.platform.website.transformer.hive;

import com.platform.website.common.GlobalConstants;
import com.platform.website.util.JdbcManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.ql.exec.UDF;

/**
 * 获取order相关属性
 * 
 * @author wulinhao
 *
 */
public class OrderInfoUDF extends UDF {
    private Connection conn = null;
    private Map<String, InnerOrderInfo> cache = new LinkedHashMap<String, InnerOrderInfo>() {
        private static final long serialVersionUID = 2498482880678491162L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, InnerOrderInfo> eldest) {
            return this.size() > 100;
        }
    };

    public OrderInfoUDF() {
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
     * 获取对应订单的订单金额, 如果数据库中没有对应的订单，那么直接返回0
     * 
     * @param orderId
     * @return
     */
    public int evaluate(String orderId) {
        if (StringUtils.isBlank(orderId)) {
            throw new IllegalArgumentException("参数异常，订单id不能为空");
        }

        orderId = orderId.trim();
        InnerOrderInfo ioi = this.fetchInnerOrderInfo(orderId);
        return ioi == null ? 0 : ioi.amount;
    }

    /**
     * 根据订单id和标识位获取对应的订单值
     * 
     * @param orderId
     * @param flag
     * @return
     */
    public String evaluate(String orderId, String flag) {
        if (StringUtils.isBlank(orderId)) {
            throw new IllegalArgumentException("参数异常，订单id不能为空");
        }

        orderId = orderId.trim();
        InnerOrderInfo ioi = this.fetchInnerOrderInfo(orderId);
        switch (flag) {
        case "pl":
            return ioi == null || StringUtils.isBlank(ioi.platform) ? GlobalConstants.DEFAULT_VALUE : ioi.platform;
        case "cut":
            return ioi == null || StringUtils.isBlank(ioi.currencyType) ? GlobalConstants.DEFAULT_VALUE : ioi.currencyType;
        case "pt":
            return ioi == null || StringUtils.isBlank(ioi.paymentType) ? GlobalConstants.DEFAULT_VALUE : ioi.paymentType;
        default:
            throw new IllegalArgumentException("参数异常flag必须为(pl,cut,pt)中的一个，给定的是:" + flag);
        }
    }

    private InnerOrderInfo fetchInnerOrderInfo(String orderId) {
        InnerOrderInfo ioi = this.cache.get(orderId);
        if (ioi == null) {
            // 进行查询操作
            PreparedStatement pstmt = null;
            ResultSet rs = null;
            try {
                pstmt = this.conn.prepareStatement("select order_id,platform,s_time,currency_type,payment_type,amount from order_info where order_id=?");
                pstmt.setString(1, orderId);
                rs = pstmt.executeQuery();
                if (rs.next()) {
                    ioi = new InnerOrderInfo();
                    ioi.orderId = orderId;
                    ioi.currencyType = rs.getString("currency_type");
                    ioi.paymentType = rs.getString("payment_type");
                    ioi.platform = rs.getString("platform");
                    ioi.sTime = rs.getLong("s_time");
                    ioi.amount = rs.getInt("amount");
                }
            } catch (SQLException e) {
                throw new RuntimeException("查询数据库异常", e);
            } finally {
                JdbcManager.close(null, pstmt, rs);
            }
        }
        return ioi;
    }

    /**
     * 内部类
     * 
     * @author wulinhao
     *
     */
    @SuppressWarnings("unused")
    private static class InnerOrderInfo {
        public String orderId;
        public String currencyType;
        public String paymentType;
        public String platform;
        public long sTime;
        public int amount;
    }
}
