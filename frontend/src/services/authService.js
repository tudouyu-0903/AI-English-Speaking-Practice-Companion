import axios from 'axios'

const AUTH_STORAGE_KEY = 'english_voice_auth'

export function getStoredAuth() {
  const rawAuth = localStorage.getItem(AUTH_STORAGE_KEY)
  if (!rawAuth) return null

  try {
    const auth = JSON.parse(rawAuth)
    return auth?.token ? auth : null
  } catch (error) {
    localStorage.removeItem(AUTH_STORAGE_KEY)
    return null
  }
}

export function getAuthToken() {
  return getStoredAuth()?.token || ''
}

export async function loginUser({ userAccount, password }) {
  const response = await axios.post('/api/user/login', {
    userAccount,
    password
  })

  assertSuccess(response.data, 'Login failed')
  return saveAuth(response.data.data)
}

export async function registerUser({ username, userAccount, password, phone, email }) {
  const response = await axios.post('/api/user/register', {
    username,
    userAccount,
    password,
    phone,
    email
  })

  assertSuccess(response.data, 'Registration failed')
  return response.data.data
}

export async function logoutUser() {
  const token = getAuthToken()
  try {
    if (token) {
      await axios.post('/api/user/logout', null, {
        headers: { Authorization: `Bearer ${token}` }
      })
    }
  } finally {
    localStorage.removeItem(AUTH_STORAGE_KEY)
  }
}

export function saveAuth(auth) {
  localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(auth))
  return auth
}

function assertSuccess(payload, fallbackMessage) {
  if (payload?.code !== 0) {
    throw new Error(payload?.message || fallbackMessage)
  }
}
