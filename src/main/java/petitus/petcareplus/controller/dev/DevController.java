package petitus.petcareplus.controller.dev;

import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.*;

import petitus.petcareplus.controller.BaseController;

import petitus.petcareplus.dto.request.auth.CreateAdminRequest;

import petitus.petcareplus.dto.response.user.UserResponse;
import petitus.petcareplus.model.User;

import petitus.petcareplus.service.AdminService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/dev")
public class DevController extends BaseController {

    private final AdminService adminService;

    @PostMapping("/create-admin")
    @Operation(summary = "Create new admin", description = "API để admin tạo admin mới")
    public ResponseEntity<UserResponse> createAdmin(@RequestBody @Valid final CreateAdminRequest request)
            throws BindException {
        User newAdmin = adminService.createAdmin(request);
        return ResponseEntity.ok(UserResponse.convert(newAdmin));
    }

    // Add debug endpoint
    @GetMapping("/debug/redis-config")
    public ResponseEntity<Map<String, Object>> checkRedisConfig() {
        Map<String, Object> config = new HashMap<>();

        config.put("REDIS_PUBLIC_URL", System.getenv("REDIS_PUBLIC_URL"));
        config.put("REDISHOST", System.getenv("REDISHOST"));
        config.put("REDISPORT", System.getenv("REDISPORT"));
        config.put("REDISPASSWORD", System.getenv("REDISPASSWORD") != null ? "***SET***" : "NOT_SET");

        return ResponseEntity.ok(config);
    }
}
