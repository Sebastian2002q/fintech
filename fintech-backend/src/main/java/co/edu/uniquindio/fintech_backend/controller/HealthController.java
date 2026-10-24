package co.edu.uniquindio.fintech_backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public String getStatus() {
        return "Fintech Backend funcionando correctamente";
    }
}
