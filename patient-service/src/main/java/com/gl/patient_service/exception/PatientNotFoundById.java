package com.gl.patient_service.exception;

public class PatientNotFoundById extends RuntimeException {

    public PatientNotFoundById(String message) {
        super(message);
    }
}
