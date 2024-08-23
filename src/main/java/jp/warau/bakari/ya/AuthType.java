package jp.warau.bakari.ya;

import lombok.Getter;

@Getter
public enum AuthType {

    OAUTH("OAuth");

    private final String type;

    AuthType(String type) {
        this.type = type;
    }
}