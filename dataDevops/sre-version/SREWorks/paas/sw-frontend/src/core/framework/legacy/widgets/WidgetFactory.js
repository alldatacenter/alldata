
/**
 * Created by caoshuaibiao on 2018/12/8.
 * 信息挂件工厂
 */
import React from 'react';

import SimpleTable from './SimpleTable';
import CompositionTabs from './CompositionTabs';
import CompositionCard from './CompositionCard';
import CommonKvList from './CommonKvList';
//卡片
import GridCard from './GridCard';
//Action表单
import ActionForm from './ActionForm';
import FilterBar from './FilterBar';
import MixFilterBar from './MixFilterBar';
import TabFilter from './TabFilter';
//前端设计器
import WebDesignerWorkbench from './WebDesignerWorkbench';
//flyadmin站点相关功能
import FlyAdminAddPackage from "./flyadmin/flyadmin-add-package"
import FlyAdminAddComponent from "./flyadmin/add-component";
import FlyAdminNamespaceList from "./flyadmin/namespace";
import FlyAdminDeploy from "./flyadmin/flyadmin-deploy";
import FlyAdminGlobalConfiguration from "./flyadmin/global-configuration";
import FlyAdminDeployComDetail from "./flyadmin/flyadmin-deploy/deploy-component-detail"
// 新增基础组件
//flyadmin站点相关功能

//运维桌面
import CompositionWidget from "./CompositionWidget/CompositionWidget";
import AsyncAntdAlert from "./AsyncAntdAlert";
import APITest from '../../core/widgets/APITest';
import Desktop from '../../core/widgets/Desktop'

export default class WidgetFactory {

    static createWidget(mode, widgetProps) {
        let type = mode.type || mode.dataType;
        switch (type) {
            case 'DESIGNER_WORKBENCH':
                return <WebDesignerWorkbench {...widgetProps} mode={mode} />;
            case 'COMPOSITION_CARD':
                return <CompositionCard {...widgetProps} mode={mode} />;
            case 'COMPOSITION':
                return <CompositionWidget {...widgetProps} mode={mode} />;
            case 'TABLE':
                return <SimpleTable {...widgetProps} mode={mode} />;
            case 'GRID_CARD':
                return <GridCard {...widgetProps} mode={mode} />;
            case 'ACTION_FORM':
                return <ActionForm {...widgetProps} mode={mode} />;
            case 'FILTER_BAR':
                return <FilterBar {...widgetProps} mode={mode} />;
            case 'MIX_FILTER_BAR':
                return <MixFilterBar {...widgetProps} mode={mode} />;
            case "FLYADMIN_ADD_PACKAGE":
                return <FlyAdminAddPackage {...widgetProps} mode={mode} />;
            case "FLYADMIN_ADD_COMPONENT":
                return <FlyAdminAddComponent {...widgetProps} mode={mode} />;
            case "FLYADMIN_NAMESPACE_LIST":
                return <FlyAdminNamespaceList  {...widgetProps} mode={mode} />;
            case "FLYADMIN_DEPLOY":
                return <FlyAdminDeploy {...widgetProps} mode={mode} />;
            case "FLYADMIN_GLOBAL_CONFIGURATION":
                return <FlyAdminGlobalConfiguration {...widgetProps} mode={mode} />;
            case "FLYADMIN_DEPLOY_COM_DETAIL":
                return <FlyAdminDeployComDetail {...widgetProps} mode={mode} />
            case "OPS_DESKTOP":
                return <Desktop {...widgetProps} mode={mode} />;
            // case "FLYAMIN_ROLE_SETTING":
            //     return <FlyAdminRoleSetting {...widgetProps} mode={mode} />;
            case "ASYNC_ANTD_ALERT":
                return <AsyncAntdAlert {...widgetProps} mode={mode} />;
            case 'COMPOSITION_TABS':
                return <CompositionTabs {...widgetProps} mode={mode} />;
            case 'COMMON_KV_LIST':
                return <CommonKvList {...widgetProps} mode={mode} />;
            case 'FILTER_TAB':
                return <TabFilter {...widgetProps} mode={mode} />;
            case 'API_TEST':
                return <APITest {...widgetProps} mode={mode}/>;
            default:
                return <span>{type} 未定义</span>;
        }
    }
}