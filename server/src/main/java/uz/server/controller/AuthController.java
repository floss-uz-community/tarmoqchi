package uz.server.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.server.domain.dto.request.AuthRequest;
import uz.server.service.UserService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final UserService userService;

    @PostMapping
    public void authorizeWithToken(@RequestBody AuthRequest request) {
        userService.authorize(request.getToken());
    }
}
