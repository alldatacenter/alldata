import React, { Component } from 'react';
//import _ from 'lodash';
import AceViewer from '../FormBuilder/FormItem/AceViewer';
//import JSONEditor from 'jsoneditor/dist/jsoneditor.min.js';
import 'brace/mode/json';
//import debounce from 'lodash.debounce';
//import './jsoneditor.css';
//import './index.less';


/*
export default class JsonEditor extends Component {

    /!**
     * 验证text是否为合法json
     *!/
    textVerify=(jsonStr)=>{
        const onChange=this.props.onChange;
        try {
            let json=JSON.parse(jsonStr);
            if(json&&onChange){
                onChange(json);
            }
            return true;
        } catch(e) {
            return false;
        }
    };

    componentDidMount() {
        //国际化转换
        let language='';
        if(window.APPLICATION_LANGUAGE){
            if(window.APPLICATION_LANGUAGE.includes("en")){
                language='en';
            } else if(window.APPLICATION_LANGUAGE.includes("zh")){
                language='zh-CN';
            }
        }
        //进行节流控制
        const {onChange,changeInterval,initialized}=this.props;
        const onTextVerify = debounce(this.textVerify, changeInterval||1000);
        const options = {
            onEditable: node => !this.props.readOnly,
            mode: this.props.mode||'view',
            onChangeJSON:(json)=>{
              if(onChange){
                  onChange(json);
              }
            },
            onChangeText:onTextVerify,
            language:language,
            ...this.props.options
        };

        if(_.isObject(this.props.json)) {
            this.editor = new JSONEditor(this.container, options, this.props.json);
        } else if(_.isString(this.props.json)) {
            try {
                this.editor = new JSONEditor(this.container, options, JSON.parse(this.props.json));
            } catch(e) {
                console.error(e);
            }
        }

        if(this.props.mode!=='code'&&this.editor){
            if(this.props.defaultExpand){
                this.editor.expandAll();
            }else{
                this.editor.collapseAll();
            }
        }
        if(this.editor&&initialized){
            initialized(this.editor);
        }
    }

    render() {
        let {style={},theme=localStorage.getItem('sreworks-theme')}=this.props;
        return <div ref={r => this.container = r} className={"jsoneditor_darkTheme"} style={style}/>;
    }
}
*/

export default class JsonEditor extends Component {
    render() {
        let { style = { height: '220px' }, json, onChange, readOnly } = this.props;
        return (
            <AceViewer model={{ defModel: { disableShowDiff: true, mode: "json", ...style } }}
                readOnly={readOnly}
                {...style}
                mode="json"
                value={Object.assign({}, json)}
                onChange={onChange}
            />
        );
    }
}




