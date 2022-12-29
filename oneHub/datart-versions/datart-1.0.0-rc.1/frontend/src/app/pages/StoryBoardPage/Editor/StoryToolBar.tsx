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
import { LeftOutlined, PlusOutlined } from '@ant-design/icons';
import { Button } from 'antd';
import { selectVizs } from 'app/pages/MainPage/pages/VizPage/slice/selectors';
import React, { memo, useCallback, useContext, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import styled from 'styled-components/macro';
import {
  FONT_SIZE_ICON_SM,
  FONT_WEIGHT_MEDIUM,
  LINE_HEIGHT_ICON_SM,
  SPACE_LG,
  SPACE_MD,
  SPACE_SM,
} from 'styles/StyleConstants';
import StoryPageAddModal from '../components/StoryPageAddModal';
import { StoryContext } from '../contexts/StoryContext';
import { addStoryPages } from '../slice/thunks';
import { StoryPageSetting } from './StoryPageSetting';
import { StorySetting } from './StorySetting';

export const StoryToolBar: React.FC<{ onCloseEditor?: () => void }> = memo(
  ({ onCloseEditor }) => {
    const dispatch = useDispatch();

    const closeEditor = useCallback(() => {
      onCloseEditor?.();
    }, [onCloseEditor]);
    const { storyId, name } = useContext(StoryContext);
    const [visible, setVisible] = useState(false);
    const chartOptionsMock = useSelector(selectVizs);
    const chartOptions = chartOptionsMock.filter(
      item => item.relType === 'DASHBOARD',
    );

    const onSelectedPages = useCallback(
      (relIds: string[]) => {
        dispatch(addStoryPages({ storyId, relIds }));
        setVisible(false);
      },
      [storyId, dispatch],
    );
    return (
      <Wrapper>
        <LeftOutlined
          onClick={closeEditor}
          style={{ marginRight: '10px', fontSize: '1.2rem' }}
        />
        <Title>{name}</Title>
        <AddButton
          type="primary"
          icon={<PlusOutlined />}
          onClick={() => setVisible(true)}
        />
        <Settings>
          <StoryPageSetting />
          <StorySetting />
        </Settings>
        <StoryPageAddModal
          pageContents={chartOptions}
          visible={visible}
          onSelectedPages={onSelectedPages}
          onCancel={() => setVisible(false)}
        />
      </Wrapper>
    );
  },
);

const Wrapper = styled.div`
  display: flex;
  align-items: center;
  padding: ${SPACE_SM} ${SPACE_LG};
  background-color: ${p => p.theme.componentBackground};
  box-shadow: ${p => p.theme.shadowSider};
`;

const Title = styled.h3`
  width: 170px;
  font-size: ${FONT_SIZE_ICON_SM};
  font-weight: ${FONT_WEIGHT_MEDIUM};
  line-height: ${LINE_HEIGHT_ICON_SM};
`;

const AddButton = styled(Button)`
  margin-left: ${SPACE_MD};
`;

const Settings = styled.div`
  display: flex;
  flex: 1;
  justify-content: center;
  padding: 0 ${SPACE_MD};
`;
