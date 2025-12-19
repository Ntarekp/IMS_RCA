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
        Optional<User> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            return null;
        }
        
        User user = userOpt.get();
        
        // Verify password using BCrypt
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return null;
        }
        
        // Check if user is enabled
        if (!user.isEnabled()) {
            return null;
        }
        
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
}

