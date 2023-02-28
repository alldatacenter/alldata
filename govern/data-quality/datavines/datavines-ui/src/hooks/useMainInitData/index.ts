import { useHistory } from 'react-router-dom';
import { useMount } from '@/common';
import shareData from '@/utils/shareData';
import { DV_STORAGE_LOGIN } from '@/utils/constants';
import { ILoginInfo } from '@/store/userReducer';
import { useUserActions } from '@/store';

export default () => {
    const history = useHistory();
    const { setLoginInfo } = useUserActions();
    useMount(() => {
        const loginInfo = shareData.sessionGet(DV_STORAGE_LOGIN) as ILoginInfo;
        if (!loginInfo?.token) {
            history.replace('/login');
            return;
        }
        setLoginInfo(loginInfo || {});
    });
};
