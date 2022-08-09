<template>
  <div>
    <Card class="change-pass">
      <p slot="title">修改密码</p>
      <div class="mt_10">
        <Form ref="editPasswordForm" :model="editPasswordForm" :label-width="100" label-position="right" :rules="passwordValidate" style="width:450px">
          <FormItem label="原密码" prop="oldPass">
            <Input type="password" v-model="editPasswordForm.oldPassword" placeholder="请输入现在使用的密码"></Input>
          </FormItem>
          <FormItem label="新密码" prop="newPass">
            <SetPassword  v-model="editPasswordForm.newPassword" @on-change="changeInputPass" />
          </FormItem>
          <FormItem label="确认新密码" prop="rePass">
            <Input type="password" v-model="editPasswordForm.rePassword" placeholder="请再次输入新密码"></Input>
          </FormItem>
          <FormItem>
            <Button type="primary" class="mr_10" :loading="savePassLoading" @click="saveEditPass">保存
            </Button>
            <Button @click="cancelEditPass">取消</Button>
          </FormItem>
        </Form>
      </div>
    </Card>
  </div>
</template>

<script>
import SetPassword from "@/views/my-components/lili/set-password";
import { changePass } from "@/api/index";

export default {
  name: "change-password",
  components: {
    SetPassword
  },
  data() {
    const valideRePassword = (rule, value, callback) => {
      if (value !== this.editPasswordForm.newPassword) {
        callback(new Error("两次输入密码不一致"));
      } else {
        callback();
      }
    };
    return {
      savePassLoading: false, // 保存加载状态
      editPasswordForm: { // 编辑密码表单
        oldPassword: "",
        newPassword: "",
        rePassword: "",
      },
      strength: "", // 密码强度
      passwordValidate: { // 验证规则
        oldPassword: [
          {
            required: true,
            message: "请输入原密码",
            trigger: "blur",
          },
        ],
        newPassword: [
          {
            required: true,
            message: "请输入新密码",
            trigger: "blur",
          },
          {
            min: 6,
            message: "请至少输入6个字符",
            trigger: "blur",
          },
          {
            max: 32,
            message: "最多输入32个字符",
            trigger: "blur",
          },
        ],
        rePassword: [
          {
            required: true,
            message: "请再次输入新密码",
            trigger: "blur",
          },
          {
            validator: valideRePassword,
            trigger: "blur",
          },
        ],
      },
    };
  },
  methods: {
    // 密码强度
    changeInputPass(v, grade, strength) {
      this.strength = strength;
    },
    // 确认修改密码
    saveEditPass() {
      let params = {
        password: this.md5(this.editPasswordForm.oldPassword),
        newPassword: this.md5(this.editPasswordForm.newPassword),
        passStrength: this.md5(this.strength),
      };
      this.$refs["editPasswordForm"].validate((valid) => {
        if (valid) {
          this.savePassLoading = true;
          changePass(params).then((res) => {
            this.savePassLoading = false;
            if (res.success) {
              this.$Modal.success({
                title: "修改密码成功",
                content: "修改密码成功，需重新登录",
                onOk: () => {
                  this.$store.commit("logout", this);
                  this.$router.push({
                    name: "login",
                  });
                },
              });
            }
          });
        }
      });
    },
    // 取消修改密码
    cancelEditPass() {
      this.$store.commit("removeTag", "change_password");
      localStorage.pageOpenedList = JSON.stringify(
        this.$store.state.app.pageOpenedList
      );
      let lastPageName = "";
      let length = this.$store.state.app.pageOpenedList.length;
      if (length > 1) {
        lastPageName = this.$store.state.app.pageOpenedList[length - 1].name;
      } else {
        lastPageName = this.$store.state.app.pageOpenedList[0].name;
      }
      this.$router.push({
        name: lastPageName,
      });
    },
  }
};
</script>
