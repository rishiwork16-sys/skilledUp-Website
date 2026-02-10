package com.skilledup.course.exception;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AmazonServiceException.class)
    public ResponseEntity<Object> handleAmazonServiceException(AmazonServiceException ex, WebRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("message", "AWS S3 Error: " + ex.getMessage());
        body.put("errorCode", ex.getErrorCode());
        body.put("statusCode", ex.getStatusCode());
        
        System.err.println("AWS S3 Error: " + ex.getErrorMessage());
        ex.printStackTrace();

        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(SdkClientException.class)
    public ResponseEntity<Object> handleSdkClientException(SdkClientException ex, WebRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("message", "AWS Client Error: " + ex.getMessage());
        
        System.err.println("AWS Client Error: " + ex.getMessage());
        ex.printStackTrace();

        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Object> handleMaxSizeException(MaxUploadSizeExceededException exc) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("message", "File too large!");
        return new ResponseEntity<>(body, HttpStatus.EXPECTATION_FAILED);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAllExceptions(Exception ex, WebRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("message", ex.getMessage());
        body.put("error", "Internal Server Error");
        
        System.err.println("Global Exception: " + ex.getMessage());
        ex.printStackTrace();
        
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
