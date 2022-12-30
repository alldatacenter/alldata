/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.connector.legacy.ftp.source;

import com.bytedance.bitsail.batch.parser.row.TextRowBuilder;
import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.type.filemapping.FileMappingTypeInfoConverter;
import com.bytedance.bitsail.component.format.api.RowBuilder;
import com.bytedance.bitsail.connector.legacy.ftp.client.FtpHandlerFactory;
import com.bytedance.bitsail.connector.legacy.ftp.client.IFtpHandler;
import com.bytedance.bitsail.connector.legacy.ftp.common.FtpConfig;
import com.bytedance.bitsail.connector.legacy.ftp.error.FtpInputFormatErrorCode;
import com.bytedance.bitsail.connector.legacy.ftp.option.FtpReaderOptions;
import com.bytedance.bitsail.flink.core.legacy.connector.InputFormatPlugin;
import com.bytedance.bitsail.flink.core.typeutils.ColumnFlinkTypeInfoUtil;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.io.DefaultInputSplitAssigner;
import org.apache.flink.api.common.io.statistics.BaseStatistics;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.core.io.InputSplit;
import org.apache.flink.core.io.InputSplitAssigner;
import org.apache.flink.types.Row;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class FtpInputFormat extends InputFormatPlugin<Row, InputSplit> implements ResultTypeQueryable<Row> {
  private transient String line;
  private transient FtpSeqBufferedReader br;
  private transient IFtpHandler ftpHandler;

  protected RowTypeInfo rowTypeInfo;
  protected RowBuilder rowBuilder;
  protected FtpConfig ftpConfig;
  protected String successFilePath;
  protected long fileSize;

  @Override
  public void initPlugin() throws Exception {
    initFtpConfig();
    this.ftpHandler = FtpHandlerFactory.createFtpHandler(ftpConfig.getProtocol());

    checkPathsExist();
    Boolean enableSuccessFileCheck = inputSliceConfig.get(FtpReaderOptions.ENABLE_SUCCESS_FILE_CHECK);
    if (enableSuccessFileCheck) {
      this.successFilePath = inputSliceConfig.getNecessaryOption(FtpReaderOptions.SUCCESS_FILE_PATH, FtpInputFormatErrorCode.SUCCESS_FILE_NOT_EXIST);
      checkSuccessFileExist();
    }

    this.rowBuilder = new TextRowBuilder(inputSliceConfig);

    List<ColumnInfo> columnInfos = inputSliceConfig.getNecessaryOption(FtpReaderOptions.COLUMNS, FtpInputFormatErrorCode.REQUIRED_VALUE);
    this.rowTypeInfo = ColumnFlinkTypeInfoUtil.getRowTypeInformation(new FileMappingTypeInfoConverter(StringUtils.lowerCase(getType())), columnInfos);
    log.info("Row Type Info: " + rowTypeInfo);
  }

  @Override
  public Row buildRow(Row reuse, String mandatoryEncoding) throws BitSailException {
    rowBuilder.build(line, reuse, mandatoryEncoding, rowTypeInfo);
    return reuse;
  }

  @Override
  public boolean isSplitEnd() throws IOException {
    line = br.readLine();
    hasNext = line != null;
    return !hasNext;
  }

  @Override
  public InputSplit[] createSplits(int minNumSplits) throws IOException {
    List<String> files = new ArrayList<>();
    String[] paths = ftpConfig.getPaths();
    IFtpHandler ftpHandler = FtpHandlerFactory.createFtpHandler(ftpConfig.getProtocol());
    ftpHandler.loginFtpServer(ftpConfig);

    for (String path : paths) {
      // get all files of paths
      if (null != path && path.length() > 0) {
        path = path.replace("\n", "").replace("\r", "");
        String[] pathArray = StringUtils.split(path, ",");
        for (String p : pathArray) {
          files.addAll(ftpHandler.getFiles(p.trim()));
        }
      }
      this.fileSize += ftpHandler.getFilesSize(path);
    }
    int numSplits = files.size();
    FtpInputSplit[] ftpInputSplits = new FtpInputSplit[numSplits];
    for (int index = 0; index < numSplits; ++index) {
      ftpInputSplits[index] = new FtpInputSplit();
      ftpInputSplits[index].getPaths().add(files.get(index));
    }

    ftpHandler.logoutFtpServer();
    log.info("FtpInputFormat Input splits size: {}", numSplits);
    return ftpInputSplits;
  }

  @Override
  public String getType() {
    return "FTP";
  }

  @Override
  public void openInputFormat() throws IOException {
    super.openInputFormat();
    this.ftpHandler = FtpHandlerFactory.createFtpHandler(ftpConfig.getProtocol());
    this.ftpHandler.loginFtpServer(ftpConfig);
  }

  @Override
  public void closeInputFormat() throws IOException {
    super.closeInputFormat();
    // if inputSplit is empty,ftpHandler will be null
    if (null != this.ftpHandler) {
      this.ftpHandler.logoutFtpServer();
    }
  }

  @Override
  public BaseStatistics getStatistics(BaseStatistics cachedStatistics) throws IOException {
    return new BaseStatistics() {
      @Override
      public long getTotalInputSize() {
        return fileSize;
      }

      @Override
      public long getNumberOfRecords() {
        return SIZE_UNKNOWN;
      }

      @Override
      public float getAverageRecordWidth() {
        return AVG_RECORD_BYTES_UNKNOWN;
      }
    };
  }

  @Override
  public InputSplitAssigner getInputSplitAssigner(InputSplit[] inputSplits) {
    return new DefaultInputSplitAssigner(inputSplits);
  }

  @Override
  public void open(InputSplit split) throws IOException {
    FtpInputSplit inputSplit = (FtpInputSplit) split;
    List<String> paths = inputSplit.getPaths();
    if (this.ftpConfig.getSkipFirstLine()) {
      this.br = new FtpSeqBufferedReader(ftpHandler, paths.iterator());
      this.br.setFromLine(1);
    } else {
      this.br = new FtpSeqBufferedReader(ftpHandler, paths.iterator());
      this.br.setFromLine(0);
    }
    br.setCharsetName("utf-8");
  }

  @Override
  public void close() throws IOException {
    if (null != br) {
      br.close();
    }
  }

  @Override
  public TypeInformation<Row> getProducedType() {
    return rowTypeInfo;
  }

  private void checkSuccessFileExist() throws IOException, InterruptedException {
    IFtpHandler ftpHandler = FtpHandlerFactory.createFtpHandler(ftpConfig.getProtocol());
    ftpHandler.loginFtpServer(ftpConfig);
    boolean fileExistFlag = false;
    int successFileRetryLeftTimes = ftpConfig.getMaxRetryTime();
    while (!fileExistFlag && successFileRetryLeftTimes-- > 0) {
      fileExistFlag = ftpHandler.isFileExist(this.successFilePath);
      if (!fileExistFlag) {
        log.info("SUCCESS tag file " + this.successFilePath + " is not exist, waiting for retry...");
        // wait for retry if not SUCCESS tag file
        Thread.sleep(ftpConfig.getRetryIntervalMs());
      }
    }
    if (!fileExistFlag) {
      throw BitSailException.asBitSailException(FtpInputFormatErrorCode.SUCCESS_FILE_NOT_EXIST,
          "Success file is not ready after " + ftpConfig.getMaxRetryTime() + " retry, please wait upstream is ready");
    }
    ftpHandler.logoutFtpServer();
  }

  private void checkPathsExist() {
    IFtpHandler ftpHandler = FtpHandlerFactory.createFtpHandler(ftpConfig.getProtocol());
    ftpHandler.loginFtpServer(ftpConfig);
    for (String path : ftpConfig.getPaths()) {
      if (!ftpHandler.isPathExist(path)) {
        throw BitSailException.asBitSailException(FtpInputFormatErrorCode.FILEPATH_NOT_EXIST,
            "Given filePath is not exist: " + path + " , please check paths is correct");
      }
    }
  }

  private void initFtpConfig() {
    int port = inputSliceConfig.get(FtpReaderOptions.PORT);
    String host = inputSliceConfig.getNecessaryOption(FtpReaderOptions.HOST, CommonErrorCode.CONFIG_ERROR);
    String[] paths = inputSliceConfig.getNecessaryOption(FtpReaderOptions.PATH_LIST, CommonErrorCode.CONFIG_ERROR).split(",");
    String user = inputSliceConfig.getNecessaryOption(FtpReaderOptions.USER, CommonErrorCode.CONFIG_ERROR);
    String password = inputSliceConfig.getNecessaryOption(FtpReaderOptions.PASSWORD, CommonErrorCode.CONFIG_ERROR);
    int timeout = inputSliceConfig.get(FtpReaderOptions.TIME_OUT);
    int maxSuccessFileDetectRetryTime = inputSliceConfig.get(FtpReaderOptions.MAX_RETRY_TIME);
    int retryIntervalMs = inputSliceConfig.get(FtpReaderOptions.RETRY_INTERVAL_MS);
    Boolean skipFirstLine = inputSliceConfig.get(FtpReaderOptions.SKIP_FIRST_LINE);
    String protocol = inputSliceConfig.getNecessaryOption(FtpReaderOptions.PROTOCOL, CommonErrorCode.CONFIG_ERROR);
    String connectPattern = inputSliceConfig.get(FtpReaderOptions.CONNECT_PATTERN);

    this.ftpConfig = new FtpConfig();
    this.ftpConfig.setPort(port);
    this.ftpConfig.setHost(host);
    this.ftpConfig.setPaths(paths);
    this.ftpConfig.setUsername(user);
    this.ftpConfig.setPassword(password);
    this.ftpConfig.setTimeout(timeout);
    this.ftpConfig.setMaxRetryTime(maxSuccessFileDetectRetryTime);
    this.ftpConfig.setRetryIntervalMs(retryIntervalMs);
    this.ftpConfig.setSkipFirstLine(skipFirstLine);
    this.ftpConfig.setProtocol(FtpConfig.Protocol.valueOf(protocol.toUpperCase()));
    this.ftpConfig.setConnectPattern(FtpConfig.ConnectPattern.valueOf(connectPattern.toUpperCase()));
  }
}
