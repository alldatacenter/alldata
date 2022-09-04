import React, { PureComponent} from 'react';
import { PlusOutlined } from '@ant-design/icons';
import { Form } from '@ant-design/compatible';
import '@ant-design/compatible/assets/index.css';
import uuid from "uuid/v4";
import DraggableTabs from './DraggableTabs';
import "./FormEditor.less";
import {
    Tabs,
} from 'antd';
import FormItemEditor from './FormItemEditor';
import Parameter from '../../../components/ParameterMappingBuilder/Parameter';
import debounce from 'lodash.debounce';

const TabPane = Tabs.TabPane;

let addSeq = 0;




class FormEditor extends PureComponent {

    constructor(props) {
        super(props);
        let { parameters = [] } = this.props;
        // this.state={activeKey:parameters[0]&&parameters[0].name,parameters};
        parameters.forEach(item => {
            if (!item.onlyKey) {
                item.onlyKey = uuid()
            }
        })
        // this.state={activeKey:'0',parameters};
        this.state = { activeKey: parameters[0] && parameters[0].onlyKey, parameters };
        addSeq = parameters.length;
        this.handleParameterChanged = debounce(this.handleParameterChanged, 2000);
    }

    componentDidMount() {
        this.sortParameters(this.props.parameters)
    }

    onEdit = (targetKey, action) => {
        this[action](targetKey);
    };

    sortParameters = (parameters) => {
        return parameters.sort((a, b) => {
            return a.order - b.order;
        });
    };

    componentWillReceiveProps(nextProps, nextContext) {
        //为了更新order
        if (!_.isEqual(this.props, nextProps)) {
            this.sortParameters(nextProps.parameters)
        }
    }

    add = () => {
        addSeq++;
        let paramName = 'arg-' + addSeq, { onChange } = this.props;
        let defaultParameter = new Parameter({
            label: paramName,
            name: paramName,
            value: '',
        }), { parameters } = this.state;
        defaultParameter.onlyKey = uuid()
        defaultParameter.order = parameters instanceof Array ? parameters.length : 0;
        let newParameters = [...parameters, defaultParameter];
        this.setState({ parameters: newParameters },()=> {
            onChange && onChange(newParameters);
        });
        this.onTabChange(newParameters[newParameters.length - 1].onlyKey)
    };

    remove = (targetKey) => {
        let { parameters } = this.state, { onChange } = this.props;
        let newParameters = parameters.filter((p, index) => p.onlyKey !== targetKey);
        // this.setState({parameters:newParameters,activeKey:newParameters[0]&&newParameters[0].name});
        onChange && onChange(newParameters);
        this.setState({ parameters: newParameters, activeKey: (newParameters[0] && newParameters[0].onlyKey) || '' });
    };

    handleParameterChanged = (parameter, allValues) => {
        if (allValues.name) {
            Object.assign(parameter, allValues);
            let { onChange } = this.props;
            onChange && onChange([...this.state.parameters]);
        }
    };

    onTabChange = (activeKey) => {
        this.setState({ activeKey: activeKey });
    };

    onChangeTab = res => {
        this.setState({
            parameters: res.items,
            activeKey: res.activeKey,
        });
    };
    orderChange=(dragIndex,hoverIndex)=> {
       let { parameters } = this.state,{ onChange } = this.props;
       if (dragIndex>hoverIndex) {
        parameters.forEach((item,index )=> {
            if(index >= hoverIndex && index<dragIndex) {
                item.order = item.order + 1
               }
            if(index === dragIndex) {
                item.order = hoverIndex
            }
        })
       }
       if (dragIndex<hoverIndex) {
        parameters.forEach((item,index )=> {
            if(index > dragIndex && index <= hoverIndex) {
                item.order = item.order -1
               }
            if(index === dragIndex) {
                item.order = hoverIndex
            }
        })
       }
       parameters = this.sortParameters(parameters);
       onChange && onChange(parameters); 
       this.setState({
        parameters: parameters
       })
    }
    render() {
        let { parameters, activeKey } = this.state, panes = [], { title, tabPosition } = this.props;
        parameters.forEach((parameter, index) => {
            panes.push(
                <TabPane tab={parameter.label || parameter.name} key={parameter.onlyKey} closable={true}>
                    <FormItemEditor parameter={parameter} onValuesChange={(changedValues, allValues) => this.handleParameterChanged(parameter, allValues)} />
                </TabPane>
            );
        });
        //替换为可拖拽tab
        return (
            <div className="draggable-form-item">
                <DraggableTabs
                    activeKey={activeKey}
                    tabPosition={tabPosition}
                    hideAdd
                    animated
                    orderChange={(dragIndex,hoverIndex)=>this.orderChange(dragIndex,hoverIndex)}
                    tabBarExtraContent={<div><a onClick={() => this.onEdit(null, 'add')} style={{ marginRight: 12, fontSize: 16}}><PlusOutlined /><span style={{ fontSize: 14}}>添加</span></a></div>}
                    type="editable-card"
                    onEdit={this.onEdit}
                    tabBarGutter={2}
                    onChange={this.onTabChange}
                >
                    {panes}
                </DraggableTabs>
            </div>
        );
    }
}
export default Form.create()(FormEditor);