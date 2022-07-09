<template>
  <Card class="_Card" :bordered="false" :dis-hover="true">
    <div slot="title" class="cardTitle">
      <span :style="{fontSize:`${_Size}px`}">{{_Title}}</span>

      <div v-if="_Tabs" class="cardTabs">
        <div @click="tabsChange(index)" :class="{active:(isActive==index)}" class="cardTabsItem" :style="{fontSize:`${_Size-2}px`}" v-for="(item,index) in _Tabs"
             :key="index">
          {{item}}
        </div>
      </div>
    </div>
    <div slot="extra" class="cardExtra" v-if="_More" @click="callBack()">
      {{_More}}
    </div>
    <div>

    </div>
  </Card>
</template>

<script>
export default {
  name: 'index',
  props:
    {
      _Tabs: { // 可点击的tab栏
        type: null,
        default: ''
      },
      // 头部
      _Title: { // 标题
        type: null,
        default: '卡片头部'
      },
      // 右侧更多
      _More: {
        type: null,
        default: false
      },
      _Size: { // 文字大小
        type: Number,
        default: 16
      },
      // 点击更多触发跳转
      _Src: {
        type: null,
        default: function (val) {
          if (this._More) {
            return val;
          } else {
            return false;
          }
        }
      }
    },
  data () {
    return {
      isActive: 0 // 已激活tab栏下标
    };
  },
  methods: {
    // 点击右侧的回调
    callBack () {
      let _this = this;
      if (this._Src !== '' || this._Src != null) {
        this.$router.push({
          path: _this._Src
        });
      }
    },
    // 点击tab的回调
    tabsChange (index) {
    //  处理并返回index
      this.isActive = index;
      this.$emit('_Change', index);
    }
  }
};
</script>

<style scoped lang="scss">
  .cardTitle {
    display: flex;
    cursor: pointer;
  }
  .active{
    color: $theme_color;
    position: relative;
    &::before{
      content: '';
      position: absolute;
      width: 100%;
      height: 3px;
      bottom: 0;
      left: 0;
      background: $theme_color;
    }
  }

  .cardTabs {
    display: flex;
    padding: 0 12px;

    > .cardTabsItem {
      padding: 0 12px;

    }

    > .cardTabsItem:hover {

      color: $theme_color;
    }
  }

  /deep/ .ivu-card, .ivu-card-head, ._Card {
    margin-bottom: 20px;
    @include white_background_color();
  }

  /deep/ .ivu-card-head {
    position: relative;
    padding: 0 14px;
    height: 50px;
    line-height: 50px;

    &::before {
      content: '';
      width: 3px;
      height: 50%;
      top: 25%;
      background: $theme_color;
      position: absolute;
      left: 0;
    }
  }

  .cardExtra {
    color: $theme_color;
    cursor: pointer;
  }

  /deep/ .ivu-card-body {
    padding: 0 !important;
    display: none;
  }

</style>
