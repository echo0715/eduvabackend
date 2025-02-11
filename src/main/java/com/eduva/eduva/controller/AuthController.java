package com.eduva.eduva.controller;


import com.eduva.eduva.dto.LoginRequest;
import com.eduva.eduva.dto.SignupRequest;
import com.eduva.eduva.model.UserData;
import com.eduva.eduva.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
@CrossOrigin(
        origins = {"http://localhost:3000"},
        allowedHeaders = "*",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS},
        allowCredentials = "true"
)

public class AuthController {
    @Autowired
    private UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest signupRequest) {
        Optional<UserData> registeredUser = userService.registerUser(signupRequest.getFullName(), signupRequest.getEmail(), signupRequest.getPassword());


        if (registeredUser.isPresent()) {
            System.out.println("not Yoho");
            return ResponseEntity.ok(registeredUser.get());
        } else {
            System.out.println("Yoho");
            return ResponseEntity.status(409).body("Email already in use");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        Optional<UserData> authenticatedUser = userService.loginUser(
                loginRequest.getEmail(),
                loginRequest.getPassword()
        );

        if (authenticatedUser.isPresent()) {
            return ResponseEntity.ok(authenticatedUser.get());
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid email or password");
        }
    }


}
