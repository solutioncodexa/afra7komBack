package com.afra7kom.backend.controller;

import com.afra7kom.backend.service.AdminService;
import com.afra7kom.backend.service.UserService;
import com.afra7kom.backend.service.StockService;
import com.afra7kom.backend.service.ReservationService;
import com.afra7kom.backend.dto.UserDto;
import com.afra7kom.backend.dto.RegisterRequest;
import com.afra7kom.backend.dto.ReservationResponseDto;
import com.afra7kom.backend.dto.MaterielRequest;
import com.afra7kom.backend.dto.StockDto;
import com.afra7kom.backend.entity.Role;
import com.afra7kom.backend.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "Admin management endpoints")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;
    private final StockService stockService;
    private final ReservationService reservationService;

    public AdminController(AdminService adminService, UserService userService, StockService stockService, ReservationService reservationService) {
        this.adminService = adminService;
        this.userService = userService;
        this.stockService = stockService;
        this.reservationService = reservationService;
    }

    @GetMapping("/dashboard/stats")
    @Operation(summary = "Get dashboard statistics", description = "Get overall statistics for admin dashboard")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // Get statistics from service
        stats.put("totalUsers", adminService.getTotalUsers());
        stats.put("monthlyReservations", adminService.getMonthlyReservations());
        stats.put("monthlyRevenue", adminService.getMonthlyRevenue());
        stats.put("availableEquipments", adminService.getAvailableEquipments());
        
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/dashboard/detailed-stats")
    @Operation(summary = "Get detailed dashboard statistics", description = "Get comprehensive statistics including monthly revenue and pack revenue")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Detailed statistics retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<Map<String, Object>> getDetailedDashboardStats() {
        Map<String, Object> stats = adminService.getDetailedDashboardStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/dashboard/monthly-revenue")
    @Operation(summary = "Get monthly revenue statistics")
    public ResponseEntity<List<Map<String, Object>>> getMonthlyRevenueStats() {
        List<Map<String, Object>> stats = adminService.getMonthlyRevenueStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/dashboard/pack-revenue")
    @Operation(summary = "Get pack revenue statistics")
    public ResponseEntity<List<Map<String, Object>>> getPackRevenueStats() {
        List<Map<String, Object>> stats = adminService.getPackRevenueStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/dashboard/total-users")
    @Operation(summary = "Get total users count")
    public ResponseEntity<Map<String, Object>> getTotalUsers() {
        Map<String, Object> response = new HashMap<>();
        response.put("totalUsers", adminService.getTotalUsers());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/dashboard/monthly-reservations")
    @Operation(summary = "Get monthly reservations count")
    public ResponseEntity<Map<String, Object>> getMonthlyReservations() {
        Map<String, Object> response = new HashMap<>();
        response.put("monthlyReservations", adminService.getMonthlyReservations());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/dashboard/available-equipments")
    @Operation(summary = "Get available equipments count")
    public ResponseEntity<Map<String, Object>> getAvailableEquipments() {
        Map<String, Object> response = new HashMap<>();
        response.put("availableEquipments", adminService.getAvailableEquipments());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/dashboard/most-profitable-product")
    @Operation(summary = "Get most profitable product this month")
    public ResponseEntity<Map<String, Object>> getMostProfitableProduct() {
        Map<String, Object> product = adminService.getMostProfitableProduct();
        return ResponseEntity.ok(product);
    }

    @GetMapping("/dashboard/popular-days-of-week")
    @Operation(summary = "Get popular days of week for reservations")
    public ResponseEntity<List<Map<String, Object>>> getPopularDaysOfWeek() {
        List<Map<String, Object>> days = adminService.getPopularDaysOfWeek();
        return ResponseEntity.ok(days);
    }

    @GetMapping("/dashboard/approval-stats-by-month")
    @Operation(summary = "Get approval statistics by month (approved vs rejected)")
    public ResponseEntity<List<Map<String, Object>>> getApprovalStatsByMonth() {
        List<Map<String, Object>> stats = adminService.getApprovalStatsByMonth();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/test")
    @Operation(summary = "Test endpoint")
    public ResponseEntity<Map<String, Object>> test() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "AdminController is working!");
        response.put("timestamp", new java.util.Date());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/public-test")
    @Operation(summary = "Public test endpoint")
    public ResponseEntity<Map<String, Object>> publicTest() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "AdminController public endpoint is working!");
        response.put("timestamp", new java.util.Date());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/cors-test")
    @Operation(summary = "CORS test endpoint")
    public ResponseEntity<Map<String, Object>> corsTest(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "CORS is working correctly!");
        response.put("timestamp", new java.util.Date());
        response.put("receivedData", body);
        response.put("method", "POST");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users")
    @Operation(summary = "Get users for admin", description = "Retrieve paginated list of users with role filter")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Page<UserDto>> getUsers(
            @Parameter(description = "Role filter") @RequestParam(required = false) Role.RoleName role,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<UserDto> users;
        
        if (role != null) {
            users = userService.getUsersByRole(role, pageable);
        } else {
            users = userService.getAllUsers(pageable);
        }

        return ResponseEntity.ok(users);
    }

    @GetMapping("/equipments")
    @Operation(summary = "Get equipments for admin", description = "Retrieve paginated list of equipments with category filter")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Equipments retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Page<StockDto>> getEquipments(
            @Parameter(description = "Category filter") @RequestParam(required = false) String category,
            @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {

        // Utiliser le service de stock pour récupérer les matériels
        Page<StockDto> equipments = stockService.getEquipmentsWithPagination(category, pageable);
        return ResponseEntity.ok(equipments);
    }

    @PostMapping(value = "/equipments", consumes = {"multipart/form-data"})
    @Operation(summary = "Create new equipment", description = "Create a new equipment/materiel with images")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Equipment created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<StockDto> createEquipment(
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam("type") String type,
            @RequestParam("price") Double price,
            @RequestParam(value = "quantity", required = false) Integer quantity,
            @RequestParam(value = "minimumStock", required = false) Integer minimumStock,
            @RequestParam(value = "packPrice", required = false) Double packPrice,
            @RequestParam(value = "selectedMaterials", required = false) List<Long> selectedMaterials,
            @RequestParam(value = "alwaysAvailable", required = false) Boolean alwaysAvailable,
            @RequestParam(value = "images", required = false) List<org.springframework.web.multipart.MultipartFile> images) {
        
        // Créer l'objet MaterielRequest
        MaterielRequest request = new MaterielRequest();
        request.setName(name);
        request.setDescription(description);
        request.setType(type);
        request.setPrice(price != null ? java.math.BigDecimal.valueOf(price) : null);
        request.setQuantity(quantity);
        request.setMinimumStock(minimumStock);
        request.setPackPrice(packPrice != null ? java.math.BigDecimal.valueOf(packPrice) : null);
        request.setSelectedMaterials(selectedMaterials);
        request.setAlwaysAvailable(alwaysAvailable);
        request.setActive(true); // Par défaut actif
        request.setCategorieId(1L); // Catégorie par défaut
        
        StockDto createdEquipment = stockService.createEquipment(request, images);
        return ResponseEntity.status(201).body(createdEquipment);
    }

    @GetMapping("/reservations")
    @Operation(summary = "Get reservations for admin", description = "Retrieve paginated list of reservations")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reservations retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Page<ReservationResponseDto>> getReservations(
            @Parameter(description = "Status filter") @RequestParam(required = false) String status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<ReservationResponseDto> reservations;
        
        if (status != null && !status.trim().isEmpty()) {
            reservations = reservationService.getReservationsByStatus(status.trim(), pageable);
        } else {
            reservations = reservationService.getAllReservations(pageable);
        }

        return ResponseEntity.ok(reservations);
    }

    @GetMapping("/reservations/calendar")
    @Operation(summary = "Get reservations for calendar view by month", description = "Retrieve reservations for a specific month for calendar display")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reservations retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid parameters"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ResponseEntity<List<ReservationResponseDto>> getReservationsForCalendar(
            @Parameter(description = "Year (e.g., 2025)") @RequestParam Integer year,
            @Parameter(description = "Month (1-12)") @RequestParam Integer month) {
        
        if (year == null || month == null || month < 1 || month > 12) {
            return ResponseEntity.badRequest().build();
        }
        
        List<ReservationResponseDto> reservations = reservationService.getReservationsByMonth(year, month);
        return ResponseEntity.ok(reservations);
    }

    @GetMapping("/audit-log")
    @Operation(summary = "Get audit logs", description = "Retrieve paginated list of audit logs")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Audit logs retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Page<Map<String, Object>>> getAuditLogs(
            @Parameter(description = "User ID filter") @RequestParam(required = false) Long userId,
            @Parameter(description = "Action filter") @RequestParam(required = false) String action,
            @Parameter(description = "Status filter") @RequestParam(required = false) String status,
            @Parameter(description = "Start date") @RequestParam(required = false) String startDate,
            @Parameter(description = "End date") @RequestParam(required = false) String endDate,
            @Parameter(description = "Date sort") @RequestParam(defaultValue = "desc") String dateSort,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<Map<String, Object>> auditLogs = adminService.getAuditLogs(userId, dateSort, pageable);
        return ResponseEntity.ok(auditLogs);
    }

    @GetMapping("/audit-log/security")
    @Operation(summary = "Get security audit logs", description = "Retrieve security-related audit logs")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Security audit logs retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Page<Map<String, Object>>> getSecurityAuditLogs(
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<Map<String, Object>> securityLogs = adminService.getSecurityAuditLogs(pageable);
        return ResponseEntity.ok(securityLogs);
    }

    @GetMapping("/audit-log/failures")
    @Operation(summary = "Get failure audit logs", description = "Retrieve failed action audit logs")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Failure audit logs retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Page<Map<String, Object>>> getFailureAuditLogs(
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<Map<String, Object>> failureLogs = adminService.getFailureAuditLogs(pageable);
        return ResponseEntity.ok(failureLogs);
    }

    @GetMapping("/audit-log/stats")
    @Operation(summary = "Get audit statistics", description = "Retrieve audit log statistics")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Audit statistics retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Object> getAuditStats() {
        Object auditStats = adminService.getAuditStats();
        return ResponseEntity.ok(auditStats);
    }

    @GetMapping("/top-clients")
    @Operation(summary = "Get top clients", description = "Retrieve list of top clients by spending")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Top clients retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Object> getTopClients() {
        Object topClients = adminService.getTopClients();
        return ResponseEntity.ok(topClients);
    }

    @GetMapping("/top-packs")
    @Operation(summary = "Get top packs", description = "Retrieve list of top selling packs")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Top packs retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Object> getTopPacks() {
        Object topPacks = adminService.getTopPacks();
        return ResponseEntity.ok(topPacks);
    }

    @GetMapping("/dashboard/product-revenue")
    @Operation(summary = "Get product revenue statistics", description = "Retrieve revenue statistics for all products (packs + equipment)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Product revenue statistics retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Object> getProductRevenueStats() {
        Object productRevenueStats = adminService.getProductRevenueStats();
        return ResponseEntity.ok(productRevenueStats);
    }

    @GetMapping("/revenus")
    @Operation(summary = "Get revenue data", description = "Retrieve revenue data for reporting")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Revenue data retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Object> getRevenueData() {
        Object revenueData = adminService.getRevenueData();
        return ResponseEntity.ok(revenueData);
    }

    // Nouveaux endpoints pour les rapports

    @GetMapping("/reports/popular-equipments")
    @Operation(summary = "Get popular equipments", description = "Retrieve list of most popular equipments")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Popular equipments retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Object> getPopularEquipments() {
        Object popularEquipments = adminService.getPopularEquipments();
        return ResponseEntity.ok(popularEquipments);
    }

    @GetMapping("/reports/reservation-status")
    @Operation(summary = "Get reservation status statistics", description = "Retrieve statistics by reservation status")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reservation status statistics retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Object> getReservationStatusStats() {
        Object statusStats = adminService.getReservationStatusStats();
        return ResponseEntity.ok(statusStats);
    }

    @GetMapping("/reports/growth-stats")
    @Operation(summary = "Get growth statistics", description = "Retrieve growth statistics for revenue, reservations, and users")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Growth statistics retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Object> getGrowthStats() {
        Object growthStats = adminService.getGrowthStats();
        return ResponseEntity.ok(growthStats);
    }

    @GetMapping("/reports/comprehensive")
    @Operation(summary = "Get comprehensive reports", description = "Retrieve all reporting data in one call")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Comprehensive reports retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> getComprehensiveReports() {
        Map<String, Object> reports = new HashMap<>();
        
        reports.put("topClients", adminService.getTopClients());
        reports.put("topPacks", adminService.getTopPacks());
        reports.put("popularEquipments", adminService.getPopularEquipments());
        reports.put("reservationStatusStats", adminService.getReservationStatusStats());
        reports.put("growthStats", adminService.getGrowthStats());
        reports.put("monthlyRevenue", adminService.getMonthlyRevenueStats());
        reports.put("packRevenue", adminService.getPackRevenueStats());
        
        return ResponseEntity.ok(reports);
    }

    @RequestMapping(value = "/reservations/{id}/approve", method = {RequestMethod.POST, RequestMethod.PATCH})
    @Operation(summary = "Approve reservation", description = "Approve a reservation (Admin/Agent only)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reservation approved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid reservation or already processed"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Reservation not found")
    })
    public ResponseEntity<ReservationResponseDto> approveReservation(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "450.00") String depositAmount,
            @RequestParam(required = false, defaultValue = "ESPECE") String paymentMethod,
            @RequestParam(required = false) String notes) {
        
        ReservationResponseDto approvedReservation = reservationService.approveReservation(id, depositAmount, paymentMethod, notes);
        return ResponseEntity.ok(approvedReservation);
    }

    @RequestMapping(value = "/reservations/{id}/reject", method = {RequestMethod.POST, RequestMethod.PATCH})
    @Operation(summary = "Reject reservation", description = "Reject a reservation (Admin/Agent only)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reservation rejected successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid reservation or already processed"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Reservation not found")
    })
    public ResponseEntity<ReservationResponseDto> rejectReservation(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "Réservation rejetée par l'administrateur") String reason) {
        
        ReservationResponseDto rejectedReservation = reservationService.rejectReservation(id, reason);
        return ResponseEntity.ok(rejectedReservation);
    }

    @PostMapping("/stock/fix-daily-rental")
    @Operation(summary = "Fix stock for daily rental", description = "Correct stock quantities for daily rental logic")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stock corrected successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> fixStockForDailyRental(
            @RequestParam(required = false) Long materielId) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (materielId != null) {
            // Corriger un matériel spécifique
            StockDto correctedStock = stockService.fixMaterielStockForDailyRental(materielId);
            response.put("message", "Stock corrected for material: " + correctedStock.getMaterielName());
            response.put("correctedStock", correctedStock);
            response.put("materielId", materielId);
        } else {
            // Corriger tous les matériels
            List<StockDto> correctedStocks = stockService.fixAllMaterielStocksForDailyRental();
            response.put("message", "All stocks corrected for daily rental logic");
            response.put("correctedStocks", correctedStocks);
            response.put("totalCorrected", correctedStocks.size());
        }
        
        response.put("timestamp", new java.util.Date());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/debug/pack-availability/{packId}")
    @Operation(summary = "Debug pack availability", description = "Debug pack availability calculation for a specific date")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Debug information retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> debugPackAvailability(
            @PathVariable Long packId,
            @RequestParam String testDate) {
        
        try {
            java.time.LocalDate date = java.time.LocalDate.parse(testDate);
            Map<String, Object> debugInfo = reservationService.getAvailabilityService().debugPackAvailabilityExample(packId, date);
            return ResponseEntity.ok(debugInfo);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("message", "Erreur lors du debug de disponibilité du pack");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/debug/simulate-reservation/{packId}")
    @Operation(summary = "Simulate reservation impact", description = "Simulate the impact of a reservation on pack availability")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Simulation completed successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> simulateReservationImpact(
            @PathVariable Long packId,
            @RequestParam String testDate,
            @RequestParam Integer quantity) {
        
        try {
            java.time.LocalDate date = java.time.LocalDate.parse(testDate);
            Map<String, Object> simulationResult = reservationService.getAvailabilityService()
                .simulateReservationImpact(packId, date, quantity);
            return ResponseEntity.ok(simulationResult);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("message", "Erreur lors de la simulation d'impact de réservation");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/debug/availability-calculation/{packId}")
    @Operation(summary = "Debug availability calculation", description = "Debug detailed availability calculation for a specific pack and date")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Debug information retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> debugAvailabilityCalculation(
            @PathVariable Long packId,
            @RequestParam String testDate) {
        
        try {
            java.time.LocalDate date = java.time.LocalDate.parse(testDate);
            Map<String, Object> debugInfo = reservationService.getAvailabilityService()
                .debugAvailabilityCalculation(packId, date);
            return ResponseEntity.ok(debugInfo);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("message", "Erreur lors du debug de calcul de disponibilité");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // ========== GESTION DES UTILISATEURS ==========
    
    @PostMapping("/users")
    @Operation(summary = "Create a new user", description = "Create a new user (Admin only)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "User created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid user data"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody Map<String, Object> request) {
        // Convertir en RegisterRequest
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail((String) request.get("email"));
        registerRequest.setPhone((String) request.get("phone"));
        registerRequest.setPassword((String) request.get("password"));
        registerRequest.setConfirmPassword((String) request.get("password")); // Pour les agents, pas de confirmation
        
        // Déterminer le rôle
        String roleStr = (String) request.getOrDefault("role", "CLIENT");
        Role.RoleName roleName;
        try {
            roleName = Role.RoleName.valueOf(roleStr);
        } catch (IllegalArgumentException e) {
            roleName = Role.RoleName.CLIENT;
        }
        
        User user = userService.createUserWithRole(registerRequest, roleName);
        UserDto userDto = userService.convertToDto(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(userDto);
    }

    @PatchMapping("/users/{id}/toggle-status")
    @Operation(summary = "Toggle user status", description = "Enable or disable a user account (Admin only)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User status toggled successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> toggleUserStatus(@PathVariable Long id) {
        User user = userService.toggleUserStatus(id);
        UserDto userDto = userService.convertToDto(user);
        return ResponseEntity.ok(userDto);
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "Delete a user", description = "Delete a user account (Admin only)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "User deleted successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/users/{id}")
    @Operation(summary = "Update user", description = "Update user information (Admin only)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User updated successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) {
        User user = userService.updateUser(id, updates);
        UserDto userDto = userService.convertToDto(user);
        return ResponseEntity.ok(userDto);
    }

}
