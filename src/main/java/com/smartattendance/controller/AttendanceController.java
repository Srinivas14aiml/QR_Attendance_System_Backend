package com.smartattendance.controller;

import com.smartattendance.config.ClientIpFilter;
import com.smartattendance.dto.AttendanceDtos;
import com.smartattendance.service.AttendanceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping({"/submit-attendance", "/attendance/submit"})
    public ResponseEntity<AttendanceDtos.AttendanceSubmitResponse> submitAttendance(
            @Valid @RequestBody AttendanceDtos.SubmitAttendanceRequest request,
            HttpServletRequest httpServletRequest
    ) {
        String ipAddress = (String) httpServletRequest.getAttribute(ClientIpFilter.CLIENT_IP_ATTRIBUTE);
        return ResponseEntity.ok(attendanceService.submitAttendance(request, ipAddress));
    }
}
