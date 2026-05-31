package com.annasuraksha.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminDashboardController {

    @GetMapping("/admin")
    public String adminHome() {
        return "redirect:/admin/ledger.html";
    }

    @GetMapping("/admin/ledger")
    public String adminLedger() {
        return "redirect:/admin/ledger.html";
    }
}