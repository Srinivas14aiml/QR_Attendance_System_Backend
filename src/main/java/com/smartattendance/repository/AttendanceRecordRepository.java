package com.smartattendance.repository;

import com.smartattendance.model.AttendanceRecord;
import com.smartattendance.model.AttendanceSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    Optional<AttendanceRecord> findBySessionAndRollNumber(AttendanceSession session, String rollNumber);
    Optional<AttendanceRecord> findBySessionAndDeviceFingerprint(AttendanceSession session, String deviceFingerprint);
    List<AttendanceRecord> findBySessionOrderByTimestampDesc(AttendanceSession session);
    long countBySession(AttendanceSession session);
    long countBySessionAndSuspiciousTrue(AttendanceSession session);
    List<AttendanceRecord> findBySessionAndIpAddressAndTimestampAfter(AttendanceSession session, String ipAddress, Instant timestamp);
    List<AttendanceRecord> findBySessionAndDeviceFingerprintAndTimestampAfter(AttendanceSession session, String deviceFingerprint, Instant timestamp);
}
