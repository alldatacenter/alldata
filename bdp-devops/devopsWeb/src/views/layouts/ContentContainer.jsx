import React from 'react';
import PropTypes from 'prop-types';
import { Layout, Icon } from 'antd';


class ContentContainer extends React.Component {
	constructor(props) {
        super(props);
        this.state = {
        };
    }

    render() {
    	// 根据children的类型复制children
    	let children = this.props.children;
        return (
            <Layout
            	id="mainbody"
            	className="g_body"
                style={this.props.style || {}}
            >
                { children }
            </Layout>
        );
    }
}

ContentContainer.propTypes = {
    hasSidebar: PropTypes.bool
}

export default ContentContainer;