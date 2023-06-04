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
import { Button, Empty } from 'antd';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { BoardContext } from 'app/pages/DashBoardPage/components/BoardProvider/BoardProvider';
import { FC, memo, useContext, useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';
import styled from 'styled-components/macro';
import { LEVEL_100 } from 'styles/StyleConstants';
import {
  getBoardStateAction,
  getWidgetChartDatasAction,
} from '../../pages/Board/slice/asyncActions';
import { exportBoardTpl } from '../../pages/Board/slice/thunk';
import { MockDataTab } from './MockDataTab';
import { getBoardTplData } from './utils';

export interface MockDataPanelProps {
  onClose: () => void;
}
export const MockDataPanel: FC<MockDataPanelProps> = memo(({ onClose }) => {
  const { boardId } = useContext(BoardContext);
  const dispatch = useDispatch();
  const t = useI18NPrefix('global.button');
  const [dataMap, setDataMap] = useState<any>();
  useEffect(() => {
    const dataMap = dispatch(getWidgetChartDatasAction(boardId));
    setDataMap(dataMap);
  }, [boardId, dispatch]);

  const onExport = async () => {
    const boardState = dispatch(getBoardStateAction(boardId));
    const data = getBoardTplData(dataMap, boardState as any);
    dispatch(exportBoardTpl({ ...data, callBack: onClose }));
  };
  const onChangeDataMap = opt => {
    const newItem = {
      ...dataMap[opt.id],
      data: { ...dataMap[opt.id].data, rows: opt.val },
    };
    const newData = Object.assign(dataMap, { [opt.id]: newItem });
    setDataMap(newData);
  };
  return (
    <StyledWrapper className="mockDataPanel">
      <div className="content">
        {Object.values(dataMap || {}).length ? (
          <MockDataTab dataMap={dataMap} onChangeDataMap={onChangeDataMap} />
        ) : (
          <div className="empty-data" style={{ flex: 1 }}>
            <Empty />
          </div>
        )}

        <div className="btn-box">
          <Button className="btn" type="primary" onClick={onExport}>
            {t('ok')}
          </Button>

          <Button className="btn" onClick={onClose}>
            {t('cancel')}
          </Button>
        </div>
      </div>
    </StyledWrapper>
  );
});
const StyledWrapper = styled.div`
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: ${LEVEL_100};
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  padding: 50px;
  background-color: rgb(0 0 0 / 50%);
  & .content {
    display: flex;
    flex: 1;
    flex-direction: column;
    background-color: ${p => p.theme.bodyBackground};
    .empty-data {
      display: flex;
      flex: 1;
      align-content: center;
      align-items: center;
      justify-content: center;
    }
  }
  .btn-box {
    display: flex;
    flex-direction: row-reverse;
    align-items: center;
    height: 50px;
    .btn {
      margin-right: 20px;
    }
  }
  .tab-box {
    display: flex;
    flex: 1;
  }
`;
