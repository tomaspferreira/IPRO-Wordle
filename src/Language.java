import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class Language {
    private String languageCode;

    // Cache: index = letters, stores loaded list (null = not loaded yet)
    private String[][] cache = new String[21][];

    Language(String languageCodeInput) {
        this.languageCode = languageCodeInput.toLowerCase();
    }

    String[] getWordList(int letters) {
        if (letters < 1 || letters >= cache.length) {
            return new String[0];
        }

        // Return cached version if already loaded
        if (cache[letters] != null) {
            return cache[letters];
        }

        String fileName = "words/" + languageCode + "_" + letters + ".txt";

        String[] loaded = loadWordsFromResource(fileName, letters);

        // Cache it (even if empty) so we don't try again every time
        cache[letters] = loaded;

        return loaded;
    }

    private String[] loadWordsFromResource(String fileName, int letters) {
        InputStream in = Language.class.getClassLoader().getResourceAsStream(fileName);
        if (in == null) {
            IO.println("Could not find resource file: " + fileName);
            return new String[0];
        }

        // Simple dynamic array (no ArrayList needed)
        String[] temp = new String[1024];
        int count = 0;

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;

            while ((line = br.readLine()) != null) {
                String w = line.trim();
                if (w.isEmpty()) {
                    continue;
                }

                // Keep only correct length (safety)
                if (w.length() != letters) {
                    continue;
                }

                // Store uppercase, because your games convert to uppercase
                w = w.toUpperCase();

                if (count == temp.length) {
                    String[] bigger = new String[temp.length * 2];
                    for (int i = 0; i < temp.length; i++) {
                        bigger[i] = temp[i];
                    }
                    temp = bigger;
                }

                temp[count] = w;
                count++;
            }

        } catch (Exception e) {
            IO.println("Error reading: " + fileName);
            return new String[0];
        }

        String[] result = new String[count];
        for (int i = 0; i < count; i++) {
            result[i] = temp[i];
        }

        return result;
    }
}
