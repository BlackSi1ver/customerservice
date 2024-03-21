package com.customerservice.service;

import com.customerservice.exception.InvalidStateException;
import com.customerservice.rest.domain.UserDto;
import com.customerservice.domain.UserEntry;
import com.customerservice.domain.ERole;
import com.customerservice.domain.UserPasswordEntry;
import com.customerservice.exception.NotFoundUserException;
import com.customerservice.repository.UserPasswordRepository;
import com.customerservice.repository.UserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserPasswordRepository passwordsRepository;
    private final ModelMapper modelMapper;

    public UserService(final UserRepository userRepository,
                       final UserPasswordRepository passwordsRepository,
                       final ModelMapper modelMapper) {

        this.userRepository = userRepository;
        this.passwordsRepository = passwordsRepository;
        this.modelMapper = modelMapper;
    }

    public UserDetails getUserDetailsByName(String username) {

        final UserEntry userEntry = userRepository.findByName(username).orElse(null);
        if (userEntry == null) {
            throw new UsernameNotFoundException("Username not found");
        }

        final UserPasswordEntry passwordEntry = passwordsRepository.findByUserId(userEntry.getId());
        if (passwordEntry == null) {
            throw new InvalidStateException("Password not found");
        }

        return User.builder()
                    .username(userEntry.getName())
                    .password(passwordEntry.getPassword())
                    .roles(userEntry.getRoles().stream().map(ERole::name).toArray(String[]::new))
                    .build();
    }

    @Transactional
    public UserDto createUser(final String name,
                              final Set<ERole> roles,
                              final String password) {

        final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        final UserEntry user = userRepository.save(
                new UserEntry(name, roles)
        );
        passwordsRepository.save(
                new UserPasswordEntry(user, passwordEncoder.encode(password))
        );

        return convertToDto(user);
    }

    public UserEntry getUserById(final long id) {
        return userRepository.findById(id).orElse(null);
    }

    public List<UserEntry> findAll() {
        return userRepository.findAll();
    }

    public void deleteAllUsers() {
        userRepository.deleteAll();
    }

    @Transactional
    public void deleteUserById(final long id) {

        final UserEntry user = userRepository.findById(id).orElse(null);
        if (user == null) {
            throw new UsernameNotFoundException("Username not found");
        }
        userRepository.deleteById(id);
    }

    public UserDto convertToDto(final UserEntry userEntry) {

        if (userEntry != null) {
            return modelMapper.map(userEntry, UserDto.class);
        }
        return null;
    }

    public UserDto getUserByName(final String name) {
        return convertToDto(userRepository.findByName(name).orElse(null));
    }

    @Transactional
    public UserEntry setUserRole(final String name,
                                 final ERole role,
                                 final boolean enable) {

        final UserEntry user = userRepository.findByName(name).orElse(null);
        if (user == null) {
            throw new NotFoundUserException("Username not found");
        }

        if (enable) {
            user.getRoles().add(role);
        }
        else {
            user.getRoles().remove(role);
        }

        return userRepository.save(user);
    }
}
