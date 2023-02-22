package center.misaki.device;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.annotation.PostConstruct;
import java.util.TimeZone;

/**
 * @author Misaki
 */
@SpringBootApplication
@EnableEurekaClient
@EnableTransactionManagement
@EnableAsync
@EnableFeignClients
@EnableAspectJAutoProxy//开启注解格式AOP功能
public class DataApp {
    public static void main(String[] args) {
        SpringApplication.run(DataApp.class, args);
    }

    @PostConstruct
    void started() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+8"));
    }
}
