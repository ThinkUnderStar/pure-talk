<template>
  <div class="register-container">
    <div class="register-form">
      <h2>注册</h2>
      
      <form @submit.prevent="handleSubmit">
        <div class="form-group">
          <label for="username">用户名</label>
          <input 
            type="text" 
            id="username" 
            v-model="form.username" 
            placeholder="请输入用户名"
            required
          />
        </div>
        
        <div class="form-group">
          <label for="phone">手机号</label>
          <div class="code-input-wrapper">
            <input 
              type="tel" 
              id="phone" 
              v-model="form.phone" 
              placeholder="请输入手机号"
              required
            />
            <button 
              type="button" 
              class="send-code-btn"
              :disabled="countdown.phone > 0"
              @click="sendPhoneCode"
            >
              {{ countdown.phone > 0 ? `${countdown.phone}秒后重发` : '发送验证码' }}
            </button>
          </div>
        </div>
        
        <div class="form-group">
          <label for="phoneCode">手机验证码</label>
          <input 
            type="text" 
            id="phoneCode" 
            v-model="form.phoneCode" 
            placeholder="请输入手机验证码"
            required
          />
        </div>
        
        <div class="form-group">
          <label for="email">邮箱</label>
          <div class="code-input-wrapper">
            <input 
              type="email" 
              id="email" 
              v-model="form.email" 
              placeholder="请输入邮箱"
              required
            />
            <button 
              type="button" 
              class="send-code-btn"
              :disabled="countdown.email > 0"
              @click="sendEmailCode"
            >
              {{ countdown.email > 0 ? `${countdown.email}秒后重发` : '发送验证码' }}
            </button>
          </div>
        </div>
        
        <div class="form-group">
          <label for="emailCode">邮箱验证码</label>
          <input 
            type="text" 
            id="emailCode" 
            v-model="form.emailCode" 
            placeholder="请输入邮箱验证码"
            required
          />
        </div>
        
        <div class="form-group">
          <label for="password">密码</label>
          <input 
            type="password" 
            id="password" 
            v-model="form.password" 
            placeholder="请输入密码"
            required
          />
        </div>
        
        <button type="submit" class="submit-btn" :disabled="loading">
          {{ loading ? '注册中...' : '注册' }}
        </button>
        
        <div class="form-footer">
          <router-link to="/login">已有账号？去登录</router-link>
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
const loading = ref<boolean>(false)
const countdown = ref({
  phone: 0,
  email: 0
})
const form = ref({
  username: '',
  phone: '',
  phoneCode: '',
  email: '',
  emailCode: '',
  password: ''
})

const sendPhoneCode = debounce(async () => {
  if (!form.value.phone) {
    alert('请输入手机号')
    return
  }
  
  try {
    const response = await userApi.sendRegisterPhoneCode(form.value.phone)
    const data = response as any
    if (data.code === 200) {
      alert('验证码已发送')
      startCountdown('phone')
    } else {
      alert(data.message || '发送失败')
    }
  } catch (error) {
    console.error('发送验证码失败:', error)
    alert('发送验证码失败')
  }
}, 1000)

const sendEmailCode = debounce(async () => {
  if (!form.value.email) {
    alert('请输入邮箱')
    return
  }
  
  try {
    const response = await userApi.sendRegisterEmailCode(form.value.email)
    const data = response as any
    if (data.code === 200) {
      alert('验证码已发送')
      startCountdown('email')
    } else {
      alert(data.message || '发送失败')
    }
  } catch (error) {
    console.error('发送验证码失败:', error)
    alert('发送验证码失败')
  }
}, 1000)

const startCountdown = (type: 'phone' | 'email') => {
  countdown.value[type] = 60
  const timer = setInterval(() => {
    if (countdown.value[type] > 0) {
      countdown.value[type]--
    } else {
      clearInterval(timer)
    }
  }, 1000)
}

const handleSubmit = async () => {
  // 先验证手机验证码
  try {
    const phoneCodeResponse = await userApi.validatePhoneCode({
      phone: form.value.phone,
      code: form.value.phoneCode
    })
    const phoneData = phoneCodeResponse as any
    if (phoneData.code !== 200) {
      alert('手机验证码有误')
      return
    }
    
    // 再验证邮箱验证码
    const emailCodeResponse = await userApi.validateEmailCode({
      email: form.value.email,
      code: form.value.emailCode
    })
    const emailData = emailCodeResponse as any
    if (emailData.code !== 200) {
      alert('邮箱验证码有误')
      return
    }
    
    // 最后注册
    loading.value = true
    const registerResponse = await userApi.register({
      username: form.value.username,
      password: form.value.password,
      phone: form.value.phone,
      email: form.value.email
    })
    const registerData = registerResponse as any
    if (registerData.code === 200) {
      alert('注册成功')
      router.push('/login')
    } else {
      alert(registerData.message || '注册失败')
    }
  } catch (error) {
    console.error('注册失败:', error)
    alert('注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f5f5f5;
  padding: 2rem;
}

.register-form {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  padding: 2rem;
  width: 100%;
  max-width: 450px;
}

.register-form h2 {
  text-align: center;
  margin-bottom: 2rem;
  color: #333;
  font-size: 1.5rem;
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
  .register-container {
    padding: 1rem;
  }
  
  .register-form {
    padding: 1.5rem;
  }
  
  .code-input-wrapper {
    flex-direction: column;
  }
  
  .send-code-btn {
    width: 100%;
  }
}
</style>