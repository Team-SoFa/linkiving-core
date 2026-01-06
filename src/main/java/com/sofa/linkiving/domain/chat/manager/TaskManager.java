package com.sofa.linkiving.domain.chat.manager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import org.springframework.stereotype.Component;

@Component
public class TaskManager {
	private final Map<Long, Future<?>> activeTasks = new ConcurrentHashMap<>();

	public void put(Long chatId, Future<?> task) {
		activeTasks.put(chatId, task);
	}

	public void cancel(Long chatId) {
		Future<?> task = activeTasks.remove(chatId);
		if (task != null && !task.isDone()) {
			task.cancel(true);
		}
	}

	public void remove(Long chatId) {
		activeTasks.remove(chatId);
	}
}
