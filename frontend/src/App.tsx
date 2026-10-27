import { useEffect, useRef, useState } from 'react'
import type { ChangeEvent, FormEvent, ReactNode } from 'react'
import { Link, Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import { jsPDF } from 'jspdf'

type User = { id: string; email: string; displayName: string }
type AuthenticationResponse = { user: User; accessToken: string; refreshToken: string }
type SignupPolicy = { termsVersion: string; privacyVersion: string; emailVerificationRequired: boolean }
type SignupResponse = { email: string; verificationRequired: boolean; verificationExpiresAt: string | null }
type Experience = {
  id: string
  companyName: string
  role: string
  department: string | null
  title: string | null
  startDate: string | null
  endDate: string | null
  currentlyEmployed: boolean
  description: string
  techStack: string | null
  createdAt: string
  updatedAt: string
}
type PageResponse<T> = { content: T[]; page: number; size: number; totalElements: number; totalPages: number; first: boolean; last: boolean }
type ExperienceForm = { companyName: string; role: string; department: string; title: string; startDate: string; endDate: string; currentlyEmployed: boolean; description: string; techStack: string }
type Project = {
  id: string
  name: string
  role: string
  startDate: string | null
  endDate: string | null
  description: string
  techStack: string
  customerOrDomain: string | null
  repositoryUrl: string | null
  createdAt: string
  updatedAt: string
}
type ProjectForm = { name: string; role: string; startDate: string; endDate: string; description: string; techStack: string; customerOrDomain: string; repositoryUrl: string }
type Resume = { id: string; name: string; latestVersionNumber: number; createdAt: string; updatedAt: string }
type ResumeVersion = { id: string; resumeId: string; versionNumber: number; content: string; createdAt: string }
type ResumeGroup = { key: string; resume: Resume; resumes: Resume[] }
type ResumeApplicationQuestion = { id: string; resumeId: string; jobPostingId: string | null; question: string; answer: string; createdAt: string; updatedAt: string }
type ApplicationQuestionEditor = { localKey: string; id: string | null; jobPostingId: string; savedJobPostingId: string; question: string; savedQuestion: string; answer: string; savedAnswer: string }
type SelfIntroductionCitation = { citationId: string; sourceType: string; sourceId: string; content: string; similarity: number }
type SelfIntroductionAnswerResponse = { answer: string; citations: SelfIntroductionCitation[]; provider: string; model: string; promptVersion: string; retrievedChunkCount: number; diagnosticsVisible: boolean }
type ExtensionAuthorizationCodeResponse = { code: string; expiresAt: string; expiresInSeconds: number }
type ResumeDraftLine = { text: string; citations: Array<{ citationId: string }> }
type ResumeDraftSection = { title: string; content: string; citations: Array<{ citationId: string }> }
type ResumeDraftDocument = { headline: ResumeDraftLine; summary: ResumeDraftLine; selfIntroductionSections: ResumeDraftSection[]; experienceBullets: ResumeDraftLine[]; projectBullets: ResumeDraftLine[]; skills: ResumeDraftLine[] }
type ResumeDraftResponse = { id: string; jobPostingId: string; draft: ResumeDraftDocument; provider: string; model: string; promptVersion: string; retrievedChunkCount: number; createdAt: string }
type ResumeVersionEditor = { headline: string; selfIntroduction: string; careerDescription: string; skills: string }
type JobPosting = { id: string; title: string; companyName: string; description: string; sourceUrl: string | null; createdAt: string; updatedAt: string }
type JobPostingForm = { title: string; companyName: string; description: string; sourceUrl: string }
type JobPostingImportPreview = { sourceUrl: string; title: string; companyName: string; description: string; extractionSource: string; warnings: string[]; imageUrls: string[] }
type EmbeddingIndexResponse = { indexedExperienceChunks: number; indexedProjectChunks: number; embeddingModel: string; embeddingDimensions: number; indexedAt: string }
type ApiError = Error & { code?: string; fieldErrors?: Record<string, string> }

function resumeGroupKey(name: string) {
  return name.trim().toLocaleLowerCase('ko-KR')
}

function groupResumes(resumes: Resume[]): ResumeGroup[] {
  const groups = new Map<string, ResumeGroup>()
  resumes.forEach((resume) => {
    const key = resumeGroupKey(resume.name)
    const existing = groups.get(key)
    if (!existing) {
      groups.set(key, { key, resume, resumes: [resume] })
      return
    }
    existing.resumes.push(resume)
    if (new Date(resume.updatedAt).getTime() > new Date(existing.resume.updatedAt).getTime()) {
      existing.resume = resume
    }
  })
  return [...groups.values()]
}

function resumeGroupVersionCount(group: ResumeGroup) {
  return group.resumes.reduce((total, resume) => total + resume.latestVersionNumber, 0)
}

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? '/api'
const refreshTokenKey = 'ai-career.refresh-token'
const jobKoreaTemplateVersion = 'JOBKOREA_STANDARD_V2'
const maxJobKoreaSkills = 20
const supportedResumePhotoTypes = new Set(['image/jpeg', 'image/png', 'image/webp'])
const koreanDateTimeFormatter = new Intl.DateTimeFormat('ko-KR', {
  timeZone: 'Asia/Seoul',
  dateStyle: 'medium',
  timeStyle: 'short',
})

function formatKoreanDateTime(value: string) {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '-' : koreanDateTimeFormatter.format(date)
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const accessToken = sessionStorage.getItem('ai-career.access-token')
  const headers = new Headers(options.headers)
  headers.set('Content-Type', 'application/json')
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`)

  const response = await fetch(`${apiBaseUrl}${path}`, { ...options, headers })
  if (!response.ok) {
    const body = await response.json().catch(() => null) as { code?: string; detail?: string; fieldErrors?: Record<string, string> } | null
    const error = new Error(body?.detail ?? '요청을 처리하지 못했습니다.') as ApiError
    error.code = body?.code
    error.fieldErrors = body?.fieldErrors
    throw error
  }
  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

async function requestAllPages<T>(path: string): Promise<T[]> {
  const firstPage = await request<PageResponse<T>>(`${path}?page=0&size=100`)
  const remainingPages = await Promise.all(
    Array.from({ length: Math.max(0, firstPage.totalPages - 1) }, (_, index) =>
      request<PageResponse<T>>(`${path}?page=${index + 1}&size=100`)
    )
  )
  return [firstPage, ...remainingPages].flatMap((page) => page.content)
}

async function refreshCareerSearchData() {
  return request<EmbeddingIndexResponse>('/v1/retrieval/index', { method: 'POST' })
}

function indexingFailureMessage(error: unknown) {
  return error instanceof Error ? error.message : '맞춤 검색 데이터를 업데이트하지 못했습니다.'
}

function saveAuthentication(result: AuthenticationResponse) {
  sessionStorage.setItem('ai-career.access-token', result.accessToken)
  localStorage.setItem(refreshTokenKey, result.refreshToken)
  sessionStorage.setItem('ai-career.user', JSON.stringify(result.user))
}

function readUser(): User | null {
  const value = sessionStorage.getItem('ai-career.user')
  return value ? JSON.parse(value) as User : null
}

function clearAuthentication() {
  sessionStorage.removeItem('ai-career.access-token')
  sessionStorage.removeItem('ai-career.user')
  localStorage.removeItem(refreshTokenKey)
}

function App() {
  const [user, setUser] = useState<User | null>(readUser)
  const [isRestoringSession, setIsRestoringSession] = useState(() => !readUser())

  useEffect(() => {
    if (user) {
      setIsRestoringSession(false)
      return
    }
    const refreshToken = localStorage.getItem(refreshTokenKey)
    if (!refreshToken) {
      setIsRestoringSession(false)
      return
    }
    request<AuthenticationResponse>('/v1/auth/refresh', {
      method: 'POST',
      body: JSON.stringify({ refreshToken }),
    }).then((result) => {
      saveAuthentication(result)
      setUser(result.user)
    }).catch(() => {
      clearAuthentication()
    }).finally(() => {
      setIsRestoringSession(false)
    })
  }, [user])

  function handleAuthenticated(result: AuthenticationResponse) {
    saveAuthentication(result)
    setUser(result.user)
  }

  async function handleLogout() {
    const refreshToken = localStorage.getItem(refreshTokenKey)
    if (refreshToken) {
      await request('/v1/auth/logout', {
        method: 'POST',
        body: JSON.stringify({ refreshToken }),
      }).catch(() => undefined)
    }
    clearAuthentication()
    setUser(null)
  }

  async function handleDeleteAccount() {
    await request<void>('/v1/account', { method: 'DELETE' })
    clearAuthentication()
    setUser(null)
  }

  if (isRestoringSession) {
    return <main className="verification-page"><section className="verification-card"><p>서비스 연결 상태를 확인하는 중입니다...</p></section></main>
  }

  return (
    <Routes>
      <Route path="/login" element={user ? <Navigate to="/" replace /> : <AuthPage mode="login" onAuthenticated={handleAuthenticated} />} />
      <Route path="/signup" element={user ? <Navigate to="/" replace /> : <AuthPage mode="signup" onAuthenticated={handleAuthenticated} />} />
      <Route path="/verify-email" element={user ? <Navigate to="/" replace /> : <EmailVerificationPage />} />
      <Route path="/extension/connect" element={user ? <ExtensionConnectPage /> : <Navigate to="/login" replace state={{ from: '/extension/connect' }} />} />
      <Route path="/terms" element={<PolicyPage type="terms" />} />
      <Route path="/privacy" element={<PolicyPage type="privacy" />} />
      <Route path="*" element={user ? <Dashboard user={user} onLogout={handleLogout} onDeleteAccount={handleDeleteAccount} /> : <Navigate to="/login" replace />} />
    </Routes>
  )
}

function ExtensionConnectPage() {
  const [authorization, setAuthorization] = useState<ExtensionAuthorizationCodeResponse | null>(null)
  const [isIssuing, setIsIssuing] = useState(false)
  const [error, setError] = useState('')

  async function issueCode() {
    setIsIssuing(true)
    setError('')
    try {
      const result = await request<ExtensionAuthorizationCodeResponse>('/v1/extension/authorization-codes', { method: 'POST' })
      setAuthorization(result)
    } catch (issueError) {
      setError(issueError instanceof Error ? issueError.message : '연결 코드를 발급하지 못했습니다.')
    } finally {
      setIsIssuing(false)
    }
  }

  useEffect(() => {
    void issueCode()
  }, [])

  return <main className="extension-connect-page"><section className="extension-connect-card">
    <span className="eyebrow">CHROME EXTENSION</span>
    <h1>확장 프로그램 연결</h1>
    <p>Career Note와 JobKorea 지원 페이지를 연결합니다. 아래 코드는 한 번만 사용할 수 있고 2분 후 만료됩니다.</p>
    <ol className="extension-connect-steps"><li>확장 프로그램에서 서비스 연결을 시작합니다.</li><li>이 페이지에서 연결 코드를 발급합니다.</li><li>코드를 확장 프로그램에 입력합니다.</li></ol>
    {authorization ? <><div className="extension-auth-code" aria-live="polite"><span>ONE-TIME CODE</span><strong>{authorization.code}</strong><small>확장 프로그램에 입력하세요 · {authorization.expiresInSeconds}초 후 만료</small></div><button className="secondary-button" type="button" onClick={() => void issueCode()} disabled={isIssuing}>{isIssuing ? '코드 발급 중...' : '새 연결 코드 발급'}</button></> : <button className="primary-button" type="button" onClick={() => void issueCode()} disabled={isIssuing}>{isIssuing ? '코드 발급 중...' : '연결 코드 발급'}</button>}
    {error && <p className="form-error" role="alert">{error}</p>}
    {/*<p className="extension-connect-note">비밀번호, refresh token, JobKorea 로그인 정보는 확장 프로그램으로 전달되지 않습니다.</p>*/}
  </section></main>
}

function AuthPage({ mode, onAuthenticated }: { mode: 'login' | 'signup'; onAuthenticated: (result: AuthenticationResponse) => void }) {
  const navigate = useNavigate()
  const location = useLocation()
  const authState = location.state as { from?: string } | null
  const returnTo = authState?.from?.startsWith('/') ? authState.from : '/'
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [termsAccepted, setTermsAccepted] = useState(false)
  const [privacyAccepted, setPrivacyAccepted] = useState(false)
  const [signupPolicy, setSignupPolicy] = useState<SignupPolicy | null>(null)
  const [showPassword, setShowPassword] = useState(false)
  const [error, setError] = useState('')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [isSubmitting, setIsSubmitting] = useState(false)
  const isLogin = mode === 'login'

  useEffect(() => {
    if (isLogin) return
    request<SignupPolicy>('/v1/auth/signup-policy').then(setSignupPolicy).catch(() => setSignupPolicy(null))
  }, [isLogin])

  function validateSignup() {
    const errors: Record<string, string> = {}
    if (displayName.trim().length < 2) errors.displayName = '이름은 2자 이상 입력해주세요.'
    if (password.length < 12) errors.password = '비밀번호는 12자 이상 입력해주세요.'
    if (password.length > 72) errors.password = '비밀번호는 72자를 초과할 수 없습니다.'
    if (confirmPassword !== password) errors.confirmPassword = '비밀번호가 일치하지 않습니다.'
    if (!termsAccepted) errors.termsAccepted = '이용약관에 동의해주세요.'
    if (!privacyAccepted) errors.privacyAccepted = '개인정보 수집·이용에 동의해주세요.'
    setFieldErrors(errors)
    return Object.keys(errors).length === 0
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setFieldErrors({})
    if (!isLogin && !validateSignup()) return
    setIsSubmitting(true)
    try {
      if (!isLogin) {
        const signup = await request<SignupResponse>('/v1/auth/signup', { method: 'POST', body: JSON.stringify({ email: email.trim(), password, displayName: displayName.trim(), termsAccepted, privacyAccepted }) })
        if (signup.verificationRequired) {
          navigate(`/verify-email?email=${encodeURIComponent(signup.email)}`)
          return
        }
      }
      const result = await request<AuthenticationResponse>('/v1/auth/login', { method: 'POST', body: JSON.stringify({ email: email.trim(), password }) })
      onAuthenticated(result)
      navigate(returnTo, { replace: true })
    } catch (submissionError) {
      const apiError = submissionError as ApiError
      setFieldErrors(apiError.fieldErrors ?? {})
      if (apiError.code === 'EMAIL_VERIFICATION_REQUIRED') {
        navigate(`/verify-email?email=${encodeURIComponent(email.trim())}`)
        return
      }
      setError(apiError.code === 'EMAIL_ALREADY_REGISTERED'
        ? '이미 가입된 이메일입니다. 로그인해 주세요.'
        : apiError.code === 'RATE_LIMIT_EXCEEDED'
          ? '요청이 많습니다. 잠시 후 다시 시도해 주세요.'
          : apiError.message || '인증에 실패했습니다.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="auth-layout">
      <section className="auth-brand-panel">
        <span className="auth-brand-kicker">YOUR CAREER, IN ONE PLACE</span>
        <h1>Career<br /><em>Note</em></h1>
        <p>경력을 정리하고, 나에게 맞는 기회를 더 빠르게 준비하세요.</p>
        <ul className="auth-benefits" aria-label="서비스 주요 기능">
          <li><span>01</span><div><strong>경력을 한 곳에</strong><small>직무 경력과 프로젝트를 안전하게 기록합니다.</small></div></li>
          <li><span>02</span><div><strong>공고에 맞춰 준비</strong><small>저장한 채용공고와 내 경력을 연결합니다.</small></div></li>
          <li><span>03</span><div><strong>검토 가능한 초안</strong><small>자동으로 만든 결과도 근거와 함께 직접 확인합니다.</small></div></li>
        </ul>
        <div className="brand-mark" aria-hidden="true">↗</div>
      </section>
      <section className="auth-form-panel">
        <div className="auth-form-wrap">
          <span className="eyebrow">{isLogin ? 'WELCOME BACK' : 'GET STARTED'}</span>
          <h2>{isLogin ? '다시 만나서 반가워요' : '커리어 공간을 시작하세요'}</h2>
          <p className="muted auth-form-intro">{isLogin ? '계정에 로그인해 작업을 이어가세요.' : '1분 안에 시작할 수 있어요. 나중에 내용을 수정할 수 있습니다.'}</p>
          {isLogin && new URLSearchParams(location.search).get('verified') === '1' && <p className="form-success" role="status">이메일 확인이 완료되었습니다. 이제 로그인할 수 있어요.</p>}
          <form onSubmit={submit} className="auth-form">
            {!isLogin && <label><span>이름</span><input autoComplete="name" value={displayName} onChange={(event) => { setDisplayName(event.target.value); setFieldErrors((current) => ({ ...current, displayName: '' })) }} placeholder="홍길동" required minLength={2} maxLength={100} />{fieldErrors.displayName && <small className="field-error">{fieldErrors.displayName}</small>}</label>}
            <label><span>이메일</span><input autoComplete="email" type="email" value={email} onChange={(event) => { setEmail(event.target.value); setFieldErrors((current) => ({ ...current, email: '' })) }} placeholder="you@example.com" required maxLength={320} />{fieldErrors.email && <small className="field-error">{fieldErrors.email}</small>}</label>
            <label><span>비밀번호</span><div className="password-input-wrap"><input autoComplete={isLogin ? 'current-password' : 'new-password'} type={showPassword ? 'text' : 'password'} value={password} onChange={(event) => { setPassword(event.target.value); setFieldErrors((current) => ({ ...current, password: '', confirmPassword: '' })) }} placeholder={isLogin ? '비밀번호를 입력하세요' : '12~72자'} required minLength={isLogin ? 1 : 12} maxLength={72} /><button className="password-toggle" type="button" onClick={() => setShowPassword((current) => !current)} aria-label={showPassword ? '비밀번호 숨기기' : '비밀번호 보기'}>{showPassword ? '숨기기' : '보기'}</button></div>{!isLogin && <small className={password.length >= 12 ? 'form-hint valid-hint' : 'form-hint'}>{password.length >= 12 ? '✓ 사용 가능한 길이입니다.' : '12자 이상 입력하고 다른 서비스와 다른 비밀번호를 사용하세요.'}</small>}{fieldErrors.password && <small className="field-error">{fieldErrors.password}</small>}</label>
            {!isLogin && <label><span>비밀번호 확인</span><input autoComplete="new-password" type={showPassword ? 'text' : 'password'} value={confirmPassword} onChange={(event) => { setConfirmPassword(event.target.value); setFieldErrors((current) => ({ ...current, confirmPassword: '' })) }} placeholder="비밀번호를 다시 입력하세요" required maxLength={72} />{confirmPassword && !fieldErrors.confirmPassword && <small className={confirmPassword === password ? 'form-hint valid-hint' : 'form-hint mismatch-hint'}>{confirmPassword === password ? '✓ 비밀번호가 일치합니다.' : '비밀번호가 아직 일치하지 않습니다.'}</small>}{fieldErrors.confirmPassword && <small className="field-error">{fieldErrors.confirmPassword}</small>}</label>}
            {!isLogin && <fieldset className="consent-group"><legend>필수 동의</legend><label className="consent-check"><input type="checkbox" checked={termsAccepted} onChange={(event) => { setTermsAccepted(event.target.checked); setFieldErrors((current) => ({ ...current, termsAccepted: '' })) }} /><span><Link to="/terms" target="_blank">이용약관</Link>에 동의합니다.</span></label>{fieldErrors.termsAccepted && <small className="field-error">{fieldErrors.termsAccepted}</small>}<label className="consent-check"><input type="checkbox" checked={privacyAccepted} onChange={(event) => { setPrivacyAccepted(event.target.checked); setFieldErrors((current) => ({ ...current, privacyAccepted: '' })) }} /><span><Link to="/privacy" target="_blank">개인정보 수집·이용 안내</Link>에 동의합니다.</span></label>{fieldErrors.privacyAccepted && <small className="field-error">{fieldErrors.privacyAccepted}</small>}{signupPolicy && <small className="policy-version">약관 {signupPolicy.termsVersion} · 개인정보 {signupPolicy.privacyVersion}</small>}</fieldset>}
            {error && <p className="form-error" role="alert">{error}</p>}
            <button className="primary-button auth-submit" disabled={isSubmitting} type="submit">{isSubmitting ? '처리 중...' : isLogin ? '로그인' : '계정 만들고 시작하기'}</button>
          </form>
          <p className="auth-trust-note"><span>✓</span> 입력한 커리어 데이터는 내 계정 안에서만 관리됩니다.</p>
          <p className="auth-switch">{isLogin ? '처음 방문하셨나요?' : '이미 계정이 있으신가요?'} <Link to={isLogin ? '/signup' : '/login'}>{isLogin ? '회원가입' : '로그인'}</Link></p>
        </div>
      </section>
    </main>
  )
}

function EmailVerificationPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const email = new URLSearchParams(location.search).get('email')?.trim() ?? ''
  const [code, setCode] = useState('')
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [resendSeconds, setResendSeconds] = useState(60)

  useEffect(() => {
    if (resendSeconds <= 0) return
    const timer = window.setInterval(() => setResendSeconds((current) => Math.max(0, current - 1)), 1000)
    return () => window.clearInterval(timer)
  }, [resendSeconds > 0])

  async function verify(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!/^\d{6}$/.test(code)) {
      setError('메일에 표시된 숫자 6자리를 입력해주세요.')
      return
    }
    setIsSubmitting(true)
    setError('')
    try {
      await request<void>('/v1/auth/verify-email', {
        method: 'POST',
        body: JSON.stringify({ email, code }),
      })
      navigate('/login?verified=1', { replace: true })
    } catch (verificationError) {
      const apiError = verificationError as ApiError
      setError(apiError.code === 'EMAIL_VERIFICATION_INVALID'
        ? '인증코드가 올바르지 않거나 만료되었습니다. 다시 확인해주세요.'
        : apiError.code === 'RATE_LIMIT_EXCEEDED'
          ? '요청이 많습니다. 잠시 후 다시 시도해주세요.'
          : '이메일 확인을 완료하지 못했습니다.')
    } finally {
      setIsSubmitting(false)
    }
  }

  async function resend() {
    setError('')
    setMessage('')
    try {
      await request<void>('/v1/auth/resend-verification', {
        method: 'POST',
        body: JSON.stringify({ email }),
      })
      setMessage('새 인증코드를 요청했습니다. 메일함을 확인해주세요.')
      setResendSeconds(60)
    } catch (resendError) {
      const apiError = resendError as ApiError
      setError(apiError.code === 'RATE_LIMIT_EXCEEDED'
        ? '재요청이 많습니다. 잠시 후 다시 시도해주세요.'
        : '인증 메일을 다시 보내지 못했습니다.')
    }
  }

  if (!email) return <Navigate to="/signup" replace />

  return <main className="verification-page"><section className="verification-card">
    <span className="verification-icon" aria-hidden="true">✉</span>
    <span className="eyebrow">CHECK YOUR EMAIL</span>
    <h1>이메일을 확인해주세요</h1>
    <p><strong>{email}</strong>으로 보낸 숫자 6자리를 입력하면 가입이 완료됩니다.</p>
    <form onSubmit={verify} className="verification-form">
      <label htmlFor="verification-code">인증코드</label>
      <input id="verification-code" inputMode="numeric" autoComplete="one-time-code" value={code} onChange={(event) => { setCode(event.target.value.replace(/\D/g, '').slice(0, 6)); setError('') }} placeholder="000000" maxLength={6} autoFocus />
      {error && <p className="form-error" role="alert">{error}</p>}
      {message && <p className="form-success" role="status">{message}</p>}
      <button className="primary-button auth-submit" type="submit" disabled={isSubmitting || code.length !== 6}>{isSubmitting ? '확인 중...' : '인증하고 가입 완료'}</button>
    </form>
    <div className="verification-actions"><button className="text-button" type="button" disabled={resendSeconds > 0} onClick={() => void resend()}>{resendSeconds > 0 ? `${resendSeconds}초 후 다시 받기` : '인증코드 다시 받기'}</button><Link to="/signup">이메일 다시 입력</Link></div>
    <p className="verification-help">메일이 보이지 않으면 스팸함을 확인해주세요. 인증코드는 5분 후 만료됩니다.</p>
  </section></main>
}

function PolicyPage({ type }: { type: 'terms' | 'privacy' }) {
  const isTerms = type === 'terms'
  return <main className="policy-page"><article className="policy-card"><Link className="policy-back" to="/signup">← 회원가입으로 돌아가기</Link><span className="eyebrow">SERVICE POLICY</span><h1>{isTerms ? '이용약관' : '개인정보 수집·이용 안내'}</h1><p className="policy-effective">시행일: 2026년 10월 21일</p>{isTerms ? <>
    <section><h2>1. 서비스 목적</h2><p>사용자가 경력, 프로젝트, 이력서와 채용공고를 정리하고 지원 문서 초안을 준비할 수 있는 작업 공간을 제공합니다.</p></section>
    <section><h2>2. 계정과 이용 책임</h2><p>사용자는 정확한 정보를 입력하고 계정 접근 정보를 안전하게 관리해야 합니다. 타인의 계정 사용, 서비스 방해, 불법적인 자동 접근은 허용되지 않습니다.</p></section>
    <section><h2>3. 작성 결과 검토</h2><p>자동으로 준비된 문서는 참고용 초안입니다. 제출 전 사실관계, 표현, 개인정보 포함 여부를 사용자가 직접 확인해야 합니다.</p></section>
    <section><h2>4. 서비스 변경과 이용 종료</h2><p>안정성 또는 보안을 위해 기능이 변경되거나 일시 중단될 수 있습니다. 사용자는 계정 삭제와 보유 데이터 처리 방법을 운영자에게 요청할 수 있습니다.</p></section>
  </> : <>
    <section><h2>1. 수집 항목</h2><p>가입 시 이름과 이메일, 암호화된 비밀번호를 처리합니다. 서비스 이용 중 사용자가 입력한 경력, 프로젝트, 이력서, 채용공고와 보안·오류 대응을 위한 최소 접속 기록을 처리합니다.</p></section>
    <section><h2>2. 이용 목적</h2><p>계정 식별, 사용자별 데이터 분리, 문서 작성 기능 제공, 보안 사고 예방과 서비스 품질 개선에 사용합니다.</p></section>
    <section><h2>3. 외부 처리</h2><p>이메일 확인 과정에서는 가입 이메일 주소가 메일 발송 제공업체에 전달됩니다. 사용자가 자동 작성 기능을 실행하면 필요한 입력 일부가 설정된 문서 작성 제공업체에 전달될 수 있습니다. 비밀번호와 로그인 토큰은 전달하지 않습니다.</p></section>
    <section><h2>4. 보유와 삭제</h2><p>계정과 입력 데이터는 서비스 이용 기간 동안 보관하며, 삭제 요청 시 법령 또는 보안상 보존 의무가 있는 항목을 제외하고 처리 목적에 따라 삭제합니다.</p></section>
    <section><h2>5. 사용자 권리</h2><p>사용자는 본인 데이터의 조회, 수정, 삭제를 요청할 수 있습니다. 정식 배포 전 운영자 정보와 개인정보 문의 채널을 배포 환경에 맞게 최종 고지해야 합니다.</p></section>
  </>}</article></main>
}

function Dashboard({ user, onLogout, onDeleteAccount }: { user: User; onLogout: () => Promise<void>; onDeleteAccount: () => Promise<void> }) {
  const location = useLocation()
  const navigate = useNavigate()
  const [isLoggingOut, setIsLoggingOut] = useState(false)
  const [isDeletingAccount, setIsDeletingAccount] = useState(false)
  const careerSection = resolveCareerSection(location.pathname)

  async function logout() {
    setIsLoggingOut(true)
    await onLogout()
    navigate('/login')
  }

  async function deleteAccount() {
    if (!window.confirm('회원탈퇴하면 계정에 다시 로그인할 수 없고 모든 로그인 세션이 종료됩니다. 저장한 커리어 데이터는 운영 정책에 따라 DB에 보존될 수 있습니다. 계속할까요?')) {
      return
    }
    setIsDeletingAccount(true)
    try {
      await onDeleteAccount()
      navigate('/login', { replace: true })
    } catch (error) {
      window.alert(error instanceof Error ? error.message : '회원탈퇴를 처리하지 못했습니다.')
    } finally {
      setIsDeletingAccount(false)
    }
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <Link className="logo" to="/" aria-label="Career Note 홈">AC<span>•</span><small>CAREER NOTE</small></Link>
        <div className="sidebar-label">WORKSPACE</div>
        <nav>
          <Link className={location.pathname === '/' ? 'nav-item active' : 'nav-item'} to="/"><span className="nav-icon">⌂</span><span className="nav-label">홈</span></Link>
          <Link className={careerSection ? 'nav-item active' : 'nav-item'} to="/career/experiences"><span className="nav-icon">↗</span><span className="nav-label">커리어</span></Link>
          <Link className={location.pathname === '/resumes' ? 'nav-item active' : 'nav-item'} to="/resumes"><span className="nav-icon">▤</span><span className="nav-label">이력서</span></Link>
          <Link className={location.pathname === '/job-postings' ? 'nav-item active' : 'nav-item'} to="/job-postings"><span className="nav-icon">◌</span><span className="nav-label">채용공고</span></Link>
        </nav>
        <div className="sidebar-footer">
          <span className="avatar">{user.displayName.charAt(0)}</span>
          <div><strong>{user.displayName}</strong><small>{user.email}</small></div>
          <button className="logout-button" onClick={() => void logout()} disabled={isLoggingOut || isDeletingAccount} aria-label="로그아웃"><span aria-hidden="true">↪</span><small>로그아웃</small></button>
          <button className="account-delete-button" onClick={() => void deleteAccount()} disabled={isLoggingOut || isDeletingAccount} aria-label="회원탈퇴"><span aria-hidden="true">×</span><small>{isDeletingAccount ? '처리 중' : '회원탈퇴'}</small></button>
        </div>
      </aside>
      <main className="main-content">
        {careerSection ? <CareerPageContent section={careerSection} /> : location.pathname === '/resumes' ? <ResumePageContent user={user} /> : location.pathname === '/job-postings' ? <JobPostingPageContent user={user} /> : <OverviewContent user={user} />}
      </main>
    </div>
  )
}

type CareerSection = 'experiences' | 'projects'

function resolveCareerSection(pathname: string): CareerSection | null {
  if (pathname === '/career' || pathname === '/career/experiences' || pathname === '/experiences') return 'experiences'
  if (pathname === '/career/projects' || pathname === '/projects') return 'projects'
  return null
}

function CareerPageContent({ section }: { section: CareerSection }) {
  return <>
    <div className="career-toolbar">
      <nav className="career-tabs" aria-label="Career data sections">
        <Link className={section === 'experiences' ? 'career-tab active' : 'career-tab'} to="/career/experiences" aria-current={section === 'experiences' ? 'page' : undefined}>경력</Link>
        <Link className={section === 'projects' ? 'career-tab active' : 'career-tab'} to="/career/projects" aria-current={section === 'projects' ? 'page' : undefined}>프로젝트</Link>
      </nav>
      <small className="career-index-auto-help">경력·프로젝트를 저장하면 맞춤 검색 데이터가 자동으로 업데이트됩니다.</small>
    </div>
    {section === 'experiences' ? <ExperiencePageContent /> : <ProjectPageContent />}
  </>
}

function OverviewContent({ user }: { user: User }) {
  const [counts, setCounts] = useState<{ experiences: number | null; projects: number | null; resumes: number | null; jobPostings: number | null }>({
    experiences: null,
    projects: null,
    resumes: null,
    jobPostings: null,
  })
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState('')

  async function loadCounts() {
    setIsLoading(true)
    setLoadError('')

    const [experiences, projects, resumes, jobPostings] = await Promise.allSettled([
      request<PageResponse<Experience>>('/v1/experiences?page=0&size=1'),
      request<PageResponse<Project>>('/v1/projects?page=0&size=1'),
      request<PageResponse<Resume>>('/v1/resumes?page=0&size=1'),
      request<PageResponse<JobPosting>>('/v1/job-postings?page=0&size=1'),
    ])

    setCounts({
      experiences: experiences.status === 'fulfilled' ? experiences.value.totalElements : null,
      projects: projects.status === 'fulfilled' ? projects.value.totalElements : null,
      resumes: resumes.status === 'fulfilled' ? resumes.value.totalElements : null,
      jobPostings: jobPostings.status === 'fulfilled' ? jobPostings.value.totalElements : null,
    })
    if ([experiences, projects, resumes, jobPostings].some((result) => result.status === 'rejected')) {
      setLoadError('일부 현황을 불러오지 못했습니다.')
    }
    setIsLoading(false)
  }

  useEffect(() => { void loadCounts() }, [])

  const displayCount = (count: number | null) => isLoading && count === null ? '…' : count?.toString() ?? '—'
  const completedWorkspaceSections = Object.values(counts).filter((count) => count !== null && count > 0).length

  return <>
    <header className="topbar"><div><span className="eyebrow">YOUR WORKSPACE</span><h1>안녕하세요, {user.displayName}님</h1><p className="topbar-subtitle">오늘은 어디부터 준비해볼까요?</p></div><span className="status-pill"><span />내 작업 공간</span></header>
    <section className="hero-card"><div className="hero-card-content"><span className="eyebrow">START HERE</span><h2>다음 기회를<br /><em>차근차근 준비해요.</em></h2><p>경력을 먼저 기록하고 관심 공고를 저장하면, 공고에 맞는 초안을 알아서 준비합니다.</p><div className="hero-actions"><Link className="hero-action-primary" to="/career/experiences">경력 기록하기 <span>→</span></Link><Link className="hero-action-secondary" to="/job-postings">공고 저장하기</Link></div></div><div className="hero-orbit" aria-hidden="true"><span>+</span><span>✦</span><span>→</span></div></section>
    <section className="stats-grid" aria-busy={isLoading}><StatCard label="경력" value={displayCount(counts.experiences)} action="경력 추가" to="/career/experiences" /><StatCard label="프로젝트" value={displayCount(counts.projects)} action="프로젝트 추가" to="/career/projects" /><StatCard label="이력서" value={displayCount(counts.resumes)} action="이력서 만들기" to="/resumes" /><StatCard label="채용공고" value={displayCount(counts.jobPostings)} action="공고 저장" to="/job-postings" /></section>
    {loadError && <p className="overview-load-error" role="alert">{loadError} <button className="text-button" onClick={() => void loadCounts()}>다시 시도</button></p>}
    <section className="next-section"><div><span className="eyebrow">NEXT UP</span><h2>작업 공간을 채워보세요</h2><p>처음이라면 경력 하나만 기록해도 충분합니다. 저장한 데이터는 언제든 수정할 수 있어요.</p><div className="next-actions"><Link className="text-button" to="/career/experiences">첫 경력 기록하기 ↗</Link><Link className="text-button" to="/job-postings">관심 공고 저장하기 ↗</Link></div></div><div className="progress-ring" aria-label={`완료한 작업 공간 ${completedWorkspaceSections}개, 전체 4개`}><strong>{isLoading ? '…' : completedWorkspaceSections}</strong><span>/ 4</span></div></section>
  </>
}

type TxtFieldDefinition<T extends string> = { key: T; aliases: string[] }

const experienceTxtTemplate = `회사명: Acme Inc.
직무: Backend Developer
부서: 플랫폼개발팀
직급: 선임
시작일: 2023-01-01
종료일: 재직 중
기술 스택: Java, Spring Boot, PostgreSQL
설명:
Spring Boot API를 개발했습니다.
PostgreSQL 성능을 개선했습니다.`

const projectTxtTemplate = `프로젝트명: Career Portfolio
역할: Backend Developer
시작일: 2025-01-01
종료일: 현재
고객사/Domain: B2B SaaS 플랫폼
기술 스택: Java, Spring Boot, PostgreSQL
Repository URL: https://github.com/example/project
설명:
커리어 데이터를 관리하는 서비스를 개발했습니다.`

const experienceTxtFields: TxtFieldDefinition<Exclude<keyof ExperienceForm, 'currentlyEmployed'>>[] = [
  { key: 'companyName', aliases: ['회사명', '회사', 'company', 'companyName'] },
  { key: 'role', aliases: ['직무', '역할', '직무 역할', '포지션', 'role', 'position'] },
  { key: 'department', aliases: ['부서', '부서명', 'department', 'team'] },
  { key: 'title', aliases: ['직급', '직책', '직급/직책', 'title', 'job title'] },
  { key: 'startDate', aliases: ['시작일', '입사일', '시작 날짜', 'startDate', 'start date'] },
  { key: 'endDate', aliases: ['종료일', '퇴사일', '종료 날짜', 'endDate', 'end date'] },
  { key: 'techStack', aliases: ['기술 스택', '기술', '사용 기술', 'techStack', 'tech stack', 'stack'] },
  { key: 'description', aliases: ['설명', '업무 내용', '주요 업무', '성과', 'description'] },
]

const projectTxtFields: TxtFieldDefinition<keyof ProjectForm>[] = [
  { key: 'name', aliases: ['프로젝트명', '프로젝트 이름', 'name', 'projectName', 'project name'] },
  { key: 'role', aliases: ['역할', '담당 역할', 'role', 'position'] },
  { key: 'startDate', aliases: ['시작일', '시작 날짜', 'startDate', 'start date'] },
  { key: 'endDate', aliases: ['종료일', '종료 날짜', 'endDate', 'end date'] },
  { key: 'customerOrDomain', aliases: ['고객사/Domain', '고객사', '도메인', 'customerOrDomain', 'customer', 'domain'] },
  { key: 'techStack', aliases: ['기술 스택', '기술', '사용 기술', 'techStack', 'tech stack', 'stack'] },
  { key: 'repositoryUrl', aliases: ['Repository URL', '저장소 URL', '리포지토리', 'repositoryUrl', 'repository'] },
  { key: 'description', aliases: ['설명', '프로젝트 설명', '구현 내용', 'description'] },
]

function normalizeTxtLabel(label: string) {
  return label.normalize('NFKC').toLowerCase().replace(/[^a-z0-9가-힣]/g, '')
}

function parseLabeledTxt<T extends string>(text: string, fields: TxtFieldDefinition<T>[]) {
  const aliases = new Map<string, T>()
  fields.forEach(({ key, aliases: fieldAliases }) => fieldAliases.forEach((alias) => aliases.set(normalizeTxtLabel(alias), key)))

  const values: Partial<Record<T, string>> = {}
  let activeKey: T | null = null
  let matchedFields = 0
  const sanitizedText = text.replace(/^\uFEFF/, '').replace(/\*{2,}/g, '')

  sanitizedText.split(/\r?\n/).forEach((line) => {
    const match = line.match(/^(.{1,40}?)[=:]\s*(.*)$/)
    const matchedKey = match ? aliases.get(normalizeTxtLabel(match[1])) : undefined
    if (match && matchedKey) {
      activeKey = matchedKey
      values[matchedKey] = match[2].trim()
      matchedFields += 1
      return
    }
    if (!activeKey) return
    const previous = values[activeKey] ?? ''
    values[activeKey] = previous ? `${previous}\n${line}` : line
  })

  Object.keys(values).forEach((key) => {
    const typedKey = key as T
    values[typedKey] = values[typedKey]?.trim()
  })
  return { values, matchedFields }
}

function normalizeImportedDate(value: string, allowCurrent: boolean): string | null {
  const trimmed = value.trim()
  if (allowCurrent && !trimmed) return ''
  if (allowCurrent && /^(현재|진행중|재직중|present|current|ongoing)$/i.test(trimmed)) return ''
  const match = trimmed.match(/^(\d{4})[./-](\d{1,2})[./-](\d{1,2})$/)
  if (!match) return null
  const year = Number(match[1])
  const month = Number(match[2])
  const day = Number(match[3])
  const date = new Date(Date.UTC(year, month - 1, day))
  if (date.getUTCFullYear() !== year || date.getUTCMonth() !== month - 1 || date.getUTCDate() !== day) return null
  return `${match[1]}-${match[2].padStart(2, '0')}-${match[3].padStart(2, '0')}`
}

function TxtImportModal({ title, template, onClose, onApply }: { title: string; template: string; onClose: () => void; onApply: (text: string) => string | null }) {
  const [text, setText] = useState('')
  const [error, setError] = useState('')

  useEffect(() => {
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    const closeOnEscape = (event: KeyboardEvent) => { if (event.key === 'Escape') onClose() }
    window.addEventListener('keydown', closeOnEscape)
    return () => {
      document.body.style.overflow = previousOverflow
      window.removeEventListener('keydown', closeOnEscape)
    }
  }, [onClose])

  async function readTxtFile(file: File | undefined) {
    setError('')
    if (!file) return
    if (!file.name.toLowerCase().endsWith('.txt')) {
      setError('.txt 파일만 선택할 수 있습니다.')
      return
    }
    if (file.size > 1024 * 1024) {
      setError('TXT 파일은 1MB 이하여야 합니다.')
      return
    }
    try {
      setText(await file.text())
    } catch {
      setError('파일을 읽지 못했습니다. UTF-8 TXT 파일인지 확인해주세요.')
    }
  }

  function apply() {
    const applyError = onApply(text)
    if (applyError) {
      setError(applyError)
      return
    }
    onClose()
  }

  return <div className="modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose() }}>
    <section className="txt-import-modal" role="dialog" aria-modal="true" aria-labelledby="txt-import-title">
      <header className="modal-heading"><div><span className="eyebrow">TXT IMPORT</span><h2 id="txt-import-title">{title}</h2></div><button className="modal-close" onClick={onClose} aria-label="닫기">×</button></header>
      <p className="modal-description">아래 형식의 내용을 붙여넣거나 UTF-8 TXT 파일을 선택하세요. 인식된 값은 폼에만 채워지며 자동 저장되지 않습니다.</p>
      <div className="txt-import-grid">
        <div className="txt-template"><strong>입력 형식 예시</strong><pre>{template}</pre></div>
        <label className="txt-input-label">가져올 내용<textarea value={text} onChange={(event) => { setText(event.target.value); setError('') }} rows={12} autoFocus placeholder="항목명: 값을 붙여넣으세요." /></label>
      </div>
      <div className="file-input-row"><label className="secondary-button compact-button">TXT 파일 선택<input type="file" accept=".txt,text/plain" onChange={(event) => void readTxtFile(event.target.files?.[0])} /></label><span>최대 1MB · UTF-8</span></div>
      {error && <p className="form-error modal-error" role="alert">{error}</p>}
      <footer className="modal-actions"><button className="text-button" onClick={onClose}>취소</button><button className="primary-button compact-button" onClick={apply} disabled={!text.trim()}>폼에 적용</button></footer>
    </section>
  </div>
}

function JobPostingUrlImportModal({ onClose, onApply }: { onClose: () => void; onApply: (preview: JobPostingImportPreview) => void }) {
  const [sourceUrl, setSourceUrl] = useState('')
  const [preview, setPreview] = useState<JobPostingImportPreview | null>(null)
  const [error, setError] = useState('')
  const [isLoading, setIsLoading] = useState(false)

  useEffect(() => {
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    const closeOnEscape = (event: KeyboardEvent) => { if (event.key === 'Escape') onClose() }
    window.addEventListener('keydown', closeOnEscape)
    return () => {
      document.body.style.overflow = previousOverflow
      window.removeEventListener('keydown', closeOnEscape)
    }
  }, [onClose])

  async function loadPreview() {
    const trimmedUrl = sourceUrl.trim()
    if (!/^https?:\/\/.+/i.test(trimmedUrl)) {
      setError('http 또는 https 형식의 채용공고 URL을 입력해주세요.')
      return
    }
    setError('')
    setPreview(null)
    setIsLoading(true)
    try {
      const result = await request<JobPostingImportPreview>('/v1/job-postings/import-preview', {
        method: 'POST',
        body: JSON.stringify({ sourceUrl: trimmedUrl }),
      })
      setPreview(result)
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : '채용공고를 가져오지 못했습니다.')
    } finally {
      setIsLoading(false)
    }
  }

  return <div className="modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose() }}>
    <section className="txt-import-modal url-import-modal" role="dialog" aria-modal="true" aria-labelledby="job-url-import-title">
      <header className="modal-heading"><div><span className="eyebrow">URL IMPORT</span><h2 id="job-url-import-title">채용공고 URL 가져오기</h2></div><button className="modal-close" onClick={onClose} aria-label="닫기">×</button></header>
      <p className="modal-description">공개된 채용공고 URL에서 제목, 회사명, Job Description을 읽어옵니다. 미리보기에서 확인한 뒤 폼에 적용하며, 이 단계에서는 자동 저장되지 않습니다.</p>
      <label className="url-import-label">채용공고 URL<input className="url-import-input" value={sourceUrl} onChange={(event) => { setSourceUrl(event.target.value); setPreview(null); setError('') }} onKeyDown={(event) => { if (event.key === 'Enter') void loadPreview() }} placeholder="https://www.example.com/jobs/123" autoFocus /></label>
      {isLoading && <p className="import-status">페이지를 읽고 있습니다...</p>}
      {error && <p className="form-error modal-error" role="alert">{error}</p>}
      {preview && <div className="url-import-preview">
        <div className="preview-meta"><span className="eyebrow">PREVIEW · {preview.extractionSource}</span><span>{preview.sourceUrl}</span></div>
        <h3>{preview.title || '제목을 찾지 못했습니다.'}</h3>
        <strong>{preview.companyName || '회사명을 찾지 못했습니다.'}</strong>
        <p>{preview.description || 'Job Description을 찾지 못했습니다.'}</p>
        {preview.imageUrls.length > 0 && <div className="import-image-preview">
          <strong>상세 공고 원문 이미지</strong>
          {preview.imageUrls.map((imageUrl) => <img key={imageUrl} src={imageUrl} alt="채용공고 상세 내용" loading="lazy" />)}
        </div>}
        {preview.warnings.length > 0 && <div className="import-warning"><strong>확인 필요</strong><ul>{preview.warnings.map((warning) => <li key={warning}>{warning}</li>)}</ul></div>}
      </div>}
      <footer className="modal-actions"><button className="text-button" onClick={onClose}>취소</button>{preview ? <><button className="secondary-button compact-button" onClick={() => { setPreview(null); setError('') }}>다시 조회</button><button className="primary-button compact-button" onClick={() => onApply(preview)}>폼에 적용</button></> : <button className="primary-button compact-button" onClick={() => void loadPreview()} disabled={isLoading || !sourceUrl.trim()}>{isLoading ? '가져오는 중...' : '공고 가져오기'}</button>}</footer>
    </section>
  </div>
}

const emptyExperienceForm: ExperienceForm = { companyName: '', role: '', department: '', title: '', startDate: '', endDate: '', currentlyEmployed: false, description: '', techStack: '' }

function ExperiencePageContent() {
  const location = useLocation()
  const [experiences, setExperiences] = useState<Experience[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [form, setForm] = useState<ExperienceForm>(emptyExperienceForm)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [formError, setFormError] = useState('')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [deletingId, setDeletingId] = useState<string | null>(null)
  const [isImportOpen, setIsImportOpen] = useState(false)
  const [indexNotice, setIndexNotice] = useState<{ message: string; error: boolean } | null>(null)

  async function loadExperiences(targetPage = page) {
    setIsLoading(true)
    setLoadError('')
    try {
      const result = await request<PageResponse<Experience>>(`/v1/experiences?page=${targetPage}&size=5`)
      setExperiences(result.content)
      setPage(result.page)
      setTotalPages(result.totalPages)
      setTotalElements(result.totalElements)
    } catch (error) {
      setLoadError(error instanceof Error ? error.message : '경력 목록을 불러오지 못했습니다.')
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => { void loadExperiences(0) }, [])

  useEffect(() => {
    const editId = new URLSearchParams(location.search).get('edit')
    if (!editId) return
    void request<Experience>(`/v1/experiences/${editId}`)
      .then(startEdit)
      .catch((error) => setLoadError(error instanceof Error ? error.message : '수정할 경력을 불러오지 못했습니다.'))
  }, [location.search])

  function updateField(field: keyof ExperienceForm, value: string) {
    setForm((current) => ({ ...current, [field]: value }))
    setFieldErrors((current) => ({ ...current, [field]: '' }))
  }

  function startCreate() {
    setEditingId(null)
    setForm(emptyExperienceForm)
    setFieldErrors({})
    setFormError('')
  }

  function startEdit(experience: Experience) {
    setEditingId(experience.id)
    setForm({ companyName: experience.companyName, role: experience.role, department: experience.department ?? '', title: experience.title ?? '', startDate: experience.startDate ?? '', endDate: experience.endDate ?? '', currentlyEmployed: experience.currentlyEmployed, description: experience.description, techStack: experience.techStack ?? '' })
    setFieldErrors({})
    setFormError('')
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  function applyTxtImport(text: string) {
    const { values, matchedFields } = parseLabeledTxt(text, experienceTxtFields)
    if (matchedFields === 0) return '인식할 수 있는 항목이 없습니다. 예시처럼 “항목명: 값” 형식으로 입력해주세요.'
    if (values.startDate !== undefined) {
      const normalized = normalizeImportedDate(values.startDate, false)
      if (normalized === null) return '시작일은 YYYY-MM-DD 형식으로 입력해주세요.'
      values.startDate = normalized
    }
    let currentlyEmployed = false
    if (values.endDate !== undefined) {
      currentlyEmployed = /^(현재|진행중|재직중|present|current|ongoing)$/i.test(values.endDate.trim())
      const normalized = normalizeImportedDate(values.endDate, true)
      if (normalized === null) return '종료일은 YYYY-MM-DD 또는 재직 중으로 입력해주세요.'
      values.endDate = normalized
    }
    setForm((current) => ({ ...current, ...values, currentlyEmployed: values.endDate === undefined ? current.currentlyEmployed : currentlyEmployed }))
    setFieldErrors({})
    setFormError('')
    window.scrollTo({ top: 0, behavior: 'smooth' })
    return null
  }

  function validateForm() {
    const errors: Record<string, string> = {}
    if (!form.companyName.trim()) errors.companyName = '회사명을 입력해주세요.'
    if (!form.role.trim()) errors.role = '직무를 입력해주세요.'
    if (!form.currentlyEmployed && form.endDate && form.startDate && form.endDate < form.startDate) errors.endDate = '종료일은 시작일 이후여야 합니다.'
    if (!form.description.trim()) errors.description = '경력 설명을 입력해주세요.'
    setFieldErrors(errors)
    return Object.keys(errors).length === 0
  }

  async function submitExperience(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setFormError('')
    if (!validateForm()) return
    setIsSubmitting(true)
    setIndexNotice(null)
    const payload = { companyName: form.companyName.trim(), role: form.role.trim(), department: form.department.trim() || null, title: form.title.trim() || null, startDate: form.startDate || null, endDate: form.currentlyEmployed ? null : form.endDate || null, currentlyEmployed: form.currentlyEmployed, description: form.description.trim(), techStack: form.techStack.trim() || null }
    try {
      await request<Experience>(editingId ? `/v1/experiences/${editingId}` : '/v1/experiences', { method: editingId ? 'PUT' : 'POST', body: JSON.stringify(payload) })
      let indexError = ''
      try {
        await refreshCareerSearchData()
      } catch (error) {
        indexError = indexingFailureMessage(error)
      }
      startCreate()
      await loadExperiences(editingId ? page : 0)
      setIndexNotice(indexError
        ? { message: `경력은 저장됐지만 맞춤 검색 데이터 업데이트에 실패했습니다: ${indexError}`, error: true }
        : { message: '경력을 저장하고 맞춤 검색 데이터를 자동 업데이트했습니다.', error: false })
    } catch (error) {
      const apiError = error as ApiError
      setFormError(apiError.message)
      setFieldErrors(apiError.fieldErrors ?? {})
    } finally {
      setIsSubmitting(false)
    }
  }

  async function deleteExperience(id: string) {
    if (!window.confirm('이 경력을 삭제할까요? 삭제된 경력은 목록에서 숨겨집니다.')) return
    setDeletingId(id)
    setIndexNotice(null)
    try {
      await request<void>(`/v1/experiences/${id}`, { method: 'DELETE' })
      let indexError = ''
      try {
        await refreshCareerSearchData()
      } catch (error) {
        indexError = indexingFailureMessage(error)
      }
      await loadExperiences(page > 0 && experiences.length === 1 ? page - 1 : page)
      setIndexNotice(indexError
        ? { message: `경력은 삭제됐지만 맞춤 검색 데이터 업데이트에 실패했습니다: ${indexError}`, error: true }
        : { message: '경력을 삭제하고 맞춤 검색 데이터를 자동 업데이트했습니다.', error: false })
    } catch (error) {
      setLoadError(error instanceof Error ? error.message : '경력을 삭제하지 못했습니다.')
    } finally {
      setDeletingId(null)
    }
  }

  return <>
    <header className="page-heading"><div><span className="eyebrow">CAREER DATA · HISTORY</span><h1>경력</h1><p>직장에서 맡았던 일과 결과를 기록해두면 채용공고와 연결할 수 있습니다.</p></div><div className="heading-actions"><button className="secondary-button compact-button" onClick={() => setIsImportOpen(true)}>TXT로 빠르게 입력</button><button className="primary-button compact-button" onClick={startCreate}>+ 경력 추가</button></div></header>
    <section className="experience-form-card">
      <div className="section-title"><div><span className="eyebrow">{editingId ? 'EDIT CAREER' : 'NEW CAREER'}</span><h2>{editingId ? '경력 수정' : '새 경력 기록'}</h2><p className="form-helper">기술 스택까지 기록하면 이력서 스킬과 경력기술서에 정확하게 반영됩니다.</p></div>{editingId && <button className="text-button" onClick={startCreate}>취소</button>}</div>
      <form className="experience-form" onSubmit={submitExperience}>
        <FormField label="회사명" value={form.companyName} onChange={(value) => updateField('companyName', value)} error={fieldErrors.companyName} placeholder="예: Acme Inc." />
        <FormField label="직무 / 역할" value={form.role} onChange={(value) => updateField('role', value)} error={fieldErrors.role} placeholder="예: Backend Developer" />
        <FormField label="부서 (선택)" value={form.department} onChange={(value) => updateField('department', value)} error={fieldErrors.department} placeholder="예: 플랫폼개발팀" />
        <FormField label="직급 / 직책 (선택)" value={form.title} onChange={(value) => updateField('title', value)} error={fieldErrors.title} placeholder="예: 선임 개발자" />
        <FormField label="시작일 (선택)" type="date" value={form.startDate} onChange={(value) => updateField('startDate', value)} error={fieldErrors.startDate} />
        <div className="period-end-field"><FormField label="종료일 (선택)" type="date" value={form.endDate} onChange={(value) => updateField('endDate', value)} error={fieldErrors.endDate} disabled={form.currentlyEmployed} /><label className="current-period-check"><input type="checkbox" checked={form.currentlyEmployed} onChange={(event) => setForm((current) => ({ ...current, currentlyEmployed: event.target.checked, endDate: event.target.checked ? '' : current.endDate }))} /><span>재직 중</span></label></div>
        <FormField label="기술 스택 (선택)" value={form.techStack} onChange={(value) => updateField('techStack', value)} error={fieldErrors.techStack} placeholder="예: Java, Spring Boot, PostgreSQL" />
        <label className="wide-field">설명<textarea value={form.description} onChange={(event) => updateField('description', event.target.value)} placeholder="담당 업무, 성과, 사용 기술을 기록해주세요." maxLength={10000} rows={4} />{fieldErrors.description && <small className="field-error">{fieldErrors.description}</small>}</label>
        {formError && <p className="form-error wide-field" role="alert">{formError}</p>}
        <div className="form-actions wide-field"><button className="primary-button compact-button" disabled={isSubmitting} type="submit">{isSubmitting ? '저장·검색 데이터 업데이트 중...' : editingId ? '변경 저장' : '경력 저장'}</button></div>
      </form>
    </section>
    {indexNotice && <p className={`career-index-message career-index-notice${indexNotice.error ? ' career-index-error' : ''}`} role={indexNotice.error ? 'alert' : 'status'}>{indexNotice.message}</p>}
    <section className="experience-list-section"><div className="list-heading"><div><span className="eyebrow">YOUR CAREER HISTORY</span><h2>{totalElements}개의 경력</h2></div><button className="text-button" onClick={() => void loadExperiences(page)}>새로고침</button></div>
      {isLoading && <div className="list-state">경력 목록을 불러오는 중입니다...</div>}
      {!isLoading && loadError && <div className="list-state error-state" role="alert">{loadError}<button className="text-button" onClick={() => void loadExperiences(page)}>다시 시도</button></div>}
      {!isLoading && !loadError && experiences.length === 0 && <div className="list-state">아직 등록된 경력이 없습니다. 위에서 첫 경력을 기록해보세요.</div>}
      {!isLoading && !loadError && experiences.length > 0 && <div className="experience-list">{experiences.map((experience) => { const period = formatCareerPeriod(experience.startDate, experience.endDate, experience.currentlyEmployed ? '재직 중' : null); return <article className={`experience-item${period ? '' : ' no-period'}`} key={experience.id}>{period && <div className="experience-date">{period}</div>}<div className="experience-main"><h3>{experience.role}</h3><strong>{experience.companyName}</strong>{(experience.department || experience.title) && <div className="career-meta">{[experience.department, experience.title].filter(Boolean).join(' · ')}</div>}<p>{experience.description}</p>{experience.techStack ? <div className="tech-stack">{experience.techStack}</div> : <div className="career-missing-hint">기술 스택을 추가하면 이력서에 반영됩니다.</div>}</div><div className="item-actions"><button className="text-button" onClick={() => startEdit(experience)}>수정</button><button className="text-button danger-button" onClick={() => void deleteExperience(experience.id)} disabled={deletingId === experience.id}>{deletingId === experience.id ? '삭제 중' : '삭제'}</button></div></article> })}</div>}
      {!isLoading && !loadError && totalPages > 1 && <div className="pagination"><button className="text-button" disabled={page === 0} onClick={() => void loadExperiences(page - 1)}>← 이전</button><span>{page + 1} / {totalPages}</span><button className="text-button" disabled={page >= totalPages - 1} onClick={() => void loadExperiences(page + 1)}>다음 →</button></div>}
    </section>
    {isImportOpen && <TxtImportModal title="경력 TXT 가져오기" template={experienceTxtTemplate} onClose={() => setIsImportOpen(false)} onApply={applyTxtImport} />}
  </>
}

const emptyProjectForm: ProjectForm = { name: '', role: '', startDate: '', endDate: '', description: '', techStack: '', customerOrDomain: '', repositoryUrl: '' }

function ProjectPageContent() {
  const location = useLocation()
  const [projects, setProjects] = useState<Project[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [form, setForm] = useState<ProjectForm>(emptyProjectForm)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [formError, setFormError] = useState('')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [deletingId, setDeletingId] = useState<string | null>(null)
  const [isImportOpen, setIsImportOpen] = useState(false)
  const [indexNotice, setIndexNotice] = useState<{ message: string; error: boolean } | null>(null)

  async function loadProjects(targetPage = page) {
    setIsLoading(true)
    setLoadError('')
    try {
      const result = await request<PageResponse<Project>>(`/v1/projects?page=${targetPage}&size=5`)
      setProjects(result.content)
      setPage(result.page)
      setTotalPages(result.totalPages)
      setTotalElements(result.totalElements)
    } catch (error) {
      setLoadError(error instanceof Error ? error.message : '프로젝트 목록을 불러오지 못했습니다.')
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => { void loadProjects(0) }, [])

  useEffect(() => {
    const editId = new URLSearchParams(location.search).get('edit')
    if (!editId) return
    void request<Project>(`/v1/projects/${editId}`)
      .then(startEdit)
      .catch((error) => setLoadError(error instanceof Error ? error.message : '수정할 프로젝트를 불러오지 못했습니다.'))
  }, [location.search])

  function updateField(field: keyof ProjectForm, value: string) {
    setForm((current) => ({ ...current, [field]: value }))
    setFieldErrors((current) => ({ ...current, [field]: '' }))
  }

  function startCreate() {
    setEditingId(null)
    setForm(emptyProjectForm)
    setFieldErrors({})
    setFormError('')
  }

  function startEdit(project: Project) {
    setEditingId(project.id)
    setForm({ name: project.name, role: project.role, startDate: project.startDate ?? '', endDate: project.endDate ?? '', description: project.description, techStack: project.techStack, customerOrDomain: project.customerOrDomain ?? '', repositoryUrl: project.repositoryUrl ?? '' })
    setFieldErrors({})
    setFormError('')
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  function applyTxtImport(text: string) {
    const { values, matchedFields } = parseLabeledTxt(text, projectTxtFields)
    if (matchedFields === 0) return '인식할 수 있는 항목이 없습니다. 예시처럼 “항목명: 값” 형식으로 입력해주세요.'
    if (values.startDate !== undefined) {
      const normalized = normalizeImportedDate(values.startDate, false)
      if (normalized === null) return '시작일은 YYYY-MM-DD 형식으로 입력해주세요.'
      values.startDate = normalized
    }
    if (values.endDate !== undefined) {
      const normalized = normalizeImportedDate(values.endDate, true)
      if (normalized === null) return '종료일은 YYYY-MM-DD 또는 현재로 입력해주세요.'
      values.endDate = normalized
    }
    setForm((current) => ({ ...current, ...values }))
    setFieldErrors({})
    setFormError('')
    window.scrollTo({ top: 0, behavior: 'smooth' })
    return null
  }

  function validateForm() {
    const errors: Record<string, string> = {}
    if (!form.name.trim()) errors.name = '프로젝트명을 입력해주세요.'
    if (!form.role.trim()) errors.role = '역할을 입력해주세요.'
    if (form.endDate && form.startDate && form.endDate < form.startDate) errors.endDate = '종료일은 시작일 이후여야 합니다.'
    if (!form.description.trim()) errors.description = '프로젝트 설명을 입력해주세요.'
    if (!form.techStack.trim()) errors.techStack = '기술 스택을 입력해주세요.'
    if (form.repositoryUrl && !/^https?:\/\/.+/i.test(form.repositoryUrl.trim())) errors.repositoryUrl = 'http 또는 https URL을 입력해주세요.'
    setFieldErrors(errors)
    return Object.keys(errors).length === 0
  }

  async function submitProject(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setFormError('')
    if (!validateForm()) return
    setIsSubmitting(true)
    setIndexNotice(null)
    const payload = { ...form, name: form.name.trim(), role: form.role.trim(), startDate: form.startDate || null, endDate: form.endDate || null, description: form.description.trim(), techStack: form.techStack.trim(), customerOrDomain: form.customerOrDomain.trim() || null, repositoryUrl: form.repositoryUrl.trim() || null }
    try {
      await request<Project>(editingId ? `/v1/projects/${editingId}` : '/v1/projects', { method: editingId ? 'PUT' : 'POST', body: JSON.stringify(payload) })
      let indexError = ''
      try {
        await refreshCareerSearchData()
      } catch (error) {
        indexError = indexingFailureMessage(error)
      }
      const nextPage = editingId ? page : 0
      startCreate()
      await loadProjects(nextPage)
      setIndexNotice(indexError
        ? { message: `프로젝트는 저장됐지만 맞춤 검색 데이터 업데이트에 실패했습니다: ${indexError}`, error: true }
        : { message: '프로젝트를 저장하고 맞춤 검색 데이터를 자동 업데이트했습니다.', error: false })
    } catch (error) {
      const apiError = error as ApiError
      setFormError(apiError.message)
      setFieldErrors(apiError.fieldErrors ?? {})
    } finally {
      setIsSubmitting(false)
    }
  }

  async function deleteProject(id: string) {
    if (!window.confirm('이 프로젝트를 삭제할까요? 삭제된 프로젝트는 목록에서 숨겨집니다.')) return
    setDeletingId(id)
    setIndexNotice(null)
    try {
      await request<void>(`/v1/projects/${id}`, { method: 'DELETE' })
      let indexError = ''
      try {
        await refreshCareerSearchData()
      } catch (error) {
        indexError = indexingFailureMessage(error)
      }
      await loadProjects(page > 0 && projects.length === 1 ? page - 1 : page)
      setIndexNotice(indexError
        ? { message: `프로젝트는 삭제됐지만 맞춤 검색 데이터 업데이트에 실패했습니다: ${indexError}`, error: true }
        : { message: '프로젝트를 삭제하고 맞춤 검색 데이터를 자동 업데이트했습니다.', error: false })
    } catch (error) {
      setLoadError(error instanceof Error ? error.message : '프로젝트를 삭제하지 못했습니다.')
    } finally {
      setDeletingId(null)
    }
  }

  return <>
    <header className="page-heading"><div><span className="eyebrow">CAREER DATA · PROJECT</span><h1>프로젝트</h1><p>직접 만든 결과물과 사용한 기술을 기록해두면 나만의 근거가 됩니다.</p></div><div className="heading-actions"><button className="secondary-button compact-button" onClick={() => setIsImportOpen(true)}>TXT로 빠르게 입력</button><button className="primary-button compact-button" onClick={startCreate}>+ 프로젝트 추가</button></div></header>
    <section className="experience-form-card">
      <div className="section-title"><div><span className="eyebrow">{editingId ? 'EDIT PROJECT' : 'NEW PROJECT'}</span><h2>{editingId ? '프로젝트 수정' : '새 프로젝트 기록'}</h2><p className="form-helper">프로젝트명과 역할부터 시작하고, 나머지는 나중에 보완해도 괜찮습니다.</p></div>{editingId && <button className="text-button" onClick={startCreate}>취소</button>}</div>
      <form className="experience-form" onSubmit={submitProject}>
        <FormField label="프로젝트명" value={form.name} onChange={(value) => updateField('name', value)} error={fieldErrors.name} placeholder="예: Career Portfolio" />
        <FormField label="역할" value={form.role} onChange={(value) => updateField('role', value)} error={fieldErrors.role} placeholder="예: Backend Developer" />
        <FormField label="시작일 (선택)" type="date" value={form.startDate} onChange={(value) => updateField('startDate', value)} error={fieldErrors.startDate} />
        <FormField label="종료일 (선택)" type="date" value={form.endDate} onChange={(value) => updateField('endDate', value)} error={fieldErrors.endDate} />
        <FormField label="고객사 / 업무 도메인 (선택)" value={form.customerOrDomain} onChange={(value) => updateField('customerOrDomain', value)} error={fieldErrors.customerOrDomain} placeholder="예: 금융권 결제 플랫폼, B2B SaaS" />
        <FormField label="기술 스택" value={form.techStack} onChange={(value) => updateField('techStack', value)} error={fieldErrors.techStack} placeholder="예: Java, Spring Boot, PostgreSQL" />
        <FormField label="저장소 URL (선택)" value={form.repositoryUrl} onChange={(value) => updateField('repositoryUrl', value)} error={fieldErrors.repositoryUrl} placeholder="https://github.com/..." />
        <label className="wide-field">설명<textarea value={form.description} onChange={(event) => updateField('description', event.target.value)} placeholder="문제, 구현 내용, 성과를 기록해주세요." maxLength={10000} rows={4} />{fieldErrors.description && <small className="field-error">{fieldErrors.description}</small>}</label>
        {formError && <p className="form-error wide-field" role="alert">{formError}</p>}
        <div className="form-actions wide-field"><button className="primary-button compact-button" disabled={isSubmitting} type="submit">{isSubmitting ? '저장·검색 데이터 업데이트 중...' : editingId ? '변경 저장' : '프로젝트 저장'}</button></div>
      </form>
    </section>
    {indexNotice && <p className={`career-index-message career-index-notice${indexNotice.error ? ' career-index-error' : ''}`} role={indexNotice.error ? 'alert' : 'status'}>{indexNotice.message}</p>}
    <section className="experience-list-section"><div className="list-heading"><div><span className="eyebrow">YOUR PROJECTS</span><h2>{totalElements}개의 프로젝트</h2></div><button className="text-button" onClick={() => void loadProjects(page)}>새로고침</button></div>
      {isLoading && <div className="list-state">프로젝트 목록을 불러오는 중입니다...</div>}
      {!isLoading && loadError && <div className="list-state error-state" role="alert">{loadError}<button className="text-button" onClick={() => void loadProjects(page)}>다시 시도</button></div>}
      {!isLoading && !loadError && projects.length === 0 && <div className="list-state">아직 등록된 프로젝트가 없습니다. 위에서 첫 프로젝트를 기록해보세요.</div>}
      {!isLoading && !loadError && projects.length > 0 && <div className="experience-list">{projects.map((project) => { const period = formatCareerPeriod(project.startDate, project.endDate, project.startDate && !project.endDate ? '진행 중' : null); return <article className={`experience-item${period ? '' : ' no-period'}`} key={project.id}>{period && <div className="experience-date">{period}</div>}<div className="experience-main"><h3>{project.name}</h3><strong>{project.role}</strong>{project.customerOrDomain && <div className="job-source-label">{project.customerOrDomain}</div>}<p>{project.description}</p><div className="tech-stack">{project.techStack}</div>{project.repositoryUrl && <a className="repo-link" href={project.repositoryUrl} target="_blank" rel="noreferrer">Repository ↗</a>}</div><div className="item-actions"><button className="text-button" onClick={() => startEdit(project)}>수정</button><button className="text-button danger-button" onClick={() => void deleteProject(project.id)} disabled={deletingId === project.id}>{deletingId === project.id ? '삭제 중' : '삭제'}</button></div></article> })}</div>}
      {!isLoading && !loadError && totalPages > 1 && <div className="pagination"><button className="text-button" disabled={page === 0} onClick={() => void loadProjects(page - 1)}>← 이전</button><span>{page + 1} / {totalPages}</span><button className="text-button" disabled={page >= totalPages - 1} onClick={() => void loadProjects(page + 1)}>다음 →</button></div>}
    </section>
    {isImportOpen && <TxtImportModal title="프로젝트 TXT 가져오기" template={projectTxtTemplate} onClose={() => setIsImportOpen(false)} onApply={applyTxtImport} />}
  </>
}

type ResumeExportSection = { title: string; lines: string[] }
type ParsedResumeExport = { headline: string; summary: string; selfIntroductions: JobKoreaSelfIntroduction[]; sections: ResumeExportSection[] }
type JobKoreaEducation = {
  schoolType: string
  schoolName: string
  admissionMonth: string
  graduationMonth: string
  status: string
  major: string
  gpa: string
  gpaScale: string
  additionalMajor: string
}
type JobKoreaCareer = {
  companyName: string
  hideCompanyName: boolean
  department: string
  startMonth: string
  endMonth: string
  isCurrent: boolean
  title: string
  job: string
  salary: string
  duties: string
}
type JobKoreaAdditionalRecord = { name: string; organization: string; date: string; description: string }
type JobKoreaOverseasExperience = { country: string; organization: string; startMonth: string; endMonth: string; description: string }
type JobKoreaLanguage = { language: string; level: string; score: string }
type JobKoreaPortfolio = { title: string; url: string; description: string }
type JobKoreaSelfIntroduction = { title: string; content: string }
const jobKoreaJobCategories = ['백엔드개발자', '웹개발자', '서버개발자', '응용프로그램개발자', '프론트엔드개발자', '데이터엔지니어', 'DevOps·인프라', '직접 입력']
type JobKoreaExportProfile = {
  name: string
  phone: string
  mobile: string
  email: string
  address: string
  birthDate: string
  gender: string
  photoDataUrl: string
  desiredJobKeywords: string
  skills: string
  educations: JobKoreaEducation[]
  careers: JobKoreaCareer[]
  careerDescription: string
  activities: JobKoreaAdditionalRecord[]
  trainings: JobKoreaAdditionalRecord[]
  certificates: JobKoreaAdditionalRecord[]
  awards: JobKoreaAdditionalRecord[]
  overseas: JobKoreaOverseasExperience[]
  languages: JobKoreaLanguage[]
  portfolios: JobKoreaPortfolio[]
  veteran: boolean
  employmentProtection: boolean
  employmentSupport: boolean
  disability: boolean
  militaryStatus: string
  enlistmentDate: string
  dischargeDate: string
  militaryDetails: string
  selfIntroductions: JobKoreaSelfIntroduction[]
  employmentType: string
  desiredSalary: string
  desiredLocations: string
  industryKeywords: string
}

const resumeExportSectionAliases: Record<string, string> = {
  experience: '경력사항',
  experiences: '경력사항',
  '경력': '경력사항',
  '경력사항': '경력사항',
  project: '프로젝트',
  projects: '프로젝트',
  '프로젝트': '프로젝트',
  skill: '기술 및 역량',
  skills: '기술 및 역량',
  '스킬': '기술 및 역량',
  '기술': '기술 및 역량',
  '기술스택': '기술 및 역량',
  '기술 및 역량': '기술 및 역량',
  '경력기술서': '경력기술서',
}

function stripResumeCitation(value: string) {
  return value.replace(/\s+\[C\d+(?:\s*,\s*C\d+)*\]\s*$/, '').trim()
}

function parseResumeExport(content: string): ParsedResumeExport {
  let headline = ''
  let summary = ''
  let inSelfIntroduction = false
  const selfIntroductions: JobKoreaSelfIntroduction[] = []
  const sections: ResumeExportSection[] = []
  let currentSection: ResumeExportSection | null = null
  let currentSelfIntroduction: JobKoreaSelfIntroduction | null = null

  function sectionFor(title: string) {
    const existing = sections.find((section) => section.title === title)
    if (existing) return existing
    const created = { title, lines: [] }
    sections.push(created)
    return created
  }

  for (const rawLine of content.split(/\r?\n/)) {
    const line = stripResumeCitation(rawLine)
    if (!line) continue
    const labeled = line.match(/^([^:]{1,50}):\s*(.*)$/)
    const label = labeled?.[1].trim().toLocaleLowerCase('ko-KR')
    const value = labeled?.[2] ? stripResumeCitation(labeled[2]) : ''

    if (label === 'headline' || label === '헤드라인' || label === '희망직무') {
      headline = value
      inSelfIntroduction = false
      currentSelfIntroduction = null
      continue
    }
    if (label === 'summary' || label === '요약' || label === '자기소개' || label === '자기소개서') {
      if (value) summary = value
      inSelfIntroduction = true
      currentSelfIntroduction = null
      continue
    }

    const selfIntroductionHeader = line.match(/^\[(?:자기소개서|대주제)\s+\d+\]\s*(.+)$/)
    if (inSelfIntroduction && selfIntroductionHeader) {
      currentSelfIntroduction = { title: selfIntroductionHeader[1].trim(), content: '' }
      selfIntroductions.push(currentSelfIntroduction)
      continue
    }

    const sectionTitle = label ? resumeExportSectionAliases[label] : undefined
    if (sectionTitle) {
      inSelfIntroduction = false
      currentSelfIntroduction = null
      currentSection = sectionFor(sectionTitle)
      if (value) currentSection.lines.push(value.replace(/^[-•]\s*/, '').trim())
      continue
    }

    if (labeled && label) {
      inSelfIntroduction = false
      currentSelfIntroduction = null
      currentSection = sectionFor(labeled[1].trim())
      if (value) currentSection.lines.push(value.replace(/^[-•]\s*/, '').trim())
      continue
    }

    if (inSelfIntroduction) {
      const selfIntroductionContent = line.replace(/^[-•]\s*/, '').trim()
      if (currentSelfIntroduction) {
        currentSelfIntroduction.content = [currentSelfIntroduction.content, selfIntroductionContent].filter(Boolean).join('\n')
      } else {
        summary = [summary, selfIntroductionContent].filter(Boolean).join('\n')
      }
      continue
    }

    if (!currentSection) currentSection = sectionFor('자기소개')
    currentSection.lines.push(line.replace(/^[-•]\s*/, '').trim())
  }

  return { headline, summary, selfIntroductions, sections }
}

const emptyResumeVersionEditor: ResumeVersionEditor = {
  headline: '',
  selfIntroduction: '',
  careerDescription: '',
  skills: '',
}

function resumeVersionEditorFromContent(content: string): ResumeVersionEditor {
  const parsed = parseResumeExport(content)
  const unclassified = parsed.sections
    .filter((section) => !['경력기술서', '경력사항', '프로젝트', '기술 및 역량'].includes(section.title))
    .flatMap((section) => section.lines)
    .filter(Boolean)
  const selfIntroduction = [
    parsed.summary,
    ...parsed.selfIntroductions.flatMap((section) => [section.title, section.content]),
    ...unclassified,
  ].filter(Boolean).join('\n\n')
  const careerDescription = parsed.sections
    .filter((section) => ['경력기술서', '경력사항', '프로젝트'].includes(section.title))
    .flatMap((section) => section.lines)
    .filter(Boolean)
    .join('\n')
  const skills = parsed.sections
    .filter((section) => section.title === '기술 및 역량')
    .flatMap((section) => section.lines)
    .filter(Boolean)
    .join('\n')
  return { headline: parsed.headline, selfIntroduction, careerDescription, skills }
}

function resumeContentFromEditor(editor: ResumeVersionEditor) {
  const sections = [
    editor.headline.trim() && `희망직무: ${editor.headline.trim()}`,
    editor.selfIntroduction.trim() && `자기소개서:\n${editor.selfIntroduction.trim()}`,
    editor.careerDescription.trim() && `경력기술서:\n${editor.careerDescription.trim()}`,
    editor.skills.trim() && `스킬:\n${editor.skills.trim()}`,
  ]
  return sections.filter(Boolean).join('\n\n')
}

function resumeContentWithSkills(content: string, skills: string[]) {
  const skillSection = skills.length > 0 ? `스킬:\n${skills.map((skill) => `- ${skill}`).join('\n')}` : ''
  const skillHeader = content.match(/^스킬\s*:\s*.*$/im)
  const contentWithoutSkills = skillHeader?.index === undefined
    ? content.trim()
    : content.slice(0, skillHeader.index).trimEnd()
  return [contentWithoutSkills, skillSection].filter(Boolean).join('\n\n')
}

function safeResumeFileName(name: string, versionNumber: number) {
  const safeName = name.trim().replace(/[\\/:*?"<>|]+/g, '-').replace(/\s+/g, ' ').slice(0, 80) || 'jobkorea-resume'
  return `${safeName}-v${versionNumber}.pdf`
}

function jobKoreaResumeTitle(companyName?: string, jobTitle?: string, fallback = '') {
  const company = companyName?.trim() ?? ''
  const title = jobTitle?.trim() ?? ''
  if (company && title) return `${company} - ${title} 이력서`.slice(0, 200)
  const fallbackName = fallback.trim()
  if (title && /^(나의 이력서|내 이력서|my resume|resume)$/i.test(fallbackName)) return `${title} 이력서`.slice(0, 200)
  return fallbackName || 'JobKorea 이력서'
}

function splitJobKoreaList(value: string) {
  return value.split(/[,，\n]/).map((item) => item.trim()).filter(Boolean)
}

function truncateResumePreview(value: string, maxLength: number) {
  const normalized = value.replace(/\s+/g, ' ').trim()
  return normalized.length > maxLength ? `${normalized.slice(0, maxLength).trimEnd()}…` : normalized
}

function ResumePreviewSummary({ editor, compact = false }: { editor: ResumeVersionEditor; compact?: boolean }) {
  const skills = splitJobKoreaList(editor.skills)
  const description = [editor.selfIntroduction, editor.careerDescription].find((value) => value.trim()) ?? ''
  const visibleSkills = skills.slice(0, compact ? 3 : 5)

  return <span className={compact ? 'resume-card-preview compact' : 'resume-card-preview'}>
    <span className="resume-card-headline">{editor.headline || '희망직무 미입력'}</span>
    {description && <span className="resume-card-summary">{truncateResumePreview(description, compact ? 92 : 150)}</span>}
    {visibleSkills.length > 0 && <span className="resume-card-skills">{visibleSkills.map((skill) => <span key={skill}>{skill}</span>)}{skills.length > visibleSkills.length && <span>+{skills.length - visibleSkills.length}</span>}</span>}
    {!description && skills.length === 0 && <span className="resume-card-empty">등록된 내용을 미리볼 수 없습니다.</span>}
  </span>
}

const careerSkillPatterns: Array<{ skill: string; pattern: RegExp }> = [
  { skill: 'Spring Boot', pattern: /\bspring\s*boot\b/i },
  { skill: 'Spring Security', pattern: /\bspring\s*security\b/i },
  { skill: 'Spring Batch', pattern: /\bspring\s*batch\b/i },
  { skill: 'Spring', pattern: /\bspring\b(?!\s*(?:boot|security|batch))/i },
  { skill: 'Java', pattern: /\bjava\b/i },
  { skill: 'Kotlin', pattern: /\bkotlin\b/i },
  { skill: 'JPA', pattern: /\bjpa\b/i },
  { skill: 'Hibernate', pattern: /\bhibernate\b/i },
  { skill: 'QueryDSL', pattern: /\bquery\s*dsl\b/i },
  { skill: 'Maven', pattern: /\bmaven\b/i },
  { skill: 'Gradle', pattern: /\bgradle\b/i },
  { skill: 'PostgreSQL', pattern: /\bpostgres(?:ql)?\b/i },
  { skill: 'MySQL', pattern: /\bmysql\b/i },
  { skill: 'Oracle', pattern: /\boracle\b/i },
  { skill: 'MongoDB', pattern: /\bmongo\s*db\b/i },
  { skill: 'Redis', pattern: /\bredis\b/i },
  { skill: 'pgvector', pattern: /\bpgvector\b/i },
  { skill: 'Docker Compose', pattern: /\bdocker\s*compose\b/i },
  { skill: 'Docker', pattern: /\bdocker\b(?!\s*compose)/i },
  { skill: 'Kubernetes', pattern: /\bkubernetes\b|\bk8s\b/i },
  { skill: 'AWS', pattern: /\baws\b|amazon web services/i },
  { skill: 'GCP', pattern: /\bgcp\b|google cloud/i },
  { skill: 'Azure', pattern: /\bazure\b/i },
  { skill: 'GitHub Actions', pattern: /\bgithub\s*actions\b/i },
  { skill: 'Jenkins', pattern: /\bjenkins\b/i },
  { skill: 'Git', pattern: /\bgit\b(?!hub)/i },
  { skill: 'Nginx', pattern: /\bnginx\b/i },
  { skill: 'Kafka', pattern: /\bkafka\b/i },
  { skill: 'RabbitMQ', pattern: /\brabbit\s*mq\b/i },
  { skill: 'REST API', pattern: /\brest(?:ful)?\s*api\b/i },
  { skill: 'GraphQL', pattern: /\bgraphql\b/i },
  { skill: 'gRPC', pattern: /\bgrpc\b/i },
  { skill: 'React', pattern: /\breact(?:\.js|js)?\b/i },
  { skill: 'TypeScript', pattern: /\btypescript\b/i },
  { skill: 'JavaScript', pattern: /\bjavascript\b/i },
  { skill: 'Node.js', pattern: /\bnode(?:\.js|js)\b/i },
  { skill: 'Python', pattern: /\bpython\b/i },
  { skill: 'FastAPI', pattern: /\bfastapi\b/i },
  { skill: 'Django', pattern: /\bdjango\b/i },
  { skill: 'Elasticsearch', pattern: /\belasticsearch\b/i },
  { skill: 'Prometheus', pattern: /\bprometheus\b/i },
  { skill: 'Grafana', pattern: /\bgrafana\b/i },
  { skill: 'LLM', pattern: /\bllm\b/i },
  { skill: 'RAG', pattern: /\brag\b/i },
  { skill: 'OpenAI API', pattern: /\bopenai(?:\s*api)?\b/i },
  { skill: 'Gemini API', pattern: /\bgemini(?:\s*api)?\b/i },
  { skill: 'Ollama', pattern: /\bollama\b/i },
]

function splitExplicitSkills(value: string) {
  return value
    .split(/[,，\n;|·]+|\s+\/\s+/)
    .map((item) => item.trim().replace(/^[-•]+\s*/, ''))
    .filter((item) => item.length > 0 && item.length <= 50)
}

function skillKey(value: string) {
  return value.toLocaleLowerCase('en-US').replace(/[^a-z0-9가-힣+#]+/g, '')
}

function mergeUniqueSkills(...collections: string[][]) {
  const merged: string[] = []
  const seen = new Set<string>()
  for (const skill of collections.flat()) {
    const normalized = skill.trim()
    const key = skillKey(normalized)
    if (!key || seen.has(key)) continue
    seen.add(key)
    merged.push(normalized)
  }
  return merged
}

function mergeJobKoreaSkills(...collections: string[][]) {
  return mergeUniqueSkills(...collections).slice(0, maxJobKoreaSkills)
}

function collectCareerSkills(resumeSkills: string[], projects: Project[], experiences: Experience[]) {
  const explicitProjectSkills = projects.flatMap((project) => splitExplicitSkills(project.techStack))
  const explicitExperienceSkills = experiences.flatMap((experience) => splitExplicitSkills(experience.techStack ?? ''))
  const evidenceText = [
    ...projects.flatMap((project) => [project.description, project.role]),
    ...experiences.flatMap((experience) => [experience.techStack ?? '', experience.description, experience.role]),
  ].join('\n')
  const mentionedSkills = careerSkillPatterns
    .filter(({ pattern }) => pattern.test(evidenceText))
    .map(({ skill }) => skill)
  return mergeUniqueSkills(resumeSkills, explicitProjectSkills, explicitExperienceSkills, mentionedSkills)
}

function formatJobKoreaMonth(value: string) {
  return value ? value.replace('-', '.') : '입력 필요'
}

function formatJobKoreaDate(value: string) {
  return value ? value.replaceAll('-', '.') : '입력 필요'
}

function formatCareerPeriod(startDate: string | null | undefined, endDate: string | null | undefined, ongoingLabel: string | null = null) {
  if (!startDate && !endDate) return ongoingLabel
  if (!startDate) return `~ ${formatJobKoreaDate(endDate ?? '')}`
  if (!endDate) return ongoingLabel ? `${formatJobKoreaDate(startDate)} ~ ${ongoingLabel}` : formatJobKoreaDate(startDate)
  return `${formatJobKoreaDate(startDate)} ~ ${formatJobKoreaDate(endDate)}`
}

function buildCareerDescription(projects: Project[], experiences: Experience[]) {
  const projectSections = projects.map((project) => {
    const period = formatCareerPeriod(project.startDate, project.endDate, project.startDate && !project.endDate ? '진행 중' : null)
    return [
      `[프로젝트] ${project.name}`,
      `① 프로젝트 명: ${project.name}`,
      `② 고객사/Domain: ${project.customerOrDomain?.trim() || '입력 필요 (Project에 고객사 / Domain을 입력해주세요.)'}`,
      period ? `③ 수행 기간: ${period}` : null,
      `④ 업무 내용 및 성과: ${project.description.trim() || '입력 필요'}`,
      `⑤ 담당 역할: ${project.role}`,
      `⑥ 보유/활용 Skill: ${project.techStack}`,
    ].filter((line): line is string => line !== null).join('\n')
  })
  const experienceSections = experiences.map((experience) => {
    const period = formatCareerPeriod(experience.startDate, experience.endDate, experience.currentlyEmployed ? '재직 중' : null)
    return [
      `[경력] ${experience.companyName}`,
      `① 프로젝트 명: 입력 필요 (Experience에 프로젝트명이 없습니다.)`,
      `② 고객사/Domain: ${experience.companyName}`,
      period ? `③ 수행 기간: ${period}` : null,
      experience.department?.trim() ? `④ 부서: ${experience.department.trim()}` : null,
      experience.title?.trim() ? `⑤ 직급/직책: ${experience.title.trim()}` : null,
      `⑥ 업무 내용 및 성과: ${experience.description.trim() || '입력 필요'}`,
      `⑦ 담당 역할: ${experience.role}`,
      `⑧ 보유/활용 Skill: ${experience.techStack?.trim() || '입력 필요 (Career > 경력에서 기술 스택을 추가해주세요.)'}`,
    ].filter((line): line is string => line !== null).join('\n')
  })
  return [...projectSections, ...experienceSections].join('\n\n')
}

function emptyJobKoreaEducation(): JobKoreaEducation {
  return { schoolType: '', schoolName: '', admissionMonth: '', graduationMonth: '', status: '', major: '', gpa: '', gpaScale: '', additionalMajor: '' }
}

function emptyJobKoreaCareer(): JobKoreaCareer {
  return { companyName: '', hideCompanyName: false, department: '', startMonth: '', endMonth: '', isCurrent: false, title: '', job: '', salary: '', duties: '' }
}

function toJobKoreaMonth(value: string | null) {
  return value?.slice(0, 7) ?? ''
}

function jobKoreaCareerFromExperience(experience: Experience): JobKoreaCareer {
  return {
    ...emptyJobKoreaCareer(),
    companyName: experience.companyName,
    department: experience.department ?? '',
    startMonth: toJobKoreaMonth(experience.startDate),
    endMonth: experience.currentlyEmployed ? '' : toJobKoreaMonth(experience.endDate),
    isCurrent: experience.currentlyEmployed,
    title: experience.title ?? '',
    job: experience.role,
    duties: experience.description,
  }
}

function emptyJobKoreaAdditionalRecord(): JobKoreaAdditionalRecord {
  return { name: '', organization: '', date: '', description: '' }
}

function emptyJobKoreaOverseasExperience(): JobKoreaOverseasExperience {
  return { country: '', organization: '', startMonth: '', endMonth: '', description: '' }
}

function emptyJobKoreaLanguage(): JobKoreaLanguage {
  return { language: '', level: '', score: '' }
}

function emptyJobKoreaPortfolio(): JobKoreaPortfolio {
  return { title: '', url: '', description: '' }
}

function defaultJobKoreaSelfIntroductions(parsed: ParsedResumeExport): JobKoreaSelfIntroduction[] {
  if (parsed.selfIntroductions.length > 0) return parsed.selfIntroductions
  const defaults = Array.from({ length: 3 }, (): JobKoreaSelfIntroduction => ({ title: '', content: '' }))
  if (parsed.summary) defaults[0] = { title: '직무 역량과 경력', content: parsed.summary }
  return defaults
}

function JobKoreaPreviewSection({ title, children }: { title: string; children: ReactNode }) {
  return <section style={{ marginTop: '10px', border: '1px solid #aeb6ae', fontSize: '11px' }}><h3 style={{ margin: 0, padding: '8px 10px', background: '#dfe5df', color: '#202820', fontSize: '12px' }}>{title}</h3><div style={{ padding: '9px 12px', lineHeight: 1.6 }}>{children}</div></section>
}

type CareerReadinessIssue = { key: string; source: string; field: string; guidance: string; path: string }

function findCareerReadinessIssues(experiences: Experience[], projects: Project[]) {
  const issues: CareerReadinessIssue[] = []
  experiences.forEach((experience) => {
    const source = `${experience.companyName} · ${experience.role}`
    const path = `/career/experiences?edit=${experience.id}`
    if (!experience.techStack?.trim()) issues.push({ key: `experience-tech-${experience.id}`, source, field: '기술 스택', guidance: '사용 기술을 쉼표로 구분해 입력하면 스킬과 경력기술서 근거로 사용됩니다.', path })
    if (!experience.startDate) issues.push({ key: `experience-start-${experience.id}`, source, field: '시작일', guidance: 'JobKorea 경력 기간에 사용할 시작일을 입력해주세요.', path })
    if (!experience.currentlyEmployed && !experience.endDate) issues.push({ key: `experience-end-${experience.id}`, source, field: '종료일', guidance: '재직 중이 아니라면 종료일을 입력해주세요.', path })
  })
  projects.forEach((project) => {
    const source = `${project.name} · ${project.role}`
    const path = `/career/projects?edit=${project.id}`
    if (!project.customerOrDomain?.trim()) issues.push({ key: `project-domain-${project.id}`, source, field: '고객사 / Domain', guidance: '프로젝트가 적용된 고객사나 서비스 도메인을 입력해주세요.', path })
    if (!project.startDate) issues.push({ key: `project-start-${project.id}`, source, field: '시작일', guidance: '경력기술서에 사용할 프로젝트 시작일을 입력해주세요.', path })
  })
  return issues
}

function CareerDataReviewModal({ title, continueLabel, reindexBeforeContinue = false, onClose, onContinue }: { title: string; continueLabel: string; reindexBeforeContinue?: boolean; onClose: () => void; onContinue: () => void }) {
  const [experiences, setExperiences] = useState<Experience[]>([])
  const [projects, setProjects] = useState<Project[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [continueError, setContinueError] = useState('')
  const [isPreparing, setIsPreparing] = useState(false)
  const [acknowledged, setAcknowledged] = useState(false)

  async function loadReadiness() {
    setIsLoading(true)
    setError('')
    try {
      const [loadedExperiences, loadedProjects] = await Promise.all([
        requestAllPages<Experience>('/v1/experiences'),
        requestAllPages<Project>('/v1/projects'),
      ])
      setExperiences(loadedExperiences)
      setProjects(loadedProjects)
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : 'Career 데이터를 점검하지 못했습니다.')
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => { void loadReadiness() }, [])

  async function continueFromReview() {
    setContinueError('')
    if (!reindexBeforeContinue) {
      onContinue()
      return
    }
    setIsPreparing(true)
    try {
      await request<EmbeddingIndexResponse>('/v1/retrieval/index', { method: 'POST' })
      onContinue()
    } catch (indexError) {
      setContinueError(indexError instanceof Error ? indexError.message : '최신 Career 검색 데이터를 준비하지 못했습니다.')
      setIsPreparing(false)
    }
  }

  const issues = findCareerReadinessIssues(experiences, projects)
  const hasCareerData = experiences.length + projects.length > 0
  const canContinue = !isLoading && !error && hasCareerData && (issues.length === 0 || acknowledged)

  return <div className="modal-backdrop" role="presentation">
    <section className="career-review-modal" role="dialog" aria-modal="true" aria-labelledby="career-review-title">
      <div className="modal-heading"><div><span className="eyebrow">STEP 1 · SOURCE REVIEW</span><h2 id="career-review-title">{title}</h2></div><button className="modal-close" onClick={onClose} aria-label="닫기">×</button></div>
      <p className="modal-description">이력서를 만들기 전에 원본 경력과 프로젝트를 먼저 확인합니다. 여기서 보완한 내용이 스킬, 경력기술서, AI 근거에 반영됩니다.{reindexBeforeContinue && ' 계속 진행하면 최신 Career 데이터로 맞춤 검색 데이터를 자동 업데이트합니다.'}</p>
      {isLoading && <div className="list-state">Career 데이터를 점검하는 중입니다...</div>}
      {error && <div className="list-state error-state" role="alert">{error}<button className="text-button" onClick={() => void loadReadiness()}>다시 시도</button></div>}
      {!isLoading && !error && !hasCareerData && <div className="career-review-empty"><strong>먼저 경력 또는 프로젝트를 등록해주세요.</strong><p>근거 데이터가 없으면 이력서 초안을 생성할 수 없습니다.</p><div><Link className="secondary-button compact-button" to="/career/experiences" onClick={onClose}>경력 등록하기</Link><Link className="secondary-button compact-button" to="/career/projects" onClick={onClose}>프로젝트 등록하기</Link></div></div>}
      {!isLoading && !error && hasCareerData && <>
        <div className={`career-review-summary ${issues.length === 0 ? 'ready' : ''}`}><strong>{issues.length === 0 ? '원본 데이터 점검 완료' : `${issues.length}개 항목을 보완하면 더 정확해집니다.`}</strong><span>경력 {experiences.length}개 · 프로젝트 {projects.length}개</span></div>
        {issues.length > 0 && <div className="career-review-list">{issues.map((issue) => <article key={issue.key}><div><span>{issue.field}</span><strong>{issue.source}</strong><p>{issue.guidance}</p></div><Link className="text-button" to={issue.path} onClick={onClose}>이전 단계에서 수정 ↗</Link></article>)}</div>}
        {issues.length > 0 && <label className="export-review-check"><input type="checkbox" checked={acknowledged} onChange={(event) => setAcknowledged(event.target.checked)} /><span><strong>누락 항목을 확인했습니다.</strong><br />지금 수정하지 않으면 해당 내용은 “입력 필요”로 남을 수 있음을 이해하고 계속합니다.</span></label>}
      </>}
      {continueError && <p className="form-error" role="alert">{continueError}</p>}
      <div className="modal-actions"><button className="text-button" onClick={onClose}>취소</button><button className="primary-button compact-button" onClick={() => void continueFromReview()} disabled={!canContinue || isPreparing}>{isPreparing ? '최신 Career 데이터 준비 중...' : continueLabel}</button></div>
    </section>
  </div>
}

function JobKoreaExportModal({
  resumeName,
  companyName,
  version,
  jobTitle,
  displayName,
  email,
  onPersistSkills,
  onClose,
}: {
  resumeName: string
  companyName?: string
  version: ResumeVersion
  jobTitle?: string
  displayName: string
  email: string
  onPersistSkills?: (skills: string[]) => Promise<ResumeVersion>
  onClose: () => void
}) {
  const previewRef = useRef<HTMLDivElement>(null)
  const parsed = parseResumeExport(version.content)
  const exportTitle = jobKoreaResumeTitle(companyName, jobTitle, resumeName)
  const generatedCareerDescription = parsed.sections
    .filter((section) => ['경력기술서', '경력사항', '프로젝트'].includes(section.title))
    .flatMap((section) => section.lines)
    .map((line) => `• ${line}`)
    .join('\n')
  const [profile, setProfile] = useState<JobKoreaExportProfile>(() => ({
    name: displayName,
    phone: '',
    mobile: '',
    email,
    address: '',
    birthDate: '',
    gender: '',
    photoDataUrl: '',
    desiredJobKeywords: parsed.headline || jobTitle || exportTitle.replace(/\s*이력서\s*$/, ''),
    skills: parsed.sections.find((section) => section.title === '기술 및 역량')?.lines.join(', ') ?? '',
    educations: [emptyJobKoreaEducation()],
    careers: [],
    careerDescription: generatedCareerDescription,
    activities: [],
    trainings: [],
    certificates: [],
    awards: [],
    overseas: [],
    languages: [],
    portfolios: [],
    veteran: false,
    employmentProtection: false,
    employmentSupport: false,
    disability: false,
    militaryStatus: '',
    enlistmentDate: '',
    dischargeDate: '',
    militaryDetails: '',
    selfIntroductions: defaultJobKoreaSelfIntroductions(parsed),
    employmentType: '',
    desiredSalary: '',
    desiredLocations: '',
    industryKeywords: '',
  }))
  const [status, setStatus] = useState('')
  const [isGenerating, setIsGenerating] = useState(false)
  const [isLoadingCareers, setIsLoadingCareers] = useState(false)
  const [isLoadingSkills, setIsLoadingSkills] = useState(false)
  const [isGeneratingCareerDescription, setIsGeneratingCareerDescription] = useState(false)
  const [reviewConfirmed, setReviewConfirmed] = useState(false)

  useEffect(() => { void loadCareerSkills(false) }, [])

  type TextField = Exclude<keyof JobKoreaExportProfile, 'educations' | 'careers' | 'overseas' | 'activities' | 'trainings' | 'certificates' | 'awards' | 'languages' | 'portfolios' | 'selfIntroductions' | 'veteran' | 'employmentProtection' | 'employmentSupport' | 'disability'>

  function updateProfile(field: TextField, value: string) {
    setProfile((current) => ({ ...current, [field]: value }))
  }

  function updateEducation(index: number, field: keyof JobKoreaEducation, value: string) {
    setProfile((current) => ({ ...current, educations: current.educations.map((education, educationIndex) => educationIndex === index ? { ...education, [field]: value } : education) }))
  }

  function updateCareer(index: number, field: Exclude<keyof JobKoreaCareer, 'isCurrent'>, value: string) {
    setProfile((current) => ({ ...current, careers: current.careers.map((career, careerIndex) => careerIndex === index ? { ...career, [field]: value } : career) }))
  }

  type AdditionalCollection = 'activities' | 'trainings' | 'certificates' | 'awards'

  function updateAdditional(collection: AdditionalCollection, index: number, field: keyof JobKoreaAdditionalRecord, value: string) {
    setProfile((current) => ({ ...current, [collection]: current[collection].map((item, itemIndex) => itemIndex === index ? { ...item, [field]: value } : item) }))
  }

  function updateOverseas(index: number, field: keyof JobKoreaOverseasExperience, value: string) {
    setProfile((current) => ({ ...current, overseas: current.overseas.map((item, itemIndex) => itemIndex === index ? { ...item, [field]: value } : item) }))
  }

  function updateLanguage(index: number, field: keyof JobKoreaLanguage, value: string) {
    setProfile((current) => ({ ...current, languages: current.languages.map((language, languageIndex) => languageIndex === index ? { ...language, [field]: value } : language) }))
  }

  function updatePortfolio(index: number, field: keyof JobKoreaPortfolio, value: string) {
    setProfile((current) => ({ ...current, portfolios: current.portfolios.map((portfolio, portfolioIndex) => portfolioIndex === index ? { ...portfolio, [field]: value } : portfolio) }))
  }

  function updateSelfIntroduction(index: number, field: keyof JobKoreaSelfIntroduction, value: string) {
    setProfile((current) => ({ ...current, selfIntroductions: current.selfIntroductions.map((item, itemIndex) => itemIndex === index ? { ...item, [field]: value } : item) }))
  }

  function withDemoCareerDates(careers: JobKoreaCareer[]) {
    return careers.map((career, index) => {
      const demoYear = 2018 + index
      return {
        ...career,
        startMonth: career.startMonth || `${demoYear}-01`,
        endMonth: career.isCurrent ? '' : career.endMonth || `${demoYear}-12`,
      }
    })
  }

  function fillCareerDemoDates() {
    if (profile.careers.length === 0) {
      setStatus('먼저 Career 경력을 불러오거나 경력을 추가해주세요.')
      return
    }
    setProfile((current) => ({ ...current, careers: withDemoCareerDates(current.careers) }))
    setReviewConfirmed(false)
    setStatus('비어 있는 입·퇴사월에 테스트용 기간을 임시 입력했습니다. 실제 제출 전 본인 경력 기간으로 교체해주세요.')
  }

  function fillRequiredDemoData() {
    const demoEducation: JobKoreaEducation = {
      ...emptyJobKoreaEducation(),
      schoolName: '테스트대학교',
      admissionMonth: '2009-03',
      graduationMonth: '2013-02',
      major: '컴퓨터공학',
    }
    setProfile((current) => ({
      ...current,
      birthDate: current.birthDate || '1990-01-01',
      gender: current.gender || '남자',
      mobile: current.mobile || '010-0000-0000',
      desiredLocations: current.desiredLocations || '서울',
      careers: withDemoCareerDates(current.careers),
      educations: current.educations.length === 0
        ? [demoEducation]
        : current.educations.map((education, index) => index === 0 ? {
          ...education,
          schoolName: education.schoolName || demoEducation.schoolName,
          admissionMonth: education.admissionMonth || demoEducation.admissionMonth,
          graduationMonth: education.graduationMonth || demoEducation.graduationMonth,
          major: education.major || demoEducation.major,
        } : education),
    }))
    setReviewConfirmed(false)
    setStatus('테스트용 필수값을 임시 입력했습니다. 실제 JobKorea 제출 전 반드시 본인 정보로 교체해주세요.')
  }

  async function recordExportAudit(result: 'DOWNLOAD_TRIGGERED' | 'GENERATION_FAILED', fileSizeBytes?: number, resumeVersionId = version.id) {
    await request('/v1/jobkorea/export-audits', {
      method: 'POST',
      body: JSON.stringify({
        resumeVersionId,
        templateVersion: jobKoreaTemplateVersion,
        result,
        fileSizeBytes: fileSizeBytes ?? null,
      }),
    })
  }

  function failGeneration(message: string) {
    setStatus(message)
    setIsGenerating(false)
    void recordExportAudit('GENERATION_FAILED').catch(() => undefined)
  }

  async function loadAllCareers() {
    setIsLoadingCareers(true)
    setStatus('Career의 경력을 불러오는 중입니다...')
    try {
      const allExperiences = await requestAllPages<Experience>('/v1/experiences')
      const careers = allExperiences.map(jobKoreaCareerFromExperience)
      const missingPeriodCount = careers.filter((career) => !career.startMonth || (!career.isCurrent && !career.endMonth)).length
      setProfile((current) => ({ ...current, careers }))
      setStatus(careers.length > 0
        ? `${careers.length}개의 Career 경력과 입·퇴사년월을 불러왔습니다.${missingPeriodCount > 0 ? ` 기간이 비어 있는 경력 ${missingPeriodCount}개를 확인해주세요.` : ''} 부서·직급·연봉을 확인해주세요.`
        : '불러올 Career 경력이 없습니다.')
    } catch (error) {
      setStatus(error instanceof Error ? error.message : 'Career 경력을 불러오지 못했습니다.')
    } finally {
      setIsLoadingCareers(false)
    }
  }

  async function loadCareerSkills(showStatus = true) {
    setIsLoadingSkills(true)
    if (showStatus) setStatus('이력서와 Career 데이터에서 스킬을 찾는 중입니다...')
    try {
      const [experiences, projects] = await Promise.all([
        requestAllPages<Experience>('/v1/experiences'),
        requestAllPages<Project>('/v1/projects'),
      ])
      const resumeSkills = parsed.sections
        .find((section) => section.title === '기술 및 역량')
        ?.lines.flatMap(splitExplicitSkills) ?? []
      const careerSkills = collectCareerSkills(resumeSkills, projects, experiences)
      setProfile((current) => ({
        ...current,
        skills: mergeJobKoreaSkills(splitJobKoreaList(current.skills), careerSkills).join(', '),
      }))
      setReviewConfirmed(false)
      if (showStatus) {
        setStatus(careerSkills.length > 0
          ? `승인 이력서와 경력·프로젝트 근거에서 스킬 ${careerSkills.length}개를 반영했습니다.`
          : '승인 이력서와 경력·프로젝트에서 확인할 수 있는 스킬이 없습니다.')
      }
    } catch (error) {
      if (showStatus) setStatus(error instanceof Error ? error.message : '경력·프로젝트 스킬을 불러오지 못했습니다.')
    } finally {
      setIsLoadingSkills(false)
    }
  }

  async function generateCareerDescription() {
    if (profile.careerDescription.trim() && !window.confirm('현재 경력기술서 내용을 Career 데이터로 교체할까요?')) return
    setIsGeneratingCareerDescription(true)
    setStatus('프로젝트와 경력 데이터를 모아 경력기술서를 작성하는 중입니다...')
    try {
      const [experiences, projects] = await Promise.all([
        requestAllPages<Experience>('/v1/experiences'),
        requestAllPages<Project>('/v1/projects'),
      ])
      if (experiences.length === 0 && projects.length === 0) {
        throw new Error('먼저 Career 메뉴에 경력 또는 프로젝트를 등록해주세요.')
      }
      const description = buildCareerDescription(projects, experiences)
      const careerSkills = collectCareerSkills([], projects, experiences)
      setProfile((current) => ({
        ...current,
        careerDescription: description,
        skills: mergeJobKoreaSkills(splitJobKoreaList(current.skills), careerSkills).join(', '),
        careers: current.careers.length > 0 ? current.careers : experiences.map(jobKoreaCareerFromExperience),
      }))
      setReviewConfirmed(false)
      setStatus(`프로젝트 ${projects.length}개와 경력 ${experiences.length}개를 경력기술서와 스킬에 반영했습니다. '입력 필요' 항목과 성과를 검토해주세요.`)
    } catch (error) {
      setStatus(error instanceof Error ? error.message : '경력기술서를 자동 작성하지 못했습니다.')
    } finally {
      setIsGeneratingCareerDescription(false)
    }
  }

  function handlePhotoChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    if (!file) return
    if (!supportedResumePhotoTypes.has(file.type)) {
      setStatus('사진은 JPG, PNG, WEBP 파일만 선택할 수 있습니다.')
      return
    }
    if (file.size > 5 * 1024 * 1024) {
      setStatus('사진 파일은 5MB 이하만 선택할 수 있습니다.')
      return
    }
    const reader = new FileReader()
    reader.onload = () => updateProfile('photoDataUrl', typeof reader.result === 'string' ? reader.result : '')
    reader.onerror = () => setStatus('사진을 읽지 못했습니다.')
    reader.readAsDataURL(file)
  }

  async function downloadPdf() {
    if (!previewRef.current) return
    const errors: string[] = []
    const requiredFields: Array<[string, string]> = [
      ['이름', profile.name],
      ['생년월일', profile.birthDate],
      ['성별', profile.gender],
      ['이메일', profile.email],
      ['휴대폰번호', profile.mobile],
    ]
    const missingFields = requiredFields.filter(([, value]) => !value.trim()).map(([label]) => label)
    if (missingFields.length > 0) errors.push(`필수값: ${missingFields.join(', ')}`)
    if (!profile.email.includes('@')) errors.push('이메일 형식을 확인해주세요.')
    if (splitJobKoreaList(profile.desiredJobKeywords).length === 0) errors.push('희망직무 키워드를 1개 이상 입력해주세요.')
    const desiredLocations = splitJobKoreaList(profile.desiredLocations)
    if (desiredLocations.length === 0) errors.push('희망근무지를 1개 이상 입력해주세요.')
    if (desiredLocations.length > 3) errors.push('희망근무지는 최대 3개까지 입력할 수 있습니다.')
    if (splitJobKoreaList(profile.skills).length > 20) errors.push('스킬은 최대 20개까지 입력할 수 있습니다.')
    if (profile.educations.length === 0) errors.push('학력을 1개 이상 추가해주세요.')
    profile.educations.forEach((education, index) => {
      const fields: Array<[string, string]> = [['학교명', education.schoolName], ['입학년월', education.admissionMonth], ['졸업년월', education.graduationMonth], ['전공명', education.major]]
      const missing = fields.filter(([, value]) => !value.trim()).map(([label]) => label)
      if (missing.length > 0) errors.push(`학력 ${index + 1}: ${missing.join(', ')} 입력 필요`)
    })
    profile.careers.forEach((career, index) => {
      const fields: Array<[string, string]> = [['회사명', career.companyName], ['입사년월', career.startMonth], ['담당직무', career.job]]
      const missing = fields.filter(([, value]) => !value.trim()).map(([label]) => label)
      if (!career.isCurrent && !career.endMonth.trim()) missing.push('퇴사년월')
      if (missing.length > 0) errors.push(`경력 ${index + 1}: ${missing.join(', ')} 입력 필요`)
    })
    profile.selfIntroductions.forEach((item, index) => {
      const missing = [['제목', item.title], ['내용', item.content]]
        .filter(([, value]) => !value.trim())
        .map(([label]) => label)
      if (missing.length > 0) errors.push(`자기소개서 ${index + 1}: ${missing.join(', ')} 입력 필요`)
      if (item.title.trim().length > 50) errors.push(`자기소개서 ${index + 1}: 제목은 최대 50자까지 입력할 수 있습니다.`)
    })
    if (!reviewConfirmed) errors.push('개인정보와 자동 생성 내용을 검토했다는 확인이 필요합니다.')
    if (errors.length > 0) {
      setStatus(errors.join(' · '))
      return
    }

    setIsGenerating(true)
    setStatus('PDF를 생성하는 중입니다...')
    try {
      const selectedSkills = mergeJobKoreaSkills(splitJobKoreaList(profile.skills))
      const savedVersion = onPersistSkills ? await onPersistSkills(selectedSkills) : version
      const element = previewRef.current
      const { default: html2canvas } = await import('html2canvas')
      const canvas = await html2canvas(element, {
        allowTaint: false,
        backgroundColor: '#ffffff',
        height: Math.ceil(element.scrollHeight),
        imageTimeout: 15000,
        logging: false,
        scale: 2,
        useCORS: true,
        width: Math.ceil(element.scrollWidth),
        windowHeight: Math.ceil(element.scrollHeight),
        windowWidth: Math.ceil(element.scrollWidth),
      })
      const pageWidth = 595.28
      const pageHeight = 841.89
      const pagePixelHeight = Math.floor(canvas.width * pageHeight / pageWidth)
      const pdf = new jsPDF({ orientation: 'portrait', unit: 'pt', format: 'a4', compress: true })

      for (let offset = 0, pageIndex = 0; offset < canvas.height; offset += pagePixelHeight, pageIndex += 1) {
        const sliceHeight = Math.min(pagePixelHeight, canvas.height - offset)
        const pageCanvas = document.createElement('canvas')
        pageCanvas.width = canvas.width
        pageCanvas.height = sliceHeight
        const pageContext = pageCanvas.getContext('2d')
        if (!pageContext) {
          failGeneration('PDF 렌더러를 초기화하지 못했습니다.')
          return
        }
        pageContext.fillStyle = '#ffffff'
        pageContext.fillRect(0, 0, pageCanvas.width, pageCanvas.height)
        pageContext.drawImage(
          canvas,
          0,
          offset,
          canvas.width,
          sliceHeight,
          0,
          0,
          pageCanvas.width,
          pageCanvas.height,
        )

        if (pageIndex > 0) pdf.addPage()
        pdf.addImage(
          pageCanvas.toDataURL('image/jpeg', 0.92),
          'JPEG',
          0,
          0,
          pageWidth,
          sliceHeight / canvas.width * pageWidth,
        )
      }

      const blob = pdf.output('blob')
      if (blob.size > 5 * 1024 * 1024) {
        failGeneration('생성된 PDF가 5MB를 초과합니다. 내용을 줄인 후 다시 시도해주세요.')
        return
      }
      const downloadUrl = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = downloadUrl
      link.download = safeResumeFileName(exportTitle, savedVersion.versionNumber)
      link.click()
      URL.revokeObjectURL(downloadUrl)
      await recordExportAudit('DOWNLOAD_TRIGGERED', blob.size, savedVersion.id).catch(() => undefined)
      setStatus(onPersistSkills && savedVersion.id !== version.id
        ? `선택한 스킬을 이력서 Version ${savedVersion.versionNumber}에 저장하고 PDF를 다운로드했습니다.`
        : 'PDF가 다운로드되었습니다. 내용을 확인한 뒤 JobKorea에 직접 업로드해주세요.')
      setIsGenerating(false)
    } catch (error) {
      failGeneration(error instanceof Error ? error.message : 'PDF 파일을 생성하지 못했습니다.')
    }
  }

  const desiredJobs = splitJobKoreaList(profile.desiredJobKeywords)
  const skills = splitJobKoreaList(profile.skills)
  const desiredLocations = splitJobKoreaList(profile.desiredLocations)
  const industryKeywords = splitJobKoreaList(profile.industryKeywords)
  const legacySections = parsed.sections.filter((section) => !['기술 및 역량', '경력기술서', '경력사항', '프로젝트'].includes(section.title))

  function renderAdditionalSection(title: string, collection: AdditionalCollection) {
    const records = profile[collection]
    return <div className="export-subsection"><div className="export-subsection-heading"><strong>{title}</strong><button className="text-button" type="button" onClick={() => setProfile((current) => ({ ...current, [collection]: [...current[collection], emptyJobKoreaAdditionalRecord()] }))}>추가</button></div>{records.length === 0 && <p className="export-empty">선택 입력 항목입니다.</p>}<div className="export-repeat-list">{records.map((record, index) => <div className="export-repeat-item" key={`${collection}-${index}`}><div className="export-repeat-heading"><strong>{title} {index + 1}</strong><button className="text-button danger-button" type="button" onClick={() => setProfile((current) => ({ ...current, [collection]: current[collection].filter((_, itemIndex) => itemIndex !== index) }))}>삭제</button></div><div className="export-form-grid"><label>항목명<input value={record.name} onChange={(event) => updateAdditional(collection, index, 'name', event.target.value)} /></label><label>기관·회사<input value={record.organization} onChange={(event) => updateAdditional(collection, index, 'organization', event.target.value)} /></label><label>년월<input type="month" value={record.date} onChange={(event) => updateAdditional(collection, index, 'date', event.target.value)} /></label><label className="export-profile-wide">내용<textarea value={record.description} onChange={(event) => updateAdditional(collection, index, 'description', event.target.value)} rows={3} /></label></div></div>)}</div></div>
  }

  function renderAdditionalPreview(title: string, records: JobKoreaAdditionalRecord[]) {
    if (records.length === 0) return null
    return <JobKoreaPreviewSection title={title}>{records.map((record, index) => <div key={`preview-${title}-${index}`} style={{ marginBottom: index === records.length - 1 ? 0 : '9px' }}><strong>{record.name || '항목명 미입력'}</strong>{record.organization && ` · ${record.organization}`}{record.date && ` · ${formatJobKoreaMonth(record.date)}`}{record.description && <div style={{ whiteSpace: 'pre-wrap' }}>{record.description}</div>}</div>)}</JobKoreaPreviewSection>
  }

  return <div className="modal-backdrop" role="presentation">
    <section className="resume-export-modal" role="dialog" aria-modal="true" aria-labelledby="jobkorea-export-title">
      <div className="modal-heading"><div><span className="eyebrow">JOBKOREA FILE EXPORT</span><h2 id="jobkorea-export-title">JobKorea 업로드 파일 만들기</h2></div><button className="modal-close" onClick={onClose} aria-label="닫기">×</button></div>
      <p className="modal-description">승인된 이력서 버전 {version.versionNumber}를 JobKorea 표준 항목 순서로 변환해 PDF를 생성합니다. 자동으로 만든 내용은 희망직무·자기소개서·경력기술서·스킬에 배치하고, 계정에 없는 개인정보·학력·근무조건은 직접 입력합니다.</p>
      <div className="export-contract-note"><strong>파일 계약</strong><span>PDF 1개 · 5MB 이하 · JobKorea 업로드 페이지에서 직접 제출</span></div>

      <div className="export-section"><div className="export-section-heading"><div><span className="eyebrow">PROFILE</span><h3>인적사항</h3></div><div className="heading-actions"><span className="export-required-note">* 필수</span><button className="secondary-button compact-button" type="button" onClick={fillRequiredDemoData}>필수 데모 데이터 채우기</button></div></div><div className="export-form-grid">
        <label>이름 *<input value={profile.name} onChange={(event) => updateProfile('name', event.target.value)} maxLength={100} /></label>
        <label>생년월일 *<input type="date" value={profile.birthDate} onChange={(event) => updateProfile('birthDate', event.target.value)} /></label>
        <label>성별 *<select value={profile.gender} onChange={(event) => updateProfile('gender', event.target.value)}><option value="">선택해주세요</option><option>남자</option><option>여자</option><option>선택하지 않음</option></select></label>
        <label>이메일 *<input type="email" value={profile.email} onChange={(event) => updateProfile('email', event.target.value)} /></label>
        <label>전화번호<input value={profile.phone} onChange={(event) => updateProfile('phone', event.target.value)} placeholder="02-0000-0000" /></label>
        <label>휴대폰번호 *<input value={profile.mobile} onChange={(event) => updateProfile('mobile', event.target.value)} placeholder="010-0000-0000" /></label>
        <label className="export-profile-wide">주소<input value={profile.address} onChange={(event) => updateProfile('address', event.target.value)} placeholder="시·군·구까지 입력" /></label>
        <label className="export-photo-control">사진<input type="file" accept="image/jpeg,image/png,image/webp" onChange={handlePhotoChange} /></label>
        {profile.photoDataUrl && <div className="export-photo-preview"><img src={profile.photoDataUrl} alt="이력서 사진 미리보기" /><button className="text-button danger-button" type="button" onClick={() => updateProfile('photoDataUrl', '')}>사진 제거</button></div>}
      </div></div>

      <div className="export-section"><div className="export-section-heading"><div><span className="eyebrow">TARGET</span><h3>희망직무 · 스킬</h3></div><button className="secondary-button compact-button" type="button" onClick={() => void loadCareerSkills()} disabled={isLoadingSkills}>{isLoadingSkills ? '스킬 찾는 중...' : '경력·프로젝트 스킬 불러오기'}</button></div><div className="export-form-grid">
        <label className="export-profile-wide">희망직무 키워드 *<input value={profile.desiredJobKeywords} onChange={(event) => updateProfile('desiredJobKeywords', event.target.value)} placeholder="백엔드개발자, 웹개발자" /></label>
        <p className="export-help export-profile-wide">승인된 이력서의 기술 및 역량, 프로젝트 기술 스택, 경력·프로젝트 설명에 명시된 기술을 합쳐 자동 입력합니다. 추측한 기술은 추가하지 않으며 스킬은 최대 20개입니다.</p>
        <label className="export-profile-wide">내가 선택한 스킬<textarea value={profile.skills} onChange={(event) => updateProfile('skills', event.target.value)} placeholder="Java, Spring Boot, PostgreSQL, Docker" rows={3} /></label>
        <label className="export-profile-wide">산업 키워드<input value={profile.industryKeywords} onChange={(event) => updateProfile('industryKeywords', event.target.value)} placeholder="핀테크, SaaS, 플랫폼" /></label>
      </div></div>

      <div className="export-section"><div className="export-section-heading"><div><span className="eyebrow">EDUCATION</span><h3>학력</h3></div><button className="secondary-button compact-button" type="button" onClick={() => setProfile((current) => ({ ...current, educations: [...current.educations, emptyJobKoreaEducation()] }))}>학력 추가</button></div>
        {profile.educations.length === 0 && <p className="export-empty">학력 정보를 추가하면 학교명·기간·전공을 구조화해서 표시합니다. 확인되지 않은 학력은 자동으로 만들지 않습니다.</p>}
        <div className="export-repeat-list">{profile.educations.map((education, index) => <div className="export-repeat-item" key={`education-${index}`}><div className="export-repeat-heading"><strong>학력 {index + 1}</strong><button className="text-button danger-button" type="button" onClick={() => setProfile((current) => ({ ...current, educations: current.educations.filter((_, itemIndex) => itemIndex !== index) }))}>삭제</button></div><div className="export-form-grid">
          <label>학교구분<select value={education.schoolType} onChange={(event) => updateEducation(index, 'schoolType', event.target.value)}><option value="">선택</option><option>고등학교</option><option>대학교(2년제)</option><option>대학교(4년제)</option><option>대학원</option></select></label>
          <label>학교명 *<input value={education.schoolName} onChange={(event) => updateEducation(index, 'schoolName', event.target.value)} /></label>
          <label>입학년월 *<input type="month" value={education.admissionMonth} onChange={(event) => updateEducation(index, 'admissionMonth', event.target.value)} /></label>
          <label>졸업년월 *<input type="month" value={education.graduationMonth} onChange={(event) => updateEducation(index, 'graduationMonth', event.target.value)} /></label>
          <label>졸업상태<select value={education.status} onChange={(event) => updateEducation(index, 'status', event.target.value)}><option value="">선택</option><option>졸업</option><option>재학</option><option>휴학</option><option>수료</option><option>중퇴</option><option>편입</option></select></label>
          <label>전공명 *<input value={education.major} onChange={(event) => updateEducation(index, 'major', event.target.value)} /></label>
          <label>학점<input value={education.gpa} onChange={(event) => updateEducation(index, 'gpa', event.target.value)} placeholder="예: 4.12" /></label>
          <label>총점<input value={education.gpaScale} onChange={(event) => updateEducation(index, 'gpaScale', event.target.value)} placeholder="예: 4.5" /></label>
          <label className="export-profile-wide">다른 전공·졸업 논문/작품<input value={education.additionalMajor} onChange={(event) => updateEducation(index, 'additionalMajor', event.target.value)} /></label>
        </div></div>)}</div>
      </div>

      <div className="export-section"><div className="export-section-heading"><div><span className="eyebrow">CAREER</span><h3>경력</h3></div><div className="heading-actions"><button className="secondary-button compact-button" type="button" onClick={() => void loadAllCareers()} disabled={isLoadingCareers}>{isLoadingCareers ? '불러오는 중...' : '모든 경력 한 번에 불러오기'}</button><button className="secondary-button compact-button" type="button" onClick={fillCareerDemoDates}>경력 데모 기간 채우기</button><button className="secondary-button compact-button" type="button" onClick={() => setProfile((current) => ({ ...current, careers: [...current.careers, emptyJobKoreaCareer()] }))}>경력 추가</button></div></div>
        <p className="export-help">Career 메뉴에 저장된 회사·직무·담당업무·입사일·퇴사일·재직 상태를 자동 입력합니다. 일 단위 날짜는 JobKorea 형식에 맞춰 년월로 변환하며, 부서·직급·연봉은 직접 확인해 입력해주세요.</p>
        <div className="export-repeat-list">
          {profile.careers.length === 0 && <p className="export-empty">경력 정보를 추가하거나 “모든 경력 한 번에 불러오기”를 눌러주세요.</p>}
          {profile.careers.map((career, index) => { const selectedJob = jobKoreaJobCategories.includes(career.job) ? career.job : career.job ? '직접 입력' : ''; return <div className="export-repeat-item" key={`career-${index}`}><div className="export-repeat-heading"><strong>경력 {index + 1}</strong><button className="text-button danger-button" type="button" onClick={() => setProfile((current) => ({ ...current, careers: current.careers.filter((_, itemIndex) => itemIndex !== index) }))}>삭제</button></div><div className="export-form-grid">
            <label>회사명 *<input value={career.companyName} onChange={(event) => updateCareer(index, 'companyName', event.target.value)} /></label>
            <label>부서명<input value={career.department} onChange={(event) => updateCareer(index, 'department', event.target.value)} /></label>
            <label>입사년월 *<input type="month" value={career.startMonth} onChange={(event) => updateCareer(index, 'startMonth', event.target.value)} /></label>
            <label>퇴사년월{!career.isCurrent && ' *'}<input type="month" value={career.endMonth} onChange={(event) => updateCareer(index, 'endMonth', event.target.value)} disabled={career.isCurrent} /></label>
            <label className="export-checkbox"><input type="checkbox" checked={career.isCurrent} onChange={(event) => setProfile((current) => ({ ...current, careers: current.careers.map((item, itemIndex) => itemIndex === index ? { ...item, isCurrent: event.target.checked, endMonth: event.target.checked ? '' : item.endMonth } : item) }))} />재직중</label>
            <label>직급/직책<input value={career.title} onChange={(event) => updateCareer(index, 'title', event.target.value)} /></label>
            <label>담당직무 *<select value={selectedJob} onChange={(event) => updateCareer(index, 'job', event.target.value === '직접 입력' ? '' : event.target.value)}><option value="">선택하기</option>{jobKoreaJobCategories.map((job) => <option key={job} value={job}>{job}</option>)}</select></label>
            {selectedJob === '직접 입력' && <label>담당직무 직접입력<input value={career.job} onChange={(event) => updateCareer(index, 'job', event.target.value)} placeholder="웹개발자" /></label>}
            <label>연봉<input value={career.salary} onChange={(event) => updateCareer(index, 'salary', event.target.value)} placeholder="예: 5,000만원" /></label>
            <label className="export-checkbox export-profile-wide" title="JobKorea 인재정보에 이 경력의 회사명을 공개하지 않습니다."><input type="checkbox" checked={career.hideCompanyName} onChange={(event) => setProfile((current) => ({ ...current, careers: current.careers.map((item, itemIndex) => itemIndex === index ? { ...item, hideCompanyName: event.target.checked } : item) }))} />인재정보에 회사명 비공개 ⓘ</label>
            <label className="export-profile-wide">담당업무<textarea value={career.duties} onChange={(event) => updateCareer(index, 'duties', event.target.value)} rows={4} placeholder="담당하신 업무와 성과에 대해 간단명료하게 적어주세요." /></label>
          </div></div> })}
        </div><div className="export-subsection-heading career-description-heading"><strong>경력기술서</strong><button className="secondary-button compact-button" type="button" onClick={() => void generateCareerDescription()} disabled={isGeneratingCareerDescription}>{isGeneratingCareerDescription ? '자동 작성 중...' : 'Career 데이터로 자동 작성'}</button></div><p className="export-help">① 프로젝트 명 ② 고객사/Domain ③ 수행 기간 ④ 업무 내용 및 성과 ⑤ 담당 역할 ⑥ 보유/활용 Skill 순서로 작성합니다. 입력되지 않은 정보는 추측하지 않고 “입력 필요”로 표시합니다.</p><label className="export-wide-label"><textarea value={profile.careerDescription} onChange={(event) => updateProfile('careerDescription', event.target.value)} rows={10} placeholder="경력을 자유롭게 작성하거나 위 버튼으로 Career 데이터를 불러오세요." /></label></div>

      <div className="export-section"><div className="export-section-heading"><div><span className="eyebrow">OPTIONAL</span><h3>추가 이력서 항목</h3></div></div>
        {renderAdditionalSection('인턴·대외활동', 'activities')}
        {renderAdditionalSection('교육이수', 'trainings')}
        {renderAdditionalSection('자격증', 'certificates')}
        {renderAdditionalSection('수상', 'awards')}
        <div className="export-subsection"><div className="export-subsection-heading"><strong>해외 근무 경력</strong><button className="text-button" type="button" onClick={() => setProfile((current) => ({ ...current, overseas: [...current.overseas, emptyJobKoreaOverseasExperience()] }))}>해외 근무 경력 추가하기</button></div>{profile.overseas.length === 0 && <p className="export-empty">선택 입력 항목입니다.</p>}<div className="export-repeat-list">{profile.overseas.map((experience, index) => <div className="export-repeat-item" key={`overseas-${index}`}><div className="export-repeat-heading"><strong>해외 근무 경력 {index + 1}</strong><button className="text-button danger-button" type="button" onClick={() => setProfile((current) => ({ ...current, overseas: current.overseas.filter((_, itemIndex) => itemIndex !== index) }))}>삭제</button></div><div className="export-form-grid"><label>국가<input value={experience.country} onChange={(event) => updateOverseas(index, 'country', event.target.value)} placeholder="미국" /></label><label>회사·기관<input value={experience.organization} onChange={(event) => updateOverseas(index, 'organization', event.target.value)} /></label><label>시작년월<input type="month" value={experience.startMonth} onChange={(event) => updateOverseas(index, 'startMonth', event.target.value)} /></label><label>종료년월<input type="month" value={experience.endMonth} onChange={(event) => updateOverseas(index, 'endMonth', event.target.value)} /></label><label className="export-profile-wide">내용<textarea value={experience.description} onChange={(event) => updateOverseas(index, 'description', event.target.value)} rows={3} /></label></div></div>)}</div></div>
        <div className="export-subsection"><div className="export-subsection-heading"><strong>어학</strong><button className="text-button" type="button" onClick={() => setProfile((current) => ({ ...current, languages: [...current.languages, emptyJobKoreaLanguage()] }))}>추가</button></div>{profile.languages.length === 0 && <p className="export-empty">선택 입력 항목입니다.</p>}<div className="export-repeat-list">{profile.languages.map((language, index) => <div className="export-repeat-item" key={`language-${index}`}><div className="export-repeat-heading"><strong>어학 {index + 1}</strong><button className="text-button danger-button" type="button" onClick={() => setProfile((current) => ({ ...current, languages: current.languages.filter((_, itemIndex) => itemIndex !== index) }))}>삭제</button></div><div className="export-form-grid"><label>언어<input value={language.language} onChange={(event) => updateLanguage(index, 'language', event.target.value)} placeholder="영어" /></label><label>수준<input value={language.level} onChange={(event) => updateLanguage(index, 'level', event.target.value)} placeholder="비즈니스 회화" /></label><label>점수·등급<input value={language.score} onChange={(event) => updateLanguage(index, 'score', event.target.value)} placeholder="TOEIC 900" /></label></div></div>)}</div></div>
        <div className="export-subsection"><div className="export-subsection-heading"><strong>포트폴리오</strong><button className="text-button" type="button" onClick={() => setProfile((current) => ({ ...current, portfolios: [...current.portfolios, emptyJobKoreaPortfolio()] }))}>추가</button></div>{profile.portfolios.length === 0 && <p className="export-empty">선택 입력 항목입니다.</p>}<div className="export-repeat-list">{profile.portfolios.map((portfolio, index) => <div className="export-repeat-item" key={`portfolio-${index}`}><div className="export-repeat-heading"><strong>포트폴리오 {index + 1}</strong><button className="text-button danger-button" type="button" onClick={() => setProfile((current) => ({ ...current, portfolios: current.portfolios.filter((_, itemIndex) => itemIndex !== index) }))}>삭제</button></div><div className="export-form-grid"><label>제목<input value={portfolio.title} onChange={(event) => updatePortfolio(index, 'title', event.target.value)} /></label><label>URL<input type="url" value={portfolio.url} onChange={(event) => updatePortfolio(index, 'url', event.target.value)} placeholder="https://" /></label><label className="export-profile-wide">설명<textarea value={portfolio.description} onChange={(event) => updatePortfolio(index, 'description', event.target.value)} rows={3} /></label></div></div>)}</div></div>
      </div>

      <div className="export-section"><div className="export-section-heading"><div><span className="eyebrow">ADDITIONAL</span><h3>취업우대 · 병역</h3></div></div><div className="export-subsection"><div className="export-subsection-heading"><strong>취업우대</strong></div><div className="export-preference-grid"><label className="export-checkbox"><input type="checkbox" checked={profile.veteran} onChange={(event) => setProfile((current) => ({ ...current, veteran: event.target.checked }))} />보훈대상</label><label className="export-checkbox"><input type="checkbox" checked={profile.employmentProtection} onChange={(event) => setProfile((current) => ({ ...current, employmentProtection: event.target.checked }))} />취업보호 대상</label><label className="export-checkbox"><input type="checkbox" checked={profile.employmentSupport} onChange={(event) => setProfile((current) => ({ ...current, employmentSupport: event.target.checked }))} />고용지원금 대상</label><label className="export-checkbox"><input type="checkbox" checked={profile.disability} onChange={(event) => setProfile((current) => ({ ...current, disability: event.target.checked }))} />장애</label></div></div><div className="export-form-grid">
        <label>병역<select value={profile.militaryStatus} onChange={(event) => updateProfile('militaryStatus', event.target.value)}><option value="">선택</option><option>군필</option><option>미필</option><option>면제</option><option>복무중</option></select></label>
        <label>군별·제대 계급<input value={profile.militaryDetails} onChange={(event) => updateProfile('militaryDetails', event.target.value)} placeholder="육군 병장" /></label>
        <label>입대일<input type="month" value={profile.enlistmentDate} onChange={(event) => updateProfile('enlistmentDate', event.target.value)} /></label>
        <label>제대일<input type="month" value={profile.dischargeDate} onChange={(event) => updateProfile('dischargeDate', event.target.value)} /></label>
      </div></div>

      <div className="export-section self-introduction-section"><div className="export-section-heading"><div><span className="eyebrow">SELF INTRODUCTION</span><h3>자기소개서</h3></div><button className="secondary-button compact-button" type="button" onClick={() => setProfile((current) => ({ ...current, selfIntroductions: [...current.selfIntroductions, { title: '', content: '' }] }))}>자기소개서 항목 추가</button></div>
        <p className="export-help self-introduction-help">각 제목은 지원자를 어필하는 독립적인 주제입니다. 생성 시 3개로 시작하며 필요에 따라 항목을 추가하거나 삭제할 수 있습니다.</p>
        <div className="export-repeat-list">{profile.selfIntroductions.length === 0 && <p className="export-empty">자기소개서 항목을 추가해주세요.</p>}{profile.selfIntroductions.map((item, index) => <div className="export-repeat-item self-introduction-item" key={`self-introduction-${index}`}><div className="export-repeat-heading"><strong>자기소개서 항목 {index + 1}</strong><button className="text-button danger-button" type="button" onClick={() => setProfile((current) => ({ ...current, selfIntroductions: current.selfIntroductions.filter((_, itemIndex) => itemIndex !== index) }))}>단락삭제</button></div><div className="export-form-grid">
          <label className="export-profile-wide">제목<input value={item.title} onChange={(event) => updateSelfIntroduction(index, 'title', event.target.value)} maxLength={50} placeholder="예: 문제 해결로 만드는 안정적인 백엔드" /><span className="export-character-count"><em>{item.title.length}</em>/50자</span></label>
          <label className="export-profile-wide">내용<textarea value={item.content} onChange={(event) => updateSelfIntroduction(index, 'content', event.target.value)} rows={10} placeholder="해당 내용을 입력하세요." /><span className="export-character-count"><em>{item.content.length}</em>자</span></label>
        </div></div>)}</div>
      </div>

      <div className="export-section"><div className="export-section-heading"><div><span className="eyebrow">CONDITIONS</span><h3>희망근무조건</h3></div></div><div className="export-form-grid">
        <label>고용형태<input value={profile.employmentType} onChange={(event) => updateProfile('employmentType', event.target.value)} placeholder="정규직" /></label>
        <label>희망연봉<input value={profile.desiredSalary} onChange={(event) => updateProfile('desiredSalary', event.target.value)} placeholder="면접 후 결정" /></label>
        <label className="export-profile-wide">희망근무지 *<input value={profile.desiredLocations} onChange={(event) => updateProfile('desiredLocations', event.target.value)} placeholder="서울전지역, 경기 성남시" /></label>
        <p className="export-help export-profile-wide">최대 3개까지 입력할 수 있습니다. 쉼표 또는 줄바꿈으로 구분합니다.</p>
      </div></div>

      <label className="export-review-check"><input type="checkbox" checked={reviewConfirmed} onChange={(event) => { setReviewConfirmed(event.target.checked); if (event.target.checked) setStatus('') }} /><span><strong>다운로드 전 확인</strong><br />개인정보와 자동 생성 문구를 직접 검토했으며, 필요한 내용을 수정한 뒤 JobKorea에 직접 업로드하겠습니다.</span></label>
      <p className="export-profile-warning" role="note">JobKorea 로그인 정보·쿠키·업로드 세션은 이 서비스에 전송되지 않습니다. JobKorea의 개인정보 동의와 최종 제출은 JobKorea 화면에서 직접 진행하세요.</p>

      <div className="resume-export-preview-shell"><div ref={previewRef} style={{ boxSizing: 'border-box', width: '794px', minHeight: '1123px', padding: '26px 30px', background: '#ffffff', color: '#202820', fontFamily: 'Arial, "Malgun Gothic", sans-serif' }}>
        <div style={{ display: 'flex', alignItems: 'start', justifyContent: 'space-between', gap: '16px' }}><div><h1 style={{ margin: 0, fontSize: '25px', lineHeight: 1.3, letterSpacing: '0.24em' }}>이 력 서</h1><p style={{ margin: '8px 0 0', color: '#718071', fontSize: '10px' }}>{exportTitle} · Version {version.versionNumber}</p></div>{profile.photoDataUrl && <img src={profile.photoDataUrl} alt="" style={{ width: '74px', height: '94px', objectFit: 'cover', border: '1px solid #aeb6ae' }} />}</div>
        <div style={{ marginTop: '14px', padding: '8px 10px', background: '#dfe5df', color: '#202820', fontSize: '12px', fontWeight: 700 }}>인적사항</div>
        <table style={{ width: '100%', borderCollapse: 'collapse', tableLayout: 'fixed', fontSize: '11px' }}><tbody>
          <tr><th style={{ width: '88px', border: '1px solid #aeb6ae', padding: '8px', background: '#eef1ee', textAlign: 'center' }}>성명</th><td style={{ border: '1px solid #aeb6ae', padding: '8px' }}>{profile.name || '입력 필요'}</td><th style={{ width: '88px', border: '1px solid #aeb6ae', padding: '8px', background: '#eef1ee', textAlign: 'center' }}>생년월일</th><td style={{ border: '1px solid #aeb6ae', padding: '8px' }}>{formatJobKoreaDate(profile.birthDate)}</td></tr>
          <tr><th style={{ border: '1px solid #aeb6ae', padding: '8px', background: '#eef1ee', textAlign: 'center' }}>성별</th><td style={{ border: '1px solid #aeb6ae', padding: '8px' }}>{profile.gender || '입력 필요'}</td><th style={{ border: '1px solid #aeb6ae', padding: '8px', background: '#eef1ee', textAlign: 'center' }}>휴대폰</th><td style={{ border: '1px solid #aeb6ae', padding: '8px' }}>{profile.mobile || '입력 필요'}</td></tr>
          <tr><th style={{ border: '1px solid #aeb6ae', padding: '8px', background: '#eef1ee', textAlign: 'center' }}>이메일</th><td style={{ border: '1px solid #aeb6ae', padding: '8px' }}>{profile.email || '입력 필요'}</td><th style={{ border: '1px solid #aeb6ae', padding: '8px', background: '#eef1ee', textAlign: 'center' }}>전화번호</th><td style={{ border: '1px solid #aeb6ae', padding: '8px' }}>{profile.phone || '-'}</td></tr>
          <tr><th style={{ border: '1px solid #aeb6ae', padding: '8px', background: '#eef1ee', textAlign: 'center' }}>주소</th><td colSpan={3} style={{ border: '1px solid #aeb6ae', padding: '8px' }}>{profile.address || '-'}</td></tr>
        </tbody></table>
        <JobKoreaPreviewSection title="희망직무"><strong>{desiredJobs.length > 0 ? desiredJobs.join(' · ') : '입력 필요'}</strong></JobKoreaPreviewSection>
        <JobKoreaPreviewSection title={`스킬 (${skills.length}/20)`}>{skills.length > 0 ? skills.join(' · ') : '입력 필요'}</JobKoreaPreviewSection>
        {industryKeywords.length > 0 && <JobKoreaPreviewSection title="산업 키워드">{industryKeywords.join(' · ')}</JobKoreaPreviewSection>}
        {profile.educations.length > 0 && <JobKoreaPreviewSection title="학력">{profile.educations.map((education, index) => <div key={`preview-education-${index}`} style={{ marginBottom: index === profile.educations.length - 1 ? 0 : '10px' }}><strong>{education.schoolName || '학교명 입력 필요'}</strong> · {education.major || '전공 입력 필요'}<br />{formatJobKoreaMonth(education.admissionMonth)} ~ {formatJobKoreaMonth(education.graduationMonth)} · {education.status || '상태 미입력'}{education.gpa && ` · 학점 ${education.gpa}${education.gpaScale ? `/${education.gpaScale}` : ''}`}{education.additionalMajor && <><br />{education.additionalMajor}</>}</div>)}</JobKoreaPreviewSection>}
        {profile.careers.length > 0 && <JobKoreaPreviewSection title="경력">{profile.careers.map((career, index) => <div key={`preview-career-${index}`} style={{ marginBottom: index === profile.careers.length - 1 ? 0 : '12px' }}><strong>{career.hideCompanyName ? '회사명 비공개' : career.companyName || '회사명 입력 필요'}</strong>{career.department && ` · ${career.department}`}<br />{formatJobKoreaMonth(career.startMonth)} ~ {career.isCurrent ? '재직중' : formatJobKoreaMonth(career.endMonth)} · {career.job || '담당직무 입력 필요'}{career.title && ` · ${career.title}`}{career.salary && ` · ${career.salary}`}{career.duties && <p style={{ margin: '5px 0 0', whiteSpace: 'pre-wrap' }}>{career.duties}</p>}</div>)}</JobKoreaPreviewSection>}
        {legacySections.map((section) => <JobKoreaPreviewSection key={section.title} title={section.title === '경력사항' ? '경력' : section.title}><ul style={{ margin: 0, paddingLeft: '18px' }}>{section.lines.map((line, index) => <li key={`${section.title}-${index}`} style={{ marginBottom: '5px' }}>{line}</li>)}</ul></JobKoreaPreviewSection>)}
        {profile.careerDescription && <JobKoreaPreviewSection title="경력기술서"><div style={{ whiteSpace: 'pre-wrap' }}>{profile.careerDescription}</div></JobKoreaPreviewSection>}
        {renderAdditionalPreview('인턴·대외활동', profile.activities)}
        {renderAdditionalPreview('교육이수', profile.trainings)}
        {renderAdditionalPreview('자격증', profile.certificates)}
        {renderAdditionalPreview('수상', profile.awards)}
        {profile.overseas.length > 0 && <JobKoreaPreviewSection title="해외 근무 경력">{profile.overseas.map((experience, index) => <div key={`preview-overseas-${index}`} style={{ marginBottom: index === profile.overseas.length - 1 ? 0 : '9px' }}><strong>{experience.country || '국가 입력 필요'}</strong>{experience.organization && ` · ${experience.organization}`}<br />{experience.startMonth || experience.endMonth ? `${formatJobKoreaMonth(experience.startMonth)} ~ ${formatJobKoreaMonth(experience.endMonth)}` : ''}{experience.description && <div style={{ whiteSpace: 'pre-wrap' }}>{experience.description}</div>}</div>)}</JobKoreaPreviewSection>}
        {profile.languages.length > 0 && <JobKoreaPreviewSection title="어학">{profile.languages.map((language, index) => <div key={`preview-language-${index}`} style={{ marginBottom: index === profile.languages.length - 1 ? 0 : '7px' }}><strong>{language.language || '언어 미입력'}</strong>{language.level && ` · ${language.level}`}{language.score && ` · ${language.score}`}</div>)}</JobKoreaPreviewSection>}
        {profile.portfolios.length > 0 && <JobKoreaPreviewSection title="포트폴리오">{profile.portfolios.map((portfolio, index) => <div key={`preview-portfolio-${index}`} style={{ marginBottom: index === profile.portfolios.length - 1 ? 0 : '9px' }}><strong>{portfolio.title || '포트폴리오'}</strong>{portfolio.url && <><br />{portfolio.url}</>}{portfolio.description && <div style={{ whiteSpace: 'pre-wrap' }}>{portfolio.description}</div>}</div>)}</JobKoreaPreviewSection>}
        {(profile.veteran || profile.employmentProtection || profile.employmentSupport || profile.disability || profile.militaryStatus) && <JobKoreaPreviewSection title="취업우대 · 병역">{[profile.veteran && '보훈대상', profile.employmentProtection && '취업보호 대상', profile.employmentSupport && '고용지원금 대상', profile.disability && '장애'].filter(Boolean).join(' · ')}{profile.militaryStatus && <><strong>{[profile.veteran, profile.employmentProtection, profile.employmentSupport, profile.disability].some(Boolean) ? ' · ' : ''}{profile.militaryStatus}</strong>{profile.militaryDetails && ` · ${profile.militaryDetails}`}{(profile.enlistmentDate || profile.dischargeDate) && <><br />{formatJobKoreaMonth(profile.enlistmentDate)} ~ {formatJobKoreaMonth(profile.dischargeDate)}</>}</>}</JobKoreaPreviewSection>}
        {profile.selfIntroductions.map((item, index) => (item.title || item.content) && <JobKoreaPreviewSection key={`preview-self-introduction-${index}`} title={item.title || '자기소개서'}><div style={{ whiteSpace: 'pre-wrap' }}>{item.content}</div></JobKoreaPreviewSection>)}
        {(profile.employmentType || profile.desiredSalary || desiredLocations.length > 0) && <JobKoreaPreviewSection title="희망근무조건">{profile.employmentType && <div>고용형태: {profile.employmentType}</div>}{profile.desiredSalary && <div>희망연봉: {profile.desiredSalary}</div>}{desiredLocations.length > 0 && <div>희망근무지: {desiredLocations.join(' · ')}</div>}</JobKoreaPreviewSection>}
        <footer style={{ paddingTop: '18px', color: '#8b958c', fontSize: '9px' }}>JOBKOREA_STANDARD_V2 · 사용자가 검토한 ResumeVersion과 입력한 프로필에서 생성된 업로드용 파일입니다.</footer>
      </div></div>
      {status && <p className="export-status" role="status">{status}</p>}
      <div className="modal-actions"><button className="text-button" onClick={onClose}>취소</button><button className="secondary-button compact-button" onClick={() => window.open('https://resume.jobkorea.co.kr/resume/upload', '_blank', 'noopener,noreferrer')}>JobKorea 업로드 페이지 열기</button><button className="primary-button compact-button" onClick={() => void downloadPdf()} disabled={isGenerating || !reviewConfirmed}>{isGenerating ? 'PDF 생성 중...' : onPersistSkills ? '검토 완료 · 이력서 저장 및 PDF 다운로드' : '검토 완료 · PDF 다운로드'}</button></div>
    </section>
  </div>
}

function ResumeDraftReviewModal({
  jobPosting,
  draft,
  isApproving,
  onClose,
  onApprove,
}: {
  jobPosting: JobPosting
  draft: ResumeDraftResponse
  isApproving: boolean
  onClose: () => void
  onApprove: () => void
}) {
  function DraftLine({ line }: { line: ResumeDraftLine | null }) {
    if (!line?.text) return null
    return <li><span>{line.text}</span><small>{line.citations.map((citation) => citation.citationId).join(', ')}</small></li>
  }

  function DraftSection({ title, lines }: { title: string; lines: ResumeDraftLine[] }) {
    const visibleLines = lines.filter((line) => line?.text)
    if (visibleLines.length === 0) return null
    return <section className="draft-review-section"><h3>{title}</h3><ul>{visibleLines.map((line, index) => <DraftLine key={`${title}-${index}`} line={line} />)}</ul></section>
  }

  function DraftSelfIntroduction({ sections, summary }: { sections: ResumeDraftSection[]; summary: ResumeDraftLine | null }) {
    const visibleSections = (sections ?? []).filter((section) => section?.title && section?.content)
    if (visibleSections.length === 0) return <DraftSection title="자기소개서" lines={summary ? [summary] : []} />
    return <section className="draft-review-section"><div className="draft-review-section-heading"><h3>자기소개서</h3><span>{visibleSections.length}개 어필 주제</span></div><div className="draft-topic-list">{visibleSections.map((section, index) => <article className="draft-topic" key={`self-introduction-${index}`}><h4>{section.title}</h4><p>{section.content}</p><small>{section.citations.map((citation) => citation.citationId).join(', ')}</small></article>)}</div></section>
  }

  return <div className="modal-backdrop" role="presentation">
    <section className="resume-draft-modal" role="dialog" aria-modal="true" aria-labelledby="resume-draft-title">
      <div className="modal-heading"><div><span className="eyebrow">JOBKOREA RESUME DRAFT</span><h2 id="resume-draft-title">JobKorea 이력서 초안 검토</h2></div><button className="modal-close" onClick={onClose} aria-label="닫기">×</button></div>
      <p className="modal-description"><strong>{jobPosting.companyName}</strong> · {jobPosting.title}<br />저장된 Career evidence에서 확인된 내용만 사용했습니다. 아래 내용은 승인 후 JobKorea 표준 항목으로 변환되며, 개인정보·학력·근무조건은 직접 입력합니다.</p>
      <div className="draft-review-meta"><span>provider: {draft.provider}</span><span>model: {draft.model}</span><span>retrieved: {draft.retrievedChunkCount}개</span></div>
      <div className="draft-review-content">
        <DraftSection title="희망직무" lines={[draft.draft.headline]} />
        <DraftSelfIntroduction sections={draft.draft.selfIntroductionSections} summary={draft.draft.summary} />
        <DraftSection title="경력기술서" lines={[...draft.draft.experienceBullets, ...draft.draft.projectBullets]} />
        <DraftSection title="스킬" lines={draft.draft.skills} />
      </div>
      <p className="export-profile-warning">자동으로 작성된 문장은 반드시 직접 검토하세요. 승인하면 새 이력서와 첫 번째 버전이 생성됩니다.</p>
      <div className="modal-actions"><button className="text-button" onClick={onClose}>취소</button><button className="primary-button compact-button" onClick={onApprove} disabled={isApproving}>{isApproving ? 'JobKorea 이력서 준비 중...' : '검토 완료 · JobKorea 양식으로 계속'}</button></div>
    </section>
  </div>
}

function ResumePageContent({ user }: { user: User }) {
  const location = useLocation()
  const autoOpenExportRef = useRef(false)
  const [resumes, setResumes] = useState<Resume[]>([])
  const [latestVersions, setLatestVersions] = useState<Record<string, ResumeVersion | null>>({})
  const [selectedResumeId, setSelectedResumeId] = useState<string | null>(null)
  const [versions, setVersions] = useState<ResumeVersion[]>([])
  const [selectedVersion, setSelectedVersion] = useState<ResumeVersion | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isLoadingVersions, setIsLoadingVersions] = useState(false)
  const [loadError, setLoadError] = useState('')
  const [renameName, setRenameName] = useState('')
  const [versionEditor, setVersionEditor] = useState<ResumeVersionEditor>(emptyResumeVersionEditor)
  const [resumeCareerSkills, setResumeCareerSkills] = useState<string[]>([])
  const [isVersionPreview, setIsVersionPreview] = useState(false)
  const [formError, setFormError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)
  const [isCareerReviewOpen, setIsCareerReviewOpen] = useState(false)
  const [isExportOpen, setIsExportOpen] = useState(false)

  async function loadResumes(preferredId?: string) {
    setIsLoading(true)
    setLoadError('')
    try {
      const result = await request<PageResponse<Resume>>('/v1/resumes?page=0&size=20')
      setResumes(result.content)
      setLatestVersions({})
      const previewEntries = await Promise.all(result.content.map(async (resume) => {
        try {
          const versions = await request<PageResponse<ResumeVersion>>(`/v1/resumes/${resume.id}/versions?page=0&size=1`)
          return [resume.id, versions.content[0] ?? null] as const
        } catch {
          return [resume.id, null] as const
        }
      }))
      setLatestVersions(Object.fromEntries(previewEntries))
      const groupedResumes = groupResumes(result.content)
      const preferredGroup = preferredId
        ? groupedResumes.find((group) => group.resumes.some((resume) => resume.id === preferredId))
        : undefined
      const selectedGroup = selectedResumeId
        ? groupedResumes.find((group) => group.resumes.some((resume) => resume.id === selectedResumeId))
        : undefined
      const nextId = preferredGroup?.resume.id ?? selectedGroup?.resume.id ?? groupedResumes[0]?.resume.id ?? null
      setSelectedResumeId(nextId)
      const selected = groupedResumes.find((group) => group.resume.id === nextId)?.resume
      setRenameName(selected?.name ?? '')
    } catch (error) {
      setLoadError(error instanceof Error ? error.message : '이력서 목록을 불러오지 못했습니다.')
    } finally {
      setIsLoading(false)
    }
  }

  async function loadVersions(resumeId: string) {
    setIsLoadingVersions(true)
    setLoadError('')
    try {
      const selectedGroup = groupResumes(resumes).find((group) => group.resumes.some((resume) => resume.id === resumeId))
      const groupMembers = selectedGroup?.resumes ?? resumes.filter((resume) => resume.id === resumeId)
      const results = await Promise.all(groupMembers.map((resume) => request<PageResponse<ResumeVersion>>(`/v1/resumes/${resume.id}/versions?page=0&size=100`)))
      const combinedVersions = results
        .flatMap((result) => result.content)
        .sort((left, right) => right.createdAt.localeCompare(left.createdAt) || right.versionNumber - left.versionNumber)
      setVersions(combinedVersions)
      const latestVersion = combinedVersions[0] ?? null
      const latestEditor = latestVersion ? resumeVersionEditorFromContent(latestVersion.content) : emptyResumeVersionEditor
      let careerSkills: string[] = []
      try {
        const [experiences, projects] = await Promise.all([
          requestAllPages<Experience>('/v1/experiences'),
          requestAllPages<Project>('/v1/projects'),
        ])
        careerSkills = collectCareerSkills([], projects, experiences)
      } catch {
        setFormError('Career 기술 스택을 불러오지 못해 이력서에 저장된 기술만 표시합니다.')
      }
      setSelectedVersion(latestVersion)
      setResumeCareerSkills(careerSkills)
      setVersionEditor({
        ...latestEditor,
        skills: mergeUniqueSkills(splitExplicitSkills(latestEditor.skills), careerSkills).join(', '),
      })
      setIsVersionPreview(true)
    } catch (error) {
      setLoadError(error instanceof Error ? error.message : '이력서 버전을 불러오지 못했습니다.')
    } finally {
      setIsLoadingVersions(false)
    }
  }

  useEffect(() => {
    const preferredResumeId = new URLSearchParams(location.search).get('resumeId') ?? undefined
    void loadResumes(preferredResumeId)
  }, [])

  useEffect(() => {
    if (selectedResumeId) void loadVersions(selectedResumeId)
    else {
      setVersions([])
      setSelectedVersion(null)
      setVersionEditor(emptyResumeVersionEditor)
      setResumeCareerSkills([])
      setRenameName('')
    }
  }, [selectedResumeId])

  function selectResume(resume: Resume) {
    setSelectedResumeId(resume.id)
    setRenameName(resume.name)
    setVersionEditor(emptyResumeVersionEditor)
    setIsVersionPreview(true)
    setFormError('')
  }

  function selectVersion(version: ResumeVersion) {
    const editor = resumeVersionEditorFromContent(version.content)
    setSelectedVersion(version)
    setVersionEditor({
      ...editor,
      skills: mergeUniqueSkills(splitExplicitSkills(editor.skills), resumeCareerSkills).join(', '),
    })
    setIsVersionPreview(true)
    setFormError('')
  }

  async function renameResume() {
    if (!selectedResumeId || !renameName.trim()) {
      setFormError('이력서 이름을 입력해주세요.')
      return
    }
    setIsSubmitting(true)
    setFormError('')
    try {
      const selectedGroup = groupResumes(resumes).find((group) => group.resumes.some((resume) => resume.id === selectedResumeId))
      const groupMembers = selectedGroup?.resumes ?? []
      await Promise.all(groupMembers.map((resume) => request<Resume>(`/v1/resumes/${resume.id}`, { method: 'PUT', body: JSON.stringify({ name: renameName.trim() }) })))
      await loadResumes(selectedResumeId)
    } catch (error) {
      setFormError(error instanceof Error ? error.message : '이력서 이름을 변경하지 못했습니다.')
    } finally {
      setIsSubmitting(false)
    }
  }

  async function saveVersion(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const content = resumeContentFromEditor(versionEditor)
    if (!selectedResumeId || !selectedVersion || !content) {
      setFormError('이력서 내용을 입력해주세요.')
      return
    }
    if (content.length > 100000) {
      setFormError('이력서 전체 내용은 100,000자 이하여야 합니다.')
      return
    }
    setIsSubmitting(true)
    setFormError('')
    try {
      await request<ResumeVersion>(`/v1/resumes/${selectedResumeId}/versions`, { method: 'POST', body: JSON.stringify({ content }) })
      await loadResumes(selectedResumeId)
      await loadVersions(selectedResumeId)
    } catch (error) {
      setFormError(error instanceof Error ? error.message : '이력서 버전을 저장하지 못했습니다.')
    } finally {
      setIsSubmitting(false)
    }
  }

  async function deleteResume() {
    if (!selectedResumeId || !window.confirm('이 공고의 이력서와 모든 버전을 삭제할까요?')) return
    setIsDeleting(true)
    setFormError('')
    try {
      const selectedGroup = groupResumes(resumes).find((group) => group.resumes.some((resume) => resume.id === selectedResumeId))
      const groupMembers = selectedGroup?.resumes ?? []
      await Promise.all(groupMembers.map((resume) => request<void>(`/v1/resumes/${resume.id}`, { method: 'DELETE' })))
      setSelectedResumeId(null)
      await loadResumes()
    } catch (error) {
      setFormError(error instanceof Error ? error.message : '이력서를 삭제하지 못했습니다.')
    } finally {
      setIsDeleting(false)
    }
  }

  const resumeGroups = groupResumes(resumes)
  const selectedResume = resumes.find((resume) => resume.id === selectedResumeId)
  const selectedResumeGroup = resumeGroups.find((group) => group.resumes.some((resume) => resume.id === selectedResumeId))
  const displayVersionNumber = selectedVersion
    ? Math.max(1, versions.length - versions.findIndex((version) => version.id === selectedVersion.id))
    : null

  useEffect(() => {
    const params = new URLSearchParams(location.search)
    if (
      params.get('export') === 'jobkorea'
      && selectedResume
      && selectedVersion
      && !isLoadingVersions
      && !autoOpenExportRef.current
    ) {
      autoOpenExportRef.current = true
      setIsCareerReviewOpen(true)
    }
  }, [isLoadingVersions, location.search, selectedResume, selectedVersion])

  return <>
    <header className="page-heading"><div><span className="eyebrow">CAREER DOCUMENTS</span><h1>이력서</h1><p>내 경력을 바탕으로 이력서를 만들고, 수정할 때마다 버전으로 보존하세요.</p></div><div className="page-tip"><span>TIP</span><strong>처음에는 짧게 시작해도 괜찮아요.</strong><small>이후 경력과 공고에 맞춰 새 버전을 만들 수 있습니다.</small></div></header>
    {formError && <p className="form-error resume-global-error" role="alert">{formError}</p>}
    <section className="resume-layout">
      <aside className="resume-list-panel">
        <div className="list-heading"><div><span className="eyebrow">YOUR RESUMES</span><h2>{resumeGroups.length}개의 이력서</h2><small className="resume-list-help">같은 공고는 하나로 묶고, 버전 기록에서 전체 내용을 확인할 수 있어요.</small></div><button className="text-button" onClick={() => void loadResumes()}>새로고침</button></div>
        {isLoading && <div className="list-state">이력서를 불러오는 중입니다...</div>}
        {!isLoading && loadError && <div className="list-state error-state">{loadError}</div>}
        {!isLoading && !loadError && resumes.length === 0 && <div className="list-state">아직 이력서가 없습니다.</div>}
        {!isLoading && !loadError && resumeGroups.length > 0 && <div className="resume-list">{resumeGroups.map((group) => {
          const resume = group.resume
          const latestVersion = latestVersions[resume.id]
          return <button className={resume.id === selectedResumeId ? 'resume-list-item selected' : 'resume-list-item'} key={group.key} onClick={() => selectResume(resume)}>
            <span className="resume-card-topline"><span className="resume-card-label">JOB RESUME</span><span className="resume-card-version">최신 v{resumeGroupVersionCount(group)}</span></span>
            <strong>{resume.name}</strong>
            {latestVersion ? <ResumePreviewSummary editor={resumeVersionEditorFromContent(latestVersion.content)} compact /> : <span className="resume-card-loading">미리보기를 준비하는 중입니다...</span>}
            <span className="resume-card-footer"><span>{formatKoreanDateTime(resume.updatedAt)}</span></span>
          </button>
        })}</div>}
      </aside>
      <section className="resume-detail-panel">
        {!selectedResume && <div className="resume-empty"><span className="empty-state-icon">✦</span><span className="eyebrow">RESUME WORKSPACE</span><h2>채용공고에서<br />이력서를 만들어보세요.</h2><p>채용공고를 저장하고 Career 데이터 점검을 완료하면 공고에 맞는 이력서를 생성할 수 있습니다.</p><Link className="primary-button compact-button resume-empty-action" to="/job-postings">채용공고에서 이력서 만들기</Link></div>}
        {selectedResume && <>
          <div className="resume-detail-header"><div><span className="eyebrow">RESUME WORKSPACE</span><h2>{selectedResume.name}</h2><span className="version-count">현재 최신 버전 v{selectedResumeGroup ? resumeGroupVersionCount(selectedResumeGroup) : selectedResume.latestVersionNumber}</span></div><button className="text-button danger-button" onClick={() => void deleteResume()} disabled={isDeleting}>{isDeleting ? '삭제 중' : '이력서 삭제'}</button></div>
          <div className="rename-row"><input value={renameName} onChange={(event) => setRenameName(event.target.value)} maxLength={200} /><button className="text-button" onClick={() => void renameResume()} disabled={isSubmitting}>이름 저장</button></div>
          <div className="version-section"><div className="section-title"><div><span className="eyebrow">VERSION HISTORY</span><h2>버전 기록</h2></div><span className="version-count">{versions.length}개 버전</span></div>
            {isLoadingVersions && <div className="list-state">버전을 불러오는 중입니다...</div>}
            {!isLoadingVersions && versions.length > 0 && <div className="version-list">{versions.map((version, index) => <button className={selectedVersion?.id === version.id ? 'version-item selected' : 'version-item'} key={version.id} onClick={() => selectVersion(version)}><span className="version-item-heading"><strong>버전 {versions.length - index}</strong><span>{formatKoreanDateTime(version.createdAt)}</span></span><ResumePreviewSummary editor={resumeVersionEditorFromContent(version.content)} compact /></button>)}</div>}
          </div>
          {selectedVersion && <div className="version-preview"><div className="section-title"><div><span className="eyebrow">{isVersionPreview ? 'RESUME PREVIEW' : 'EDITING VERSION'}</span><h2>Version {displayVersionNumber}</h2></div><div className="version-preview-actions"><span className="immutable-badge">기존 버전 보존</span><button className="secondary-button compact-button" type="button" onClick={() => setIsVersionPreview((current) => !current)}>{isVersionPreview ? '편집하기' : '미리보기'}</button><button className="secondary-button compact-button" type="button" onClick={() => setIsCareerReviewOpen(true)}>JobKorea 파일 생성</button></div></div>
          {isVersionPreview ? <article className="resume-version-preview-card">
            <header><span>RESUME</span><h3>{renameName || selectedResume.name}</h3><p>{versionEditor.headline || '희망직무를 입력해주세요.'}</p></header>
            {versionEditor.selfIntroduction && <section><h4>자기소개서</h4><p>{versionEditor.selfIntroduction}</p></section>}
            {versionEditor.careerDescription && <section><h4>경력기술서</h4><p>{versionEditor.careerDescription}</p></section>}
            {versionEditor.skills && <section><h4>기술 및 역량</h4><div className="resume-preview-skills">{splitJobKoreaList(versionEditor.skills).map((skill) => <span key={skill}>{skill}</span>)}</div></section>}
            {!versionEditor.headline && !versionEditor.selfIntroduction && !versionEditor.careerDescription && !versionEditor.skills && <p className="resume-preview-empty">미리볼 이력서 내용을 입력해주세요.</p>}
          </article> : <><p className="version-editor-help">항목별로 내용을 확인하고 수정하세요. AI 검증에 사용된 내부 근거 번호는 편집 본문에서 자동으로 숨깁니다.</p><form className="new-version-form version-editor-form" onSubmit={saveVersion}>
            <label><strong>희망직무</strong><small>지원하려는 직무나 이력서의 핵심 제목입니다.</small><textarea value={versionEditor.headline} onChange={(event) => setVersionEditor((current) => ({ ...current, headline: event.target.value }))} placeholder="예: 백엔드 개발자" maxLength={100000} rows={2} /></label>
            <label><strong>자기소개서</strong><small>핵심 역량, 문제 해결 경험과 지원 직무에 기여할 내용을 작성합니다.</small><textarea value={versionEditor.selfIntroduction} onChange={(event) => setVersionEditor((current) => ({ ...current, selfIntroduction: event.target.value }))} placeholder="자기소개서 내용을 입력하세요." maxLength={100000} rows={8} /></label>
            <label><strong>경력기술서</strong><small>경력과 프로젝트에서 맡은 역할, 수행 업무와 성과를 작성합니다.</small><textarea value={versionEditor.careerDescription} onChange={(event) => setVersionEditor((current) => ({ ...current, careerDescription: event.target.value }))} placeholder="경력 및 프로젝트 내용을 입력하세요." maxLength={100000} rows={8} /></label>
            <label><strong>기술 및 역량</strong><small>업무에 사용한 기술 스택과 도구를 작성합니다.</small><textarea value={versionEditor.skills} onChange={(event) => setVersionEditor((current) => ({ ...current, skills: event.target.value }))} placeholder="예: Java, Spring Boot, PostgreSQL" maxLength={100000} rows={4} /></label>
            <span className="version-count">저장하면 기존 Version {displayVersionNumber}는 보존되고 Version {versions.length + 1}로 저장됩니다.</span><button className="primary-button compact-button" disabled={isSubmitting} type="submit">{isSubmitting ? '저장 중...' : `Version ${versions.length + 1} 저장`}</button>
          </form></>}</div>}
          <ResumeApplicationQuestions key={selectedResume.id} resumeId={selectedResume.id} />
          {isCareerReviewOpen && selectedVersion && <CareerDataReviewModal title="JobKorea 파일 생성 전 Career 점검" continueLabel="확인하고 파일 작성하기" onClose={() => setIsCareerReviewOpen(false)} onContinue={() => { setIsCareerReviewOpen(false); setIsExportOpen(true) }} />}
          {isExportOpen && selectedVersion && <JobKoreaExportModal resumeName={selectedResume.name} version={selectedVersion} displayName={user.displayName} email={user.email} onClose={() => setIsExportOpen(false)} />}
        </>}
      </section>
    </section>
  </>
}

function ResumeApplicationQuestions({ resumeId }: { resumeId: string }) {
  const [items, setItems] = useState<ApplicationQuestionEditor[]>([])
  const [jobPostings, setJobPostings] = useState<JobPosting[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [savingId, setSavingId] = useState<string | null>(null)
  const [deletingId, setDeletingId] = useState<string | null>(null)
  const [generatingId, setGeneratingId] = useState<string | null>(null)
  const [draftCitations, setDraftCitations] = useState<Record<string, SelfIntroductionCitation[]>>({})
  const [draftProviders, setDraftProviders] = useState<Record<string, { provider: string; model: string; promptVersion: string; retrievedChunkCount: number }>>({})

  async function loadQuestions() {
    setIsLoading(true)
    setError('')
    try {
      const [questions, postings] = await Promise.all([
        requestAllPages<ResumeApplicationQuestion>(`/v1/resumes/${resumeId}/application-questions`),
        requestAllPages<JobPosting>('/v1/job-postings'),
      ])
      setJobPostings(postings)
      setDraftProviders({})
      setItems(questions.map((item) => ({
        localKey: item.id,
        id: item.id,
        jobPostingId: item.jobPostingId ?? '',
        savedJobPostingId: item.jobPostingId ?? '',
        question: item.question,
        savedQuestion: item.question,
        answer: item.answer,
        savedAnswer: item.answer,
      })))
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : '자기소개서 질문을 불러오지 못했습니다.')
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => { void loadQuestions() }, [resumeId])

  function updateItem(id: string, field: 'question' | 'answer', value: string) {
    setItems((current) => current.map((item) => item.localKey === id ? { ...item, [field]: value } : item))
    if (field === 'question') {
      setDraftCitations((current) => {
        const next = { ...current }
        delete next[id]
        return next
      })
      setDraftProviders((current) => {
        const next = { ...current }
        delete next[id]
        return next
      })
    }
  }

  function updateJobPosting(id: string, jobPostingId: string) {
    setItems((current) => current.map((item) => item.localKey === id
      ? { ...item, jobPostingId, answer: jobPostingId === item.savedJobPostingId ? item.answer : '' }
      : item))
    setDraftCitations((current) => {
      const next = { ...current }
      delete next[id]
      return next
    })
    setDraftProviders((current) => {
      const next = { ...current }
      delete next[id]
      return next
    })
  }

  function addQuestion() {
    const defaultJobPostingId = jobPostings.length === 1 ? jobPostings[0].id : ''
    setItems((current) => [...current, {
      localKey: crypto.randomUUID(),
      id: null,
      jobPostingId: defaultJobPostingId,
      savedJobPostingId: '',
      question: '',
      savedQuestion: '',
      answer: '',
      savedAnswer: '',
    }])
    setError('')
  }

  async function saveQuestionAndGenerate(item: ApplicationQuestionEditor) {
    const question = item.question.trim()
    if (!question) {
      setError('기업 자기소개서 질문을 입력해주세요.')
      return
    }
    if (!item.jobPostingId) {
      setError('이 질문에 연결할 기업공고를 선택해주세요.')
      return
    }
    setSavingId(item.localKey)
    setError('')
    setDraftProviders((current) => {
      const next = { ...current }
      delete next[item.localKey]
      return next
    })
    let questionSaved = false
    try {
      const saved = await request<ResumeApplicationQuestion>(item.id
        ? `/v1/resumes/${resumeId}/application-questions/${item.id}`
        : `/v1/resumes/${resumeId}/application-questions`, {
        method: item.id ? 'PUT' : 'POST',
        body: JSON.stringify({
          jobPostingId: item.jobPostingId,
          question,
          answer: item.jobPostingId === item.savedJobPostingId ? item.savedAnswer : '',
        }),
      })
      const postingChanged = item.jobPostingId !== item.savedJobPostingId
      setItems((current) => current.map((currentItem) => currentItem.localKey === item.localKey
        ? {
          ...currentItem,
          id: saved.id,
          jobPostingId: saved.jobPostingId ?? '',
          savedJobPostingId: saved.jobPostingId ?? '',
          question: saved.question,
          savedQuestion: saved.question,
          ...(postingChanged ? { answer: saved.answer, savedAnswer: saved.answer } : {}),
        }
        : currentItem))
      questionSaved = true
      setSavingId(null)
      setGeneratingId(item.localKey)
      const result = await request<SelfIntroductionAnswerResponse>('/v1/ai/generate-self-introduction-answer', {
        method: 'POST',
        body: JSON.stringify({ resumeId, questionId: saved.id }),
      })
      updateItem(item.localKey, 'answer', result.answer)
      setDraftCitations((current) => ({ ...current, [item.localKey]: result.citations }))
      setDraftProviders((current) => {
        const next = { ...current }
        if (result.diagnosticsVisible) {
          next[item.localKey] = {
            provider: result.provider,
            model: result.model,
            promptVersion: result.promptVersion,
            retrievedChunkCount: result.retrievedChunkCount,
          }
        } else {
          delete next[item.localKey]
        }
        return next
      })
    } catch (submissionError) {
      const detail = submissionError instanceof Error ? submissionError.message : '요청을 처리하지 못했습니다.'
      const errorCode = (submissionError as ApiError).code
      const reason = errorCode === 'RAG_CONTEXT_EMPTY'
        ? '질문과 관련성이 높은 Experience·Project 근거를 찾지 못했습니다. 데이터를 보완하거나 Career 인덱스를 갱신해주세요.'
        : errorCode === 'RAG_ANSWER_UNSUPPORTED'
          ? 'Career 근거는 검색했지만 질문에 답할 내용을 확인하지 못했습니다. 질문과 관련된 구체적인 역할·행동·결과를 Career에 추가해주세요.'
        : errorCode === 'RAG_CITATION_INVALID' || errorCode === 'RAG_FINDING_UNCITED' || errorCode === 'RAG_FINDING_INVALID'
          ? '자동 작성된 답변을 근거와 연결하지 못해 초안을 표시하지 않았습니다. 다시 시도하거나 검색된 Career 근거가 질문에 맞는지 확인해주세요.'
        : errorCode === 'AI_PROVIDER_INVALID_RESPONSE'
          ? '작성된 답변의 형식을 처리하지 못했습니다. 잠시 후 다시 시도해주세요.'
          : errorCode === 'AI_PROVIDER_INCOMPLETE_RESPONSE'
            ? '답변 생성이 길이 한도에 도달했습니다. 답변 길이를 줄인 뒤 다시 시도해주세요.'
          : errorCode === 'AI_PROVIDER_REQUEST_FAILED'
            ? '자동 작성 서비스가 요청을 처리하지 못했습니다. 연결 키, 모델 설정, 요청 한도를 확인해주세요.'
            : errorCode === 'AI_PROVIDER_UNAVAILABLE'
              ? '자동 작성 서비스에 연결하지 못했습니다. 네트워크와 Ollama 실행 상태를 확인한 뒤 다시 시도해주세요.'
              : detail
      setError(questionSaved
        ? `질문과 기업공고는 저장됐지만 자동 답변 생성에 실패했습니다. ${reason}`
        : `질문을 저장하지 못했습니다. (${detail})`)
    } finally {
      setGeneratingId(null)
      setSavingId(null)
    }
  }

  async function saveAnswer(item: ApplicationQuestionEditor) {
    if (!item.id) {
      setError('질문을 먼저 저장해주세요.')
      return
    }
    setSavingId(item.localKey)
    setError('')
    try {
      const saved = await request<ResumeApplicationQuestion>(`/v1/resumes/${resumeId}/application-questions/${item.id}`, {
        method: 'PUT',
        body: JSON.stringify({ jobPostingId: item.jobPostingId, question: item.savedQuestion, answer: item.answer }),
      })
      setItems((current) => current.map((currentItem) => currentItem.localKey === item.localKey
        ? {
          ...currentItem,
          jobPostingId: saved.jobPostingId ?? '',
          savedJobPostingId: saved.jobPostingId ?? '',
          savedQuestion: saved.question,
          answer: saved.answer,
          savedAnswer: saved.answer,
        }
        : currentItem))
      setDraftCitations((current) => {
        const next = { ...current }
        delete next[item.localKey]
        return next
      })
    } catch (submissionError) {
      setError(submissionError instanceof Error ? submissionError.message : '자기소개서 답변을 저장하지 못했습니다.')
    } finally {
      setSavingId(null)
    }
  }

  async function deleteQuestion(item: ApplicationQuestionEditor) {
    if (!item.id) {
      setItems((current) => current.filter((currentItem) => currentItem.localKey !== item.localKey))
      return
    }
    if (!window.confirm('이 기업 자기소개서 질문을 삭제할까요?')) return
    setDeletingId(item.localKey)
    setError('')
    try {
      await request<void>(`/v1/resumes/${resumeId}/application-questions/${item.id}`, { method: 'DELETE' })
      setItems((current) => current.filter((currentItem) => currentItem.localKey !== item.localKey))
      setDraftCitations((current) => {
        const next = { ...current }
        delete next[item.localKey]
        return next
      })
      setDraftProviders((current) => {
        const next = { ...current }
        delete next[item.localKey]
        return next
      })
    } catch (deletionError) {
      setError(deletionError instanceof Error ? deletionError.message : '질문을 삭제하지 못했습니다.')
    } finally {
      setDeletingId(null)
    }
  }

  return <section className="application-question-section">
    <div className="section-title"><div><span className="eyebrow">COMPANY APPLICATION</span><h2>기업별 자기소개서 질문</h2></div><span className="version-count">{items.length}개 항목</span></div>
    <p className="application-question-help">각 질문을 해당 기업공고와 연결하세요. 저장한 경력과 프로젝트에서 질문에 맞는 근거를 찾아 답변 초안을 자동으로 작성합니다. 내용을 검토·수정한 뒤 답변을 저장하세요.</p>
    {!isLoading && jobPostings.length === 0 && <p className="application-question-empty">연결할 기업공고가 없습니다. <Link to="/job-postings">채용공고 메뉴에서 먼저 공고를 등록하세요.</Link></p>}
    {error && <p className="form-error" role="alert">{error}</p>}
    {isLoading && <div className="list-state">질문을 불러오는 중입니다...</div>}
    {!isLoading && items.length === 0 && <p className="application-question-empty">아직 등록한 기업 질문이 없습니다. 아래에서 첫 질문을 추가해보세요.</p>}
    {!isLoading && <div className="application-question-list">{items.map((item, index) => <article className="application-question-item" key={item.localKey}>
      <div className="application-question-heading"><strong>질문 {index + 1}{item.id ? '' : ' · 새 항목'}</strong><button className="text-button danger-button" type="button" onClick={() => void deleteQuestion(item)} disabled={deletingId === item.localKey || savingId === item.localKey || generatingId === item.localKey}>{deletingId === item.localKey ? '삭제 중...' : '삭제'}</button></div>
      <label>연결할 기업공고<select value={item.jobPostingId} onChange={(event) => updateJobPosting(item.localKey, event.target.value)} disabled={savingId === item.localKey || generatingId === item.localKey}><option value="">기업공고 선택</option>{jobPostings.map((posting) => <option key={posting.id} value={posting.id}>{posting.companyName} · {posting.title}</option>)}</select></label>
      <label>기업 질문<textarea value={item.question} onChange={(event) => updateItem(item.localKey, 'question', event.target.value)} disabled={savingId === item.localKey || generatingId === item.localKey} maxLength={2000} rows={3} placeholder="예: 지원동기와 입사 후 이루고 싶은 목표를 작성해주세요." /></label>
      {item.id && (item.question.trim() !== item.savedQuestion || item.jobPostingId !== item.savedJobPostingId) && <small className="application-question-citations">질문이나 기업공고가 변경되었습니다. 저장하면 선택한 공고를 기준으로 새 답변을 작성합니다.</small>}
      <label>답변<textarea value={item.answer} onChange={(event) => updateItem(item.localKey, 'answer', event.target.value)} disabled={!item.id || savingId === item.localKey || generatingId === item.localKey} maxLength={100000} rows={8} placeholder={item.id ? '자동으로 만든 초안을 검토하거나 직접 작성해주세요.' : '질문을 저장하면 초안이 자동으로 작성됩니다.'} /></label>
      {draftProviders[item.localKey] && <small className="application-question-citations">자동 생성 정보: {draftProviders[item.localKey].provider} · {draftProviders[item.localKey].model} · prompt {draftProviders[item.localKey].promptVersion} · 근거 청크 {draftProviders[item.localKey].retrievedChunkCount}개</small>}
      {draftCitations[item.localKey]?.length > 0 && <details className="application-question-evidence"><summary>문장 인용 [C1]과 Career 근거 {draftCitations[item.localKey].length}개 대조하기</summary><p>답변의 각 [C#] 표시와 아래 원문을 대조해 사실이 맞는지 검토하세요. 인용 자체가 주장의 정확성을 보증하지는 않습니다.</p>{draftCitations[item.localKey].map((citation) => <div className="application-question-evidence-item" key={citation.citationId}><strong>{citation.citationId} · {citation.sourceType}</strong><pre>{citation.content}</pre></div>)}</details>}
      <div className="application-question-actions"><button className="secondary-button compact-button" type="button" onClick={() => void saveQuestionAndGenerate(item)} disabled={savingId === item.localKey || generatingId === item.localKey || jobPostings.length === 0}>{savingId === item.localKey ? '질문 저장 중...' : generatingId === item.localKey ? '답변 작성 중...' : '질문 저장 · 답변 자동 작성'}</button>{item.id && <button className="primary-button compact-button" type="button" onClick={() => void saveAnswer(item)} disabled={savingId === item.localKey || generatingId === item.localKey || item.question.trim() !== item.savedQuestion || item.jobPostingId !== item.savedJobPostingId}>{savingId === item.localKey ? '저장 중...' : '답변 저장'}</button>}</div>
    </article>)}</div>}
    {!isLoading && <div className="application-question-add"><button className="secondary-button compact-button" type="button" onClick={addQuestion}>+ 자기소개서 항목 추가</button></div>}
  </section>
}

const emptyJobPostingForm: JobPostingForm = { title: '', companyName: '', description: '', sourceUrl: '' }

function JobPostingPageContent({ user }: { user: User }) {
  const [jobPostings, setJobPostings] = useState<JobPosting[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [form, setForm] = useState<JobPostingForm>(emptyJobPostingForm)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [formError, setFormError] = useState('')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [deletingId, setDeletingId] = useState<string | null>(null)
  const [isImportOpen, setIsImportOpen] = useState(false)
  const [generatingDraftId, setGeneratingDraftId] = useState<string | null>(null)
  const [preflightPosting, setPreflightPosting] = useState<JobPosting | null>(null)
  const [draftState, setDraftState] = useState<{ jobPosting: JobPosting; draft: ResumeDraftResponse } | null>(null)
  const [isApprovingDraft, setIsApprovingDraft] = useState(false)
  const [exportState, setExportState] = useState<{ resumeName: string; version: ResumeVersion; companyName?: string; jobTitle?: string } | null>(null)

  async function loadJobPostings(targetPage = page) {
    setIsLoading(true)
    setLoadError('')
    try {
      const result = await request<PageResponse<JobPosting>>(`/v1/job-postings?page=${targetPage}&size=5`)
      setJobPostings(result.content)
      setPage(result.page)
      setTotalPages(result.totalPages)
      setTotalElements(result.totalElements)
    } catch (error) {
      setLoadError(error instanceof Error ? error.message : '채용공고 목록을 불러오지 못했습니다.')
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => { void loadJobPostings(0) }, [])

  function updateField(field: keyof JobPostingForm, value: string) {
    setForm((current) => ({ ...current, [field]: value }))
    setFieldErrors((current) => ({ ...current, [field]: '' }))
  }

  function startCreate() {
    setEditingId(null)
    setForm(emptyJobPostingForm)
    setFieldErrors({})
    setFormError('')
  }

  function startEdit(jobPosting: JobPosting) {
    setEditingId(jobPosting.id)
    setForm({ title: jobPosting.title, companyName: jobPosting.companyName, description: jobPosting.description, sourceUrl: jobPosting.sourceUrl ?? '' })
    setFieldErrors({})
    setFormError('')
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  function validateForm() {
    const errors: Record<string, string> = {}
    if (!form.title.trim()) errors.title = '채용공고 제목을 입력해주세요.'
    if (!form.companyName.trim()) errors.companyName = '회사명을 입력해주세요.'
    if (!form.description.trim()) errors.description = '채용공고 내용을 입력해주세요.'
    if (form.sourceUrl && !/^https?:\/\/.+/i.test(form.sourceUrl.trim())) errors.sourceUrl = 'http 또는 https URL을 입력해주세요.'
    setFieldErrors(errors)
    return Object.keys(errors).length === 0
  }

  async function submitJobPosting(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setFormError('')
    if (!validateForm()) return
    setIsSubmitting(true)
    const payload = { title: form.title.trim(), companyName: form.companyName.trim(), description: form.description.trim(), sourceUrl: form.sourceUrl.trim() || null }
    try {
      await request<JobPosting>(editingId ? `/v1/job-postings/${editingId}` : '/v1/job-postings', { method: editingId ? 'PUT' : 'POST', body: JSON.stringify(payload) })
      const nextPage = editingId ? page : 0
      startCreate()
      await loadJobPostings(nextPage)
    } catch (error) {
      const apiError = error as ApiError
      setFormError(apiError.message)
      setFieldErrors(apiError.fieldErrors ?? {})
    } finally {
      setIsSubmitting(false)
    }
  }

  async function deleteJobPosting(id: string) {
    if (!window.confirm('이 채용공고를 삭제할까요? 삭제된 공고는 목록에서 숨겨집니다.')) return
    setDeletingId(id)
    try {
      await request<void>(`/v1/job-postings/${id}`, { method: 'DELETE' })
      await loadJobPostings(page > 0 && jobPostings.length === 1 ? page - 1 : page)
    } catch (error) {
      setLoadError(error instanceof Error ? error.message : '채용공고를 삭제하지 못했습니다.')
    } finally {
      setDeletingId(null)
    }
  }

  async function generateResumeDraft(jobPosting: JobPosting) {
    setGeneratingDraftId(jobPosting.id)
    setFormError('')
    try {
      const draft = await request<ResumeDraftResponse>('/v1/ai/generate-resume-draft', {
        method: 'POST',
        body: JSON.stringify({ jobPostingId: jobPosting.id }),
      })
      setDraftState({ jobPosting, draft })
    } catch (error) {
      setFormError(error instanceof Error ? error.message : '채용공고 기준 이력서 초안을 생성하지 못했습니다.')
    } finally {
      setGeneratingDraftId(null)
    }
  }

  async function approveGeneratedDraft() {
    if (!draftState) return
    setIsApprovingDraft(true)
    setFormError('')
    try {
      const resumeName = jobKoreaResumeTitle(draftState.jobPosting.companyName, draftState.jobPosting.title)
      const resume = await request<Resume>(`/v1/ai/resume-drafts/${draftState.draft.id}/approve-new`, {
        method: 'POST',
        body: JSON.stringify({ resumeName }),
      })
      const versions = await request<PageResponse<ResumeVersion>>(`/v1/resumes/${resume.id}/versions?page=0&size=1`)
      const version = versions.content[0]
      if (!version) throw new Error('생성된 ResumeVersion을 찾지 못했습니다.')
      setDraftState(null)
       setExportState({ resumeName: resume.name, version, companyName: draftState.jobPosting.companyName, jobTitle: draftState.jobPosting.title })
    } catch (error) {
      setFormError(error instanceof Error ? error.message : 'ResumeVersion을 생성하지 못했습니다.')
    } finally {
      setIsApprovingDraft(false)
    }
  }

  async function persistGeneratedResumeSkills(skills: string[]) {
    if (!exportState) throw new Error('스킬을 저장할 이력서를 찾지 못했습니다.')
    const currentEditor = resumeVersionEditorFromContent(exportState.version.content)
    const currentSkills = mergeJobKoreaSkills(splitExplicitSkills(currentEditor.skills))
    const nextSkills = mergeJobKoreaSkills(skills)
    const unchanged = currentSkills.length === nextSkills.length
      && currentSkills.every((skill, index) => skillKey(skill) === skillKey(nextSkills[index]))
    if (unchanged) return exportState.version

    const content = resumeContentWithSkills(exportState.version.content, nextSkills)
    const savedVersion = await request<ResumeVersion>(`/v1/resumes/${exportState.version.resumeId}/versions`, {
      method: 'POST',
      body: JSON.stringify({ content }),
    })
    setExportState((current) => current ? { ...current, version: savedVersion } : current)
    return savedVersion
  }

  return <>
    <header className="page-heading"><div><span className="eyebrow">OPPORTUNITIES</span><h1>채용공고</h1><p>관심 있는 공고를 저장하면 내 경력과 비교하고 맞춤형 이력서 초안을 만들 수 있습니다.</p></div><div className="heading-actions"><button className="secondary-button compact-button" onClick={() => setIsImportOpen(true)}>URL로 빠르게 입력</button><button className="primary-button compact-button" onClick={startCreate}>+ 공고 추가</button></div></header>
    <section className="experience-form-card">
      <div className="section-title"><div><span className="eyebrow">{editingId ? 'EDIT JOB POSTING' : 'NEW JOB POSTING'}</span><h2>{editingId ? '채용공고 수정' : '새 채용공고 기록'}</h2><p className="form-helper">URL을 가져오거나 공고 원문을 붙여 넣으면 필요한 내용을 알아서 정리합니다.</p></div>{editingId && <button className="text-button" onClick={startCreate}>취소</button>}</div>
      <form className="experience-form" onSubmit={submitJobPosting}>
        <FormField label="공고 제목" value={form.title} onChange={(value) => updateField('title', value)} error={fieldErrors.title} placeholder="예: Senior Backend Engineer" />
        <FormField label="회사명" value={form.companyName} onChange={(value) => updateField('companyName', value)} error={fieldErrors.companyName} placeholder="예: Acme Inc." />
        <FormField label="원문 URL (선택)" value={form.sourceUrl} onChange={(value) => updateField('sourceUrl', value)} error={fieldErrors.sourceUrl} placeholder="https://jobs.example.com/..." />
        <label className="wide-field">채용공고 내용<textarea value={form.description} onChange={(event) => updateField('description', event.target.value)} placeholder="채용공고 원문을 붙여 넣어주세요." maxLength={100000} rows={8} />{fieldErrors.description && <small className="field-error">{fieldErrors.description}</small>}</label>
        {formError && <p className="form-error wide-field" role="alert">{formError}</p>}
        <div className="form-actions wide-field"><button className="primary-button compact-button" disabled={isSubmitting} type="submit">{isSubmitting ? '저장 중...' : editingId ? '변경 저장' : '공고 저장'}</button></div>
      </form>
    </section>
    <section className="experience-list-section"><div className="list-heading"><div><span className="eyebrow">SAVED OPPORTUNITIES</span><h2>{totalElements}개의 채용공고</h2></div><button className="text-button" onClick={() => void loadJobPostings(page)}>새로고침</button></div>
      {isLoading && <div className="list-state">채용공고 목록을 불러오는 중입니다...</div>}
      {!isLoading && loadError && <div className="list-state error-state" role="alert">{loadError}<button className="text-button" onClick={() => void loadJobPostings(page)}>다시 시도</button></div>}
      {!isLoading && !loadError && jobPostings.length === 0 && <div className="list-state">아직 저장된 채용공고가 없습니다.</div>}
      {!isLoading && !loadError && jobPostings.length > 0 && <div className="experience-list">{jobPostings.map((jobPosting) => <article className="experience-item job-posting-item" key={jobPosting.id}><div className="job-source-label"><span className="job-dot" /> 저장됨<br /><small>{formatKoreanDateTime(jobPosting.updatedAt)}</small></div><div className="experience-main"><h3>{jobPosting.title}</h3><strong>{jobPosting.companyName}</strong><p>{jobPosting.description}</p>{jobPosting.sourceUrl && <a className="repo-link" href={jobPosting.sourceUrl} target="_blank" rel="noreferrer">원문 보기 ↗</a>}</div><div className="item-actions"><button className="primary-button compact-button" onClick={() => setPreflightPosting(jobPosting)} disabled={generatingDraftId === jobPosting.id}>{generatingDraftId === jobPosting.id ? '생성 중...' : '커리어 점검 후 이력서 생성'}</button><button className="text-button" onClick={() => startEdit(jobPosting)}>수정</button><button className="text-button danger-button" onClick={() => void deleteJobPosting(jobPosting.id)} disabled={deletingId === jobPosting.id}>{deletingId === jobPosting.id ? '삭제 중' : '삭제'}</button></div></article>)}</div>}
      {!isLoading && !loadError && totalPages > 1 && <div className="pagination"><button className="text-button" disabled={page === 0} onClick={() => void loadJobPostings(page - 1)}>← 이전</button><span>{page + 1} / {totalPages}</span><button className="text-button" disabled={page >= totalPages - 1} onClick={() => void loadJobPostings(page + 1)}>다음 →</button></div>}
    </section>
    {isImportOpen && <JobPostingUrlImportModal onClose={() => setIsImportOpen(false)} onApply={(preview) => {
      setEditingId(null)
      setForm({ title: preview.title, companyName: preview.companyName, description: preview.description, sourceUrl: preview.sourceUrl })
      setFieldErrors({})
      setFormError(preview.warnings.length > 0 ? `가져온 값 중 확인이 필요한 항목: ${preview.warnings.join(' ')}` : '')
      setIsImportOpen(false)
      window.scrollTo({ top: 0, behavior: 'smooth' })
    }} />}
    {preflightPosting && <CareerDataReviewModal title="이력서 생성 전 Career 점검" continueLabel="확인하고 이력서 생성" reindexBeforeContinue onClose={() => setPreflightPosting(null)} onContinue={() => { const jobPosting = preflightPosting; setPreflightPosting(null); void generateResumeDraft(jobPosting) }} />}
    {draftState && <ResumeDraftReviewModal jobPosting={draftState.jobPosting} draft={draftState.draft} isApproving={isApprovingDraft} onClose={() => setDraftState(null)} onApprove={() => void approveGeneratedDraft()} />}
    {exportState && <JobKoreaExportModal resumeName={exportState.resumeName} version={exportState.version} companyName={exportState.companyName} jobTitle={exportState.jobTitle} displayName={user.displayName} email={user.email} onPersistSkills={persistGeneratedResumeSkills} onClose={() => setExportState(null)} />}
  </>
}

function FormField({ label, value, onChange, error, placeholder, type = 'text', disabled = false }: { label: string; value: string; onChange: (value: string) => void; error?: string; placeholder?: string; type?: string; disabled?: boolean }) {
  return <label>{label}<input type={type} value={value} onChange={(event) => onChange(event.target.value)} placeholder={placeholder} disabled={disabled} />{error && <small className="field-error">{error}</small>}</label>
}

function StatCard({ label, value, action, to }: { label: string; value: string; action: string; to: string }) {
  return <article className="stat-card"><div><span className="eyebrow">{label}</span><strong>{value}</strong></div><Link className="stat-card-action" to={to}>{action} <span>+</span></Link></article>
}

export default App
