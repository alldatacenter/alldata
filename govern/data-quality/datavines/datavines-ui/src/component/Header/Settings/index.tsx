import React from 'react';
import {
    Dropdown, Menu, message, Popconfirm,
} from 'antd';
import { LoginOutlined } from '@ant-design/icons';
import { useHistory } from 'react-router-dom';
import { useIntl } from 'react-intl';
import { SwitchLanguage } from '@/component';
import { useSelector } from '@/store';
import { $http } from '@/http';
import { DV_STORAGE_LOGIN } from '@/utils/constants';
import { useLoading } from '@/common';
import { getWorkSpaceList } from '@/action/workSpace';

const HeaderRight = () => {
    const intl = useIntl();
    const history = useHistory();
    const setBodyLoading = useLoading();
    const { loginInfo } = useSelector((r) => r.userReducer);
    const { workspaceId } = useSelector((r) => r.workSpaceReducer);
    const handleMenuClick = () => {
        window.localStorage.removeItem(DV_STORAGE_LOGIN);
        history.push('/login');
    };
    const exitWorkspace = async () => {
        try {
            setBodyLoading(true);
            const res = await $http.delete('/workspace/removeUser', {
                workspaceId,
                userId: loginInfo.id,
            });
            if (res) {
                getWorkSpaceList(true);
                message.success(intl.formatMessage({ id: 'common_success' }));
            } else {
                message.success(intl.formatMessage({ id: 'common_fail' }));
            }
        } catch (error) {
        } finally {
            setBodyLoading(false);
        }
    };
    return (
        <div className="dv-header-right">
            <Popconfirm
                title={intl.formatMessage({ id: 'workspace_exit_tip' })}
                onConfirm={() => { exitWorkspace?.(); }}
                okText={intl.formatMessage({ id: 'common_Ok' })}
                cancelText={intl.formatMessage({ id: 'common_Cancel' })}
            >
                <a style={{ color: 'red', marginRight: 15 }}>{intl.formatMessage({ id: 'workspace_exit' })}</a>
            </Popconfirm>

            <div className="dv-header__switch-language"><SwitchLanguage /></div>
            <span style={{ margin: '0 15px' }}>{loginInfo.username}</span>
            <Dropdown
                overlay={(
                    <Menu
                        onClick={handleMenuClick}
                        items={[
                            {
                                label: 'Log out',
                                key: '0',
                            },
                        ]}
                    />
                )}
            >
                <LoginOutlined style={{ fontSize: 14, cursor: 'pointer' }} />
            </Dropdown>
        </div>
    );
};

export default React.memo(HeaderRight);
