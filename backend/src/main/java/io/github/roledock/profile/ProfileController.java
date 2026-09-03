package io.github.roledock.profile;

import io.github.roledock.profile.ProfileDtos.Response;
import io.github.roledock.profile.ProfileDtos.SaveRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
class ProfileController {
    private final ProfileService service;

    ProfileController(ProfileService service) {
        this.service = service;
    }

    @GetMapping
    ResponseEntity<Response> getCurrentProfile() {
        return service.getCurrentProfile().map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping
    Response saveCurrentProfile(@Valid @RequestBody SaveRequest request) {
        return service.saveCurrentProfile(request);
    }
}
