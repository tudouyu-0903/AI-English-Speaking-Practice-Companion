<script setup>
defineProps({
  isSending: {
    type: Boolean,
    default: false
  },
  transcript: {
    type: String,
    required: true
  }
})

defineEmits(['cancel', 'confirm', 'update:transcript'])
</script>

<template>
  <section class="transcript-review">
    <div class="transcript-header">
      <div>
        <p class="eyebrow">Check Your Words</p>
        <h2>Confirm what you said</h2>
      </div>
      <span>{{ isSending ? 'Sending' : 'Review' }}</span>
    </div>

    <textarea
      :value="transcript"
      rows="4"
      :disabled="isSending"
      @input="$emit('update:transcript', $event.target.value)"
    ></textarea>

    <div class="transcript-actions">
      <button class="ghost-action" type="button" :disabled="isSending" @click="$emit('cancel')">Record again</button>
      <button class="confirm-action" type="button" :disabled="isSending || !transcript.trim()" @click="$emit('confirm')">
        {{ isSending ? 'Sending to AI' : 'Confirm & reply' }}
      </button>
    </div>
  </section>
</template>
