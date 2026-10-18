# GA 식별자 계약

회원 프로필과 링크 생성·목록·상세·채팅 링크 응답의 `analyticsId`는 DB 기본키의 10진 문자열입니다. 기존 `id`는 Hashids를 유지하며 API 경로·요청에는 기존 `id`를 사용합니다.

- 웹 `user_id`: 회원 `analyticsId`. 서버 `user_id`: 같은 회원 DB ID 문자열.
- 웹 `link_id`: 링크 `analyticsId`. 서버 `summary_complete.link_id`: 같은 링크 DB ID.
- `app_query_id`: `messages.query_id`와 같은 UUID. 메시지의 화면용 `id`와 다릅니다.
- `client_id`: 웹은 GA 브라우저 ID, 확장은 설치별 ID입니다. 서로 같게 만들지 않으며 같은 회원의 연결에는 `user_id`를 사용합니다.
- 확장 저장 이벤트는 저장 요청의 `clientId`와 `source=extension`을 받은 서버에서 발행합니다.

백엔드의 `analyticsId` 응답을 먼저 배포한 뒤 웹 연동(Team-SoFa/linkiving#609)을 배포합니다. 구버전 응답에서는 웹이 Hashids를 대신 보내지 않고 해당 분석 ID를 생략합니다. 과거에 쌓인 숫자/Hashids 데이터는 소급 변환되지 않습니다. 분석 ID는 인증 토큰이 아니며 기존 소유권 검사는 그대로 적용됩니다.
