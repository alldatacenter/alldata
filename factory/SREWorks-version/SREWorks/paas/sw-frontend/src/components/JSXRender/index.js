/**
 * @author caoshuaibiao
 * @date 2021/5/20 10:45
 * @Description:全站的JSX Render组件
 */

import React, { Component } from 'react';
import JsxParser from 'react-jsx-parser';
import getRenders from "../RenderFactory";

const renders = getRenders();
class JSXRender extends Component {

    static defaultProps = {
        jsx: ""
    };

    render() {
        const { jsx, ...otherProps } = this.props;
        return (
            <JsxParser
                bindings={{ ...otherProps }}
                components={{ ...renders }}
                showWarnings={false}
                jsx={jsx}
                blacklistedAttrs={[]}
            />
        );
    }
}

export default JSXRender;