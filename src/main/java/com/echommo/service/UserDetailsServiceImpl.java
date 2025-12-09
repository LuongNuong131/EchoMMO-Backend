package com.echommo.service;

import com.echommo.entity.User;
import com.echommo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Tìm user trong Database
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + username));

        // 2. Xử lý quyền (Role)
        // Nếu role null thì mặc định là USER, ngược lại lấy tên role
        String roleName = (user.getRole() != null) ? user.getRole().name() : "USER";
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(roleName));

        // 3. Trả về đối tượng User của Spring Security
        // [QUAN TRỌNG] Dùng user.getPasswordHash() thay vì getPassword()
        // Vì passwordHash mới là chuỗi mã hóa BCrypt dùng để so sánh
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPasswordHash(),
                user.getIsActive() != null ? user.getIsActive() : true, // enabled (mặc định true nếu null)
                true,                   // accountNonExpired
                true,                   // credentialsNonExpired
                user.getIsCaptchaLocked() == null || !user.getIsCaptchaLocked(), // accountNonLocked (không bị khóa captcha)
                authorities
        );
    }
}