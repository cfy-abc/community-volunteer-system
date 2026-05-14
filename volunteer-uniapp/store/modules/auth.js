import { loginAPI, registerAPI, getUserInfoAPI } from '@/api/auth'

export default {
  namespaced: true,
  state: () => ({
    token: uni.getStorageSync('token') || '',
    userInfo: null,
    loading: false,
    error: null
  }),
  getters: {
    isAuthenticated: state => !!state.token,
    userRole: state => state.userInfo?.role || 'user',
    isAdmin: state => state.userInfo?.role === 'admin',
    username: state => state.userInfo?.username || '',
    userId: state => state.userInfo?.id || null
  },
  mutations: {
    SET_TOKEN(state, token) {
      state.token = token
      if (token) {
        uni.setStorageSync('token', token)
      } else {
        uni.removeStorageSync('token')
      }
    },
    SET_USER_INFO(state, userInfo) {
      state.userInfo = userInfo
    },
    SET_LOADING(state, loading) {
      state.loading = loading
    },
    SET_ERROR(state, error) {
      state.error = error
    }
  },
  actions: {
    async login({ commit }, credentials) {
      commit('SET_LOADING', true)
      commit('SET_ERROR', null)
      
      try {
        const response = await loginAPI(credentials)
        if (response.data) {
          const token = response.data
          commit('SET_TOKEN', token)
          
          // 获取用户信息
          await this.dispatch('auth/getUserInfo')
          
          return true
        }
        return false
      } catch (error) {
        const errorMsg = error.errMsg ? '网络错误' : (error.data?.msg || '登录失败')
        commit('SET_ERROR', errorMsg)
        console.error('登录失败:', error)
        return false
      } finally {
        commit('SET_LOADING', false)
      }
    },
    
    async register({ commit }, userData) {
      commit('SET_LOADING', true)
      commit('SET_ERROR', null)
      
      try {
        const response = await registerAPI(userData)
        return response.data
      } catch (error) {
        const errorMsg = error.errMsg ? '网络错误' : (error.data?.msg || '注册失败')
        commit('SET_ERROR', errorMsg)
        console.error('注册失败:', error)
        throw error
      } finally {
        commit('SET_LOADING', false)
      }
    },
    
    async getUserInfo({ commit }) {
      const token = uni.getStorageSync('token')
      if (!token) return null
      
      try {
        const response = await getUserInfoAPI()
        commit('SET_USER_INFO', response.data)
        return response.data
      } catch (error) {
        console.error('获取用户信息失败:', error)
        // 如果获取用户信息失败，可能是token过期，清除token
        commit('SET_TOKEN', '')
        commit('SET_USER_INFO', null)
        throw error
      }
    },
    
    logout({ commit }) {
      commit('SET_TOKEN', '')
      commit('SET_USER_INFO', null)
    }
  }
}