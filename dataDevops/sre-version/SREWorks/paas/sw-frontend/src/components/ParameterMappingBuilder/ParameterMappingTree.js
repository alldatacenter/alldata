/**
 * Created by caoshuaibiao on 2019/1/22.
 * 映射树
 */
import React, { PureComponent } from 'react';
import { Tree,Tooltip,Tag } from 'antd';
import localeHelper from '../../utils/localeHelper';


const TreeNode = Tree.TreeNode;

class ParameterMappingTree extends PureComponent {

    constructor(props) {
        super(props);
        let defaultCheckeds=[],{parameterDefiner,parameter}=this.props;
        let paramMapping=parameterDefiner.getParamMapping(parameter.name);
        paramMapping.mapping.forEach(m=>{
            m.params.forEach(p=>{
                defaultCheckeds.push(m.operateKey+"/"+p.name);
            });
        });
        this.state = {
            defaultCheckedKeys:defaultCheckeds
        };
    }
    onCheck = (checkedKeys) => {
        let {parameterDefiner,parameter}=this.props;
        //从绑定的参数树上提取已选择的参数片段
        if(checkedKeys.length>0){
            let groupSet={},bindingParamTree=parameterDefiner.bindingParamTree;
            checkedKeys.forEach(ckeys=>{
                let ksp=ckeys.split("/");
                let pgn=ksp[0],pgp=ksp[1];
                if(groupSet[pgn]){
                    groupSet[pgn].names.push(pgp);
                }else{
                    groupSet[pgn]={
                        names:[pgp]
                    };
                }

            });
            //搜索当前选择下的模板的绑定参数
            bindingParamTree.forEach(paramTree=>{
                let selectParamSet=groupSet[paramTree.operateKey];
                if(selectParamSet){
                    let params=paramTree.params,names=selectParamSet.names,selectParams=[];
                    params.forEach(p=>{
                        names.forEach(n=>{
                            if(p.name===n){
                                selectParams.push(p);
                            }
                        })
                    });
                    selectParamSet.node=paramTree;
                    selectParamSet.params=selectParams;
                }
            });
            //根据筛选生成重新组装成新片段
            let genValues=Object.values(groupSet);
            let fragment=[];
            genValues.forEach(gv=>{
                fragment.push({
                    label:gv.node.label,
                    name:gv.node.name,
                    value:gv.node.value,
                    id:gv.node.id,
                    operateKey:gv.node.operateKey,
                    runMode:gv.node.runMode,
                    startMode:gv.node.startMode,
                    params:gv.params
                })
            });
            parameterDefiner.addMapping(parameter.name,fragment)
        }else{
            parameterDefiner.clearParamMapping(parameter.name);
        }
    };
    renderTreeNodes = (paramTree,parentKey) => {
        return paramTree.map((item,index) => {
            if (item.params) {
                return (
                    <TreeNode title={item.label} key={parentKey+item.operateKey} dataRef={item}>
                        {this.renderTreeNodes(item.params,item.operateKey)}
                    </TreeNode>
                );
            }
            return <TreeNode title={<ParamTitle param={item} />} key={parentKey+"/"+item.name} dataRef={item} />;
        });
    };
    render() {
        let {parameterDefiner}=this.props;
        return (
            <Tree
                checkable
                defaultExpandAll
                defaultCheckedKeys={this.state.defaultCheckedKeys}
                onCheck={this.onCheck}
            >
                {this.renderTreeNodes(parameterDefiner.bindingParamTree,"")}
            </Tree>
        );
    }
}

const ParamTitle=({param})=>{
    let scopeTag=null,defaultValueTag=null,paramLabel=param.label;
    if(!param.scope) return <span>{paramLabel}</span>;
    paramLabel=paramLabel.replace("作业参数: ","");
    paramLabel=paramLabel.replace(param.scope+": ","");
    if(param.scope==='__GLOBAL__'){
        scopeTag=(
            <Tooltip placement="topLeft" title={localeHelper.get('paramMap.globalParam',"作业的全局参数")}>
                <Tag color="gold">{localeHelper.get('paramMap.global',"全局")}</Tag>
            </Tooltip>
        )
    }else{
        scopeTag=(
            <Tooltip placement="topLeft" title={param.scope+localeHelper.get('paramMap.stepParam',"步骤参数")}>
                <Tag color="blue">{localeHelper.get('manage.taskplatform.common.step',"步骤")}</Tag>
            </Tooltip>
        )
    }
    if(param.value||param.value===0){
        let deValue=param.value+"";
        defaultValueTag=(
            <Tooltip placement="topLeft" title={localeHelper.get('paramMap.defaultValue',"默认值")+deValue}>
                <Tag color="green">{deValue.substring(0,20)+(deValue.length>20?"...":"")}</Tag>
            </Tooltip>
        )
    }
    return(

        <div style={{display:'inline-flex'}}>
            <div style={{width:200}}>
                <Tooltip placement="topLeft" title={paramLabel}>
                      <span className="text-overflow" style={{maxWidth: 200}}>
                         {paramLabel}
                      </span>
                </Tooltip>
            </div>
            <div>
                {scopeTag}
                {defaultValueTag}
            </div>
        </div>

    )


};

export default ParameterMappingTree;
