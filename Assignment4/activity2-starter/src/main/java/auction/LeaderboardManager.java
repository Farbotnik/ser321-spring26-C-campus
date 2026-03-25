package auction;

import buffers.LeaderboardEntry;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Leaderboard manager that tracks top scores across all players.
 * Supports adding scores and querying top N scores.
 *
 * NOTE: This class is NOT thread-safe yet. You need make it so
 * since multiple threads will access the leaderboard concurrently.
 */
public class LeaderboardManager {
    private List<ScoreEntry> scores;
    private final String persistenceFile;
    private static final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Internal class to store score entries.
     */
    private static class ScoreEntry implements Comparable<ScoreEntry> {
        String playerName;
        int score;
        LocalDateTime timestamp;

        ScoreEntry(String playerName, int score, LocalDateTime timestamp) {
            this.playerName = playerName;
            this.score = score;
            this.timestamp = timestamp;
        }

        @Override
        public int compareTo(ScoreEntry other) {
            // Sort by score descending, then by time ascending (earlier is better)
            if (this.score != other.score) {
                return Integer.compare(other.score, this.score);
            }
            return this.timestamp.compareTo(other.timestamp);
        }
    }

    /**
     * Create a new leaderboard manager.
     */
    public LeaderboardManager(String persistenceFile) {
        this.scores = new ArrayList<>();
        this.persistenceFile = persistenceFile;
        loadFromFile();
    }

    /**
     * Add a score to the leaderboard.
     * Returns the rank (1-based) of the added score.
     */
    public int addScore(String playerName, int score) {
        ScoreEntry entry = new ScoreEntry(playerName, score, LocalDateTime.now());
        scores.add(entry);
        Collections.sort(scores);

        // Save to file
        saveToFile();

        // Find rank
        for (int i = 0; i < scores.size(); i++) {
            if (scores.get(i) == entry) {
                return i + 1;
            }
        }
        return scores.size();
    }

    /**
     * Get top N scores.
     */
    public List<LeaderboardEntry> getTopScores(int n) {
        List<LeaderboardEntry> result = new ArrayList<>();

        int count = Math.min(n, scores.size());
        for (int i = 0; i < count; i++) {
            ScoreEntry entry = scores.get(i);
            LeaderboardEntry protoEntry = LeaderboardEntry.newBuilder()
                    .setRank(i + 1)
                    .setPlayerName(entry.playerName)
                    .setScore(entry.score)
                    .setTimestamp(entry.timestamp.format(formatter))
                    .build();
            result.add(protoEntry);
        }

        return result;
    }

    /**
     * Get total number of scores.
     */
    public int size() {
        return scores.size();
    }

    /**
     * Load leaderboard from file.
     */
    private void loadFromFile() {
        File file = new File(persistenceFile);
        if (!file.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 3) {
                    String name = parts[0];
                    int score = Integer.parseInt(parts[1]);
                    LocalDateTime time = LocalDateTime.parse(parts[2], formatter);
                    scores.add(new ScoreEntry(name, score, time));
                }
            }
            Collections.sort(scores);
        } catch (IOException | NumberFormatException e) {
            System.err.println("Warning: Could not load leaderboard from file: " + e.getMessage());
        }
    }

    /**
     * Save leaderboard to file.
     */
    private void saveToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(persistenceFile))) {
            for (ScoreEntry entry : scores) {
                writer.write(entry.playerName + "|" +
                            entry.score + "|" +
                            entry.timestamp.format(formatter));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not save leaderboard to file: " + e.getMessage());
        }
    }

    /**
     * Clear all scores (for testing).
     */
    public void clear() {
        scores.clear();
        saveToFile();
    }
}
