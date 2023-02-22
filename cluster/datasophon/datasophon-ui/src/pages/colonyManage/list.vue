
<template>
  <div class="card-list card-shadow">
    <a-list :grid="{ gutter: 24, lg: 3, md: 2, sm: 1, xs: 1 }" :dataSource="dataSource">
      <a-list-item slot="renderItem" slot-scope="item">
        <template v-if="item.add">
          <div class="new-btn" @click="addColony({})">
            <div class="add-icon">
              <svg-icon icon-class="add-cluster" style="font-size: 80px"></svg-icon>
            </div>
            <div>创建集群</div>
          </div>
        </template>
        <template v-else>
          <div :class="['colony-card', item.clusterStateCode === 2 ? 'colony-running-card' : 'colony-configured-card']">
            <div class="card-header flex-bewteen-container">
              <div class="flex-container">
                <div :class="['colony-icon-warp', item.clusterStateCode === 2 ? 'running-status-bg' : 'configured-status-bg']">
                  <svg-icon :class="['colony-icon', item.clusterStateCode === 2 ? 'running-status-color' : 'configured-status-color']" icon-class="colony"></svg-icon>
                </div>
                <div class="colony-title">{{ item.clusterName }}</div>
              </div>
              <div :class="['colony-status']">
                <svg-icon :class="['colony-status-icon', item.clusterStateCode === 2 ? 'running-status-color' : 'configured-status-color']" :icon-class="item.clusterStateCode === 2 ? 'running-status' : 'configured-status'"></svg-icon>
                <span class="mgl5">{{item.clusterState}}</span>
              </div>
            </div>
            <div class="card-content">
              <div>
                集群管理员：
                <span>{{item.userManageName || '-'}}</span>
              </div>
              <div>
                创建时间：
                <span>{{item.createTime}}</span>
              </div>
            </div>
            <div class="card-footer flex-bewteen-container">
              <a-button v-if="user && user.userType === 1" type="link" @click="authCluster(item)">授权</a-button>
              <a-button type="link" @click="addColony(item)" :disabled="item.clusterStateCode === 2">编辑</a-button>
              <a-button type="link" @click="getInto(item)" :disabled="item.clusterStateCode === 1">进入</a-button>
              <a-button type="link" :disabled="item.clusterStateCode === 2" @click="configCluster(item)">配置集群</a-button>
              <a-button type="link" @click="delectColony(item)" :disabled="item.clusterStateCode === 2">删除集群</a-button>
            </div>
          </div>
        </template>
      </a-list-item>
    </a-list>
    <!-- 配置集群的modal -->
    <a-modal v-if="visible" title :visible="visible" :maskClosable="false" :closable="false" :width="1576" :confirm-loading="confirmLoading" @cancel="handleCancel" :footer="null">
      <Steps :clusterId="clusterId" />
    </a-modal>
  </div>
</template>

<script>
import AddColony from "./commponents/addColony.vue";
import AuthCluster from "./commponents/authCluster.vue";
import DelectColony from "./commponents/delectColony.vue";
import { mapGetters, mapActions, mapMutations } from "vuex";
import Steps from "@/components/steps";
import { changeRouter } from '@/utils/changeRouter'
export default {
  name: "COLONYLIST",
  components: { Steps },
  provide() {
    return {
      handleCancel: this.handleCancel,
      onSearch: null
    };
  },
  data() {
    return {
      visible: false,
      dataSource: [],
      confirmLoading: false,
      clusterId: "", // 操作的集群Id
    };
  },
  computed: {
    ...mapGetters("account", ["user"]),
  },
  methods: {
    ...mapActions("steps", ["setClusterId"]),
    ...mapMutations("setting", ["setIsCluster", "setMenuData"]),
    // 进入
    getInto(row) {
      this.$axiosPost(global.API.getServiceListByCluster, {
        clusterId: row.id,
      }).then((res) => {
        changeRouter(res.data, row.id)
        this.$router.push("/overview");
      });
    },
    addColony(obj) {
      const self = this;
      let width = 520;
      let title = JSON.stringify(obj) !== "{}" ? "编辑集群" : "创建集群";
      let content = (
        <AddColony detail={obj} callBack={() => self.getColonyList()} />
      );
      this.$confirm({
        width: width,
        title: title,
        content: content,
        closable: true,
        icon: () => {
          return <div />;
        },
      });
    },
    delectColony(obj) {
      const self = this;
      let width = 400;
      let content = (
        <DelectColony
          sysTypeTxt="集群"
          detail={obj}
          callBack={() => self.getColonyList()}
        />
      );
      this.$confirm({
        width: width,
        title: () => {
          return (
            <div>
              <a-icon
                type="question-circle"
                style="color:#2F7FD1 !important;margin-right:10px"
              />
              提示
            </div>
          );
        },
        content,
        closable: true,
        icon: () => {
          return <div />;
        },
      });
    },
    getColonyList() {
      this.$axiosPost(global.API.getColonyList, {}).then((res) => {
        console.log(res);
        this.dataSource = res.data;
        this.dataSource.forEach((item) => {
          let arr = [];
          item.clusterManagerList.map((childItem) => {
            arr.push(childItem.username);
          });
          item["userManageName"] = arr.join(",");
        });
        console.log(this.dataSource, "2222");
        this.dataSource.push({
          add: true,
        });
      });
    },
    // 集群授权
    authCluster(obj) {
      const self = this;
      let width = 520;
      let title = "授权";
      let content = (
        <AuthCluster detail={obj} callBack={() => self.getColonyList()} />
      );
      this.$confirm({
        width: width,
        title: title,
        content: content,
        closable: true,
        icon: () => {
          return <div />;
        },
      });
    },
    // 配置集群
    configCluster(row) {
      this.clusterId = row.id;
      this.visible = true;
    },
    handleCancel(e) {
      this.visible = false;
      this.getColonyList()
    },
  },
  mounted() {
    this.getColonyList();
  },
};
</script>

<style lang="less" scoped>
/deep/ .ant-modal-body {
  padding: 0;
}
/deep/ .ant-modal {
  top: 62px;
  .ant-modal-content {
    border-radius: 4px;
  }
}
.card-list {
  padding: 20px 10px;
  background: #fff;
  /deep/ .ant-row {
    margin: 0 !important;
  }
  /deep/ .ant-col {
    padding-left: 10px !important;
    padding-right: 10px !important;
  }
}
.colony-card {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  // cursor: pointer;
  height: 220.48px;
  padding: 20px 10px 0px;
  background: #fff;
  border: 1px solid rgba(227, 228, 230, 1);
  border-radius: 4px;
  .card-header {
    padding: 0 10px;
    .colony-icon-warp {
      width: 50px;
      height: 50px;
      border-radius: 50%;
      text-align: center;
      line-height: 50px;
      .colony-icon {
        // color: @primary-color;
        font-size: 24px;
        cursor: pointer;
      }
    }
    .colony-title {
      margin-left: 20px;
      font-size: 16px;
      color: #333333;
      letter-spacing: 0;
      font-weight: 600;
    }
    .colony-status {
      .colony-status-icon {
        font-size: 14px;
      }
    }
  }
  .card-content {
    margin-left: 70px;
    div {
      margin-top: 10px;
      margin-bottom: 6px;
      font-size: 14px;
      color: #666666;
      letter-spacing: 0;
      font-weight: 400;
      span {
        color: #333333;
        word-break: break-all;
        white-space: normal;
      }
    }
  }
  .card-footer {
    border-top: 1px solid #e3e4e6;
    height: 50px;
    line-height: 50px;
    /deep/ .ant-btn-link {
      width: 20%;
      margin: 12px 0;
      border-radius: 0;
      font-size: 14px;
      color: #555555;
      letter-spacing: 0;
      font-weight: 400;
      border: none;
    }
    /deep/ .ant-btn-link:not(:last-child) {
      border: none;
      border-right: 1px solid#e3e4e6;
    }
    /deep/ .ant-btn-link:not(:last-child):hover,
    .ant-btn-link:not(:last-child):focus {
      border: none;
      border-right: 1px solid#e3e4e6;
    }
  }
  /deep/ .ant-btn-link:not(.ant-btn-link[disabled]):hover {
    color: @primary-color;
  }
  /deep/ .ant-btn-link[disabled] {
    background: #fff;
    color: #bbb;
  }
}
.colony-running-card:hover {
  border: 1px solid @running-status-color;
}
.colony-configured-card:hover {
  border: 1px solid @configured-status-color;
}
.card-avatar {
  width: 48px;
  height: 48px;
  border-radius: 48px;
}
.new-btn {
  display: flex;
  flex-direction: column;
  justify-content: center;
  border-radius: 2px;
  width: 100%;
  height: 220.48px;
  border-radius: 2px;
  text-align: center;
  font-size: 16px;
  border: 1px dashed #e3e4e6;
  cursor: pointer;
  .add-icon {
    // background-image: url("../../assets/img/colony/add-colony.svg");
    // background-size: 100% 100%;
    // margin: 0 auto 20px;
  }
  &:hover {
    color: @primary-color;
    background: rgba(2, 121, 254, 0.03);
    border: 1px dashed @primary-color;
    .add-icon {
      // background-image: url("../../assets/img/colony/add-colony-hover.svg");
    }
  }
}
.meta-content {
  position: relative;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  height: 64px;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}
</style>
