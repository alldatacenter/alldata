/**
 * Created by caoshuaibiao on 2020/11/30.
 * 页面编辑器
 */
import BaseModel from './BaseModel';
import ContainerModel from './ContainerModel';
import uuidv4 from 'uuid/v4';

export default class PageModel extends BaseModel {

    static CREATE_DEFAULT_INSTANCE() {
        let unKey = uuidv4();
        return new PageModel({
            widgets: [],
            name: unKey,
            id: unKey,
            config: {},
        }
        )
    };

    rootWidgetModel;
    functionMeta;
    esSearch;


    constructor(modelJson) {
        super(modelJson);
        if (modelJson.config && modelJson.config.dataSourceMeta) {
            let { dataSourceMeta } = modelJson.config;
            this.setDataSourceMeta(dataSourceMeta);
        }
        if(modelJson.config && modelJson.config.esSearch) {
            let { esSearch } = modelJson.config;
            this.setEsSearch(esSearch);
        }
        this.rootWidgetModel = new ContainerModel(modelJson, true);
    }


    delete() {


    }


    save(description) {

    }

    getWizardSteps() {
        if (!this.id) {

        }
        return [];
    }

    /**
     * 由页面json数据生成模型对象
     */
    fromJSON() {

    }
    /**
     * 由模型对象序列化为json数据
     */
    toJSON() {
        let jsonData = super.toJSON();
        let { name, id, dataSourceMeta, functionMeta, esSearch } = this;
        return {
            ...jsonData,
            name: name,
            version: 0,
            label: name,
            id: id,
            elements: this.rootWidgetModel.toJSON().widgets,
            config: {
                dataSourceMeta: dataSourceMeta && { ...dataSourceMeta },
                functionMeta: functionMeta || [],
                esSearch: esSearch
            }
        }
    }

    /**
     * 获取模板布局
     * @returns {[*,*,*,*]}
     */
    getLayoutTemplate() {
        return [
            {
                title: '双栏布局',
            },
            {
                title: '多Tab布局',
            },
            {
                title: '折叠布局',
            },
            {
                title: '自定义',
            },
        ];
    }

    /**
     * 通过向导收集的选项信息创建初始页面模型
     * @param wizardInfo
     * @returns {*}
     */
    createFromWizard(wizardInfo) {

    }


    /**
     * 获取根容器模型
     * @returns {*}
     */
    getRootWidgetModel() {
        return this.rootWidgetModel;
    }

    /**
     * 获取包含的widgets
     */
    getWidgets() {
        return this.rootWidgetModel.getWidgets();
    }

    /**
     * 设置页面关联的nodeModel
     * @param nodeModel
     */
    setNodeModel(nodeModel) {
        this.rootWidgetModel.setNodeModel(nodeModel);
        this.rootWidgetModel.getWidgets().forEach(widget => widget.setNodeModel(nodeModel));
        this.nodeModel = nodeModel;
    }

    setFunctionMeta(meta) {
        this.functionMeta = meta;
    }
    getNodeModel() {
        return this.nodeModel;
    }
    setEsSearch(params){
        this.esSearch = params
    }
    getEsSearch() {
        return this.esSearch
    }




}