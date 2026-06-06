<script setup>
import { computed, nextTick, ref } from 'vue'
import ConversationStage from '../components/ConversationStage.vue'
import RecorderDock from '../components/RecorderDock.vue'
import SceneRail from '../components/SceneRail.vue'
import { customSceneTemplate, presetScenes, scenes } from '../services/scenes'
import {
  createSessionId,
  fetchConversationSummary,
  getOrCreateSessionId,
  sendVoiceMessage
} from '../services/voiceService'

defineEmits(['back'])

const activeSceneId = ref('order_food')
const customScenario = ref('An IELTS examiner testing my speaking skills with follow-up questions about daily life.')
const messages = ref([
  {
    role: 'ai',
    text: "Welcome. Choose a scene, press record, and speak in English when you're ready.",
    audioUrl: ''
  }
])
const isRecording = ref(false)
const isSending = ref(false)
const isSummarizing = ref(false)
const conversationEnded = ref(false)
const errorMessage = ref('')
const summaryError = ref('')
const conversationSummary = ref(null)
const audioLevel = ref(0)
const sessionId = ref(getOrCreateSessionId())
const mediaRecorder = ref(null)
const audioChunks = ref([])
const streamRef = ref(null)
const meterTimer = ref(null)

const isCustomScene = computed(() => activeSceneId.value === 'custom')
const activeScene = computed(() => {
  if (isCustomScene.value) {
    return {
      ...customSceneTemplate,
      opening: customScenario.value.trim() || customSceneTemplate.opening
    }
  }
  return presetScenes.find((scene) => scene.id === activeSceneId.value) || presetScenes[0]
})
const canRecord = computed(() => (
  !isSending.value &&
  !isSummarizing.value &&
  !conversationEnded.value &&
  (!isCustomScene.value || customScenario.value.trim().length > 0)
))
const recordLabel = computed(() => {
  if (isRecording.value) return 'Stop'
  if (isSending.value) return 'Sending'
  if (isSummarizing.value) return 'Reviewing'
  if (conversationEnded.value) return 'Ended'
  return 'Record'
})

function selectScene(sceneId) {
  activeSceneId.value = sceneId
  errorMessage.value = ''
}

function resetConversation() {
  sessionId.value = createSessionId()
  messages.value = [
    {
      role: 'ai',
      text: `New scene loaded: ${activeScene.value.name}. ${activeScene.value.opening}`,
      audioUrl: ''
    }
  ]
  errorMessage.value = ''
  summaryError.value = ''
  conversationSummary.value = null
  conversationEnded.value = false
}

async function toggleRecording() {
  if (conversationEnded.value) {
    errorMessage.value = 'This conversation has ended. Start a new session to keep practicing.'
    return
  }
  if (!canRecord.value) {
    errorMessage.value = 'Please write a custom scene before recording.'
    return
  }
  if (isRecording.value) {
    stopRecording()
    return
  }
  await startRecording()
}

async function startRecording() {
  errorMessage.value = ''
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    streamRef.value = stream
    audioChunks.value = []
    const mimeType = getSupportedMimeType()
    const recorder = new MediaRecorder(stream, mimeType ? { mimeType } : undefined)

    recorder.ondataavailable = (event) => {
      if (event.data.size > 0) audioChunks.value.push(event.data)
    }

    recorder.onstop = async () => {
      stopMeter()
      stream.getTracks().forEach((track) => track.stop())
      streamRef.value = null
      await sendRecording(mimeType || 'audio/webm')
    }

    mediaRecorder.value = recorder
    recorder.start()
    isRecording.value = true
    startMeter()
  } catch (error) {
    errorMessage.value = 'Unable to access the microphone. Please check browser permissions and try again.'
  }
}

function stopRecording() {
  if (mediaRecorder.value && mediaRecorder.value.state !== 'inactive') {
    mediaRecorder.value.stop()
  }
  isRecording.value = false
}

function getSupportedMimeType() {
  const types = ['audio/webm;codecs=opus', 'audio/webm', 'audio/mp4', 'audio/wav']
  return types.find((type) => MediaRecorder.isTypeSupported(type))
}

async function sendRecording(mimeType) {
  if (!audioChunks.value.length) return
  isSending.value = true
  errorMessage.value = ''

  const audioBlob = new Blob(audioChunks.value, { type: mimeType })
  messages.value.push({
    role: 'user',
    text: `Voice message sent in ${activeScene.value.name}`,
    audioUrl: ''
  })

  try {
    const data = await sendVoiceMessage({
      audioBlob,
      mimeType,
      sessionId: sessionId.value,
      sceneId: activeSceneId.value,
      customScenario: isCustomScene.value ? customScenario.value : ''
    })

    messages.value.push({
      role: 'ai',
      text: data?.aiText || '',
      audioUrl: data?.aiAudioUrl || ''
    })
  } catch (error) {
    errorMessage.value = error.response?.data?.message || error.message || 'Send failed. Please try again.'
  } finally {
    isSending.value = false
    audioChunks.value = []
    await nextTick()
  }
}

async function generateSummary() {
  if (isSending.value || isRecording.value) {
    summaryError.value = 'Please wait until the current voice message is finished.'
    return
  }

  isSummarizing.value = true
  conversationEnded.value = true
  errorMessage.value = ''
  summaryError.value = ''
  conversationSummary.value = null

  try {
    conversationSummary.value = await fetchConversationSummary(sessionId.value)
  } catch (error) {
    conversationEnded.value = false
    summaryError.value = error.response?.data?.message || error.message || 'Summary failed. Please try again.'
  } finally {
    isSummarizing.value = false
  }
}

function startMeter() {
  stopMeter()
  meterTimer.value = window.setInterval(() => {
    audioLevel.value = 20 + Math.round(Math.random() * 80)
  }, 120)
}

function stopMeter() {
  if (meterTimer.value) window.clearInterval(meterTimer.value)
  meterTimer.value = null
  audioLevel.value = 0
}
</script>

<template>
  <section class="training-view">
    <SceneRail
      v-model:custom-scenario="customScenario"
      :active-scene="activeScene"
      :active-scene-id="activeSceneId"
      :conversation-ended="conversationEnded"
      :is-custom-scene="isCustomScene"
      :is-recording="isRecording"
      :is-sending="isSending"
      :is-summarizing="isSummarizing"
      :scenes="scenes"
      @back="$emit('back')"
      @generate-summary="generateSummary"
      @reset-conversation="resetConversation"
      @select-scene="selectScene"
    />

    <ConversationStage
      :active-scene="activeScene"
      :conversation-ended="conversationEnded"
      :conversation-summary="conversationSummary"
      :is-sending="isSending"
      :is-summarizing="isSummarizing"
      :messages="messages"
      :session-id="sessionId"
      :summary-error="summaryError"
    >
      <template #recorder>
        <RecorderDock
          :audio-level="audioLevel"
          :can-record="canRecord"
          :error-message="errorMessage"
          :is-recording="isRecording"
          :record-label="recordLabel"
          @toggle-recording="toggleRecording"
        />
      </template>
    </ConversationStage>
  </section>
</template>
