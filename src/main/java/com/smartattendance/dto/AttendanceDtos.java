package com.smartattendance.dto;

import java.time.Instant;
import java.util.List;

public final class AttendanceDtos {

    private AttendanceDtos() {
    }

    public record CreateSessionRequest(
            String subjectName,
            String className,
            Instant startAt,
            Integer durationMinutes,
            Integer totalStudents
    ) {
    }

    public record SessionResponse(
            Long id,
            String subjectName,
            String className,
            Integer totalStudents,
            Instant startAt,
            Instant endAt,
            Integer durationMinutes,
            String qrToken,
            boolean active,
            Instant endedAt,
            Instant createdAt,
            String teacherName
    ) {
    }

    public record GenerateQrResponse(
            Long sessionId,
            String qrBase64,
            String qrToken,
            String attendanceUrl,
            Instant expiresAt
    ) {
    }

    public record StudentSessionResponse(
            Long sessionId,
            String subjectName,
            String className,
            Instant startAt,
            Instant endAt,
            boolean active,
            String teacherName
    ) {
    }

    public record SubmitAttendanceRequest(
            String qrToken,
            String studentName,
            String rollNumber,
            String deviceFingerprint
    ) {
    }

    public record AttendanceRecordResponse(
            Long id,
            String studentName,
            String rollNumber,
            Instant timestamp,
            String ipAddress,
            String deviceFingerprint,
            boolean suspicious,
            Integer riskScore,
            String suspicionReason
    ) {
    }

    public record SessionStatsResponse(
            Long sessionId,
            Integer totalStudents,
            long presentStudents,
            long absentStudents,
            long suspiciousSubmissions,
            boolean active
    ) {
    }

    public record SuspiciousPatternResponse(
            String type,
            String value,
            long count,
            String detail
    ) {
    }

    public record SessionAttendanceResponse(
            SessionResponse session,
            SessionStatsResponse stats,
            List<AttendanceRecordResponse> records,
            List<SuspiciousPatternResponse> suspiciousPatterns
    ) {
    }

    public record AttendanceSubmitResponse(
            String message,
            AttendanceRecordResponse record,
            SessionStatsResponse stats
    ) {
    }

    public record ProxyCheckResponse(
            String ip,
            boolean inCampusNetwork,
            boolean suspiciousProxyPattern,
            String reason
    ) {
    }
}
