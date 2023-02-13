import {
  DeleteOutlined,
  EditOutlined,
  LoadingOutlined,
  MoreOutlined,
} from '@ant-design/icons';
import { Button, List as ListComponent, Menu, message, Popconfirm } from 'antd';
import { ListItem, MenuListItem, Popup } from 'app/components';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { Access } from 'app/pages/MainPage/Access';
import { selectOrgId } from 'app/pages/MainPage/slice/selectors';
import classNames from 'classnames';
import { CommonFormTypes } from 'globalConstants';
import { memo, useCallback, useContext, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useHistory } from 'react-router';
import styled from 'styled-components/macro';
import { stopPPG } from 'utils/utils';
import { SaveFormContext } from '../../SaveFormContext';
import { selectStoryboardListLoading } from '../../slice/selectors';
import {
  deleteViz,
  editStoryboard,
  getStoryboards,
  removeTab,
} from '../../slice/thunks';
import { StoryboardViewModel } from '../../slice/types';
import { allowManageStoryboard } from '../../utils';

interface StoryboardListProps {
  selectedId?: string;
  list?: StoryboardViewModel[];
}

export const List = memo(({ list, selectedId }: StoryboardListProps) => {
  const dispatch = useDispatch();
  const history = useHistory();
  const listLoading = useSelector(selectStoryboardListLoading);
  const orgId = useSelector(selectOrgId);
  const { showSaveForm } = useContext(SaveFormContext);
  const tg = useI18NPrefix('global');

  useEffect(() => {
    dispatch(getStoryboards(orgId));
  }, [dispatch, orgId]);

  const toDetail = useCallback(
    id => () => {
      history.push(`/organizations/${orgId}/vizs/${id}`);
    },
    [history, orgId],
  );

  const redirect = useCallback(
    tabKey => {
      if (tabKey) {
        history.push(`/organizations/${orgId}/vizs/${tabKey}`);
      } else {
        history.push(`/organizations/${orgId}/vizs`);
      }
    },
    [history, orgId],
  );

  const archiveStoryboard = useCallback(
    id => () => {
      dispatch(
        deleteViz({
          params: { id, archive: true },
          type: 'STORYBOARD',
          resolve: () => {
            message.success(tg('operation.archiveSuccess'));
            dispatch(removeTab({ id, resolve: redirect }));
          },
        }),
      );
    },
    [dispatch, redirect, tg],
  );

  const moreMenuClick = useCallback(
    storyboard =>
      ({ key, domEvent }) => {
        domEvent.stopPropagation();
        switch (key) {
          case 'info':
            showSaveForm({
              vizType: 'STORYBOARD',
              type: CommonFormTypes.Edit,
              visible: true,
              initialValues: { id: storyboard.id, name: storyboard.name },
              onSave: (values, onClose) => {
                dispatch(
                  editStoryboard({
                    storyboard: { ...storyboard, ...values },
                    resolve: onClose,
                  }),
                );
              },
            });
            break;
          case 'delete':
            break;
          default:
            break;
        }
      },
    [dispatch, showSaveForm],
  );

  return (
    <Wrapper>
      <ListComponent
        dataSource={list}
        loading={listLoading && { indicator: <LoadingOutlined /> }}
        renderItem={s => (
          <ListItem
            actions={[
              <Access {...allowManageStoryboard(s.id)}>
                <Popup
                  trigger={['click']}
                  placement="bottom"
                  content={
                    <Menu
                      prefixCls="ant-dropdown-menu"
                      selectable={false}
                      onClick={moreMenuClick(s)}
                    >
                      <MenuListItem
                        key="info"
                        prefix={<EditOutlined className="icon" />}
                      >
                        {tg('button.info')}
                      </MenuListItem>
                      <MenuListItem
                        key="delete"
                        prefix={<DeleteOutlined className="icon" />}
                      >
                        <Popconfirm
                          title={tg('operation.archiveConfirm')}
                          onConfirm={archiveStoryboard(s.id)}
                        >
                          {tg('button.archive')}
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
              </Access>,
            ]}
            selected={selectedId === s.id}
            className={classNames({ recycle: true, disabled: s.deleteLoading })}
            onClick={toDetail(s.id)}
          >
            <ListComponent.Item.Meta title={s.name} />
          </ListItem>
        )}
      />
    </Wrapper>
  );
});

const Wrapper = styled.div`
  flex: 1;
  overflow-y: auto;
`;
