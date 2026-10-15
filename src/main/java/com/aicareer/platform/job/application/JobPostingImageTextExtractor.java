package com.aicareer.platform.job.application;

import java.util.List;

@FunctionalInterface
public interface JobPostingImageTextExtractor {

    String extract(List<String> imageUrls);
}
