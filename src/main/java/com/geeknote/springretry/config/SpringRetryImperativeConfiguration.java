package com.geeknote.springretry.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;

/**
 * SpringRetry配置类
 *
 * @author feng
 * @create 2020-06-16 13:55
 */
@Configuration
public class SpringRetryImperativeConfiguration {

	@Bean
	public RetryTemplate retryTemplate() {
		RetryTemplate rt = RetryTemplate.builder()
				.retryOn(Exception.class) // 发生Exception时重试
				.maxAttempts(6) // 最大重试次数为6次
				.fixedBackoff(5000) // 重试间隔时长为5s
				.build() ;
		return rt ;
	}
}
