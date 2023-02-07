import {createApp, App} from 'vue';
import ElementPlus from 'element-plus';
import {FontAwesomeIcon} from '@fortawesome/vue-fontawesome';
import {installer as CrawlabUI} from '@/package/index';
import AppComp from './App.vue';
import {getStore} from '@/store';
import {getI18n} from '@/i18n';
import {initBaiduTonji} from '@/admin/baidu';
import {importScripts, importStylesheets, initWindowGlobals} from '@/package/utils';
import {getRouter} from '@/router';
// import {initPlugins} from '@/utils/plugin';
import {initRequest} from '@/services/request';
import {initUmeng} from '@/admin/umeng';
import {setGlobalLang} from '@/utils/i18n';
import {track, locate, auth, export_} from '@/directives';
import {initDemo} from '@/demo';

export const getDefaultCreateAppOptions = (): CreateAppOptions => {
  return {
    initStylesheet: true,
    initScripts: true,
    initBaiduTongji: true,
    initUmeng: true,
    initDemo: false,
    loadStore: true,
    loadRouter: true,
    loadElementPlus: true,
    loadCrawlabUI: true,
    loadI18n: true,
    loadFontAwesome: true,
    loadTrack: true,
    loadLocate: true,
    loadAuth: true,
    loadExport: true,
    mount: true,
    store: undefined,
    rootRoutes: undefined,
    routes: undefined,
    allRoutes: undefined,
  };
};

export const normalizeOptions = (options: CreateAppOptions): CreateAppOptions => {
  // if (process.env.VUE_APP_INIT_UMENG === 'false' || window.VUE_APP_INIT_UMENG === 'false') options.initUmeng = false;
  // if (process.env.VUE_APP_INIT_BAIDU_TONGJI === 'false' || window.VUE_APP_INIT_BAIDU_TONGJI === 'false') options.initBaiduTongji = false;
  return options;
};

const _createApp = async (options?: CreateAppOptions): Promise<App> => {
  // merge options
  options = {
    ...getDefaultCreateAppOptions(),
    ...options,
  };

  // normalize options
  options = normalizeOptions(options);

  // import stylesheets
  if (options.initStylesheet) importStylesheets();

  // import scripts
  if (options.initScripts) importScripts();

  // baidu tongji
  if (options.initBaiduTongji) initBaiduTonji();

  // umeng
  if (options.initUmeng) {
    try {
      await initUmeng();
    } finally {
      console.info('initialized umeng');
    }
  }

  // demo
  if (options.initDemo) initDemo();

  // remove loading placeholder
  document.querySelector('#loading-placeholder')?.remove();

  // store
  const store = options.store || getStore();

  // router
  const router = getRouter(options.rootRoutes, options.routes, options.allRoutes, options.createRouterOptions);

  // window globals
  initWindowGlobals();

  // app
  const app = createApp(AppComp);

  // initialize plugins
  // try {
  //   await initPlugins(router, store);
  // } catch (e) {
  //   console.warn(e);
  // }

  // initialize request
  initRequest(router);

  // load modules
  if (options.loadElementPlus) app.use(ElementPlus);
  if (options.loadCrawlabUI) app.use(CrawlabUI);
  if (options.loadStore) app.use(store);
  if (options.loadRouter) app.use(router);
  if (options.loadI18n) {
    app.use(getI18n());
    setGlobalLang(window.localStorage.getItem('lang') as Lang || 'en');
  }
  if (options.loadFontAwesome) app.component('font-awesome-icon', FontAwesomeIcon);
  if (options.loadTrack) app.directive('track', track);
  if (options.loadLocate) app.directive('locate', locate);
  if (options.loadAuth) app.directive('auth', auth);
  if (options.loadExport) app.directive('export', export_);

  // mount
  if (options.mount) app.mount(typeof options.mount === 'string' ? options.mount : '#app');

  return app;
};

export default _createApp;
