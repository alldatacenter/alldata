import React, {
    createContext, useContext, useReducer, useMemo, useRef, useCallback,
} from 'react';

function createUseStore<T>(reducer: (...args: any[]) => T, initState: T) {
    const StateContext = createContext([initState, () => initState] as [T, React.DispatchWithoutAction]);
    const useContextState = () => useContext(StateContext);

    function Provider({ children }: any) {
        const [state, dispatch] = useReducer(reducer, initState);
        const value = useMemo(() => [state, dispatch], [state, dispatch]);
        return (
            <StateContext.Provider value={value as any}>
                {children}
            </StateContext.Provider>
        );
    }
    const useContextActions = (actions: { [key: string]: (...args: any[]) => any }) => {
        const [state, dispatch] = useContextState();
        const fns = useRef(actions);
        fns.current = actions;
        const stateRef = useRef(state);
        stateRef.current = state;
        const getState = useCallback(() => stateRef.current, []);
        const val = useMemo(
            () => Object.keys(fns.current || {}).reduce((prev, key) => {
                // @ts-ignore
                prev[key] = fns.current[key]({ dispatch, getState });
                return prev;
            }, {}),
            [dispatch, getState],
        );
        return val;
    };

    function useActions<K extends { [key: string]:(...args: any[]) => any }>(actions: K) {
        return useContextActions(actions) as Record<keyof K, (...args: any) => void>;
    }

    return {
        useContextState,
        useActions,
        Provider,
    };
}

export default createUseStore;
