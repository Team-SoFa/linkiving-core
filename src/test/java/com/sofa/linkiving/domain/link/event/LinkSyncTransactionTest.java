package com.sofa.linkiving.domain.link.event;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.ZoneId;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.transaction.TestTransaction;

import com.sofa.linkiving.domain.link.ai.LinkSyncClient;
import com.sofa.linkiving.domain.link.dto.request.LinkSyncUpdateReq;
import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.entity.Summary;
import com.sofa.linkiving.domain.link.enums.Format;
import com.sofa.linkiving.domain.link.enums.SummaryStatus;
import com.sofa.linkiving.domain.link.facade.SummaryWorkerFacade;
import com.sofa.linkiving.domain.link.repository.LinkRepository;
import com.sofa.linkiving.domain.link.service.LinkCommandService;
import com.sofa.linkiving.domain.link.service.LinkQueryService;
import com.sofa.linkiving.domain.link.service.LinkService;
import com.sofa.linkiving.domain.link.service.LinkSyncSnapshotService;
import com.sofa.linkiving.domain.link.service.SummaryCommandService;
import com.sofa.linkiving.domain.link.service.SummaryQueryService;
import com.sofa.linkiving.domain.link.service.SummaryService;
import com.sofa.linkiving.domain.link.util.UrlNormalizer;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.member.enums.MemberStatus;
import com.sofa.linkiving.global.error.exception.BusinessException;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.persistence.EntityManager;

@DataJpaTest
@ContextConfiguration(classes = LinkSyncTransactionTest.JpaConfig.class)
@Import({LinkSyncEventListener.class, LinkSyncSnapshotService.class, LinkService.class,
	LinkCommandService.class, LinkQueryService.class, UrlNormalizer.class, SummaryWorkerFacade.class,
	SummaryService.class, SummaryCommandService.class, SummaryQueryService.class,
	LinkSyncTransactionTest.MetricsConfig.class})
class LinkSyncTransactionTest {

	@Autowired
	private EntityManager entityManager;
	@Autowired
	private LinkRepository linkRepository;
	@Autowired
	private LinkService linkService;
	@Autowired
	private SummaryWorkerFacade workerFacade;
	@Autowired
	private SummaryCommandService summaries;
	@Autowired
	private LinkSyncSnapshotService snapshots;
	@MockitoBean
	private LinkSyncClient client;

	@Test
	void initialCompletionSendsCommittedSelectedSummaryAndOriginalSavedAt() {
		Link link = fixture(SummaryStatus.PROCESSING, MemberStatus.ACTIVE);
		long savedAt = link.getCreatedAt().atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();

		workerFacade.createInitialSummaryAndUpdateStatus(link.getId(), "선택된 초기 요약");
		verifyNoInteractions(client);
		commit();

		LinkSyncUpdateReq sent = sentUpdate();
		assertThat(sent.summary()).isEqualTo("선택된 초기 요약");
		assertThat(sent.summaryStatus()).isEqualTo(SummaryStatus.COMPLETED);
		assertThat(sent.savedAtEpochMs()).isEqualTo(savedAt);
		assertThat(sent.userId()).isEqualTo(link.getMember().getId());
	}

	@Test
	void failedLinkWithoutSummaryStillSendsMetadata() {
		Link link = fixture(SummaryStatus.PROCESSING, MemberStatus.ACTIVE);

		workerFacade.updateSummaryStatus(link.getId(), SummaryStatus.FAILED);
		verifyNoInteractions(client);
		commit();

		LinkSyncUpdateReq sent = sentUpdate();
		assertThat(sent.summaryStatus()).isEqualTo(SummaryStatus.FAILED);
		assertThat(sent.summary()).isNull();
		assertThat(sent.title()).isEqualTo("합성 문서");
		assertThat(sent.memo()).isEqualTo("합성 메모");
	}

	@Test
	void userRetrySendsCommittedPendingState() {
		Link link = fixture(SummaryStatus.FAILED, MemberStatus.ACTIVE);

		linkService.resetSummaryStatusForRetry(link.getId(), link.getMember());
		verifyNoInteractions(client);
		commit();

		assertThat(sentUpdate().summaryStatus()).isEqualTo(SummaryStatus.PENDING);
	}

	@Test
	void adminRetryStatusChangeAlsoPublishesPending() {
		Link link = fixture(SummaryStatus.FAILED, MemberStatus.ACTIVE);

		linkService.updateSummaryStatus(link.getId(), SummaryStatus.PENDING);
		commit();

		assertThat(sentUpdate().summaryStatus()).isEqualTo(SummaryStatus.PENDING);
	}

	@Test
	void selectedSummaryIsReloadedAfterBulkSelectionClearsPersistenceContext() {
		Link link = fixture(SummaryStatus.COMPLETED, MemberStatus.ACTIVE);
		Summary previous = summaries.initialSave(link, Format.CONCISE, "과거 요약");
		Summary selected = summaries.save(link, Format.DETAILED, "사용자가 선택한 새 요약");

		summaries.selectSummary(link.getId(), selected.getId());
		verifyNoInteractions(client);
		commit();

		assertThat(sentUpdate().summary()).isEqualTo("사용자가 선택한 새 요약");
		assertThat(selected.getId()).isNotEqualTo(previous.getId());
	}

	@Test
	void savingUnselectedSummaryDoesNotPublish() {
		Link link = fixture(SummaryStatus.COMPLETED, MemberStatus.ACTIVE);
		summaries.save(link, Format.CONCISE, "미선택 요약");
		commit();

		verifyNoInteractions(client);
	}

	@Test
	void transactionRollbackDoesNotSendStatusOrSelection() {
		Link link = fixture(SummaryStatus.FAILED, MemberStatus.ACTIVE);
		Summary selected = summaries.save(link, Format.CONCISE, "취소한 요약");
		linkService.updateSummaryStatus(link.getId(), SummaryStatus.COMPLETED);
		summaries.selectSummary(link.getId(), selected.getId());

		TestTransaction.flagForRollback();
		TestTransaction.end();

		verifyNoInteractions(client);
		assertThat(snapshots.findCurrent(link.getId())).isEmpty();
	}

	@Test
	void selectionOfOtherLinksSummaryRollsBackAndKeepsOriginalSelection() {
		Link first = fixture(SummaryStatus.COMPLETED, MemberStatus.ACTIVE);
		summaries.initialSave(first, Format.CONCISE, "원래 선택 요약");
		Link second = fixture(SummaryStatus.COMPLETED, MemberStatus.ACTIVE);
		Summary other = summaries.save(second, Format.CONCISE, "다른 링크 요약");
		commit();

		assertThatThrownBy(() -> summaries.selectSummary(first.getId(), other.getId()))
			.isInstanceOf(BusinessException.class);

		assertThat(snapshots.findCurrent(first.getId()).orElseThrow().summary()).isEqualTo("원래 선택 요약");
		verifyNoInteractions(client);
	}

	@ParameterizedTest
	@EnumSource(value = MemberStatus.class, names = {"PENDING_TERMS", "WITHDRAWING", "WITHDRAWAL_ANALYTICS_SENT"})
	void inactiveOwnerIsNotAValidSnapshot(MemberStatus status) {
		Link link = fixture(SummaryStatus.COMPLETED, status);
		commit();

		assertThat(snapshots.findCurrent(link.getId())).isEmpty();
		assertThat(snapshots.isSyncTarget(link.getId())).isFalse();
	}

	@Test
	void deletedLinkIsNotAValidSnapshot() {
		Link link = fixture(SummaryStatus.COMPLETED, MemberStatus.ACTIVE);
		link.markDeleted();
		commit();

		assertThat(snapshots.findCurrent(link.getId())).isEmpty();
		assertThat(snapshots.isSyncTarget(link.getId())).isFalse();
	}

	@Test
	void deletedMemberIsNotAValidSnapshot() {
		Link link = fixture(SummaryStatus.COMPLETED, MemberStatus.ACTIVE);
		link.getMember().markDeleted();
		commit();

		assertThat(snapshots.findCurrent(link.getId())).isEmpty();
		assertThat(snapshots.isSyncTarget(link.getId())).isFalse();
	}

	@Test
	void deletedSelectedSummaryDoesNotLeakIntoSnapshot() {
		Link link = fixture(SummaryStatus.COMPLETED, MemberStatus.ACTIVE);
		Summary summary = summaries.initialSave(link, Format.CONCISE, "삭제된 요약");
		entityManager.flush();
		entityManager.find(Summary.class, summary.getId()).markDeleted();
		commit();

		assertThat(snapshots.findCurrent(link.getId()).orElseThrow().summary()).isNull();
	}

	private Link fixture(SummaryStatus status, MemberStatus ownerStatus) {
		Member member = Member.builder().email(UUID.randomUUID() + "@example.com").status(ownerStatus).build();
		entityManager.persist(member);
		Link link = Link.create(member, "https://example.com/synthetic", "합성 문서", "합성 메모", null);
		link.updateSummaryStatus(status);
		linkRepository.saveAndFlush(link);
		return link;
	}

	private LinkSyncUpdateReq sentUpdate() {
		ArgumentCaptor<LinkSyncUpdateReq> captor = ArgumentCaptor.forClass(LinkSyncUpdateReq.class);
		verify(client).syncUpdate(captor.capture());
		verifyNoMoreInteractions(client);
		return captor.getValue();
	}

	private void commit() {
		TestTransaction.flagForCommit();
		TestTransaction.end();
	}

	@TestConfiguration
	static class MetricsConfig {
		@Bean
		MeterRegistry meterRegistry() {
			return new SimpleMeterRegistry();
		}
	}

	@Configuration
	@EnableJpaAuditing
	@EntityScan("com.sofa.linkiving.domain")
	@EnableJpaRepositories("com.sofa.linkiving.domain.link.repository")
	static class JpaConfig {
	}
}
