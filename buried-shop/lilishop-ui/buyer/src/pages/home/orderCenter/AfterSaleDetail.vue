<template>
  <div class="order-detail">
    <card _Title="售后详情" :_Size="16"></card>
    <!-- 操作按钮 -->
    <Card class="mb_10" v-if="(afterSale.serviceStatus == 'PASS' &&
          afterSale.serviceType != 'RETURN_MONEY') || (afterSale.afterSaleAllowOperationVO && afterSale.afterSaleAllowOperationVO.cancel)">
      <Button type="success" @click="openModal" v-if="afterSale.serviceStatus == 'PASS' &&
          afterSale.serviceType != 'RETURN_MONEY'" size="small">提交物流</Button>
      <Button type="error" @click="cancel(afterSale.sn)" v-if="afterSale.afterSaleAllowOperationVO && afterSale.afterSaleAllowOperationVO.cancel" size="small">取消售后</Button>
    </Card>
    <div class="order-card">
      <h3>{{afterSale.serviceName}}</h3>
      <p class="global_color fontsize_18">{{ afterSale.orderStatusValue }}</p>
      <p>售后单号：{{ afterSale.sn }} &nbsp;&nbsp;&nbsp;订单号：{{afterSale.orderSn}}</p>
      <div style="color:#999;" class="operation-time">创建时间：{{afterSale.createTime}}</div>
      <div class="service-after">
        <div>
          本次售后服务由<span>{{afterSale.storeName}}</span>为您提供
        </div>
        <div>
          <img :src="afterSale.goodsImage" alt="" width="60" height="60">
          <span>{{afterSale.goodsName}}</span>
        </div>
      </div>
    </div>
    <div class="order-card">
      <h3>售后进程</h3>
      <Steps class="progress" :current="logList.length" direction="vertical">
        <Step
          :content="'操作人：'+ log.operatorName + '   ' + log.createTime"
          :title="log.message"
          v-for="(log, index) in logList"
          :key="index"
        ></Step>
      </Steps>
    </div>
    <div class="order-card">
      <h3 class="mb_10">服务单信息</h3>
      <table border="1" cellpadding='0' cellspacing="0">
        <tr>
          <td>退款方式</td><td>{{afterSale.refundWay == 'ORIGINAL' ? '原路退回' : '账号退款'}}</td>
        </tr>
        <tr>
          <td>申请退款金额</td><td>{{afterSale.applyRefundPrice | unitPrice('￥')}}</td>
        </tr>
        <tr v-if="afterSale.actualRefundPrice">
          <td>实际退款金额</td><td>{{afterSale.actualRefundPrice | unitPrice('￥')}}</td>
        </tr>
        <template v-if="afterSale.refundWay === 'OFFLINE'">
          <tr>
            <td>退款开户行</td><td>{{afterSale.bankDepositName}}</td>
          </tr>
          <tr>
            <td>退款开户名</td><td>{{afterSale.bankAccountName}}</td>
          </tr>
          <tr>
            <td>退款卡号</td><td>{{afterSale.bankAccountNumber}}</td>
          </tr>
        </template>
        <tr>
          <td>商品处理方式</td><td>{{afterSale.serviceType == 'RETURN_MONEY' ? '退款' : '退货'}}</td>
        </tr>
        <tr>
          <td>退款原因</td><td>{{afterSale.reasonName}}</td>
        </tr>
        <tr>
          <td>问题描述</td><td>{{afterSale.problemDesc}}</td>
        </tr>
      </table>
    </div>
    <div class="order-card" v-if="afterSale.afterSaleImage">
      <h3 class="mb_10">图片信息</h3>
      <div v-for="img in afterSale.afterSaleImage.split(',')" :key="img">
        <img :src="img" width="200" height="200" @click="perviewImg(img)" class="hover-pointer" alt="">
      </div>
    </div>
    <Modal v-model="logisticsShow" width="530">
      <p slot="header">
        <span>提交物流信息</span>
      </p>
      <div>
        <Form ref="form" :model="form" label-position="left" :label-width="100" :rules="rules">
          <FormItem label="物流公司" prop="logisticsId">
            <Select v-model="form.logisticsId" placeholder="请选择物流公司">
              <Option v-for="item in companyList" :value="item.id" :key="item.id">{{ item.name }}</Option>
            </Select>
          </FormItem>
          <FormItem label="快递单号" prop="logisticsNo">
            <Input v-model="form.logisticsNo" placeholder="请填写快递单号"></Input>
          </FormItem>
          <FormItem label="发货时间" prop="mDeliverTime">
            <DatePicker type="date" style="width:100%" v-model="form.mDeliverTime" @on-change="changeTime" format="yyyy-MM-dd" placeholder="选择发货时间"></DatePicker>
          </FormItem>
        </Form>
      </div>
      <div slot="footer" style="text-align: right">
        <Button @click="logisticsShow = false">关闭</Button>
        <Button type="primary" :loading="submitLoading" @click="submitDelivery">提交</Button>
      </div>
    </Modal>
  </div>
</template>
<script>
import { afterSaleDetail, afterSaleLog, afterSaleReason, cancelAfterSale } from '@/api/member.js';
import { afterSaleDelivery, getLogisticsCompany } from '@/api/order.js';
import { afterSaleStatusList } from '../enumeration.js'
export default {
  name: 'aftersale-detail',
  data () {
    return {
      afterSale: {}, // 售后详情数据
      logList: [], // 日志
      reasonList: [], // 售后原因列表
      afterSaleStatusList, // 售后状态列表
      companyList: [], // 物流公司列表
      logisticsShow: false, // 物流信息modal
      form: { // 物流信息数据
        afterSaleSn: '',
        logisticsId: '',
        logisticsNo: '',
        mDeliverTime: ''
      },
      rules: { // 必填校验
        logisticsId: [{ required: true, message: '请选择物流公司' }],
        logisticsNo: [{ required: true, message: '请填写物流编号' }],
        mDeliverTime: [{ required: true, message: '请选择发货时间' }]
      },
      submitLoading: false // 提交加载状态
    };
  },
  methods: {
    getDetail () { // 售后详情
      afterSaleDetail(this.$route.query.sn).then(res => {
        if (res.success) {
          this.afterSale = res.result;
          this.afterSale.serviceName = this.filterOrderStatus(this.afterSale.serviceStatus)
          // 如果是原因id，则调接口查询原因中文释义
          const pattern3 = new RegExp('[0-9]+');
          if (pattern3.test(this.afterSale.reason)) {
            this.getReason(this.afterSale.serviceType)
          } else {
            this.$set(this.afterSale, 'reasonName', this.afterSale.reason)
          }
        }
      })
    },
    getReason (type) { // 获取售后原因
      afterSaleReason(type).then(res => {
        if (res.success) {
          this.reasonList = res.result
          this.reasonList.forEach(e => {
            if (e.id === this.afterSale.reason) {
              this.$set(this.afterSale, 'reasonName', e.reason)
            }
          })
        }
      })
    },
    getLog () { // 获取售后日志
      afterSaleLog(this.$route.query.sn).then(res => {
        this.logList = res.result;
      })
    },
    filterOrderStatus (status) { // 获取订单状态中文
      const ob = this.afterSaleStatusList.filter(e => { return e.status === status });
      if (ob.length) return ob[0].name
    },
    perviewImg (img) { // 查看图片
      window.open(img, '_blank')
    },
    cancel (sn) { // 取消售后申请
      this.$Modal.confirm({
        title: '取消',
        content: '<p>确定取消此次售后申请吗？</p>',
        onOk: () => {
          cancelAfterSale(sn).then(res => {
            if (res.success) {
              this.$Message.success('取消售后申请成功')
              this.getDetail()
            }
          })
        },
        onCancel: () => {}
      });
    },
    // 获取物流公司列表
    getCompany () {
      getLogisticsCompany().then(res => {
        if (res.success) {
          this.companyList = res.result
        }
      })
    },
    // 提交物流信息
    submitDelivery () {
      this.submitLoading = true
      afterSaleDelivery(this.form).then(res => {
        if (res.success) {
          this.logisticsShow = false;
          this.$Message.success('提交成功')
          this.getDetail()
        }
        this.submitLoading = false
      }).catch(() => {
        this.submitLoading = false
      })
    },
    // 提交物流modal
    openModal () {
      this.form.afterSaleSn = this.afterSale.sn
      this.logisticsShow = true;
      this.$refs.form.resetFields()
      if (!this.companyList.length) {
        this.getCompany()
      }
    },
    // 格式化时间
    changeTime (time) {
      this.form.mDeliverTime = time;
    }
  },
  mounted () {
    this.getDetail();
    this.getLog()
  }
};
</script>
<style lang="scss" scoped>
.order-card {
  padding: 10px;
  padding-bottom: 10px;
  margin-bottom: 10px;
  border-bottom: 1px solid #e8eaec;
  position: relative;
  .global_color {
    color: $theme_color;
  }
  p {
    color: #999;
    margin: 3px;
  }
  h3 {
    font-weight: normal;
    font-size: 16px;
  }
  .operation-time {
    position: absolute;
    right: 20px;
    top: 10px;
  }
  &:last-child{
    border: none;
  }
}
.service-after {
  border: 1px solid #eee;
  display: flex;
  height: 80px;
  padding: 10px;
  >div:nth-child(1) {
    width: 400px;
    font-size: 15px;
    font-weight: bold;
    text-align: center;
    line-height: 60px;
    span{color: $theme_color;}
    border-right: 1px solid #eee;
  }
  >div:nth-child(2){
    padding-left: 15px;
    img{vertical-align: top;}
    span{color: #999;}
  }
}
/** 售后进度条 */
.progress {
  margin: 15px 0;
}
table{
  border-color: #eee;
  width: 100%;
  color: #999;
  tr{
    td:nth-child(1){
      width: 200px;
    }
  }
  td{
    padding: 5px;
  }
}
</style>
