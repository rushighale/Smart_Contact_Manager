package com.smart.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
@EnableWebSecurity
public class MyConfig extends WebSecurityConfigurerAdapter{

	@Bean
	public UserDetailsService getUserDetailService() {   // here we define bean of interface and return the implimented class
		return new UserDetailsServiceImpl();              //now spring make object of this implimented class and u can call it using autowired
	}
	
	@Bean
	public BCryptPasswordEncoder passwordEncoder() {     // here define bean of BCryptPasswordEncoder class
		return new BCryptPasswordEncoder();
	}
	
	@Bean
	public DaoAuthenticationProvider authenticationProvider() {
	DaoAuthenticationProvider daoAuthenticationProvider =new DaoAuthenticationProvider();    // set it by using get and set method of DaoAuthenticationProvider
	
	daoAuthenticationProvider.setUserDetailsService(this.getUserDetailService());  //we passed above 2 here
	daoAuthenticationProvider.setPasswordEncoder(passwordEncoder());
	return daoAuthenticationProvider;
	
	}

	
	
	/// configure method...
	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.authenticationProvider(authenticationProvider());  //we passed here Daoauthentication.. ob which have jiske pas Detialservice ka obj hia, passEncoder hai
	}
	// builder ko hame btane hai ki ham kis type ka authentication provide krna hai(DataBase authentication or inmemoryBase authe[auth.inmemoryAuthenticationprovider)

	
	
	@Override      													 
	protected void configure(HttpSecurity http) throws Exception {
		http.authorizeRequests().antMatchers("/admin/**").hasRole("ADMIN")      
		.antMatchers("/user/**").hasRole("USER")                            // in DB ROLE_USER 
		.antMatchers("/**").permitAll().and().formLogin()
		.loginPage("/signin")
		.loginProcessingUrl("/dologin")          	 // submit username and password here
		.defaultSuccessUrl("/user/index")              //login success ho gya to is page pe forward krna	
		.and().csrf().disable();  
	
		
		
		// we have mention here loginPage("/signin") which return->login.html // then he dident use their login page
		// issse ham batayenge ki ap sare route protect mat kro, jo kaha usko hi protect kro
	}










}

