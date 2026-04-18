<template>
  <div class="notification-container">
    <div class="notification-header">
      <h2>我的通知</h2>
      <div class="header-actions">
        <button
          v-if="notifications.length > 0"
          class="mark-all-btn"
          @click="markAllRead"
        >
          全部标为已读
        </button>
      </div>
    </div>

    <div class="notification-list" v-if="notifications.length > 0">
      <div
        v-for="notification in notifications"
        :key="notification.id"
        :class="['notification-item', { unread: !notification.isRead }]"
        @click="handleNotificationClick(notification)"
      >
        <div class="notification-icon" :class="notification.type">
          {{ getNotificationIcon(notification.type) }}
        </div>
        <div class="notification-content">
          <p class="notification-text">{{ notification.content }}</p>
          <span class="notification-time">{{ formatTime(notification.createTime) }}</span>
        </div>
        <button
          class="delete-btn"
          @click.stop="deleteNotification(notification.id)"
        >
          ×
        </button>
      </div>
    </div>

    <div v-else class="empty-state">
      <div class="empty-icon">🔔</div>
      <p>暂无通知</p>
    </div>

    <div v-if="loading" class="loading-indicator">
      <div class="spinner"></div>
    </div>

    <div v-if="hasMore && !loading" class="load-more" @click="loadMore">
      加载更多
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { notificationApi, Notification } from '@/api/notification'
import { debounce } from '@/utils/debounce'
import { getUserId } from '@/api/user'

const router = useRouter()
const notifications = ref<Notification[]>([])
const loading = ref(false)
const currentPage = ref(1)
const hasMore = ref(true)

const getNotificationIcon = (type: string) => {
  const icons: Record<string, string> = {
    like: '❤️',
    comment: '💬',
    follow: '👤',
    system: '🔔',
    reply: '↩️'
  }
  return icons[type] || '🔔'
}

const formatTime = (time: string) => {
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()

  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`

  return date.toLocaleDateString()
}

const fetchNotifications = async (page = 1) => {
  const userId = getUserId()
  if (!userId) {
    router.push('/login')
    return
  }

  try {
    loading.value = true
    const response = await notificationApi.getNotifications(page, 20)
    const data = response as any
    if (data.code === 200) {
      if (page === 1) {
        notifications.value = data.data.records
      } else {
        notifications.value.push(...data.data.records)
      }
      hasMore.value = data.data.current < data.data.pages
      currentPage.value = page
    }
  } catch (error) {
    console.error('获取通知失败:', error)
  } finally {
    loading.value = false
  }
}

const loadMore = debounce(() => {
  if (hasMore.value && !loading.value) {
    fetchNotifications(currentPage.value + 1)
  }
}, 500)

const handleNotificationClick = debounce(async (notification: Notification) => {
  if (!notification.isRead) {
    try {
      await notificationApi.markAsRead(notification.id)
      notification.isRead = true
    } catch (error) {
      console.error('标记已读失败:', error)
    }
  }

  if (notification.relatedId && notification.relatedType === 'post') {
    router.push(`/post/${notification.relatedId}`)
  }
}, 300)

const markAllRead = debounce(async () => {
  try {
    await notificationApi.markAllAsRead()
    notifications.value.forEach(n => n.isRead = true)
  } catch (error) {
    console.error('标记全部已读失败:', error)
  }
}, 500)

const deleteNotification = debounce(async (id: number) => {
  try {
    await notificationApi.deleteNotification(id)
    notifications.value = notifications.value.filter(n => n.id !== id)
  } catch (error) {
    console.error('删除通知失败:', error)
  }
}, 300)

onMounted(() => {
  fetchNotifications()
})
</script>

<style scoped>
.notification-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%);
  padding: 1rem;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  background: #fff;
  border-radius: 12px;
  margin-bottom: 1rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.notification-header h2 {
  font-size: 1.5rem;
  color: #333;
  margin: 0;
}

.mark-all-btn {
  padding: 0.5rem 1rem;
  border: 1px solid #667eea;
  border-radius: 20px;
  background: #fff;
  color: #667eea;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.3s ease;
}

.mark-all-btn:hover {
  background: #667eea;
  color: #fff;
}

.notification-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  padding: 1.25rem;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: all 0.3s ease;
}

.notification-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.notification-item.unread {
  background: linear-gradient(135deg, #f0f3ff 0%, #e8ecff 100%);
  border-left: 3px solid #667eea;
}

.notification-icon {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.25rem;
  margin-right: 1rem;
  flex-shrink: 0;
}

.notification-icon.like {
  background: #ffe0e6;
}

.notification-icon.comment {
  background: #e0f0ff;
}

.notification-icon.follow {
  background: #e0ffe0;
}

.notification-icon.system {
  background: #fff4e0;
}

.notification-icon.reply {
  background: #f0e0ff;
}

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-text {
  margin: 0 0 0.5rem 0;
  color: #333;
  font-size: 0.95rem;
  line-height: 1.5;
}

.notification-time {
  font-size: 0.8rem;
  color: #999;
}

.delete-btn {
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 50%;
  background: #f0f0f0;
  color: #999;
  font-size: 1.2rem;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: 0.75rem;
  flex-shrink: 0;
}

.delete-btn:hover {
  background: #ff4d4f;
  color: #fff;
}

.empty-state {
  text-align: center;
  padding: 4rem 2rem;
  background: #fff;
  border-radius: 12px;
}

.empty-icon {
  font-size: 4rem;
  margin-bottom: 1rem;
}

.empty-state p {
  color: #999;
  font-size: 1.1rem;
}

.loading-indicator {
  display: flex;
  justify-content: center;
  padding: 2rem;
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #e0e0e0;
  border-top-color: #667eea;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.load-more {
  text-align: center;
  padding: 1rem;
  color: #667eea;
  font-size: 0.95rem;
  cursor: pointer;
  background: #fff;
  border-radius: 8px;
  margin-top: 0.75rem;
  transition: all 0.3s ease;
}

.load-more:hover {
  background: #667eea;
  color: #fff;
}

@media (max-width: 768px) {
  .notification-header {
    padding: 1rem;
  }

  .notification-header h2 {
    font-size: 1.25rem;
  }

  .notification-item {
    padding: 1rem;
  }

  .notification-icon {
    width: 38px;
    height: 38px;
    font-size: 1rem;
    margin-right: 0.75rem;
  }

  .notification-text {
    font-size: 0.9rem;
  }
}
</style>