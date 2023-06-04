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

import { Modal, Switch } from 'antd';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { useWorkbenchSlice } from 'app/pages/ChartWorkbenchPage/slice';
import { FC, memo, useMemo } from 'react';
import { useDispatch } from 'react-redux';
import styled from 'styled-components/macro';

const AggregationOperationMenu: FC<{
  defaultValue?: boolean;
  onChangeAggregation: () => void;
}> = memo(({ defaultValue = true, onChangeAggregation }) => {
  const checkedValue = useMemo(() => defaultValue, [defaultValue]);
  const t = useI18NPrefix(`viz.workbench.header`);
  const { actions } = useWorkbenchSlice();
  const dispatch = useDispatch();

  const onChange = value => {
    Modal.confirm({
      icon: <></>,
      content: t('aggregationSwitchTip'),
      okText: checkedValue ? t('close') : t('open'),
      onOk() {
        onChangeAggregation();
        dispatch(actions.updateChartAggregation(value));
      },
    });
  };

  return (
    <Aggregation>
      {t('aggregationSwitch') + ' '}
      <Switch checked={checkedValue} size="small" onChange={onChange} />
    </Aggregation>
  );
});

export default AggregationOperationMenu;

const Aggregation = styled.div`
  color: ${p => p.theme.textColor};
`;
