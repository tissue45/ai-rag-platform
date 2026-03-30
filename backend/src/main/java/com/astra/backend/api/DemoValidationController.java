package com.astra.backend.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/demo")
public class DemoValidationController {

    public record DemoRequest(
            @NotBlank(message = "name은 필수입니다.")
            @Size(max = 50, message = "name은 50자 이하여야 합니다.")
            String name
    ) {}

    @PostMapping("/validate")
    public Map<String, Object> validate(@Valid @RequestBody DemoRequest req) {
        return Map.of("ok", true);
    }
}

