package petitus.petcareplus.controller.dev;

import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
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
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired(required = false)
    private RedisConnectionFactory redisConnectionFactory;

    @PostMapping("/create-admin")
    @Operation(summary = "Create new admin", description = "API để admin tạo admin mới")
    public ResponseEntity<UserResponse> createAdmin(@RequestBody @Valid final CreateAdminRequest request)
            throws BindException {
        User newAdmin = adminService.createAdmin(request);
        return ResponseEntity.ok(UserResponse.convert(newAdmin));
    }

    // Add debug endpoint
    @GetMapping("/redis-config")
    public ResponseEntity<Map<String, Object>> checkRedisConfig() {
        Map<String, Object> config = new HashMap<>();

        // Check environment variables
        config.put("REDIS_PUBLIC_URL", System.getenv("REDIS_PUBLIC_URL"));
        config.put("REDISHOST", System.getenv("REDISHOST"));
        config.put("REDISPORT", System.getenv("REDISPORT"));
        config.put("REDISPASSWORD", System.getenv("REDISPASSWORD") != null ? "***SET***" : "NOT_SET");

        // Check Spring properties
        config.put("spring.data.redis.url", System.getProperty("spring.data.redis.url"));
        config.put("spring.data.redis.host", System.getProperty("spring.data.redis.host"));
        config.put("spring.data.redis.port", System.getProperty("spring.data.redis.port"));

        // Check Redis connection
        try {
            if (redisConnectionFactory != null) {
                RedisConnection connection = redisConnectionFactory.getConnection();
                connection.ping();
                connection.close();
                config.put("redis_connection", "SUCCESS");
            } else {
                config.put("redis_connection", "RedisConnectionFactory is NULL");
            }
        } catch (Exception e) {
            config.put("redis_connection", "FAILED: " + e.getMessage());
            config.put("redis_error_class", e.getClass().getSimpleName());
        }

        return ResponseEntity.ok(config);
    }

    @GetMapping("/redis-test")
    public ResponseEntity<Map<String, Object>> testRedis() {
        Map<String, Object> result = new HashMap<>();

        try {
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set("test-key-" + System.currentTimeMillis(), "test-value",
                        Duration.ofMinutes(1));
                result.put("redis_template", "SUCCESS");
            } else {
                result.put("redis_template", "RedisTemplate is NULL");
            }
        } catch (Exception e) {
            result.put("redis_template", "FAILED: " + e.getMessage());
            result.put("redis_error_details", e.getCause() != null ? e.getCause().getMessage() : "No cause");
        }

        return ResponseEntity.ok(result);
    }
}
