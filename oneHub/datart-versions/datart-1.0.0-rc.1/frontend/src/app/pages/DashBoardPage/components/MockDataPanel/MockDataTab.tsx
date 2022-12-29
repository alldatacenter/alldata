/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { Tabs } from 'antd';
import { FC, memo, useEffect, useState } from 'react';
import styled from 'styled-components/macro';
import { MockDataEditor } from './MockDataEditor';
const { TabPane } = Tabs;
export interface MockDataPanelProps {
  onClose: () => void;
}

export const MockDataTab: FC<{
  dataMap: Record<string, { id: string; name: string; data }>;
  onChangeDataMap: (val) => void;
}> = memo(({ dataMap, onChangeDataMap }) => {
  const dataList = Object.values(dataMap || {});
  const [wId, setWid] = useState<string>();
  const [curDataVal, setCurDataVal] = useState<any>();
  useEffect(() => {
    const dataList = Object.values(dataMap || {});
    if (dataList && dataList[0]) {
      setWid(dataList[0].id);
    }
  }, [dataMap]);

  const onChange = (key: string) => {
    setWid(key);
  };
  useEffect(() => {
    const widgetData = dataMap?.[wId || ''];
    if (widgetData) {
      const dataVal = widgetData.data?.rows;
      setCurDataVal(dataVal);
    }
  }, [dataMap, wId]);
  const onDataChange = strVal => {
    onChangeDataMap({
      id: wId,
      val: strVal,
    });
  };
  if (!wId) {
    return null;
  }
  return (
    <StyledWrapper className="tab">
      <Tabs centered onChange={onChange}>
        {dataList.map(t => {
          return <TabPane tab={t.name} key={t.id}></TabPane>;
        })}
      </Tabs>
      <MockDataEditor originalData={curDataVal} onDataChange={onDataChange} />
    </StyledWrapper>
  );
});
const StyledWrapper = styled.div`
  display: flex;
  flex: 1;
  flex-direction: column;
`;
