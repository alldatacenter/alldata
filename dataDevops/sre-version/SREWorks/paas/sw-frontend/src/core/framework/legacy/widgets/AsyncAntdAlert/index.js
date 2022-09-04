
import React, { Component } from 'react';
import { Alert } from 'antd';
import _ from 'lodash';
import {renderTemplateString, stringToObject} from '../../../../../utils/utils'
import JSXRender from "../../../../../components/JSXRender";
import  httpClient from '../../../../../utils/httpClient';

class AsyncAntdAlert extends Component {

    constructor(props) {
        super(props);
        this.state = {
            message:"",
            description:""
        }
    }


    getParams(){
        return Object.assign({} , this.props.nodeParams , stringToObject(this.props.history.location.search))
    }

    url(props) {
        return renderTemplateString(props.url || '', {...props.defaultContext, ...props.nodeParams});
    }

    componentWillMount() {
        httpClient.get(renderTemplateString(_.get(this.props,"mode.config.apiUrl",""), this.getParams())).then(data => {
           this.setState({
            message: renderTemplateString(_.get(this.props,"mode.config.message",""),  Object.assign({}, this.getParams(), data)),
            description: renderTemplateString(_.get(this.props,"mode.config.description",""),  Object.assign({}, this.getParams(), data))
           })
        });
    }

    componentDidUpdate(prevProps) {
        if(this.url(prevProps) !== this.url(this.props) || !_.isEqual(prevProps.nodeParams , this.props.nodeParams)) {
            httpClient.get(renderTemplateString(_.get(this.props,"mode.config.apiUrl",""), this.getParams())).then(data => {
                this.setState({
                    message: renderTemplateString(_.get(this.props,"mode.config.message",""),  Object.assign({}, this.getParams(), data)),
                    description: renderTemplateString(_.get(this.props,"mode.config.description",""),  Object.assign({}, this.getParams(), data))
                })
            });
        }
    }

    renderJSX = jsx => {
        return <JSXRender jsx={jsx}/>
    };

    render() {
        let description = this.state.description;
        if (description) {
            description = this.renderJSX(this.state.description);
        }
        return (
            <Alert
                message={this.renderJSX(this.state.message)}
                description={description}
                type={_.get(this.props, 'mode.config.type')}
                showIcon={_.get(this.props, 'mode.config.showIcon')}
            />
        );
    }
}


export default AsyncAntdAlert;