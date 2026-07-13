package com.afra7kom.backend.service;

import com.afra7kom.backend.entity.Paiement;
import com.afra7kom.backend.entity.Reservation;
import com.afra7kom.backend.entity.ReservationItem;
import com.afra7kom.backend.repository.PaiementRepository;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FactureService {

    private final PaiementRepository paiementRepository;

    // Couleurs
    private static final BaseColor PRIMARY_COLOR = new BaseColor(33, 150, 243); // Bleu
    private static final BaseColor SECONDARY_COLOR = new BaseColor(96, 125, 139); // Gris bleu
    private static final BaseColor SUCCESS_COLOR = new BaseColor(76, 175, 80); // Vert
    private static final BaseColor WARNING_COLOR = new BaseColor(255, 152, 0); // Orange

    // Polices
    private Font titleFont;
    private Font headerFont;
    private Font normalFont;
    private Font boldFont;
    private Font smallFont;

    public byte[] genererFacturePDF(Long paiementId) throws DocumentException, IOException {
        Paiement paiement = paiementRepository.findById(paiementId)
                .orElseThrow(() -> new RuntimeException("Paiement non trouvé"));

        if (paiement.getFactureNumero() == null) {
            paiement.genererNumeroFacture();
            paiementRepository.save(paiement);
        }

        return creerFacturePDF(paiement);
    }

    public byte[] genererRecapitulatifReservation(Long reservationId) throws DocumentException, IOException {
        List<Paiement> paiements = paiementRepository.findByReservationId(reservationId);
        if (paiements.isEmpty()) {
            throw new RuntimeException("Aucun paiement trouvé pour cette réservation");
        }

        return creerRecapitulatifPDF(paiements.get(0).getReservation(), paiements);
    }

    private byte[] creerFacturePDF(Paiement paiement) throws DocumentException, IOException {
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = PdfWriter.getInstance(document, baos);

        // Initialiser les polices
        initializeFonts();

        document.open();

        // En-tête
        ajouterEnTete(document, paiement);
        
        // Informations facture
        ajouterInfosFacture(document, paiement);
        
        // Informations client et réservation
        ajouterInfosClientReservation(document, paiement);
        
        // Détails du paiement
        ajouterDetailsPaiement(document, paiement);
        
        // Récapitulatif réservation
        ajouterRecapitulatifReservation(document, paiement.getReservation());
        
        // Pied de page
        ajouterPiedDePage(document);

        document.close();
        return baos.toByteArray();
    }

    private byte[] creerRecapitulatifPDF(Reservation reservation, List<Paiement> paiements) 
            throws DocumentException, IOException {
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = PdfWriter.getInstance(document, baos);

        initializeFonts();
        document.open();

        // En-tête
        ajouterEnTeteRecapitulatif(document, reservation);
        
        // Informations réservation
        ajouterInfosReservationComplete(document, reservation);
        
        // Tableau des paiements
        ajouterTableauPaiements(document, paiements);
        
        // Situation financière
        ajouterSituationFinanciere(document, reservation, paiements);
        
        // Pied de page
        ajouterPiedDePage(document);

        document.close();
        return baos.toByteArray();
    }

    private void initializeFonts() throws DocumentException, IOException {
        // Utiliser des polices intégrées pour éviter les problèmes
        titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, PRIMARY_COLOR);
        headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, SECONDARY_COLOR);
        boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.BLACK);
        normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.BLACK);
        smallFont = FontFactory.getFont(FontFactory.HELVETICA, 8, BaseColor.GRAY);
    }

    private void ajouterEnTete(Document document, Paiement paiement) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{3, 2});

        // Logo et infos entreprise
        PdfPCell companyCell = new PdfPCell();
        companyCell.setBorder(Rectangle.NO_BORDER);
        
        Paragraph companyName = new Paragraph("AFRA7KOM", titleFont);
        companyName.setSpacingAfter(5);
        companyCell.addElement(companyName);
        
        companyCell.addElement(new Paragraph("Location de matériel événementiel", normalFont));
        companyCell.addElement(new Paragraph("Casablanca, Maroc", normalFont));
        companyCell.addElement(new Paragraph("Tél: +212 XXX XXX XXX", normalFont));
        companyCell.addElement(new Paragraph("Email: contact@afra7kom.ma", normalFont));

        // Numéro de facture et date
        PdfPCell invoiceCell = new PdfPCell();
        invoiceCell.setBorder(Rectangle.NO_BORDER);
        invoiceCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        
        Paragraph factureTitle = new Paragraph("FACTURE", headerFont);
        factureTitle.setSpacingAfter(10);
        invoiceCell.addElement(factureTitle);
        
        invoiceCell.addElement(new Paragraph("N° " + paiement.getFactureNumero(), boldFont));
        invoiceCell.addElement(new Paragraph("Date: " + formatDate(LocalDateTime.now()), normalFont));
        
        headerTable.addCell(companyCell);
        headerTable.addCell(invoiceCell);
        
        document.add(headerTable);
        document.add(new Paragraph(" ", normalFont)); // Espacement
    }

    private void ajouterInfosFacture(Document document, Paiement paiement) throws DocumentException {
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setSpacingBefore(20);

        // Statut du paiement avec couleur
        BaseColor statutColor = getStatutColor(paiement.getStatut());
        Font statutFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, statutColor);
        
        PdfPCell statutCell = new PdfPCell(new Paragraph("STATUT: " + paiement.getStatutDisplayName(), statutFont));
        statutCell.setColspan(2);
        statutCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        statutCell.setPadding(10);
        statutCell.setBackgroundColor(new BaseColor(245, 245, 245));
        
        infoTable.addCell(statutCell);
        document.add(infoTable);
    }

    private void ajouterInfosClientReservation(Document document, Paiement paiement) throws DocumentException {
        Reservation reservation = paiement.getReservation();
        
        PdfPTable clientTable = new PdfPTable(2);
        clientTable.setWidthPercentage(100);
        clientTable.setSpacingBefore(20);

        // Informations client
        PdfPCell clientCell = new PdfPCell();
        clientCell.setBorder(Rectangle.BOX);
        clientCell.setPadding(10);
        
        Paragraph clientTitle = new Paragraph("FACTURER À:", headerFont);
        clientTitle.setSpacingAfter(10);
        clientCell.addElement(clientTitle);
        
        clientCell.addElement(new Paragraph(reservation.getUser().getEmail(), boldFont));
        if (reservation.getUser().getPhone() != null) {
            clientCell.addElement(new Paragraph("Tél: " + reservation.getUser().getPhone(), normalFont));
        }
        if (reservation.getDeliveryAddress() != null) {
            clientCell.addElement(new Paragraph("Adresse: " + reservation.getDeliveryAddress(), normalFont));
        }

        // Informations réservation
        PdfPCell reservationCell = new PdfPCell();
        reservationCell.setBorder(Rectangle.BOX);
        reservationCell.setPadding(10);
        
        Paragraph reservationTitle = new Paragraph("RÉSERVATION:", headerFont);
        reservationTitle.setSpacingAfter(10);
        reservationCell.addElement(reservationTitle);
        
        reservationCell.addElement(new Paragraph("N° RES-" + reservation.getId(), boldFont));
        reservationCell.addElement(new Paragraph("Période: " + formatDate(reservation.getStartDate()) + 
                                                " au " + formatDate(reservation.getEndDate()), normalFont));
        reservationCell.addElement(new Paragraph("Statut: " + reservation.getStatus().getDisplayName(), normalFont));

        clientTable.addCell(clientCell);
        clientTable.addCell(reservationCell);
        
        document.add(clientTable);
    }

    private void ajouterDetailsPaiement(Document document, Paiement paiement) throws DocumentException {
        Paragraph paymentHeader = new Paragraph("DÉTAILS DU PAIEMENT", headerFont);
        paymentHeader.setSpacingBefore(20);
        document.add(paymentHeader);
        
        PdfPTable paymentTable = new PdfPTable(2);
        paymentTable.setWidthPercentage(100);
        paymentTable.setSpacingBefore(10);

        // Détails du paiement
        ajouterLignePaiement(paymentTable, "Montant:", formatMontant(paiement.getAmount()));
        ajouterLignePaiement(paymentTable, "Type:", paiement.getTypeDisplayName());
        ajouterLignePaiement(paymentTable, "Statut:", paiement.getStatutDisplayName());
        
        if (paiement.getDatePaiement() != null) {
            ajouterLignePaiement(paymentTable, "Date de paiement:", formatDate(paiement.getDatePaiement()));
        }
        
        if (paiement.getReferenceExterne() != null) {
            ajouterLignePaiement(paymentTable, "Référence:", paiement.getReferenceExterne());
        }

        document.add(paymentTable);
    }

    private void ajouterRecapitulatifReservation(Document document, Reservation reservation) throws DocumentException {
        Paragraph reservationHeader = new Paragraph("RÉCAPITULATIF RÉSERVATION", headerFont);
        reservationHeader.setSpacingBefore(20);
        document.add(reservationHeader);
        
        // Tableau des items
        PdfPTable itemsTable = new PdfPTable(4);
        itemsTable.setWidthPercentage(100);
        itemsTable.setWidths(new float[]{3, 1, 2, 2});
        itemsTable.setSpacingBefore(10);

        // En-têtes
        ajouterCelluleEntete(itemsTable, "Article");
        ajouterCelluleEntete(itemsTable, "Qté");
        ajouterCelluleEntete(itemsTable, "Prix unitaire");
        ajouterCelluleEntete(itemsTable, "Total");

        // Items
        for (ReservationItem item : reservation.getItems()) {
            itemsTable.addCell(new PdfPCell(new Paragraph(item.getItemName(), normalFont)));
            itemsTable.addCell(new PdfPCell(new Paragraph(item.getQuantity().toString(), normalFont)));
            itemsTable.addCell(new PdfPCell(new Paragraph(formatMontant(item.getUnitPrice()), normalFont)));
            itemsTable.addCell(new PdfPCell(new Paragraph(formatMontant(item.getTotalPrice()), normalFont)));
        }

        // Total
        PdfPCell totalLabelCell = new PdfPCell(new Paragraph("TOTAL", boldFont));
        totalLabelCell.setColspan(3);
        totalLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalLabelCell.setPadding(10);
        
        PdfPCell totalValueCell = new PdfPCell(new Paragraph(formatMontant(reservation.getTotalAmount()), boldFont));
        totalValueCell.setPadding(10);
        totalValueCell.setBackgroundColor(new BaseColor(240, 240, 240));

        itemsTable.addCell(totalLabelCell);
        itemsTable.addCell(totalValueCell);

        document.add(itemsTable);
    }

    private void ajouterTableauPaiements(Document document, List<Paiement> paiements) throws DocumentException {
        Paragraph paymentsHeader = new Paragraph("HISTORIQUE DES PAIEMENTS", headerFont);
        paymentsHeader.setSpacingBefore(20);
        document.add(paymentsHeader);
        
        PdfPTable paymentsTable = new PdfPTable(5);
        paymentsTable.setWidthPercentage(100);
        paymentsTable.setWidths(new float[]{2, 2, 2, 2, 2});
        paymentsTable.setSpacingBefore(10);

        // En-têtes
        ajouterCelluleEntete(paymentsTable, "Date");
        ajouterCelluleEntete(paymentsTable, "Montant");
        ajouterCelluleEntete(paymentsTable, "Type");
        ajouterCelluleEntete(paymentsTable, "Statut");
        ajouterCelluleEntete(paymentsTable, "Référence");

        // Paiements
        for (Paiement paiement : paiements) {
            paymentsTable.addCell(new PdfPCell(new Paragraph(
                paiement.getDatePaiement() != null ? formatDate(paiement.getDatePaiement()) : "En attente", 
                normalFont)));
            paymentsTable.addCell(new PdfPCell(new Paragraph(formatMontant(paiement.getAmount()), normalFont)));
            paymentsTable.addCell(new PdfPCell(new Paragraph(paiement.getTypeDisplayName(), normalFont)));
            paymentsTable.addCell(new PdfPCell(new Paragraph(paiement.getStatutDisplayName(), normalFont)));
            paymentsTable.addCell(new PdfPCell(new Paragraph(
                paiement.getReferenceExterne() != null ? paiement.getReferenceExterne() : "-", 
                normalFont)));
        }

        document.add(paymentsTable);
    }

    private void ajouterSituationFinanciere(Document document, Reservation reservation, List<Paiement> paiements) 
            throws DocumentException {
        Paragraph situationHeader = new Paragraph("SITUATION FINANCIÈRE", headerFont);
        situationHeader.setSpacingBefore(20);
        document.add(situationHeader);
        
        // Calculer les montants
        BigDecimal totalPaye = paiements.stream()
                .filter(Paiement::isPaye)
                .map(Paiement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal resteAPayer = reservation.getTotalAmount().subtract(totalPaye);
        
        PdfPTable situationTable = new PdfPTable(2);
        situationTable.setWidthPercentage(100);
        situationTable.setSpacingBefore(10);

        ajouterLignePaiement(situationTable, "Total réservation:", formatMontant(reservation.getTotalAmount()));
        ajouterLignePaiement(situationTable, "Total payé:", formatMontant(totalPaye));
        
        // Reste à payer avec couleur
        BaseColor couleurReste = resteAPayer.compareTo(BigDecimal.ZERO) > 0 ? WARNING_COLOR : SUCCESS_COLOR;
        Font resteFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, couleurReste);
        
        PdfPCell resteLabelCell = new PdfPCell(new Paragraph("Reste à payer:", boldFont));
        PdfPCell resteValueCell = new PdfPCell(new Paragraph(formatMontant(resteAPayer), resteFont));
        resteValueCell.setBackgroundColor(new BaseColor(245, 245, 245));
        
        situationTable.addCell(resteLabelCell);
        situationTable.addCell(resteValueCell);

        document.add(situationTable);
    }

    private void ajouterPiedDePage(Document document) throws DocumentException {
        Paragraph spacer = new Paragraph(" ", normalFont);
        spacer.setSpacingBefore(30);
        document.add(spacer);
        
        Paragraph footer = new Paragraph(
            "Merci de votre confiance ! Pour toute question, contactez-nous à contact@afra7kom.ma", 
            smallFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);
        
        Paragraph generated = new Paragraph(
            "Facture générée le " + formatDate(LocalDateTime.now()), 
            smallFont);
        generated.setAlignment(Element.ALIGN_CENTER);
        generated.setSpacingBefore(10);
        document.add(generated);
    }

    // Méthodes utilitaires
    private void ajouterLignePaiement(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Paragraph(label, boldFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(5);
        
        PdfPCell valueCell = new PdfPCell(new Paragraph(value, normalFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(5);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void ajouterCelluleEntete(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, boldFont));
        cell.setBackgroundColor(PRIMARY_COLOR);
        cell.setPadding(8);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        
        // Texte blanc sur fond bleu
        Paragraph p = new Paragraph(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.WHITE));
        cell = new PdfPCell(p);
        cell.setBackgroundColor(PRIMARY_COLOR);
        cell.setPadding(8);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        
        table.addCell(cell);
    }

    private BaseColor getStatutColor(Paiement.StatutPaiement statut) {
        switch (statut) {
            case ACOMPTE:
            case SOLDE:
                return SUCCESS_COLOR;
            case IMPAYE:
                return WARNING_COLOR;
            default:
                return SECONDARY_COLOR;
        }
    }

    private String formatDate(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private String formatDate(java.time.LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private String formatMontant(BigDecimal montant) {
        return String.format("%.2f MAD", montant);
    }

    private void ajouterEnTeteRecapitulatif(Document document, Reservation reservation) throws DocumentException {
        Paragraph title = new Paragraph("RÉCAPITULATIF DE RÉSERVATION", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);
        
        Paragraph reservationRef = new Paragraph("Réservation N° RES-" + reservation.getId(), headerFont);
        reservationRef.setAlignment(Element.ALIGN_CENTER);
        reservationRef.setSpacingAfter(30);
        document.add(reservationRef);
    }

    private void ajouterInfosReservationComplete(Document document, Reservation reservation) throws DocumentException {
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);

        ajouterLignePaiement(infoTable, "Client:", reservation.getUser().getEmail());
        ajouterLignePaiement(infoTable, "Période:", 
            formatDate(reservation.getStartDate()) + " au " + formatDate(reservation.getEndDate()));
        ajouterLignePaiement(infoTable, "Statut:", reservation.getStatus().getDisplayName());
        ajouterLignePaiement(infoTable, "Créée le:", formatDate(reservation.getCreatedAt()));

        document.add(infoTable);
    }
}



