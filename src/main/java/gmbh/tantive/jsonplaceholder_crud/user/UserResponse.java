package gmbh.tantive.jsonplaceholder_crud.user;

public record UserResponse(Long id, String name, String username, String email, UserRequest.Address address,
        String phone, String website, UserRequest.Company company) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getUsername(), user.getEmail(),
                UserRequest.Address.from(user.getAddress()), user.getPhone(), user.getWebsite(),
                UserRequest.Company.from(user.getCompany()));
    }
}
