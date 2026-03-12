package com.smartattendance.service;

import com.smartattendance.dto.AttendanceDtos;
import com.smartattendance.model.AttendanceRecord;
import com.smartattendance.model.AttendanceSession;
import com.smartattendance.model.Student;
import com.smartattendance.repository.AttendanceRecordRepository;
import com.smartattendance.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AttendanceService {

    private final AttendanceRecordRepository recordRepository;
    private final StudentRepository studentRepository;
    private final AttendanceSessionService sessionService;
    private final CampusNetworkService campusNetworkService;
    private final AiService aiService;

    public AttendanceService(
            AttendanceRecordRepository recordRepository,
            StudentRepository studentRepository,
            AttendanceSessionService sessionService,
            CampusNetworkService campusNetworkService,
            AiService aiService
    ) {
        this.recordRepository = recordRepository;
        this.studentRepository = studentRepository;
        this.sessionService = sessionService;
        this.campusNetworkService = campusNetworkService;
        this.aiService = aiService;
    }

    public AttendanceDtos.AttendanceSubmitResponse submitAttendance(AttendanceDtos.SubmitAttendanceRequest request, String ipAddress) {
        if (request.qrToken() == null || request.qrToken().isBlank()) {
            throw new IllegalArgumentException("QR session token is required.");
        }
        if (request.studentName() == null || request.studentName().isBlank()) {
            throw new IllegalArgumentException("Student name is required.");
        }
        if (request.rollNumber() == null || request.rollNumber().isBlank()) {
            throw new IllegalArgumentException("Roll number is required.");
        }
        if (request.deviceFingerprint() == null || request.deviceFingerprint().isBlank()) {
            throw new IllegalArgumentException("Device fingerprint is required.");
        }
        if (!campusNetworkService.isAllowed(ipAddress)) {
            throw new SecurityException("Attendance can only be submitted inside campus network.");
        }

        AttendanceSession session = sessionService.findByQrToken(request.qrToken());
        sessionService.ensureSessionAcceptingAttendance(session);

        if (recordRepository.findBySessionAndRollNumber(session, request.rollNumber()).isPresent()) {
            throw new IllegalArgumentException("Attendance already submitted.");
        }

        recordRepository.findBySessionAndDeviceFingerprint(session, request.deviceFingerprint())
                .ifPresent(existing -> {
                    if (!existing.getRollNumber().equalsIgnoreCase(request.rollNumber())) {
                        throw new SecurityException("Multiple attendance attempts detected.");
                    }
                });

        Student student = studentRepository.findByRollNumber(request.rollNumber())
                .map(existing -> updateStudent(existing, request.studentName()))
                .orElseGet(() -> studentRepository.save(new Student(request.studentName(), request.rollNumber(), null)));

        AiService.RiskAssessment risk = aiService.assessRisk(session, ipAddress, request.deviceFingerprint());

        AttendanceRecord record = new AttendanceRecord();
        record.setSession(session);
        record.setStudent(student);
        record.setStudentName(student.getFullName());
        record.setRollNumber(student.getRollNumber());
        record.setIpAddress(ipAddress);
        record.setDeviceFingerprint(request.deviceFingerprint());
        record.setSuspicious(risk.suspicious());
        record.setRiskScore(risk.score());
        record.setSuspicionReason(risk.reason());

        AttendanceRecord saved = recordRepository.save(record);
        return new AttendanceDtos.AttendanceSubmitResponse(
                risk.suspicious() ? "Attendance submitted and flagged for review." : "Attendance submitted successfully.",
                toRecordResponse(saved),
                getStats(session)
        );
    }

    public AttendanceDtos.SessionAttendanceResponse getSessionAttendance(AttendanceSession session) {
        List<AttendanceDtos.AttendanceRecordResponse> records = recordRepository.findBySessionOrderByTimestampDesc(session)
                .stream()
                .map(this::toRecordResponse)
                .toList();

        return new AttendanceDtos.SessionAttendanceResponse(
                toSessionResponse(session),
                getStats(session),
                records,
                aiService.detectSuspiciousPatterns(session)
        );
    }

    public AttendanceDtos.SessionStatsResponse getStats(AttendanceSession session) {
        long presentStudents = recordRepository.countBySession(session);
        return new AttendanceDtos.SessionStatsResponse(
                session.getId(),
                session.getTotalStudents(),
                presentStudents,
                Math.max(0, session.getTotalStudents() - presentStudents),
                recordRepository.countBySessionAndSuspiciousTrue(session),
                session.isActive()
        );
    }

    public AttendanceDtos.AttendanceRecordResponse toRecordResponse(AttendanceRecord record) {
        return new AttendanceDtos.AttendanceRecordResponse(
                record.getId(),
                record.getStudentName(),
                record.getRollNumber(),
                record.getTimestamp(),
                record.getIpAddress(),
                record.getDeviceFingerprint(),
                record.isSuspicious(),
                record.getRiskScore(),
                record.getSuspicionReason()
        );
    }

    public AttendanceDtos.SessionResponse toSessionResponse(AttendanceSession session) {
        return new AttendanceDtos.SessionResponse(
                session.getId(),
                session.getSubjectName(),
                session.getClassName(),
                session.getTotalStudents(),
                session.getStartAt(),
                session.getEndAt(),
                session.getDurationMinutes(),
                session.getQrToken(),
                session.isActive(),
                session.getEndedAt(),
                session.getCreatedAt(),
                session.getTeacher().getUser().getFullName()
        );
    }

    private Student updateStudent(Student student, String fullName) {
        student.setFullName(fullName);
        return studentRepository.save(student);
    }
}
