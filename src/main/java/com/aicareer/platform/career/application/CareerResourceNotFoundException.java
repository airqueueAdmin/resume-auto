package com.aicareer.platform.career.application;

public class CareerResourceNotFoundException extends RuntimeException {

    public CareerResourceNotFoundException() {
        super("Career resource was not found");
    }
}
