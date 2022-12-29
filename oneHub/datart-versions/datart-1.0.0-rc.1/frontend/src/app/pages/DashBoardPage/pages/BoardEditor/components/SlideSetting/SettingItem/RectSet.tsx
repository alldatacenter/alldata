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
import { Collapse, InputNumber } from 'antd';
import { BW } from 'app/components/FormGenerator/Basic/components/BasicWrapper';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { WidgetActionContext } from 'app/pages/DashBoardPage/components/ActionProvider/WidgetActionProvider';
import { RectConfig } from 'app/pages/DashBoardPage/pages/Board/slice/types';
import debounce from 'lodash/debounce';
import {
  FC,
  memo,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react';
import styled from 'styled-components/macro';
import { Group } from '../SettingPanel';
const { Panel } = Collapse;
export const RectSet: FC<{ wid: string; rect: RectConfig }> = memo(
  ({ wid, rect }) => {
    const { onEditFreeWidgetRect } = useContext(WidgetActionContext);
    const t = useI18NPrefix(`viz.board.setting`);
    const [rectVal, setRectVal] = useState<RectConfig>({} as RectConfig);
    useEffect(() => {
      setRectVal(rect);
    }, [rect]);

    const debounceSetVal = useMemo(
      () =>
        debounce(value => {
          onEditFreeWidgetRect(value, wid, false);
        }, 300),
      [onEditFreeWidgetRect, wid],
    );

    const changeRect = useCallback(
      (key: string, value) => {
        const newRect = { ...rect!, [key]: value };
        setRectVal(newRect);
        debounceSetVal(newRect);
      },
      [debounceSetVal, rect],
    );
    const changeX = useCallback(val => changeRect('x', val), [changeRect]);
    const changeY = useCallback(val => changeRect('y', val), [changeRect]);
    const changeW = useCallback(val => changeRect('width', val), [changeRect]);
    const changeH = useCallback(val => changeRect('height', val), [changeRect]);
    return (
      <Collapse className="datart-config-panel" ghost>
        <Panel
          header={t('position') + '&' + t('size')}
          key="position"
          forceRender
        >
          <Group>
            <BW label={t('position')}>
              <StyledFlex>
                <StyledPadding>
                  <label>X</label>
                  <InputNumber
                    value={rectVal.x?.toFixed(1)}
                    className="datart-ant-input-number"
                    onChange={changeX}
                  />
                </StyledPadding>

                <StyledPadding>
                  <label>Y</label>
                  <InputNumber
                    value={rectVal.y?.toFixed(1)}
                    className="datart-ant-input-number"
                    onChange={changeY}
                  />
                </StyledPadding>
              </StyledFlex>
            </BW>
          </Group>

          <Group>
            <BW label={t('size')}>
              <StyledFlex>
                <StyledPadding>
                  <label>W</label>
                  <InputNumber
                    value={rectVal.width?.toFixed(1)}
                    className="datart-ant-input-number"
                    onChange={changeW}
                  />
                </StyledPadding>
                <StyledFlex>
                  <StyledPadding>
                    <label>H</label>
                    <InputNumber
                      value={rectVal?.height?.toFixed(1)}
                      className="datart-ant-input-number"
                      onChange={changeH}
                    />
                  </StyledPadding>
                </StyledFlex>
              </StyledFlex>
            </BW>
          </Group>
        </Panel>
      </Collapse>
    );
  },
);
const StyledFlex = styled.div`
  display: flex;
`;
const StyledPadding = styled.div`
  display: flex;
  min-height: 0;
  padding: 0 4px;
  margin: 0 4px;
  overflow-y: auto;
  line-height: 30px;
  background-color: ${p => p.theme.bodyBackground};
`;
