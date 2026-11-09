export type AnalyticsConsent = 'granted' | 'denied'

type RuntimeConfig = {
  gaMeasurementId?: string
}

type AnalyticsWindow = Window & {
  __CAREER_NOTE_CONFIG__?: RuntimeConfig
  dataLayer?: unknown[][]
  gtag?: (...args: unknown[]) => void
}

const consentStorageKey = 'career-note.analytics-consent'
const analyticsWindow = window as AnalyticsWindow
const runtimeMeasurementId = analyticsWindow.__CAREER_NOTE_CONFIG__?.gaMeasurementId
const buildMeasurementId = import.meta.env.VITE_GA_MEASUREMENT_ID
const measurementId = String(runtimeMeasurementId || buildMeasurementId || '').trim()
const validMeasurementId = /^G-[A-Z0-9]+$/i.test(measurementId) ? measurementId : ''
let initialized = false
let lastTrackedPath = ''

function command(...args: unknown[]) {
  analyticsWindow.dataLayer = analyticsWindow.dataLayer || []
  analyticsWindow.dataLayer.push(args)
}

export function isAnalyticsConfigured() {
  return Boolean(validMeasurementId)
}

export function readAnalyticsConsent(): AnalyticsConsent | null {
  const value = localStorage.getItem(consentStorageKey)
  return value === 'granted' || value === 'denied' ? value : null
}

export function saveAnalyticsConsent(consent: AnalyticsConsent) {
  localStorage.setItem(consentStorageKey, consent)
  if (initialized) {
    command('consent', 'update', {
      analytics_storage: consent,
      ad_storage: 'denied',
      ad_user_data: 'denied',
      ad_personalization: 'denied',
    })
  }
  if (consent === 'denied') {
    lastTrackedPath = ''
    document.cookie.split(';').forEach((cookie) => {
      const name = cookie.split('=')[0]?.trim()
      if (name?.startsWith('_ga')) {
        document.cookie = `${name}=; Max-Age=0; Path=/; SameSite=Lax`
      }
    })
  }
}

export function initializeAnalytics() {
  if (!validMeasurementId || readAnalyticsConsent() !== 'granted') return false

  if (initialized) {
    command('consent', 'update', {
      analytics_storage: 'granted',
      ad_storage: 'denied',
      ad_user_data: 'denied',
      ad_personalization: 'denied',
    })
    return true
  }

  command('consent', 'default', {
    analytics_storage: 'granted',
    ad_storage: 'denied',
    ad_user_data: 'denied',
    ad_personalization: 'denied',
  })

  analyticsWindow.gtag = (...args: unknown[]) => command(...args)
  command('js', new Date())
  command('config', validMeasurementId, {
    send_page_view: false,
    allow_google_signals: false,
    allow_ad_personalization_signals: false,
  })

  const script = document.createElement('script')
  script.async = true
  script.src = `https://www.googletagmanager.com/gtag/js?id=${encodeURIComponent(validMeasurementId)}`
  script.dataset.careerNoteAnalytics = 'true'
  document.head.append(script)
  initialized = true
  return true
}

export function trackPageView(pathname: string) {
  if (!initializeAnalytics() || pathname === lastTrackedPath) return
  lastTrackedPath = pathname
  command('event', 'page_view', {
    page_title: document.title,
    page_location: `${window.location.origin}${pathname}`,
    page_path: pathname,
    send_to: validMeasurementId,
  })
}
