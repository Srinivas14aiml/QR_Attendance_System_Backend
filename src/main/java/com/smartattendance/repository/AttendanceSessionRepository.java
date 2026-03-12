package com.smartattendance.repository;

import com.smartattendance.model.AttendanceSession;
import com.smartattendance.model.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {
    Optional<AttendanceSession> findByQrToken(String qrToken);
    List<AttendanceSession> findByTeacherOrderByCreatedAtDesc(Teacher teacher);
    List<AttendanceSession> findAllByOrderByCreatedAtDesc();
}
