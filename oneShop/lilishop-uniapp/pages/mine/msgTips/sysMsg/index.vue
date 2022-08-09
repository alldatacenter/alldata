<template>
  <view class="container">
    <block v-for="(row, index) in messageList" :key="index">
      <view class="msgItem">
        <div  class="is_read">
			<!-- {{row.is_read}} -->
			<span v-if="row.is_read"></span>
			<span v-else class="red">·</span>

		</div>
        <div class="msgMsg">{{$u.timeFormat(row.send_time, 'yyyy-mm-dd')}}</div>
        <u-card :title="title" :title-size="35" :border="false">
          <view class slot="body">
            <view class="u-body-item u-flex u-row-between u-p-b-0">
              <view class="u-body-item-title u-line-2">{{row.content}}</view>
            </view>
          </view>
        </u-card>
      </view>
    </block>
    <uni-load-more :status="loadStatus"></uni-load-more>
  </view>
</template>

<script>
import { mapMutations } from "vuex";
import * as API_Message from "@/api/message.js";
export default {
  data() {
    return {
      title: "系统消息",
      subTitle: "未读",
      finished: false,
      loadStatus: "more",
      params: {
        pageNumber: 0,
        pageSize: 5
      },
      messageList: []
    };
  },
  onLoad() {
    this.GET_MessageList(true);
  },
  onReachBottom() {
    this.params.pageNumber++;
    this.GET_MessageList(false);
  },
  methods: {
    ...mapMutations(["logout"]),
   
    /** 获取站内消息 */
    GET_MessageList(reset) {
      if (reset) {
        this.params.pageNumber = 1;
        this.messageList = [];
      }
      uni.showLoading({
        title: "加载中"
      });
      API_Message.getMessages(this.params).then(async response => {
        uni.hideLoading();
        const { data } = response;
        if (!data || !data.length) {
          this.messageList.push(...data.data);
          this.handleReadPageMessages();
        }
      });
    },
    /** 设置消息已读 **/
    handleReadPageMessages() {
      const ids = this.messageList.map(item => item.id).join(",");
      API_Message.messageMarkAsRead(ids).then(async () => {});
    }
  }
};
</script>

<style scoped lang='scss'>
.is_read{
	position: absolute;
    right: 25px;
    top: 80rpx;
    z-index: 999;
}
.container {
  background: #f9f9f9;
  min-height: 100vh;
}
.red{
	color: coral;
	font-size: 100rpx;
}
.msgMsg {
  text-align: center;
  color: $u-tips-color;
}
.msgItem {
  padding: 1em 0;
  position: relative;
}
</style>