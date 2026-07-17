package com.sofa.linkiving.global.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@EnableRetry
@EnableAsync
@Configuration
public class AsyncConfig implements AsyncConfigurer {

	private final TaskDecorator taskDecorator;

	public AsyncConfig(TaskDecorator taskDecorator) {
		this.taskDecorator = taskDecorator;
	}

	/**
	 * 기본 async executor. 이름 없는 @Async 가 여기로 온다.
	 * 빠른 이벤트 후속 처리(웹소켓 푸시 등) 용도이며, 느린 AI 연동 작업은
	 * aiTaskExecutor 로 명시 지정(opt-in)한다.
	 */
	@Bean
	public ThreadPoolTaskExecutor applicationTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

		executor.setCorePoolSize(5);
		executor.setMaxPoolSize(20);
		executor.setQueueCapacity(100);
		executor.setThreadNamePrefix("async-");

		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(30);

		executor.setTaskDecorator(taskDecorator);

		executor.initialize();
		return executor;
	}

	/**
	 * AI 연동 전용 executor. AI 서버 호출(Feign, 최대 60s)처럼 느린 작업만 태운다.
	 * AI 호출은 스레드가 아니라 AI 서버 처리량이 병목이므로 동시성을 낮게(max 8) 잡고
	 * 큐(30)로 흡수한다 — 스레드를 늘려봐야 AI 서버에 동시 요청만 몰려 타임아웃을 유발한다.
	 * 느린 AI 작업이 기본 executor 를 점유해 빠른 이벤트 처리(웹소켓 푸시)를 지연시키는 것을 격리.
	 */
	@Bean
	public ThreadPoolTaskExecutor aiTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

		executor.setCorePoolSize(3);
		executor.setMaxPoolSize(8);
		executor.setQueueCapacity(30);
		executor.setThreadNamePrefix("ai-async-");

		// 유실 불가 정합성 작업(link-sync 등)이므로 드롭 대신 백프레셔 유지
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(30);

		executor.setTaskDecorator(taskDecorator);

		executor.initialize();
		return executor;
	}

	@Override
	public Executor getAsyncExecutor() {
		return applicationTaskExecutor();
	}
}
