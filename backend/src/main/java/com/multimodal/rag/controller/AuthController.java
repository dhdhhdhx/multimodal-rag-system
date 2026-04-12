package com.multimodal.rag.controller;

import com.multimodal.rag.model.dto.AuthResponse;
import com.multimodal.rag.model.dto.LoginRequest;
import com.multimodal.rag.model.dto.RegisterRequest;
import com.multimodal.rag.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "认证管理", description = "用户注册、登录、令牌刷新等认证相关接口")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(
        summary = "用户注册",
        description = "创建新用户账户，注册成功后返回访问令牌和刷新令牌"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "注册成功", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "用户名已存在或请求参数无效")
    })
    public ResponseEntity<AuthResponse> register(
            @Parameter(description = "注册信息，包含用户名、密码、邮箱等字段")
            @RequestBody RegisterRequest request) {
        log.info("Received registration request for user: {}", request.getUsername());
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    @Operation(
        summary = "用户登录",
        description = "使用用户名和密码登录，登录成功后返回访问令牌和刷新令牌"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "登录成功", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "401", description = "用户名或密码错误")
    })
    public ResponseEntity<AuthResponse> login(
            @Parameter(description = "登录信息，包含用户名和密码")
            @RequestBody LoginRequest request) {
        log.info("Received login request for user: {}", request.getUsername());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(
        summary = "刷新访问令牌",
        description = "使用刷新令牌获取新的访问令牌，刷新令牌有效期7天"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "刷新成功", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "401", description = "刷新令牌无效或已过期")
    })
    public ResponseEntity<?> refreshToken(
            @Parameter(description = "刷新令牌")
            @RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "缺少刷新令牌"));
        }
        try {
            AuthResponse response = authService.refreshAccessToken(refreshToken);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.warn("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(401).body(Map.of("message", e.getMessage()));
        }
    }
}
