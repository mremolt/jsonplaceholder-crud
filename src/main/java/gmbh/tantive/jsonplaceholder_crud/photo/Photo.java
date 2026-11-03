package gmbh.tantive.jsonplaceholder_crud.photo;

import gmbh.tantive.jsonplaceholder_crud.shared.ResourceModel;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Photo extends ResourceModel {

    private Long albumId;
    private String title;
    private String url;
    private String thumbnailUrl;
}