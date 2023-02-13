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
  CaretRightFilled,
  EditOutlined,
  MoreOutlined,
  SendOutlined,
} from '@ant-design/icons';
import { Button, Dropdown } from 'antd';
import SaveToDashboard from 'app/components/SaveToDashboard';
import {
  ShareManageModal,
  VizOperationMenu,
} from 'app/components/VizOperationMenu';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { TITLE_SUFFIX } from 'globalConstants';
import { FC, memo, useCallback, useMemo, useState } from 'react';
import styled from 'styled-components/macro';
import { ChartMockDataPanel } from '../ChartMockDataPanel';
import { DetailPageHeader } from '../DetailPageHeader';

const VizHeader: FC<{
  chartName?: string;
  status?: number;
  publishLoading?: boolean;
  chartDescription?: string;
  backendChartId?: string;
  onRun?;
  onGotoEdit?;
  onPublish?: () => void;
  onGenerateShareLink?: ({
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
  }) => any;
  onDownloadData?;
  onSaveAsVizs?;
  onReloadData?;
  onAddToDashBoard?;
  onRecycleViz?;
  allowDownload?: boolean;
  allowShare?: boolean;
  allowManage?: boolean;
  orgId?: string;
}> = memo(
  ({
    chartName,
    status,
    publishLoading,
    chartDescription,
    onRun,
    onPublish,
    onGotoEdit,
    onGenerateShareLink,
    onDownloadData,
    onSaveAsVizs,
    onReloadData,
    onAddToDashBoard,
    onRecycleViz,
    allowDownload,
    allowShare,
    allowManage,
    orgId,
    backendChartId,
  }) => {
    const t = useI18NPrefix(`viz.action`);
    const [showShareLinkModal, setShowShareLinkModal] = useState(false);
    const [isModalVisible, setIsModalVisible] = useState<boolean>(false);
    const [mockDataModal, setMockDataModal] = useState(false);
    const isArchived = Number(status) === 0;

    const handleCloseShareLinkModal = useCallback(() => {
      setShowShareLinkModal(false);
    }, []);

    const handleOpenShareLinkModal = useCallback(() => {
      setShowShareLinkModal(true);
    }, []);

    const handleModalOk = useCallback(
      (dashboardId: string, dashboardType: string) => {
        setIsModalVisible(false);
        onAddToDashBoard?.(dashboardId, dashboardType);
      },
      [onAddToDashBoard],
    );

    const handleModalCancel = useCallback(() => {
      setIsModalVisible(false);
    }, []);

    const handleModalOpen = useCallback(() => {
      setIsModalVisible(true);
    }, []);

    const getOverlays = () => {
      return (
        <VizOperationMenu
          onShareLinkClick={onGenerateShareLink && handleOpenShareLinkModal}
          onDownloadDataLinkClick={onDownloadData}
          onSaveAsVizs={onSaveAsVizs}
          onReloadData={onReloadData}
          onAddToDashBoard={onAddToDashBoard && setIsModalVisible}
          allowDownload={allowDownload}
          allowShare={allowShare}
          allowManage={allowManage}
          openMockData={() => setMockDataModal(true)}
          onPublish={Number(status) === 2 ? onPublish : ''}
          onRecycleViz={onRecycleViz}
        />
      );
    };

    const title = useMemo(() => {
      const base = chartName || '';
      const suffix = TITLE_SUFFIX[Number(status)]
        ? `[${t(TITLE_SUFFIX[Number(status)])}]`
        : '';
      return base + suffix;
    }, [chartName, status, t]);

    return (
      <Wrapper>
        <DetailPageHeader
          title={title}
          description={chartDescription}
          disabled={Number(status) < 2}
          actions={
            <>
              {onRun && (
                <Button key="run" icon={<CaretRightFilled />} onClick={onRun}>
                  {t('run')}
                </Button>
              )}
              {allowManage && !isArchived && onPublish && Number(status) === 1 && (
                <Button
                  key="publish"
                  icon={<SendOutlined />}
                  loading={publishLoading}
                  onClick={onPublish}
                >
                  {t('publish')}
                </Button>
              )}
              {allowManage && !isArchived && onGotoEdit && (
                <Button key="edit" icon={<EditOutlined />} onClick={onGotoEdit}>
                  {t('edit')}
                </Button>
              )}
              {!isArchived && (
                <Dropdown key="more" arrow overlay={getOverlays()}>
                  <Button icon={<MoreOutlined />} />
                </Dropdown>
              )}
            </>
          }
        />
        {allowShare && (
          <ShareManageModal
            vizId={backendChartId as string}
            orgId={orgId as string}
            vizType="DATACHART"
            visibility={showShareLinkModal}
            onOk={handleCloseShareLinkModal}
            onCancel={handleCloseShareLinkModal}
            onGenerateShareLink={onGenerateShareLink}
          />
        )}
        {onSaveAsVizs && (
          <SaveToDashboard
            backendChartId={backendChartId}
            title={t('addToDash')}
            orgId={orgId as string}
            isModalVisible={isModalVisible}
            handleOk={handleModalOk}
            handleCancel={handleModalCancel}
            handleOpen={handleModalOpen}
          ></SaveToDashboard>
        )}
        {mockDataModal && (
          <ChartMockDataPanel
            chartId={backendChartId || ''}
            onClose={() => setMockDataModal(false)}
          />
        )}
      </Wrapper>
    );
  },
);

export default VizHeader;

const Wrapper = styled.div`
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
`;
