package center.misaki.device.Enum;

/**
 * @author Misaki
 * 员工状态枚举类
 */
public enum UserStateEnum {
    NO_ARRIVE("未到岗"),
    READY("在岗"),
    DEAD("离职");
    
    private final String name;

    UserStateEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
