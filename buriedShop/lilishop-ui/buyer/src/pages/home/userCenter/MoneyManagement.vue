<template>
  <div class="wrapper">
    <card _Title="资金管理" />

    <div class="box">
      <div class="mb_20 account-price">
        <span class="subTips">账户余额：</span>
        <span class="global_color mr_10" style="font-size:26px">￥{{ memberDeposit | unitPrice }}</span>
        <span class="subTips">冻结金额：</span>
        <span class="">￥{{ frozenDeposit | unitPrice}}</span>
      </div>
      <div class="account-btns">
        <Button type="primary" @click="recharge">在线充值</Button>
        <Button @click="withdrawalApply">申请提现</Button>
      </div>
    </div>
    <Modal v-model="modal" width="530">
      <p slot="header">
        <Icon type="edit"></Icon>
        <span>充值金额</span>
      </p>
      <div>
        <Form
          ref="formData"
          :model="formData"
          label-position="left"
          :label-width="100"
          :rules="formValidate"
        >
          <FormItem label="充值金额" prop="price">
            <Input v-model="formData.price" size="large" number maxlength="9"
              ><span slot="append">元</span></Input>
          </FormItem>
        </Form>
      </div>
      <div slot="footer" style="text-align: center">
        <Button type="success" size="large" @click="rechargePrice">充值</Button>
      </div>
    </Modal>
    <!-- 提现申请 -->
    <Modal v-model="withdrawApplyModal" width="530">
      <p slot="header">
        <Icon type="edit"></Icon>
        <span>提现金额</span>
      </p>
      <div>
        <Form
          ref="withdrawApplyFormData"
          :model="withdrawApplyFormData"
          label-position="left"
          :label-width="100"
          :rules="withdrawApplyFormValidate"
        >
          <FormItem label="提现金额" prop="price">
            <Input
              v-model="withdrawApplyFormData.price"
              size="large"
              number
              maxlength="9"
              ><span slot="append">元</span></Input>
          </FormItem>
        </Form>
      </div>
      <div slot="footer" style="text-align: center">
        <Button type="success" size="large" @click="withdrawal">提现</Button>
      </div>
    </Modal>
    <!-- 余额日志 -->
    <Tabs value="log" @on-click="tabPaneChange">
      <TabPane label="余额日志" name="log">
        <Table :columns="logColumns" :data="logColumnsData.records"></Table>
        <!-- 分页 -->
        <div class="page-size">
          <Page
            :current="walletForm.pageNumber"
            :total="logColumnsData.total"
            :page-size="walletForm.pageSize"
            @on-change="changePage"
            @on-page-size-change="changePageSize"
            :page-size-opts="[10, 20, 50]"
            size="small"
            show-total
            show-sizer
            transfer
          ></Page>
        </div>
      </TabPane>
      <!-- 充值记录 -->
      <TabPane label="充值记录" name="recharge">
        <Table
          :columns="rechargeListColumns"
          :data="rechargeListData.records"
        ></Table>
        <!-- 分页 -->
        <div class="page-size">
          <Page
            :current="rechargeForm.pageNumber"
            :total="rechargeListData.total"
            :page-size="rechargeForm.pageSize"
            @on-change="rechargeChangePage"
            @on-page-size-change="rechargeChangePageSize"
            :page-size-opts="[10, 20, 50]"
            size="small"
            show-total
            show-sizer
            transfer
          ></Page>
        </div>
      </TabPane>

      <TabPane label="提现记录" name="withdrawApply">
        <Table
          :columns="withdrawApplyColumns"
          :data="withdrawApplyColumnsListData.records"
        ></Table>
        <!-- 分页 -->
        <div class="page-size">
          <Page
            :current="withdrawApplyForm.pageNumber"
            :total="withdrawApplyColumnsListData.total"
            :page-size="withdrawApplyForm.pageSize"
            @on-change="withdrawChangePage"
            @on-page-size-change="withdrawChangePageSize"
            :page-size-opts="[10, 20, 50]"
            size="small"
            show-total
            show-sizer
            transfer
          ></Page>
        </div>
      </TabPane>
    </Tabs>
  </div>
</template>

<script>
import {
  getMembersWallet,
  getDepositLog,
  getRecharge,
  getWithdrawApply,
  recharge,
  withdrawalApply
} from '@/api/member';
export default {
  name: 'MoneyManagement',
  data () {
    return {
      frozenDeposit: 0, // 冻结余额
      memberDeposit: 0, // 余额

      modal: false, // 余额充值
      withdrawApplyModal: false, // 提现申请
      formData: {
        // 充值金额
        price: 1
      },
      // 提现金额
      withdrawApplyFormData: {
        price: 1
      },
      // 余额日志
      walletForm: {
        // 搜索框初始化对象
        pageNumber: 1,
        pageSize: 10
      },
      // 充值记录
      rechargeForm: {
        // 搜索框初始化对象
        pageNumber: 1, // 当前页数
        pageSize: 10 // 页面大小
      },
      // 提现记录
      withdrawApplyForm: {
        // 搜索框初始化对象
        pageNumber: 1, // 当前页数
        pageSize: 10 // 页面大小
      },
      // 提现申请校验
      withdrawApplyFormValidate: {
        price: [
          { required: true, message: '请输入大于0小于9999的合法提现金额' },
          {
            pattern: /^[1-9]\d{0,3}(\.\d{1,2})?$/,
            message: '请输入大于0小于9999的合法提现金额',
            trigger: 'change'
          }
        ]
      },
      formValidate: {
        price: [
          { required: true, message: '请输入大于等于1小于9999的合法充值金额' },
          {
            pattern: /^[1-9]\d{0,3}(\.\d{1,2})?$/,
            message: '请输入大于等于1小于9999的合法充值金额',
            trigger: 'change'
          }
        ]
      },
      // 余额日志
      logColumns: [
        {
          title: '时间',
          width: 190,
          key: 'createTime'
        },
        {
          title: '金额',
          key: 'money',
          width: 180,
          render: (h, params) => {
            if (params.row.money > 0) {
              return h('div', [
                h(
                  'span',
                  {
                    style: {
                      color: 'green'
                    }
                  },
                  this.$options.filters.unitPrice(params.row.money, '+ ¥')
                )
              ]);
            } else if (params.row.money < 0) {
              return h('div', [
                h(
                  'span',
                  {
                    style: {
                      color: 'red'
                    }
                  },
                  this.$options.filters.unitPrice(0 - params.row.money, '- ¥')
                )
              ]);
            }
          }
        },
        {
          title: '变动日志',
          key: 'detail'
        }
      ],
      logColumnsData: {}, // 余额日志
      // 充值记录
      rechargeListColumns: [
        {
          title: '充值时间',
          key: 'createTime',
          width: 168
        },
        {
          title: '支付单号',
          key: 'rechargeSn',
          width: 200
        },
        {
          title: '充值金额',
          key: 'rechargeMoney',
          render: (h, params) => {
            if (params.row.payStatus === 'PAID') {
              return h('div', [h('span', {
                style: {
                  color: 'green'
                }
              }, this.$options.filters.unitPrice(params.row.rechargeMoney, '+ ¥'))]);
            } else {
              return h('div', [h('span', this.$options.filters.unitPrice(params.row.rechargeMoney, '¥'))]);
            }
          }
        },
        {
          title: '支付状态',
          key: 'payStatus',
          render: (h, params) => {
            if (params.row.payStatus === 'PAID') {
              return h('div', [h('span', {}, '已付款')]);
            } else if (params.row.payStatus === 'UNPAID') {
              return h('div', [h('span', {}, '未付款')]);
            } else if (params.row.payStatus === 'CANCEL') {
              return h('div', [h('span', {}, '已取消')]);
            }
          }
        },
        {
          title: '支付方式',
          key: 'rechargeWay',
          render: (h, params) => {
            if (params.row.rechargeWay === 'ALIPAY') {
              return h('div', [h('span', {}, '支付宝')]);
            } else if (params.row.rechargeWay === 'WECHAT') {
              return h('div', [h('span', {}, '微信')]);
            } else if (params.row.rechargeWay === 'BANK_TRANSFER') {
              return h('div', [h('span', {}, '线下转账')]);
            } else {
              return h('div', [h('span', {}, '')]);
            }
          }
        },
        {
          title: '支付时间',
          key: 'payTime',
          width: 180
        }
      ],
      rechargeListData: {}, // 充值记录数据
      // 提现记录
      withdrawApplyColumns: [
        {
          title: '申请时间',
          key: 'createTime',
          width: 168
        },
        {
          title: '提现单号',
          key: 'sn',
          width: 200
        },
        {
          title: '提现金额',
          key: 'applyMoney',
          width: 110,
          render: (h, params) => {
            if (params.row.applyStatus === 'VIA_AUDITING') {
              return h('div', [h('span', {
                style: {
                  color: 'green'
                }
              }, this.$options.filters.unitPrice(params.row.applyMoney, '+ ¥'))]);
            } else {
              return h('div', [h('span', this.$options.filters.unitPrice(params.row.applyMoney, '¥'))]);
            }
          }
        },
        {
          title: '提现状态',
          key: 'applyStatus',
          width: 95,
          render: (h, params) => {
            if (params.row.applyStatus === 'APPLY') {
              return h('div', [h('span', {}, '申请中')]);
            } else if (params.row.applyStatus === 'VIA_AUDITING') {
              return h('div', [h('span', {}, '提现成功')]);
            } else {
              return h('div', [h('span', {}, '审核拒绝')]);
            }
          }
        },
        {
          title: '审核时间',
          key: 'inspectTime',
          width: 168
        },
        {
          title: '审核备注',
          key: 'inspectRemark',
          tooltip: true

        }
      ],
      withdrawApplyColumnsListData: {} // 提现记录
    };
  },
  mounted () {
    this.init();
  },
  methods: {
    // 初始化数据
    init () {
      getMembersWallet().then((res) => {
        this.frozenDeposit = res.result.memberFrozenWallet;
        this.memberDeposit = res.result.memberWallet;
      });
      getDepositLog(this.walletForm).then((res) => {
        if (res.message === 'success') {
          this.logColumnsData = res.result;
        }
      });
    },
    tabPaneChange (v) {
      // 如果查询充值记录
      if (v === 'recharge') {
        this.getRechargeData();
      }
      // 如果是余额日志
      if (v === 'log') {
        this.init();
      }
      // 如果是提现记录
      if (v === 'withdrawApply') {
        this.getWithdrawApplyData();
      }
    },
    // 充值记录
    getRechargeData () {
      getRecharge(this.rechargeForm).then((res) => {
        if (res.message === 'success') {
          this.rechargeListData = res.result;
        }
      });
    },
    // 提现记录
    getWithdrawApplyData () {
      getWithdrawApply(this.withdrawApplyForm).then((res) => {
        if (res.message === 'success') {
          this.withdrawApplyColumnsListData = res.result;
        }
      });
    },
    // 余额日志
    changePage (v) {
      this.walletForm.pageNumber = v;
      this.init();
    },
    changePageSize (v) {
      this.walletForm.pageNumber = 1;
      this.walletForm.pageSize = v;
      this.init();
    },
    // 充值记录
    rechargeChangePage (v) {
      this.rechargeForm.pageNumber = v;
      this.getRechargeData();
    },
    rechargeChangePageSize (v) {
      this.rechargeForm.pageNumber = 1;
      this.rechargeForm.pageSize = v;
      this.getRechargeData();
    },
    // 提现记录
    withdrawChangePage (v) {
      this.withdrawApplyForm.pageNumber = v;
      this.getWithdrawApplyData();
    },
    withdrawChangePageSize (v) {
      this.withdrawApplyForm.pageNumber = 1;
      this.withdrawApplyForm.pageSize = v;
      this.getWithdrawApplyData();
    },
    // 弹出在线充值框
    recharge () {
      this.formData.price = 1;
      this.modal = true;
    },
    // 在线充值
    rechargePrice () {
      this.$refs['formData'].validate((valid) => {
        if (valid) {
          recharge(this.formData).then((res) => {
            if (res.message === 'success') {
              this.$router.push({
                path: '/payment',
                query: { orderType: 'RECHARGE', sn: res.result.rechargeSn }
              });
            }
          });
        }
      });
    },
    // 申请提现弹出框
    withdrawalApply () {
      this.withdrawApplyFormData.price = 1;
      this.withdrawApplyModal = true;
    },
    // 提现
    withdrawal () {
      this.$refs['withdrawApplyFormData'].validate((valid) => {
        if (valid) {
          withdrawalApply(this.withdrawApplyFormData).then((res) => {
            if (res && res.success) {
              this.$Message.success('提现申请成功，关注提现状态');
              this.withdrawApplyModal = false;
              this.init(); // 余额查询
              this.getWithdrawApplyData(); // 提现记录
            }
          });
        }
      });
    }
  }
};
</script>

<style scoped lang="scss">
.box {
  margin: 20px 0;
}
.page-size {
  margin: 15px 0px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
}
.account-price {
  font-weight: bold;
}
.subTips {
  margin-left: 10px;
}
.account-btns {
  margin: 10px 0;
}
.ivu-btn {
  margin: 0 4px;
}

</style>
