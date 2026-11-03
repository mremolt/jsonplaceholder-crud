package gmbh.tantive.jsonplaceholder_crud.comment;

import gmbh.tantive.jsonplaceholder_crud.shared.ResourceModel;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Comment extends ResourceModel {

    private Long postId;
    private String name;
    private String email;
    private String body;
}