<template>
  <div class="msg-list">
    <card _Title="我的消息" :_Tabs="status" :_Size="16"  @_Change="statusChange"/>

    <Table v-if="params.status != 'ALREADY_REMOVE' " :columns="messageColumns" :data="messageData.records"></Table>
    <Table v-if="params.status == 'ALREADY_REMOVE' " :columns="messageDelColumns" :data="messageData.records"></Table>
    <!-- 分页 -->
    <Page
      style="float:right;margin-top:10px"
      :current="params.pageNumber"
      :total="messageData.total"
      :page-size="params.pageSize"
      @on-change="changePage"
      @on-page-size-change="changePageSize"
      :page-size-opts="[10, 20, 50]"
      size="small"
      show-total
      show-elevator
    ></Page>
  </div>
</template>
<script>
import {memberMsgList, readMemberMsg, delMemberMsg} from '@/api/member.js'
export default {
  data() {
    return {
      messageData: {}, // 消息数据
      status: ['未读', '已读', '回收站'],
      params: { // 请求参数
        pageNumber: 1,
        pageSize: 10,
        status: 'UN_READY'
      },
      messageDelColumns: [ // table展示数据
        {
          title: '消息标题',
          key: 'title',
          align: 'left',
          tooltip: true,
        },
        {
          title: '消息内容',
          key: 'content',
          align: 'left',
          tooltip: true
        },
        {
          title: '发送时间',
          key: 'createTime',
          align: 'left',
          width: 240
        },
      ],
      messageColumns: [ // table展示数据
        {
          title: '消息标题',
          key: 'title',
          align: 'left',
          tooltip: true,
        },
        {
          title: '消息内容',
          key: 'content',
          align: 'left',
          tooltip: true
        },
        {
          title: '发送时间',
          key: 'createTime',
          align: 'left',
          width: 240
        },
        {
          title: '操作',
          key: 'action',
          align: 'center',
          fixed: 'right',
          width: 150,
          render: (h, params) => {
            if (params.row.status === 'UN_READY') {
              return h('div', [
                h(
                  'Button',
                  {
                    props: {
                      type: 'info',
                      size: 'small'
                    },
                    style: {
                      marginRight: '5px'
                    },
                    on: {
                      click: () => {
                        this.setRead(params.row.id);
                      }
                    }
                  },
                  '已读'
                ), h(
                  'Button',
                  {
                    props: {
                      size: 'small',
                      type: 'error'
                    },
                    on: {
                      click: () => {
                        this.removeMessage(params.row.id);
                      }
                    }
                  },
                  '删除'
                )
              ]);
            } else if (params.row.status === 'ALREADY_READY') {
              return h('div', [
                h(
                  'Button',
                  {
                    props: {
                      size: 'small',
                      type: 'error'
                    },
                    on: {
                      click: () => {
                        this.removeMessage(params.row.id);
                      }
                    }
                  },
                  '删除'
                )
              ]);
            } else {

            }
          }
        }
      ]
    }
  },
  methods: {
    // 消息状态发生变化
    statusChange (index) {
      if (index === 0) { this.params.status = 'UN_READY' }
      if (index === 1) { this.params.status = 'ALREADY_READY' }
      if (index === 2) { this.params.status = 'ALREADY_REMOVE' }
      this.getList()
    },
    // 修改页码
    changePage (v) {
      this.params.pageNumber = v;
      this.getList();
    },
    // 修改页数
    changePageSize (v) {
      this.params.pageSize = v;
      this.getList();
    },
    getList () { // 获取消息列表
      memberMsgList(this.params).then(res => {
        if (res.success) {
          this.messageData = res.result;
        }
      })
    },
    // 设置消息已读
    setRead (id) {
      readMemberMsg(id).then(res => {
        if (res.success) {
          this.getList()
        }
      })
    },
    // 消息放入回收站
    removeMessage (id) {
      this.$Modal.confirm({
        title: '确认删除',
        // 记得确认修改此处
        content: '确认要删除此消息?',
        loading: true,
        onOk: () => {
          // 删除
          delMemberMsg(id).then((res) => {
            this.$Modal.remove();
            if (res.success) {
              this.$Message.success('消息已成功放入回收站');
              this.getList();
            }
          });
        },
      });
    }
  },
  mounted () {
    this.getList()
  }
}
</script>
<style lang="scss" scoped>

</style>
