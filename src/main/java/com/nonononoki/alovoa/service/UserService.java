package com.nonononoki.alovoa.service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.imageio.ImageIO;
import javax.mail.MessagingException;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.commons.lang3.RandomStringUtils;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedin.urls.Url;
import com.linkedin.urls.detection.UrlDetector;
import com.linkedin.urls.detection.UrlDetectorOptions;
import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.Conversation;
import com.nonononoki.alovoa.entity.user.Gender;
import com.nonononoki.alovoa.entity.user.UserAudio;
import com.nonononoki.alovoa.entity.user.UserBlock;
import com.nonononoki.alovoa.entity.user.UserDeleteToken;
import com.nonononoki.alovoa.entity.user.UserHide;
import com.nonononoki.alovoa.entity.user.UserImage;
import com.nonononoki.alovoa.entity.user.UserIntention;
import com.nonononoki.alovoa.entity.user.UserInterest;
import com.nonononoki.alovoa.entity.user.UserLike;
import com.nonononoki.alovoa.entity.user.UserMiscInfo;
import com.nonononoki.alovoa.entity.user.UserNotification;
import com.nonononoki.alovoa.entity.user.UserProfilePicture;
import com.nonononoki.alovoa.entity.user.UserReport;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.ProfileOnboardingDto;
import com.nonononoki.alovoa.model.UserDeleteAccountDto;
import com.nonononoki.alovoa.model.UserDeleteParams;
import com.nonononoki.alovoa.model.UserDto;
import com.nonononoki.alovoa.model.UserGdpr;
import com.nonononoki.alovoa.repo.ConversationRepository;
import com.nonononoki.alovoa.repo.GenderRepository;
import com.nonononoki.alovoa.repo.UserBlockRepository;
import com.nonononoki.alovoa.repo.UserHideRepository;
import com.nonononoki.alovoa.repo.UserImageRepository;
import com.nonononoki.alovoa.repo.UserIntentionRepository;
import com.nonononoki.alovoa.repo.UserInterestRepository;
import com.nonononoki.alovoa.repo.UserLikeRepository;
import com.nonononoki.alovoa.repo.UserMiscInfoRepository;
import com.nonononoki.alovoa.repo.UserNotificationRepository;
import com.nonononoki.alovoa.repo.UserReportRepository;
import com.nonononoki.alovoa.repo.UserRepository;
import com.sipgate.mp3wav.Converter;

@Service
public class UserService {

	@Autowired
	private AuthService authService;

	@Autowired
	private UserRepository userRepo;

	@Autowired
	private GenderRepository genderRepo;

	@Autowired
	private UserMiscInfoRepository userMiscInfoRepo;

	@Autowired
	private UserIntentionRepository userIntentionRepo;

	@Autowired
	private UserInterestRepository userInterestRepo;

	@Autowired
	private UserImageRepository userImageRepo;

	@Autowired
	private UserLikeRepository userLikeRepo;

	@Autowired
	private UserHideRepository userHideRepo;

	@Autowired
	private UserBlockRepository userBlockRepo;

	@Autowired
	private UserReportRepository userReportRepo;

	@Autowired
	private UserNotificationRepository userNotificationRepo;

	@Autowired
	private ConversationRepository conversationRepo;

	@Autowired
	private CaptchaService captchaService;

	@Autowired
	private MailService mailService;

	@Autowired
	private NotificationService notificationService;

	@Value("${app.age.min}")
	private int minAge;

	@Value("${app.age.max}")
	private int maxAge;

	@Value("${app.profile.image.length}")
	private int imageLength;

	@Value("${app.profile.image.max}")
	private int imageMax;

	@Value("${app.profile.description.size}")
	private int descriptionSize;

	@Value("${app.token.length}")
	private int tokenLength;

	@Value("${app.interest.max}")
	private int interestSize;

	@Value("${app.interest.min-chars}")
	private int interestMinCharSize;

	@Value("${app.interest.max-chars}")
	private int interestMaxCharSize;

	@Value("${app.audio.max-time}")
	private int audioMaxTime; // in seconds

	@Value("${app.user.delete.delay}")
	private long userDeleteDelay;

	@Value("${app.intention.delay}")
	private long intentionDelay;

	@Value("${app.schedule.delay.delete-account}")
	private long accountDeleteDuration;

	@Autowired
	private TextEncryptorConverter textEncryptor;

	@Autowired
	private ObjectMapper objectMapper;

	public UserDeleteToken deleteAccountRequest() throws MessagingException, IOException, AlovoaException {
		User user = authService.getCurrentUser();
		UserDeleteToken token = new UserDeleteToken();
		Date currentDate = new Date();

		token.setContent(RandomStringUtils.randomAlphanumeric(tokenLength));
		token.setDate(currentDate);
		token.setUser(user);
		user.setDeleteToken(token);
		user = userRepo.saveAndFlush(user);

		mailService.sendAccountDeleteRequest(user);

		return user.getDeleteToken();
	}

	public void deleteAccountConfirm(UserDeleteAccountDto dto)
			throws MessagingException, IOException, AlovoaException, NoSuchAlgorithmException {
		User user = authService.getCurrentUser();
		UserDeleteToken deleteToken = user.getDeleteToken();
		String userTokenString = deleteToken.getContent();
		
		dto.setEmail(Tools.cleanEmail(dto.getEmail()));

		if (!dto.isConfirm() || deleteToken == null) {
			throw new AlovoaException("deletion_not_confirmed");
		}

		long ms = new Date().getTime();
		if (user.getDeleteToken().getDate().getTime() + accountDeleteDuration < ms) {
			throw new AlovoaException("deletion_not_active");
		}

		if (!dto.getTokenString().equals(userTokenString)) {
			throw new AlovoaException("deletion_wrong_token");
		}

		if (!dto.getEmail().equals(user.getEmail())) {
			throw new AlovoaException("deletion_wrong_email");
		}

		if (!captchaService.isValid(dto.getCaptchaId(), dto.getCaptchaText())) {
			throw new AlovoaException("captcha_invalid");
		}

		UserDeleteParams userDeleteParam = UserDeleteParams.builder().conversationRepo(conversationRepo)
				.userBlockRepo(userBlockRepo).userHideRepo(userHideRepo).userLikeRepo(userLikeRepo)
				.userNotificationRepo(userNotificationRepo).userRepo(userRepo).userReportRepo(userReportRepo).build();

		removeUserDataCascading(user, userDeleteParam);

		user = authService.getCurrentUser();
		user = userRepo.saveAndFlush(user);
		userRepo.delete(user);
		userRepo.flush();

		mailService.sendAccountDeleteConfirm(user);
	}

	public static void removeUserDataCascading(User user, UserDeleteParams userDeleteParam) {

		UserRepository userRepo = userDeleteParam.getUserRepo();
		UserLikeRepository userLikeRepo = userDeleteParam.getUserLikeRepo();
		ConversationRepository conversationRepo = userDeleteParam.getConversationRepo();
		UserNotificationRepository userNotificationRepo = userDeleteParam.getUserNotificationRepo();
		UserHideRepository userHideRepo = userDeleteParam.getUserHideRepo();
		UserBlockRepository userBlockRepo = userDeleteParam.getUserBlockRepo();
		UserReportRepository userReportRepo = userDeleteParam.getUserReportRepo();

		// DELETE USER LIKE
		for (UserLike like : userLikeRepo.findByUserFrom(user)) {
			User u = like.getUserTo();
			if (u != null && u.getLikedBy() != null) {
				u.getLikedBy().remove(like);
				userRepo.save(u);
			}
			like.setUserTo(null);
			userLikeRepo.save(like);

		}
		for (UserLike like : userLikeRepo.findByUserTo(user)) {
			User u = like.getUserFrom();
			if (u != null && u.getLikes() != null) {
				u.getLikes().remove(like);
				userRepo.save(u);
			}
			like.setUserFrom(null);
			userLikeRepo.save(like);

		}
		userRepo.flush();
		userLikeRepo.flush();

		// DELETE USER NOTIFICATION
		for (UserNotification notification : userNotificationRepo.findByUserFrom(user)) {
			User u = notification.getUserTo();
			if (u != null && u.getNotificationsFrom() != null) {
				u.getNotificationsFrom().remove(notification);
				userRepo.save(u);
			}
			notification.setUserTo(null);
			userNotificationRepo.save(notification);
		}
		for (UserNotification notificaton : userNotificationRepo.findByUserTo(user)) {
			User u = notificaton.getUserFrom();
			if (u != null && u.getNotifications() != null) {
				u.getNotifications().remove(notificaton);
				userRepo.save(u);
			}
			notificaton.setUserFrom(null);
			userNotificationRepo.save(notificaton);
		}
		userRepo.flush();
		userNotificationRepo.flush();

		// DELETE USER HIDE
		for (UserHide hide : userHideRepo.findByUserFrom(user)) {
			User u = hide.getUserTo();
			if (u != null && u.getHiddenByUsers() != null) {
				u.getHiddenByUsers().remove(hide);
				userRepo.save(u);
			}
			hide.setUserTo(null);
			userHideRepo.save(hide);
		}
		for (UserHide hide : userHideRepo.findByUserTo(user)) {
			User u = hide.getUserFrom();
			if (u != null && u.getHiddenUsers() != null) {
				u.getHiddenUsers().remove(hide);
				userRepo.save(u);
			}

			hide.setUserFrom(null);
			userHideRepo.save(hide);
		}
		userRepo.flush();
		userHideRepo.flush();

		// DELETE USER BLOCK
		for (UserBlock block : userBlockRepo.findByUserFrom(user)) {
			User u = block.getUserTo();
			if (u != null && u.getBlockedByUsers() != null) {
				u.getBlockedByUsers().remove(block);
				userRepo.save(u);
			}
			block.setUserTo(null);
			userBlockRepo.save(block);
		}
		for (UserBlock block : userBlockRepo.findByUserTo(user)) {
			User u = block.getUserFrom();
			if (u != null && u.getBlockedUsers() != null) {
				u.getBlockedUsers().remove(block);
				userRepo.save(u);
			}
			block.setUserFrom(null);
			userBlockRepo.save(block);
		}
		userRepo.flush();
		userBlockRepo.flush();

		// DELETE USER REPORT
		for (UserReport report : userReportRepo.findByUserFrom(user)) {
			User u = report.getUserTo();
			if (u != null && u.getReportedByUsers() != null) {
				u.getReportedByUsers().remove(report);
				userRepo.save(u);
			}
			report.setUserTo(null);
			userReportRepo.save(report);
		}
		for (UserReport report : userReportRepo.findByUserTo(user)) {
			User u = report.getUserFrom();
			if (u != null && u.getReported() != null) {
				u.getReported().remove(report);
				userRepo.save(u);
			}
			report.setUserFrom(null);
			userReportRepo.save(report);
		}
		userRepo.flush();
		userReportRepo.flush();

		// DELETE USER CONVERSATION
		for (Conversation c : conversationRepo.findByUsers_Id(user.getId())) {
			for (User u : c.getUsers()) {
				if (u != null && u.getConversations() != null) {
					u.getConversations().remove(c);
					userRepo.save(u);
				}
			}
			conversationRepo.delete(c);
		}

		userRepo.flush();
		conversationRepo.flush();
	}

	public void updateProfilePicture(String imgB64) throws AlovoaException, IOException {
		User user = authService.getCurrentUser();
		String newImgB64 = adjustPicture(imgB64);
		if (user.getProfilePicture() == null) {
			UserProfilePicture profilePic = new UserProfilePicture();
			profilePic.setData(newImgB64);
			profilePic.setUser(user);
			user.setProfilePicture(profilePic);
		} else {
			user.getProfilePicture().setData(newImgB64);
		}

		userRepo.saveAndFlush(user);
	}

	public void onboarding(ProfileOnboardingDto model) throws AlovoaException {
		User user = authService.getCurrentUser();
		if(user.getProfilePicture() != null || user.getDescription() != null) {
			return;
		}

		Date now = new Date();

		UserProfilePicture profilePic = new UserProfilePicture();
		profilePic.setData(model.getProfilePicture());
		user.setProfilePicture(profilePic);

		UserIntention intention = userIntentionRepo.findById(model.getIntention()).orElse(null);
		user.getDates().setIntentionChangeDate(now);
		user.setIntention(intention);

		List<Gender> preferredGenders = genderRepo.findAllById(model.getPreferredGenders());
		user.setPreferedGenders(new HashSet<>(preferredGenders));

		user.setDescription(model.getDescription());
		user.getInterests().addAll((model.getInterests().stream().map(i -> {
			UserInterest interest = new UserInterest();
			interest.setText(i.toLowerCase());
			interest.setUser(user);
			return interest;
		}).collect(Collectors.toList())));

		userRepo.saveAndFlush(user);
	}

	public void updateDescription(String description) throws AlovoaException {
		if (description != null) {
			if (description.length() > descriptionSize) {
				throw new AlovoaException("max_length_exceeded");
			}
			if (description.trim().length() == 0) {
				description = null;
			} else {
				UrlDetector parser = new UrlDetector(description, UrlDetectorOptions.Default);
				List<Url> urls = parser.detect();
				if (!urls.isEmpty()) {
					throw new AlovoaException("url_detected");
				}
			}
		}
		User user = authService.getCurrentUser();
		user.setDescription(description);
		userRepo.saveAndFlush(user);
	}

	public void updateIntention(long intention) throws AlovoaException {
		User user = authService.getCurrentUser();

		Date now = new Date();
		if (user.getDates().getIntentionChangeDate() == null
				|| now.getTime() >= user.getDates().getIntentionChangeDate().getTime() + intentionDelay) {
			boolean isLegal = Tools.calcUserAge(user) >= Tools.AGE_LEGAL;
			if (!isLegal && intention == UserIntention.SEX) {
				throw new AlovoaException("not_supported");
			}
			UserIntention i = userIntentionRepo.findById(intention).orElse(null);
			user.setIntention(i);
			user.getDates().setIntentionChangeDate(now);
			userRepo.saveAndFlush(user);
		} else {
			throw new AlovoaException("intention cooldown not finished");
		}
	}

	public void updateMinAge(int userMinAge) throws AlovoaException {
		if (userMinAge < minAge) {
			userMinAge = minAge;
		}
		User user = authService.getCurrentUser();
		user.setPreferedMinAge(userMinAge);
		userRepo.saveAndFlush(user);
	}

	public void updateMaxAge(int userMaxAge) throws AlovoaException {
		if (userMaxAge > maxAge) {
			userMaxAge = maxAge;
		}
		User user = authService.getCurrentUser();
		user.setPreferedMaxAge(userMaxAge);
		userRepo.saveAndFlush(user);
	}

	public void updatePreferedGender(long genderId, boolean activated) throws AlovoaException {
		User user = authService.getCurrentUser();
		Set<Gender> list = user.getPreferedGenders();
		if (list == null) {
			list = new HashSet<>();
		}

		Gender g = genderRepo.findById(genderId).orElse(null);
		if (activated) {
			if (!list.contains(g)) {
				list.add(g);
			}
		} else {
			if (list.contains(g)) {
				list.remove(g);
			}
		}
		user.setPreferedGenders(list);
		userRepo.saveAndFlush(user);
	}

	public void updateUserMiscInfo(long infoValue, boolean activated) throws AlovoaException {
		User user = authService.getCurrentUser();
		Set<UserMiscInfo> list = user.getMiscInfos();
		if (list == null) {
			list = new HashSet<>();
		}

		UserMiscInfo info = userMiscInfoRepo.findByValue(infoValue);
		if (activated) {
			if (!list.contains(info)) {
				list.add(info);
			}

			if (infoValue >= UserMiscInfo.KIDS_NO && infoValue <= UserMiscInfo.KIDS_YES) {
				list.removeIf(o -> o.getValue() != infoValue && o.getValue() >= UserMiscInfo.KIDS_NO
						&& o.getValue() <= UserMiscInfo.KIDS_YES);
			}

			else if (infoValue >= UserMiscInfo.RELATIONSHIP_SINGLE && infoValue <= UserMiscInfo.RELATIONSHIP_OTHER) {
				list.removeIf(o -> o.getValue() != infoValue && o.getValue() >= UserMiscInfo.RELATIONSHIP_SINGLE
						&& o.getValue() <= UserMiscInfo.RELATIONSHIP_OTHER);
			}

		} else {
			if (list.contains(info)) {
				list.remove(info);
			}
		}
		user.setMiscInfos(list);
		userRepo.saveAndFlush(user);
	}

	public void addInterest(String value) throws AlovoaException {
		User user = authService.getCurrentUser();

		if (value.length() < interestMinCharSize || value.length() > interestMaxCharSize
				|| user.getInterests().size() >= interestSize) {
			throw new AlovoaException("interest_invalid_size");
		}

		Pattern pattern = Pattern.compile("[a-zA-Z0-9-]+");
		Matcher matcher = pattern.matcher(value);
		if (!matcher.matches()) {
			throw new AlovoaException("interest_unsupported_characters");
		}

		UserInterest interest = new UserInterest();
		interest.setText(value.toLowerCase());
		interest.setUser(user);

		if (user.getInterests().contains(interest)) {
			throw new AlovoaException("interest_already_exists");
		}

		if (user.getInterests() == null) {
			user.setInterests(new ArrayList<>());
		}

		user.getInterests().add(interest);

		userRepo.saveAndFlush(user);
	}

	public void deleteInterest(long interestId) throws AlovoaException {
		User user = authService.getCurrentUser();
		UserInterest interest = userInterestRepo.findById(interestId).orElse(null);

		if (interest == null) {
			throw new AlovoaException("interest_is_null");
		}

		if (!user.getInterests().contains(interest)) {
			throw new AlovoaException("interest_does_not_exists");
		}

		user.getInterests().remove(interest);
		userRepo.saveAndFlush(user);
	}

	public void updateAccentColor(String accentColor) throws AlovoaException {
		User user = authService.getCurrentUser();
		user.setAccentColor(accentColor);
		userRepo.saveAndFlush(user);
	}

	public void updateUiDesign(String uiDesign) throws AlovoaException {
		User user = authService.getCurrentUser();
		user.setUiDesign(uiDesign);
		userRepo.saveAndFlush(user);
	}

	public void updateShowZodiac(int showZodiac) throws AlovoaException {
		User user = authService.getCurrentUser();
		user.setShowZodiac(showZodiac == 1);
		userRepo.saveAndFlush(user);
	}

	public void updateUnits(int units) throws AlovoaException {
		User user = authService.getCurrentUser();
		user.setUnits(units);
		userRepo.saveAndFlush(user);
	}

	public void addImage(String imgB64) throws AlovoaException, IOException {
		User user = authService.getCurrentUser();
		if (user.getImages() != null && user.getImages().size() < imageMax) {

			UserImage img = new UserImage();
			img.setContent(adjustPicture(imgB64));
			img.setDate(new Date());
			img.setUser(user);
			user.getImages().add(img);
			userRepo.saveAndFlush(user);
		} else {
			throw new AlovoaException("max_image_amount_exceeded");
		}
	}

	public void deleteImage(long id) throws AlovoaException {
		User user = authService.getCurrentUser();
		UserImage img = userImageRepo.findById(id).orElse(null);
		if (img != null && user.getImages().contains(img)) {
			user.getImages().remove(img);
			userRepo.saveAndFlush(user);
		}
	}

	private String adjustPicture(String imgB64) throws IOException {
		ByteArrayOutputStream bos = null;
		ByteArrayInputStream bis = null;

		try {
			// convert b64 to bufferedimage
			BufferedImage image = null;
			byte[] decodedBytes = Base64.getDecoder().decode(stripB64Type(imgB64));
			bis = new ByteArrayInputStream(decodedBytes);
			image = ImageIO.read(bis);
			
			if(image.getWidth() == imageLength && image.getHeight() == imageLength) {
				return imgB64;
			}

			if (image.getWidth() != image.getHeight()) {

				int idealLength = image.getHeight();
				boolean heightShorter = true;
				if (image.getWidth() < image.getHeight()) {
					heightShorter = false;
					idealLength = image.getWidth();
				}

				// cut to a square
				int x = 0;
				int y = 0;

				if (heightShorter) {
					y = 0;
					x = image.getWidth() / 2 - image.getHeight() / 2;
				} else {
					x = 0;
					y = image.getHeight() / 2 - image.getWidth() / 2;
				}
				image = image.getSubimage(x, y, idealLength, idealLength);
			}

			// all images are equal in size
			BufferedImage scaledImage = new BufferedImage(imageLength, imageLength, image.getType());
			Graphics2D graphics2D = scaledImage.createGraphics();

			graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

			graphics2D.drawImage(image, 0, 0, imageLength, imageLength, null);
			graphics2D.dispose();
			image = scaledImage;

			// image to b64
			bos = new ByteArrayOutputStream();
			String fileType = "webp";
			ImageIO.write(image, fileType, bos);
			byte[] bytes = bos.toByteArray();
			String base64bytes = Base64.getEncoder().encodeToString(bytes);
			String src = Tools.B64IMAGEPREFIX + fileType + Tools.B64PREFIX + base64bytes;

			return src;

		} catch (Exception e) {
			throw e;
		}

	}

	public static String stripB64Type(String s) {
		if (s.contains(",")) {
			return s.split(",")[1];
		}
		return s;
	}

	public void likeUser(String idEnc) throws AlovoaException, GeneralSecurityException, IOException, JoseException {
		User user = encodedIdToUser(idEnc);
		User currUser = authService.getCurrentUser();

		if (user.equals(currUser)) {
			throw new AlovoaException("user_is_same_user");
		}

		if (user.getBlockedUsers().stream().anyMatch(o -> o.getUserTo().getId().equals(currUser.getId()))) {
			throw new AlovoaException("is_blocked");
		}

		if (currUser.getBlockedUsers().stream().anyMatch(o -> o.getUserTo().getId().equals(user.getId()))) {
			throw new AlovoaException("is_blocked");
		}

		if (!Tools.usersCompatible(currUser, user)) {
			throw new AlovoaException("users_not_compatible");
		}

		if (userLikeRepo.findByUserFromAndUserTo(currUser, user) == null) {
			UserLike like = new UserLike();
			like.setDate(new Date());
			like.setUserFrom(currUser);
			like.setUserTo(user);
			currUser.getLikes().add(like);

			UserNotification not = new UserNotification();
			not.setContent(UserNotification.USER_LIKE);
			not.setDate(new Date());
			not.setUserFrom(currUser);
			not.setUserTo(user);
			currUser.getNotifications().add(not);
			notificationService.newLike(user);

			user.getDates().setNotificationDate(new Date());

			currUser.getHiddenUsers().removeIf(hide -> hide.getUserTo().getId().equals(user.getId()));

			userRepo.saveAndFlush(user);
			userRepo.saveAndFlush(currUser);

			if (user.getLikes().stream().anyMatch(o -> o.getUserTo().getId().equals(currUser.getId()))) {
				Conversation convo = new Conversation();
				convo.setUsers(new ArrayList<>());
				convo.setDate(new Date());
				convo.getUsers().add(currUser);
				convo.getUsers().add(user);
				convo.setLastUpdated(new Date());
				convo.setMessages(new ArrayList<>());
				conversationRepo.saveAndFlush(convo);

				notificationService.newMatch(user);
				notificationService.newMatch(currUser);

				user.getConversations().add(convo);
				currUser.getConversations().add(convo);

				userRepo.saveAndFlush(currUser);
				userRepo.saveAndFlush(user);
			}

		}
	}

	public void hideUser(String idEnc)
			throws NumberFormatException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException,
			NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, AlovoaException {
		User user = encodedIdToUser(idEnc);
		User currUser = authService.getCurrentUser();
		if (userHideRepo.findByUserFromAndUserTo(currUser, user) == null) {
			UserHide hide = new UserHide();
			hide.setDate(new Date());
			hide.setUserFrom(currUser);
			hide.setUserTo(user);
			currUser.getHiddenUsers().add(hide);
			userRepo.saveAndFlush(currUser);
		}
	}

	public void blockUser(String idEnc)
			throws AlovoaException, NumberFormatException, InvalidKeyException, IllegalBlockSizeException,
			BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException {
		User user = encodedIdToUser(idEnc);
		User currUser = authService.getCurrentUser();
		if (userBlockRepo.findByUserFromAndUserTo(currUser, user) == null) {
			UserBlock block = new UserBlock();
			block.setDate(new Date());
			block.setUserFrom(currUser);
			block.setUserTo(user);
			currUser.getBlockedUsers().add(block);
			userRepo.saveAndFlush(currUser);
		}
	}

	public void unblockUser(String idEnc)
			throws AlovoaException, NumberFormatException, InvalidKeyException, IllegalBlockSizeException,
			BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException {
		User user = encodedIdToUser(idEnc);
		User currUser = authService.getCurrentUser();

		UserBlock block = userBlockRepo.findByUserFromAndUserTo(currUser, user);
		if (block != null) {
			currUser.getBlockedUsers().remove(block);
			userRepo.save(currUser);
		}
	}

	public UserReport reportUser(String idEnc, long captchaId, String captchaText, String comment)
			throws AlovoaException, UnsupportedEncodingException, NoSuchAlgorithmException, NumberFormatException,
			InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException,
			InvalidAlgorithmParameterException {
		User user = encodedIdToUser(idEnc);
		User currUser = authService.getCurrentUser();
		if (userReportRepo.findByUserFromAndUserTo(currUser, user) == null) {

			boolean isValid = captchaService.isValid(captchaId, captchaText);
			if (!isValid) {
				throw new AlovoaException("captcha_invalid");
			}
			UserReport report = new UserReport();
			report.setDate(new Date());
			report.setUserFrom(currUser);
			report.setUserTo(user);
			report.setComment(comment);
			currUser.getReported().add(report);
			currUser = userRepo.saveAndFlush(currUser);

			return currUser.getReported().get(currUser.getReported().size() - 1);
		}

		return null;
	}

	public User encodedIdToUser(String idEnc)
			throws NumberFormatException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException,
			NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException {
		long id = UserDto.decodeId(idEnc, textEncryptor);
		return userRepo.findById(id).orElse(null);
	}

	public boolean hasNewAlert() throws AlovoaException {
		User user = authService.getCurrentUser();
		// user always check their alerts periodically in the background, so just update
		// it here
		if (user != null) {
			updateUserInfo(user);
			return user.getDates().getNotificationDate().after(user.getDates().getNotificationCheckedDate());
		} else {
			return false;
		}
	}

	public boolean hasNewMessage() throws AlovoaException {
		User user = authService.getCurrentUser();
		if (user != null && user.getDates().getMessageDate() != null
				&& user.getDates().getMessageCheckedDate() != null) {
			return user.getDates().getMessageDate().after(user.getDates().getMessageCheckedDate());
		} else {
			return false;
		}
	}

	public void updateUserInfo(User user) {
		if (user != null && user.getDates() != null) {
			user.getDates().setActiveDate(new Date());
			Locale locale = LocaleContextHolder.getLocale();
			user.setLanguage(locale.getLanguage());
			userRepo.saveAndFlush(user);
		}
	}

	public ResponseEntity<Resource> getUserdata(String idEnc) throws AlovoaException, JsonProcessingException,
			UnsupportedEncodingException, NumberFormatException, InvalidKeyException, IllegalBlockSizeException,
			BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException {

		User user = authService.getCurrentUser();
		User userFromIdEnc = encodedIdToUser(idEnc);
		if (!user.getId().equals(userFromIdEnc.getId())) {
			throw new AlovoaException("wrong_id_enc");
		}

		UserGdpr ug = UserGdpr.userToUserGdpr(user);
		String json = objectMapper.writeValueAsString(ug);
		ByteArrayResource resource = new ByteArrayResource(json.getBytes(StandardCharsets.UTF_8.name()));

		ContentDisposition contentDisposition = ContentDisposition.builder("inline").filename("alovoa_userdata.json")
				.build();

		MediaType mediaType = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(mediaType);
		headers.setContentDisposition(contentDisposition);

		return new ResponseEntity<>(resource, headers, HttpStatus.OK);
	}

	public void deleteProfilePicture() throws AlovoaException {
		User user = authService.getCurrentUser();
		user.setProfilePicture(null);
		userRepo.saveAndFlush(user);
	}

	public String getAudio(String userIdEnc)
			throws NumberFormatException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException,
			NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException {
		User user = encodedIdToUser(userIdEnc);
		if (user.getAudio() == null) {
			return null;
		}
		return user.getAudio().getData();
	}

	public void deleteAudio() throws AlovoaException {
		User user = authService.getCurrentUser();
		user.setAudio(null);
		userRepo.saveAndFlush(user);
	}

	public void updateAudio(String audioB64, String mimeType)
			throws AlovoaException, UnsupportedAudioFileException, IOException {
		User user = authService.getCurrentUser();
		String newAudioB64 = adjustAudio(audioB64, mimeType);

		if (user.getAudio() == null) {
			UserAudio audio = new UserAudio();
			audio.setData(newAudioB64);
			audio.setUser(user);
			user.setAudio(audio);
		} else {
			user.getAudio().setData(newAudioB64);
		}

		userRepo.saveAndFlush(user);
	}

	private static final String MIME_X_WAV = "x-wav";
	private static final String MIME_WAV = "wav";
	private static final String MIME_MPEG = "mpeg";
	private static final String MIME_MP3 = "mp3";

	private String adjustAudio(String audioB64, String mimeType) throws UnsupportedAudioFileException, IOException {
		if (mimeType.equals(MIME_X_WAV) || mimeType.equals(MIME_WAV)) {
			String trimmedWav = trimAudioWav(audioB64, audioMaxTime);
			return convertAudioMp3Wav(trimmedWav, MIME_MP3);
		} else if (mimeType.equals(MIME_MPEG) || mimeType.equals(MIME_MP3)) {
			String b64Wav = convertAudioMp3Wav(audioB64, MIME_WAV);
			String trimmedWav = trimAudioWav(b64Wav, audioMaxTime);
			return convertAudioMp3Wav(trimmedWav, MIME_MP3);
		}
		return null;
	}

	private static String convertAudioMp3Wav(String audioB64, String mimeType) throws UnsupportedEncodingException {
		byte[] bytes = Base64.getDecoder().decode(stripB64Type(audioB64).getBytes(StandardCharsets.UTF_8.name()));
		InputStream inputStream = new ByteArrayInputStream(bytes);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		final AudioFormat audioFormat = new AudioFormat(16000, 8, 1, false, false);
		Converter.convertFrom(inputStream).withTargetFormat(audioFormat).to(output);
		final byte[] wavContent = output.toByteArray();
		return Tools.B64AUDIOPREFIX + mimeType + Tools.B64PREFIX + Base64.getEncoder().encodeToString(wavContent);
	}

	private String trimAudioWav(String audioB64, int maxSeconds) throws UnsupportedAudioFileException, IOException {

		ByteArrayInputStream bis = null;
		AudioInputStream ais = null;
		AudioInputStream aisShort = null;
		ByteArrayOutputStream baos = null;

		try {
			byte[] decodedBytes = Base64.getDecoder().decode(stripB64Type(audioB64));
			bis = new ByteArrayInputStream(decodedBytes);
			ais = AudioSystem.getAudioInputStream(bis);
			AudioFormat format = ais.getFormat();
			long frameLength = ais.getFrameLength();

			// check if audio is shorter or equal max length
			double durationInSeconds = frameLength * format.getFrameRate();
			if (durationInSeconds < 0.0) {
				durationInSeconds = durationInSeconds * (-1);
			}
			if (durationInSeconds <= maxSeconds) {
				ais.close();
				audioB64 = Tools.B64AUDIOPREFIX + MIME_WAV + Tools.B64PREFIX + stripB64Type(audioB64);
				return audioB64;
			} else {
				long frames = (long) (format.getFrameRate() * maxSeconds);
				aisShort = new AudioInputStream(ais, format, frames);
				AudioFileFormat.Type targetType = AudioFileFormat.Type.WAVE;
				baos = new ByteArrayOutputStream();
				AudioSystem.write(aisShort, targetType, baos);

				String base64bytes = Base64.getEncoder().encodeToString(baos.toByteArray());
				base64bytes = Tools.B64AUDIOPREFIX + MIME_WAV + Tools.B64PREFIX + base64bytes;
				aisShort.close();
				ais.close();
				return base64bytes;
			}
		} catch (Exception e) {
			if (ais != null) {
				ais.close();
			}
			if (aisShort != null) {
				aisShort.close();
			}
			throw e;
		}
	}
}
