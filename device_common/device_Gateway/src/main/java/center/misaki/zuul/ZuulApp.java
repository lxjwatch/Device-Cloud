package center.misaki.zuul;

import center.misaki.zuul.filter.DeviceFilterProcessor;
import com.netflix.zuul.FilterProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.TimeZone;

@SpringBootApplication
@EnableEurekaClient // 开启 EurekaClient 注解，目前版本如果配置了 Eureka 注册中心，默认会开启该注解,即：可不写该行代码
@EnableZuulProxy // 开启 Zuul 注解
public class ZuulApp {


    public static void main(String[] args) {
        SpringApplication.run(ZuulApp.class, args);
        FilterProcessor.setProcessor(new DeviceFilterProcessor());
    }

    @Bean
    public RestTemplate restTemplate(){
        return  new RestTemplate();
    }
    @PostConstruct
    void started() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+8"));
    }
}
