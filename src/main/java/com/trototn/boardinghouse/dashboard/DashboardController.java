package com.trototn.boardinghouse.dashboard;

import com.trototn.boardinghouse.auth.domain.User;
import com.trototn.boardinghouse.common.dto.Responses;
import com.trototn.boardinghouse.auth.repository.UserRepository;
import com.trototn.boardinghouse.dashboard.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;
    private final UserRepository userRepository;

    public DashboardController(DashboardService dashboardService, UserRepository userRepository) {
        this.dashboardService = dashboardService;
        this.userRepository = userRepository;
    }

    @GetMapping("/landlord")
    @PreAuthorize("hasRole('LANDLORD')")
    public Map<String, Object> landlordDashboard(Principal principal) {
        User landlord = userRepository.findByEmail(principal.getName()).orElseThrow();
        Responses.LandlordDashboard dashboard = dashboardService.landlordDashboard(landlord);
        return Map.of("dashboard", dashboard);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> adminDashboard() {
        Responses.AdminDashboard dashboard = dashboardService.adminDashboard();
        return Map.of("dashboard", dashboard);
    }

    @GetMapping("/backup")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> backup() {
        return ResponseEntity.ok(dashboardService.backup());
    }
}
