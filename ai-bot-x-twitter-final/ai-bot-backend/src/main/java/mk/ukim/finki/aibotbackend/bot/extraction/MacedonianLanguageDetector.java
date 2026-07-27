package mk.ukim.finki.aibotbackend.bot.extraction;

import java.util.Arrays;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class MacedonianLanguageDetector implements LanguageDetector {
    private static final String STRONG_MACEDONIAN_DISTINCTIVE = "ѓќѕ";
    private static final String SHARED_SOUTH_SLAVIC = "јљњџ";
    private static final String NON_MACEDONIAN_CYRILLIC = "ёыэъщяюіўєґћђ";

    private static final Set<String> MACEDONIAN_WORDS = Set.of(
        "на", "во", "и", "за", "со", "од", "до", "што", "се", "не", "како",
        "ние", "вие", "тие", "ова", "овој", "оваа", "овие", "тоа", "кој", "која",
        "кои", "секој", "секоја", "има", "нема", "беше", "биле", "сме", "сум",
        "денес", "утре", "вчера", "многу", "повеќе", "може", "треба", "сака",
        "македонија", "македонски", "скопје", "охрид", "јазик", "објава"
    );

    private static final Set<String> FOREIGN_WORDS = Set.of(
        // Russian
        "что", "это", "уже", "когда", "все", "типа", "года", "мне", "взяли",
        "нравится", "которые", "союзники", "союзников", "перебежали",
        // Serbian/Bosnian
        "nije", "koji", "koja", "ovo", "samo", "zbog",
        "је", "су", "биле", "стране", "народ", "више", "држава", "изјавио",
        "председник", "данас", "руски", "српски", "овог", "овом", "који",
        "тему", "совјетски", "разлике", "историчара", "продаја", "обвезница",
        "својина", "целог", "србије", "дугови", "оног", "главници", "тајним",
        "уговорима", "лоповлук",
        // Bulgarian
        "това", "този", "което", "със", "беше", "няма", "съм"
    );

    @Override
    public double macedonianConfidence(String text) {
        if (text == null || text.isBlank()) {
            return 0.0;
        }

        String normalized = text.toLowerCase();
        String[] words = Arrays.stream(normalized.split("[^\\p{L}]+"))
            .filter(word -> !word.isBlank())
            .toArray(String[]::new);

        long letters = normalized.codePoints().filter(Character::isLetter).count();
        if (letters == 0) {
            return 0.0;
        }

        long cyrillic = normalized.codePoints()
            .filter(codePoint ->
                Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.CYRILLIC)
            .count();
        long strongDistinctive = normalized.codePoints()
            .filter(codePoint -> STRONG_MACEDONIAN_DISTINCTIVE.indexOf(codePoint) >= 0)
            .count();
        long sharedSouthSlavic = normalized.codePoints()
            .filter(codePoint -> SHARED_SOUTH_SLAVIC.indexOf(codePoint) >= 0)
            .count();
        long unsupportedCyrillic = normalized.codePoints()
            .filter(codePoint -> NON_MACEDONIAN_CYRILLIC.indexOf(codePoint) >= 0)
            .count();
        long macedonianWords = Arrays.stream(words).filter(MACEDONIAN_WORDS::contains).count();
        long foreignWords = Arrays.stream(words).filter(FOREIGN_WORDS::contains).count();

        double cyrillicRatio = (double) cyrillic / letters;
        if (cyrillicRatio < 0.35) {
            return 0.0;
        }

        double scriptScore = Math.min(1.0, cyrillicRatio);
        double strongDistinctiveScore = Math.min(1.0, strongDistinctive / 2.0);
        double sharedSouthSlavicScore = Math.min(1.0, sharedSouthSlavic / 2.0);
        double lexicalScore = Math.min(1.0, macedonianWords / 3.0);
        double confidence =
            scriptScore * 0.15
                + strongDistinctiveScore * 0.25
                + sharedSouthSlavicScore * 0.15
                + lexicalScore * 0.45;

        // Russian, Bulgarian, Ukrainian and Serbian Cyrillic contain letters that are
        // not part of the Macedonian alphabet. Multiple such letters are decisive.
        if (unsupportedCyrillic >= 2) {
            confidence = Math.min(confidence, 0.15);
        } else if (unsupportedCyrillic == 1) {
            confidence *= 0.55;
        }

        // Lexical evidence prevents another Cyrillic language from passing only
        // because it shares common short words such as "на" and "и".
        if (foreignWords >= 2) {
            confidence = Math.min(confidence, 0.20);
        } else if (foreignWords == 1) {
            confidence *= 0.70;
        }

        return Math.max(0.0, Math.min(1.0, confidence));
    }
}
