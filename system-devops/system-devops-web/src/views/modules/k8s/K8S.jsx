import React from 'react';
import {Layout} from 'antd';
import HeaderContainer from 'views/layouts/HeaderContainer';
import SiderMenu from 'views/layouts/SiderMenu';
import ContentContainer from 'views/layouts/ContentContainer';
import UtilFn from 'js/util.js';
import InterfaceFn from 'js/interface.js';

class K8S extends React.Component {
    constructor(props) {
        super(props);
        this.state = {};
    }
   
    componentDidMount() {
    
    }
    render() {
        let pathnameArr = location.pathname.split('/'),
            current = pathnameArr.length > 2 ? pathnameArr[pathnameArr.length - 1] : pathnameArr[1];
        let searchObj = UtilFn.getLocationSearchObj(location.search);
        return (
            <Layout className="g_layout_fill">
                <SiderMenu currentNav={current}/>
                <Layout>
                    <HeaderContainer/>
                    <ContentContainer
                        style={{
                            // padding: '63px 15px 15px',
                            // background: current == 'query' ? '#f1f1f1' : '#fff',
                            // minWidth: 1000
                        }}
                    >
                        {React.cloneElement(this.props.children, {projectId: Number(searchObj.projectId) || '', appId: Number(searchObj.appId) || ''} )}
                    </ContentContainer>
                </Layout>
            </Layout>
        );
    }
}

export default K8S;