import store from '@/store'

// 创建请求实例
const request = {
  config: {
    baseUrl: 'http://localhost:8080', // 修改为你的后端地址
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
            } else if (data.code === 401 || data.msg === '无效的token') {
              // token 过期处理
              uni.showToast({
                title: '登录已过期，请重新登录',
                icon: 'none'
              })
              store.commit('auth/SET_TOKEN', '')
              setTimeout(() => {
                uni.reLaunch({
                  url: '/pages/auth/login'
                })
              }, 1500)
              reject(new Error(data.msg || 'Error'))
            } else {
              // 其他错误
              uni.showToast({
                title: data.msg || '请求失败',
                icon: 'none'
              })
              reject(new Error(data.msg || 'Error'))
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
      case 401:
        msg = '登录已过期，请重新登录'
        break
      case 403:
        msg = '没有权限访问此资源'
        break
      case 404:
        msg = '请求的资源不存在'
        break
      case 500:
        msg = '服务器内部错误'
        break
      default:
        msg = `请求失败: ${statusCode}`
    }
    uni.showToast({
      title: msg,
      icon: 'none'
    })
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

export default request