<script setup>
defineProps({
  audioLevel: {
    type: Number,
    default: 0
  },
  canRecord: {
    type: Boolean,
    default: false
  },
  errorMessage: {
    type: String,
    default: ''
  },
  isRecording: {
    type: Boolean,
    default: false
  },
  recordLabel: {
    type: String,
    required: true
  }
})

defineEmits(['toggle-recording'])
</script>

<template>
  <footer class="recorder-dock">
    <div class="wave-meter" :class="{ active: isRecording }">
      <span
        v-for="bar in 16"
        :key="bar"
        :style="{ height: `${isRecording ? Math.max(12, (audioLevel + bar * 7) % 82) : 18}px` }"
      ></span>
    </div>
    <button
      class="record-button"
      :class="{ recording: isRecording }"
      type="button"
      :disabled="!canRecord && !isRecording"
      @click="$emit('toggle-recording')"
    >
      <span>{{ recordLabel }}</span>
    </button>
    <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>
  </footer>
</template>
