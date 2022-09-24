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
@EnableEurekaClient
@EnableZuulProxy
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
