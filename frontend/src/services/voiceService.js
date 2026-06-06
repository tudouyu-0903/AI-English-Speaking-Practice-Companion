import axios from 'axios'

const SESSION_STORAGE_KEY = 'english_voice_session_id'

export function getOrCreateSessionId() {
  const saved = localStorage.getItem(SESSION_STORAGE_KEY)
  if (saved) return saved
  return createSessionId()
}

export function createSessionId() {
  const id = `session-${crypto.randomUUID()}`
  localStorage.setItem(SESSION_STORAGE_KEY, id)
  return id
}

export async function sendVoiceMessage({ audioBlob, mimeType, sessionId, sceneId, customScenario }) {
  const formData = new FormData()
  const extension = getAudioExtension(mimeType)

  formData.append('audioFile', audioBlob, `voice-${Date.now()}.${extension}`)
  formData.append('sessionId', sessionId)

  if (customScenario?.trim()) {
    formData.append('customScenario', customScenario.trim())
  } else {
    formData.append('scene', sceneId)
  }

  const response = await axios.post('/api/voice/file-chat', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })

  assertSuccess(response.data, 'AI response failed')
  return response.data.data
}

export async function fetchConversationSummary(sessionId) {
  const response = await axios.get('/api/voice/summary', {
    params: { sessionId }
  })

  assertSuccess(response.data, 'Summary failed')
  return normalizeSummary(response.data.data)
}

function assertSuccess(payload, fallbackMessage) {
  if (payload?.code !== 0) {
    throw new Error(payload?.message || fallbackMessage)
  }
}

function getAudioExtension(mimeType) {
  if (mimeType.includes('mp4')) return 'm4a'
  if (mimeType.includes('wav')) return 'wav'
  return 'webm'
}

function normalizeSummary(summary) {
  return {
    summary: summary?.summary || 'No summary returned.',
    totalScore: summary?.totalScore,
    fluencyScore: summary?.fluencyScore,
    pronunciationScore: summary?.pronunciationScore,
    grammarScore: summary?.grammarScore,
    corrections: Array.isArray(summary?.corrections) ? summary.corrections : [],
    suggestions: Array.isArray(summary?.suggestions) ? summary.suggestions : []
  }
}
