/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.log4j.hadoop.mapreduce.jobhistory;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.ambari.log4j.common.LogParser;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.tools.rumen.Hadoop20JHParser;
import org.apache.hadoop.tools.rumen.JobHistoryParser;
import org.apache.hadoop.util.LineReader;
import org.apache.log4j.spi.LoggingEvent;

public class MapReduceJobHistoryParser implements LogParser {
  private JobHistoryParser parser;
  private LogLineReader reader = new LogLineReader("Meta VERSION=\"1\" .");
  
  public MapReduceJobHistoryParser() {
    try {
      parser = new Hadoop20JHParser(reader);
    } catch (IOException ioe) {
      // SHOULD NEVER HAPPEN!
      throw new RuntimeException(ioe);
    }
  }

  @Override
  public void addEventToParse(LoggingEvent event) {
    reader.addLine(event.getMessage().toString());
  }
  
  @Override
  public Object getParseResult() throws IOException {
    return parser.nextEvent();
  }

  static class LogLineReader extends LineReader {

    private Queue<String> lines = new LinkedBlockingQueue<String>();
    
    public LogLineReader(String line) {
      super(null);
      addLine(line);
    }

    private void addLine(String line) {
      lines.add(line);
    }
    
    public int readLine(Text str) throws IOException {
      String line = lines.poll();
      if (line != null) {
        str.set(line);
        return line.length();
      }
      
      return 0;
    }
    
    public void close() throws IOException {
    }
  }
}
