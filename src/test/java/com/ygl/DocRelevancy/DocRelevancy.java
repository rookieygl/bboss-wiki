package com.ygl.DocRelevancy;

import com.ygl.FunctionScore.FunctionScore;
import com.frameworkset.util.SimpleStringUtil;
import org.frameworkset.elasticsearch.ElasticSearchException;
import org.frameworkset.elasticsearch.ElasticSearchHelper;
import org.frameworkset.elasticsearch.boot.BBossESStarter;
import org.frameworkset.elasticsearch.client.ClientInterface;
import org.frameworkset.elasticsearch.client.ClientUtil;
import org.frameworkset.elasticsearch.entity.ESDatas;
import org.frameworkset.elasticsearch.entity.MetaMap;
import org.frameworkset.elasticsearch.template.ESInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @USER: rookie_ygl
 * @DATE: 2020/6/6
 * @TIME: 23:42
 * @DESC: open stack
 **/
@SpringBootTest
@RunWith(SpringRunner.class)
public class DocRelevancy {
    private Logger logger = LoggerFactory.getLogger(FunctionScore.class);//日志

    @Autowired
    private BBossESStarter bbossESStarter;//bboss启动器

    private ClientInterface clientInterface;//bboss dsl工具

    /**
     * 关闭词频TF
     */
    @Test
    public void closeTF(){
        try {
            clientInterface = bbossESStarter.getConfigRestClient("esmapper/doc_relevancy.xml");//bboss读取xml
            /*检查索引是否存在，存在就删除重建*/
            if (clientInterface.existIndice("close_tf")) {
                clientInterface.dropIndice("close_tf");
            }
            clientInterface.createIndiceMapping("close_tf", "closeTF");
            logger.info("创建索引 close_tf 成功");
        } catch (ElasticSearchException e) {
            logger.error("创建索引 close_tf 执行失败", e);
        }
    }

    /**
     * 关闭字段长度归一值
     */
    @Test
    public void closeNorms(){
        try {
            clientInterface = bbossESStarter.getConfigRestClient("esmapper/doc_relevancy.xml");//bboss读取xml
            /*检查索引是否存在，存在就删除重建*/
            if (clientInterface.existIndice("close_orms")) {
                clientInterface.dropIndice("close_orms");
            }
            clientInterface.createIndiceMapping("close_orms", "closeTF");
            logger.info("创建索引 close_orms 成功");
        } catch (ElasticSearchException e) {
            logger.error("创建索引 close_orms 执行失败", e);
        }
    }

    /**
     * 创建索引，指定字段为BM25评分算法
     */
    @Test
    public void bm25Index(){
        try {
            clientInterface = bbossESStarter.getConfigRestClient("esmapper/doc_relevancy.xml");//bboss读取xml
            /*检查索引是否存在，存在就删除重建*/
            if (clientInterface.existIndice("bm25_index")) {
                clientInterface.dropIndice("bm25_index");
            }
            clientInterface.createIndiceMapping("bm25_index", "bm25Index");
            logger.info("创建索引 bm25_index 成功");
        } catch (ElasticSearchException e) {
            logger.error("创建索引 bm25_index 执行失败", e);
        }
    }

    /**
     * 设置BM25的参数
     */
    @Test
    public void setBM25(){
        try {
            clientInterface = bbossESStarter.getConfigRestClient("esmapper/doc_relevancy.xml");//bboss读取xml
            /*检查索引是否存在，存在就删除重建*/
            if (clientInterface.existIndice("bm25_index")) {
                clientInterface.dropIndice("bm25_index");
            }
            clientInterface.createIndiceMapping("bm25_index", "setBM25");
            logger.info("创建索引 bm25_index 成功");
        } catch (ElasticSearchException e) {
            logger.error("创建索引 bm25_index 执行失败", e);
        }
    }

    /**
     * 创建explain测试索引
     */
    @Test
    public void createExplainIndex(){
        try {
            clientInterface = bbossESStarter.getConfigRestClient("esmapper/doc_relevancy.xml");//bboss读取xml
            /*检查索引是否存在，存在就删除重建*/
            if (clientInterface.existIndice("explain_index")) {
                clientInterface.dropIndice("explain_index");
            }
            clientInterface.createIndiceMapping("explain_index", "createExplainIndex");
            logger.info("创建索引 explain_index 成功");
        } catch (ElasticSearchException e) {
            logger.error("创建索引 explain_index 执行失败", e);
        }
    }

    /**
     * 添加explain索引数据
     */
    @Test
    public void blukExplainIndex() {
        try {
            clientInterface = bbossESStarter.getConfigRestClient("esmapper/doc_relevancy.xml");//bboss读取xml
            ClientInterface restClient = ElasticSearchHelper.getRestClientUtil();//插入数据用RestClient
            ESInfo esInfo = clientInterface.getESInfo("blukExplainIndex");//获取插入数据
            StringBuilder recipedata = new StringBuilder();
            recipedata.append(esInfo.getTemplate().trim())
                    .append("\n");//换行符不能省
            //插入数据
            restClient.executeHttp("explain_index/_bulk?refresh", recipedata.toString(), ClientUtil.HTTP_POST);

            //统计当前索引数据
            long recipeCount = clientInterface.countAll("explain_index");
            logger.info("explain_index 当前条数：{}", recipeCount);
        } catch (ElasticSearchException e) {
            logger.error("explain_index 插入数据失败", e);
        }
    }

    /**
     * 测试explain查看ES查询执行计划
     */
    @Test
    public void testExplain() {
        try {
            clientInterface = bbossESStarter.getConfigRestClient("esmapper/doc_relevancy.xml");

            ESDatas<MetaMap> metaMapESDatas = clientInterface.searchList("explain_index/_search?search_type=dfs_query_then_fetch",
                    "testExplain",//DSL模板ID
                    MetaMap.class);//文档信息

            //ES返回结果遍历

            metaMapESDatas.getDatas().forEach(metaMap -> {
                logger.info("\n文档_source:{} \n_explanation:\n{}", metaMap,
                        SimpleStringUtil.object2json(metaMap.getExplanation())
                );
            });
        } catch (ElasticSearchException e) {
            logger.error("testSpanTermQuery 执行失败", e);
        }
    }

    /**
     * 测试Boost权重
     */
    @Test
    public void testBoost() {
        try {
            clientInterface = bbossESStarter.getConfigRestClient("esmapper/doc_relevancy.xml");

            ESDatas<MetaMap> metaMapESDatas = clientInterface.searchList("explain_index/_search?search_type=dfs_query_then_fetch",
                    "testBoost",//DSL模板ID
                    MetaMap.class);//文档信息

            //ES返回结果遍历

            metaMapESDatas.getDatas().forEach(metaMap -> {
                logger.info("\n文档_source:{} \n_explanation:\n{}", metaMap,
                        SimpleStringUtil.object2json(metaMap.getExplanation())
                );
            });
        } catch (ElasticSearchException e) {
            logger.error("testSpanTermQuery 执行失败", e);
        }
    }

    /**
     * 测试Boost权重
     */
    @Test
    public void testConstantScore() {
        try {
            clientInterface = bbossESStarter.getConfigRestClient("esmapper/doc_relevancy.xml");

            ESDatas<MetaMap> metaMapESDatas = clientInterface.searchList("explain_index/_search?search_type=dfs_query_then_fetch",
                    "testConstantScore",//DSL模板ID
                    MetaMap.class);//文档信息

            //ES返回结果遍历

            metaMapESDatas.getDatas().forEach(metaMap -> {
                logger.info("\n文档_source:{} \n_explanation:\n{}", metaMap,
                        SimpleStringUtil.object2json(metaMap.getExplanation())
                );
            });
        } catch (ElasticSearchException e) {
            logger.error("testSpanTermQuery 执行失败", e);
        }
    }

    /**
     * 测试Boost权重
     */
    @Test
    public void testFunctionScore() {
        try {
            clientInterface = bbossESStarter.getConfigRestClient("esmapper/doc_relevancy.xml");

            ESDatas<MetaMap> metaMapESDatas = clientInterface.searchList("explain_index/_search?search_type=dfs_query_then_fetch",
                    "testFunctionScore",//DSL模板ID
                    MetaMap.class);//文档信息

            //ES返回结果遍历

            metaMapESDatas.getDatas().forEach(metaMap -> {
                logger.info("\n文档_source:{} \n_explanation:\n{}", metaMap,
                        SimpleStringUtil.object2json(metaMap.getExplanation())
                );
            });
        } catch (ElasticSearchException e) {
            logger.error("testSpanTermQuery 执行失败", e);
        }
    }
}
