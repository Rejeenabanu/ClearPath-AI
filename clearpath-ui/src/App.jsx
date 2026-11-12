import { useState } from 'react'
import './App.css'

const DEFAULT_ORDER_ID = '2C795789K60919927'

async function apiRequest(url, options = {}) {
  const response = await fetch(url, options)
  const text = await response.text()

  let data = {}
  try {
    data = text ? JSON.parse(text) : {}
  } catch {
    data = { message: text }
  }

  if (!response.ok) {
    throw new Error(data.message || data.error || `HTTP ${response.status}`)
  }

  return data
}

function formatDate(value) {
  if (!value) return 'Not available'

  return new Date(value).toLocaleString()
}

function StatusBadge({ status }) {
  const normalized = (status || 'UNKNOWN').toUpperCase()

  let color = 'neutral'

  if (['COMPLETED', 'MATCH', 'APPROVED'].includes(normalized)) {
    color = 'success'
  } else if (['MISMATCH', 'NOT_RECEIVED', 'FAILED'].includes(normalized)) {
    color = 'danger'
  } else if (['PENDING', 'REVIEW'].includes(normalized)) {
    color = 'warning'
  }

  return (
      <span className={`status-badge ${color}`}>
      {normalized.replaceAll('_', ' ')}
    </span>
  )
}

function App() {
  const [orderId, setOrderId] = useState(DEFAULT_ORDER_ID)
  const [timeline, setTimeline] = useState(null)
  const [history, setHistory] = useState([])
  const [aiResult, setAiResult] = useState(null)

  const [loading, setLoading] = useState(false)
  const [aiLoading, setAiLoading] = useState(false)
  const [error, setError] = useState('')

  // Bulk merchant CSV import state
  const [csvFile, setCsvFile] = useState(null)
  const [csvLoading, setCsvLoading] = useState(false)
  const [csvResult, setCsvResult] = useState(null)

  async function loadEvidence() {
    const id = orderId.trim()

    if (!/^[A-Z0-9]+$/.test(id)) {
      setError('Please enter a valid PayPal order ID.')
      return
    }

    setLoading(true)
    setError('')
    setAiResult(null)

    try {
      const encodedId = encodeURIComponent(id)

      const [timelineData, historyData] = await Promise.all([
        apiRequest(`/api/clearpath/transactions/${encodedId}/timeline`),
        apiRequest(`/api/clearpath/investigations/${encodedId}/ai/history`)
      ])

      setTimeline(timelineData)
      setHistory(historyData)

      // Restore the most recently saved AI report.
      if (historyData.length > 0) {
        const latest = historyData[0]

        setAiResult({
          model: latest.model,
          deterministicInvestigation: {
            orderId: latest.orderId,
            result: latest.result,
            summary: latest.executiveSummary
          },
          aiAnalysis: JSON.parse(latest.aiAnalysisJson)
        })
      }
    } catch (err) {
      setError(err.message || 'Unable to load transaction evidence.')
      setTimeline(null)
      setHistory([])
      setAiResult(null)
    } finally {
      setLoading(false)
    }
  }

  async function uploadMerchantCsv() {
    if (!csvFile) {
      setError('Please choose a CSV file first.')
      return
    }

    setCsvLoading(true)
    setError('')
    setCsvResult(null)

    try {
      const formData = new FormData()
      formData.append('file', csvFile)

      // Do not set Content-Type manually; the browser adds the multipart boundary.
      const result = await apiRequest(
          '/api/clearpath/merchant-reports/bulk',
          { method: 'POST', body: formData }
      )

      setCsvResult(result)
      setCsvFile(null)
    } catch (err) {
      setError(err.message || 'CSV import failed.')
    } finally {
      setCsvLoading(false)
    }
  }

  async function runAiInvestigation() {
    const id = orderId.trim()

    if (!/^[A-Z0-9]+$/.test(id)) {
      setError('Please enter a valid PayPal order ID.')
      return
    }

    setAiLoading(true)
    setError('')

    try {
      const encodedId = encodeURIComponent(id)

      const result = await apiRequest(
          `/api/clearpath/investigations/${encodedId}/ai`,
          { method: 'POST' }
      )

      setAiResult(result)

      // Refresh saved investigation history.
      const reports = await apiRequest(
          `/api/clearpath/investigations/${encodedId}/ai/history`
      )

      setHistory(reports)
    } catch (err) {
      setError(err.message || 'AI investigation failed.')
    } finally {
      setAiLoading(false)
    }
  }

  const paypalCapture = aiResult?.deterministicInvestigation
      ?.paypalEvidence

  const merchantEvidence = aiResult?.deterministicInvestigation
      ?.merchantEvidence

  const analysis = aiResult?.aiAnalysis

  return (
      <div className="app-shell">

        <aside className="sidebar">
          <div className="brand">
            <div className="brand-icon">C</div>
            <div>
              <h2>ClearPath</h2>
              <span>FINANCIAL INTELLIGENCE</span>
            </div>
          </div>

          <div className="nav-label">WORKSPACE</div>

          <div className="nav-item active">
            <span>▦</span> Overview
          </div>

          <div className="nav-item">
            <span>⌕</span> Investigations
          </div>

          <div className="nav-item">
            <span>⇄</span> Transactions
          </div>

          <div className="nav-item">
            <span>◷</span> Evidence Timeline
          </div>

          <div className="sidebar-bottom">
            <div className="connection-dot" />
            <div>
              <strong>Sandbox environment</strong>
              <small>PayPal connection active</small>
            </div>
          </div>
        </aside>

        <main className="main-content">

          <header className="topbar">
            <div>
              <span className="breadcrumb">Workspace / Overview</span>
              <h1>Financial Investigation</h1>
              <p>Understand transactions. Detect discrepancies. Resolve faster.</p>
            </div>

            <div className="topbar-status">
              <span className="live-dot" />
              Systems operational
            </div>
          </header>

          <section className="welcome-section">
            <div>
              <span className="eyebrow">AI-POWERED PAYMENT INTELLIGENCE</span>
              <h2>Welcome to ClearPath AI</h2>
              <p>
                Investigate payment discrepancies using verified evidence
                from PayPal and merchant systems.
              </p>
            </div>

            <div className="welcome-symbol">✦</div>
          </section>

          <section className="metrics-grid">

            <div className="metric-card">
              <span className="metric-icon blue">⇄</span>
              <p>Events for selected order</p>
              <h2>{timeline?.eventCount ?? '—'}</h2>
              <span className="metric-caption">Verified PayPal events</span>
            </div>

            <div className="metric-card">
              <span className="metric-icon purple">✧</span>
              <p>Saved investigations</p>
              <h2>{history.length}</h2>
              <span className="metric-caption">For selected order</span>
            </div>

            <div className="metric-card">
              <span className="metric-icon orange">⚑</span>
              <p>Latest result</p>
              <div className="metric-status">
                {aiResult
                    ? <StatusBadge status={
                      aiResult.deterministicInvestigation.result
                    } />
                    : '—'}
              </div>
              <span className="metric-caption">Deterministic assessment</span>
            </div>

          </section>

          <section className="panel search-panel">
            <div className="panel-heading">
              <div>
                <span className="eyebrow">TRANSACTION LOOKUP</span>
                <h3>Investigate a payment</h3>
                <p>Enter a PayPal order ID to retrieve its evidence.</p>
              </div>
            </div>

            <div className="search-form">
              <div className="input-wrapper">
                <span>⌕</span>
                <input
                    value={orderId}
                    onChange={(e) => setOrderId(e.target.value)}
                    placeholder="Enter PayPal order ID"
                />
              </div>

              <button
                  className="primary-button"
                  onClick={loadEvidence}
                  disabled={loading || aiLoading}
              >
                {loading ? 'Loading...' : 'Load evidence'}
              </button>

              <button
                  className="ai-button"
                  onClick={runAiInvestigation}
                  disabled={loading || aiLoading}
              >
                {aiLoading ? 'Investigating...' : '✧ Run AI investigation'}
              </button>
            </div>

            {error && (
                <div className="error-message">
                  {error}
                </div>
            )}
          </section>

          <section className="panel csv-upload-panel">
            <div className="panel-heading">
              <div>
                <span className="eyebrow">BULK MERCHANT EVIDENCE</span>
                <h3>Import merchant reports</h3>
                <p>Upload a CSV file to validate and save multiple merchant payment records.</p>
              </div>
            </div>

            <div className="csv-upload-form">
              <input
                  type="file"
                  accept=".csv,text/csv"
                  onChange={(event) => {
                    setCsvFile(event.target.files?.[0] || null)
                    setCsvResult(null)
                    setError('')
                  }}
              />
              <button
                  className="primary-button"
                  onClick={uploadMerchantCsv}
                  disabled={!csvFile || csvLoading || loading || aiLoading}
              >
                {csvLoading ? 'Importing...' : 'Upload CSV'}
              </button>
            </div>

            <p className="csv-hint">
              Required columns: paypalOrderId, merchantOrderId, paymentStatus, amount, currency.
              Accepted statuses: COMPLETED, NOT_RECEIVED, PENDING, FAILED.
            </p>

            {csvResult && (
                <div className="csv-import-result">
                  <div className="csv-import-stats">
                    <div className="csv-stat">
                      <span>Total rows</span>
                      <strong>{csvResult.totalRows}</strong>
                    </div>
                    <div className="csv-stat">
                      <span>Imported</span>
                      <strong>{csvResult.importedRows}</strong>
                    </div>
                    <div className="csv-stat">
                      <span>Failed</span>
                      <strong>{csvResult.failedRows}</strong>
                    </div>
                  </div>

                  <div className="csv-results-list">
                    {csvResult.results?.map((row) => (
                        <div className="csv-result-row" key={row.rowNumber}>
                          <div className="csv-result-main">
                            <strong>Row {row.rowNumber}</strong>
                            {row.paypalOrderId && <small>{row.paypalOrderId}</small>}
                          </div>
                          <StatusBadge status={row.status} />
                          <span className="csv-result-message">{row.message}</span>
                        </div>
                    ))}
                  </div>

                  <p className="csv-next-step">
                    Import saves merchant reports. To compare one with PayPal evidence, enter its PayPal order ID above, select Load evidence, then Run AI investigation.
                  </p>
                </div>
            )}
          </section>

          {timeline && (
              <>
                <section className="content-grid">

                  <div className="panel">
                    <div className="panel-heading">
                      <div>
                        <span className="eyebrow">PAYMENT EVIDENCE</span>
                        <h3>Transaction overview</h3>
                      </div>
                    </div>

                    <div className="evidence-row">
                      <div>
                        <span>PayPal order ID</span>
                        <strong>{timeline.orderId}</strong>
                      </div>
                    </div>

                    <div className="evidence-row">
                      <div>
                        <span>PayPal capture status</span>
                        <strong>
                          {paypalCapture?.captureStatus || 'See timeline'}
                        </strong>
                      </div>
                      {paypalCapture && (
                          <StatusBadge status={paypalCapture.captureStatus} />
                      )}
                    </div>

                    <div className="evidence-row">
                      <div>
                        <span>Merchant payment status</span>
                        <strong>
                          {merchantEvidence?.paymentStatus || 'Not loaded'}
                        </strong>
                      </div>
                      {merchantEvidence && (
                          <StatusBadge status={merchantEvidence.paymentStatus} />
                      )}
                    </div>

                    <div className="evidence-row">
                      <div>
                        <span>Captured amount</span>
                        <strong>
                          {paypalCapture?.amount != null
                              ? `${paypalCapture.amount} ${paypalCapture.currency}`
                              : 'Not available'}
                        </strong>
                      </div>
                    </div>

                    {merchantEvidence && (
                        <div className="evidence-row">
                          <div>
                            <span>Merchant order reference</span>
                            <strong>{merchantEvidence.merchantOrderId}</strong>
                          </div>
                        </div>
                    )}
                  </div>

                  <div className="panel">
                    <div className="panel-heading">
                      <div>
                        <span className="eyebrow">EVENT HISTORY</span>
                        <h3>Payment timeline</h3>
                      </div>
                      <span className="event-count">
                    {timeline.eventCount} events
                  </span>
                    </div>

                    <div className="timeline">
                      {timeline.events.map((event) => (
                          <div className="timeline-event" key={event.eventId}>
                            <div className="timeline-marker" />

                            <div className="timeline-details">
                              <strong>
                                {event.eventType.replaceAll('.', ' ')}
                              </strong>
                              <span>
                          {event.resourceStatus || 'Status unavailable'}
                        </span>
                              <small>
                                {formatDate(event.paypalEventTime)}
                              </small>
                            </div>

                            <StatusBadge status={event.resourceStatus} />
                          </div>
                      ))}
                    </div>
                  </div>

                </section>
              </>
          )}

          {aiResult && (
              <section className="panel ai-report">

                <div className="report-header">
                  <div>
                    <span className="eyebrow">GEMINI AI ANALYSIS</span>
                    <h3>Investigation report</h3>
                    <p>
                      Generated by {aiResult.model}
                    </p>
                  </div>
                  <StatusBadge
                      status={aiResult.deterministicInvestigation.result}
                  />
                </div>

                <div className="summary-box">
                  <span>EXECUTIVE SUMMARY</span>
                  <p>{analysis?.executiveSummary}</p>
                </div>

                <div className="analysis-grid">

                  <div className="analysis-block">
                    <h4>◈ Plausible causes</h4>
                    <ul>
                      {analysis?.plausibleCauses?.map((item, index) => (
                          <li key={index}>{item}</li>
                      ))}
                    </ul>
                  </div>

                  <div className="analysis-block">
                    <h4>⌕ Missing evidence</h4>
                    <ul>
                      {analysis?.missingEvidence?.map((item, index) => (
                          <li key={index}>{item}</li>
                      ))}
                    </ul>
                  </div>

                  <div className="analysis-block">
                    <h4>✓ Recommended actions</h4>
                    <ul>
                      {analysis?.recommendedActions?.map((item, index) => (
                          <li key={index}>{item}</li>
                      ))}
                    </ul>
                  </div>

                  <div className="analysis-block">
                    <h4>♧ Customer explanation</h4>
                    <p>{analysis?.customerExplanation}</p>
                  </div>

                </div>

                <div className="limitations">
                  <strong>Evidence limitations</strong>
                  <p>{analysis?.evidenceLimitations}</p>
                </div>

                <div className="report-footer">
                  <span>Response ID: {aiResult.responseId || 'Saved report'}</span>
                  <span>AI findings are recommendations, not confirmed root causes.</span>
                </div>

              </section>
          )}

          {history.length > 0 && (
              <section className="panel history-panel">
                <div className="panel-heading">
                  <div>
                    <span className="eyebrow">AUDIT HISTORY</span>
                    <h3>Previous investigations</h3>
                  </div>
                </div>

                {history.map((report) => (
                    <div className="history-row" key={report.reportId}>
                      <div>
                        <strong>Investigation #{report.reportId}</strong>
                        <small>
                          {formatDate(report.createdAt)}
                        </small>
                      </div>

                      <StatusBadge status={report.result} />

                      <span>{report.model}</span>
                    </div>
                ))}
              </section>
          )}

          <footer className="footer">
            <span>ClearPath AI © 2026</span>
            <span>Powered by PayPal Sandbox & Gemini AI</span>
            <span>Hackathon prototype · Synthetic data only</span>
          </footer>

        </main>
      </div>
  )
}

export default App
