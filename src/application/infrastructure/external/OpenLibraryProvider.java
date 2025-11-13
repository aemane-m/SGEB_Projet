package application.infrastructure.external;

import application.domain.ports.external.BookInfoProvider;
import application.domain.ports.external.BookMeta;
import com.google.gson.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Adapter concret pour OpenLibrary.
 * CORRIGÉ : Parse 'subjects' (genre) et 'number_of_pages'.
 */
public class OpenLibraryProvider implements BookInfoProvider {

    private static final String API_URL = "https://openlibrary.org/api/books?bibkeys=ISBN:%s&format=json&jscmd=data";
    private static final String COVER_URL_FALLBACK = "https://covers.openlibrary.org/b/isbn/%s-L.jpg";

    private final HttpClient http = HttpClient.newHttpClient();

    @Override
    public Optional<BookMeta> findBookByIsbn(String isbn) {
        if (isbn == null || isbn.isBlank()) return Optional.empty();
        String cleanIsbn = isbn.replaceAll("[^0-9Xx]", "");
        String isbnKey = "ISBN:" + cleanIsbn;

        try {
            HttpRequest req = HttpRequest.newBuilder(
                            URI.create(String.format(API_URL, cleanIsbn)))
                    .GET()
                    .timeout(Duration.ofSeconds(7))
                    .header("Accept", "application/json")
                    .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            
            if (res.statusCode() != 200 || res.body() == null || res.body().isBlank() || res.body().equals("{}")) {
                return Optional.empty();
            }

            JsonObject root = JsonParser.parseString(res.body()).getAsJsonObject();
            
            if (!root.has(isbnKey)) {
                return Optional.empty();
            }

            JsonObject json = root.getAsJsonObject(isbnKey);

            String titre = getAsString(json, "title");
            String auteur = firstObjectPropertyFromArray(json, "authors", "name");
            String editeur = firstObjectPropertyFromArray(json, "publishers", "name");
            Integer annee = extractYear(getAsString(json, "publish_date"));
            String coverUrl = getCoverUrl(json, "cover", cleanIsbn);

            // --- CORRECTIONS ICI ---
            // 1. Récupérer le premier sujet comme genre
            String genre = firstObjectPropertyFromArray(json, "subjects", "name");
            // 2. Récupérer le nombre de pages
            Integer nbPages = getAsInt(json, "number_of_pages");
            // --- FIN CORRECTIONS ---

            BookMeta meta = new BookMeta(
                    cleanIsbn,
                    safe(titre),
                    safe(auteur),
                    safe(editeur),
                    annee,
                    safe(genre),     // <-- Champ 'genre' maintenant peuplé
                    nbPages,         // <-- Champ 'nbPages' maintenant peuplé
                    safe(coverUrl)
            );
            return Optional.of(meta);

        } catch (Exception e) {
            System.err.println("[OpenLibraryProvider] " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    // ----------------- Helpers Gson -----------------

    private static String safe(String s) { return s == null ? "" : s; }

    private static String getAsString(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return "";
        JsonElement el = obj.get(key);
        if (el.isJsonPrimitive()) return el.getAsString();
        return "";
    }

    // <-- NOUVEAU HELPER -->
    private static Integer getAsInt(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return null;
        JsonElement el = obj.get(key);
        if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber()) {
            return el.getAsInt();
        }
        return null;
    }

    private static String firstObjectPropertyFromArray(JsonObject obj, String key, String property) {
        if (!obj.has(key) || !obj.get(key).isJsonArray()) return "";
        
        JsonArray arr = obj.get(key).getAsJsonArray();
        if (arr.isEmpty()) return "";

        JsonElement first = arr.get(0);
        if (first.isJsonObject() && first.getAsJsonObject().has(property)) {
            JsonObject o = first.getAsJsonObject();
            if (o.get(property).isJsonPrimitive()) {
                return o.get(property).getAsString();
            }
        }
        return "";
    }

    private static String getCoverUrl(JsonObject obj, String key, String isbn) {
        if (obj.has(key) && obj.get(key).isJsonObject()) {
            JsonObject coverObj = obj.get(key).getAsJsonObject();
            // CORRIGÉ : L'URL de l'image est parfois /b/id/.... et non /b/isbn/...
            // L'API nous donne le bon lien, on l'utilise.
            if (coverObj.has("large") && coverObj.get("large").isJsonPrimitive()) {
                return coverObj.get("large").getAsString();
            }
            if (coverObj.has("medium") && coverObj.get("medium").isJsonPrimitive()) {
                return coverObj.get("medium").getAsString();
            }
        }
        return String.format(COVER_URL_FALLBACK, isbn);
    }

    private static Integer extractYear(String publishDate) {
        if (publishDate == null || publishDate.isBlank()) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{4})").matcher(publishDate);
        if (m.find()) {
            try { return Integer.parseInt(m.group(1)); }
            catch (NumberFormatException ignored) {}
        }
        return null;
    }
}

