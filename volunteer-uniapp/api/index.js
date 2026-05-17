import request from '@/utils/request'

// 用户相关接口
export const userApi = {
  login: (data) => request.post('/api/users/login', data),
  register: (data) => request.post('/api/users/register', data),
  getUserInfo: () => request.get('/api/users/info'),
  getUserById: (userId) => request.get(`/api/users/info/${userId}`),
  updateUserInfo: (data) => request.put('/api/users/update', data),
  changePassword: (data) => request.put('/api/users/change-password', data),
  createOrganization: (data) => request.post('/api/users/organizations', data),
  getApplications: (params) => request.get('/api/users/applications', params),
  getStats: () => request.get('/api/users/stats'),
  verifyIdentity: (data) => request.post('/api/users/verify-identity', data),
  getVerifyStatus: () => request.get('/api/users/verify-status'),
  applyOrgUpgrade: (data) => request.post('/api/users/apply-org-upgrade', data),
  getOrgUpgradeStatus: () => request.get('/api/users/org-upgrade-status'),
  getWechatAuthUrl: () => request.get('/api/users/wechat/auth-url'),
  wechatLogin: (code) => request.post('/api/users/wechat/login', { code })
}

// 活动相关接口
export const activityApi = {
  getActivityList: (params) => request.get('/api/activities', params),
  getActivityDetail: (id) => request.get(`/api/activities/${id}`),
  publishActivity: (data) => request.post('/api/activities', data),
  registerForActivity: (id) => request.post(`/api/activities/${id}/register`),
  getSignStatus: (id) => request.get(`/api/activities/${id}/sign-status`),
  checkIn: (id, data) => request.post(`/api/activities/${id}/checkin`, data),
  checkOut: (id, data) => request.post(`/api/activities/${id}/checkout`, data),
  getComments: (id) => request.get(`/api/activities/${id}/comments`),
  addComment: (id, data) => request.post(`/api/activities/${id}/comments`, data),
  getReplies: (commentId) => request.get(`/api/activities/comments/${commentId}/replies`),
  replyComment: (commentId, data) => request.post(`/api/activities/comments/${commentId}/reply`, data),
  deleteComment: (commentId) => request.delete(`/api/activities/comments/${commentId}`),
  applyForActivity: (id, data) => request.post(`/api/activities/${id}/apply`, data),
  getApplicants: (id) => request.get(`/api/activities/${id}/applicants`),
  exportApplicants: (id) => request.get(`/api/activities/${id}/applicants/export`),
  organizerCheckIn: (id, data) => request.post(`/api/activities/${id}/organizer-checkin`, data),
  getSignApprovals: (id) => request.get(`/api/activities/${id}/sign-approvals`),
  approveSignRecord: (id) => request.put(`/api/activities/sign-approvals/${id}/approve`),
  rejectSignRecord: (id) => request.put(`/api/activities/sign-approvals/${id}/reject`)
}

// 组织相关接口
export const organizationApi = {
  createOrganization: (data) => request.post('/api/organizations', data),
  getCurrentOrganization: () => request.get('/api/organizations/current'),
  getAllOrganizations: () => request.get('/api/organizations'),
  joinOrganization: (orgId) => request.post(`/api/organizations/${orgId}/join`),
  leaveOrganization: (orgId) => request.delete(`/api/organizations/${orgId}/leave`),
  getMembers: (orgId) => request.get(`/api/organizations/${orgId}/members`)
}

// 管理员相关接口
export const adminApi = {
  login: (data) => request.post('/admin/login', data),
  getDashboard: () => request.get('/admin/dashboard'),
  getUsers: (params) => request.get('/admin/users', params),
  auditUser: (id, status) => request.put(`/admin/users/${id}/status`, { status }),
  auditOrganization: (id, status) => request.put(`/admin/organizations/${id}/status`, { status }),
  auditActivity: (id, status) => request.put(`/admin/activities/${id}/status`, { status }),
  // 组织升级申请
  getOrgUpgradeApplications: (params) => request.get('/admin/org-upgrade-applications', params),
  auditOrgUpgrade: (id, status) => request.put(`/admin/org-upgrade-applications/${id}/status`, { status }),
  // 实名认证管理
  getRealNameVerifications: (params) => request.get('/admin/real-name-verifications', params),
  approveRealName: (userId) => request.put(`/admin/real-name-verifications/${userId}/approve`),
  // 签到记录管理（手动修改时长）
  updateSignRecordHours: (id, hours) => request.put(`/admin/sign-records/${id}/hours`, { hours }),
  approveSignRecord: (id, data) => request.post(`/admin/sign-records/${id}/approve`, data),
  getSignApprovals: (params) => request.get('/admin/sign-approvals', params),
  rejectSignApproval: (id) => request.put(`/admin/sign-approvals/${id}/reject`)
}

// 导出所有接口
export default {
  userApi,
  activityApi,
  organizationApi,
  adminApi
}
