package com.plane.files.demo;

import java.io.IOException;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.codec.net.URLCodec;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KbKnowledgeBase {

    private HttpClient httpClient;

    private String instance;

    protected static Logger log = LoggerFactory.getLogger(KbKnowledgeAPI.class);

    final static String DEFAULT_ASSIGNMENT_GROUP = "HR Europe";

    final static String DEFAULT_SHORT_DESC = "Personnel Handbook";

    KbKnowledge getKbKnowledge(Path html) throws Exception {

        KbKnowledge kb = null;
        try {

            kb = fromFile(html);

            kb.setKnowledgeBaseId(getKbIdByName(kb.getKnowledgeBaseTitle()));

            kb.setCategoryId(getKbCategoryIdByName(kb.getCategoryFullName()));

        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return null;
        }

        kb.setAssignmentGroupId(getAssignmentGroupByName(DEFAULT_ASSIGNMENT_GROUP));
        return kb;
    }

    String getKbIdByName(String kbName) throws Exception {

        log.info("Reading kb_knowledge_base for {}", kbName);

        return getIdByURLQuery("table/kb_knowledge_base" + "?title=" + new URLCodec().encode(kbName));
    }

    String getAssignmentGroupByName(String name) throws Exception {

        return getIdByURLQuery("table/sys_user_group" + "?name=" + new URLCodec().encode(name));
    }

    String getIdByURLQuery(String q) {

        if (this.httpClient == null) {
            log.error("HttpClient not set");
            throw new IllegalStateException("HttpClient not set");
        }

        HttpGet request = new HttpGet(instance + KbKnowledgeAPI.API_PATH + q + "&sysparm_fields=sys_id");

        log.debug("Request uri: {}", request.getURI().toString());

        request.addHeader("Accept", "application/json");

        HttpResponse response;
        try {
            response = httpClient.execute(request);

            handleTableApiResponse(response);

            log.info("Response status: {}", response.getStatusLine().getStatusCode());

            return getSysId(response);

        } catch (ClientProtocolException e) {
            log.error(e.getMessage(), e);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }

    void handleTableApiResponse(HttpResponse response) {
        if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300) {
            log.error("Internal error, server response code {}", response.getStatusLine().getStatusCode());
            System.exit(-1);
        }

    }

    final ObjectMapper mapper = new ObjectMapper();

    protected String getSysId(HttpResponse response) throws IOException {
        HttpEntity respEntity = response.getEntity();
        String sysId = null;
        if (respEntity != null) {

            String result = EntityUtils.toString(respEntity);

            JsonNode root = mapper.readTree(result);
            sysId = root.at("/result/0/sys_id").asText();

            log.debug("Response: {}\n", root.toPrettyString());

            log.info("Record sys_id:{}", sysId);

        }
        return sysId;
    }

    String getKbCategoryIdByName(String kbCategory) throws Exception {

        log.info("Reading kb_category for {}", kbCategory);

        return getIdByURLQuery("table/kb_category" + "?full_category=" + new URLCodec().encode(kbCategory));
    }

    static class KbKnowledge {

        private String sysId;

        private String categoryId;

        private String shortDesc;

        private String knowledgeBaseId;

        private String assignmentGroupId;

        private String categoryFullName;

        private String knowledgeBaseTitle;

        public String getSysId() {
            return sysId;
        }

        public void setSysId(String sysId) {
            this.sysId = sysId;
        }

        public String getCategoryId() {
            return categoryId;
        }

        public void setCategoryId(String categoryId) {
            this.categoryId = categoryId;
        }

        public String getKnowledgeBaseId() {
            return knowledgeBaseId;
        }

        public void setKnowledgeBaseId(String knowledgeBaseId) {
            this.knowledgeBaseId = knowledgeBaseId;
        }

        public String getAssignmentGroupId() {
            return assignmentGroupId;
        }

        public void setAssignmentGroupId(String assignmentGroupId) {
            this.assignmentGroupId = assignmentGroupId;
        }

        public String getCategoryFullName() {
            return categoryFullName;
        }

        public void setCategoryFullName(String categoryFullName) {
            this.categoryFullName = categoryFullName;
        }

        public String getKnowledgeBaseTitle() {
            return knowledgeBaseTitle;
        }

        public void setKnowledgeBaseTitle(String knowledgeBaseTitle) {
            this.knowledgeBaseTitle = knowledgeBaseTitle;
        }

        public String getShortDesc() {
            return shortDesc;
        }

        public void setShortDesc(String shortDesc) {
            this.shortDesc = shortDesc;
        }
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public KbKnowledgeBase(String instance) {
        this.instance = instance;
    }

    public final static String DEFAULT_KB = "Internal HR Knowledge - Europe";

    KbKnowledge fromFile(Path html) throws IOException {
        Document doc = Jsoup.parse(html.toFile(), "UTF-8");

        KbKnowledge kl = new KbKnowledge();

        Element slugs = doc.select("#breadcrumbs").first();

        String lLevel = extractLowestLevel(doc);

        if (slugs == null) {
            kl.setKnowledgeBaseTitle(DEFAULT_KB);
        } else {
            int i = slugs.children().size();

            log.debug("Number of slugs to analyze {}, file {}", i, html.toString());
            switch (i) {
            case 4: {
                kl.setKnowledgeBaseTitle(slugs.child(2).select("span a").first().text());
                kl.setCategoryFullName(slugs.child(3).select("span a").first().text());
                break;
            }
            case 3: {
                kl.setKnowledgeBaseTitle(slugs.child(2).select("span a").first().text());
                kl.setCategoryFullName(lLevel);
                break;
            }
            case 2: {
                kl.setKnowledgeBaseTitle(lLevel);
                break;
            }
            case 1: {
                kl.setKnowledgeBaseTitle(DEFAULT_KB);
                break;
            }
            default: {
                if (i > 0) {
                    kl.setKnowledgeBaseTitle(slugs.child(2).select("span a").first().text());
                    kl.setCategoryFullName(slugs.child(slugs.children().size() - 1).select("span a").first().text());
                    break;
                }
            }
            }
        }

        kl.setShortDesc(lLevel);

        log.debug("Parsed fields: shortdesc: [{}], knowledgebase: [{}], catgegory: [{}]", kl.getShortDesc(), kl.getKnowledgeBaseTitle(),
                kl.getCategoryFullName());

        return kl;
    }

    String extractLowestLevel(Document doc) {

        String lLevel = doc.select("title").first().text();

        if (lLevel != null) {
            String[] fragments = lLevel.split(":");
            if (fragments.length > 1) {
                return fragments[1].trim();
            }
        }

        return DEFAULT_SHORT_DESC;

    }
}