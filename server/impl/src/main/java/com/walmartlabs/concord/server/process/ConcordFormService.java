package com.walmartlabs.concord.server.process;

import com.walmartlabs.concord.common.ConfigurationUtils;
import com.walmartlabs.concord.project.Constants;
import com.walmartlabs.concord.server.api.process.FormListEntry;
import com.walmartlabs.concord.server.process.pipelines.ResumePipeline;
import com.walmartlabs.concord.server.process.pipelines.processors.Chain;
import com.walmartlabs.concord.server.process.state.ProcessStateManagerImpl;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.form.*;
import io.takari.bpm.form.DefaultFormService.ResumeHandler;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Named
@Singleton
public class ConcordFormService {

    private final PayloadManager payloadManager;
    private final ProcessStateManagerImpl stateManager;
    private final FormValidator validator;
    private final Chain resumePipeline;

    @Inject
    public ConcordFormService(
            PayloadManager payloadManager,
            ProcessStateManagerImpl stateManager,
            ResumePipeline resumePipeline) {

        this.payloadManager = payloadManager;
        this.stateManager = stateManager;
        this.resumePipeline = resumePipeline;
        this.validator = new DefaultFormValidator();
    }

    public Form get(String processInstanceId, String formInstanceId) {
        // TODO cache?
        String resource = Constants.Files.JOB_ATTACHMENTS_DIR_NAME + "/" +
                Constants.Files.JOB_STATE_DIR_NAME + "/" +
                Constants.Files.JOB_FORMS_DIR_NAME + "/" +
                formInstanceId;

        Optional<Form> o = stateManager.get(processInstanceId, resource, ConcordFormService::deserialize);
        return o.orElse(null);
    }

    private static Optional<Form> deserialize(InputStream data) {
        try (ObjectInputStream in = new ObjectInputStream(data)) {
            return Optional.ofNullable((Form) in.readObject());
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Error while deserializing a form", e);
        }
    }

    public List<FormListEntry> list(String processInstanceId) throws ExecutionException {
        String resource = Constants.Files.JOB_ATTACHMENTS_DIR_NAME + "/" +
                Constants.Files.JOB_STATE_DIR_NAME + "/" +
                Constants.Files.JOB_FORMS_DIR_NAME + "/";

        List<Form> forms = stateManager.list(processInstanceId, resource, ConcordFormService::deserialize);
        return forms.stream().map(f -> {
            String name = f.getFormDefinition().getName();

            // TODO constants
            String s = "forms/" + f.getFormDefinition().getName();
            boolean branding = stateManager.exists(processInstanceId, s);

            return new FormListEntry(f.getFormInstanceId().toString(), name, branding);
        }).collect(Collectors.toList());
    }

    public String nextFormId(String processInstanceId) throws ExecutionException {
        String resource = Constants.Files.JOB_ATTACHMENTS_DIR_NAME + "/" +
                Constants.Files.JOB_STATE_DIR_NAME + "/" +
                Constants.Files.JOB_FORMS_DIR_NAME + "/";

        Function<String, Optional<String>> getId = s -> {
            int i = s.lastIndexOf("/");
            if (i < 0 || i + 1 >= s.length()) {
                return Optional.empty();
            }
            return Optional.of(s.substring(i + 1));
        };

        Optional<String> o = stateManager.findPath(processInstanceId, resource,
                files -> files.findFirst().flatMap(getId));

        return o.orElse(null);
    }

    public FormSubmitResult submit(String processInstanceId, String formInstanceId, Map<String, Object> data) throws ExecutionException {
        Form form = get(processInstanceId, formInstanceId);
        if (form == null) {
            throw new ExecutionException("Form not found: " + formInstanceId);
        }

        ResumeHandler resumeHandler = (f, args) -> {
            String resource = Constants.Files.JOB_ATTACHMENTS_DIR_NAME + "/" +
                    Constants.Files.JOB_STATE_DIR_NAME + "/" +
                    Constants.Files.JOB_FORMS_DIR_NAME + "/" +
                    formInstanceId;

            stateManager.delete(processInstanceId, resource);

            // TODO refactor into the process manager
            Map<String, Object> m = new HashMap<>();
            m.put("arguments", args);
            resume(f.getProcessBusinessKey(), f.getEventName(), m);
        };

        Map<String, Object> merged = merge(form, data);
        return DefaultFormService.submit(resumeHandler, validator, form, merged);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> merge(Form form, Map<String, Object> data) {
        String formName = form.getFormDefinition().getName();

        Map<String, Object> env = form.getEnv();
        if (env == null) {
            env = Collections.emptyMap();
        }

        Map<String, Object> formState = (Map<String, Object>) env.get(formName);
        if (formState == null) {
            formState = Collections.emptyMap();
        }

        Map<String, Object> a = new HashMap<>(formState);
        Map<String, Object> b = new HashMap<>(data != null ? data : Collections.emptyMap());

        ConfigurationUtils.merge(a, b);
        return a;
    }

    private void resume(String instanceId, String eventName, Map<String, Object> req) throws ExecutionException {
        Payload payload;
        try {
            payload = payloadManager.createResumePayload(instanceId, eventName, req);
        } catch (IOException e) {
            throw new ExecutionException("Error while creating a payload for: " + instanceId, e);
        }

        resumePipeline.process(payload);
    }
}
