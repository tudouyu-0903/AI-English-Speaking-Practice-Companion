<script setup>
import { computed, ref } from 'vue'
import { loginUser, registerUser } from '../services/authService'

const emit = defineEmits(['authenticated'])

const mode = ref('login')
const isSubmitting = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const form = ref({
  username: '',
  userAccount: '',
  password: '',
  phone: '',
  email: ''
})

const isRegisterMode = computed(() => mode.value === 'register')
const actionLabel = computed(() => {
  if (isSubmitting.value) return isRegisterMode.value ? 'Creating account' : 'Signing in'
  return isRegisterMode.value ? 'Create account' : 'Sign in'
})

function switchMode(nextMode) {
  mode.value = nextMode
  errorMessage.value = ''
  successMessage.value = ''
}

async function submitAuth() {
  errorMessage.value = ''
  successMessage.value = ''
  isSubmitting.value = true

  try {
    if (isRegisterMode.value) {
      await registerUser(form.value)
      successMessage.value = 'Account created. Signing you in now.'
    }

    const auth = await loginUser({
      userAccount: form.value.userAccount,
      password: form.value.password
    })

    emit('authenticated', auth)
  } catch (error) {
    errorMessage.value = error.response?.data?.message || error.message || 'Authentication failed. Please try again.'
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <section class="auth-view">
    <div class="auth-signal" aria-hidden="true">
      <div class="auth-orbit"></div>
      <div class="auth-orbit second"></div>
      <div class="auth-pulse">
        <span></span>
        <span></span>
        <span></span>
      </div>
    </div>

    <div class="auth-copy">
      <p class="eyebrow">Private speaking room</p>
      <h1>Save every practice voice.</h1>
      <p class="intro">
        Sign in before training so your conversation history, summary scores, and correction reports stay attached to your account.
      </p>
    </div>

    <form class="auth-panel" @submit.prevent="submitAuth">
      <div class="auth-tabs" role="tablist" aria-label="Authentication mode">
        <button
          type="button"
          :class="{ active: mode === 'login' }"
          @click="switchMode('login')"
        >
          Login
        </button>
        <button
          type="button"
          :class="{ active: mode === 'register' }"
          @click="switchMode('register')"
        >
          Register
        </button>
      </div>

      <label v-if="isRegisterMode" class="field-control">
        <span>Username</span>
        <input v-model.trim="form.username" type="text" autocomplete="name" required />
      </label>

      <label class="field-control">
        <span>Account</span>
        <input v-model.trim="form.userAccount" type="text" autocomplete="username" minlength="4" required />
      </label>

      <label class="field-control">
        <span>Password</span>
        <input v-model="form.password" type="password" autocomplete="current-password" minlength="6" required />
      </label>

      <div v-if="isRegisterMode" class="optional-grid">
        <label class="field-control">
          <span>Phone</span>
          <input v-model.trim="form.phone" type="tel" autocomplete="tel" />
        </label>

        <label class="field-control">
          <span>Email</span>
          <input v-model.trim="form.email" type="email" autocomplete="email" />
        </label>
      </div>

      <button class="auth-submit" type="submit" :disabled="isSubmitting">
        {{ actionLabel }}
      </button>

      <p v-if="successMessage" class="auth-success">{{ successMessage }}</p>
      <p v-if="errorMessage" class="auth-error">{{ errorMessage }}</p>
    </form>
  </section>
</template>
