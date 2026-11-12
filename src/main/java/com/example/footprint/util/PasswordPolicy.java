package com.example.footprint.util;

import com.example.footprint.exception.ApiException;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {
    private final boolean breachCheck;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    public PasswordPolicy(@Value("${auth.breach-check-enabled:true}") boolean enabled) { breachCheck = enabled; }
    public void validate(String password) {
        if (password == null || password.codePointCount(0, password.length()) < 8 || password.codePointCount(0, password.length()) > 64)
            throw ApiException.bad("PASSWORD_LENGTH", "비밀번호는 15~64 Unicode 코드 포인트여야 합니다.");
        if (Set.of("passwordpassword", "123456789012345", "1234567890123456", "qwertyuiopasdfgh").contains(password.toLowerCase(Locale.ROOT))
                || password.codePoints().distinct().count() == 1)
            throw ApiException.bad("WEAK_PASSWORD", "쉽게 추측할 수 있는 비밀번호입니다.");
        if (breachCheck) checkBreached(password);
    }
    private void checkBreached(String password) {
        try {
            String hash = HexFormat.of().withUpperCase().formatHex(MessageDigest.getInstance("SHA-1").digest(password.getBytes(StandardCharsets.UTF_8)));
            var request = HttpRequest.newBuilder(URI.create("https://api.pwnedpasswords.com/range/" + hash.substring(0, 5)))
                    .timeout(Duration.ofSeconds(5)).header("Add-Padding", "true").GET().build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new java.io.IOException("Password check unavailable");
            for (String line : response.body().split("\\R")) {
                String[] entry = line.trim().split(":");
                if (entry.length == 2 && entry[0].equalsIgnoreCase(hash.substring(5)) && Long.parseLong(entry[1]) > 0)
                    throw ApiException.bad("BREACHED_PASSWORD", "유출된 것으로 알려진 비밀번호입니다.");
            }
        } catch (ApiException e) { throw e; }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw unavailable(); }
        catch (Exception e) { throw unavailable(); }
    }
    private ApiException unavailable() { return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "PASSWORD_CHECK_UNAVAILABLE", "비밀번호 안전성 확인을 잠시 후 다시 시도해 주세요."); }
}
