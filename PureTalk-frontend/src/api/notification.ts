import axios from './axios'
import { Result } from './user'

export interface Notification {
  id: number
  userId: number
  type: string
  content: string
  isRead: boolean
  createTime: string
}

export interface NotificationTarget {
  id: number
  type: string
  content: string
}

export interface NotificationPageResult {
  records: Notification[]
  total: number
  current: number
  pages: number
  size: number
}

export const notificationApi = {
  // 获取通知列表
  getNotifications: (userId: number, page: number = 1, size: number = 20) => 
    axios.get<Result<NotificationPageResult>>('/notification', { params: { userId, page, size } }),
  // 获取通知目标
  getNotificationTarget: (notificationId: number) => 
    axios.get<Result<NotificationTarget>>(`/notification/${notificationId}/target`)
}