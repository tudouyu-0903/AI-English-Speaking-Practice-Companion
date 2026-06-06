<script setup>
import { ref } from 'vue'
import TrainingView from './views/TrainingView.vue'
import WelcomeView from './views/WelcomeView.vue'

const view = ref('welcome')

function enterTraining() {
  view.value = 'training'
}
</script>

<template>
  <main class="app-shell">
    <WelcomeView v-if="view === 'welcome'" @enter-training="enterTraining" />
    <TrainingView v-else @back="view = 'welcome'" />
  </main>
</template>
