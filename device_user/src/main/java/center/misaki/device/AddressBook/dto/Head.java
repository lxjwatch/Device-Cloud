package center.misaki.device.AddressBook.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public  class Head implements Serializable {
        private static final long serialVersionUID = -2L;
        private Integer[] department;
        private Integer[] role;
        private Integer[] user;
}