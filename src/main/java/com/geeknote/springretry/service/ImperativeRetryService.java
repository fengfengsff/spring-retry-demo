package com.geeknote.springretry.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

/**
 * 命令式spring-retry服务
 *
 * @author feng
 * @create 2020-06-16 13:57
 */
@Service
public class ImperativeRetryService {

	private static final Logger LOG = LoggerFactory.getLogger(ImperativeRetryService.class);

	private Integer retryCount = 0 ;

	@Autowired
	private RetryTemplate retryTemplate ;

	public boolean sendMessage(String message)  {
		try {
			return retryTemplate.execute((RetryCallback<Boolean, Exception>) retryContext -> {
				// 编写业务逻辑
				retryCount ++;
				if (retryCount <= 3) {
					LOG.info("retry count is : {}" , retryCount);
					throw new NullPointerException();
				}
				LOG.info("message send successful，message is : {} !", message);
				return true;
			}, retryContext -> {
				// 编写达到重试最大次数后的业务逻辑
				LOG.info("message send aborted !");
				return false;
			});
		} catch (Exception e){
			LOG.error("retryTemplate excute failure",e);
		}
		return false ;
	}
}
