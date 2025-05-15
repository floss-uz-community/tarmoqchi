package uz.server.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import uz.server.domain.dto.AuthDTO;
import uz.server.service.UserService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final UserService userService;

    @PostMapping
    public void authorizeWithToken(@RequestBody AuthDTO request) {
        userService.authorizeWithToken(request.token());
    }
}
