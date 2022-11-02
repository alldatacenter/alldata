/**
 * Created by caoshuaibiao on 2019/3/20.
 * 根据选择展示动态的输入组件
 */
import React, { PureComponent } from 'react';
import FormElementFactory from '../FormElementFactory'
import { Collapse, Checkbox } from 'antd';

const Panel = Collapse.Panel;
const CheckboxGroup = Checkbox.Group;

/*let mdata=[
    {
        value:'blink',label:'blink',
        items:[
            {type:1,name:'version',initValue:'',required:true,label:"版本"},
            {type:1,name:'package',initValue:'',required:true,label:"包名"}
        ]
    },
    {
        value:'hdfs',label:'hdfs',
        items:[
            {type:1,name:'version',initValue:'',required:true,label:"版本"},
            {type:1,name:'package',initValue:'',required:true,label:"包名"}
        ]
    },
    {
        value:'yam',label:'yam',
        items:[
            {type:1,name:'version',initValue:'',required:true,label:"版本"},
            {type:1,name:'package',initValue:'',required:true,label:"包名"}
        ]
    },
    {
        value:'kafka',label:'kafka',
        items:[
            {type:1,name:'version',initValue:'',required:true,label:"版本"},
            {type:1,name:'package',initValue:'',required:true,label:"包名"}
        ]
    },
];*/


class SelectInput extends PureComponent {

    constructor(props) {
        super(props);
        let model = props.model, groupsInputs = [], groupValue = props.value || {};
        if (model.defModel && Object.keys(model.defModel).length > 0) {
            let defMode = model.defModel;
            //选项同输入的定义
            if (defMode.items) {
                let options = model.optionValues;
                options.forEach(option => {
                    let cloneItems = JSON.parse(JSON.stringify(defMode.items));
                    if (option.items) {
                        cloneItems = cloneItems.concat(...option.items)
                    }
                    groupsInputs.push({
                        value: option.value,
                        label: option.label,
                        items: cloneItems
                    });
                })
            } else {
                groupsInputs = defMode;
            }
        }
        let mname = model.name;
        groupsInputs.forEach(gi => {
            //未通过parameter生成需要在此处理
            gi.items.forEach(gii => {
                if (groupValue[gi.value] && groupValue[gi.value][gii.name]) {
                    gii.initValue = groupValue[gi.value][gii.name];
                    //开关类型
                    if (gii.type === 16) {
                        gii.defaultChecked = gii.initValue
                    }
                }
                gii.name = mname + "." + gi.value + "." + gii.name;
            })
        });
        this.state = {
            checks: Object.keys(groupValue),
            groupsInputs: groupsInputs,
            activeKeys: []

        };
    }

    buildingFormElements = () => {
        const formItemLayout = this.props.layout || {
            labelCol: {
                xs: { span: 24 },
                sm: { span: 6 },
            },
            wrapperCol: {
                xs: { span: 24 },
                sm: { span: 12 },
                md: { span: 10 },
            },
        };
        let formChildrens = [];
        let { form } = this.props, { checks, groupsInputs } = this.state;
        groupsInputs.forEach(group => {
            if (checks.includes(group.value)) {
                formChildrens.push(
                    <Panel header={group.label} key={group.value}>
                        {
                            group.items.map(item => {
                                return FormElementFactory.createFormItem(item, form, formItemLayout);
                            })
                        }
                    </Panel>
                )
            }
        });
        return formChildrens;

    };

    handleChecked = (checkedValue) => {
        let { activeKeys, checks } = this.state, newKeys = [];
        checkedValue.forEach(cv => {
            if (!checks.includes(cv)) {
                newKeys = [...activeKeys].concat([cv]);
            }
        });
        this.setState({
            checks: checkedValue,
            activeKeys: newKeys,
        });
        let { onChange } = this.props;
        onChange && onChange(checkedValue);
    };

    handlePanelClick = (keys) => {


    };

    render() {
        let formChildrens = this.buildingFormElements();
        let { checks, groupsInputs, activeKeys } = this.state;
        return (
            <div>
                <div>
                    <CheckboxGroup defaultValue={checks} options={groupsInputs} onChange={this.handleChecked} />
                </div>
                <div>
                    {
                        formChildrens.length > 0 ?
                            <Collapse defaultActiveKey={checks} onChange={this.handlePanelClick}>
                                {formChildrens}
                            </Collapse>
                            : null
                    }
                </div>
            </div>

        )

    }
}

export default SelectInput;
