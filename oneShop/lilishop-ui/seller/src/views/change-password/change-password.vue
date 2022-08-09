<template>
  <div>
    <Card class="change-pass">
      <p slot="title"><Icon type="key"></Icon>修改密码</p>
      <div>
        <Form
          ref="editPasswordForm"
          :model="editPasswordForm"
          :label-width="100"
          label-position="right"
          :rules="passwordValidate"
          style="width:450px"
        >
          <FormItem label="原密码" prop="oldPass">
            <Input type="password" v-model="editPasswordForm.oldPass" placeholder="请输入现在使用的密码"></Input>
          </FormItem>
          <FormItem label="新密码" prop="newPassword">
            <SetPassword style="width:350px;" v-model="editPasswordForm.newPassword" @on-change="changeInputPass" />
          </FormItem>
          <FormItem label="确认新密码" prop="rePass">
            <Input type="password" v-model="editPasswordForm.rePass" placeholder="请再次输入新密码"></Input>
          </FormItem>
          <FormItem>
            <Button
              type="primary"
              style="width: 100px;margin-right:5px"
              :loading="savePassLoading"
              @click="editPassword"
            >保存</Button>
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
  name: "change_pass",
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
      savePassLoading: false, // 保存loading
      editPasswordForm: { // 修改密码表单 
        oldPass: "", // 旧密码
        newPassword: "", // 新密码
        rePass: "" // 从新输入新密码
      },
      strength: "", // 密码强度
      // 验证规则
      passwordValidate: {
        oldPass: [
          {
            required: true,
            message: "请输入原密码",
            trigger: "blur"
          }
        ],
        newPassword: [
          {
            required: true,
            message: "请输入新密码",
            trigger: "blur"
          },
          {
            min: 6,
            message: "请至少输入6个字符",
            trigger: "blur"
          },
          {
            max: 32,
            message: "最多输入32个字符",
            trigger: "blur"
          }
        ],
        rePass: [
          {
            required: true,
            message: "请再次输入新密码",
            trigger: "blur"
          },
          {
            validator: valideRePassword,
            trigger: "blur"
          }
        ]
      }
    };
  },
  methods: {
    // 新密码回调
    changeInputPass(v, grade, strength) {
      this.strength = strength;
    },
    // 修改密码
    editPassword() {
      let params = {
        password: this.md5(this.editPasswordForm.oldPass),
        newPassword: this.md5(this.editPasswordForm.newPassword)
      };
      this.$refs["editPasswordForm"].validate(valid => {
        if (valid) {
          this.savePassLoading = true;
          changePass(params).then(res => {
            this.savePassLoading = false;
            if (res.success) {
              this.$Modal.success({
                title: "修改密码成功",
                content: "修改密码成功，需重新登录",
                onOk: () => {
                  this.$store.commit("logout", this);
                  this.$store.commit("clearOpenedSubmenu");
                  this.$router.push({
                    name: "login"
                  });
                }
              });
            }
          });
        }
      });
    },
    // 取消修改密码
    cancelEditPass() {
      this.$store.commit("removeTag", "change_pass");
      localStorage.storeOpenedList = JSON.stringify(
        this.$store.state.app.storeOpenedList
      );
      let lastPageName = "";
      let length = this.$store.state.app.storeOpenedList.length;
      if (length > 1) {
        lastPageName = this.$store.state.app.storeOpenedList[length - 1].name;
      } else {
        lastPageName = this.$store.state.app.storeOpenedList[0].name;
      }
      this.$router.push({
        name: lastPageName
      });
    }
  }
};
</script>
<style lang="scss" scoped>
.change-pass {
  &-btn-box {
    margin-bottom: 10px;

    button {
      padding-left: 0;

      span {
        color: #2d8cf0;
        transition: all 0.2s;
      }

      span:hover {
        color: #0c25f1;
        transition: all 0.2s;
      }
    }
  }
}
</style>
