<template>
  <div class="search">
    <Card style="margin-left: 10px">
      <Tabs v-model="type" @on-click="handleClickType">
        <TabPane label="基本信息" name="INFO">
          <Form
            ref="form"
            :model="form"
            :label-width="100"
            :rules="formValidate"
          >
            <FormItem label="店铺名称">
              <Input
                v-model="storeName"
                disabled
                clearable
                style="width: 20%"
              />
            </FormItem>
            <FormItem label="店铺地址" prop="address">
              <Input
                v-model="form.address"
                @on-focus="$refs.liliMap.showMap = true"
                clearable
                style="width: 20%"
              />
            </FormItem>
            <FormItem label="详细地址" prop="shopAddressDetail">
              <Input
                v-model="form.storeAddressDetail"
                clearable
                style="width: 20%"
                maxlength="50"
              />
            </FormItem>
            <FormItem label="店铺LOGO：">
              <upload-pic-thumb
                v-model="form.storeLogo"
                :multiple="false"
              ></upload-pic-thumb>
            </FormItem>
            <FormItem label="店铺简介" prop="content" class="wangEditor">
              <Input
                type="textarea"
                :rows="8"
                v-model="form.storeDesc"
                style="width: 30%"
              ></Input>
            </FormItem>
            <Form-item>
              <Button
                @click="handleSubmit"
                :loading="submitLoading"
                type="primary"
                style="margin-right: 5px"
                >修改
              </Button>
            </Form-item>
          </Form>
        </TabPane>
        <TabPane label="退货地址" name="REFUND_GOODS_ADDRESS">
          <Form
            ref="addressForm"
            :model="addressForm"
            :label-width="100"
            :rules="afterFormValidate"
          >
            <FormItem label="收货人" prop="salesConsigneeName">
              <Input
                v-model="addressForm.salesConsigneeName"
                maxlength="11"
                clearable
                style="width: 20%"
              />
            </FormItem>
            <FormItem label="收货人电话" prop="salesConsigneeMobile">
              <Input
                v-model="addressForm.salesConsigneeMobile"
                maxlength="11"
                style="width: 20%"
              />
            </FormItem>
            <FormItem label="售后地址">
              <Input
                v-model="region"
                disabled
                style="width: 20%"
                v-if="showRegion == false"
              />
              <Button
                v-if="showRegion == false"
                @click="regionClick"
                :loading="submitLoading"
                type="primary"
                style="margin-left: 8px"
                >修改
              </Button>
              <regionMap
                style="width: 20%"
                @selected="selectedRegion"
                v-if="showRegion == true"
              />
            </FormItem>
            <FormItem label="详细地址" prop="salesConsigneeDetail">
              <Input
                v-model="addressForm.salesConsigneeDetail"
                clearable
                style="width: 20%"
                maxlength="50"
              />
            </FormItem>

            <Form-item>
              <Button
                @click="afterHandleSubmit"
                :loading="submitLoading"
                type="primary"
                style="margin-right: 5px"
                >修改
              </Button>
            </Form-item>
          </Form>
        </TabPane>
        <TabPane label="库存预警" name="STOCK_WARNING">
          <Form
            ref="stockWarningForm"
            :model="stockWarningForm"
            :label-width="100"
            :rules="stockWarningFormValidate"
          >
            <FormItem label="预警数" prop="stockWarning">
              <InputNumber
                :min="0"
                :max="99999"
                v-model="stockWarningForm.stockWarning"
                type="number"
                maxlength="6"
                clearable
                style="width: 20%"
              />
            </FormItem>
            <Form-item>
              <Button
                @click="stockWarningHandleSubmit"
                :loading="submitLoading"
                type="primary"
                style="margin-right: 5px"
                >修改
              </Button>
            </Form-item>
          </Form>
        </TabPane>
        <TabPane label="客服设置" name="UDESK">
          <Form
            ref="udeskForm"
            :model="udeskForm"
            :label-width="100"
            :rules="udeskFormValidate"
          >
            <FormItem label="坐席id" prop="merchantEuid">
              <Input
                v-model="udeskForm.merchantEuid"
                maxlength="30"
                clearable
                style="width: 20%"
              />
            </FormItem>
            <Form-item>
              <Button
                @click="merchantSubmit"
                :loading="submitLoading"
                type="primary"
                style="margin-right: 5px"
                >修改
              </Button>
            </Form-item>
          </Form>
        </TabPane>
      </Tabs>
    </Card>

    <liliMap ref="liliMap" @getAddress="getAddress"></liliMap>
  </div>
</template>

<script>
import * as API_Shop from "@/api/shops";
import { validateMobile } from "@/libs/validate";
import uploadPicThumb from "@/views/my-components/lili/upload-pic-thumb";
import liliMap from "@/views/my-components/map/index";
import regionMap from "@/views/lili-components/region";
import * as RegExp from "@/libs/RegExp.js";
import Cookies from "js-cookie";

export default {
  name: "shopSetting",
  components: {
    uploadPicThumb,
    liliMap,
    regionMap,
  },
  data() {
    return {
      type: "INFO",
      showRegion: false, // 选择地址模态框显隐
      storeName: "", //店铺名称
      region: "", // 地区名称
      regionId: [], // 地区id
      addressForm: {
        // 退货地址
        salesConsigneeName: "", // 收货人姓名
        salesConsigneeMobile: "", // 收货人电话
        salesConsigneeAddressId: "", // 售后地址id,逗号分割
        salesConsigneeAddressPath: "", // 售后地址，逗号分割
        salesConsigneeDetail: "", // 详细地址
      },
      //库存预警form
      stockWarningForm: {
        stockWarning: "", // 库存预警数量
      },
      //im form
      udeskForm: {
        merchantEuid: "",
      },
      stockWarningFormValidate: {
        stockWarning: [
          { required: true, type:'number', message: "请输入库存预警数", trigger: "blur" },
        ],
      },
      udeskFormValidate: {
        merchantEuid: [
          { required: true, message: "请输入店铺坐席ID", trigger: "blur" },
        ],
      },
      afterFormValidate: {
        salesConsigneeMobile: [
          { required: true, message: "手机号不能为空", trigger: "blur" },
          {
            pattern: RegExp.mobile,
            trigger: "blur",
            message: "请输入正确的手机号",
          },
        ],
        salesConsigneeName: [
          { required: true, message: "请输入收货人", trigger: "blur" },
        ],
        salesConsigneeDetail: [
          { required: true, message: "请输入详细地址", trigger: "blur" },
        ],
      },
      form: {
        // 添加或编辑表单对象初始化数据
        storeAddressPath: "", // 店铺地址中文
        storeCenter: "", // 经度 + 纬度
        longitude: "", //经度
        latitude: "", //纬度
        storeAddressDetail: "", //详细地址
        storeAddressIdPath: "", //地址
        storeDesc: "", // 店铺描述
      },

      // 表单验证规则
      formValidate: {
        addressName: [
          {
            required: true,
            message: "请输入地址名称",
            trigger: "blur",
          },
        ],
        longitude: [
          {
            required: true,
            message: "请输入地址经度",
            trigger: "blur",
          },
        ],
        latitude: [
          {
            required: true,
            message: "请输入地址纬度",
            trigger: "blur",
          },
        ],
        mobile: [
          {
            required: true,
            message: "请输入地址纬度",
            trigger: "blur",
          },
          {
            validator: validateMobile,
            trigger: "blur",
          },
        ],
        storeAddressDetail: [
          {
            required: true,
            message: "请输入详细地址",
            trigger: "blur",
          },
        ],
      },
      submitLoading: false, // 添加或编辑提交状态
    };
  },
  methods: {
    // 初始化数据
    init() {
      this.getShopInfo();
    },
    //获取店铺信息
    getShopInfo() {
      this.loading = true;
      API_Shop.getShopInfo().then((res) => {
        this.loading = false;
        if (res.success) {
          this.form = res.result;
          this.$set(this.form, "address", res.result.storeAddressPath);
          this.storeName = res.result.storeName;
          this.form.storeCenter = res.result.storeCenter;
          Cookies.set("userInfoSeller", JSON.stringify(res.result));
          //库存预警数赋值
          this.$nextTick(() => {
            this.stockWarningForm.stockWarning = res.result.stockWarning + "";
          });
          if (res.result.merchantEuid) {
            //赋予坐席id
            this.udeskForm.merchantEuid = res.result.merchantEuid;
          }
        }
      });
    },
    //修改售后地址
    regionClick() {
      this.showRegion = true;
      this.regionId = "";
    },
    //重置
    handleReset() {
      this.$refs.form.resetFields();
    },
    //提交保存
    handleSubmit() {
      this.$refs.form.validate((valid) => {
        if (valid) {
          this.submitLoading = true;
          API_Shop.saveShopInfo(this.form).then((res) => {
            this.submitLoading = false;
            if (res.success) {
              this.$Message.success("修改成功");
              this.getShopInfo();
            }
          });
        }
      });
    },
    //修改库存预警数
    stockWarningHandleSubmit() {
      this.$refs.stockWarningForm.validate((valid) => {
        if (valid) {
          this.submitLoading = true;
          API_Shop.updateStockWarning(this.stockWarningForm).then((res) => {
            this.submitLoading = false;
            if (res.success) {
              this.$Message.success("修改成功");
              this.getShopInfo();
            }
          });
        }
      });
    },
    merchantSubmit() {
      this.$refs.udeskForm.validate((valid) => {
        if (valid) {
          this.submitLoading = true;
          API_Shop.updatEmerchantId(this.udeskForm).then((res) => {
            this.submitLoading = false;
            if (res.success) {
              this.$Message.success("修改成功");
              this.getShopInfo();
            }
          });
        }
      });
    },
    // 选中的地址
    selectedRegion(val) {
      this.region = val[1];
      this.regionId = val[0];
    },
    //tab切换
    handleClickType(v) {
      //退款
      if (v == "INFO") {
        this.getShopInfo();
      }
      //退货
      if (v == "REFUND_GOODS_ADDRESS") {
        this.getRefundGoodsAddress();
      }
    },
    //获取商家退货地址
    getRefundGoodsAddress() {
      API_Shop.getRefundGoodsAddress().then((res) => {
        if (res.result != null) {
          this.addressForm = res.result;
          this.regionId = res.result.salesConsigneeAddressId;
          this.region = res.result.salesConsigneeAddressPath;
        }
      });
    },
    //提交保存
    afterHandleSubmit() {
      if (this.regionId == "") {
        this.$Message.error("请选择地址");
        return;
      }
      this.$refs.addressForm.validate((valid) => {
        if (valid) {
          this.addressForm.salesConsigneeAddressPath = this.region;
          this.addressForm.salesConsigneeAddressId = this.regionId;
          this.submitLoading = true;
          API_Shop.saveRefundGoodsAddress(this.addressForm).then((res) => {
            this.submitLoading = false;
            if (res.success) {
              this.$Message.success("修改成功");
              this.getRefundGoodsAddress();
              this.showRegion = false;
            }
          });
        }
      });
    },
    //获取地址
    getAddress(item) {
      this.$set(this.form, "address", item.addr);
      this.form.storeAddressPath = item.addr;
      this.form.storeAddressIdPath = item.addrId;
      this.form.storeCenter = item.position.lng + "," + item.position.lat;
    },
  },
  mounted() {
    this.init();
  },
};
</script>
