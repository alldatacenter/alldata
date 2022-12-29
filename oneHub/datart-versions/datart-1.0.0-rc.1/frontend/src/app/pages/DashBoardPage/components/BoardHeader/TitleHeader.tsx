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

import { EditOutlined, MoreOutlined, SendOutlined } from '@ant-design/icons';
import { Button, Dropdown, Space } from 'antd';
import { ShareManageModal } from 'app/components/VizOperationMenu';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { selectPublishLoading } from 'app/pages/MainPage/pages/VizPage/slice/selectors';
import classnames from 'classnames';
import { FC, memo, useCallback, useContext, useState } from 'react';
import { useSelector } from 'react-redux';
import { useHistory } from 'react-router-dom';
import styled from 'styled-components/macro';
import {
  FONT_SIZE_ICON_SM,
  FONT_WEIGHT_MEDIUM,
  LINE_HEIGHT_ICON_SM,
  SPACE_LG,
  SPACE_SM,
} from 'styles/StyleConstants';
import { usePublishBoard } from '../../hooks/usePublishBoard';
import { useStatusTitle } from '../../hooks/useStatusTitle';
import { BoardActionContext } from '../ActionProvider/BoardActionProvider';
import { BoardDropdownList } from '../BoardDropdownList/BoardDropdownList';
import { BoardContext } from '../BoardProvider/BoardProvider';
import { MockDataPanel } from '../MockDataPanel';
import SaveToStoryBoard from '../SaveToStoryBoard';

export const TitleHeader: FC = memo(() => {
  const t = useI18NPrefix(`viz.action`);
  const publishLoading = useSelector(selectPublishLoading);
  const history = useHistory();
  const [showShareLinkModal, setShowShareLinkModal] = useState(false);
  const [mockDataModal, setMockDataModal] = useState(false);
  const [showSaveToStory, setShowSaveToStory] = useState<boolean>(false);
  const { name, status, allowManage, allowShare, boardId, orgId } =
    useContext(BoardContext);
  const title = useStatusTitle(name, status);
  const { onGenerateShareLink } = useContext(BoardActionContext);
  const { publishBoard } = usePublishBoard(boardId, 'DASHBOARD', status);
  const onOpenShareLink = useCallback(() => {
    setShowShareLinkModal(true);
  }, []);
  const isArchived = Number(status) === 0;

  const toBoardEditor = () => {
    const pathName = history.location.pathname;
    if (pathName.includes(boardId)) {
      history.push(`${pathName.split(boardId)[0]}${boardId}/boardEditor`);
    } else if (pathName.includes('/vizs')) {
      history.push(
        `${pathName.split('/vizs')[0]}${'/vizs/'}${boardId}/boardEditor`,
      );
    }
  };

  const saveToStoryOk = useCallback(
    (storyId: string) => {
      history.push({
        pathname: `/organizations/${orgId}/vizs/storyEditor/${storyId}`,
        state: {
          addDashboardId: boardId,
        },
      });
      setShowSaveToStory(false);
    },
    [boardId, history, orgId],
  );

  return (
    <Wrapper>
      <h1 className={classnames({ disabled: status < 2 })}>{title}</h1>
      <Space>
        {!isArchived && allowManage && (
          <>
            {Number(status) === 1 && (
              <Button
                key="publish"
                icon={<SendOutlined />}
                loading={publishLoading}
                onClick={publishBoard}
              >
                {t('publish')}
              </Button>
            )}
            <Button key="edit" icon={<EditOutlined />} onClick={toBoardEditor}>
              {t('edit')}
            </Button>
          </>
        )}
        {!isArchived && (
          <Dropdown
            overlay={
              <BoardDropdownList
                onOpenShareLink={onOpenShareLink}
                openStoryList={() => setShowSaveToStory(true)}
                openMockData={() => setMockDataModal(true)}
              />
            }
            placement="bottomRight"
            arrow
          >
            <Button icon={<MoreOutlined />} />
          </Dropdown>
        )}
        {!isArchived && allowManage && (
          <SaveToStoryBoard
            title={t('addToStory')}
            orgId={orgId as string}
            isModalVisible={showSaveToStory}
            handleOk={saveToStoryOk}
            handleCancel={() => setShowSaveToStory(false)}
          ></SaveToStoryBoard>
        )}
        {!isArchived && allowShare && (
          <ShareManageModal
            vizId={boardId as string}
            orgId={orgId as string}
            vizType="DASHBOARD"
            visibility={showShareLinkModal}
            onOk={() => setShowShareLinkModal(false)}
            onCancel={() => setShowShareLinkModal(false)}
            onGenerateShareLink={onGenerateShareLink}
          />
        )}
        {!isArchived && mockDataModal && (
          <MockDataPanel onClose={() => setMockDataModal(false)} />
        )}
      </Space>
    </Wrapper>
  );
});

const Wrapper = styled.div`
  display: flex;
  flex-shrink: 0;
  align-items: center;
  padding: ${SPACE_SM} ${SPACE_LG};
  background-color: ${p => p.theme.componentBackground};
  box-shadow: ${p => p.theme.shadowSider};

  h1 {
    flex: 1;
    font-size: ${FONT_SIZE_ICON_SM};
    font-weight: ${FONT_WEIGHT_MEDIUM};
    line-height: ${LINE_HEIGHT_ICON_SM};

    &.disabled {
      color: ${p => p.theme.textColorLight};
    }
  }
`;
