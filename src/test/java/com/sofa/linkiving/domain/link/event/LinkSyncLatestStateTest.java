package com.sofa.linkiving.domain.link.event;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.sofa.linkiving.domain.link.ai.LinkSyncClient;
import com.sofa.linkiving.domain.link.dto.request.LinkSyncUpdateReq;
import com.sofa.linkiving.domain.link.enums.SummaryStatus;
import com.sofa.linkiving.domain.link.enums.SyncAction;
import com.sofa.linkiving.domain.link.service.LinkSyncSnapshotService;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class LinkSyncLatestStateTest {

	private final LinkSyncClient client = mock(LinkSyncClient.class);
	private final LinkSyncSnapshotService snapshots = mock(LinkSyncSnapshotService.class);
	private LinkSyncEventListener listener;
	private final LinkSyncUpdateReq latest = LinkSyncUpdateReq.builder()
		.linkId(1L).userId(9L).title("현재 제목").summary("선택된 최신 요약")
		.summaryStatus(SummaryStatus.COMPLETED).build();

	@BeforeEach
	void setUp() {
		listener = new LinkSyncEventListener(client, new SimpleMeterRegistry(), snapshots);
	}

	@ParameterizedTest
	@EnumSource(value = SyncAction.class, names = {"CREATE", "UPDATE"})
	void delayedEventsAndDuplicatesUseCurrentSelectedSummary(SyncAction action) {
		when(snapshots.findCurrent(1L)).thenReturn(Optional.of(latest));
		when(snapshots.isSyncTarget(1L)).thenReturn(true);
		LinkSyncUpdateReq stale = LinkSyncUpdateReq.builder().linkId(1L).summary("과거 요약").build();

		listener.handleLinkSyncEvent(new LinkSyncEvent(stale, action));
		listener.handleLinkSyncEvent(new LinkSyncEvent(stale, action));

		if (action == SyncAction.CREATE) {
			verify(client, times(2)).syncCreate(latest);
		} else {
			verify(client, times(2)).syncUpdate(latest);
		}
		verifyNoMoreInteractions(client);
	}

	@ParameterizedTest
	@EnumSource(value = SyncAction.class, names = {"CREATE", "UPDATE"})
	void missingDeletedOrInactiveTargetOnlyDeletes(SyncAction action) {
		when(snapshots.findCurrent(1L)).thenReturn(Optional.empty());

		listener.handleLinkSyncEvent(new LinkSyncEvent(latest, action));

		verify(client).syncDelete(1L);
		verifyNoMoreInteractions(client);
	}

	@Test
	void deletionDuringSuccessfulWriteTriggersCleanup() {
		when(snapshots.findCurrent(1L)).thenReturn(Optional.of(latest));
		when(snapshots.isSyncTarget(1L)).thenReturn(false);

		listener.handleLinkSyncEvent(LinkSyncEvent.refreshEvent(1L));

		var order = inOrder(client);
		order.verify(client).syncUpdate(latest);
		order.verify(client).syncDelete(1L);
	}

	@Test
	void deleteDoesNotRequireMemberOrLinkToStillExist() {
		listener.handleLinkSyncEvent(LinkSyncEvent.deleteEvent(1L));

		verify(client).syncDelete(1L);
		verifyNoInteractions(snapshots);
	}

	@Test
	void snapshotFailureDoesNotTreatDatabaseOutageAsDeletion() {
		when(snapshots.findCurrent(1L)).thenThrow(new IllegalStateException("database unavailable"));

		assertThatThrownBy(() -> listener.handleLinkSyncEvent(LinkSyncEvent.refreshEvent(1L)))
			.isInstanceOf(IllegalStateException.class);

		verifyNoInteractions(client);
	}

	@Test
	void updateAndDeleteOfSameLinkAreSerialized() throws Exception {
		when(snapshots.findCurrent(1L)).thenReturn(Optional.of(latest));
		when(snapshots.isSyncTarget(1L)).thenReturn(true);
		CountDownLatch writing = new CountDownLatch(1);
		CountDownLatch releaseWrite = new CountDownLatch(1);
		CountDownLatch attemptingDelete = new CountDownLatch(1);
		CountDownLatch deleting = new CountDownLatch(1);
		doAnswer(call -> {
			writing.countDown();
			assertThat(releaseWrite.await(3, TimeUnit.SECONDS)).isTrue();
			return null;
		}).when(client).syncUpdate(latest);
		doAnswer(call -> {
			deleting.countDown();
			return null;
		}).when(client).syncDelete(1L);
		var executor = Executors.newFixedThreadPool(2);
		try {
			var update = executor.submit(() -> listener.handleLinkSyncEvent(LinkSyncEvent.refreshEvent(1L)));
			assertThat(writing.await(2, TimeUnit.SECONDS)).isTrue();
			var delete = executor.submit(() -> {
				attemptingDelete.countDown();
				listener.handleLinkSyncEvent(LinkSyncEvent.deleteEvent(1L));
			});
			assertThat(attemptingDelete.await(2, TimeUnit.SECONDS)).isTrue();
			assertThat(deleting.await(100, TimeUnit.MILLISECONDS)).isFalse();
			releaseWrite.countDown();
			update.get(2, TimeUnit.SECONDS);
			delete.get(2, TimeUnit.SECONDS);
			assertThat(deleting.getCount()).isZero();
		} finally {
			releaseWrite.countDown();
			executor.shutdownNow();
		}
	}

	@Test
	void differentLinksCanStillSyncWhileOneIsWaiting() throws Exception {
		when(snapshots.findCurrent(1L)).thenReturn(Optional.of(latest));
		when(snapshots.isSyncTarget(1L)).thenReturn(true);
		CountDownLatch writing = new CountDownLatch(1);
		CountDownLatch releaseWrite = new CountDownLatch(1);
		doAnswer(call -> {
			writing.countDown();
			assertThat(releaseWrite.await(3, TimeUnit.SECONDS)).isTrue();
			return null;
		}).when(client).syncUpdate(latest);
		var executor = Executors.newFixedThreadPool(2);
		try {
			var update = executor.submit(() -> listener.handleLinkSyncEvent(LinkSyncEvent.refreshEvent(1L)));
			assertThat(writing.await(2, TimeUnit.SECONDS)).isTrue();
			executor.submit(() -> listener.handleLinkSyncEvent(LinkSyncEvent.deleteEvent(2L)))
				.get(2, TimeUnit.SECONDS);
			verify(client).syncDelete(2L);
			releaseWrite.countDown();
			update.get(2, TimeUnit.SECONDS);
		} finally {
			releaseWrite.countDown();
			executor.shutdownNow();
		}
	}
}
