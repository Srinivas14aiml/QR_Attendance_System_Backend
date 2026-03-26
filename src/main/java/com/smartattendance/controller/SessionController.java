package com.smartattendance.controller;

import com.smartattendance.dto.AttendanceDtos;
import com.smartattendance.model.AttendanceSession;
import com.smartattendance.security.CustomUserDetails;
import com.smartattendance.service.AttendanceService;
import com.smartattendance.service.AttendanceSessionService;
import com.smartattendance.util.QrCodeGenerator;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SessionController {

    private final AttendanceSessionService sessionService;
    private final AttendanceService attendanceService;
    private final String frontendBaseUrl;

    public SessionController(
            AttendanceSessionService sessionService,
            AttendanceService attendanceService,
            @Value("${app.frontend-base-url:https://qr-attendance-system-frontend.vercel.app/}") String frontendBaseUrl
    ) {
        this.sessionService = sessionService;
        this.attendanceService = attendanceService;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    @PostMapping({"/create-session", "/sessions"})
    public ResponseEntity<AttendanceDtos.SessionResponse> createSession(
            @Valid @RequestBody AttendanceDtos.CreateSessionRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        AttendanceSession session = sessionService.createSession(request, userDetails.getUser());
        return ResponseEntity.ok(attendanceService.toSessionResponse(session));
    }

    @GetMapping("/sessions")
    public List<AttendanceDtos.SessionResponse> listSessions(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return sessionService.listSessions(userDetails.getUser()).stream()
                .map(attendanceService::toSessionResponse)
                .toList();
    }

    @GetMapping("/student-session/{token}")
    public AttendanceDtos.StudentSessionResponse getStudentSession(@PathVariable String token) {
        AttendanceSession session = sessionService.findByQrToken(token);
        return new AttendanceDtos.StudentSessionResponse(
                session.getId(),
                session.getSubjectName(),
                session.getClassName(),
                session.getStartAt(),
                session.getEndAt(),
                session.isActive(),
                session.getTeacher().getUser().getFullName()
        );
    }

    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    @GetMapping({"/generate-qr", "/sessions/{id}/qr"})
    public ResponseEntity<AttendanceDtos.GenerateQrResponse> generateQr(
            @RequestParam(required = false) Long sessionId,
            @PathVariable(required = false) Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long targetId = sessionId != null ? sessionId : id;
        AttendanceSession session = sessionService.getById(targetId);
        sessionService.validateTeacherOwnership(session, userDetails.getUser());

        String attendanceUrl = frontendBaseUrl.replaceAll("/+$", "") + "/?page=student&token=" + session.getQrToken();
        String base64 = QrCodeGenerator.toBase64Png(attendanceUrl, 320, 320);
        return ResponseEntity.ok(new AttendanceDtos.GenerateQrResponse(
                session.getId(),
                base64,
                session.getQrToken(),
                attendanceUrl,
                session.getEndAt()
        ));
    }

    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    @GetMapping({"/session-attendance/{id}", "/sessions/{id}/attendance"})
    public AttendanceDtos.SessionAttendanceResponse getSessionAttendance(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        AttendanceSession session = sessionService.getById(id);
        sessionService.validateTeacherOwnership(session, userDetails.getUser());
        return attendanceService.getSessionAttendance(session);
    }

    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    @PostMapping({"/end-session/{id}", "/sessions/{id}/end"})
    public AttendanceDtos.SessionAttendanceResponse endSession(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        AttendanceSession session = sessionService.endSession(id, userDetails.getUser());
        return attendanceService.getSessionAttendance(session);
    }
}
