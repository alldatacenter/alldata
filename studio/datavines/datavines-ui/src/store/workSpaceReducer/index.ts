import { Dispatch, Action } from 'redux';
import { useActions } from '../useActions';
import { IWorkSpaceListItem } from '@/type/workSpace';
import { getDefaultWorkspaceId } from '@/utils';

export interface WorkSpaceReducer {
    workspaceId?: any,
    spaceList: IWorkSpaceListItem[],
}
type TActionType =
| 'save_current_space'
| 'save_space_list'
| 'save_batch_space'

type TAction = {
    type?: TActionType,
    payload?: Partial<WorkSpaceReducer>
}
type TDispatch = Dispatch<Action<TAction['type']>>;

const initialState: WorkSpaceReducer = {
    workspaceId: getDefaultWorkspaceId() || undefined,
    spaceList: [],
};

export const workspaceActionsMap = {
    setCurrentSpace: (workspaceId: WorkSpaceReducer['workspaceId']) => (dispatch: TDispatch) => {
        dispatch({
            type: 'save_current_space',
            payload: workspaceId,
        });
    },

    setSpaceList: (spaceList: WorkSpaceReducer['spaceList']) => (dispatch: TDispatch) => {
        dispatch({
            type: 'save_space_list',
            payload: spaceList,
        });
    },
};

export const useWorkSpaceActions = () => useActions(workspaceActionsMap);

const workSpaceReducer = (state: WorkSpaceReducer = initialState, action: TAction = {}) => {
    switch (action.type) {
        case 'save_current_space':
            return { ...state, workspaceId: action.payload };
        case 'save_space_list':
            return { ...state, spaceList: action.payload };
        case 'save_batch_space':
            return { ...state, ...action.payload };
        default:
            return state;
    }
};

export default workSpaceReducer;
