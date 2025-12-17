package com.sofa.linkiving.domain.link.integration;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sofa.linkiving.domain.link.dto.request.LinkCreateReq;
import com.sofa.linkiving.domain.link.dto.request.LinkMemoUpdateReq;
import com.sofa.linkiving.domain.link.dto.request.LinkTitleUpdateReq;
import com.sofa.linkiving.domain.link.dto.request.LinkUpdateReq;
import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.error.LinkErrorCode;
import com.sofa.linkiving.domain.link.repository.LinkRepository;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.member.enums.Role;
import com.sofa.linkiving.domain.member.repository.MemberRepository;
import com.sofa.linkiving.security.userdetails.CustomMemberDetail;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class LinkApiIntegrationTest {

	private static final String BASE_URL = "/v1/links";

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	LinkRepository linkRepository;

	@Autowired
	MemberRepository memberRepository;
	private Member testMember;

	private Member otherMember;
	private UserDetails testUserDetails;
	private UserDetails otherUserDetails;

	@BeforeEach
	void setUp() {
		testMember = memberRepository.save(Member.builder()
			.email("test@test.com")
			.password("password")
			.build());

		otherMember = memberRepository.save(Member.builder()
			.email("other@test.com")
			.password("password")
			.build());

		testUserDetails = new CustomMemberDetail(testMember, Role.USER);
		otherUserDetails = new CustomMemberDetail(otherMember, Role.USER);
	}

	@Test
	@DisplayName("링크 생성 성공 시 DB에 저장되고 200 OK 응답")
	void shouldCreateLinkSuccessfully() throws Exception {
		// given
		LinkCreateReq req = new LinkCreateReq("https://example.com", "테스트 링크", "테스트 메모", null);

		// when & then
		mockMvc.perform(
				post(BASE_URL)
					.with(csrf())
					.with(user(testUserDetails))
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(req))
					.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.status").value("OK"))
			.andExpect(jsonPath("$.message").value("링크 생성 완료"))
			.andExpect(jsonPath("$.data.url").value(req.url()))
			.andExpect(jsonPath("$.data.title").value(req.title()))
			.andExpect(jsonPath("$.data.memo").value(req.memo()));

		// DB 검증
		boolean exists = linkRepository.existsByMemberAndUrlAndIsDeleteFalse(testMember, req.url());
		assertThat(exists).isTrue();
	}

	@Test
	@DisplayName("중복 URL로 링크 생성 시 400 BAD_REQUEST 응답")
	void shouldFailWhenDuplicateUrl() throws Exception {
		// given
		String duplicateUrl = "https://example.com";
		linkRepository.save(Link.builder()
			.member(testMember)
			.url(duplicateUrl)
			.title("기존 링크")
			.build());

		LinkCreateReq req = new LinkCreateReq(duplicateUrl, "새 링크", null, null);

		// when & then
		mockMvc.perform(
				post(BASE_URL)
					.with(csrf())
					.with(user(testUserDetails))
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(req))
			)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.status").value(LinkErrorCode.DUPLICATE_URL.getStatus().name()))
			.andExpect(jsonPath("$.message").value(LinkErrorCode.DUPLICATE_URL.getMessage()))
			.andExpect(jsonPath("$.data").value(LinkErrorCode.DUPLICATE_URL.getCode()));
	}

	@Test
	@DisplayName("링크 조회 성공 시 200 OK 응답")
	void shouldGetLinkSuccessfully() throws Exception {
		// given
		Link link = linkRepository.save(Link.builder()
			.member(testMember)
			.url("https://example.com")
			.title("테스트 링크")
			.memo("테스트 메모")
			.build());

		// when & then
		mockMvc.perform(
				get(BASE_URL + "/{id}", link.getId())
					.with(csrf())
					.with(user(testUserDetails))
					.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.url").value(link.getUrl()))
			.andExpect(jsonPath("$.data.title").value(link.getTitle()))
			.andExpect(jsonPath("$.data.memo").value(link.getMemo()));
	}

	@Test
	@DisplayName("다른 사용자의 링크 조회 시 404 NOT_FOUND 응답 (IDOR 방지)")
	void shouldFailWhenAccessingOtherUserLink() throws Exception {
		// given
		Link otherUserLink = linkRepository.save(Link.builder()
			.member(otherMember)
			.url("https://example.com")
			.title("다른 사용자 링크")
			.build());

		// when & then
		mockMvc.perform(
				get(BASE_URL + "/{id}", otherUserLink.getId())
					.with(csrf())
					.with(user(testUserDetails))
					.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.status").value(LinkErrorCode.LINK_NOT_FOUND.getStatus().name()))
			.andExpect(jsonPath("$.message").value(LinkErrorCode.LINK_NOT_FOUND.getMessage()));
	}

	@Test
	@DisplayName("링크 제목 수정 성공 시 DB 업데이트 및 200 OK 응답")
	void shouldUpdateTitleSuccessfully() throws Exception {
		// given
		Link link = linkRepository.save(Link.builder()
			.member(testMember)
			.url("https://example.com")
			.title("원래 제목")
			.build());

		LinkTitleUpdateReq req = new LinkTitleUpdateReq("수정된 제목");

		// when & then
		mockMvc.perform(
				patch(BASE_URL + "/{id}/title", link.getId())
					.with(csrf())
					.with(user(testUserDetails))
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(req))
					.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.message").value("제목 수정 완료"))
			.andExpect(jsonPath("$.data.title").value(req.title()));

		// DB 검증
		Link updated = linkRepository.findById(link.getId()).orElseThrow();
		assertThat(updated.getTitle()).isEqualTo(req.title());
	}

	@Test
	@DisplayName("링크 삭제 성공 시 Soft Delete 처리 및 200 OK 응답")
	void shouldDeleteLinkSuccessfully() throws Exception {
		// given
		Link link = linkRepository.save(Link.builder()
			.member(testMember)
			.url("https://example.com")
			.title("삭제할 링크")
			.build());

		// when & then
		mockMvc.perform(
				delete(BASE_URL + "/{id}", link.getId())
					.with(csrf())
					.with(user(testUserDetails))
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.message").value("링크 삭제 완료"));

		// DB 검증 - Soft Delete 확인
		Link deleted = linkRepository.findById(link.getId()).orElseThrow();
		assertThat(deleted.isDeleted()).isTrue();
	}

	@Test
	@DisplayName("URL 중복 체크 - 중복된 URL인 경우 true 반환")
	void shouldReturnTrueWhenUrlExists() throws Exception {
		// given
		String existingUrl = "https://example.com";
		linkRepository.save(Link.builder()
			.member(testMember)
			.url(existingUrl)
			.title("기존 링크")
			.build());

		// when & then
		mockMvc.perform(
				get(BASE_URL + "/duplicate")
					.param("url", existingUrl)
					.with(csrf())
					.with(user(testUserDetails))
					.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.exists").value(true))
			.andExpect(jsonPath("$.data.linkId").isNumber())
			.andExpect(jsonPath("$.message").value("URL 중복 체크 완료"));
	}

	@Test
	@DisplayName("URL 중복 체크 - 중복되지 않은 URL인 경우 false 반환")
	void shouldReturnFalseWhenUrlDoesNotExist() throws Exception {
		// given
		String newUrl = "https://new-example.com";

		// when & then
		mockMvc.perform(
				get(BASE_URL + "/duplicate")
					.param("url", newUrl)
					.with(csrf())
					.with(user(testUserDetails))
					.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.exists").value(false))
			.andExpect(jsonPath("$.data.linkId").isEmpty());
	}

	@Test
	@DisplayName("링크 목록 조회 성공 시 페이징된 결과 반환")
	void shouldGetLinkListSuccessfully() throws Exception {
		// given
		linkRepository.save(Link.builder()
			.member(testMember)
			.url("https://example1.com")
			.title("링크 1")
			.build());

		linkRepository.save(Link.builder()
			.member(testMember)
			.url("https://example2.com")
			.title("링크 2")
			.build());

		// when & then
		mockMvc.perform(
				get(BASE_URL)
					.param("page", "0")
					.param("size", "20")
					.with(csrf())
					.with(user(testUserDetails))
					.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.content").isArray())
			.andExpect(jsonPath("$.data.content.length()").value(2))
			.andExpect(jsonPath("$.data.totalElements").value(2));
	}

	@Test
	@DisplayName("링크 전체 수정 성공 시 DB 업데이트 및 200 OK 응답")
	void shouldUpdateLinkSuccessfully() throws Exception {
		// given
		Link link = linkRepository.save(Link.builder()
			.member(testMember)
			.url("https://example.com")
			.title("원래 제목")
			.memo("원래 메모")
			.build());

		LinkUpdateReq req = new LinkUpdateReq("수정된 제목", "수정된 메모");

		// when & then
		mockMvc.perform(
				put(BASE_URL + "/{id}", link.getId())
					.with(csrf())
					.with(user(testUserDetails))
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(req))
					.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.message").value("링크 수정 완료"))
			.andExpect(jsonPath("$.data.title").value(req.title()))
			.andExpect(jsonPath("$.data.memo").value(req.memo()));

		// DB 검증
		Link updated = linkRepository.findById(link.getId()).orElseThrow();
		assertThat(updated.getTitle()).isEqualTo(req.title());
		assertThat(updated.getMemo()).isEqualTo(req.memo());
	}

	@Test
	@DisplayName("링크 메모 수정 성공 시 DB 업데이트 및 200 OK 응답")
	void shouldUpdateMemoSuccessfully() throws Exception {
		// given
		Link link = linkRepository.save(Link.builder()
			.member(testMember)
			.url("https://example.com")
			.title("테스트 링크")
			.memo("원래 메모")
			.build());

		LinkMemoUpdateReq req = new LinkMemoUpdateReq("수정된 메모");

		// when & then
		mockMvc.perform(
				patch(BASE_URL + "/{id}/memo", link.getId())
					.with(csrf())
					.with(user(testUserDetails))
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(req))
					.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.message").value("메모 수정 완료"))
			.andExpect(jsonPath("$.data.memo").value(req.memo()));

		// DB 검증
		Link updated = linkRepository.findById(link.getId()).orElseThrow();
		assertThat(updated.getMemo()).isEqualTo(req.memo());
	}

	@Test
	@DisplayName("존재하지 않는 링크 조회 시 404 NOT_FOUND 응답")
	void shouldFailWhenLinkNotFound() throws Exception {
		// given
		Long nonExistentId = 99999L;

		// when & then
		mockMvc.perform(
				get(BASE_URL + "/{id}", nonExistentId)
					.with(csrf())
					.with(user(testUserDetails))
					.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.status").value(LinkErrorCode.LINK_NOT_FOUND.getStatus().name()))
			.andExpect(jsonPath("$.message").value(LinkErrorCode.LINK_NOT_FOUND.getMessage()));
	}

	@Test
	@DisplayName("다른 사용자의 링크 수정 시도 시 404 NOT_FOUND 응답 (IDOR 방지)")
	void shouldFailWhenUpdatingOtherUserLink() throws Exception {
		// given
		Link otherUserLink = linkRepository.save(Link.builder()
			.member(otherMember)
			.url("https://example.com")
			.title("다른 사용자 링크")
			.build());

		LinkTitleUpdateReq req = new LinkTitleUpdateReq("해킹 시도");

		// when & then
		mockMvc.perform(
				patch(BASE_URL + "/{id}/title", otherUserLink.getId())
					.with(csrf())
					.with(user(testUserDetails))
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(req))
					.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.status").value(LinkErrorCode.LINK_NOT_FOUND.getStatus().name()));

		// DB 검증 - 변경되지 않았는지 확인
		Link unchanged = linkRepository.findById(otherUserLink.getId()).orElseThrow();
		assertThat(unchanged.getTitle()).isEqualTo("다른 사용자 링크");
	}

	@Test
	@DisplayName("다른 사용자의 링크 삭제 시도 시 404 NOT_FOUND 응답 (IDOR 방지)")
	void shouldFailWhenDeletingOtherUserLink() throws Exception {
		// given
		Link otherUserLink = linkRepository.save(Link.builder()
			.member(otherMember)
			.url("https://example.com")
			.title("다른 사용자 링크")
			.build());

		// when & then
		mockMvc.perform(
				delete(BASE_URL + "/{id}", otherUserLink.getId())
					.with(csrf())
					.with(user(testUserDetails))
			)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.status").value(LinkErrorCode.LINK_NOT_FOUND.getStatus().name()));

		// DB 검증 - 삭제되지 않았는지 확인
		Link unchanged = linkRepository.findById(otherUserLink.getId()).orElseThrow();
		assertThat(unchanged.isDeleted()).isFalse();
	}

	@Test
	@DisplayName("삭제된 링크 조회 시 404 NOT_FOUND 응답")
	void shouldFailWhenAccessingDeletedLink() throws Exception {
		// given
		Link link = linkRepository.save(Link.builder()
			.member(testMember)
			.url("https://example.com")
			.title("삭제된 링크")
			.build());

		link.markDeleted(); // Soft delete
		linkRepository.save(link);

		// when & then
		mockMvc.perform(
				get(BASE_URL + "/{id}", link.getId())
					.with(csrf())
					.with(user(testUserDetails))
					.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.status").value(LinkErrorCode.LINK_NOT_FOUND.getStatus().name()));
	}

	@Test
	@DisplayName("필수 값 누락 시 400 BAD_REQUEST 응답 - URL 누락")
	void shouldFailWhenUrlIsMissing() throws Exception {
		// given
		String invalidJson = """
			{
				"title": "테스트 링크",
				"memo": "메모"
			}
			""";

		// when & then
		mockMvc.perform(
				post(BASE_URL)
					.with(csrf())
					.with(user(testUserDetails))
					.contentType(MediaType.APPLICATION_JSON)
					.content(invalidJson)
			)
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("필수 값 누락 시 400 BAD_REQUEST 응답 - 제목 누락")
	void shouldFailWhenTitleIsMissing() throws Exception {
		// given
		String invalidJson = """
			{
				"url": "https://example.com",
				"memo": "메모"
			}
			""";

		// when & then
		mockMvc.perform(
				post(BASE_URL)
					.with(csrf())
					.with(user(testUserDetails))
					.contentType(MediaType.APPLICATION_JSON)
					.content(invalidJson)
			)
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("제목 100자 초과 시 400 BAD_REQUEST 응답")
	void shouldFailWhenTitleExceedsMaxLength() throws Exception {
		// given
		String longTitle = "a".repeat(101);
		LinkCreateReq req = new LinkCreateReq(
			"https://example.com",
			longTitle,
			null,
			null
		);

		// when & then
		mockMvc.perform(
				post(BASE_URL)
					.with(csrf())
					.with(user(testUserDetails))
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(req))
			)
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("URL 2048자 초과 시 400 BAD_REQUEST 응답")
	void shouldFailWhenUrlExceedsMaxLength() throws Exception {
		// given
		String longUrl = "https://example.com/" + "a".repeat(2048);
		LinkCreateReq req = new LinkCreateReq(longUrl, "테스트 링크", null, null);

		// when & then
		mockMvc.perform(
				post(BASE_URL)
					.with(csrf())
					.with(user(testUserDetails))
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(req))
			)
			.andExpect(status().isBadRequest());
	}
}
