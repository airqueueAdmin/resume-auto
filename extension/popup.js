const scanButton = document.querySelector('#scan')
const statusElement = document.querySelector('#status')
const fieldsElement = document.querySelector('#fields')
const serviceBaseUrlElement = document.querySelector('#service-base-url')
const authorizationCodeElement = document.querySelector('#authorization-code')
const openConnectButton = document.querySelector('#open-connect')
const exchangeButton = document.querySelector('#exchange')
const connectionStatusElement = document.querySelector('#connection-status')
const resumeVersionElement = document.querySelector('#resume-version')
const jobPostingElement = document.querySelector('#job-posting')
const questionElement = document.querySelector('#question')
const maxLengthElement = document.querySelector('#max-length')
const loadContextButton = document.querySelector('#load-context')
const generateAnswerButton = document.querySelector('#generate-answer')
const contextElement = document.querySelector('#context')

const localSettingsKey = 'career-note.service-base-url'
const defaultServiceBaseUrl = 'https://career-note-web-sg.onrender.com'
const legacyLocalServiceBaseUrls = new Set(['http://localhost:5173', 'http://127.0.0.1:5173'])
const serviceBaseUrlMigrationKey = 'career-note.service-base-url-production-migrated'
const accessTokenKey = 'career-note.access-token'
const accessTokenExpiresAtKey = 'career-note.access-token-expires-at'
const sessionKeys = [accessTokenKey, accessTokenExpiresAtKey]
const koreanDateTimeFormatter = new Intl.DateTimeFormat('ko-KR', {
  timeZone: 'Asia/Seoul',
  dateStyle: 'medium',
  timeStyle: 'short',
})
let detectedFields = []
let selectedFieldIndex = null
let reviewedSuggestedValue = ''

function formatKoreanDateTime(value) {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '-' : koreanDateTimeFormatter.format(date)
}

function setStatus(message) {
  statusElement.textContent = message
}

function setConnectionStatus(message) {
  connectionStatusElement.textContent = message
}

function normalizeServiceBaseUrl(value) {
  return value.trim().replace(/\/$/, '')
}

async function serviceBaseUrl() {
  const currentValue = normalizeServiceBaseUrl(serviceBaseUrlElement.value)
  if (currentValue) {
    await chrome.storage.local.set({ [localSettingsKey]: currentValue, [serviceBaseUrlMigrationKey]: true })
    return currentValue
  }
  const saved = await chrome.storage.local.get([localSettingsKey, serviceBaseUrlMigrationKey])
  const savedValue = normalizeServiceBaseUrl(saved[localSettingsKey] || '')
  const value = savedValue && !(legacyLocalServiceBaseUrls.has(savedValue) && !saved[serviceBaseUrlMigrationKey])
    ? savedValue
    : defaultServiceBaseUrl
  await chrome.storage.local.set({ [localSettingsKey]: value, [serviceBaseUrlMigrationKey]: true })
  serviceBaseUrlElement.value = value
  return value
}

async function sessionAccessToken() {
  const session = await chrome.storage.session.get(sessionKeys)
  if (!session[accessTokenKey] || Number(session[accessTokenExpiresAtKey] || 0) <= Date.now()) {
    await chrome.storage.session.remove(sessionKeys)
    return null
  }
  return session[accessTokenKey]
}

async function apiRequest(path, options = {}) {
  const token = await sessionAccessToken()
  if (!token) throw new Error('서비스를 먼저 연결해주세요.')
  const headers = new Headers(options.headers)
  headers.set('Content-Type', 'application/json')
  headers.set('Authorization', `Bearer ${token}`)
  const baseUrl = await serviceBaseUrl()
  const response = await fetch(`${baseUrl}/api${path}`, { ...options, headers })
  const body = await response.json().catch(() => null)
  if (!response.ok) throw new Error(body?.detail || '서비스 요청을 처리하지 못했습니다.')
  return body
}

function renderResumeVersions(versions) {
  resumeVersionElement.replaceChildren()
  if (versions.length === 0) {
    resumeVersionElement.add(new Option('사용 가능한 ResumeVersion이 없습니다.', ''))
    resumeVersionElement.disabled = true
    loadContextButton.disabled = true
    generateAnswerButton.disabled = true
    return
  }
  versions.forEach((version) => {
    const option = new Option(`${version.resumeName} · 버전 ${version.versionNumber} · ${formatKoreanDateTime(version.createdAt)}`, version.versionId)
    resumeVersionElement.add(option)
  })
  resumeVersionElement.disabled = false
  loadContextButton.disabled = false
  generateAnswerButton.disabled = jobPostingElement.disabled || !jobPostingElement.value
}

function renderJobPostings(postings) {
  jobPostingElement.replaceChildren()
  if (postings.length === 0) {
    jobPostingElement.add(new Option('저장된 채용공고가 없습니다.', ''))
    jobPostingElement.disabled = true
    generateAnswerButton.disabled = true
    return
  }
  postings.forEach((posting) => {
    jobPostingElement.add(new Option(`${posting.companyName} · ${posting.title}`, posting.id))
  })
  jobPostingElement.disabled = false
  generateAnswerButton.disabled = resumeVersionElement.disabled
}

async function loadResumeVersions() {
  const [versions, postings] = await Promise.all([
    apiRequest('/v1/extension/resume-versions?page=0&size=100'),
    apiRequest('/v1/job-postings?page=0&size=100'),
  ])
  renderResumeVersions(versions.content || [])
  renderJobPostings(postings.content || [])
  setConnectionStatus(`연결됨 · ResumeVersion ${versions.totalElements || 0}개 · 채용공고 ${postings.totalElements || 0}개`)
}

function renderContext(result) {
  contextElement.replaceChildren()
  const selected = result?.resumeVersion
  const summary = document.createElement('p')
  summary.className = 'muted'
  summary.textContent = selected
    ? `${selected.resumeName} · 버전 ${selected.versionNumber} · 근거 ${result.evidence?.length || 0}개`
    : '컨텍스트를 찾지 못했습니다.'
  contextElement.append(summary)

  ;(result?.evidence || []).forEach((evidence) => {
    const item = document.createElement('div')
    item.style.borderTop = '1px solid #e5ebf3'
    item.style.marginTop = '8px'
    item.style.paddingTop = '8px'
    const source = document.createElement('strong')
    source.textContent = `${evidence.sourceType} · 유사도 ${Number(evidence.similarity).toFixed(2)}`
    source.style.display = 'block'
    source.style.color = '#334155'
    source.style.fontSize = '11px'
    const content = document.createElement('span')
    content.textContent = evidence.content
    content.style.display = 'block'
    content.style.marginTop = '4px'
    content.style.lineHeight = '1.45'
    item.append(source, content)
    contextElement.append(item)
  })
}

async function connectWithCode() {
  exchangeButton.disabled = true
  setConnectionStatus('연결 코드를 확인하는 중입니다...')
  try {
    const baseUrl = await serviceBaseUrl()
    const code = authorizationCodeElement.value.trim()
    if (!code) throw new Error('연결 코드를 입력해주세요.')
    const response = await fetch(`${baseUrl}/api/v1/extension/authorization-codes/exchange`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ code }),
    })
    const body = await response.json().catch(() => null)
    if (!response.ok) throw new Error(body?.detail || '연결 코드를 교환하지 못했습니다.')
    await chrome.storage.session.set({
      [accessTokenKey]: body.accessToken,
      [accessTokenExpiresAtKey]: Date.now() + (Number(body.accessTokenExpiresInSeconds || 0) * 1000),
    })
    authorizationCodeElement.value = ''
    await loadResumeVersions()
  } catch (error) {
    setConnectionStatus(error instanceof Error ? error.message : '서비스 연결에 실패했습니다.')
  } finally {
    exchangeButton.disabled = false
  }
}

async function openConnectPage() {
  await serviceBaseUrl()
  const response = await chrome.runtime.sendMessage({ type: 'career-note-open-connect' })
  if (!response?.ok) setConnectionStatus(response?.error || '서비스 연결 페이지를 열지 못했습니다.')
}

async function loadContext() {
  loadContextButton.disabled = true
  contextElement.textContent = '경력 근거를 조회하는 중입니다...'
  try {
    const question = questionElement.value.trim()
    if (!question) throw new Error('필드 또는 지원 질문을 입력해주세요.')
    const result = await apiRequest('/v1/extension/application-context', {
      method: 'POST',
      body: JSON.stringify({ resumeVersionId: resumeVersionElement.value, question }),
    })
    renderContext(result)
  } catch (error) {
    contextElement.textContent = error instanceof Error ? error.message : '경력 근거를 조회하지 못했습니다.'
  } finally {
    loadContextButton.disabled = false
  }
}

function selectDetectedField(field) {
  selectedFieldIndex = field.fieldIndex
  questionElement.value = field.label
  if (field.maxLength > 0) maxLengthElement.value = field.maxLength
  setStatus(`선택된 필드: ${field.label} · 검토용 답변을 생성할 수 있습니다.`)
  renderFields(detectedFields)
}

function renderAnswer(result) {
  contextElement.replaceChildren()
  reviewedSuggestedValue = result.suggestedValue || result.answer || ''
  const heading = document.createElement('strong')
  heading.textContent = `검토용 답변 · ${result.answerLength}자${result.truncated ? ` · ${result.maxLength}자로 잘라낸 제안` : ''}`
  heading.style.display = 'block'
  heading.style.color = '#334155'
  heading.style.fontSize = '12px'
  const answer = document.createElement('textarea')
  answer.value = reviewedSuggestedValue
  answer.maxLength = result.maxLength || 100000
  answer.style.minHeight = '130px'
  answer.addEventListener('input', () => { reviewedSuggestedValue = answer.value })
  const provenance = document.createElement('p')
  provenance.className = 'muted'
  provenance.textContent = `근거 ${result.citations?.length || 0}개 · ${result.resumeName} 버전 ${result.versionNumber} · 반드시 내용을 검토하세요.`
  const actions = document.createElement('div')
  actions.className = 'actions'
  const fill = document.createElement('button')
  fill.className = 'secondary'
  fill.textContent = '검토 완료 · 현재 필드에 입력'
  fill.addEventListener('click', () => void fillApprovedAnswer())
  actions.append(fill)
  const notice = document.createElement('small')
  notice.className = 'muted'
  notice.textContent = '이 버튼은 현재 선택한 텍스트 필드에만 입력하며, 제출은 하지 않습니다.'
  contextElement.append(heading, answer, provenance, actions, notice)
}

async function generateAnswer() {
  generateAnswerButton.disabled = true
  contextElement.textContent = '근거 기반 답변 초안을 생성하는 중입니다...'
  try {
    const question = questionElement.value.trim()
    const maxLength = Number(maxLengthElement.value)
    if (!question) throw new Error('필드 또는 지원 질문을 입력해주세요.')
    if (!resumeVersionElement.value) throw new Error('ResumeVersion을 선택해주세요.')
    if (!jobPostingElement.value) throw new Error('기준 채용공고를 선택해주세요.')
    const result = await apiRequest('/v1/extension/application-answer', {
      method: 'POST',
      body: JSON.stringify({
        resumeVersionId: resumeVersionElement.value,
        jobPostingId: jobPostingElement.value,
        question,
        maxLength: Number.isFinite(maxLength) && maxLength > 0 ? maxLength : null,
      }),
    })
    renderAnswer(result)
  } catch (error) {
    contextElement.textContent = error instanceof Error ? error.message : '답변 초안을 생성하지 못했습니다.'
  } finally {
    generateAnswerButton.disabled = resumeVersionElement.disabled || jobPostingElement.disabled
  }
}

async function fillApprovedAnswer() {
  if (selectedFieldIndex === null) {
    contextElement.textContent = '먼저 검사 결과에서 입력할 필드를 선택해주세요.'
    return
  }
  try {
    const [tab] = await chrome.tabs.query({ active: true, currentWindow: true })
    if (!tab?.id) throw new Error('활성 탭을 찾을 수 없습니다.')
    const [result] = await chrome.scripting.executeScript({
      target: { tabId: tab.id },
      func: fillVisibleField,
      args: [selectedFieldIndex, reviewedSuggestedValue],
    })
    if (!result?.result) throw new Error('선택한 필드에 입력할 수 없습니다. 텍스트 필드를 다시 선택해주세요.')
    contextElement.textContent = '검토한 답변을 현재 필드에 입력했습니다. 최종 제출은 JobKorea에서 직접 진행해주세요.'
  } catch (error) {
    contextElement.textContent = error instanceof Error ? error.message : '필드에 답변을 입력하지 못했습니다.'
  }
}

function isJobKoreaUrl(url) {
  try {
    const hostname = new URL(url).hostname.toLowerCase()
    return hostname === 'jobkorea.co.kr' || hostname.endsWith('.jobkorea.co.kr')
  } catch {
    return false
  }
}

function renderFields(fields) {
  detectedFields = fields
  fieldsElement.replaceChildren()
  if (fields.length === 0) {
    const empty = document.createElement('li')
    empty.className = 'empty'
    empty.textContent = '화면에서 입력 필드를 찾지 못했습니다.'
    fieldsElement.append(empty)
    return
  }

  fields.forEach((field) => {
    const item = document.createElement('li')
    const label = document.createElement('span')
    label.className = 'field-label'
    label.textContent = field.label
    item.append(label)

    const metadata = document.createElement('span')
    metadata.className = 'field-meta'
    ;[field.semanticRole, field.type, field.required ? '필수' : '선택', field.maxLength > 0 ? `${field.maxLength}자` : null]
      .filter(Boolean)
      .forEach((value) => {
        const badge = document.createElement('span')
        badge.className = 'badge'
        badge.textContent = value
        metadata.append(badge)
      })
    item.append(metadata)
    const select = document.createElement('button')
    select.className = 'secondary'
    select.type = 'button'
    select.textContent = field.fieldIndex === selectedFieldIndex ? '선택됨 · 질문으로 사용' : '이 필드 선택'
    select.addEventListener('click', () => selectDetectedField(field))
    item.append(select)
    fieldsElement.append(item)
  })
}

async function scanActiveTab() {
  scanButton.disabled = true
  setStatus('현재 페이지를 검사하는 중입니다...')
  try {
    const [tab] = await chrome.tabs.query({ active: true, currentWindow: true })
    if (!tab?.id) throw new Error('활성 탭을 찾을 수 없습니다.')
    const isJobKorea = isJobKoreaUrl(tab.url ?? '')
    const [result] = await chrome.scripting.executeScript({
      target: { tabId: tab.id },
      func: inspectVisibleFields,
    })
    const fields = result?.result ?? []
    renderFields(fields)
    setStatus(`${isJobKorea ? 'JobKorea' : '지원 대상 외 페이지'} · ${fields.length}개 필드를 찾았습니다.`)
  } catch (error) {
    renderFields([])
    setStatus(error instanceof Error ? error.message : '페이지를 검사하지 못했습니다.')
  } finally {
    scanButton.disabled = false
  }
}

function inspectVisibleFields() {
  const elements = [...document.querySelectorAll('input, textarea, select, [contenteditable="true"]')]
  return elements
    .filter((element) => {
      if (element instanceof HTMLInputElement && ['hidden', 'submit', 'button', 'reset', 'image'].includes(element.type)) return false
      const rect = element.getBoundingClientRect()
      const style = window.getComputedStyle(element)
      return rect.width > 0 && rect.height > 0 && style.visibility !== 'hidden' && style.display !== 'none'
    })
    .map((element, index) => {
      const input = element instanceof HTMLInputElement || element instanceof HTMLTextAreaElement || element instanceof HTMLSelectElement ? element : null
      const id = element.getAttribute('id')
      const linkedLabel = id ? document.querySelector(`label[for="${CSS.escape(id)}"]`) : null
      const parentLabel = element.closest('label')
      const fieldset = element.closest('fieldset')
      const legend = fieldset?.querySelector('legend')
      const label = linkedLabel?.textContent?.trim()
        || parentLabel?.textContent?.trim()
        || element.getAttribute('aria-label')
        || element.getAttribute('placeholder')
        || legend?.textContent?.trim()
        || element.getAttribute('name')
        || id
        || `입력 필드 ${index + 1}`
      const type = element instanceof HTMLInputElement ? element.type || 'text' : element.tagName.toLowerCase()
      const searchable = `${label} ${element.getAttribute('name') || ''} ${id || ''} ${element.getAttribute('autocomplete') || ''}`.toLowerCase()
      const roleRules = [
        ['EMAIL', ['이메일', 'email', 'mail']],
        ['PHONE', ['휴대폰', '전화', '연락처', 'phone', 'mobile', 'tel']],
        ['PORTFOLIO', ['포트폴리오', 'portfolio', 'github', 'gitlab', '링크', 'url']],
        ['NAME', ['이름', '성명', 'name', '지원자명']],
        ['CAREER_DESCRIPTION', ['경력기술', '경력 사항', '경력사항', 'career', 'experience']],
        ['SELF_INTRODUCTION', ['자기소개', '지원동기', '입사 후', '성장과정', '지원 분야', 'motivation', 'introduction']],
      ]
      const semanticRole = roleRules.find(([, terms]) => terms.some((term) => searchable.includes(term)))?.[0] || 'UNKNOWN'
      return {
        fieldIndex: index,
        label: label.replace(/\s+/g, ' '),
        type,
        semanticRole,
        required: Boolean(input?.required || element.getAttribute('aria-required') === 'true'),
        maxLength: input?.maxLength ?? -1,
      }
    })
}

function fillVisibleField(index, value) {
  const elements = [...document.querySelectorAll('input, textarea, select, [contenteditable="true"]')]
    .filter((element) => {
      if (element instanceof HTMLInputElement && ['hidden', 'submit', 'button', 'reset', 'image'].includes(element.type)) return false
      const rect = element.getBoundingClientRect()
      const style = window.getComputedStyle(element)
      return rect.width > 0 && rect.height > 0 && style.visibility !== 'hidden' && style.display !== 'none'
    })
  const element = elements[index]
  if (!element || element instanceof HTMLSelectElement || (element instanceof HTMLInputElement && ['file', 'checkbox', 'radio'].includes(element.type))) return false
  if (element instanceof HTMLElement && element.isContentEditable) {
    element.textContent = value
  } else if (element instanceof HTMLInputElement || element instanceof HTMLTextAreaElement) {
    const prototype = element instanceof HTMLTextAreaElement ? HTMLTextAreaElement.prototype : HTMLInputElement.prototype
    const setter = Object.getOwnPropertyDescriptor(prototype, 'value')?.set
    if (!setter) return false
    setter.call(element, value)
  } else {
    return false
  }
  element.dispatchEvent(new Event('input', { bubbles: true }))
  element.dispatchEvent(new Event('change', { bubbles: true }))
  return true
}

async function initialize() {
  const saved = await chrome.storage.local.get([localSettingsKey, serviceBaseUrlMigrationKey])
  const savedValue = normalizeServiceBaseUrl(saved[localSettingsKey] || '')
  const value = savedValue && !(legacyLocalServiceBaseUrls.has(savedValue) && !saved[serviceBaseUrlMigrationKey])
    ? savedValue
    : defaultServiceBaseUrl
  serviceBaseUrlElement.value = value
  if (savedValue !== value || !saved[serviceBaseUrlMigrationKey]) {
    await chrome.storage.local.set({ [localSettingsKey]: value, [serviceBaseUrlMigrationKey]: true })
  }
  if (await sessionAccessToken()) {
    await loadResumeVersions().catch((error) => setConnectionStatus(error instanceof Error ? error.message : '연결 상태를 확인하지 못했습니다.'))
  }
}

openConnectButton.addEventListener('click', () => void openConnectPage())
exchangeButton.addEventListener('click', () => void connectWithCode())
loadContextButton.addEventListener('click', () => void loadContext())
generateAnswerButton.addEventListener('click', () => void generateAnswer())
jobPostingElement.addEventListener('change', () => { generateAnswerButton.disabled = resumeVersionElement.disabled || jobPostingElement.disabled || !jobPostingElement.value })
scanButton.addEventListener('click', () => void scanActiveTab())
void initialize()
