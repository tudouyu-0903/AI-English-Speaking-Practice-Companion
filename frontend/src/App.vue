<script setup>
import axios from 'axios'
import { computed, nextTick, ref } from 'vue'

const scenes = [
  {
    id: 'cafe',
    name: 'Cafe Check-in',
    place: 'London morning cafe',
    goal: 'Order a drink, ask about the menu, and keep small talk going.',
    opening: 'You walk into a busy cafe before work. Start by greeting the barista.'
  },
  {
    id: 'airport',
    name: 'Airport Help Desk',
    place: 'International airport',
    goal: 'Ask for help, clarify directions, and respond politely under pressure.',
    opening: 'Your flight gate changed. Ask the staff what you should do next.'
  },
  {
    id: 'interview',
    name: 'First Interview',
    place: 'Remote job interview',
    goal: 'Introduce yourself, describe experience, and answer follow-up questions.',
    opening: 'The interviewer asks you to briefly tell them about yourself.'
  },
  {
    id: 'neighbor',
    name: 'New Neighbor',
    place: 'Apartment hallway',
    goal: 'Make a friendly introduction and arrange a casual future chat.',
    opening: 'You meet a neighbor for the first time while checking your mail.'
  }
]

const view = ref('welcome')
const activeSceneId = ref('cafe')
const messages = ref([
  {
    role: 'ai',
    text: "Welcome. Choose a scene, press record, and speak in English when you're ready.",
    audioUrl: ''
  }
])
const isRecording = ref(false)
const isSending = ref(false)
const errorMessage = ref('')
const audioLevel = ref(0)
const sessionId = ref(getOrCreateSessionId())
const mediaRecorder = ref(null)
const audioChunks = ref([])
const streamRef = ref(null)
const meterTimer = ref(null)
const threadEl = ref(null)

const activeScene = computed(() => scenes.find((scene) => scene.id === activeSceneId.value) || scenes[0])
const canRecord = computed(() => !isSending.value)

function getOrCreateSessionId() {
  const saved = localStorage.getItem('english_voice_session_id')
  if (saved) return saved
  const id = `session-${crypto.randomUUID()}`
  localStorage.setItem('english_voice_session_id', id)
  return id
}

function enterTraining() {
  view.value = 'training'
  nextTick(scrollToBottom)
}

function resetConversation() {
  sessionId.value = `session-${crypto.randomUUID()}`
  localStorage.setItem('english_voice_session_id', sessionId.value)
  messages.value = [
    {
      role: 'ai',
      text: `New scene loaded: ${activeScene.value.name}. ${activeScene.value.opening}`,
      audioUrl: ''
    }
  ]
  errorMessage.value = ''
}

async function toggleRecording() {
  if (!canRecord.value) return
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
    errorMessage.value = '无法访问麦克风，请检查浏览器权限后再试。'
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

  const extension = mimeType.includes('mp4') ? 'm4a' : mimeType.includes('wav') ? 'wav' : 'webm'
  const blob = new Blob(audioChunks.value, { type: mimeType })
  const formData = new FormData()
  formData.append('audioFile', blob, `voice-${Date.now()}.${extension}`)
  formData.append('sessionId', sessionId.value)

  messages.value.push({
    role: 'user',
    text: `Voice message sent in ${activeScene.value.name}`,
    audioUrl: ''
  })
  await nextTick(scrollToBottom)

  try {
    const response = await axios.post('/api/voice/file-chat', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })

    if (response.data?.code !== 0) {
      throw new Error(response.data?.message || 'AI response failed')
    }

    messages.value.push({
      role: 'ai',
      text: response.data.data?.aiText || '',
      audioUrl: response.data.data?.aiAudioUrl || ''
    })
  } catch (error) {
    errorMessage.value = error.response?.data?.message || error.message || '发送失败，请稍后重试。'
  } finally {
    isSending.value = false
    audioChunks.value = []
    await nextTick(scrollToBottom)
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

function scrollToBottom() {
  if (threadEl.value) {
    threadEl.value.scrollTop = threadEl.value.scrollHeight
  }
}
</script>

<template>
  <main class="app-shell">
    <section v-if="view === 'welcome'" class="welcome-view">
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
        <p class="eyebrow">AI English Speaking Companion</p>
        <h1>Speak first. Think in English next.</h1>
        <p class="intro">
          Pick a real-life scene, record your voice, and let the AI answer like a patient English partner.
        </p>
        <button class="primary-action" type="button" @click="enterTraining">
          <span>Enter conversation</span>
          <strong>↗</strong>
        </button>
      </div>

      <div class="welcome-strip">
        <span>Scenario practice</span>
        <span>Voice upload</span>
        <span>AI text + audio reply</span>
      </div>
    </section>

    <section v-else class="training-view">
      <aside class="scene-rail">
        <button class="back-button" type="button" @click="view = 'welcome'">←</button>
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
            @click="activeSceneId = scene.id"
          >
            <span>{{ scene.name }}</span>
            <small>{{ scene.place }}</small>
          </button>
        </div>

        <div class="mission">
          <span>Goal</span>
          <p>{{ activeScene.goal }}</p>
        </div>

        <button class="ghost-action" type="button" @click="resetConversation">New session</button>
      </aside>

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
              <audio v-if="message.audioUrl" controls :src="message.audioUrl"></audio>
            </div>
          </article>
          <article v-if="isSending" class="message ai">
            <div class="avatar">AI</div>
            <div class="bubble thinking">
              <span></span>
              <span></span>
              <span></span>
            </div>
          </article>
        </div>

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
            :disabled="!canRecord"
            @click="toggleRecording"
          >
            <span>{{ isRecording ? 'Stop' : isSending ? 'Sending' : 'Record' }}</span>
          </button>
          <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>
        </footer>
      </div>
    </section>
  </main>
</template>
