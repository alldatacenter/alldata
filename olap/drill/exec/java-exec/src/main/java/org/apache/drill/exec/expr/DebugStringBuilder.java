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
package org.apache.drill.exec.expr;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.sun.codemodel.JFormatter;

/**
 * Utility class to build a debug string for an object
 * in a standard format. That format is:
 * <pre>[<i>className</i>:
 *  <i>variable=<value>... ]</pre>
 */

public class DebugStringBuilder {

  private final StringWriter strWriter;
  private final PrintWriter writer;
  private final JFormatter fmt;

  public DebugStringBuilder( Object obj ) {
    strWriter = new StringWriter( );
    writer = new PrintWriter( strWriter );
    writer.print( "[" );
    writer.print( obj.getClass().getSimpleName() );
    writer.print( ": " );
    fmt = new JFormatter( writer );
  }

  public DebugStringBuilder append( String s ) {
    writer.print( s );
    return this;
  }

  @Override
  public String toString( ) {
    writer.print( "]" );
    writer.flush();
    return strWriter.toString();
  }

  public JFormatter formatter() { return fmt; }
  public PrintWriter writer() { return writer; }

}
