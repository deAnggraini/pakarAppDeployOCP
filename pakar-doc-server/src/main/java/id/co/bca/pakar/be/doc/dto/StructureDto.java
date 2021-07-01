package id.co.bca.pakar.be.doc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.*;


public class StructureDto {
    @JsonProperty("id")
    protected Long id;
    @NotEmpty(message = "name is required")
    @Size(max = 50, message = "maximum length 50 characters")
    @Pattern(regexp = "[A-Za-z\\s]+$", message = "desc field must contain alpha numeric and space only")
    @JsonProperty("name")
    protected String name;
    @NotEmpty(message = "desc is required")
    @Size(max = 200, message = "maximum length 200 characters")
    @Pattern(regexp = "[A-Za-z\\s]+$", message = "desc field must contain alpha numeric and space only")
    @JsonProperty("desc")
    protected String desc;

    @NotNull(message = "sort is required")
    @Min(value = 1, message = "minimum value is 1")
    @JsonProperty("sort")
    protected Long sort=1L;

    @NotNull(message = "level is required")
    @Min(value = 1, message = "minimum value is 1")
    @JsonProperty("level")
    protected Long level=1L;

    @NotNull(message = "parent param must exist in request")
    @Min(value = 0, message = "minimum value is 0")
    @JsonProperty("parent")
    protected Long parent = 0L;

    protected String uri;
    protected Boolean edit;
    protected String location;
    protected String location_text;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public Long getSort() {
        return sort;
    }

    public void setSort(Long sort) {
        this.sort = sort;
    }

    public Long getLevel() {
        return level;
    }

    public void setLevel(Long level) {
        this.level = level;
    }

    public Long getParent() {
        return parent;
    }

    public void setParent(Long parent) {
        this.parent = parent;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Boolean getEdit() {
        return edit;
    }

    public void setEdit(Boolean edit) {
        this.edit = edit;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getLocation_text() {
        return location_text;
    }

    public void setLocation_text(String location_text) {
        this.location_text = location_text;
    }

    @Override
    public String toString() {
        return "StructureDto{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", desc='" + desc + '\'' +
                ", sort=" + sort +
                ", level=" + level +
                ", parent=" + parent +
                ", uri='" + uri + '\'' +
                ", edit=" + edit +
                ", location='" + location + '\'' +
                ", location_text='" + location_text + '\'' +
                '}';
    }
}
