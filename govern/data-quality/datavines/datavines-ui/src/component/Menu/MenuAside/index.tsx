/* eslint-disable array-callback-return */
import React, { memo, useCallback, useState } from 'react';
import { Menu, Popover, Select } from 'antd';
import {
    RouteComponentProps, withRouter,
} from 'react-router-dom';
import useWatch from '@Editor/common/useWatch';
import logoPng from 'assets/images/logo-light.png';
import EscapePng from 'assets/images/escape.svg';
import EhPng from 'assets/images/en.svg';
import ChPng from 'assets/images/ch.svg';
import './index.less';
import { LogoutOutlined, PlusOutlined } from '@ant-design/icons';
import { useIntl } from 'react-intl';
import { useSelector, useCommonActions, useWorkSpaceActions } from '@/store';
import shareData from '@/utils/shareData';
import { DV_LANGUAGE, DV_STORAGE_LOGIN, DV_WORKSPACE_ID } from '@/utils/constants';
import { useAddSpace } from '../../Header/WorkSpaceSwitch/useAddSpace';

const Logo = memo(({ style }:any) => <img style={style} className="logo" src={logoPng} />);
// const Escape = memo(({ style, onClick }:any) => <img style={style} className="escape" src={EscapePng} onClick={onClick} />);
const Eh = memo(({ style, onClick }:any) => <img style={style} className="escape" src={EhPng} onClick={onClick} />);
const Ch = memo(({ style, onClick }:any) => <img style={style} className="escape" src={ChPng} onClick={onClick} />);
export type MenuItem = {
    label: React.ReactNode,
    key: string,
    path: string,
    icon?: React.ReactNode,
    children?: MenuItem[],
    menuHide?: boolean,
    exact?: boolean,
    [key: string]: any,
}

interface TMenuAside extends RouteComponentProps {
    menus: MenuItem[]
}

const MenuAside: React.FC<TMenuAside> = ({ menus, history }) => {
    const [activeKeys, setActiveKeys] = useState<string[]>(['']);
    const { workspaceId, spaceList } = useSelector((r) => r.workSpaceReducer);
    const { loginInfo } = useSelector((r) => r.userReducer);
    const intl = useIntl();
    const { Render: RenderSpace, show } = useAddSpace({});
    const setActiveKeyRedirect = (redirect = false) => {
        const url = window.location.href;
        const match = menus.find((item) => {
            if (item.path.includes(':id')) {
                const pathArr = item.path.split(':id');
                return url.includes(pathArr[0]) && url.includes(pathArr[1]);
            }
            if (url.includes(item.path)) {
                return true;
            }
        });
        const currItem = (match || menus[0]) as MenuItem;
        if (!currItem) {
            return;
        }
        if (!activeKeys.includes(currItem.key as string)) {
            setActiveKeys([currItem.key as string]);
        }
        if (!match && redirect) {
            history.replace(currItem.path);
        }
    };
    useWatch(menus, () => setActiveKeyRedirect(true), { immediate: true });
    const clickMenuItem = useCallback((obj:any) => {
        setActiveKeys(obj.key);
        history.push(obj.key);
    }, []);
    const handleMenuClick = () => {
        window.localStorage.removeItem(DV_STORAGE_LOGIN);
        history.push('/login');
    };
    const { setCurrentSpace } = useWorkSpaceActions();

    const onChangeSpace = (id: any) => {
        setCurrentSpace(id);
        shareData.sessionSet(`${DV_WORKSPACE_ID}_${loginInfo.id}`, id);
        if (!window.location.href.includes('/main/home')) {
            history.push('/main/home');
        }
    };
    const onShowSpace = (val: any) => {
        show(val);
    };
    const content = (
        <div className="dv-user-Popover">
            <div className="dv-user">AD</div>
            <ul>
                <li>
                    <div style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                    }}
                    >
                        {intl.formatMessage({ id: 'header_top_workspace' })}
                        <span
                            style={{
                                cursor: 'pointer',
                            }}
                            onClick={() => onShowSpace(undefined)}
                        >
                            <PlusOutlined style={{ marginRight: '4px' }} />
                            {intl.formatMessage({ id: 'common_create_btn' })}
                        </span>
                    </div>
                    <div
                        className="btn"
                        style={{
                            display: 'flex',
                            justifyContent: 'space-between',
                            cursor: 'pointer',
                        }}
                    >
                        <Select
                            className="select-workspace"
                            optionFilterProp="children"
                            filterOption={(input, option: any) => option.children.toLowerCase().indexOf(input.toLowerCase()) >= 0}
                            options={spaceList}
                            value={workspaceId}
                            onChange={onChangeSpace}
                            key="id"
                            fieldNames={
                                {
                                    label: 'name',
                                    value: 'id',
                                }
                            }
                        />
                        {/* <span>mysql</span>
                        <SwapOutlined /> */}
                        {/* <RightOutlined /> */}
                    </div>
                </li>
                {/* <li style={{
                    cursor: 'pointer',
                }}
                >
                    <LogoutOutlined style={{ marginRight: '10px' }} />
                    退出空间
                </li> */}
                <li
                    style={{
                        cursor: 'pointer',
                    }}
                    onClick={handleMenuClick}
                >
                    <LogoutOutlined style={{ marginRight: '10px' }} />
                    {intl.formatMessage({ id: 'common_Logout' })}
                </li>
            </ul>
        </div>
    );
    const $munus = menus.map((item) => {
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        const { exact, ...rest } = item;
        return {
            ...rest,
            style: {
                // paddingLeft: 15,
                // paddingRight: 15,
                fontSize: 14,
                color: '#000',
            },
        };
    }).filter((item) => (!item.menuHide)) as MenuItem[];

    const { locale } = useSelector((r) => r.commonReducer);
    const { setLocale } = useCommonActions();
    const swichFn = () => {
        const $locale = locale === 'zh_CN' ? 'en_US' : 'zh_CN';
        setLocale($locale);
        shareData.storageSet(DV_LANGUAGE, $locale);
    };

    return (
        <div style={{
            height: '100%',
            marginRight: '20px',
            boxShadow: '0px 4px 13px rgb(166 166 166 / 25%)',
            borderRadius: '0px 32px 32px 0px',
            padding: '20px 0px',
        }}
        >
            <Logo style={{ width: '40px', marginLeft: '8px', marginBottom: '10px' }} />
            <Menu
                style={{
                    border: 'none',
                }}
                selectedKeys={activeKeys}
                mode="inline"
                onClick={clickMenuItem}
                items={$munus}
            />
            {
                locale === 'zh_CN' ? (
                    <Ch
                        style={{
                            position: 'absolute',
                            bottom: '70px',
                            marginLeft: '5px',
                            cursor: 'pointer',
                            width: '46px',
                        }}
                        onClick={swichFn}
                    />
                ) : (
                    <Eh
                        style={{
                            position: 'absolute',
                            bottom: '70px',
                            marginLeft: '5px',
                            cursor: 'pointer',
                            width: '46px',
                        }}
                        onClick={swichFn}
                    />
                )
            }
            {/* <Escape
                style={{
                    position: 'absolute',
                    bottom: '70px',
                    marginLeft: '10px',
                    cursor: 'pointer',
                }}
                onClick={swichFn}
            /> */}
            <Popover placement="rightBottom" content={content} trigger="click">
                <span className="dv-user">
                    AD
                </span>
            </Popover>
            <RenderSpace />
        </div>
    );
};

export default withRouter(MenuAside);
