package com.alibaba.datax.plugin.reader.hdfsreader;

import com.alibaba.datax.common.plugin.RecordSender;
import com.alibaba.datax.common.plugin.TaskPluginCollector;
import com.alibaba.datax.common.util.Configuration;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-07-12 11:36
 **/
public interface IDFSUtil {
    HashSet<String> getAllFiles(List<String> srcPaths, String specifiedFileType);

    HashSet<String> getHDFSAllFiles(String hdfsPath);

    InputStream getInputStream(String filepath);

    void sequenceFileStartRead(String sourceSequenceFilePath, Configuration readerSliceConfig,
                               RecordSender recordSender, TaskPluginCollector taskPluginCollector);

    void rcFileStartRead(String sourceRcFilePath, Configuration readerSliceConfig,
                         RecordSender recordSender, TaskPluginCollector taskPluginCollector);

    void orcFileStartRead(String sourceOrcFilePath, Configuration readerSliceConfig,
                          RecordSender recordSender, TaskPluginCollector taskPluginCollector);

    boolean checkHdfsFileType(String filepath, String specifiedFileType);
}
