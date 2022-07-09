<template>
  <!-- 预览保存 -->
  <div class="model-title">
    <div>店铺装修</div>
    <div class="btns">
      <Button
        @click="clickBtn(item)"
        size="small"
        v-for="(item, index) in way"
        :key="index"
        :type="item.selected ? 'primary' : ''"
      >
        {{ item.title }}
      </Button>
    </div>
    <div class="model-title-view-btn">
      <!-- TODO 后期会补全 目前版本暂无 -->
      <!-- <Poptip placement="bottom" width="100">
        <Button size="default" @click="creatQrCode">预览模板</Button>
        <div slot="content" class="default-view-content">
          <div>临时预览</div>
          <div ref="qrCodeUrl"></div>
        </div>
      </Poptip> -->
      <Button size="default" type="primary" @click="handleSpinShow">保存模板</Button>

      <Modal
        title="保存中"
        v-model="saveDialog"
        :closable="true"
        :mask-closable="false"
        :footer-hide="true"
      >
        <div v-if="progress">
          <div class="model-item">
            模板名称 <Input style="width: 200px" v-model="submitWay.name" />
          </div>
          <div class="model-item">
            是否立即发布
            <i-switch v-model="submitWay.pageShow">
              <span slot="open">开</span>
              <span slot="close">关</span>
            </i-switch>
          </div>

          <Button type="primary" @click="save()">保存</Button>
        </div>
        <Progress v-else :percent="num" status="active" />
      </Modal>
    </div>
  </div>
</template>
<script>
import * as API_Other from "@/api/other.js";

export default {
  data() {
    return {
      progress: true, // 展示进度
      num: 20, // 提交进度
      saveDialog: false, // 加载状态
      way: [
        // 装修tab栏切换
        {
          title: "首页",
          name: "index",
          selected: true,
        },
        // {
        //   title: "全屏广告",
        //   name: "advertising",
        //   selected: false,
        // },
        // {
        //   title: "弹窗广告",
        //   name: "alertAdvertising",
        //   selected: false,
        // },
      ],

      submitWay: {
        // 表单信息
        pageShow: this.$route.query.type || false,
        name: this.$route.query.name || "模板名称",
        pageClientType: "H5",
      },
    };
  },
  watch: {},
  mounted() {},
  methods: {
    clickBtn(val) {
      this.way.forEach((item, index) => {
        item.selected = false;
      });
      val.selected = true;
      this.$emit("selected", val.name);
    },

    /**
     * 加载，并保存模板
     */
    handleSpinShow() {
      this.saveDialog = true;
    },

    // 填写是否发布，模板名称之后保存
    save() {
      if (this.$store.state.styleStore == void 0) {
        this.$Message.error("请装修楼层");
        return false;
      }

      this.submitWay?.pageShow === true
        ? (this.submitWay.pageShow = "OPEN")
        : (this.submitWay.pageShow = "CLOSE");

      this.submitWay.pageData = JSON.stringify(this.$store.state.styleStore);
      this.submitWay.pageType = "INDEX";

      this.$route.query.id ? this.update() : this.submit(this.submitWay);
    },

    // 更新
    update() {
      this.progress = false;
      API_Other.updateHome(this.$route.query.id, {
        pageData: JSON.stringify(this.$store.state.styleStore),
        name: this.submitWay.name,
        pageShow: this.submitWay.pageShow,
        pageType: "INDEX",
        pageClientType: "H5",
      })
        .then((res) => {
          this.num = 50;
          if (res.success) {
            this.num = 80;
            /**制作保存成功动画¸ */
            setTimeout(() => {
              this.saveDialog = false;
              this.$Message.success("修改成功");
              this.goback();
            }, 1000);
          } else {
            this.saveDialog = false;
            this.$Message.error("修改失败，请稍后重试");
          }
          console.log(res);
        })
        .catch((error) => {});
    },

    // 返回查询数据页面
    goback() {
      this.$router.push({
        path: "/wapList",
      });
    },

    // 保存
    submit(submitWay) {
      this.progress = false;
      API_Other.setHomeSetup(submitWay)
        .then((res) => {
          this.num = 50;
          if (res.success) {
            this.num = 80;
            /**制作保存成功动画¸ */
            setTimeout(() => {
              this.saveDialog = false;
              this.$Message.success("保存成功");
              this.goback();
            }, 1000);
          } else {
            this.progress = true;
            this.saveDialog = false;
            this.$Message.error("保存失败，请稍后重试");
          }
          console.log(res);
        })
        .catch((error) => {});
    },
  },
};
</script>
<style scoped lang="scss">
.model-item {
  width: 100%;
  padding: 10px 0;
  display: flex;
  align-items: center;
  > * {
    margin: 0 8px;
  }
}
.model-title {
  height: 70px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #333;
  font-size: 14px;
  > .model-title-view-btn {
    > * {
      margin: 0 10px;
    }
  }
}
.btns {
  * {
    margin: 0 4px;
  }
}
</style>
