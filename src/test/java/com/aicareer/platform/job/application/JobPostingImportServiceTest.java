package com.aicareer.platform.job.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;

import com.aicareer.platform.job.api.JobPostingImportPreview;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class JobPostingImportServiceTest {

    private final JobPostingImportService service = new JobPostingImportService(new ObjectMapper());

    @Test
    void extractsJobPostingJsonLd() {
        JobPostingImportPreview preview = service.extract(
                URI.create("https://jobs.example.com/backend-123"),
                "text/html",
                """
                        <html><head>
                          <script type="application/ld+json">
                            {"@context":"https://schema.org","@type":"JobPosting","title":"Backend Developer","description":"Build APIs\\nOperate services","hiringOrganization":{"@type":"Organization","name":"Acme"}}
                          </script>
                        </head><body>ignored fallback</body></html>
                        """
        );

        assertThat(preview.sourceUrl()).isEqualTo("https://jobs.example.com/backend-123");
        assertThat(preview.title()).isEqualTo("Backend Developer");
        assertThat(preview.companyName()).isEqualTo("Acme");
        assertThat(preview.description()).contains("Build APIs", "Operate services");
        assertThat(preview.extractionSource()).isEqualTo("json-ld");
        assertThat(preview.warnings()).isEmpty();
    }

    @Test
    void fallsBackToMetadataAndBodyWhenJsonLdIsUnavailable() {
        JobPostingImportPreview preview = service.extract(
                URI.create("https://jobs.example.com/fallback"),
                "text/html",
                """
                        <html><head>
                          <title>Platform Engineer</title>
                          <meta property="og:site_name" content="Acme Careers">
                          <meta name="description" content="Build platform tooling and APIs.">
                        </head><body><h1>Platform Engineer</h1><p>Build platform tooling and APIs.</p></body></html>
                        """
        );

        assertThat(preview.title()).isEqualTo("Platform Engineer");
        assertThat(preview.companyName()).isEqualTo("Acme Careers");
        assertThat(preview.description()).isEqualTo("Build platform tooling and APIs.");
        assertThat(preview.extractionSource()).isEqualTo("metadata/html");
    }

    @Test
    void prefersDetailedHtmlSectionOverShortMetadataDescription() {
        JobPostingImportPreview preview = service.extract(
                URI.create("https://jobs.example.com/tmoney-123"),
                "text/html",
                """
                        <html><head>
                          <title>백엔드 개발자</title>
                          <meta property="og:site_name" content="㈜티머니">
                          <meta name="description" content="티머니 백엔드 개발자 채용">
                        </head><body>
                          <ul class="detail-section">
                            <li class="detail-article">
                              <h3 class="title">이런 업무를 해요</h3>
                              <ul class="sentence">
                                <li>ㆍ백엔드 서버 기능 설계, 개발, 운영</li>
                                <li>　- 호출/배차 등 택시 서비스 API 설계 및 구현</li>
                                <li><br></li>
                              </ul>
                            </li>
                            <li class="detail-article">
                              <h3 class="title">이런 기술이 필요해요</h3>
                              <ul class="view-content-detail-skill"><li>JAVA</li><li>Redis</li></ul>
                            </li>
                            <li class="detail-article">
                              <h3 class="title">근무지</h3>
                              <ul class="sentence"><li>서울 중구 후암로 110 <button type="button">지도보기</button></li></ul>
                            </li>
                            <!--\n<AREA_APPLY_CLOSE_DT_TEXT/>\n-->
                          </ul>
                        </body></html>
                        """
        );

        assertThat(preview.description())
                .contains("이런 업무를 해요", "백엔드 서버 기능 설계, 개발, 운영", "호출/배차 등 택시 서비스 API 설계 및 구현", "JAVA", "Redis")
                .contains("이런 업무를 해요\n", "JAVA\nRedis")
                .doesNotContain(
                        "티머니 백엔드 개발자 채용",
                        "지도보기",
                        "<button",
                        "AREA_APPLY_CLOSE_DT_TEXT",
                        "-->",
                        "\\n",
                        "해요n",
                        "운영n",
                        "JAVAn",
                        "\n \n"
                );
        assertThat(preview.extractionSource()).isEqualTo("html-detail");
    }

    @Test
    void extractsDetailArticlesWhenTheDetailSectionWrapperIsMissing() {
        JobPostingImportPreview preview = service.extract(
                URI.create("https://jobs.example.com/detail-articles"),
                "text/html",
                """
                        <html><head>
                          <meta name="description" content="㈜티머니에서 정규직 경력 채용을 진행합니다.">
                        </head><body>
                          <li class="detail-article"><h3>이런 업무를 해요</h3><ul><li>ㆍ백엔드 서버 기능 설계, 개발, 운영</li></ul></li>
                          <li class="detail-article"><h3>이런 기술이 필요해요</h3><ul><li>JAVA</li><li>Spring Boot</li></ul></li>
                        </body></html>
                        """
        );

        assertThat(preview.description())
                .contains("이런 업무를 해요", "백엔드 서버 기능 설계, 개발, 운영", "Spring Boot")
                .doesNotContain("㈜티머니에서 정규직 경력 채용을 진행합니다.");
        assertThat(preview.extractionSource()).isEqualTo("html-detail");
    }

    @Test
    void extractsImageOnlyJobKoreaDetailContent() {
        JobPostingImportPreview preview = new JobPostingImportService(
                new ObjectMapper(),
                imageUrls -> "유통 시스템 운영 및 개발 담당자 모집\nJava\nSpring Boot"
        ).extract(
                URI.create("https://www.jobkorea.co.kr/Recruit/GI_Read/50020670"),
                "text/html",
                """
                        <html><head>
                          <title>유통 시스템 운영/개발 담당자 모집</title>
                          <meta property="og:site_name" content="㈜지에스아이티엠(GSITM)">
                          <meta name="description" content="정규직 경력 채용 공고입니다.">
                        </head><body>
                          <div id="details-section">
                            <div id="detail-content">
                              <div class="attachment-thumbnail">
                                <img src="//file2.jobkorea.co.kr/Net/Mng/DownImage/CorpEditor?file_No=1885265" title="슬라이드1.JPG">
                                <br>
                                <img src="https://file2.jobkorea.co.kr/Net/Mng/DownImage/CorpEditor?file_No=1885266" title="슬라이드2.JPG">
                              </div>
                            </div>
                          </div>
                        </body></html>
                        """
        );

        assertThat(preview.description())
                .isEqualTo("유통 시스템 운영 및 개발 담당자 모집\nJava\nSpring Boot");
        assertThat(preview.imageUrls())
                .containsExactly(
                        "https://file2.jobkorea.co.kr/Net/Mng/DownImage/CorpEditor?file_No=1885265",
                        "https://file2.jobkorea.co.kr/Net/Mng/DownImage/CorpEditor?file_No=1885266"
                );
        assertThat(preview.extractionSource()).isEqualTo("html-detail-image-ocr");
        assertThat(preview.warnings()).anyMatch(warning -> warning.contains("OCR로 추출"));
    }

    @Test
    void findsJobKoreaDetailIframeFromNextData() {
        String framePath = service.detailFramePath(
                """
                        <div id="details-section"></div>
                        <script>self.__next_f.push([1,"src\\u003d\\\"/Recruit/GI_Read_Comt_Ifrm?Gno=48104066\\u0026isHiringCenter=false\\u0026hideMapView=false\\\""])</script>
                        """
        );

        assertThat(framePath)
                .isEqualTo("/Recruit/GI_Read_Comt_Ifrm?Gno=48104066&isHiringCenter=false&hideMapView=false");
    }

    @Test
    void rejectsPrivateImportTargetsBeforeFetching() {
        assertThatThrownBy(() -> service.preview("http://localhost/job"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("sourceUrl host is not allowed");
    }
}
