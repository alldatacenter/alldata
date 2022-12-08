<template>
  <div>
    <div style="display:flex;">
      <Input
        v-model="departmentTitle"
        readonly
        style="margin-right:10px;"
        :placeholder="placeholder"
        :clearable="clearable"
        @on-clear="clearSelect"
      />
      <Poptip transfer trigger="click" placement="right" title="选择部门" width="250">
        <Button icon="md-list">选择部门</Button>
        <div slot="content">
          <Input
            v-model="searchKey"
            suffix="ios-search"
            @on-change="searchDep"
            placeholder="输入部门名搜索"
            clearable
          />
          <div class="dep-tree-bar">
            <Tree
              :data="dataDep"
              @on-select-change="selectTree"
            ></Tree>
            <Spin size="large" fix v-if="depLoading"></Spin>
          </div>
        </div>
      </Poptip>
    </div>
  </div>
</template>

<script>
import {initDepartment, searchDepartment} from "@/api/index";

export default {
  name: "departmentTreeChoose",
  props: {
    multiple: {
      type: Boolean,
      default: false
    },
    clearable: {
      type: Boolean,
      default: true
    },
    placeholder: {
      type: String,
      default: "点击选择部门"
    }
  },
  data() {
    return {
      depLoading: false, // 加载状态
      departmentTitle: "", // modal标题
      searchKey: "", // 搜索关键词
      dataDep: [], // 部门列表
      selectDep: [], // 已选部门
      departmentId: [] // 部门id
    };
  },
  methods: {
    // 获取部门数据
    initDepartmentData() {
      initDepartment().then(res => {
        if (res.success) {
          this.dataDep = res.result;
        }
      });
    },
    searchDep() {
      // 搜索部门
      if (this.searchKey) {
        this.depLoading = true;
        searchDepartment({title: this.searchKey}).then(res => {
          this.depLoading = false;
          if (res.success) {
            res.result.forEach(function (e) {
              if (e.status == -1) {
                e.title = "[已禁用] " + e.title;
                e.disabled = true;
              }
            });
            this.dataDep = res.result;
          }
        });
      } else {
        this.initDepartmentData();
      }
    },
    // 选择回调
    selectTree(v) {
      if (v.length === 0) {
        this.$emit("on-change", null);
        this.departmentId = "";
        this.departmentTitle = "";
        return
      }
      this.departmentId = v[0].id;
      this.departmentTitle = v[0].title;
      let department = {
        departmentId: this.departmentId,
        departmentTitle: this.departmentTitle
      }
      this.$emit("on-change", department);
    },
    // 清除选中方法
    clearSelect() {
      this.departmentId = [];
      this.departmentTitle = "";
      this.initDepartmentData();
      if (this.multiple) {
        this.$emit("on-change", []);
      } else {
        this.$emit("on-change", "");
      }
      this.$emit("on-clear");
    },
    // 设置数据 回显用
    setData(ids, title) {
      this.departmentTitle = title;
      if (this.multiple) {
        this.departmentId = ids;
      } else {
        this.departmentId = [];
        this.departmentId.push(ids);
      }
    }
  },
  created() {
    this.initDepartmentData();
  }
};
</script>

<style lang="scss" scoped>
.dep-tree-bar {
  position: relative;
  min-height: 80px;
  max-height: 500px;
  overflow: auto;
  margin-top: 5px;
}

.dep-tree-bar::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

.dep-tree-bar::-webkit-scrollbar-thumb {
  border-radius: 4px;
  -webkit-box-shadow: inset 0 0 2px #d1d1d1;
  background: #e4e4e4;
}

</style>

