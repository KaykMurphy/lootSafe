package com.lootsafe.exception;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {

        ErrorResponse errorResponse = ErrorResponse.of(
                ex.getMessage(),
                "ILLEGAL_STATE",
                List.of()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {

        List<FieldErrorResponse> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorResponse(
                        error.getField(),
                        error.getDefaultMessage()
                ))
                .toList();

        List<FieldErrorResponse> globalErrors = ex.getBindingResult().getGlobalErrors().stream()
                .map(error -> new FieldErrorResponse(
                       error.getObjectName(),
                       error.getDefaultMessage()
                   ))
                    .toList();

        List<FieldErrorResponse> allErrors = new ArrayList<>(fieldErrors);
        allErrors.addAll(globalErrors);

        ErrorResponse errorResponse = ErrorResponse.of("Validation failed", "VALIDATION_ERROR", allErrors);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {

        ErrorResponse errorResponse = new ErrorResponse(
                ex.getMessage(),
                "NOT_FOUND",
                List.of()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {

        ErrorResponse errorResponse = ErrorResponse.of(
                ex.getMessage(),
                "ACCESS_DENIED",
                List.of()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedOperation(UnsupportedOperationException ex) {

        ErrorResponse errorResponse = ErrorResponse.of(
                ex.getMessage(),
                "NOT_IMPLEMENTED",
                List.of()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_IMPLEMENTED);
    }

    @ExceptionHandler(PaymentProviderException.class)
    public ResponseEntity<ErrorResponse> handlePaymentProviderException(PaymentProviderException ex) {

        ErrorResponse errorResponse = ErrorResponse.of(
                ex.getMessage(),
                "PAYMENT_PROVIDER_ERROR",
                List.of()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_GATEWAY);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex){
        ErrorResponse errorResponse = ErrorResponse.of(
                ex.getMessage(),
                "DUPLICATE_RESOURCE",
                List.of()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex){
        ErrorResponse errorResponse = ErrorResponse.of(
                "Credenciais inválidas",
                "INVALID_CREDENTIALS",
                List.of()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex){
              ErrorResponse errorResponse = ErrorResponse.of(
                       "Parâmetro inválido: " + ex.getName() + " — valor '" + ex.getValue() + "' não é válido",
                       "INVALID_PARAMETER",
                       List.of()
               );
               return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
           }



}
