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
import org.apache.drill.common.AutoCloseables;
import org.apache.drill.common.exceptions.ChildErrorContext;
import org.apache.drill.common.exceptions.CustomErrorContext;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.physical.impl.scan.framework.SchemaNegotiator;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.store.ImplicitColumnUtils.ImplicitColumns;
import org.apache.drill.exec.store.http.paginator.Paginator;
import org.apache.drill.exec.store.http.util.SimpleHttp;
import org.apache.drill.exec.store.xml.XMLReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.InputStream;

public class HttpXMLBatchReader extends HttpBatchReader {
  private static final Logger logger = LoggerFactory.getLogger(HttpXMLBatchReader.class);

  private final HttpSubScan subScan;
  private final int maxRecords;
  private final int dataLevel;
  private InputStream inStream;
  private XMLReader xmlReader;
  private ResultSetLoader resultLoader;

  public HttpXMLBatchReader(HttpSubScan subScan) {
    super(subScan);
    this.subScan = subScan;
    this.maxRecords = subScan.maxRecords();
    this.dataLevel = subScan.tableSpec().connectionConfig().xmlDataLevel();
  }


  public HttpXMLBatchReader(HttpSubScan subScan, Paginator paginator) {
    super(subScan, paginator);
    this.subScan = subScan;
    this.maxRecords = subScan.maxRecords();
    this.dataLevel = subScan.tableSpec().connectionConfig().xmlDataLevel();
  }

  @Override
  public boolean open(SchemaNegotiator negotiator) {

    HttpUrl url = buildUrl();

    // Result set loader setup
    String tempDirPath = negotiator.drillConfig().getString(ExecConstants.DRILL_TMP_DIR);

    // Create user-friendly error context
    CustomErrorContext errorContext = new ChildErrorContext(negotiator.parentErrorContext()) {
      @Override
      public void addContext(UserException.Builder builder) {
        super.addContext(builder);
        builder.addContext("URL", url.toString());
      }
    };
    negotiator.setErrorContext(errorContext);

    // Http client setup
    SimpleHttp http = SimpleHttp.builder()
      .scanDefn(subScan)
      .url(url)
      .tempDir(new File(tempDirPath))
      .paginator(paginator)
      .proxyConfig(proxySettings(negotiator.drillConfig(), url))
      .errorContext(errorContext)
      .build();

    // Get the input stream
    inStream = http.getInputStream();
    // Initialize the XMLReader the reader
    try {
      xmlReader = new XMLReader(inStream, dataLevel, maxRecords);
      resultLoader = negotiator.build();

      if (implicitColumnsAreProjected()) {
        implicitColumns = new ImplicitColumns(resultLoader.writer());
        buildImplicitColumns();
        populateImplicitFieldMap(http);
      }

      RowSetLoader rootRowWriter = resultLoader.writer();
      xmlReader.open(rootRowWriter, errorContext);
      xmlReader.implicitFields(implicitColumns);
    } catch (XMLStreamException e) {
      throw UserException
        .dataReadError(e)
        .message("Error opening XML stream: " + e.getMessage())
        .addContext(errorContext)
        .build(logger);
    }

    // Close cache resources
    http.close();
    return true;
  }

  @Override
  public boolean next() {
    boolean result;
    try {
      result = xmlReader.next();
    } catch (UserException e) {
      // This covers the case of an empty XML response.  We don't want to throw an
      // exception, just catch anything and halt execution.  Otherwise, throw the original exception.
      if (e.getMessage().contains("EOF")) {
        return false;
      } else {
        throw e;
      }
    }

    if (paginator != null &&
      maxRecords < 0 && (resultLoader.totalRowCount()) < paginator.getPageSize()) {
      paginator.notifyPartialPage();
    }

    return result;
  }

  @Override
  public void close() {
    AutoCloseables.closeSilently(xmlReader);
    AutoCloseables.closeSilently(inStream);
  }
}
