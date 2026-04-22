<template>
  <div class="user-profile">
    <header class="header">
      <button class="back-btn" @click="goBack">← 返回</button>
      <h1>PureTalk</h1>
      <div class="header-actions">
        <button class="logout-btn" @click="handleLogout">登出</button>
      </div>
    </header>

    <main class="main">
      <div class="profile-card">
        <div class="profile-header">
          <div class="avatar-wrapper">
            <img
              :src="avatar || 'https://ui-avatars.com/api/?name=' + userInfo.username + '&background=random&size=128'"
              :alt="userInfo.username"
              class="profile-avatar"
              @click="viewFullAvatar"
            />
          </div>
          <h2>{{ userInfo.username }}</h2>
          <button class="change-avatar-btn" @click="triggerAvatarUpload">
            更改头像
          </button>
          <input
            type="file"
            ref="avatarInput"
            accept="image/*"
            style="display: none"
            @change="handleAvatarChange"
          />
          <div v-if="uploadingAvatar" class="avatar-uploading">上传中...</div>
        </div>

        <div class="profile-stats">
          <div class="stat-item">
            <span class="stat-value">{{ userInfo.likeCount || 0 }}</span>
            <span class="stat-label">获赞</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ userInfo.postCount || 0 }}</span>
            <span class="stat-label">帖子</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ userInfo.commentCount || 0 }}</span>
            <span class="stat-label">评论</span>
          </div>
        </div>

        <div class="profile-menu">
          <button class="menu-item" @click="showEditForm = true">
            <span class="menu-icon">✏️</span>
            <span class="menu-text">修改信息</span>
            <span class="menu-arrow">›</span>
          </button>
          <router-link to="/notification" class="menu-item">
            <span class="menu-icon">🔔</span>
            <span class="menu-text">我的通知</span>
            <span class="menu-arrow">›</span>
          </router-link>
          <router-link to="/feedback" class="menu-item">
            <span class="menu-icon">📝</span>
            <span class="menu-text">意见反馈</span>
            <span class="menu-arrow">›</span>
          </router-link>
        </div>

        <div v-if="showEditForm" class="edit-section">
          <h3>修改个人信息</h3>
          <div class="profile-form">
            <div class="form-group">
              <label for="username">用户名</label>
              <input
                type="text"
                id="username"
                v-model="form.username"
                placeholder="请输入用户名"
              />
              <span v-if="errors.username" class="error-text">{{ errors.username }}</span>
            </div>

            <div class="form-group">
              <label for="phone">手机号</label>
              <input
                type="tel"
                id="phone"
                v-model="form.phone"
                placeholder="请输入手机号"
              />
              <span v-if="errors.phone" class="error-text">{{ errors.phone }}</span>
            </div>

            <div class="form-group">
              <label for="email">邮箱</label>
              <input
                type="email"
                id="email"
                v-model="form.email"
                placeholder="请输入邮箱"
              />
              <span v-if="errors.email" class="error-text">{{ errors.email }}</span>
            </div>

            <div class="form-group">
              <label for="password">密码（选填）</label>
              <input
                type="password"
                id="password"
                v-model="form.password"
                placeholder="请输入新密码，不修改则留空"
              />
              <span v-if="errors.password" class="error-text">{{ errors.password }}</span>
            </div>

            <div class="form-actions">
              <button class="cancel-btn" @click="showEditForm = false">取消</button>
              <button
                class="update-btn"
                :disabled="loading"
                @click="handleUpdate"
              >
                {{ loading ? '更新中...' : '确认修改' }}
              </button>
            </div>
          </div>
        </div>

        <div class="danger-zone">
          <h3>危险操作</h3>
          <button class="delete-btn" @click="confirmDelete">删除账户</button>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { userApi } from '@/api/user'

const router = useRouter()
const loading = ref<boolean>(false)
const showEditForm = ref<boolean>(false)
const avatar = ref<string>('')
const uploadingAvatar = ref<boolean>(false)
const avatarInput = ref<HTMLInputElement | null>(null)
const userInfo = ref({
  username: '',
  likeCount: 0,
  postCount: 0,
  commentCount: 0
})
const form = ref({
  username: '',
  phone: '',
  email: '',
  password: ''
})
const errors = ref({
  username: '',
  phone: '',
  email: '',
  password: ''
})

const viewFullAvatar = () => {
  if (avatar.value) {
    window.open(avatar.value, '_blank')
  }
}

const triggerAvatarUpload = () => {
  avatarInput.value?.click()
}

const handleAvatarChange = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  if (!file.type.startsWith('image/')) {
    alert('请选择图片文件')
    return
  }

  if (file.size > 5 * 1024 * 1024) {
    alert('图片大小不能超过5MB')
    return
  }

  uploadingAvatar.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const response = await userApi.uploadAvatar(formData)
    const data = response as any
    if (data.code === 200) {
      avatar.value = data.data
      localStorage.setItem('avatar', data.data)
      alert('头像上传成功')
    } else {
      alert(data.message || '头像上传失败')
    }
  } catch (error) {
    console.error('头像上传失败:', error)
    alert('头像上传失败')
  } finally {
    uploadingAvatar.value = false
    target.value = ''
  }
}

const validateForm = (): boolean => {
  let isValid = true
  errors.value = { username: '', phone: '', email: '', password: '' }

  if (!form.value.username || form.value.username.trim().length < 2) {
    errors.value.username = '用户名至少2个字符'
    isValid = false
  }

  if (form.value.username && form.value.username.length > 20) {
    errors.value.username = '用户名不能超过20个字符'
    isValid = false
  }

  if (form.value.phone && !/^1[3-9]\d{9}$/.test(form.value.phone)) {
    errors.value.phone = '请输入有效的手机号'
    isValid = false
  }

  if (form.value.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.value.email)) {
    errors.value.email = '请输入有效的邮箱'
    isValid = false
  }

  if (form.value.password && form.value.password.length < 6) {
    errors.value.password = '密码至少6个字符'
    isValid = false
  }

  return isValid
}

onMounted(() => {
  userInfo.value.username = localStorage.getItem('username') || ''
  avatar.value = localStorage.getItem('avatar') || ''
})

const handleUpdate = async () => {
  if (!validateForm()) {
    return
  }
  loading.value = true
  try {
    const updateData: any = {}
    if (form.value.username) updateData.username = form.value.username
    if (form.value.phone) updateData.phone = form.value.phone
    if (form.value.email) updateData.email = form.value.email
    if (form.value.password) updateData.password = form.value.password

    const response = await userApi.update(updateData)
    const data = response as any
    if (data.code === 200) {
      if (form.value.username) {
        localStorage.setItem('username', form.value.username)
        userInfo.value.username = form.value.username
      }
      alert('更新成功')
      showEditForm.value = false
      form.value.password = ''
    } else {
      alert(data.message || '更新失败')
    }
  } catch (error) {
    console.error('更新失败:', error)
    alert('更新失败')
  } finally {
    loading.value = false
  }
}

const confirmDelete = () => {
  if (confirm('确定要删除账户吗？此操作不可恢复。')) {
    handleDelete()
  }
}

const handleDelete = async () => {
  try {
    const response = await userApi.deleteAccount()
    const data = response as any
    if (data.code === 200) {
      localStorage.removeItem('token')
      localStorage.removeItem('userId')
      router.push('/login')
    } else {
      alert(data.message || '删除失败')
    }
  } catch (error) {
    console.error('删除失败:', error)
    alert('删除失败')
  }
}

const handleLogout = async () => {
  try {
    const response = await userApi.logout()
    const data = response as any
    if (data.code === 200) {
      localStorage.removeItem('token')
      localStorage.removeItem('userId')
      router.push('/login')
    } else {
      alert(data.message || '登出失败')
    }
  } catch (error) {
    console.error('登出失败:', error)
    alert('登出失败')
  }
}

const goBack = () => {
  router.push('/')
}
</script>

<style scoped>
.user-profile {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.header {
  background-color: #fff;
  padding: 1rem 2rem;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.back-btn {
  background: none;
  border: none;
  font-size: 1.2rem;
  cursor: pointer;
  color: #333;
  padding: 0.5rem;
  border-radius: 4px;
  transition: background-color 0.3s ease;
}

.back-btn:hover {
  background-color: #f5f5f5;
}

.header h1 {
  font-size: 1.8rem;
  font-weight: bold;
  color: #333;
}

.logout-btn {
  padding: 0.5rem 1rem;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  background-color: #fff;
  color: #333;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.3s ease;
}

.logout-btn:hover {
  background-color: #f5f5f5;
  border-color: #ccc;
}

.main {
  flex: 1;
  padding: 2rem;
  max-width: 800px;
  margin: 0 auto;
  width: 100%;
}

.profile-card {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 2rem;
}

.profile-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 1.5rem;
}

.profile-avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  object-fit: cover;
  margin-bottom: 1rem;
  border: 3px solid #f0f0f0;
}

.avatar-wrapper {
  margin-bottom: 0.5rem;
}

.profile-avatar {
  cursor: pointer;
  transition: opacity 0.2s ease;
}

.profile-avatar:hover {
  opacity: 0.8;
}

.change-avatar-btn {
  background: none;
  border: none;
  color: #667eea;
  font-size: 0.9rem;
  cursor: pointer;
  margin-top: 0.5rem;
  padding: 0.25rem 0.5rem;
  transition: color 0.2s ease;
}

.change-avatar-btn:hover {
  color: #764ba2;
}

.avatar-uploading {
  font-size: 0.85rem;
  color: #667eea;
  margin-top: 0.25rem;
}

.error-text {
  color: #e74c3c;
  font-size: 0.8rem;
  margin-top: 0.25rem;
  display: block;
}

.profile-header h2 {
  font-size: 1.5rem;
  font-weight: bold;
  color: #333;
  margin: 0;
}

.profile-stats {
  display: flex;
  justify-content: center;
  gap: 3rem;
  margin-bottom: 2rem;
  padding-bottom: 1.5rem;
  border-bottom: 1px solid #f0f0f0;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.25rem;
}

.stat-value {
  font-size: 1.5rem;
  font-weight: bold;
  color: #333;
}

.stat-label {
  font-size: 0.85rem;
  color: #999;
}

.profile-menu {
  margin-bottom: 2rem;
}

.menu-item {
  display: flex;
  align-items: center;
  width: 100%;
  padding: 1rem;
  border: none;
  background: none;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  text-decoration: none;
  color: #333;
  transition: background-color 0.3s ease;
}

.menu-item:first-child {
  border-top: 1px solid #f0f0f0;
}

.menu-item:hover {
  background-color: #f9f9f9;
}

.menu-icon {
  font-size: 1.2rem;
  margin-right: 1rem;
}

.menu-text {
  flex: 1;
  text-align: left;
  font-size: 1rem;
}

.menu-arrow {
  font-size: 1.2rem;
  color: #ccc;
}

.edit-section {
  margin-bottom: 2rem;
  padding: 1.5rem;
  background-color: #f9f9f9;
  border-radius: 8px;
}

.edit-section h3 {
  font-size: 1.1rem;
  font-weight: bold;
  color: #333;
  margin-bottom: 1.5rem;
  text-align: center;
}

.profile-form {
  margin-bottom: 0;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  color: #333;
  font-size: 0.9rem;
  font-weight: 500;
}

.form-group input {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  font-size: 1rem;
  transition: border-color 0.3s ease;
}

.form-group input:focus {
  outline: none;
  border-color: #4CAF50;
  box-shadow: 0 0 0 2px rgba(76, 175, 80, 0.1);
}

.form-actions {
  display: flex;
  gap: 1rem;
  margin-top: 1.5rem;
}

.cancel-btn {
  flex: 1;
  padding: 0.75rem;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  background-color: #fff;
  color: #666;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.3s ease;
}

.cancel-btn:hover {
  background-color: #f5f5f5;
}

.update-btn {
  flex: 1;
  padding: 0.75rem;
  border: none;
  border-radius: 4px;
  background-color: #4CAF50;
  color: #fff;
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.update-btn:hover:not(:disabled) {
  background-color: #45a049;
}

.update-btn:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.danger-zone {
  border-top: 1px solid #e0e0e0;
  padding-top: 2rem;
}

.danger-zone h3 {
  font-size: 1.1rem;
  font-weight: bold;
  color: #333;
  margin-bottom: 1.5rem;
}

.delete-btn {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #ff4d4f;
  border-radius: 4px;
  background-color: #fff;
  color: #ff4d4f;
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.delete-btn:hover {
  background-color: #fff2f0;
  border-color: #ff7875;
}

@media (max-width: 768px) {
  .header {
    padding: 1rem;
  }

  .main {
    padding: 1rem;
  }

  .profile-card {
    padding: 1.5rem;
  }

  .profile-stats {
    gap: 2rem;
  }

  .stat-value {
    font-size: 1.2rem;
  }
}
</style>
