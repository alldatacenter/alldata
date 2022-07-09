<template>
  <view class="add-address">
    <div class="uForm">
      <u-form :border-bottom="false" :model="form" ref="uForm" :error-type="['toast']" :rule="rules">
        <view class="selectAddress" @click="clickUniMap">
          选择收货地址
        </view>
        <u-form-item class="border" label="收货人" label-width="130" prop="name">
          <u-input v-model="form.name" clearable placeholder="请输入收货人姓名" />
        </u-form-item>

        <u-form-item label="手机号码" label-width="130" prop="mobile">
          <u-input v-model="form.mobile" type="number" maxlength="11" placeholder="请输入收货人手机号码" />
        </u-form-item>
        <u-form-item label="所在区域" label-width="130" prop="___path">
          <u-input v-model="form.___path" type="select" @click="showPicker" placeholder="请选择所在地区" />
        </u-form-item>
        <u-form-item class="detailAddress" label="详细地址" label-width="130" prop="detail">
          <u-input type="textarea" v-model="form.detail" maxlength="100" height="150" placeholder="街道楼牌号等" />
        </u-form-item>
        <u-form-item label="地址别名" label-width="130">
          <u-input v-model="form.alias" placeholder="请输入地址别名" />
        </u-form-item>
        <u-checkbox-group shape="circle" size="30">
          <u-checkbox :active-color="lightColor" v-model="form.isDefault">设为默认地址</u-checkbox>
        </u-checkbox-group>

        <div class="saveBtn" @click="save">保存</div>
      </u-form>

      <m-city :provinceData="list" headTitle="区域选择" ref="cityPicker" @funcValue="getpickerParentValue" pickerSize="4">
      </m-city>

      <uniMap v-if="mapFlage" @close="closeMap" @callback="callBackAddress" />
    </div>
  </view>
</template>
<script>
import { addAddress, editAddress, getAddressDetail } from "@/api/address.js";
import gkcity from "@/components/m-city/m-city.vue";
import uniMap from "./uniMap";
import permision from "@/js_sdk/wa-permission/permission.js";
export default {
  components: {
    "m-city": gkcity,
    uniMap,
  },
  onShow() {
    // 判断当前系统权限定位是否开启
  },
  methods: {
    // 关闭地图
    closeMap() {
      this.mapFlage = false;
    },
    // 打开地图并访问权限
    clickUniMap() {
      // #ifdef APP-PLUS
      if (plus.os.name == "iOS") {
        // ios系统
        permision.judgeIosPermission("location")
          ? (this.mapFlage = true)
          : this.refuseMapOuther();
      } else {
        // 安卓
        this.requestAndroidPermission(
          "android.permission.ACCESS_FINE_LOCATION"
        );
      }
      // #endif

      // #ifndef APP-PLUS
      this.mapFlage = true;
      // #endif
    },

    // 如果拒绝权限 提示区设置
    refuseMapOuther() {
      uni.showModal({
        title: "温馨提示",
        content: "您已拒绝定位,请开启",
        confirmText: "去设置",
        success(res) {
          if (res.confirm) {
            //打开授权设置
            // #ifndef MP-WEIXIN
            uni.getSystemInfo({
              success(res) {
                if (res.platform == "ios") {
                  //IOS
                  plus.runtime.openURL("app-settings://");
                } else if (res.platform == "android") {
                  //安卓
                  let main = plus.android.runtimeMainActivity();
                  let Intent = plus.android.importClass(
                    "android.content.Intent"
                  );
                  let mIntent = new Intent("android.settings.ACTION_SETTINGS");
                  main.startActivity(mIntent);
                }
              },
            });
            // #endif
          }
        },
      });
    },

    // 获取安卓是否拥有地址权限
    async requestAndroidPermission(permisionID) {
      var result = await permision.requestAndroidPermission(permisionID);

      if (result == 1) {
        this.mapFlage = true;
      } else {
        this.refuseMapOuther();
      }
    },

    // 选择地址后数据的回调
    callBackAddress(val) {
      uni.showLoading({
        title: "加载中",
      });

      if (val.regeocode && val) {
        let address = val.regeocode;
        this.form.detail = address.formatted_address; //地址详情
        this.form.___path = val.data.result.name;
        this.form.consigneeAddressIdPath = val.data.result.id; // 地址id分割
        this.form.consigneeAddressPath = val.data.result.name; //地址名称， '，'分割
        this.form.lat = val.latitude; //纬度
        this.form.lon = val.longitude; //经度
        uni.hideLoading();
      }

      this.mapFlage = !this.mapFlage; //关闭地图
    },

    // 保存当前 地址
    save() {
      this.$refs.uForm.validate((valid) => {
        if (valid) {
          let pages = getCurrentPages(); //获取页面栈
          let beforePage = pages[pages.length - 2]; //上个页面

          // 如果没有id则为新增地址
          if (!this.form.id) {
            // 删除没有的数据
            delete this.form.___path;
            addAddress(this.form).then((res) => {
              if (res.data.success) {
                uni.navigateBack();
              }
            });
          } else {
            // 修改地址
            delete this.form.___path;
            delete this.form.updateBy;
            delete this.form.updateTime;
            editAddress(this.form).then((res) => {
              if (res.data.success) {
                uni.navigateTo({
                  url: `/${beforePage.route}`,
                });
              }
            });
          }
        }
      });
    },

    // 三级地址联动回调
    getpickerParentValue(e) {
      // 将需要绑定的地址设置为空，并赋值
      this.form.consigneeAddressIdPath = [];
      this.form.consigneeAddressPath = [];
      let name = "";

      e.forEach((item, index) => {
        if (item.id) {
          // 遍历数据
          this.form.consigneeAddressIdPath.push(item.id);
          this.form.consigneeAddressPath.push(item.localName);
          name += item.localName;
          this.form.___path = name;
        }
        if (index == e.length - 1) {
          //如果是最后一个
          let _town = item.children.filter((_child) => {
            return _child.id == item.id;
          });

          this.form.lat = _town[0].center.split(",")[1];
          this.form.lon = _town[0].center.split(",")[0];
        }
      });
    },

    // 显示三级地址联动
    showPicker() {
      this.$refs.cityPicker.show();
    },
  },
  mounted() {},
  data() {
    return {
      lightColor: this.$lightColor, //高亮颜色
      mapFlage: false, // 地图选择开
      routerVal: "",
      form: {
        detail: "", //地址详情
        name: "", //收货人姓名
        mobile: "", //手机号码
        consigneeAddressIdPath: [], //地址id
        consigneeAddressPath: [], //地址名字
        ___path: "", //所在区域
        isDefault: false, //是否默认地址
      },
      // 表单提交校验规则
      rules: {
        name: [
          {
            required: true,
            message: "收货人姓名不能为空",
            trigger: ["blur", "change"],
          },
        ],
        mobile: [
          {
            required: true,
            message: "手机号码不能为空",
            trigger: ["blur", "change"],
          },
          {
            validator: (rule, value, callback) => {
              return this.$u.test.mobile(value);
            },
            message: "手机号码不正确",
            trigger: ["change", "blur"],
          },
        ],
        ___path: [
          {
            required: true,
            message: "请选择所在区域",
            trigger: ["change"],
          },
        ],
        detail: [
          {
            required: true,
            message: "请填写详细地址",
            trigger: ["blur", "change"],
          },
        ],
      },
      list: [
        {
          id: "",
          localName: "请选择",
          children: [],
        },
      ],
    };
  },
  onLoad(option) {
    uni.showLoading({
      title: "加载中",
    });
    this.routerVal = option;
    // 如果当前是编辑地址,则需要查询出地址详情信息
    if (option.id) {
      getAddressDetail(option.id).then((res) => {
        const params = res.data.result;
        params.___path = params.consigneeAddressPath;
        this.$set(this, "form", params);

        uni.hideLoading();
      });
    }
    uni.hideLoading();
  },
  // 初始化rules必须要在onReady生命周期，因为onLoad生命周期组件可能尚未创建完毕
  onReady() {
    this.$refs.uForm.setRules(this.rules);
  },
};
</script>
<style scoped lang="scss">
.detailAddress {
  /deep/ .u-form-item--left {
    display: flex;
    align-items: flex-start;
  }
}
.saveBtn,
.selectAddress {
  height: 70rpx;

  line-height: 70rpx;
  text-align: center;
  font-size: 30rpx;
  background: $aider-light-color;
  color: #fff;
  width: 70%;
  margin: 40rpx auto 0 auto;
  border-radius: 20rpx;
}
.selectAddress {
  margin-top: 40rpx;
  background: #fff;

  color: $aider-light-color;
  border: 2rpx solid $aider-light-color;
}

.uForm {
  width: 94%;
  overflow: hidden;
  left: 3%;
  position: relative;
  top: 2%;
  background: #fff;
  border-radius: 20rpx;
  padding: 0 0 40rpx 0;
}
.add-address {
  width: 100%;
  padding-top: 3%;

  /deep/ .u-form-item {
    background-color: #fff;
    padding: 24rpx 30rpx;
  }
  .u-btn {
    margin: 30rpx 30rpx 0 30rpx;
    background-color: $main-color;
  }

  /deep/.u-checkbox {
    margin: 30rpx 30rpx 0 30rpx;

    .u-label-class.u-checkbox__label {
      color: $font-color-light;
      font-size: $font-sm;
    }
  }
}
/deep/ .u-checkbox__label {
  font-size: 28rpx;
}
</style>
