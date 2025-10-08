package com.ecommerce.project.util;

import com.ecommerce.project.model.User;
import com.ecommerce.project.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class AuthUtil {
    @Autowired
    private UserRepo userRepo;

    public String loggedInEmail(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepo.findByUserName(authentication.getName()).
                orElseThrow(()-> new UsernameNotFoundException("User not found with username: " + authentication.getName()));

        return user.getEmail();
    }

    public Integer loggedInUserId(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepo.findByUserName(authentication.getName()).
                orElseThrow(()-> new UsernameNotFoundException("User not found with username: " + authentication.getName()));

        return user.getUserId();
    }

    public User loggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        return userRepo.findByUserName(authentication.getName()).
                orElseThrow(()-> new UsernameNotFoundException("User not found with username: " + authentication.getName()));
    }
}
