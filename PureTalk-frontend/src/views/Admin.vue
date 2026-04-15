<template>
  <div class="admin">
    <header class="header">
      <button class="back-btn" @click="goBack">← 返回</button>
      <h1>管理中心</h1>
      <div class="header-actions">
        <button class="logout-btn" @click="handleLogout">登出</button>
      </div>
    </header>
    
    <main class="main">
      <div class="admin-card">
        <h2>管理员控制台</h2>
        
        <div class="admin-menu">
          <button class="menu-item" @click="activeMenu = 'users'">
            用户管理
          </button>
          <button class="menu-item" @click="activeMenu = 'posts'">
            帖子管理
          </button>
          <button class="menu-item" @click="activeMenu = 'comments'">
            评论管理
          </button>
          <button class="menu-item" @click="activeMenu = 'settings'">
            系统设置
          </button>
        </div>
        
        <div class="menu-content">
          <div v-if="activeMenu === 'users'" class="content-section">
            <h3>用户管理</h3>
            <p>这里可以管理所有用户</p>
          </div>
          
          <div v-if="activeMenu === 'posts'" class="content-section">
            <h3>帖子管理</h3>
            <p>这里可以管理所有帖子</p>
          </div>
          
          <div v-if="activeMenu === 'comments'" class="content-section">
            <h3>评论管理</h3>
            <p>这里可以管理所有评论</p>
          </div>
          
          <div v-if="activeMenu === 'settings'" class="content-section">
            <h3>系统设置</h3>
            <p>这里可以进行系统设置</p>
          </div>
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
const activeMenu = ref<string>('users')

const handleLogout = async () => {
  try {
    const response = await userApi.logout()
    const data = response as any
    if (data.code === 200) {
      localStorage.removeItem('token')
      localStorage.removeItem('userId')
      localStorage.removeItem('userType')
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
  router.back()
}
</script>

<style scoped>
.admin {
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
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
}

.admin-card {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 2rem;
}

.admin-card h2 {
  font-size: 1.5rem;
  font-weight: bold;
  color: #333;
  margin-bottom: 2rem;
  text-align: center;
}

.admin-menu {
  display: flex;
  gap: 1rem;
  margin-bottom: 2rem;
  border-bottom: 1px solid #e0e0e0;
  padding-bottom: 0.5rem;
}

.menu-item {
  padding: 0.75rem 1.5rem;
  border: none;
  background: none;
  font-size: 1rem;
  color: #666;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.3s ease;
}

.menu-item:hover {
  color: #333;
}

.menu-item.active {
  color: #333;
  border-bottom-color: #4CAF50;
  font-weight: bold;
}

.menu-content {
  min-height: 400px;
}

.content-section {
  padding: 2rem;
  background-color: #f9f9f9;
  border-radius: 8px;
}

.content-section h3 {
  font-size: 1.2rem;
  font-weight: bold;
  color: #333;
  margin-bottom: 1rem;
}

.content-section p {
  color: #666;
  line-height: 1.5;
}

@media (max-width: 768px) {
  .header {
    padding: 1rem;
  }
  
  .main {
    padding: 1rem;
  }
  
  .admin-card {
    padding: 1.5rem;
  }
  
  .admin-menu {
    overflow-x: auto;
    white-space: nowrap;
  }
  
  .menu-item {
    padding: 0.5rem 1rem;
  }
  
  .content-section {
    padding: 1rem;
  }
}
</style>