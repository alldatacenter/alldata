import { useEffect, useRef } from 'react';

type Config = {
  immediate: boolean;
}

const useWatch = <T = any>(dep: T, callback: (...args: any[]) => void, config?: Config) => {
    const initied = useRef(false);

    useEffect(() => {
        if (!initied.current) {
            initied.current = true;
            if (config?.immediate) {
                callback();
            }
        } else {
            callback();
        }
    }, Array.isArray(dep) ? dep : [dep]);
};

export default useWatch;
