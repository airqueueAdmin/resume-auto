package com.aicareer.platform.retrieval.application;

import java.util.List;

public interface EmbeddingClient {

    List<double[]> embed(List<String> texts);

    String model();

    int dimensions();
}
