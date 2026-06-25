package com.proxio.backend.controllers.forms;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupForm {

    @NotBlank(message = "Full name is required.")
    private String fullName;

    @Email(message = "Please enter a valid email address.")
    @NotBlank(message = "Email is required.")
    private String email;

    @Size(min = 6, message = "Password must contain at least 6 characters.")
    @NotBlank(message = "Password is required.")
    private String password;

    @NotBlank(message = "Account type is required.")
    private String accountType = "CUSTOMER";

    private String farmName;
}
