# JobKorea-first field contract

The extension maps only visible controls and uses semantic hints for review. The scan result is a
human-readable guide, not a promise that every visible control is an application question. Search boxes and
select lists are intentionally shown as excluded controls so that users understand why they cannot be used
to generate or fill an answer.

These roles are labels for user review, not private JobKorea selectors:

| Role | Korean/English hints | Supported proposal input |
| --- | --- | --- |
| `NAME` | 이름, 성명, name, 지원자명 | No generated answer by default |
| `EMAIL` | 이메일, email, mail | No generated answer by default |
| `PHONE` | 휴대폰, 전화, 연락처, phone, mobile, tel | No generated answer by default |
| `PORTFOLIO` | 포트폴리오, portfolio, GitHub, GitLab, 링크, URL | Manual review only |
| `CAREER_DESCRIPTION` | 경력기술, 경력사항, career, experience | Grounded proposal eligible |
| `SELF_INTRODUCTION` | 자기소개, 지원동기, 입사 후, 성장과정, motivation, introduction | Grounded proposal eligible |
| `UNKNOWN` | No matching semantic hint | Select only if it is a text answer field; enter the exact application question manually |

## What to enter

- `SELF_INTRODUCTION` or `CAREER_DESCRIPTION`: select the answer field, check the suggested question, and
  edit it to match the exact question shown on the JobKorea page.
- `UNKNOWN`: do not use the field name as the answer prompt unless it is really the question. For a
  company-homepage application only, enter the complete question into **홈페이지 지원 질문**. It is not
  needed for the JobKorea resume draft flow.
- `SEARCH_FILTER` and `SELECT_OPTION`: these are page navigation controls such as `JOB 검색` or `선택`.
  They are not answer fields and are excluded from generation and filling.
- `NAME`, `EMAIL`, `PHONE`, and `PORTFOLIO`: enter these values directly. They are not generated from career
  evidence by the extension.

The mapping is intentionally heuristic. A page re-render can change the visible control order, so the
extension requires a fresh scan before an approved fill. File, select, checkbox, radio, password, hidden, and
submit controls are never filled by the proposal action. Final submission remains manual on JobKorea.
