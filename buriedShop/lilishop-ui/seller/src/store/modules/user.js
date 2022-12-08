import Cookies from 'js-cookie';

const user = {
    state: {},
    mutations: {
        logout () {
            Cookies.remove('userInfoSeller');
            // 清空打开的页面等数据
            localStorage.clear();
        }
    }
};

export default user;
