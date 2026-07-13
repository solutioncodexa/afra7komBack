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
    private CatalogDto catalog;
    private ReservationDto reservation;

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

    @Data
    public static class CatalogDto {
        private String heroTitle = "Buffet de";
        private String heroHighlight = "Fête";
        private String heroDescription = "Packs, buffets, gâteaux et matériel premium pour vos mariages, fiançailles et célébrations — réservez en ligne en quelques clics.";
        private String statQualityValue = "100%";
        private String statQualityLabel = "Qualité";
        private String statSupportValue = "24/7";
        private String statSupportLabel = "Support";
    }

    @Data
    public static class ReservationDto {
        private String bookingTitle = "Composez votre événement";
        private String deliveryNotice = "Les frais de livraison ne sont pas inclus dans le prix affiché.";
        private String featureTagDelivery = "Livraison incluse";
        private String featureTagInstallation = "Installation";
        private String featureTagPickup = "Reprise sur place";
        private String whatsappHelpPrefix = "Besoin d'aide ?";
        private String whatsappHelpLinkLabel = "Contactez-nous sur WhatsApp";
        private String stockOutMessage = "Ce matériel est en rupture de stock (0 unité disponible). Les dates apparaissent grisées car aucune réservation n'est possible pour le moment.";
    }
}
