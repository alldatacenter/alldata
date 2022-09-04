/**
 * Created by caoshuaibiao on 2020/12/23.
 * 容器模型,是所有内部可以放置widget的父类
 */
import WidgetModel from './WidgetModel';
import Constants from './Constants';

export default class ContainerModel extends WidgetModel {
    /**
     * 包含的子挂件
     * @type {Array}
     */
    widgets = [];

    constructor(containerData) {
        super({});
        this.widgets = [];
        (containerData.elements || containerData.components || []).forEach(widgetData => {
            this.addWidget(new WidgetModel(widgetData));
        });
        this.id = containerData.elementId;
    }


    /**
     * 获取页面上的所有展示组件
     */
    getWidgets() {
        let widgetList = [...this.widgets];
        return widgetList;
    }

    getId() {
        return this.id;
    }

    /**
     * 添加显示单元
     * @param widget
     */
    addWidget(widget) {
        if (!widget) {
            widget = WidgetModel.CREATE_DEFAULT_INSTANCE();
        }
        this.widgets.push(widget);
        widget.setContainerModel(this);
        this.events.emit(Constants.CONTAINER_MODEL_TYPE_WIDGETS_CHANGED);
    }

    /**
     * 移除显示单元
     * @param widget
     */
    removeWidget(widget) {
        this.widgets = this.widgets.filter(w => w != widget);
        this.events.emit(Constants.CONTAINER_MODEL_TYPE_WIDGETS_CHANGED);
    }

    /**
     * 用新的widget替换原有的widget
     * @param oldWidget
     * @param newWidget
     */
    replaceWidget(oldWidget, newWidget) {
        let newWidgets = [];
        for (let w = 0; w < this.widgets.length; w++) {
            let cWidget = this.widgets[w];
            if (cWidget !== oldWidget) {
                newWidgets.push(cWidget)
            } else {
                newWidgets.push(newWidget);
            }
        }
        this.widgets = newWidgets;
        this.events.emit(Constants.CONTAINER_MODEL_TYPE_WIDGETS_CHANGED);
    }

    fromJSON(modelJson) {

    }

    /**
     * 获取页面变量
     * @returns {Array}
     */
    getVariables() {
        return [];
    }

    /**
     * 序列化为json
     * @returns {{widgets}}
     */
    toJSON() {
        let jsonData = super.toJSON(), { } = this;
        let widgetJsonList = this.getWidgets().map(w => w.toJSON());
        return {
            ...jsonData,
            widgets: widgetJsonList
        }
    }


}


