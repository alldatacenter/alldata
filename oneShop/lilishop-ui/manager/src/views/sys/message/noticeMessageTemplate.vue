<template>
  <div class="search">
    <Row>
      <Col>
        <Card>
          <Tabs value="MESSAGE" @on-click="paneChange">

            <TabPane label="站内信列表" name="MESSAGE">
              <Form ref="searchForm" :model="searchMessageForm" inline :label-width="70" class="search-form">
                <Form-item label="消息标题" prop="title">
                  <Input
                    type="text"
                    v-model="searchMessageForm.title"
                    placeholder="请输入消息标题"
                    clearable
                    style="width: 200px"
                  />
                </Form-item>

                <Form-item label="消息内容" prop="content">
                  <Input
                    type="text"
                    v-model="searchMessageForm.content"
                    placeholder="请输入消息内容"
                    clearable
                    style="width: 200px"
                  />
                </Form-item>

                <Button @click="handleSearch" type="primary" icon="ios-search" class="search-btn">搜索</Button>

              </Form>
              <Row class="operation" style="margin-top: 20px">
                <Button @click="sendMessage" type="primary">发送消息</Button>
              </Row>
              <Table
                class="mr_10"
                :loading="loading"
                border
                :columns="messageColumns"
                :data="messageData"
              ></Table>
              <Row type="flex" justify="end" class="mt_10 mb_10 mr_10">
                <Page
                  :current="searchMessageForm.pageNumber"
                  :total="messageDataTotal"
                  :page-size="searchMessageForm.pageSize"
                  @on-change="messageChangePage"
                  @on-page-size-change="messageChangePageSize"
                  :page-size-opts="[10, 20, 50]"
                  size="small"
                  show-total
                  show-elevator
                  show-sizer
                ></Page>
              </Row>
            </TabPane>

            <TabPane label="通知类站内信" name="SETTING">
              <Table
                :loading="loading"
                border
                class="mr_10"
                :columns="noticeColumns"
                :data="noticeData"
              ></Table>
              <Row type="flex" justify="end" class="mt_10 mr_10">
                <Page
                  :current="searchForm.pageNumber"
                  :total="noticeDataTotal"
                  :page-size="searchForm.pageSize"
                  @on-change="changePage"
                  @on-page-size-change="changePageSize"
                  :page-size-opts="[10, 20, 50]"
                  size="small"
                  show-total
                  show-elevator
                  show-sizer
                ></Page>
              </Row>
            </TabPane>
          </Tabs>

        </Card>
      </Col>
    </Row>
    <!-- 站内信模板编辑 -->
    <Modal
      :title="modalTitle"
      v-model="modalVisible"
      :mask-closable="false"
      :width="800"
    >
      <div class="message-title">
        <p>1、左侧#{xxx}为消息变量</p>
        <p>2、如果要发送的消息包含消息变量则将消息变量复制到消息内容中即可，注意格式</p>
        <p>3、例：比如消息变量为#{订单号}，发送的内容为：订单号为xxx的订单已经发货注意查收，完整的消息内容应该为订单号为#{订单号}的订单已经发货注意查收</p>
      </div>
      <div class="send-setting">
        <div class="left-show">
          <div v-for="(item, index) in form.variables" :key="index">
            #{<span>{{item}}</span>}
          </div>
        </div>
        <div class="send-form">
          <Form ref="form" :model="form" :label-width="100" :rules="formValidate">

            <FormItem label="通知节点" prop="noticeNode">
              <Input v-model="form.noticeNode" clearable type="text" style="width: 90%" maxlength="20" disabled/>
            </FormItem>
            <FormItem label="消息标题" prop="noticeTitle">
              <Input v-model="form.noticeTitle" clearable type="text" style="width: 90%" maxlength="20"/>
            </FormItem>
            <FormItem label="消息内容" prop="noticeContent">
              <Input v-model="form.noticeContent" clearable type="textarea" style="width: 90%" maxlength="50"
                     :autosize="{maxRows:4,minRows: 4}" show-word-limit/>
            </FormItem>
          </Form>
        </div>

      </div>
      <div slot="footer">
        <Button type="text" @click="modalVisible = false">取消</Button>
        <Button type="primary" :loading="submitLoading" @click="handleSubmit">保存</Button>
      </div>
    </Modal>

    <!-- 站内信发送 -->
    <Modal
      :title="messageModalTitle"
      v-model="messageModalVisible"
      :mask-closable="false"
      :width="800"
    >
      <Form ref="messageSendForm" :model="messageSendForm" :label-width="100" :rules="messageFormValidate">
        <FormItem label="消息标题" prop="title">
          <Input v-model="messageSendForm.title" maxlength="15" clearable style="width: 70%"/>
        </FormItem>
        <FormItem label="消息内容" prop="content">
          <Input
            v-model="messageSendForm.content"
            :rows="4"
            type="textarea"
            maxlength="200"
            style="max-height:60vh;overflow:auto;width: 70%"
          />
        </FormItem>

        <FormItem label="发送对象">
          <RadioGroup type="button" button-style="solid" v-model="messageSendForm.messageClient"
                      @on-change="selectObject">
            <Radio label="member">会员</Radio>
            <Radio label="store">商家</Radio>
          </RadioGroup>
        </FormItem>

        <FormItem label="发送范围">
          <RadioGroup type="button" button-style="solid" v-model="messageSendForm.messageRange" @on-change="selectShop">
            <Radio label="ALL">全站</Radio>
            <Radio v-if="messageSendForm.messageClient == 'store'" label="APPOINT">指定商家</Radio>
            <Radio v-if="messageSendForm.messageClient == 'member'" label="MEMBER">指定会员</Radio>
          </RadioGroup>
        </FormItem>
        <FormItem label="指定商家" v-if="shopShow">
          <Select v-model="messageSendForm.userIds" filterable multiple style="width: 90%;"
                  label-in-value @on-change="getName">
            <Option v-for="item in shopList" :value="item.id" :key="item.id" :lable="item.storeName">{{ item.storeName
              }}
            </Option>
          </Select>
        </FormItem>
        <FormItem label="选择会员" prop="scopeType"
                  v-if="memberShow">
          <Button type="primary" icon="ios-add" @click="addVip" ghost>选择会员</Button>
          <div style="margin-top:24px;" v-if="messageSendForm.messageClient == 'member'">
            <Table border :columns="userColumns" :data="selectedMember">
            </Table>
          </div>
        </FormItem>
        <Modal width="1200" v-model="checkUserList">
          <userList v-if="checkUserList" @callback="callbackSelectUser" :selectedList="selectedMember" ref="memberLayout"/>
        </Modal>
      </Form>
      <div slot="footer">
        <Button type="text" @click="messageModalVisible = false">取消</Button>
        <Button type="primary" :loading="submitLoading" @click="sendMessageSubmit"
        >发送
        </Button>
      </div>
    </Modal>

    <!-- 站内信发送 -->
    <Modal
      :title="modalTitle"
      v-model="messageDetailModalVisible"
      :mask-closable="false"
      :width="800"
    >
      <Form ref="messageSendForm" :model="messageSendForm" :label-width="100" :rules="messageFormValidate">
        <FormItem label="消息标题" prop="title">
          <Input v-model="messageSendForm.title" maxlength="15" clearable style="width: 50%" disabled/>
        </FormItem>
        <FormItem label="消息内容" prop="content">
          <Input
            disabled
            v-model="messageSendForm.content"
            :rows="4"
            type="textarea"
            style="max-height:60vh;overflow:auto;width: 50%"
          />
        </FormItem>
        <FormItem label="发送对象">
          <RadioGroup type="button" button-style="solid" v-model="messageSendForm.messageClient">
            <Radio disabled label="member">会员</Radio>
            <Radio disabled label="store">商家</Radio>
          </RadioGroup>
        </FormItem>
        <FormItem label="发送范围">
          <RadioGroup type="button" button-style="solid" v-model="messageSendForm.messageRange">
            <Radio disabled label="ALL">全站</Radio>
            <Radio v-if="messageSendForm.messageClient == 'store'" disabled label="APPOINT">指定商家</Radio>
            <Radio v-if="messageSendForm.messageClient == 'member'" disabled label="MEMBER">指定会员</Radio>
          </RadioGroup>
        </FormItem>
        <FormItem label="指定商家" v-if="messageSendForm.messageClient == 'store'">
          <Row>
            <Table
              :loading="loading"
              border
              :columns="messageDetailColumns"
              :data="shopMessageData"
              ref="table"
              sortable="custom"
              @on-sort-change="shopMessageChangeSort"
            ></Table>
          </Row>
          <Row type="flex" justify="end" class="mt_10">
            <Page
              :current="searchShopMessageForm.pageNumber"
              :total="shopMessageDataTotal"
              :page-size="searchShopMessageForm.pageSize"
              @on-change="shopMessageChangePage"
              @on-page-size-change="shopMessageChangePageSize"
              :page-size-opts="[10, 20, 50]"
              size="small"
              show-total
              show-elevator
              show-sizer
            ></Page>
          </Row>
        </FormItem>
        <FormItem label="指定会员" v-if="messageSendForm.messageClient == 'member'">
          <Row>
            <Table
              :loading="loading"
              border
              :columns="memberMessageDetailColumns"
              :data="memberMessageData"
              ref="table"
              sortable="custom"
              @on-sort-change="memberMessageChangeSort"
            ></Table>
          </Row>
          <Row type="flex" justify="end" class="mt_10">
            <Page
              :current="searchMemberMessageForm.pageNumber"
              :total="memberMessageDataTotal"
              :page-size="searchMemberMessageForm.pageSize"
              @on-change="memberMessageChangePage"
              @on-page-size-change="memberMessageChangePageSize"
              :page-size-opts="[10, 20, 50]"
              size="small"
              show-total
              show-elevator
              show-sizer
            ></Page>
          </Row>
        </FormItem>
      </Form>
      <div slot="footer">
        <Button type="text" @click="messageDetailModalVisible = false">取消</Button>
      </div>
    </Modal>
  </div>
</template>

<script>
  import * as API_Setting from "@/api/setting.js";
  import * as API_Other from "@/api/other.js";
  import * as API_Shop from "@/api/shops.js";
  import userList from "@/views/member/list/index";
  import { regular } from "@/utils";
  export default {
    name: "noticeMessageTemplate",
    components: {
      userList
    },
    data() {
      return {
        checkUserList: false, //会员选择器
        selectedMember: [], //选择的会员
        loading: true, // 表单加载状态
        modalVisible: false, // 添加或编辑显示
        modalTitle: "", // 添加或编辑标题
        messageModalVisible: false, // 发送站内信模态框
        messageModalTitle: "", // 发送站内信标题
        messageDetailModalVisible: false, // 添加或编辑显示
        shopShow: false, //指定商家是否出现
        memberShow: false, //指定会员是否出现
        shopList: [],//店铺列表
        searchForm: {
          // 搜索框初始化对象
          pageNumber: 1, // 当前页数
          pageSize: 10, // 页面大小
          sort: "createTime", // 默认排序字段
          order: "desc", // 默认排序方式
        },
        messageFormValidate: {
          title: [
            regular.REQUIRED,
            regular.VARCHAR20
          ],
          content: [
            regular.REQUIRED,
            regular.VARCHAR255
          ],
        },
        //管理端消息汇总
        searchMessageForm: {
          // 搜索框初始化对象
          pageNumber: 1, // 当前页数
          pageSize: 10, // 页面大小
        },
        //发送给店铺的消息
        searchShopMessageForm: {
          // 搜索框初始化对象
          pageNumber: 1, // 当前页数
          pageSize: 10, // 页面大小
        },
        //发送给会员的消息
        searchMemberMessageForm: {
          // 搜索框初始化对象
          pageNumber: 1, // 当前页数
          pageSize: 10, // 页面大小
        },
        form: {
          noticeNode: "",
          noticeTitle: ""
        },
        //消息发送表单
        messageSendForm: {
          messageRange: "ALL",
          messageClient: "member",
          userIds: [],
          userNames: [],
        },
        // 表单验证规则
        formValidate: {
          noticeNode: [
            {required: true, message: '请输入通知节点', trigger: 'blur'},
          ],
          noticeTitle: [
            {required: true, message: '请输入通知标题', trigger: 'blur'},
          ],
          noticeContent: [
            {required: true, message: '请输通知内容', trigger: 'blur'},
          ],

        },
        submitLoading: false, // 添加或编辑提交状态
        selectCount: 0, // 多选计数
        noticeColumns: [
          {
            title: "通知节点",
            key: "noticeNode",
            maxWidth: 270,
            sortable: false,
          },
          {
            title: "通知标题",
            key: "noticeTitle",
            minWidth: 200,
            sortable: false,
          },
          {
            title: "通知内容",
            key: "noticeContent",
            minWidth: 300,
            sortable: false,
          },
          {
            title: "状态",
            key: "noticeStatus",
            maxWidth: 100,
            sortType: "desc",
            render: (h, params) => {
              if (params.row.noticeStatus == "OPEN") {
                return h("Badge", {props: {status: "success", text: "开启"}})
              } else if (params.row.noticeStatus == "CLOSE") {
                return h("Badge", {props: {status: "processing", text: "关闭"}})
              }
            }
          },
          {
            title: "操作",
            key: "action",
            align: "center",
            fixed: "right",
            width: 140,
            render: (h, params) => {
              let enableOrDisable = "";
              if (params.row.noticeStatus == "OPEN") {
                enableOrDisable = h(
                  "Button",
                  {
                    props: {
                      size: "small"
                    },
                    style: {
                      marginRight: "5px"
                    },
                    on: {
                      click: () => {
                        this.disable(params.row);
                      }
                    }
                  },
                  "关闭"
                );
              } else {
                enableOrDisable = h(
                  "Button",
                  {
                    props: {
                      type: "success",
                      size: "small"
                    },
                    style: {
                      marginRight: "5px"
                    },
                    on: {
                      click: () => {
                        this.enable(params.row);
                      }
                    }
                  },
                  "开启"
                );
              }
              return h("div", [
                enableOrDisable,
                h(
                  "Button",
                  {
                    props: {
                      type: "primary",
                      size: "small"
                    },
                    style: {
                      marginRight: "5px"
                    },
                    on: {
                      click: () => {
                        this.edit(params.row);
                      }
                    }
                  },
                  "编辑"
                ),
              ]);
            }
          },
        ],
        // 用户表格
        userColumns: [
          {
            title: "用户名称",
            key: "nickName",
            minWidth: 120,
          },
          {
            title: "手机号",
            key: "mobile",
            render: (h, params) => {
              return h("div", params.row.mobile || "暂未填写");
            },
          },
          {
            title: "操作",
            key: "action",
            minWidth: 50,
            align: "center",
            render: (h, params) => {
              return h(
                "Button",
                {
                  props: {
                    size: "small",
                    type: "error",
                    ghost: true,
                  },
                  on: {
                    click: () => {
                      this.delUser(params.index);
                    },
                  },
                },
                "删除"
              );
            },
          },
        ],
        noticeData: [], // 表单数据
        noticeDataTotal: 0, // 表单数据总数
        messageColumns: [
          {
            title: "消息标题",
            key: "title",
            minWidth: 150,
          },
          {
            title: "消息内容",
            key: "content",
            minWidth: 350,
            tooltip: true
          },
          {
            title: "发送对象",
            key: "messageClient",
            width: 100,
            render: (h, params) => {
              if (params.row.messageClient == "member") {
                return h('div', [
                  h('span', {}, '会员'),
                ]);
              } else if (params.row.messageClient == "store") {
                return h('div', [
                  h('span', {}, '商家'),
                ]);
              }
            }
          },
          {
            title: "发送类型",
            key: "messageRange",
            width: 100,
            render: (h, params) => {
              if (params.row.messageRange == "ALL") {
                return h('div', [
                  h('span', {}, '全站'),
                ]);
              } else if (params.row.messageRange == "APPOINT") {
                return h('div', [
                  h('span', {}, '指定商家'),
                ]);
              } else if (params.row.messageRange == "MEMBER") {
                return h('div', [
                  h('span', {}, '指定会员'),
                ]);
              }
            }
          },
          {
            title: "发送时间",
            key: "createTime",
            sortable: true,
            width: 180,
            sortable: false,
          },
          {
            title: "操作",
            key: "action",
            align: "center",
            fixed: "right",
            width: 140,
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
                      marginRight: "5px"
                    },
                    on: {
                      click: () => {
                        this.detail(params.row);
                      }
                    }
                  },
                  "详细"
                ),
                h(
                  "Button",
                  {
                    props: {
                      type: "error",
                      size: "small"
                    },
                    style: {
                      marginRight: "5px"
                    },
                    on: {
                      click: () => {
                        this.delete(params.row.id);
                      }
                    }
                  },
                  "删除"
                ),
              ]);
            }
          },
        ],
        messageData: [], // 表单数据
        messageDataTotal: 0, // 表单数据总数
        messageDetailColumns: [
          {
            title: "店铺ID",
            key: "storeId",
            maxWidth: 300,
            sortable: false,
          },
          {
            title: "店铺名称",
            key: "storeName",
            sortable: false,
          },
          {
            title: "是否已读",
            key: "status",
            render: (h, params) => {
              if (params.row.status == "ALREADY_READY") {
                return h("Badge", {props: {status: "success", text: "已读"}})
              } else if (params.row.status == "UN_READY") {
                return h("Badge", {props: {status: "processing", text: "未读"}})
              } else {
                return h("Badge", {props: {status: "processing", text: "回收站"}})
              }
            }
          },
        ],
        shopMessageData: [], // 发送给店铺的消息数据
        shopMessageDataTotal: 0, // 发送给店铺的消息数据总数

        memberMessageDetailColumns: [
          {
            title: "会员ID",
            key: "memberId",
            maxWidth: 300,
            sortable: false,
          },
          {
            title: "会员名称",
            key: "memberName",
            sortable: false,
          },
          {
            title: "是否已读",
            key: "status",
            maxWidth: 120,
            render: (h, params) => {
              if (params.row.status == "ALREADY_READY") {
                return h("Badge", {props: {status: "success", text: "已读"}})
              } else if (params.row.status == "UN_READY") {
                return h("Badge", {props: {status: "processing", text: "未读"}})
              } else {
                return h("Badge", {props: {status: "processing", text: "回收站"}})
              }
            }
          },
        ],
        memberMessageData: [], // 发送给会员的消息数据
        memberMessageDataTotal: 0, // 发送给会员的消息数据总数
      };
    },
    methods: {
      // 初始化数据
      init() {
        this.getMessage();
      },
      // 返回已选择的用户
      callbackSelectUser(val) {
        // 每次将返回的数据回调判断
        let findUser = this.selectedMember.find((item) => {
          return item.id === val.id;
        });
        // 如果没有则添加
        if (!findUser) {
          this.selectedMember.push(val);
        } else {
          // 有重复数据就删除
          this.selectedMember.map((item, index) => {
            if (item.id === findUser.id) {
              this.selectedMember.splice(index, 1);
            }
          });
        }
        this.reSelectMember();
      },

      // 删除选择的会员
      delUser(index) {
        this.selectedMember.splice(index, 1);
        this.reSelectMember();
      },
      //更新选择的会员
      reSelectMember() {
        this.form.memberDTOS = this.selectedMember.map((item) => {
          return {
            nickName: item.nickName,
            id: item.id
          }
        });
      },

      //获取全部商家
      getShopList() {
        this.loading = true;
        API_Shop.getShopList().then((res) => {
          this.loading = false;
          if (res.success) {
            this.shopList = res.result;
          }
        });
        this.loading = false;

      },
      // 添加指定用户
      addVip() {
        this.checkUserList = true;
        this.$nextTick(() => {
          this.$refs.memberLayout.selectedMember = true;
        });
      },
      // tab切换
      paneChange(v) {
        if (v == "SETTING") {
          this.getNoticeMessage()
        }
        if (v == "MESSAGE") {
          this.getMessage()
        }
      },
      // 分页 修改页码
      changePage(v) {
        this.searchForm.pageNumber = v;
        this.getNoticeMessage();
      },
      // 分页 修改页数
      changePageSize(v) {
        this.searchForm.pageNumber = 1;
        this.searchForm.pageSize = v;
        this.getNoticeMessage();
      },
      // 搜索
      handleSearch() {
        this.searchMessageForm.pageNumber = 1;
        this.getMessage();
      },

      //消息每页条数发生变化
      messageChangePageSize(v) {
        this.searchMessageForm.pageSize = v;
        this.searchMessageForm.pageNumber = 1;
        this.getMessage();
      },
      //消息页数变化
      messageChangePage(v) {
        this.searchMessageForm.pageNumber = v;
        this.getMessage();
        this.clearSelectAll();
      },

      //会员消息每页条数发生变化
      memberMessageChangePageSize(v) {
        this.searchMemberMessageForm.pageSize = v;
        this.searchMemberMessageForm.pageNumber = 1;
        this.messageDetail();
      },
      //会员消息页数变化
      memberMessageChangePage(v) {
        this.searchMemberMessageForm.pageNumber = v;
        this.messageDetail();
        this.clearSelectAll();
      },
      //会员消息
      memberMessageChangeSort(e) {
        this.searchMemberMessageForm.sort = e.key;
        this.searchMemberMessageForm.order = e.order;
        this.messageDetail()
      },

      //店铺消息每页条数发生变化
      shopMessageChangePageSize(v) {
        this.searchShopMessageForm.pageSize = v;
        this.messageDetail();
      },
      //店铺消息页数变化
      shopMessageChangePage(v) {
        this.searchShopMessageForm.pageNumber = v;
        this.messageDetail();
        this.clearSelectAll();
      },
      //店铺消息
      shopMessageChangeSort(e) {
        this.searchShopMessageForm.sort = e.key;
        this.searchShopMessageForm.order = e.order;
        this.messageDetail()
      },
      //获取发送段铺的名称
      getName(value) {
        this.messageSendForm.userNames = new Array()
        value.forEach((item) => {
          this.messageSendForm.userNames.push(item.label)
        })
      },
      //删除站内信
      delete(id) {
        console.warn(id)
        this.$Modal.confirm({
          title: "确认删除",
          // 记得确认修改此处
          content: "您确认删除此站内信 ?",
          loading: true,
          onOk: () => {
            API_Setting.deleteMessage(id).then((res) => {
              if (res.success) {
                this.$Message.success("删除成功");
              }
              this.$Modal.remove();
              this.getMessage();
            });


          }
        });
      },
      //管理员发送消息
      sendMessage() {
        this.messageModalVisible = true
        this.messageModalTitle = "发送站内信"
        this.shopShow = false
        this.memberShow = false
        this.messageSendForm =
          {
            messageRange: "ALL",
            messageClient: "member",
            content: "",
            title: "",
            userIds: [],
            userNames: [],
          }
      },
      //管理员发送站内信提交
      sendMessageSubmit() {
        let userIds = [];
        let userNames = [];
        console.warn(this.selectedMember)
        if (this.messageSendForm.messageClient == 'member' && this.messageSendForm.messageRange == 'MEMBER'){
          this.selectedMember.forEach(function(item, index) {
            userIds.push(item.id)
            userNames.push(item.username)
          })
          this.messageSendForm.userIds = userIds
          this.messageSendForm.userNames = userNames
        }

        if (this.messageSendForm.userIds.length <= 0 && this.messageSendForm.messageRange == "APPOINT") {
          this.$Message.error("请选择发送对象");
          return
        }
        this.$refs["messageSendForm"].validate(valid => {
          if (valid) {
            API_Other.sendMessage(this.messageSendForm).then((res) => {
              this.loading = false;
              if (res.success) {
                this.$Message.success("发送成功");
                this.messageModalVisible = false
                this.getMessage();
              }
            });
            this.loading = false;
          }
        })

      },
      //发送对象选择
      selectObject(v) {
        this.messageSendForm.messageRange = "ALL"
        this.shopShow = false
        this.memberShow =false
      },
      //弹出选择商家的框
      selectShop(v) {
        if (v == "APPOINT") {
          this.getShopList()
          this.shopShow = true
          this.memberShow = false
        }
        if (v == "ALL") {
          this.shopShow = false
          this.memberShow = false
        }
        if (v == "MEMBER") {
          this.shopShow = false
          this.memberShow = true
          this.selectedMember = []
        }
      },
      //获取管理员发送列表
      getMessage() {
        this.loading = true;
        API_Other.getMessagePage(this.searchMessageForm).then((res) => {
          this.loading = false;
          if (res.success) {
            this.messageData = res.result.records;
            this.messageDataTotal = res.result.total;
          }
        });
        this.loading = false;
      },
      // 获取通知数据
      getNoticeMessage() {
        this.loading = true;
        API_Setting.getNoticeMessageData(this.searchForm).then((res) => {
          this.loading = false;
          if (res.success) {
            this.noticeData = res.result.records;
            this.noticeDataTotal = res.result.total;
          }
        });
        this.loading = false;
      },
      //保存通知类站内信
      handleSubmit() {
        this.$refs.form.validate((valid) => {
          if (valid) {
            let params = {
              noticeContent: this.form.noticeContent,
              noticeTitle: this.form.noticeTitle
            }
            API_Setting.editNoticeMessage(this.form.id, params).then((res) => {
              this.submitLoading = false;
              if (res.success) {
                this.$Message.success("修改成功");
                this.modalVisible = false;
                this.getNoticeMessage();
              }
            });
          }
        });
      },
      //消息详情
      messageDetail() {
        if (this.messageSendForm.messageClient == 'member') {
          API_Other.getMemberMessage(this.searchMemberMessageForm).then((res) => {
            if (res.success) {
              this.memberMessageData = res.result.records;
              this.memberMessageDataTotal = res.result.total;
            }
          });
        } else {
          console.warn(this.searchShopMessageForm)
          API_Other.getShopMessage(this.searchShopMessageForm).then((res) => {
            if (res.success) {
              this.shopMessageData = res.result.records;
              this.shopMessageDataTotal = res.result.total;
            }
          });
        }

      },
      //消息详情弹出框
      detail(v) {
        console.warn(this.searchShopMessageForm)
        this.messageSendForm = v
        if (this.messageSendForm.messageClient == 'member') {
          this.searchMemberMessageForm.messageId = v.id
        } else {
          this.searchShopMessageForm.messageId = v.id
        }
        this.messageDetail();
        this.messageDetailModalVisible = true;
        this.modalTitle = "消息详情"
      },
      // 编辑通知
      edit(v) {
        API_Setting.getNoticeMessageDetail(v.id).then((res) => {
          if (res.success) {
            this.modalTitle = "编辑通知类推送"
            this.modalVisible = true
            this.form = res.result
          }
        });
      },
      //禁用站内信模板
      disable(v) {
        API_Setting.updateMessageStatus(v.id, "CLOSE").then((res) => {
          if (res.success) {
            this.$Message.success("禁用成功");
            this.getNoticeMessage();
          }
        });
      },
      //启用站内信模板
      enable(v) {
        API_Setting.updateMessageStatus(v.id, "OPEN").then((res) => {
          if (res.success) {
            this.$Message.success("启用成功");
            this.getNoticeMessage();
          }
        });
      },

    },
    mounted() {
      this.init();
    },
  };
</script>
<style lang="scss">
  @import "sms.scss";
</style>
