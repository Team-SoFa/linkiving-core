package com.sofa.linkiving.domain.member.repository;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.member.enums.MemberStatus;

@DataJpaTest(properties = {"test.external.base-url=http://127.0.0.1:1", "ai.server.url=http://127.0.0.1:1"})
@ActiveProfiles("test")
class MemberRepositoryTest {

	private static final LocalDateTime CUTOFF = LocalDateTime.of(2026, 10, 1, 4, 0);

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void shouldDeleteExpiredPendingMembersAndKeepMembersAtOrAfterCutoff() {
		Long expired = saveMember(MemberStatus.PENDING_TERMS, CUTOFF.minusDays(1), null, null);
		Long justExpired = saveMember(MemberStatus.PENDING_TERMS, CUTOFF.minusSeconds(1), null, null);
		Long atCutoff = saveMember(MemberStatus.PENDING_TERMS, CUTOFF, null, null);
		Long recent = saveMember(MemberStatus.PENDING_TERMS, CUTOFF.plusSeconds(1), null, null);

		var deletedCount = memberRepository
			.deleteByStatusAndTermsAgreedAtIsNullAndPrivacyAgreedAtIsNullAndCreatedAtBefore(
				MemberStatus.PENDING_TERMS, CUTOFF);

		assertThat(deletedCount).isEqualTo(2);
		assertThat(memberRepository.findAllById(List.of(expired, justExpired))).isEmpty();
		assertThat(memberRepository.findAll()).extracting(Member::getId).containsExactlyInAnyOrder(atCutoff, recent);
		assertThat(memberRepository.deleteByStatusAndTermsAgreedAtIsNullAndPrivacyAgreedAtIsNullAndCreatedAtBefore(
			MemberStatus.PENDING_TERMS, CUTOFF)).isZero();
	}

	@ParameterizedTest
	@EnumSource(value = MemberStatus.class, names = {"ACTIVE", "WITHDRAWING", "WITHDRAWAL_ANALYTICS_SENT"})
	void shouldKeepMembersOutsidePendingTermsStatus(MemberStatus status) {
		Long memberId = saveMember(status, CUTOFF.minusDays(1), null, null);

		var deletedCount = memberRepository
			.deleteByStatusAndTermsAgreedAtIsNullAndPrivacyAgreedAtIsNullAndCreatedAtBefore(
				MemberStatus.PENDING_TERMS, CUTOFF);

		assertThat(deletedCount).isZero();
		assertThat(memberRepository.existsById(memberId)).isTrue();
	}

	@ParameterizedTest
	@CsvSource({"true, false", "false, true", "true, true"})
	void shouldKeepMembersWithEitherAgreement(boolean termsAgreed, boolean privacyAgreed) {
		LocalDateTime agreedAt = CUTOFF.minusHours(1);
		Long memberId = saveMember(MemberStatus.PENDING_TERMS, CUTOFF.minusDays(1),
			termsAgreed ? agreedAt : null, privacyAgreed ? agreedAt : null);

		var deletedCount = memberRepository
			.deleteByStatusAndTermsAgreedAtIsNullAndPrivacyAgreedAtIsNullAndCreatedAtBefore(
				MemberStatus.PENDING_TERMS, CUTOFF);

		assertThat(deletedCount).isZero();
		assertThat(memberRepository.existsById(memberId)).isTrue();
	}

	private Long saveMember(MemberStatus status, LocalDateTime createdAt,
		LocalDateTime termsAgreedAt, LocalDateTime privacyAgreedAt) {
		Member member = entityManager.persistAndFlush(Member.builder()
			.email("member-" + UUID.randomUUID() + "@example.com")
			.status(status)
			.build());
		// 감사 리스너가 설정한 생성 시각을 테스트의 고정 경계값으로 교체한다.
		jdbcTemplate.update("UPDATE member SET created_at = ?, terms_agreed_at = ?, privacy_agreed_at = ? WHERE id = ?",
			createdAt, termsAgreedAt, privacyAgreedAt, member.getId());
		entityManager.clear();
		return member.getId();
	}
}
