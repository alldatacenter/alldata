import userReducer, { UserReducer, useUserActions } from './userReducer';
import commonReducer, { CommonReducer, useCommonActions } from './commonReducer';
import workSpaceReducer, { WorkSpaceReducer, useWorkSpaceActions } from './workSpaceReducer';
import datasourceReducer, { DatasourceReducer, useDatasourceActions } from './datasourceReducer';

export interface RootReducer {
    userReducer: UserReducer;
    commonReducer: CommonReducer,
    workSpaceReducer: WorkSpaceReducer,
    datasourceReducer: DatasourceReducer
}

export {
    useUserActions,
    useCommonActions,
    useWorkSpaceActions,
    useDatasourceActions,
};

export default {
    userReducer,
    commonReducer,
    workSpaceReducer,
    datasourceReducer,
};
