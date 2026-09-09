# Changelog

All notable changes to this project are documented in this file.

Before creating a release tag, add a section whose heading matches the tag name.
For example, tag `v1.2.3` must have a `## [v1.2.3]` section.

## [Unreleased]

### Added
- 서버 GA 이벤트 `query_response_complete`에 RAG 실행 경로(`execution_path`), 생성·선택 모델 사용 여부(`is_model_used`), 임베딩 사용 여부(`is_embedding_used`)를 추가했습니다. 해당 백엔드 버전 배포 후 n8n이 전달한 정보를 수집하며, 클라이언트 API/WebSocket 응답에 추가되는 필드는 아닙니다.
- 같은 이벤트에서 fallback 여부·사유(`is_fallback`, `used_fallback_path`, `fallback_reason`), 모델·RAG 버전(`model_name`, `rag_version`), 대화 이력 존재 여부(`has_history`), 질문 길이 구간(`query_length_bucket`)을 함께 분석할 수 있습니다.
- `is_model_used=true`는 생성·선택 모델 노드가 실행됐다는 의미이며 호출 성공이나 답변 정확도를 보장하지 않습니다. 오류 여부는 `is_error`, 임베딩 사용 여부는 `is_embedding_used`로 구분해야 합니다.
- 새 선택 필드는 명시적인 `false`를 그대로 전송하고, 미수집·`null` 값은 생략합니다. 분석 시 누락된 `is_model_used`를 모델 미사용으로 집계하지 않아야 하며, n8n 응답을 받은 뒤 백엔드 후처리가 실패한 경우에는 오류 이벤트에도 이미 받은 실행 메타데이터가 유지됩니다.

### Changed
- RAG 요청의 이전 답변에 소유권을 검증한 링크 맥락을 함께 전달하고, 답변 카드가 검색 결과의 선택 순서를 유지하도록 개선했습니다. 저장되지 않은 과거 카드 순서는 추정하지 않습니다.
- RAG 요약·재요약·링크 동기화 요청에 최초 저장 시각과 요약 상태를 전달합니다. 제목이나 메모를 수정해도 최초 저장일이 바뀌지 않습니다.
- 서버 GA 이벤트 `query_submit`, `query_response_complete`의 질의 연결 키를 `query_id`에서 `app_query_id`로 변경했습니다. GA 웹 태그의 내부 식별자와 충돌하지 않도록 하며, API/WebSocket의 `queryId` 필드와 값은 유지됩니다.
- 클라이언트의 `query_result_click`, `query_feedback`도 응답의 `queryId`를 `app_query_id`로 전송해야 네 질의 이벤트를 연결할 수 있습니다. 백엔드 배포 시 클라이언트에도 변경된 연결 키가 함께 적용되어야 합니다.
- 질의별 GA 분석·리포트의 연결 기준도 `app_query_id`로 맞춰야 합니다.

### Fixed
- 운영 배포에서 GA4 DebugView 설정이 서버 컨테이너에 전달되지 않던 문제를 수정했습니다. GitHub Actions Secret `ANALYTICS_GA4_DEBUG_MODE=true`를 설정하고 배포하면 서버 GA 이벤트에 `debug_mode`가 포함되어 클라이언트 연동 QA에 활용할 수 있습니다. 미설정 시 기본값은 `false`입니다.
- DebugView 확인에는 기존 GA 수집 설정과 요청의 `clientId`가 필요하며, 브라우저 이벤트의 디버그 설정은 별도로 적용해야 합니다. 클라이언트 요청·응답 형식은 유지됩니다.
- blue/green 중 한 인스턴스만 실행되는 정상 상태를 장애로 알리던 문제를 수정하고, 장애 알림 전송 실패를 감지하도록 보강했습니다.
- 운영 장애 알림에 필요한 Secret 파일 접근 권한을 바로잡고, 배포 시 변경된 모니터링 설정과 경보 규칙이 실제로 적용되도록 개선했습니다.

## [v0.1.1] - 2026-09-01

### Added
- `DELETE /v1/member` 회원 탈퇴 API를 추가했습니다. 요청 본문에는 `confirmed: true`, 탈퇴 사유인 `deleteReason`, GA 클라이언트 식별자인 `clientId`를 전달해야 합니다.
- `deleteReason`은 `NO_USEFUL_LINKS`, `POOR_SEARCH`, `NO_REVISIT`, `SWITCHED_SERVICE`, `PRIVACY_CONCERN`, `OTHER` 중 하나를 사용합니다.
- 최초 탈퇴 요청에는 최근 발급된 access token이 필요합니다. 재인증이 필요하면 `401 M-009`를 반환하며, 탈퇴가 완료되면 access/refresh token 쿠키가 만료되므로 클라이언트도 로그아웃 상태로 전환해야 합니다.

### Changed
- 링크 저장·중복 확인·메타데이터 수집 시 URL의 공백과 중복 프로토콜을 자동으로 정리하고, 프로토콜이 없으면 `https://`를 보완해 같은 링크를 일관되게 처리합니다.
- 기존 `http://` 주소와 path·query·fragment는 그대로 유지하며, 유효하지 않은 URL은 저장 전에 오류로 안내합니다.
- BREAKING: 자체 회원가입·로그인 API(`POST /v1/member/signup`, `POST /v1/member/login`)를 제거했습니다. 인증 진입점은 Google OAuth(`/oauth2/**`)와 토큰 재발급(`/v1/auth/reissue`)으로 단일화되며, 제거된 두 경로는 공개 URL 목록에서도 빠져 401을 응답합니다.
- 자체 로그인 전용 에러코드 `M-001`(중복 이메일)과 `M-003`(비밀번호 불일치)를 제거했습니다. Swagger User 태그의 회원가입·로그인 문서도 함께 사라집니다.
- 회원 정보에서 비밀번호 필드를 제거했습니다. Base64로 인코딩해 저장하던 비밀번호와 OAuth 가입 시 이메일 평문을 더미 비밀번호로 저장하던 동작이 없어집니다.
- 채팅 히스토리에서 한 질의의 사용자 메시지와 AI 응답에 동일한 `queryId`를 제공합니다. 클라이언트는 이 값으로 질문과 답변을 연결하고 GA 이벤트와도 동일한 질의를 추적할 수 있습니다.

### Fixed
- 새 백엔드 인스턴스의 healthcheck가 성공한 뒤에만 API 트래픽을 전환하고, 실패 시 기존 인스턴스를 유지하도록 배포 흐름을 보강했습니다. 배포 중 클라이언트 요청이 실패할 가능성을 줄였습니다.

## [v0.1.0] - 2026-08-10

### Added
- 링크 저장 요청에 analytics context를 전달할 수 있어 URL 원문을 노출하지 않고 저장 시도, 성공, 실패 흐름을 측정할 수 있습니다.
- 요약 생성 완료, 지연 시간, 실패 여부를 기록해 늦거나 실패한 요약 흐름을 더 쉽게 파악할 수 있습니다.
- 채팅 질문과 답변에 `queryId`를 포함해 프론트 이벤트와 백엔드 응답을 연결할 수 있습니다.
- AI/RAG 서버가 내려주는 검색 후보 수, 선택 링크 수, 최고 유사도 정보를 채팅 답변 응답에서 받을 수 있습니다.
- 신규 OAuth 사용자는 약관 동의 페이지로 이동하고, 동의 완료 후 활성 토큰을 받습니다.
- 최종 실패한 요약 작업은 관리자 추적 및 재처리가 가능하도록 보존됩니다.
- HTTP 상태, AI 호출, 비동기 작업, executor 포화 상태를 메트릭과 대시보드, 알림으로 확인할 수 있습니다.
- 운영 배포는 버전 태그 기준으로 실행되며, 릴리즈 노트는 이 changelog에서 생성됩니다.
- 사용자가 링크를 저장하고 AI 요약을 받는 흐름의 안정성을 높였습니다.
- 채팅 답변과 저장된 링크, RAG 검색 메타데이터를 함께 분석할 수 있도록 응답 정보를 확장했습니다.
- Google 최초 가입 사용자는 서비스 진입 전에 필수 약관 동의 흐름을 거치도록 변경했습니다.
- 약관 동의를 완료하지 않은 사용자는 필수 동의 전까지 보호된 API와 WebSocket 흐름을 사용할 수 없습니다.
- 브라우저/SockJS 기반 채팅 연결은 유지하면서 STOMP 메시징 인증을 강화했습니다.
- 느린 AI 호출이 빠른 이벤트 후속 처리에 영향을 덜 주도록 비동기 작업 경로를 분리했습니다.
- AI 서버 장애를 더 명확한 오류 상태와 재시도/서킷브레이커 흐름으로 처리합니다.
- access token이 아닌 토큰으로 WebSocket/STOMP 세션이 성립되지 않도록 인증 경계를 강화했습니다.
- 약관 미동의 사용자가 WebSocket 경로로 약관 동의를 우회할 수 없도록 보강했습니다.
- OAuth 약관 동의 저장과 회원 상태 토큰 클레임 처리를 안정화했습니다.
- 링크, 요약, 채팅 질의 이벤트의 analytics 이름과 payload 정합성을 맞췄습니다.
- 성공한 비동기 작업이 실패로 집계되지 않도록 메트릭 집계 흐름을 정리했습니다.

[v0.1.1]: https://github.com/Team-SoFa/linkiving-core/compare/v0.1.0...v0.1.1
[v0.1.0]: https://github.com/Team-SoFa/linkiving-core/releases/tag/v0.1.0
