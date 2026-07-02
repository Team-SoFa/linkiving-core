package com.sofa.linkiving.global.config;

import java.util.concurrent.Executor;

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
		executor.setCorePoolSize(10);
		executor.setMaxPoolSize(50);
		executor.setQueueCapacity(200);
		executor.setThreadNamePrefix("async-");
		executor.setTaskDecorator(taskDecorator);
		executor.initialize();
		return executor;
	}

	@Override
	public Executor getAsyncExecutor() {
		return applicationTaskExecutor();
	}
}
