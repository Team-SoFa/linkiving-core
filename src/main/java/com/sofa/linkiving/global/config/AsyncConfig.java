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

	@Override
	public Executor getAsyncExecutor() {
		return applicationTaskExecutor();
	}
}
