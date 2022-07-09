<template>
  <div>
    <!--微信模板-->
    <Modal v-model="wechatModal" width="530">
      <p slot="header">
        <Icon type="edit"></Icon>
        <span>微信设置</span>
      </p>
      <div>
        <Form ref="wechatFormData" :model="wechatFormData" label-position="left" :label-width="100">
          <FormItem v-if="tab === 'WECHAT'" label="模板名称">
            <Input v-model="wechatFormData.name" size="large" maxlength="9" disabled></Input>
          </FormItem>
          <FormItem v-if="tab === 'WECHAT'" label="头部信息" prop="first">
            <Input v-model="wechatFormData.first" size="large" maxlength="50"></Input>
          </FormItem>
          <FormItem v-if="tab === 'WECHAT'" label="备注" prop="remark">
            <Input class='textarea' :rows="5" :autosize="{maxRows:5,minRows: 5}" v-model="wechatFormData.remark"
                   type="textarea" maxlength="150"/>
          </FormItem>
          <FormItem label="是否开启" prop="enable">
            <i-switch v-model="wechatFormData.enable" size="large">
              <span slot="open">开启</span>
              <span slot="close">关闭</span>
            </i-switch>
          </FormItem>
        </Form>

      </div>
      <div slot="footer" style="text-align: right">
        <Button v-if="tab === 'WECHAT'" type="success" size="large" @click="wechatFormDataEdit">保存</Button>

        <Button v-else type="success" size="large" @click="wechatMPFormDataEdit">保存</Button>
      </div>
    </Modal>


    <Tabs @on-click="tabPaneChange" v-model="tab">
      <TabPane label="微信消息" name="WECHAT">
        <div class="search">
          <Card>

            <Row class="operation mt_10">
              <Button @click="weChatSync" type="primary">同步微信消息</Button>
            </Row>
            <Table
              :loading="loading"
              border
              :columns="weChatColumns"
              :data="weChatData"
              ref="weChatTable"
            ></Table>
            <Row type="flex" justify="end" class="mt_10">
              <Page
                :current="weChatSearchForm.pageNumber"
                :total="weChatTotal"
                :page-size="weChatSearchForm.pageSize"
                @on-change="changePage"
                @on-page-size-change="changePageSize"
                :page-size-opts="[10,20,50]"
                size="small"
              ></Page>
            </Row>
          </Card>
        </div>
      </TabPane>

      <TabPane label="微信小程序订阅消息" name="WECHATMP">
        <div class="search">
          <Card>

            <Row class="operation mt_10">
              <Button @click="weChatSync('mp')" type="primary">同步微信小程序订阅消息</Button>
            </Row>
            <Table
              :loading="loading"
              border
              :columns="weChatColumns"
              :data="weChatMPData"
              sortable="custom"
              ref="weChatMPTable"
            ></Table>
            <Row type="flex" justify="end" class="mt_10">
              <Page
                :current="weChatMPSearchForm.pageNumber"
                :total="weChatMPTotal"
                :page-size="weChatMPSearchForm.pageSize"
                @on-change="changePage"
                @on-page-size-change="changePageSize"
                :page-size-opts="[10,20,50]"
                size="small"
              ></Page>
            </Row>
          </Card>
        </div>
      </TabPane>
    </Tabs>
  </div>


</template>

<script>
import {
  wechatMessageSync,
  getWechatMessagePage,
  editWechatMessageTemplate,
  delWechatMessageTemplate,

  wechatMPMessageSync,
  getWechatMPMessagePage,
  editWechatMPMessageTemplate,
  delWechatMPMessageTemplate
} from "@/api/setting";

export default {
  title: "wechat-message-manage",
  data() {
    return {

      wechatModal: false,// modal展示
      wechatFormData: {}, // 微信数据
      wechatMPFormData: {}, // 微信订阅消息
      tab: "WECHAT", // tab栏分类
      searchForm: { // 请求参数
        type: "WECHAT"
      },
      loading: true, // 表单加载状态
      id: '', // 模板id
      //微信消息查询
      weChatSearchForm: {
        // 搜索框对应data对象
        pageNumber: 1, // 当前页数
        pageSize: 10, // 页面大小
      },
      weChatMPSearchForm: {
        // 搜索框对应data对象
        pageNumber: 1, // 当前页数
        pageSize: 10, // 页面大小
      },
      weChatColumns: [
        {
          title: "模板编号",
          key: "code",
          width: 500,
          sortable: true
        },
        {
          title: "是否开启",
          key: "enable",
          sortable: true,
          width: 150,
          render: (h, params) => {
            if (params.row.enable == true) {
              return h('div', [
                h('span', {}, '开启'),
              ]);
            } else {
              return h('div', [
                h('span', {}, '关闭'),
              ]);
            }
          },
        },
        {
          title: "模板名称",
          key: "name",
          width: 200,
          sortable: true
        },
        {
          title: "创建时间",
          key: "createTime",
          sortable: true,
          sortType: "desc",
        },
        {
          title: "操作",
          key: "action",
          width: 200,
          align: "center",
          fixed: "right",
          render: (h, params) => {
            return h("div", [
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
                      this.wechatSettingAlert(params.row);
                    }
                  }
                },
                "编辑"
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
                      this.delWeChat(params.row);
                    }
                  }
                },
                "删除"
              )
            ]);
          }
        }
      ],
      weChatData: [], // 表单数据
      weChatMPData: [], // 表单数据
      weChatTotal: 0, // 表单数据总数
      weChatMPTotal: 0, // 表单数据总数
    };
  },
  methods: {
    // 初始化数据
    init() {
      this.getDataList();
    },
    changePage(v) {
      this.searchForm.type = this.tab;
      this.getDataList();
    },
    changePageSize(v) {
      this.searchForm.type = this.tab;
      this.getDataList();
    },
    //微信弹出框
    wechatSettingAlert(v) {
      this.wechatFormData = v
      this.id = v.id
      this.wechatModal = true
    },

    //同步微信消息
    weChatSync(mp) {
      this.$Modal.confirm({
        title: "提示",
        // 记得确认修改此处
        content: "确认要初始化微信小程序消息订阅?",
        loading: true,
        onOk: () => {
          // 同步微信消息模板

          if (mp === "mp") {
            wechatMPMessageSync().then(res => {
              this.$Modal.remove();
              if (res.success) {
                this.$Message.success('微信小程序消息订阅初始化');
              }
            });
          } else {
            // 同步微信消息模板
            wechatMessageSync().then(res => {
              this.$Modal.remove();
              if (res.success) {
                this.$Message.success('微信消息模板初始化成功');
              }
            });
          }
        }
      });
    },
    //微信设置保存
    wechatFormDataEdit() {
      this.$refs['wechatFormData'].validate((valid) => {
        if (valid) {
          editWechatMessageTemplate(this.id, this.wechatFormData).then(res => {
            if (res.message === 'success') {
              this.$Message.success('微信模板修改成功');
              this.wechatModal = false;
              this.getWechatMessagePage();
            }
          });
        }
      })
    },
    wechatMPFormDataEdit() {
      this.$refs['wechatFormData'].validate((valid) => {
        if (valid) {
          editWechatMPMessageTemplate(this.id, this.wechatMPFormData).then(res => {
            if (res.message === 'success') {
              this.$Message.success('微信消息订阅模板修改成功');
              this.wechatModal = false;
              this.getWechatMessagePage();
            }
          });
        }
      })
    },

    //删除微信模消息
    delWeChat(v) {
      this.$Modal.confirm({
        title: "提示",
        content: "确定删除此模板?",
        loading: true,
        onOk: () => {
          // 删除微信消息模板
          if (this.tab === "WECHAT") {
            delWechatMessageTemplate(v.id).then(res => {
              if (res.success) {
                this.$Modal.remove();
                this.$Message.success('微信模板删除成功');
                this.getWechatMessagePage()
              }
            });
          } else {
            delWechatMPMessageTemplate(v.id).then(res => {
              if (res.success) {
                this.$Modal.remove();
                this.$Message.success('微信消息订阅删除成功');
                this.getWechatMessagePage()
              }
            });
          }
        }
      });

    },
    selectDateRange(v) {
      if (v) {
        this.searchForm.startDate = v[0];
        this.searchForm.endDate = v[1];
      }
    },
    getDataList() {
      this.loading = true;
      getWechatMessagePage(this.searchWe).then(res => {
        this.loading = false;
        if (res.success) {
          this.weChatData = res.result.records;
          this.weChatTotal = res.result.total;
        }
      });
    },
    //分页获取微信消息
    getWechatMessagePage() {
      getWechatMessagePage(this.weChatSearchForm).then(res => {
        this.loading = false;
        if (res.success) {
          this.weChatData = res.result.records;
          this.weChatTotal = res.result.total;
        }
      });
    },
    //分页获取微信小程序消息订阅
    getWechatMPMessagePage() {
      getWechatMPMessagePage(this.weChatMPSearchForm).then(res => {
        this.loading = false;
        if (res.success) {
          this.weChatMPData = res.result.records;
          this.weChatMPTotal = res.result.total;
        }
      });
    },
    //tab切换事件
    tabPaneChange(v) {
      this.searchForm.type = v;
      //如果是微信消息则走单独的接口
      if (v === "WECHAT") {
        this.getWechatMessagePage();
      } else if (v === "WECHATMP") {
        this.getWechatMPMessagePage();
      }

    }
  },
  mounted() {
    this.init();
  }
};
</script>
