package org.example.recommendhouse.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller  // @RestController가 아닌 @Controller 사용
public class AuthViewController {

    @GetMapping("/login")
    public String loginPage() {
        return "login";  // templates/login.html을 찾아 렌더링
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";  // templates/register.html을 찾아 렌더링
    }

    @GetMapping("/first-login-form")
    public String showFirstLoginForm() {
        return "first-login-form";  // templates/first-login-form.html을 찾아 렌더링
    }

}