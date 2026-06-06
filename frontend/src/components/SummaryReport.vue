<script setup>
import { computed } from 'vue'

const props = defineProps({
  conversationEnded: {
    type: Boolean,
    default: false
  },
  isSummarizing: {
    type: Boolean,
    default: false
  },
  summary: {
    type: Object,
    default: null
  },
  summaryError: {
    type: String,
    default: ''
  }
})

const summaryScores = computed(() => {
  if (!props.summary) return []
  return [
    { label: 'Overall', value: props.summary.totalScore },
    { label: 'Fluency', value: props.summary.fluencyScore },
    { label: 'Pronunciation', value: props.summary.pronunciationScore },
    { label: 'Grammar', value: props.summary.grammarScore }
  ].filter((item) => item.value !== undefined && item.value !== null)
})
</script>

<template>
  <section v-if="summary || isSummarizing || summaryError" class="summary-report">
    <div class="summary-report-header">
      <div>
        <p class="eyebrow">Session Report</p>
        <h2>Practice review</h2>
      </div>
      <span class="report-status">{{ isSummarizing ? 'Building' : conversationEnded ? 'Closed' : 'Ready' }}</span>
    </div>

    <div v-if="isSummarizing" class="summary-loading">
      <span></span>
      <span></span>
      <span></span>
    </div>

    <p v-else-if="summaryError" class="summary-error">{{ summaryError }}</p>

    <template v-else>
      <div class="score-grid">
        <div v-for="score in summaryScores" :key="score.label" class="score-tile">
          <strong>{{ score.value }}</strong>
          <span>{{ score.label }}</span>
        </div>
      </div>

      <div class="summary-block">
        <span>Summary</span>
        <p>{{ summary.summary }}</p>
      </div>

      <div v-if="summary.corrections.length" class="summary-block">
        <span>Corrections</span>
        <ul>
          <li v-for="(correction, index) in summary.corrections" :key="`correction-${index}`">
            {{ correction }}
          </li>
        </ul>
      </div>

      <div v-if="summary.suggestions.length" class="summary-block">
        <span>Next practice</span>
        <ul>
          <li v-for="(suggestion, index) in summary.suggestions" :key="`suggestion-${index}`">
            {{ suggestion }}
          </li>
        </ul>
      </div>
    </template>
  </section>
</template>
