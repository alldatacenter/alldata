import React, { Suspense, useState } from 'react';
import useRoute from 'src/router/useRoute';
import { Route, Switch } from 'react-router-dom';
import { useIntl } from 'react-intl';
import { Spin } from 'antd';
import MenuLayout from '@/component/Menu/Layout';
import { MenuItem } from '@/component/Menu/MenuAside';
import { useDetailPage, useMainInitData } from '@/hooks';
import { useMount } from '@/common';
import { useSelector } from '@/store';
import { getWorkSpaceList } from '@/action/workSpace';

const Main = () => {
    const { isDetailPage } = useSelector((r) => r.commonReducer);
    useMainInitData();
    useDetailPage();
    const [visible, setVisible] = useState(false);
    const { routes } = useRoute();
    const intl = useIntl();

    useMount(async () => {
        await getWorkSpaceList();
        setVisible(true);
    });

    if (!visible || !routes.length) {
        return null;
    }
    const menus = routes.map((item) => ({
        ...item,
        label: intl.formatMessage({ id: item.path as any }),
    })) as MenuItem[];

    const generateRoute = (menusArray: MenuItem[]) => menusArray.map((route) => (
        <Route
            key={`${route.label}-${route.path}`}
            path={route.path}
            exact={route.exact ? true : undefined}
            component={route.component}
        />
    ));

    return (
        <MenuLayout visible={!isDetailPage} menus={menus}>
            <Suspense fallback={(
                <Spin style={{
                    width: '100vw', height: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center',
                }}
                />
            )}
            >
                <Switch>
                    {generateRoute(menus)}
                </Switch>
            </Suspense>

        </MenuLayout>
    );
};

export default Main;
