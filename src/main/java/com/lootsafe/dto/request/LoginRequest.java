package com.lootsafe.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "Name cannot be blank")
        String name,

        @NotBlank(message = "Password cannot be blank")
        String password
) {

}
