package com.genesys.knowledge.classification.exception;

/**
 * Created by rhorilyi on 25.07.2017.
 */
public class ClassifierNotTrainedException extends Exception {

    public ClassifierNotTrainedException() {
        super("The specified classifier hasn't been trained yet.");
    }
}
