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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /* =========================
       FIND & AUTHENTICATION
       ========================= */

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User authenticate(String email, String password) {
        if (email == null || password == null) {
            log.warn("Authentication attempt with null credentials");
            return null;
        }

        email = email.trim().toLowerCase();

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return null;

        if (!passwordEncoder.matches(password, user.getPassword())) return null;
        if (!user.isEnabled()) return null;

        return user;
    }

    /* =========================
       USER CREATION
       ========================= */

    @Transactional
    public User createUser(String email, String password, String role) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("User with email " + email + " already exists");
        }

        User user = new User();
        user.setEmail(email);
        user.setSystemEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role != null ? role : "USER");
        user.setEnabled(true);

        return userRepository.save(user);
    }

    public boolean userExists(String email) {
        return userRepository.existsByEmail(email);
    }

    /* =====================================================
       PRIMARY updateProfile (USED BY AuthController)
       ===================================================== */

    @Transactional
    public User updateProfile(
            String email,
            String newEmail,
            String name,
            String phone,
            String department,
            String location,
            String avatarUrl,
            String coverUrl,
            Boolean emailNotifications,
            Boolean smsNotifications,
            Boolean twoFactorAuth,
            String theme,
            String language
    ) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found with email: " + email));

        // Email change
        if (newEmail != null && !newEmail.equals(email)) {
            if (userRepository.existsByEmail(newEmail)) {
                throw new IllegalArgumentException("Email " + newEmail + " is already in use");
            }

            if (user.getSystemEmail() == null) {
                user.setSystemEmail(user.getEmail());
            }

            user.setEmail(newEmail.trim().toLowerCase());
        }

        if (name != null) user.setName(name);
        if (phone != null) user.setPhone(phone);
        if (department != null) user.setDepartment(department);
        if (location != null) user.setLocation(location);
        if (avatarUrl != null) user.setAvatarUrl(avatarUrl);
        if (coverUrl != null) user.setCoverUrl(coverUrl);

        if (emailNotifications != null) user.setEmailNotifications(emailNotifications);
        if (smsNotifications != null) user.setSmsNotifications(smsNotifications);
        if (twoFactorAuth != null) user.setTwoFactorAuth(twoFactorAuth);

        if (theme != null) user.setTheme(theme);
        if (language != null) user.setLanguage(language);

        return userRepository.save(user);
    }

    /* =====================================================
       OVERLOADED updateProfile (USED BY UNIT TESTS)
       ===================================================== */

    @Transactional
    public User updateProfile(
            String email,
            String newEmail,
            String name,
            String phone,
            String department,
            String location,
            Boolean notifications,
            Boolean emailAlerts,
            Boolean smsAlerts,
            String theme,
            String language
    ) {
        return updateProfile(
                email,
                newEmail,
                name,
                phone,
                department,
                location,
                null, // avatarUrl
                null, // coverUrl
                emailAlerts != null ? emailAlerts : notifications,
                smsAlerts != null ? smsAlerts : notifications,
                null, // twoFactorAuth
                theme,
                language
        );
    }

    /* =========================
       PASSWORD MANAGEMENT
       ========================= */

    @Transactional
    public User changePassword(String email, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found with email: " + email));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters long");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        return userRepository.save(user);
    }

    @Transactional
    public User resetPassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found with email: " + email));

        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters long");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        return userRepository.save(user);
    }

    /* =========================
       PROFILE FETCH
       ========================= */

    public User getProfile(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found with email: " + email));
    }
}
