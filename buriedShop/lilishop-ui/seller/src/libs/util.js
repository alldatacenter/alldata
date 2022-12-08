import lazyLoading from './lazyLoading.js';
import Cookies from "js-cookie";
import { result } from './routerJson.js';

const config = require('@/config/index')

let util = {

};

util.title = function (title) {
    title = title || `${config.title} 商家后台`;
    window.document.title = title;
};

util.millsToTime = function (mills) {
    if (!mills) {
        return "";
    }
    let s = mills / 1000;
    if (s < 60) {
        return s.toFixed(0) + " 秒"
    }
    let m = s / 60;
    if (m < 60) {
        return m.toFixed(0) + " 分钟"
    }
    let h = m / 60;
    if (h < 24) {
        return h.toFixed(0) + " 小时"
    }
    let d = h / 24;
    if (d < 30) {
        return d.toFixed(0) + " 天"
    }
    let month = d / 30
    if (month < 12) {
        return month.toFixed(0) + " 个月"
    }
    let year = month / 12
    return year.toFixed(0) + " 年"

};

util.inOf = function (arr, targetArr) {
    let res = true;
    arr.forEach(item => {
        if (targetArr.indexOf(item) < 0) {
            res = false;
        }
    });
    return res;
};

util.oneOf = function (ele, targetArr) {
    if (targetArr.indexOf(ele) >= 0) {
        return true;
    } else {
        return false;
    }
};

util.getRouterObjByName = function (routers, name) {
    if (!name || !routers || !routers.length) {
        return null;
    }
    let routerObj = null;
    for (let item of routers) {
        if (item.name == name) {
            return item;
        }
        routerObj = util.getRouterObjByName(item.children, name);
        if (routerObj) {
            return routerObj;
        }
    }
    return null;
};

util.handleTitle = function (vm, item) {
    if (typeof item.title == 'object') {
        return item.title;
    } else {
        return item.title;
    }
};

util.setCurrentPath = function (vm, name) {
    let title = '';
    let isOtherRouter = false;
    vm.$store.state.app.routers.forEach(item => {
        if (item.children.length == 1) {
            if (item.children[0].name == name) {
                title = util.handleTitle(vm, item);
                if (item.name == 'otherRouter') {
                    isOtherRouter = true;
                }
            }
        } else {
            item.children.forEach(child => {
                if (child.name == name) {
                    title = util.handleTitle(vm, child);
                    if (item.name == 'otherRouter') {
                        isOtherRouter = true;
                    }
                }
            });
        }
    });
    let currentPathArr = [];
    if (name == 'home_index') {
        currentPathArr = [
            {
                title: util.handleTitle(vm, util.getRouterObjByName(vm.$store.state.app.routers, 'home_index')),
                path: '',
                name: 'home_index'
            }
        ];
    } else if ((name.indexOf('_index') >= 0 || isOtherRouter) && name !== 'home_index') {
        currentPathArr = [
            {
                title: util.handleTitle(vm, util.getRouterObjByName(vm.$store.state.app.routers, 'home_index')),
                path: '/home',
                name: 'home_index'
            },
            {
                title: title,
                path: '',
                name: name
            }
        ];
    } else {
        let currentPathObj = vm.$store.state.app.routers.filter(item => {
            if (item.children.length <= 1) {
                return item.children[0].name == name;
            } else {
                let i = 0;
                let childArr = item.children;
                let len = childArr.length;
                while (i < len) {
                    if (childArr[i].name == name) {
                        return true;
                    }
                    i++;
                }
                return false;
            }
        })[0];
        if (currentPathObj.children.length <= 1 && currentPathObj.name == 'home') {
            currentPathArr = [
                {
                    title: '首页',
                    path: '',
                    name: 'home_index'
                }
            ];
        } else if (currentPathObj.children.length <= 1 && currentPathObj.name !== 'home') {
            currentPathArr = [
                {
                    title: '首页',
                    path: '/home',
                    name: 'home_index'
                },
                {
                    title: currentPathObj.title,
                    path: '',
                    name: name
                }
            ];
        } else {
            let childObj = currentPathObj.children.filter((child) => {
                return child.name == name;
            })[0];
            currentPathArr = [
                {
                    title: '首页',
                    path: '/home',
                    name: 'home_index'
                },
                {
                    title: currentPathObj.title,
                    path: '',
                    name: currentPathObj.name
                },
                {
                    title: childObj.title,
                    path: currentPathObj.path + '/' + childObj.path,
                    name: name
                }
            ];
        }
    }
    vm.$store.commit('setCurrentPath', currentPathArr);

    return currentPathArr;
};

util.openNewPage = function (vm, name, argu, query) {
    if (!vm.$store) {
        return;
    }
    let storeOpenedList = vm.$store.state.app.storeOpenedList;
    let openedPageLen = storeOpenedList.length;
    let i = 0;
    let tagHasOpened = false;
    while (i < openedPageLen) {
        if (name == storeOpenedList[i].name) { // 页面已经打开
            vm.$store.commit('storeOpenedList', {
                index: i,
                argu: argu,
                query: query
            });
            tagHasOpened = true;
            break;
        }
        i++;
    }
    if (!tagHasOpened) {
        let tag = vm.$store.state.app.tagsList.filter((item) => {
            if (item.children) {
                return name == item.children[0].name;
            } else {
                return name == item.name;
            }
        });
        tag = tag[0];
        if (tag) {
            tag = tag.children ? tag.children[0] : tag;
            if (argu) {
                tag.argu = argu;
            }
            if (query) {
                tag.query = query;
            }
            vm.$store.commit('increateTag', tag);
        }
    }
    vm.$store.commit('setCurrentPageName', name);
};

util.toDefaultPage = function (routers, name, route, next) {
    let len = routers.length;
    let i = 0;
    let notHandle = true;
    while (i < len) {
        if (routers[i].name == name && routers[i].children && routers[i].redirect == undefined) {
            route.replace({
                name: routers[i].children[0].name
            });
            notHandle = false;
            next();
            break;
        }
        i++;
    }
    if (notHandle) {
        next();
    }
};

// 将Csv文件解析为二维数组
export const getArrayFromFile = (file) => {
    let nameSplit = file.name.split('.')
    let format = nameSplit[nameSplit.length - 1]
    return new Promise((resolve, reject) => {
        let reader = new FileReader()
        reader.readAsText(file) // 以文本格式读取
        let arr = []
        reader.onload = function (evt) {
            let data = evt.target.result // 读到的数据
            let pasteData = data.trim()
            arr = pasteData.split((/[\n\u0085\u2028\u2029]|\r\n?/g)).map(row => {
                return row.split('\t')
            }).map(item => {
                return item[0].split(',')
            })
            if (format == 'csv') resolve(arr)
            else reject(new Error('[Format Error]:不是Csv文件'))
        }
    })
}

// 将二维数组转为表格数据
export const getTableDataFromArray = (array) => {
    let columns = []
    let tableData = []
    if (array.length > 1) {
        let titles = array.shift()
        columns = titles.map(item => {
            return {
                title: item,
                key: item
            }
        })
        tableData = array.map(item => {
            let res = {}
            item.forEach((col, i) => {
                res[titles[i]] = col
            })
            return res
        })
    }
    return {
        columns,
        tableData
    }
}

util.initRouter = function (vm) { // 初始化路由
    const constRoutes = [];
    const otherRoutes = [];

    // 404路由需要和动态路由一起加载
    const otherRouter = [{
        path: '/*',
        name: 'error-404',
        meta: {
            title: '404-页面不存在'
        },
        component: 'error-page/404'
    }];
    // 判断用户是否登录
    let userInfo = Cookies.get('userInfoSeller')
    if (!userInfo) {
        // 未登录
        return;
    }

    if (!vm.$store.state.app.added) {
        // 加载菜单
        let menuData = result;
        // 格式化数据，设置 空children 为 null
        for(let i =0;i<menuData.length;i++){
            let t = menuData[i].children
            for(let k = 0;k<t.length;k++){
                let tt = t[k].children;
                for(let z = 0;z<tt.length;z++){
                    tt[z].children = null
                    // 给所有三级路由添加字段，显示一级菜单name，方便点击页签时的选中筛选
                    tt[z].firstRouterName = menuData[i].name
                }
            }
        }
        util.initAllMenuData(constRoutes, menuData);
        util.initRouterNode(otherRoutes, otherRouter);
        // 添加所有主界面路由
        vm.$store.commit('updateAppRouter', constRoutes.filter(item => item.children.length > 0));
        // 添加全局路由
        vm.$store.commit('updateDefaultRouter', otherRoutes);
        // 添加菜单路由
        util.initMenuData(vm, menuData);
        // 缓存数据 修改加载标识
        window.localStorage.setItem('menuData', JSON.stringify(menuData));
        vm.$store.commit('setAdded', true);
    } else {
        // 读取缓存数据
        let data = window.localStorage.getItem('menuData');
        if (!data) {
            vm.$store.commit('setAdded', false);
            return;
        }
        let menuData = JSON.parse(data);
        // 添加菜单路由
        util.initMenuData(vm, menuData);
    }
};

// 添加所有顶部导航栏下的菜单路由
util.initAllMenuData = function (constRoutes, data) {

    let allMenuData = [];
    data.forEach(e => {
        if (e.type == -1) {
            e.children.forEach(item => {
                allMenuData.push(item);
            })
        }
    })
    util.initRouterNode(constRoutes, allMenuData);
}

// 生成菜单格式数据
util.initMenuData = function (vm, data) {
    const menuRoutes = [];
    let menuData = data;
    // 顶部菜单
    let navList = [];
    menuData.forEach(e => {
        let nav = {
            name: e.name,
            title: e.title,
        }
        navList.push(nav);
    })
    if (navList.length < 1) {
        return;
    }
    // 存入vuex
    vm.$store.commit('setNavList', navList);
    let currNav = window.localStorage.getItem('currNav')
    if (currNav) {
        // 读取缓存title
        for (var item of navList) {
            if (item.name == currNav) {
                vm.$store.commit('setCurrNavTitle', item.title);
                break;
            }
        }
    } else {
        // 默认第一个
        currNav = navList[0].name;
        vm.$store.commit('setCurrNavTitle', navList[0].title);
    }
    vm.$store.commit('setCurrNav', currNav);
    for (var item of menuData) {
        if (item.name == currNav) {
            // 过滤
            menuData = item.children;
            break;
        }
    }
    util.initRouterNode(menuRoutes, menuData);
    // 刷新界面菜单
    vm.$store.commit('updateMenulist', menuRoutes.filter(item => item.children.length > 0));

    let tagsList = [];
    vm.$store.state.app.routers.map((item) => {
        if (item.children.length <= 1) {
            tagsList.push(item.children[0]);
        } else {
            tagsList.push(...item.children);
        }
    });
    vm.$store.commit('setTagsList', tagsList);
};

// 生成路由节点
util.initRouterNode = function (routers, data) {  // data为所有子菜单数据

    for (var item of data) {
        let menu = Object.assign({}, item);
        menu.component = lazyLoading(menu.component);

        if (item.children && item.children.length > 0) {
            menu.children = [];
            util.initRouterNode(menu.children, item.children);
        }
        let meta = {};
        // 给页面添加标题
        meta.title = menu.title ? menu.title + " - "+config.title+"商家后台" : null;
        meta.firstRouterName = menu.firstRouterName
        meta.keepAlive = menu.keepAlive ? true : false
        menu.meta = meta;

        routers.push(menu);
    }
};

export default util;
