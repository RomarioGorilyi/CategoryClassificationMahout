package com.genesys.knowledge.classification.defaults;

/**
 * Created by rhorilyi on 25.04.2017.
 */
public class LogisticRegressionDefaults {

    public static final double DEFAULT_LR_LEARNING_RATE = 50.0;
    public static final double DEFAULT_LR_ALPHA = 1.0; // must be less or equals to 1.0
    public static final double DEFAULT_LR_LAMBDA = 0.000001;
    public static final int DEFAULT_LR_STEP_OFFSET = 10000;
    public static final double DEFAULT_LR_DECAY_EXPONENT = 1.0;
}
