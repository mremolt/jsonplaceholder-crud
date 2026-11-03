package gmbh.tantive.jsonplaceholder_crud.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** API representation of a user without id; the nested records are shared with {@link UserResponse}. */
public record UserRequest(@NotBlank String name, @NotBlank String username, @NotBlank @Email String email,
        @NotNull @Valid Address address, @NotBlank String phone, @NotBlank String website,
        @NotNull @Valid Company company) {

    public record Address(@NotBlank String street, @NotBlank String suite, @NotBlank String city,
            @NotBlank String zipcode, @NotNull @Valid Geo geo) {

        static Address from(User.Address address) {
            return address == null ? null : new Address(address.getStreet(), address.getSuite(), address.getCity(),
                    address.getZipcode(), Geo.from(address.getGeo()));
        }

        User.Address toModel() {
            User.Address address = new User.Address();
            address.setStreet(street);
            address.setSuite(suite);
            address.setCity(city);
            address.setZipcode(zipcode);
            address.setGeo(geo == null ? null : geo.toModel());
            return address;
        }
    }

    public record Geo(@NotBlank String lat, @NotBlank String lng) {

        static Geo from(User.Geo geo) {
            return geo == null ? null : new Geo(geo.getLat(), geo.getLng());
        }

        User.Geo toModel() {
            User.Geo geo = new User.Geo();
            geo.setLat(lat);
            geo.setLng(lng);
            return geo;
        }
    }

    public record Company(@NotBlank String name, @NotBlank String catchPhrase, @NotBlank String bs) {

        static Company from(User.Company company) {
            return company == null ? null : new Company(company.getName(), company.getCatchPhrase(), company.getBs());
        }

        User.Company toModel() {
            User.Company company = new User.Company();
            company.setName(name);
            company.setCatchPhrase(catchPhrase);
            company.setBs(bs);
            return company;
        }
    }

    static UserRequest from(User user) {
        return new UserRequest(user.getName(), user.getUsername(), user.getEmail(), Address.from(user.getAddress()),
                user.getPhone(), user.getWebsite(), Company.from(user.getCompany()));
    }

    User toModel() {
        User user = new User();
        user.setName(name);
        user.setUsername(username);
        user.setEmail(email);
        user.setAddress(address == null ? null : address.toModel());
        user.setPhone(phone);
        user.setWebsite(website);
        user.setCompany(company == null ? null : company.toModel());
        return user;
    }
}
