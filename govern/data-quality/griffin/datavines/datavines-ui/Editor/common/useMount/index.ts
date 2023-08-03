import { useEffect } from 'react';

const useMount = (fn: (...args: any[]) => any) => {
    useEffect(() => {
        fn();
    }, []);
};
export default useMount;
