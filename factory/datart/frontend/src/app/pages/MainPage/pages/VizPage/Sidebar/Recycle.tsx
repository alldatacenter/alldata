import {
  DeleteOutlined,
  MoreOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import { Menu, message, Popconfirm, TreeDataNode } from 'antd';
import { MenuListItem, Popup, Tree, TreeTitle } from 'app/components';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { selectIsOrgOwner } from 'app/pages/MainPage/slice/selectors';
import { CommonFormTypes } from 'globalConstants';
import { memo, useCallback, useContext, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useHistory } from 'react-router-dom';
import { getInsertedNodeIndex, stopPPG } from 'utils/utils';
import { SaveFormContext } from '../SaveFormContext';
import { selectVizs } from '../slice/selectors';
import { deleteViz, removeTab, unarchiveViz } from '../slice/thunks';

interface RecycleProps {
  type: 'viz' | 'storyboard';
  orgId: string;
  selectedId?: string;
  list?: TreeDataNode[];
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

    const treeSelect = useCallback(
      (_, { node }) => {
        if (node.id !== selectedId) {
          history.push(`/organizations/${orgId}/vizs/${node.id}`);
        }
      },
      [history, orgId, selectedId],
    );

    const renderTreeTitle = useCallback(
      ({ key, title, vizType }) => {
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
                    onClick={moreMenuClick(key, title, vizType)}
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
                        onConfirm={del(key, vizType)}
                      >
                        {tg('button.delete')}
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
      [isOwner, moreMenuClick, tg, del],
    );

    return (
      <Tree
        loading={listLoading}
        treeData={list}
        titleRender={renderTreeTitle}
        onSelect={treeSelect}
      />
    );
  },
);
