package com.platform.website.transformer.mr;

import com.platform.website.common.GlobalConstants;
import com.platform.website.common.KpiType;
import com.platform.website.transformer.model.dim.base.BaseDimension;
import com.platform.website.transformer.model.value.BaseStatsValueWritable;
import com.platform.website.transformer.service.rpc.IDimensionConverter;
import com.platform.website.transformer.service.rpc.client.DimensionConverterClient;
import com.platform.website.util.JdbcManager;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.OutputCommitter;
import org.apache.hadoop.mapreduce.OutputFormat;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.output.FileOutputCommitter;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.log4j.Logger;

/**
 * 自定义输出到mysql的outputformat类
 */
public class TransformerOutputFormat extends OutputFormat<BaseDimension, BaseStatsValueWritable> {
    private static final Logger logger = Logger.getLogger(TransformerOutputFormat.class);

    @Override
    public RecordWriter<BaseDimension, BaseStatsValueWritable> getRecordWriter(TaskAttemptContext context) throws IOException, InterruptedException {
        Configuration conf = context.getConfiguration();
        Connection conn = null;
        IDimensionConverter converter = DimensionConverterClient.createDimensionConverter(conf);
        try {
            conn = JdbcManager.getConnection(conf, GlobalConstants.WAREHOUSE_OF_WEBSITE);
            conn.setAutoCommit(false);
        } catch (SQLException e) {
            logger.error("获取数据库连接失败", e);
            throw new IOException("获取数据库连接失败", e);
        }
        return new TransformerRecordWriter(conn, conf, converter);
    }

    @Override
    public void checkOutputSpecs(JobContext context) throws IOException, InterruptedException {
        // 检测输出空间，输出到mysql不用检测
    }

    @Override
    public OutputCommitter getOutputCommitter(TaskAttemptContext context) throws IOException, InterruptedException {
        return new FileOutputCommitter(FileOutputFormat.getOutputPath(context), context);
    }

    /**
     * 自定义具体数据输出writer
     * 
     * @author wulinhao
     *
     */
    public class TransformerRecordWriter extends RecordWriter<BaseDimension, BaseStatsValueWritable> {
        private Connection conn = null;
        private Configuration conf = null;
        private IDimensionConverter converter = null;
        private Map<KpiType, PreparedStatement> map = new HashMap<KpiType, PreparedStatement>();
        private Map<KpiType, Integer> batch = new HashMap<KpiType, Integer>();

        public TransformerRecordWriter(Connection conn, Configuration conf, IDimensionConverter converter) {
            super();
            this.conn = conn;
            this.conf = conf;
            this.converter = converter;
        }

        @Override
        public void write(BaseDimension key, BaseStatsValueWritable value) throws IOException, InterruptedException {
            if (key == null || value == null) {
                return;
            }

            try {
                KpiType kpi = value.getKpi();
                PreparedStatement pstmt = null;
                int count = 1;
                if (map.get(kpi) == null) {
                    // 使用kpi进行区分，返回sql保存到config中
                    pstmt = this.conn.prepareStatement(conf.get(kpi.name));
                    map.put(kpi, pstmt);
                } else {
                    pstmt = map.get(kpi);
                    count = batch.get(kpi);
                    count++;
                }
                batch.put(kpi, count); // 批量次数的存储

                String collectorName = conf.get(GlobalConstants.OUTPUT_COLLECTOR_KEY_PREFIX + kpi.name);
                Class<?> clazz = Class.forName(collectorName);
                IOutputCollector collector = (IOutputCollector) clazz.newInstance();
                collector.collect(conf, key, value, pstmt, converter);

                if (count % Integer.valueOf(conf.get(GlobalConstants.JDBC_BATCH_NUMBER, GlobalConstants.DEFAULT_JDBC_BATCH_NUMBER)) == 0) {
                    pstmt.executeBatch();
                    conn.commit();
                    batch.remove(kpi); // 对应批量计算删除
                }
            } catch (Throwable e) {
                logger.error("在writer中写数据出现异常", e);
                throw new IOException(e);
            }
        }

        @Override
        public void close(TaskAttemptContext context) throws IOException, InterruptedException {
            try {
                for (Map.Entry<KpiType, PreparedStatement> entry : this.map.entrySet()) {
                    entry.getValue().executeBatch();
                }
            } catch (SQLException e) {
                logger.error("执行executeUpdate方法异常", e);
                throw new IOException(e);
            } finally {
                try {
                    if (conn != null) {
                        conn.commit(); // 进行connection的提交动作
                    }
                } catch (Exception e) {
                    // nothing
                } finally {
                    for (Map.Entry<KpiType, PreparedStatement> entry : this.map.entrySet()) {
                        try {
                            entry.getValue().close();
                        } catch (SQLException e) {
                            // nothing
                        }
                    }
                    if (conn != null) {
                        try {
                            conn.close();
                        } catch (Exception e) {
                            // nothing
                        }
                    }
                }
            }

            // 关闭rpc连接
            DimensionConverterClient.stopDimensionConverterProxy(this.converter);
        }

    }
}
