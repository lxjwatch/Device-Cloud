package center.misaki.device.Auth.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @author Misaki
 */
@Data
public class WxAuthUserDto {
    @NotNull
    private String code;
    @NotNull
    private String clientId;
    @NotNull
    private String clientSecret;
}
