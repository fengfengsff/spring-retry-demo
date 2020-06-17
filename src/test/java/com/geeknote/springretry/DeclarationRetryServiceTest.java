package com.geeknote.springretry;

import com.geeknote.springretry.service.DeclarativeRetryService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * 测试声明式Spring-retry
 *
 * @author feng
 * @create 2020-06-16 14:26
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class DeclarationRetryServiceTest {

	@Autowired
	DeclarativeRetryService declarationRetryService ;

	@Test
	public void testSendMessage() {
		String message = "hello world" ;
		boolean result = this.declarationRetryService.sendMessage(message);
		Assert.assertTrue(result);
	}
}
