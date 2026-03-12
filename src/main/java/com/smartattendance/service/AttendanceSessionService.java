package com.smartattendance.service;

import com.smartattendance.dto.AttendanceDtos;
import com.smartattendance.model.AttendanceSession;
import com.smartattendance.model.Teacher;
import com.smartattendance.model.User;
import com.smartattendance.model.UserRole;
import com.smartattendance.repository.AttendanceSessionRepository;
import com.smartattendance.repository.TeacherRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class AttendanceSessionService {

    private final AttendanceSessionRepository sessionRepository;
    private final TeacherRepository teacherRepository;

    public AttendanceSessionService(AttendanceSessionRepository sessionRepository, TeacherRepository teacherRepository) {
        this.sessionRepository = sessionRepository;
        this.teacherRepository = teacherRepository;
    }

    public AttendanceSession createSession(AttendanceDtos.CreateSessionRequest request, User user) {
        if (user.getRole() != UserRole.ROLE_TEACHER) {
            throw new SecurityException("Only teachers can create attendance sessions.");
        }
        if (request.subjectName() == null || request.subjectName().isBlank() || request.className() == null || request.className().isBlank()) {
            throw new IllegalArgumentException("Subject name and class name are required.");
        }
        if (request.startAt() == null || request.durationMinutes() == null || request.durationMinutes() <= 0) {
            throw new IllegalArgumentException("Start time and a positive session duration are required.");
        }
        if (request.totalStudents() == null || request.totalStudents() <= 0) {
            throw new IllegalArgumentException("Total students must be greater than zero.");
        }

        Teacher teacher = teacherRepository.findByUser(user)
                .orElseGet(() -> teacherRepository.save(new Teacher(user)));

        AttendanceSession session = new AttendanceSession();
        session.setSubjectName(request.subjectName());
        session.setClassName(request.className());
        session.setTotalStudents(request.totalStudents());
        session.setStartAt(request.startAt());
        session.setDurationMinutes(request.durationMinutes());
        session.setEndAt(request.startAt().plusSeconds(request.durationMinutes() * 60L));
        session.setTeacher(teacher);
        session.setActive(true);
        return sessionRepository.save(session);
    }

    public List<AttendanceSession> listSessions(User user) {
        if (user.getRole() == UserRole.ROLE_TEACHER) {
            Teacher teacher = teacherRepository.findByUser(user)
                    .orElseGet(() -> teacherRepository.save(new Teacher(user)));
            return sessionRepository.findByTeacherOrderByCreatedAtDesc(teacher);
        }
        return sessionRepository.findAllByOrderByCreatedAtDesc();
    }

    public AttendanceSession getById(Long id) {
        AttendanceSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Session not found."));
        refreshActivity(session);
        return session;
    }

    public AttendanceSession findByQrToken(String qrToken) {
        AttendanceSession session = sessionRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid QR session token."));
        refreshActivity(session);
        return session;
    }

    public AttendanceSession endSession(Long id, User user) {
        AttendanceSession session = getById(id);
        validateTeacherOwnership(session, user);
        session.setActive(false);
        session.setEndedAt(Instant.now());
        return sessionRepository.save(session);
    }

    public void ensureSessionAcceptingAttendance(AttendanceSession session) {
        refreshActivity(session);
        if (!session.isActive()) {
            throw new IllegalArgumentException("Attendance session is no longer active.");
        }
        if (Instant.now().isBefore(session.getStartAt())) {
            throw new IllegalArgumentException("Attendance session has not started yet.");
        }
    }

    public void validateTeacherOwnership(AttendanceSession session, User user) {
        if (user.getRole() != UserRole.ROLE_TEACHER) {
            throw new SecurityException("Teacher access is required.");
        }
        if (!session.getTeacher().getUser().getId().equals(user.getId())) {
            throw new SecurityException("You can only manage your own attendance sessions.");
        }
    }

    private void refreshActivity(AttendanceSession session) {
        if (session.getEndedAt() == null && Instant.now().isAfter(session.getEndAt())) {
            session.setEndedAt(session.getEndAt());
            session.setActive(false);
            sessionRepository.save(session);
        }
    }
}
