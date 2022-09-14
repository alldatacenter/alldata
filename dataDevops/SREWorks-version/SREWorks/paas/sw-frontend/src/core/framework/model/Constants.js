/**
 * Created by caoshuaibiao on 2020/12/3.
 * 前端设计器用到的常量定义
 */

/**
 * 组件默认占几个宽度单位,相对于布局中定义的每行总的宽度单位
 * @type {number}
 */
const WIDGET_DEFAULT_WIDTH = 12;
/**
 * 组件默认包含几个高度单位
 * @type {number}
 */
const WIDGET_DEFAULT_HEIGHT = 16;
/**
 * 组件默认每个高度单位的高 单位px
 * @type {number}
 */
const WIDGET_DEFAULT_ROW_HEIGHT = 12;
/**
 * 组件与组件之间的边距,单位px
 * @type {number}
 */
const WIDGET_DEFAULT_MARGIN = 8;

export default class Constants {

    /**
     * 区块分类
     * @type {string}
     */
    static BLOCK_CATEGORY_ACTION = "ACTION";
    static BLOCK_CATEGORY_PAGE = "PAGE";
    static BLOCK_CATEGORY_FILTER = "FILTER";
    static BLOCK_CATEGORY_FORM = "FORM";
    static BLOCK_CATEGORY_CHART = "CHART";

    /**
     * 节点页面类型
     * @type {string}
     */
    static PAGE_TYPE_ABM_PAGE = "ABM_PAGE";
    static PAGE_TYPE_GRAFANA = "Grafana";
    /**
     * 页面布局类型
     * @type {string}
     */
    static PAGE_LAYOUT_TYPE_CUSTOM = "CUSTOM";
    static PAGE_LAYOUT_TYPE_FLUID = "FLUID";

    /**
     * 工具栏工具类型
     * @type {string}
     */
    static TOOL_TYPE_ADD = "Add Widget";

    /**
     * 容器中挂件改变事件定义
     * @type {string}
     */
    static CONTAINER_MODEL_TYPE_WIDGETS_CHANGED = "Widgets Changed";


    /**
     * widget默认grid尺寸定义
     * @type {number}
     */
    static WIDGET_DEFAULT_WIDTH = WIDGET_DEFAULT_WIDTH;
    static WIDGET_DEFAULT_HEIGHT = WIDGET_DEFAULT_HEIGHT;
    static WIDGET_DEFAULT_ROW_HEIGHT = WIDGET_DEFAULT_ROW_HEIGHT;
    static WIDGET_DEFAULT_MARGIN = WIDGET_DEFAULT_MARGIN;

    static DESIGNER_DEFAULT_PROPS = {
        cols: { lg: WIDGET_DEFAULT_WIDTH, md: WIDGET_DEFAULT_WIDTH, sm: WIDGET_DEFAULT_WIDTH, xs: WIDGET_DEFAULT_WIDTH, xxs: parseInt(WIDGET_DEFAULT_WIDTH * 0.5) },
        rowHeight: WIDGET_DEFAULT_ROW_HEIGHT,
        margin: [WIDGET_DEFAULT_MARGIN, WIDGET_DEFAULT_MARGIN],
        compactType: "vertical",
    };
    /**
     * 未知gridpos定义主要用于未找到grid pos的例外场景
     * @type {{x: number, y: number, h: number, w: number, i: string}}
     */
    static UNKNOWN_GRID_POS = { x: 0, y: 0, h: 29, w: 12, i: 'unknown' };
    /**
     * 区块类型
     * @type {string}
     */
    static BLOCK_TYPE_FORM = "FORM";
    static BLOCK_TYPE_BLOCK = "BLOCK";

    /**
     * 数据源类型定义
     * @type {string}
     */
    static DATASOURCE_API = "API";
    static DATASOURCE_CMDB = "CMDB";
    static DATASOURCE_FUNCTION = "FUNCTION";
    static DATASOURCE_JSONDATA = "JSON_DATA";
    static DATASOURCE_NODEDATA = "NODE_DATA";
    /**
     * 内置的过滤器类型集合定义
     * @type {[*]}
     */
    static FILTERS = ["FILTER_BAR", "FILTER_FORM", "FILTER_MIX", "FILTER_SINGLE", "FILTER_TAB"];
    /**
     * 内置的操作集合定义
     * @type {[*]}
     */
    static ACTIONS = ["ACTION", "STEP_ACTION", "STEP_FORM"];

    static STEP_ACTION = "STEP_ACTION";

    static BLOCK = "BLOCK";
    /**
     * 包装器类型
     * @type {string}
     */
    static CARD_WRAPPER_DEFAULT = "default";
    static CARD_WRAPPER_NONE = "none";
    static CARD_WRAPPER_TRANSPARENT = "transparent";
    static CARD_WRAPPER_TITLE_TRANSPARENT = "title_transparent";
    static CARD_WRAPPER_ADVANCED = "advanced"

    static CARD_WRAPPER_EXSIST = "exist"

    /**
     *
     * @type {string}
     */
    static WIDGET_MODE_EDIT = "edit";
    static WIDGET_MODE_VIEW = "view";
    // 需要在组件内部获取数据的组件类型
    static EXCLUDE_COMP_TYPES = ['TABLE', 'GRID_CARD']
}