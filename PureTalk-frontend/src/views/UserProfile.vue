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
        <h2>个人信息</h2>

        <div class="quick-links">
          <router-link to="/notification" class="quick-link">
            <span class="link-icon">🔔</span>
            <span class="link-text">我的通知</span>
          </router-link>
          <router-link to="/feedback" class="quick-link">
            <span class="link-icon">📝</span>
            <span class="link-text">意见反馈</span>
          </router-link>
        </div>

        <div class="profile-form">
          <div class="form-group">
            <label for="username">用户名</label>
            <input 
              type="text" 
              id="username" 
              v-model="form.username" 
              placeholder="请输入用户名"
            />
          </div>
          
          <div class="form-group">
            <label for="phone">手机号</label>
            <input 
              type="tel" 
              id="phone" 
              v-model="form.phone" 
              placeholder="请输入手机号"
            />
          </div>
          
          <div class="form-group">
            <label for="email">邮箱</label>
            <input 
              type="email" 
              id="email" 
              v-model="form.email" 
              placeholder="请输入邮箱"
            />
          </div>
          
          <div class="form-group">
            <label for="password">密码</label>
            <input 
              type="password" 
              id="password" 
              v-model="form.password" 
              placeholder="请输入新密码"
            />
          </div>
          
          <button 
            class="update-btn" 
            :disabled="loading"
            @click="handleUpdate"
          >
            {{ loading ? '更新中...' : '更新信息' }}
          </button>
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
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { userApi } from '@/api/user'

const router = useRouter()
const loading = ref<boolean>(false)
const form = ref({
  username: '',
  phone: '',
  email: '',
  password: ''
})

const handleUpdate = async () => {
  loading.value = true
  try {
    const response = await userApi.update(form.value)
    const data = response as any
    if (data.code === 200) {
      alert('更新成功')
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
  // 直接返回首页，避免循环
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

.profile-card h2 {
  font-size: 1.5rem;
  font-weight: bold;
  color: #333;
  margin-bottom: 2rem;
  text-align: center;
}

.quick-links {
  display: flex;
  gap: 1rem;
  margin-bottom: 2rem;
  padding-bottom: 1.5rem;
  border-bottom: 1px solid #f0f0f0;
}

.quick-link {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 1rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  text-decoration: none;
  border-radius: 10px;
  transition: all 0.3s ease;
}

.quick-link:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.link-icon {
  font-size: 1.2rem;
}

.link-text {
  font-size: 0.95rem;
  font-weight: 500;
}

.profile-form {
  margin-bottom: 3rem;
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

.update-btn {
  width: 100%;
  padding: 0.75rem;
  border: none;
  border-radius: 4px;
  background-color: #4CAF50;
  color: #fff;
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.3s ease;
  margin-top: 1rem;
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
}
</style>