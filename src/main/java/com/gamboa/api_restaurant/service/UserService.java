package com.gamboa.api_restaurant.service;


import com.gamboa.api_restaurant.domain.entity.User;
import com.gamboa.api_restaurant.domain.enums.Role;
import com.gamboa.api_restaurant.dto.request.AuthRequestDTO;
import com.gamboa.api_restaurant.dto.request.SignupRequestDTO;
import com.gamboa.api_restaurant.dto.response.AuthResponseDTO;
import com.gamboa.api_restaurant.dto.response.UserProfileResponseDTO;
import com.gamboa.api_restaurant.exception.BadRequestException;
import com.gamboa.api_restaurant.exception.ResourceNotFoundException;
import com.gamboa.api_restaurant.mapper.UserMapper;
import com.gamboa.api_restaurant.repository.UserRepository;
import com.gamboa.api_restaurant.security.TokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final UserMapper userMapper;

  private final TokenProvider tokenProvider;
  private final AuthenticationManagerBuilder authenticateManagerBuilder;

  @Transactional(readOnly = true)
  public AuthResponseDTO signIn(AuthRequestDTO authRequest) {
    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
      authRequest.getEmail(),
      authRequest.getPassword()
    );
    Authentication authentication = authenticateManagerBuilder.getObject().authenticate(authenticationToken);
    SecurityContextHolder.getContext().setAuthentication(authentication);

    String accessToken = tokenProvider.createAccessToken(authentication);
    UserProfileResponseDTO userProfileDTO = findByEmail(authRequest.getEmail());

    return userMapper.toAuthResponseDTO(accessToken, userProfileDTO);
  }

  @Transactional
  public UserProfileResponseDTO signup(SignupRequestDTO signupFormDTO) {
    boolean emailAlreadyExists = userRepository.existsByEmail(signupFormDTO.getEmail());

    if (emailAlreadyExists) {
      throw new BadRequestException("El email ya está siendo usado por otro usuario.");
    }

    User user = userMapper.toUser(signupFormDTO);
    user.setPassword(passwordEncoder.encode(signupFormDTO.getPassword()));
    user.setRole(Role.USER);

    userRepository.save(user);

    return userMapper.toUserProfileResponseDTO(user);
  }

  @Transactional(readOnly = true)
  public UserProfileResponseDTO findByEmail(String email) {
    User user = userRepository.findOneByEmail(email)
            .orElseThrow(ResourceNotFoundException::new);

    return userMapper.toUserProfileResponseDTO(user);
  }

}
