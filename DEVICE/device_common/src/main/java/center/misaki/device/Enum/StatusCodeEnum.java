package center.misaki.device.Enum;

/**
 * 状态枚举类
 * @author Misaki
 */
public enum StatusCodeEnum {

    // 20000～30000 预留系统状态
    SUCCESS(20000, "操作成功"),
    NO_EXIST(20001, "用户不存在"),
    LOGOUT(20002, "退出登陆成功"),
    TOKEN_INVALID(20003, "token失效"),
    CONTENT_INVALID(20004, "内容非法"),
    VISIT_INVALID(20005, "访问非法"),
    LOGIN_SUCCESS(20006, "用户登陆成功"),
    LOGIN_FAIL(20007, "用户登陆失败"),
    NOT_LOG_IN(20008, "用户未登陆"),
    INVALID_PARAMS(20010, "无效参数"),
    PARAMS_CHECK_FAILED(20011, "参数检查失败"),

    // 30001～40000 预留业务状态
    NO_DATA(30001, "未查询到数据"),
    REMOVE_NODE_SUCCESS(30100, "删除节点成功"),
    REMOVE_NODE_FAILED(30101, "删除节点失败"),
    REGISTER_SERVICE_SUCCESS(30102, "注册节点成功"),
    REGISTER_SERVICE_FAILED(30103, "注册节点失败"),
    NONE_SERVICES(30104, "未发现任何节点"),
    USER_CREATE(30201, "用户创建成功"),
    USER_CREATE_FAILED(30202, "用户创建失败"),
    USER_INFO_UPDATE_SUCCESS(30203, "用户信息更新成功"),
    USER_INFO_UPDATE_FAILED(30204, "用户信息更新失败"),

    // 40000~50000 业务错误信息
    INVALID_USER_PUBLISH(40001, "操作失败"),
    UPDATE_PUBLISH_FAILED(40003, "更新失败"),
    PUBLISH_FAILED(40005, "发布失败"),
    INVALID_UGC_CATEGORY (40006, "一级分类不正确"),
    INVALID_UGC_CLASSIFY (40007, "二级分类不正确"),
    INVALID_PUBLISH_PARAMS (40008, "自定义数据结构不正确"),
    ACTIVITY_ATTEND_ENOUGH (40100, "活动报名人数已满"),
    INVALID_PUBLISH_REPLY_TYPE (40500, "回复类型不正确"),
    TODAY_CAN_NOT_LOTTERY_BOX (40600, "今日不能再抽取盲盒"),
    RETRY_LOTTERY_BOX (40601, "再试试手气吧");


    private Integer code;
    private String message;

    private StatusCodeEnum(){
    }
    private StatusCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
    public Integer getCode() {
        return this.code;
    }

    public String getMessage() {
        return this.message;
    }

}
