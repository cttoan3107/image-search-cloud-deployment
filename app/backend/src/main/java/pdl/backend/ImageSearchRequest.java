/*
  Global role of this class:
  - Represents the search request sent from frontend to backend
  - Contains all filtering criteria (name, format, size, keywords, reaction)
*/

package pdl.backend;

import java.util.List;

public class ImageSearchRequest {
    private String name;
    private String format;
    private Integer width;
    private Integer height;
    private List<String> keywords;
    private String reaction;

    public ImageSearchRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }
    
    public String getReaction() {
        return reaction;
    }

    public void setReaction(String reaction) {
        this.reaction = reaction;
    }
}