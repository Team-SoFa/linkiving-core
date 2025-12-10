package com.sofa.linkiving.domain.chat.manager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import reactor.core.Disposable;

@Component
public class SubscriptionManager {

	private final Map<String, Disposable> activeSubscriptions = new ConcurrentHashMap<>();

	/**
	 * 구독 추가 (기존 작업이 있다면 취소 후 등록)
	 */
	public void add(String key, Disposable subscription) {
		cancel(key); // 안전하게 기존 작업 정리
		activeSubscriptions.put(key, subscription);
	}

	/**
	 * 구독 취소 및 자원 해제
	 */
	public void cancel(String key) {
		Disposable subscription = activeSubscriptions.remove(key);
		if (subscription != null && !subscription.isDisposed()) {
			subscription.dispose();
		}
	}

	/**
	 * 완료된 구독 제거 (자원 해제 없이 Map에서만 삭제)
	 */
	public void remove(String key) {
		activeSubscriptions.remove(key);
	}
}
