package SafeRoute.service;

import SafeRoute.dto.request.LoginRequest;
import SafeRoute.dto.request.RegisterRequest;
import SafeRoute.dto.response.AuthResponse;
import SafeRoute.entity.User;
import SafeRoute.enums.Role;
import SafeRoute.exception.EmailAlreadyExistsException;
import SafeRoute.exception.InvalidCredentialsException;
import SafeRoute.repository.UserRepository;
import SafeRoute.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .fullName("John Doe")
                .email("john@example.com")
                .password("securePassword123")
                .phoneNumber("1234567890")
                .build();

        loginRequest = LoginRequest.builder()
                .email("john@example.com")
                .password("securePassword123")
                .build();

        testUser = User.builder()
                .id(1L)
                .fullName("John Doe")
                .email("john@example.com")
                .passwordHash("$2a$10$hashedPasswordHere")
                .phoneNumber("1234567890")
                .role(Role.USER)
                .build();
    }

    @Test
    @DisplayName("Should successfully register new user with hashed password and USER role")
    void testRegisterSuccess() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("securePassword123")).thenReturn("$2a$10$hashedPasswordHere");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtTokenProvider.generateToken("john@example.com", 1L, "USER")).thenReturn("mocked-jwt-token");

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("mocked-jwt-token", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(1L, response.getId());
        assertEquals("john@example.com", response.getEmail());
        assertEquals("John Doe", response.getFullName());
        assertEquals(Role.USER, response.getRole());

        verify(passwordEncoder).encode("securePassword123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw EmailAlreadyExistsException on duplicate email registration")
    void testRegisterDuplicateEmail() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> authService.register(registerRequest));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should successfully login with valid credentials")
    void testLoginSuccess() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("securePassword123", testUser.getPasswordHash())).thenReturn(true);
        when(jwtTokenProvider.generateToken("john@example.com", 1L, "USER")).thenReturn("mocked-jwt-token");

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("mocked-jwt-token", response.getToken());
        assertEquals(1L, response.getId());
        assertEquals("john@example.com", response.getEmail());
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when password does not match")
    void testLoginIncorrectPassword() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", testUser.getPasswordHash())).thenReturn(false);

        LoginRequest wrongLogin = LoginRequest.builder()
                .email("john@example.com")
                .password("wrongPassword")
                .build();

        assertThrows(InvalidCredentialsException.class, () -> authService.login(wrongLogin));
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when email not found")
    void testLoginUserNotFound() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        LoginRequest unknownLogin = LoginRequest.builder()
                .email("unknown@example.com")
                .password("somePassword")
                .build();

        assertThrows(InvalidCredentialsException.class, () -> authService.login(unknownLogin));
    }
}

