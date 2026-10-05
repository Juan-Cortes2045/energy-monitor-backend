package com.energymonitor.home.adapter.in.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.energymonitor.home.adapter.in.web.dto.CreateHomeRequest;
import com.energymonitor.home.adapter.in.web.dto.HomeResponse;
import com.energymonitor.home.adapter.in.web.dto.JoinHomeRequest;
import com.energymonitor.home.adapter.in.web.dto.UserHomeResponse;
import com.energymonitor.home.application.command.CreateHomeCommand;
import com.energymonitor.home.application.command.JoinHomeCommand;
import com.energymonitor.home.application.command.LeaveHomeCommand;
import com.energymonitor.home.application.command.ListHomesQuery;
import com.energymonitor.home.application.command.ListMembersQuery;
import com.energymonitor.home.application.command.RemoveUserCommand;
import com.energymonitor.home.application.command.ToggleFavoriteCommand;
import com.energymonitor.home.application.port.in.CreateHome;
import com.energymonitor.home.application.port.in.JoinHome;
import com.energymonitor.home.application.port.in.LeaveHome;
import com.energymonitor.home.application.port.in.ListHomes;
import com.energymonitor.home.application.port.in.ListMembers;
import com.energymonitor.home.application.port.in.RemoveUser;
import com.energymonitor.home.application.port.in.ToggleFavorite;
import com.energymonitor.home.application.result.HomeMembershipResult;
import com.energymonitor.home.application.result.HomeResult;
import com.energymonitor.home.application.result.UserHomeResult;

import jakarta.validation.Valid;

/**
 * Controller for home management endpoints.
 *
 * <p>All endpoints require authentication via the {@code X-User-Id} header.
 */
@RestController
@RequestMapping("/api/v1/homes")
public class HomeController {

    private final CreateHome createHome;
    private final JoinHome joinHome;
    private final ListHomes listHomes;
    private final ListMembers listMembers;
    private final ToggleFavorite toggleFavorite;
    private final RemoveUser removeUser;
    private final LeaveHome leaveHome;
    private final CurrentUserResolver currentUser;

    public HomeController(CreateHome createHome, JoinHome joinHome, ListHomes listHomes, ListMembers listMembers, ToggleFavorite toggleFavorite, RemoveUser removeUser,LeaveHome leaveHome, CurrentUserResolver currentUser) {
        this.createHome = createHome;
        this.joinHome = joinHome;
        this.listHomes = listHomes;
        this.listMembers = listMembers;
        this.toggleFavorite = toggleFavorite;
        this.removeUser = removeUser;
        this.leaveHome = leaveHome;
        this.currentUser = currentUser;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HomeResponse createHome(@Valid @RequestBody CreateHomeRequest request) {
        String userId = currentUser.resolveCurrentUserId();
        HomeResult result = createHome.create(new CreateHomeCommand(
                userId, request.name(), request.homeTypeId(), request.address(), request.description()));
        return new HomeResponse(result.idHome(), result.name(), result.homeTypeId(),
                result.address(), result.accessCode(), result.description(), result.creationDate());
    }

    @GetMapping
    public List<HomeMembershipResult> listHomes() {
        String userId = currentUser.resolveCurrentUserId();
        return listHomes.list(new ListHomesQuery(userId));
    }

    @GetMapping("/{homeId}/members")
    public List<UserHomeResponse> listMembers(@PathVariable String homeId) {
        String userId = currentUser.resolveCurrentUserId();
        return listMembers.list(new ListMembersQuery(userId, homeId)).stream()
                .map(r -> new UserHomeResponse(r.userId(), r.homeId(), r.role(), r.favorite()))
                .toList();
    }

    @PostMapping("/join")
    @ResponseStatus(HttpStatus.CREATED)
    public UserHomeResponse joinHome(@Valid @RequestBody JoinHomeRequest request) {
        String userId = currentUser.resolveCurrentUserId();
        UserHomeResult result = joinHome.join(new JoinHomeCommand(userId, request.accessCode()));
        return new UserHomeResponse(result.userId(), result.homeId(), result.role(), result.favorite());
    }

    @PutMapping("/{homeId}/favorite")
    public UserHomeResponse toggleFavorite(@PathVariable String homeId) {
        String userId = currentUser.resolveCurrentUserId();
        UserHomeResult result = toggleFavorite.toggle(new ToggleFavoriteCommand(userId, homeId));
        return new UserHomeResponse(result.userId(), result.homeId(), result.role(), result.favorite());
    }

    @DeleteMapping("/{homeId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeUser(@PathVariable String homeId, @PathVariable String userId) {
        String currentUserId = currentUser.resolveCurrentUserId();
        removeUser.remove(new RemoveUserCommand(currentUserId, homeId, userId));
    }

    @DeleteMapping("/{homeId}/members/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leaveHome(@PathVariable String homeId) {
        String userId = currentUser.resolveCurrentUserId();
        leaveHome.leave(new LeaveHomeCommand(userId, homeId));
    }
}
