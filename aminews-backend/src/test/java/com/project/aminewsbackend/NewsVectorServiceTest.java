package com.project.aminewsbackend;


import com.project.aminewsbackend.entity.NewsVector;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;

import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest
public class NewsVectorServiceTest {

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Test
    public void testFindVectorsWithMissingFields() {
        // 构建 must_not exists 查询
        Criteria titleVectorMissing = new Criteria("titleVector").exists();
        Criteria contentVectorMissing = new Criteria("contentVector").exists();
        Criteria criteria = new Criteria().or(titleVectorMissing).or(contentVectorMissing);

        CriteriaQuery query = new CriteriaQuery(criteria);

        SearchHits<NewsVector> searchHits = elasticsearchOperations.search(query, NewsVector.class);

        List<NewsVector> result = searchHits.getSearchHits().stream()
                .map(hit -> hit.getContent())
                .collect(Collectors.toList());

        System.out.println("缺失向量字段的文档数量: " + result.size());
        result.forEach(nv -> System.out.println("itemId: " + nv.getItemId()));
    }


}