package com.example.customerservice.servicies;

import com.example.customerservice.data.ERole;
import com.example.customerservice.data.UserDto;
import com.example.customerservice.data.UserEntry;
import com.example.customerservice.repositories.UsersRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    UsersRepository usersRepository;

    @Autowired
    private ModelMapper modelMapper;

    public UserDetails getUserDetailsByName(String username) {
        final UserEntry userEntry = usersRepository.findByName(username).orElse(null);
        if (userEntry != null) {
            return User.builder()
                    .username(userEntry.getName())
                    .password(userEntry.getPassword())
                    .roles(userEntry.getRoles().stream().map(ERole::name).toArray(String[]::new))
                    .build();
        }
        throw new UsernameNotFoundException("Username not found");
    }

    public UserDto convertToDto(final UserEntry userEntry) {
        return modelMapper.map(userEntry, UserDto.class);
    }
}
