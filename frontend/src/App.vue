<script setup>
import { ref } from 'vue'
import { getStoredAuth, logoutUser } from './services/authService'
import AuthView from './views/AuthView.vue'
import TrainingView from './views/TrainingView.vue'
import WelcomeView from './views/WelcomeView.vue'

const auth = ref(getStoredAuth())
const view = ref(auth.value ? 'welcome' : 'auth')

function enterTraining() {
  view.value = 'training'
}

function handleAuthenticated(nextAuth) {
  auth.value = nextAuth
  view.value = 'welcome'
}

async function handleLogout() {
  await logoutUser()
  auth.value = null
  view.value = 'auth'
}
</script>

<template>
  <main class="app-shell">
    <AuthView v-if="view === 'auth'" @authenticated="handleAuthenticated" />
    <WelcomeView
      v-else-if="view === 'welcome'"
      :user="auth"
      @enter-training="enterTraining"
      @logout="handleLogout"
    />
    <TrainingView v-else :user="auth" @back="view = 'welcome'" @logout="handleLogout" />
  </main>
</template>
