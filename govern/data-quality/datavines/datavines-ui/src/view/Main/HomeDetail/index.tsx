import React, { useEffect, useState } from 'react';
import useRoute from 'src/router/useRoute';
import { useIntl } from 'react-intl';
import {
    Route, Switch, useHistory, useLocation, useParams, useRouteMatch,
} from 'react-router-dom';
import { ArrowLeftOutlined } from '@ant-design/icons';
import { Button } from 'antd';
import { CustomSelect } from '@Editor/common';
import { useSelector } from 'react-redux';
import { MenuItem } from '@/component/Menu/MenuAside';
import MenuLayout from '@/component/Menu/Layout';
import { $http } from '@/http';
import store from '@/store';
import EditorData from '@/view/Main/HomeDetail/EditorData';
import Jobs from '@/view/Main/HomeDetail/Jobs';

type DataSource = {
    id:number,
    name:'hellow'
}
const DetailMain = () => {
    const { isDetailPage } = useSelector((r:any) => r.commonReducer);
    const { workspaceId } = useSelector((r:any) => r.workSpaceReducer);
    const { editType } = useSelector((r:any) => r.datasourceReducer);
    const location = useLocation();
    // const [uuid, setUuid] = useState(''); // 添加uuid选项,获取数据库列表需要uuid
    const [dataSourceList, setDataSourceList] = useState<DataSource[]>([]);
    const params = useParams<{ id: string}>();
    useEffect(() => {
        if (isDetailPage) {
            try {
                $http.get('/datasource/page', {
                    workSpaceId: workspaceId,
                    pageNumber: 1,
                    pageSize: 9999,
                }).then((res) => {
                    setDataSourceList(res?.records || []);
                });
            } catch (error) {
                console.log('error', error);
            }
        }
    }, [isDetailPage]);
    const match = useRouteMatch();
    const intl = useIntl();
    const { detailRoutes, visible } = useRoute();
    if (!visible || !detailRoutes.length) {
        return null;
    }
    const detailMenus = detailRoutes.map((item) => ({
        ...item,
        key: item.path.replace(/:id/, (match.params as any).id || ''),
        label: intl.formatMessage({ id: item.path as any }),
    })) as MenuItem[];
    // console.log('menusArray', detailMenus);
    const generateRoute = (menusArray: MenuItem[]) => menusArray.map((route) => (
        // <KeepAlive name={route.path} key={`${route.label}-${route.path}`}>
        <Route
            key={`${route.label}-${route.path}`}
            path={route.path}
            exact={route.exact ? true : undefined}
            component={route.component}
        />
        // </KeepAlive>
    ));
    const history = useHistory();
    const goBack = () => {
        // eslint-disable-next-line no-unused-expressions
        location.pathname.includes('jobs/instance') ? history.goBack() : history.push('/main/home');
    };
    const onChangeDataSource = (id: string) => {
        const url = `${match.path}`.replace(/:id/, id);
        history.push(`${url}/editor`);
    };
    const changeType = () => {
        store.dispatch({
            type: 'save_datasource_editType',
            payload: !editType,
        });
    };
    const renderDataSourcSelect = () => (
        <CustomSelect
            showSearch
            style={{
                width: 240,
            }}
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
    const renderTopContent = () => (
        <div
            className="dv-title-edit"
            style={{
                paddingTop: '20px',
            }}
        >
            <div onClick={goBack} style={{ cursor: 'pointer' }}>
                <ArrowLeftOutlined />
            </div>
            {renderDataSourcSelect()}
            {!location.pathname.includes('jobs') ? (
                <Button style={{ marginLeft: '10px' }} onClick={changeType}>
                    { editType ? intl.formatMessage({ id: 'jobs_Directory' }) : intl.formatMessage({ id: 'jobs_Editor' })}
                </Button>
            ) : ''}

        </div>
    );
    return (

        <MenuLayout menus={detailMenus}>
            {renderTopContent()}
            <div style={{
                display: !location.pathname.includes('jobs') ? 'block' : 'none',
            }}
            >
                <EditorData />
            </div>
            <div style={{
                display: location.pathname.includes('jobs') ? 'block' : 'none',
            }}
            >
                <Jobs />
            </div>
            {/* <Switch>
                {generateRoute(detailMenus)}
            </Switch> */}
        </MenuLayout>

    );
};

export default DetailMain;
