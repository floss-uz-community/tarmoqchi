package uz.server.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import uz.server.domain.entity.User;
import uz.server.service.UserService;

@Controller
@RequestMapping("/github")
@RequiredArgsConstructor
@Slf4j
public class GithubOAuthController {
    private final UserService userService;

    @GetMapping("/callback")
    public String handleGithubCallback(@RequestParam("code") String code, Model model) {
        try {
            log.info("Authorizing user with code: {}", code);
            User user = userService.authorizeWithGithub(code);
            model.addAttribute("user", user);
        } catch (Exception e) {
            log.error("Failed to authorize user with code: {}", code);
        }

        log.info("Redirecting to /front/");
        return "index";
    }
}
