package com.bboss.hellword.po;

public class Item {
    private Long docId;
    private String title;
    private String name;
    private Long sales;

    public Long getDocId() {
        return docId;
    }

    public void setDocId(Long docId) {
        this.docId = docId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getSales() {
        return sales;
    }

    public void setSales(Long sales) {
        this.sales = sales;
    }

    @Override
    public String toString() {
        return "Item{" +
                "docId=" + docId +
                ", title='" + title + '\'' +
                ", name='" + name + '\'' +
                ", sales=" + sales +
                '}';
    }
}
