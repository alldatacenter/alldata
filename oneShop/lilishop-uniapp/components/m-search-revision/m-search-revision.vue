<template>
  <view class="serach">
    <view class="left-box" @tap="onClickLeft">
      <u-icon name="arrow-left" size="40" color="#666"></u-icon>
    </view>
    <view class="content" :style="{ 'border-radius': radius + 'px' }">
      <!-- HM修改 增加进入输入状态的点击范围 -->
      <view class="content-box" :class="{ center: mode === 2 }">
        <u-icon name="search" size="32" style="padding:0 15rpx;"></u-icon>
        <!-- HM修改 增加placeholder input confirm-type confirm-->
        <input style="width:100%; " :placeholder="placeholder" placeholder-class="placeholder-color"
          @input="inputChange" confirm-type="search" @confirm="triggerConfirm" class="input"
          :class="{ center: !active && mode === 2 }" :focus="isFocus" v-model="inputVal" @focus="focus" @blur="blur" />
        <u-icon name="close" v-if="isDelShow" style="padding:0 30rpx;" @click="clear"></u-icon>
      </view>

    </view>
    <view class="button active" >
      <view v-if="isShowSeachGoods !=true" class="button-item">
        <div @click="out()">取消</div>
      </view>

      <view v-else class="button-item">
        <u-icon name="grid-fill" size="50" @click="handelListClass()" v-if="!switchLayout"></u-icon>
        <u-icon v-else @click="handelListClass()" name="list-dot" size="50"></u-icon>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  props: {
    mode: {
      value: Number,
      default: 1,
    },
    //HM修改 定义默认搜索关键词(水印文字)
    placeholder: {
      value: String,
      default: "请输入搜索内容",
    },
    value: {
      type: String,
      default: "",
    },
    // 默认半径为60
    radius: {
      value: String,
      default: 60,
    },
    // 是否获取焦点
    isFocusVal: {
      value: Boolean,
      default: true,
    },
  },
  data() {
    return {
      isShowSeachGoods: false, //是否显示查询的商品
      active: false, //是否选中
      inputVal: "", //Input中内容
      isDelShow: false, //是否显示右侧删除icon
      isFocus: false, //是否获取焦点
      switchLayout: true, //切换当前商品的布局，默认为两列
    };
  },
  mounted() {
    this.isFocus = this.isFocusVal;
  },
  methods: {
    //
    out() {
      uni.reLaunch({
        url: "/pages/tabbar/home/index",
      });
    },
    // 切换排列顺序
    handelListClass() {
      this.switchLayout = !this.switchLayout;
      this.$emit("SwitchType");
    },
    //HM修改 触发组件confirm事件
    triggerConfirm() {
      this.$emit("confirm", false);
      uni.hideKeyboard();
    },
    //HM修改 触发组件input事件
    inputChange(event) {
      var keyword = event.detail.value;
      this.$emit("input", keyword);
      if (this.inputVal) {
        this.isDelShow = true;
      }
    },
    focus() {
      this.active = true;
      //HM修改 增加获取焦点判断
      if (this.inputVal) {
        this.isDelShow = true;
      }
    },
    blur() {
      this.isFocus = false;
      if (!this.inputVal) {
        this.active = false;
      }
    },
    clear() {
      //HM修改 收起键盘
      uni.hideKeyboard();
      this.isFocus = false;
      this.inputVal = "";
      this.active = false;
      //HM修改 清空内容时候触发组件input
      this.$emit("input", "");
      //this.$emit('search', '');//HM修改 清空内容时候不进行搜索
    },

    /**
     * 回退到上一级
     */
    onClickLeft() {
      uni.navigateBack();
    },

    /**
     * 内容为空时，输入默认关键字
     */
    search() {
      if (!this.inputVal) {
        if (this.searchName == "取消") {
          uni.hideKeyboard();
          this.isFocus = false;
          this.active = false;
          return;
        }
      }
      this.$emit("search", this.inputVal ? this.inputVal : this.placeholder);
    },
  },
  watch: {
    /**
     * 监听当前是否有值 是否显示清除图标
     */
    inputVal(newVal) {
      newVal ? (this.isDelShow = true) : (this.isDelShow = false);
    },
  },
};
</script>

<style lang="scss" scoped>
.serach {
  display: flex;
  width: 100%;
  //border-bottom: 1px #f5f5f5 solid; //HM修改 去掉边框
  box-sizing: border-box;
  font-size: $uni-font-size-base;

  .left-box {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 15%;
    /* #ifndef APP-NVUE */
    text-align: center;
    // display: flex;
    // /* #endif */
  }

  .content {
    display: flex;
    align-items: center;
    width: 100%;
    height: 70rpx;
    color: #999;
    background: #eee;
    overflow: hidden;
    transition: all 0.2s linear;
    .content-box {
      width: 100%;
      display: flex;
      align-items: center;
      &.center {
        justify-content: center;
      }

      .icon {
        padding: 0 15rpx;

        &.icon-serach:before {
          content: "\e61c";
        }
      }

      .input {
        width: 100%;
        max-width: 100%;
        line-height: 60rpx;
        height: 60rpx;
        transition: all 0.2s linear;

        &.center {
          width: 200rpx;
        }

        &.sub {
          // position: absolute;
          width: auto;
          color: grey;
        }
      }
    }
  }

  .button {
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;
    flex-shrink: 0;
    width: 0;
    transition: all 0.2s linear;
    white-space: nowrap;
    overflow: hidden;

    &.active {
      padding-left: 15rpx;
      width: 100rpx;
    }
  }
}

.icon {
  font-family: iconfont;
  font-size: 32rpx;
  font-style: normal;
  color: #999;
}

.placeholder-color {
  color: #999;
  opacity: 0.4;
}
</style>
