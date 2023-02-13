import {
  DeleteOutlined,
  LoadingOutlined,
  MoreOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import { Button, List, Menu, message, Popconfirm } from 'antd';
import { ListItem, MenuListItem, Popup } from 'app/components';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { calcAc, getCascadeAccess } from 'app/pages/MainPage/Access';
import {
  selectIsOrgOwner,
  selectPermissionMap,
} from 'app/pages/MainPage/slice/selectors';
import classnames from 'classnames';
import { CommonFormTypes } from 'globalConstants';
import { memo, useCallback, useContext, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useHistory } from 'react-router-dom';
import styled from 'styled-components/macro';
import { getInsertedNodeIndex, getPath, stopPPG } from 'utils/utils';
import {
  PermissionLevels,
  ResourceTypes,
  VizResourceSubTypes,
} from '../../PermissionPage/constants';
import { SaveFormContext } from '../SaveFormContext';
import { selectVizs } from '../slice/selectors';
import { deleteViz, removeTab, unarchiveViz } from '../slice/thunks';
import { ArchivedViz } from '../slice/types';

interface RecycleProps {
  type: 'viz' | 'storyboard';
  orgId: string;
  selectedId?: string;
  list?: ArchivedViz[];
  listLoading: boolean;
  onInit: () => void;
}

export const Recycle = memo(
  ({ type, orgId, selectedId, list, listLoading, onInit }: RecycleProps) => {
    const dispatch = useDispatch();
    const history = useHistory();
    const { showSaveForm } = useContext(SaveFormContext);
    const vizs = useSelector(selectVizs);
    const isOwner = useSelector(selectIsOrgOwner);
    const permissionMap = useSelector(selectPermissionMap);
    const tg = useI18NPrefix('global');

    useEffect(() => {
      onInit();
    }, [onInit]);

    const redirect = useCallback(
      vizId => {
        if (vizId) {
          history.push(`/organizations/${orgId}/vizs/${vizId}`);
        } else {
          history.push(`/organizations/${orgId}/vizs`);
        }
      },
      [history, orgId],
    );

    const del = useCallback(
      (id, type) => e => {
        e.stopPropagation();
        dispatch(
          deleteViz({
            params: { id, archive: false },
            type,
            resolve: () => {
              message.success(tg('operation.deleteSuccess'));
              dispatch(removeTab({ id, resolve: redirect }));
            },
          }),
        );
      },
      [dispatch, redirect, tg],
    );

    const moreMenuClick = useCallback(
      (id, name, vizType) =>
        ({ key, domEvent }) => {
          domEvent.stopPropagation();
          switch (key) {
            case 'reset':
              showSaveForm({
                vizType,
                type: CommonFormTypes.Edit,
                visible: true,
                initialValues: { id, name, parentId: void 0 },
                onSave: (values, onClose) => {
                  let index = getInsertedNodeIndex(values, vizs);

                  dispatch(
                    unarchiveViz({
                      params: {
                        id,
                        vizType,
                        ...values,
                        parentId: values.parentId || null,
                        index,
                      },
                      resolve: () => {
                        message.success(tg('operation.restoreSuccess'));
                        dispatch(removeTab({ id, resolve: redirect }));
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
      [dispatch, showSaveForm, redirect, vizs, tg],
    );

    const toDetail = useCallback(
      id => () => {
        history.push(`/organizations/${orgId}/vizs/${id}`);
      },
      [history, orgId],
    );

    return (
      <Wrapper>
        <List
          dataSource={list}
          loading={listLoading && { indicator: <LoadingOutlined /> }}
          renderItem={({ id, name, vizType, loading }) => {
            let allowManage = false;
            if (type === 'viz') {
              const viz = vizs.find(v => v.id === id);
              const path = viz
                ? getPath(
                    vizs as Array<{ id: string; parentId: string }>,
                    { id, parentId: viz.parentId },
                    VizResourceSubTypes.Folder,
                  )
                : [id];
              allowManage = getCascadeAccess(
                isOwner,
                permissionMap,
                ResourceTypes.View,
                path,
                PermissionLevels.Manage,
              );
            } else {
              allowManage = !!calcAc(
                isOwner,
                permissionMap,
                ResourceTypes.Viz,
                PermissionLevels.Manage,
                id,
              );
            }
            return (
              <ListItem
                selected={selectedId === id}
                className={classnames({
                  recycle: true,
                  disabled: loading,
                })}
                onClick={toDetail(id)}
                actions={[
                  allowManage && (
                    <Popup
                      trigger={['click']}
                      placement="bottomRight"
                      content={
                        <Menu
                          prefixCls="ant-dropdown-menu"
                          selectable={false}
                          onClick={moreMenuClick(id, name, vizType)}
                        >
                          <MenuListItem
                            key="reset"
                            prefix={<ReloadOutlined className="icon" />}
                          >
                            {tg('button.restore')}
                          </MenuListItem>
                          <MenuListItem
                            key="delelte"
                            prefix={<DeleteOutlined className="icon" />}
                          >
                            <Popconfirm
                              title={tg('operation.deleteConfirm')}
                              onConfirm={del(id, vizType)}
                            >
                              {tg('button.delete')}
                            </Popconfirm>
                          </MenuListItem>
                        </Menu>
                      }
                    >
                      <Button
                        type="link"
                        icon={<MoreOutlined />}
                        className="btn-hover"
                        onClick={stopPPG}
                      />
                    </Popup>
                  ),
                ]}
              >
                <List.Item.Meta title={name} />
              </ListItem>
            );
          }}
        />
      </Wrapper>
    );
  },
);

const Wrapper = styled.div`
  flex: 1;
  overflow-y: auto;
`;
