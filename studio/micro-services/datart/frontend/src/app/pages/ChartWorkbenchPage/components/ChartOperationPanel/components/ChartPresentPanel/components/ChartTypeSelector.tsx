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

import {
  AreaChartOutlined,
  CloudDownloadOutlined,
  ConsoleSqlOutlined,
  TableOutlined,
} from '@ant-design/icons';
import { Popconfirm } from 'antd';
import { IW } from 'app/components';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import classnames from 'classnames';
import { FC, memo, useCallback } from 'react';
import styled from 'styled-components/macro';
import { FONT_SIZE_HEADING } from 'styles/StyleConstants';

export enum ChartPresentType {
  GRAPH = 'graph',
  RAW = 'raw',
  SQL = 'sql',
  DOWNLOAD = 'download',
}

const ChartTypeSelector: FC<{
  type;
  translate: (title: string) => string;
  onChange: (value) => void;
  onCreateDownloadDataTask?: () => void;
}> = memo(({ type, onChange, onCreateDownloadDataTask }) => {
  const t = useI18NPrefix(`viz.action.common`);
  const typeChange = useCallback(
    type => () => {
      onChange(type);
    },
    [onChange],
  );

  return (
    <StyledChartTypeSelector>
      <TypeSelector
        fontSize={FONT_SIZE_HEADING}
        className={classnames({ active: type === ChartPresentType.GRAPH })}
        onClick={typeChange(ChartPresentType.GRAPH)}
      >
        <AreaChartOutlined />
      </TypeSelector>
      <TypeSelector
        fontSize={FONT_SIZE_HEADING}
        className={classnames({ active: type === ChartPresentType.RAW })}
        onClick={typeChange(ChartPresentType.RAW)}
      >
        <TableOutlined />
      </TypeSelector>
      <TypeSelector
        fontSize={FONT_SIZE_HEADING}
        className={classnames({ active: type === ChartPresentType.SQL })}
        onClick={typeChange(ChartPresentType.SQL)}
      >
        <ConsoleSqlOutlined />
      </TypeSelector>
      <TypeSelector
        fontSize={FONT_SIZE_HEADING}
        className={classnames({ active: type === ChartPresentType.DOWNLOAD })}
      >
        <Popconfirm
          placement="left"
          title={t('exportForExcel')}
          onConfirm={onCreateDownloadDataTask}
        >
          <CloudDownloadOutlined />
        </Popconfirm>
      </TypeSelector>
    </StyledChartTypeSelector>
  );
});

export default ChartTypeSelector;

const StyledChartTypeSelector = styled.div`
  display: flex;
  justify-content: flex-end;
  color: ${p => p.theme.textColorLight};
`;

const TypeSelector = styled(IW)`
  cursor: pointer;

  &.active {
    color: ${p => p.theme.primary};
  }
`;
