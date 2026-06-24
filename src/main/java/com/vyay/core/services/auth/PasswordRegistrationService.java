package com.vyay.core.services.auth;

import com.vyay.core.common.utils.EmailUtils;
import com.vyay.core.dto.requests.auth.BasePasswordRegisterRequest;
import com.vyay.core.dto.response.Auth.RegisterResponseDTO;
import com.vyay.core.entity.User;
import com.vyay.core.exception.auth.PasswordPolicyViolationException;
import com.vyay.core.repository.UserRepository;
import com.vyay.core.security.JwtService;
import com.vyay.core.security.PasswordPolicy;
import com.vyay.core.services.notification.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PasswordRegistrationService {

    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final JwtService jwtService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final List<PasswordUserRegistrationHandler<? extends BasePasswordRegisterRequest>> handlers;

    private final boolean skipEmailVerification;

    public PasswordRegistrationService(PasswordEncoder passwordEncoder,
                                       PasswordPolicy passwordPolicy,
                                       JwtService jwtService,
                                       NotificationService notificationService,
                                       UserRepository userRepository,
                                       List<PasswordUserRegistrationHandler<? extends BasePasswordRegisterRequest>> handlers,
                                       @Value("${app.auth.skip-email-verification:false}") boolean skipEmailVerification) {
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.jwtService = jwtService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.handlers = handlers;
        this.skipEmailVerification = skipEmailVerification;
    }

    @SuppressWarnings("unchecked")
    public <T extends BasePasswordRegisterRequest> RegisterResponseDTO register(T request) {
        PasswordUserRegistrationHandler<T> handler = (PasswordUserRegistrationHandler<T>) handlers.stream()
                .filter(h -> h.supports().isAssignableFrom(request.getClass()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No registration handler for " + request.getClass().getSimpleName()));

        List<String> violations = passwordPolicy.validate(request.getPassword());
        if (!violations.isEmpty()) {
            throw new PasswordPolicyViolationException(String.join(", ", violations));
        }

        String email = EmailUtils.normalize(request.getEmail());

        String hash = passwordEncoder.encode(request.getPassword());
        User user = handler.createUser(request, hash);

        if (skipEmailVerification) {
            user.setEmailVerified(true);
            userRepository.save(user);
        } else {
            String verificationToken = jwtService.generateEmailVerificationToken(user.getId());

            notificationService.send(
                    "EMAIL_VERIFICATION",
                    "EMAIL",
                    email,
                    Map.of(
                            "firstName", user.getFirstName(),
                            "link", verificationToken
                    )
            );
        }

        return RegisterResponseDTO.builder()
                .email(email)
                .message(skipEmailVerification
                        ? "User registered and auto-verified (dev mode)."
                        : "Verification email sent. Please check your inbox.")
                .build();
    }
}