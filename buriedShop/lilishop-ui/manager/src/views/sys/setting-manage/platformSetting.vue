<template>
  <div>
    <Card>
      <Tabs v-model="tabName" :animated="false" style="overflow: visible">
        <TabPane label="基础设置" name="base">
          <div style="display:flex;position:relative">
            <Form
              ref="baseForm"
              :model="base"
              :label-width="140"
              label-position="right"
              :rules="baseValidate"
            >
              <FormItem label="网站名称" prop="siteName">
                <Input type="text" v-model="base.siteName" placeholder="请输入网站名称" style="width: 350px"/>
              </FormItem>
              <FormItem label="ICP证书号" prop="icp">
                <Input type="text" v-model="base.icp" placeholder="请输入ICP证书号"
                       style="width: 350px"/>
              </FormItem>
              <FormItem label="Logo" prop="logo">
                <upload-pic-input v-model="base.logo" style="width: 350px"></upload-pic-input>
              </FormItem>
              <FormItem label="商家中心Logo" prop="sellerLogo">
                <upload-pic-input v-model="base.sellerLogo" style="width: 350px"></upload-pic-input>
              </FormItem>
              <FormItem>
                <Button type="primary" style="width: 100px;margin-right:5px" :loading="saveLoading"
                        @click="saveBase">保存
                </Button>
              </FormItem>
            </Form>
            <Spin fix v-if="loading"></Spin>
          </div>
        </TabPane>
        <TabPane label="积分设置" name="point">
          <div style="display:flex;position:relative">
            <Form
                    ref="pointForm"
                    :model="point"
                    :label-width="140"
                    label-position="right"
                    :rules="pointValidate"
            >
              <FormItem label="注册" prop="register">
                <Input type="text" v-model="point.register" placeholder="请输入注册赠送积分"
                       style="width: 350px"/>
              </FormItem>
              <FormItem label="登陆" prop="login">
                <Input type="text" v-model="point.login" placeholder="请输入登陆赠送积分"
                       style="width: 350px"/>
              </FormItem>
              <FormItem label="消费一元" prop="money">
                <Input type="text" v-model="point.money" placeholder="请输入积分"
                       style="width: 350px"/>
              </FormItem>
              <FormItem>
                <Button type="primary" style="width: 100px;margin-right:5px" :loading="saveLoading"
                        @click="savePoint">保存
                </Button>
              </FormItem>
            </Form>
            <Spin fix v-if="loading"></Spin>
          </div>
        </TabPane>
        <TabPane label="订单设置" name="order">
          <div style="display:flex;position:relative">
            <Form
                    ref="orderForm"
                    :model="order"
                    :label-width="140"
                    label-position="right"
                    :rules="orderValidate"
            >
              <FormItem label="自动取消 分钟" prop="autoCancel">
                <Input type="text" v-model="order.autoCancel" placeholder="请输入自动取消分钟"
                       style="width: 350px"/>
              </FormItem>
              <FormItem label="自动收货 天" prop="autoReceive">
                <Input type="text" v-model="order.autoReceive" placeholder="请输入自动收货天数"
                       style="width: 350px"/>
              </FormItem>
              <FormItem label="自动收货 天" prop="autoComplete">
                <Input type="text" v-model="order.autoComplete" placeholder="请输入自动完成天数"
                       style="width: 350px"/>
              </FormItem>
              <FormItem>
                <Button type="primary" style="width: 100px;margin-right:5px" :loading="saveLoading"
                        @click="saveOrder">保存
                </Button>
              </FormItem>
            </Form>
            <Spin fix v-if="loading"></Spin>
          </div>
        </TabPane>
        <TabPane label="商品设置" name="goods">
          <div style="display:flex;position:relative">
            <Form
                    ref="goodsForm"
                    :model="goods"
                    :label-width="140"
                    label-position="right"
                    :rules="goodsValidate"
            >
              <FormItem label="是否开启商品审核" prop="goodsCheck">
                <RadioGroup type="button" button-style="solid" v-model="goods.goodsCheck">
                  <Radio label="OPEN">开启</Radio>
                  <Radio label="CLOSE">关闭</Radio>
                </RadioGroup>
              </FormItem>
              <FormItem label="商品页面小图宽度" prop="smallPictureWidth">
                <Input type="text" v-model="goods.smallPictureWidth" placeholder="商品页面小图宽度"
                       style="width: 350px"/>
              </FormItem>
              <FormItem label="商品页面小图高度" prop="smallPictureHeight">
                <Input type="text" v-model="goods.smallPictureHeight" placeholder="商品页面小图高度"
                       style="width: 350px"/>
              </FormItem>
              <FormItem label="缩略图宽度" prop="abbreviationPictureWidth">
                <Input type="text" v-model="goods.abbreviationPictureWidth" placeholder="缩略图宽度"
                       style="width: 350px"/>
              </FormItem>
              <FormItem label="缩略图高度" prop="abbreviationPictureHeight">
                <Input type="text" v-model="goods.abbreviationPictureHeight" placeholder="缩略图高度"
                       style="width: 350px"/>
              </FormItem>
              <FormItem label="原图宽" prop="originalPictureWidth">
                <Input type="text" v-model="goods.originalPictureWidth" placeholder="原图宽"
                       style="width: 350px"/>
              </FormItem>
              <FormItem label="原图高" prop="originalPictureHeight">
                <Input type="text" v-model="goods.originalPictureHeight" placeholder="原图高"
                       style="width: 350px"/>
              </FormItem>
              <FormItem>
                <Button type="primary" style="width: 100px;margin-right:5px" :loading="saveLoading"
                        @click="saveGoods">保存
                </Button>
              </FormItem>
            </Form>
            <Spin fix v-if="loading"></Spin>
          </div>
        </TabPane>
        <TabPane label="信任登陆" name="trust">
          <div>
            <Row style="background:#eee;padding:10px;" :gutter="16">
              <Col span="12">
                <Card>
                  <p slot="title">微信信任登陆</p>
                  <Form
                          ref="wechatForm"
                          :model="wechat"
                          :label-width="140"
                          label-position="right"
                  >
                    <FormItem label="appId" prop="appId">
                      <Input type="text" v-model="wechat.appId" placeholder="appId"
                             style="width: 350px"/>
                    </FormItem>
                    <FormItem label="appSecret" prop="appSecret">
                      <Input type="text" v-model="wechat.appSecret" placeholder="appSecret"
                             style="width: 350px"/>
                    </FormItem>
                    <FormItem label="callbackUrl" prop="callbackUrl">
                      <Input type="text" v-model="wechat.callbackUrl" placeholder="callbackUrl"
                             style="width: 350px"/>
                    </FormItem>
                    <FormItem label="callbackLoginUrl" prop="callbackLoginUrl">
                      <Input type="text" v-model="wechat.callbackLoginUrl"
                             placeholder="callbackLoginUrl"
                             style="width: 350px"/>
                    </FormItem>
                    <FormItem label="callbackBindUrl" prop="callbackBindUrl">
                      <Input type="text" v-model="wechat.callbackBindUrl"
                             placeholder="callbackBindUrl"
                             style="width: 350px"/>
                    </FormItem>
                    <FormItem>
                      <Button type="primary" style="width: 100px;margin-right:5px"
                              :loading="saveLoading"
                              @click="saveWechat">保存
                      </Button>
                    </FormItem>
                  </Form>
                  <Spin fix v-if="loading"></Spin>
                </Card>
              </Col>
              <Col span="12">
                <Card>
                  <p slot="title">QQ信任登陆</p>
                  <Form
                          ref="qqForm"
                          :model="qq"
                          :label-width="140"
                          label-position="right"
                  >
                    <FormItem label="appId" prop="appId">
                      <Input type="text" v-model="qq.appId" placeholder="appId"
                             style="width: 350px"/>
                    </FormItem>
                    <FormItem label="appKey" prop="appKey">
                      <Input type="text" v-model="qq.appKey" placeholder="appKey"
                             style="width: 350px"/>
                    </FormItem>
                    <FormItem label="callbackUrl" prop="callbackUrl">
                      <Input type="text" v-model="qq.callbackUrl" placeholder="callbackUrl"
                             style="width: 350px"/>
                    </FormItem>
                    <FormItem label="callbackLoginUrl" prop="callbackLoginUrl">
                      <Input type="text" v-model="qq.callbackLoginUrl"
                             placeholder="callbackLoginUrl"
                             style="width: 350px"/>
                    </FormItem>
                    <FormItem label="callbackBindUrl" prop="callbackBindUrl">
                      <Input type="text" v-model="qq.callbackBindUrl"
                             placeholder="callbackBindUrl"
                             style="width: 350px"/>
                    </FormItem>
                    <FormItem>
                      <Button type="primary" style="width: 100px;margin-right:5px"
                              :loading="saveLoading"
                              @click="saveQQ">保存
                      </Button>
                    </FormItem>
                  </Form>
                  <Spin fix v-if="loading"></Spin>
                </Card>
              </Col>
            </Row>
            <Row style="background:#eee;padding:10px;" :gutter="16">
              <Col span="12">
                <Card>
                  <p slot="title">微博信任登陆</p>
                  <Form
                          ref="weiboForm"
                          :model="weibo"
                          :label-width="140"
                          label-position="right"
                  >
                    <FormItem label="appKey" prop="appKey">
                      <Input type="text" v-model="weibo.appKey" placeholder="appKey"
                             style="width: 350px"/>
                    </FormItem>
                    <FormItem label="appSecret" prop="appSecret">
                      <Input type="text" v-model="weibo.appSecret" placeholder="appSecret"
                             style="width: 350px"/>
                    </FormItem>
                    <FormItem label="callbackUrl" prop="callbackUrl">
                      <Input type="text" v-model="weibo.callbackUrl" placeholder="callbackUrl"
                             style="width: 350px"/>
                    </FormItem>
                    <FormItem label="callbackLoginUrl" prop="callbackLoginUrl">
                      <Input type="text" v-model="weibo.callbackLoginUrl"
                             placeholder="callbackLoginUrl"
                             style="width: 350px"/>
                    </FormItem>
                    <FormItem label="callbackBindUrl" prop="callbackBindUrl">
                      <Input type="text" v-model="weibo.callbackBindUrl"
                             placeholder="callbackBindUrl"
                             style="width: 350px"/>
                    </FormItem>
                    <FormItem>
                      <Button type="primary" style="width: 100px;margin-right:5px"
                              :loading="saveLoading"
                              @click="saveWeibo">保存
                      </Button>
                    </FormItem>
                  </Form>
                  <Spin fix v-if="loading"></Spin>
                </Card>
              </Col>
              <Col span="12">
                <Card>
                  <p slot="title">支付宝信任登陆</p>
                  <Form
                          ref="alipayForm"
                          :model="alipay"
                          :label-width="140"
                          label-position="right"
                  >
                    <FormItem label="appId" prop="appId">
                      <Input type="text" v-model="alipay.appId" placeholder="appId"
                             style="width: 350px"/>
                    </FormItem>
                    <FormItem label="appSecret" prop="appSecret">
                      <Input type="text" v-model="alipay.appSecret" placeholder="appSecret"
                             style="width: 350px"/>
                    </FormItem>
                    <FormItem label="callbackUrl" prop="callbackUrl">
                      <Input type="text" v-model="alipay.callbackUrl" placeholder="callbackUrl"
                             style="width: 350px"/>
                    </FormItem>
                    <FormItem label="callbackLoginUrl" prop="callbackLoginUrl">
                      <Input type="text" v-model="alipay.callbackLoginUrl"
                             placeholder="callbackLoginUrl"
                             style="width: 350px"/>
                    </FormItem>
                    <FormItem label="callbackBindUrl" prop="callbackBindUrl">
                      <Input type="text" v-model="alipay.callbackBindUrl"
                             placeholder="callbackBindUrl"
                             style="width: 350px"/>
                    </FormItem>
                    <FormItem>
                      <Button type="primary" style="width: 100px;margin-right:5px"
                              :loading="saveLoading"
                              @click="saveAlipay">保存
                      </Button>
                    </FormItem>
                  </Form>
                  <Spin fix v-if="loading"></Spin>
                </Card>
              </Col>
            </Row>
            <Spin fix v-if="loading"></Spin>
          </div>
        </TabPane>
      </Tabs>
    </Card>
  </div>
</template>

<script>
  import {
    getParams,
    editParams
  } from "@/api/platform.js";

  import uploadPicInput from "@/views/my-components/lili/upload-pic-input";

  import {regular} from '@/utils'

  export default {
    name: "setting-manage",
    components: {
      uploadPicInput
    },
    data() {
      return {
        tabName: "base", // tab栏名字
        loading: false, // 表单加载状态
        saveLoading: false, // 保存加载状态
        base: { // 基本设置
          siteName: "",
          icp: "",
          logo: "",
          sellerLogo: "",
        },
        point: { // 积分设置
          register: "",
          login: "",
          money: ""
        },
        order: { // 订单设置
          autoCancel: "",
          autoReceive: "",
          autoComplete: ""
        },
        goods: { // 商品设置
          goodsCheck: "OPEN",
          smallPictureWidth: "",
          smallPictureHeight: "",
          abbreviationPictureWidth: "",
          abbreviationPictureHeight: "",
          originalPictureWidth: "",
          originalPictureHeight: ""
        },
        wechat: { // 微信设置
          appId: "",
          appSecret: "",
          callbackUrl: "",
          callbackLoginUrl: "",
          callbackBindUrl: ""
        },
        qq: { // qq设置
          appId: "",
          appKey: "",
          callbackUrl: "",
          callbackLoginUrl: "",
          callbackBindUrl: ""
        },
        weibo: { // 微博
          appKey: "",
          appSecret: "",
          callbackUrl: "",
          callbackLoginUrl: "",
          callbackBindUrl: ""
        },
        alipay: { // 阿里
          appId: "",
          appSecret: "",
          callbackUrl: "",
          callbackLoginUrl: "",
          callbackBindUrl: ""
        },
        baseValidate: {
          // 表单验证规则
          siteName: [{required: true, message: "不能为空", trigger: "blur"}],
          icp: [{required: true, message: "不能为空", trigger: "blur"}],
          logo: [{required: true, message: "不能为空", trigger: "blur"}],
          sellerLogo: [{required: true, message: "不能为空", trigger: "blur"}]
        },
        pointValidate: {
          // 表单验证规则
          register: [{
            required: true,
            validator: (rule, value, callback) => {
              if (!regular.integer.test(value)) {
                callback(new Error('请输入正整数，且不为零！'))
              } else {
                callback()
              }
            },
            trigger: 'blur'
          }],
          login: [{
            required: true,
            validator: (rule, value, callback) => {
              if (!regular.integer.test(value)) {
                callback(new Error('请输入正整数，且不为零！'))
              } else {
                callback()
              }
            },
            trigger: 'blur'
          }],
          money: [
            {
              required: true,
              validator: (rule, value, callback) => {
                if (!regular.integer.test(value)) {
                  callback(new Error('请输入正整数，且不为零！'))
                } else {
                  callback()
                }
              },
              trigger: 'blur'
            }]
        },
        orderValidate: {
          // 表单验证规则
          autoCancel: [{
            required: true,
            validator: (rule, value, callback) => {
              if (!regular.integer.test(value)) {
                callback(new Error('请输入正整数，且不为零！'))
              } else {
                callback()
              }
            },
            trigger: 'blur'
          }],
          autoReceive: [{
            required: true,
            validator: (rule, value, callback) => {
              if (!regular.integer.test(value)) {
                callback(new Error('请输入正整数，且不为零！'))
              } else {
                callback()
              }
            },
            trigger: 'blur'
          }],
          autoComplete: [{
            required: true,
            validator: (rule, value, callback) => {
              if (!regular.integer.test(value)) {
                callback(new Error('请输入正整数，且不为零！'))
              } else {
                callback()
              }
            },
            trigger: 'blur'
          }]
        },
        goodsValidate: {
          smallPictureWidth: [{
            required: true,
            validator: (rule, value, callback) => {
              if (!regular.integer.test(value)) {
                callback(new Error('请输入正整数，且不为零！'))
              } else {
                callback()
              }
            },
            trigger: 'blur'
          }],
          smallPictureHeight: [{
            required: true,
            validator: (rule, value, callback) => {
              if (!regular.integer.test(value)) {
                callback(new Error('请输入正整数，且不为零！'))
              } else {
                callback()
              }
            },
            trigger: 'blur'
          }],
          abbreviationPictureWidth: [{
            required: true,
            validator: (rule, value, callback) => {
              if (!regular.integer.test(value)) {
                callback(new Error('请输入正整数，且不为零！'))
              } else {
                callback()
              }
            },
            trigger: 'blur'
          }],
          abbreviationPictureHeight: [{
            required: true,
            validator: (rule, value, callback) => {
              if (!regular.integer.test(value)) {
                callback(new Error('请输入正整数，且不为零！'))
              } else {
                callback()
              }
            },
            trigger: 'blur'
          }],
          originalPictureWidth: [{
            required: true,
            validator: (rule, value, callback) => {
              if (!regular.integer.test(value)) {
                callback(new Error('请输入正整数，且不为零！'))
              } else {
                callback()
              }
            },
            trigger: 'blur'
          }],
          originalPictureHeight: [{
            required: true,
            validator: (rule, value, callback) => {
              if (!regular.integer.test(value)) {
                callback(new Error('请输入正整数，且不为零！'))
              } else {
                callback()
              }
            },
            trigger: 'blur'
          }]
        }
      };
    },
    methods: {
      init() {
        this.initBase();
        this.initPoint();
        this.initOrder();
        this.initGoods();
        this.initWechat();
        this.initQQ();
        this.initWeibo();
        this.initAlipay();
      },
      // 基础配置
      initBase() {
        this.loading = true;
        getParams('base').then(res => {
          if (res.success) {
            this.loading = false;
            if (res.result) {
              this.base = res.result
            }
          }
        });
      },
      // 积分设置
      initPoint() {
        this.loading = true;
        getParams('point').then(res => {
          if (res.success) {
            this.loading = false;
            if (res.result) {
              this.point = res.result
            }
          }
        });
      },
      // 订单设置
      initOrder() {
        this.loading = true;
        getParams('order').then(res => {
          if (res.success) {
            this.loading = false;
            if (res.result) {
              this.order = res.result
            }
          }
        });
      },
      // 商品设置
      initGoods() {
        this.loading = true;
        getParams('goods').then(res => {
          if (res.success) {
            this.loading = false;
            if (res.result) {
              this.goods = res.result
            }
          }
        });
      },
      // 
      initWeibo() {
        this.loading = true;
        getParams('weibo').then(res => {
          if (res.success) {
            this.loading = false;
            if (res.result) {
              this.weibo = res.result
            }
          }
        });
      },
      // 微信设置
      initWechat() {
        this.loading = true;
        getParams('wechat').then(res => {
          if (res.success) {
            this.loading = false;
            if (res.result) {
              this.wechat = res.result
            }
          }
        });
      },
      initQQ() {
        this.loading = true;
        getParams('qq').then(res => {
          if (res.success) {
            this.loading = false;
            if (res.result) {
              this.qq = res.result
            }
          }
        });
      },
      // 阿里配置
      initAlipay() {
        this.loading = true;
        getParams('alipay').then(res => {
          if (res.success) {
            this.loading = false;
            if (res.result) {
              this.alipay = res.result
            }
          }
        });
      },
      // 保存基础配置
      saveBase() {
        this.$refs.baseForm.validate(valid => {
          if (valid) {
            this.saveLoading = true;
            this.base.id = 'base'
            editParams(this.base, 'base').then(res => {
              this.saveLoading = false;
              if (res.success) {
                this.$Message.success("保存成功");
              }
            });
          }
        });
      },
      // 保存积分配置
      savePoint() {
        this.$refs.pointForm.validate(valid => {
          if (valid) {
            this.saveLoading = true;
            this.point.id = 'point'
            editParams(this.point, 'point').then(res => {
              this.saveLoading = false;
              if (res.success) {
                this.$Message.success("保存成功");
              }
            });
          }
        });
      },
      // 保存订单配置
      saveOrder() {
        this.$refs.orderForm.validate(valid => {
          if (valid) {
            this.saveLoading = true;
            this.order.id = 'order'
            editParams(this.order, 'order').then(res => {
              this.saveLoading = false;
              if (res.success) {
                this.$Message.success("保存成功");
              }
            });
          }
        });
      },
      // 保存商品设置
      saveGoods() {
        this.$refs.goodsForm.validate(valid => {
          if (valid) {
            this.saveLoading = true;
            this.goods.id = 'goods'
            editParams(this.goods, 'goods').then(res => {
              this.saveLoading = false;
              if (res.success) {
                this.$Message.success("保存成功");
              }
            });
          }
        });
      },
      // 保存微信设置
      saveWechat() {
        this.$refs.wechatForm.validate(valid => {
          if (valid) {
            this.saveLoading = true;
            this.wechat.id = 'wechat'
            editParams(this.wechat, 'wechat').then(res => {
              this.saveLoading = false;
              if (res.success) {
                this.$Message.success("保存成功");
              }
            });
          }
        });
      },
      // 保存qq设置
      saveQQ() {
        this.$refs.qqForm.validate(valid => {
          if (valid) {
            this.saveLoading = true;
            this.qq.id = 'qq'
            editParams(this.qq, 'qq').then(res => {
              this.saveLoading = false;
              if (res.success) {
                this.$Message.success("保存成功");
              }
            });
          }
        });
      },
      // 保存微博设置
      saveWeibo() {
        this.$refs.wechatForm.validate(valid => {
          if (valid) {
            this.saveLoading = true;
            this.weibo.id = 'weibo'
            editParams(this.weibo, 'weibo').then(res => {
              this.saveLoading = false;
              if (res.success) {
                this.$Message.success("保存成功");
              }
            });
          }
        });
      },
      // 保存支付宝设置
      saveAlipay() {
        this.$refs.alipayForm.validate(valid => {
          if (valid) {
            this.saveLoading = true;
            this.alipay.id = 'alipay'
            editParams(this.alipay, 'alipay').then(res => {
              this.saveLoading = false;
              if (res.success) {
                this.$Message.success("保存成功");
              }
            });
          }
        });
      }
    },
    mounted() {
      let name = this.$route.query.name;
      if (name) {
        this.tabName = name;
      }
      this.init();
    }
  };

</script>
