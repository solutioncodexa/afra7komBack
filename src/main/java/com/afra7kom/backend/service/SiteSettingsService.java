package com.afra7kom.backend.service;

import com.afra7kom.backend.dto.SiteSettingsDto;
import com.afra7kom.backend.entity.SiteSettings;
import com.afra7kom.backend.exception.BadRequestException;
import com.afra7kom.backend.repository.SiteSettingsRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SiteSettingsService {

    private static final Long SETTINGS_ID = 1L;

    private final SiteSettingsRepository siteSettingsRepository;
    private final ObjectMapper objectMapper;
    private final FileStorageService fileStorageService;

    @PostConstruct
    public void initializeDefaults() {
        if (!siteSettingsRepository.existsById(SETTINGS_ID)) {
            SiteSettings settings = new SiteSettings();
            settings.setId(SETTINGS_ID);
            settings.setConfigJson(serialize(createDefaultSettings()));
            settings.setUpdatedAt(LocalDateTime.now());
            siteSettingsRepository.save(settings);
            log.info("Paramètres du site initialisés avec les valeurs par défaut");
        }
    }

    @Transactional(readOnly = true)
    public SiteSettingsDto getSettings() {
        SiteSettings settings = siteSettingsRepository.findById(SETTINGS_ID)
                .orElseThrow(() -> new BadRequestException("Paramètres du site introuvables"));
        return deserialize(settings.getConfigJson());
    }

    @Transactional
    public SiteSettingsDto updateSettings(SiteSettingsDto dto) {
        SiteSettings settings = siteSettingsRepository.findById(SETTINGS_ID)
                .orElseGet(() -> {
                    SiteSettings s = new SiteSettings();
                    s.setId(SETTINGS_ID);
                    return s;
                });

        SiteSettingsDto merged = mergeWithDefaults(dto);
        settings.setConfigJson(serialize(merged));
        settings.setUpdatedAt(LocalDateTime.now());
        siteSettingsRepository.save(settings);
        return merged;
    }

    @Transactional
    public SiteSettingsDto uploadLogo(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Fichier logo requis");
        }

        String logoUrl = fileStorageService.storeImage(file, "site");
        SiteSettingsDto settings = getSettings();
        if (settings.getCompany() == null) {
            settings.setCompany(new SiteSettingsDto.CompanyDto());
        }
        settings.getCompany().setLogo(logoUrl);
        return updateSettings(settings);
    }

    @Transactional
    public SiteSettingsDto uploadHomepageHeroImage(MultipartFile file) throws IOException {
        return uploadHomepageImage(file, true);
    }

    @Transactional
    public SiteSettingsDto uploadHomepageAboutImage(MultipartFile file) throws IOException {
        return uploadHomepageImage(file, false);
    }

    private SiteSettingsDto uploadHomepageImage(MultipartFile file, boolean hero) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Fichier image requis");
        }

        String folder = hero ? "homepage/hero" : "homepage/about";
        String imageUrl = fileStorageService.storeImage(file, folder);
        SiteSettingsDto settings = getSettings();
        ensureHomepage(settings);
        SiteSettingsDto.RealisationsDto realisations = settings.getHomepage().getRealisations();
        if (hero) {
            realisations.setHeroImageUrl(imageUrl);
        } else {
            realisations.setAboutImageUrl(imageUrl);
        }
        return updateSettings(settings);
    }

    private void ensureHomepage(SiteSettingsDto settings) {
        if (settings.getHomepage() == null) {
            settings.setHomepage(new SiteSettingsDto.HomepageDto());
        }
        if (settings.getHomepage().getRealisations() == null) {
            settings.getHomepage().setRealisations(new SiteSettingsDto.RealisationsDto());
        }
    }

    private SiteSettingsDto deserialize(String json) {
        try {
            SiteSettingsDto dto = objectMapper.readValue(json, SiteSettingsDto.class);
            return mergeWithDefaults(dto);
        } catch (JsonProcessingException e) {
            log.error("Erreur de désérialisation des paramètres du site", e);
            return createDefaultSettings();
        }
    }

    private String serialize(SiteSettingsDto dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Impossible de sauvegarder les paramètres du site");
        }
    }

    private SiteSettingsDto mergeWithDefaults(SiteSettingsDto incoming) {
        SiteSettingsDto defaults = createDefaultSettings();
        if (incoming == null) {
            return defaults;
        }

        if (incoming.getCompany() != null) {
            defaults.setCompany(incoming.getCompany());
        }
        if (incoming.getContact() != null) {
            defaults.setContact(incoming.getContact());
        }
        if (incoming.getSocial() != null) {
            defaults.setSocial(incoming.getSocial());
        }
        if (incoming.getTheme() != null) {
            defaults.setTheme(incoming.getTheme());
        }
        if (incoming.getHomepage() != null) {
            if (incoming.getHomepage().getSections() != null) {
                defaults.getHomepage().getSections().putAll(incoming.getHomepage().getSections());
            }
            if (incoming.getHomepage().getDisplayCounts() != null) {
                defaults.getHomepage().getDisplayCounts().putAll(incoming.getHomepage().getDisplayCounts());
            }
            if (incoming.getHomepage().getRealisations() != null) {
                SiteSettingsDto.RealisationsDto src = incoming.getHomepage().getRealisations();
                SiteSettingsDto.RealisationsDto dest = defaults.getHomepage().getRealisations();
                if (src.getHeroImageUrl() != null) dest.setHeroImageUrl(src.getHeroImageUrl());
                if (src.getHeroImageAlt() != null) dest.setHeroImageAlt(src.getHeroImageAlt());
                if (src.getAboutImageUrl() != null) dest.setAboutImageUrl(src.getAboutImageUrl());
                if (src.getAboutImageAlt() != null) dest.setAboutImageAlt(src.getAboutImageAlt());
            }
        }
        return defaults;
    }

    private SiteSettingsDto createDefaultSettings() {
        SiteSettingsDto dto = new SiteSettingsDto();

        SiteSettingsDto.CompanyDto company = new SiteSettingsDto.CompanyDto();
        company.setName("Afra7kom");
        company.setLogo("assets/images/logo.png");
        company.setTagline("Créez des moments magiques inoubliables");
        company.setDescription("Votre partenaire de confiance pour tous vos événements au Maroc.");
        company.setFoundedYear(2020);
        dto.setCompany(company);

        SiteSettingsDto.ContactDto contact = new SiteSettingsDto.ContactDto();
        contact.setPhone("+212 706 532 774");
        contact.setWhatsapp("+212706532774");
        contact.setEmail("contact@afra7kom.ma");
        contact.setAddress("Voir sur Google Maps");
        contact.setCity("Casablanca");
        contact.setCountry("Maroc");
        dto.setContact(contact);

        SiteSettingsDto.SocialDto social = new SiteSettingsDto.SocialDto();
        social.setFacebook("https://www.facebook.com/share/1A2AfHYxEE/?mibextid=wwXIfr");
        social.setInstagram("https://www.instagram.com/afra77kom?igsh=b3g3Ymp3Y2o2bWZk");
        social.setTiktok("https://www.tiktok.com/@afra7kom?_t=ZS-90uDXdZQejx&_r=1");
        social.setWhatsapp("https://wa.me/212706532774");
        dto.setSocial(social);

        SiteSettingsDto.ThemeDto theme = new SiteSettingsDto.ThemeDto();
        theme.setPrimaryColor("#8b2942");
        theme.setSecondaryColor("#d4869a");
        theme.setAccentColor("#5c1a2e");
        theme.setFontFamily("Lato");
        theme.setHeadingFontFamily("Playfair Display");
        theme.setHeroGradientStart("#f8d7de");
        theme.setHeroGradientEnd("#d4869a");
        theme.setHeroTitleColor("#ffffff");
        theme.setHeroHighlightColor("#5c1a2e");
        dto.setTheme(theme);

        SiteSettingsDto.HomepageDto homepage = new SiteSettingsDto.HomepageDto();
        Map<String, Boolean> sections = new HashMap<>();
        sections.put("home", true);
        sections.put("about", true);
        sections.put("gallery", true);
        sections.put("packs", true);
        sections.put("buffets", true);
        sections.put("packBuffets", true);
        sections.put("cadeaux", true);
        sections.put("gateaux", true);
        sections.put("contact", true);
        homepage.setSections(sections);

        Map<String, Integer> displayCounts = new HashMap<>();
        displayCounts.put("PACK", 3);
        displayCounts.put("BUFFET", 3);
        displayCounts.put("PACK_BUFFET", 3);
        displayCounts.put("CADEAU", 3);
        displayCounts.put("GATEAU", 3);
        displayCounts.put("MATERIEL", 3);
        displayCounts.put("GALLERY", 3);
        homepage.setDisplayCounts(displayCounts);
        homepage.setRealisations(new SiteSettingsDto.RealisationsDto());
        dto.setHomepage(homepage);

        return dto;
    }
}
