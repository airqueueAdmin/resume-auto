package com.aicareer.platform.ai.application;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Applies the same grounding contract before accepting either provider's answer. */
public final class SelfIntroductionAnswerValidator {

    private static final Pattern INLINE_CITATION = Pattern.compile("\\s*\\[(C\\d+)]");
    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile("(?<=[.!?。！？])(?=\\s|$)");

    private SelfIntroductionAnswerValidator() {
    }

    public static void validate(LlmSelfIntroductionAnswerCompletion completion, List<RetrievedContext> contexts) {
        if (contexts.isEmpty()) {
            throw GroundingValidationException.emptyContext();
        }
        if (completion == null || completion.answer() == null) {
            throw AiIntegrationException.invalidResponse("The provider returned no self-introduction document", null);
        }
        if (completion.answer().sentences().isEmpty()) {
            throw GroundingValidationException.answerUnsupported();
        }
        Set<String> knownIds = contexts.stream().map(RetrievedContext::citationId).collect(Collectors.toSet());
        for (SelfIntroductionAnswerDocument.Sentence sentence : completion.answer().sentences()) {
            if (sentence == null) {
                throw GroundingValidationException.invalidFinding();
            }
            sentenceTexts(sentence.text());
            if (sentence.citationIds().isEmpty()) {
                throw GroundingValidationException.uncitedFinding();
            }
            for (String citationId : sentence.citationIds()) {
                if (!knownIds.contains(citationId)) {
                    throw GroundingValidationException.unknownCitation(citationId);
                }
            }
            var inline = INLINE_CITATION.matcher(sentence.text());
            while (inline.find()) {
                if (!sentence.citationIds().contains(inline.group(1))) {
                    throw GroundingValidationException.uncitedFinding();
                }
            }
        }
    }

    public static List<String> sentenceTexts(String text) {
        if (text == null || text.isBlank()) {
            throw GroundingValidationException.invalidFinding();
        }
        String normalized = INLINE_CITATION.matcher(text.strip()).replaceAll("")
                .replaceAll("\\s+([.!?。！？])", "$1").strip();
        List<String> sentences = SENTENCE_BOUNDARY.splitAsStream(normalized)
                .map(String::strip)
                .filter(sentence -> !sentence.isEmpty())
                .toList();
        if (sentences.isEmpty() || sentences.stream()
                .anyMatch(sentence -> sentence.codePoints().noneMatch(Character::isLetterOrDigit))) {
            throw GroundingValidationException.invalidFinding();
        }
        return sentences;
    }
}
