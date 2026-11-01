package gmbh.tantive.jsonplaceholder_crud.album;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import gmbh.tantive.jsonplaceholder_crud.photo.Photo;
import gmbh.tantive.jsonplaceholder_crud.photo.PhotoService;
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

@RestController
@RequestMapping("/albums")
@Tag(name = "Albums")
@SecurityRequirement(name = "bearerAuth")
public class AlbumController {

    private final AlbumService albumService;
    private final PhotoService photoService;

    public AlbumController(AlbumService albumService, PhotoService photoService) {
        this.albumService = albumService;
        this.photoService = photoService;
    }

    @GetMapping
    @Operation(summary = "List albums, optionally filtered by user")
    public List<Album> findAll(@RequestParam(required = false) Long userId,
            @RequestParam(name = "_start", defaultValue = "0") int start,
            @RequestParam(name = "_limit", defaultValue = "100") int limit) {
        return albumService.findByUserId(userId, new CollectionPage(start, limit));
    }

    @GetMapping("/{id:\\d+}")
    @Operation(summary = "Get an album by ID")
    public Album findById(@PathVariable long id) {
        return albumService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an album")
    public Album create(@Valid @RequestBody Album album) {
        return albumService.create(album);
    }

    @PutMapping("/{id:\\d+}")
    @Operation(summary = "Replace an album")
    public Album replace(@PathVariable long id, @Valid @RequestBody Album album) {
        return albumService.replace(id, album);
    }

    @PatchMapping("/{id:\\d+}")
    @Operation(summary = "Partially update an album")
    public Album update(@PathVariable long id, @RequestBody Album changes) {
        return albumService.update(id, changes);
    }

    @DeleteMapping("/{id:\\d+}")
    @Operation(summary = "Delete an album")
    public Map<String, Object> delete(@PathVariable long id) {
        albumService.delete(id);
        return Map.of();
    }

    @GetMapping("/{id:\\d+}/photos")
    @Operation(summary = "List photos for an album")
    public List<Photo> photos(@PathVariable long id,
            @RequestParam(name = "_start", defaultValue = "0") int start,
            @RequestParam(name = "_limit", defaultValue = "100") int limit) {
        return photoService.findByAlbumId(id, new CollectionPage(start, limit));
    }
}