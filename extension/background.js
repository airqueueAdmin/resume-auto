const defaultServiceBaseUrl = 'https://career-note-web-sg.onrender.com'
const legacyLocalServiceBaseUrls = new Set(['http://localhost:5173', 'http://127.0.0.1:5173'])
const serviceBaseUrlKey = 'career-note.service-base-url'
const serviceBaseUrlMigrationKey = 'career-note.service-base-url-production-migrated'
const accessTokenKey = 'career-note.access-token'
const accessTokenExpiresAtKey = 'career-note.access-token-expires-at'

async function serviceBaseUrl() {
  const saved = await chrome.storage.local.get([serviceBaseUrlKey, serviceBaseUrlMigrationKey])
  const value = String(saved[serviceBaseUrlKey] || '').replace(/\/$/, '')
  if (!value || (legacyLocalServiceBaseUrls.has(value) && !saved[serviceBaseUrlMigrationKey])) {
    await chrome.storage.local.set({
      [serviceBaseUrlKey]: defaultServiceBaseUrl,
      [serviceBaseUrlMigrationKey]: true,
    })
    return defaultServiceBaseUrl
  }
  return value
}

async function accessToken() {
  const session = await chrome.storage.session.get([accessTokenKey, accessTokenExpiresAtKey])
  if (!session[accessTokenKey] || Number(session[accessTokenExpiresAtKey] || 0) <= Date.now()) {
    await chrome.storage.session.remove([accessTokenKey, accessTokenExpiresAtKey])
    return null
  }
  return session[accessTokenKey]
}

async function apiRequest(path, options = {}) {
  const token = await accessToken()
  const isPublicExchange = path === '/v1/extension/authorization-codes/exchange'
  if (!token && !isPublicExchange) throw new Error('서비스를 먼저 연결해주세요.')
  const headers = new Headers(options.headers)
  headers.set('Content-Type', 'application/json')
  if (token) headers.set('Authorization', `Bearer ${token}`)
  const response = await fetch(`${await serviceBaseUrl()}/api${path}`, { ...options, headers })
  const body = await response.json().catch(() => null)
  if (!response.ok) throw new Error(body?.detail || '서비스 요청을 처리하지 못했습니다.')
  return body
}

chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message?.type === 'career-note-open-connect') {
    serviceBaseUrl().then((baseUrl) => chrome.tabs.create({ url: `${baseUrl}/extension/connect` }))
      .then(() => sendResponse({ ok: true }))
      .catch((error) => sendResponse({ ok: false, error: error instanceof Error ? error.message : '서비스 연결 페이지를 열지 못했습니다.' }))
    return true
  }

  if (message?.type === 'career-note-open-resume-export') {
    serviceBaseUrl().then((baseUrl) => {
      const resumeId = encodeURIComponent(String(message.resumeId || ''))
      if (!resumeId) throw new Error('생성된 이력서 ID가 없습니다.')
      return chrome.tabs.create({ url: `${baseUrl}/resumes?resumeId=${resumeId}&export=jobkorea` })
    })
      .then(() => sendResponse({ ok: true }))
      .catch((error) => sendResponse({ ok: false, error: error instanceof Error ? error.message : '이력서 파일 작성 페이지를 열지 못했습니다.' }))
    return true
  }

  if (message?.type === 'career-note-set-service-url') {
    const value = String(message.value || defaultServiceBaseUrl).replace(/\/$/, '')
    chrome.storage.local.set({ [serviceBaseUrlKey]: value, [serviceBaseUrlMigrationKey]: true })
      .then(() => sendResponse({ ok: true }))
      .catch((error) => sendResponse({ ok: false, error: error instanceof Error ? error.message : '서비스 주소를 저장하지 못했습니다.' }))
    return true
  }

  if (message?.type === 'career-note-api') {
    apiRequest(message.path, message.options || {})
      .then((data) => sendResponse({ ok: true, data }))
      .catch((error) => sendResponse({ ok: false, error: error instanceof Error ? error.message : '서비스 요청에 실패했습니다.' }))
    return true
  }

  if (message?.type === 'career-note-save-session') {
    chrome.storage.session.set({
      [accessTokenKey]: message.accessToken,
      [accessTokenExpiresAtKey]: Date.now() + (Number(message.expiresInSeconds || 0) * 1000),
    }).then(() => sendResponse({ ok: true }))
      .catch((error) => sendResponse({ ok: false, error: error instanceof Error ? error.message : '서비스 세션을 저장하지 못했습니다.' }))
    return true
  }
})

chrome.action.onClicked.addListener((tab) => {
  if (!tab.id) return
  chrome.tabs.sendMessage(tab.id, { type: 'career-note-toggle-panel' }).catch(() => undefined)
})
