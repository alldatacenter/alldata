/**
 * @author caoshuaibiao
 * @date 2021/7/9 16:31
 * @Description:嵌入iframe组件
 */
import React, { Component } from 'react';

export default class CustomRender extends Component {
    constructor(props) {
        super(props)
        this.testIframe = React.createRef()
    }
    componentDidMount() {
        this.reinitIframe()
        window.onresize=()=> {
            this.reinitIframe();
        }
    }
    reinitIframe=()=>{
        var iframe = this.testIframe.current;
        let {customHeight} = this.props.widgetConfig;
        if(customHeight) {
            iframe.height = customHeight;
            return false
        }
        try{
            var bHeight = document.documentElement.clientHeight;
            var dHeight = iframe.contentWindow.document.documentElement.scrollHeight;
            var height = Math.max(bHeight, dHeight);
            iframe.height = height - 84;
            }catch (ex){}
        }
    render() {
        let {url,otherConfig}=this.props.widgetConfig;
       return (
            <iframe ref={this.testIframe} frameborder={0} width="100%"  src={url} {...otherConfig}/>
        );
    }
}