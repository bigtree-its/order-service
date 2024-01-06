package com.bigtree.order.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException{
    private String title;
    private String message;
    private HttpStatus status;

    public ApiException(HttpStatus status, String title, String message){
        super(message);
        this.title = title;
        this.status = status;
        this.message = message;
    }

}
