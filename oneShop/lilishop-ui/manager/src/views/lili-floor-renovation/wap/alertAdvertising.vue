<template>
  <div class="model-view">
    <div class="model-view-content">
      <div class="content">
        <div class="wap-title">首页</div>
        <div class="draggable">
          <!-- 弹窗广告 -->
          <div class="window-shadow">
            <img :src="advertising[0].img" alt="" />
          </div>
        </div>
      </div>
    </div>
    <div class="model-config">
      <div class="decorate">
        <div class="decorate-title">弹窗广告</div>

        <div class="decorate-list">
          <div
            class="decorate-item"
            v-for="(item, index) in advertising"
            :key="index"
          >
            <div class="decorate-item-title">
              <div>设置</div>
            </div>
            <div class="decorate-item-box">
              <!-- 选择照片 -->
              <div class="decorate-view">
                <div class="decorate-view-title">选择图片</div>
                <div>
                  <img class="show-image" :src="item.img" alt />
                  <input
                    type="file"
                    class="hidden-input"
                    @change="changeFile(item, index)"
                    ref="files"
                    :id="'files' + index"
                  />
                  <div class="tips">
                    建议尺寸
                    <span>{{ item.size }}</span>
                  </div>
                </div>
                <div class="selectBtn">
                  <Button
                    size="small"
                    @click="handleClickFile(item, index)"
                    ghost
                    type="primary"
                    >选择照片</Button
                  >
                </div>
              </div>

              <!-- 选择连接 -->
              <div class="decorate-view">
                <div class="decorate-view-title">选择图片</div>
                <div>
                  <Button
                    ghost
                    type="primary"
                    size="small"
                    @click="clickLink(item)"
                    >选择链接</Button
                  >
                </div>
              </div>
            </div>
          </div>
        </div>
        <liliDialog ref="liliDialog" :types="linkType"></liliDialog>
      </div>
    </div>
  </div>
</template>
<script>
export default {
  data() {
    return {
      type: "full", // 展示方式
      //全屏广告
      advertising: [
        {
          img:
            "https://shopro-1253949872.file.myqcloud.com/uploads/20200704/9136ecddcddf6607184fab689207e7e3.png",
          size: "612*836",
        },
      ],
      linkType: "", // 选择类型
    };
  },
  methods: {
    // 点击链接
    clickLink(item) {
      this.$refs.liliDialog.open('link')
    },
    //点击图片解析成base64
    changeFile(item, index) {
      const file = document.getElementById("files" + index).files[0];
      if (file == void 0) return false;
      const reader = new FileReader();
      reader.readAsDataURL(file);
      this.$nextTick((res) => {
        reader.onload = (e) => {
          item.img = e.target.result;
        };
      });
    },
    // 点击选择照片
    handleClickFile(item, index) {
      document.getElementById("files" + index).click();
    },
  },
};
</script>
<style scoped lang="scss">
@import "./style.scss";
@import "./decorate.scss";
.decorate-radio {
  margin: 10px 0;
}
.window-shadow,
.full-shadow {
  display: flex;
  position: absolute;
  top: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.2);
  left: 0;
  align-items: center;
  justify-content: center;
}
.window-shadow {
  > img {
    width: 306px;
    height: 418px;
  }
}
.full-shadow {
  > img {
    width: 100%;
    height: 100%;
  }
}
.draggable {
  position: relative;
}

.btn-item {
  > img {
    margin: 4px 0;
    border-radius: 50%;
    width: 30px;
    height: 30px;
  }
}
</style>
