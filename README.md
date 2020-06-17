# spring-retry-demo
使用spring-retry实现重试机制

## 缘起

当服务间存在依赖调用或者服务需要请求二方或三方接口等的场景时，往往由于一些外部的原因，例如，网络抖动，服务升级等会导致一个服务请求另一个服务失败。如果这样的外部原因影响到业务或功能，那么就会很费劲的做一些线下补偿措施，这个过程总是耗时耗力，如果在做人为补偿时在出点差错就可能会出现一些不可挽回的事故。

为了避免这样的情况发生，就需要我们自己的服务有一定的自补偿的能力——重试。

## 方案

实现重试请求的方式有多种。比如，最容易想到是，捕获异常，异常发生就不断重试请求直到成功；还可以使用定时任务，将请求的元信息入库，而后，定时轮询重试。但是，显然这两种方式有很多缺点，例如：

* 什么异常情况下重试？
* 最大重试多少次？
* 每次重试的时间间隔如何？
* 如果二方或三方服务一直请求失败？

很显然以上很容易造成系统资源浪费，并且重试不够平滑友好。期望的是可以自定义重试策略，可以适应满足不同的业务场景。

通过调研目前用的比较多的是Spring的spring-retry和Google出品的guava-retrying两种重试方案。并且这两种方案也分别提供了以上期望可以自定义重试策略。

本篇以Spring的spring-retry为主来说明该组件的使用。

[ref：Github spring-retry](https://github.com/spring-projects/spring-retry)

## 实施

示例以springboot框架为前提进行演示。

**添加依赖：**

spring-retry的实现原理是动态代理，因此需要aop的支持。

```
<dependency>
	<groupId>org.springframework.retry</groupId>
	<artifactId>spring-retry</artifactId>
	<version>1.3.0</version>
</dependency>

<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

spring-retry 分为声明式和命令式两种方式可以实现重试机制。

### 声明式

使用@EnableRetry注解开启重试机制。

使用@Retryable注解定义在需要重试的方法或者类上，如果定义在类上那么类中的所有方法都会有重试的机制，如果定义在方法上，那么只有该方法会有重试的功能。

使用@Recover定义重试最终失败后的回调方法，跟重试方法定义在同一个类中，需要第一个参数是重试方法的异常并且返回值需要跟重试方法保持一致。

**@Retryable的常用属性：**

* recover：定义重试达到最大请求次数后的回调方法，可以做后续的业务处理。
* value：抛出指定异常时重试；
* maxAttempts：最大重试次数，默认为3次；
* backoff：重试策略，默认为@Backoff，其有多个属性：
    * delay：延时时长；
    * maxDelay：最大延时时长；
    * multiplier：延时系数，例如，delay=2，multiplier=2，那么首次延时为2s，第二次延时2×2=4s，第三次延时4×2=8s；
    * random：默认为false，如果为true，那么在定义delay和maxDelay后，每次重试会在两者之间随机取一个数最为本次的延时时长。

声明式实现方式如下：

**配置类，启用重试机制：**

```
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.support.RetryTemplate;

/**
 * SpringRetry配置类
 *
 * @author feng
 * @create 2020-06-16 13:55
 */
@EnableRetry
@Configuration
public class SpringRetryImperativeConfiguration {

}
```

**定义重试的业务类和方法：**

```
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
			LOG.info("retry count is : {} !", retryCount);
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
```

**注意：** 如果没有定义recover，那么在达到最大的重试依然没有成功时，会抛出异常结束。

**测试类：**

```
import com.geeknote.tool.service.DeclarativeRetryService;
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
```

控制台输出：

```
2020-06-17 11:47:59.182  INFO 2340 --- [           main] c.g.t.service.DeclarativeRetryService    : retry count is : 1 !
2020-06-17 11:48:01.185  INFO 2340 --- [           main] c.g.t.service.DeclarativeRetryService    : retry count is : 2 !
2020-06-17 11:48:05.186  INFO 2340 --- [           main] c.g.t.service.DeclarativeRetryService    : retry count is : 3 !
2020-06-17 11:48:13.187  INFO 2340 --- [           main] c.g.t.service.DeclarativeRetryService    : message send successful，message is : hello world !
```
count为1应该是第一次正常的业务执行，重第2次开始才算是重试。

从日志打印时间来看，第一次重试是2s，第二次是4s，第三次是8s重试成功。

### 命令式

在spring-retry的1.3版本之后，增加了RetryTemplate，可以统一定义多个需要重试的方法的重试策略。

**配置类，定义RetryTemplate：**

```
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
```

**定义重试的业务类和方法：**

```
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
```

**测试类：**

```
import com.geeknote.tool.service.ImperativeRetryService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * 测试命令式Spring-retry
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
```

**控制台输出：**

```
2020-06-17 12:03:19.781  INFO 11268 --- [           main] c.g.tool.service.ImperativeRetryService  : retry count is : 1
2020-06-17 12:03:24.783  INFO 11268 --- [           main] c.g.tool.service.ImperativeRetryService  : retry count is : 2
2020-06-17 12:03:29.784  INFO 11268 --- [           main] c.g.tool.service.ImperativeRetryService  : retry count is : 3
2020-06-17 12:03:34.784  INFO 11268 --- [           main] c.g.tool.service.ImperativeRetryService  : message send successful，message is : hello world !
```

## 总结

以上，就是本篇主要说明的以spring-retry来实现的重试机制，不管是声明式注解@Retrybale还是命令式的RetryTemplate使用起来非常简单，各有特点，声明式适合为各个需要重试的方法做个性化设置；RetryTemplate适合通用的重试策略。

重试主要在于一个服务调用另一个服务，首次调用发生异常导致服务调用失败。重试其实也是一种补偿机制，往往是外部原因引起，比如网络，或三方服务不可用等。

能够重试的前提是发生请求的异常必须是明确的，防止非幂等引发的业务事故。
