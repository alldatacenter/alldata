import React from 'react';
//import Particles from 'react-particles-js';
import { connect } from 'react-redux';
import FormWrapper from './formWrapper';
import { message ,Row,Col,} from 'antd';
import  localeHelper from '../../utils/localeHelper';
import properties from '../../properties';

import './index.less';
import logoImg from "./login.png"

function mapStateToProps(state, props) {
  return { ...props };
}


class LoginContainer extends React.Component {
  loginOption
  params
  constructor(props) {
    super(props);
    this.state = {
      loading: false,
      inputLoading: false,
    }
  }
  render() {
    const {platformName, platformLogo} = properties;
    return (
        <div className="login_page">
          {/*<Particles
              className="login-particles"
              style={{ position: 'absolute' }}
              params={{
                "particles": {
                  "number": {
                    "value": 80
                  },
                  "size": {
                    "value": 3
                  }
                },
                "interactivity": {
                  "events": {
                    "onhover": {
                      "enable": true,
                      "mode": "repulse"
                    }
                  }
                }
              }} />*/}
          <div className="datav-login">
            <Row className="datav-rectangle">
              <Col span="12" className="login-left">
                <div><img src={platformLogo} className="logo" /></div>
              </Col>
              <Col span="12" className="login-right">
                <h2 className="login-title">{platformName}</h2>
                <FormWrapper
                    loginOption={this.loginOption}
                    onSubmit={this.onSubmit.bind(this)}
                    submitLoading={this.submitLoading.bind(this)}
                    loading={this.state.inputLoading}
                    layout={{
                      labelCol: { span: 7, offset: 0 },
                      wrapperCol: { span: 23 },
                    }}
                />
              </Col>
            </Row>
          </div>
        </div>
    );
  }
  onSubmit(value) {
      message.info(localeHelper.get('LoginFormSubmitSuccess','登录成功'));
      window.location.reload();
  }

  submitLoading(value) {
    if (value) {
      this.setState({ inputLoading: true });
    } else {
      this.setState({ inputLoading: false });
    }
  }
}
export default connect(mapStateToProps)(LoginContainer );
