package com.aicareer.platform.job.application;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.aicareer.platform.job.api.JobPostingImportPreview;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JobPostingImportService {

    private static final int MAX_REDIRECTS = 3;
    private static final int MAX_DOCUMENT_BYTES = 2 * 1024 * 1024;
    private static final Pattern JSON_LD_SCRIPT = Pattern.compile(
            "<script\\b[^>]*type\\s*=\\s*[\\\"']application/ld\\+json[\\\"'][^>]*>(.*?)</script>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern META_TAG = Pattern.compile("<meta\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ATTRIBUTE = Pattern.compile(
            "([a-zA-Z_:][-a-zA-Z0-9_:.]*)\\s*=\\s*[\\\"'](.*?)[\\\"']",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern CHARSET = Pattern.compile("charset\\s*=\\s*[\\\"']?([a-zA-Z0-9._-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern DETAIL_FRAME_PATH = Pattern.compile(
            "(/Recruit/GI_Read_Comt_Ifrm\\?[^\\\"'<>\\s\\\\]+)",
            Pattern.CASE_INSENSITIVE
    );

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final JobPostingImageTextExtractor imageTextExtractor;

    public JobPostingImportService(ObjectMapper objectMapper) {
        this(objectMapper, imageUrls -> "");
    }

    @Autowired
    public JobPostingImportService(
            ObjectMapper objectMapper,
            JobPostingImageTextExtractor imageTextExtractor
    ) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        this.objectMapper = objectMapper;
        this.imageTextExtractor = imageTextExtractor;
    }

    public JobPostingImportPreview preview(String sourceUrl) {
        URI requestedUri = parseAndValidateUri(sourceUrl);
        FetchedDocument document = fetch(requestedUri);
        String html = document.body();
        String detailFramePath = detailFramePath(html);
        if (!detailFramePath.isBlank()) {
            URI detailUri = document.uri().resolve(detailFramePath);
            FetchedDocument detailDocument = fetch(detailUri, document.uri());
            html = html + "\n" + detailDocument.body();
        }
        return extract(document.uri(), document.contentType(), html);
    }

    private FetchedDocument fetch(URI requestedUri) {
        return fetch(requestedUri, null);
    }

    private FetchedDocument fetch(URI requestedUri, URI referer) {
        URI current = requestedUri;
        for (int redirect = 0; redirect <= MAX_REDIRECTS; redirect++) {
            try {
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(current)
                        .timeout(Duration.ofSeconds(15))
                        .header("User-Agent", "AI-Career-Platform-Job-Importer/1.0")
                        .header("Accept", "text/html, application/xhtml+xml;q=0.9");
                if (referer != null) requestBuilder.header("Referer", referer.toString());
                HttpRequest request = requestBuilder.GET().build();
                HttpResponse<InputStream> response = httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofInputStream()
                );
                try (InputStream body = response.body()) {
                    int status = response.statusCode();
                    if (status >= 300 && status < 400) {
                        String location = response.headers().firstValue("location").orElse(null);
                        if (location == null || redirect == MAX_REDIRECTS) {
                            throw new JobPostingImportException("채용공고 URL의 리다이렉트를 처리하지 못했습니다.");
                        }
                        current = validateUri(current.resolve(location));
                        continue;
                    }
                    if (status < 200 || status >= 300) {
                        throw new JobPostingImportException("채용공고 페이지를 가져오지 못했습니다. HTTP " + status);
                    }
                    byte[] bytes = readLimited(body);
                    String contentType = response.headers().firstValue("content-type").orElse("");
                    if (!contentType.isBlank()
                            && !contentType.toLowerCase(Locale.ROOT).contains("text/html")
                            && !contentType.toLowerCase(Locale.ROOT).contains("application/xhtml+xml")) {
                        throw new JobPostingImportException("HTML 형식의 채용공고 페이지만 가져올 수 있습니다.");
                    }
                    return new FetchedDocument(current, contentType, decode(bytes, contentType));
                }
            } catch (JobPostingImportException exception) {
                throw exception;
            } catch (IOException exception) {
                throw new JobPostingImportException("채용공고 페이지에 연결하지 못했습니다.", exception);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new JobPostingImportException("채용공고 페이지 조회가 중단되었습니다.", exception);
            }
        }
        throw new JobPostingImportException("채용공고 URL 리다이렉트 횟수가 제한을 초과했습니다.");
    }

    String detailFramePath(String html) {
        String normalized = html
                .replace("\\u0026", "&")
                .replace("\\u002F", "/")
                .replace("\\u002f", "/")
                .replace("\\/", "/");
        Matcher matcher = DETAIL_FRAME_PATH.matcher(normalized);
        return matcher.find() ? decodeHtml(matcher.group(1)) : "";
    }

    private byte[] readLimited(InputStream inputStream) throws IOException {
        byte[] bytes = inputStream.readNBytes(MAX_DOCUMENT_BYTES + 1);
        if (bytes.length > MAX_DOCUMENT_BYTES) {
            throw new JobPostingImportException("채용공고 페이지는 2MB 이하만 가져올 수 있습니다.");
        }
        return bytes;
    }

    JobPostingImportPreview extract(URI uri, String contentType, String html) {
        JsonNode jobPosting = findJobPostingJsonLd(html);
        List<String> imageUrls = detailImageUrls(html, uri);
        String detailDescription = detailSectionText(html);
        String imageText = detailDescription.isBlank() ? extractImageText(imageUrls) : "";
        String title = firstNonBlank(
                text(jobPosting, "title"),
                text(jobPosting, "name"),
                meta(html, "og:title"),
                meta(html, "twitter:title"),
                tagText(html, "title")
        );
        String companyName = firstNonBlank(
                nestedText(jobPosting, "hiringOrganization", "name"),
                nestedText(jobPosting, "organization", "name"),
                meta(html, "job:company"),
                meta(html, "og:site_name")
        );
        String description = firstNonBlank(
                detailDescription,
                imageText,
                imageOnlyDescription(imageUrls),
                text(jobPosting, "description"),
                meta(html, "description"),
                meta(html, "og:description"),
                bodyText(html)
        );
        title = clean(title);
        companyName = clean(companyName);
        description = clean(description);

        if (title.isBlank() && companyName.isBlank() && description.isBlank()) {
            throw new JobPostingImportException("채용공고에서 가져올 수 있는 내용을 찾지 못했습니다.");
        }

        List<String> warnings = new ArrayList<>();
        if (title.isBlank()) warnings.add("공고 제목을 찾지 못했습니다.");
        if (companyName.isBlank()) warnings.add("회사명을 찾지 못했습니다.");
        if (description.isBlank()) warnings.add("Job Description을 찾지 못했습니다.");
        if (!imageUrls.isEmpty() && detailDescription.isBlank()) {
            if (imageText.isBlank()) {
                warnings.add("상세 공고가 이미지로 제공되었지만 OCR 텍스트를 가져오지 못했습니다. 미리보기 이미지를 확인해주세요.");
            } else {
                warnings.add("상세 공고 이미지에서 OCR로 추출한 텍스트입니다. 원문 이미지와 함께 확인해주세요.");
            }
        }
        String extractionSource = !detailDescription.isBlank()
                ? "html-detail"
                : !imageText.isBlank() ? "html-detail-image-ocr"
                : !imageUrls.isEmpty() ? "html-detail-image"
                : jobPosting == null ? "metadata/html" : "json-ld";
        return new JobPostingImportPreview(
                uri.toString(),
                title,
                companyName,
                description,
                extractionSource,
                warnings,
                imageUrls
        );
    }

    private String detailSectionText(String html) {
        String section = detailMarkup(html);
        if (section.isBlank()) {
            String decodedHtml = decodeHtml(html);
            if (!decodedHtml.equals(html)) section = detailMarkup(decodedHtml);
        }
        if (section.isBlank()) return "";

        String text = section
                .replace("\\r\\n", "\n")
                .replace("\\n", "\n")
                .replace("\\r", "\n")
                // Remove comments before generic tag stripping so nested tags do not leave a trailing "-->".
                .replaceAll("(?s)<!--.*?-->", " ")
                // The map button is a page control, not part of the job description.
                .replaceAll("(?is)<button\\b[^>]*>.*?</button>", " ")
                .replaceAll("(?is)<(script|style|noscript|svg)\\b[^>]*>.*?</\\1>", " ")
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</(p|div|li|h[1-6]|tr)>", "\n")
                .replaceAll("(?s)<[^>]+>", " ")
                .replaceAll("(?m)^\\s*--?>\\s*$", " ");
        return clean(text);
    }

    private String detailMarkup(String html) {
        String section = elementByClass(html, "ul", "detail-section");
        if (section.isBlank()) section = elementByClass(html, "div", "detail-section");
        if (!section.isBlank()) return section;

        // The newer JobKorea page renders image-only postings inside this container.
        section = elementById(html, "div", "detail-content");
        if (!section.isBlank()) return section;

        // Some responses omit the wrapper and keep only the individual detail articles.
        return String.join("\n", elementsByClass(html, "li", "detail-article"));
    }

    private List<String> detailImageUrls(String html, URI baseUri) {
        String section = detailMarkup(html);
        if (section.isBlank()) return List.of();

        Matcher matcher = Pattern.compile(
                "<img\\b[^>]*\\bsrc\\s*=\\s*[\\\"']([^\\\"']+)[\\\"'][^>]*>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        ).matcher(section);
        List<String> imageUrls = new ArrayList<>();
        while (matcher.find()) {
            String imageUrl = decodeHtml(matcher.group(1))
                    .replace("\\\\u0026", "&")
                    .replace("\\\\u002F", "/")
                    .replace("\\\\u002f", "/")
                    .replace("\\\\/", "/");
            try {
                URI resolved = baseUri.resolve(imageUrl);
                String scheme = resolved.getScheme();
                if (("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                        && !imageUrls.contains(resolved.toString())) {
                    imageUrls.add(resolved.toString());
                }
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed image references and keep importing the rest of the posting.
            }
        }
        return List.copyOf(imageUrls);
    }

    private String imageOnlyDescription(List<String> imageUrls) {
        if (imageUrls.isEmpty()) return "";
        return "상세 공고 내용은 이미지로 제공됩니다. 미리보기에서 원문 이미지를 확인해주세요.";
    }

    private String extractImageText(List<String> imageUrls) {
        if (imageUrls.isEmpty()) return "";
        try {
            return clean(imageTextExtractor.extract(imageUrls));
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private String elementByClass(String html, String tagName, String className) {
        return elementsByClass(html, tagName, className).stream().findFirst().orElse("");
    }

    private String elementById(String html, String tagName, String id) {
        Pattern openingTag = Pattern.compile(
                "<" + tagName + "\\b(?=[^>]*\\bid\\s*=\\s*[\\\"']" + Pattern.quote(id) + "[\\\"'])[^>]*>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        Matcher openingMatcher = openingTag.matcher(html);
        if (!openingMatcher.find()) return "";

        int depth = 1;
        Pattern elementTag = Pattern.compile(
                "<(/?)" + tagName + "\\b[^>]*>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        Matcher elementMatcher = elementTag.matcher(html);
        elementMatcher.region(openingMatcher.end(), html.length());
        while (elementMatcher.find()) {
            if (elementMatcher.group(1).isEmpty()) {
                depth++;
            } else if (--depth == 0) {
                return html.substring(openingMatcher.start(), elementMatcher.end());
            }
        }
        return "";
    }

    private List<String> elementsByClass(String html, String tagName, String className) {
        List<String> elements = new ArrayList<>();
        Pattern openingTag = Pattern.compile(
                "<" + tagName + "\\b[^>]*\\bclass\\s*=\\s*[\\\"']([^\\\"']*)[\\\"'][^>]*>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        Matcher openingMatcher = openingTag.matcher(html);
        Pattern elementTag = Pattern.compile(
                "<(/?)" + tagName + "\\b[^>]*>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );

        while (openingMatcher.find()) {
            String classes = " " + openingMatcher.group(1).trim().replaceAll("\\s+", " ") + " ";
            if (!classes.contains(" " + className + " ")) continue;

            int depth = 1;
            Matcher elementMatcher = elementTag.matcher(html);
            elementMatcher.region(openingMatcher.end(), html.length());
            while (elementMatcher.find()) {
                if (elementMatcher.group(1).isEmpty()) {
                    depth++;
                } else if (--depth == 0) {
                    elements.add(html.substring(openingMatcher.start(), elementMatcher.end()));
                    break;
                }
            }
        }
        return elements;
    }

    private JsonNode findJobPostingJsonLd(String html) {
        Matcher matcher = JSON_LD_SCRIPT.matcher(html);
        while (matcher.find()) {
            try {
                JsonNode parsed = objectMapper.readTree(decodeHtml(matcher.group(1).trim()));
                JsonNode found = findJobPostingNode(parsed);
                if (found != null) return found;
            } catch (Exception ignored) {
                // A page may contain unrelated or malformed JSON-LD blocks. Continue with fallback extraction.
            }
        }
        return null;
    }

    private JsonNode findJobPostingNode(JsonNode node) {
        if (node == null) return null;
        if (node.isArray()) {
            for (JsonNode child : node) {
                JsonNode found = findJobPostingNode(child);
                if (found != null) return found;
            }
            return null;
        }
        if (!node.isObject()) return null;
        JsonNode type = node.get("@type");
        if (isJobPostingType(type)) return node;
        JsonNode graph = node.get("@graph");
        return findJobPostingNode(graph);
    }

    private boolean isJobPostingType(JsonNode type) {
        if (type == null) return false;
        if (type.isArray()) {
            for (JsonNode item : type) if ("JobPosting".equalsIgnoreCase(item.asText())) return true;
            return false;
        }
        return "JobPosting".equalsIgnoreCase(type.asText());
    }

    private String meta(String html, String targetName) {
        Matcher matcher = META_TAG.matcher(html);
        while (matcher.find()) {
            Map<String, String> attributes = attributes(matcher.group());
            String name = firstNonBlank(attributes.get("property"), attributes.get("name"), attributes.get("itemprop"));
            if (targetName.equalsIgnoreCase(name)) return decodeHtml(attributes.get("content"));
        }
        return "";
    }

    private Map<String, String> attributes(String tag) {
        java.util.LinkedHashMap<String, String> attributes = new java.util.LinkedHashMap<>();
        Matcher matcher = ATTRIBUTE.matcher(tag);
        while (matcher.find()) attributes.put(matcher.group(1).toLowerCase(Locale.ROOT), matcher.group(2));
        return attributes;
    }

    private String tagText(String html, String tagName) {
        Matcher matcher = Pattern.compile("<" + tagName + "\\b[^>]*>(.*?)</" + tagName + ">", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(html);
        return matcher.find() ? decodeHtml(matcher.group(1)) : "";
    }

    private String bodyText(String html) {
        String body = html.replaceAll("(?s)<!--.*?-->", " ")
                .replaceAll("(?is)<(script|style|noscript|svg)\\b[^>]*>.*?</\\1>", " ")
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</(p|div|li|h[1-6]|tr)>", "\n")
                .replaceAll("(?s)<[^>]+>", " ")
                .replaceAll("(?m)^\\s*--?>\\s*$", " ");
        return decodeHtml(body).replaceAll("[ \\t\\x0B\\f\\r]+", " ").replaceAll("\\n{3,}", "\n\n").trim();
    }

    private String text(JsonNode node, String field) {
        return node == null ? "" : node.path(field).asText("");
    }

    private String nestedText(JsonNode node, String parent, String field) {
        return node == null ? "" : node.path(parent).path(field).asText("");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return "";
    }

    private String clean(String value) {
        return value == null ? "" : decodeHtml(value)
                .replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                .replaceAll(" *\\n *", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private String decode(byte[] bytes, String contentType) {
        Charset charset = StandardCharsets.UTF_8;
        Matcher matcher = CHARSET.matcher(contentType == null ? "" : contentType);
        if (matcher.find()) {
            try {
                charset = Charset.forName(matcher.group(1));
            } catch (Exception ignored) {
                // Use UTF-8 when a page declares an unsupported charset.
            }
        }
        return new String(bytes, charset);
    }

    private String decodeHtml(String value) {
        if (value == null) return "";
        return value.replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&nbsp;", " ");
    }

    private URI parseAndValidateUri(String sourceUrl) {
        URI uri;
        try {
            uri = URI.create(sourceUrl.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("sourceUrl must be a valid public http or https URL", exception);
        }
        return validateUri(uri);
    }

    private URI validateUri(URI uri) {
        String scheme = uri.getScheme();
        String host = uri.getHost();
        int port = uri.getPort();
        if ((scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")))
                || host == null || host.isBlank()
                || uri.getUserInfo() != null
                || (port != -1 && port != 80 && port != 443)) {
            throw new IllegalArgumentException("sourceUrl must be a public http or https URL");
        }
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        if (normalizedHost.equals("localhost") || normalizedHost.endsWith(".local") || normalizedHost.endsWith(".internal")) {
            throw new IllegalArgumentException("sourceUrl host is not allowed");
        }
        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (address.isAnyLocalAddress()
                        || address.isLoopbackAddress()
                        || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress()
                        || address.isMulticastAddress()) {
                    throw new IllegalArgumentException("sourceUrl host is not allowed");
                }
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("sourceUrl host could not be resolved", exception);
        }
        return uri;
    }

    private record FetchedDocument(URI uri, String contentType, String body) {
    }
}
