<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import ConversationStage from '../components/ConversationStage.vue'
import RecorderDock from '../components/RecorderDock.vue'
import SceneRail from '../components/SceneRail.vue'
import TranscriptReview from '../components/TranscriptReview.vue'
import { customSceneTemplate, presetScenes, scenes } from '../services/scenes'
import {
  createSessionId,
  fetchConversationSummary,
  fetchSceneOpening,
  getOrCreateSessionId,
  sendConfirmedTextMessage,
  transcribeVoice
} from '../services/voiceService'

const props = defineProps({
  user: {
    type: Object,
    default: null
  }
})

defineEmits(['back', 'logout'])

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
const isOpening = ref(false)
const isSending = ref(false)
const isTranscribing = ref(false)
const isSummarizing = ref(false)
const conversationEnded = ref(false)
const errorMessage = ref('')
const summaryError = ref('')
const conversationSummary = ref(null)
const pendingTranscript = ref('')
const audioLevel = ref(0)
const sessionId = ref(getOrCreateSessionId())
const mediaRecorder = ref(null)
const audioChunks = ref([])
const streamRef = ref(null)
const meterTimer = ref(null)
const openingRequestId = ref(0)

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
  !isOpening.value &&
  !isSending.value &&
  !isTranscribing.value &&
  !isSummarizing.value &&
  !pendingTranscript.value &&
  !conversationEnded.value &&
  (!isCustomScene.value || customScenario.value.trim().length > 0)
))
const recordLabel = computed(() => {
  if (isRecording.value) return 'Stop'
  if (isOpening.value) return 'Opening'
  if (isTranscribing.value) return 'Texting'
  if (isSending.value) return 'Sending'
  if (isSummarizing.value) return 'Reviewing'
  if (conversationEnded.value) return 'Ended'
  return 'Record'
})

function selectScene(sceneId) {
  activeSceneId.value = sceneId
  startNewScene()
}

function resetConversation() {
  startNewScene()
}

function startNewScene() {
  sessionId.value = createSessionId()
  errorMessage.value = ''
  summaryError.value = ''
  conversationSummary.value = null
  pendingTranscript.value = ''
  conversationEnded.value = false
  requestSceneOpening()
}

async function toggleRecording() {
  if (conversationEnded.value) {
    errorMessage.value = 'This conversation has ended. Start a new session to keep practicing.'
    return
  }
  if (!canRecord.value) {
    errorMessage.value = isOpening.value
      ? 'Please wait for the AI opening question.'
      : pendingTranscript.value
        ? 'Confirm or discard the transcript before recording again.'
        : 'Please write a custom scene before recording.'
    return
  }
  if (isRecording.value) {
    stopRecording()
    return
  }
  await startRecording()
}

async function requestSceneOpening() {
  const requestId = openingRequestId.value + 1
  openingRequestId.value = requestId
  isOpening.value = true
  errorMessage.value = ''
  messages.value = [
    {
      role: 'ai',
      text: `Preparing ${activeScene.value.name}...`,
      audioUrl: ''
    }
  ]

  try {
    const data = await fetchSceneOpening({
      sessionId: sessionId.value,
      sceneId: activeSceneId.value,
      customScenario: isCustomScene.value ? customScenario.value : ''
    })

    if (requestId !== openingRequestId.value) return

    messages.value = [
      {
        role: 'ai',
        text: data?.aiText || activeScene.value.opening,
        audioUrl: data?.aiAudioUrl || '',
        autoPlay: true
      }
    ]
  } catch (error) {
    if (requestId !== openingRequestId.value) return

    messages.value = [
      {
        role: 'ai',
        text: activeScene.value.opening,
        audioUrl: ''
      }
    ]
    errorMessage.value = error.response?.data?.message || error.message || 'Scene opening failed. You can still record your answer.'
  } finally {
    if (requestId === openingRequestId.value) {
      isOpening.value = false
    }
  }
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
      await transcribeRecording(mimeType || 'audio/webm')
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

async function transcribeRecording(mimeType) {
  if (!audioChunks.value.length) return
  isTranscribing.value = true
  errorMessage.value = ''

  const audioBlob = new Blob(audioChunks.value, { type: mimeType })

  try {
    const data = await transcribeVoice({
      audioBlob,
      mimeType,
      sessionId: sessionId.value
    })

    pendingTranscript.value = data?.userText || ''
    if (!pendingTranscript.value.trim()) {
      errorMessage.value = 'No speech text was recognized. Please record again.'
    }
  } catch (error) {
    errorMessage.value = error.response?.data?.message || error.message || 'Transcription failed. Please try again.'
  } finally {
    isTranscribing.value = false
    audioChunks.value = []
    await nextTick()
  }
}

async function confirmTranscript() {
  const userText = pendingTranscript.value.trim()
  if (!userText) {
    errorMessage.value = 'Transcript cannot be empty.'
    return
  }

  isSending.value = true
  errorMessage.value = ''
  messages.value.push({
    role: 'user',
    text: userText,
    audioUrl: ''
  })

  try {
    const data = await sendConfirmedTextMessage({
      userText,
      sessionId: sessionId.value,
      sceneId: activeSceneId.value,
      customScenario: isCustomScene.value ? customScenario.value : ''
    })

    pendingTranscript.value = ''
    messages.value.push({
      role: 'ai',
      text: data?.aiText || '',
      audioUrl: data?.aiAudioUrl || ''
    })
  } catch (error) {
    errorMessage.value = error.response?.data?.message || error.message || 'AI response failed. Please try again.'
  } finally {
    isSending.value = false
    await nextTick()
  }
}

function cancelTranscript() {
  pendingTranscript.value = ''
  errorMessage.value = ''
}

async function generateSummary() {
  if (isSending.value || isRecording.value || isTranscribing.value || pendingTranscript.value) {
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

onMounted(() => {
  requestSceneOpening()
})
</script>

<template>
  <section class="training-view">
    <SceneRail
      v-model:custom-scenario="customScenario"
      :active-scene="activeScene"
      :active-scene-id="activeSceneId"
      :conversation-ended="conversationEnded"
      :is-opening="isOpening"
      :is-custom-scene="isCustomScene"
      :is-recording="isRecording"
      :is-sending="isSending"
      :is-summarizing="isSummarizing"
      :scenes="scenes"
      :user="props.user"
      @back="$emit('back')"
      @generate-summary="generateSummary"
      @logout="$emit('logout')"
      @reset-conversation="resetConversation"
      @select-scene="selectScene"
    />

    <ConversationStage
      :active-scene="activeScene"
      :conversation-ended="conversationEnded"
      :conversation-summary="conversationSummary"
      :is-opening="isOpening"
      :is-sending="isSending"
      :is-transcribing="isTranscribing"
      :is-summarizing="isSummarizing"
      :messages="messages"
      :session-id="sessionId"
      :summary-error="summaryError"
    >
      <TranscriptReview
        v-if="pendingTranscript"
        v-model:transcript="pendingTranscript"
        :is-sending="isSending"
        @cancel="cancelTranscript"
        @confirm="confirmTranscript"
      />

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
