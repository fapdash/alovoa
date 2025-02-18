package com.nonononoki.alovoa.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.nonononoki.alovoa.entity.Captcha;
import com.nonononoki.alovoa.entity.Contact;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserDeleteToken;
import com.nonononoki.alovoa.entity.user.UserHide;
import com.nonononoki.alovoa.entity.user.UserPasswordToken;
import com.nonononoki.alovoa.repo.CaptchaRepository;
import com.nonononoki.alovoa.repo.ContactRepository;
import com.nonononoki.alovoa.repo.UserDeleteTokenRepository;
import com.nonononoki.alovoa.repo.UserHideRepository;
import com.nonononoki.alovoa.repo.UserPasswordTokenRepository;
import com.nonononoki.alovoa.repo.UserRepository;

@Service
public class ScheduleService {

	@Autowired
	private CaptchaRepository captchaRepo;

	@Autowired
	private ContactRepository contactRepo;

	@Autowired
	private UserHideRepository userHideRepo;

	@Autowired
	private UserRepository userRepo;

	@Autowired
	private UserPasswordTokenRepository passwordTokenRepository;

	@Autowired
	private UserDeleteTokenRepository userDeleteTokenRepository;

	@Value("${app.schedule.enabled}")
	private boolean enableSchedules;

	@Value("${app.schedule.delay.captcha}")
	private long captchaDelay;

	@Value("${app.schedule.delay.hide}")
	private long hideDelay;

	@Value("${app.schedule.delay.password-reset}")
	private long passwordResetDelay;

	@Value("${app.schedule.delay.delete-account}")
	private long deleteAccountDelay;

	@Value("${app.schedule.delay.contact}")
	private long contactDelay;

	@Scheduled(fixedDelayString = "${app.schedule.short}")
	public void scheduleShort() {
		if (enableSchedules) {
			Date date = new Date();
			cleanCaptcha(date);
			cleanUserPasswordToken(date);
		}
	}

	@Scheduled(fixedDelayString = "${app.schedule.long}")
	public void scheduleLong() {
		if (enableSchedules) {
			Date date = new Date();
			cleanUserHide(date);
			cleanUserDeleteToken(date);
			cleanContact(date);
		}
	}

	public void cleanCaptcha(Date date) {
		long ms = date.getTime();
		ms -= captchaDelay;
		Date d = new Date(ms);

		List<Captcha> captchas = captchaRepo.findTop1000ByDateBefore(d);
		captchaRepo.deleteAll(captchas);
		captchaRepo.flush();
	}

	public void cleanContact(Date date) {
		long ms = date.getTime();
		ms -= contactDelay;
		Date d = new Date(ms);

		List<Contact> contacts = contactRepo.findTop100ByDateBefore(d);
		contactRepo.deleteAll(contacts);
		contactRepo.flush();
	}

	public void cleanUserHide(Date date) {
		long ms = date.getTime();
		ms -= hideDelay;
		Date d = new Date(ms);

		List<UserHide> tokens = userHideRepo.findTop100ByDateBefore(d);
		List<User> users = new ArrayList<>();
		for (UserHide hide : tokens) {
			User u = hide.getUserFrom();
			User u2 = hide.getUserTo();
			u.getHiddenUsers().remove(hide);
			u2.getHiddenByUsers().remove(hide);
			users.add(u);
			users.add(u2);
		}
		userRepo.saveAll(users);
		userRepo.flush();
	}

	public void cleanUserPasswordToken(Date date) {
		long ms = date.getTime();
		ms -= passwordResetDelay;
		Date d = new Date(ms);

		List<UserPasswordToken> tokens = passwordTokenRepository.findTop100ByDateBefore(d);
		List<User> users = new ArrayList<>();
		for (UserPasswordToken token : tokens) {
			User u = token.getUser();
			u.setPasswordToken(null);
			users.add(u);
		}
		userRepo.saveAll(users);
		userRepo.flush();
	}

	public void cleanUserDeleteToken(Date date) {
		long ms = date.getTime();
		ms += deleteAccountDelay;
		Date d = new Date(ms);
		List<UserDeleteToken> tokens = userDeleteTokenRepository.findTop100ByDateAfter(d);
		List<User> users = new ArrayList<>();
		for (UserDeleteToken token : tokens) {
			User u = token.getUser();
			u.setDeleteToken(null);
			users.add(u);
		}
		userRepo.saveAll(users);
		userRepo.flush();
	}

}
