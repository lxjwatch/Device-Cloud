package center.misaki.test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Misaki
 */

public class JsonTest {
    @Test
    public void testJ(){
        String str = "{\"typeId\":\"0\",\"check\":{},\"title\":\"联系人\",\"type\":\"string\",\"fieldId\":\"0_24l9ZPuMRLQ-qzW19Yw\"}";
        System.out.println(str.replace("0_24l9ZPuMRLQ-qzW19Yw","123456"));
    } 
}
