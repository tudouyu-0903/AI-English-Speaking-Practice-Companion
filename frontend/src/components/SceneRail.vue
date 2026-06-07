<script setup>
defineProps({
  activeScene: {
    type: Object,
    required: true
  },
  activeSceneId: {
    type: String,
    required: true
  },
  conversationEnded: {
    type: Boolean,
    default: false
  },
  customScenario: {
    type: String,
    required: true
  },
  isCustomScene: {
    type: Boolean,
    default: false
  },
  isRecording: {
    type: Boolean,
    default: false
  },
  isSending: {
    type: Boolean,
    default: false
  },
  isSummarizing: {
    type: Boolean,
    default: false
  },
  scenes: {
    type: Array,
    required: true
  },
  user: {
    type: Object,
    default: null
  }
})

defineEmits(['back', 'generate-summary', 'logout', 'reset-conversation', 'select-scene', 'update:customScenario'])
</script>

<template>
  <aside class="scene-rail">
    <button class="back-button" type="button" aria-label="Back to welcome" @click="$emit('back')">&lt;-</button>
    <div class="rail-user">
      <div>
        <span>Signed in</span>
        <strong>{{ user?.username || user?.userAccount || 'Learner' }}</strong>
      </div>
      <button type="button" @click="$emit('logout')">Logout</button>
    </div>
    <div>
      <p class="eyebrow">Training Scene</p>
      <h2>{{ activeScene.name }}</h2>
    </div>

    <div class="scene-list">
      <button
        v-for="scene in scenes"
        :key="scene.id"
        class="scene-item"
        :class="{ active: scene.id === activeSceneId }"
        type="button"
        @click="$emit('select-scene', scene.id)"
      >
        <span>{{ scene.name }}</span>
        <small>{{ scene.place }}</small>
      </button>
    </div>

    <label v-if="isCustomScene" class="custom-scene-box">
      <span>Custom prompt</span>
      <textarea
        :value="customScenario"
        rows="7"
        maxlength="500"
        placeholder="Example: A hotel receptionist helping me change a booking after my flight was delayed."
        @input="$emit('update:customScenario', $event.target.value)"
      ></textarea>
      <small>{{ customScenario.trim().length }}/500 characters</small>
    </label>

    <div class="mission">
      <span>Goal</span>
      <p>{{ activeScene.goal }}</p>
    </div>

    <div class="summary-actions">
      <button
        class="summary-action"
        type="button"
        :disabled="isSending || isRecording || isSummarizing || conversationEnded"
        @click="$emit('generate-summary')"
      >
        {{ isSummarizing ? 'Generating report' : conversationEnded ? 'Conversation ended' : 'End & summarize' }}
      </button>
      <small>Creates a coaching report for this session.</small>
    </div>

    <button class="ghost-action" type="button" @click="$emit('reset-conversation')">New session</button>
  </aside>
</template>
