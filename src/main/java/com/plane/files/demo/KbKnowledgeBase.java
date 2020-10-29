package com.plane.files.demo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.codec.net.URLCodec;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KbKnowledgeBase {

    private HttpClient httpClient;

    private String instance;

    protected static Logger log = LoggerFactory.getLogger(KbKnowledgeBase.class);

    final static String DEFAULT_ASSIGNMENT_GROUP = "HR Europe";

    final static String DEFAULT_SHORT_DESC = "Personnel Handbook";

    static Map<String, String> languages;

    private static boolean useTranslatedVersion = false;

    static Map<String, String> getSupportedLanguages() {
        if (languages == null) {
            languages = new HashMap<>();
            languages.put("Denmark", "da");
            languages.put("Personnel Handbook - Poland", "pl");
            languages.put("Sweden", "sv");
            languages.put("Estonia", "et");
            languages.put("Latvia", "lv");
            languages.put("Lithuania", "lt");
            languages.put("Norway", "nb");
        }

        return languages;
    }

    KbKnowledge getKbKnowledge(Path html) throws Exception {

        final KbKnowledge kb;
        try {

            kb = fromFile(html);

            kb.setKnowledgeBaseId(getKbIdByName(kb.getKnowledgeBaseTitle()));

            if (kb.getCategoryId() == null) {
                kb.setCategoryId(getKbCategoryIdByName(kb.getCategoryFullName()));
            }

            if (KbKnowledgeBase.useTranslatedVersion) {
                getSupportedLanguages().keySet().stream().filter(k -> kb.getKnowledgeBaseTitle().contains(k)).findAny()
                        .ifPresent(k -> kb.setLang(languages.get(k)));

                log.debug("Path [{}], Language [{}]", html.toString(), kb.getLang());
            }
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
        return getSysId(response, "/result/0/sys_id");
    }

    protected String getSysId(HttpResponse response, String path) throws IOException {
        HttpEntity respEntity = response.getEntity();
        String sysId = null;
        if (respEntity != null) {

            String result = EntityUtils.toString(respEntity);

            JsonNode root = mapper.readTree(result);
            sysId = root.at(path).asText();

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

        private String lang = "en";

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

        public String getLang() {
            return lang;
        }

        public void setLang(String lang) {
            this.lang = lang;
        }
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public KbKnowledgeBase(String instance) {
        this.instance = instance;
    }

    static class KbCategory {

        private KbCategory parent;

        private String label;

        public String getFullName() {
            if (parent == null || parent.getParent() == null) {
                return label;
            } else {
                return parent.getFullName() + " / " + label;
            }
        }

        public KbCategory getParent() {
            return parent;
        }

        public void setParent(KbCategory parent) {
            this.parent = parent;
        }

        public KbCategory(KbCategory parent, String label) {
            this.parent = parent;
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

    }

    public final static String DEFAULT_KB = "Internal HR Knowledge - Europe";

    KbKnowledge fromFile(Path html) throws Exception {
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
            case 3: {
                // default
                String potentialKblTitle = slugs.child(2).select("span a").first().text();

                String kblId = getKbIdByName(potentialKblTitle);

                if (kblId != null && !kblId.isEmpty()) {
                    // this is edge case
                    kl.setCategoryFullName(lLevel);
                    kl.setKnowledgeBaseTitle(potentialKblTitle);
                } else {
                    // can be a variant with kb_knowledge_base under index 1
                    potentialKblTitle = slugs.child(1).select("span a").first().text();

                    String categoryLabel = slugs.child(2).select("span a").first().text();

                    kl.setKnowledgeBaseTitle(potentialKblTitle);

                    KbCategory parent = new KbCategory(null, kl.getKnowledgeBaseTitle());
                    KbCategory curr = new KbCategory(parent, categoryLabel);

                    String nCategory = createIfNotExist(curr);

                    kl.setCategoryId(nCategory);

                    kl.setCategoryFullName(curr.getFullName());
                }

                break;
            }
            case 2: {
                

                String kblId = getKbIdByName(lLevel);

                if (kblId != null && !kblId.isEmpty()) {
                    kl.setKnowledgeBaseTitle(lLevel);
                } else {
                    String potentialKblTitle = slugs.child(1).select("span a").first().text();
                    
                    kl.setKnowledgeBaseTitle(potentialKblTitle);
                    
                }

                KbCategory curr = new KbCategory(null, lLevel);

                String nCategory = createIfNotExist(curr);

                kl.setCategoryId(nCategory);

                kl.setCategoryFullName(curr.getFullName());

                break;
            }
            case 1: {
                kl.setKnowledgeBaseTitle(DEFAULT_KB);
                break;
            }
            default: {
                if (i > 0) {

                    // default
                    String potentialKblTitle = slugs.child(2).select("span a").first().text();

                    String kblId = getKbIdByName(potentialKblTitle);

                    int k = 3;
                    if (kblId == null || kblId.isEmpty()) {
                        // can be a variant with kb_knowledge_base under index 1
                        potentialKblTitle = slugs.child(1).select("span a").first().text();
                        k = 2;
                    }

                    kl.setKnowledgeBaseTitle(potentialKblTitle);

                    KbCategory parent = new KbCategory(null, kl.getKnowledgeBaseTitle());

                    KbCategory curr = null;
                    for (; k < i; k++) {
                        curr = new KbCategory(parent, slugs.child(k).select("span a").first().text());
                        parent = curr;
                    }

                    String nCategory = createIfNotExist(curr);

                    log.debug("Category sys_id: [{}], full_name:[{}]", nCategory, curr.getFullName());

                    kl.setCategoryId(nCategory);

                    kl.setCategoryFullName(curr.getFullName());
                    break;
                }
            }
            }
        }

        kl.setShortDesc(lLevel);

        log.debug("Parsed fields: shortdesc: [{}], knowledgebase: [{}], catgegory: [{}]", kl.getShortDesc(),
                kl.getKnowledgeBaseTitle(), kl.getCategoryFullName());

        return kl;
    }

    String createIfNotExist(KbCategory c) throws Exception {
        log.debug("Category [{}], parent [{}]", c.getFullName(),
                c.getParent() != null ? c.getParent().getFullName() : null);
        if (c.getParent() == null) {

            String nCategory = getKbCategoryIdByName(c.getFullName());

            if (nCategory == null || nCategory.isEmpty()) {

                String kblId = getKbIdByName(c.getLabel());

                nCategory = createKbCategory(c.getLabel(), kblId, "kb_knowledge_base");
            }

            return nCategory;

        } else {
            String id = getKbCategoryIdByName(c.getFullName());
            if (id == null || id.isEmpty()) {
                String p = getKbCategoryIdByName(c.getParent().getFullName());
                if (p == null || p.isEmpty()) {
                    p = createIfNotExist(c.getParent());
                }
                return createKbCategory(c.getLabel(), p, c.getParent() == null ? "kb_knowledge_base" : "kb_category");
            }
            return id;
        }
    }

    String createKbCategory(String label, String parentSysId, String parentTable) throws Exception {

        Map<String, Object> params = new HashMap<>();

        params.put("parent_id", parentSysId);
        params.put("parent_table", parentTable);
        params.put("label", label);
        params.put("value", Math.abs(hashCode()));

        String payload = mapper.writeValueAsString(params);

        log.debug("Creating category from data: {}", payload);

        StringEntity entity = new StringEntity(payload, ContentType.APPLICATION_JSON);

        HttpPost request = new HttpPost(instance + "/api/now/table/kb_category");

        request.setEntity(entity);

        if (this.httpClient == null) {
            log.error("HttpClient not set");
            throw new IllegalStateException("HttpClient not set");
        }

        HttpResponse response = httpClient.execute(request);
        log.info("Response status: {}", response.getStatusLine().getStatusCode());

        handleTableApiResponse(response);

        return getSysId(response, "/result/sys_id");

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

    public static boolean isUseTranslatedVersion() {
        return useTranslatedVersion;
    }

    static void setUseTranslatedVersion(boolean useTranslatedVersion) {
        KbKnowledgeBase.useTranslatedVersion = useTranslatedVersion;
    }
}