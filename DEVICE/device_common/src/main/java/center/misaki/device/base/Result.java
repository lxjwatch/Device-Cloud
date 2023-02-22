package center.misaki.device.base;

import center.misaki.device.Enum.StatusCodeEnum;
import lombok.Data;

import java.io.Serializable;

/**
 * @author Misaki
 */
@Data
public class Result<T> implements Serializable {

    public static final Integer SUCCESS = 0;

    public static final Integer ERROR = 1;

    private Integer code;

    private String msg;

    private T data;

    private Result(){}

    public Result(Integer code, T data, String msg){
        this.code = code;
        this.data = data;
        this.msg=msg;
    }

    public Result(Integer code, String msg){
        this.code=code;
        this.msg=msg;
    }

    public Result(StatusCodeEnum statusCodeEnum, T data){
        this.code= statusCodeEnum.getCode();
        this.msg= statusCodeEnum.getMessage();
        this.data=data;
    }
    public Result(StatusCodeEnum statusCodeEnum){
        this.code= statusCodeEnum.getCode();
        this.msg= statusCodeEnum.getMessage();
    }


    public static <T> Result<T> ok(T data,String msg){
        return new Result<T>(SUCCESS, data,msg);
    }

    public static Result error(String msg){
        return new Result(ERROR,null,msg);
    }
    
    public static <T> Result<T> error(T data ,String msg){
        return new Result<T>(ERROR,data,msg);
    }




}
