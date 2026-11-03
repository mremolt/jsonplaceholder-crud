package gmbh.tantive.jsonplaceholder_crud.api;

import java.util.List;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import gmbh.tantive.jsonplaceholder_crud.shared.ResourceBulkService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class ResourceIndexController {

    private final List<ResourceBulkService<?>> resources;

    @GetMapping("/")
    public List<String> resources() {
        return resources.stream().map(ResourceBulkService::resourceName).toList();
    }
}