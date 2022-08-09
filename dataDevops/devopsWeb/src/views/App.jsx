import React from 'react';
import {Router, Route, browserHistory, IndexRedirect} from 'react-router'
import Login from './modules/login/Login';

import K8S from './modules/k8s/K8S';
import Project from './modules/k8s/pages/Project';
import MachineManage from './modules/k8s/pages/MachineManage';

import '../sass/main.scss';

const getPathname4redirect = (nextState, replace) => {
    let location = nextState.location;
    localStorage.setItem('pathname4redirect', location.pathname + location.search);
}

// 需权限
const requireRole = (nextState, replace) => {
    let roleData = localStorage.getItem('roleData');
        roleData = roleData ? JSON.parse(roleData) : roleData;
    let pathnameArr = nextState.location.pathname.split('/');
    let flag = true;
    if (roleData.role) {
        // switch(pathnameArr[1]) {
        //     case 'config': 
        //         if (!(roleData.role.authorityRule && roleData.role.authorityRule.includes('manage_authority'))) {
        //             flag = false;
        //         }
        //         break;
        //     default: ;
        // }
    } else {
        flag = false;
    }
    
    if (!flag) {
        replace({
            pathname: '/'
        });
    }
}

class App extends React.Component {
    render() {
        return (
            <Router key={this.props.counter || 0} history={browserHistory}>
                <Route path="/login" component={Login}></Route>
                <Route path="/" onEnter={getPathname4redirect} onChange={getPathname4redirect}>
                    <IndexRedirect to="/k8s/project"/>
                    <Route path="k8s" component={K8S}>
                        <Route path="project" component={Project}/>
                        <Route path="machine" component={MachineManage}/>
                    </Route>
                    
                </Route>
            </Router>
        );
    }
}

export default App;
