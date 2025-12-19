package npk.rca.ims.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import npk.rca.ims.exceptions.ResourceNotFoundException;
import npk.rca.ims.model.User;
import npk.rca.ims.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * UserService - Business logic for user management
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Find user by email
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Verify user credentials
     * Returns user if credentials are valid, null otherwise
     */
    public User authenticate(String email, String password) {
        if (email == null || password == null) {
            log.warn("Authentication attempt with null email or password");
            return null;
        }
        
        // Trim email to handle whitespace
        email = email.trim().toLowerCase();
        
        Optional<User> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            log.warn("Authentication failed: User not found with email: {}", email);
            return null;
        }
        
        User user = userOpt.get();
        
        // Verify password using BCrypt
        boolean passwordMatches = passwordEncoder.matches(password, user.getPassword());
        if (!passwordMatches) {
            log.warn("Authentication failed: Invalid password for email: {}", email);
            return null;
        }
        
        // Check if user is enabled
        if (!user.isEnabled()) {
            log.warn("Authentication failed: User account is disabled for email: {}", email);
            return null;
        }
        
        log.info("Authentication successful for email: {}", email);
        return user;
    }

    /**
     * Create a new user (for admin use)
     */
    @Transactional
    public User createUser(String email, String password, String role) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("User with email " + email + " already exists");
        }
        
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password)); // Encrypt password
        user.setRole(role != null ? role : "USER");
        user.setEnabled(true);
        
        return userRepository.save(user);
    }

    /**
     * Check if user exists
     */
    public boolean userExists(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Update user profile
     */
    @Transactional
    public User updateProfile(String email, String newEmail, String name, String phone, String department) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        
        // If email is being changed, check if new email already exists
        if (newEmail != null && !newEmail.equals(email)) {
            if (userRepository.existsByEmail(newEmail)) {
                throw new IllegalArgumentException("Email " + newEmail + " is already in use");
            }
            user.setEmail(newEmail.trim().toLowerCase());
        }
        
        // Note: User entity doesn't have name, phone, department fields yet
        // For now, we'll store them in a notes field or extend the entity
        // This is a placeholder - you may want to extend User entity with these fields
        
        return userRepository.save(user);
    }

    /**
     * Change user password
     */
    @Transactional
    public User changePassword(String email, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        
        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        
        // Validate new password
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters long");
        }
        
        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        
        log.info("Password changed successfully for user: {}", email);
        return userRepository.save(user);
    }

    /**
     * Get current user profile
     */
    public User getProfile(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }
}

