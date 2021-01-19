package com.platform.task.datasource;

import com.platform.task.util.Property;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;

public class HbaseSource extends RichSourceFunction<Tuple3<String, String, Double>> {
    private static Table table;
    private static Connection conn;
    private static Scan scan;
    private static Admin admin;
    private String familyName = "log";
    private static Logger logger = LoggerFactory.getLogger(HbaseSource.class);

    public HbaseSource() {
    }

    static {
        org.apache.hadoop.conf.Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.rootdir", Property.getStrValue("hbase.rootdir"));
        conf.set("hbase.zookeeper.quorum", Property.getStrValue("hbase.zookeeper.quorum"));
        conf.set("hbase.client.scanner.timeout.period", Property.getStrValue("hbase.client.scanner.timeout.period"));
        conf.set("hbase.rpc.timeout", Property.getStrValue("hbase.rpc.timeout"));
        conf.set("hbase.rpc.timeout", "1800000");
        conf.set("hbase.client.scanner.timeout.period", "1800000");
        System.out.println(Property.getStrValue("hbase.rootdir"));
        try {
            conn = ConnectionFactory.createConnection(conf);
            admin = conn.getAdmin();
            table = conn.getTable(TableName.valueOf("rating"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        scan = new Scan();
    }
    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
    }

    public static void main(String[] args) throws IOException {
        ResultScanner rs = table.getScanner(scan);
        for(Result r : rs) {
            System.out.println(new String(r.getRow()));
        }
    }


    @Override
    public void run(SourceContext<Tuple3<String, String, Double>> sourceContext) throws Exception {
        ResultScanner rs = table.getScanner(scan);
        Iterator<Result> iterator = rs.iterator();
        while(iterator.hasNext()) {
            Result result = iterator.next();
            byte[] userId = result.getValue(familyName.getBytes(), "userId".getBytes());
            byte[] productId = result.getValue(familyName.getBytes(), "productId".getBytes());
            byte[] score = result.getValue(familyName.getBytes(), "score".getBytes());

            Tuple3<String, String, Double> tuple3 = new Tuple3<>();
            if(userId != null && productId != null && score != null) {
                tuple3.setFields(new String(userId), new String(productId), Double.parseDouble(new String(score)));
                sourceContext.collect(tuple3);
            }
        }
    }

    @Override
    public void cancel() {
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
