package com.smartattendance.service;

import com.smartattendance.dto.AttendanceDtos;
import com.smartattendance.model.AttendanceRecord;
import com.smartattendance.model.AttendanceSession;
import com.smartattendance.repository.AttendanceRecordRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class AiService {

    private final AttendanceRecordRepository recordRepository;
    private final CampusNetworkService campusNetworkService;

    public AiService(AttendanceRecordRepository recordRepository, CampusNetworkService campusNetworkService) {
        this.recordRepository = recordRepository;
        this.campusNetworkService = campusNetworkService;
    }

    public RiskAssessment assessRisk(AttendanceSession session, String ipAddress, String deviceFingerprint) {
        int score = 0;
        List<String> reasons = new ArrayList<>();
        Instant recentWindow = Instant.now().minus(90, ChronoUnit.SECONDS);

        List<AttendanceRecord> ipMatches = recordRepository.findBySessionAndIpAddressAndTimestampAfter(session, ipAddress, recentWindow);
        if (ipMatches.size() >= 3) {
            score += 40;
            reasons.add("High submission velocity from same IP");
        }

        List<AttendanceRecord> deviceMatches = recordRepository.findBySessionAndDeviceFingerprintAndTimestampAfter(session, deviceFingerprint, recentWindow);
        if (deviceMatches.size() >= 2) {
            score += 60;
            reasons.add("Repeated submissions from same device in short time");
        }

        if (!campusNetworkService.isAllowed(ipAddress)) {
            score += 100;
            reasons.add("Request originated outside campus network");
        }

        return new RiskAssessment(score, !reasons.isEmpty(), String.join("; ", reasons));
    }

    public List<AttendanceDtos.SuspiciousPatternResponse> detectSuspiciousPatterns(AttendanceSession session) {
        List<AttendanceDtos.SuspiciousPatternResponse> patterns = new ArrayList<>();
        recordRepository.findBySessionOrderByTimestampDesc(session).stream()
                .filter(AttendanceRecord::isSuspicious)
                .limit(5)
                .forEach(record -> patterns.add(new AttendanceDtos.SuspiciousPatternResponse(
                        "submission",
                        record.getRollNumber(),
                        (long) record.getRiskScore(),
                        record.getSuspicionReason() == null || record.getSuspicionReason().isBlank()
                                ? "Suspicious submission flagged"
                                : record.getSuspicionReason()
                )));
        return patterns;
    }

    public AttendanceDtos.ProxyCheckResponse inspectCurrentRequest(String ipAddress) {
        boolean inCampus = campusNetworkService.isAllowed(ipAddress);
        return new AttendanceDtos.ProxyCheckResponse(
                ipAddress,
                inCampus,
                !inCampus,
                inCampus ? "Campus network validated" : "Outside configured campus IP ranges"
        );
    }

    public record RiskAssessment(int score, boolean suspicious, String reason) {
    }
}
