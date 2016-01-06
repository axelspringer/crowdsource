package de.asideas.crowdsource.controller;

import de.asideas.crowdsource.domain.exception.InvalidRequestException;
import de.asideas.crowdsource.presentation.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseBody
@ControllerAdvice
public class ControllerExceptionAdvice {

    @ExceptionHandler(InvalidRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidRequest(InvalidRequestException e) {
        return new ErrorResponse(e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleConstraintViolations(MethodArgumentNotValidException e) {

        ErrorResponse errorResponse = new ErrorResponse("field_errors");
        // field errors
        e.getBindingResult()
                .getFieldErrors()
                .forEach(fieldError -> errorResponse.addConstraintViolation(fieldError.getField(), fieldError.getDefaultMessage()));
        // class level errors
        e.getBindingResult().getGlobalErrors().forEach(globalError -> errorResponse.addConstraintViolation("global", globalError.getDefaultMessage()));
        return errorResponse;
    }
}
