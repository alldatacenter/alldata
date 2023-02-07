import {RouteRecordRaw} from 'vue-router';
import {Store} from 'vuex';

export declare global {
  interface CreateAppOptions {
    initStylesheet?: boolean;
    initScripts?: boolean;
    initBaiduTongji?: boolean;
    initUmeng?: boolean;
    initDemo?: boolean;
    loadStore?: boolean;
    loadRouter?: boolean;
    loadElementPlus?: boolean;
    loadCrawlabUI?: boolean;
    loadI18n?: boolean;
    loadFontAwesome?: boolean;
    loadTrack?: boolean;
    loadLocate?: boolean;
    loadAuth?: boolean;
    loadExport?: boolean;
    mount?: boolean | string;
    store?: Store;
    rootRoutes?: Array<RouteRecordRaw>;
    routes?: Array<RouteRecordRaw>;
    allRoutes?: Array<RouteRecordRaw>;
    createRouterOptions?: CreateRouterOptions;
  }
}
