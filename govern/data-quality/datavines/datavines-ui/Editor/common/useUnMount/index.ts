import { useEffect } from 'react';

const useUnMount = (fn: (...args: any[]) => any) => {
    useEffect(() => () => {
        fn();
    }, []);
};
export default useUnMount;
