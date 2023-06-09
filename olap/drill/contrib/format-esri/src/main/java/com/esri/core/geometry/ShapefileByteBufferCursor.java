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
package com.esri.core.geometry;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ShapefileByteBufferCursor extends ByteBufferCursor {
  ByteBuffer fileBuffer;
  int headerOffset = 100;
  int currOffset = headerOffset; //skip shp header
  int fileLen;
  int recordIndex = 0;

  public ShapefileByteBufferCursor(ByteBuffer buffer) {
    this.fileBuffer = buffer;

    fileBuffer.order(ByteOrder.BIG_ENDIAN);
    int fileLengthWords = fileBuffer.getInt(24); // length in 16bit words
    fileLen = fileLengthWords * 2;
  }

  @Override
  public int getByteBufferID() {
    return recordIndex;
  }

  @Override
  public ByteBuffer next() {
    ByteBuffer shpGeometryRow = getSingleRowFromShp();
    return shpGeometryRow;
  }

  private ByteBuffer getSingleRowFromShp() {
    fileBuffer.order(ByteOrder.BIG_ENDIAN);
    ByteBuffer shapeBuffer = null;

    if (currOffset < fileLen) {
      fileBuffer.order(ByteOrder.BIG_ENDIAN);
      recordIndex = fileBuffer.getInt(currOffset);
      int contentLen = fileBuffer.getInt(currOffset + 4) * 2;
      fileBuffer.order(ByteOrder.LITTLE_ENDIAN);
      byte[] dstArr = new byte[contentLen];

      fileBuffer.position(currOffset + 8); // skip record header
      fileBuffer.get(dstArr, 0, contentLen);

      shapeBuffer = ByteBuffer.wrap(dstArr);
      shapeBuffer.order(ByteOrder.LITTLE_ENDIAN);

      int tmpPos = shapeBuffer.position();
      shapeBuffer.position(tmpPos);
      currOffset = currOffset + 8 + contentLen; // skip rec header and go to next rec
    }
    return shapeBuffer;
  }
}
