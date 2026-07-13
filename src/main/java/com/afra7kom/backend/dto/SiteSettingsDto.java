package com.afra7kom.backend.dto;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class SiteSettingsDto {
    private CompanyDto company;
    private ContactDto contact;
    private SocialDto social;
    private ThemeDto theme;
    private HomepageDto homepage;

    @Data
    public static class CompanyDto {
        private String name;
        private String logo;
        private String tagline;
        private String description;
        private Integer foundedYear;
    }

    @Data
    public static class ContactDto {
        private String phone;
        private String whatsapp;
        private String email;
        private String address;
        private String city;
        private String country;
    }

    @Data
    public static class SocialDto {
        private String facebook;
        private String instagram;
        private String tiktok;
        private String whatsapp;
    }

    @Data
    public static class ThemeDto {
        private String primaryColor;
        private String secondaryColor;
        private String accentColor;
        private String fontFamily;
        private String headingFontFamily;
        private String heroGradientStart;
        private String heroGradientEnd;
        private String heroTitleColor;
        private String heroHighlightColor;
    }

    @Data
    public static class HomepageDto {
        private Map<String, Boolean> sections = new HashMap<>();
        private Map<String, Integer> displayCounts = new HashMap<>();
        private RealisationsDto realisations = new RealisationsDto();
    }

    @Data
    public static class RealisationsDto {
        /** Image hero — Réalisation 1 */
        private String heroImageUrl = "";
        private String heroImageAlt = "Décor de cérémonie marocain";
        /** Image section à propos — Réalisation 2 */
        private String aboutImageUrl = "";
        private String aboutImageAlt = "Salle de réception marocaine décorée";
    }
}
