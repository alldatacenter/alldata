import {
  DeleteOutlined,
  FileOutlined,
  FolderFilled,
  FolderOpenFilled,
  FolderOutlined,
  MailOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  WechatOutlined,
} from '@ant-design/icons';
import { message } from 'antd';
import { ListNav, ListPane, ListTitle } from 'app/components';
import { useDebouncedSearch } from 'app/hooks/useDebouncedSearch';
import useI18NPrefix, { I18NComponentProps } from 'app/hooks/useI18NPrefix';
import { selectOrgId } from 'app/pages/MainPage/slice/selectors';
import { dispatchResize } from 'app/utils/dispatchResize';
import { CommonFormTypes } from 'globalConstants';
import { memo, useCallback, useContext, useMemo } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useRouteMatch } from 'react-router';
import styled from 'styled-components/macro';
import { LEVEL_5, SPACE_TIMES, SPACE_XS } from 'styles/StyleConstants';
import { getInsertedNodeIndex } from 'utils/utils';
import { JobTypes } from '../constants';
import { useToScheduleDetails } from '../hooks';
import { SaveForm } from '../SaveForm';
import { SaveFormContext } from '../SaveFormContext';
import {
  makeSelectScheduleTree,
  selectArchived,
  selectSchedules,
} from '../slice/selectors';
import { addSchedule } from '../slice/thunks';
import { ScheduleSimpleViewModel } from '../slice/types';
import { Recycle } from './Recycle';
import { ScheduleList } from './ScheduleList';

interface SidebarProps extends I18NComponentProps {
  isDragging: boolean;
  width: number;
  sliderVisible: boolean;
  handleSliderVisible: (status: boolean) => void;
}

export const Sidebar = memo(
  ({ width, isDragging, sliderVisible, handleSliderVisible }: SidebarProps) => {
    const dispatch = useDispatch();
    const matchScheduleDetail = useRouteMatch<{
      scheduleId: string;
    }>('/organizations/:orgId/schedules/:scheduleId');
    const scheduleId = matchScheduleDetail?.params?.scheduleId;
    const orgId = useSelector(selectOrgId);
    const archived = useSelector(selectArchived);
    const scheduleData = useSelector(selectSchedules);
    const { showSaveForm } = useContext(SaveFormContext);
    const t = useI18NPrefix('schedule.sidebar');
    const tg = useI18NPrefix('global');

    const selectScheduleTree = useMemo(makeSelectScheduleTree, []);
    const getIcon = useCallback(
      ({ isFolder, type }: ScheduleSimpleViewModel) =>
        isFolder ? (
          p => (p.expanded ? <FolderOpenFilled /> : <FolderFilled />)
        ) : type === JobTypes.Email ? (
          <MailOutlined />
        ) : (
          <WechatOutlined />
        ),
      [],
    );
    const getDisabled = useCallback(
      ({ deleteLoading }: ScheduleSimpleViewModel) => deleteLoading,
      [],
    );

    const treeData = useSelector(state =>
      selectScheduleTree(state, { getIcon, getDisabled }),
    );

    const recycleList = useMemo(
      () =>
        archived?.map(({ id, name, parentId, isFolder, deleteLoading }) => ({
          id,
          key: id,
          title: name,
          parentId,
          icon: isFolder ? <FolderOutlined /> : <FileOutlined />,
          isFolder,
          disabled: deleteLoading,
        })),
      [archived],
    );

    const { filteredData: scheduleList, debouncedSearch: listSearch } =
      useDebouncedSearch(treeData, (keywords, d) =>
        d.title.toLowerCase().includes(keywords.toLowerCase()),
      );
    const { filteredData: archivedList, debouncedSearch: archivedSearch } =
      useDebouncedSearch(recycleList, (keywords, d) =>
        d.title.toLowerCase().includes(keywords.toLowerCase()),
      );

    const { toDetails } = useToScheduleDetails();
    const toAdd = useCallback(
      ({ key }) => {
        switch (key) {
          case 'add':
            toDetails(orgId, 'add');
            break;
          case 'folder':
            showSaveForm({
              scheduleType: 'folder',
              type: CommonFormTypes.Add,
              visible: true,
              simple: false,
              parentIdLabel: t('scheduleList.parent'),
              onSave: (values, onClose) => {
                let index = getInsertedNodeIndex(values, scheduleData);
                dispatch(
                  addSchedule({
                    params: {
                      ...values,
                      parentId: values.parentId || null,
                      index,
                      orgId,
                      isFolder: true,
                    },
                    resolve: () => {
                      onClose();
                      message.success(t('index.addSuccess'));
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
      [toDetails, orgId, showSaveForm, scheduleData, dispatch, t],
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
          title: t('index.scheduledTaskList'),
          search: true,
          onSearch: listSearch,
          add: {
            items: [
              { key: 'add', text: t('index.newTimedTask') },
              { key: 'folder', text: t('index.addFolder') },
            ],
            callback: toAdd,
          },
          more: {
            items: [
              {
                key: 'recycle',
                text: t('index.recycle'),
                prefix: <DeleteOutlined className="icon" />,
              },
              {
                key: 'collapse',
                text: t(sliderVisible ? 'index.open' : 'index.close'),
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
          title: t('index.recycle'),
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
            <ScheduleList
              orgId={orgId}
              scheduleId={scheduleId}
              list={scheduleList}
            />
          </ListPane>
          <ListPane key="recycle">
            <ListTitle {...titles[1]} />
            <Recycle scheduleId={scheduleId} list={archivedList} />
          </ListPane>
        </ListNavWrapper>
        <SaveForm
          formProps={{
            labelAlign: 'left',
            labelCol: { offset: 1, span: 8 },
            wrapperCol: { span: 13 },
          }}
          okText={tg('button.save')}
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

const ListNavWrapper = styled(ListNav)`
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  padding: ${SPACE_XS} 0;
  background-color: ${p => p.theme.componentBackground};
`;
