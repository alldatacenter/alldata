package com.platform.schedule.datasource;

import com.platform.schedule.entity.Property;
import org.apache.flink.addons.hbase.TableInputFormat;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.configuration.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class HbaseTableSource extends TableInputFormat<Tuple4<String, String, Double, String>> {
    private static Connection conn;
    private String familyName = "p";
    private static Logger logger = LoggerFactory.getLogger(HbaseTableSource.class);

    public HbaseTableSource() {
        super();
    }

    @Override
    protected Scan getScanner() {
        return this.scan;
    }

    @Override
    protected String getTableName() {
        return "rating";
    }

    @Override
    protected Tuple4<String, String, Double, String> mapResultToTuple(Result result) {
        byte[] userId = null;
        byte[] productId = null;
        byte[] score = null;
        byte[] timestamp = null;
        Tuple4<String, String, Double, String> tuple4 = null;
       try {
           userId = result.getValue(familyName.getBytes(), "userId".getBytes());
           productId = result.getValue(familyName.getBytes(), "productId".getBytes());
           score = result.getValue(familyName.getBytes(), "score".getBytes());
           timestamp = result.getValue(familyName.getBytes(), "timestamp".getBytes());
       } catch (Exception e) {
           e.printStackTrace();
       }
       Double s = 0.0;
       try {
           if(score != null) {
               s = Double.parseDouble(new String(score));
           }
       } catch (Exception e) {
           System.out.println(userId);
           System.out.println(productId);
           System.out.println(score);
           System.out.println(timestamp);
           e.printStackTrace();
       }
        if(userId != null && productId != null && score != null && timestamp != null) {
            try {
                tuple4 = new Tuple4<String, String, Double, String>(new String(userId), new String(productId), s, new String(timestamp));
            } catch (Exception e){
                System.out.println("===========================");
                System.out.println(userId);
                System.out.println(productId);
                System.out.println(score);
                System.out.println("===========================");
                e.printStackTrace();
            }
        }
        return tuple4;
    }

    @Override
    public void configure(Configuration parameters) {
        try {
            this.table = createTable();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.scan = new Scan();
    }
    private HTable createTable() throws IOException {
        LOG.info("Initializing HBaseConfiguration");
        org.apache.hadoop.conf.Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.rootdir", Property.getStrValue("hbase.rootdir"));
        conf.set("hbase.zookeeper.quorum", Property.getStrValue("hbase.zookeeper.quorum"));
        conf.set("hbase.client.scanner.timeout.period", Property.getStrValue("hbase.client.scanner.timeout.period"));
        conf.set("hbase.rpc.timeout", Property.getStrValue("hbase.rpc.timeout"));
        System.out.println(Property.getStrValue("hbase.rootdir"));
        conn = ConnectionFactory.createConnection(conf);
        return new HTable(conf, this.getTableName());
    }

    @Override
    protected Tuple4<String, String, Double, String> mapResultToOutType(Result r) {
        return this.mapResultToTuple(r);
    }

}
