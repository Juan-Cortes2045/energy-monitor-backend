package com.energymonitor.home.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.energymonitor.home.application.command.ListMembersQuery;
import com.energymonitor.home.application.exception.HomeNotFoundException;
import com.energymonitor.home.application.usecase.ListMembersService;
import com.energymonitor.home.domain.model.Role;
import com.energymonitor.home.domain.model.UserHome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ListMembersServiceTest {

    private HomeUseCaseFixtures.FakeUserHomePersistencePort userHomePort;
    private ListMembersService service;

    @BeforeEach
    void setUp() {
        userHomePort = new HomeUseCaseFixtures.FakeUserHomePersistencePort();
        userHomePort.save(UserHome.owner("use0000001", "hom0000001"));
        userHomePort.save(UserHome.member("use0000002", "hom0000001"));
        service = new ListMembersService(userHomePort);
    }

    @Test
    void listMembersReturnsAllActiveMembers() {
        var results = service.list(new ListMembersQuery("use0000001", "hom0000001"));

        assertEquals(2, results.size());
        var owner = results.stream().filter(r -> r.userId().equals("use0000001")).findFirst().orElseThrow();
        assertEquals(Role.OWNER, owner.role());
        var member = results.stream().filter(r -> r.userId().equals("use0000002")).findFirst().orElseThrow();
        assertEquals(Role.MEMBER, member.role());
    }

    @Test
    void listMembersThrows404WhenActorIsNotMember() {
        assertThrows(HomeNotFoundException.class,
                () -> service.list(new ListMembersQuery("use0000099", "hom0000001")));
    }

    @Test
    void memberCanListMembers() {
        var results = service.list(new ListMembersQuery("use0000002", "hom0000001"));

        assertEquals(2, results.size());
    }
}
