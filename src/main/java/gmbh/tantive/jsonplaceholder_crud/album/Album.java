package gmbh.tantive.jsonplaceholder_crud.album;

import gmbh.tantive.jsonplaceholder_crud.shared.ResourceModel;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Album extends ResourceModel {

    private Long userId;
    private String title;
}