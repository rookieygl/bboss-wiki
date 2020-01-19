package com.bboss.hellword.po;

import java.util.Date;

public class Student {
    private Long docId;
    private String name;
    private String phone;
    private String city;
    private  Long age;
    private Date creat_date;

    public Long getDocId() {
        return docId;
    }

    public void setDocId(Long docId) {
        this.docId = docId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Long getAge() {
        return age;
    }

    public void setAge(Long age) {
        this.age = age;
    }

    public Date getCreat_date() {
        return creat_date;
    }

    public void setCreat_date(Date creat_date) {
        this.creat_date = creat_date;
    }

    @Override
    public String toString() {
        return "Student{" +
                "docId=" + docId +
                ", name='" + name + '\'' +
                ", phone='" + phone + '\'' +
                ", city='" + city + '\'' +
                ", age=" + age +
                ", creat_date=" + creat_date +
                '}';
    }
}
