package uz.server.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import uz.server.domain.dto.request.AuthRequest;
import uz.server.domain.entity.User;
import uz.server.service.UserService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final UserService userService;

    @PostMapping
    public void authorizeWithToken(@RequestBody AuthRequest request) {
        userService.authorize(request.getToken());
    }

    @GetMapping("/")
    public List<User> getUsers() {
        return userService.getUsers();
    }
}
