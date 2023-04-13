import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) throws IOException {
//        String firstSentence = "abca";
//        String secondSentence = "abcabc";
//        ArrayList<String> sentences = new ArrayList<>(Arrays.asList(firstSentence, secondSentence));
//        ArrayList<String> phonemes = new ArrayList<>(Arrays.asList("a", "bc"));

//        ArrayList<ArrayList<String>> segmentedSentences = segmentSentences(sentences, phonemes);
//        System.out.println(segmentedSentences);


//        ArrayList<String> phonemes = getPhonemes("na_phonemes.txt");
//        segmentTranscriptions("phonemic_transcription_pangloss.txt", phonemes);

//        ArrayList<String> phonemes = getPhonemes("na_phonemes.txt");
//        checkSegmentationQuality("phonemic_transcription_pangloss.txt", phonemes);


        HashMap<String, Integer> frequency = getPhonemeFreq("phonemic_transcription_pangloss.txt");
        HashMap<String, Integer> sortedFrequency = sortMap(frequency);
        System.out.println(sortedFrequency);

        ArrayList<String> phonemes = getPhonemes("na_phonemes.txt");
        Set<String> usedPhonemes = sortedFrequency.keySet();
        ArrayList<String> unusedPhonemes = getUnusedPhonemes(usedPhonemes, phonemes);
        System.out.println(unusedPhonemes);
    }


    public static ArrayList<ArrayList<String>> segmentSentences(ArrayList<String> sentences, ArrayList<String> phonemes) {
        ArrayList<ArrayList<String>> segmentedSentences = new ArrayList<>();

        for (String sentence : sentences) {
            ArrayList<String> segmentedSentence = segmentSentence(sentence, phonemes);
            segmentedSentences.add(segmentedSentence);
        }

        return segmentedSentences;
    }

    public static ArrayList<String> segmentSentence(String sentence, ArrayList<String> phonemes) {
        ArrayList<String> segmentedSentence = new ArrayList<>();

        Collections.sort(phonemes, Comparator.comparing(String::length).reversed());

        while (!sentence.isEmpty()) {
            boolean phonemeIsUsed = false;

            for (String phoneme : phonemes) {
                if (sentence.startsWith(phoneme) && !phonemeIsUsed) {
                    sentence = sentence.replaceFirst(Pattern.quote(phoneme), "");
                    segmentedSentence.add(phoneme);
                    phonemeIsUsed = true;
                }
            }

            if (!phonemeIsUsed) {
                break;
            }
        }

        return segmentedSentence;
    }

    public static ArrayList<String> getPhonemes(String phonemeFileName) throws IOException {
        ArrayList<String> phonemes = new ArrayList<>();

        Path phonemeFilePath = Paths.get(phonemeFileName);
        if (Files.exists(phonemeFilePath)) {
            String phonemeFileContent = new String(Files.readAllBytes(phonemeFilePath));

            String[] lines = phonemeFileContent.split("\n");
            for(String line : lines) {
                String[] splitLine = line.split(":");
                String phoneme = splitLine[0];
                phonemes.add(phoneme);
            }
        }

        phonemes.addAll(getExceptions());
        phonemes.addAll(getTones());

        return phonemes;
    }

    public static ArrayList<String> getExceptions() {
        ArrayList<String> exceptions = new ArrayList<>();
        exceptions.add("|");
        exceptions.add("mmm…");
        exceptions.add("əəə…");

        return exceptions;
    }

    public static ArrayList<String> getTones() {
        ArrayList<String> tones = new ArrayList<>();
        tones.add("˩");
        tones.add("˥");
        tones.add("˧");
        tones.add("˧˥");
        tones.add("˩˥");
        tones.add("˩˧");
        tones.add("˧˩");

        return tones;
    }

    public static void segmentTranscriptions(String transcriptionFileName, ArrayList<String> phonemes) throws IOException {
        Path transcriptionFilePath = Paths.get(transcriptionFileName);
        if (Files.exists(transcriptionFilePath)) {
            String fileContent = new String(Files.readAllBytes(transcriptionFilePath));

            String[] lines = fileContent.split("\n");
            for(int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
                String[] splitLine = lines[lineIndex].split(" @@@ ");
                String transcription = splitLine[0];
                String cleanedTranscription = cleanTranscription(transcription);

                ArrayList<String> segmentedTranscription = segmentSentence(cleanedTranscription, phonemes);
                System.out.println("Line: " + lineIndex + " " + segmentedTranscription);
            }
        }
    }

    public static String cleanTranscription(String transcription) {
        if(transcription.contains("BEGAIEMENT")) {
            return "";
        }

        transcription = transcription.replaceAll(" ", "");
        transcription = transcription.replaceAll("◊", "|");
        transcription = transcription.replaceAll("F", "");
        transcription = transcription.replaceAll("«", "");
        transcription = transcription.replaceAll("»", "");
        transcription = transcription.replaceAll("wæ̃", "w̃æ");
        transcription = transcription.replaceAll("\\[[^]]*\\]", "");
        transcription = transcription.replaceAll("[\\p{Punct}&&[^|]]", "");
        transcription = transcription.replaceAll("(?<![mə]{3})…", "");

        return transcription;
    }

    public static void checkSegmentationQuality(String transcriptionFileName, ArrayList<String> phonemes)  throws IOException {
        Path transcriptionFilePath = Paths.get(transcriptionFileName);
        if (Files.exists(transcriptionFilePath)) {
            String fileContent = new String(Files.readAllBytes(transcriptionFilePath));

            String[] lines = fileContent.split("\n");

            int totalValidSegmentations = 0;
            for(int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
                String[] splitLine = lines[lineIndex].split(" @@@ ");
                String transcription = splitLine[0];
                String cleanedTranscription = cleanTranscription(transcription);
                ArrayList<String> segmentedTranscription = segmentSentence(cleanedTranscription, phonemes);

                ArrayList<String> reference;
                if(splitLine.length <= 1 || splitLine[1].isBlank()) {
                    reference = new ArrayList<>();
                } else {
                    reference = new ArrayList<>(Arrays.asList(splitLine[1].split(" ")));
                }

                Boolean segmentationIsValid = segmentationIsValid(segmentedTranscription, reference);

                if(segmentationIsValid) {
                    totalValidSegmentations++;
                } else {
                    int lineNumber = lineIndex + 1;
                    System.out.println("Error line: " + lineNumber);
                    System.out.println("Result found: " + segmentedTranscription);
                    System.out.println("Reference: " + reference);
                    System.out.println();
                }
            }

            System.out.println("Score: " + totalValidSegmentations + "/" + lines.length);
        }
    }

    public static Boolean segmentationIsValid(ArrayList<String> segmentedTranscription, ArrayList<String> reference) {
        if(segmentedTranscription.size() != reference.size()) {
            return false;
        }

        for(int index = 0; index != segmentedTranscription.size(); index++) {
            if(!segmentedTranscription.get(index).equals(reference.get(index))) {
                return false;
            }
        }

        return true;
    }

    public static HashMap<String, Integer> getPhonemeFreq(String transcriptionFileName)  throws IOException {
        HashMap<String, Integer> phonemeFreqMap = new HashMap<>();

        Path transcriptionFilePath = Paths.get(transcriptionFileName);
        if (Files.exists(transcriptionFilePath)) {
            String fileContent = new String(Files.readAllBytes(transcriptionFilePath));
            String[] lines = fileContent.split("\n");

            for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
                String[] splitLine = lines[lineIndex].split(" @@@ ");

                if(splitLine.length > 1 && !splitLine[1].isBlank()) {
                    ArrayList<String> reference = new ArrayList<>(Arrays.asList(splitLine[1].split(" ")));

                    for(String phoneme : reference) {
                        Integer phonemeFreq = phonemeFreqMap.getOrDefault(phoneme, 0) + 1;
                        phonemeFreqMap.put(phoneme, phonemeFreq);
                    }
                }
            }
        }

        return phonemeFreqMap;
    }

    public static LinkedHashMap<String, Integer> sortMap(HashMap<String, Integer> phonemeFreqMap) {
        LinkedHashMap<String, Integer> sortedPhonemeFreqMap = new LinkedHashMap<>();

        HashSet<Integer> uniqueScores = new HashSet<>();
        for (Entry<String, Integer> phonemeEntry : phonemeFreqMap.entrySet()) {
            uniqueScores.add(phonemeEntry.getValue());
        }

        ArrayList<Integer> scores = new ArrayList<>(uniqueScores);
        scores.sort(Comparator.reverseOrder());

        for(Integer score : scores) {
            for (Entry<String, Integer> phonemeEntry : phonemeFreqMap.entrySet()) {
                if(score.equals(phonemeEntry.getValue())) {
                    sortedPhonemeFreqMap.put(phonemeEntry.getKey(), score);
                }
            }
        }

        return sortedPhonemeFreqMap;
    }

    public static ArrayList<String> getUnusedPhonemes(Set<String> usedPhonemes, ArrayList<String> allPhonemes) {
        ArrayList<String> unusedPhonemes = new ArrayList<>();

        for(String phoneme : allPhonemes) {
            if(!usedPhonemes.contains(phoneme)) {
                unusedPhonemes.add(phoneme);
            }
        }

        return unusedPhonemes;
    }
}