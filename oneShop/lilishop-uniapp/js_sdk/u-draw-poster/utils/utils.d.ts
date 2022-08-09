import { DrawPosterBuildOpts } from "./interface";
/** 是否是base64本地地址 */
export declare const isBaseUrl: (str: string) => boolean;
/** 是否是小程序本地地址 */
export declare const isTmpUrl: (str: string) => boolean;
/** 是否是网络地址 */
export declare const isNetworkUrl: (str: string) => boolean;
/** 对象target挂载到对象current */
export declare const extendMount: (current: Record<any, any>, target: Record<any, any>, handle?: (extend: Function, target?: Record<any, any> | undefined) => any) => void;
/** 处理构建配置 */
export declare const handleBuildOpts: (options: string | DrawPosterBuildOpts) => {
    selector: string;
    componentThis: any;
    type2d: boolean;
    loading: boolean;
    debugging: boolean;
    loadingText: string;
    createText: string;
    gcanvas: boolean;
};
