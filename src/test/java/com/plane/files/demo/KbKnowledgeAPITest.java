package com.plane.files.demo;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

public class KbKnowledgeAPITest {
    
    @Test
	void testWhenCreateResourcesReferencesThenNoExceptionIsThrown() throws IOException {

		final String SYS_ID = new RandomString(32).nextString();

		StringEntity entity = new StringEntity("{\"result\":{\"sys_id\": \"" + SYS_ID + "\"}}",
				ContentType.APPLICATION_JSON);

		KbKnowledgeAPI kb = new KbKnowledgeAPI("user", "pass", "instance", Paths.get("src/test/resources/kb"));

		HttpClient httpClient = mock(HttpClient.class);
		HttpResponse httpResponse = mock(HttpResponse.class);

		StatusLine statusLine = mock(StatusLine.class);

		when(statusLine.getStatusCode()).thenReturn(200);
		when(statusLine.getReasonPhrase()).thenReturn("");
		when(httpResponse.getStatusLine()).thenReturn(statusLine);
		when(httpResponse.getEntity()).thenReturn(entity);
		when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
		when(httpClient.execute(any(HttpPatch.class))).thenReturn(httpResponse);
		when(httpClient.execute(any(HttpGet.class))).thenReturn(httpResponse);

		kb.setHttpClient(httpClient);

		kb.withKbKnowledgeBase();

		try {
			kb.createResourceReferences();
		} catch (IOException e) {

			fail(e.getMessage(), e);

		}
	}

	@AfterAll
	public static void deleteOutputFiles() throws IOException {

        Files.deleteIfExists(Paths.get("src/test/resources", "HELP.html.1"));
        Files.deleteIfExists(Paths.get("src/test/resources/kb", "index.html.1"));
		Files.deleteIfExists(Paths.get("src/test/resources/kb", "48728526.html.1"));
		Files.deleteIfExists(Paths.get("src/test/resources/kb", "77955928.html.1"));
		Files.deleteIfExists(Paths.get("src/test/resources/kb", "Bonus-program_181111011.html.1"));
		Files.deleteIfExists(Paths.get("src/test/resources/kb", "Bonus-program_181111012.html.1"));
		Files.deleteIfExists(Paths.get("src/test/resources/kb", "Employee-offers_38240556.html.1"));
		Files.deleteIfExists(Paths.get("src/test/resources/kb", "Common-Personnel-Handbook-in-Circle-K_38240562.html.1"));
		Files.deleteIfExists(Paths.get("src/test/resources/kb", "Personnel-Handbook-Norway_38240668.html.1"));
	}
	
}