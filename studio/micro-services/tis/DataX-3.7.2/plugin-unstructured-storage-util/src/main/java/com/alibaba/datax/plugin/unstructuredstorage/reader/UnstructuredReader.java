package com.alibaba.datax.plugin.unstructuredstorage.reader;

import com.alibaba.datax.common.util.Configuration;
import com.alibaba.datax.plugin.unstructuredstorage.writer.Constant;
import com.alibaba.fastjson.JSON;
import com.csvreader.CsvReader;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-10-17 16:13
 **/
public abstract class UnstructuredReader {

    private static final Logger LOG = LoggerFactory.getLogger(UnstructuredReader.class);

    public static UnstructuredReader create(Configuration cfg, BufferedReader reader) {
        String fileFormat = cfg.getString(com.alibaba.datax.plugin.unstructuredstorage.writer.Key.FILE_FORMAT);

        if (Constant.FILE_FORMAT_TEXT.equals(fileFormat)) {
//BufferedReader reader, boolean skipHeader, String fieldDelimiter
            //  com.alibaba.datax.plugin.unstructuredstorage.reader.Constant.DEFAULT_SKIP_HEADER
            Boolean skipHeader = cfg.getBool(Key.SKIP_HEADER);
            if (skipHeader == null) {
                throw new IllegalArgumentException("param " + Key.SKIP_HEADER + " can not be null");
            }
            //com.alibaba.datax.plugin.unstructuredstorage.reader.Constant.DEFAULT_FIELD_DELIMITER
            Character fieldDelimiter = cfg.getChar(Key.FIELD_DELIMITER);
            if (fieldDelimiter == null) {
                throw new IllegalArgumentException("param " + Key.FIELD_DELIMITER + " can not be null");
            }
            TEXTFormat textFormat = new TEXTFormat(reader, skipHeader, fieldDelimiter);
            return textFormat;
        } else if (Constant.FILE_FORMAT_CSV.equals(fileFormat)) {
            return new CSVFormat(reader);
        } else {
            throw new IllegalArgumentException("illegal fileFormat:" + fileFormat);
        }
    }

    public abstract boolean hasNext() throws IOException;

    public abstract String[] next() throws IOException;

    private static class CSVFormat extends UnstructuredReader {
        private final CsvReader csvReader;

        public CSVFormat(BufferedReader reader) {
            csvReader = new CsvReader(reader);
            //csvReader.setDelimiter(fieldDelimiter);
            this.setCsvReaderConfig();
        }

        private void setCsvReaderConfig() {
            if (null != UnstructuredStorageReaderUtil.csvReaderConfigMap && !UnstructuredStorageReaderUtil.csvReaderConfigMap.isEmpty()) {
                try {
                    BeanUtils.populate(csvReader, UnstructuredStorageReaderUtil.csvReaderConfigMap);
                    LOG.info(String.format("csvReaderConfig设置成功,设置后CsvReader:%s", JSON.toJSONString(csvReader)));
                } catch (Exception e) {
                    LOG.info(String.format("WARN!!!!忽略csvReaderConfig配置!通过BeanUtils.populate配置您的csvReaderConfig发生异常,您配置的值为: %s;请检查您的配置!CsvReader使用默认值[%s]",
                            JSON.toJSONString(UnstructuredStorageReaderUtil.csvReaderConfigMap), JSON.toJSONString(csvReader)));
                }
            } else {
                //默认关闭安全模式, 放开10W字节的限制
                csvReader.setSafetySwitch(false);
                LOG.info(String.format("CsvReader使用默认值[%s],csvReaderConfig值为[%s]", JSON.toJSONString(csvReader), JSON.toJSONString(UnstructuredStorageReaderUtil.csvReaderConfigMap)));
            }
        }

        @Override
        public boolean hasNext() throws IOException {
            return csvReader.readRecord();
        }

        @Override
        public String[] next() throws IOException {
            String[] vals = csvReader.getValues();
            return vals;
        }

    }

    private static class TEXTFormat extends UnstructuredReader {

        private final BufferedReader reader;
        private final String header;
        private final String fieldDelimiter;

        private String[] rowVals = null;


        public TEXTFormat(BufferedReader reader, boolean skipHeader, Character fieldDelimiter) {
            this.reader = reader;
            this.fieldDelimiter = String.valueOf(fieldDelimiter.charValue());
            try {
                // skipHeader: false 说明header中有内容需要进行读取
                this.header = skipHeader ? null : reader.readLine();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public boolean hasNext() throws IOException {
            String line = reader.readLine();
            if (line == null) {
                return false;
            }
            rowVals = line.split(fieldDelimiter);
            return true;
        }

        @Override
        public String[] next() throws IOException {
            return this.rowVals;
        }
    }
}
