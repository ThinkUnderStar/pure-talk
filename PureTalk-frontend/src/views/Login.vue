<template>
  <div class="login-container">
    <div class="login-form">
      <h2>登录</h2>
      
      <div class="form-tabs">
        <button 
          :class="['tab', { active: userType === 'user' }]"
          @click="userType = 'user'"
        >
          用户登录
        </button>
        <button 
          :class="['tab', { active: userType === 'admin' }]"
          @click="userType = 'admin'"
        >
          管理员登录
        </button>
      </div>
      
      <div class="form-tabs" v-if="userType === 'user'">
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
        <!-- 用户登录表单 -->
        <div v-if="userType === 'user'">
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
        </div>
        
        <!-- 管理员登录表单 -->
        <div v-else-if="userType === 'admin'">
          <div class="form-group">
            <label for="adminUsername">管理员用户名</label>
            <input 
              type="text" 
              id="adminUsername" 
              v-model="form.adminUsername" 
              placeholder="请输入管理员用户名"
              required
            />
          </div>
          
          <div class="form-group">
            <label for="adminPassword">管理员密码</label>
            <input 
              type="password" 
              id="adminPassword" 
              v-model="form.adminPassword" 
              placeholder="请输入管理员密码"
              required
            />
          </div>
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
const userType = ref<'user' | 'admin'>('user')
const loginType = ref<'password' | 'code'>('password')
const loading = ref<boolean>(false)
const countdown = ref<number>(0)
const form = ref({
  phone: '',
  password: '',
  code: '',
  remember: false,
  adminUsername: '',
  adminPassword: ''
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
    if (userType.value === 'user') {
      if (loginType.value === 'password') {
        response = await userApi.login({
          phone: form.value.phone,
          password: form.value.password
        })
      } else {
        response = await userApi.loginWithCode({
          phone: form.value.phone,
          phoneCode: form.value.code,
          remember: form.value.remember
        })
      }
    } else if (userType.value === 'admin') {
      // 调用管理员登录接口
      response = await userApi.adminLogin({
        phone: form.value.adminUsername,
        password: form.value.adminPassword
      })
    }
    
    const data = response as any
    if (data.code === 200) {
      localStorage.setItem('token', data.data?.token || '')
      localStorage.setItem('userId', data.data?.id?.toString() || '')
      localStorage.setItem('userType', userType.value)
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 2rem;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
}

.login-form {
  background-color: #fff;
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
  padding: 2.5rem;
  width: 100%;
  max-width: 450px;
  animation: fadeInUp 0.6s ease-out;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.login-form h2 {
  text-align: center;
  margin-bottom: 2.5rem;
  color: #333;
  font-size: 1.8rem;
  font-weight: 600;
  letter-spacing: 1px;
}

.form-tabs {
  display: flex;
  margin-bottom: 2rem;
  border-bottom: 1px solid #f0f0f0;
}

.tab {
  flex: 1;
  padding: 0.75rem;
  border: none;
  background: none;
  font-size: 1rem;
  color: #666;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.3s ease;
  font-weight: 500;
}

.tab:hover {
  color: #667eea;
}

.tab.active {
  color: #667eea;
  border-bottom-color: #667eea;
  font-weight: 600;
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
  letter-spacing: 0.5px;
}

.form-group input {
  width: 100%;
  padding: 0.9rem 1rem;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 1rem;
  transition: all 0.3s ease;
  background-color: #f9f9f9;
}

.form-group input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
  background-color: #fff;
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
  padding: 0.9rem 1.2rem;
  border: 1px solid #667eea;
  border-radius: 8px;
  background-color: #fff;
  color: #667eea;
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.send-code-btn:hover:not(:disabled) {
  background-color: #667eea;
  color: #fff;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.send-code-btn:disabled {
  border-color: #e0e0e0;
  color: #999;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  font-size: 0.9rem;
  color: #666;
  font-weight: 400;
}

.checkbox-label input {
  width: auto;
  transform: scale(1.1);
}

.submit-btn {
  width: 100%;
  padding: 0.9rem;
  border: none;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  font-size: 1.1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-top: 1.5rem;
  letter-spacing: 1px;
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

.form-footer {
  text-align: center;
  margin-top: 2rem;
  font-size: 0.9rem;
  color: #666;
}

.form-footer a {
  color: #667eea;
  text-decoration: none;
  font-weight: 500;
  transition: color 0.3s ease;
}

.form-footer a:hover {
  color: #764ba2;
  text-decoration: underline;
}

@media (max-width: 768px) {
  .login-container {
    padding: 1rem;
  }
  
  .login-form {
    padding: 2rem;
  }
  
  .login-form h2 {
    font-size: 1.5rem;
  }
  
  .code-input-wrapper {
    flex-direction: column;
  }
  
  .send-code-btn {
    width: 100%;
  }
}
</style>