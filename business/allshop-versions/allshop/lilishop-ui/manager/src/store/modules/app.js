import {
  otherRouter
} from '@/router/router';
import {
  router
} from '@/router/index';
import Util from '@/libs/util';
import Vue from 'vue';

const app = {
  state: {
    styleStore: "", // 移动端楼层装修中选择风格存储
    added: false, // 加载路由标识
    navList: [], // 顶部菜单
    currNav: "", // 当前顶部菜单name
    currNavTitle: "", // 当前顶部菜单标题
    cachePage: [],
    lang: '',
    pageOpenedList: [{ // 打开过的页面，顶部历史菜单项
      title: '首页',
      path: '',
      name: 'home_index'
    }],
    hotWords:[], //今日热词数据
    // 面包屑数组 左侧菜单
    menuList: [],
    tagsList: [...otherRouter.children], //这块是面包屑的内容不能删除
    routers: [otherRouter],
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
    closePage(state, name) {
      state.cachePage.forEach((item, index) => {
        if (item == name) {
          state.cachePage.splice(index, 1);
        }
      });
    },
    initCachePage(state) {
      if (localStorage.cachePage) {
        state.cachePage = JSON.parse(localStorage.cachePage);
      }
    },
    removeTag(state, name) {
      state.pageOpenedList.map((item, index) => {
        if (item.name == name) {
          state.pageOpenedList.splice(index, 1);
        }
      });
    },
    pageOpenedList(state, get) {
      let openedPage = state.pageOpenedList[get.index];
      if (get.argu) {
        openedPage.argu = get.argu;
      }
      if (get.query) {
        openedPage.query = get.query;
      }
      state.pageOpenedList.splice(get.index, 1, openedPage);
      localStorage.pageOpenedList = JSON.stringify(state.pageOpenedList);
    },
    clearAllTags(state) {
      state.pageOpenedList.splice(1);
      state.cachePage.length = 0;
      localStorage.pageOpenedList = JSON.stringify(state.pageOpenedList);
    },
    clearOtherTags(state, vm) {
      let currentName = vm.$route.name;
      let currentIndex = 0;
      state.pageOpenedList.forEach((item, index) => {
        if (item.name == currentName) {
          currentIndex = index;
        }
      });
      if (currentIndex == 0) {
        state.pageOpenedList.splice(1);
      } else {
        state.pageOpenedList.splice(currentIndex + 1);
        state.pageOpenedList.splice(1, currentIndex - 1);
      }
      let newCachepage = state.cachePage.filter(item => {
        return item == currentName;
      });
      state.cachePage = newCachepage;
      localStorage.pageOpenedList = JSON.stringify(state.pageOpenedList);
    },
    setOpenedList(state) {
      state.pageOpenedList = localStorage.pageOpenedList ? JSON.parse(localStorage.pageOpenedList) : [otherRouter.children[0]];
    },
    setAvatarPath(state, path) {
      localStorage.avatorImgPath = path;
    },
    switchLang(state, lang) {
      state.lang = lang;
      localStorage.lang = lang;
      Vue.config.lang = lang;
    },
    setMessageCount(state, count) {
      state.messageCount = count;
    },
    increateTag(state, tagObj) {
      if (!Util.oneOf(tagObj.name, state.dontCache)) {
        state.cachePage.push(tagObj.name);
        localStorage.cachePage = JSON.stringify(state.cachePage);
      }
      state.pageOpenedList.push(tagObj);
      localStorage.pageOpenedList = JSON.stringify(state.pageOpenedList);
    }
  }
};

export default app;
