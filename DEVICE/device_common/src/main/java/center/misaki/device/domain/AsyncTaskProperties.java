package center.misaki.device.domain;

import lombok.Data;

/**
 * 线程池配置属性类
 * @author Misaki
 */
@Data
public class AsyncTaskProperties {
    public int corePoolSize=2;
    public int maxPoolSize=10;
    public int keepAliveSeconds=120;
    public int queueCapacity=20;
}