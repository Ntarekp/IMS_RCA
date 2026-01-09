package npk.rca.ims.service;

import npk.rca.ims.exceptions.ResourceNotFoundException;
import npk.rca.ims.model.User;
import npk.rca.ims.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setEnabled(true);
        testUser.setRole("USER");
    }

    @Test
    void findByEmail_ShouldReturnUser_WhenUserExists() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        Optional<User> result = userService.findByEmail("test@example.com");

        assertTrue(result.isPresent());
        assertEquals("test@example.com", result.get().getEmail());
    }

    @Test
    void authenticate_ShouldReturnUser_WhenCredentialsAreValid() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);

        User result = userService.authenticate("test@example.com", "password");

        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void authenticate_ShouldReturnNull_WhenUserNotFound() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        User result = userService.authenticate("unknown@example.com", "password");

        assertNull(result);
    }

    @Test
    void authenticate_ShouldReturnNull_WhenPasswordIsInvalid() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        User result = userService.authenticate("test@example.com", "wrongPassword");

        assertNull(result);
    }

    @Test
    void authenticate_ShouldReturnNull_WhenUserIsDisabled() {
        testUser.setEnabled(false);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);

        User result = userService.authenticate("test@example.com", "password");

        assertNull(result);
    }

    @Test
    void createUser_ShouldCreateUser_WhenEmailIsUnique() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.createUser("new@example.com", "password", "ADMIN");

        assertNotNull(result);
        assertEquals("new@example.com", result.getEmail());
        assertEquals("ADMIN", result.getRole());
        assertTrue(result.isEnabled());
    }

    @Test
    void createUser_ShouldThrowException_WhenEmailAlreadyExists() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> 
            userService.createUser("test@example.com", "password", "USER"));
    }

    @Test
    void updateProfile_ShouldUpdateUser_WhenUserExists() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.updateProfile(
                "test@example.com",
                "test@example.com",
                "New Name",
                "1234567890",
                "IT",
                "HQ",
                true,
                true,
                false,
                "dark",
                "en"
        );

        assertEquals("New Name", result.getName());
        assertEquals("IT", result.getDepartment());
    }

    @Test
    void updateProfile_ShouldThrowException_WhenUserNotFound() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> 
            userService.updateProfile("unknown@example.com", null, null, null, null, null, null, null, null, null, null));
    }
    
    @Test
    void changePassword_ShouldUpdatePassword_WhenCurrentPasswordIsCorrect() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.changePassword("test@example.com", "oldPassword", "newPassword");

        assertEquals("newEncodedPassword", result.getPassword());
    }

    @Test
    void changePassword_ShouldThrowException_WhenCurrentPasswordIsIncorrect() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> 
            userService.changePassword("test@example.com", "wrongPassword", "newPassword"));
    }
}
