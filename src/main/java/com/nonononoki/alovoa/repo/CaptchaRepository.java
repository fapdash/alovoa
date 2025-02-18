package com.nonononoki.alovoa.repo;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nonononoki.alovoa.entity.Captcha;

public interface CaptchaRepository extends JpaRepository<Captcha, Long> {
	List<Captcha> findTop1000ByDateBefore(Date date);
	Captcha findByHashCode(String hashCode); 
}

