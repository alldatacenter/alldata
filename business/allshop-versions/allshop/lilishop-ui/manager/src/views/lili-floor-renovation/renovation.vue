<template>
  <div class="renovation">
    <!-- 左侧模块列表 -->
    <div class="model-list">
      <div class="classification-title">基础模块</div>
      <draggable tag="ul" :list="modelData" v-bind="{group:{ name:'model', pull:'clone',put:false},sort:false, ghostClass: 'ghost'}"   >
        <li v-for="(model, index) in modelData" :key="index" class="model-item">
          <Icon :type="model.icon" />
          <span>{{model.name}}</span>
        </li>
      </draggable>
    </div>
    <!-- 中间展示模块 -->
    <div class="show-content">
      <model-form ref="modelForm" :data="modelForm"></model-form>
    </div>
    <!-- 操作按钮 -->
    <div class="btn-bar">
      <Button type="primary" :loading="submitLoading" @click="saveTemplate">保存模板</Button>
      <Button class="ml_10" @click="resetTemplate">还原模板</Button>
    </div>
  </div>
</template>
<script>
import { modelData } from "./modelConfig";
import Draggable from "vuedraggable";
import ModelForm from "./modelForm.vue";
import * as API_floor from "@/api/other.js";
export default {
  components: {
    Draggable,
    ModelForm,
  },
  mounted() {
    this.getTemplateItem(this.$route.query.id);
  },
  data() {
    return {
      modelData, // 可选模块数据
      modelForm: { list: [] }, // 模板数据
      submitLoading: false, // 提交加载状态
    };
  },
  methods: {
    saveTemplate() {
      // 保存模板
      this.submitTemplate(this.$route.query.pageShow ? 'OPEN' : 'CLOSE')
    },
    // 提交模板
    submitTemplate(pageShow) {
      this.submitLoading = true
      const modelForm = JSON.parse(JSON.stringify(this.modelForm)) 
      modelForm.list.unshift(this.$refs.modelForm.navList);
      modelForm.list.unshift(this.$refs.modelForm.topAdvert);
      const data = {
        id: this.$route.query.id,
        pageData: JSON.stringify(modelForm),
        pageShow
      };
      API_floor.updateHome(this.$route.query.id, data).then((res) => {
        this.submitLoading = false
        if (res.success) {
          this.$Message.success("保存模板成功");
        }
      });
    },
    resetTemplate() {
      // 还原模板
      this.getTemplateItem(this.$route.query.id);
    },
    getTemplateItem(id) {
      // 获取模板数据
      API_floor.getHomeData(id).then((res) => {
        if (res.success) {
          let pageData = res.result.pageData;
          if (pageData) {
            pageData = JSON.parse(pageData);
            if (pageData.list[0].type === "topAdvert") {
              // topAdvert 为顶部广告 navList为导航栏
              this.$refs.modelForm.topAdvert = pageData.list[0];
              this.$refs.modelForm.navList = pageData.list[1];
              pageData.list.splice(0, 2);
              this.modelForm = pageData;
            } else {
              this.modelForm = { list: [] };
            }
          } else {
            this.modelForm = { list: [] };
          }
        }
      });
    },
  },
  watch: {
    modelForm: {
      deep: true,
      handler: function (val) {
        console.log(val);
      },
    },
  },
};
</script>
<style lang="scss" scoped>
.renovation {
  position: relative;
  display: flex;
}
.model-list {
  width: 120px;
  height: auto;
  padding: 10px;
  background: #fff;
  margin-top: 60px;
  position: fixed;
  z-index: 100;
  box-shadow: 1px 1px 10px #999;
  .classification-title {
    width: 100%;
    height: 30px;
    line-height: 30px;
  }
  .model-item {
    width: 100px;
    height: 30px;
    background: #eee;
    margin-top: 10px;
    line-height: 30px;
    text-align: center;
    color: #999;
    &:hover {
      border: 1px dashed #409eff;
      color: #409eff;
      cursor: move;
    }
  }
  .ghost::after {
    border: none;
    height: 0;
    content: "";
  }
}
.show-content {
  margin-left: 150px;
  margin-top: 60px;
}
.ghost {
  background: #fff;
  height: 30px;
  position: relative;
  &::after {
    content: "松开鼠标添加模块";
    position: absolute;
    background: #fff;
    border: 1px dashed #409eff;
    color: #409eff;
    top: 0;
    left: 0;
    width: 100%;
    height: 50px;
    text-align: center;
    line-height: 50px;
  }
}
.btn-bar {
  position: fixed;
  width: 100%;
  background: #fff;
  height: 50px;
  padding: 10px;
  box-shadow: 1px 1px 10px #999;
  z-index: 99;
  top: 100px;
}
</style>
