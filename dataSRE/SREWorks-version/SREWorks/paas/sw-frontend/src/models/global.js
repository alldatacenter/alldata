/**
 * Created by caoshuaibiao on 18-8-14.
 * 应用的全局视图模型
 **/
import appService from '../core/services/appService'
import properties from 'appRoot/properties';
import cacheRepository from '../utils/cacheRepository';

let themeType = localStorage.getItem('sreworks-theme') ? localStorage.getItem('sreworks-theme') : 'light';
//let themeType ='dark';
export default {
    namespace: 'global',
    state: {
        // 侧边栏折叠
        siderFold: localStorage.getItem('siderFold') === 'true',
        // 侧边栏响应式
        siderRespons: false,
        menuResponsVisible: false,
        // 主题
        theme: themeType,
        // 菜单布局
        menuMode: 'left',
        // 头部固定
        headerFixed: true,
        siderOpenKeys: [],
        fakeGlobal: false,
        //当前模块
        moduleName: '',
        language: window.APPLICATION_LANGUAGE,
        currentUser: null,
        //所有模块的路由数据
        routes: [],
        //模块所有分组
        moduleGroups: [],
        //可访问资源
        accessRoutes: [],
        //是否已登录标识
        logined: true,
        //系统全局设置项
        settings: false,
        //当前产品
        currentProduct: false,
        //当前的应用 app
        app: null,
        //对外服务提供者
        provider: {},
        //权限点集合
        auths: [],
        loading: true,
        // 全局平台名称
        platformName: 'SREworks',
        platformLogo: '//g.alicdn.com/bcc/bigdata-manager/new_favicon.png',
        btnLoading: false,
        remoteComp: [],
    },
    subscriptions: {
        setup({ dispatch }) {
            //检查用户登录
            dispatch({ type: 'checkLogin' });
        }
    },
    effects: {
        * changeMenuMode({ payload }, { put, select }) {
            // 窗体宽度 < 1600px，菜单模式切换为：左侧
            const isLeftMune = document.body.clientWidth < 1600;
            if (isLeftMune) {
                yield put({ type: 'switchMenuMode', payload: 'left' });
            }
        },
        * siderResponsive({ payload }, { put, select }) {
            const { global } = yield select(state => state);
            const isResponsive = document.body.clientWidth < 992;
            if (isResponsive !== global.siderRespons) {
                yield put({ type: 'userLogged', payload: isResponsive })
            }
        },

        * checkLogin({ payload }, { call, put, select }) {
            const result = yield call(appService.checkLogin);
            if (result) {
                if (result.code === 401) {
                    yield put({
                        type: 'login',
                    });
                } else {
                    yield put({
                        type: 'userLogged',
                        payload: result,
                    });
                    const routes = yield select(state => state.global.routes);
                    //获取当前应用设置
                    const settings = yield call(appService.getApplicationSettings, routes, result);
                    if (settings) {
                        yield put({
                            type: 'initSettings',
                            settings: settings
                        });
                    }

                }
            }

        },

    },
    reducers: {
        hookRoutes(state, { payload }) {
            return {
                ...state,
                ...payload,
                routes: payload.routes
            }
        },
        switchSidebar(state) {
            localStorage.setItem('siderFold', !state.siderFold);
            return {
                ...state,
                siderFold: !state.siderFold,
            }
        },
        switchTheme(state, { theme }) {
            localStorage.setItem('sreworks-theme', theme);
            return {
                ...state,
                theme: theme,
            }
        },
        switchMenuMode(state, { payload }) {
            localStorage.setItem('menuMode', payload);
            return {
                ...state,
                menuMode: payload,
            }
        },
        onRoleChange(state, { productId }) {
            let products = state.settings.products, currentProduct = state.currentProduct;
            products.forEach(product => {
                if (productId === product.productId) {
                    currentProduct = product;
                }
            });
            return {
                ...state,
                currentProduct: currentProduct
            }
        },
        switchSidebarResponsive(state, { payload }) {
            return {
                ...state,
                siderRespons: payload,
            }
        },
        switchMenuPopver(state) {
            return {
                ...state,
                menuResponsVisible: !state.menuResponsVisible,
            }
        },
        switchFakeGlobal(state, { payload }) {
            return {
                ...state,
                fakeGlobal: payload,
            }
        },
        switchHeaderFixed(state, { payload }) {
            localStorage.setItem('headerFixed', !state.headerFixed);
            return {
                ...state,
                headerFixed: !state.headerFixed,
            }
        },
        onModuleMenuChange(state, { payload }) {
            //localStorage.setItem('moduleName', JSON.stringify(payload.moduleName));
            return {
                ...state,
                ...payload,
            }
        },
        onEnvChange(state, { payload }) {
            return {
                ...state,
                ...payload,
            }
        },
        switchLanguage(state, { language }) {
            localStorage.setItem('t_lang_locale', language);
            appService.switchLanguage(language);
            return {
                ...state,
                language: language,
            }
        },

        switchRole(state, { roleId, reloadCurrentPath = false }) {
            let app = state.currentProduct.name;
            cacheRepository.setRole(app, roleId);
            if (!reloadCurrentPath) {
                window.location.href = window.location.href.split("#")[0] + "#/" + app;
            }
            window.location.reload();
            return {
                ...state
            }
        },

        userLogged(state, { payload }) {
            return {
                ...state,
                currentUser: payload,
                logined: true,
            }
        },
        login(state) {
            return {
                ...state,
                logined: false,
            }
        },
        logout(state) {
            appService.logout();
        },
        routesAuth(state, { payload }) {
            let moduleGroups = [], accessRoutes = [], accessKeys = payload.map(resource => resource.key);
            //TODO 监控热升级,运维管理 暂时没有权限,全部都有,后续需要全部由后台返回
            accessKeys.push('upgrade-manage', 'operation', 'taskplatform', 'operation-history');
            state.routes.forEach(module => {
                //分组暂时未启用先不做处理
                if (module.group && !moduleGroups.includes(module.group)) {
                    moduleGroups.push({ group: module.group, defaultLink: module.path });
                }
                //因旧版本只对管理进行了是否显示控制,因此先硬编码写死,只控制一层,全新重构后支持多级,如果开发过程中前端全量功能调试,可以把路由全部赋值到可访问路由即可
                if (module.name === 'Manage') {
                    module.children = module.children.filter(c => accessKeys.includes(c.name));
                }
                if (module.name === 'Portal') {//大小专有云管控,其他没有临时过渡方案
                    if (properties.envFlag === 'DXZ') {
                        //过滤产品
                        accessRoutes.push(module);
                    }
                } else {
                    accessRoutes.push(module);
                }
            });


            return {
                ...state,
                ...payload,
                accessRoutes: accessRoutes,
                moduleGroups: moduleGroups
            }
        },

        initSettings(state, { settings }) {
            return {
                ...state,
                settings: settings,
                currentProduct: settings.currentProduct,
                accessRoutes: settings.accessRoutes,
                auths: settings.auths,
                loading: false
            }
        },

        showLoading(state, { payload }) {
            return {
                ...state,
                loading: true
            }
        },

        // 修改全局配置平台名称和logo
        setplatformNameAndLogo(state, { payload }) {
            return {
                ...state,
                ...payload
            }
        },
        resetLogo(state, { payload }) {
            return {
                ...state,
                ...payload
            }
        },
        // 前端设计器，表单编辑
        switchBtnLoading(state, { payload }) {
            return {
                ...state,
                ...payload
            }
        },
        // 挂载远程组件列表到window
        assignRemoteComp(state, { payload }) {
            return {
                ...state,
                ...payload
            };
        }
    },
}
