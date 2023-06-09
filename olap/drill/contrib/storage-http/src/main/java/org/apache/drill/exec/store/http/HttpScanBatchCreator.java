/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.store.http;

import okhttp3.HttpUrl;
import org.apache.drill.common.exceptions.ChildErrorContext;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.ops.ExecutorFragmentContext;
import org.apache.drill.exec.physical.impl.BatchCreator;
import org.apache.drill.exec.physical.impl.scan.framework.ManagedReader;
import org.apache.drill.exec.physical.impl.scan.framework.ManagedScanFramework;
import org.apache.drill.exec.physical.impl.scan.framework.ManagedScanFramework.ReaderFactory;
import org.apache.drill.exec.physical.impl.scan.framework.ManagedScanFramework.ScanFrameworkBuilder;
import org.apache.drill.exec.physical.impl.scan.framework.SchemaNegotiator;
import org.apache.drill.exec.record.CloseableRecordBatch;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.store.http.HttpPaginatorConfig.PaginatorMethod;
import org.apache.drill.exec.store.http.paginator.OffsetPaginator;
import org.apache.drill.exec.store.http.paginator.PagePaginator;
import org.apache.drill.exec.store.http.paginator.Paginator;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class HttpScanBatchCreator implements BatchCreator<HttpSubScan> {

  private static final Logger logger = LoggerFactory.getLogger(HttpScanBatchCreator.class);

  @Override
  public CloseableRecordBatch getBatch(ExecutorFragmentContext context,
                                       HttpSubScan subScan,
                                       List<RecordBatch> children) throws ExecutionSetupException {
    Preconditions.checkArgument(children.isEmpty());
    try {
      ScanFrameworkBuilder builder = createBuilder(context.getOptions(), subScan);
      return builder.buildScanOperator(context, subScan);
    } catch (UserException e) {
      // Rethrow user exceptions directly
      throw e;
    } catch (Throwable e) {
      // Wrap all others
      throw new ExecutionSetupException(e);
    }
  }

  private ScanFrameworkBuilder createBuilder(OptionManager options,
      HttpSubScan subScan) {
    ScanFrameworkBuilder builder = new ScanFrameworkBuilder();
    builder.projection(subScan.columns());
    builder.setUserName(subScan.getUserName());

    // Provide custom error context
    builder.errorContext(
        new ChildErrorContext(builder.errorContext()) {
          @Override
          public void addContext(UserException.Builder builder) {
            builder.addContext("Connection", subScan.tableSpec().connection());
            builder.addContext("Plugin", subScan.tableSpec().pluginName());
          }
        });

    // Reader
    ReaderFactory readerFactory = new HttpReaderFactory(subScan);
    builder.setReaderFactory(readerFactory);
    builder.nullType(Types.optional(MinorType.VARCHAR));

    // TODO Add page size limit here to ScanFramework Builder

    return builder;
  }

  private static class HttpReaderFactory implements ReaderFactory {
    private final HttpSubScan subScan;
    private final HttpPaginatorConfig paginatorConfig;
    private Paginator paginator;

    private int count;

    public HttpReaderFactory(HttpSubScan subScan) {
      this.subScan = subScan;

      paginatorConfig = subScan.tableSpec().connectionConfig().paginator();
      if (paginatorConfig != null) {
        // TODO Handle the case of no limit queries in pagination
        logger.debug("Creating paginator using config: {}", paginatorConfig);

        // Initialize the paginator and generate the base URLs
        this.paginator = getPaginator();
      }
    }

    private Paginator getPaginator() {
      HttpUrl.Builder urlBuilder;
      HttpUrl rawUrl;

      // Append table name, if present.
      if (subScan.tableSpec().tableName() != null) {
        rawUrl = HttpUrl.parse(subScan.tableSpec().connectionConfig().url() + subScan.tableSpec().tableName());
      } else {
        rawUrl = HttpUrl.parse(subScan.tableSpec().connectionConfig().url());
      }



      // If the URL is not parsable or otherwise invalid
      if (rawUrl == null) {
        throw UserException.validationError()
          .message("Invalid URL: " + subScan.tableSpec().connectionConfig().url())
          .build(logger);
      }

      urlBuilder = rawUrl.newBuilder();

      Paginator paginator = null;
      if (paginatorConfig.getMethodType() == PaginatorMethod.OFFSET) {
        paginator = new OffsetPaginator(urlBuilder,
          subScan.maxRecords(),
          paginatorConfig.pageSize(),
          paginatorConfig.limitParam(),
          paginatorConfig.offsetParam());
      } else if (paginatorConfig.getMethodType() == PaginatorMethod.PAGE) {
        paginator = new PagePaginator(urlBuilder,
          subScan.maxRecords(),
          paginatorConfig.pageSize(),
          paginatorConfig.pageParam(),
          paginatorConfig.pageSizeParam());
      }
      return paginator;
    }

    @Override
    public void bind(ManagedScanFramework framework) { }

    @Override
    public ManagedReader<SchemaNegotiator> next() {
      logger.debug("Getting new batch reader.");

      // Get the expected input type
      String inputType = subScan.tableSpec().connectionConfig().inputType();

      // Only a single scan (in a single thread)
      if (count++ == 0 && paginatorConfig == null) {
        // Case for no pagination
        if (inputType.equalsIgnoreCase(HttpApiConfig.CSV_INPUT_FORMAT)) {
          return new HttpCSVBatchReader(subScan);
        } else if (inputType.equalsIgnoreCase(HttpApiConfig.XML_INPUT_FORMAT)) {
          return new HttpXMLBatchReader(subScan);
        } else {
          return new HttpBatchReader(subScan);
        }
      } else if (paginatorConfig != null) {
        /*
        * If the paginator is not null we create a new batch reader for each
        * URL that it generates. In the future, this could be parallelized in
        * the group scan such that the calls could be sent to different drillbits.
        */
        if (!paginator.hasNext()) {
          return null;
        }

        if (inputType.equalsIgnoreCase(HttpApiConfig.CSV_INPUT_FORMAT)) {
          return new HttpCSVBatchReader(subScan, paginator);
        } else if (inputType.equalsIgnoreCase(HttpApiConfig.XML_INPUT_FORMAT)) {
          return new HttpXMLBatchReader(subScan, paginator);
        } else {
          return new HttpBatchReader(subScan, paginator);
        }
      }
      logger.debug("No new batch reader.");
      return null;
    }
  }
}
