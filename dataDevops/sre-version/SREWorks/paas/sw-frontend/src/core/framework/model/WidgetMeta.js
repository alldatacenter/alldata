/**
 * Created by caoshuaibiao on 2020/12/4.
 * 挂件的元数据对象。
 */

export default class WidgetMeta {
    /**
     * 唯一标识
     * @type {string}
     */
    id = "";
    /**
     * 类型
     * @type {string}
     */
    type = "";
    /**
     * 组件名称
     * @type {string}
     */
    name = "";
    /**
     * 组件显示的标题
     * @type {string}
     */
    title = "";
    /**
     * 组件的基础信息
     * @type {{author: {name: string, url: string}, description: string, links: Array, logos: {large: string, small: string}, build: {time: string, repo: string, branch: string, hash: string}, screenshots: Array, updated: string, version: string}}
     */
    info = {
        /**
         * 作者
         */
        author: {
            name: "",
            url: ""
        },
        /**
         * 描述
         */
        description: "",
        /**
         * 相关的文档、功能链接、示例配置等
         */
        links: [],
        /**
         * 显示在选择面板中的图标
         */
        logos: {
            large: "",
            small: ""
        },
        /**
         * 构建信息,内置的组件无
         */
        build: {
            time: "",
            repo: "",
            branch: "",
            hash: "",
        },
        /**
         * 截图
         */
        screenshots: [],
        updated: "",
        version: ""
    };
    /**
     * 组件状态信息
     * alpha|beta|deprecated
     * @type {string}
     */
    state = "";
    /**
     * 版本信息
     * @type {string}
     */
    latestVersion = "";
    /**
     * 组件配置项的schema定义,一个组件中最核心的内容。由schema生成组件配置项,最终生成组件实例
     * 值为标准json schema,并增加abm扩展的UI定义规范
     * @type {{}}
     */
    configSchema = {
        /**
         * 默认值
         */
        defaults: {},
        /**
         * 生成属性
         */
        schema: {},
        /**
         * 组件的data的mock数据格式定义,符合Mock规范
         */
        dataMock: {}
    };
    /**
     * 分类
     * @type {string}
     */
    catgory = "";

    constructor(metaData) {
        Object.assign(this, metaData);
    }

}