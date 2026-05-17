import App from './App'

// 路由守卫
const authRequiredPaths = [
  '/pages/activities/publish',
  '/pages/activities/apply',
  '/pages/activities/sign',
  '/pages/my/applications',
  '/pages/my/my-activities',
  '/pages/my/certificates',
  '/pages/my/org-upgrade',
  '/pages/auth/verify-identity'
]

const adminRequiredPaths = [
  '/pages/admin/dashboard',
  '/pages/admin/users',
  '/pages/admin/activities',
  '/pages/admin/organizations',
  '/pages/admin/sign-approvals'
]

uni.addInterceptor('navigateTo', {
  invoke(e) {
    const token = uni.getStorageSync('token')
    const adminToken = uni.getStorageSync('adminToken')
    const url = e.url.split('?')[0]

    if (authRequiredPaths.some(p => url.startsWith(p)) && !token) {
      uni.showToast({ title: '请先登录', icon: 'none' })
      uni.navigateTo({ url: '/pages/auth/login' })
      return false
    }

    if (adminRequiredPaths.some(p => url.startsWith(p)) && !adminToken) {
      uni.showToast({ title: '请先以管理员身份登录', icon: 'none' })
      uni.navigateTo({ url: '/pages/auth/admin-login' })
      return false
    }
  }
})

// #ifndef VUE3
import Vue from 'vue'
import './uni.promisify.adaptor'
Vue.config.productionTip = false
App.mpType = 'app'
const app = new Vue({
  ...App
})
app.$mount()
// #endif

// #ifdef VUE3
import { createSSRApp } from 'vue'
export function createApp() {
  const app = createSSRApp(App)
  return {
    app
  }
}
// #endif