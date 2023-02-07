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
import org.apache.drill.exec.expr.DrillAggFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.annotations.Workspace;
import org.apache.drill.exec.expr.holders.BigIntHolder;
import org.apache.drill.exec.expr.holders.IntHolder;
import org.apache.drill.exec.expr.holders.NullableVarBinaryHolder;
import org.apache.drill.exec.expr.holders.ObjectHolder;
import org.apache.drill.exec.expr.holders.UInt1Holder;

import javax.inject.Inject;

/**
 * Returns a geometry that represents the point set union of the Geometries
 */
@FunctionTemplate(name = "st_unionaggregate", scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE)
public class STUnionAggregate implements DrillAggFunc {

  @Param NullableVarBinaryHolder in;
  @Workspace ObjectHolder value;
  @Workspace UInt1Holder init;
  @Workspace BigIntHolder nonNullCount;
  @Workspace IntHolder srid;
  @Inject DrillBuf buf;
  @Output NullableVarBinaryHolder out;

  public void setup() {
    init = new UInt1Holder();
    nonNullCount = new BigIntHolder();
    nonNullCount.value = 0;
    init.value = 0;
    value = new ObjectHolder();
    value.obj = new java.util.ArrayList<com.esri.core.geometry.Geometry>();
  }

  @Override
  public void add() {

    if (in.isSet == 0) {
      // processing nullable input and the value is null, so don't do anything...
      return;
    }
    nonNullCount.value = 1;
    java.util.List<com.esri.core.geometry.Geometry> tmp = (java.util.ArrayList<com.esri.core.geometry.Geometry>) value.obj;

    com.esri.core.geometry.ogc.OGCGeometry geom;
    geom = com.esri.core.geometry.ogc.OGCGeometry.fromBinary(in.buffer.nioBuffer(in.start, in.end - in.start));

    tmp.add(geom.getEsriGeometry());

    if (init.value == 0) {
      init.value = 1;
      srid.value = geom.SRID();
    }
  }

  @Override
  public void output() {
    if (nonNullCount.value > 0) {
      out.isSet = 1;

      java.util.List<com.esri.core.geometry.Geometry> tmp = (java.util.ArrayList<com.esri.core.geometry.Geometry>) value.obj;

      com.esri.core.geometry.SpatialReference spatialRef = null;
      if (srid.value != 0){
        spatialRef = com.esri.core.geometry.SpatialReference.create(4326);
      }
      com.esri.core.geometry.Geometry[] geomArr =
          (com.esri.core.geometry.Geometry[]) tmp.toArray(new com.esri.core.geometry.Geometry[0]);
      com.esri.core.geometry.Geometry geom = com.esri.core.geometry.GeometryEngine.union(geomArr, spatialRef);

      com.esri.core.geometry.ogc.OGCGeometry unionGeom = com.esri.core.geometry.ogc.OGCGeometry.createFromEsriGeometry(geom, spatialRef);
      java.nio.ByteBuffer unionGeomBytes = unionGeom.asBinary();

      int outputSize = unionGeomBytes.remaining();
      buf = out.buffer = buf.reallocIfNeeded(outputSize);
      out.start = 0;
      out.end = outputSize;
      buf.setBytes(0, unionGeomBytes);
    } else {
      out.isSet = 0;
    }
  }

  @Override
  public void reset() {
    value = new ObjectHolder();
    value.obj = new java.util.ArrayList<com.esri.core.geometry.Geometry>();
    init.value = 0;
    nonNullCount.value = 0;
  }
}
