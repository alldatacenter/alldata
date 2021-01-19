package com.platform.task.sink;

import com.platform.task.util.Property;
import org.apache.flink.api.common.io.OutputFormat;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.configuration.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


public class HbaseSink implements OutputFormat<Object> {
    private static Logger logger = LoggerFactory.getLogger(HbaseSink.class);

    private Connection conn = null;
    private HTable table = null;
    private String tableName = null;
    private Integer rowKey = 1;
    private String familyName = "p";

    public HbaseSink(String name) {
        super();
        this.tableName = name;
    }

    public HbaseSink() {
    }

    @Override
    public void configure(Configuration configuration) {
    }

    @Override
    public void open(int i, int i1) throws IOException {
        org.apache.hadoop.conf.Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.rootdir", Property.getStrValue("hbase.rootdir"));
        conf.set("hbase.zookeeper.quorum", Property.getStrValue("hbase.zookeeper.quorum"));
        conf.set("hbase.client.scanner.timeout.period", Property.getStrValue("hbase.client.scanner.timeout.period"));
        System.out.println("超时：" + Property.getStrValue("hbase.rpc.timeout"));
        conf.set("hbase.rpc.timeout", Property.getStrValue("hbase.rpc.timeout"));
        System.out.println(Property.getStrValue("hbase.rootdir"));
        this.conn = ConnectionFactory.createConnection(conf);
        this.table =  new HTable(conf, this.tableName);
    }

    @Override
    public void writeRecord(Object o) throws IOException {
        if(tableName.equals("historyHotProducts")) {
            Tuple2<String, Long> s = (Tuple2<String, Long>) o;
            String rowKey = String.valueOf(s.f0);
            Put put = new Put(rowKey.getBytes());
            String count = String.valueOf(s.f1);
            put.addColumn(familyName.getBytes(), "count".getBytes(), count.getBytes());
            table.put(put);
        } else if(tableName.equals("recentHotProducts")) {
            Tuple3<String, Long, String> s = (Tuple3<String, Long, String>) o;
            String key = s.f2;
            Put put = new Put(String.valueOf(key).getBytes());
            String value1 = s.f0;
            String value2 = String.valueOf(s.f1);
            put.addColumn(familyName.getBytes(), "productId".getBytes(), value1.getBytes());
            put.addColumn(familyName.getBytes(), "count".getBytes(), value2.getBytes());
            table.put(put);
        } else if(tableName.equals("goodProducts")) {
            Tuple2<String, Double> s = (Tuple2<String, Double>) o;
            String rowKey = String.valueOf(s.f0);
            Put put = new Put(rowKey.getBytes());
            String count = String.valueOf(s.f1);
            put.addColumn(familyName.getBytes(), "count".getBytes(), count.getBytes());
            table.put(put);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            if(table != null) {
                table.close();
            }
            if(conn != null) {
                conn.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("Close hbase exception: ", e.toString());
        }
    }
}
