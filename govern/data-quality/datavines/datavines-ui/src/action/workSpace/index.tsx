import { $http } from '@/http';
import store, { RootReducer } from '@/store';
import { IWorkSpaceListItem } from '@/type/workSpace';

export const getWorkSpaceList = async (resetWorkspaceId?: boolean) => {
    try {
        const res = (await $http.get<IWorkSpaceListItem[]>('/workspace/list')) || [];
        store.dispatch({
            type: 'save_space_list',
            payload: res,
        });
        const state: RootReducer = store.getState();
        const { workspaceId } = state.workSpaceReducer;
        const findItem = res.find((item) => item.id === workspaceId);
        let bool = (!workspaceId || !findItem);
        if (resetWorkspaceId) {
            bool = true;
        }
        if (res.length && bool) {
            store.dispatch({
                type: 'save_current_space',
                payload: res[0].id,
            });
        }
        return res;
    } catch (error: any) {
    }
    return [];
};
