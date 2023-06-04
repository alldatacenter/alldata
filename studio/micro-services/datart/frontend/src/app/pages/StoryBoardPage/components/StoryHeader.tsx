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

import { MoreOutlined, SendOutlined } from '@ant-design/icons';
import { Button, Dropdown } from 'antd';
import { DetailPageHeader } from 'app/components/DetailPageHeader';
import { ShareManageModal } from 'app/components/VizOperationMenu';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { useStatusTitle } from 'app/pages/DashBoardPage/hooks/useStatusTitle';
import { generateShareLinkAsync } from 'app/utils/fetch';
import React, { FC, memo, useCallback, useContext, useState } from 'react';
import { StoryContext } from '../contexts/StoryContext';
import { StoryOverLay } from './StoryOverLay';

interface StoryHeaderProps {
  orgId: string;
  name: string;
  status: number;
  publishLoading?: boolean;
  onPublish: () => void;
  toggleEdit: () => void;
  playStory: () => void;
  allowShare?: boolean;
  allowManage?: boolean;
}
export const StoryHeader: FC<StoryHeaderProps> = memo(
  ({
    orgId,
    name,
    toggleEdit,
    status,
    publishLoading,
    playStory,
    onPublish,
    allowShare,
    allowManage,
  }) => {
    const t = useI18NPrefix(`viz.action`);
    const title = useStatusTitle(name, status);
    const isArchived = Number(status) === 0;
    const [showShareLinkModal, setShowShareLinkModal] = useState(false);
    const { storyId: stroyBoardId } = useContext(StoryContext);
    const onOpenShareLink = useCallback(() => {
      setShowShareLinkModal(true);
    }, []);

    const onGenerateShareLink = useCallback(
      async ({
        expiryDate,
        authenticationMode,
        roles,
        users,
        rowPermissionBy,
      }: {
        expiryDate: string;
        authenticationMode: string;
        roles: string[];
        users: string[];
        rowPermissionBy: string;
      }) => {
        const result: any = await generateShareLinkAsync({
          expiryDate,
          authenticationMode,
          roles,
          users,
          rowPermissionBy,
          vizId: stroyBoardId,
          vizType: 'STORYBOARD',
        });
        return result;
      },
      [stroyBoardId],
    );

    if (isArchived) {
      return <div></div>;
    }

    return (
      <DetailPageHeader
        title={title}
        disabled={Number(status) < 2}
        actions={
          <>
            {allowManage && Number(status) === 1 && (
              <Button
                key="publish"
                icon={<SendOutlined />}
                loading={publishLoading}
                onClick={onPublish}
              >
                {t('publish')}
              </Button>
            )}
            {allowManage && (
              <Button key="edit" onClick={toggleEdit}>
                {t('edit')}
              </Button>
            )}
            <Button key="run" onClick={playStory}>
              {t('play')}
            </Button>
            {(allowManage || allowShare) && (
              <Dropdown
                overlay={
                  <StoryOverLay
                    allowShare={allowShare}
                    allowManage={allowManage}
                    onOpenShareLink={onOpenShareLink}
                    onPublish={Number(status) === 2 ? onPublish : ''}
                  />
                }
                arrow
              >
                <Button icon={<MoreOutlined />} />
              </Dropdown>
            )}

            {allowShare && (
              <ShareManageModal
                vizId={stroyBoardId as string}
                orgId={orgId as string}
                vizType="STORYBOARD"
                visibility={showShareLinkModal}
                onOk={() => setShowShareLinkModal(false)}
                onCancel={() => setShowShareLinkModal(false)}
                onGenerateShareLink={onGenerateShareLink}
              />
            )}
          </>
        }
      />
    );
  },
);

export default StoryHeader;
