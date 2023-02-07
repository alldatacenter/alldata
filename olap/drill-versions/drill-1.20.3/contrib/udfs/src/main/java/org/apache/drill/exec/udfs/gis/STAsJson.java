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
/*
 * Wrapper for ESRI ST_AsJson to convert geometry into REST Json.
 * Emulates functionality from spatial-framework-for-hadoop.
 */
package org.apache.drill.exec.udfs.gis;

import io.netty.buffer.DrillBuf;
import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.holders.VarBinaryHolder;
import org.apache.drill.exec.expr.holders.VarCharHolder;

import javax.inject.Inject;

@FunctionTemplate(name = "st_asjson", scope = FunctionTemplate.FunctionScope.SIMPLE,
  nulls = FunctionTemplate.NullHandling.NULL_IF_NULL)
public class STAsJson implements DrillSimpleFunc {
  @Param
  VarBinaryHolder geomParam;

  @Output
  VarCharHolder out;

  @Inject
  DrillBuf buffer;

  public void setup() {
  }

  public void eval() {
    com.esri.core.geometry.ogc.OGCGeometry geom = com.esri.core.geometry.ogc.OGCGeometry
      .fromBinary(geomParam.buffer.nioBuffer(geomParam.start, geomParam.end - geomParam.start));

    String json = geom.asJson();
    byte[] jsonBytes = json.getBytes();
    int outputSize = jsonBytes.length;

    buffer = out.buffer = buffer.reallocIfNeeded(outputSize);
    out.start = 0;
    out.end = outputSize;
    buffer.setBytes(0, jsonBytes);
  }
}
