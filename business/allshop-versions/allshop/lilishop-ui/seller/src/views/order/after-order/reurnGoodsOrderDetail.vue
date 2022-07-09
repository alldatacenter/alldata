<template>
  <div class="search">
    <Row>
      <Col>
      <Card>
        <div class="main-content">
          <div class="div-flow-left">
            <div class="div-form-default">
              <h3>售后申请</h3>
              <dl>
                <dt>售后状态</dt>
                <dd>{{filterStatus(afterSaleInfo.serviceStatus)}}</dd>
              </dl>

              <dl>
                <dt>退货退款编号</dt>
                <dd>{{ afterSaleInfo.sn }}</dd>
              </dl>
              <dl>
                <dt>退货退款原因</dt>
                <dd>{{ afterSaleInfo.reason }}</dd>
              </dl>
              <dl>
                <dt>申请退款金额</dt>
                <dd>{{ afterSaleInfo.applyRefundPrice | unitPrice('￥') }}</dd>
              </dl>
              <dl v-if="afterSaleInfo.actualRefundPrice">
                <dt>实际退款金额</dt>
                <dd>{{ afterSaleInfo.actualRefundPrice | unitPrice('￥') }}</dd>
              </dl>
              <dl v-if="afterSaleInfo.refundPoint">
                <dt>退还积分</dt>
                <dd>{{ afterSaleInfo.refundPoint }}</dd>
              </dl>
              <dl>
                <dt>退货数量</dt>
                <dd>{{ afterSaleInfo.num }}</dd>
              </dl>
              <dl>
                <dt>问题描述</dt>
                <dd>{{ afterSaleInfo.problemDesc }}</dd>
              </dl>
              <dl>
                <dt>凭证</dt>
                <dd v-if="afterSaleImage == ''">
                  暂无凭证
                </dd>
                <dd v-else>
                  <div class="div-img" @click="()=>{picFile=item; picVisible = true}" v-for="(item, index) in afterSaleImage" :key="index">
                    <img class="complain-img" :src="item">

                  </div>

                  <Modal footer-hide mask-closable v-model="picVisible">
                    <img :src="picFile" alt="无效的图片链接" style="width: 100%; margin: 0 auto; display: block" />
                  </Modal>

                </dd>
              </dl>
            </div>

            <div class="div-form-default" v-if="afterSaleInfo.serviceStatus=='APPLY'">
              <h3>商家处理意见</h3>
              <dl>
                <dt>商家</dt>
                <dd>
                  <div class="div-content">
                    {{ afterSaleInfo.storeName }}
                  </div>

                </dd>
              </dl>
              <dl>
                <dt>是否同意</dt>
                <dd>
                  <div class="div-content">
                    <RadioGroup type="button" button-style="solid" v-model="params.serviceStatus">
                      <Radio label="PASS">
                        <span>同意</span>
                      </Radio>
                      <Radio label="REFUSE">
                        <span>拒绝</span>
                      </Radio>
                    </RadioGroup>
                  </div>

                </dd>
              </dl>

              <dl>
                <dt>申请退款金额</dt>
                <dd>{{ afterSaleInfo.applyRefundPrice | unitPrice('￥') }}</dd>
              </dl>
              <dl v-if="params.serviceStatus == 'PASS'">
                <dt>实际退款金额</dt>
                <dd>
                  <Input v-model="params.actualRefundPrice" style="width:260px" />
                </dd>
              </dl>
              <dl>
                <dt>备注信息</dt>
                <dd>
                  <Input v-model="params.remark" type="textarea" maxlength="200" :rows="4" clearable style="width:260px" />
                </dd>
              </dl>
              <dl>
                <dt></dt>
                <dd>
                  <div style="text-align: right;width: 45%;margin-top: 10px">
                    <Button type="primary" :loading="submitLoading" @click="handleSubmit" style="margin-left: 5px">
                      确定
                    </Button>
                  </div>
                </dd>
              </dl>
            </div>
            <div class="div-form-default" v-if="afterSaleInfo.serviceStatus !='APPLY'">
              <h3>商家处理</h3>
              <dl>
                <dt>商家</dt>
                <dd>
                  <div class="div-content">
                    {{ afterSaleInfo.storeName }}
                  </div>

                </dd>
              </dl>
              <dl>
                <dt>审核结果</dt>
                <dd>
                  <div class="div-content">
                    <span v-if="params.serviceStatus=='PASS'">
                      审核通过
                    </span>
                    <span v-else>
                      审核拒绝
                    </span>
                  </div>

                </dd>
              </dl>
              <dl>
                <dt>备注信息</dt>
                <dd>
                  {{ afterSaleInfo.auditRemark }}
                </dd>
              </dl>
            </div>
          </div>
          <div class="div-flow-center">

          </div>
          <div class="div-flow-right">
            <div class="div-form-default">
              <h3>相关商品交易信息</h3>
              <dl>
                <dt>
                  <img :src="afterSaleInfo.goodsImage" height="60px">
                </dt>
                <dd>
                  <a>{{ afterSaleInfo.goodsName }}</a><br>
                  <span>{{ afterSaleInfo.num }}(数量)</span><br>

                </dd>
              </dl>

            </div>
            <div class="div-form-default">
              <h3>订单相关信息</h3>
              <dl>
                <dt>
                  订单编号
                </dt>
                <dd>
                  {{ afterSaleInfo.orderSn }}
                </dd>
              </dl>
              <dl v-if="afterSaleInfo.bankDepositName">
                <dt>银行开户行</dt>
                <dd>
                  {{afterSaleInfo.bankDepositName}}
                </dd>
              </dl>
              <dl v-if="afterSaleInfo.bankAccountName">
                <dt>银行开户名</dt>
                <dd>
                  {{afterSaleInfo.bankAccountName}}
                </dd>
              </dl>
              <dl v-if="afterSaleInfo.bankAccountNumber">
                <dt>银行卡号</dt>
                <dd>
                  {{afterSaleInfo.bankAccountNumber}}
                </dd>
              </dl>

            </div>
            <div class="div-form-default" v-if="afterSaleInfo.serviceStatus =='BUYER_RETURN' || afterSaleInfo.serviceStatus =='COMPLETE' &&  afterSaleInfo.serviceType !='RETURN_MONEY'">
              <h3>回寄物流信息</h3>
              <dl>
                <dt>
                  物流公司
                </dt>
                <dd>
                  {{ afterSaleInfo.mlogisticsName }}
                </dd>
              </dl>
              <dl>
                <dt>
                  物流单号
                </dt>
                <dd>
                  {{ afterSaleInfo.mlogisticsNo }}
                </dd>
              </dl>
              <dl>
                <dt>操作</dt>
                <dd>
                  <Button type="info" :loading="submitLoading" @click="sellerConfirmSubmit('PASS')" style="margin-left: 5px" v-if="afterSaleInfo.afterSaleAllowOperationVO.rog">
                    确认收货
                  </Button>
                  <Button type="primary" :loading="submitLoading" @click="sellerConfirmSubmit('REFUSE')" style="margin-left: 5px" v-if="afterSaleInfo.afterSaleAllowOperationVO.rog">
                    拒收
                  </Button>
                  <Button type="default" :loading="submitLoading" @click="logisticsBuyer()" style="margin-left: 5px">
                    查询物流
                  </Button>
                </dd>
              </dl>

            </div>
            <div class="div-form-default" v-if="afterSaleInfo.afterSaleAllowOperationVO.return_goods && afterSaleInfo.serviceType == 'EXCHANGE_GOODS'">
              <h3>换货</h3>
              <dl>
                <dt>
                  换货
                </dt>
                <dd>
                  <Button type="primary" :loading="submitLoading" @click="exchangeGoods" style="margin-left: 5px">
                    发货
                  </Button>
                </dd>
              </dl>
            </div>
            <div class="div-form-default" v-if=" afterSaleInfo.serviceType == 'EXCHANGE_GOODS' && afterSaleInfo.serviceStatus =='SELLER_RE_DELIVERY'">
              <h3>物流信息</h3>
              <dl>
                <dt>
                  物流公司
                </dt>
                <dd>
                  {{ afterSaleInfo.slogisticsName }}
                </dd>
              </dl>
              <dl>
                <dt>
                  物流单号
                </dt>
                <dd>
                  {{ afterSaleInfo.slogisticsNo }}
                </dd>
              </dl>
              <dl>
                <dt>操作</dt>
                <dd>
                  <Button type="primary" :loading="submitLoading" @click="logisticsSeller()" style="margin-left: 5px">
                    查询物流
                  </Button>
                </dd>
              </dl>

            </div>

          </div>
        </div>

      </Card>
      </Col>
    </Row>
    <!-- 订单发货 -->
    <Modal v-model="modalVisible" width="500px">
      <p slot="header">
        <span>订单发货</span>
      </p>
      <div>
        <Form ref="form" :model="form" :label-width="90" :rules="formValidate" style="position:relative">
          <FormItem label="物流公司" prop="logisticsId">
            <Select v-model="form.logisticsId" placeholder="请选择" style="width:250px">
              <Option v-for="(item, i) in checkedLogistics" :key="i" :value="item.id">{{ item.name }}
              </Option>
            </Select>
          </FormItem>
          <FormItem label="物流单号" prop="logisticsNo">
            <Input v-model="form.logisticsNo" style="width:250px" />
          </FormItem>
        </Form>

      </div>

      <div slot="footer" style="text-align: right">
        <Button size="large" @click="modalVisible = false">取消</Button>
        <Button type="success" size="large" @click="orderDeliverySubmit">发货</Button>

      </div>
    </Modal>
    <!-- 查询物流 -->
    <Modal v-model="logisticsModal" width="40">
      <p slot="header">
        <span>查询物流</span>
      </p>
      <div class="layui-layer-wrap">
        <dl>
          <dt>售后单号：</dt>
          <dd>
            <div class="text-box">{{ sn }}</div>
          </dd>
        </dl>
        <dl>
          <dt>物流公司：</dt>
          <dd>
            <div class="text-box">{{ logisticsInfo.shipper }}</div>
          </dd>
        </dl>
        <dl>
          <dt>快递单号：</dt>
          <dd>
            <div nctype="ordersSn" class="text-box">{{ logisticsInfo.logisticCode }}</div>
          </dd>
        </dl>
        <div class="div-express-log">
          <ul class="express-log">
            <li v-for="(item,index) in logisticsInfo.traces" :key="index">
              <span class="time">{{ item.AcceptTime }}</span>
              <span class="detail">{{ item.AcceptStation }}</span>
            </li>
          </ul>
        </div>

      </div>

      <div slot="footer" style="text-align: right">
        <Button @click="logisticsModal = false">取消</Button>
      </div>
    </Modal>
  </div>
</template>

<script>
import * as API_Order from "@/api/order";
import uploadPicThumb from "@/views/my-components/lili/upload-pic-thumb";

export default {
  name: "orderComplaint",
  components: {
    uploadPicThumb,
  },
  data() {
    return {
      picFile: "", // 预览图片地址
      picVisible: false, // 预览图片
      sn: "", // 订单号
      logisticsModal: false, //查询物流模态框
      logisticsInfo: {}, //物流信息
      form: {
        // 物流信息
        logisticsNo: "",
        logisticsId: "",
      }, //换货发货form
      formValidate: {
        logisticsNo: [
          { required: true, message: "发货单号不能为空", trigger: "change" },
        ],
        logisticsId: [
          { required: true, message: "请选择物流公司", trigger: "blur" },
        ],
      },
      modalVisible: false, // 添加或编辑显示
      afterSaleInfo: {
        // 售后信息
        afterSaleAllowOperationVO: {
          return_goods: false,
        },
      },
      afterSaleImage: [], //会员申诉图片
      appealImages: [], //商家申诉的图片
      submitLoading: false, // 添加或编辑提交状态
      checkedLogistics: [], //选中的物流公司集合
      //商家处理意见
      params: {
        serviceStatus: "PASS",
        remark: "",
        actualRefundPrice: 0,
      },
      // 售后状态
      afterSaleStatus: [
        { status: "APPLY", label: "申请售后" },
        { status: "PASS", label: "申请通过" },
        { status: "REFUSE", label: "申请拒绝" },
        { status: "BUYER_RETURN", label: "买家退货，待卖家收货" },
        { status: "SELLER_RE_DELIVERY", label: "商家换货" },
        { status: "SELLER_CONFIRM", label: "卖家确认收货" },
        { status: "SELLER_TERMINATION", label: "卖家终止售后" },
        { status: "BUYER_CONFIRM", label: "买家确认收货" },
        { status: "BUYER_CANCEL", label: "买家取消售后" },
        { status: "WAIT_REFUND", label: "等待平台退款" },
        { status: "COMPLETE", label: "已完成" },
      ],
    };
  },
  methods: {
    // 获取售后详情
    getDetail() {
      this.loading = true;
      API_Order.afterSaleOrderDetail(this.sn).then((res) => {
        this.loading = false;
        if (res.success) {
          this.afterSaleInfo = res.result;
          this.afterSaleImage = (res.result.afterSaleImage || "").split(",");
          this.params.actualRefundPrice = res.result.applyRefundPrice;
        }
      });
    },
    //换货弹出框
    exchangeGoods() {
      API_Order.getLogisticsChecked().then((res) => {
        if (res.success) {
          this.checkedLogistics = res.result;
          this.modalVisible = true;
          this.getDetail();
        }
      });
    },
    //商家确认收货
    sellerConfirmSubmit(type) {
      let title = "确认收货";
      let content = "请确认已经收到退货货物?";
      let message = "收货成功";
      if (type !== "PASS") {
        title = "确认拒收";
        content = "确认拒收此货物？";
        message = "拒收成功";
        this.params.serviceStatus = "REFUSE";
      }

      this.$Modal.confirm({
        title: title,
        content: content,
        loading: true,
        onOk: () => {
          API_Order.afterSaleSellerConfirm(this.sn, this.params).then((res) => {
            this.$Modal.remove();
            if (res.success) {
              this.$Message.success(message);
              this.getDetail();
            }
          });
        },
      });
    },
    //查询物流
    logisticsSeller() {
      this.logisticsModal = true;
      API_Order.getSellerDeliveryTraces(this.sn).then((res) => {
        if (res.success && res.result != null) {
          this.logisticsInfo = res.result;
        }
      });
    },
    //查询物流
    logisticsBuyer() {
      this.logisticsModal = true;
      API_Order.getAfterSaleTraces(this.sn).then((res) => {
        if (res.success && res.result != null) {
          this.logisticsInfo = res.result;
        }
      });
    },

    //换货发货
    orderDeliverySubmit() {
      this.$refs.form.validate((valid) => {
        if (valid) {
          API_Order.afterSaleSellerDelivery(this.sn, this.form).then((res) => {
            if (res.success) {
              this.$Message.success("订单发货成功");
              this.modalVisible = false;
              this.getDataDetail();
            }
          });
        }
      });
    },
    //回复
    handleSubmit() {
      this.submitLoading = true;
      if (this.params.remark == "") {
        this.$Message.error("请输入备注信息");
        return;
      }
      API_Order.afterSaleSellerReview(this.sn, this.params).then((res) => {
        this.submitLoading = false;
        if (res.success) {
          this.$Message.success("审核成功");
          this.params.remark = "";
          this.getDetail();
        }
      });
    },
    // 返回售后状态中文描述
    filterStatus(status) {
      let label = '';
      for (let i = 0; i < this.afterSaleStatus.length; i++) {
        const obj = this.afterSaleStatus[i];
        if (obj.status === status) {
          label = obj.label;
          break;
        }
      }
      return label;
    },
  },
  mounted() {
    this.sn = this.$route.query.sn;
    this.getDetail();
  },
  // 如果是从详情页返回列表页，修改列表页keepAlive为true，确保不刷新页面
  beforeRouteLeave(to, from, next){
    if(to.name === 'returnGoodsOrder' || to.name === 'returnMoneyOrder') {
      to.meta.keepAlive = true
    }
    next()
  }
};
</script>
<style lang="scss" scoped>
.ivu-row {
  display: block !important;
}

.main-content {
  min-height: 600px;
  padding: 10px;
}

.div-flow-left {
  width: 49%;
  letter-spacing: normal;
  display: inline-block;
  border-right: solid #f5f5f5 1px;

  .div-form-default {
    width: 97%;

    h3 {
      font-weight: 600;
      line-height: 22px;
      background-color: #f5f5f5;
      padding: 6px 0 6px 12px;
      border-bottom: solid 1px #e7e7e7;
    }

    dl {
      font-size: 0;
      line-height: 30px;
      clear: both;
      padding: 0;
      margin: 0;
      border-bottom: dotted 1px #e6e6e6;
      overflow: hidden;
      display: flex;

      dt {
        display: inline-block;
        flex: 2;
        vertical-align: top;
        text-align: right;
        padding: 15px 1% 15px 0;
        margin: 0;
        font-size: 12px;
      }

      dd {
        flex: 10;
        display: inline-block;

        padding: 15px 0 15px 1%;
        margin: 0;
        border-left: 1px solid #f0f0f0;
        font-size: 12px;
      }
    }
  }
}

dl dt {
  width: 100px;
  text-align: right;
}

.div-express-log {
  max-height: 300px;
  border: solid 1px #e7e7e7;
  background: #fafafa;
  overflow-y: auto;
  overflow-x: auto;
}

.express-log {
  margin-right: -10px;
  margin: 5px;
  padding: 10px;
  list-style-type: none;

  .time {
    width: 30%;
    display: inline-block;
    float: left;
  }

  .detail {
    width: 60%;
    margin-left: 30px;
    display: inline-block;
  }

  li {
    line-height: 30px;
  }
}

.layui-layer-wrap {
  dl {
    border-top: solid 1px #f5f5f5;
    margin-top: -1px;
    overflow: hidden;

    dt {
      font-size: 14px;
      line-height: 28px;
      display: inline-block;
      padding: 8px 1% 8px 0;
      color: #999;
    }

    dd {
      font-size: 14px;
      line-height: 28px;
      display: inline-block;
      padding: 8px 0 8px 8px;
      border-left: solid 1px #f5f5f5;

      .text-box {
        line-height: 40px;
        color: #333;
        word-break: break-all;
      }
    }
  }
}

.div-img {
  width: 130px;
  height: 130px;
  text-align: center;
  float: left;
}

.div-flow-center {
  width: 2%;
  display: inline-block;
}

.div-flow-right {
  width: 49%;
  vertical-align: top;
  word-spacing: normal;
  display: inline-block;

  .div-form-default {
    width: 97%;

    h3 {
      font-weight: 600;
      line-height: 22px;
      background-color: #f5f5f5;
      padding: 6px 0 6px 12px;
      border-bottom: solid 1px #e7e7e7;
    }

    dl {
      font-size: 0;
      line-height: 30px;
      clear: both;
      padding: 0;
      margin: 0;
      border-bottom: dotted 1px #e6e6e6;
      overflow: hidden;

      dt {
        display: inline-block;
        width: 13%;
        vertical-align: top;
        text-align: right;
        padding: 15px 1% 15px 0;
        margin: 0;
        font-size: 12px;
      }

      dd {
        display: inline-block;
        width: 84%;
        padding: 15px 0 15px 1%;
        margin: 0;
        border-left: 1px solid #f0f0f0;
        font-size: 12px;
      }
    }
  }
}

.complain-img {
  width: 120px;
  height: 120px;
  text-align: center;
}
</style>
