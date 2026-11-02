package gmbh.tantive.jsonplaceholder_crud.photo;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import gmbh.tantive.jsonplaceholder_crud.shared.CollectionPage;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/photos")
@Tag(name = "Photos")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class PhotoController {

    private final PhotoService photoService;

    @GetMapping
    @Operation(summary = "List photos, optionally filtered by album")
    public List<Photo> findAll(@RequestParam(required = false) Long albumId,
            @RequestParam(name = "_start", defaultValue = "0") int start,
            @RequestParam(name = "_limit", defaultValue = "100") int limit) {
        return photoService.findByAlbumId(albumId, new CollectionPage(start, limit));
    }

    @GetMapping("/{id:\\d+}")
    @Operation(summary = "Get a photo by ID")
    public Photo findById(@PathVariable long id) {
        return photoService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a photo")
    public Photo create(@Valid @RequestBody Photo photo) {
        return photoService.create(photo);
    }

    @PutMapping("/{id:\\d+}")
    @Operation(summary = "Replace a photo")
    public Photo replace(@PathVariable long id, @Valid @RequestBody Photo photo) {
        return photoService.replace(id, photo);
    }

    @PatchMapping("/{id:\\d+}")
    @Operation(summary = "Partially update a photo")
    public Photo update(@PathVariable long id, @RequestBody Photo changes) {
        return photoService.update(id, changes);
    }

    @DeleteMapping("/{id:\\d+}")
    @Operation(summary = "Delete a photo")
    public Map<String, Object> delete(@PathVariable long id) {
        photoService.delete(id);
        return Map.of();
    }
}