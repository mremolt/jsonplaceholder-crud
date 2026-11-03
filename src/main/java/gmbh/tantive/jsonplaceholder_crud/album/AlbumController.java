package gmbh.tantive.jsonplaceholder_crud.album;

import static gmbh.tantive.jsonplaceholder_crud.api.ResourceController.AFTER_DESCRIPTION;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import gmbh.tantive.jsonplaceholder_crud.api.JsonMergePatcher;
import gmbh.tantive.jsonplaceholder_crud.api.ResourceController;
import gmbh.tantive.jsonplaceholder_crud.photo.PhotoResponse;
import gmbh.tantive.jsonplaceholder_crud.photo.PhotoService;
import gmbh.tantive.jsonplaceholder_crud.shared.CollectionPage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/albums")
@Tag(name = "Albums")
@SecurityRequirement(name = "bearerAuth")
public class AlbumController extends ResourceController<Album, AlbumRequest, AlbumResponse> {

    private final AlbumService albumService;
    private final PhotoService photoService;

    public AlbumController(AlbumService albumService, PhotoService photoService, JsonMergePatcher patcher) {
        super(albumService, AlbumRequest.class, patcher);
        this.albumService = albumService;
        this.photoService = photoService;
    }

    @GetMapping
    @Operation(summary = "List albums, optionally filtered by user")
    public List<AlbumResponse> findAll(@RequestParam(required = false) Long userId,
            @RequestParam(name = "_start", defaultValue = "0") int start,
            @RequestParam(name = "_limit", defaultValue = "100") int limit,
            @Parameter(description = AFTER_DESCRIPTION) @RequestParam(name = "_after", required = false) Long after) {
        return albumService.findByUserId(userId, new CollectionPage(start, limit, after)).stream()
                .map(AlbumResponse::from)
                .toList();
    }

    @GetMapping("/{id:\\d+}/photos")
    @Operation(summary = "List photos for an album")
    public List<PhotoResponse> photos(@PathVariable long id,
            @RequestParam(name = "_start", defaultValue = "0") int start,
            @RequestParam(name = "_limit", defaultValue = "100") int limit,
            @Parameter(description = AFTER_DESCRIPTION) @RequestParam(name = "_after", required = false) Long after) {
        return photoService.findByAlbumId(id, new CollectionPage(start, limit, after)).stream()
                .map(PhotoResponse::from)
                .toList();
    }

    @Override
    protected Album toModel(AlbumRequest request) {
        return request.toModel();
    }

    @Override
    protected AlbumRequest toRequest(Album album) {
        return AlbumRequest.from(album);
    }

    @Override
    protected AlbumResponse toResponse(Album album) {
        return AlbumResponse.from(album);
    }
}
