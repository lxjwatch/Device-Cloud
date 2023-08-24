package mail;

import center.misaki.device.Mail.MailServiceImpl;
import freemarker.template.TemplateException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;

/**
 * @author Misaki
 */
public class TestSend {

    public void handlerConsumer(Integer number, Consumer<Integer> consumer){
        consumer.accept(number);
    }

    @Test
    public void test1(){
        this.handlerConsumer(10000, (i) -> System.out.println(i));
    }


    @Test
    public void test() throws TemplateException, IOException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, ClassNotFoundException {

        Member member = new Member();
        Class aClass1 = member.getClass();
        String name = aClass1.getName();
        System.out.println(name);

        //反射
        Class<Member> memberClass = Member.class;
        Method method = memberClass.getMethod("getName");
        Constructor constructor = memberClass.getConstructor();
        Object object = constructor.newInstance();
        Object invoke = method.invoke(object);





    }
    
    @Test
    public void SendTome() throws TemplateException, IOException, InterruptedException {
        Map<String, Object> map = new HashMap<>();
        map.put("name","Misaki");
        map.put("code","123456");
        MailServiceImpl mailService = new MailServiceImpl(new FreeMarkerConfigurer());
        mailService.asyncSendTemplateMail("1926653120@qq.com","验证码",map, "code.ftl");
        Thread.sleep(10000);
    }
    public boolean lemonadeChange(String str) {
        int count_five = 0;
        int[] arr = Arrays.stream(str.split(" ")).mapToInt(Integer::parseInt).toArray();
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == 5) {
                count_five++;
            } else if (arr[i] == 10) {
                if (count_five > 0) {
                    count_five--;
                } else {
                    return false;
                }
            } else if (arr[i] == 20) {
                if (count_five >= 1 && (arr[i] - 5) <= (count_five * 5)) {
                    count_five--;
                    if (arr[i] - 5 > 0) {
                        count_five -= (arr[i] - 5) / 5;
                    }
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    @Test
    public void TConn() throws TemplateException, IOException, InterruptedException {

        Optional<Member> optional = getMemberByIdFromDB();

    }
    public static Optional<Member> getMemberByIdFromDB() {
        boolean hasName = true;
        if (hasName) {
            return Optional.of(new Member());
        }
        return Optional.empty();
    }
}

class Member {
    private String name;

    private String age;
    public Member(String name, String age) {
        this.name = name;
        this.age = age;
    }

    public Member() {
    }

    public String test1(){
        return "test1";
    }

    public String test2(String a){
        return a;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }










}



