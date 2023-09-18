import {
  DeleteOutlined,
  FolderFilled,
  FolderOpenFilled,
  FundProjectionScreenOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  PlusOutlined,
} from '@ant-design/icons';
import { ListNav, ListPane, ListTitle } from 'app/components';
import { useDebouncedSearch } from 'app/hooks/useDebouncedSearch';
import useI18NPrefix, { I18NComponentProps } from 'app/hooks/useI18NPrefix';
import { selectOrgId } from 'app/pages/MainPage/slice/selectors';
import { CommonFormTypes } from 'globalConstants';
import React, { memo, useCallback, useContext, useMemo } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import styled from 'styled-components/macro';
import { SPACE_XS } from 'styles/StyleConstants';
import { getInsertedNodeIndex } from 'utils/utils';
import { VizResourceSubTypes } from '../../../PermissionPage/constants';
import { SaveFormContext } from '../../SaveFormContext';
import {
  makeSelectArchivedStoryboradsTree,
  makeSelectStoryboradTree,
  selectArchivedStoryboardLoading,
  selectStoryboards,
} from '../../slice/selectors';
import { addStoryboard, getArchivedStoryboards } from '../../slice/thunks';
import { ArchivedViz, StoryboardViewModel } from '../../slice/types';
import { Recycle } from '../Recycle';
import { List } from './List';

interface FoldersProps extends I18NComponentProps {
  sliderVisible: boolean;
  handleSliderVisible: (status: boolean) => void;
  selectedId?: string;
  className?: string;
}

export const Storyboards = memo(
  ({
    selectedId,
    className,
    i18nPrefix,
    sliderVisible,
    handleSliderVisible,
  }: FoldersProps) => {
    const dispatch = useDispatch();
    const orgId = useSelector(selectOrgId);
    const { showSaveForm } = useContext(SaveFormContext);
    const storyborads = useSelector(selectStoryboards);
    const selectStoryboradTree = useMemo(makeSelectStoryboradTree, []);
    const t = useI18NPrefix(i18nPrefix);

    const getIcon = useCallback(
      ({ isFolder }: StoryboardViewModel) =>
        isFolder ? (
          p => (p.expanded ? <FolderOpenFilled /> : <FolderFilled />)
        ) : (
          <FundProjectionScreenOutlined />
        ),
      [],
    );

    const getDisabled = useCallback(
      ({ deleteLoading }: StoryboardViewModel) => deleteLoading,
      [],
    );

    const treeData = useSelector(state =>
      selectStoryboradTree(state, { getIcon, getDisabled }),
    );

    const { filteredData: filteredListData, debouncedSearch: listSearch } =
      useDebouncedSearch(treeData, (keywords, d) =>
        d.title.toLowerCase().includes(keywords.toLowerCase()),
      );

    const selectArchivedStoryboradsTree = useMemo(
      makeSelectArchivedStoryboradsTree,
      [],
    );
    const archivedListLoading = useSelector(selectArchivedStoryboardLoading);
    const getArchivedDisabled = useCallback(
      ({ deleteLoading }: ArchivedViz) => deleteLoading,
      [],
    );
    const archivedTreeData = useSelector(state =>
      selectArchivedStoryboradsTree(state, {
        getDisabled: getArchivedDisabled,
      }),
    );
    const {
      filteredData: filteredRecycleData,
      debouncedSearch: recycleSearch,
    } = useDebouncedSearch(archivedTreeData, (keywords, d) =>
      d.title.toLowerCase().includes(keywords.toLowerCase()),
    );
    const recycleInit = useCallback(() => {
      dispatch(getArchivedStoryboards(orgId));
    }, [dispatch, orgId]);

    const add = useCallback(
      ({ key }) => {
        switch (key) {
          case 'add':
            showSaveForm({
              vizType: VizResourceSubTypes.Storyboard,
              type: CommonFormTypes.Add,
              visible: true,
              onSave: (values, onClose) => {
                const index = getInsertedNodeIndex(values, storyborads);
                dispatch(
                  addStoryboard({
                    storyboard: {
                      ...values,
                      parentId: values.parentId || null,
                      orgId,
                      isFolder: false,
                      index,
                    },
                    resolve: onClose,
                  }),
                );
              },
            });
            break;
          case 'folder':
            showSaveForm({
              vizType: VizResourceSubTypes.Storyboard,
              type: CommonFormTypes.Add,
              visible: true,
              onSave: (values, onClose) => {
                const index = getInsertedNodeIndex(values, storyborads);
                dispatch(
                  addStoryboard({
                    storyboard: {
                      ...values,
                      parentId: values.parentId || null,
                      orgId,
                      isFolder: true,
                      index,
                    },
                    resolve: onClose,
                  }),
                );
              },
            });
            break;
          default:
        }
      },
      [showSaveForm, storyborads, dispatch, orgId],
    );

    const titles = useMemo(
      () => [
        {
          subTitle: t('storyboards.title'),
          search: true,
          add: {
            items: [
              { key: 'add', text: t('storyboards.add') },
              { key: 'folder', text: t('storyboards.addFolder') },
            ],
            icon: <PlusOutlined />,
            callback: add,
          },
          more: {
            items: [
              {
                key: 'recycle',
                text: t('storyboards.recycle'),
                prefix: <DeleteOutlined className="icon" />,
              },
              {
                key: 'collapse',
                text: t(sliderVisible ? 'folders.open' : 'folders.close'),
                prefix: sliderVisible ? (
                  <MenuUnfoldOutlined className="icon" />
                ) : (
                  <MenuFoldOutlined className="icon" />
                ),
              },
            ],
            callback: (key, _, onNext) => {
              switch (key) {
                case 'recycle':
                  onNext();
                  break;
                case 'collapse':
                  handleSliderVisible(!sliderVisible);
                  break;
              }
            },
          },
          onSearch: listSearch,
        },
        {
          key: 'recycle',
          subTitle: t('storyboards.recycle'),
          back: true,
          search: true,
          onSearch: recycleSearch,
        },
      ],
      [add, listSearch, recycleSearch, t, handleSliderVisible, sliderVisible],
    );

    return (
      <Wrapper className={className} defaultActiveKey="list">
        <ListPane key="list">
          <ListTitle {...titles[0]} />
          <List list={filteredListData} selectedId={selectedId} />
        </ListPane>
        <ListPane key="recycle">
          <ListTitle {...titles[1]} />
          <Recycle
            type="storyboard"
            orgId={orgId}
            list={filteredRecycleData}
            listLoading={archivedListLoading}
            selectedId={selectedId}
            onInit={recycleInit}
          />
        </ListPane>
      </Wrapper>
    );
  },
);

const Wrapper = styled(ListNav)`
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  padding: ${SPACE_XS} 0;
  background-color: ${p => p.theme.componentBackground};
`;
