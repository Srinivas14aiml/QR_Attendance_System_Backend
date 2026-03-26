package com.smartattendance.controller;

import com.smartattendance.dto.AttendanceDtos;
import com.smartattendance.model.AttendanceSession;
import com.smartattendance.security.CustomUserDetails;
import com.smartattendance.service.AttendanceService;
import com.smartattendance.service.AttendanceSessionService;
import com.smartattendance.util.QrCodeGenerator;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final AttendanceSessionService sessionService;
    private final AttendanceService attendanceService;

    public SessionController(
            AttendanceSessionService sessionService,
            AttendanceService attendanceService
    ) {
        this.sessionService = sessionService;
        this.attendanceService = attendanceService;
    }

    // ✅ CREATE SESSION
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    @PostMapping
    public ResponseEntity<AttendanceDtos.SessionResponse> createSession(
            @Valid @RequestBody AttendanceDtos.CreateSessionRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        AttendanceSession session = sessionService.createSession(request, userDetails.getUser());
        return ResponseEntity.ok(attendanceService.toSessionResponse(session));
    }

    // ✅ LIST SESSIONS
    @GetMapping
    public ResponseEntity<List<AttendanceDtos.SessionResponse>> listSessions(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<AttendanceDtos.SessionResponse> sessions = sessionService
                .listSessions(userDetails.getUser())
                .stream()
                .map(attendanceService::toSessionResponse)
                .toList();

        return ResponseEntity.ok(sessions);
    }

    // ✅ STUDENT SESSION (QR SCAN)
    @GetMapping("/student/{token}")
    public ResponseEntity<AttendanceDtos.StudentSessionResponse> getStudentSession(
            @PathVariable String token
    ) {
        AttendanceSession session = sessionService.findByQrToken(token);

        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(
                new AttendanceDtos.StudentSessionResponse(
                        session.getId(),
                        session.getSubjectName(),
                        session.getClassName(),
                        session.getStartAt(),
                        session.getEndAt(),
                        session.isActive(),
                        session.getTeacher().getUser().getFullName()
                )
        );
    }

    // ✅ GENERATE QR (UPDATED WITH YOUR VERCEL URL)
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    @GetMapping("/{id}/qr")
    public ResponseEntity<AttendanceDtos.GenerateQrResponse> generateQr(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        AttendanceSession session = sessionService.getById(id);

        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        sessionService.validateTeacherOwnership(session, userDetails.getUser());

        // 🔥 YOUR UPDATED URL
        String token = session.getQrToken();
        String attendanceUrl = "https://qr-attendance-frontend.vercel.app/?page=student&token=" + token;

        String base64Qr = QrCodeGenerator.toBase64Png(attendanceUrl, 320, 320);

        return ResponseEntity.ok(
                new AttendanceDtos.GenerateQrResponse(
                        session.getId(),
                        base64Qr,
                        token,
                        attendanceUrl,
                        session.getEndAt()
                )
        );
    }

    // ✅ GET ATTENDANCE
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    @GetMapping("/{id}/attendance")
    public ResponseEntity<AttendanceDtos.SessionAttendanceResponse> getSessionAttendance(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        AttendanceSession session = sessionService.getById(id);

        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        sessionService.validateTeacherOwnership(session, userDetails.getUser());

        return ResponseEntity.ok(attendanceService.getSessionAttendance(session));
    }

    // ✅ END SESSION
    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    @PostMapping("/{id}/end")
    public ResponseEntity<AttendanceDtos.SessionAttendanceResponse> endSession(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        AttendanceSession session = sessionService.endSession(id, userDetails.getUser());

        return ResponseEntity.ok(attendanceService.getSessionAttendance(session));
    }
}
