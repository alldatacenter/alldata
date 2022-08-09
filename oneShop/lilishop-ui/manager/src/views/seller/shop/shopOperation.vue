<template>
  <div>
    <Card>
      <Button class="mb_10" v-if="shopForm.storeDisable === 'APPLYING'" type="primary" @click="auditHandler">审核</Button>
      <Tabs v-model="tabName" :animated="false" style="overflow: visible">
        <Form
          ref="shopForm"
          :model="shopForm"
          :label-width="130"
          label-position="right"
          :rules="shopValidate"
          @on-validate="formValidate"
        >
          <TabPane label="基本信息" class="tab" name="base">
            <Divider orientation="left">基本信息</Divider>
            <!-- 遮罩层  -->
            <div v-if="isRead" class="mask">只读不可修改</div>
            <div>
              <FormItem label="会员名称" prop="memberName">
                <div class="item">
                  <Input disabled v-model="shopForm.memberName" />
                  <Button @click="selectMember()" v-if="!$route.query.shopId"
                    >选择会员</Button
                  >
                </div>
              </FormItem>
              <FormItem label="店铺名称" prop="storeName">
                <Input v-model="shopForm.storeName" clearable style="width: 350px" />
              </FormItem>

              <FormItem label="是否自营" prop="selfOperated">
                <RadioGroup
                  type="button"
                  button-style="solid"
                  v-model="shopForm.selfOperated"
                >
                  <Radio label="true">自营</Radio>
                  <Radio label="false">非自营</Radio>
                </RadioGroup>
              </FormItem>

              <FormItem label="店铺定位" prop="shopCenter">
                <!-- <Input v-model="shopForm.storeCenter" @on-focus="$refs.liliMap.showMap = true" clearable style="width: 350px" /> -->
                <Button
                  type="info"
                  v-if="!shopForm.storeCenter"
                  @click="$refs.liliMap.showMap = true"
                  >点击获取店铺定位</Button
                >
                <Button type="success" v-else @click="$refs.liliMap.showMap = true"
                  >已定位</Button
                >
              </FormItem>

              <FormItem label="店铺所在地" prop="storeAddressPath">
                <Input
                  disabled
                  v-model="shopForm.storeAddressPath"
                  style="width: 350px"
                />
              </FormItem>
              <FormItem label="店铺详细地址" prop="storeAddressDetail">
                <Input
                  v-model="shopForm.storeAddressDetail"
                  clearable
                  style="width: 350px"
                />
              </FormItem>

              <FormItem label="店铺logo" class="storeLogo">
                <Avatar
                  style="height: 100px; width: 100px"
                  v-if="shopForm.storeLogo"
                  shape="square"
                  icon="ios-person"
                  size="default"
                  :src="shopForm.storeLogo"
                />
                <div>
                  <Button @click="handleCLickImg('storeLogo')" type="primary"
                    >选择图片</Button
                  >
                </div>
              </FormItem>

              <FormItem label="店铺简介" prop="storeDesc" style="width: 350px">
                <Input
                  v-model="shopForm.storeDesc"
                  type="textarea"
                  :rows="4"
                  maxlength="200"
                  show-word-limit
                  clearable
                  style="width: 400px"
                />
              </FormItem>
              <br />
              <Divider orientation="left">退货收件地址</Divider>
              <FormItem label="收件人姓名">
                <Input
                  v-model="shopForm.salesConsigneeName"
                  clearable
                  style="width: 350px"
                />
              </FormItem>
              <FormItem label="收件人手机">
                <Input
                  v-model="shopForm.salesConsigneeMobile"
                  clearable
                  maxlength="11"
                  style="width: 350px"
                />
              </FormItem>
              <FormItem label="地址信息">
                <region
                  style="width: 350px"
                  @selected="selectedConsigneeRegion"
                  :addressId="returnAddress"
                />
              </FormItem>
              <FormItem label="详细地址">
                <Input
                  v-model="shopForm.salesConsigneeDetail"
                  clearable
                  style="width: 350px"
                />
              </FormItem>
              <!-- <Divider orientation="left">腾讯云智服</Divider>
              <FormItem label="唯一标识">
                <Input v-model="shopForm.yzfSign" clearable style="width: 350px" />
              </FormItem>
              <FormItem label="小程序唯一标识">
                <Input v-model="shopForm.yzfMpSign" clearable style="width: 350px" />
              </FormItem>
              <Spin fix v-if="loading"></Spin> -->
            </div>
          </TabPane>

          <!-- 入驻信息 -->
          <TabPane label="入驻信息" class="tab" name="entry">
            <!-- 遮罩层  -->
            <div v-if="isRead" class="mask">只读不可修改</div>
            <Divider orientation="left">公司信息</Divider>
            <div>
              <FormItem label="公司名称" prop="companyName">
                <Input v-model="shopForm.companyName" clearable style="width: 350px" />
              </FormItem>

              <FormItem label="公司电话" prop="companyPhone">
                <Input v-model="shopForm.companyPhone" clearable style="width: 350px" />
              </FormItem>
              <FormItem label="公司所在地" prop="companyAddressIdPath">
                <region
                  style="width: 350px"
                  @selected="selectedRegion"
                  :addressId="address"
                />
              </FormItem>
              <FormItem label="公司详细地址" prop="companyAddress">
                <Input v-model="shopForm.companyAddress" clearable style="width: 350px" />
              </FormItem>
              <FormItem label="员工总数" prop="employeeNum">
                <InputNumber
                  style="width: 150px"
                  :min="1"
                  :max="9999999"
                  v-model="shopForm.employeeNum"
                >
                </InputNumber>
              </FormItem>
              <FormItem label="注册资金" prop="registeredCapital">
                <InputNumber
                  style="width: 150px"
                  :min="1"
                  :max="9999999"
                  v-model="shopForm.registeredCapital"
                >
                </InputNumber>
                <span style="margin-left: 10px">万</span>
              </FormItem>
              <FormItem label="联系人姓名" prop="linkName">
                <Input v-model="shopForm.linkName" clearable style="width: 200px" />
              </FormItem>
              <FormItem label="联系人手机" prop="linkPhone">
                <Input
                  v-model="shopForm.linkPhone"
                  maxlength="11"
                  clearable
                  style="width: 200px"
                />
              </FormItem>
              <FormItem label="电子邮箱" prop="companyEmail">
                <Input v-model="shopForm.companyEmail" clearable style="width: 200px" />
              </FormItem>

              <Divider orientation="left">营业执照信息</Divider>

              <FormItem label="营业执照号" prop="licenseNum">
                <Input v-model="shopForm.licenseNum" clearable style="width: 200px" />
              </FormItem>

              <FormItem label="法定经营范围" prop="scope">
                <Input v-model="shopForm.scope" clearable style="width: 200px" />
              </FormItem>

              <Divider orientation="left">法人信息</Divider>

              <FormItem label="法人姓名" prop="legalName">
                <Input v-model="shopForm.legalName" clearable style="width: 200px" />
              </FormItem>
              <FormItem label="法人证件号" prop="legalId">
                <Input v-model="shopForm.legalId" clearable style="width: 200px" />
              </FormItem>
              <FormItem label="法人身份证照片" prop="legalPhoto">
                <Avatar
                  class="legal-photo"
                  shape="square"
                  size="100"
                  icon="md-add"
                  @click.native="handleCLickImg('legalPhoto', 0)"
                  :src="shopForm.legalPhoto[0]"
                />
                <Avatar
                  class="ml_10 legal-photo"
                  shape="square"
                  size="100"
                  icon="md-add"
                  @click.native="handleCLickImg('legalPhoto', 1)"
                  :src="shopForm.legalPhoto[1]"
                />
                <span>点击图片上传身份证正反面，要求身份证清晰，四角无缺漏</span>
              </FormItem>

              <Divider orientation="left">结算银行信息</Divider>
              <FormItem label="银行开户名" prop="settlementBankAccountName">
                <Input
                  v-model="shopForm.settlementBankAccountName"
                  clearable
                  style="width: 200px"
                />
              </FormItem>
              <FormItem label="银行账号" prop="settlementBankAccountNum">
                <Input
                  v-model="shopForm.settlementBankAccountNum"
                  clearable
                  style="width: 200px"
                />
              </FormItem>
              <FormItem label="银行支行名称" prop="settlementBankBranchName">
                <Input
                  v-model="shopForm.settlementBankBranchName"
                  clearable
                  style="width: 200px"
                />
              </FormItem>
              <FormItem label="支行联行号" prop="settlementBankJointName">
                <Input
                  v-model="shopForm.settlementBankJointName"
                  clearable
                  style="width: 200px"
                />
              </FormItem>

              <FormItem label="许可证电子版">
                <Avatar
                  style="height: 100px; width: 100px"
                  v-if="shopForm.licencePhoto"
                  shape="square"
                  icon="ios-person"
                  size="default"
                  :src="shopForm.licencePhoto"
                />
                <div>
                  <Button @click="handleCLickImg('licencePhoto')" type="primary"
                    >选择图片</Button
                  >
                </div>
              </FormItem>
              <Spin fix v-if="loading"></Spin>
            </div>
          </TabPane>
          <TabPane label="经营范围" class="tab" name="category">
            <!-- 遮罩层  -->
            <div v-if="isRead" class="mask">只读不可修改</div>
            <FormItem label="经营类目" prop="goodsManagementCategory">
              <div>
                <Checkbox
                  :indeterminate="indeterminate"
                  :value="checkAll"
                  @click.prevent.native="handleCheckAll"
                  >全选
                </Checkbox>
              </div>
              <CheckboxGroup v-model="checkAllGroup" @on-change="checkAllGroupChange">
                <Checkbox v-for="(item, i) in categories" :key="i + 1" :label="item.id"
                  >{{ item.name }}
                </Checkbox>
              </CheckboxGroup>
            </FormItem>
          </TabPane>

          <!-- <TabPane label="配送信息" class="tab" name="send"> -->
            <!-- 遮罩层  -->
            <!-- <FormItem label="达达编码" prop="ddCode">
              <Input
                v-model="shopForm.ddCode"
                maxlength="20"
                clearable
                style="width: 200px"
              />
            </FormItem> -->
          <!-- </TabPane> -->

          <TabPane label="结算信息" class="tab" name="settlement">
            <Alert type="error"
              >已添加<span class="theme_color">{{ settlementCycle.length }}</span
              >个结算日，最多可添加5个结算日，当月不包含所设日期时，将会顺延到下一个结算日</Alert
            >
            <FormItem label="结算日期">
              <Tag
                v-for="item in settlementCycle"
                :key="item"
                :name="item"
                closable
                style="marrgin-left: 10px"
                @on-close="removesettlementCycle"
                >{{ item }}
              </Tag>
              <InputNumber
                size="small"
                :max="31"
                :min="1"
                v-model="day"
                v-show="settlementShow"
              ></InputNumber>
              <Button
                type="default"
                @click="addsettlementCycle"
                size="small"
                v-if="addSettlementBtn && settlementCycle.length < 5"
                style="margin-left: 8px"
                >添加结算日期
              </Button>
              <Button
                v-if="addSettlementConfirmBtn"
                type="default"
                @click="addsettlementCycleConfirm"
                size="small"
                style="margin-left: 8px"
                >确认
              </Button>
            </FormItem>
          </TabPane>
        </Form>
      </Tabs>
      <div align="center">
        <Button
          type="info"
          v-show="tabNameList.indexOf(tabName) > 0"
          class="mr_10"
          @click="prev"
          >上一步</Button
        >
        <Button type="primary" v-show="tabNameList.indexOf(tabName) < 4" @click="next"
          >下一步</Button
        >
        <Button
          type="primary"
          v-show="tabNameList.indexOf(tabName) === 4"
          @click="save"
          v-if="!isRead"
        >
          {{ shopId ? "修改" : "保存" }}
        </Button>
      </div>
    </Card>
    <liliMap ref="liliMap" @getAddress="getAddress"></liliMap>

    <Modal width="1200px" v-model="picModalFlag">
      <ossManage @callback="callbackSelected" ref="ossManage" />
    </Modal>

    <Modal width="1200px" v-model="memberModalFlag">
      <memberLayout
        @callback="callbackMember"
        class="selectedMember"
        ref="memberLayout"
      />
    </Modal>

    <Modal v-model="auditModel" width="360">
      <p slot="header" style="color: #f60; text-align: center">
        <Icon type="ios-information-circle"></Icon>
        <span>审核店铺</span>
      </p>
      <div style="text-align: center">
        <p>您确认要审核通过该店铺</p>
      </div>
      <div slot="footer">
        <Button type="error" :loading="auditModalLoading" @click="audit('REFUSED')"
          >驳回</Button
        >
        <Button type="info" :loading="auditModalLoading" @click="audit('PASS')"
          >通过</Button
        >
      </div>
    </Modal>
  </div>
</template>

<script>
import memberLayout from "@/views/member/list/index";
import ossManage from "@/views/sys/oss-manage/ossManage";
import { getCategoryTree } from "@/api/goods";
import { shopDetail, shopAdd, shopEdit, getShopByMemberId, shopAudit } from "@/api/shops";
import uploadPicInput from "@/views/my-components/lili/upload-pic-input";
import region from "@/views/lili-components/region";
import liliMap from "@/views/my-components/map/index";

export default {
  name: "shop-operation",
  components: {
    uploadPicInput,
    ossManage,
    region,
    memberLayout,
    liliMap,
  },

  data() {
    return {
      shopId: this.$route.query.shopId, // 店铺id
      isRead: false, // 是否只读，只有在店铺通过审核才可修改
      selectedFormBtnName: "", // 点击图片绑定form
      loading: false,
      auditModel: false,
      auditModalLoading: false,
      picModalFlag: false, // 图片选择器
      address: "", // 地址
      returnAddress: "", // 退货地址
      memberModalFlag: false, // 商家账号
      settlementShow: false, // 是否展示结算日输入框
      addSettlementConfirmBtn: false, // 添加结算日确认按钮
      addSettlementBtn: true, // 添加结算日按钮
      day: 1, //结算日
      tabName: "base", // tab栏name值
      tabNameList: ["base", "entry", "category", "send", "settlement"], // tab栏name值数组
      shopValidate: {
        // 表单验证规则
        memberName: [{ required: true, message: "会员不能为空" }],
        storeName: [{ required: true, message: "店铺名称不能为空" }],
        companyAddress: [{ required: true, message: "公司地址不能为空" }],
        storeAddressDetail: [{ required: true, message: "店铺详细地址不能为空" }],
        storeDesc: [{ required: true, message: "店铺简介不能为空" }],
        storeCenter: [{ required: true, message: "店铺未定位" }],
        companyName: [{ required: true, message: "公司名称不能为空" }],
        companyPhone: [{ required: true, message: "公司电话不能为空" }],
        employeeNum: [
          {
            required: true,
            type: "number",
            message: "员工总数不能为空",
            trigger: "blur",
          },
        ],
        companyAddressIdPath: [{ required: true, message: "请选择公司地址" }],
        registeredCapital: [
          {
            required: true,
            type: "number",
            message: "注册资金不能为空",
            trigger: "blur",
          },
        ],
        linkName: [{ required: true, message: "联系人姓名不能为空" }],
        linkPhone: [
          { required: true, message: "联系人手机号不能为空" },
          {
            type: "string",
            pattern: /^1[3|4|5|6|7|8][0-9]{9}$/,
            message: "手机号格式出错",
            trigger: "blur",
          },
        ],
        companyEmail: [
          { required: true, message: "邮箱不能为空" },
          { type: "email", message: "邮箱格式错误" },
        ],
        licenseNum: [{ required: true, message: "营业执照号不能为空" }],
        scope: [{ required: true, message: "法定经营范围不能为空" }],
        legalName: [{ required: true, message: "法人姓名不能为空" }],
        legalId: [{ required: true, message: "法人证件号不能为空" }],
        settlementBankAccountName: [{ required: true, message: "银行开户名不能为空" }],
        settlementBankAccountNum: [{ required: true, message: "银行账号不能为空" }],
        settlementBankBranchName: [{ required: true, message: "银行支行名称不能为空" }],
        settlementBankJointName: [{ required: true, message: "支行联行号不能为空" }],

        salesConsigneeMobile: [
          {
            type: "string",
            pattern: /^1[3|4|5|6|7|8][0-9]{9}$/,
            message: "手机号格式出错",
            trigger: "blur",
          },
        ],
      },
      indeterminate: true, // 复选框全选样式
      checkAll: false, // 全选
      checkAllGroup: [], // 全选数组
      submitLoading: false, // 添加或编辑提交状态
      settlementCycle: [], // 结算周期
      shopForm: {
        // 店铺数据
        settlementCycle: "",
        selfOperated: "false",
        memberName: "",
        companyName: "",
        addressPath: "",
        addressIdPath: "",
        companyAddressPath: "",
        companyAddressIdPath: "",
        companyAddress: "",
        companyEmail: "",
        employeeNum: 1,
        registeredCapital: 1,
        linkName: "",
        linkPhone: "",
        licenseNum: "",
        scope: "",
        licencePhoto: "",
        legalName: "",
        legalId: "",
        legalPhoto: ["", ""],
        companyPhone: "",
        settlementBankAccountName: "",
        settlementBankAccountNum: "",
        settlementBankBranchName: "",
        settlementBankJointName: "",
        businesses: "",
        storeName: "",
        storeLogo: "",
        storeDesc: "",
        ddCode: "",
      },
      categories: [], // 分类

      infoResult: {}, // 店铺详情
      picIndex: "", // 存储身份证图片下标，方便赋值
    };
  },
  methods: {
    // 选择会员的回调
    callbackMember(val) {
      if (val) {
        //选择会员后需要检验此会员是否开过店铺
        getShopByMemberId(val.id).then((res) => {
          if (res.success) {
            if (res.result != null) {
              this.$Message.error("当前会员已经拥有店铺");
            } else {
              this.shopForm.memberId = val.id;
              this.shopForm.memberName = val.username;
            }
            this.memberModalFlag = false;
          }
        });
      }
    },

    //选择会员
    selectMember() {
      this.$refs["memberLayout"].selectedMember = true;
      this.memberModalFlag = true;
    },

    //修改地址
    regionClick() {
      this.showRegion = true;
      this.regionId = "";
    },
    //删除所选择的结算日
    removesettlementCycle(event, name) {
      this.settlementCycle = this.settlementCycle.filter((i) => i !== name);
    },
    //确认添加方法
    addsettlementCycle() {
      this.settlementShow = true;
      this.addSettlementConfirmBtn = true;
      this.addSettlementBtn = false;
    },
    //添加结算日
    addsettlementCycleConfirm() {
      if (!this.day) {
        this.$Message.warning("请输入正确的结算周期，1-31的整数");
        return;
      }
      if (this.settlementCycle.includes(this.day)) {
        this.$Message.warning("已有该结算周期，不能重复输入");
        return;
      }
      this.settlementCycle.push(this.day);
      this.addSettlementConfirmBtn = false;
      this.addSettlementBtn = true;
      this.settlementShow = false;
      this.day = 1;
    },
    // 选择公司地址
    selectedRegion(val) {
      this.$set(this.shopForm, "companyAddressIdPath", val[0].toString());
      this.$set(
        this.shopForm,
        "companyAddressPath",
        val[1].toString().replace(/\s/g, "")
      );
    },
    // 选择退货收件地址
    selectedConsigneeRegion(val) {
      this.$set(this.shopForm, "salesConsigneeAddressId", val[0].toString());
      this.$set(
        this.shopForm,
        "salesConsigneeAddressPath",
        val[1].toString().replace(/\s/g, "")
      );
    },
    // 选择图片modal
    handleCLickImg(val, index) {
      this.$refs.ossManage.selectImage = true;
      this.picModalFlag = true;
      this.selectedFormBtnName = val;
      this.picIndex = index;
    },
    // 图片回显
    callbackSelected(val) {
      this.picModalFlag = false;
      if (this.picIndex === 0 || this.picIndex === 1) {
        this.shopForm[this.selectedFormBtnName][this.picIndex] = val.url;
      } else {
        this.shopForm[this.selectedFormBtnName] = val.url;
      }
      this.picIndex = "";
    },
    // 初始化数据
    init() {
      this.getCategories();
      if (this.shopId) {
        this.getShopDetail();
      }
    },
    next() {
      // 下一步
      let index = this.tabNameList.indexOf(this.tabName) + 1;
      this.tabName = this.tabNameList[index];
    },
    prev() {
      // 上一步
      let index = this.tabNameList.indexOf(this.tabName) - 1;
      this.tabName = this.tabNameList[index];
    },
    formValidate(prop, status, error) {},
    // 获取店铺详情
    getShopDetail() {
      shopDetail(this.shopId).then((res) => {
        if (res.success) {
          this.infoResult = res.result;
          this.shopForm = res.result;
          this.shopForm.selfOperated
            ? (this.shopForm.selfOperated = "true")
            : (this.shopForm.selfOperated = "false");

          this.checkAllGroup = this.shopForm.goodsManagementCategory.split(",");
          if (this.shopForm.settlementCycle) {
            this.settlementCycle = this.shopForm.settlementCycle.split(",");
          }
          this.shopForm.legalPhoto = this.shopForm.legalPhoto.split(",");

          this.address = this.shopForm.companyAddressIdPath;
          this.returnAddress = this.shopForm.salesConsigneeAddressId;
        }
      });
    },
    // 保存
    save() {
      this.$refs.shopForm.validate((valid) => {
        //校验结算日是否已经确认完成
        if (this.settlementShow) {
          this.$Message.error("请确认当前所填结算日信息");
          return;
        }
        //校验经营类目
        if (this.checkAllGroup == "") {
          this.$Message.error("请选择店铺经营类目");
          this.tabName = "cagetory";
          return;
        }
        if (valid) {
          const params = JSON.parse(JSON.stringify(this.shopForm));
          //处理经营类目，结算日
          params.goodsManagementCategory = this.checkAllGroup;
          params.settlementCycle = this.settlementCycle;
          if (this.shopId) {
            delete params.memberId;
            shopEdit(this.shopId, params).then((res) => {
              if (res.success) {
                this.$Message.success("编辑成功");
                this.$router.push({ name: "shopList" });
              }
            });
          } else {
            //添加店铺单独需要检验的参数
            if (params.memberName == "") {
              this.$Message.error("请选择开店的会员");
              return;
            }
            shopAdd(params).then((resp) => {
              if (resp.success) {
                this.$Message.success("添加成功");
                this.shopForm = {};
                this.$router.push({ name: "shopList" });
              }
            });
          }
        }
      });
    },
    // 点击定位获取店铺地址
    getAddress(item) {
      this.shopForm.storeCenter = item.position.lng + "," + item.position.lat;
      this.$set(this.shopForm, "storeAddressPath", item.addr);
      this.$set(this.shopForm, "storeAddressIdPath", item.addrId);
    },
    // 全部选中
    handleCheckAll() {
      if (this.indeterminate) {
        this.checkAll = false;
      } else {
        this.checkAll = !this.checkAll;
      }
      this.indeterminate = false;

      if (this.checkAll) {
        let checkAllDate = [];
        this.categories.forEach((i) => checkAllDate.push(i.id));
        this.checkAllGroup = checkAllDate;
      } else {
        this.checkAllGroup = [];
      }
    },
    // 经营类目的选择
    checkAllGroupChange(data) {
      if (data.length === this.categories.length) {
        this.indeterminate = false;
        this.checkAll = true;
      } else if (data.length > 0) {
        this.indeterminate = true;
        this.checkAll = false;
      } else {
        this.indeterminate = false;
        this.checkAll = false;
      }
    },
    // 获取所有分类
    getCategories() {
      getCategoryTree().then((res) => {
        if (res.success) {
          this.categories = res.result;
        }
      });
    },
    auditHandler() {
      this.auditModel = true;
    },
    // 审核店铺
    audit(operation) {
      let id = this.$route.query.shopId;
      if (operation === "PASS") {
        shopAudit(id, 0).then((res) => {
          this.auditModel = false;
          if (res.success) {
            this.$Message.success("操作成功");
            this.$router.push({ name: "shopAuth" });
          }
        });
      } else {
        shopAudit(id, 1).then((res) => {
          this.auditModel = false;
          if (res.success) {
            this.$Message.success("操作成功");
            this.$router.push({ name: "shopAuth" });
          }
        });
      }
    },
  },
  created() {
    this.init();
  },
};
</script>
<style lang="scss" scoped>
.selectedMember {
  width: 100%;
}

.mask {
  display: flex;
  align-items: center;
  justify-content: center;
  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: 100%;
  background: rgba(105, 105, 105, 0.1);
  z-index: 9;
}

/deep/ .ivu-tabs-bar {
  margin: 0;
}

.tab {
  padding: 16px;
  position: relative;
}

.categories-checkbox {
  display: flex;
  align-items: center;
}

.img {
  margin-right: 10px;
  width: 100px;
  height: 100px;
}

.item {
  width: 350px !important;
  display: flex;

  > * {
    margin: 0 4px;
  }
}
.legal-photo {
  width: 100px;
  height: 100px;
  cursor: pointer;
}
</style>
