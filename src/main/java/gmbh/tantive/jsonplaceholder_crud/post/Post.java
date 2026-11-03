package gmbh.tantive.jsonplaceholder_crud.post;

import gmbh.tantive.jsonplaceholder_crud.shared.ResourceModel;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Post extends ResourceModel {

    private Long userId;
    private String title;
    private String body;
}