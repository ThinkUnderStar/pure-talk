<template>
  <div class="post-detail">
    <header class="header">
      <button class="back-btn" @click="goBack">← 返回</button>
      <h1>PureTalk</h1>
      <div class="header-actions">
        <router-link to="/login" v-if="!isLoggedIn" class="btn">登录</router-link>
        <router-link to="/user/profile" v-else class="btn">个人中心</router-link>
      </div>
    </header>
    
    <main class="main">
      <div v-if="loading" class="loading">
        <div class="loading-spinner"></div>
        <p>加载中...</p>
      </div>
      
      <div v-else-if="post" class="post-content">
        <h2 class="post-title">{{ post.title }}</h2>
        <div class="post-meta">
          <span class="post-author">{{ post.username }}</span>
          <span class="post-time">{{ formatTime(post.createTime) }}</span>
          <div class="post-stats">
            <span class="stat-item" @click="likePost">
              <span class="stat-icon">{{ isLiked ? '❤️' : '👍' }}</span>
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
        <div class="post-body">
          {{ post.content }}
        </div>
      </div>
      
      <div class="comment-section">
        <h3>评论</h3>
        
        <div class="comment-form" v-if="isLoggedIn">
          <textarea 
            v-model="commentContent" 
            placeholder="写下你的评论..."
            rows="3"
          ></textarea>
          <button 
            class="submit-comment-btn" 
            :disabled="!commentContent.trim() || submittingComment"
            @click="submitComment"
          >
            {{ submittingComment ? '提交中...' : '提交评论' }}
          </button>
        </div>
        
        <div class="comment-list" ref="commentListRef" @scroll="handleCommentScroll">
          <div 
            v-for="comment in comments" 
            :key="comment.id" 
            class="comment-item"
          >
            <div class="comment-header">
              <span class="comment-author">{{ comment.username }}</span>
              <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
            </div>
            <div class="comment-body">
              {{ comment.content }}
            </div>
            <div class="comment-footer">
              <span class="comment-like" @click="likeComment(comment.id)">
                <span class="like-icon">{{ comment.isLiked ? '❤️' : '👍' }}</span>
                <span>{{ comment.likeCount }}</span>
              </span>
            </div>
          </div>
          
          <div v-if="loadingComments" class="loading">
            <div class="loading-spinner"></div>
            <p>加载评论中...</p>
          </div>
          
          <div v-if="hasMoreComments === false && comments.length > 0" class="no-more">
            没有更多评论了
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { postApi, Post } from '@/api/post'
import { commentApi, Comment } from '@/api/comment'
import { debounce } from '@/utils/debounce'

const router = useRouter()
const route = useRoute()
const postId = computed(() => Number(route.params.id))
const post = ref<Post | null>(null)
const comments = ref<(Comment & { isLiked?: boolean })[]>([])
const loading = ref<boolean>(true)
const loadingComments = ref<boolean>(false)
const submittingComment = ref<boolean>(false)
const hasMoreComments = ref<boolean>(true)
const currentCommentPage = ref<number>(1)
const commentContent = ref<string>('')
const isLiked = ref<boolean>(false)
const isLoggedIn = ref<boolean>(!!localStorage.getItem('token'))
const commentListRef = ref<HTMLElement | null>(null)

const loadPost = async () => {
  loading.value = true
  try {
    // 这里应该调用获取单个帖子的接口，但是后端没有提供，所以我们暂时从列表中获取
    // 实际项目中应该实现一个获取单个帖子的接口
    const response = await postApi.getPosts(0, 1, 100)
    const data = response as any
    if (data.code === 200) {
      post.value = data.data?.records.find((p: Post) => p.id === postId.value) || null
      if (post.value) {
        // 更新浏览量
        await postApi.updateViewCount({ [postId.value]: 1 })
      }
    }
  } catch (error) {
    console.error('加载帖子失败:', error)
  } finally {
    loading.value = false
  }
}

const loadComments = async () => {
  if (loadingComments.value) return
  
  loadingComments.value = true
  try {
    const response = await commentApi.getComments(postId.value, currentCommentPage.value, 20)
    const data = response as any
    if (data.code === 200) {
      const newComments = data.data?.records || []
      comments.value = currentCommentPage.value === 1 ? newComments : [...comments.value, ...newComments]
      hasMoreComments.value = newComments.length === 20
      currentCommentPage.value++
    }
  } catch (error) {
    console.error('加载评论失败:', error)
  } finally {
    loadingComments.value = false
  }
}

const likePost = async () => {
  if (!isLoggedIn.value) {
    router.push('/login')
    return
  }
  
  try {
    const response = await postApi.likePost(postId.value)
    const data = response as any
    if (data.code === 200) {
      if (post.value) {
        post.value.likeCount++
        isLiked.value = true
      }
    }
  } catch (error) {
    console.error('点赞失败:', error)
  }
}

const likeComment = async (commentId: number) => {
  if (!isLoggedIn.value) {
    router.push('/login')
    return
  }
  
  try {
    const response = await commentApi.likeComment(commentId)
    const data = response as any
    if (data.code === 200) {
      const comment = comments.value.find(c => c.id === commentId)
      if (comment) {
        comment.likeCount++
        comment.isLiked = true
      }
    }
  } catch (error) {
    console.error('点赞评论失败:', error)
  }
}

const submitComment = async () => {
  if (!commentContent.value.trim() || !isLoggedIn.value) return
  
  submittingComment.value = true
  try {
    const userId = localStorage.getItem('userId') || '1'
    const response = await commentApi.sendComment({
      postId: postId.value,
      content: commentContent.value,
      userId: Number(userId)
    })
    const data = response as any
    if (data.code === 200) {
      commentContent.value = ''
      // 重新加载评论
      currentCommentPage.value = 1
      comments.value = []
      hasMoreComments.value = true
      loadComments()
    } else {
      alert(data.msg || '评论失败')
    }
  } catch (error: any) {
    console.error('提交评论失败:', error)
    if (error.msg) {
      alert('评论失败: ' + error.msg)
    } else {
      alert('评论失败，请稍后重试')
    }
  } finally {
    submittingComment.value = false
  }
}

const goBack = () => {
  router.back()
}

const formatTime = (time: string) => {
  return new Date(time).toLocaleString()
}

const handleCommentScroll = debounce(() => {
  if (!commentListRef.value) return
  
  const { scrollTop, scrollHeight, clientHeight } = commentListRef.value
  if (scrollTop + clientHeight >= scrollHeight - 100 && !loadingComments.value && hasMoreComments.value) {
    loadComments()
  }
}, 200)

onMounted(() => {
  loadPost()
  loadComments()
})
</script>

<style scoped>
.post-detail {
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

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 4rem;
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

.post-content {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 2rem;
  margin-bottom: 2rem;
}

.post-title {
  font-size: 1.8rem;
  font-weight: bold;
  color: #333;
  margin-bottom: 1rem;
  line-height: 1.4;
}

.post-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.9rem;
  color: #999;
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #e0e0e0;
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
  cursor: pointer;
  transition: color 0.3s ease;
}

.stat-item:hover {
  color: #4CAF50;
}

.stat-icon {
  font-size: 1rem;
}

.post-body {
  font-size: 1rem;
  line-height: 1.6;
  color: #333;
  white-space: pre-wrap;
}

.comment-section {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 2rem;
}

.comment-section h3 {
  font-size: 1.2rem;
  font-weight: bold;
  color: #333;
  margin-bottom: 1.5rem;
}

.comment-form {
  margin-bottom: 2rem;
  padding-bottom: 2rem;
  border-bottom: 1px solid #e0e0e0;
}

.comment-form textarea {
  width: 100%;
  padding: 1rem;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  font-size: 1rem;
  resize: vertical;
  margin-bottom: 1rem;
  font-family: inherit;
  line-height: 1.5;
}

.comment-form textarea:focus {
  outline: none;
  border-color: #4CAF50;
  box-shadow: 0 0 0 2px rgba(76, 175, 80, 0.1);
}

.submit-comment-btn {
  padding: 0.5rem 1.5rem;
  border: none;
  border-radius: 4px;
  background-color: #4CAF50;
  color: #fff;
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.submit-comment-btn:hover:not(:disabled) {
  background-color: #45a049;
}

.submit-comment-btn:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.comment-list {
  max-height: 500px;
  overflow-y: auto;
}

.comment-item {
  padding: 1.5rem 0;
  border-bottom: 1px solid #e0e0e0;
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}

.comment-author {
  font-weight: 500;
  color: #333;
  font-size: 0.95rem;
}

.comment-time {
  font-size: 0.8rem;
  color: #999;
}

.comment-body {
  font-size: 0.95rem;
  line-height: 1.5;
  color: #333;
  margin-bottom: 0.5rem;
}

.comment-footer {
  display: flex;
  justify-content: flex-start;
}

.comment-like {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  cursor: pointer;
  font-size: 0.85rem;
  color: #999;
  transition: color 0.3s ease;
}

.comment-like:hover {
  color: #4CAF50;
}

.like-icon {
  font-size: 1rem;
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
  
  .post-content {
    padding: 1.5rem;
  }
  
  .comment-section {
    padding: 1.5rem;
  }
}
</style>