// 验证手机号
export const validatePhone = (phone) => {
  const phoneRegex = /^1[3-9]\d{9}$/
  return phoneRegex.test(phone)
}

// 验证邮箱
export const validateEmail = (email) => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  return emailRegex.test(email)
}

// 验证密码强度
export const validatePassword = (password) => {
  if (password.length < 6) return false
  if (password.length > 30) return false
  // 至少包含大小写字母和数字
  if (!/(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/.test(password)) return false
  return true
}

// 验证用户名
export const validateUsername = (username) => {
  if (username.length < 3) return false
  if (username.length > 20) return false
  if (!/^[a-zA-Z0-9_\u4e00-\u9fa5]+$/.test(username)) return false
  return true
}

// 验证身份证号
export const validateIdCard = (idCard) => {
  const idCardRegex = /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/
  return idCardRegex.test(idCard)
}

// 验证年龄范围
export const validateAge = (age) => {
  const ageNum = parseInt(age)
  return ageNum >= 18 && ageNum <= 65
}