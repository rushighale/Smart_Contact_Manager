package com.smart.config;

import com.smart.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.smart.dao.UserRepository;

public class UserDetailsServiceImpl implements UserDetailsService{

	@Autowired
	private UserRepository userRepository;
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		
		// fetching user from Database
		User user=userRepository.getUserByUserName(username);
		
		if(user==null)
		{
			throw new UsernameNotFoundException("Could not found user");
		}
		CustomUserDetails customUserDetails=new CustomUserDetails(user);  // we called here constructor of "public class CustomUserDetails implements UserDetails{"
		
		return customUserDetails;
	}
			
}
