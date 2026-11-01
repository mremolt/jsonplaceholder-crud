package gmbh.tantive.jsonplaceholder_crud.shared;

public abstract class ResourceModel {

    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}