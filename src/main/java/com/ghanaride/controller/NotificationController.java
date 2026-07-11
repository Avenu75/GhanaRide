package com.ghanaride.controller;

import com.ghanaride.entity.User;
import com.ghanaride.service.NotificationService;
import com.ghanaride.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/notifications")
    public String inbox(Principal principal, Model model,
                        @RequestParam(defaultValue = "0") int page) {
        User u = userService.getCurrentUser(principal);
        model.addAttribute("notifications", notificationService.inbox(u.getId(), page));
        model.addAttribute("unreadCount", notificationService.unreadCount(u.getId()));
        model.addAttribute("currentUser", u);
        return "notifications";
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/api/notifications/read-all")
    @ResponseBody
    public ResponseEntity<?> readAll(Principal principal) {
        User u = userService.getCurrentUser(principal);
        int n = notificationService.markAllRead(u.getId());
        return ResponseEntity.ok(Map.of("marked", n));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/api/notifications/unread-count")
    @ResponseBody
    public ResponseEntity<?> unread(Principal principal) {
        User u = userService.getCurrentUser(principal);
        return ResponseEntity.ok(Map.of("unread", notificationService.unreadCount(u.getId())));
    }
}
