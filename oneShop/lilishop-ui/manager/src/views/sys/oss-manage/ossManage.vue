<style lang="scss">
@import "./ossManage.scss";
</style>
<template>
  <div class="search">
    <Card>
      <div class="operation">
        <Row @keydown.enter.native="handleSearch">
          <Form ref="searchForm" :model="searchForm" inline :label-width="85"    class="search-form"
          >
            <Form-item label="原文件名" prop="name">
              <Input
                type="text"
                v-model="searchForm.name"
                placeholder="请输入原文件名"
                clearable
                style="width: 200px"
              />
            </Form-item>
            <Form-item label="存储文件名" prop="fileKey">
              <Input
                type="text"
                v-model="searchForm.fileKey"
                placeholder="请输入存储文件名"
                clearable
                style="width: 200px"
              />
            </Form-item>

            <Form-item label="上传时间">
              <DatePicker
                v-model="selectDate"
                type="daterange"
                format="yyyy-MM-dd"
                clearable
                @on-change="selectDateRange"
                placeholder="选择起始时间"
                style="width: 200px"
              ></DatePicker>
            </Form-item>
             <Button @click="handleSearch" type="primary" icon="ios-search" class="search-btn"
              >搜索
              </Button>
          </Form>
        </Row>
        <div class="oss-operation padding-row">
          <div>
            <Upload
              style="display:inline-block;"
              :action="commonUrl + '/common/common/upload/file'"
              :headers="accessToken"
              :on-success="handleSuccess"
              :on-error="handleError"
              :show-upload-list="false"
              :max-size="2048"
              :on-exceeded-size="handleMaxSize"
              multiple
              ref="up"
            >
              <Button type="primary">上传文件</Button>
            </Upload>
            <Dropdown @on-click="handleDropdown">
              <Button>
                更多操作
                <Icon type="md-arrow-dropdown"/>
              </Button>
              <DropdownMenu slot="list">
                <DropdownItem name="refresh">刷新</DropdownItem>
                <DropdownItem v-show="showType == 'list'" name="removeAll">批量删除
                </DropdownItem>
              </DropdownMenu>
            </Dropdown>
          </div>

          <div>
            <RadioGroup
              v-model="fileType"
              @on-change="changeFileType"
              type="button" button-style="solid"
              style="margin-right: 25px"
            >
              <Radio label="all">所有类型</Radio>
              <Radio label="pic">图片</Radio>
              <Radio label="video">视频</Radio>
            </RadioGroup>
            <RadioGroup
              v-model="showType"
              type="button" button-style="solid"
              @on-change="changeShowType"
            >
              <Radio title="列表" label="list">
                <Icon type="md-list"></Icon>
              </Radio>
              <Radio title="缩略图" label="thumb">
                <Icon type="ios-apps"></Icon>
              </Radio>
            </RadioGroup>
          </div>
        </div>
      </div>

      <div v-show="showType == 'list'">
        <Row >
          <Alert show-icon>
            已选择
            <span >{{ selectCount }}</span> 项
            <a class="select-clear" @click="clearSelectAll">清空</a>
            <span v-if="selectCount > 0" style="margin-left: 15px"
            >共计 {{ totalSize }} 存储量</span
            >
          </Alert>
        </Row>
        <Table
          :loading="loading"
          border
          :columns="columns"
          :data="data"
          ref="table"
          sortable="custom"
          @on-sort-change="changeSort"
          @on-selection-change="changeSelect"
        ></Table>
      </div>
      <div v-show="showType == 'thumb'">
        <div class="oss-wrapper">
          <Card v-for="(item, i) in data" :key="i" class="oss-card">
            <div class="content">
              <img
                @click="showPic(item)"
                v-if="item.fileType.indexOf('image') >= 0"
                class="img"
                :src="item.url"
              />
              <div
                v-else-if="item.fileType.indexOf('video') >= 0"
                class="video"
                @click="showVideo(item)"
              >
                <!-- 文件小于5MB显示video -->
                <video class="cover" v-if="item.fileSize < 1024 * 1024 * 5">
                  <source :src="item.url"/>
                </video>
                <img class="play" src="@/assets/play.png"/>
              </div>
              <div v-else class="other">
                <div class="name">{{ item.name }}</div>
                <div class="key">{{ item.fileKey }}</div>
                <div class="info">
                  文件类型：{{ item.fileType }} 文件大小：{{
                    ((item.fileSize * 1.0) / (1024 * 1024)).toFixed(2)
                  }}
                  MB 创建时间：{{ item.createTime }}
                </div>
              </div>
              <div class="actions">
                <div class="btn">
                  <Tooltip content="下载" placement="top">
                    <Icon @click="download(item)" type="md-download" size="16"/>
                  </Tooltip>
                </div>
                <div class="btn">
                  <Tooltip content="重命名" placement="top">
                    <Icon @click="rename(item)" type="md-create" size="16"/>
                  </Tooltip>
                </div>
                <div class="btn-no">
                  <Tooltip content="删除" placement="top">
                    <Icon @click="remove(item)" type="md-trash" size="16"/>
                  </Tooltip>
                </div>
              </div>
            </div>
          </Card>
        </div>
      </div>
      <Row type="flex" justify="end" class="mt_10">
        <Page
          :current="searchForm.pageNumber"
          :total="total"
          :page-size="searchForm.pageSize"
          @on-change="changePage"
          @on-page-size-change="changePageSize"
          :page-size-opts="pageSizeOpts"
          size="small"
          show-total
          show-elevator
          show-sizer
        ></Page>
      </Row>
    </Card>



    <Modal
      :title="modalTitle"
      v-model="modalVisible"
      :mask-closable="false"
      :width="500"
    >
      <Form ref="form" :model="form" :label-width="95" :rules="formValidate">
        <FormItem label="原文件名" prop="name">
          <Input v-model="form.name"/>
        </FormItem>
        <FormItem label="存储文件名" prop="fileKey">
          <Input v-model="form.fileKey" disabled/>
        </FormItem>
      </Form>
      <div slot="footer">
        <Button type="text" @click="handleCancel">取消</Button>
        <Button type="primary" :loading="submitLoading" @click="handleSubmit"
        >提交
        </Button
        >
      </div>
    </Modal>

    <Modal v-model="picVisible" :title="picTitle" draggable>
      <img
        :src="file.url"
        alt="无效的图片链接"
        style="width: 100%; margin: 0 auto; display: block"
      />
      <div slot="footer">
        <span
        >文件类型：{{ file.fileType }} 文件大小：{{ file.msize }} 创建时间：{{
            file.createTime
          }}</span
        >
      </div>
    </Modal>

    <Modal
      v-model="videoVisible"
      :title="videoTitle"
      :width="800"
      @on-cancel="closeVideo"
      draggable
    >
      <div id="dplayer"></div>
      <div slot="footer">
        <span
        >文件类型：{{ file.fileType }} 文件大小：{{ file.msize }} 创建时间：{{
            file.createTime
          }}</span
        >
      </div>
    </Modal>
  </div>
</template>

<script>
import {
  getFileListData,
  renameFile,
  deleteFile,
} from "@/api/index";
import DPlayer from "dplayer";

const config = require('@/config/index')
import { commonUrl } from "@/libs/axios";
var dp;
export default {
  name: "oss-manage",
  props:{
    isComponent:{
      default: false,
      type:Boolean
    }
  },
  data() {
    return {
      commonUrl, // 上传文件路径
      config, // api地址
      selectImage: false, //是否是选择
      accessToken: {}, // 上传token鉴权
      loading: false, // 表单加载状态
      fileType: "all", // 文件类型
      showType: "list", // 展示类型
      modalVisible: false, // 添加或编辑显示
      uploadVisible: false, // 上传展示
      videoVisible: false, // 文件modal
      picVisible: false, // 图片modal
      picTitle: "", // 图片title
      videoTitle: "", // 视频title
      modalTitle: "", // 添加或编辑标题
      searchForm: {
        // 搜索框对应data对象
        name: "",
        fileKey: "",
        fileType: "",
        pageNumber: 1, // 当前页数
        pageSize: 10, // 页面大小
        sort: "createTime", // 默认排序字段
        order: "desc", // 默认排序方式
        startDate: "", // 起始时间
        endDate: "", // 终止时间
      },
      selectDate: null, // 选择日期绑定modal
      oldKey: "", // 请求参数
      form: { // 表单
        name: "",
        fileKey: "",
      },
      file: {}, // 文件数据
      // 表单验证规则
      formValidate: {
        name: [{required: true, message: "不能为空", trigger: "blur"}],
        fileKey: [{required: true, message: "不能为空", trigger: "blur"}],
      },
      submitLoading: false, // 添加或编辑提交状态
      selectList: [], // 多选数据
      selectCount: 0, // 多选计数
      totalSize: "", // 文件大小统计
      columns: [
        // 表头
        {
          type: "selection",
          width: 60,
          align: "center",
        },
        {
          title: "原文件名",
          key: "name",
          minWidth: 130,
          sortable: true,
          ellipsis: false,
          tooltip: true,
        },
        {
          title: "存储文件名",
          key: "fileKey",
          width: 165,
          sortable: true,
          ellipsis: false,
          tooltip: true,
        },
        {
          title: "缩略图(点击预览)",
          key: "url",
          width: 150,
          align: "center",
          render: (h, params) => {
            if (params.row.fileType.includes("image") > 0) {
              return h("img", {
                attrs: {
                  src: params.row.url || '',
                  alt: "加载图片失败",
                },
                style: {
                  cursor: "pointer",
                  width: "80px",
                  height: "60px",
                  margin: "10px 0",
                  "object-fit": "contain",
                },
                on: {
                  click: () => {
                    this.showPic(params.row);
                  },
                },
              });
            } else if (params.row.fileType.includes("video") > 0) {
              // 如果视频文件大小超过5MB不予加载video
              if (params.row.fileSize < 1024 * 1024 * 5) {
                return h(
                  "video",
                  {
                    style: {
                      cursor: "pointer",
                      width: "80px",
                      height: "60px",
                      margin: "10px 0",
                      "object-fit": "contain",
                    },
                    on: {
                      click: () => {
                        this.showVideo(params.row);
                      },
                    },
                  },
                  [
                    h("source", {
                      attrs: {
                        src: params.row.url,
                      },
                    }),
                  ]
                );
              } else {
                return h("img", {
                  attrs: {
                    src: require("@/assets/play.png"),
                  },
                  style: {
                    cursor: "pointer",
                    width: "80px",
                    height: "60px",
                    margin: "10px 0",
                    "object-fit": "contain",
                  },
                  on: {
                    click: () => {
                      this.showVideo(params.row);
                    },
                  },
                });
              }
            } else {
              return h("span", "非多媒体类型");
            }
          },
        },
        {
          title: "文件类型",
          key: "fileType",
          width: 115,
          sortable: true,
          className: this.selectImage == true ? "none" : "",
        },
        {
          title: "文件大小",
          key: "fileSize",
          width: 115,
          sortable: true,
          className: this.selectImage == true ? "none" : "",
          render: (h, params) => {
            let m =
              ((params.row.fileSize * 1.0) / (1024 * 1024)).toFixed(2) + " MB";
            return h("span", m);
          },
        },
        {
          title: "上传者",
          key: "createBy",
          width: 120,
          sortable: true,
          render: (h, params) => {
            let m = "";
            if(params.row.userEnums=="MANAGER"){
            m="[管理员]"
            }else if(params.row.userEnums=="STORE"){
              m="[店铺]"
            }else{
              m="[会员]"
            }
            m += params.row.createBy
            return h("span", m);
          },
        },
        {
          title: "上传时间",
          key: "createTime",
          width: 180,
          sortable: true,
          sortType: "desc",
        },
        {
          title: "操作",
          key: "action",
          align: "center",
          fixed: "right",
          width: 200,
          render: (h, params) => {
            return h("div", [
              h(
                "Button",
                {
                  props: {
                    type: "primary",
                    size: "small",
                  },
                  style: {
                    marginRight: "5px",
                    display: this.selectImage == true ? "inline-block" : "none",
                  },
                  on: {
                    click: () => {
                      this.selectedParams(params.row);
                    },
                  },
                },
                "选择"
              ),
              h(
                "Button",
                {
                  props: {
                    type: "info",
                    size: "small",
                  },
                  style: {
                    marginRight: "5px",
                    display: this.selectImage == true ? "none" : "inline-block",
                  },
                  on: {
                    click: () => {
                      this.download(params.row);
                    },
                  },
                },
                "下载"
              ),
              h(
                "Button",
                {
                  props: {
                    size: "small",
                    type: 'success'
                  },
                  style: {
                    marginRight: "5px",
                    display: this.selectImage == true ? "none" : "inline-block",
                  },
                  on: {
                    click: () => {
                      this.rename(params.row);
                    },
                  },
                },
                "重命名"
              ),
              h(
                "Button",
                {
                  props: {
                    type: "error",
                    size: "small",
                  },
                  style: {
                    display: this.selectImage == true ? "none" : "inline-block",
                  },
                  on: {
                    click: () => {
                      this.remove(params.row);
                    },
                  },
                },
                "删除"
              ),
            ]);
          },
        },
      ],
      data: [], // 表单数据
      total: 0, // 表单数据总数
      pageSizeOpts: [5, 10, 20], // 页码展示项
    };
  },
  watch:{
    selectImage(val) {
      if (val && !this.data.length) this.init()
    }
  },
  methods: {
    /**
     * 选择
     */
    selectedParams(val) {
      this.$emit("callback", val);
    },
    // 更多操作
    handleDropdown(name) {
      if (name == "refresh") {
        this.getDataList();
      } else if (name == "removeAll") {
        this.removeAll();
      }
    },
    // 初始化数据
    init() {
      this.accessToken = {
        accessToken: this.getStore("accessToken"),
      };
      this.getDataList();
    },
    // 查看大图
    showPic(v) {
      this.file = v;
      this.file.msize = ((v.fileSize * 1.0) / (1024 * 1024)).toFixed(2) + " MB";

      this.picTitle = v.name + "(" + v.fileKey + ")";
      this.picVisible = true;
    },
    // 查看视频
    showVideo(v) {
      dp = new DPlayer({
        container: document.getElementById("dplayer"),
        screenshot: true,
        video: {
          url: v.url,
        },
      });
      this.file = v;
      this.file.msize = ((v.fileSize * 1.0) / (1024 * 1024)).toFixed(2) + " MB";
      this.videoTitle = v.name + "(" + v.fileKey + ")";
      this.videoVisible = true;
    },
    // 关闭视频
    closeVideo() {
      dp.destroy();
    },
    // 分页 改变页码
    changePage(v) {
      this.searchForm.pageNumber = v;
      this.getDataList();
      this.clearSelectAll();
    },
    // 分页 改变页数
    changePageSize(v) {
      this.searchForm.pageNumber = 1;
      this.searchForm.pageSize = v;
      this.getDataList();
    },
    // 排序
    changeSort(e) {
      this.searchForm.sort = e.key;
      this.searchForm.order = e.order;
      if (e.order == "normal") {
        this.searchForm.order = "";
      }
      this.getDataList();
    },
    // 起止时间从新赋值
    selectDateRange(v) {
      if (v) {
        this.searchForm.startDate = v[0];
        this.searchForm.endDate = v[1];
      }
    },
    // 改变查看方式
    changeShowType() {
      this.searchForm.pageNumber = 1;
      if (this.showType == "list") {
        this.searchForm.pageSize = 10;
      } else {
        this.searchForm.pageSize = 12;
      }
      this.getDataList();
    },
    // 获取列表数据
    getDataList() {
      if (this.showType == "list") {
        this.pageSizeOpts = [10, 20, 50];
      } else {
        this.pageSizeOpts = [12, 24, 48];
      }
      this.loading = true;
      getFileListData(this.searchForm).then((res) => {
        this.loading = false;

        this.data = res.result.records;
        this.total = res.result.total;
      });
    },
    // 搜索
    handleSearch() {
      this.searchForm.title = this.searchForm.name;
      this.searchForm.pageNumber = 1;
      if (this.showType == "list") {
        this.searchForm.pageSize = 5;
      } else {
        this.searchForm.pageSize = 12;
      }
      this.getDataList();
    },
    // 文件类型筛选
    changeFileType() {
      let name = this.fileType;
      if (name == "all") {
        this.searchForm.fileType = "";
      } else if (name == "pic") {
        this.searchForm.fileType = "image";
      } else if (name == "video") {
        this.searchForm.fileType = "video";
      }
      this.handleSearch();
    },
    // 上传文件超过大小限制
    handleMaxSize(file) {
      this.$Notice.warning({
        title: "文件大小过大",
        desc: "所选文件大小过大, 不得超过 1M.",
      });
    },
    // 上传成功回调
    handleSuccess(res, file) {
      if (res.success) {
        this.$Message.success("上传文件 " + file.name + " 成功");
        this.getDataList();
      } else {
        this.$Message.error(res.message);
      }
    },
    // 上传失败回调
    handleError(error, file, fileList) {
      this.$Message.error(error.toString());
    },
    // 清空上传文件
    clearFiles() {
      this.$refs.up.clearFiles();
    },
    // 取消修改文件名
    handleCancel() {
      this.modalVisible = false;
    },
    // 下载文件
    download(v) {
      window.open(
        v.url + "?attname=&response-content-type=application/octet-stream"
      );
    },
    // 批量删除文件
    removeAll() {
      if (this.selectCount <= 0) {
        this.$Message.warning("您还未选择要删除的数据");
        return;
      }
      this.$Modal.confirm({
        title: "确认删除",
        content: "您确认要删除所选的 " + this.selectCount + " 个文件?",
        loading: true,
        onOk: () => {
          let ids = "";
          this.selectList.forEach(function (e) {
            ids += e.id + ",";
          });
          ids = ids.substring(0, ids.length - 1);
          deleteFile(ids).then((res) => {
            this.$Modal.remove();
            if (res.success) {
              this.$Message.success("批量删除文件成功");
              this.clearSelectAll();
              this.getDataList();
            }
          });
        },
      });
    },
    // 单个删除文件
    remove(v) {
      this.$Modal.confirm({
        title: "确认删除",
        content: "您确认要删除文件 " + v.name + " ?",
        loading: true,
        onOk: () => {
          deleteFile(v.id).then((res) => {
            this.$Modal.remove();
            if (res.success) {
              this.$Message.success("删除文件 " + v.name + " 成功");
              this.getDataList();
            }
          });
        },
      });
    },
    // 提交修改文件名
    handleSubmit() {
      this.$refs.form.validate((valid) => {
        if (valid) {
          this.submitLoading = true;
          let params = {
            id: this.form.id,
            key: this.oldKey,
            newKey: this.form.fileKey,
            newName: this.form.name,
          };
          renameFile(params).then((res) => {
            this.submitLoading = false;
            if (res.success) {
              this.$Message.success("操作成功");
              this.getDataList();
              this.modalVisible = false;
            }
          });
        }
      });
    },
    // 修改文件名modal
    rename(v) {
      this.modalTitle = "编辑文件名";
      // 转换null为""
      for (let attr in v) {
        if (v[attr] == null) {
          v[attr] = "";
        }
      }
      let str = JSON.stringify(v);
      let data = JSON.parse(str);
      this.form = data;
      this.oldKey = data.fileKey;
      this.modalVisible = true;
    },
    // 清除选中状态
    clearSelectAll() {
      this.$refs.table.selectAll(false);
      this.totalSize = "";
    },
    // 选中回调
    changeSelect(e) {
      this.selectList = e;
      this.selectCount = e.length;
      let size = 0;
      e.forEach((item) => {
        size += item.fileSize * 1.0;
      });
      this.totalSize = ((size * 1.0) / (1024 * 1024)).toFixed(2) + " MB";
    },
  },
  mounted() {
    if(!this.isComponent) { // 是组件的话，初始化不调用接口
      this.init();
    }
  },

};
</script>
