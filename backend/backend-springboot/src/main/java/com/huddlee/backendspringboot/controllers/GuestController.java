package com.huddlee.backendspringboot.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GuestController {

    @GetMapping("/create-account")
    public String createAccount() {
        // create a random username, save it in the temp memory, and this can be used in Interceptor etc.
        // Also issue a small time JWT token
        return "create-account";
    }

}
