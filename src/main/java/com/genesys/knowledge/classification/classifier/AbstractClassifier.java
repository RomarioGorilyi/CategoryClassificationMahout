package com.genesys.knowledge.classification;

import com.genesys.knowledge.domain.Document;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by rhorilyi on 25.04.2017.
 */
@Slf4j
public abstract class AbstractClassifier {

    public abstract AbstractClassifier train(Document queryTerms);

    public abstract byte[] serializeModel();

    public abstract void deserializeModel(byte[] modelData);
}
