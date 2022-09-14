/**
 * Created by caoshuaibiao on 2019/9/9.
 * 依赖于public下的screen_loading.css，css样式在其中已经定义
 */
import React, { Component } from 'react';
import properties from '../../properties';
class ABMLoading extends Component {

    constructor(props, context) {
        super(props, context);

    }
    render() {
        const { platformName } = properties
        return (
            <div id="abm-pre-screen-loading">
                <div id="abm-pre-screen-loading-center">
                    <div id="abm-pre-screen-loading-center-absolute">
                        <div className="abm-pre-screen-object" id="abm-pre-screen-object_four" />
                        <div className="abm-pre-screen-object" id="abm-pre-screen-object_three" />
                        <div className="abm-pre-screen-object" id="abm-pre-screen-object_two" />
                        <div className="abm-pre-screen-object" id="abm-pre-screen-object_one" />
                    </div>
                    <h1 className="abm-pre-screen-text">
                        <span>{platformName}</span>
                    </h1>
                    <h1 className="abm-pre-screen-text">
                        <span>{platformName}</span>
                    </h1>
                </div>
            </div>
        );
    }
}


export default ABMLoading;