# Changelog

All notable changes to this project are documented in this file.

Before creating a release tag, add a section whose heading matches the tag name.
For example, tag `v1.2.3` must have a `## [v1.2.3]` section.

## [Unreleased]

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

[v0.1.0]: https://github.com/Team-SoFa/linkiving-core/releases/tag/v0.1.0
