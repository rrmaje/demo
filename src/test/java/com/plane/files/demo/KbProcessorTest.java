package com.plane.files.demo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

public class KbProcessorTest {

	@Test
	void testWhenTextProcessedHeaderSectionIsDeleted() throws IOException {

		final Path path = Paths.get("src/test/resources/kb", "48728526.html");

		final String[] keywords = { "div id=\"main-header\"", "div id=\"footer\"", "div class=\"page-metadata\"" };

		try {

			Stream.of(keywords).forEach(k -> {
				try {
					assertTrue(Files.readAllLines(path).stream().filter(l -> l.contains(k)).count() > 0);
				} catch (IOException e) {
					fail(e.getMessage(), e);
				}
			});

			String text = KbProcessor.processText(path);

			Stream.of(keywords).forEach(k -> assertFalse(text.contains(k)));
		} catch (IOException e) {
			fail(e.getMessage(), e);
		}

	}

}