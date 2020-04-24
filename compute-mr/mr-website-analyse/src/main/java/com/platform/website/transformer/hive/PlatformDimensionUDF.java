package com.platform.website.transformer.hive;

import com.platform.website.transformer.model.dim.base.PlatformDimension;
import com.platform.website.transformer.service.rpc.IDimensionConverter;
import com.platform.website.transformer.service.rpc.client.DimensionConverterClient;
import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.ql.exec.UDF;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;

/**
 * 操作平台dimension 相关的udf
 * 
 * @author wulinhao
 *
 */
public class PlatformDimensionUDF extends UDF {
    private IDimensionConverter converter = null;

    public PlatformDimensionUDF() {
        try {
            this.converter = DimensionConverterClient.createDimensionConverter(new Configuration());
        } catch (IOException e) {
            throw new RuntimeException("创建converter异常");
        }

        // 添加一个钩子进行关闭操作
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    DimensionConverterClient.stopDimensionConverterProxy(converter);
                } catch (Throwable e) {
                    // nothing
                }
            }
        }));
    }

    /**
     * 根据给定的平台名称返回对应的id
     * 
     * @param platformName
     * @return
     */
    public IntWritable evaluate(Text platformName) {
        PlatformDimension dimension = new PlatformDimension(platformName.toString());
        try {
            int id = this.converter.getDimensionIdByValue(dimension);
            return new IntWritable(id);
        } catch (IOException e) {
            throw new RuntimeException("获取id异常");
        }
    }
}
