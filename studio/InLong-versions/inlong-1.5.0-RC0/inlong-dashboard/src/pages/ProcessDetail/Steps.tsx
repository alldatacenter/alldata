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

import React, { useMemo } from 'react';
import { Steps, Tooltip } from 'antd';

const { Step } = Steps;

export interface StepsProps {
  data: {
    title: string;
    desc: string;
    status: string;
    tooltip?: string;
  }[];
}

const Comp: React.FC<StepsProps> = ({ data }) => {
  const canceledIndex = useMemo(() => {
    return data.findIndex(item => item.status === 'CANCELED');
  }, [data]);

  const rejectIndex = useMemo(() => {
    return data.findIndex(item => item.status === 'REJECTED');
  }, [data]);

  const current = useMemo(() => {
    if (canceledIndex !== -1) return canceledIndex;
    if (rejectIndex !== -1) return rejectIndex;
    const pendingIndex = data.findIndex(item => item.status === 'PENDING');
    return pendingIndex !== -1 ? pendingIndex : data.length;
  }, [data, canceledIndex, rejectIndex]);

  return (
    <Steps
      size="small"
      status={canceledIndex !== -1 || rejectIndex !== -1 ? 'error' : 'process'}
      current={current}
    >
      {data.map((item, index) => (
        <Step
          key={index}
          title={
            item.tooltip ? (
              <Tooltip placement="bottom" title={item.tooltip}>
                {item.title}
              </Tooltip>
            ) : (
              item.title
            )
          }
          description={item.desc}
        />
      ))}
    </Steps>
  );
};

export default Comp;
