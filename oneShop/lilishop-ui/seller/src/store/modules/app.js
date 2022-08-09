import { otherRouter } from '@/router/router';
import { router } from '@/router/index';
import Util from '@/libs/util';
import Cookies from 'js-cookie';
import Vue from 'vue';

const app = {
    state: {
        shipTemplates:"",
        regions:[], //此处是在地区选择器时赋值一次
        styleStore:"", //移动端楼层装修中选择风格存储
        loading: false, // 全局加载动画
        added: false, // 加载路由标识
        navList: [], // 顶部菜单
        currNav: "", // 当前顶部菜单name
        currNavTitle: "", // 当前顶部菜单标题
        cachePage: [], // 缓存的页面
        lang: '',
        isFullScreen: false,
        openedSubmenuArr: [], // 要展开的菜单数组
        menuTheme: 'dark', // 主题
        themeColor: '',
        storeOpenedList: [{
            title: '首页',
            path: '',
            name: 'home_index'
        }],
        currentPageName: '',
        currentPath: [
            {
                title: '首页',
                path: '',
                name: 'home_index'
            }
        ],
        // 面包屑数组 左侧菜单
        menuList: [],
        routers: [
            otherRouter
        ],
        tagsList: [...otherRouter.children],
        messageCount: 0,
        // 在这里定义你不想要缓存的页面的name属性值(参见路由配置router.js)
        dontCache: ['test', 'test']
    },
    mutations: {
        // 动态添加主界面路由，需要缓存
        updateAppRouter(state, routes) {
            state.routers.push(...routes);
            router.addRoutes(routes);
        },
        // 动态添加全局路由404、500等页面，不需要缓存
        updateDefaultRouter(state, routes) {
            router.addRoutes(routes);
        },
        setLoading(state, v) {
            state.loading = v;
        },
        setAdded(state, v) {
            state.added = v;
        },
        setNavList(state, list) {
            state.navList = list;
        },
        setCurrNav(state, v) {
            state.currNav = v;
        },
        setCurrNavTitle(state, v) {
            state.currNavTitle = v;
        },
        setTagsList(state, list) {
            state.tagsList.push(...list);
        },
        updateMenulist(state, routes) {
            state.menuList = routes;
        },
        addOpenSubmenu(state, name) {
            let hasThisName = false;
            let isEmpty = false;
            if (name.length == 0) {
                isEmpty = true;
            }
            if (state.openedSubmenuArr.indexOf(name) > -1) {
                hasThisName = true;
            }
            if (!hasThisName && !isEmpty) {
                state.openedSubmenuArr.push(name);
            }
        },
        closePage(state, name) {
            state.cachePage.forEach((item, index) => {
                if (item == name) {
                    state.cachePage.splice(index, 1);
                }
            });
            localStorage.cachePage = JSON.stringify(state.cachePage);
        },
        initCachepage(state) {
            if (localStorage.cachePage) {
                state.cachePage = JSON.parse(localStorage.cachePage);
            }
        },
        removeTag(state, name) {
            state.storeOpenedList.map((item, index) => {
                if (item.name == name) {
                    state.storeOpenedList.splice(index, 1);
                }
            });
        },
        storeOpenedList(state, get) {
            let openedPage = state.storeOpenedList[get.index];
            if (get.argu) {
                openedPage.argu = get.argu;
            }
            if (get.query) {
                openedPage.query = get.query;
            }
            state.storeOpenedList.splice(get.index, 1, openedPage);
            localStorage.storeOpenedList = JSON.stringify(state.storeOpenedList);
        },
        clearAllTags(state) {
            state.storeOpenedList.splice(1);
            state.cachePage.length = 0;
            localStorage.cachePage = '';
            localStorage.storeOpenedList = JSON.stringify(state.storeOpenedList);
        },
        clearOtherTags(state, vm) {
            let currentName = vm.$route.name;
            let currentIndex = 0;
            state.storeOpenedList.forEach((item, index) => {
                if (item.name == currentName) {
                    currentIndex = index;
                }
            });
            if (currentIndex == 0) {
                state.storeOpenedList.splice(1);
            } else {
                state.storeOpenedList.splice(currentIndex + 1);
                state.storeOpenedList.splice(1, currentIndex - 1);
            }
            let newCachepage = state.cachePage.filter(item => {
                return item == currentName;
            });
            state.cachePage = newCachepage;
            localStorage.cachePage = JSON.stringify(state.cachePage);
            localStorage.storeOpenedList = JSON.stringify(state.storeOpenedList);
        },
        setOpenedList(state) {
            state.storeOpenedList = localStorage.storeOpenedList ? JSON.parse(localStorage.storeOpenedList) : [otherRouter.children[0]];
        },
        setCurrentPath(state, pathArr) {
            state.currentPath = pathArr;
        },
        setCurrentPageName(state, name) {
            state.currentPageName = name;
        },
        setAvatarPath(state, path) {
            localStorage.avatorImgPath = path;
        },
        switchLang(state, lang) {
            state.lang = lang;
            localStorage.lang = lang;
            Vue.config.lang = lang;
        },
        clearOpenedSubmenu(state) {
            state.openedSubmenuArr.length = 0;
        },
        setMessageCount(state, count) {
            state.messageCount = count;
        },
        // 新增页签
        increateTag(state, tagObj) {
            if (!Util.oneOf(tagObj.name, state.dontCache)) {
                state.cachePage.push(tagObj.name);
                localStorage.cachePage = JSON.stringify(state.cachePage);
            }
            state.storeOpenedList.push(tagObj);
            localStorage.storeOpenedList = JSON.stringify(state.storeOpenedList);
        }
    }
};

export default app;
