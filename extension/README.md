# Career Note Chrome Extension Prototype

This is the JobKorea-first browser application-assistance prototype. The existing service remains the
source of truth for Career data, retrieval evidence, AI generation, provenance, and export. The JobKorea
posting currently open in the tab is temporary request context and is not saved as a JobPosting.

## Current scope

- Manifest V3 extension with `activeTab`, `scripting`, and `storage` permissions plus narrowly scoped
  JobKorea, local-service, and production-service host access required for the automatic page panel.
- Recognizes JobKorea pages and shows a ChannelTalk-style circular Career Note launcher fixed above the
  lower-right JobKorea chatbot safe area. Clicking it opens the panel, which stays within the viewport,
  keeps its header and close button visible, and
  scans visible `input`, `textarea`, `select`, and `contenteditable` fields in the current page.
- Displays a human-readable purpose for each control: answer field, direct-input field, search/filter,
  or select list. It also shows required state and maximum length without exposing raw values such as
  `UNKNOWN`, `text`, or `선택` as the primary explanation.
- Connects through a one-time code issued by the logged-in web service; the extension stores only a
  short-lived access token in `chrome.storage.session` and never receives a refresh token.
- Extracts the company, title, and description from the current JobKorea posting, preferring public
  `JobPosting` JSON-LD and falling back to visible page content.
- In local/debug mode, shows the extraction source, structured/page character counts, the captured
  character count, and an expandable full-text preview so development can verify the posting. The
  production service hides these diagnostic details and the full-text preview from end users.
- Checks whether the connected account has Experience/Project records and indexed retrieval evidence,
  and guides the user to Career management when preparation is incomplete.
- Generates a structured review-only resume draft from the current unsaved posting, including headline,
  summary, self-introduction topics, career/project bullets, skills, and source citations. After the user
  reviews it, **검토 완료 · JobKorea 파일 준비** explicitly saves a new ResumeVersion and opens the
  service's JobKorea file export workspace. The posting itself is still not saved automatically.
- Generates a review-only answer proposal for the current unsaved posting, with career-source citations
  and a max-length-aware suggested value. It does not create a Resume, ResumeVersion, or JobPosting.
- Fills only the explicitly approved answer into the currently selected visible text field; it never
  copies generated content, selects an upload file, submits forms, or reads passwords/cookies.

## Local verification

1. Open a JobKorea posting or application page.
2. Open `chrome://extensions`, enable Developer mode, and choose **Load unpacked**.
3. Select the `extension` directory. After code changes, click the extension's **Reload** button.
4. Refresh the JobKorea page. The **Career Note** button should appear in the bottom-right corner.
5. The default service address is `https://career-note-web-sg.onrender.com`. For local Docker testing,
   replace it with `http://localhost:5173`.
6. Choose **연결 코드 발급**, sign in to the service tab if requested, and issue the one-time code.
7. Enter the code in the bottom-right panel and choose **확장 프로그램 연결**.
8. Confirm that **지원 답변 준비** shows Experience, Project, and indexed-evidence counts. Career Note
   automatically updates the search data after saving or deleting career data. If an existing account is
   not ready, choose **맞춤 검색 데이터 업데이트하러 가기**; the service opens Career and focuses the
   update button. After it finishes, choose **준비 상태 새로고침** in the extension.
9. Confirm that the current company and posting title are detected. In local/debug mode, check
   **공고 읽기 확인됨**, compare the structured/page character counts, and open **읽은 공고 원문 전체 보기**
   to verify the captured content. The production service intentionally hides these diagnostic details.
10. Choose **이 공고로 이력서 초안 생성** and review every generated section and Career citation. Then
   choose **검토 완료 · JobKorea 파일 준비**. This explicitly saves the reviewed draft as a new
   ResumeVersion and opens the service's Resume Workspace with the JobKorea export review flow.
11. In the service export screen, enter missing personal information, education, work conditions, and
   other JobKorea fields. Review the preview and download the PDF. Open the JobKorea upload page from
   the service and select the downloaded PDF there.
12. In the field scan, select a long-text
   answer field such as 지원동기 or 경력기술, then review the suggested question. If the field is shown as
   지원 질문 확인 필요, use the optional **회사 홈페이지 지원 질문 답변** section only when you are
   applying on a company homepage. It is not required for JobKorea. Do not select JOB 검색 or a 선택
   목록; those are navigation controls and are intentionally excluded.
13. For a homepage question, enter the exact question, generate the answer, and review the cited Career
   evidence before choosing **선택 필드에 반영**.
14. Submit only through the target site after reviewing every filled value.

The panel is injected only on `jobkorea.co.kr` and its subdomains. Semantic roles are heuristics documented
in [field-contract.md](field-contract.md), not private JobKorea selectors. The one-time code exchange is
intentionally initiated by the existing web service, and final submission remains manual on the target site.
Clicking the Chrome toolbar action toggles the same in-page panel; there is no separate ResumeVersion or
saved-JobPosting selection popup.

The backend permits extension API calls from Chrome extension origins and the local web service origins
`http://localhost:5173` and `http://127.0.0.1:5173`. The Render backend should keep
`EXTENSION_ALLOWED_ORIGIN_PATTERNS` configured with `chrome-extension://*`; the extension's production
requests go through `https://career-note-web-sg.onrender.com` and its Nginx `/api` proxy.
