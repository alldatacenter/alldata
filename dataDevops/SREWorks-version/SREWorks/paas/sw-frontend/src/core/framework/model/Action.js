/**
 * Created by caoshuaibiao on 2019/1/23.
 * 动作执行实体
 */
import ParameterDefiner from '../../../components/ParameterMappingBuilder/ParameterDefiner';
import localeHelper from '../../../utils/localeHelper';
import actionService from '../../services/actionService';


export default class Action {

    /**
     * 作业类型
     */
    static JOB = 'JOB';
    /**
     * 工单类型
     */
    static ORDER = 'ORDER';
    /**
     * 配置类型
     */
    static CONF = 'CONF';
    /**
     * api类型
     */
    static API = 'API';
    /**
     * 流程类型
     */
    static FLOW = 'FLOW';
    /**
     * 只读类型
     */
    static READ = 'READ';
    /**
     * 表单输出类型
     */
    static OUTPUT = 'OUTPUT';



    constructor(actionData, userInfo, product, runtimeContext) {
        if (userInfo) {
            this.userInfo = userInfo;
        }
        if (product) {
            this.product = product;
        }
        if (runtimeContext) {
            this.runtimeContext = runtimeContext;
        }
        this.initFromJson(actionData);
    }

    initFromJson(actionData) {
        Object.assign(this, actionData);
        this.label = actionData.label;
        this.icon = actionData.icon || 'tool';
        this.id = actionData.id;
        this.parameterDefiner = new ParameterDefiner(actionData.parameterDefiner, this.runtimeContext);

        this.size = actionData.size ? actionData.size : "default";
        this.refresh = actionData.refresh && actionData.refresh[0] ? actionData.refresh[0] : "N";
        this.actionData = actionData;
    }

    /**
     * 获取动作的输入参数
     */
    getInputParamDef() {
        let inputParamDef = this.parameterDefiner.getParameterDef();
        if (this.enableApproval || this.approveEnable) {
            return Promise.all([inputParamDef, this.getChangInputItems(), this.getApproveInputItems()]).then((results) => {
                let dispalyItems = results[0], changeItems = results[1], approveItems = results[2];
                dispalyItems.push(...changeItems);
                dispalyItems.forEach(item => {
                    if (!item.layout) {
                        item.layout = {
                            "category": "操作信息",
                            "span": 24,
                            "order": 0
                        };
                    }
                });
                approveItems.forEach(item => {
                    if (!item.layout && dispalyItems.length) {
                        item.layout = {
                            "category": "审批信息",
                            "span": 24,
                            "order": 0
                        };
                    }
                });
                dispalyItems.push(...approveItems);
                return dispalyItems;
            })
        } else {
            return Promise.all([inputParamDef, this.getChangInputItems()]).then(results => {
                let items = results[0], changeItems = results[1];
                items.push(...changeItems);
                return items;
            });
        }

    }

    /**
     * 能否执行,控制上一个未执行完成不能开始下一次等操作
     */
    canExecute() {
        return this.actionType === Action.OUTPUT;
    }

    /**
     * 获取能否执行信息,包括能否执行以及不能执行原因等
     */
    getExecuteInfo() {
        return {
            executeAble: true,
            info: localeHelper.get('monitor.oam.actionErrorMsg', '有正在执行的活动,请结束上次活动'),
        }
    }

    setDefaultParams(defParams) {
        this.parameterDefiner.updateParametersValue(defParams);
    }

    getApproveInputItems() {
        if (this.enableApproval) {
            return actionService.getApprovalRoles().then(roles => {
                let approveList = this.getApproveList(false, roles);
                this.approveList = approveList;
                let approveText = approveList.map(aprv => aprv.approveTitle + "(" + aprv.approvers.map(ap => ap.showName).join(",") + ")").join(" --> ");
                let approveElements = [], enableCountersign = this.enableCountersign;
                if (enableCountersign && enableCountersign[0] && enableCountersign[0] === "Y") {
                    approveElements.push(
                        { type: 98, name: '__approveCountersign', initValue: "", label: "加签人", required: false, tooltip: "选择的加签人会追加到审批链路后,逐个进行审批" }
                    );
                }
                approveElements.push(
                    {
                        type: 2, name: '__approveComment', initValue: "", label: "审批说明", required: true, inputTip: "本操作启用了审批,请详细填写审批说明,否则可能会被拒绝",
                        defModel: {
                            jsxHint: `<p><label class="ant-empty-normal" style="margin-left: -70px">审批链路:&nbsp;</label>&nbsp;${approveText}</p>`
                        }
                    }
                );
                return approveElements;
            })

        } if (this.approveEnable) {
            let approveElements = [];
            approveElements.push(
                {
                    type: 2, name: '__approveComment', initValue: "", label: "审批说明", required: true, inputTip: "本操作启用了审批,请详细填写审批说明,否则可能会被拒绝",
                    defModel: {}
                }
            );
            //暂时先不显示审批链路,等后续审批服务完善
            return new Promise(
                function (resolve, reject) {
                    return resolve(approveElements);
                }
            );
        } else {
            return new Promise(
                function (resolve, reject) {
                    return resolve([]);
                }
            );
        }

    }

    getChangInputItems() {
        let changeItems = [];
        if (this.planEnable) {
            changeItems.push(
                { type: 5, name: '__actionPlanExecutionTime', label: "计划开始时间", required: true, tooltip: "到指定时间后开始执行" },
                { type: 1, name: '__affectApps', label: "影响应用", required: true, inputTip: "填写影响应用" },
                { type: 1, name: '__affectScope', label: "影响范围", required: true, inputTip: "填写影响范围" },
                { type: 2, name: '__rollbackPlan', label: "回滚计划", required: true, inputTip: "填写回滚计划" },
                { type: 2, name: '__verifyPlan', label: "验证方案", required: true, inputTip: "填写验证方案" },
            );
        }
        return new Promise(
            function (resolve, reject) {
                return resolve(changeItems);
            }
        );
    }

    /**
     * 获取审批链路
     * @param approveGroupName
     * @param roles
     * @returns {Array}
     */
    getApproveList(approveGroupName, roles) {
        let supervisorCode = 'abf36b18-a3e1-4a77-bbd2-ff2d8cb3nn1p', blocking = [], meltdown = [], policies = [];
        let approverList = [], approveSteps = [], emergencyRules;
        //支持多审批流程后需要进行审批流程的选取
        let multiApproves = this.multiApproves;
        if (multiApproves && multiApproves.length > 0) {//存在多审批组的设置时,不存在的情况仍然按照老的方式
            if (multiApproves.length === 1) {//只存在一个直接取第一个
                approveSteps = multiApproves[0].approveSteps;
            } else {
                if (approveGroupName) {//指定审批流程情况
                    let exist = false;
                    multiApproves.forEach(approve => {
                        if (approve.name === approveGroupName) {
                            exist = approve;
                        }
                    });
                    if (exist) {
                        approveSteps = exist.approveSteps;
                        emergencyRules = exist.emergencyRules;
                        if (exist.notifyDingGroup && exist.notifyDingGroup.length > 0) {
                            this.notifyDingGroups = exist.notifyDingGroup;
                        }
                    } else {
                        //不存在指定审批组直接取第一个审批作为默认
                        approveSteps = multiApproves[0].approveSteps;
                        emergencyRules = multiApproves[0].emergencyRules;
                    }
                } else {
                    approveSteps = multiApproves[0].approveSteps;
                    emergencyRules = multiApproves[0].emergencyRules;
                }
            }

        }
        //TODO 熔断封网暂时未实现
        //policies.push(new BlockingPolicy(blocking));
        //policies.push(new MeltdownPolicy(meltdown));
        /*let workDays=emergencyRules.workingDays;
        //添加模板设置的局部策略
        if(workDays){
            policies.push(new NonDailyPolicy(workDays));
        }

        if(emergencyRules.emergencyBeforeHour>0){//大于0代表有限制
            policies.push(new BeforePolicy(emergencyRules.emergencyBeforeHour));
        }
        let emergencyHelper= new EmergencyHelper(policies);
        let emergency=emergencyHelper.isEmergency(new Data());*/
        if (approveSteps && approveSteps.length > 0) {//存在审批设置,若不存在则表示无需审批
            approveSteps.forEach(aps => {
                let approveInfo = false;
                if (aps.rolesCode === supervisorCode) {//主管审批
                    approveInfo = {
                        approveTitle: '提单人主管',
                        approvers: [{
                            emplId: this.userInfo.supervisorEmpId,
                            name: this.userInfo.supervisorName,
                            nickNameCn: this.userInfo.supervisorName,
                            showName: this.userInfo.supervisorName
                        }]
                    };
                } else {//其他从业务角色中找出
                    roles.forEach(role => {
                        if (aps.rolesCode === role.getRolesCode()) {
                            approveInfo = {
                                approveTitle: role.getRolesName(),
                                approvers: role.getMembers()
                            }
                        }
                    })
                }
                //根据审批性质来及单据的实时状态决定是否加入审批列表
                if (approveInfo) {
                    if (aps.approveType === 1) {//通用审批直接加入
                        approverList.push(approveInfo);
                    }/*else if(aps.approveType===2){//紧急审批需要看是否紧急状态再加入
                        if(emergency>0){
                            approverList.push(approveInfo);
                        }
                    }else if(aps.approveType===3){//影响度审批需要查看是否是具有影响度的单子
                        let degree=this.details.affectDegree;
                        if(degree&&degree.level>0){
                            approverList.push(approveInfo);
                        }
                    }*/

                }

            });
            //如果是封网熔断都增加审批控制,报备人为审批人,并且为串行审批
            /*if(this.emergency===1){//封网审批
                blocking.forEach(block=>{
                    if((block.isGlobal()&&this.template.groupId!==8)||block.bizId===this.template.groupId){
                        let checkers=block.getChecker();
                        approverList.push(
                            {
                                approveTitle:'封网期审批',
                                approvers:checkers
                            }
                        );
                    }
                });
            }else if(this.emergency===2){//熔断
                meltdown.forEach(melt=>{
                    if((melt.isGlobal()&&this.template.groupId!==8)||melt.bizId===this.template.groupId){
                        let checkers=melt.getChecker();
                        checkers.forEach(check=>{
                            approverList.push(
                                {
                                    approveTitle:'熔断期审批',
                                    approvers:[check]
                                }
                            );
                        })
                    }
                });
            }*/
            //分析审批列表,把出现的重复审批去掉,规则去掉前面的留后面的
            let hasFlag = false, refApproverList = [];
            approverList.reverse();
            approverList.forEach(ap => {
                hasFlag = false;
                refApproverList.forEach(rap => {
                    if (ap.approvers.length === 1 && rap.approvers.length === 1 && !hasFlag) {
                        hasFlag = (ap.approvers[0].emplId == rap.approvers[0].emplId);
                    }
                });
                if (!hasFlag) {
                    refApproverList.push(ap);
                }
            });
            refApproverList.reverse();
            approverList = refApproverList;

        }
        return approverList;
    }


}
