package com.sofa.linkiving.domain.chat.manager;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.Disposable;

@ExtendWith(MockitoExtension.class)
public class SubscriptionManagerTest {

	@InjectMocks
	private SubscriptionManager subscriptionManager;

	@Mock
	private Disposable disposable;

	@Test
	@DisplayName("구독 추가 요청 시 기존 구독이 있다면 취소 후 등록")
	void shouldDisposeOldSubscriptionWhenAdd() {
		// given
		String key = "chat-1";
		Disposable oldDisposable = mock(Disposable.class);

		// 먼저 하나 등록
		subscriptionManager.add(key, oldDisposable);

		// when: 같은 키로 새로운 구독 등록
		subscriptionManager.add(key, disposable);

		// then: 이전 구독은 dispose 되어야 함
		verify(oldDisposable).dispose();
	}

	@Test
	@DisplayName("구독 취소 요청 시 dispose 호출 및 제거")
	void shouldDisposeWhenCancel() {
		// given
		String key = "chat-1";
		subscriptionManager.add(key, disposable);

		// when
		subscriptionManager.cancel(key);

		// then
		verify(disposable).dispose();
	}

	@Test
	@DisplayName("완료된 구독 제거 요청 시 dispose 없이 맵에서만 제거")
	void shouldNotDisposeWhenRemove() {
		// given
		String key = "chat-1";
		subscriptionManager.add(key, disposable);

		// when
		subscriptionManager.remove(key);

		// then: remove는 dispose를 호출하지 않음 (이미 완료된 상태 가정)
		verify(disposable, never()).dispose();
	}
}
