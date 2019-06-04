package com.yuzu.service;

import org.springframework.security.core.userdetails.UserDetailsService;

import com.yuzu.entities.User;
import com.yuzu.security.UserRegistrationDto;

public interface UserService extends UserDetailsService{

    User findByLastName(String lastName);
	 User save(UserRegistrationDto registration);
}
