package com.geeknote.springretry;

import com.geeknote.springretry.service.ImperativeRetryService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * 测试Spring-retry
 *
 * @author feng
 * @create 2020-06-16 14:26
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class ImperativeRetryServiceTest {

	@Autowired
	ImperativeRetryService imperativeRetryService ;

	@Test
	public void testSendMessage() {
		String message = "hello world";
		boolean result = this.imperativeRetryService.sendMessage(message);
		Assert.assertTrue(result);
	}
}
