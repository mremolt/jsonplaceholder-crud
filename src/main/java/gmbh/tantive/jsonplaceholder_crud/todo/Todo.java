package gmbh.tantive.jsonplaceholder_crud.todo;

import gmbh.tantive.jsonplaceholder_crud.shared.ResourceModel;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Todo extends ResourceModel {

    private Long userId;
    private String title;
    private Boolean completed;
}