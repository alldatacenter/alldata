<template>
  <view class="address">

    <u-empty class="empty" v-if="addressList.length == 0" text="暂无收货地址" mode="address"></u-empty>
    <view class="list" v-else>
      <view class="item c-content" v-for="(item, index) in addressList" :key="index">
        <view class="basic" @click="selectAddressData(item)">
          <text>{{ item.name }}</text>
          <text>{{ item.mobile }}</text>
          <text class="default" v-show="item.isDefault">默认</text>
          <view>
            <div class="region">
              <span v-if="item.consigneeAddressPath[0]">{{item.consigneeAddressPath[0]}}</span>
              <span v-if="item.consigneeAddressPath[1]">{{item.consigneeAddressPath[1]}}</span>
              <span v-if="item.consigneeAddressPath[2]">{{item.consigneeAddressPath[2]}}</span>
              <span v-if="item.consigneeAddressPath[3]">{{item.consigneeAddressPath[3]}}</span>
              <span>{{ item.detail }}</span>
            </div>
          </view>
        </view>
        <view class="edit">
          <view class="relative" @click="setDefault(item)">
            <view v-if="item.isDefault" class="alifont icon-xuanzhong"></view>
            <text v-else class="unchecked"></text>
            <text>{{ item.isDefault ? "默认地址" : "设为默认" }}</text>
          </view>
          <view class="relative">
            <view class="alifont icon-bianji-copy"></view>
            <text class="mr-40" @click="addAddress(item.id)">编辑</text>
            <view class="alifont icon-lajitong"></view>
            <text @click="removeAddress(item.id)">删除</text>
          </view>
        </view>
      </view>
      <view style="height: 100px"></view>
    </view>
    <button type="default" class="btn" @click="addAddress('')">
      <u-icon name="plus-circle"></u-icon>
      添加新收货人
    </button>
    <u-action-sheet :list="removeList" :tips="tips" v-model="showAction" @click="deleteAddressMessage"></u-action-sheet>
  </view>
</template>

<script>
import * as API_Trade from "@/api/trade";
import * as API_Address from "@/api/address.js";
export default {
  data() {
    return {
      addressList: [], //地址列表
      showAction: false, //是否显示下栏框
      removeList: [
        {
          text: "确定",
        },
      ],
      tips: {
        text: "确定要删除该收货人信息吗？",
      },
      removeId: "", //删除的地址id
      routerVal: "",
      params: {
        pageNumber: 1,
        pageSize: 1000,
      },
    };
  },
  onPullDownRefresh() {
    //下拉刷新
    this.addressList = [];
    this.getAddressList();
  },
  onLoad: function (val) {
    this.routerVal = val;
  },
  onShow() {
    this.addressList = [];
    this.getAddressList();
  },
  onHide() {},
  methods: {
    async selectAddressData(val) {
      await API_Trade.setAddressId(val.id, this.routerVal.way);

      uni.navigateBack({
        delta: 1,
      });
    },
    //获取地址列表
    getAddressList() {
      uni.showLoading();

      API_Address.getAddressList(
        this.params.pageNumber,
        this.params.pageSize
      ).then((res) => {
        res.data.result.records.forEach((item) => {
          item.consigneeAddressPath = item.consigneeAddressPath.split(",");
        });
        this.addressList = res.data.result.records;
        console.log(this.addressList);

        uni.hideLoading();
      });
    },
    //删除地址
    removeAddress(id) {
      this.removeId = id;
      this.showAction = true;
    },
    deleteAddressMessage() {
      API_Address.deleteAddress(this.removeId).then((res) => {
        if (res.statusCode == 200) {
          uni.showToast({
            icon: "none",
            title: "删除成功",
          });
          this.getAddressList();
        } else {
          uni.showToast({
            icon: "none",
            title: res.data.message,
            duration: 2000,
          });
        }
      });
    },
    //新建。编辑地址
    addAddress(id) {
      if (id) {
        uni.navigateTo({
          url:
            "/pages/mine/address/add?id=" +
            id +
            "&way=" +
            this.routerVal.way +
            "&type=order",
        });
      } else {
        uni.navigateTo({
          url:
            "/pages/mine/address/add?way=" + this.routerVal.way + "&type=order",
        });
      }
    },
    //设为默认地址
    setDefault(item) {
      delete item.updateBy;
      delete item.updateTime;
      delete item.deleteFlag;

      item.isDefault ? "" : (item.isDefault = !item.isDefault);

      API_Address.editAddress(item).then((res) => {
        uni.showToast({
          title: "设置默认地址成功",
          icon: "none",
        });
        this.getAddressList();
      });
    },
  },
};
</script>

<style lang="scss" scoped>
@import "./address.scss";
</style>
