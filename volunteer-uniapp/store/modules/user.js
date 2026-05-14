// 用户模块
const state = () => ({
  userInfo: null,
  token: '',
  isAuthenticated: false
})

const getters = {
  // 获取用户信息
  userInfo: (state) => state.userInfo,
  
  // 获取用户名
  username: (state) => state.userInfo?.username || '',
  
  // 获取真实姓名
  realName: (state) => state.userInfo?.realName || '',
  
  // 获取用户角色
  role: (state) => state.userInfo?.role || 'volunteer',
  
  // 是否已认证
  isAuthenticated: (state) => state.isAuthenticated,
  
  // 是否为管理员
  isAdmin: (state) => state.userInfo?.role === 'admin',
  
  // 是否为志愿者
  isVolunteer: (state) => state.userInfo?.role === 'volunteer'
}

const mutations = {
  // 设置用户信息
  SET_USER_INFO(state, userInfo) {
    state.userInfo = userInfo
  },
  
  // 设置 token
  SET_TOKEN(state, token) {
    state.token = token
  },
  
  // 设置认证状态
  SET_AUTHENTICATED(state, isAuthenticated) {
    state.isAuthenticated = isAuthenticated
  },
  
  // 清空用户信息
  CLEAR_USER_INFO(state) {
    state.userInfo = null
    state.token = ''
    state.isAuthenticated = false
  }
}

const actions = {
  // 用户登录
  async login({ commit }, credentials) {
    try {
      // 调用登录接口
      const response = await this.$api.userApi.login(credentials)
      
      if (response.success) {
        const { token, userInfo } = response.data
        
        // 存储 token 和用户信息
        commit('SET_TOKEN', token)
        commit('SET_USER_INFO', userInfo)
        commit('SET_AUTHENTICATED', true)
        
        // 存储到本地缓存
        uni.setStorageSync('token', token)
        uni.setStorageSync('userInfo', userInfo)
        
        return { success: true, message: '登录成功' }
      } else {
        return { success: false, message: response.message || '登录失败' }
      }
    } catch (error) {
      console.error('登录失败:', error)
      return { success: false, message: error.message || '登录失败' }
    }
  },
  
  // 用户注册
  async register({ commit }, userData) {
    try {
      // 调用注册接口
      const response = await this.$api.userApi.register(userData)
      
      if (response.success) {
        return { success: true, message: '注册成功' }
      } else {
        return { success: false, message: response.message || '注册失败' }
      }
    } catch (error) {
      console.error('注册失败:', error)
      return { success: false, message: error.message || '注册失败' }
    }
  },
  
  // 获取用户信息
  async getUserInfo({ commit, state }) {
    try {
      if (!state.token) {
        throw new Error('未登录')
      }
      
      // 调用获取用户信息接口
      const response = await this.$api.userApi.getUserInfo()
      
      if (response.success) {
        const userInfo = response.data
        commit('SET_USER_INFO', userInfo)
        
        // 更新本地缓存
        uni.setStorageSync('userInfo', userInfo)
        
        return { success: true, data: userInfo }
      } else {
        throw new Error(response.message || '获取用户信息失败')
      }
    } catch (error) {
      console.error('获取用户信息失败:', error)
      throw error
    }
  },
  
  // 更新用户信息
  async updateUserInfo({ commit, state }, userInfo) {
    try {
      if (!state.token) {
        throw new Error('未登录')
      }
      
      // 调用更新用户信息接口
      const response = await this.$api.userApi.updateUserInfo(userInfo)
      
      if (response.success) {
        const updatedUserInfo = { ...state.userInfo, ...userInfo }
        commit('SET_USER_INFO', updatedUserInfo)
        
        // 更新本地缓存
        uni.setStorageSync('userInfo', updatedUserInfo)
        
        return { success: true, message: '更新成功' }
      } else {
        return { success: false, message: response.message || '更新失败' }
      }
    } catch (error) {
      console.error('更新用户信息失败:', error)
      return { success: false, message: error.message || '更新失败' }
    }
  },
  
  // 修改密码
  async changePassword({ state }, passwords) {
    try {
      if (!state.token) {
        throw new Error('未登录')
      }
      
      // 调用修改密码接口
      const response = await this.$api.userApi.changePassword(passwords)
      
      if (response.success) {
        return { success: true, message: '密码修改成功' }
      } else {
        return { success: false, message: response.message || '密码修改失败' }
      }
    } catch (error) {
      console.error('修改密码失败:', error)
      return { success: false, message: error.message || '密码修改失败' }
    }
  },
  
  // 上传头像
  async uploadAvatar({ commit, state }, fileData) {
    try {
      if (!state.token) {
        throw new Error('未登录')
      }
      
      // 调用上传头像接口
      const response = await this.$api.userApi.uploadAvatar(fileData)
      
      if (response.success) {
        const avatarUrl = response.data.url
        const updatedUserInfo = { ...state.userInfo, avatar: avatarUrl }
        commit('SET_USER_INFO', updatedUserInfo)
        
        // 更新本地缓存
        uni.setStorageSync('userInfo', updatedUserInfo)
        
        return { success: true, data: avatarUrl }
      } else {
        return { success: false, message: response.message || '上传失败' }
      }
    } catch (error) {
      console.error('上传头像失败:', error)
      return { success: false, message: error.message || '上传失败' }
    }
  },
  
  // 退出登录
  async logout({ commit }) {
    try {
      // 清空状态
      commit('CLEAR_USER_INFO')
      
      // 清空本地缓存
      uni.removeStorageSync('token')
      uni.removeStorageSync('userInfo')
      
      return { success: true, message: '退出成功' }
    } catch (error) {
      console.error('退出登录失败:', error)
      return { success: false, message: error.message || '退出失败' }
    }
  },
  
  // 自动登录
  async autoLogin({ commit }) {
    try {
      const token = uni.getStorageSync('token')
      const userInfo = uni.getStorageSync('userInfo')
      
      if (token && userInfo) {
        commit('SET_TOKEN', token)
        commit('SET_USER_INFO', userInfo)
        commit('SET_AUTHENTICATED', true)
        
        return { success: true }
      } else {
        return { success: false, message: '未找到登录信息' }
      }
    } catch (error) {
      console.error('自动登录失败:', error)
      return { success: false, message: error.message || '自动登录失败' }
    }
  }
}

export default {
  namespaced: true,
  state,
  getters,
  mutations,
  actions
}