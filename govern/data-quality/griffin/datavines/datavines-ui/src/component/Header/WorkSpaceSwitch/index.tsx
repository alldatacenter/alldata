import React, { useState } from 'react';
import {
    Dropdown, Menu, Popconfirm, Button,
} from 'antd';
import { useIntl } from 'react-intl';
import {
    EditOutlined, EllipsisOutlined, DeleteOutlined,
} from '@ant-design/icons';
import { useHistory, useParams, useRouteMatch } from 'react-router-dom';
import { useAddSpace } from './useAddSpace';
import { useSelector, useWorkSpaceActions } from '@/store';
import { CustomSelect, IF, useWatch } from '@/common';
import { $http } from '@/http';
import { getWorkSpaceList } from '@/action/workSpace';
import { DV_WORKSPACE_ID } from '@/utils/constants';
import shareData from '@/utils/shareData';
import './index.less';

export default React.memo(() => {
    const intl = useIntl();
    const history = useHistory();
    const params = useParams<{ id: string}>();
    const match = useRouteMatch();
    const [visible, setVisible] = useState(false);
    const { Render: RenderSpace, show } = useAddSpace({});
    const { workspaceId, spaceList } = useSelector((r) => r.workSpaceReducer);
    const { isDetailPage } = useSelector((r) => r.commonReducer);
    const { loginInfo } = useSelector((r) => r.userReducer);
    const [dataSourceList, setDataSourceList] = useState([]);
    const { setCurrentSpace } = useWorkSpaceActions();
    const onEllips = (e: any) => {
        e.stopPropagation();
        setVisible(true);
    };
    const onShowSpace = (val: any) => {
        setVisible(false);
        show(val);
    };
    const onDelete = async () => {
        try {
            await $http.delete(`/workspace/${workspaceId}`);
            getWorkSpaceList();
        } catch (error) {
        }
    };
    useWatch([isDetailPage], async () => {
        if (isDetailPage) {
            try {
                const res = (await $http.get('/datasource/page', {
                    workSpaceId: workspaceId,
                    pageNumber: 1,
                    pageSize: 9999,
                })) || {};
                setDataSourceList(res?.records || []);
            } catch (error) {
            }
        }
    }, { immediate: true });
    const goBack = () => {
        history.push('/main/home');
    };
    const onChangeSpace = (id: any) => {
        setCurrentSpace(id);
        shareData.sessionSet(`${DV_WORKSPACE_ID}_${loginInfo.id}`, id);
        if (!window.location.href.includes('/main/home')) {
            goBack();
        }
    };
    const onChangeDataSource = (id: any) => {
        const url = `${match.path}`.replace(/:id/, id);
        history.push(`${url}/editor`);
    };
    const renderSelect = () => (
        <CustomSelect
            showSearch
            style={{
                width: 240,
                height: 30,
                display: 'flex',
            }}
            size="small"
            placeholder={intl.formatMessage({ id: 'header_top_search_msg' })}
            optionFilterProp="children"
            filterOption={(input, option: any) => option.children.toLowerCase().indexOf(input.toLowerCase()) >= 0}
            source={spaceList}
            value={workspaceId}
            onChange={onChangeSpace}
            sourceLabelMap="name"
            sourceValueMap="id"
        />
    );
    const renderDataSourcSelect = () => (
        <CustomSelect
            showSearch
            style={{
                width: 240,
                height: 30,
                display: 'flex',
            }}
            size="small"
            placeholder={intl.formatMessage({ id: 'header_top_search_msg' })}
            optionFilterProp="children"
            filterOption={(input, option: any) => option.children.toLowerCase().indexOf(input.toLowerCase()) >= 0}
            source={dataSourceList}
            value={params?.id ? +params.id : undefined}
            onChange={onChangeDataSource}
            sourceLabelMap="name"
            sourceValueMap="id"
        />
    );
    return (
        <>
            <span className="main-color" style={{ fontSize: 18, fontWeight: 700, marginRight: 35 }}>Datavines1</span>
            <IF visible={isDetailPage}>
                <a onClick={goBack} style={{ marginLeft: 20 }}>{intl.formatMessage({ id: 'common_back' })}</a>
                <div className="dv-header__work-space" style={{ paddingRight: 0 }}>
                    {renderDataSourcSelect()}
                </div>
            </IF>
            <IF visible={!isDetailPage}>
                <div className="dv-header__work-space">
                    {renderSelect()}
                    <Dropdown
                        open={visible}
                        onVisibleChange={($visible: boolean) => {
                            if (!$visible) {
                                setVisible(false);
                            }
                        }}
                        overlay={(
                            <Menu
                                selectable={false}
                                items={[
                                    {
                                        icon: <EditOutlined />,
                                        label: <span onClick={() => (onShowSpace(workspaceId))}>{intl.formatMessage({ id: 'workspace_edit' })}</span>,
                                        key: 'edit',
                                    },
                                    spaceList.length !== 1 && {
                                        icon: <DeleteOutlined style={{ color: '#f81d22' }} />,
                                        label: (
                                            <Popconfirm
                                                title={intl.formatMessage({ id: 'workspace_delete_tip' })}
                                                onConfirm={() => { onDelete(); }}
                                                okText={intl.formatMessage({ id: 'common_Ok' })}
                                                cancelText={intl.formatMessage({ id: 'common_Cancel' })}
                                            >
                                                <span>{intl.formatMessage({ id: 'workspace_delete' })}</span>
                                            </Popconfirm>
                                        ),
                                        key: 'delete',
                                    },
                                ].filter(Boolean) as any}
                            />
                        )}
                    >
                        <EllipsisOutlined
                            onClick={onEllips}
                            style={{
                                fontSize: 14,
                                cursor: 'pointer',
                                color: '#000',
                                right: 5,
                                position: 'absolute',
                                top: '50%',
                                transform: 'translateY(-50%)',
                            }}
                        />
                    </Dropdown>
                </div>
                <Button
                    type="primary"
                    onClick={() => {
                        onShowSpace(undefined);
                    }}
                    style={{
                        marginLeft: 10,
                        height: 32,
                        display: 'inline-block',
                        verticalAlign: 'middle',
                        marginTop: 10,
                    }}
                >
                    {intl.formatMessage({ id: 'workspace_add' })}
                </Button>
            </IF>
            <RenderSpace />
        </>
    );
});
