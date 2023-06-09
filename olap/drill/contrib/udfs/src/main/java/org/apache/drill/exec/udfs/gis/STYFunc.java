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
package org.apache.drill.exec.udfs.gis;

import io.netty.buffer.DrillBuf;
import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.holders.Float8Holder;
import org.apache.drill.exec.expr.holders.VarBinaryHolder;

import javax.inject.Inject;

/**
 * Return the Y coordinate of the point, or NaN if not available
 */
@FunctionTemplate(name = "st_y", scope = FunctionTemplate.FunctionScope.SIMPLE,
  nulls = FunctionTemplate.NullHandling.NULL_IF_NULL)
public class STYFunc implements DrillSimpleFunc {
  @Param
  VarBinaryHolder geomParam;

  @Output
  Float8Holder out;

  @Inject
  DrillBuf buffer;

  public void setup() {
  }

  public void eval() {

    com.esri.core.geometry.ogc.OGCGeometry geom;

    geom = com.esri.core.geometry.ogc.OGCGeometry
        .fromBinary(geomParam.buffer.nioBuffer(geomParam.start, geomParam.end - geomParam.start));

    if (geom != null && geom.geometryType().equals("Point")) {
      out.value = ((com.esri.core.geometry.ogc.OGCPoint) geom).Y();
    } else {
      out.value = Double.NaN;
    }
  }
}
