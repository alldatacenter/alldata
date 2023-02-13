import {
  FolderAddFilled,
  FundProjectionScreenOutlined,
  MenuUnfoldOutlined,
} from '@ant-design/icons';
import { ListSwitch } from 'app/components';
import useI18NPrefix, { I18NComponentProps } from 'app/hooks/useI18NPrefix';
import classnames from 'classnames';
import { memo, useCallback, useEffect, useMemo, useState } from 'react';
import { useSelector } from 'react-redux';
import { useRouteMatch } from 'react-router';
import styled from 'styled-components/macro';
import { LEVEL_5, SPACE_TIMES } from 'styles/StyleConstants';
import { selectStoryboards, selectVizs } from '../slice/selectors';
import { Folder } from '../slice/types';
import { Folders } from './Folders';
import { Storyboards } from './Storyboards';

interface SidebarProps extends I18NComponentProps {
  isDragging: boolean;
  width: number;
  sliderVisible: boolean;
  handleSliderVisible: (status: boolean) => void;
}

export const Sidebar = memo(
  ({
    width,
    isDragging,
    i18nPrefix,
    sliderVisible,
    handleSliderVisible,
  }: SidebarProps) => {
    const [selectedKey, setSelectedKey] = useState('folder');
    const vizs = useSelector(selectVizs);
    const storyboards = useSelector(selectStoryboards);

    const matchDetail = useRouteMatch<{ vizId: string }>(
      '/organizations/:orgId/vizs/:vizId',
    );
    const vizId = matchDetail?.params.vizId;
    const t = useI18NPrefix(i18nPrefix);
    const selectedFolderId = useMemo(() => {
      if (vizId && vizs) {
        const viz = vizs.find(({ relId }) => relId === vizId);
        return viz && viz.id;
      }
    }, [vizId, vizs]);

    useEffect(() => {
      if (vizId) {
        const viz =
          vizs.find(({ relId }) => relId === vizId) ||
          storyboards.find(({ id }) => id === vizId);
        if (viz) {
          setSelectedKey((viz as Folder).relType ? 'folder' : 'presentation');
        }
      }
    }, [vizId, storyboards, vizs]); // just switch when vizId changed

    const listTitles = useMemo(
      () => [
        { key: 'folder', icon: <FolderAddFilled />, text: t('folder') },
        {
          key: 'presentation',
          icon: <FundProjectionScreenOutlined />,
          text: t('presentation'),
        },
      ],
      [t],
    );

    const switchSelect = useCallback(key => {
      setSelectedKey(key);
    }, []);

    return (
      <Wrapper
        sliderVisible={sliderVisible}
        className={sliderVisible ? 'close' : ''}
        isDragging={isDragging}
        width={width}
      >
        {sliderVisible ? (
          <MenuUnfoldOutlined className="menuUnfoldOutlined" />
        ) : (
          ''
        )}
        <ListSwitch
          titles={listTitles}
          selectedKey={selectedKey}
          onSelect={switchSelect}
        />
        <Folders
          sliderVisible={sliderVisible}
          handleSliderVisible={handleSliderVisible}
          selectedId={selectedFolderId}
          i18nPrefix={i18nPrefix}
          className={classnames({ hidden: selectedKey !== 'folder' })}
        />
        <Storyboards
          sliderVisible={sliderVisible}
          handleSliderVisible={handleSliderVisible}
          selectedId={vizId}
          className={classnames({ hidden: selectedKey !== 'presentation' })}
          i18nPrefix={i18nPrefix}
        />
      </Wrapper>
    );
  },
);

const Wrapper = styled.div<{
  sliderVisible: boolean;
  isDragging: boolean;
  width: number;
}>`
  z-index: ${LEVEL_5};
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  min-height: 0;
  background-color: ${p => p.theme.componentBackground};
  box-shadow: ${p => p.theme.shadowSider};
  transition: ${p => (!p.isDragging ? 'width 0.3s ease' : 'none')};
  .hidden {
    display: none;
  }
  > ul {
    display: ${p => (p.sliderVisible ? 'none' : 'block')};
  }
  > div {
    display: ${p => (p.sliderVisible ? 'none' : 'flex')};
  }
  &.close {
    position: absolute;
    width: ${SPACE_TIMES(7.5)} !important;
    height: 100%;
    .menuUnfoldOutlined {
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
    }
    &:hover {
      width: ${p => p.width + '%'} !important;
      .menuUnfoldOutlined {
        display: none;
      }
      > ul {
        display: block;
      }
      > div {
        display: flex;
        &.hidden {
          display: none;
        }
      }
    }
  }
`;
