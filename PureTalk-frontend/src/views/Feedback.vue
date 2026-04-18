<template>
  <div class="feedback-container">
    <div class="feedback-header">
      <h2>意见反馈</h2>
      <p class="subtitle">您的反馈是我们前进的动力</p>
    </div>

    <div class="feedback-form">
      <div class="form-group">
        <label>反馈类型</label>
        <div class="type-selector">
          <button
            v-for="type in feedbackTypes"
            :key="type.value"
            :class="['type-btn', { active: form.type === type.value }]"
            @click="form.type = type.value"
          >
            {{ type.label }}
          </button>
        </div>
      </div>

      <div class="form-group">
        <label for="content">反馈内容</label>
        <textarea
          id="content"
          v-model="form.content"
          placeholder="请详细描述您的问题或建议..."
          rows="6"
        ></textarea>
        <span class="char-count">{{ form.content.length }}/500</span>
      </div>

      <button
        class="submit-btn"
        :disabled="!isFormValid || loading"
        @click="submitFeedback"
      >
        {{ loading ? '提交中...' : '提交反馈' }}
      </button>
    </div>

    <div v-if="submitSuccess" class="success-message">
      <div class="success-icon">✓</div>
      <p>感谢您的反馈！</p>
      <p class="success-sub">我们会尽快处理并回复您</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { feedbackApi } from '@/api/feedback'
import { debounce } from '@/utils/debounce'
import { getUserId } from '@/api/user'

const router = useRouter()
const loading = ref(false)
const submitSuccess = ref(false)

const feedbackTypes = [
  { label: '功能建议', value: 'suggestion' },
  { label: 'Bug反馈', value: 'bug' },
  { label: '体验问题', value: 'experience' },
  { label: '其他', value: 'other' }
]

const form = ref({
  type: 'suggestion',
  content: ''
})

const isFormValid = computed(() => {
  return form.value.content.trim().length >= 10 && form.value.content.length <= 500
})

const submitFeedback = debounce(async () => {
  if (!isFormValid.value) {
    alert('请输入至少10个字符的反馈内容')
    return
  }

  const userId = getUserId()
  if (!userId) {
    alert('请先登录')
    router.push('/login')
    return
  }

  try {
    loading.value = true
    const response = await feedbackApi.sendFeedback({
      userId,
      content: form.value.content,
      type: form.value.type
    })
    const data = response as any
    if (data.code === 200) {
      submitSuccess.value = true
      setTimeout(() => {
        router.push('/')
      }, 2000)
    } else {
      alert(data.msg || '提交失败')
    }
  } catch (error: any) {
    console.error('提交反馈失败:', error)
    alert(error.msg || '提交失败，请稍后重试')
  } finally {
    loading.value = false
  }
}, 500)
</script>

<style scoped>
.feedback-container {
  min-height: 100vh;
  padding: 2rem;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%);
}

.feedback-header {
  text-align: center;
  margin-bottom: 3rem;
  padding-top: 1rem;
}

.feedback-header h2 {
  font-size: 2rem;
  color: #333;
  margin-bottom: 0.5rem;
}

.subtitle {
  color: #666;
  font-size: 1rem;
}

.feedback-form {
  max-width: 600px;
  margin: 0 auto;
  background: #fff;
  border-radius: 16px;
  padding: 2.5rem;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.form-group {
  margin-bottom: 2rem;
  position: relative;
}

.form-group label {
  display: block;
  margin-bottom: 0.75rem;
  color: #333;
  font-size: 1rem;
  font-weight: 600;
}

.type-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.type-btn {
  padding: 0.75rem 1.5rem;
  border: 2px solid #e0e0e0;
  border-radius: 25px;
  background: #fff;
  color: #666;
  font-size: 0.95rem;
  cursor: pointer;
  transition: all 0.3s ease;
}

.type-btn:hover {
  border-color: #667eea;
  color: #667eea;
}

.type-btn.active {
  border-color: #667eea;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

textarea {
  width: 100%;
  padding: 1rem;
  border: 2px solid #e0e0e0;
  border-radius: 12px;
  font-size: 1rem;
  resize: none;
  transition: all 0.3s ease;
  font-family: inherit;
  line-height: 1.6;
}

textarea:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.char-count {
  position: absolute;
  right: 0.5rem;
  bottom: -1.5rem;
  font-size: 0.75rem;
  color: #999;
}

.submit-btn {
  width: 100%;
  padding: 1rem;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  font-size: 1.1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-top: 1rem;
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

.submit-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.success-message {
  max-width: 600px;
  margin: 2rem auto 0;
  background: #fff;
  border-radius: 16px;
  padding: 3rem;
  text-align: center;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  animation: fadeInUp 0.5s ease-out;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.success-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, #4CAF50 0%, #45a049 100%);
  color: #fff;
  font-size: 3rem;
  line-height: 80px;
  margin: 0 auto 1.5rem;
  animation: scaleIn 0.5s ease-out;
}

@keyframes scaleIn {
  from {
    transform: scale(0);
  }
  to {
    transform: scale(1);
  }
}

.success-message p {
  font-size: 1.3rem;
  color: #333;
  margin: 0;
}

.success-sub {
  font-size: 0.95rem !important;
  color: #999 !important;
  margin-top: 0.5rem !important;
}

@media (max-width: 768px) {
  .feedback-container {
    padding: 1rem;
  }

  .feedback-header h2 {
    font-size: 1.5rem;
  }

  .feedback-form {
    padding: 1.5rem;
    border-radius: 12px;
  }

  .type-selector {
    flex-direction: column;
  }

  .type-btn {
    width: 100%;
  }

  textarea {
    min-height: 150px;
  }
}
</style>