/**
 * Created by caoshuaibiao on 2019/1/22.
 * 表单元素类型定义
 */

export default class FormElementType {

    /**
     * 基本显示元素类型定义
     */
    /**
     * 图标选择器
     * @type {number}
     */
     static ICON_SELECTOR = 170;
    /**
     * 一般输入
     * 数据格式:{type:1,name:'string',initValue:'',required:true,label:"标题"}
     * @type {number}
     */
    static INPUT = 1;
    /**
   * 增强输入，自拼接前后缀
   * 数据格式:{type:1,name:'string',initValue:'',required:true,label:"标题"}
   * @type {number}
   */
    static HIDDEN_INPUT = 110;
    static DISABLED_INPUT = 112
    static DISABLED_TEXTAREA = 113
    static INPUT_NUMBER = 114
    /**
* 不可见表单
* 数据格式:{type:1,name:'string',initValue:'',required:true,label:"标题"}
* @type {number}
*/
    static ENHANCED_INPUT = 111;
    /**
     * 文本输入
     * 数据格式:{type:2,name:'string',initValue:'',required:true,label:"标题"}
     * @type {number}
     */
    static TEXTAREA = 2;
    /**
     * 下拉单选
     * 数据格式:{type:3,name:'string',initValue:'string',required:true,label:"标题",optionValues:[{value:o,label:o}]}
     * @type {number}
     */
    static SELECT = 3;

    /**
     * 下拉多选
     * 数据格式:{type:4,name:'string',initValue:[],required:true,label:"标题",optionValues:[{value:o,label:o}]}
     * @type {number}
     */
    static MULTI_SELECT = 4;
    /**
     * 日期选择
     * 数据格式:{type:5,name:'planCompleteTime',initValue:this.changeReason,required:true,label:"期望完成时间"}
     * @type {number}
     */
    static DATE = 5;
    /**
     * 日期范围选择
     * 数据格式:{type:6,name:'planCompleteTime',initValue:this.changeReason,required:true,label:"起止时间"}
     * @type {number}
     */
    static DATA_RANGE = 6;
    /**
     * 可输入多选标签
     * 数据格式:{type:6,name:'planCompleteTime',initValue:this.changeReason,required:true,label:"自定义标签",optionValues:[{value:o,label:o}]}
     * @type {number}
     */
    static SELECT_TAG = 7;
    /**
     * 下拉选择树
     * 数据格式:{type:9,name:'tree',initValue:[],required:true,label:"下拉选择",treeData:{
                        value:arr[0],
                        label:arr[0],
                        key:arr[0],
                        children:[]
                    }}
     * @type {number}
     */
    static SELECT_TREE = 9;

    /**
     * radio 单选
     * 数据格式:{type:10,name:'string',initValue:'',required:true,label:"性别",optionValues:[{value:o,label:o}]}
     * @type {number}
     */
    static RADIO = 10;
    /**
     * checkbox多选
     * 数据格式:{type:11,name:'string',initValue:'',required:true,label:"性别",optionValues:[{value:o,label:o}]}
     * @type {number}
     */
    static CHECKBOX = 11;
    /**
     * 时间选择
     * @type {number}
     */
    static TIME_PICKER = 13;
    /**
     * 开关
     * @type {number}
     */
    static SWITCH = 16;
    /**
     * 业务组件元素类型定义
     */
    /**
     * 表单元素分组显示
     * {type:89,name:'groupName',required:true,label:"分组名称",groups:[{name:'group1',items:[${参数定义模型}]}]}
     * @type {number}
     */
    static GROUP_ITEM = 89;

    /**
     * 参数绑定器
     * {type:90,name:'parameterDefiner',required:true,label:"参数绑定器",parameterDefiner:this.parameterDefiner}
     * @type {number}
     */
    static MAPPING_BUILDER = 90;

    /**
     * 机器目标
     * {type:91,name:'targetDefiner',required:true,label:"目标选择器",targetDefiner:this.targetDefiner}
     * @type {number}
     */
    static MACHINE_TARGET = 91;

    /**
     * 自定义集群分组
     * 数据格式:{type:92,name:'',initValue:"",required:true,label:"自定义集群分组",clusterGroups:[],bizId:this.bizId}
     * @type {number}
     */
    static CLUSTER_GROUP = 92;
    /**
     * 工作流自定义步骤
     * {type:93,name:'stepsObj',initValue:"",required:true,label:"详细步骤",steps:this.stepsObj,bizId:this.bizId}
     * @type {number}
     */
    static WORKFLOW = 93;
    /**
     * {type:94,name:'apps',initValue:"",required:true,label:"影响应用"}
     * changfree影响应用
     * @type {number}
     */
    static AFFECT_APPS = 94;
    /**
     * changfree变更类型
     *
     * {type:95,name:'type',initValue:"",required:true,label:"类型"}
     * @type {number}
     */
    static CHANGE_TYPE = 95;
    /**
     * 影响度
     * {type:96,name:'affectDegree',required:true,initValue:"",label:"影响度"}
     * @type {number}
     */
    static AFFECT_DEGREE = 96;
    /**
     * 工作流集群树
     * {type:97,name:'clusters',initValue:initClusters,required:true,label:"目标集群",clustersTree:treeData}
     * @type {number}
     */
    static WORKFLOW_CLUSTER_TREE = 97;
    /**
     * 人员选择
     * {type:98,name:'attention',initValue:this.attention,label:"CF变更关注人"}
     * @type {number}
     */
    static USER_SELECTOR = 98;
    /**
     * 工单时间选择
     * {type:99,name:'execInfo',initValue:"",required:true,label:"执行时间",emergencyHelper:this.emergencyHelper}
     * @type {number}
     */
    static WORKORDER_CALENDAR = 99;
    /**
     * 选择卡片
     * @type {number}
     */
    static GRID_CHECKBOX = 70;
    /**
     * 选择卡片
     * @type {number}
     */
    static MODAL_ACE = 78;
    /**
     * 拾色器
     * @type {number}
     */
    static COLOR_PICKER = 77;
    /**
     * 新建标签
     * @type {number}
     */
    static HANDLE_TAG = 27;
    /**
     *     /**
     * CRON
     * @type {number}
     */
    static CRON = 28;
    /**
    *     /**
    * Dynamic Form
    * @type {number}
    */
    static DYNAMIC_FORM = 29;
    /**
    * 新建标签
    * @type {number}
    */
    static FILE_UPLOAD_NODEFER = 812;
    static FILE_UPLOAD_SINGLE = 813;
    static IMAGE_UPLOAD_MULTI = 814
    static ACEVIEW_JAVASCRIPT = 831;
    /**
     * 全类型标签映射
     * @type {{}}
     */
    static SIMPLE_TYPE_LABEL_MAPPING_OPTIONS = [
        { value: 27, label: '可编辑tag组' },
        { value: 91, label: '机器选择器' },
        { value: 1, label: '普通输入' },
        { value: 114, label: '数字输入' },
        { value: 2, label: '文本输入' },
        { value: 3, label: '下拉单选' },
        { value: 4, label: '下拉多选' },
        { value: 5, label: '日期选择' },
        { value: 6, label: '日期范围' },
        { value: 111, label: '格式文本输入' },
        { value: 7, label: '可输入标签' },
        { value: 12, label: '选择树' },
        { value: 10, label: 'Radio 单选' },
        { value: 11, label: 'CheckBox 多选' },
        { value: 13, label: '时间选择' },
        { value: 14, label: 'Radio 按钮' },
        { value: 15, label: 'Slider 滑条' },
        { value: 16, label: 'Switch 开关' },
        { value: 17, label: '级联单选' },
        { value: 18, label: '密码输入' },
        { value: 98, label: '人员选择' },
        // { value: 87, label: '分组输入' },
        { value: 86, label: 'JSONEditor' },
        { value: 85, label: 'Table' },
        // { value: 84, label: '关联分组' },
        { value: 83, label: 'AceView' },
        { value: 82, label: 'OamWidget' },
        { value: 81, label: '文件上传' },
        { value: 812, label: '多文件上传' },
        { value: 813, label: '单文件上传' },
        { value: 811, label: '上传图片' },
        { value: 79, label: 'SchemaForm' },
        { value: 78, label: '脚本气泡卡片' },
        { value: 70, label: '卡片选择' },
        { value: 110, label: '隐藏式输入框' },
        { value: 28, label: 'cron表达式组件' },
        { value: 29, label: '动态表单项' },
        { value: 814, label: '多图片上传' },
        { value: 170, label: '图标选择器' }
    ];

    constructor() {
        //console.log(this);
    }

}

//const formElementType=new FormElementType();
//export default formElementType;

