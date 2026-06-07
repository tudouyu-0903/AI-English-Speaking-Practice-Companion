<script setup>
import { nextTick, ref, watch } from 'vue'
import SummaryReport from './SummaryReport.vue'

const props = defineProps({
  activeScene: {
    type: Object,
    required: true
  },
  conversationEnded: {
    type: Boolean,
    default: false
  },
  conversationSummary: {
    type: Object,
    default: null
  },
  isOpening: {
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
  messages: {
    type: Array,
    required: true
  },
  sessionId: {
    type: String,
    required: true
  },
  summaryError: {
    type: String,
    default: ''
  }
})

const threadEl = ref(null)

watch(
  () => [
    props.messages.length,
    props.isOpening,
    props.isSending,
    props.isSummarizing,
    props.conversationSummary,
    props.summaryError
  ],
  async () => {
    await nextTick()
    if (threadEl.value) {
      threadEl.value.scrollTop = threadEl.value.scrollHeight
    }
  },
  { deep: true }
)
</script>

<template>
  <div class="conversation-stage">
    <header class="stage-header">
      <div>
        <p class="eyebrow">{{ activeScene.place }}</p>
        <h1>{{ activeScene.opening }}</h1>
      </div>
      <div class="session-chip">{{ sessionId.slice(0, 18) }}</div>
    </header>

    <div ref="threadEl" class="message-thread">
      <article
        v-for="(message, index) in messages"
        :key="`${message.role}-${index}`"
        class="message"
        :class="message.role"
      >
        <div class="avatar">{{ message.role === 'ai' ? 'AI' : 'YOU' }}</div>
        <div class="bubble">
          <p>{{ message.text }}</p>
          <audio v-if="message.audioUrl" controls :src="message.audioUrl" :autoplay="message.autoPlay"></audio>
        </div>
      </article>

      <article v-if="isOpening || isSending" class="message ai">
        <div class="avatar">AI</div>
        <div class="bubble thinking">
          <span></span>
          <span></span>
          <span></span>
        </div>
      </article>

      <SummaryReport
        :conversation-ended="conversationEnded"
        :is-summarizing="isSummarizing"
        :summary="conversationSummary"
        :summary-error="summaryError"
      />
    </div>

    <slot name="recorder"></slot>
  </div>
</template>
