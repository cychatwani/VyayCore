package com.splitEasy.core.services.profile;

import com.splitEasy.core.dto.requests.profile.UpdateProfileRequestDTO;
import com.splitEasy.core.entity.User;
import com.splitEasy.core.entity.UserProfile;
import com.splitEasy.core.entity.reference.Currency;
import com.splitEasy.core.entity.reference.Language;
import com.splitEasy.core.exception.business.InvalidCurrencyException;
import com.splitEasy.core.exception.business.InvalidLanguageException;
import com.splitEasy.core.exception.business.ProfileAlreadyExistsException;
import com.splitEasy.core.exception.business.ProfileNotFoundException;
import com.splitEasy.core.repository.CurrencyRepository;
import com.splitEasy.core.repository.LanguageRepository;
import com.splitEasy.core.repository.UserProfileRepository;
import com.splitEasy.core.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final CurrencyRepository currencyRepository;
    private final LanguageRepository languageRepository;
    private final UserRepository userRepository;

    public UserProfileService(UserProfileRepository userProfileRepository,
                              CurrencyRepository currencyRepository,
                              LanguageRepository languageRepository,
                              UserRepository userRepository) {
        this.userProfileRepository = userProfileRepository;
        this.currencyRepository = currencyRepository;
        this.languageRepository = languageRepository;
        this.userRepository = userRepository;
    }

    public boolean hasProfile(UUID userId) {
        return userProfileRepository.existsById(userId);
    }

    public UserProfile getProfile(UUID userId) {
        return userProfileRepository.findById(userId)
                .orElseThrow(ProfileNotFoundException::new);
    }

    public UserProfile updateProfile(User user, UpdateProfileRequestDTO request) {
        UserProfile profile = userProfileRepository.findById(user.getId())
                .orElseThrow(ProfileNotFoundException::new);

        if (request.getDefaultCurrency() != null) {
            Currency currency = currencyRepository.findByCode(request.getDefaultCurrency())
                    .orElseThrow(() -> new InvalidCurrencyException(request.getDefaultCurrency()));
            profile.setDefaultCurrency(currency);
        }

        if (request.getDefaultLanguage() != null) {
            Language language = languageRepository.findByCode(request.getDefaultLanguage())
                    .orElseThrow(() -> new InvalidLanguageException(request.getDefaultLanguage()));
            profile.setLanguage(language);
        }

        if (request.getPreferences() != null) {
            profile.setPreferences(request.getPreferences());
        }

        return userProfileRepository.save(profile);
    }

    public UserProfile createProfile(User user, String currencyCode, String languageCode) {
        if (userProfileRepository.existsById(user.getId())) {
            throw new ProfileAlreadyExistsException();
        }

        Currency currency = currencyRepository.findByCode(currencyCode)
                .orElseThrow(() -> new InvalidCurrencyException(currencyCode));

        Language language = languageRepository.findByCode(languageCode)
                .orElseThrow(() -> new InvalidLanguageException(languageCode));

        User managedUser = userRepository.getReferenceById(user.getId());

        UserProfile profile = UserProfile.builder()
                .user(managedUser)
                .defaultCurrency(currency)
                .language(language)
                .build();

        return userProfileRepository.save(profile);
    }
}
