package com.lootsafe.security;

import com.lootsafe.dto.request.SignupRequest;
import com.lootsafe.repository.PasswordMatches;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, SignupRequest> {

    @Override
    public boolean isValid(SignupRequest dto, ConstraintValidatorContext context) {
        return dto.getPassword() != null &&
                dto.getPassword().equals(dto.getConfirmPassword());
    }
}