package gmbh.tantive.jsonplaceholder_crud.user;

import gmbh.tantive.jsonplaceholder_crud.shared.ResourceModel;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class User extends ResourceModel {

    @NotBlank
    private String name;
    @NotBlank
    private String username;
    @NotBlank
    @Email
    private String email;
    @NotNull
    @Valid
    private Address address;
    @NotBlank
    private String phone;
    @NotBlank
    private String website;
    @NotNull
    @Valid
    private Company company;

    @Getter
    @Setter
    public static class Address {

        @NotBlank
        private String street;
        @NotBlank
        private String suite;
        @NotBlank
        private String city;
        @NotBlank
        private String zipcode;
        @NotNull
        @Valid
        private Geo geo;
    }

    @Getter
    @Setter
    public static class Geo {

        @NotBlank
        private String lat;
        @NotBlank
        private String lng;
    }

    @Getter
    @Setter
    public static class Company {

        @NotBlank
        private String name;
        @NotBlank
        private String catchPhrase;
        @NotBlank
        private String bs;
    }
}