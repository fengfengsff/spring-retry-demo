package com.geeknote.springretry.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * 声明式spring-retry服务
 *
 * @author feng
 * @create 2020-06-16 13:57
 */
@Service
public class DeclarativeRetryService {

	private static final Logger LOG = LoggerFactory.getLogger(DeclarativeRetryService.class);

	// 重试的次数
	private Integer retryCount = 0 ;

	@Retryable(value = Exception.class, maxAttempts = 5,
			backoff = @Backoff(delay = 2000L, multiplier = 2),
			recover = "sendMessageCallBack")
	public boolean sendMessage(String message) {
		// 编写业务逻辑
		retryCount ++ ;
		if (retryCount <= 3) {
			LOG.info("retry count is : {} ", retryCount);
			throw new NullPointerException();
		}
		LOG.info("message send successful，message is : {} !", message);
		return true ;
	}

	@Recover
	public boolean sendMessageCallBack(Exception e) {
		// 编写达到重试最大次数后的业务逻辑
		LOG.error("message send is failure !", e);
		return false ;
	}
}
