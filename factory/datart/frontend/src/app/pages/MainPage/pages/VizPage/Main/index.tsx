import { CloseOutlined } from '@ant-design/icons';
import { Dropdown, Menu } from 'antd';
import { EmptyFiller, TabPane, Tabs } from 'app/components';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import BoardEditor from 'app/pages/DashBoardPage/pages/BoardEditor';
import { selectOrgId } from 'app/pages/MainPage/slice/selectors';
import { dispatchResize } from 'app/utils/dispatchResize';
import { useCallback, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  Route,
  Switch,
  useHistory,
  useLocation,
  useRouteMatch,
} from 'react-router-dom';
import styled from 'styled-components/macro';
import { LEVEL_1 } from 'styles/StyleConstants';
import { useVizSlice } from '../slice';
import {
  selectArchivedDashboards,
  selectArchivedDatacharts,
  selectArchivedStoryboards,
  selectSelectedTab,
  selectStoryboards,
  selectTabs,
  selectVizs,
} from '../slice/selectors';
import { closeAllTabs, closeOtherTabs, removeTab } from '../slice/thunks';
import { ArchivedViz, Folder, Storyboard } from '../slice/types';
import { VizContainer } from './VizContainer';

export function Main({ sliderVisible }: { sliderVisible: boolean }) {
  const { actions } = useVizSlice();
  const dispatch = useDispatch();
  const history = useHistory();
  const {
    params: { vizId },
  } = useRouteMatch<{ vizId: string }>();
  const location = useLocation();
  const vizs = useSelector(selectVizs);
  const storyboards = useSelector(selectStoryboards);
  const archivedDatacharts = useSelector(selectArchivedDatacharts);
  const archivedDashboards = useSelector(selectArchivedDashboards);
  const archivedStoryboards = useSelector(selectArchivedStoryboards);
  const tabs = useSelector(selectTabs);
  const selectedTab = useSelector(selectSelectedTab);
  const orgId = useSelector(selectOrgId);

  const t = useI18NPrefix('viz.main');

  useEffect(() => {
    if (vizId) {
      const viz =
        vizs.find(v => v.relId === vizId) ||
        storyboards.find(({ id }) => id === vizId) ||
        archivedDatacharts.find(({ id }) => id === vizId) ||
        archivedDashboards.find(({ id }) => id === vizId) ||
        archivedStoryboards.find(({ id }) => id === vizId);
      if (viz) {
        if ((viz as ArchivedViz).vizType) {
          const { id, name, vizType } = viz as ArchivedViz;
          dispatch(
            actions.addTab({
              id,
              type: vizType,
              name,
              search: location.search,
              parentId: null,
            }),
          );
        }
        if ((viz as Folder).relType) {
          const { id, name, relId, relType, parentId } = viz as Folder;
          dispatch(
            actions.addTab({
              id: relId,
              type: relType,
              name,
              search: location.search,
              parentId,
              permissionId: id,
            }),
          );
        } else {
          const { id, name } = viz as Storyboard;
          dispatch(
            actions.addTab({
              id,
              type: 'STORYBOARD',
              name,
              search: location.search,
              parentId: null,
            }),
          );
        }
      }
    }
  }, [
    dispatch,
    location,
    actions,
    vizs,
    storyboards,
    archivedDatacharts,
    archivedDashboards,
    archivedStoryboards,
    vizId,
  ]);

  useEffect(() => {
    if (selectedTab && !vizId) {
      history.push(`/organizations/${orgId}/vizs/${selectedTab.id}`);
    }
  }, [history, selectedTab, orgId, vizId]);

  const tabChange = useCallback(
    activeKey => {
      const activeTab = tabs.find(v => v.id === activeKey);
      if (activeTab) {
        history.push(
          `/organizations/${orgId}/vizs/${activeKey}${activeTab.search || ''}`,
        );
      }
      setTimeout(() => {
        dispatchResize();
      }, 500);
    },
    [history, orgId, tabs],
  );

  const tabEdit = useCallback(
    (targetKey, action) => {
      switch (action) {
        case 'remove':
          dispatch(
            removeTab({
              id: targetKey,
              resolve: activeKey => {
                const activeTab = tabs.find(v => v.id === activeKey);
                if (activeTab) {
                  history.push(
                    `/organizations/${orgId}/vizs/${activeKey}${
                      activeTab.search || ''
                    }`,
                  );
                } else {
                  history.push(`/organizations/${orgId}/vizs`);
                }
              },
            }),
          );
          break;
        default:
          break;
      }
    },
    [dispatch, history, orgId, tabs],
  );

  const handleClickMenu = (e: any, id: string) => {
    e.domEvent.stopPropagation();
    if (e.key === 'CLOSE_ALL') {
      dispatch(
        closeAllTabs({
          resolve() {
            history.push(`/organizations/${orgId}/vizs`);
          },
        }),
      );
      return;
    }
    dispatch(
      closeOtherTabs({
        id,
        resolve: activeKey => {
          const activeTab = tabs.find(v => v.id === activeKey);
          if (activeTab) {
            history.push(
              `/organizations/${orgId}/vizs/${activeKey}${
                activeTab.search || ''
              }`,
            );
          } else {
            history.push(`/organizations/${orgId}/vizs`);
          }
        },
      }),
    );
  };

  const menu = (id: string) => (
    <Menu onClick={e => handleClickMenu(e, id)}>
      <Menu.Item key="CLOSE_OTHER">
        <span>{t('closeOther')}</span>
      </Menu.Item>
      <Menu.Item key="CLOSE_ALL">
        <span>{t('closeAll')}</span>
      </Menu.Item>
    </Menu>
  );

  const Tab = (id: string, name: string) => (
    <span>
      <Dropdown overlay={menu(id)} trigger={['contextMenu']}>
        <span className="ant-dropdown-link">{name}</span>
      </Dropdown>
    </span>
  );

  return (
    <Wrapper className={sliderVisible ? 'close datart-viz' : 'datart-viz'}>
      <TabsWrapper>
        <Tabs
          hideAdd
          mode="dashboard"
          type="editable-card"
          activeKey={selectedTab?.id}
          onChange={tabChange}
          onEdit={tabEdit}
        >
          {tabs.map(({ id, name }) => (
            <TabPane
              key={id}
              tab={Tab(id, name)}
              closeIcon={
                <CloseIconWrapper>
                  <CloseOutlined />
                </CloseIconWrapper>
              }
            />
          ))}
        </Tabs>
      </TabsWrapper>
      {tabs.map(tab => (
        <VizContainer
          key={tab.id}
          tab={tab}
          orgId={orgId}
          vizs={vizs}
          selectedId={selectedTab?.id}
        />
      ))}
      {!tabs.length && <EmptyFiller title={t('empty')} />}

      <Switch>
        <Route
          path="/organizations/:orgId/vizs/:vizId?/boardEditor"
          render={() => <BoardEditor boardId={vizId} />}
        />
      </Switch>
    </Wrapper>
  );
}

const Wrapper = styled.div`
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  &.close {
    width: calc(100% - 30px) !important;
    min-width: calc(100% - 30px) !important;
    padding-left: 30px;
  }
`;

const TabsWrapper = styled.div`
  z-index: ${LEVEL_1};
  flex-shrink: 0;
`;

const CloseIconWrapper = styled.span`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 12px;
  height: 12px;
`;
