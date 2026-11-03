package gmbh.tantive.jsonplaceholder_crud.photo;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import gmbh.tantive.jsonplaceholder_crud.api.JsonMergePatcher;
import gmbh.tantive.jsonplaceholder_crud.api.ResourceController;
import gmbh.tantive.jsonplaceholder_crud.shared.CollectionPage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/photos")
@Tag(name = "Photos")
@SecurityRequirement(name = "bearerAuth")
public class PhotoController extends ResourceController<Photo, PhotoRequest, PhotoResponse> {

    private final PhotoService photoService;

    public PhotoController(PhotoService photoService, JsonMergePatcher patcher) {
        super(photoService, PhotoRequest.class, patcher);
        this.photoService = photoService;
    }

    @GetMapping
    @Operation(summary = "List photos, optionally filtered by album")
    public List<PhotoResponse> findAll(@RequestParam(required = false) Long albumId,
            @RequestParam(name = "_start", defaultValue = "0") int start,
            @RequestParam(name = "_limit", defaultValue = "100") int limit,
            @Parameter(description = AFTER_DESCRIPTION) @RequestParam(name = "_after", required = false) Long after) {
        return photoService.findByAlbumId(albumId, new CollectionPage(start, limit, after)).stream()
                .map(PhotoResponse::from)
                .toList();
    }

    @Override
    protected Photo toModel(PhotoRequest request) {
        return request.toModel();
    }

    @Override
    protected PhotoRequest toRequest(Photo photo) {
        return PhotoRequest.from(photo);
    }

    @Override
    protected PhotoResponse toResponse(Photo photo) {
        return PhotoResponse.from(photo);
    }
}
