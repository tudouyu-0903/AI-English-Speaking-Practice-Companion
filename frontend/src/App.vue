<script setup>
import axios from 'axios'
import { computed, nextTick, ref } from 'vue'

const presetScenes = [
  {
    id: 'order_food',
    name: 'Diner Order',
    place: 'Star Bites diner',
    goal: 'Order drinks, choose food, ask for details, and keep the exchange natural.',
    opening: 'The waiter greets you at a busy American diner and asks what you would like to drink.'
  },
  {
    id: 'check_in',
    name: 'Airport Check-in',
    place: 'Global Airways counter',
    goal: 'Share booking details, talk about baggage, and answer check-in questions politely.',
    opening: 'The ground crew agent asks for your passport and booking details.'
  },
  {
    id: 'interview',
    name: 'Tech Interview',
    place: 'TechGen hiring room',
    goal: 'Introduce yourself, explain experience, and answer one interview question at a time.',
    opening: 'The HR manager introduces the interview and asks the first question.'
  },
  {
    id: 'meeting',
    name: 'Launch Meeting',
    place: 'Mobile app launch sync',
    goal: 'Give a short progress update, discuss roadblocks, and agree on next steps.',
    opening: 'The project manager welcomes you and asks for a quick task status update.'
  }
]

const customSceneTemplate = {
  id: 'custom',
  name: 'Custom Scene',
  place: 'Your own roleplay',
  goal: 'Describe the role, situation, and speaking challenge you want to practice.',
  opening: 'Create a roleplay prompt below, then record your first message.'
}

const view = ref('welcome')
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
const errorMessage = ref('')
const audioLevel = ref(0)
const sessionId = ref(getOrCreateSessionId())
const mediaRecorder = ref(null)
const audioChunks = ref([])
const streamRef = ref(null)
const meterTimer = ref(null)
const threadEl = ref(null)

const scenes = computed(() => [...presetScenes, customSceneTemplate])
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
const canRecord = computed(() => !isSending.value && (!isCustomScene.value || customScenario.value.trim().length > 0))

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

function selectScene(sceneId) {
  activeSceneId.value = sceneId
  errorMessage.value = ''
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
  nextTick(scrollToBottom)
}

async function toggleRecording() {
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

  const extension = mimeType.includes('mp4') ? 'm4a' : mimeType.includes('wav') ? 'wav' : 'webm'
  const blob = new Blob(audioChunks.value, { type: mimeType })
  const formData = new FormData()
  formData.append('audioFile', blob, `voice-${Date.now()}.${extension}`)
  formData.append('sessionId', sessionId.value)

  if (isCustomScene.value) {
    formData.append('customScenario', customScenario.value.trim())
  } else {
    formData.append('scene', activeSceneId.value)
  }

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
    errorMessage.value = error.response?.data?.message || error.message || 'Send failed. Please try again.'
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
          Pick a real-life scene or design your own roleplay, record your voice, and let the AI answer like a patient English partner.
        </p>
        <button class="primary-action" type="button" @click="enterTraining">
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

    <section v-else class="training-view">
      <aside class="scene-rail">
        <button class="back-button" type="button" aria-label="Back to welcome" @click="view = 'welcome'">&lt;-</button>
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
            @click="selectScene(scene.id)"
          >
            <span>{{ scene.name }}</span>
            <small>{{ scene.place }}</small>
          </button>
        </div>

        <label v-if="isCustomScene" class="custom-scene-box">
          <span>Custom prompt</span>
          <textarea
            v-model="customScenario"
            rows="7"
            maxlength="500"
            placeholder="Example: A hotel receptionist helping me change a booking after my flight was delayed."
          ></textarea>
          <small>{{ customScenario.trim().length }}/500 characters</small>
        </label>

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
            :disabled="isSending"
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
