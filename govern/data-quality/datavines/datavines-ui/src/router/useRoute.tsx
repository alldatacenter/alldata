import React, { useEffect, useState } from 'react';
import { Route } from 'react-router-dom';
import mainRouter from './mainRouter';
import detailRouter from './detailRouter';
import { TRouterItem, TRouter } from './type';

type TMenuObj = {
    visible: boolean,
    routes: TRouterItem[]
    detailRoutes: TRouterItem[]
}

const getRoutes = (routerArray: TRouter) => {
    const array: TRouterItem[] = [];
    const keys = Object.keys(routerArray);
    keys.forEach((element) => {
        array.push(routerArray[element]);
    });
    return array;
};

function useRoute() {
    const [menuObj, setMenuObj] = useState<TMenuObj>({
        visible: false,
        routes: [],
        detailRoutes: [],
    });

    useEffect(() => {
        setMenuObj({
            visible: true,
            routes: getRoutes(mainRouter),
            detailRoutes: getRoutes(detailRouter),
        });
    }, []);

    return menuObj;
}

export const generateMainRoute = (menusArray: TRouterItem[]) => menusArray.map((route) => {
    const exact = Object.prototype.hasOwnProperty.call(route, 'exact') ? route.exact : true;
    return <Route key={`${route.label}-${route.path}`} path={route.path} exact={exact} component={route.component} />;
});

const generateRoute = (menu: TRouterItem): any => {
    // has sub routes
    if (menu.children) {
        return menu.children.map((item) => generateRoute(item));
    }
    const exact = Object.prototype.hasOwnProperty.call(menu, 'exact') ? menu.exact : true;
    const Com = menu.component;
    if (!Com) {
        return null;
    }
    return (
        <Route
            key={menu.path}
            path={menu.path}
            exact={exact}
        >
            <Com />
        </Route>
    );
};
export {
    generateRoute,
};
export default useRoute;
