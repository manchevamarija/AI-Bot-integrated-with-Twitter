package mk.ukim.finki.aibotbackend.bot.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MacedonianLanguageDetectorTest {
    private final MacedonianLanguageDetector detector = new MacedonianLanguageDetector();

    @Test
    void recognizesMacedonianCyrillic() {
        double confidence = detector.macedonianConfidence(
            "Секој македонски збор е важен за нашиот јазик и култура.");
        assertThat(confidence).isGreaterThanOrEqualTo(0.75);
    }

    @Test
    void rejectsLatinEnglishText() {
        assertThat(detector.macedonianConfidence(
            "This is an English post about software development.")).isLessThan(0.2);
    }

    @Test
    void rejectsRussianCyrillicThatMentionsFinki() {
        double confidence = detector.macedonianConfidence(
            "Мне нравится что взяли карту на 7 мая 45 года, когда уже все нейтралы "
                + "и союзники оси типа Италии, Аргентины, финки, Венгрии и тд "
                + "перебежали за союзников");

        assertThat(confidence).isLessThan(0.2);
    }

    @Test
    void rejectsBulgarianCyrillic() {
        assertThat(detector.macedonianConfidence(
            "Това е публикация, която беше написана на български език."))
            .isLessThan(0.3);
    }

    @Test
    void rejectsSerbianCyrillicWithSharedLetters() {
        assertThat(detector.macedonianConfidence(
            "\"Одлуке донешене од стране колективног Запада да се подели српски "
                + "народ у више различитих држава биле су супротне принципима УН\" "
                + "- изјавио је данас руски председник Владимир Путин."))
            .isLessThan(0.3);
    }

    @Test
    void rejectsMixedPostWhereMacedonianIsNotDominant() {
        assertThat(detector.macedonianConfidence(
            "Знам само македонски; не знам многу руски. 엔진은 책임을 요구합니다. "
                + "#Hold_HYBE_Accountable_Q2 #BoycottHYBE_ZeroCredibility"))
            .isLessThan(0.5);
    }

    @Test
    void rejectsSerbianLectureAnnouncement() {
        assertThat(detector.macedonianConfidence(
            "Предавање историчара Милована Балабана на тему „Совјетски Савез и "
                + "Русија – сличности и разлике“ (26. 7. 2026)"))
            .isLessThan(0.3);
    }

    @Test
    void rejectsSerbianPoliticalPostWithoutSerbianSpecificLetters() {
        assertThat(detector.macedonianConfidence(
            "ПОЛИТИКА је машина за ЛОПОВЛУК. Продаја државни обвезница: Својина "
                + "целог народа Р.Србије и дугови само на обвезнице, без оног дуга "
                + "у главници ка ММФ-у и оних под тајним уговорима са УАЕ, КИНА, РУСИЈА"))
            .isLessThan(0.3);
    }

    @Test
    void doesNotTreatCyrillicAloneAsMacedonianEvidence() {
        assertThat(detector.macedonianConfidence(
            "Совершенно обычное предложение без македонских слов."))
            .isLessThan(0.3);
    }

    @Test
    void handlesEmptyInput() {
        assertThat(detector.macedonianConfidence(null)).isZero();
        assertThat(detector.macedonianConfidence("   ")).isZero();
    }
}
