/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

// Convert form data into interface submission data format
export const valuesToData = (values, inlongGroupId) => {
  const array = values.map(item => {
    const {
      inlongStreamId,
      predefinedFields = [],
      rowTypeFields = [],
      dataSourceType,
      dataSourcesConfig = [],
      streamSink = [],
      ...rest
    } = item;
    const output = {} as any;
    if (dataSourceType !== 'AUTO_PUSH') {
      output.sourceInfo = dataSourcesConfig.map(k => {
        return {
          ...k,
          sourceType: dataSourceType,
          inlongGroupId,
          inlongStreamId,
        };
      });
    } else {
      output.sourceInfo = [
        {
          sourceType: dataSourceType,
          sourceName: inlongStreamId,
          inlongGroupId,
          inlongStreamId,
        },
      ];
    }

    output.sinkInfo = streamSink.reduce((acc, type) => {
      if (!type) return acc;

      const data = rest[`streamSink${type}`] || [];
      delete rest[`streamSink${type}`];
      const formatData = data.map(ds => ({
        ...ds,
        inlongGroupId,
        inlongStreamId,
        sinkType: type,
      }));

      return acc.concat(formatData);
    }, []);

    const fieldList = predefinedFields.concat(rowTypeFields).map((item, idx) => ({
      ...item,
      inlongGroupId,
      inlongStreamId,
      isPredefinedField: idx < predefinedFields.length ? 1 : 0,
    }));

    output.streamInfo = {
      ...rest,
      inlongGroupId,
      inlongStreamId,
      dataSourceType,
    };

    if (fieldList?.length) output.streamInfo.fieldList = fieldList;

    return output;
  });

  return array;
};

// Convert interface data to form data
export const dataToValues = data => {
  const array = data.map(item => {
    const { sourceInfo, sinkInfo, streamInfo } = item;
    let output = {
      dataSourceType: sourceInfo[0]?.sourceType || 'AUTO_PUSH',
      dataSourcesConfig: sourceInfo,
    } as any;

    sinkInfo.forEach(({ sinkType, ...item }) => {
      if (!output[`streamSink${sinkType}`]) output[`streamSink${sinkType}`] = [];
      output[`streamSink${sinkType}`].push(item);
    });
    output.streamSink = sinkInfo.map(item => item.sinkType);

    const fieldList = streamInfo.fieldList?.reduce(
      (acc, cur) => {
        cur.isPredefinedField ? acc.predefinedFields.push(cur) : acc.rowTypeFields.push(cur);
        return acc;
      },
      {
        predefinedFields: [],
        rowTypeFields: [],
      },
    );

    output = {
      hasHigher: false,
      ...output,
      ...fieldList,
      ...streamInfo,
    };

    return output;
  });

  return array;
};
