/// <reference types="@dcloudio/types" />
/** 当前环境类型 */
export declare type UniPlatforms = 'app-plus' | 'app-plus-nvue' | 'h5' | 'mp-weixin' | 'mp-alipay' | 'mp-baidu' | 'mp-toutiao' | 'mp-qq' | 'mp-360' | 'mp' | 'quickapp-webview' | 'quickapp-webview-union' | 'quickapp-webview-huawei' | undefined;
export declare const PLATFORM: UniPlatforms;
/** 全局对象 */
declare const _uni: UniApp.Uni;
export default _uni;
