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
  DeleteOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
} from '@ant-design/icons';
import { message } from 'antd';
import { ListNav, ListPane, ListTitle } from 'app/components';
import { useDebouncedSearch } from 'app/hooks/useDebouncedSearch';
import useGetSourceDbTypeIcon from 'app/hooks/useGetSourceDbTypeIcon';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { selectOrgId } from 'app/pages/MainPage/slice/selectors';
import { dispatchResize } from 'app/utils/dispatchResize';
import { CommonFormTypes } from 'globalConstants';
import { memo, useCallback, useContext, useMemo } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useRouteMatch } from 'react-router';
import { useHistory } from 'react-router-dom';
import styled from 'styled-components/macro';
import { LEVEL_5, SPACE_TIMES, SPACE_XS } from 'styles/StyleConstants';
import { getInsertedNodeIndex } from 'utils/utils';
import { SaveFormContext } from '../SaveFormContext';
import {
  makeSelectSourceTree,
  selectArchived,
  selectSources,
} from '../slice/selectors';
import { addSource } from '../slice/thunks';
import { SourceSimpleViewModel } from '../slice/types';
import { Recycle } from './Recycle';
import { SourceList } from './SourceList';

interface SidebarProps {
  isDragging: boolean;
  width: number;
  sliderVisible: boolean;
  handleSliderVisible: (status: boolean) => void;
}

export const Sidebar = memo(
  ({ width, isDragging, sliderVisible, handleSliderVisible }: SidebarProps) => {
    const dispatch = useDispatch();
    const history = useHistory();
    const orgId = useSelector(selectOrgId);
    const archived = useSelector(selectArchived);
    const sourceData = useSelector(selectSources);
    const matchSourceDetail = useRouteMatch<{ sourceId: string }>(
      '/organizations/:orgId/sources/:sourceId',
    );
    const sourceId = matchSourceDetail?.params.sourceId;
    const t = useI18NPrefix('source');
    const selectSourceTree = useMemo(makeSelectSourceTree, []);
    const { showSaveForm } = useContext(SaveFormContext);

    const getIcon = useGetSourceDbTypeIcon();
    const getDisabled = useCallback(
      ({ deleteLoading }: SourceSimpleViewModel) => deleteLoading,
      [],
    );

    const treeData = useSelector(state =>
      selectSourceTree(state, { getIcon, getDisabled }),
    );
    const recycleList = useMemo(
      () =>
        archived?.map(config => {
          const { id, name, parentId, isFolder, deleteLoading } = config;
          return {
            id,
            key: id,
            title: name,
            icon: getIcon(config),
            parentId,
            isFolder,
            disabled: deleteLoading,
          };
        }),
      [archived, getIcon],
    );

    const { filteredData: sourceList, debouncedSearch: listSearch } =
      useDebouncedSearch(treeData, (keywords, d) =>
        d.title.toLowerCase().includes(keywords.toLowerCase()),
      );
    const { filteredData: archivedList, debouncedSearch: archivedSearch } =
      useDebouncedSearch(recycleList, (keywords, d) =>
        d.title.toLowerCase().includes(keywords.toLowerCase()),
      );
    const toAdd = useCallback(
      ({ key }) => {
        switch (key) {
          case 'add':
            history.push(`/organizations/${orgId}/sources/add`);
            break;
          case 'folder':
            showSaveForm({
              sourceType: 'folder',
              type: CommonFormTypes.Add,
              visible: true,
              simple: false,
              parentIdLabel: t('sidebar.parent'),
              onSave: (values, onClose) => {
                let index = getInsertedNodeIndex(values, sourceData);
                dispatch(
                  addSource({
                    source: {
                      ...values,
                      config: JSON.stringify(values.config),
                      parentId: values.parentId || null,
                      index,
                      orgId,
                      isFolder: true,
                    },
                    resolve: () => {
                      onClose();
                      message.success(t('sidebar.addSuccess'));
                    },
                  }),
                );
              },
            });
            break;
          default:
            break;
        }
      },
      [dispatch, history, orgId, showSaveForm, sourceData, t],
    );

    const moreMenuClick = useCallback(
      (key, _, onNext) => {
        switch (key) {
          case 'recycle':
            onNext();
            break;
          case 'collapse':
            handleSliderVisible(!sliderVisible);
            dispatchResize();
            break;
        }
      },
      [handleSliderVisible, sliderVisible],
    );

    const titles = useMemo(
      () => [
        {
          key: 'list',
          title: t('sidebar.title'),
          search: true,
          onSearch: listSearch,
          add: {
            items: [
              { key: 'add', text: t('sidebar.add') },
              { key: 'folder', text: t('sidebar.addFolder') },
            ],
            callback: toAdd,
          },
          more: {
            items: [
              {
                key: 'recycle',
                text: t('sidebar.recycle'),
                prefix: <DeleteOutlined className="icon" />,
              },
              {
                key: 'collapse',
                text: t(sliderVisible ? 'sidebar.open' : 'sidebar.close'),
                prefix: sliderVisible ? (
                  <MenuUnfoldOutlined className="icon" />
                ) : (
                  <MenuFoldOutlined className="icon" />
                ),
              },
            ],
            callback: moreMenuClick,
          },
        },
        {
          key: 'recycle',
          title: t('sidebar.recycle'),
          back: true,
          search: true,
          onSearch: archivedSearch,
        },
      ],
      [t, listSearch, toAdd, sliderVisible, moreMenuClick, archivedSearch],
    );

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
        <ListNavWrapper defaultActiveKey="list">
          <ListPane key="list">
            <ListTitle {...titles[0]} />
            <SourceList sourceId={sourceId} list={sourceList} />
          </ListPane>
          <ListPane key="recycle">
            <ListTitle {...titles[1]} />
            <Recycle sourceId={sourceId} list={archivedList} />
          </ListPane>
        </ListNavWrapper>
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

const ListNavWrapper = styled(ListNav)`
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  padding: ${SPACE_XS} 0;
  background-color: ${p => p.theme.componentBackground};
`;
