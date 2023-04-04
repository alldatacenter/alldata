import {
  DeleteOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
} from '@ant-design/icons';
import { ListNav, ListPane, ListTitle } from 'app/components';
import { useDebouncedSearch } from 'app/hooks/useDebouncedSearch';
import useGetVizIcon from 'app/hooks/useGetVizIcon';
import useI18NPrefix, { I18NComponentProps } from 'app/hooks/useI18NPrefix';
import { selectOrgId } from 'app/pages/MainPage/slice/selectors';
import { dispatchResize } from 'app/utils/dispatchResize';
import { CommonFormTypes } from 'globalConstants';
import React, { memo, useCallback, useContext, useMemo } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useHistory } from 'react-router';
import styled from 'styled-components/macro';
import { SPACE_XS } from 'styles/StyleConstants';
import { useAddViz } from '../../hooks/useAddViz';
import { SaveFormContext } from '../../SaveFormContext';
import {
  makeSelectArchivedDashboardsTree,
  makeSelectArchivedDatachartsTree,
  makeSelectVizTree,
  selectArchivedDashboardLoading,
  selectArchivedDatachartLoading,
} from '../../slice/selectors';
import {
  getArchivedDashboards,
  getArchivedDatacharts,
} from '../../slice/thunks';
import { ArchivedViz, FolderViewModel } from '../../slice/types';
import { Recycle } from '../Recycle';
import { FolderTree } from './FolderTree';

interface FoldersProps extends I18NComponentProps {
  sliderVisible: boolean;
  handleSliderVisible: (status: boolean) => void;
  selectedId?: string;
  className?: string;
}

export const Folders = memo(
  ({
    selectedId,
    className,
    i18nPrefix,
    sliderVisible,
    handleSliderVisible,
  }: FoldersProps) => {
    const dispatch = useDispatch();
    const orgId = useSelector(selectOrgId);
    const selectVizTree = useMemo(makeSelectVizTree, []);
    const t = useI18NPrefix(i18nPrefix);
    const history = useHistory();
    const { showSaveForm } = useContext(SaveFormContext);
    const addVizFn = useAddViz({ showSaveForm });

    const getIcon = useGetVizIcon();

    const getDisabled = useCallback(
      ({ deleteLoading }: FolderViewModel) => deleteLoading,
      [],
    );

    const treeData = useSelector(state =>
      selectVizTree(state, { getIcon, getDisabled }),
    );

    const { filteredData: filteredTreeData, debouncedSearch: treeSearch } =
      useDebouncedSearch(treeData, (keywords, d) =>
        d.title.toLowerCase().includes(keywords.toLowerCase()),
      );

    const selectArchivedDatachartsTree = useMemo(
      makeSelectArchivedDatachartsTree,
      [],
    );
    const selectArchivedDashboardsTree = useMemo(
      makeSelectArchivedDashboardsTree,
      [],
    );

    const getArchivedDisabled = useCallback(
      ({ deleteLoading }: ArchivedViz) => deleteLoading,
      [],
    );

    const archivedDatachartsTreeData = useSelector(state =>
      selectArchivedDatachartsTree(state, { getDisabled: getArchivedDisabled }),
    );
    const archivedDashboardsTreeData = useSelector(state =>
      selectArchivedDashboardsTree(state, { getDisabled: getArchivedDisabled }),
    );
    const archivedDataChartLoading = useSelector(
      selectArchivedDatachartLoading,
    );
    const archivedDashboardLoading = useSelector(
      selectArchivedDashboardLoading,
    );
    const { filteredData: filteredListData, debouncedSearch: listSearch } =
      useDebouncedSearch(
        (archivedDatachartsTreeData || []).concat(archivedDashboardsTreeData),
        (keywords, d) => d.title.toLowerCase().includes(keywords.toLowerCase()),
      );

    const recycleInit = useCallback(() => {
      dispatch(getArchivedDatacharts(orgId));
      dispatch(getArchivedDashboards(orgId));
    }, [dispatch, orgId]);

    const add = useCallback(
      ({ key }) => {
        if (key === 'DATACHART') {
          history.push({
            pathname: `/organizations/${orgId}/vizs/chartEditor`,
            search: `dataChartId=&chartType=dataChart&container=dataChart`,
          });
          return false;
        }

        addVizFn({
          vizType: key,
          type: CommonFormTypes.Add,
          visible: true,
          initialValues: undefined,
        });
      },
      [orgId, history, addVizFn],
    );

    const titles = useMemo(
      () => [
        {
          subTitle: t('folders.folderTitle'),
          add: {
            items: [
              { key: 'DATACHART', text: t('folders.startAnalysis') },
              { key: 'DASHBOARD', text: t('folders.dashboard') },
              { key: 'FOLDER', text: t('folders.folder') },
              { key: 'TEMPLATE', text: t('folders.template') },
            ],
            callback: add,
          },
          more: {
            items: [
              {
                key: 'recycle',
                text: t('folders.recycle'),
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
                  dispatchResize();
                  break;
              }
            },
          },
          search: true,
          onSearch: treeSearch,
        },
        {
          key: 'recycle',
          subTitle: t('folders.recycle'),
          back: true,
          search: true,
          onSearch: listSearch,
        },
      ],
      [add, treeSearch, listSearch, t, sliderVisible, handleSliderVisible],
    );

    return (
      <Wrapper className={className} defaultActiveKey="list">
        <ListPane key="list">
          <ListTitle {...titles[0]} />
          <FolderTree
            treeData={filteredTreeData}
            selectedId={selectedId}
            i18nPrefix={i18nPrefix}
          />
        </ListPane>
        <ListPane key="recycle">
          <ListTitle {...titles[1]} />
          <Recycle
            type="viz"
            orgId={orgId}
            list={filteredListData}
            listLoading={archivedDashboardLoading || archivedDataChartLoading}
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
