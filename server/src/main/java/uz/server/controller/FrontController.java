package uz.server.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.view.RedirectView;
import uz.server.domain.entity.User;

@Controller
@RequestMapping("/front")
public class FrontController {
    @GetMapping("/")
    public String index() {
        return "index";
    }

    @Value("${github.client-id}")
    private String clientId;

    @Value("${github.redirect-uri}")
    private String redirectUri;

    @GetMapping("/github")
    public RedirectView redirectToGithub() {
        return new RedirectView(String.format(
                "https://github.com/login/oauth/authorize?client_id=%s&redirect_uri=%s&scope=user",
                clientId,
                redirectUri
        ));
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}
