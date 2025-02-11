package com.eduva.eduva.service;


import com.eduva.eduva.model.UserData;
import com.eduva.eduva.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Optional<UserData> registerUser(String userName, String email, String password) {
        if (userRepository.findByEmail(email).isPresent()) {
            return Optional.empty(); // Email already exists
        }
        if (userRepository.findByEmail(email).isPresent()) {
            return Optional.empty(); // Email already exists
        }
        UserData user = new UserData();
        user.setUserName(userName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        return Optional.of(userRepository.save(user));
    }

    public Optional<UserData> loginUser(String email, String password) {
        // Find user by email
        Optional<UserData> userOptional = userRepository.findByEmail(email);

        // Check if user exists and password matches
        if (userOptional.isPresent() &&
                passwordEncoder.matches(password, userOptional.get().getPassword())) {
            return userOptional;
        }

        // Return empty if login fails (wrong email or password)
        return Optional.empty();
    }

    // You might also want to add this utility method
    public boolean isValidPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }


}
