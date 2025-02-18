package com.nonononoki.alovoa.html;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.repo.GenderRepository;
import com.nonononoki.alovoa.repo.UserIntentionRepository;
import com.nonononoki.alovoa.service.AuthService;

@Controller
public class ProfileOnboardingResource {

	@Autowired
	private AuthService authService;

	@Autowired
	private GenderRepository genderRepo;

	@Autowired
	private UserIntentionRepository userIntentionRepo;
	@Value("${app.media.max-size}")
	private int mediaMaxSize;

	@Value("${app.interest.max}")
	private int interestMaxSize;
	
	public static final String URL = "/user/onboarding";
	
	public static String getUrl() {
		return URL;
	}

	@GetMapping(URL)
	public ModelAndView onboarding() throws AlovoaException, InvalidKeyException, IllegalBlockSizeException,
			BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
			UnsupportedEncodingException {

		User user = authService.getCurrentUser();
		if (user.isAdmin()) {
			return new ModelAndView("redirect:" + AdminResource.getUrl());
		} else if (user.getProfilePicture() != null || user.getDescription() != null) {
			return new ModelAndView("redirect:" + ProfileResource.getUrl());
		} else {
			int age = Tools.calcUserAge(user);
			boolean isLegal = age >= Tools.AGE_LEGAL;
			ModelAndView mav = new ModelAndView("profile-onboarding");
			mav.addObject("genders", genderRepo.findAll());
			mav.addObject("intentions", userIntentionRepo.findAll());
			mav.addObject("isLegal", isLegal);
			mav.addObject("mediaMaxSize", mediaMaxSize);
			mav.addObject("interestMaxSize", interestMaxSize);
			return mav;
		}
	}
}
