import { useRef } from 'react';

const usePersistFn = <T extends (...args: any[]) => any>(callback: T) => {
    const fnRef = useRef(callback);
    fnRef.current = callback;
    const persistFn = useRef<T>();
    if (!persistFn.current) {
        persistFn.current = function (...args) {
            fnRef.current(...args);
        } as T;
    }

    return persistFn.current as T;
};

export default usePersistFn;
