<template>
  <div class="self-address">
    <Card>
      <Button @click="add" type="primary">添加</Button>
      <Table
        :loading="loading"
        border
        :columns="columns"
        :data="data"
        ref="table"
        style="margin-top:10px"
      ></Table>
      <Row type="flex" justify="end" style="margin-top:10px;">
        <Page
          :current="searchForm.pageNumber"
          :total="total"
          :page-size="searchForm.pageSize"
          @on-change="changePage"
          @on-page-size-change="changePageSize"
          :page-size-opts="[10, 20, 50]"
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
      <Form ref="form" :model="form" :label-width="100" :rules="formValidate">
        <FormItem label="名称" prop="addressName">
          <Input v-model="form.addressName" clearable style="width: 90%"/>
        </FormItem>
        <FormItem label="详细地址" prop="address">
          <Input v-model="form.address" @on-focus="$refs.liliMap.showMap = true" clearable style="width: 90%"/>
        </FormItem>
        <FormItem label="联系电话" prop="mobile">
          <Input v-model="form.mobile" clearable style="width: 90%" maxlength="11"/>
        </FormItem>
      </Form>
      <div slot="footer">
        <Button type="text" @click="modalVisible = false">取消</Button>
        <Button type="primary" :loading="submitLoading" @click="handleSubmit"
        >提交
        </Button
        >
      </div>
    </Modal>
    <liliMap ref="liliMap" @getAddress="getAddress"></liliMap>
  </div>
</template>

<script>
  import * as API_Shop from "@/api/shops";
  import { validateMobile } from "@/libs/validate";
  import liliMap from "@/views/my-components/map/index";


  export default {
    name: "shopAddress",
    components: {
      liliMap
    },
    data() {
      return {
        loading: true, // 表单加载状态
        modalType: 0, // 添加或编辑标识
        modalVisible: false, // 添加或编辑显示
        modalTitle: "", // 添加或编辑标题
        searchForm: {
          // 搜索框初始化对象
          pageNumber: 1, // 当前页数
          pageSize: 10, // 页面大小
        },
        form: {
          // 添加或编辑表单对象初始化数据
          addressName: "",
          center: "",
          address:"",//详细地址
          mobile:"",//手机号码
        },

        // 表单验证规则
        formValidate: {
          addressName: [
            {
              required: true,
              message: "请输入地址名称",
              trigger: "blur",
            },
          ],
          longitude: [
            {
              required: true,
              message: "请输入地址经度",
              trigger: "blur",
            },
          ],
          latitude: [
            {
              required: true,
              message: "请输入地址纬度",
              trigger: "blur",
            },
          ],
          mobile: [
            {
              required: true,
              message: "请输入联系电话号",
              trigger: "blur",
            },
            { validator: validateMobile,
              trigger: "blur"
            }
          ],
          address: [
            {
              required: true,
              message: " ",
              trigger: "blur",
            },
          ],
        },
        submitLoading: false, // 添加或编辑提交状态
        columns: [
          // 表头
          {
            title: "自提点名称",
            key: "addressName",
            minWidth: 120,
            sortable: false,
          },
          {
            title: "详细地址",
            key: "address",
            minWidth: 280
          },
          {
            title: "创建时间",
            key: "createTime",
            minWidth: 120,
            sortable: true,
          },
          {
            title: "操作",
            key: "action",
            align: "center",
            width: 200,
            render: (h, params) => {
              return h("div", [
                h(
                  "Button",
                  {
                    props: {
                      type: "success",
                      size: "small",
                    },
                    style: {
                      marginRight: "5px",
                    },
                    on: {
                      click: () => {
                        this.edit(params.row);
                      },
                    },
                  },
                  "修改"
                ),
                h(
                  "Button",
                  {
                    props: {
                      type: "error",
                      size: "small",
                    },
                    style: {
                      marginRight: "5px",
                    },
                    on: {
                      click: () => {
                        this.deleteSubmit(params.row);
                      },
                    },
                  },
                  "删除"
                )
              ]);
            },
          },
        ],
        data: [], // 表单数据
        total: 0, // 表单数据总数
      };
    },
    methods: {
      init() { // 初始化数据
        this.getDataList();
      },
      // 分页 改变页码
      changePage(v) {
        this.searchForm.pageNumber = v;
        this.getDataList();
      },
      // 分页 改变页数
      changePageSize(v) {
        this.searchForm.pageSize = v;
        this.getDataList();
      },
      // 搜索
      handleSearch() {
        this.searchForm.pageNumber = 1;
        this.searchForm.pageSize = 10;
        this.getDataList();
      },
      // 重置
      handleReset() {
        this.$refs.searchForm.resetFields();
        this.searchForm.pageNumber = 1;
        this.searchForm.pageSize = 10;
        this.getDataList();
      },
      //获取地址
      getAddress(item){
        this.$set(this.form, 'address', item.addr)
        this.form.address = item.address
        this.form.center = item.position.lat + "," + item.position.lng
      },
      // 获取数据
      getDataList() {
        this.loading = true;
        API_Shop.getShopAddress(this.searchForm).then((res) => {
          this.loading = false;
          if (res.success) {
            this.data = res.result.records;
            this.total = res.result.total;
          }
        });
        this.total = this.data.length;
        this.loading = false;
      },
      //添加弹出框
      add() {
        this.$refs.form.resetFields()
        this.modalVisible = true
        this.modalTitle = "添加自提地址"
      },
      //修改弹出框
      edit(v) {
        this.modalType = 1
        this.modalVisible = true
        this.modalTitle = "修改自提地址"
        this.form.id = v.id
        this.form.address = v.address
        this.form.addressName = v.addressName
        this.form.mobile = v.mobile
        this.form.center = v.center
        this.form.longitude = v.center.split(',')[0]
        this.form.latitude = v.center.split(',')[1]
      },

      //保存或者编辑
      handleSubmit() {
        this.$refs.form.validate((valid) => {
          if (valid) {
            this.submitLoading = true;
            if (this.modalType == 0) {
              // 添加
              delete this.form.id;
              API_Shop.addShopAddress(this.form).then((res) => {
                this.submitLoading = false;
                if (res.success) {
                  this.$Message.success("添加成功");
                  this.getDataList();
                  this.modalVisible = false;
                }
              });
            } else {
              // 编辑
              API_Shop.editShopAddress(this.form.id, this.form).then((res) => {
                this.submitLoading = false;
                if (res.success) {
                  this.$Message.success("修改成功");
                  this.getDataList();
                  this.modalVisible = false;
                }
              });
            }
          }
        });
      },
      //删除
      deleteSubmit(v){
        this.$Modal.confirm({
          title: "确认删除",
          // 记得确认修改此处
          content: "确认删除自提地址么？",
          loading: true,
          onOk: () => {
            API_Shop.deleteShopAddress(v.id).then((res) => {
              this.$Modal.remove();
              if (res.success) {
                this.$Message.success("此自自提地址已删除");
                this.init();
              }
            });
          }
        });
      }
    },
    mounted() {
      this.init();
    },
  };
</script>

