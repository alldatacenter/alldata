<template>
  <div @click="showMessage" class="message-con">
    <Tooltip :always="value>0" :content="value > 0 ? '有' + value + message : '无未读消息'" placement="bottom">
      <Badge :count="value" dot>
        <Icon type="md-notifications" :size="22" />
      </Badge>
    </Tooltip>
  </div>
</template>

<script>
import util from "@/libs/util.js";
export default {
  name: "messageTip",
  props: {
    value: { // 未读消息数量
      type: Number,
      default: 0
    },
    message:{ // 消息展示内容
      type: String,
      default: ""
    }
  },
  methods: {
    showMessage() {
      util.openNewPage(this, "message_index");
      this.$router.push({
        name: "message_index"
      });
    }
  }
};
</script>
