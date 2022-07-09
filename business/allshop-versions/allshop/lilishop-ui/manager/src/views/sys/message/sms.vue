<template>
  <div class="search">
      <Card>
        <Tabs value="LIST" @on-click="paneChange">
          <TabPane label="发送任务列表" name="LIST">
            <Row class="operation" style="margin-bottom: 10px">
              <Button @click="sendBatchSmsModal" type="primary">发送短信</Button>
            </Row>
            <Table :loading="loading" border :columns="smsColumns" :data="smsData" ref="table">
            </Table>

            <Row type="flex" justify="end" class="mt_10">
              <Page :current="smsSearchForm.pageNumber" :total="smsTotal" :page-size="smsSearchForm.pageSize" @on-change="smsChangePage" @on-page-size-change="smsChangePageSize"
                :page-size-opts="[10, 20, 50]" size="small" show-total show-elevator show-sizer></Page>
            </Row>
          </TabPane>
          <TabPane label="短信模板" name="TEMPLATE">
            <Row class="operation" style="margin-bottom: 10px">
              <Button @click="addTemplate" type="primary">添加短信模板</Button>
              <Button @click="syncTemplate" type="info">同步</Button>
            </Row>
            <Table :loading="loading" border :columns="templateColumns" :data="templateData" ref="table">
            </Table>
            <Row type="flex" justify="end" class="mt_10">
              <Page :current="templateSearchForm.pageNumber" :total="templateTotal" :page-size="templateSearchForm.pageSize" @on-change="templateChangePage"
                @on-page-size-change="templateChangePageSize" :page-size-opts="[10, 20, 50]" size="small" show-total show-elevator show-sizer></Page>
            </Row>
          </TabPane>
          <TabPane label="短信签名" name="SIGN">
            <Row class="operation" style="margin-bottom: 10px">
              <Button @click="addSign" type="primary">添加短信签名</Button>
              <Button @click="syncSign" type="info">同步</Button>
            </Row>
            <Table :loading="loading" border :columns="signColumns" :data="signData" ref="table">
              <template slot="signStatus" slot-scope="scope">
                <div v-if="scope.row.signStatus ==2 ">
                  审核拒绝
                  <Poptip trigger="hover" :content=scope.row.reason placement="top-start" transfer>
                    <span style="color: #ed3f14">【原因】</span>
                  </Poptip>
                </div>
                <div v-if="scope.row.signStatus ==0 ">
                  审核中
                </div>
                <div v-if="scope.row.signStatus ==1 ">
                  审核通过
                </div>
              </template>
            </Table>
            <Row type="flex" justify="end" class="mt_10">
              <Page :current="signSearchForm.pageNumber" :total="signTotal" :page-size="signSearchForm.pageSize" @on-change="signChangePage" @on-page-size-change="signChangePageSize"
                :page-size-opts="[10, 20, 50]" size="small" show-total show-elevator show-sizer></Page>
            </Row>
          </TabPane>
        </Tabs>
      </Card>
    <Modal :title="templateModalTitle" v-model="templateModalVisible" :mask-closable="false" :width="500">
      <Form ref="templateForm" :model="templateForm" :label-width="100" :rules="templateFormValidate">
        <FormItem label="模板名称" prop="templateName">
          <Input v-model="templateForm.templateName" maxlength="30" clearable style="width: 90%" placeholder="请输入模板名称，不超过30字符" />
        </FormItem>
        <FormItem label="模板内容" prop="templateContent">
          <Input v-model="templateForm.templateContent" clearable type="textarea" style="width: 90%" maxlength="200" :autosize="{maxRows:5,minRows: 5}" show-word-limit
            placeholder="请输入短信内容，不超过500字符，不支持【】、★、 ※、 →、 ●等特殊符号；" />
        </FormItem>
        <FormItem label="申请说明" prop="remark">
          <Input v-model="templateForm.remark" clearable type="textarea" style="width: 90%" maxlength="150" :autosize="{maxRows:4,minRows: 4}" show-word-limit
            placeholder="请描述您的业务使用场景，不超过100字符；如：用于春节集五福" />
        </FormItem>
      </Form>
      <div slot="footer">
        <Button type="text" @click="templateModalVisible = false">取消</Button>
        <Button type="primary" :loading="submitLoading" @click="templateSubmit">提交
        </Button>
      </div>
    </Modal>
    <Modal :title="sendSmsModalTitle" v-model="sendSmsModal" :mask-closable="false" :width="900">
      <div class="send-setting">
        <div class="left-show" v-html="smsContent">
          <div class="sms">效果预览</div>
        </div>
        <div class="send-form">
          <Form ref="smsForm" :model="smsForm" :label-width="100" :rules="smsFormValidate">
            <FormItem label="短信签名" prop="signName">
              <Select @on-change="selectSmsSign" v-model="smsForm.signName" style="width: 35%;">
                <Option v-for="item in smsSigns" :value="item.signName" :key="item.signName">{{ item.signName }}
                </Option>
              </Select>
            </FormItem>
            <FormItem label="短信模板" prop="messageCode">
              <Select @on-change="selectSmsTemplate" v-model="smsForm.messageCode" style="width: 35%;">
                <Option v-for="(item,index) in smsTemplates" :key="index" :value="item.templateCode">{{
                  item.templateName }}
                  <input type="hidden"></input>
                </Option>
              </Select>
            </FormItem>
            <FormItem label="短信内容" prop="context">
              <Input v-model="smsForm.context" clearable type="textarea" style="width: 90%" maxlength="50" :autosize="{maxRows:4,minRows: 4}" show-word-limit disabled />
            </FormItem>
            <FormItem label="接收人" prop="smsRange">
              <p>
                已选<span style="color: #f56c1d"> {{memberNum}}</span>人，预计耗费条数<span style="color: #f56c1d">{{this.smsForm.num}}条</span>
              </p>
              <RadioGroup type="button" button-style="solid" @on-change="smsRangeChange" v-model="smsForm.smsRange">
                <Radio label="1">全部会员</Radio>
                <Radio label="2">自定义选择</Radio>
              </RadioGroup>
            </FormItem>
            <FormItem>
              <div class="choose-member" v-if="customSms">
                <div class="source-member">
                  <Input suffix="ios-search" @on-enter="memberSearch" v-model="memberSearchParam.mobile" @on-blur="memberSearch" placeholder="请输入手机号码" style="width: 92%" />
                  <div style="margin-top: 5px">
                    <Scroll :on-reach-bottom="memberSearchEdge">

                      <div dis-hover v-for="(item, index) in members" :key="index" class="scroll-card">
                        <Button class="btns" :class="{'active':item.____selected}" @click="moveMember(index,item)" style="width: 100%;text-align: left">
                          <span v-if="item.mobile" class="mobile">
                            {{item.mobile}}
                          </span>
                          <span class="nickname">
                            {{  item.nickName}}
                          </span>
                        </Button>

                      </div>
                    </Scroll>
                  </div>
                </div>
                <div class="traget-member" style="overflow:auto">
                  <div v-for="(item, index) in alreadyCheckShow" :key="index"  >
                    <Tag class="checkbox-tag" closable @on-close="alreadyCheckClose(item,index)">{{item.mobile ||
                      item.nickName}}
                    </Tag>
                  </div>
                </div>
              </div>
            </FormItem>
          </Form>
        </div>

      </div>
      <div slot="footer">
        <Button type="text" @click="sendSmsModal = false">取消</Button>
        <Button type="primary" :loading="submitLoading" @click="sendSms">发送</Button>
      </div>
    </Modal>
  </div>
</template>


<script>
import * as API_Setting from "@/api/setting.js";
import * as API_Member from "@/api/member.js";

export default {
  name: "sms",
  components: {},

  data() {
    return {
      loading:false,
      customSms: false, //当选择自动发送对象 展示
      alreadyCheck: [], //已经选中的数据
      alreadyCheckShow: [], //已经选择的值负责显示
      memberPage: 0, // 会员信息
      members: [], //所有会员
      smsTemplateContent: "", //短信模板内容
      memberNum: 0, //会员总数
      smsContent: "<div class='sms'>效果预览</div>", //短信内容
      smsTemplates: [], //短信模板
      smsSigns: [], //短信签名
      sendSmsModal: false, //弹出发送短信模态框
      sendSmsModalTitle: "短信发送", //发送短信模态框标题
      modalType: 0, // 添加或编辑标识
      templateModalVisible: false, //添加短信模板弹出框
      templateModalTitle: "", //添加短信模板弹出框标题
      templateForm: {}, //短信模板添加form
      submitLoading: false, // 提交加载状态
      signSearchForm: {
        // 搜索框初始化对象
        pageNumber: 1, // 当前页数
        pageSize: 10, // 页面大小
      },
      //短信模板查询form
      templateSearchForm: {
        pageNumber: 1, // 当前页数
        pageSize: 10, // 页面大小
      },
      //会员条数查询form
      memberSearchFrom: {
        disabled: "OPEN", // 会员状态
      },
      //会员查询条件
      memberSearchParam: {
        pageNumber: 1, // 当前页数
        pageSize: 8, // 页面大小
        disabled: "OPEN", // 会员状态
      },
      //短信记录查询form
      smsSearchForm: {
        sort: "createTime",
        order: "desc",
        pageNumber: 1, // 当前页数
        pageSize: 10, // 页面大小
      },
      smsForm: { // 短信模板数据
        smsName: "",
        signName: "",
        context: "",
        smsRange: "1",
        num: 0,
        messageCode: "",
      }, //短信发送form
      //短信发送参数校验
      smsFormValidate: {
        signName: [
          {
            required: true,
            message: "请选择短信签名",
            trigger: "blur",
          },
        ],
        messageCode: [
          {
            required: true,
            message: "请选择短信模板",
            trigger: "blur",
          },
        ],
      },
      templateFormValidate: {
        templateName: [
          {
            required: true,
            message: "请输入短信模板名称",
            trigger: "blur",
          },
        ],
        templateContent: [
          {
            required: true,
            message: "请输入短信模板内容",
            trigger: "blur",
          },
        ],
        remark: [
          {
            required: true,
            message: "请输入短信模板申请说明",
            trigger: "blur",
          },
        ],
      },
      smsColumns: [
        {
          title: "模板名称",
          key: "smsName",
          width: 250,
        },
        {
          title: "签名",
          width: 150,
          key: "signName",
        },
        {
          title: "短信内容",
          minWidth: 300,
          key: "context",
          tooltip: true,
        },
        {
          title: "预计发送条数",
          key: "num",
          width: 140,
        },
        {
          title: "操作",
          key: "action",
          align: "center",
          width: 150,
          render: (h, params) => {
            return h("div", [
              h(
                "Button",
                {
                  props: {
                    type: "primary",
                    size: "small",
                  },
                  style: {
                    marginRight: "5px",
                  },
                  on: {
                    click: () => {
                      this.detail(params.row);
                    },
                  },
                },
                "详细"
              ),
            ]);
          },
        },
      ],
      smsData: [], // 表单数据
      smsTotal: 0, // 表单数据总数
      templateColumns: [
        {
          title: "模板code",
          key: "templateCode",
        },
        {
          title: "模板名称",
          key: "templateName",
        },
        {
          title: "模板内容",
          key: "templateContent",
        },
        {
          title: "状态",
          key: "templateStatus",
          headerAlign: "center",
          Width: "100px",
          render: (h, params) => {
            if (params.row.templateStatus == 0) {
              return h("div", {}, "审核中");
            } else if(params.row.templateStatus == 1){
              return h("div", {}, "审核通过");
            } else {
              return h("div", {}, "审核失败");
            }
          },
        },
        {
          title: "操作",
          key: "action",
          fixed: "right",
          width: 200,
          align: "center",
          render: (h, params) => {
            return h("div", [
              h(
                "Button",
                {
                  props: {
                    type: "info",
                    size: "small"
                  },
                  style: {
                    marginRight: "5px",
                  },
                  attrs: {
                    disabled: params.row.templateStatus == 2 ? false : true,
                  },
                  on: {
                    click: () => {
                      this.editTemplate(params.row);
                    },
                  },
                },
                "编辑"
              ),
              h(
                "Button",
                {
                  props: {
                    type: "error",
                    size: "small",
                  },
                  style: {
                    marginRight: "5px",
                  },
                  attrs: {
                    disabled: params.row.templateStatus == 0 ? true : false,
                  },
                  on: {
                    click: () => {
                      this.deleteSmsTemplate(params.row);
                    },
                  },
                },
                "删除"
              ),
            ]);
          },
        },
      ],
      templateData: [], // 表单数据
      templateTotal: 0, // 表单数据总数
      signColumns: [
        {
          title: "签名名称",
          key: "signName",
        },
        {
          title: "申请说明",
          key: "remark",
        },
        {
          title: "状态",
          key: "signStatus",
          headerAlign: "center",
          Width: "100px",
          slot: "signStatus",
        },
        {
          title: "操作",
          key: "action",
          fixed: "right",
          width: 200,
          align: "center",
          render: (h, params) => {
            return h("div", [
              h(
                "Button",
                {
                  props: {
                    type: "info",
                    size: "small",
                    icon: "ios-create-outline",
                  },
                  attrs: {
                    disabled: params.row.signStatus == 2 ? false : true,
                  },
                  style: {
                    marginRight: "5px",
                  },
                  on: {
                    click: () => {
                      this.editSign(params.row);
                    },
                  },
                },
                "编辑"
              ),
              h(
                "Button",
                {
                  props: {
                    type: "error",
                    size: "small",
                    icon: "ios-create-outline",
                  },
                  attrs: {
                    disabled: params.row.signStatus == 0 ? true : false,
                  },
                  style: {
                    marginRight: "5px",
                  },
                  on: {
                    click: () => {
                      this.deleteSmsSign(params.row);
                    },
                  },
                },
                "删除"
              ),
            ]);
          },
        },
      ],
      signData: [], // 表单数据
      signTotal: 0, // 表单数据总数
    };
  },
  methods: {
    // 初始化数据
    init() {
      this.getSms();
      //查询会员总数
      this.getMemberNum();
    },
    //查询会员条数
    getMemberNum() {
      API_Member.getMemberNum(this.memberSearchFrom).then((res) => {
        this.loading = false;
        if (res.success) {
          this.memberNum = res.result;
          this.smsForm.num = this.memberNum; //全部会员则会员数就等于条数
        }
      });
    },
    //已经选择的人取消选中
    alreadyCheckClose(val,index) {
      this.alreadyCheck.splice(index, 1);
      this.alreadyCheckShow.splice(index, 1);
      this.members.forEach((item,index)=>{
        if(item.____selected && item.mobile == val.mobile){
          item.____selected = false
        }
      })
      this.smsForm.num--;
      this.memberNum--;
    },
    //发送短信详细
    detail(){

    },
    //选择接收人事件
    smsRangeChange(v) {
      this.memberNum = 0;
      this.smsForm.num = 0;
      if (v == 1) {
        this.alreadyCheck = [];
        this.alreadyCheckShow = [];
        this.customSms = false;
        this.getMemberNum();
      }
      if (v == 2) {
        this.customSms = true;
        this.getMembers();
      }
    },
    //搜索会员
    memberSearch() {
      this.memberSearchParam.pageNumber = 1;
      this.members = [];
      this.getMembers();
    },
    //移动会员
    moveMember(index, item) {
      if (!item.mobile) {
        this.$Message.error("当前用户暂无手机号绑定");
        return false;
      }
      item.____selected = true;
      if (this.alreadyCheck.length == 0) {
        this.alreadyCheck.push(item.mobile);
        this.alreadyCheckShow.push(item);
        this.smsForm.num++;
        this.memberNum++;
      } else {
        //如果已选择数组内存在此用户则不在进行选择
        let result = this.alreadyCheck.indexOf(item.mobile);
        if (result < 0) {
          this.smsForm.num++;
          this.memberNum++;
          this.alreadyCheck.push(item.mobile);
          this.alreadyCheckShow.push(item);
        }
      }
    },
    //底部滑动查询会员
    memberSearchEdge() {
      return new Promise((resolve) => {
        setTimeout(() => {
          if (this.memberPage != this.memberSearchParam.pageNumber) {
            this.memberSearchParam.pageNumber++;
            this.getMembers();
            resolve();
          } else {
            resolve();
            return false;
          }
        }, 1000);
      });
    },
    //分页查询会员信息
    getMembers() {
      API_Member.getMemberListData(this.memberSearchParam).then((res) => {
        this.loading = false;
        if (res.success) {
          res.result.records.forEach((item) => {
            item.____selected = false;
            this.members.push(item);
          });

          this.memberPage = res.result.pages;
        }
      });
    },
    //弹出发送短信模态框
    sendBatchSmsModal() {
      this.templateSearchForm.templateStatus = 1;
      API_Setting.getSmsTemplatePage(this.templateSearchForm).then((res) => {
        if (res.success) {
          this.smsTemplates = res.result.records;
        }
      });
      this.signSearchForm.signStatus = 1;
      API_Setting.getSmsSignPage(this.signSearchForm).then((res) => {
        if (res.success) {
          this.smsSigns = res.result.records;
        }
      });
      this.smsContent = "<div class='sms'>效果预览</div>";
      //添加的时候将已经选过的手机号码置为空
      this.alreadyCheck = [];
      this.alreadyCheckShow = [];
      this.smsTemplateContent = "效果预览";
      // this.smsTemplateContent = "";
      this.smsForm = {
        smsName: "",
        signName: "",
        context: "",
        smsRange: "1",
        num: 0,
        messageCode: "",
      };
      this.getMemberNum();
      this.loading = false;
      this.sendSmsModal = true;
    },
    //pane切换事件
    paneChange(v) {
      if (v == "TEMPLATE") {
        this.getSmsTemplate();
      }
      if (v == "SIGN") {
        this.getSmsSign();
      }
    },
    //短信签名变化方法
    selectSmsSign(v) {
      if (v != void 0) {
        //给预览赋值
        this.smsContent =
          "<div class='sms'>【" +
          v +
          "】" +
          " " +
          this.smsTemplateContent +
          "</div>";
      } else {
        this.smsContent =
          "<div class='sms'>效果预览" + this.smsTemplateContent + "</div>";
      }
    },
    //短信模板变化方法
    selectSmsTemplate(v) {
      //循环短信模板 如果选择短信模板匹配则查询出当前模板的内容
      this.smsTemplates.forEach((e) => {
        if (this.smsForm.messageCode == e.templateCode) {
          this.smsTemplateContent = e.templateContent;
          this.smsForm.smsName = e.templateName;
        }
      });
      if (this.smsForm.signName != "" && this.smsForm.signName != void 0) {
        this.smsContent =
          "<div class='sms'>【" +
          this.smsForm.signName +
          "】" +
          this.smsTemplateContent +
          "</div>";
      } else {
        this.smsContent =
          "<div class='sms'>" + this.smsTemplateContent + "</div>";
      }
      this.smsForm.context = this.smsTemplateContent;
    },
    //删除短信模板
    deleteSmsTemplate(v) {
      let params = {
        templateCode: v.templateCode,
      };
      this.$Modal.confirm({
        title: "确认删除",
        content: "您确认要删除此短信模板？",
        loading: true,
        onOk: () => {
          API_Setting.deleteSmsTemplatePage(params).then((res) => {
            this.$Modal.remove();
            if (res.success) {
              this.$Message.success("删除成功");
              this.getSmsTemplate();
            }
          });
        },
      });
    },
    //发送短信
    sendSms() {
      this.$refs.smsForm.validate((valid) => {
        const mobile = this.alreadyCheck;
        this.smsForm.mobile = mobile;
        if (valid) {
          API_Setting.sendSms(this.smsForm).then((res) => {
            if (res.success) {
              this.$Message.success("发送成功");
              this.getSms();
              this.sendSmsModal = false;
            }
          });
        }
      });
    },
    //添加短信签名
    addSign() {
      this.$router.push({ name: "add-sms-sign" });
    },
    //新增短信模板
    addTemplate() {
      this.templateModalVisible = true;
      this.templateModalTitle = "添加短信模板";
      this.templateForm = {};
    },
    //修改短信模板
    editTemplate(v) {
      this.templateModalVisible = true;
      this.templateModalTitle = "修改短信模板";
      this.templateForm = v;
      this.modalType = 1;
    },
    //同步签名
    syncSign() {
      this.loading = true;
      API_Setting.syncSign().then((res) => {
        this.loading = false;
        if (res.success) {
          this.$Message.success("同步成功");
          this.getSmsSign();
        }
      });
      this.loading = false;
    },
    //短信模板添加提交
    templateSubmit() {
      this.$refs.templateForm.validate((valid) => {
        if (valid) {
          this.loading = true;
          if (this.modalType == 0) {
            API_Setting.addSmsTemplatePage(this.templateForm)
              .then((res) => {
                this.loading = false;
                if (res.success) {
                  this.$Message.success("添加成功");
                  this.loading = false;
                  this.templateModalVisible = false;
                  this.getSmsTemplate();
                }
              })
              .catch(() => {
                this.loading = false;
              });
          } else {
            API_Setting.editSmsTemplatePage(this.templateForm)
              .then((res) => {
                this.loading = false;
                if (res.success) {
                  this.$Message.success("修改成功");
                  this.loading = false;
                  this.templateModalVisible = false;
                  this.getSmsTemplate();
                }
              })
              .catch(() => {
                this.loading = false;
              });
          }
        }
      });
    },
    //删除短信签名
    deleteSmsSign(v) {
      this.$Modal.confirm({
        title: "确认删除",
        content: "您确认要删除此短信签名？",
        loading: true,
        onOk: () => {
          API_Setting.deleteSign(v.id).then((res) => {
            this.$Modal.remove();
            if (res.success) {
              this.$Message.success("删除成功");
              this.getSmsTemplate();
            }
          });
        },
      });
    },
    //短信模板同步
    syncTemplate() {
      this.loading = true;
      API_Setting.syncTemplate().then((res) => {
        this.loading = false;
        if (res.success) {
          this.$Message.success("同步成功");
          this.getSmsTemplate();
        }
      });
      this.loading = false;
    },
    //短信记录页数变化
    smsChangePage(v) {
      this.smsSearchForm.pageNumber = v;
      this.getSms();
    },
    //短信记录页数变化
    smsChangePageSize(v) {
      this.smsSearchForm.pageNumber = 1;
      this.smsSearchForm.pageSize = v;
      this.getSms();
    },
    //短信模板页数变化
    templateChangePage(v) {
      this.templateSearchForm.pageNumber = v;
      this.getSmsTemplate();
    },
    //短信模板页数变化
    templateChangePageSize(v) {
      this.templateSearchForm.pageNumber =1;
      this.templateSearchForm.pageSize = v;
      this.getSmsTemplate();
    },
    //分页获取短信模板数据
    getSmsTemplate() {
      this.loading = true;
      API_Setting.getSmsTemplatePage(this.templateSearchForm).then((res) => {
        this.loading = false;
        if (res.success) {
          this.templateData = res.result.records;
          this.templateTotal = res.result.total;
        }
      });
      this.loading = false;
    },
    //分页获取短信记录数据
    getSms() {
      this.loading = true;
      API_Setting.getSmsPage(this.smsSearchForm).then((res) => {
        this.loading = false;
        if (res.success) {
          this.smsData = res.result.records;
          this.smsTotal = res.result.total;
        }
      });
      this.loading = false;
    },
    //短信模板页数变化
    signChangePage(v) {
      this.signSearchForm.pageNumber = v;
      this.getSmsSign();
    },
    //短信模板页数变化
    signChangePageSize(v) {
      this.signSearchForm.pageNumber = 1;
      this.signSearchForm.pageSize = v;
      this.getSmsSign();
    },
    //修改短信签名
    editSign(v) {
      this.$router.push({ name: "add-sms-sign", query: { id: v.id } });
    },
    //分页获取短信签名数据
    getSmsSign() {
      this.loading = true;
      API_Setting.getSmsSignPage(this.signSearchForm).then((res) => {
        this.loading = false;
        if (res.success) {
          this.signData = res.result.records;
          this.signTotal = res.result.total;
        }
      });
      this.loading = false;
    },
  },
  mounted() {
    this.init();
  }
};
</script>

<style lang="scss">
@import "sms.scss";

.split {
  height: 200px;
  border: 1px solid #dcdee2;
}

.split-pane {
  padding: 10px;
}
.ivu-tabs{
   min-height: 500px;
}
</style>
