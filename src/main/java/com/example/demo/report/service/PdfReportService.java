package com.example.demo.report.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.example.demo.report.dto.AiReportResponse;
import com.example.demo.report.dto.ImplementedFeature;
import com.example.demo.report.dto.ReportStatistics;

@Service
public class PdfReportService {

    private static final float PAGE_MARGIN = 52f;
    private static final float CONTENT_WIDTH =
            PDRectangle.A4.getWidth()
                    - PAGE_MARGIN * 2;

    private static final float TITLE_SIZE = 21f;
    private static final float SECTION_SIZE = 15f;
    private static final float BODY_SIZE = 10.5f;
    private static final float SMALL_SIZE = 9f;

    private static final float BODY_LINE_HEIGHT = 17f;
    private static final float SMALL_LINE_HEIGHT = 14f;

    private static final int MAX_REPORT_TEXT_LENGTH = 20_000;
    private static final int MAX_FEATURE_GROUPS = 50;
    private static final int MAX_FEATURE_ITEMS_PER_GROUP = 100;
    private static final int MAX_FUTURE_IMPROVEMENTS = 100;
    private static final int MAX_SHORT_TEXT_LENGTH = 2_000;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern(
                    "yyyy.MM.dd"
            );

    private final String koreanFontPath;
    private final String japaneseFontPath;

    public PdfReportService(
            @Value(
                    "${report.pdf.korean-font-path:"
                            + "classpath:fonts/NanumGothic.ttf}"
            )
            String koreanFontPath,

            @Value(
                    "${report.pdf.japanese-font-path:"
                            + "classpath:fonts/ipaexg.ttf}"
            )
            String japaneseFontPath
    ) {
        this.koreanFontPath = koreanFontPath;
        this.japaneseFontPath = japaneseFontPath;
    }

    /**
     * 기존 Controller 코드와의 호환을 위한 메서드입니다.
     * 보고서 본문에 일본어 가나가 있으면 일본어 폰트를,
     * 그렇지 않으면 한국어 폰트를 자동으로 선택합니다.
     */
    public byte[] createPdf(
            AiReportResponse report
    ) {
        ReportLanguage detectedLanguage =
                detectReportLanguage(report);

        return createPdf(
                report,
                detectedLanguage.code
        );
    }

    /**
     * Controller에서 language 파라미터를 명시적으로 넘길 때 사용할 수 있습니다.
     * 지원값은 ko, ja이며 그 외 값은 ko로 처리합니다.
     */
    public byte[] createPdf(
            AiReportResponse report,
            String language
    ) {

        validateReport(report);

        ReportLanguage reportLanguage =
                ReportLanguage.from(language);

        PdfText text =
                PdfText.forLanguage(
                        reportLanguage
                );

        try (
                PDDocument document =
                        new PDDocument();

                ByteArrayOutputStream outputStream =
                        new ByteArrayOutputStream()
        ) {

            PDType0Font font =
                    loadFont(
                            document,
                            reportLanguage
                    );

            PdfWriter writer =
                    new PdfWriter(
                            document,
                            font
                    );

            writeReport(
                    writer,
                    report,
                    text,
                    reportLanguage
            );

            writer.close();

            document.save(outputStream);

            return outputStream.toByteArray();

        } catch (IOException exception) {
            throw new RuntimeException(
                    reportLanguage == ReportLanguage.JAPANESE
                            ? "PDFレポートを生成できませんでした。"
                            : "PDF 보고서를 생성하지 못했습니다.",
                    exception
            );
        }
    }

    private void writeReport(
            PdfWriter writer,
            AiReportResponse report,
            PdfText text,
            ReportLanguage language
    ) throws IOException {

        writer.writeCenteredText(
                text.title,
                TITLE_SIZE,
                0f,
                28f
        );

        writer.writeCenteredText(
                text.subtitle,
                BODY_SIZE,
                0f,
                28f
        );

        writer.writeHorizontalLine(
                12f,
                24f
        );

        writeStatistics(
                writer,
                report.getStatistics(),
                text,
                language
        );

        writer.writeSectionTitle(
                text.workSummaryTitle
        );

        writer.writeParagraph(
                report.getWorkSummary()
        );

        writer.writeSectionTitle(
                text.implementedFeaturesTitle
        );

        writeImplementedFeatures(
                writer,
                report.getImplementedFeatures(),
                text
        );

        writer.writeSectionTitle(
                text.difficultyAnalysisTitle
        );

        writer.writeParagraph(
                report.getDifficultyAnalysis()
        );

        writer.writeSectionTitle(
                text.projectAchievementsTitle
        );

        writer.writeParagraph(
                report.getProjectAchievements()
        );

        writer.writeSectionTitle(
                text.futureImprovementsTitle
        );

        writeFutureImprovements(
                writer,
                report.getFutureImprovements(),
                text
        );

        writer.writeHorizontalLine(
                10f,
                18f
        );

        writer.writeText(
                text.generatedDateLabel
                        + LocalDate.now()
                                .format(
                                        DATE_FORMATTER
                                ),
                SMALL_SIZE,
                SMALL_LINE_HEIGHT,
                0f
        );
    }

    private void writeStatistics(
            PdfWriter writer,
            ReportStatistics statistics,
            PdfText text,
            ReportLanguage language
    ) throws IOException {

        writer.writeSectionTitle(
                text.overviewTitle
        );

        writer.writeText(
                text.totalWorkLogsLabel
                        + statistics.getTotalWorkLogs()
                        + text.countSuffix,
                BODY_SIZE,
                BODY_LINE_HEIGHT,
                0f
        );

        writer.writeText(
                text.workPeriodLabel
                        + formatDate(
                                statistics.getStartDate()
                        )
                        + " ~ "
                        + formatDate(
                                statistics.getEndDate()
                        ),
                BODY_SIZE,
                BODY_LINE_HEIGHT,
                0f
        );

        writer.writeText(
                text.averageDifficultyLabel
                        + statistics.getAverageDifficulty()
                        + " / 3.0",
                BODY_SIZE,
                BODY_LINE_HEIGHT,
                0f
        );

        writer.writeText(
                text.difficultyDistributionLabel
                        + formatMap(
                                statistics.getDifficultyCounts(),
                                language
                        ),
                BODY_SIZE,
                BODY_LINE_HEIGHT,
                0f
        );

        writer.writeWrappedText(
                text.mainTechnologyTagsLabel
                        + formatMap(
                                statistics.getTagCounts(),
                                language
                        ),
                BODY_SIZE,
                BODY_LINE_HEIGHT,
                0f
        );

        writer.addSpacing(12f);
    }

    private void writeImplementedFeatures(
            PdfWriter writer,
            List<ImplementedFeature> implementedFeatures,
            PdfText text
    ) throws IOException {

        if (implementedFeatures == null
                || implementedFeatures.isEmpty()) {

            writer.writeText(
                    text.noImplementedFeatures,
                    BODY_SIZE,
                    BODY_LINE_HEIGHT,
                    0f
            );

            return;
        }

        for (int index = 0;
             index < implementedFeatures.size();
             index++) {

            ImplementedFeature feature =
                    implementedFeatures.get(index);

            writer.writeSubTitle(
                    "2."
                            + (index + 1)
                            + " "
                            + safeText(
                                    feature.getCategory()
                            )
            );

            if (feature.getFeatures() != null) {
                for (String value :
                        feature.getFeatures()) {

                    writer.writeWrappedText(
                            "• " + safeText(value),
                            BODY_SIZE,
                            BODY_LINE_HEIGHT,
                            12f
                    );
                }
            }

            if (feature.getDescription() != null
                    && !feature.getDescription()
                            .isBlank()) {

                writer.writeParagraph(
                        feature.getDescription()
                );
            }

            writer.addSpacing(8f);
        }
    }

    private void writeFutureImprovements(
            PdfWriter writer,
            List<String> futureImprovements,
            PdfText text
    ) throws IOException {

        if (futureImprovements == null
                || futureImprovements.isEmpty()) {

            writer.writeText(
                    text.noFutureImprovements,
                    BODY_SIZE,
                    BODY_LINE_HEIGHT,
                    0f
            );

            return;
        }

        for (int index = 0;
             index < futureImprovements.size();
             index++) {

            writer.writeWrappedText(
                    (index + 1)
                            + ". "
                            + safeText(
                                    futureImprovements.get(index)
                            ),
                    BODY_SIZE,
                    BODY_LINE_HEIGHT,
                    0f
            );
        }
    }

    private PDType0Font loadFont(
            PDDocument document,
            ReportLanguage language
    ) throws IOException {

        String configuredPath =
                language == ReportLanguage.JAPANESE
                        ? japaneseFontPath
                        : koreanFontPath;

        String fallbackPath =
                language == ReportLanguage.JAPANESE
                        ? "classpath:fonts/ipaexg.ttf"
                        : "classpath:fonts/NanumGothic.ttf";

        String fontPath =
                configuredPath == null
                        || configuredPath.isBlank()
                        ? fallbackPath
                        : configuredPath.trim();

        Resource fontResource =
                createFontResource(fontPath);

        if (!fontResource.exists()
                || !fontResource.isReadable()) {

            throw new IllegalStateException(
                    "PDF font not found or not readable. "
                            + "language="
                            + language.code
                            + ", path="
                            + fontPath
            );
        }

        try (
                InputStream inputStream =
                        fontResource.getInputStream()
        ) {
            return PDType0Font.load(
                    document,
                    inputStream,
                    true
            );
        }
    }

    private Resource createFontResource(
            String fontPath
    ) {

        if (fontPath.startsWith(
                "classpath:"
        )) {

            String classpathLocation =
                    fontPath.substring(
                            "classpath:".length()
                    );

            while (classpathLocation.startsWith(
                    "/"
            )) {
                classpathLocation =
                        classpathLocation.substring(1);
            }

            return new ClassPathResource(
                    classpathLocation
            );
        }

        return new FileSystemResource(
                fontPath
        );
    }

    private ReportLanguage detectReportLanguage(
            AiReportResponse report
    ) {

        if (report == null) {
            return ReportLanguage.KOREAN;
        }

        StringBuilder combinedText =
                new StringBuilder();

        appendText(
                combinedText,
                report.getWorkSummary()
        );

        appendText(
                combinedText,
                report.getDifficultyAnalysis()
        );

        appendText(
                combinedText,
                report.getProjectAchievements()
        );

        if (report.getFutureImprovements() != null) {
            report.getFutureImprovements()
                    .forEach(value ->
                            appendText(
                                    combinedText,
                                    value
                            )
                    );
        }

        if (report.getImplementedFeatures() != null) {
            for (ImplementedFeature feature :
                    report.getImplementedFeatures()) {

                if (feature == null) {
                    continue;
                }

                appendText(
                        combinedText,
                        feature.getCategory()
                );

                appendText(
                        combinedText,
                        feature.getDescription()
                );

                if (feature.getFeatures() != null) {
                    feature.getFeatures()
                            .forEach(value ->
                                    appendText(
                                            combinedText,
                                            value
                                    )
                            );
                }
            }
        }

        String value =
                combinedText.toString();

        for (int index = 0;
             index < value.length();) {

            int codePoint =
                    value.codePointAt(index);

            if (isJapaneseKana(codePoint)) {
                return ReportLanguage.JAPANESE;
            }

            index += Character.charCount(
                    codePoint
            );
        }

        return ReportLanguage.KOREAN;
    }

    private void appendText(
            StringBuilder builder,
            String value
    ) {
        if (value != null
                && !value.isBlank()) {
            builder.append(value)
                    .append('\n');
        }
    }

    private boolean isJapaneseKana(
            int codePoint
    ) {
        return (codePoint >= 0x3040
                && codePoint <= 0x309F)
                || (codePoint >= 0x30A0
                && codePoint <= 0x30FF)
                || (codePoint >= 0x31F0
                && codePoint <= 0x31FF)
                || (codePoint >= 0xFF66
                && codePoint <= 0xFF9D);
    }

    private void validateReport(
            AiReportResponse report
    ) {

        if (report == null) {
            throw new IllegalArgumentException(
                    "PDF로 변환할 보고서가 없습니다."
            );
        }

        if (report.getStatistics() == null) {
            throw new IllegalArgumentException(
                    "보고서 통계 데이터가 없습니다."
            );
        }

        validateTextLength(
                report.getWorkSummary(),
                MAX_REPORT_TEXT_LENGTH,
                "업무 요약"
        );
        validateTextLength(
                report.getDifficultyAnalysis(),
                MAX_REPORT_TEXT_LENGTH,
                "난이도 분석"
        );
        validateTextLength(
                report.getProjectAchievements(),
                MAX_REPORT_TEXT_LENGTH,
                "프로젝트 성과"
        );

        List<ImplementedFeature> implementedFeatures =
                report.getImplementedFeatures();

        if (implementedFeatures != null) {
            if (implementedFeatures.size() > MAX_FEATURE_GROUPS) {
                throw new IllegalArgumentException(
                        "구현 기능 그룹이 너무 많습니다."
                );
            }

            for (ImplementedFeature feature : implementedFeatures) {
                if (feature == null) {
                    continue;
                }

                validateTextLength(
                        feature.getCategory(),
                        MAX_SHORT_TEXT_LENGTH,
                        "기능 카테고리"
                );
                validateTextLength(
                        feature.getDescription(),
                        MAX_REPORT_TEXT_LENGTH,
                        "기능 설명"
                );

                List<String> features = feature.getFeatures();
                if (features != null) {
                    if (features.size() > MAX_FEATURE_ITEMS_PER_GROUP) {
                        throw new IllegalArgumentException(
                                "기능 항목이 너무 많습니다."
                        );
                    }

                    for (String value : features) {
                        validateTextLength(
                                value,
                                MAX_SHORT_TEXT_LENGTH,
                                "기능 항목"
                        );
                    }
                }
            }
        }

        List<String> futureImprovements =
                report.getFutureImprovements();

        if (futureImprovements != null) {
            if (futureImprovements.size() > MAX_FUTURE_IMPROVEMENTS) {
                throw new IllegalArgumentException(
                        "향후 개선 항목이 너무 많습니다."
                );
            }

            for (String value : futureImprovements) {
                validateTextLength(
                        value,
                        MAX_SHORT_TEXT_LENGTH,
                        "향후 개선 항목"
                );
            }
        }
    }

    private void validateTextLength(
            String value,
            int maxLength,
            String fieldName
    ) {
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException(
                    fieldName + "은(는) " + maxLength
                            + "자 이하로 입력해 주세요."
            );
        }
    }

    private String formatDate(
            LocalDate date
    ) {

        if (date == null) {
            return "-";
        }

        return date.format(
                DATE_FORMATTER
        );
    }

    private String formatMap(
            Map<String, Long> values,
            ReportLanguage language
    ) {

        if (values == null
                || values.isEmpty()) {

            return "-";
        }

        String countSuffix =
                language == ReportLanguage.JAPANESE
                        ? "件"
                        : "건";

        return values.entrySet()
                .stream()
                .map(entry ->
                        localizeMapKey(
                                entry.getKey(),
                                language
                        )
                                + " "
                                + entry.getValue()
                                + countSuffix
                )
                .reduce(
                        (first, second) ->
                                first + ", " + second
                )
                .orElse("-");
    }

    private String localizeMapKey(
            String key,
            ReportLanguage language
    ) {

        String normalized =
                safeText(key)
                        .toLowerCase();

        if (language == ReportLanguage.JAPANESE) {
            return switch (normalized) {
                case "초급", "初級", "beginner" -> "初級";
                case "중급", "中級", "intermediate" -> "中級";
                case "고급", "상급", "上級", "advanced" -> "上級";
                case "미분류", "未分類", "unclassified" -> "未分類";
                default -> safeText(key);
            };
        }

        return switch (normalized) {
            case "초급", "初級", "beginner" -> "초급";
            case "중급", "中級", "intermediate" -> "중급";
            case "고급", "상급", "上級", "advanced" -> "고급";
            case "미분류", "未分類", "unclassified" -> "미분류";
            default -> safeText(key);
        };
    }

    private static String safeText(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value.trim();
    }

    private enum ReportLanguage {
        KOREAN("ko"),
        JAPANESE("ja");

        private final String code;

        ReportLanguage(
                String code
        ) {
            this.code = code;
        }

        private static ReportLanguage from(
                String language
        ) {
            if ("ja".equalsIgnoreCase(language)) {
                return JAPANESE;
            }

            return KOREAN;
        }
    }

    private record PdfText(
            String title,
            String subtitle,
            String overviewTitle,
            String totalWorkLogsLabel,
            String workPeriodLabel,
            String averageDifficultyLabel,
            String difficultyDistributionLabel,
            String mainTechnologyTagsLabel,
            String workSummaryTitle,
            String implementedFeaturesTitle,
            String difficultyAnalysisTitle,
            String projectAchievementsTitle,
            String futureImprovementsTitle,
            String generatedDateLabel,
            String countSuffix,
            String noImplementedFeatures,
            String noFutureImprovements
    ) {

        private static PdfText forLanguage(
                ReportLanguage language
        ) {

            if (language == ReportLanguage.JAPANESE) {
                return new PdfText(
                        "AI 最終業務レポート",
                        "全業務日誌に基づく総合分析",
                        "レポート概要",
                        "業務日誌の総数: ",
                        "業務期間: ",
                        "平均難易度: ",
                        "難易度分布: ",
                        "主要技術タグ: ",
                        "1. これまでの業務要約",
                        "2. 実装した機能",
                        "3. 難易度の解説",
                        "4. プロジェクト成果",
                        "5. 今後の改善事項",
                        "レポート生成日: ",
                        "件",
                        "整理された実装機能はありません。",
                        "整理された改善事項はありません。"
                );
            }

            return new PdfText(
                    "AI 최종 업무 보고서",
                    "전체 업무 로그 기반 종합 분석",
                    "보고서 개요",
                    "총 업무 로그: ",
                    "업무 기간: ",
                    "평균 난이도: ",
                    "난이도 분포: ",
                    "주요 기술 태그: ",
                    "1. 지금까지 한 일들 요약",
                    "2. 구현 기능들",
                    "3. 난이도 해설",
                    "4. 프로젝트 성과",
                    "5. 향후 개선 사항",
                    "보고서 생성일: ",
                    "건",
                    "정리된 구현 기능이 없습니다.",
                    "정리된 향후 개선 사항이 없습니다."
            );
        }
    }

    private static class PdfWriter {

        private final PDDocument document;
        private final PDType0Font font;

        private PDPage page;
        private PDPageContentStream contentStream;
        private float currentY;

        private PdfWriter(
                PDDocument document,
                PDType0Font font
        ) throws IOException {

            this.document = document;
            this.font = font;

            createPage();
        }

        private void createPage()
                throws IOException {

            closeCurrentStream();

            page = new PDPage(
                    PDRectangle.A4
            );

            document.addPage(page);

            contentStream =
                    new PDPageContentStream(
                            document,
                            page
                    );

            currentY =
                    page.getMediaBox()
                            .getHeight()
                            - PAGE_MARGIN;
        }

        private void writeCenteredText(
                String text,
                float fontSize,
                float topSpacing,
                float bottomSpacing
        ) throws IOException {

            addSpacing(topSpacing);

            ensureSpace(
                    fontSize + bottomSpacing
            );

            float textWidth =
                    getTextWidth(
                            text,
                            fontSize
                    );

            float x =
                    Math.max(
                            PAGE_MARGIN,
                            (
                                    page.getMediaBox()
                                            .getWidth()
                                            - textWidth
                            ) / 2
                    );

            writeSingleLine(
                    text,
                    fontSize,
                    x,
                    currentY
            );

            currentY -=
                    fontSize + bottomSpacing;
        }

        private void writeSectionTitle(
                String title
        ) throws IOException {

            ensureSpace(
                    SECTION_SIZE + 28f
            );

            writeSingleLine(
                    title,
                    SECTION_SIZE,
                    PAGE_MARGIN,
                    currentY
            );

            currentY -=
                    SECTION_SIZE + 11f;
        }

        private void writeSubTitle(
                String title
        ) throws IOException {

            ensureSpace(
                    BODY_SIZE + 22f
            );

            writeSingleLine(
                    title,
                    BODY_SIZE + 1.5f,
                    PAGE_MARGIN,
                    currentY
            );

            currentY -=
                    BODY_LINE_HEIGHT;
        }

        private void writeParagraph(
                String paragraph
        ) throws IOException {

            String safeParagraph =
                    safeText(paragraph);

            if (safeParagraph.isBlank()) {
                writeText(
                        "-",
                        BODY_SIZE,
                        BODY_LINE_HEIGHT,
                        0f
                );

                addSpacing(10f);

                return;
            }

            String[] paragraphs =
                    safeParagraph.split(
                            "\\R+"
                    );

            for (String value : paragraphs) {
                if (value.isBlank()) {
                    addSpacing(
                            BODY_LINE_HEIGHT / 2
                    );
                } else {
                    writeWrappedText(
                            value.trim(),
                            BODY_SIZE,
                            BODY_LINE_HEIGHT,
                            0f
                    );
                }
            }

            addSpacing(12f);
        }

        private void writeWrappedText(
                String text,
                float fontSize,
                float lineHeight,
                float indent
        ) throws IOException {

            List<String> lines =
                    wrapText(
                            safeText(text),
                            fontSize,
                            CONTENT_WIDTH - indent
                    );

            for (String line : lines) {
                writeText(
                        line,
                        fontSize,
                        lineHeight,
                        indent
                );
            }
        }

        private void writeText(
                String text,
                float fontSize,
                float lineHeight,
                float indent
        ) throws IOException {

            ensureSpace(lineHeight);

            writeSingleLine(
                    safeText(text),
                    fontSize,
                    PAGE_MARGIN + indent,
                    currentY
            );

            currentY -= lineHeight;
        }

        private void writeHorizontalLine(
                float topSpacing,
                float bottomSpacing
        ) throws IOException {

            addSpacing(topSpacing);

            ensureSpace(
                    bottomSpacing + 2f
            );

            contentStream.moveTo(
                    PAGE_MARGIN,
                    currentY
            );

            contentStream.lineTo(
                    page.getMediaBox()
                            .getWidth()
                            - PAGE_MARGIN,
                    currentY
            );

            contentStream.setLineWidth(
                    0.7f
            );

            contentStream.stroke();

            currentY -= bottomSpacing;
        }

        private void addSpacing(
                float spacing
        ) throws IOException {

            if (spacing <= 0) {
                return;
            }

            ensureSpace(spacing);

            currentY -= spacing;
        }

        private void ensureSpace(
                float requiredHeight
        ) throws IOException {

            if (currentY - requiredHeight
                    < PAGE_MARGIN) {

                createPage();
            }
        }

        private void writeSingleLine(
                String text,
                float fontSize,
                float x,
                float y
        ) throws IOException {

            contentStream.beginText();

            contentStream.setFont(
                    font,
                    fontSize
            );

            contentStream.newLineAtOffset(
                    x,
                    y
            );

            contentStream.showText(
                    sanitizePdfText(text)
            );

            contentStream.endText();
        }

        private List<String> wrapText(
                String text,
                float fontSize,
                float maxWidth
        ) throws IOException {

            List<String> lines =
                    new ArrayList<>();

            if (text == null
                    || text.isBlank()) {

                lines.add("");

                return lines;
            }

            StringBuilder currentLine =
                    new StringBuilder();

            for (int index = 0;
                 index < text.length();) {

                int codePoint =
                        text.codePointAt(index);

                String character =
                        new String(
                                Character.toChars(
                                        codePoint
                                )
                        );

                String candidate =
                        currentLine + character;

                if (!currentLine.isEmpty()
                        && getTextWidth(
                                candidate,
                                fontSize
                        ) > maxWidth) {

                    lines.add(
                            currentLine
                                    .toString()
                                    .stripTrailing()
                    );

                    currentLine =
                            new StringBuilder();

                    if (!character.isBlank()) {
                        currentLine.append(
                                character
                        );
                    }

                } else {
                    currentLine.append(
                            character
                    );
                }

                index +=
                        Character.charCount(
                                codePoint
                        );
            }

            if (!currentLine.isEmpty()) {
                lines.add(
                        currentLine
                                .toString()
                                .stripTrailing()
                );
            }

            return lines;
        }

        private float getTextWidth(
                String text,
                float fontSize
        ) throws IOException {

            return font.getStringWidth(
                    sanitizePdfText(text)
            ) / 1000f * fontSize;
        }

        private String sanitizePdfText(
                String text
        ) {

            if (text == null) {
                return "";
            }

            return text
                    .replace("\t", "    ")
                    .replace("\r", "")
                    .replace("\n", " ");
        }

        private void close()
                throws IOException {

            closeCurrentStream();
        }

        private void closeCurrentStream()
                throws IOException {

            if (contentStream != null) {
                contentStream.close();
                contentStream = null;
            }
        }
    }
}