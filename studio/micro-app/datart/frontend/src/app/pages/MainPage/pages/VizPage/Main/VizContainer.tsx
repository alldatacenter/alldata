import Board from 'app/pages/DashBoardPage/pages/Board';
import { useCascadeAccess } from 'app/pages/MainPage/Access';
import { StoryPagePreview } from 'app/pages/StoryBoardPage/Preview/Preview';
import classnames from 'classnames';
import { memo, useMemo } from 'react';
import styled from 'styled-components/macro';
import { getPath } from 'utils/utils';
import {
  PermissionLevels,
  ResourceTypes,
  VizResourceSubTypes,
} from '../../PermissionPage/constants';
import { ChartPreviewBoard } from '../ChartPreview';
import { FolderViewModel, VizTab } from '../slice/types';

interface VizContainerProps {
  tab: VizTab;
  orgId: string;
  vizs: FolderViewModel[];
  selectedId?: string;
  hideTitle?: boolean;
}

export const VizContainer = memo(
  ({ tab, orgId, vizs, selectedId, hideTitle }: VizContainerProps) => {
    const { id, name, type, search, parentId, permissionId } = tab;
    const path = useMemo(
      () =>
        ['DATACHART', 'DASHBOARD'].includes(type)
          ? getPath(
              vizs as Array<{ id: string; parentId: string }>,
              { id: permissionId!, parentId },
              VizResourceSubTypes.Folder,
            )
          : [id],
      [vizs, id, permissionId, type, parentId],
    );
    const allowDownload = useCascadeAccess({
      module: ResourceTypes.Viz,
      path,
      level: PermissionLevels.Download,
    })(true);
    const allowShare = useCascadeAccess({
      module: ResourceTypes.Viz,
      path,
      level: PermissionLevels.Share,
    })(true);
    const allowManage = useCascadeAccess({
      module: ResourceTypes.Viz,
      path,
      level: PermissionLevels.Manage,
    })(true);

    let content;

    switch (type) {
      case 'DASHBOARD':
        content = (
          <Board
            key={id}
            id={id}
            autoFit={true}
            filterSearchUrl={search}
            allowDownload={allowDownload}
            allowShare={allowShare}
            showZoomCtrl={true}
            allowManage={allowManage}
            renderMode="read"
            hideTitle={hideTitle}
          />
        );
        break;
      case 'DATACHART':
        content = (
          <ChartPreviewBoard
            key={id}
            backendChartId={id}
            orgId={orgId}
            filterSearchUrl={search}
            allowDownload={allowDownload}
            allowShare={allowShare}
            allowManage={allowManage}
            hideTitle={hideTitle}
          />
        );
        break;
      case 'STORYBOARD':
        content = (
          <StoryPagePreview
            orgId={orgId}
            storyId={id}
            allowShare={allowShare}
            allowManage={allowManage}
          />
        );
        break;
      default:
        content = (
          <Dumb>
            <h2 key={name}>{name}</h2>
          </Dumb>
        );
        break;
    }
    return (
      <Container
        key={id}
        className={classnames({ selected: id === selectedId })}
      >
        {content}
      </Container>
    );
  },
);

const Container = styled.div`
  display: none;
  flex: 1;
  flex-direction: column;
  height: 100%;

  &.selected {
    display: flex;
    min-width: 0;
    min-height: 0;
  }
`;

const Dumb = styled.div`
  height: 100%;
`;
