import {
  DeleteOutlined,
  MoreOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import { Menu, message, Popconfirm, TreeDataNode } from 'antd';
import { MenuListItem, Popup, Tree, TreeTitle } from 'app/components';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import {
  selectIsOrgOwner,
  selectOrgId,
} from 'app/pages/MainPage/slice/selectors';
import { CommonFormTypes } from 'globalConstants';
import { memo, useCallback, useContext, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useHistory } from 'react-router';
import { getInsertedNodeIndex, stopPPG } from 'utils/utils';
import { useToScheduleDetails } from '../hooks';
import { SaveFormContext } from '../SaveFormContext';
import { selectArchivedListLoading } from '../slice/selectors';
import {
  deleteSchedule,
  getArchivedSchedules,
  unarchiveSchedule,
} from '../slice/thunks';

interface ListData extends TreeDataNode {
  id: string;
  parentId: string | null;
}

interface RecycleProps {
  scheduleId?: string;
  list?: ListData[];
}

export const Recycle = memo(({ scheduleId, list }: RecycleProps) => {
  const dispatch = useDispatch();
  const history = useHistory();
  const loading = useSelector(selectArchivedListLoading);
  const orgId = useSelector(selectOrgId);
  const t = useI18NPrefix('schedule.editor.index');
  const { toDetails } = useToScheduleDetails();
  const isOwner = useSelector(selectIsOrgOwner);
  const { showSaveForm } = useContext(SaveFormContext);
  useEffect(() => {
    dispatch(getArchivedSchedules(orgId));
  }, [dispatch, orgId]);

  const moreMenuClick = useCallback(
    (id, name) =>
      ({ key, domEvent }) => {
        domEvent.stopPropagation();
        switch (key) {
          case 'reset':
            showSaveForm({
              scheduleType: 'folder',
              type: CommonFormTypes.Edit,
              visible: true,
              simple: false,
              initialValues: { id, name, parentId: null },
              parentIdLabel: t('parent'),
              onSave: (values, onClose) => {
                let index = getInsertedNodeIndex(values, list);
                dispatch(
                  unarchiveSchedule({
                    schedule: { ...values, id, index },
                    resolve: () => {
                      message.success(t('restoredSuccess'));
                      toDetails(orgId);
                      onClose();
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
    [showSaveForm, t, list, dispatch, toDetails, orgId],
  );

  const del = useCallback(
    id => () => {
      dispatch(
        deleteSchedule({
          id,
          archive: false,
          resolve: () => {
            message.success(`${t('success')}${t('delete')}`);
            toDetails(orgId);
          },
        }),
      );
    },
    [dispatch, orgId, t, toDetails],
  );

  const renderTreeTitle = useCallback(
    ({ key, title }) => {
      return (
        <TreeTitle>
          <h4>{`${title}`}</h4>
          {isOwner && (
            <Popup
              trigger={['click']}
              placement="bottomRight"
              content={
                <Menu
                  prefixCls="ant-dropdown-menu"
                  selectable={false}
                  onClick={moreMenuClick(key, title)}
                >
                  <MenuListItem
                    key="reset"
                    prefix={<ReloadOutlined className="icon" />}
                  >
                    {t('restore')}
                  </MenuListItem>
                  <MenuListItem
                    key="delelte"
                    prefix={<DeleteOutlined className="icon" />}
                  >
                    <Popconfirm title={t('sureToDelete')} onConfirm={del(key)}>
                      {t('delete')}
                    </Popconfirm>
                  </MenuListItem>
                </Menu>
              }
            >
              <span className="action" onClick={stopPPG}>
                <MoreOutlined />
              </span>
            </Popup>
          )}
        </TreeTitle>
      );
    },
    [isOwner, moreMenuClick, t, del],
  );

  const treeSelect = useCallback(
    (_, { node }) => {
      if (!node.isFolder && node.id !== scheduleId) {
        history.push(`/organizations/${orgId}/schedules/${node.id}`);
      }
    },
    [history, orgId, scheduleId],
  );

  return (
    <Tree
      loading={loading}
      treeData={list}
      titleRender={renderTreeTitle}
      onSelect={treeSelect}
    />
  );
});
