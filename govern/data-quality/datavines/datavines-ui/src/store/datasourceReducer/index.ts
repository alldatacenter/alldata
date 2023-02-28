import { Dispatch, Action } from 'redux';
import { useActions } from '../useActions';

type TTableType = 'CARD' | 'TABLE';

type DsiabledEdit = {
    isTable:boolean
}
export interface DatasourceReducer {
    tableType: TTableType,
    modeType: string,
    entityUuid: string,
    dsiabledEdit: DsiabledEdit | null,
    editType:boolean
}

type TAction = {
    type?: 'save_datasource_type' | 'save_datasource_modeType' | 'save_datasource_entityUuid' | 'save_datasource_dsiabledEdit' | 'save_datasource_editType',
    payload?: Partial<DatasourceReducer>,

}
type TDispatch = Dispatch<Action<TAction['type']>>;

const initialState: DatasourceReducer = {
    tableType: 'CARD',
    modeType: '',
    entityUuid: '',
    dsiabledEdit: null,
    editType: false,
};

export const datasourceActionsMap = {
    setDatasourceType: (tableType: TTableType) => (dispatch: TDispatch) => {
        dispatch({
            type: 'save_datasource_type',
            payload: tableType,
        });
    },
    setDatasourceModeType: (modeType: string) => (dispatch: TDispatch) => {
        dispatch({
            type: 'save_datasource_type',
            payload: modeType,
        });
    },
};

export const useDatasourceActions = () => useActions(datasourceActionsMap);

const datasourceReducer = (state: DatasourceReducer = initialState, action: TAction = {}) => {
    switch (action.type) {
        case 'save_datasource_type':
            return { ...state, tableType: action.payload };
        case 'save_datasource_modeType':
            return { ...state, modeType: action.payload };
        case 'save_datasource_entityUuid':
            return { ...state, entityUuid: action.payload };
        case 'save_datasource_dsiabledEdit':
            return { ...state, dsiabledEdit: action.payload };
        case 'save_datasource_editType':
            return { ...state, editType: action.payload };

        default:
            return state;
    }
};

export default datasourceReducer;
