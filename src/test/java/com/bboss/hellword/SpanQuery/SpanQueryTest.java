package com.bboss.hellword.SpanQuery;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.bboss.hellword.FunctionScore.FunctionScoreTest;
import org.apache.commons.lang.StringEscapeUtils;
import org.frameworkset.elasticsearch.ElasticSearchException;
import org.frameworkset.elasticsearch.ElasticSearchHelper;
import org.frameworkset.elasticsearch.boot.BBossESStarter;
import org.frameworkset.elasticsearch.client.ClientInterface;
import org.frameworkset.elasticsearch.client.ClientUtil;
import org.frameworkset.elasticsearch.entity.MapRestResponse;
import org.frameworkset.elasticsearch.template.ESInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: ydzy-report
 * @Author: ygl
 * @Date: 2020/5/6 14:33
 * @Desc:
 */

@SpringBootTest
@RunWith(SpringRunner.class)
public class SpanQueryTest {

	private Logger logger = LoggerFactory.getLogger(FunctionScoreTest.class);//日志

	@Autowired
	private BBossESStarter bbossESStarter;//bboss启动器

	private ClientInterface clientInterface;//bboss dsl工具

	private String indiceName = "article";//索引名称


	/**
	 * 创建student索引
	 */
	@Test
	public void dropAndCreateIndice() {
		try {
			clientInterface = bbossESStarter.getConfigRestClient("esmapper/span_query.xml");//bboss读取xml
			if (clientInterface.existIndice(indiceName)) {
				clientInterface.dropIndice(indiceName);
			}
			clientInterface.createIndiceMapping(indiceName, "createArticleIndice");
			logger.info("create indice" + indiceName + "is done");
		} catch (ElasticSearchException e) {
			logger.error("create indice" + indiceName + "is faild" + e);
		}
	}

	/**
	 * 添加article索引数据
	 */
	@Test
	public void insertIndiceData() {
		try {
			clientInterface = bbossESStarter.getConfigRestClient("esmapper/span_query.xml");//bboss读取xml
			ClientInterface restClient = ElasticSearchHelper.getRestClientUtil();//插入数据用RestClient
			ESInfo esInfo = clientInterface.getESInfo("bulkInsertArticleData");//获取插入数据
			StringBuilder recipedata = new StringBuilder();
			recipedata.append(esInfo.getTemplate().trim());
			recipedata.append("\n");
			restClient.executeHttp(indiceName + "/_bulk?refresh", recipedata.toString(), ClientUtil.HTTP_POST);
		} catch (ElasticSearchException e) {
			logger.error(indiceName + "插入数据失败，请检查错误日志");
		}
		long recipeCount = clientInterface.countAll(indiceName);
		logger.info(indiceName + "当前条数" + recipeCount);
	}

	/**
	 * 添加article索引数据
	 */
	@Test
	public void testSpanTermQuery() {
		try {
			clientInterface = bbossESStarter.getConfigRestClient("esmapper/span_query.xml");

			Map<String,String> queryParams = new HashMap<>(5);
			queryParams.put("spanTermValue","red");
			String queryResult = clientInterface.executeRequest(indiceName + "/_search?search_type=dfs_query_then_fetch", "testSpanTermQuery",queryParams);
			String resultJson = JSON.toJSONString(JSON.parseObject(queryResult), SerializerFeature.PrettyFormat, SerializerFeature.WriteMapNullValue, SerializerFeature.WriteDateUseDateFormat);
			logger.info("testSpanTermQuery查询结果： ");
			logger.info(resultJson);
		} catch (ElasticSearchException e) {
			logger.error("testSpanTermQuery 执行失败" + e);
		}
	}
}
