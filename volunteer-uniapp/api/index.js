import request from '@/utils/request'

// 用户相关接口
export const userApi = {
  login: (data) => request.post('/api/users/login', data),
  register: (data) => request.post('/api/users/register', data),
  getUserInfo: () => request.get('/api/users/info'),
  getUserById: (userId) => request.get(`/api/users/info/${userId}`),
  updateUserInfo: (data) => request.put('/api/users/update', data),
  changePassword: (data) => request.put('/api/users/change-password', data),
  createOrganization: (data) => request.post('/api/users/organizations', data)
}

// 活动相关接口
export const activityApi = {
  getActivityList: (params) => request.get('/activities', params),
  getActivityDetail: (id) => request.get(`/activities/${id}`),
  publishActivity: (data) => request.post('/activities', data),
  registerForActivity: (id) => request.post(`/activities/${id}/register`),
  getSignStatus: (id) => request.get(`/activities/${id}/sign-status`),
  checkIn: (id, data) => request.post(`/activities/${id}/checkin`, data),
  checkOut: (id, data) => request.post(`/activities/${id}/checkout`, data)
}

// 组织相关接口
export const organizationApi = {
  createOrganization: (data) => request.post('/organizations', data),
  getCurrentOrganization: () => request.get('/organizations/current'),
  getAllOrganizations: () => request.get('/organizations'),
  joinOrganization: (orgId) => request.post(`/organizations/${orgId}/join`),
  leaveOrganization: (orgId) => request.delete(`/organizations/${orgId}/leave`),
  getMembers: (orgId) => request.get(`/organizations/${orgId}/members`)
}

// 管理员相关接口
export const adminApi = {
  login: (data) => request.post('/admin/login', data),
  getDashboard: () => request.get('/admin/dashboard'),
  getUsers: (params) => request.get('/admin/users', params),
  auditUser: (id, status) => request.put(`/admin/users/${id}/status`, { status }),
  auditOrganization: (id, status) => request.put(`/admin/organizations/${id}/status`, { status }),
  auditActivity: (id, status) => request.put(`/admin/activities/${id}/status`, { status })
}

// 导出所有接口
export default {
  userApi,
  activityApi,
  organizationApi,
  adminApi
}
