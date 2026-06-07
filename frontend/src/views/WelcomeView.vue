<script setup>
defineProps({
  user: {
    type: Object,
    default: null
  }
})

defineEmits(['enter-training', 'logout'])
</script>

<template>
  <section class="welcome-view">
    <div class="signal-panel" aria-hidden="true">
      <div class="signal-ring ring-one"></div>
      <div class="signal-ring ring-two"></div>
      <div class="signal-core">
        <span></span>
        <span></span>
        <span></span>
        <span></span>
        <span></span>
      </div>
    </div>

    <div class="welcome-copy">
      <div class="user-strip">
        <span>{{ user?.username || user?.userAccount || 'Learner' }}</span>
        <button type="button" @click="$emit('logout')">Logout</button>
      </div>
      <p class="eyebrow">AI English Speaking Companion</p>
      <h1>Speak first. Think in English next.</h1>
      <p class="intro">
        Pick a real-life scene or design your own roleplay, record your voice, and let the AI answer like a patient English partner.
      </p>
      <button class="primary-action" type="button" @click="$emit('enter-training')">
        <span>Enter conversation</span>
        <strong>-&gt;</strong>
      </button>
    </div>

    <div class="welcome-strip">
      <span>Preset prompts</span>
      <span>Custom scenarios</span>
      <span>AI text + audio reply</span>
    </div>
  </section>
</template>
