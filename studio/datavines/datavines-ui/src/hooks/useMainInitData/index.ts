import { useMount } from '@/common';
import shareData from '@/utils/shareData';
import { DV_STORAGE_LOGIN } from '@/utils/constants';
import { ILoginInfo } from '@/store/userReducer';
import { useUserActions } from '@/store';

export default () => {
    const { setLoginInfo } = useUserActions();
    useMount(() => {
        const loginInfo = shareData.sessionGet(DV_STORAGE_LOGIN) as ILoginInfo;
        setLoginInfo(loginInfo || {});
    });
};
