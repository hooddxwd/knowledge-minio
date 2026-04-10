<template>
  <div class="main">
    <a-alert message="CheckAI登录" type="info">
      <div slot="description" v-html="errMsg"></div>
    </a-alert>

    <login-select-tenant ref="loginSelect" @success="loginSelectOk"></login-select-tenant>
  </div>
</template>

<script>
import Vue from 'vue'
import {mapActions} from 'vuex'
import {ACCESS_TOKEN} from '@/store/mutation-types'
import LoginSelectTenant from './LoginSelectTenant'

export default {
  components: {LoginSelectTenant},
  data() {
    return {
      paramCookie: '',
      errMsg: '',
    }
  },
  mounted() {
    debugger
    // this.paramCookie = this.getCookie();
    this.paramCookie = this.getCookie('KOAL_CERT_E');
    console.log("keylogin========paramCookie=============" + this.paramCookie);
    this.handleBzyptLogin();
  },
  created() {
    Vue.ls.remove(ACCESS_TOKEN);
    console.log('userkeylogin ........');
  },
  methods: {
    ...mapActions(['KeyLogin']),

    handleBzyptLogin() {
      debugger
      let loginParams = {'paramCookie': this.paramCookie};
      this.KeyLogin().then((res) => {
        let userName = res.result.userInfo.username;
        this.loginSuccess(userName)
      }).catch((err) => {
        debugger
        this.errMsg = "<h4>登录失败!</h4><p>" + err.code + ": " + err.message + "</p>";
        //this.requestFailed(err);
      });

    },
    // 登录后台成功
    requestSuccess(loginResult) {
      this.$refs.loginSelect.show(loginResult)
    },
    //登录后台失败
    requestFailed(err) {
      let description = ((err.response || {}).data || {}).message || err.message || '请求出现错误，请稍后再试'
      this.$notification['error']({
        message: '登录失败',
        description: description,
        duration: 4
      })
    },
    loginSelectOk() {
      this.loginSuccess()
    },
    //登录成功
    loginSuccess() {
      console.log("keylogin success.............");
      this.$router.push({path: '/home'}).catch(() => {
      })
    },

    stepCaptchaSuccess() {
      this.loginSuccess()
    },

    getCookie() {
      let matches = document.cookie.match(new RegExp("(?:^|; )" + name.replace(/([\.$?*|{}\(\)\[\]\\\/\+^])/g, '\\$1') + "=([^;]*)"));
      console.log("document.cookie==========" + document.cookie)
      return matches ? decodeURIComponent(matches[1]) : "";
    }

  }

}
</script>
<style lang="less" scoped>

</style>