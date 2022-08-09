
<template>
  <div class="message-main-con">
    <div class="message-mainlist-con">
      <div>
        <Button @click="setCurrentMesType('unread')" size="large" long type="text">
          <div class="mes-wrap">
            <transition name="mes-current-type-btn">
              <Icon v-show="currentMessageType == 'unread'" type="md-checkmark"></Icon>
            </transition>
            <span class="mes-type-btn-text">未读消息</span>
            <Badge
              class="message-count-badge-outer"
              class-name="message-count-badge-red"
              :count="unReadCount"
            ></Badge>
          </div>
        </Button>
      </div>
      <div>
        <Button @click="setCurrentMesType('read')" size="large" long type="text">
          <div class="mes-wrap">
            <transition name="mes-current-type-btn">
              <Icon v-show="currentMessageType == 'read'" type="md-checkmark"></Icon>
            </transition>
            <span class="mes-type-btn-text">已读消息</span>
            <Badge
              class="message-count-badge-outer"
              class-name="message-count-badge"
              :count="hasReadCount"
            ></Badge>
          </div>
        </Button>
      </div>
      <div>
        <Button @click="setCurrentMesType('recycleBin')" size="large" long type="text">
          <div class="mes-wrap">
            <transition name="mes-current-type-btn">
              <Icon v-show="currentMessageType == 'recycleBin'" type="md-checkmark"></Icon>
            </transition>
            <span class="mes-type-btn-text">回收站</span>
            <Badge
              class="message-count-badge-outer"
              class-name="message-count-badge"
              :count="recycleBinCount"
            ></Badge>
          </div>
        </Button>
      </div>
    </div>
    <div class="message-content-con">
      <transition name="view-message">
        <div v-if="showMesTitleList" class="message-title-list-con">
          <Table
            class="mt_10"
            ref="messageList"
            :loading="loading"
            :columns="mesTitleColumns"
            :data="currentMesList"
            :no-data-text="noDataText"
          ></Table>
          <Page
            :current="params.pageNumber"
            :total="total"
            :page-size="params.pageSize"
            @on-change="changePage"
            @on-page-size-change="changePageSize"
            :page-size-opts="[5,10]"
            size="small"
            show-total
            show-elevator
            show-sizer
            class="page-fix"
          ></Page>
        </div>
      </transition>
      <transition name="back-message-list">
        <div v-if="!showMesTitleList" class="message-view-content-con">
          <div class="message-content-top-bar">
            <span class="mes-back-btn-con">
              <Button type="text" @click="backMesTitleList">
                <Icon type="ios-arrow-back"></Icon>&nbsp;&nbsp;返回
              </Button>
            </span>
            <h3 class="mes-title">{{ mes.title }}</h3>
          </div>
          <p class="mes-time-con">
            <Icon type="android-time"></Icon>
            &nbsp;&nbsp;{{ mes.time }}
          </p>
          <div class="message-content-body">
            <p class="message-content" v-html="mes.content">{{ mes.content }}</p>
          </div>
        </div>
      </transition>
    </div>
  </div>
</template>

<script>
  import Cookies from "js-cookie";
  import * as API_Index from "@/api/index";

  export default {
    name: "message_index",
    data() {
      const markAsReadBtn = (h, params) => {
        return h(
          "Button",
          {
            props: {
              icon: "md-eye-off",
              size: "small"
            },
            on: {
              click: () => {
                // 标记已读
                let v = params.row;
                this.loading = true;
                API_Index.read(v.id).then(res => {
                  this.loading = false;
                  if (res.success) {
                    this.$Message.success("操作成功");
                    this.currentMessageType = "unread"
                    this.getAll();
                  }
                });
              }
            }
          },
          "标为已读"
        );
      };
      const deleteMesBtn = (h, params) => {
        return h(
          "Button",
          {
            props: {
              icon: "md-trash",
              size: "small",
              type: "error"
            },
            on: {
              click: () => {
                // 移除
                let v = params.row;
                this.loading = true;
                API_Index.deleteMessage(v.id).then(res => {
                  this.loading = false;
                  if (res.success) {
                    this.$Message.success("删除成功");
                    this.currentMessageType = "read"
                    this.getAll();
                  }
                });
              }
            }
          },
          "删除"
        );
      };
      const restoreBtn = (h, params) => {
        return h(
          "Button",
          {
            props: {
              icon: "md-redo",
              size: "small"
            },
            style: {
              margin: "0 5px 0 0"
            },
            on: {
              click: () => {
                // 还原
                let v = params.row;
                API_Index.reductionMessage(v.id).then(res => {
                  this.loading = false;
                  if (res.success) {
                    this.setCurrentMesType("read");
                    this.recycleBinCount -= 1
                    this.hasReadCount +=1
                  }
                });
              }
            }
          },
          "还原"
        );
      };
      const deleteRealBtn = (h, params) => {
        return h(
          "Button",
          {
            props: {
              icon: "md-trash",
              size: "small",
              type: "error"
            },
            on: {
              click: () => {
                // 彻底删除
                let v = params.row;
                this.loading = true;
                API_Index.clearMessage(v.id).then(res => {
                  this.loading = false;
                  if (res.success) {
                    this.$Message.success("删除成功");
                    this.currentMessageType = "recycleBin"
                    this.getAll();
                  }
                });
              }
            }
          },
          "彻底删除"
        );
      };
      return {
        loading: true, // 列表加载的loading
        params: { // 请求消息列表参数
          status: "UN_READY",
          pageNumber: 1, // 当前页数
          pageSize: 10, // 页面大小
          sort: "createTime", // 默认排序字段
          order: "desc" // 默认排序方式
        },
        total: 0, // 消息列表总数
        totalUnread: 0, // 未读总数
        totalRead: 0, // 已读总数
        totalRemove: 0, // 回收站消息数
        currentMesList: [], // 当前状态消息
        unreadMesList: [], // 未读消息
        hasReadMesList: [], // 已读消息
        recyclebinList: [], // 回收站消息
        currentMessageType: "unread", // 当前列表消息状态
        showMesTitleList: true, // 是否展示消息状态列表
        unReadCount: 0, // 未读消息数量
        hasReadCount: 0, // 已读消息数量
        recycleBinCount: 0, // 回收站消息数量
        noDataText: "暂无未读消息",
        mes: { // 展示消息详情
          title: "",
          time: "",
          content: ""
        },
        mesTitleColumns: [ // 表格表头

          {
            title: " ",
            key: "title",
            align: "left",
            ellipsis: true,
            render: (h, params) => {
              return h("span", [
                h(
                  "a",
                  {
                    style: {
                      margin: "0 30px 0 0"
                    },
                    on: {
                      click: () => {
                        this.showMesTitleList = false;
                        this.mes.title = params.row.title;
                        this.mes.time = params.row.createTime;
                        this.getContent(params.row);
                      }
                    }
                  },
                  params.row.title
                )
              ]);
            }
          },
          {
            title: " ",
            key: "time",
            align: "center",
            width: 190,
            render: (h, params) => {
              return h("span", [
                h("Icon", {
                  props: {
                    type: "md-time",
                    size: 16
                  },
                  style: {
                    margin: "0 5px 3px 0"
                  }
                }),
                h("span", params.row.createTime)
              ]);
            }
          },
          {
            title: " ",
            key: "asread",
            align: "center",
            width: 210,
            render: (h, params) => {
              if (this.currentMessageType == "unread") {
                return h("div", [markAsReadBtn(h, params)]);
              } else if (this.currentMessageType == "read") {
                return h("div", [deleteMesBtn(h, params)]);
              } else {
                return h("div", [
                  restoreBtn(h, params),
                  deleteRealBtn(h, params)
                ]);
              }
            }
          }
        ]
      };
    },
    methods: {
      // 改变页数
      changePage(v) {
        this.params.pageNumber = v;
        this.refreshMessage();
      },
      // 改变页码
      changePageSize(v) {
        this.params.pageSize = v;
        this.refreshMessage();
      },
      // 刷新消息
      refreshMessage() {
        let status = "UN_READY";
        let type = this.currentMessageType;
        if (type == "unread") {
          status = "UN_READY";
        } else if (type == "read") {
          status = "ALREADY_READY";
        } else {
          status = "ALREADY_REMOVE";
        }
        this.params.status = status;
        this.loading = true;
        API_Index.getMessageSendData(this.params).then(res => {
          this.loading = false;
          if (res.success) {
            this.currentMesList = res.result.records;
            this.total = res.result.total;
          }
        });
      },
      //获取全部数据
      getAll() {
        API_Index.getAllMessage(this.params).then(res => {
          this.loading = false;
          if (res.success) {
            //未读消息
            this.unReadCount = res.result.UN_READY.total;
            this.currentMesList = res.result.UN_READY.records;
            //已读消息
            this.hasReadCount = res.result.ALREADY_READY.total;
            //回收站
            this.recycleBinCount = res.result.ALREADY_REMOVE.total;
          }
        });
      },
      // 删除消息
      deleteMessage(id) {
        API_Index.deleteMessage(id).then(res => {
          if (res.success) {
            this.$Message.success("删除成功");
          }
        });
      },
      backMesTitleList() {
        this.showMesTitleList = true;
      },
      // 设置当前消息分类
      setCurrentMesType(type) {
        if (this.currentMessageType !== type) {
          this.showMesTitleList = true;
        }
        this.currentMessageType = type;
        if (type == "unread") {
          this.noDataText = "暂无未读消息";
        } else if (type == "read") {
          this.noDataText = "暂无已读消息";
        } else {
          this.noDataText = "回收站无消息";
        }
        this.params.pageNumber = 1;
        this.refreshMessage();
      },
      getContent(v) {
        this.mes.content = v.content;
      }
    },
    mounted() {
      this.getAll();
    },
    watch: {
      // 监听路由变化通过id获取数据
      $route(to, from) {
        if (to.name == "message_index") {
          this.getAll();
        }
      }
    }
  };
</script>
<style lang="scss" scoped>
@import "./message.scss";
</style>

