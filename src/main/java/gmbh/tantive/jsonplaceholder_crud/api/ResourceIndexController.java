package gmbh.tantive.jsonplaceholder_crud.api;

import java.util.List;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityRequirement(name = "bearerAuth")
public class ResourceIndexController {

    @GetMapping("/")
    public List<String> resources() {
        return List.of("posts", "comments", "albums", "photos", "todos", "users");
    }
}