<template>
  <div class="login-container">
    <div class="login-form">
      <h2>登录</h2>
      
      <div class="form-tabs">
        <button 
          :class="['tab', { active: loginType === 'password' }]"
          @click="loginType = 'password'"
        >
          密码登录
        </button>
        <button 
          :class="['tab', { active: loginType === 'code' }]"
          @click="loginType = 'code'"
        >
          验证码登录
        </button>
      </div>
      
      <form @submit.prevent="handleSubmit">
        <div class="form-group">
          <label for="phone">手机号</label>
          <input 
            type="tel" 
            id="phone" 
            v-model="form.phone" 
            placeholder="请输入手机号"
            required
          />
        </div>
        
        <div class="form-group" v-if="loginType === 'password'">
          <label for="password">密码</label>
          <input 
            type="password" 
            id="password" 
            v-model="form.password" 
            placeholder="请输入密码"
            required
          />
        </div>
        
        <div class="form-group" v-if="loginType === 'code'">
          <div class="code-input-group">
            <label for="code">验证码</label>
            <div class="code-input-wrapper">
              <input 
                type="text" 
                id="code" 
                v-model="form.code" 
                placeholder="请输入验证码"
                required
              />
              <button 
                type="button" 
                class="send-code-btn"
                :disabled="countdown > 0"
                @click="sendCode"
              >
                {{ countdown > 0 ? `${countdown}秒后重发` : '发送验证码' }}
              </button>
            </div>
          </div>
        </div>
        
        <div class="form-group" v-if="loginType === 'code'">
          <label class="checkbox-label">
            <input type="checkbox" v-model="form.remember" />
            记住登录状态
          </label>
        </div>
        
        <button type="submit" class="submit-btn" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
        
        <div class="form-footer">
          <router-link to="/register">还没有账号？去注册</router-link>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { userApi } from '@/api/user'
import { debounce } from '@/utils/debounce'

const router = useRouter()
const loginType = ref<'password' | 'code'>('password')
const loading = ref<boolean>(false)
const countdown = ref<number>(0)
const form = ref({
  phone: '',
  password: '',
  code: '',
  remember: false
})

const sendCode = debounce(async () => {
  if (!form.value.phone) {
    alert('请输入手机号')
    return
  }
  
  try {
    const response = await userApi.sendLoginCode(form.value.phone)
    const data = response as any
    if (data.code === 200) {
      alert('验证码已发送')
      startCountdown()
    } else {
      alert(data.message || '发送失败')
    }
  } catch (error) {
    console.error('发送验证码失败:', error)
    alert('发送验证码失败')
  }
}, 1000)

const startCountdown = () => {
  countdown.value = 60
  const timer = setInterval(() => {
    if (countdown.value > 0) {
      countdown.value--
    } else {
      clearInterval(timer)
    }
  }, 1000)
}

const handleSubmit = async () => {
  loading.value = true
  try {
    let response
    if (loginType.value === 'password') {
      response = await userApi.login({
        phone: form.value.phone,
        password: form.value.password
      })
    } else {
      response = await userApi.loginWithCode({
        phone: form.value.phone,
        code: form.value.code,
        remember: form.value.remember
      })
    }
    
    const data = response as any
    if (data.code === 200) {
      localStorage.setItem('token', data.data?.token || '')
      localStorage.setItem('userId', data.data?.id?.toString() || '')
      router.push('/')
    } else {
      alert(data.message || '登录失败')
    }
  } catch (error) {
    console.error('登录失败:', error)
    alert('登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f5f5f5;
  padding: 2rem;
}

.login-form {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  padding: 2rem;
  width: 100%;
  max-width: 400px;
}

.login-form h2 {
  text-align: center;
  margin-bottom: 2rem;
  color: #333;
  font-size: 1.5rem;
}

.form-tabs {
  display: flex;
  margin-bottom: 1.5rem;
  border-bottom: 1px solid #e0e0e0;
}

.tab {
  flex: 1;
  padding: 0.5rem;
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
  color: #4CAF50;
  border-bottom-color: #4CAF50;
  font-weight: bold;
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

.code-input-group {
  width: 100%;
}

.code-input-wrapper {
  display: flex;
  gap: 1rem;
}

.code-input-wrapper input {
  flex: 1;
}

.send-code-btn {
  padding: 0.75rem 1rem;
  border: 1px solid #4CAF50;
  border-radius: 4px;
  background-color: #fff;
  color: #4CAF50;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.send-code-btn:hover:not(:disabled) {
  background-color: #4CAF50;
  color: #fff;
}

.send-code-btn:disabled {
  border-color: #e0e0e0;
  color: #999;
  cursor: not-allowed;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  font-size: 0.9rem;
  color: #666;
}

.checkbox-label input {
  width: auto;
}

.submit-btn {
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

.submit-btn:hover:not(:disabled) {
  background-color: #45a049;
}

.submit-btn:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.form-footer {
  text-align: center;
  margin-top: 1.5rem;
  font-size: 0.9rem;
}

.form-footer a {
  color: #4CAF50;
  text-decoration: none;
  transition: color 0.3s ease;
}

.form-footer a:hover {
  color: #45a049;
  text-decoration: underline;
}

@media (max-width: 768px) {
  .login-container {
    padding: 1rem;
  }
  
  .login-form {
    padding: 1.5rem;
  }
}
</style>