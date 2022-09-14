import AppService from "../core/services/appService";
import * as util from "../utils/utils";

/**
 * Created by caoshuaibiao on 2020/12/11.
 * 运维桌面相关dav model
 */
export default {
    namespace: 'home',
    state: {
        switchDeleteState: false,
        workspaces: [],
        currentUser: {},
        appStore: [],
        checkedAppStore: {},
        desktopIndex: 0,
        collectList: [],
        customQuickList: [],
        imgList: []
    },
    effects: {

    },
    reducers: {
        setDesktopData(state, { collectList, workspaces, customQuickList, isEqual = false }) {
            let namespaceId = util.getNewBizApp().split(",")[1];
            let stageId = util.getNewBizApp().split(",")[2];
            if (!isEqual) {
                AppService.postWorkspaces({ collectList, workspaces, customQuickList }, namespaceId, stageId)
            }
            return {
                ...state,
                collectList,
                workspaces,
                customQuickList,
            };
        },
        setDesktopIndex(state, { desktopIndex }) {
            return {
                ...state,
                desktopIndex,
            };
        },
        updateCheckedAppStore(state, { checkedAppStore }) {
            return {
                ...state,
                checkedAppStore,
            };
        },
        setWorkspaces(state, { workspaces, collectList, customQuickList }) {
            let namespaceId = util.getNewBizApp().split(",")[1];
            let stageId = util.getNewBizApp().split(",")[2];
            AppService.postWorkspaces({
                collectList: collectList || state.collectList,
                workspaces,
                customQuickList: customQuickList || state.customQuickList,
            }, namespaceId, stageId);
            return {
                ...state,
                workspaces,
            };
        },
        setAppStore(state, { appStore }) {
            return {
                ...state,
                appStore,
            };
        },
        /**
         * 切换图标删除状态
         * @param state
         * @returns {{switchDeleteState: boolean}}
         */
        switchDeleteState(state) {
            return {
                ...state,
                switchDeleteState: !state.switchDeleteState,
            }
        },
        /**
         * 添加桌面
         * @param state
         * @returns {{workspaces: [*,*]}}
         */
        addWorkspace(state, { workspace }) {
            let namespaceId = util.getNewBizApp().split(",")[1];
            let stageId = util.getNewBizApp().split(",")[2];
            AppService.postWorkspaces({
                collectList: state.collectList, workspaces: [...state.workspaces, Object.assign({
                    name: "新建桌面",
                    type: "custom",
                    hasSearch: false,
                    items: [],
                    background: ""
                }, workspace)]
            }, namespaceId, stageId)
            return {
                ...state,
                workspaces: [...state.workspaces, Object.assign({
                    name: "新建桌面",
                    type: "custom",
                    hasSearch: false,
                    items: [],
                    background: ""
                }, workspace)],
            }
        },
        /**
         * 移除/更新桌面
         * @param state
         * @returns {{workspaces: [*,*]}}
         */
        updateWorkspace(state, { workspaces }) {
            let namespaceId = util.getNewBizApp().split(",")[1];
            let stageId = util.getNewBizApp().split(",")[2];
            AppService.postWorkspaces({ collectList: state.collectList, workspaces: workspaces }, namespaceId, stageId)
            return {
                ...state,
                workspaces,
            }
        },
        // 设置桌面背景图片列表
        setImgList(state, { imgList }) {
            return {
                ...state,
                imgList
            }
        }
    },
}