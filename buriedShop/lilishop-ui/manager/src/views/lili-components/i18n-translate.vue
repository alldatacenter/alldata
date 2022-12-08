<template>
  <div>
    <Button @click="enable = true">语言设定</Button>
    <Modal v-model="enable" draggable sticky scrollable :mask="false" :title="title">

      <Tabs closable type="card" @on-tab-remove="handleTabRemove" :value="language[0].title">
        <TabPane v-for="(item,index) in language" :key="index" :label="item.title" :name="item.title">
          <Input v-model="item.___i18n" />
        </TabPane>
      </Tabs>
    </Modal>
  </div>
</template>

<script>
import {language} from "./languages";
export default {
  /**
   * tabs 循环的语言内容格式 [{'title':'test','value':'val'}]
   */
  props: {
    value: {
      type: null,
      default: "",
    },
  },
  watch: {
    language: {
      handler(val) {
        this.$emit("language", { language: [...val], val: this.value });
      },
      deep: true,
    },
  },
  data() {
    return {
      language,
      tabVal: "",
      enable: false, //是否开启modal
      title: "转换语言",
    };
  },
  methods: {
    //   删除tab标签将没有用的语音进行删除
    handleTabRemove(tab) {
      this.language = this.language.filter((item) => {
        return item.value != tab;
      });
    },
  },
};
</script>

<style lang="scss" scoped>
</style>