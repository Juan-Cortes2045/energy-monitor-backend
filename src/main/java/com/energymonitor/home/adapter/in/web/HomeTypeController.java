package com.energymonitor.home.adapter.in.web;

import com.energymonitor.home.adapter.in.web.dto.HomeTypeResponse;
import com.energymonitor.home.application.command.GetHomeTypeQuery;
import com.energymonitor.home.application.command.ListHomeTypesQuery;
import com.energymonitor.home.application.port.in.GetHomeType;
import com.energymonitor.home.application.port.in.ListHomeTypes;
import com.energymonitor.home.application.result.HomeTypeResult;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for home type endpoints (read-only catalog).
 */
@RestController
@RequestMapping("/api/v1/home-types")
public class HomeTypeController {

    private final ListHomeTypes listHomeTypes;
    private final GetHomeType getHomeType;

    public HomeTypeController(ListHomeTypes listHomeTypes, GetHomeType getHomeType) {
        this.listHomeTypes = listHomeTypes;
        this.getHomeType = getHomeType;
    }

    @GetMapping
    public List<HomeTypeResponse> listHomeTypes() {
        return listHomeTypes.list(new ListHomeTypesQuery()).stream()
                .map(result -> new HomeTypeResponse(result.idHomeType(), result.name()))
                .toList();
    }

    @GetMapping("/{idHomeType}")
    public HomeTypeResponse getHomeType(@PathVariable String idHomeType) {
        HomeTypeResult result = getHomeType.get(new GetHomeTypeQuery(idHomeType));
        return new HomeTypeResponse(result.idHomeType(), result.name());
    }
}
