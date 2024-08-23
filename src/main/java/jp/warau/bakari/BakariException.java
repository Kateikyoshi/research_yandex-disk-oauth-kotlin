package jp.warau.bakari;

import java.io.IOException;

public class BakariException extends IOException {

    public BakariException(String errorMessage) {
        super(errorMessage);
    }
}
