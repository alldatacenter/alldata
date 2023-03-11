package com.hw.lineage.server.interfaces.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Add a fake login and logout method in your API just to generate the Swagger documentation,
 * it'll be automatically override by Spring Security filters.
 *
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Validated
@RestController
@Api(tags = "Securities API")
@RequestMapping("/")
public class SecurityController {

    @PostMapping("/login")
    public void fakeLogin(@ApiParam("username") @RequestParam String username,
                          @ApiParam("password") @RequestParam String password) {
        throw new IllegalStateException("This method shouldn't be called. It's implemented by Spring Security filters.");
    }

    @PostMapping("/logout")
    public void fakeLogout() {
        throw new IllegalStateException("This method shouldn't be called. It's implemented by Spring Security filters.");
    }
}
