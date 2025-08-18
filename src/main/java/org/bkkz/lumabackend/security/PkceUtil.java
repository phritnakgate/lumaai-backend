package org.bkkz.lumabackend.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class PkceUtil {
    public static boolean verifyCodeChallenge(String codeVerifier, String codeChallenge, String codeChallengeMethod) throws NoSuchAlgorithmException {
        if ("S256".equalsIgnoreCase(codeChallengeMethod)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            String encodedChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(hashedBytes);
            return encodedChallenge.equals(codeChallenge);
        }
        return false;
    }
}
