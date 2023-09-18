package com.alibaba.datax.plugin.writer.hdfswriter;

import com.alibaba.datax.common.element.Record;
import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.plugin.RecordReceiver;
import com.alibaba.datax.common.plugin.TaskPluginCollector;
import com.alibaba.datax.common.util.Configuration;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.mapred.*;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-02-08 17:24
 **/
public class TextFileUtils {
    /**
     * 写textfile类型文件
     *
     * @param lineReceiver
     * @param config
     * @param fileName
     * @param taskPluginCollector
     */
    public static void startTextWrite(HdfsHelper fsHelper, RecordReceiver lineReceiver, Configuration config, String fileName,
                                      TaskPluginCollector taskPluginCollector) {
        char fieldDelimiter = config.getChar(Key.FIELD_DELIMITER);
        //   List<Configuration> columns = config.getListConfiguration(Key.COLUMN);
        FileFormatUtils.ColumnTypeValInspectors colsMeta
                = FileFormatUtils.getColumnTypeInspectors(HdfsColMeta.getColsMeta(config));
        String compress = config.getString(Key.COMPRESS, null);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmm");
        String attempt = "attempt_" + dateFormat.format(new Date()) + "_0001_m_000000_0";
        Path outputPath = new Path(fileName);
        //todo 需要进一步确定TASK_ATTEMPT_ID
        fsHelper.conf.set(JobContext.TASK_ATTEMPT_ID, attempt);
        FileOutputFormat outFormat = new TextOutputFormat();
        outFormat.setOutputPath(fsHelper.conf, outputPath);
        outFormat.setWorkOutputPath(fsHelper.conf, outputPath);
        if (null != compress) {
            Class<? extends CompressionCodec> codecClass = FileFormatUtils.getCompressCodec(compress);
            if (null != codecClass) {
                outFormat.setOutputCompressorClass(fsHelper.conf, codecClass);
            }
        }
        try {
            RecordWriter writer = outFormat.getRecordWriter(fsHelper.fileSystem, fsHelper.conf, outputPath.toString(), Reporter.NULL);
            Record record = null;
            Object[] tmpRowVals = new Object[colsMeta.getColsSize()];
            while ((record = lineReceiver.getFromReader()) != null) {
                MutablePair<Text, Boolean> transportResult
                        = FileFormatUtils.transportOneRecord(record, fieldDelimiter, tmpRowVals, colsMeta.getColumnValGetters(), taskPluginCollector);
                if (!transportResult.getRight()) {
                    writer.write(NullWritable.get(), transportResult.getLeft());
                }
            }
            writer.close(Reporter.NULL);
        } catch (Exception e) {
            String message = String.format("写文件文件[%s]时发生IO异常,请检查您的网络是否正常！", fileName);
            HdfsHelper.LOG.error(message);
            Path path = new Path(fileName);
            HdfsHelper.deleteDir(fsHelper.fileSystem, path.getParent());
            throw DataXException.asDataXException(HdfsWriterErrorCode.Write_FILE_IO_ERROR, e);
        }
    }
}
