package com.nonononoki.alovoa.rest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.mail.MessagingException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.UserDeleteAccountDto;
import com.nonononoki.alovoa.model.ProfileOnboardingDto;
import com.nonononoki.alovoa.service.UserService;

@RestController
@RequestMapping("/user")
public class UserController {

	@Autowired
	private UserService userService;

	@Value("${app.media.max-size}")
	private int mediaMaxSize;
	
	//simple post to test session
	@PostMapping("/post")
	public void post(){
	}

	// GDPR
	@PostMapping(value = "/delete-account", consumes = "text/plain")
	public void deleteAccount() throws MessagingException, IOException, AlovoaException {
		userService.deleteAccountRequest();
	}

	@PostMapping(value = "/delete-account-confirm", consumes = "application/json")
	public void deleteAccount(@RequestBody UserDeleteAccountDto dto)
			throws NoSuchAlgorithmException, MessagingException, IOException, AlovoaException {
		userService.deleteAccountConfirm(dto);
	}

	@GetMapping(value = "/userdata/{idEnc}")
	public ResponseEntity<Resource> getUserdata(@PathVariable String idEnc)
			throws JsonProcessingException, UnsupportedEncodingException, AlovoaException, NumberFormatException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException {
		return userService.getUserdata(idEnc);
	}

	@PostMapping(value = "/onboarding", consumes = "application/json")
	public void onboarding(@RequestBody ProfileOnboardingDto dto) throws AlovoaException {
		userService.onboarding(dto);
	}
	
	@PostMapping(value = "/delete/profile-picture")
	public void deleteProfilePicture() throws AlovoaException {
		userService.deleteProfilePicture();
	}

	@PostMapping(value = "/update/profile-picture", consumes = "text/plain")
	public void updateProfilePicture(@RequestBody String imageB64) throws AlovoaException, IOException {
		if (Tools.getBase64Size(imageB64) > mediaMaxSize) {
			throw new AlovoaException(AlovoaException.MAX_MEDIA_SIZE_EXCEEDED);
		}
		userService.updateProfilePicture(imageB64);
	}

	@GetMapping(value = "/get/audio/{userIdEnc}")
	public String getAudio(@PathVariable String userIdEnc)
			throws NumberFormatException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException,
			NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException {
		return userService.getAudio(userIdEnc);
	}

	@PostMapping(value = "/delete/audio")
	public void deleteAudio() throws AlovoaException {
		userService.deleteAudio();
	}

	@PostMapping(value = "/update/audio/{mimeType}", consumes = "text/plain")
	public void updateAudio(@RequestBody String audioB64, @PathVariable String mimeType)
			throws AlovoaException, UnsupportedAudioFileException, IOException {
		if (Tools.getBase64Size(audioB64) > mediaMaxSize) {
			throw new AlovoaException(AlovoaException.MAX_MEDIA_SIZE_EXCEEDED);
		}
		userService.updateAudio(audioB64, mimeType);
	}

	@PostMapping(value = "/update/description", consumes = "text/plain")
	public void updateDescription(@RequestBody(required = false) String description) throws AlovoaException {
		userService.updateDescription(description);
	}

	@PostMapping("/update/intention/{intention}")
	public void updateIntention(@PathVariable long intention) throws AlovoaException {
		userService.updateIntention(intention);
	}

	@PostMapping("/update/min-age/{minAge}")
	public void updateMinAge(@PathVariable int minAge) throws AlovoaException {
		userService.updateMinAge(minAge);
	}

	@PostMapping("/update/max-age/{maxAge}")
	public void updateMaxAge(@PathVariable int maxAge) throws AlovoaException {
		userService.updateMaxAge(maxAge);
	}

	@PostMapping("/update/preferedGender/{genderId}/{activated}")
	public void updatePreferedGenders(@PathVariable int genderId, @PathVariable String activated)
			throws AlovoaException {
		userService.updatePreferedGender(genderId, Tools.binaryStringToBoolean(activated));
	}
	
	@PostMapping("/update/misc-info/{infoValue}/{activated}")
	public void updatePreferedGenders(@PathVariable long infoValue, @PathVariable String activated)
			throws AlovoaException {
		userService.updateUserMiscInfo(infoValue, Tools.binaryStringToBoolean(activated));
	}

	@PostMapping("/interest/add/{value}")
	public void addInterest(@PathVariable String value) throws AlovoaException {
		userService.addInterest(value);
	}

	@PostMapping("/interest/delete/{interestId}")
	public void deleteInterest(@PathVariable long interestId) throws AlovoaException {
		userService.deleteInterest(interestId);
	}

	@PostMapping("/accent-color/update/{accentColor}")
	public void updateAccentColor(@PathVariable String accentColor) throws AlovoaException {
		userService.updateAccentColor(accentColor);
	}

	@PostMapping("/ui-design/update/{uiDesign}")
	public void updateUiDesign(@PathVariable String uiDesign) throws AlovoaException {
		userService.updateUiDesign(uiDesign);
	}
	
	@PostMapping("/show-zodiac/update/{showZodiac}")
	public void updateUiDesign(@PathVariable int showZodiac) throws AlovoaException {
		userService.updateShowZodiac(showZodiac);
	}
	
	@PostMapping("/units/update/{units}")
	public void updateUnits(@PathVariable int units) throws AlovoaException {
		userService.updateUnits(units);
	}

	@PostMapping(value = "/image/add", consumes = "text/plain")
	public void addImage(@RequestBody String imageB64) throws AlovoaException, IOException {
		if (Tools.getBase64Size(imageB64) > mediaMaxSize) {
			throw new AlovoaException(AlovoaException.MAX_MEDIA_SIZE_EXCEEDED);
		}
		userService.addImage(imageB64);
	}

	@PostMapping("/image/delete/{imageId}")
	public void deleteImage(@PathVariable long imageId) throws AlovoaException {
		userService.deleteImage(imageId);
	}

	@PostMapping(value = "/like/{idEnc}")
	public void likeUser(@PathVariable String idEnc)
			throws AlovoaException, GeneralSecurityException, IOException, JoseException {
		userService.likeUser(idEnc);
	}

	@PostMapping(value = "/hide/{idEnc}")
	public void hideUser(@PathVariable String idEnc)
			throws NumberFormatException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException,
			NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, AlovoaException {
		userService.hideUser(idEnc);
	}

	@PostMapping(value = "/block/{idEnc}")
	public void blockUser(@PathVariable String idEnc)
			throws NumberFormatException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException,
			NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, AlovoaException {
		userService.blockUser(idEnc);
	}

	@PostMapping(value = "/unblock/{idEnc}")
	public void unblockUser(@PathVariable String idEnc)
			throws NumberFormatException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException,
			NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, AlovoaException {
		userService.unblockUser(idEnc);
	}

	@PostMapping(value = "/report/{idEnc}/{captchaId}/{captchaText}", consumes = "text/plain")
	public void reportUser(@PathVariable String idEnc, @PathVariable long captchaId, @PathVariable String captchaText,
			@RequestBody String comment) throws NumberFormatException, InvalidKeyException,
			UnsupportedEncodingException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException,
			NoSuchPaddingException, InvalidAlgorithmParameterException, AlovoaException {
		userService.reportUser(idEnc, captchaId, captchaText, comment);
	}

	@GetMapping(value = "/status/new-alert")
	public boolean newAlert() throws AlovoaException {
		return userService.hasNewAlert();
	}

	@GetMapping(value = "/status/new-message")
	public boolean newMessage() throws AlovoaException {
		return userService.hasNewMessage();
	}

}
