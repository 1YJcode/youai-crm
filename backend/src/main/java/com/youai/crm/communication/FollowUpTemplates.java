package com.youai.crm.communication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.util.*;
@Component
public class FollowUpTemplates {
    public record Template(String name,List<String> labels){}
    private final List<Template> templates;
    public FollowUpTemplates(ObjectMapper json)throws java.io.IOException {
        try(var input=getClass().getResourceAsStream("/followup-templates.json")){templates=List.of(json.readValue(input,Template[].class));}
    }
    public List<Template> all(){return templates;}
    public boolean contains(String name){return templates.stream().anyMatch(t->t.name().equals(name));}
}
