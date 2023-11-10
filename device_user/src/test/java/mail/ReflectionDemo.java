package mail;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectionDemo {
    public static void main(String[] args) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchFieldException {

        //正射创建对象
       Member member = new Member();

       //反射创建对象,随便选一个获取class对象的方法
       Class clazz = Member.class;
       Constructor constructor = clazz.getConstructor();
       Member member1 = (Member) constructor.newInstance();

        //有参构造
        Constructor constructor1 = clazz.getConstructor(String.class,String.class);
        Member member2 = (Member) constructor1.newInstance("药面", "19");

        //获取成员变量

        Field name = clazz.getDeclaredField("name");
        Field age = clazz.getDeclaredField("age");

        name.setAccessible(true);
        name.set(member2,"药面222");
        System.out.println(name.get(member2));


    }
}
