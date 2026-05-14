// 获取 token
export const getToken = () => {
  return uni.getStorageSync('token') || ''
}

// 设置 token
export const setToken = (token) => {
  uni.setStorageSync('token', token)
}

// 移除 token
export const removeToken = () => {
  uni.removeStorageSync('token')
}

// 检查是否已登录
export const isAuthenticated = () => {
  return !!getToken()
}

// 获取用户信息
export const getUserInfo = () => {
  return uni.getStorageSync('userInfo') || null
}

// 设置用户信息
export const setUserInfo = (userInfo) => {
  uni.setStorageSync('userInfo', userInfo)
}

// 移除用户信息
export const removeUserInfo = () => {
  uni.removeStorageSync('userInfo')
}

// 清除所有认证信息
export const clearAuth = () => {
  removeToken()
  removeUserInfo()
}