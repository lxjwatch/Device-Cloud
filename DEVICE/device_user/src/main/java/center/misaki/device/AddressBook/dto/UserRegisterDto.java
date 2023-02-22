package center.misaki.device.AddressBook.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class UserRegisterDto {
    @NotNull
    private String username;

    @NotNull
    private String password;
}
