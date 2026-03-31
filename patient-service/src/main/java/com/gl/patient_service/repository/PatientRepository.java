package com.gl.patient_service.repository;

import com.gl.patient_service.model.Patient;
import com.gl.patient_service.model.PatientStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {

    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, UUID id);

    Page<Patient> findAll(Pageable pageable);
    Page<Patient> findByDiagnosisAndStatus(
            String diagnosis, PatientStatus status, Pageable pageable);
    Page<Patient> findByDiagnosis(String diagnosis, Pageable pageable);
    Page<Patient> findByStatus(PatientStatus status, Pageable pageable);
}
