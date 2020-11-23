package com.example.mobileplayer.entity;

import java.util.List;

public class NewsResult {
    private String flag;
    private Integer total;
    private List<NewsItem> list;

    public String getFlag() {
        return flag;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public List<NewsItem> getList() {
        return list;
    }

    public void setList(List<NewsItem> list) {
        this.list = list;
    }
}
