package gmbh.tantive.jsonplaceholder_crud.user;

final class UserFixtures {

    private UserFixtures() {
    }

    static User leanne(Long id) {
        User.Geo geo = new User.Geo();
        geo.setLat("-37.3159");
        geo.setLng("81.1496");
        User.Address address = new User.Address();
        address.setStreet("Kulas Light");
        address.setSuite("Apt. 1");
        address.setCity("Gwenborough");
        address.setZipcode("92998-3874");
        address.setGeo(geo);
        User.Company company = new User.Company();
        company.setName("Romaguera-Crona");
        company.setCatchPhrase("Multi-layered client-server neural-net");
        company.setBs("harness real-time e-markets");
        User user = new User();
        user.setId(id);
        user.setName("Leanne Graham");
        user.setUsername("Bret");
        user.setEmail("sincere@april.biz");
        user.setAddress(address);
        user.setPhone("1-770-736-8031");
        user.setWebsite("hildegard.org");
        user.setCompany(company);
        return user;
    }
}
