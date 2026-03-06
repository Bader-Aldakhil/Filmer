package com.filmer.parser;

import java.io.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.zip.GZIPInputStream;

/**
 * TSV Parser for IMDb title.ratings.tsv.gz file
 * Parses movie ratings and updates the movies table
 * 
 * TSV Format:
 * tconst averageRating numVotes
 * tt0000001 5.7 1856
 */
public class RatingTSVParser {

    private Connection connection;
    private PreparedStatement updateStmt;
    private static final int BATCH_SIZE = 1000;
    private int batchCount = 0;
    private int processedCount = 0;
    private int skippedCount = 0;

    public RatingTSVParser(Connection connection) throws SQLException {
        this.connection = connection;
        this.connection.setAutoCommit(false);
        this.updateStmt = connection.prepareStatement(
                "UPDATE movies SET rating = ?, num_votes = ? WHERE id = ?");
    }

    public void parse(String filePath) throws IOException, SQLException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        new GZIPInputStream(
                                new FileInputStream(filePath)),
                        "UTF-8"))) {

            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // Skip header
                }

                processLine(line);
            }

            finalizeParsing();
        }
    }

    private void processLine(String line) throws SQLException {
        String[] fields = line.split("\t", -1);

        if (fields.length < 3) {
            skippedCount++;
            return;
        }

        String tconst = fields[0];
        String averageRating = fields[1];
        String numVotes = fields[2];

        try {
            updateStmt.setBigDecimal(1, new BigDecimal(averageRating));
            updateStmt.setInt(2, Integer.parseInt(numVotes));
            updateStmt.setString(3, tconst);
            updateStmt.addBatch();
            batchCount++;

            if (batchCount >= BATCH_SIZE) {
                updateStmt.executeBatch();
                connection.commit();
                batchCount = 0;
                System.out.println("✓ Updated ratings for " + processedCount + " titles...");
            }
            processedCount++;
        } catch (NumberFormatException | SQLException e) {
            skippedCount++;
        }
    }

    public void finalizeParsing() throws SQLException {
        if (batchCount > 0) {
            updateStmt.executeBatch();
            connection.commit();
        }
        updateStmt.close();
    }

    public int getProcessedCount() {
        return processedCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }
}
