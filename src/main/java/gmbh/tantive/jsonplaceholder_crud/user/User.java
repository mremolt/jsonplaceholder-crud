package gmbh.tantive.jsonplaceholder_crud.user;

import gmbh.tantive.jsonplaceholder_crud.shared.ResourceModel;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class User extends ResourceModel {

    private String name;
    private String username;
    private String email;
    private Address address;
    private String phone;
    private String website;
    private Company company;

    @Getter
    @Setter
    public static class Address {

        private String street;
        private String suite;
        private String city;
        private String zipcode;
        private Geo geo;
    }

    @Getter
    @Setter
    public static class Geo {

        private String lat;
        private String lng;
    }

    @Getter
    @Setter
    public static class Company {

        private String name;
        private String catchPhrase;
        private String bs;
    }
}