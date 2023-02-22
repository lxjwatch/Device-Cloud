package center.misaki.device.Enum;

public enum WorkLogEnum {
    WAIT("待办",0),
    ORIGIN("我发起的",1),
    SOLVED("我处理的",2),
    COPY("抄送我的",3);
    
    public String name;
    public Integer value;
    
    WorkLogEnum(String name,Integer value) {
        this.name=name;
        this.value=value;
    }
    
}
