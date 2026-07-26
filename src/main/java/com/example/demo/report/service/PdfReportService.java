package com.example.demo.report.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
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
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
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

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern(
                    "yyyy.MM.dd"
            );

    private final String configuredFontPath;
    private final ResourceLoader resourceLoader;

    public PdfReportService(
            @Value(
                    "${report.pdf.font-path:"
                            + "classpath:fonts/NotoSansKR-Regular.ttf}"
            )
            String configuredFontPath,
            ResourceLoader resourceLoader
    ) {

        this.configuredFontPath =
                configuredFontPath;

        this.resourceLoader =
                resourceLoader;
    }

    public byte[] createPdf(
            AiReportResponse report
    ) {

        validateReport(report);

        try (
                PDDocument document =
                        new PDDocument();

                ByteArrayOutputStream outputStream =
                        new ByteArrayOutputStream()
        ) {

            PDType0Font font =
                    loadFont(document);

            PdfWriter writer =
                    new PdfWriter(
                            document,
                            font
                    );

            writeReport(
                    writer,
                    report
            );

            writer.close();

            document.save(outputStream);

            return outputStream.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException(
                    "PDF 보고서를 생성하지 못했습니다.",
                    e
            );
        }
    }

    private void writeReport(
            PdfWriter writer,
            AiReportResponse report
    ) throws IOException {

        writer.writeCenteredText(
                "AI 최종 업무 보고서",
                TITLE_SIZE,
                0f,
                28f
        );

        writer.writeCenteredText(
                "전체 업무 로그 기반 종합 분석",
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
                report.getStatistics()
        );

        writer.writeSectionTitle(
                "1. 지금까지 한 일들 요약"
        );

        writer.writeParagraph(
                report.getWorkSummary()
        );

        writer.writeSectionTitle(
                "2. 구현 기능들"
        );

        writeImplementedFeatures(
                writer,
                report.getImplementedFeatures()
        );

        writer.writeSectionTitle(
                "3. 난이도 해설"
        );

        writer.writeParagraph(
                report.getDifficultyAnalysis()
        );

        writer.writeSectionTitle(
                "4. 프로젝트 성과"
        );

        writer.writeParagraph(
                report.getProjectAchievements()
        );

        writer.writeSectionTitle(
                "5. 향후 개선 사항"
        );

        writeFutureImprovements(
                writer,
                report.getFutureImprovements()
        );

        writer.writeHorizontalLine(
                10f,
                18f
        );

        writer.writeText(
                "보고서 생성일: "
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
            ReportStatistics statistics
    ) throws IOException {

        writer.writeSectionTitle(
                "보고서 개요"
        );

        writer.writeText(
                "총 업무 로그: "
                        + statistics.getTotalWorkLogs()
                        + "건",
                BODY_SIZE,
                BODY_LINE_HEIGHT,
                0f
        );

        writer.writeText(
                "업무 기간: "
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
                "평균 난이도: "
                        + statistics.getAverageDifficulty()
                        + " / 3.0",
                BODY_SIZE,
                BODY_LINE_HEIGHT,
                0f
        );

        writer.writeText(
                "난이도 분포: "
                        + formatMap(
                                statistics.getDifficultyCounts()
                        ),
                BODY_SIZE,
                BODY_LINE_HEIGHT,
                0f
        );

        writer.writeWrappedText(
                "주요 기술 태그: "
                        + formatMap(
                                statistics.getTagCounts()
                        ),
                BODY_SIZE,
                BODY_LINE_HEIGHT,
                0f
        );

        writer.addSpacing(12f);
    }

    private void writeImplementedFeatures(
            PdfWriter writer,
            List<ImplementedFeature> implementedFeatures
    ) throws IOException {

        if (implementedFeatures == null
                || implementedFeatures.isEmpty()) {

            writer.writeText(
                    "정리된 구현 기능이 없습니다.",
                    BODY_SIZE,
                    BODY_LINE_HEIGHT,
                    0f
            );

            return;
        }

        for (
                int index = 0;
                index < implementedFeatures.size();
                index++
        ) {

            ImplementedFeature feature =
                    implementedFeatures.get(index);

            if (feature == null) {
                continue;
            }

            writer.writeSubTitle(
                    "2."
                            + (index + 1)
                            + " "
                            + safeText(
                                    feature.getCategory()
                            )
            );

            if (feature.getFeatures() != null) {

                for (
                        String value :
                        feature.getFeatures()
                ) {

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
            List<String> futureImprovements
    ) throws IOException {

        if (futureImprovements == null
                || futureImprovements.isEmpty()) {

            writer.writeText(
                    "정리된 향후 개선 사항이 없습니다.",
                    BODY_SIZE,
                    BODY_LINE_HEIGHT,
                    0f
            );

            return;
        }

        for (
                int index = 0;
                index < futureImprovements.size();
                index++
        ) {

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
            PDDocument document
    ) throws IOException {

        List<String> candidates =
                createFontCandidates();

        List<String> checkedLocations =
                new ArrayList<>();

        for (String candidate : candidates) {

            if (candidate == null
                    || candidate.isBlank()) {

                continue;
            }

            String normalizedLocation =
                    normalizeResourceLocation(
                            candidate
                    );

            checkedLocations.add(
                    normalizedLocation
            );

            try {
                Resource resource =
                        resourceLoader.getResource(
                                normalizedLocation
                        );

                if (!resource.exists()
                        || !resource.isReadable()) {

                    continue;
                }

                try (
                        InputStream inputStream =
                                resource.getInputStream()
                ) {

                    return PDType0Font.load(
                            document,
                            inputStream
                    );
                }

            } catch (Exception ignored) {
                // 해당 폰트를 읽지 못하면
                // 다음 후보 폰트를 확인합니다.
            }
        }

        throw new IllegalStateException(
                "PDF 한글 폰트를 찾을 수 없습니다. "
                        + "확인한 위치: "
                        + String.join(
                                ", ",
                                checkedLocations
                        )
                        + ". "
                        + "src/main/resources/fonts/"
                        + "NotoSansKR-Regular.ttf 파일을 "
                        + "추가해 주세요."
        );
    }

    private List<String> createFontCandidates() {

        List<String> candidates =
                new ArrayList<>();

        if (configuredFontPath != null
                && !configuredFontPath.isBlank()) {

            candidates.add(
                    configuredFontPath.trim()
            );
        }

        /*
         * 배포 환경에서 가장 먼저 사용할 폰트입니다.
         *
         * 실제 파일 위치:
         * src/main/resources/fonts/
         * NotoSansKR-Regular.ttf
         */
        candidates.add(
                "classpath:fonts/"
                        + "NotoSansKR-Regular.ttf"
        );

        /*
         * 기존 로컬 Windows 환경에서도
         * 폰트를 사용할 수 있도록 유지합니다.
         */
        candidates.add(
                "C:/Windows/Fonts/malgun.ttf"
        );

        candidates.add(
                "C:/Windows/Fonts/malgunbd.ttf"
        );

        /*
         * 일부 Linux 배포 환경에서 사용할 수 있는
         * 시스템 폰트 후보입니다.
         */
        candidates.add(
                "/usr/share/fonts/truetype/noto/"
                        + "NotoSansCJK-Regular.ttc"
        );

        candidates.add(
                "/usr/share/fonts/opentype/noto/"
                        + "NotoSansCJK-Regular.ttc"
        );

        candidates.add(
                "/usr/share/fonts/truetype/nanum/"
                        + "NanumGothic.ttf"
        );

        /*
         * macOS 로컬 실행 후보입니다.
         */
        candidates.add(
                "/System/Library/Fonts/"
                        + "AppleSDGothicNeo.ttc"
        );

        return candidates;
    }

    private String normalizeResourceLocation(
            String location
    ) {

        if (location == null
                || location.isBlank()) {

            return "";
        }

        String trimmed =
                location.trim();

        if (trimmed.startsWith("classpath:")
                || trimmed.startsWith("file:")) {

            return trimmed;
        }

        try {
            Path path =
                    Path.of(trimmed);

            if (path.isAbsolute()) {
                return path.toUri()
                        .toString();
            }

        } catch (Exception ignored) {
            // Path로 변환할 수 없는 경우
            // 일반 파일 경로로 처리합니다.
        }

        return "file:" + trimmed;
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
            Map<String, Long> values
    ) {

        if (values == null
                || values.isEmpty()) {

            return "-";
        }

        return values.entrySet()
                .stream()
                .map(entry ->
                        entry.getKey()
                                + " "
                                + entry.getValue()
                                + "건"
                )
                .reduce(
                        (first, second) ->
                                first
                                        + ", "
                                        + second
                )
                .orElse("-");
    }

    private static String safeText(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value.trim();
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

            page =
                    new PDPage(
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
                    fontSize
                            + bottomSpacing
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
                    fontSize
                            + bottomSpacing;
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

            if (
                    currentY - requiredHeight
                            < PAGE_MARGIN
            ) {

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

            for (
                    int index = 0;
                    index < text.length();
            ) {

                int codePoint =
                        text.codePointAt(index);

                String character =
                        new String(
                                Character.toChars(
                                        codePoint
                                )
                        );

                String candidate =
                        currentLine
                                + character;

                if (
                        !currentLine.isEmpty()
                                && getTextWidth(
                                        candidate,
                                        fontSize
                                ) > maxWidth
                ) {

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
                    .replace(
                            "\t",
                            "    "
                    )
                    .replace(
                            "\r",
                            ""
                    )
                    .replace(
                            "\n",
                            " "
                    );
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