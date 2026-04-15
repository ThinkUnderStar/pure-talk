import axios from './axios'

export interface DoLogin {
  phone: string
  password: string
}

export interface DoRegister {
  username: string
  password: string
  phone: string
  email: string
}

export interface DoUpdate {
  username?: string
  password?: string
  phone?: string
  email?: string
}

export interface ValidateCode {
  phone: string
  code: string
  remember?: boolean
}

export interface LoginReturnData {
  id: number
  username: string
  phone: string
  email: string
  token: string
}

export interface Result<T = any> {
  code: number
  message: string
  data?: T
}

export const userApi = {
  // 登录
  login: (data: DoLogin) => axios.post<Result<LoginReturnData>>('/user/login', data),
  // 发送验证码（登录用）
  sendLoginCode: (phone: string) => axios.get<Result>('/user/login/code', { params: { phone } }),
  // 验证码登录
  loginWithCode: (data: ValidateCode) => axios.post<Result<LoginReturnData>>('/user/login/code', data),
  // 注册
  register: (data: DoRegister) => axios.post<Result>('/user/register', data),
  // 发送手机验证码（注册用）
  sendRegisterPhoneCode: (phone: string) => axios.get<Result>('/user/register/phone', { params: { phone } }),
  // 验证手机验证码
  validatePhoneCode: (data: ValidateCode) => axios.post<Result>('/user/register/phone', data),
  // 发送邮箱验证码（注册用）
  sendRegisterEmailCode: (email: string) => axios.get<Result>('/user/register/email', { params: { email } }),
  // 验证邮箱验证码
  validateEmailCode: (data: { email: string; code: string }) => axios.post<Result>('/user/register/email', data),
  // 删除账户
  deleteAccount: () => axios.delete<Result>('/user/delete'),
  // 登出
  logout: () => axios.get<Result>('/user/logout'),
  // 更新用户信息
  update: (data: DoUpdate) => axios.put<Result>('/user/update', data)
}