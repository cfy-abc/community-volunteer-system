import request from '@/utils/request'

// 登录
export function loginAPI(data) {
  return request.post('/api/users/login', data)
}

// 注册
export function registerAPI(data) {
  return request.post('/api/users/register', data)
}

// 获取用户信息
export function getUserInfoAPI() {
  return request.get('/api/users/info')
}

// 退出登录
export function logoutAPI() {
  return request.post('/api/users/logout')
}

// 更新用户信息
export function updateUserInfoAPI(data) {
  return request.put('/api/users/update', data)
}

// 修改密码
export function changePasswordAPI(data) {
  return request.put('/api/users/change-password', data)
}