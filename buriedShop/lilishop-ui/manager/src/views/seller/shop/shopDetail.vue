<template>
  <div>
    <Card style="padding: 10px 12px 0px">
      <div class="head-title">基本信息</div>
      <div class="detail-body">
        <div class="ant-col-md-6" style="width: 25%;">
          <div class="info">
            <div class="head-info">
              <Avatar size="large" :src="storeInfo.storeLogo"/>
              <div>
                <div class="name" v-if="storeInfo.storeName && storeInfo.storeName.length > 15">
                  {{storeInfo.storeName.slice(0,15)}}...
                </div>
                <div class="name" v-else>
                  {{storeInfo.storeName}}
                </div>
              </div>
            </div>
            <div class="bottom-info">
              <p v-if="storeInfo.createTime">{{storeInfo.createTime}}&nbsp;开店</p>
              <p v-if="storeInfo.selfOperated == 1">
                <Tag color="success">自营</Tag>
              </p>
              <p v-else>
                <Tag color="error">非自营</Tag>
              </p>
              <p>
                <i-switch size="large" v-model="storeInfo.storeDisable" :true-value="true" :false-value="false"
                          @on-change="shopStatusChange">
                  <span slot="open">启用</span>
                  <span slot="close">禁用</span>
                </i-switch>
              </p>
            </div>
            <div style="margin-top: 20px">
              <p class="item">
                <span class="label">公司名称：</span>
                <span class="info">{{storeInfo.companyName}}</span>
              </p>
              <p class="item">
                <span class="label">公司电话：</span>
                <span class="info">{{storeInfo.companyPhone}}</span>
              </p>
              <p class="item">
                <span class="label">电子邮箱：</span>
                <span class="info">{{storeInfo.companyEmail}}</span>
              </p>
              <p class="item">
                <span class="label">员工总数：</span>
                <span class="info">{{storeInfo.employeeNum}}人</span>
              </p>
              <p class="item">
                <span class="label">注册资金：</span>
                <span class="info">{{storeInfo.registeredCapital}}万</span>
              </p>
              <p class="item">
                <span class="label">电子邮箱：</span>
                <span class="info">{{storeInfo.companyEmail}}</span>
              </p>
              <p class="item">
                <span class="label">联系人姓名：</span>
                <span class="info">{{storeInfo.linkName}}</span>
              </p>
              <p class="item">
                <span class="label">联系人电话：</span>
                <span class="info">{{storeInfo.linkPhone}}</span>
              </p>
              <p class="item">
                <span class="label">公司地址：</span>
                <span class="info">
                  {{storeInfo.companyAddressPath || storeInfo.companyAddress?storeInfo.companyAddressPath + ' '+storeInfo.companyAddress:"暂未完善"}}
                </span>
              </p>
            </div>

          </div>
        </div>
        <div class="ant-col-md-6">
          <p class="item">
            <span class="label">商家账号：</span>
            <span class="info">{{storeInfo.memberName}}</span>
          </p>
          <p class="item">
            <span class="label">库存预警数：</span>
            <span class="info">{{storeInfo.stockWarning?storeInfo.stockWarning:'0'}}</span>
          </p>
          <p class="item">
            <span class="label">店铺所在地：</span>
            <span class="info">
              {{storeInfo.storeAddressPath || storeInfo.storeAddressDetail?storeInfo.storeAddressPath +storeInfo.storeAddressDetail:"暂未完善"}}
            </span>
          </p>
          <p class="item">
            <span class="label">退货地址：</span>
            <span class="info">
               {{storeInfo.salesConsigneeName !== 'null' ? storeInfo.salesConsigneeName : '' || storeInfo.salesConsigneeMobile !=='null' ?storeInfo.salesConsigneeMobile:''|| storeInfo.salesConsigneeAddressPath !=='null'?storeInfo.salesConsigneeAddressPath:'' || storeInfo.salesConsigneeDetail !=='null'?storeInfo.salesConsigneeDetail:'' ?storeInfo.salesConsigneeName + storeInfo.salesConsigneeMobile +' '+ storeInfo.salesConsigneeAddressPath + storeInfo.salesConsigneeDetail:'暂未完善'}}
               
            </span>
          </p>
          <p class="item">
            <span class="label">店铺定位：</span>
            <span class="info">{{storeInfo.storeCenter?'已定位':'未定位'}}</span>
          </p>
          <p class="item">
            <span class="label">经营范围：</span>
            <span class="info">
               <CheckboxGroup v-model="checkAllGroup">
                <Checkbox disabled v-for="(item, i) in categories" :key="i + 1" :label="item.id">{{ item.name }}
                </Checkbox>
              </CheckboxGroup>
            </span>
          </p>
          <p class="item">
            <span class="label">店铺简介：</span>
            <span class="info">
              {{storeInfo.storeDesc?storeInfo.storeDesc:'暂未完善'}}
            </span>
          </p>
        </div>
        <div class="ant-col-md-6">
          <p class="item">
            <span class="label">法人姓名：</span>
            <span class="info">{{storeInfo.legalName}}人</span>
          </p>
          <p class="item">
            <span class="label">法人身份证：</span>
            <span class="info">{{storeInfo.legalId}}人</span>
          </p>
          <p class="item">
            <span class="label">身份证照片：</span>
            <span class="info">
              <img style="height: 100px;width: 100px" class="mr_10" v-for="item in storeInfo.legalPhoto" :src="item" :key="item">
            </span>
          </p>
          <p class="item">
            
            <span class="label">营业执照号：</span>
            <span class="info">{{storeInfo.licenseNum}}</span>
          </p>
          <p class="item">
            <span class="label">法定经营范围：</span>
            <span class="info">{{storeInfo.scope}}</span>
          </p>
          <p class="item">
            <span class="label">营业执照电子版：</span>
            <span class="info">
              <img style="height: 100px;width: 100px" :src="storeInfo.licencePhoto">
            </span>
          </p>

          <p class="item">
            <span class="label">银行名称：</span>
            <span class="info">{{storeInfo.settlementBankAccountName}}</span>
          </p>
          <p class="item">
            <span class="label">银行账号：</span>
            <span class="info">{{storeInfo.settlementBankAccountNum}}</span>
          </p>
          <p class="item">
            <span class="label">银行开户支行名称：</span>
            <span class="info">{{storeInfo.settlementBankBranchName}}</span>
          </p>
          <p class="item">
            <span class="label">银行支行联行号：</span>
            <span class="info">{{storeInfo.settlementBankJointName}}</span>
          </p>
          <p class="item">
            <span class="label">结算周期：</span>
            <span class="info" v-if="storeInfo.settlementCycle">
              <template v-for="item in storeInfo.settlementCycle.split(',')">
               <Tag
                  :key="item"
                  v-if="item!==''"
                  :name="item"
                  style="marrgin-left: 10px"
                >{{ item }}
                </Tag>
              </template>
               
            </span>
          </p>
        </div>
      </div>
    </Card>

    <Card class="mt_10">
      <Tabs value="order" @on-click="storeInfoChange">
        <TabPane label="TA的订单" name="order" style="min-height: 200px">
          <Row>
            <Form ref="searchForm" :model="orderSearchForm" inline :label-width="70" class="search-form">
              <Form-item label="订单号" prop="orderSn">
                <Input
                  type="text"
                  v-model="orderSearchForm.orderSn"
                  placeholder="请输入订单号"
                  clearable
                  style="width: 200px"
                />
              </Form-item>
              <Form-item label="订单状态" prop="orderStatus">
                <Select v-model="orderSearchForm.orderStatus" placeholder="请选择" clearable style="width: 200px">
                  <Option value="UNPAID">未付款</Option>
                  <Option value="PAID">已付款</Option>
                  <Option value="UNDELIVERED">待发货</Option>
                  <Option value="DELIVERED">已发货</Option>
                  <Option value="COMPLETED">已完成</Option>
                  <Option value="TAKE">待核验</Option>
                  <Option value="CANCELLED">已取消</Option>
                </Select>
              </Form-item>
              <Form-item label="支付状态" prop="payStatus">
                <Select v-model="orderSearchForm.payStatus" placeholder="请选择" clearable style="width: 200px">
                  <Option value="UNPAID">未付款</Option>
                  <Option value="PAID">已付款</Option>
                </Select>
              </Form-item>
              <Form-item label="订单类型" prop="orderType">
                <Select v-model="orderSearchForm.orderType" placeholder="请选择" clearable style="width: 200px">
                  <Option value="NORMAL">普通订单</Option>
                  <Option value="VIRTUAL">虚拟订单</Option>
                  <Option value="GIFT">赠品订单</Option>
                  <Option value="PINTUAN">拼团订单</Option>
                </Select>
              </Form-item>
              <Form-item label="订单来源" prop="clientType">
                <Select v-model="orderSearchForm.clientType" placeholder="请选择" clearable style="width: 200px">
                  <Option value="H5">移动端</Option>
                  <Option value="PC">PC端</Option>
                  <Option value="WECHAT_MP">小程序</Option>
                  <Option value="APP">移动应用端</Option>
                  <Option value="UNKNOWN">未知</Option>
                </Select>
              </Form-item>
              <Form-item label="下单时间">
                <DatePicker
                  v-model="selectDate"
                  type="datetimerange"
                  format="yyyy-MM-dd HH:mm:ss"
                  clearable
                  @on-change="selectDateRange"
                  placeholder="选择起始时间"
                  style="width: 200px"
                  transfer
                ></DatePicker>
              </Form-item>
              <Button @click="getOrderData" type="primary" icon="ios-search" class="search-btn">搜索</Button>
            </Form>
          </Row>
          <div style="min-height: 180px">
            <Table
              :loading="loading"
              border
              :columns="orderColumns"
              :data="orderData"
              ref="table"
              class="mt_10"
            >
              <!-- 订单详情格式化 -->
              <template slot="orderSlot" slot-scope="scope">
                <a @click="$router.push({name: 'order-detail',query: {sn: scope.row.sn}})">{{scope.row.sn}}</a>
              </template>
            </Table>

            <Row type="flex" justify="end" class="mt_10" style="margin-top: 10px">
              <Page
                :current="orderSearchForm.pageNumber"
                :total="orderTotal"
                :page-size="orderSearchForm.pageSize"
                @on-change="orderChangePage"
                @on-page-size-change="orderChangePageSize"
                :page-size-opts="[10, 20, 50]"
                size="small"
                show-total
                show-elevator
                show-sizer
              ></Page>
            </Row>
          </div>
        </TabPane>
        <TabPane label="TA的退货单" name="refundGoods">
          <Row>
            <Form ref="refundGoodsOrderSearchForm" :model="refundGoodsOrderSearchForm" inline :label-width="70"
                  class="search-form">
              <Form-item label="订单编号" prop="orderSn">
                <Input
                  type="text"
                  v-model="refundGoodsOrderSearchForm.orderSn"
                  placeholder="请输入订单编号"
                  clearable
                  style="width: 200px"
                />
              </Form-item>
              <Form-item label="售后单号" prop="sn">
                <Input
                  type="text"
                  v-model="refundGoodsOrderSearchForm.sn"
                  placeholder="请输入售后单号"
                  clearable
                  style="width: 200px"
                />
              </Form-item>
              <Form-item label="售后状态">
                <Select v-model="refundGoodsOrderSearchForm.serviceStatus" placeholder="全部" clearable
                        style="width: 200px">
                  <Option value="APPLY">申请售后</Option>
                  <Option value="PASS">通过售后</Option>
                  <Option value="REFUSE">拒绝售后</Option>
                  <Option value="BUYER_RETURN">买家退货，待卖家收货</Option>
                  <Option value="SELLER_RE_DELIVERY">商家换货/补发</Option>
                  <Option value="SELLER_CONFIRM">卖家确认收货</Option>
                  <Option value="SELLER_TERMINATION">卖家终止售后</Option>
                  <Option value="BUYER_CONFIRM">买家确认收货</Option>
                  <Option value="BUYER_CANCEL">买家取消售后</Option>
                  <Option value="COMPLETE">完成售后</Option>
                </Select>
              </Form-item>
              <Form-item label="申请时间">
                <DatePicker
                  v-model="selectDate"
                  type="datetimerange"
                  format="yyyy-MM-dd HH:mm:ss"
                  clearable
                  @on-change="selectRefundGoodsDateRange"
                  placeholder="选择起始时间"
                  style="width: 200px"
                  transfer
                ></DatePicker>
              </Form-item>
              <Form-item label="商家名称" prop="storeName">
                <Input
                  type="text"
                  v-model="refundGoodsOrderSearchForm.storeName"
                  placeholder="请输入商家名称"
                  clearable
                  style="width: 200px"
                />
              </Form-item>
              <Form-item label="会员名称" prop="memberName">
                <Input
                  type="text"
                  v-model="refundGoodsOrderSearchForm.memberName"
                  placeholder="请输入会员名称"
                  clearable
                  style="width: 200px"
                />
              </Form-item>
              <Button @click="getRefundGoodsOrderData" type="primary" icon="ios-search" class="search-btn">搜索</Button>
            </Form>
          </Row>
          <div style="min-height: 180px">
            <Table
              :loading="loading"
              border
              :columns="refundGoodsOrderColumns"
              :data="refundGoodsOrderData"
              ref="table"
              class="mt_10"
            >
              <!-- 商品栏目格式化 -->
              <template slot="goodsSlot" slot-scope="scope">
                <div style="margin-top: 5px;height: 80px; display: flex;">
                  <div style="">
                    <img :src="scope.row.goodsImage" style="height: 60px;margin-top: 3px">
                  </div>

                  <div style="margin-left: 13px;margin-top: 3px;">
                    <div class="div-zoom">
                      <a>{{scope.row.goodsName}}</a>
                    </div>
                  </div>
                </div>

              </template>

              <!-- 订单详情格式化 -->
              <template slot="orderSlot" slot-scope="scope">
                <a
                  @click="$router.push({name: 'order-detail',query: {sn: scope.row.orderSn}})">{{scope.row.orderSn}}</a>
              </template>

              <!-- 售后单详情格式化 -->
              <template slot="refundGoodsOrderSlot" slot-scope="scope">
                <a @click="$router.push({name: 'after-order-detail',query: {sn: scope.row.sn}})">{{scope.row.sn}}</a>
              </template>
            </Table>

            <Row type="flex" justify="end" class="mt_10" style="margin-top: 10px">
              <Page
                :current="refundGoodsOrderSearchForm.pageNumber"
                :total="refundGoodsOrderTotal"
                :page-size="refundGoodsOrderSearchForm.pageSize"
                @on-change="refundGoodsOrderChangePage"
                @on-page-size-change="refundGoodsOrderChangePageSize"
                :page-size-opts="[10, 20, 50]"
                size="small"
                show-total
                show-elevator
                show-sizer
              ></Page>
            </Row>
          </div>
        </TabPane>
        <TabPane label="TA的退款单" name="refund">
          <Row>
            <Form ref="refundOrderSearchForm" :model="refundOrderSearchForm" inline :label-width="70"
                  class="search-form">
              <Form-item label="订单编号" prop="orderSn">
                <Input
                  type="text"
                  v-model="refundOrderSearchForm.orderSn"
                  placeholder="请输入订单编号"
                  clearable
                  style="width: 200px"
                />
              </Form-item>
              <Form-item label="售后单号" prop="sn">
                <Input
                  type="text"
                  v-model="refundOrderSearchForm.sn"
                  placeholder="请输入售后单号"
                  clearable
                  style="width: 200px"
                />
              </Form-item>
              <Form-item label="售后状态">
                <Select v-model="refundOrderSearchForm.serviceStatus" placeholder="全部" clearable
                        style="width: 200px">
                  <Option value="APPLY">申请售后</Option>
                  <Option value="PASS">通过售后</Option>
                  <Option value="REFUSE">拒绝售后</Option>
                  <Option value="BUYER_RETURN">买家退货，待卖家收货</Option>
                  <Option value="SELLER_RE_DELIVERY">商家换货/补发</Option>
                  <Option value="SELLER_CONFIRM">卖家确认收货</Option>
                  <Option value="SELLER_TERMINATION">卖家终止售后</Option>
                  <Option value="BUYER_CONFIRM">买家确认收货</Option>
                  <Option value="BUYER_CANCEL">买家取消售后</Option>
                  <Option value="COMPLETE">完成售后</Option>
                </Select>
              </Form-item>
              <Form-item label="申请时间">
                <DatePicker
                  v-model="selectDate"
                  type="datetimerange"
                  format="yyyy-MM-dd HH:mm:ss"
                  clearable
                  @on-change="selectRefundDateRange"
                  placeholder="选择起始时间"
                  style="width: 200px"
                  transfer
                ></DatePicker>
              </Form-item>
              <Form-item label="商家名称" prop="storeName">
                <Input
                  type="text"
                  v-model="refundOrderSearchForm.storeName"
                  placeholder="请输入商家名称"
                  clearable
                  style="width: 200px"
                />
              </Form-item>
              <Form-item label="会员名称" prop="memberName">
                <Input
                  type="text"
                  v-model="refundOrderSearchForm.memberName"
                  placeholder="请输入会员名称"
                  clearable
                  style="width: 200px"
                />
              </Form-item>
              <Button @click="getRefundOrder" type="primary" icon="ios-search" class="search-btn">搜索</Button>
            </Form>
          </Row>
          <div style="min-height: 180px">
            <Table
              :loading="loading"
              border
              :columns="refundGoodsOrderColumns"
              :data="refundOrderData"
              ref="table"
              class="mt_10"
            >
              <!-- 商品栏目格式化 -->
              <template slot="goodsSlot" slot-scope="scope">
                <div style="margin-top: 5px;height: 80px; display: flex;">
                  <div style="">
                    <img :src="scope.row.goodsImage" style="height: 60px;margin-top: 3px">
                  </div>

                  <div style="margin-left: 13px;margin-top: 3px;">
                    <div class="div-zoom">
                      <a>{{scope.row.goodsName}}</a>
                    </div>
                  </div>
                </div>

              </template>

              <!-- 订单详情格式化 -->
              <template slot="orderSlot" slot-scope="scope">
                <a
                  @click="$router.push({name: 'order-detail',query: {sn: scope.row.orderSn}})">{{scope.row.orderSn}}</a>
              </template>

              <!-- 售后单详情格式化 -->
              <template slot="refundGoodsOrderSlot" slot-scope="scope">
                <a @click="$router.push({name: 'after-order-detail',query: {sn: scope.row.sn}})">{{scope.row.sn}}</a>
              </template>
            </Table>

            <Row type="flex" justify="end" class="mt_10" style="margin-top: 10px">
              <Page
                :current="refundOrderSearchForm.pageNumber"
                :total="refundOrderTotal"
                :page-size="refundOrderSearchForm.pageSize"
                @on-change="refundOrderChangePage"
                @on-page-size-change="refundOrderChangePageSize"
                :page-size-opts="[10, 20, 50]"
                size="small"
                show-total
                show-elevator
                show-sizer
              ></Page>
            </Row>
          </div>
        </TabPane>
      </Tabs>
    </Card>

  </div>
</template>

<script>
  import region from "@/views/lili-components/region";
  import ossManage from "@/views/sys/oss-manage/ossManage";
  import * as RegExp from '@/libs/RegExp.js';
  import {getCategoryTree} from "@/api/goods";
  import * as API_Store from "@/api/shops.js";
  import * as API_Order from "@/api/order.js";


  export default {
    name: "member",
    components: {
      region,
      ossManage,
    },
    data() {
      return {
        id: "",//店铺id
        categories: [], //店铺静音范围
        loading: true, // 表单加载状态
        storeInfo: {},//店铺信息
        checkAllGroup: [], //选中的经营分类
        selectDate: null, // 申请时间

        orderColumns: [
          {
            title: "订单编号",
            key: "sn",
            minWidth: 100,
            tooltip: true,
            slot: "orderSlot",
          },
          {
            title: "订单金额",
            key: "flowPrice",
            width: 140,
            render: (h, params) => {
              return h("div", this.$options.filters.unitPrice(params.row.flowPrice, '￥'));
            }
          },
          {
            title: "订单类型",
            key: "orderType",
            width: 100,
            render: (h, params) => {
              if (params.row.orderType == "NORMAL") {
                return h('div', [h('span', {}, '普通订单'),]);
              } else if (params.row.orderType == "VIRTUAL") {
                return h('div', [h('span', {}, '虚拟订单'),]);
              } else if (params.row.orderType == "GIFT") {
                return h('div', [h('span', {}, '赠品订单'),]);
              } else if (params.row.orderType == "PINTUAN") {
                return h('div', [h('span', {}, '拼团订单'),]);
              }

            }
          },
          {
            title: "来源",
            key: "clientType",
            width: 80,
            render: (h, params) => {
              if (params.row.clientType == "H5") {
                return h("div",{},"移动端");
              }else if(params.row.clientType == "PC") {
                return h("div",{},"PC端");
              }else if(params.row.clientType == "WECHAT_MP") {
                return h("div",{},"小程序端");
              }else if(params.row.clientType == "APP") {
                return h("div",{},"移动应用端");
              }
              else{
                return h("div",{},params.row.clientType);
              }
            },
          },
          {
            title: "订单状态",
            key: "orderStatus",
            width: 95,
            render: (h, params) => {
              if (params.row.orderStatus == "UNPAID") {
                return h('div', [h('span', {}, '未付款'),]);
              } else if (params.row.orderStatus == "PAID") {
                return h('div', [h('span', {}, '已付款'),]);
              } else if (params.row.orderStatus == "UNDELIVERED") {
                return h('div', [h('span', {}, '待发货'),]);
              } else if (params.row.orderStatus == "DELIVERED") {
                return h('div', [h('span', {}, '已发货'),]);
              } else if (params.row.orderStatus == "COMPLETED") {
                return h('div', [h('span', {}, '已完成'),]);
              } else if (params.row.orderStatus == "TAKE") {
                return h('div', [h('span', {}, '待核验'),]);
              } else if (params.row.orderStatus == "CANCELLED") {
                return h('div', [h('span', {}, '已取消'),]);
              }
            }
          },
          {
            title: "支付状态",
            key: "payStatus",
            width: 95,
            render: (h, params) => {
              if (params.row.payStatus == "UNPAID") {
                return h('div', [h('span', {}, '未付款'),]);
              } else if (params.row.payStatus == "PAID") {
                return h('div', [h('span', {}, '已付款'),]);
              }
            }
          },

          {
            title: "售后状态",
            key: "groupAfterSaleStatus",
            width: 100,
            render: (h, params) => {
              if (params.row.groupAfterSaleStatus == "NEW") {
                return h('div', [h('span', {}, '未申请'),]);
              } else if (params.row.groupAfterSaleStatus == "NOT_APPLIED") {
                return h('div', [h('span', {}, '未申请'),]);
              } else if (params.row.groupAfterSaleStatus == "ALREADY_APPLIED") {
                return h('div', [h('span', {}, '已申请'),]);
              } else if (params.row.groupAfterSaleStatus == "EXPIRED") {
                return h('div', [h('span', {}, '已失效'),]);
              }
            }
          },
          {
            title: "投诉状态",
            key: "groupComplainStatus",
            width: 95,
            render: (h, params) => {
              if (params.row.groupComplainStatus == "NEW") {
                return h('div', [h('span', {}, '未申请'),]);
              } else if (params.row.groupComplainStatus == "NO_APPLY") {
                return h('div', [h('span', {}, '未申请'),]);
              } else if (params.row.groupComplainStatus == "APPLYING") {
                return h('div', [h('span', {}, '申请中'),]);
              } else if (params.row.groupComplainStatus == "COMPLETE") {
                return h('div', [h('span', {}, '已完成'),]);
              } else if (params.row.groupComplainStatus == "EXPIRED") {
                return h('div', [h('span', {}, '已失效'),]);
              } else if (params.row.groupComplainStatus == "CANCEL") {
                return h('div', [h('span', {}, '取消投诉'),]);
              }
            }
          },
          {
            title: "购买店铺",
            key: "storeName",
            width: 180,
            tooltip: true
          },
          {
            title: "下单时间",
            key: "createTime",
            width: 170,
          },

        ],
        orderData: [],//订单数据
        orderTotal: 0,//订单总条数
        //TA的订单form
        orderSearchForm: {
          pageNumber: 1, // 当前页数
          pageSize: 10, // 页面大小
          payStatus: "",
          orderSn: "",
          orderType: "",
        },
        refundGoodsOrderColumns: [
          {
            title: "售后服务单号",
            key: "sn",
            minWidth: 140,
            slot: "refundGoodsOrderSlot",

          },
          {
            title: "订单编号",
            key: "orderSn",
            minWidth: 120,
            slot: "orderSlot",
          },
          {
            title: "商品",
            key: "goodsName",
            minWidth: 300,
            tooltip: true,
            slot: "goodsSlot",
          },
          {
            title: "会员名称",
            key: "memberName",
            width: 140,
          },
          {
            title: "商家名称",
            key: "storeName",
            minWidth: 100,
            tooltip: true
          },
          {
            title: "售后金额",
            key: "applyRefundPrice",
            width: 110,
            render: (h, params) => {
              if (params.row.applyRefundPrice == null) {
                return h(
                  "div",
                  this.$options.filters.unitPrice(0, "￥")
                );
              } else {
                return h(
                  "div",
                  this.$options.filters.unitPrice(params.row.applyRefundPrice, "￥")
                );
              }

            },
          },
          {
            title: "售后类型",
            key: "serviceType",
            width: 100,
            render: (h, params) => {
              if (params.row.serviceType == "RETURN_MONEY") {
                return h('div', [h('span', {}, '退款'),]);
              } else if (params.row.serviceType == "RETURN_GOODS") {
                return h('div', [h('span', {}, '退货'),]);
              } else if (params.row.serviceType == "EXCHANGE_GOODS") {
                return h('div', [h('span', {}, '换货'),]);
              }
            }
          },

          {
            title: "售后状态",
            key: "serviceStatus",
            width: 110,
            render: (h, params) => {
              if (params.row.serviceStatus == "APPLY") {
                return h('div', [h('span', {}, '申请中'),]);
              } else if (params.row.serviceStatus == "PASS") {
                return h('div', [h('span', {}, '通过售后'),]);
              } else if (params.row.serviceStatus == "REFUSE") {
                return h('div', [h('span', {}, '拒绝售后'),]);
              } else if (params.row.serviceStatus == "BUYER_RETURN") {
                return h('div', [h('span', {}, '买家退货，待卖家收货'),]);
              } else if (params.row.serviceStatus == "SELLER_RE_DELIVERY") {
                return h('div', [h('span', {}, '商家换货/补发'),]);
              } else if (params.row.serviceStatus == "SELLER_CONFIRM") {
                return h('div', [h('span', {}, '卖家确认收货'),]);
              } else if (params.row.serviceStatus == "SELLER_TERMINATION") {
                return h('div', [h('span', {}, '卖家终止售后'),]);
              } else if (params.row.serviceStatus == "BUYER_CONFIRM") {
                return h('div', [h('span', {}, '买家确认收货'),]);
              } else if (params.row.serviceStatus == "BUYER_CANCEL") {
                return h('div', [h('span', {}, '买家取消售后'),]);
              } else if (params.row.serviceStatus == "COMPLETE") {
                return h('div', [h('span', {}, '完成售后'),]);
              } else if (params.row.serviceStatus == "WAIT_REFUND") {
                return h('div', [h('span', {}, '待平台退款'),]);
              }
            }
          },
          {
            title: "申请时间",
            key: "createTime",
            minWidth: 145,
            tooltip: true
          },
        ],
        refundGoodsOrderData: [],//售后单数据
        refundGoodsOrderTotal: 0,//售后单总条数
        //TA的退货单form
        refundGoodsOrderSearchForm: {
          pageNumber: 1, // 当前页数
          pageSize: 10, // 页面大小
        },
        //TA的退款单form
        refundOrderSearchForm: {
          pageNumber: 1, // 当前页数
          pageSize: 10, // 页面大小
        },
        refundOrderData: [],//售后单数据
        refundOrderTotal: 0,//售后单总条数
      };
    },
    methods: {
      init() {
        //查店铺基本信息
        this.getStoreInfo();
        //查询店铺分类
        this.getCategories();
        //查询订单信息
        this.getOrderData();
      },
      //会员信息tab改变事件
      storeInfoChange(v) {
        if (v == "order") {
          this.getOrderData();
        }
        if (v == "refundGoods") {
          this.getRefundGoodsOrderData();
        }
        if(v == "refund"){
          this.getRefundOrder();
        }
      },
      //查询会员信息
      getStoreInfo() {
        API_Store.getShopDetailData(this.id).then((res) => {
          this.$set(this, "storeInfo", res.result);
          //因switch开关需要用到true或者false 所以进行一次格式化
          this.storeInfo.storeDisable = this.storeInfo.storeDisable === "OPEN" ? true : false
          this.checkAllGroup = this.storeInfo.goodsManagementCategory.split(",");
          this.storeInfo.legalPhoto = this.storeInfo.legalPhoto.split(",");
        });
      },
      //店铺状态改变事件
      shopStatusChange(v) {
        if (v) {
          API_Store.enableBrand(this.id).then(res => {
          });
        } else {
          API_Store.disableShop(this.id).then(res => {
          });
        }
      },
      //查询TA的订单
      getOrderData() {
        this.loading = true;
        this.orderSearchForm.storeId = this.id
        API_Order.getOrderList(this.orderSearchForm).then((res) => {
          this.loading = false;
          if (res.success) {
            this.orderData = res.result.records;
            this.orderTotal = res.result.total;
          }
        });
        this.loading = false;
      },
      //查询TA的售后单
      getRefundOrder() {
        this.loading = true;
        this.refundOrderSearchForm.storeId = this.id
        this.refundOrderSearchForm.serviceType = "RETURN_MONEY"
        API_Order.getAfterSaleOrderPage(this.refundOrderSearchForm).then((res) => {
          this.loading = false;
          if (res.success) {
            this.refundOrderData = res.result.records;
            this.refundOrderTotal = res.result.total;
          }
        });
        this.loading = false;
      },
      //查询TA的售后单
      getRefundGoodsOrderData() {
        this.loading = true;
        this.refundGoodsOrderSearchForm.storeId = this.id
        this.refundGoodsOrderSearchForm.serviceType = "RETURN_GOODS"
        API_Order.getAfterSaleOrderPage(this.refundGoodsOrderSearchForm).then((res) => {
          this.loading = false;
          if (res.success) {
            this.refundGoodsOrderData = res.result.records;
            this.refundGoodsOrderTotal = res.result.total;
          }
        });
        this.loading = false;
      },
      //查询分类
      getCategories() {
        getCategoryTree().then((res) => {
          if (res.success) {
            this.categories = res.result;
          }
        });
      },
      //售后单页数变化
      refundGoodsOrderChangePage(v) {
        this.refundGoodsOrderSearchForm.pageNumber = v;
        this.getRefundGoodsOrderData()
      }
      ,
      //售后单页数变化
      refundGoodsOrderChangePageSize(v) {
        this.refundGoodsOrderSearchForm.pageSize = v;
        this.refundGoodsOrderSearchForm.pageNumber = 1;
        this.getRefundGoodsOrderData();
      },
      //退款单页数变化
      refundOrderChangePage(v) {
        this.refundOrderSearchForm.pageNumber = v;
        this.getRefundOrder()
      }
      ,
      //售后单页数变化
      refundOrderChangePageSize(v) {
        this.refundOrderSearchForm.pageSize = v;
        this.refundOrderSearchForm.pageNumber = 1;
        this.getRefundOrder();
      },
      //订单记录页数变化
      orderChangePage(v) {
        this.orderSearchForm.pageNumber = v;
        this.getOrderData()
      }
      ,
      //订单记录页数变化
      orderChangePageSize(v) {
        this.orderSearchForm.pageSize = v;
        this.orderSearchForm.pageNumber = 1;
        this.getOrderData();
      },
      // 起止时间格式化
      selectDateRange(v) {
        if (v) {
          this.orderSearchForm.startDate = v[0];
          this.orderSearchForm.endDate = v[1];
        }
      },
      // 起止时间格式化
      selectRefundGoodsDateRange(v) {
        if (v) {
          this.refundGoodsOrderSearchForm.startDate = v[0];
          this.refundGoodsOrderSearchForm.endDate = v[1];
        }
      },
      // 起止时间格式化
      selectRefundDateRange(v) {
        if (v) {
          this.refundOrderSearchForm.startDate = v[0];
          this.refundOrderSearchForm.endDate = v[1];
        }
      },
    },
    mounted() {
      this.id = this.$route.query.id;
      this.init();
    }
  };
</script>
<style lang="scss" scoped>
  @import "shopDetail.scss";
</style>
