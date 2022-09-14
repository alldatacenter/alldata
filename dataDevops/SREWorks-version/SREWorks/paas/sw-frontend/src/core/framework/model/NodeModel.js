/**
 * Created by caoshuaibiao on 2020/11/30.
 * 节点模型定义
 */
import service from '../../services/appMenuTreeService';
import PageModel from './PageModel';
import Constants from './Constants';
import uuidv4 from 'uuid/v4';
import _ from 'lodash';
import { page_template_meta, template_app_id } from '../../../core/designer/editors/TemplateConstant';




export default class NodeModel {

    constructor(nodeData) {
        if (!nodeData.nodeId) {
            throw new Error("节点缺失无法初始化设计器!");
        }
        this.nodeId = nodeData.nodeId;
        this.nodeData = nodeData;
        this.initFromJson(nodeData);
    }

    initFromJson(nodeData) {
        this.blocks = nodeData.blocks || [];
        this.forms = nodeData.forms || [];
        this.pageModel = nodeData.pageData ? new PageModel(nodeData.pageData) : PageModel.CREATE_DEFAULT_INSTANCE();
        this.pageModel.setNodeModel(this);
    }

    load(stageId) {
        if (this.nodeId) {
            return Promise.all([service.getMainPage(this.nodeId, stageId), service.getElements(this.nodeId, stageId)]).then(result => {
                let blocks = [], forms = [];
                result[1].forEach(item => {
                    let { config, ...other } = item, itemData = {};
                    itemData = {
                        ...other,
                        ...config
                    };
                    if (item.type === Constants.BLOCK_TYPE_BLOCK) {
                        blocks.push(itemData);
                    } else if (item.type === Constants.BLOCK_TYPE_FORM) {
                        forms.push(itemData);
                    }
                });
                this.initFromJson({
                    pageData: result[0],
                    blocks: blocks,
                    forms: forms
                });
            })
        } else {
            return new Promise(
                function (resolve, reject) {
                    return reject({ success: false, message: "加载失败" });
                }
            );
        }
    }

    /**
     * 获取节点上的区块和表单分组数据
     */
    getGroupData() {
        return [
            {
                "items": [...this.blocks],
                "label": "页面区块",
                "icon": "",
                "tooltip": "页面复杂的内容区域包装块",
                "type": Constants.BLOCK_TYPE_BLOCK

            },
            /*{
                "items": [...this.forms],
                "label": "表单设计器",
                "icon": "",
                "tooltip":"定义页面表单,是过滤器和操作的依赖",
                "type":Constants.BLOCK_TYPE_FORM

            }*/
        ]
    }


    getBlocks() {
        return this.blocks;
    }

    getForms() {
        return this.forms;
    }

    /**
     * 更新节点上的块
     * @param item
     */
    saveItem(item) {
        let items = item.type === Constants.BLOCK_TYPE_FORM ? this.forms : this.blocks, nodeId = this.nodeId;
        let index = items.findIndex(i => {
            if (i.id === item.id && i.elementId !== item.elementId) {
                item.elementId = i.elementId;
            }
            return i.id === item.id
        });
        items.splice(index, 1, item);
        let elementData = {
            nodeTypePath: nodeId,
            elementId: item.elementId,
            appId: nodeId.split("|")[0],
            type: item.type,
            name: item.id,
            version: 0,
            tags: item.tags,
            config: {
                ...item
            }
        };
        return service.saveElement(elementData).then(result => {
            let { elementId } = result;
            item.elementId = elementId;
            if (elementId) {
                service.attachNode(nodeId, elementId)
            }
        });
    }

    /**
     * 更新页面模型
     */
    updatePageModel() {

    }


    removeItem(item) {
        let items = item.type === Constants.BLOCK_TYPE_FORM ? this.forms : this.blocks;
        let index = items.findIndex(i => {
            return i.id === item.id
        });
        items.splice(index, 1);
        if (!item.elementId) {
            return Promise.resolve({});
        }
        return service.deleteElement({ elementId: item.elementId, nodeTypePath: this.nodeId });
    }


    addItem(item) {
        item.type === Constants.BLOCK_TYPE_FORM ? this.forms.push(item) : this.blocks.push(item);
    }

    /**
     * 获取节点主页面模型
     */
    getPageModel() {
        return this.pageModel;
    }

    savePageModel() {
        let pageData = this.pageModel.toJSON();
        pageData.nodeTypePath = this.nodeId;
        return service.saveMainPage(pageData)
    }

    /**
     * 获取节点参数
     * @returns {Array}
     */
    getVariables() {
        return []
    }

    /**
     * 以新的定义方式生成旧版本的Action定义
     */
    getLegacyActions() {

    }

    /**
     * 由区块标识获取区块
     * @param blockKey
     * @returns {T}
     */
    getBlock(blockKey) {
        return this.blocks.filter(blockMeta => blockMeta.elementId === blockKey || blockMeta.name === blockKey)[0];
    }

    /**
     * 由表单标识获取表单
     * @param formKey
     * @returns {T}
     */
    getForm(formKey) {
        return this.forms.filter(formMeta => formMeta.elementId === formKey)[0];
    }
    // 从模板创建,json序列化替换appId等，规避类对象变为平面对象
    loadFromTemplate(stageId, originNodeTypeId) {
        let originAppId = originNodeTypeId && originNodeTypeId.split("|")[0];
        if (this.nodeId) {
            return Promise.all([service.getMainPage(this.nodeId, stageId), service.getElements(this.nodeId, stageId)]).then(result => {
                let blocks = [], forms = [];
                let clonePageModel = _.cloneDeep(result[0]);
                let pageModelStr = JSON.stringify(clonePageModel);
                result[1].forEach(item => {
                    let newBlockId = uuidv4();
                    item.name = newBlockId;
                    item.id = newBlockId;
                    item.elementId = originAppId + ":BLOCK:" + newBlockId;
                    item.appId = originAppId;
                    let toolbarBlockRegExp = new RegExp(template_app_id + ":BLOCK:" + item.name, 'g');
                    let newToolbarBlockId = originAppId + ":BLOCK:" + newBlockId;
                    let cloneItem = _.cloneDeep(item);
                    let itemStr = JSON.stringify(cloneItem);
                    itemStr = itemStr.replace(toolbarBlockRegExp, newToolbarBlockId);
                    item = JSON.parse(itemStr);
                    pageModelStr = pageModelStr.replace(toolbarBlockRegExp, newToolbarBlockId);
                })
                let replacedPageModel = JSON.parse(pageModelStr)
                let newPageId = uuidv4();
                replacedPageModel.id = newPageId;
                replacedPageModel.tabId = newPageId;
                replacedPageModel.label = newPageId;
                replacedPageModel.name = newPageId;
                replacedPageModel.nodeTypePath = originNodeTypeId;
                result[1].forEach(item => {
                    let { config, ...other } = item, itemData = {};
                    itemData = {
                        ...other,
                        ...config
                    };
                    if (item.type === Constants.BLOCK_TYPE_BLOCK) {
                        blocks.push(itemData);
                    } else if (item.type === Constants.BLOCK_TYPE_FORM) {
                        forms.push(itemData);
                    }
                });
                this.nodeId = originNodeTypeId;
                this.initFromJson({
                    pageData: replacedPageModel,
                    blocks: blocks,
                    forms: forms
                });
            })
        } else {
            return new Promise(
                function (resolve, reject) {
                    return reject({ success: false, message: "加载失败" });
                }
            );
        }
    }
    /**
     * 更新主节点主页面模型,一般用于模板创建
     */

}