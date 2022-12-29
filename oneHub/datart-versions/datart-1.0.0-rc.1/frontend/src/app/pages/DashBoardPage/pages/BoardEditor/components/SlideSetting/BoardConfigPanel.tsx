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

import { Collapse } from 'antd';
import { CollapseHeader } from 'app/components/FormGenerator';
import { FormGroupLayoutMode } from 'app/components/FormGenerator/constants';
import GroupLayout from 'app/components/FormGenerator/Layout/GroupLayout';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import ChartI18NContext from 'app/pages/ChartWorkbenchPage/contexts/Chart18NContext';
import { BoardConfigContext } from 'app/pages/DashBoardPage/components/BoardProvider/BoardConfigProvider';
import { ChartStyleConfig } from 'app/types/ChartConfig';
import { FC, memo, useContext } from 'react';
import { useDispatch } from 'react-redux';
import styled from 'styled-components/macro';
import { editBoardStackActions } from '../../slice';

const StyledWrapper = styled.div`
  width: 100%;
  min-height: 0;
  overflow-y: auto;
`;
export const BoardConfigPanel: FC<{}> = memo(() => {
  const dispatch = useDispatch();
  const t = useI18NPrefix(`viz.board.setting`);
  const boardConfig = useContext(BoardConfigContext);
  const configs = boardConfig.jsonConfig.props;
  const i18ns = boardConfig.jsonConfig.i18ns;
  const onChange = (
    ancestors: number[],
    configItem: ChartStyleConfig,
    needRefresh?: boolean,
  ) => {
    dispatch(
      editBoardStackActions.updateBoardConfigByKey({ ancestors, configItem }),
    );
  };
  return (
    <ChartI18NContext.Provider value={{ i18NConfigs: i18ns }}>
      <h3 style={{ textAlign: 'center' }}>{t('board')}</h3>
      <StyledWrapper onClick={e => e.stopPropagation()}>
        <BoardConfigCollapse configs={configs} onChange={onChange} />
      </StyledWrapper>
    </ChartI18NContext.Provider>
  );
});

export const BoardConfigCollapse: FC<{
  configs: ChartStyleConfig[];
  onChange: (
    ancestors: number[],
    config: ChartStyleConfig,
    needRefresh?: boolean,
  ) => void;
}> = memo(({ configs, onChange }) => {
  const t = useI18NPrefix();
  return (
    <Collapse className="" ghost>
      {configs
        ?.filter(c => !Boolean(c.hidden))
        .map((c, index) => (
          <Collapse.Panel
            header={<CollapseHeader title={t(c.label, true)} />}
            key={c.key}
          >
            <GroupLayout
              ancestors={[index]}
              mode={
                c.comType === 'group'
                  ? FormGroupLayoutMode.INNER
                  : FormGroupLayoutMode.OUTER
              }
              data={c}
              translate={t}
              dataConfigs={[]}
              onChange={onChange}
            />
          </Collapse.Panel>
        ))}
    </Collapse>
  );
});
