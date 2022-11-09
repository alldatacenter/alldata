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

package com.bytedance.bitsail.connector.legacy.larksheet.source;

import com.bytedance.bitsail.connector.legacy.larksheet.meta.SheetInfo;
import com.bytedance.bitsail.connector.legacy.larksheet.meta.SheetMeta;

import org.apache.flink.core.io.InputSplit;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class LarkSheetInputFormatTest {

  @Test
  public void testCalculateLarkSheetInputSplits() throws Exception {
    SheetMeta sheetMeta = new SheetMeta(4, 20, 40, "", "", new Object());
    SheetInfo sheetInfo = new SheetInfo(sheetMeta, "", "");
    List<SheetInfo> sheetInfoList = new ArrayList<>();
    sheetInfoList.add(sheetInfo);
    sheetInfoList.add(sheetInfo);
    List<Integer>skipNums = new ArrayList<>();
    Method method = Class.forName("com.bytedance.bitsail.connector.legacy.larksheet.source.LarkSheetInputFormat")
        .getDeclaredMethod("calculateLarkSheetInputSplits", List.class, int.class, List.class);
    method.setAccessible(true);
    InputSplit[] inputSplits = (InputSplit[]) method.invoke(LarkSheetInputFormat.class, sheetInfoList, 20, skipNums);
    Assert.assertEquals(2, ((LarkSheetInputSplit) inputSplits[0]).getStartRowNumber());
    Assert.assertEquals(21, ((LarkSheetInputSplit) inputSplits[0]).getEndRowNumber());
    Assert.assertEquals(22, ((LarkSheetInputSplit) inputSplits[1]).getStartRowNumber());
    Assert.assertEquals(41, ((LarkSheetInputSplit) inputSplits[1]).getEndRowNumber());
    Assert.assertEquals(2, ((LarkSheetInputSplit) inputSplits[2]).getStartRowNumber());
    Assert.assertEquals(21, ((LarkSheetInputSplit) inputSplits[2]).getEndRowNumber());
    Assert.assertEquals(22, ((LarkSheetInputSplit) inputSplits[3]).getStartRowNumber());
    Assert.assertEquals(41, ((LarkSheetInputSplit) inputSplits[3]).getEndRowNumber());
    skipNums.add(15);
    skipNums.add(21);
    inputSplits = (InputSplit[]) method.invoke(LarkSheetInputFormat.class, sheetInfoList, 20, skipNums);
    Assert.assertEquals(17, ((LarkSheetInputSplit) inputSplits[0]).getStartRowNumber());
    Assert.assertEquals(36, ((LarkSheetInputSplit) inputSplits[0]).getEndRowNumber());
    Assert.assertEquals(37, ((LarkSheetInputSplit) inputSplits[1]).getStartRowNumber());
    Assert.assertEquals(56, ((LarkSheetInputSplit) inputSplits[1]).getEndRowNumber());
    Assert.assertEquals(23, ((LarkSheetInputSplit) inputSplits[2]).getStartRowNumber());
    Assert.assertEquals(42, ((LarkSheetInputSplit) inputSplits[2]).getEndRowNumber());
    skipNums.set(0, 50);
    inputSplits = (InputSplit[]) method.invoke(LarkSheetInputFormat.class, sheetInfoList, 20, skipNums);
    Assert.assertEquals(23, ((LarkSheetInputSplit) inputSplits[0]).getStartRowNumber());
    Assert.assertEquals(42, ((LarkSheetInputSplit) inputSplits[0]).getEndRowNumber());
  }
}
