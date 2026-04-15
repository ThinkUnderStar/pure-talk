<template>
  <div class="home">
    <header class="header">
      <h1>PureTalk</h1>
      <div class="header-actions">
        <router-link to="/login" v-if="!isLoggedIn" class="btn">登录</router-link>
        <router-link to="/register" v-if="!isLoggedIn" class="btn">注册</router-link>
        <router-link to="/user/profile" v-else-if="userType === 'user'" class="btn">个人中心</router-link>
        <router-link to="/admin" v-else-if="userType === 'admin'" class="btn">管理中心</router-link>
      </div>
    </header>
    
    <main class="main">
      <div class="category-tabs">
        <button 
          :class="['tab', { active: currentCategory === 'all' }]"
          @click="switchCategory('all')"
        >
          综合
        </button>
        <button 
          :class="['tab', { active: currentCategory === 'hot' }]"
          @click="switchCategory('hot')"
        >
          最热
        </button>
        <button 
          :class="['tab', { active: currentCategory === 'latest' }]"
          @click="switchCategory('latest')"
        >
          最新
        </button>
      </div>
      
      <div class="post-list" ref="postListRef" @scroll="handleScroll">
        <div 
          v-for="post in posts" 
          :key="post.id" 
          class="post-item"
          @click="goToPostDetail(post.id)"
        >
          <h2 class="post-title">{{ post.title }}</h2>
          <p class="post-content">{{ post.content }}</p>
          <div class="post-meta">
            <span class="post-author">{{ post.username }}</span>
            <span class="post-time">{{ formatTime(post.createTime) }}</span>
            <div class="post-stats">
              <span class="stat-item">
                <span class="stat-icon">👍</span>
                <span>{{ post.likeCount }}</span>
              </span>
              <span class="stat-item">
                <span class="stat-icon">👁️</span>
                <span>{{ post.viewCount }}</span>
              </span>
              <span class="stat-item">
                <span class="stat-icon">💬</span>
                <span>{{ post.commentCount }}</span>
              </span>
            </div>
          </div>
        </div>
        
        <div v-if="loading" class="loading">
          <div class="loading-spinner"></div>
          <p>加载中...</p>
        </div>
        
        <div v-if="hasMore === false && posts.length > 0" class="no-more">
          没有更多内容了
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { postApi, Post } from '@/api/post'
import { debounce } from '@/utils/debounce'

const router = useRouter()
const postListRef = ref<HTMLElement | null>(null)
const posts = ref<Post[]>([])
const currentCategory = ref<string>('all')
const currentPage = ref<number>(1)
const loading = ref<boolean>(false)
const hasMore = ref<boolean>(true)
const isLoggedIn = ref<boolean>(!!localStorage.getItem('token'))
const userType = ref<string>(localStorage.getItem('userType') || 'user')

const loadPosts = async () => {
  if (loading.value) return
  
  loading.value = true
  try {
    let response
    if (currentCategory.value === 'all') {
      response = await postApi.getPosts(0, currentPage.value, 20)
    } else if (currentCategory.value === 'hot') {
      // 这里应该调用获取最热帖子的接口，但是后端没有提供，所以我们暂时使用getPosts接口
      response = await postApi.getPosts(0, currentPage.value, 20)
    } else if (currentCategory.value === 'latest') {
      // 这里应该调用获取最新帖子的接口，但是后端没有提供，所以我们暂时使用getPosts接口
      response = await postApi.getPosts(0, currentPage.value, 20)
    }
    
    const data = response as any
    if (data.code === 200) {
      const newPosts = data.data?.list || []
      posts.value = currentPage.value === 1 ? newPosts : [...posts.value, ...newPosts]
      hasMore.value = newPosts.length === 20
      currentPage.value++
    }
  } catch (error) {
    console.error('加载帖子失败:', error)
  } finally {
    loading.value = false
  }
}

const switchCategory = (category: string) => {
  currentCategory.value = category
  currentPage.value = 1
  posts.value = []
  hasMore.value = true
  loadPosts()
}

const goToPostDetail = (postId: number) => {
  router.push(`/post/${postId}`)
}

const formatTime = (time: string) => {
  return new Date(time).toLocaleString()
}

const handleScroll = debounce(() => {
  if (!postListRef.value) return
  
  const { scrollTop, scrollHeight, clientHeight } = postListRef.value
  if (scrollTop + clientHeight >= scrollHeight - 100 && !loading.value && hasMore.value) {
    loadPosts()
  }
}, 200)

onMounted(() => {
  loadPosts()
})
</script>

<style scoped>
.home {
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

.header h1 {
  font-size: 1.8rem;
  font-weight: bold;
  color: #333;
}

.header-actions {
  display: flex;
  gap: 1rem;
}

.btn {
  padding: 0.5rem 1rem;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  text-decoration: none;
  color: #333;
  font-size: 0.9rem;
  transition: all 0.3s ease;
}

.btn:hover {
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

.category-tabs {
  display: flex;
  gap: 1rem;
  margin-bottom: 2rem;
  border-bottom: 1px solid #e0e0e0;
  padding-bottom: 0.5rem;
}

.tab {
  padding: 0.5rem 1rem;
  border: none;
  background: none;
  font-size: 1rem;
  color: #666;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.3s ease;
}

.tab:hover {
  color: #333;
}

.tab.active {
  color: #333;
  border-bottom-color: #4CAF50;
  font-weight: bold;
}

.post-list {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 1rem;
  max-height: 80vh;
  overflow-y: auto;
}

.post-item {
  padding: 1.5rem;
  border-bottom: 1px solid #e0e0e0;
  cursor: pointer;
  transition: all 0.3s ease;
}

.post-item:hover {
  background-color: #f9f9f9;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.05);
}

.post-item:last-child {
  border-bottom: none;
}

.post-title {
  font-size: 1.2rem;
  font-weight: bold;
  color: #333;
  margin-bottom: 0.5rem;
  line-height: 1.4;
}

.post-content {
  font-size: 0.95rem;
  color: #666;
  margin-bottom: 1rem;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.post-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.85rem;
  color: #999;
}

.post-author {
  font-weight: 500;
  color: #666;
}

.post-stats {
  display: flex;
  gap: 1rem;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 0.3rem;
}

.stat-icon {
  font-size: 1rem;
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 2rem;
  color: #666;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #4CAF50;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 1rem;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.no-more {
  text-align: center;
  padding: 2rem;
  color: #999;
  font-size: 0.9rem;
}

@media (max-width: 768px) {
  .header {
    padding: 1rem;
  }
  
  .main {
    padding: 1rem;
  }
  
  .category-tabs {
    overflow-x: auto;
    white-space: nowrap;
  }
  
  .post-item {
    padding: 1rem;
  }
}
</style>