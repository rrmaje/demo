package com.plane.files.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.plane.files.demo.KbKnowledgeBase.KbKnowledge;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

public class KbKnowledgeBaseTest {

	@Test
	void testWhenGetKbKnowledgeThenCategoryAndKnowledgeBaseArePopulated() throws IOException {

		final Path[] paths = { Paths.get("src/test/resources/kb", "48728526.html"),
				Paths.get("src/test/resources/kb", "Bonus-program_181111011.html"),
				Paths.get("src/test/resources/kb", "Employee-offers_38240556.html"),
				Paths.get("src/test/resources/kb", "Common-Personnel-Handbook-in-Circle-K_38240562.html"),
				Paths.get("src/test/resources/kb", "Bonus-program_181111012.html"), };

		final String[][] hierarchy = { { "Personnel Handbook - Sweden", "Blanketter" },
				{ "Common Personnel Handbook in Circle K", "Employee offers / Bonus program / Performance bonus" },
				{ "Common Personnel Handbook in Circle K", "Employee offers" },
				{ "Common Personnel Handbook in Circle K", null },
				{ "Common Personnel Handbook in Circle K", "Employee offers" } };

		KbKnowledgeBase kbl = new KbKnowledgeBase("instance");

		final String SYS_ID = new RandomString(32).nextString();

		StringEntity entity = new StringEntity("{\"result\":[{\"sys_id\": \"" + SYS_ID + "\"}]}",
				ContentType.APPLICATION_JSON);

		HttpClient httpClient = mock(HttpClient.class);
		HttpResponse httpResponse = mock(HttpResponse.class);

		StatusLine statusLine = mock(StatusLine.class);

		when(statusLine.getStatusCode()).thenReturn(200);
		when(statusLine.getReasonPhrase()).thenReturn("");
		when(httpResponse.getStatusLine()).thenReturn(statusLine);
		when(httpResponse.getEntity()).thenReturn(entity);
		when(httpClient.execute(any(HttpGet.class))).thenReturn(httpResponse);
		when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);

		kbl.setHttpClient(httpClient);

		KbKnowledge kl = null;

		int i = 0;
		for (Path html : paths) {
			try {
				kl = kbl.getKbKnowledge(html);
			} catch (Exception e) {
				fail(e.getMessage(), e);
			}

			assertNotNull(kl);

			assertEquals(SYS_ID, kl.getKnowledgeBaseId());

			assertEquals(hierarchy[i][0], kl.getKnowledgeBaseTitle());

			if (hierarchy[i][1] != null) {
				assertEquals(SYS_ID, kl.getCategoryId());
				assertEquals(hierarchy[i][1], kl.getCategoryFullName());
			}

			assertEquals(SYS_ID, kl.getAssignmentGroupId());

			i++;
		}

	}

	@Test
	void testWhenCreateIfNotExistThenCategoryTreeIsCreated() throws IOException {

		final Path[] paths = { Paths.get("src/test/resources/kb", "48728526.html"),
				Paths.get("src/test/resources/kb", "Bonus-program_181111011.html"),
				Paths.get("src/test/resources/kb", "Common-Personnel-Handbook-in-Circle-K_38240562.html"),
				Paths.get("src/test/resources/kb", "Bonus-program_181111012.html"), };

		final String[][] hierarchy = { { "Personnel Handbook - Sweden", "Blanketter", null },
				{ "Common Personnel Handbook in Circle K", "Employee offers / Bonus program / Performance bonus",
						"Performance bonus" },
				{ "Common Personnel Handbook in Circle K", null },
				{ "Common Personnel Handbook in Circle K", "Employee offers", "Employee offers" } };

		final String ROOT_SYS_ID = new RandomString(32).nextString();

		KbKnowledgeBase kbl = new KbKnowledgeBase("instance") {

			@Override
			String getKbCategoryIdByName(String kbCategory) throws Exception {
				return "Personnel Handbook - Sweden".equals(kbCategory)
						|| "Common Personnel Handbook in Circle K".equals(kbCategory) ? ROOT_SYS_ID : null;
			}

		};

		final String NEW_SYS_ID = new RandomString(32).nextString();

		HttpClient httpClient = mock(HttpClient.class);

		StatusLine statusLine = mock(StatusLine.class);

		when(statusLine.getStatusCode()).thenReturn(200);
		when(statusLine.getReasonPhrase()).thenReturn("");

		StringEntity createEntity = new StringEntity("{\"result\":{\"sys_id\": \"" + NEW_SYS_ID + "\"}}",
				ContentType.APPLICATION_JSON);

		HttpResponse httpPostResponse = mock(HttpResponse.class);

		when(httpPostResponse.getStatusLine()).thenReturn(statusLine);
		when(httpPostResponse.getEntity()).thenReturn(createEntity);
		when(httpClient.execute(any(HttpPost.class))).thenReturn(httpPostResponse);

		kbl.setHttpClient(httpClient);

		KbKnowledge kl = null;

		int i = 0;
		for (Path html : paths) {
			try {
				kl = kbl.fromFile(html);
			} catch (Exception e) {
				fail(e.getMessage(), e);
			}

			assertNotNull(kl);

			assertEquals(hierarchy[i][0], kl.getKnowledgeBaseTitle());

			if (hierarchy[i][1] != null) {
				assertEquals(NEW_SYS_ID, kl.getCategoryId());
				assertEquals(hierarchy[i][1], kl.getCategoryFullName());
			}

			i++;
		}

	}

	@AfterAll
	public static void deleteOutputFiles() throws IOException {

	}

}