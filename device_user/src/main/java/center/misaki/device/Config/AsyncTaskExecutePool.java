
package center.misaki.device.Config;

import center.misaki.device.domain.AsyncTaskProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务线程池装配类
 * @author Misaki
 */
@Slf4j
@Configuration
public class AsyncTaskExecutePool implements AsyncConfigurer {
    
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        //核心线程池大小
        executor.setCorePoolSize(asyncTaskProperties().corePoolSize);
        //最大线程数
        executor.setMaxPoolSize(asyncTaskProperties().maxPoolSize);
        //队列容量
        executor.setQueueCapacity(asyncTaskProperties().queueCapacity);
        //活跃时间
        executor.setKeepAliveSeconds(asyncTaskProperties().keepAliveSeconds);
        //线程名字前缀
        executor.setThreadNamePrefix("device-async-");
        // setRejectedExecutionHandler：当pool已经达到max size的时候，如何处理新任务
        // CallerRunsPolicy：不在新线程中执行任务，而是由调用者所在的线程来执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return new DelegatingSecurityContextExecutorService(executor.getThreadPoolExecutor());
    }

    /**
     * 异步方法执行的过程中抛出的异常捕获
     *
     * @return
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, objects) -> {
            log.error("===="+throwable.getMessage()+"====", throwable);
            log.error("exception method:"+method.getName());
        };
    }

    /**
     * 从配置文件中读取线程池配置
     * @return
     */
    @Bean
    @ConfigurationProperties(prefix = "task.pool")
    public AsyncTaskProperties asyncTaskProperties(){
        return  new AsyncTaskProperties();
    }    
    
}
