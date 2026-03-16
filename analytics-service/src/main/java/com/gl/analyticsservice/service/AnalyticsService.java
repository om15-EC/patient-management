package com.gl.analyticsservice.service;

import com.gl.analyticsservice.dto.EnrichedPatientData;
import com.gl.analyticsservice.dto.PatientStatisticsDto;
import com.gl.analyticsservice.model.PatientEventLog;
import com.gl.analyticsservice.repository.PatientEventRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);
    private final PatientEventRepository patientEventRepository;

    public void recordPatientEven(String patientId, String name, String email, String eventType){
        PatientEventLog eventLog = PatientEventLog.builder()
                .patientId(patientId)
                .name(name)
                .email(email)
                .eventType(eventType)
                .build();

        patientEventRepository.save(eventLog);
        log.info("Recorded event: {} for patientId: {}", eventLog, patientId);
    }

    public PatientStatisticsDto getStatistics(){
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        long totalPatients = patientEventRepository.countByEventType("PATIENT_CREATED");
        long todayRegistrations = patientEventRepository
                .countByEventTypeAndCreatedAtBetween("PATIENT_CREATED", startOfToday, now);
        long thisMonthRegistrations = patientEventRepository
                .countByEventTypeAndCreatedAtBetween("PATIENT_CREATED", startOfMonth, now);

        return PatientStatisticsDto.builder()
                .totalPatients(totalPatients)
                .todayRegistrations(todayRegistrations)
                .thisMonthRegistrations(thisMonthRegistrations)
                .build();
    }


    public void recordPatientEven(String patientId, String name, String email, String eventType, String diagnosis, String country, EnrichedPatientData enriched) {

        PatientEventLog eventLog = PatientEventLog.builder()
                .patientId(patientId)
                .name(name)
                .email(email)
                .eventType(eventType)
                .diagnosis(diagnosis)
                .country(country)
                .diseaseDescription(enriched.diseaseInfo().description())
                .countryRegion(enriched.countryInfo().region())
                .countryPopulation(enriched.countryInfo().population())
                .build();

        patientEventRepository.save(eventLog);
        log.info("Recorded enriched event for patientId: {}", patientId);
    }
}
