package com.simformsolutions.appointment.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UserDetailsDto {

    private int userId;
    @Size(min = 5, message = "Please Enter Appropriate Name")
    private String name;
    @Email(message = "Please Enter Valid Email")
    @NotEmpty(message = "Please Enter Email")
    private String email;
    @Size(max = 10, min = 10, message = "Please Enter Appropriate Number")
    @NotEmpty(message = "Please Enter Number")
    private String number;

    @Size(min = 5, message = "Please Enter Password With More Than 5 Letters")
    @NotEmpty(message = "Please Enter Password")
    private String password;

    public UserDetailsDto(String name, String email, String number, String password) {
        this.name = name;
        this.email = email;
        this.number = number;
        this.password = password;
    }
}
