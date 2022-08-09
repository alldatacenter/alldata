<template>
  <div class="container">
    <el-form :model="form">
      <el-card class="login-panel">
        <el-form-item label="用户名">
          <el-input v-model="form.username" type="text"/>
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password"/>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="doLogin" v-if="!isRegisterStatus">登陆</el-button>
          <el-button type="success" @click="doRegister" v-if="isRegisterStatus">注册</el-button>
        </el-form-item>
        <a href="#" class="register" @click="goRegister" v-if="!isRegisterStatus">注册</a>
      </el-card>
    </el-form>
  </div>
</template>

<script src="./Login.ts" lang="ts" />

<style scoped lang="stylus">
@import url('./Login.stylus');
</style>
