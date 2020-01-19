package com.bboss.hellword.po;

import com.frameworkset.orm.annotation.ESMetaInnerHits;
import org.frameworkset.elasticsearch.entity.InnerSearchHits;

import java.util.Map;

public class RecipesPo {
    private String name;
    private Integer rating;
    private String type;

    @ESMetaInnerHits//文档对应的innerHits信息
    private Map<String, Map<String, InnerSearchHits>> innerHitsRecipesPo;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Map<String, InnerSearchHits>> getInnerHitsRecipesPo() {
        return innerHitsRecipesPo;
    }

    public void setInnerHitsRecipesPo(Map<String, Map<String, InnerSearchHits>> innerHitsRecipesPo) {
        this.innerHitsRecipesPo = innerHitsRecipesPo;
    }

    @Override
    public String toString() {
        return "RecipesPo{" +
                "name='" + name + '\'' +
                ", rating=" + rating +
                ", type='" + type + '\'' +
                ", innerHitsRecipesPo=" + innerHitsRecipesPo +
                '}';
    }
}
