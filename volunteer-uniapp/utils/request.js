import store from '@/store'

// H5模式用相对路径走Vite代理（避免跨域），App/小程序用完整URL
// __UNI_PLATFORM__ 由 uni-app 编译时注入，值为 'h5'/'app-plus'/'mp-weixin' 等
const BASE_URL = (typeof __UNI_PLATFORM__ !== 'undefined' && __UNI_PLATFORM__ === 'h5')
  ? ''
  : 'http://localhost:8080'

// 创建请求实例
const request = {
  config: {
    baseUrl: BASE_URL,
    timeout: 15000
  },
  
  // 统一请求方法
  request(options = {}) {
    return new Promise((resolve, reject) => {
      // 添加 token (admin接口使用adminToken，登录接口不传token)
      const isAdminApi = options.url && options.url.startsWith('/admin') && !options.url.includes('/admin/login')
      const storageKey = isAdminApi ? 'adminToken' : 'token'
      const token = uni.getStorageSync(storageKey)
      const defaultHeader = {}
      if (token) {
        defaultHeader.Authorization = `Bearer ${token}`
      }
      // 非上传请求设置默认Content-Type
      if (!options.header || !options.header['Content-Type']) {
        defaultHeader['Content-Type'] = 'application/json'
      }
      options.header = { ...defaultHeader, ...options.header }
      
      // 完整 URL
      options.url = this.config.baseUrl + options.url
      
      // 发起请求
      uni.request({
        ...options,
        success: (res) => {
          // 处理响应
          if (res.statusCode === 200) {
            const data = res.data
            
            // 检查业务状态码
            if (data.code === 200 || data.success) {
              resolve(data)
            } else if (data.code === 401 || data.msg === '无效的token' || data.msg === 'Token无效或已过期') {
              // token 过期处理
              uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
              store.commit('auth/SET_TOKEN', '')
              uni.removeStorageSync('token')
              uni.removeStorageSync('adminToken')
              uni.removeStorageSync('userInfo')
              setTimeout(() => uni.reLaunch({ url: '/pages/auth/login' }), 1500)
              reject(new Error(data.msg || 'Error'))
            } else {
              // 业务错误：只 reject 但不额外弹 toast（调用方自行处理提示）
              reject(new Error(data.msg || '请求失败'))
            }
          } else {
            // HTTP 状态码错误
            this.handleError(res.statusCode)
            reject(new Error(`HTTP Error: ${res.statusCode}`))
          }
        },
        fail: (err) => {
          console.error('请求失败:', err)
          uni.showToast({
            title: '网络连接失败',
            icon: 'none'
          })
          reject(err)
        }
      })
    })
  },
  
  // 错误处理
  handleError(statusCode) {
    let msg = '请求失败'
    switch (statusCode) {
      case 401: msg = '登录已过期，请重新登录'; break
      case 403: msg = '没有权限访问此资源'; break
      case 404: msg = '请求的资源不存在'; break
      case 500: msg = '服务器内部错误'; break
      default:  msg = `请求失败: ${statusCode}`
    }
    uni.showToast({ title: msg, icon: 'none' })
  },
  
  // GET 请求
  get(url, data = {}, options = {}) {
    return this.request({
      url,
      data,
      method: 'GET',
      ...options
    })
  },
  
  // POST 请求
  post(url, data = {}, options = {}) {
    return this.request({
      url,
      data,
      method: 'POST',
      ...options
    })
  },
  
  // PUT 请求
  put(url, data = {}, options = {}) {
    return this.request({
      url,
      data,
      method: 'PUT',
      ...options
    })
  },
  
  // DELETE 请求
  delete(url, data = {}, options = {}) {
    return this.request({
      url,
      data,
      method: 'DELETE',
      ...options
    })
  }
}

// JWT解码工具函数
export function decodeJwt(token) {
  try {
    const payload = token.split('.')[1]
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/')
    const decoded = atob(base64)
    return JSON.parse(decoded)
  } catch (e) { return null }
}

export default request