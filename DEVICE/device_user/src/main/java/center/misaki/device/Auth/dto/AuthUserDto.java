package center.misaki.device.Auth.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @author Misaki
 */
@Data
public class AuthUserDto {
    @NotNull
    private String username;
    @NotNull
    private String password;
    @NotNull
    private String clientId;
    @NotNull
    private String clientSecret;
}
