package application.infrastructure.external;

import application.domain.ports.external.BookInfoProvider;
import application.domain.ports.external.BookMeta;
import com.google.gson.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

/**
 * Adaptateur concret pour l’API publique OpenLibrary.
 *
 * Il permet de :
 * <ul>
 *     <li>chercher un livre à partir de son ISBN,</li>
 *     <li>récupérer titre, auteur, éditeur, année, genre, nombre de pages,</li>
 *     <li>récupérer une URL de couverture.</li>
 * </ul>
 */
public class OpenLibraryProvider implements BookInfoProvider {

    private static final String API_URL =
            "https://openlibrary.org/api/books?bibkeys=ISBN:%s&format=json&jscmd=data";

    private static final String COVER_URL_FALLBACK =
            "https://covers.openlibrary.org/b/isbn/%s-L.jpg";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public Optional<BookMeta> findBookByIsbn(String isbn) {
        if (isbn == null || isbn.isBlank()) {
            return Optional.empty();
        }

        // Nettoyage de l’ISBN (on garde chiffres + X/x)
        String cleanIsbn = isbn.replaceAll("[^0-9Xx]", "");
        String isbnKey = "ISBN:" + cleanIsbn;

        try {
            // 1. Préparation de la requête
            HttpRequest request = HttpRequest.newBuilder(
                            URI.create(String.format(API_URL, cleanIsbn)))
                    .GET()
                    .timeout(Duration.ofSeconds(7))
                    .header("Accept", "application/json")
                    .build();

            // 2. Appel HTTP
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );

            // 3. Vérifications de base
            if (response.statusCode() != 200) {
                return Optional.empty();
            }
            String body = response.body();
            if (body == null || body.isBlank() || "{}".equals(body.trim())) {
                return Optional.empty();
            }

            // 4. Parsing JSON
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            if (!root.has(isbnKey)) {
                return Optional.empty();
            }

            JsonObject json = root.getAsJsonObject(isbnKey);

            // 5. Extraction des champs principaux
            String titre = getAsString(json, "title");
            String auteur = firstObjectPropertyFromArray(json, "authors", "name");
            String editeur = firstObjectPropertyFromArray(json, "publishers", "name");
            Integer anneePublication = extractYear(getAsString(json, "publish_date"));
            String coverUrl = getCoverUrl(json, "cover", cleanIsbn);

            // 6. Champs ajoutés : genre et nombre de pages
            String genre = firstObjectPropertyFromArray(json, "subjects", "name");
            Integer nbPages = getAsInt(json, "number_of_pages");

            // 7. Construction du DTO BookMeta
            BookMeta meta = new BookMeta(
                    cleanIsbn,
                    safe(titre),
                    safe(auteur),
                    safe(editeur),
                    anneePublication,
                    safe(genre),
                    nbPages,
                    safe(coverUrl)
            );

            return Optional.of(meta);

        } catch (Exception e) {
            System.err.println("[OpenLibraryProvider] " + e.getClass().getSimpleName()
                    + " : " + e.getMessage());
            return Optional.empty();
        }
    }

    // =====================================================================
    // == Helpers JSON / GSON ==
    // =====================================================================

    /** Retourne une chaîne non nulle ("" si null). */
    private static String safe(String s) {
        return (s == null) ? "" : s;
    }

    /** Récupère une propriété simple (String) d’un JsonObject. */
    private static String getAsString(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) {
            return "";
        }
        JsonElement el = obj.get(key);
        if (el.isJsonPrimitive()) {
            return el.getAsString();
        }
        return "";
    }

    /** Récupère un entier si la propriété est bien numérique, sinon null. */
    private static Integer getAsInt(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) {
            return null;
        }
        JsonElement el = obj.get(key);
        if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber()) {
            return el.getAsInt();
        }
        return null;
    }

    /**
     * Récupère la première valeur d’un tableau d’objets JSON,
     * en lisant une propriété donnée (ex: "name").
     */
    private static String firstObjectPropertyFromArray(JsonObject obj, String key, String property) {
        if (!obj.has(key) || !obj.get(key).isJsonArray()) {
            return "";
        }

        JsonArray arr = obj.get(key).getAsJsonArray();
        if (arr.isEmpty()) {
            return "";
        }

        JsonElement first = arr.get(0);
        if (first.isJsonObject()) {
            JsonObject o = first.getAsJsonObject();
            if (o.has(property) && o.get(property).isJsonPrimitive()) {
                return o.get(property).getAsString();
            }
        }

        return "";
    }

    /**
     * Détermine la meilleure URL de couverture à partir du JSON,
     * sinon utilise une URL de secours basée sur l’ISBN.
     */
    private static String getCoverUrl(JsonObject obj, String key, String isbn) {
        if (obj.has(key) && obj.get(key).isJsonObject()) {
            JsonObject coverObj = obj.get(key).getAsJsonObject();

            // OpenLibrary fournit parfois directement l’URL de l’image
            if (coverObj.has("large") && coverObj.get("large").isJsonPrimitive()) {
                return coverObj.get("large").getAsString();
            }
            if (coverObj.has("medium") && coverObj.get("medium").isJsonPrimitive()) {
                return coverObj.get("medium").getAsString();
            }
        }
        // Fallback : URL basée sur l’ISBN
        return String.format(COVER_URL_FALLBACK, isbn);
    }

    /**
     * Extrait une année à partir d'une chaîne de type "May 5, 2001"
     * en cherchant un groupe de 4 chiffres.
     */
    private static Integer extractYear(String publishDate) {
        if (publishDate == null || publishDate.isBlank()) {
            return null;
        }
        var pattern = java.util.regex.Pattern.compile("(\\d{4})");
        var matcher = pattern.matcher(publishDate);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
                // On ignore et renvoie null
            }
        }
        return null;
    }
}