import React from 'react';
import PropTypes from 'prop-types';
import { Layout } from 'antd';
import HeaderContainer from 'views/layouts/HeaderContainer';
import UtilFn from 'js/util.js';
import LoginForm from './components/LoginForm';
const { Content } = Layout;

class NormalLogin extends React.Component {
    constructor(props) {
        super(props);
    }

    succCallback = (username, roleData) => {
        localStorage.setItem('username', username);
        localStorage.setItem('roleData', JSON.stringify(roleData));
        let redirect = localStorage.getItem('pathname4redirect');
        if (!redirect) redirect = '/';
        this.context.router.push(redirect);
    }

    render() {
        return (
            <Layout>
                <HeaderContainer/>
                <Content className="m_login">
                    <LoginForm succCallback={this.succCallback} />
                </Content>
            </Layout>
        );
    }
}

NormalLogin.contextTypes = {
    router: PropTypes.object
}

export default NormalLogin;