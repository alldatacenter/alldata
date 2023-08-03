import { useEffect } from 'react';
import { useHistory } from 'react-router-dom';
import { useCommonActions } from '@/store';

// TDL state store
export default () => {
    const { setIsDetailPage } = useCommonActions();
    const history = useHistory();
    useEffect(() => {
        const listenUnmounted = history.listen(() => {
            setIsDetailPage(window.location.href.indexOf('/main/detail') > -1);
        });
        return () => {
            listenUnmounted?.();
        };
    }, []);
};
