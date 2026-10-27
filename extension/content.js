const defaultServiceBaseUrl = 'https://career-note-web-sg.onrender.com'
const legacyLocalServiceBaseUrls = new Set(['http://localhost:5173', 'http://127.0.0.1:5173'])
const serviceBaseUrlMigrationKey = 'career-note.service-base-url-production-migrated'

function isProductionService() {
  try {
    return new URL(serviceUrl?.value || defaultServiceBaseUrl).origin === new URL(defaultServiceBaseUrl).origin
  } catch {
    return false
  }
}

const panelHost = document.createElement('div')
panelHost.id = 'career-note-extension-host'
document.documentElement.append(panelHost)

const shadowRoot = panelHost.attachShadow({ mode: 'open' })
shadowRoot.innerHTML = `
  <style>
    :host { all: initial; }
    * { box-sizing: border-box; }
    .toggle {
      position: fixed; right: 22px; bottom: 108px; z-index: 2147483647;
      display: grid; place-items: center; width: 58px; height: 58px; padding: 0;
      border: 0; border-radius: 50%;
      background: #2563eb; color: #fff; box-shadow: 0 10px 30px rgba(37, 99, 235, .38);
      cursor: pointer; font: 700 13px/1.2 Arial, sans-serif;
    }
    .toggle:hover { background: #1d4ed8; }
    .toggle svg { width: 29px; height: 29px; fill: none; stroke: currentColor; stroke-linecap: round; stroke-linejoin: round; stroke-width: 2; }
    .toggle-dot { position: absolute; top: 2px; right: 1px; width: 11px; height: 11px; border: 2px solid #fff; border-radius: 50%; background: #22c55e; }
    .panel {
      position: fixed; right: 20px; bottom: 168px; z-index: 2147483647;
      display: none; width: min(420px, calc(100vw - 32px)); max-height: min(720px, calc(100vh - 190px));
      overflow: auto; border: 1px solid #dbe4f0; border-radius: 16px;
      background: #f8fafc; color: #172033; box-shadow: 0 20px 60px rgba(15, 23, 42, .28);
      font: 13px/1.45 Arial, sans-serif;
    }
    .panel.open { display: block; }
    .header { position: sticky; top: 0; z-index: 1; display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 16px 18px 12px; background: #fff; border-bottom: 1px solid #e5ebf3; }
    .header h1 { margin: 0; color: #172033; font-size: 16px; }
    .header p { margin: 4px 0 0; color: #64748b; font-size: 11px; }
    .close { flex: 0 0 32px; width: 32px; height: 32px; display: grid; place-items: center; border: 1px solid #dbe4f0; border-radius: 8px; padding: 0; background: #fff; color: #475569; cursor: pointer; font-size: 22px; line-height: 1; }
    .close:hover { border-color: #bfdbfe; background: #eff6ff; color: #1d4ed8; }
    .section { margin: 12px; padding: 13px; border: 1px solid #e5ebf3; border-radius: 11px; background: #fff; }
    .section h2 { margin: 0 0 9px; color: #334155; font-size: 12px; }
    label { display: grid; gap: 5px; margin-top: 9px; color: #475569; font-size: 11px; font-weight: 700; }
    input, textarea, select { width: 100%; min-width: 0; border: 1px solid #dbe4f0; border-radius: 7px; padding: 9px 10px; background: #fff; color: #172033; font: inherit; font-size: 12px; }
    textarea { min-height: 70px; resize: vertical; line-height: 1.45; }
    button { border: 0; border-radius: 7px; padding: 9px 11px; background: #172033; color: #fff; cursor: pointer; font: 700 11px/1.25 Arial, sans-serif; }
    button:hover { background: #263653; }
    button:disabled { cursor: wait; opacity: .55; }
    .secondary { background: #eaf0f7; color: #334155; }
    .secondary:hover { background: #dce7f3; }
    .actions { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 7px; margin-top: 9px; }
    .actions.single { grid-template-columns: minmax(0, 1fr); }
    .status { margin: 8px 0 0; color: #2563eb; font-size: 11px; }
    .muted { color: #64748b; font-size: 11px; }
    .notice { margin: 9px 0 0; color: #94a3b8; font-size: 10px; line-height: 1.45; }
    .field-list { display: grid; gap: 7px; max-height: 190px; overflow: auto; margin: 9px 0 0; padding: 0; list-style: none; }
    .field-guide { margin: 9px 0 0; padding: 9px; border-radius: 8px; background: #eff6ff; color: #475569; font-size: 10px; line-height: 1.55; }
    .field-guide strong { color: #1e40af; }
    .field-guide p { margin: 3px 0 0; }
    .field-item { padding: 9px; border: 1px solid #dbe4f0; border-radius: 8px; background: #f8fafc; }
    .field-item.not-answerable { border-color: #e2e8f0; background: #f8fafc; }
    .field-label { display: block; overflow: hidden; color: #334155; font-size: 11px; font-weight: 700; text-overflow: ellipsis; white-space: nowrap; }
    .field-description { margin: 6px 0 0; color: #64748b; font-size: 10px; line-height: 1.45; }
    .field-meta { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 5px; }
    .badge { border-radius: 999px; padding: 3px 6px; background: #eff6ff; color: #1d4ed8; font-size: 10px; }
    .badge.muted-badge { background: #f1f5f9; color: #64748b; }
    .field-item button { margin-top: 7px; width: 100%; }
    .result { margin-top: 10px; padding-top: 10px; border-top: 1px solid #e5ebf3; }
    .result strong { display: block; color: #334155; font-size: 11px; }
    .citation { margin-top: 7px; padding-top: 7px; border-top: 1px solid #e5ebf3; color: #475569; font-size: 10px; line-height: 1.45; }
    .empty { padding: 12px 4px; color: #94a3b8; font-size: 11px; text-align: center; }
    .posting-card, .readiness-card { border: 1px solid #dbe4f0; border-radius: 9px; padding: 10px; background: #f8fafc; }
    .posting-card strong { display: block; color: #1e293b; font-size: 12px; }
    .posting-card span { display: block; margin-top: 3px; color: #475569; font-size: 11px; }
    .posting-card p { display: -webkit-box; overflow: hidden; margin: 7px 0 0; color: #64748b; font-size: 10px; line-height: 1.45; -webkit-box-orient: vertical; -webkit-line-clamp: 3; }
    .posting-read-status { margin-top: 7px; color: #166534; font-size: 10px; font-weight: 700; }
    .posting-read-warning { margin-top: 4px; color: #9a3412; font-size: 10px; line-height: 1.45; }
    .posting-card details { margin-top: 8px; border-top: 1px solid #e2e8f0; padding-top: 7px; }
    .posting-card summary { color: #1d4ed8; cursor: pointer; font-size: 10px; font-weight: 700; }
    .posting-card pre { max-height: 240px; overflow: auto; margin: 7px 0 0; white-space: pre-wrap; color: #475569; font: 10px/1.5 Arial, sans-serif; }
    .readiness-card.ready { border-color: #bbf7d0; background: #f0fdf4; }
    .readiness-card.blocked { border-color: #fed7aa; background: #fff7ed; }
    .readiness-counts { display: flex; flex-wrap: wrap; gap: 5px; margin-top: 6px; }
    .readiness-card p { margin: 7px 0 0; color: #64748b; font-size: 10px; }
    .field-help { color: #64748b; font-size: 10px; font-weight: 400; line-height: 1.45; }
    .resume-draft-result { margin-top: 10px; padding: 10px; border-top: 1px solid #e5ebf3; }
    .resume-draft-result > strong { display: block; color: #334155; font-size: 11px; }
    .resume-draft-meta { margin-top: 5px; color: #64748b; font-size: 10px; line-height: 1.45; }
    .resume-draft-block { margin-top: 8px; padding: 8px; border: 1px solid #e5ebf3; border-radius: 8px; background: #f8fafc; }
    .resume-draft-block strong { display: block; color: #334155; font-size: 10px; }
    .resume-draft-block p { margin: 5px 0 0; white-space: pre-wrap; color: #475569; font-size: 10px; line-height: 1.5; }
    .resume-draft-block small { display: block; margin-top: 5px; color: #94a3b8; font-size: 9px; line-height: 1.4; }
    .optional-tools { margin-top: 12px; border-top: 1px solid #e5ebf3; padding-top: 10px; }
    .optional-tools summary { color: #475569; cursor: pointer; font-size: 11px; font-weight: 700; }
    .optional-tools > .field-help { margin: 8px 0 0; }
  </style>
  <button class="toggle" id="toggle" type="button" aria-label="Career Note 열기" aria-expanded="false" title="Career Note 열기">
    <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M20 11.5a8 8 0 0 1-8 8 8.8 8.8 0 0 1-3.5-.7L4 20l1.2-3.4A8 8 0 1 1 20 11.5Z"></path><path d="M8.5 11.5h.01M12 11.5h.01M15.5 11.5h.01"></path></svg>
    <span class="toggle-dot" aria-hidden="true"></span>
  </button>
  <aside class="panel" id="panel" aria-label="Career Note JobKorea 지원 도우미">
    <div class="header">
      <div><h1>Career Note · JobKorea</h1><p>검토 후 직접 입력하고 제출하세요.</p></div>
      <button class="close" id="close" type="button" aria-label="패널 닫기">×</button>
    </div>
    <section class="section">
      <h2>서비스 연결</h2>
      <label>서비스 주소<input id="service-url" type="url" value="${defaultServiceBaseUrl}" autocomplete="url"></label>
      <div class="actions"><button class="secondary" id="save-url" type="button">주소 저장</button><button class="secondary" id="open-connect" type="button">연결 코드 발급</button></div>
      <label>연결 코드<input id="code" type="text" inputmode="text" autocomplete="one-time-code" placeholder="서비스에서 발급한 코드"></label>
      <div class="actions single"><button id="exchange" type="button">확장 프로그램 연결</button></div>
      <div class="status" id="connection-status" role="status">아직 연결되지 않았습니다.</div>
    </section>
    <section class="section">
      <h2>지원 답변 준비</h2>
      <div class="readiness-card blocked" id="readiness"><strong>서비스 연결 후 경력 근거를 확인합니다.</strong><p>답변은 Career Note에 등록하고 인덱싱한 경력·프로젝트만 근거로 사용합니다.</p></div>
      <div class="actions"><button class="secondary" id="open-career" type="button">경력·프로젝트 관리</button><button class="secondary" id="refresh-readiness" type="button">준비 상태 새로고침</button></div>
      <h2 style="margin-top: 14px">현재 JobKorea 공고</h2>
      <div class="posting-card" id="posting"><strong>현재 공고를 확인하는 중입니다.</strong></div>
      <div class="actions single"><button id="generate-resume" type="button" disabled>이 공고로 이력서 초안 생성</button></div>
      <div class="resume-draft-result" id="resume-draft-result" role="status"><span class="muted">공고를 읽은 뒤, 이 공고에 맞춘 검토용 이력서를 생성할 수 있습니다.</span></div>
      <div class="actions"><button class="secondary" id="detect-posting" type="button">현재 공고 다시 읽기</button><button class="secondary" id="scan" type="button">지원서 필드 검사</button></div>
      <div class="status" id="scan-status" role="status">화면의 입력 필드를 검사할 수 있습니다.</div>
      <div class="field-guide"><strong>검사 결과 읽는 방법</strong><p>검색창·선택 목록은 지원 답변 대상이 아닙니다. JobKorea에서는 이 공고로 이력서 초안을 생성하면 됩니다. 회사 홈페이지 지원 질문이 있을 때만 아래 선택 기능을 사용하세요.</p></div>
      <ul class="field-list" id="fields" aria-live="polite"><li class="empty">아직 검사 결과가 없습니다.</li></ul>
      <details class="optional-tools">
        <summary>회사 홈페이지 지원 질문 답변 (선택)</summary>
        <p class="field-help">JobKorea 지원에는 필요하지 않습니다. 회사 홈페이지 지원서에 별도 질문이 있을 때만 질문을 입력하고 답변을 생성하세요.</p>
        <label>홈페이지 지원 질문<textarea id="question" maxlength="3000" placeholder="예: 우리 회사에 지원한 동기와 입사 후 기여할 수 있는 점을 작성해주세요."></textarea><span class="field-help">실제 홈페이지 지원 질문을 그대로 입력하면 가장 정확합니다. 필드를 선택하면 기본 질문이 자동으로 채워지며, 생성 전에 직접 수정할 수 있습니다.</span></label>
        <label>최대 글자 수<input id="max-length" type="number" min="1" max="100000" value="1000"></label>
        <div class="actions single"><button id="generate" type="button" disabled>홈페이지 지원 질문 답변 생성</button></div>
        <div class="result" id="result" role="status"><span class="muted">홈페이지 지원 질문을 입력하면 Career 근거 기반 답변이 여기에 표시됩니다.</span></div>
      </details>
      <p class="notice">자동 제출은 하지 않습니다. 생성된 답변은 반드시 검토한 후 현재 필드에 입력하고, 최종 제출은 JobKorea에서 직접 진행하세요.</p>
    </section>
  </aside>
`

const $ = (id) => shadowRoot.getElementById(id)
const panel = $('panel')
const toggle = $('toggle')
const close = $('close')
const serviceUrl = $('service-url')
const saveUrl = $('save-url')
const openConnect = $('open-connect')
const code = $('code')
const exchange = $('exchange')
const connectionStatus = $('connection-status')
const readiness = $('readiness')
const openCareer = $('open-career')
const refreshReadiness = $('refresh-readiness')
const posting = $('posting')
const detectPosting = $('detect-posting')
const generateResume = $('generate-resume')
const resumeDraftResult = $('resume-draft-result')
const scan = $('scan')
const scanStatus = $('scan-status')
const fields = $('fields')
const question = $('question')
const maxLength = $('max-length')
const generate = $('generate')
const result = $('result')

let detectedFields = []
let selectedFieldIndex = null
let reviewedSuggestedValue = ''
let currentPosting = null
let serviceConnected = false
let careerReady = false
let resumeDraft = null

function sendMessage(message) {
  return new Promise((resolve, reject) => {
    chrome.runtime.sendMessage(message, (response) => {
      if (chrome.runtime.lastError) reject(new Error(chrome.runtime.lastError.message))
      else resolve(response)
    })
  })
}

async function request(path, options = {}) {
  const response = await sendMessage({ type: 'career-note-api', path, options })
  if (!response?.ok) throw new Error(response?.error || '서비스 요청을 처리하지 못했습니다.')
  return response.data
}

function setPanelOpen(open) {
  panel.classList.toggle('open', open)
  toggle.setAttribute('aria-expanded', String(open))
  toggle.setAttribute('aria-label', open ? 'Career Note 닫기' : 'Career Note 열기')
  toggle.setAttribute('title', open ? 'Career Note 닫기' : 'Career Note 열기')
}

function setConnectionStatus(message) {
  connectionStatus.textContent = message
}

function setResultMessage(message) {
  result.replaceChildren()
  const messageElement = document.createElement('span')
  messageElement.className = 'muted'
  messageElement.textContent = message
  result.append(messageElement)
}

function updateAnswerButtons() {
  generate.disabled = !(serviceConnected && careerReady && currentPosting && question.value.trim())
  generateResume.disabled = !(serviceConnected && careerReady && currentPosting)
}

function normalizedText(value, maxLength = 50000) {
  return String(value || '').replace(/\s+/g, ' ').trim().slice(0, maxLength)
}

function textFromHtml(value, maxLength = 50000) {
  if (!value) return ''
  const documentValue = new DOMParser().parseFromString(String(value), 'text/html')
  return normalizedText(documentValue.body.textContent || '', maxLength)
}

function findJobPostingJson(value) {
  if (!value) return null
  if (Array.isArray(value)) {
    for (const item of value) {
      const found = findJobPostingJson(item)
      if (found) return found
    }
    return null
  }
  if (typeof value !== 'object') return null
  const types = Array.isArray(value['@type']) ? value['@type'] : [value['@type']]
  if (types.some((type) => String(type).toLowerCase() === 'jobposting')) return value
  return findJobPostingJson(value['@graph'])
}

function metaContent(selector) {
  return document.querySelector(selector)?.getAttribute('content') || ''
}

function firstVisibleText(selectors, maxLength = 500) {
  for (const selector of selectors) {
    for (const element of document.querySelectorAll(selector)) {
      const rect = element.getBoundingClientRect()
      if (rect.width > 0 && rect.height > 0) {
        const value = normalizedText(element.textContent, maxLength)
        if (value) return value
      }
    }
  }
  return ''
}

function longestVisibleText(selectors, maxLength = 50000) {
  let longest = ''
  for (const selector of selectors) {
    for (const element of document.querySelectorAll(selector)) {
      const rect = element.getBoundingClientRect()
      if (rect.width <= 0 || rect.height <= 0) continue
      const value = normalizedText(element.textContent, maxLength)
      if (value.length > longest.length) longest = value
    }
  }
  return longest
}

function inspectCurrentPosting() {
  let structured = null
  for (const script of document.querySelectorAll('script[type="application/ld+json"]')) {
    try {
      structured = findJobPostingJson(JSON.parse(script.textContent || 'null'))
      if (structured) break
    } catch {
      // Ignore unrelated malformed structured data from the target page.
    }
  }
  const organization = structured?.hiringOrganization
  const companyName = normalizedText(
    typeof organization === 'object' ? organization?.name : organization,
    200
  ) || firstVisibleText(['[class*="corpName"]', '[class*="companyName"]', '[class*="company-name"]', '[class*="company"] a']) || '회사명 확인 필요'
  const title = normalizedText(structured?.title, 200)
    || firstVisibleText(['h1', '[class*="jobTitle"]', '[class*="title"] h2'])
    || normalizedText(metaContent('meta[property="og:title"]') || document.title, 200)
  const structuredDescription = textFromHtml(structured?.description, 50000)
  const visibleDescription = longestVisibleText(
    ['[class*="jobDescription"]', '[class*="description"]', '[class*="recruitDetail"]', 'article', 'main'],
    50000
  )
  const metaDescription = normalizedText(
    metaContent('meta[name="description"]') || metaContent('meta[property="og:description"]'),
    50000
  )
  const description = [visibleDescription, structuredDescription, metaDescription]
    .filter(Boolean)
    .sort((left, right) => right.length - left.length)[0] || ''
  if (!title || description.length < 30) return null
  const sources = [structuredDescription && '구조화 공고 정보', visibleDescription && '화면 본문'].filter(Boolean)
  return {
    companyName,
    jobTitle: title,
    jobDescription: description,
    sourceUrl: location.href.slice(0, 2048),
    readSource: sources.join(' + ') || '페이지 메타 정보',
    structuredDescriptionLength: structuredDescription.length,
    visibleDescriptionLength: visibleDescription.length,
    readStatus: sources.length >= 2 ? 'COMPLETE' : 'CHECK',
  }
}

function renderPosting() {
  currentPosting = inspectCurrentPosting()
  resumeDraft = null
  posting.replaceChildren()
  resumeDraftResult.replaceChildren()
  const draftMessage = document.createElement('span')
  draftMessage.className = 'muted'
  draftMessage.textContent = '공고를 읽은 뒤, 이 공고에 맞춘 검토용 이력서를 생성할 수 있습니다.'
  resumeDraftResult.append(draftMessage)
  const title = document.createElement('strong')
  if (!currentPosting) {
    title.textContent = '현재 페이지에서 공고 내용을 찾지 못했습니다.'
    const help = document.createElement('p')
    help.textContent = 'JobKorea 공고 상세가 모두 표시된 뒤 다시 읽어주세요.'
    posting.append(title, help)
    updateAnswerButtons()
    return
  }
  title.textContent = currentPosting.jobTitle
  const company = document.createElement('span')
  company.textContent = currentPosting.companyName
  const readStatus = document.createElement('div')
  readStatus.className = currentPosting.readStatus === 'COMPLETE' ? 'posting-read-status' : 'posting-read-warning'
  const productionMode = isProductionService()
  const sourceLengths = [
    currentPosting.structuredDescriptionLength > 0 ? `구조화 정보 ${currentPosting.structuredDescriptionLength}자` : null,
    currentPosting.visibleDescriptionLength > 0 ? `화면 본문 ${currentPosting.visibleDescriptionLength}자` : null,
  ].filter(Boolean).join(' · ')
  readStatus.textContent = productionMode
    ? (currentPosting.readStatus === 'COMPLETE' ? '공고 읽기 확인됨' : '공고 내용을 충분히 읽지 못했습니다. 다시 읽어주세요.')
    : currentPosting.readStatus === 'COMPLETE'
      ? `공고 읽기 확인됨 · ${currentPosting.jobDescription.length}자 사용 · ${sourceLengths}`
      : `공고 ${currentPosting.jobDescription.length}자 확인됨 · ${currentPosting.readSource}만 읽혀 추가 확인이 필요합니다.`
  const description = document.createElement('p')
  description.textContent = currentPosting.jobDescription
  if (productionMode) {
    posting.append(title, company, readStatus, description)
  } else {
    const details = document.createElement('details')
    const summary = document.createElement('summary')
    summary.textContent = `읽은 공고 원문 전체 보기 (${currentPosting.jobDescription.length}자)`
    const fullText = document.createElement('pre')
    fullText.textContent = currentPosting.jobDescription
    details.append(summary, fullText)
    posting.append(title, company, readStatus, description, details)
  }
  updateAnswerButtons()
}

function draftCitationSummary(line) {
  return (line?.citations || [])
    .map((citation) => `${citation.citationId} · ${citation.sourceType}`)
    .join(', ')
}

function appendResumeDraftBlock(title, line) {
  if (!line?.text) return
  const block = document.createElement('div')
  block.className = 'resume-draft-block'
  const heading = document.createElement('strong')
  heading.textContent = title
  const text = document.createElement('p')
  text.textContent = line.text
  block.append(heading, text)
  const citations = draftCitationSummary(line)
  if (citations) {
    const source = document.createElement('small')
    source.textContent = `Career 근거: ${citations}`
    block.append(source)
  }
  resumeDraftResult.append(block)
}

function renderResumeDraft(response) {
  resumeDraft = response
  resumeDraftResult.replaceChildren()
  const heading = document.createElement('strong')
  heading.textContent = `이 공고용 이력서 초안 · ${response.companyName} / ${response.jobTitle}`
  resumeDraftResult.append(heading)
  const meta = document.createElement('p')
  meta.className = 'resume-draft-meta'
  meta.textContent = isProductionService()
    ? `Career 근거 ${response.retrievedChunkCount}개 · 서비스에 자동 저장하지 않음`
    : `공고 원문 ${response.jobDescriptionLength}자 확인 후 생성 · Career 근거 ${response.retrievedChunkCount}개 · 서비스에 자동 저장하지 않음`
  resumeDraftResult.append(meta)
  appendResumeDraftBlock('헤드라인', response.draft?.headline)
  appendResumeDraftBlock('요약', response.draft?.summary)
  ;(response.draft?.selfIntroductionSections || []).forEach((section) => {
    appendResumeDraftBlock(section.title, { text: section.content, citations: section.citations })
  })
  ;(response.draft?.experienceBullets || []).forEach((line, index) => appendResumeDraftBlock(`경력 ${index + 1}`, line))
  ;(response.draft?.projectBullets || []).forEach((line, index) => appendResumeDraftBlock(`프로젝트 ${index + 1}`, line))
  ;(response.draft?.skills || []).forEach((line, index) => appendResumeDraftBlock(`스킬 ${index + 1}`, line))
  const actions = document.createElement('div')
  actions.className = 'actions single'
  const prepareFile = document.createElement('button')
  prepareFile.className = 'secondary'
  prepareFile.type = 'button'
  prepareFile.textContent = '검토 완료 · JobKorea 파일 준비'
  prepareFile.addEventListener('click', async () => {
    prepareFile.disabled = true
    prepareFile.textContent = '이력서 버전 저장 중...'
    try {
      const saved = await request('/v1/extension/page-resume-draft/approve', {
        method: 'POST',
        body: JSON.stringify({
          companyName: response.companyName,
          jobTitle: response.jobTitle,
          draft: response.draft,
        }),
      })
      const opened = await sendMessage({
        type: 'career-note-open-resume-export',
        resumeId: saved.resumeId,
      })
      if (!opened?.ok) throw new Error(opened?.error || 'JobKorea 파일 작성 페이지를 열지 못했습니다.')
      prepareFile.textContent = '서비스에서 파일 작성 열림'
    } catch (error) {
      prepareFile.disabled = false
      prepareFile.textContent = '검토 완료 · JobKorea 파일 준비'
      const failure = document.createElement('p')
      failure.className = 'muted'
      failure.textContent = error instanceof Error ? error.message : 'JobKorea 파일을 준비하지 못했습니다.'
      resumeDraftResult.append(failure)
    }
  })
  actions.append(prepareFile)
  resumeDraftResult.append(actions)
}

async function generateResumeDraftFromPage() {
  generateResume.disabled = true
  resumeDraftResult.replaceChildren()
  const message = document.createElement('span')
  message.className = 'muted'
  message.textContent = '현재 공고 전체 내용과 Career 근거를 바탕으로 이력서 초안을 생성하는 중입니다...'
  resumeDraftResult.append(message)
  try {
    if (!currentPosting) throw new Error('현재 JobKorea 공고를 먼저 읽어주세요.')
    if (!careerReady) throw new Error('서비스에서 경력·프로젝트를 등록하고 저장해주세요. 저장하면 맞춤 검색 데이터가 자동으로 업데이트됩니다.')
    const response = await request('/v1/extension/page-resume-draft', {
      method: 'POST',
      body: JSON.stringify({
        companyName: currentPosting.companyName,
        jobTitle: currentPosting.jobTitle,
        jobDescription: currentPosting.jobDescription,
        sourceUrl: currentPosting.sourceUrl,
      }),
    })
    renderResumeDraft(response)
  } catch (error) {
    resumeDraftResult.replaceChildren()
    const failure = document.createElement('span')
    failure.className = 'muted'
    failure.textContent = error instanceof Error ? error.message : '이력서 초안을 생성하지 못했습니다.'
    resumeDraftResult.append(failure)
  } finally {
    updateAnswerButtons()
  }
}

function renderReadiness(state) {
  careerReady = Boolean(state?.ready)
  readiness.classList.toggle('ready', careerReady)
  readiness.classList.toggle('blocked', !careerReady)
  readiness.replaceChildren()
  const title = document.createElement('strong')
  title.textContent = careerReady ? '답변 생성 준비 완료' : '서비스 데이터 준비가 필요합니다.'
  const counts = document.createElement('div')
  counts.className = 'readiness-counts'
  ;[`경력 ${state?.experienceCount || 0}개`, `프로젝트 ${state?.projectCount || 0}개`, `검색 근거 ${state?.indexedEvidenceCount || 0}개`].forEach((value) => {
    const badge = document.createElement('span')
    badge.className = 'badge'
    badge.textContent = value
    counts.append(badge)
  })
  const guidance = document.createElement('p')
  guidance.textContent = state?.guidance || '서비스에서 경력·프로젝트를 등록하면 맞춤 검색 데이터가 자동으로 업데이트됩니다.'
  readiness.append(title, counts, guidance)
  updateAnswerButtons()
}

async function loadReadiness() {
  try {
    renderReadiness(await request('/v1/extension/readiness'))
  } catch (error) {
    careerReady = false
    renderReadiness(null)
    throw error
  }
}

async function loadWorkspace() {
  setConnectionStatus('연결 상태를 확인하는 중입니다...')
  renderPosting()
  try {
    await request('/v1/extension/session')
    serviceConnected = true
    await loadReadiness()
    setConnectionStatus('Career Note 서비스에 연결되었습니다.')
  } catch (error) {
    serviceConnected = false
    careerReady = false
    updateAnswerButtons()
    setConnectionStatus(error instanceof Error ? error.message : '서비스 연결이 필요합니다.')
  }
}

const IGNORED_INPUT_TYPES = ['hidden', 'submit', 'button', 'reset', 'image', 'password']
const FIELD_ROLE_LABELS = {
  EMAIL: '이메일 직접 입력',
  PHONE: '연락처 직접 입력',
  PORTFOLIO: '포트폴리오 링크 직접 입력',
  NAME: '이름 직접 입력',
  CAREER_DESCRIPTION: '경력기술 답변',
  SELF_INTRODUCTION: '자기소개·지원동기 답변',
  UNKNOWN: '지원 질문 확인 필요',
}
const FIELD_ROLE_DESCRIPTIONS = {
  EMAIL: '이메일 주소를 직접 입력하는 칸입니다. AI 답변 생성 대상이 아닙니다.',
  PHONE: '휴대폰 번호나 연락처를 직접 입력하는 칸입니다. AI 답변 생성 대상이 아닙니다.',
  PORTFOLIO: 'GitHub·포트폴리오 URL을 직접 입력하는 칸입니다. AI 답변 생성 대상이 아닙니다.',
  NAME: '지원자 이름을 직접 입력하는 칸입니다. AI 답변 생성 대상이 아닙니다.',
  CAREER_DESCRIPTION: '관련 경력, 본인의 역할, 수행 내용과 성과를 작성하는 칸입니다.',
  SELF_INTRODUCTION: '자기소개, 지원동기, 입사 후 포부처럼 문장형 답변을 작성하는 칸입니다.',
  UNKNOWN: '필드 이름만으로 질문을 확인할 수 없습니다. 회사 홈페이지 지원 질문이라면 실제 질문을 선택 기능에 직접 넣어주세요.',
}
const FIELD_QUESTION_HINTS = {
  CAREER_DESCRIPTION: '이 직무와 관련된 경력, 본인의 역할, 수행 내용과 성과를 작성해주세요.',
  SELF_INTRODUCTION: '이 회사와 직무에 지원한 동기, 관련 강점과 입사 후 기여 방안을 작성해주세요.',
}

function fieldPresentation({ type, semanticRole, searchable }) {
  const isSearch = type === 'search' || ['검색', 'search', 'query', 'keyword'].some((term) => searchable.includes(term))
  if (isSearch) {
    return {
      controlKind: 'SEARCH_FILTER',
      displayLabel: '검색·필터 입력',
      description: '공고나 목록을 찾는 검색창입니다. 지원 답변 생성 및 자동 입력 대상이 아닙니다.',
      answerable: false,
    }
  }
  if (type === 'select') {
    return {
      controlKind: 'SELECT_OPTION',
      displayLabel: '선택 목록',
      description: '목록에서 항목을 직접 고르는 칸입니다. 답변 생성 및 자동 입력 대상이 아닙니다.',
      answerable: false,
    }
  }
  const directInput = ['EMAIL', 'PHONE', 'PORTFOLIO', 'NAME'].includes(semanticRole)
  return {
    controlKind: directInput ? 'PROFILE_VALUE' : 'TEXT_ANSWER',
    displayLabel: directInput ? FIELD_ROLE_LABELS[semanticRole] : FIELD_ROLE_LABELS[semanticRole] || FIELD_ROLE_LABELS.UNKNOWN,
    description: FIELD_ROLE_DESCRIPTIONS[semanticRole] || FIELD_ROLE_DESCRIPTIONS.UNKNOWN,
    answerable: !directInput,
  }
}

function inspectVisibleFields() {
  const elements = [...document.querySelectorAll('input, textarea, select, [contenteditable="true"]')]
  return elements.filter((element) => {
    if (element instanceof HTMLInputElement && IGNORED_INPUT_TYPES.includes(element.type)) return false
    const rect = element.getBoundingClientRect()
    const style = window.getComputedStyle(element)
    return rect.width > 0 && rect.height > 0 && style.visibility !== 'hidden' && style.display !== 'none'
  }).map((element, index) => {
    const input = element instanceof HTMLInputElement || element instanceof HTMLTextAreaElement || element instanceof HTMLSelectElement ? element : null
    const id = element.getAttribute('id')
    const linkedLabel = id ? document.querySelector(`label[for="${CSS.escape(id)}"]`) : null
    const parentLabel = element.closest('label')
    const fieldset = element.closest('fieldset')
    const legend = fieldset?.querySelector('legend')
    const label = linkedLabel?.textContent?.trim() || parentLabel?.textContent?.trim() || element.getAttribute('aria-label') || element.getAttribute('placeholder') || legend?.textContent?.trim() || element.getAttribute('name') || id || `입력 필드 ${index + 1}`
    const type = element instanceof HTMLInputElement ? element.type || 'text' : element.tagName.toLowerCase()
    const searchable = `${label} ${element.getAttribute('name') || ''} ${id || ''} ${element.getAttribute('autocomplete') || ''} ${element.getAttribute('placeholder') || ''} ${element.getAttribute('title') || ''}`.toLowerCase()
    const roleRules = [
      ['EMAIL', ['이메일', 'email', 'mail']],
      ['PHONE', ['휴대폰', '전화', '연락처', 'phone', 'mobile', 'tel']],
      ['PORTFOLIO', ['포트폴리오', 'portfolio', 'github', 'gitlab', '링크', 'url']],
      ['NAME', ['이름', '성명', 'name', '지원자명']],
      ['CAREER_DESCRIPTION', ['경력기술', '경력 사항', '경력사항', 'career', 'experience']],
      ['SELF_INTRODUCTION', ['자기소개', '지원동기', '입사 후', '성장과정', '지원 분야', 'motivation', 'introduction']],
    ]
    const semanticRole = roleRules.find(([, terms]) => terms.some((term) => searchable.includes(term)))?.[0] || 'UNKNOWN'
    const presentation = fieldPresentation({ type, semanticRole, searchable })
    return {
      fieldIndex: index,
      label: label.replace(/\s+/g, ' '),
      type,
      semanticRole,
      required: Boolean(input?.required || element.getAttribute('aria-required') === 'true'),
      maxLength: input?.maxLength ?? -1,
      ...presentation,
      questionHint: FIELD_QUESTION_HINTS[semanticRole] || '',
    }
  })
}

function renderFields(nextFields) {
  detectedFields = nextFields
  fields.replaceChildren()
  if (!nextFields.length) {
    const empty = document.createElement('li')
    empty.className = 'empty'
    empty.textContent = '화면에서 입력 필드를 찾지 못했습니다.'
    fields.append(empty)
    return
  }
  nextFields.forEach((field) => {
    const item = document.createElement('li')
    item.className = `field-item${field.answerable ? '' : ' not-answerable'}`
    const label = document.createElement('span')
    label.className = 'field-label'
    label.textContent = field.label
    item.append(label)
    const description = document.createElement('p')
    description.className = 'field-description'
    description.textContent = field.description
    item.append(description)
    const metadata = document.createElement('div')
    metadata.className = 'field-meta'
    ;[
      field.displayLabel,
      field.required && field.answerable ? '필수 입력' : field.answerable ? '선택 입력' : null,
      field.maxLength > 0 && field.answerable ? `${field.maxLength}자까지` : null,
    ].filter(Boolean).forEach((value) => {
      const badge = document.createElement('span')
      badge.className = `badge${field.answerable ? '' : ' muted-badge'}`
      badge.textContent = value
      metadata.append(badge)
    })
    item.append(metadata)
    if (field.answerable) {
      const select = document.createElement('button')
      select.className = 'secondary'
      select.type = 'button'
      select.textContent = field.fieldIndex === selectedFieldIndex ? '선택됨 · 질문 수정 가능' : '이 답변 칸을 선택'
      select.addEventListener('click', () => {
        selectedFieldIndex = field.fieldIndex
        question.value = field.questionHint || field.label
        if (field.maxLength > 0) maxLength.value = field.maxLength
        scanStatus.textContent = field.questionHint
          ? `선택된 답변 칸: ${field.label} · 기본 질문을 확인하고 수정해주세요.`
          : `선택된 답변 칸: ${field.label} · 홈페이지 지원 질문인 경우 실제 질문을 입력해주세요.`
        renderFields(detectedFields)
      })
      item.append(select)
    } else {
      const excluded = document.createElement('p')
      excluded.className = 'field-help'
      excluded.textContent = '이 항목은 답변 생성에서 제외됩니다.'
      item.append(excluded)
    }
    fields.append(item)
  })
}

function scanPage() {
  scan.disabled = true
  scanStatus.textContent = '현재 페이지를 검사하는 중입니다...'
  try {
    const nextFields = inspectVisibleFields()
    selectedFieldIndex = null
    renderFields(nextFields)
    const answerableCount = nextFields.filter((field) => field.answerable).length
    scanStatus.textContent = `${nextFields.length}개 컨트롤을 찾았습니다. 답변 작성 가능 ${answerableCount}개 · 검색/선택 등 제외 ${nextFields.length - answerableCount}개`
  } catch (error) {
    renderFields([])
    scanStatus.textContent = error instanceof Error ? error.message : '페이지를 검사하지 못했습니다.'
  } finally {
    scan.disabled = false
  }
}

function renderAnswer(answerResult) {
  result.replaceChildren()
  reviewedSuggestedValue = answerResult.suggestedValue || answerResult.answer || ''
  const heading = document.createElement('strong')
  heading.textContent = `검토용 답변 · ${answerResult.answerLength}자${answerResult.truncated ? ` · ${answerResult.maxLength}자로 잘라낸 제안` : ''}`
  result.append(heading)
  const editor = document.createElement('textarea')
  editor.value = reviewedSuggestedValue
  editor.maxLength = answerResult.maxLength || 100000
  editor.style.marginTop = '8px'
  editor.addEventListener('input', () => { reviewedSuggestedValue = editor.value })
  result.append(editor)
  const provenance = document.createElement('p')
  provenance.className = 'muted'
  provenance.textContent = `근거 ${answerResult.citations?.length || 0}개 · 현재 공고 ${answerResult.companyName} / ${answerResult.jobTitle} · 공고 저장 안 함`
  result.append(provenance)
  const actions = document.createElement('div')
  actions.className = 'actions single'
  const fill = document.createElement('button')
  fill.className = 'secondary'
  fill.type = 'button'
  fill.textContent = '검토 완료 · 현재 필드에 입력'
  fill.addEventListener('click', () => {
    if (selectedFieldIndex === null) {
      setResultMessage('먼저 검사 결과에서 입력할 필드를 선택해주세요.')
      return
    }
    if (!fillVisibleField(selectedFieldIndex, reviewedSuggestedValue)) {
      setResultMessage('선택한 필드에 입력할 수 없습니다. 텍스트 필드를 다시 선택해주세요.')
      return
    }
    setResultMessage('검토한 답변을 현재 필드에 입력했습니다. 최종 제출은 JobKorea에서 직접 진행해주세요.')
  })
  actions.append(fill)
  result.append(actions)
  ;(answerResult.citations || []).forEach((citation) => {
    const item = document.createElement('div')
    item.className = 'citation'
    item.textContent = `${citation.sourceType} · ${citation.content}`
    result.append(item)
  })
}

async function generateAnswer() {
  generate.disabled = true
  setResultMessage('근거 기반 답변 초안을 생성하는 중입니다...')
  try {
    const value = question.value.trim()
    const length = Number(maxLength.value)
    if (!value) throw new Error('회사 홈페이지 지원 질문을 입력해주세요.')
    if (!currentPosting) throw new Error('현재 JobKorea 공고를 먼저 감지해주세요.')
    if (!careerReady) throw new Error('서비스에서 경력·프로젝트를 등록하고 저장해주세요. 저장하면 맞춤 검색 데이터가 자동으로 업데이트됩니다.')
    const answer = await request('/v1/extension/page-application-answer', {
      method: 'POST',
      body: JSON.stringify({
        companyName: currentPosting.companyName,
        jobTitle: currentPosting.jobTitle,
        jobDescription: currentPosting.jobDescription,
        sourceUrl: currentPosting.sourceUrl,
        question: value,
        maxLength: Number.isFinite(length) && length > 0 ? length : null,
      }),
    })
    renderAnswer(answer)
  } catch (error) {
    setResultMessage(error instanceof Error ? error.message : '답변 초안을 생성하지 못했습니다.')
  } finally {
    updateAnswerButtons()
  }
}

function fillVisibleField(index, value) {
  const elements = [...document.querySelectorAll('input, textarea, select, [contenteditable="true"]')].filter((element) => {
    if (element instanceof HTMLInputElement && IGNORED_INPUT_TYPES.includes(element.type)) return false
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
  } else return false
  element.dispatchEvent(new Event('input', { bubbles: true }))
  element.dispatchEvent(new Event('change', { bubbles: true }))
  return true
}

async function connectWithCode() {
  exchange.disabled = true
  setConnectionStatus('연결 코드를 확인하는 중입니다...')
  try {
    const value = code.value.trim()
    if (!value) throw new Error('연결 코드를 입력해주세요.')
    const response = await request('/v1/extension/authorization-codes/exchange', { method: 'POST', body: JSON.stringify({ code: value }) })
    const saved = await sendMessage({ type: 'career-note-save-session', accessToken: response.accessToken, expiresInSeconds: response.accessTokenExpiresInSeconds })
    if (!saved?.ok) throw new Error(saved?.error || '확장 프로그램 세션을 저장하지 못했습니다.')
    code.value = ''
    await loadWorkspace()
  } catch (error) {
    setConnectionStatus(error instanceof Error ? error.message : '서비스 연결에 실패했습니다.')
  } finally {
    exchange.disabled = false
  }
}

async function saveServiceUrl() {
  const value = serviceUrl.value.trim().replace(/\/$/, '')
  if (!value) {
    setConnectionStatus('서비스 주소를 입력해주세요.')
    return
  }
  const response = await sendMessage({ type: 'career-note-set-service-url', value })
  setConnectionStatus(response?.ok ? '서비스 주소를 저장했습니다.' : (response?.error || '서비스 주소를 저장하지 못했습니다.'))
}

function openServiceConnectPage() {
  const baseUrl = serviceUrl.value.trim().replace(/\/$/, '')
  if (!baseUrl) {
    setConnectionStatus('서비스 주소를 입력해주세요.')
    return
  }
  const connectWindow = window.open('about:blank', '_blank')
  if (!connectWindow) {
    setConnectionStatus('새 탭이 차단되었습니다. 브라우저에서 팝업을 허용해주세요.')
    return
  }
  void chrome.storage.local.set({
    'career-note.service-base-url': baseUrl,
    [serviceBaseUrlMigrationKey]: true,
  })
  connectWindow.opener = null
  connectWindow.location.href = `${baseUrl}/extension/connect`
  setConnectionStatus('서비스 연결 페이지를 새 탭으로 열었습니다. 로그인 후 코드를 발급해주세요.')
}

function openCareerPage() {
  const baseUrl = serviceUrl.value.trim().replace(/\/$/, '')
  if (!baseUrl) {
    setConnectionStatus('서비스 주소를 입력해주세요.')
    return
  }
  const careerWindow = window.open(`${baseUrl}/career/experiences`, '_blank')
  if (!careerWindow) setConnectionStatus('새 탭이 차단되었습니다. 브라우저에서 팝업을 허용해주세요.')
  else careerWindow.opener = null
}

toggle.addEventListener('click', () => {
  const open = !panel.classList.contains('open')
  setPanelOpen(open)
  if (open) renderPosting()
})
close.addEventListener('click', () => setPanelOpen(false))
saveUrl.addEventListener('click', () => void saveServiceUrl())
openConnect.addEventListener('click', openServiceConnectPage)
exchange.addEventListener('click', () => void connectWithCode())
openCareer.addEventListener('click', openCareerPage)
refreshReadiness.addEventListener('click', async () => {
  refreshReadiness.disabled = true
  try {
    await loadReadiness()
    setConnectionStatus('서비스 데이터 준비 상태를 새로고침했습니다.')
  } catch (error) {
    setConnectionStatus(error instanceof Error ? error.message : '준비 상태를 확인하지 못했습니다.')
  } finally {
    refreshReadiness.disabled = false
  }
})
detectPosting.addEventListener('click', renderPosting)
generateResume.addEventListener('click', () => void generateResumeDraftFromPage())
scan.addEventListener('click', scanPage)
question.addEventListener('input', updateAnswerButtons)
generate.addEventListener('click', () => void generateAnswer())

chrome.runtime.onMessage.addListener((message) => {
  if (message?.type === 'career-note-toggle-panel') {
    setPanelOpen(!panel.classList.contains('open'))
    renderPosting()
  }
})

chrome.storage.local.get(['career-note.service-base-url', serviceBaseUrlMigrationKey]).then((saved) => {
  const savedValue = String(saved['career-note.service-base-url'] || '').replace(/\/$/, '')
  const value = savedValue && !(legacyLocalServiceBaseUrls.has(savedValue) && !saved[serviceBaseUrlMigrationKey])
    ? savedValue
    : defaultServiceBaseUrl
  serviceUrl.value = value
  if (savedValue !== value || !saved[serviceBaseUrlMigrationKey]) {
    void chrome.storage.local.set({
      'career-note.service-base-url': value,
      [serviceBaseUrlMigrationKey]: true,
    })
  }
})

setTimeout(() => { void loadWorkspace() }, 0)
