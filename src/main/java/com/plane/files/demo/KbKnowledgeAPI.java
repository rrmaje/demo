package com.plane.files.demo;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KbKnowledgeAPI {

    private String user;

    private String pass;

    private String instance;

    private Path basedir;

    private Map<String, String> pathToKbSysId = new HashMap<>();

    private Map<String, String> pathToAttachmentSysId = new HashMap<>();

    final ObjectMapper mapper = new ObjectMapper();

    protected static Logger log = LoggerFactory.getLogger(KbKnowledgeAPI.class);

    public static final String SRC_PATTERN = "<img.+?(src=\"(.*?)\")";
    public static final String HREF_PATTERN = "(href=\"(.*?)\")";

    static final String DEFAULT_EXTENSSION = ".html";

    static final String OUT_EXTENSSION = ".1";

    public static final String SYS_ATTACHMENT_SRC_PREFIX = "sys_attachment.do?sys_id=";

    public static final String KB_SRC_PREFIX = "kb_view.do?sys_kb_id=";

    public static final String DEFAULT_INSTANCE = "https://circlekdev.service-now.com";

    public final static Pattern[] REWRITE_PATTERNS = { Pattern.compile(SRC_PATTERN), Pattern.compile(HREF_PATTERN) };

    void createResourceReferences() throws IOException {
        processFiles(basedir, createReferences, createKb, patchKb);

        int linksCount = pathToKbSysId.keySet() != null ? pathToKbSysId.keySet().size() : 0;
        int attachmentsCount = pathToAttachmentSysId.keySet() != null ? pathToAttachmentSysId.keySet().size() : 0;
        log.info("\nProcessing stats:\n\tLinks count: {}\n\tAttachments count: {}", linksCount, attachmentsCount);
    }

    void processFiles(Path p, Function<KbLine, String> processingFunc, Function<Path, String> createKb,
            BiFunction<String, Path, String> updateKbFunc) throws IOException {
        log.debug("Running from {}", p.toAbsolutePath());
        List<Path> files = Files.walk(p, 1).filter(s -> {
            return s.toAbsolutePath().getFileName().toString().endsWith(DEFAULT_EXTENSSION);
        }).collect(Collectors.toList());
        for (Path path : files) {

            procesFile(path, processingFunc, createKb, updateKbFunc);

        }
    }

    static class KbLine {
        private String sysId;

        private String line;

        public String getSysId() {
            return sysId;
        }

        public void setSysId(String sysId) {
            this.sysId = sysId;
        }

        public String getLine() {
            return line;
        }

        public void setLine(String line) {
            this.line = line;
        }

        public KbLine(String sysId, String line) {
            this.sysId = sysId;
            this.line = line;
        }
    }

    protected String procesFile(Path path, Function<KbLine, String> processingFunc, Function<Path, String> createKb,
            BiFunction<String, Path, String> updateKbFunc) throws IOException {
        Path newPath = getWritePath(path);
        log.debug("Writing to {}", newPath);

        // create sys_id
        final String sysId = createKb != null ? createKb.apply(newPath) : null;

        // rewrite references
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(newPath))) {
            Files.lines(path).map(x -> {
                return new KbLine(sysId, x);
            }).forEach(k -> {

                String line = processingFunc.apply(k);

                out.println(line);

            });
        }

        // update kb_knwoledge with rewritten references
        return updateKbFunc != null ? updateKbFunc.apply(sysId, newPath) : null;

    }

    Path getWritePath(Path p) {
        return Paths.get(p.toAbsolutePath().toString().concat(OUT_EXTENSSION));
    }

    String getKbFileName(Path p) {
        String s = p.getName(p.getNameCount() - 1).toString().replace(OUT_EXTENSSION, "");
        log.info("KbFileName: {}", s);
        return s;
    }

    Function<KbLine, String> createReferences = (KbLine line) -> {
        for (Pattern pattern : REWRITE_PATTERNS) {
            Matcher m = pattern.matcher(line.getLine());

            while (m.find()) {

                String path = m.group(2);
                String sysId = null;

                log.debug("Analyzing resource [{}]", path);

                if (isKnowledgeRef(path)) {
                    sysId = pathToKbSysId.get(getKbFileName(Paths.get(path)));
                    if (sysId == null) {
                        Path kbPath = basedir.resolve(path);
                        if (!Files.exists(kbPath)) {
                            log.error("Kb Knowledge file not found: {}", kbPath);
                        } else {

                            // create Kb recursively
                            try {
                                sysId = this.procesFile(kbPath, this.createReferences, this.createKb, this.patchKb);
                            } catch (IOException e) {
                                log.error("Error creating Kb Knowledge: {}", kbPath, e);
                                continue;
                            }
                        }
                    } else {
                        log.info("Returning with sys_id from cache {}", sysId);
                    }

                    if (sysId != null) {
                        line.setLine(line.getLine().replace(m.group(2), KB_SRC_PREFIX.concat(sysId)));
                    }
                } else if (isAttachmentRef(path)) {
                    sysId = pathToAttachmentSysId.get(m.group(2));
                    if (sysId == null) {
                        sysId = this.createAttachment.apply(line.getSysId(), path);
                        if (sysId != null) {
                            pathToAttachmentSysId.put(path, sysId);
                        }
                    }
                    if (sysId != null) {
                        line.setLine(line.getLine().replace(m.group(2), SYS_ATTACHMENT_SRC_PREFIX.concat(sysId)));
                    }
                }

            }

        }
        return line.getLine();
    };

    protected boolean isKnowledgeRef(String name) {
        return !name.startsWith("http") && name.endsWith(DEFAULT_EXTENSSION);
    }

    protected boolean isAttachmentRef(String name) {
        return !name.startsWith("http") && !name.endsWith(DEFAULT_EXTENSSION)
                && !name.contains(SYS_ATTACHMENT_SRC_PREFIX) && !name.startsWith("mailto");
    }

    BiFunction<String, String, String> createAttachment = (String sysId, String name) -> {
        log.info("Creating Attachment record from [{}]", name);

        try {
            Path path = basedir.resolve(Paths.get(name));

            if (Files.exists(path)) {
                String mimeType = null;
                try {
                    mimeType = Files.probeContentType(path);
                } catch (IOException e1) {
                    log.error("Exception probing content type", e1);
                }

                if (mimeType == null) {
                    log.error("Unknown content type, trying application/octet-stream, path: {}", path);
                    mimeType = ContentType.APPLICATION_OCTET_STREAM.getMimeType();
                }

                try {

                    FileEntity file = new FileEntity(path.toFile(), ContentType.create(mimeType));

                    HttpResponse response = postRecord(file,
                            String.format(ATTACHMENT_API_PATH, sysId, path.getFileName().toString()));

                    String aSysId = getSysId(response);

                    log.debug("Attachment sys_id {}", aSysId);

                    return aSysId;

                } catch (IOException e) {
                    log.error("Exception when creating attachment:", e);

                }
            } else {
                log.error("File not found {}", path);
            }
        } catch (Exception e) {
            log.error("Unexpected error", e);
        }

        return null;
    };

    protected String getSysId(HttpResponse response) throws IOException {
        HttpEntity respEntity = response.getEntity();
        String sysId = null;
        if (respEntity != null) {

            String result = EntityUtils.toString(respEntity);

            JsonNode root = mapper.readTree(result);
            sysId = root.at("/result/sys_id").asText();

            log.info("Record sys_id:{}", sysId);

            log.debug("Response: {}", result);

        }
        return sysId;
    }

    Function<Path, String> createKb = (Path name) -> {
        log.info("Creating Knowledge record from [{}]", name);

        String fileName = getKbFileName(name);

        String sysId = pathToKbSysId.get(fileName);

        if (sysId == null) {
            try {

                Map<String, Object> params = new HashMap<>();
                params.put("short_description", getShortDesc(name));
                String payload = mapper.writeValueAsString(params);

                HttpResponse response = postRecord(payload, KB_KNOWLEDGE_API_PATH);

                handleTableApiResponse(response);

                sysId = getSysId(response);

                pathToKbSysId.put(fileName, sysId);

                log.info("kb_knowledge {}, path {}", pathToKbSysId.get(fileName), fileName);

                return sysId;

            } catch (IOException e) {
                log.error("Exception when creating kb:", e);
                System.exit(-1);

            }
        } else {
            log.debug("Reading sys_id from cache {}", sysId);
        }

        return sysId;
    };

    BiFunction<String, Path, String> patchKb = (String sysId, Path name) -> {
        log.info("Patching Knowledge record from [{}]", name);

        try {
            String text = new String(Files.readAllBytes(name));

            Map<String, Object> params = new HashMap<>();
            params.put("text", text);
            String payload = mapper.writeValueAsString(params);

            HttpResponse response = patchRecord(payload, KB_KNOWLEDGE_API_PATH + "/" + sysId);

           handleTableApiResponse(response);

            return getSysId(response);

        } catch (IOException e) {
            log.error("Exception when creating kb:", e);

        }

        return null;
    };

    void handleTableApiResponse(HttpResponse response) {
        if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300) {
            log.error("Internal error, server response code {}", response.getStatusLine().getStatusCode());
            System.exit(-1);
        }

    }

    protected static final String API_PATH = "/api/now/";

    protected static final String KB_KNOWLEDGE_API_PATH = "table/kb_knowledge";

    protected static final String ATTACHMENT_API_PATH = "attachment/file?table_name=kb_knowledge&table_sys_id=%s&file_name=%s";

    protected HttpResponse patchRecord(String payload, String apiCall) throws IOException {

        StringEntity entity = new StringEntity(payload, ContentType.APPLICATION_JSON);

        HttpPatch request = new HttpPatch(instance + API_PATH + apiCall);

        request.setEntity(entity);

        return sendRecord(request);
    }

    protected HttpResponse postRecord(String payload, String apiCall) throws IOException {

        StringEntity entity = new StringEntity(payload, ContentType.APPLICATION_JSON);

        return postRecord(entity, apiCall);
    }

    protected HttpResponse postRecord(HttpEntity entity, String apiCall) throws IOException {

        HttpPost request = new HttpPost(instance + API_PATH + apiCall);

        request.setEntity(entity);

        return sendRecord(request);

    }

    private HttpResponse sendRecord(HttpEntityEnclosingRequestBase request) throws IOException {

        CredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(user, pass));

        HttpClient httpClient = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();

        HttpResponse response = httpClient.execute(request);
        log.info("Response status: {}", response.getStatusLine().getStatusCode());

        return response;
    }

    String getShortDesc(Path p) {
        String fileName = p.getFileName().toString();

        String desc = fileName.substring(0, fileName.length() - (OUT_EXTENSSION.length()));

        return desc.replaceAll("_[0-9]+", "").replace(DEFAULT_EXTENSSION, "");

    }

    public KbKnowledgeAPI(String user, String pass, String instance, Path basedir) {
        this.user = user;
        this.pass = pass;
        this.instance = instance;
        this.basedir = basedir;
    }

}
