<template>
  <div class="model-carousel1" :style="{ background: bgColor }">
    <div class="nav-body clearfix">
      <!-- 侧边导航 -->
      <div class="nav-side">分类占位区</div>
      <div class="nav-content setup-content">
        <!-- 轮播图 -->
        <Carousel autoplay @on-change="autoChange">
          <CarouselItem v-for="(item, index) in data.options.list" :key="index">
            <div style="overflow: hidden">
              <img :src="item.img" width="1200" height="470" />
            </div>
          </CarouselItem>
        </Carousel>
        <div class="setup-box">
          <div>
            <Button size="small" @click.stop="handleSelectModel">编辑</Button>
          </div>
        </div>
      </div>
    </div>
    <Modal
      v-model="showModal"
      title="快捷导航"
      draggable
      width="800"
      :z-index="100"
      :mask-closable="false"
    >
      <div class="modal-tab-bar">
        <Button type="primary" size="small" @click="handleAdd">添加轮播</Button>
        &nbsp;
        <span class="ml_10">图片尺寸:{{ data.size }}</span>
        <span style="color: red" class="fz_12 ml_10"
          >点击缩略图替换图片、点击颜色选择器选择背景色</span
        >
        <table cellspacing="0">
          <thead>
            <tr>
              <th width="250">所选图片</th>
              <th width="250">链接地址</th>
              <th width="250">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, index) in data.options.list" :key="index">
              <td>
                <img
                  style="cursor: pointer"
                  :src="item.img"
                  @click="handleSelectImg(item)"
                  width="200"
                  height="100"
                  alt=""
                />
              </td>
              <td>
                <Input
                  class="outsideUrl"
                  v-model="item.url"
                  :disabled="!!item.type && item.type !== 'link'"
                />
              </td>
              <td>
                <Button type="info" size="small" @click="handleSelectLink(item)"
                  >选择链接</Button
                >&nbsp;
                <ColorPicker size="small" v-model="item.bgColor" />
                &nbsp;
                <Button
                  type="error"
                  ghost
                  size="small"
                  @click="handleDel(index)"
                  >删除</Button
                >
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </Modal>
    <!-- 选择商品。链接 -->
    <liliDialog ref="liliDialog" @selectedLink="selectedLink"></liliDialog>
    <!-- 选择图片 -->
    <Modal width="1200px" v-model="picModelFlag" footer-hide>
      <ossManage @callback="callbackSelected" ref="ossManage" />
    </Modal>
  </div>
</template>

<script>
import ossManage from "@/views/sys/oss-manage/ossManage";
export default {
  name: "modelCarousel",
  props: ["data"],
  components: {
    ossManage,
  },
  data() {
    return {
      showModal: false, // modal显隐
      selected: null, // 已选数据
      picModelFlag: false, // 选择图片modal
      bgColor: "#fff", // 轮播背景色
    };
  },
  mounted() {
    this.bgColor = this.data.options.list[0].bgColor;
  },
  methods: {
    handleSelectModel() {
      // 编辑模块
      this.showModal = true;
    },
    // 自动切换时改变背景色
    autoChange(oVal, val) {
      this.bgColor = this.data.options.list[val].bgColor;
    },
    // 添加轮播图片和链接
    handleAdd() {
      this.data.options.list.push({ img: "", url: "", bgColor: "#fff" });
      this.$forceUpdate();
    },
    // 打开选择链接modal
    handleSelectLink(item) {
      this.$refs.liliDialog.open("link");
      this.selected = item;
    },
    callbackSelected(item) {
      // 选择图片回调
      this.picModelFlag = false;
      this.selected.img = item.url;
    },
    // 删除图片
    handleDel(index) {
      this.data.options.list.splice(index, 1);
    },
    selectedLink(val) {
      // 选择链接回调
      this.selected.url = this.$options.filters.formatLinkType(val);
      this.selected.type =
        val.___type === "other" && val.url === "" ? "link" : "other";
    },
    // 打开选择图片modal
    handleSelectImg(item) {
      this.selected = item;
      this.$refs.ossManage.selectImage = true;
      this.picModelFlag = true;
    },
  },
};
</script>

<style scoped lang="scss">
@import "./setup-box.scss";
.model-carousel1 {
  width: 1500px;
  height: 470px;
  margin-left: -150px;
  background: #fff;
}

/*大的导航信息，包含导航，幻灯片等*/
.nav-body {
  width: 1200px;
  height: 470px;
  margin: 0px auto;
}
.nav-side {
  height: 100%;
  width: 200px;
  padding: 0px;
  color: #fff;
  background-color: rgba(0, 0, 0, 0.5);
  line-height: 470px;
  text-align: center;
  position: absolute;
  z-index: 1;
}

/*导航内容*/
.nav-content {
  width: 1200px;
  height: 470px;
  overflow: hidden;
  float: left;
  position: relative;
}
</style>
