import axios, { AxiosInstance, AxiosResponse } from 'axios'

const instance: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
instance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
instance.interceptors.response.use(
  (response: AxiosResponse) => {
    return response.data
  },
  (error) => {
    console.error('请求错误:', error)
    // 处理后端返回的错误
    if (error.response) {
      console.error('响应错误:', error.response.data)
      // 直接返回错误数据，让前端能够处理
      return Promise.reject(error.response.data)
    } else if (error.request) {
      // 请求已发出，但没有收到响应
      console.error('请求已发出，但没有收到响应:', error.request)
      return Promise.reject({ message: '网络错误，请检查网络连接' })
    } else {
      // 请求配置出错
      console.error('请求配置出错:', error.message)
      return Promise.reject({ message: '请求配置出错，请稍后重试' })
    }
  }
)

export default instance