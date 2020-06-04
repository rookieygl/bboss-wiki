package com.bboss.hellword.SpanQuery;

import com.bboss.hellword.FunctionScore.FunctionScoreTest;
import org.frameworkset.elasticsearch.ElasticSearchException;
import org.frameworkset.elasticsearch.ElasticSearchHelper;
import org.frameworkset.elasticsearch.boot.BBossESStarter;
import org.frameworkset.elasticsearch.client.ClientInterface;
import org.frameworkset.elasticsearch.client.ClientUtil;
import org.frameworkset.elasticsearch.entity.ESDatas;
import org.frameworkset.elasticsearch.entity.MapRestResponse;
import org.frameworkset.elasticsearch.entity.MetaMap;
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

	/**
	 * 创建article索引
	 */
	@Test
	public void dropAndCreateArticleIndice() {
		try {
			clientInterface = bbossESStarter.getConfigRestClient("esmapper/span_query.xml");//bboss读取xml
			/*检查索引是否存在，存在就删除重建*/
			if (clientInterface.existIndice("article")) {
				clientInterface.dropIndice("article");
			}
			clientInterface.createIndiceMapping("article", "createArticleIndice");
			logger.info("创建索引 article 成功");
		} catch (ElasticSearchException e) {
			logger.error("创建索引 article 执行失败", e);
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
			recipedata.append(esInfo.getTemplate().trim())
					.append("\n");//换行符不能省
			restClient.executeHttp("article/_bulk?refresh", recipedata.toString(), ClientUtil.HTTP_POST);
		} catch (ElasticSearchException e) {
			logger.error("article 插入数据失败", e);
		}
		long recipeCount = clientInterface.countAll("article");
		logger.info("article 当前条数：{}", recipeCount);
	}

	/**
	 * 测试SpanTermQuery
	 */
	@Test
	public void testSpanTermQuery() {
		try {
			clientInterface = bbossESStarter.getConfigRestClient("esmapper/span_query.xml");
			//封装请求参数
			Map<String, String> queryParams = new HashMap<>(5);
			queryParams.put("spanTermValue", "red");
			//Bboss执行查询DSL
			ESDatas<MetaMap> metaMapESDatas = clientInterface.searchList("article/_search?search_type=dfs_query_then_fetch",
					"testSpanTermQuery",//DSL模板ID
					queryParams,//查询参数
					MetaMap.class);//文档信息

			//ES返回结果遍历
			metaMapESDatas.getDatas().forEach(metaMap -> {
				logger.info("\n文档_source:{}", metaMap);
			});
		} catch (ElasticSearchException e) {
			logger.error("testSpanTermQuery 执行失败", e);
		}
	}

	/**
	 * 测试SpanNearQuery
	 */
	@Test
	public void testSpanNearQuery() {
		try {
			clientInterface = bbossESStarter.getConfigRestClient("esmapper/span_query.xml");
			//封装请求参数
			Map<String, String> queryParams = new HashMap<>(5);
			queryParams.put("spanTermValue1", "quick");
			queryParams.put("spanTermValue2", "brown");
			queryParams.put("slop", "0");

			//Bboss执行查询DSL
			ESDatas<MetaMap> metaMapESDatas = clientInterface.searchList("article/_search?search_type=dfs_query_then_fetch",
					"testS_panNearQuery",//DSL模板ID
					queryParams,//查询参数
					MetaMap.class);//文档信息
			//ES返回结果遍历
			metaMapESDatas.getDatas().forEach(metaMap -> {
				logger.info("\n文档_source:{}" + metaMap);
			});
		} catch (ElasticSearchException e) {
			logger.error("testSpanTermQuery 执行失败", e);
		}
	}

	/**
	 * 测试SpanNotQuery
	 */
	@Test
	public void testSpanNotQuery() {
		try {
			clientInterface = bbossESStarter.getConfigRestClient("esmapper/span_query.xml");
			//封装请求参数
			Map<String, String> queryParams = new HashMap<>(5);
			queryParams.put("spanTermValue1", "quick");
			queryParams.put("spanTermValue2", "fox");
			queryParams.put("slop", "1");
			queryParams.put("spanNotValue", "red");

			//Bboss执行查询DSL
			ESDatas<MetaMap> metaMapESDatas = clientInterface.searchList("article/_search?search_type=dfs_query_then_fetch",
					"testSpanNotQuery",//DSL模板ID
					queryParams,//查询参数
					MetaMap.class);//文档信息
			//ES返回结果遍历
			metaMapESDatas.getDatas().forEach(metaMap -> {
				logger.info("\n文档_source:{}", metaMap);
			});
		} catch (ElasticSearchException e) {
			logger.error("testSpanNotQuery 执行失败", e);
		}
	}

	/**
	 * 创建simple1索引
	 */
	@Test
	public void dropAndCreateSample1Indice() {
		try {
			clientInterface = bbossESStarter.getConfigRestClient("esmapper/sentence_paragrah.xml");//bboss读取xml
			/*检查索引是否存在，存在就删除重建*/
			if (clientInterface.existIndice("sample1")) {
				clientInterface.dropIndice("sample1");
			}

			/*传参，创建索引*/
			clientInterface.createIndiceMapping("sample1", "createSample1Indice");
			logger.info("创建索引 sample1 成功");
		} catch (ElasticSearchException e) {
			logger.error("创建索引 sample1 执行失败", e);
		}
	}

	/**
	 * 添加simp1数据
	 */
	@Test
	public void insertSimple1IndiceData() {
		try {
			clientInterface = bbossESStarter.getConfigRestClient("esmapper/sentence_paragrah.xml");//bboss读取xml
			ClientInterface restClient = ElasticSearchHelper.getRestClientUtil();//插入数据用RestClient
			ESInfo esInfo = clientInterface.getESInfo("bulkSample1Data");//获取插入数据
			StringBuilder recipedata = new StringBuilder();
			recipedata.append(esInfo.getTemplate().trim());
			recipedata.append("\n");
			restClient.executeHttp("sample1" + "/_bulk?refresh", String.valueOf(recipedata), ClientUtil.HTTP_POST);
		} catch (ElasticSearchException e) {
			logger.error("sample1 插入数据失败", e);
		}
		long recipeCount = clientInterface.countAll("sample1");
		logger.info("sample1 当前条数:{}", recipeCount);
	}


	/**
	 * 测试html分词
	 */
	@Test
	public void testHtmlAnalyze() {
		try {
			clientInterface = bbossESStarter.getConfigRestClient("esmapper/sentence_paragrah.xml");//bboss读取xml
			String analyzeResult = clientInterface.executeHttp("sample1" + "/_analyze", "testHtmlAnalyze", ClientUtil.HTTP_POST);
			//分词结果
			logger.info("分词结果:{}", analyzeResult);
		} catch (ElasticSearchException e) {
			logger.error("testHtmlAnalyze 执行失败", e);
		}
	}

	/**
	 * 测试html同段搜索
	 */
	@Test
	public void testHtmlParagraphQuery() {
		try {
			clientInterface = bbossESStarter.getConfigRestClient("esmapper/sentence_paragrah.xml");//bboss读取xml
			//封装请求参数
			Map<String, String> queryParams = new HashMap<>(5);
			queryParams.put("spanTermValue1", "java");
			queryParams.put("spanTermValue2", "javascript");
			queryParams.put("slop", "3");
			queryParams.put("queryType", "paragraph");

			//Bboss执行查询DSL
			ESDatas<MetaMap> metaMapESDatas = clientInterface.searchList("sample1" + "/_search?search_type=dfs_query_then_fetch",
					"testParagraphQuery",//DSL模板ID
					queryParams,//查询参数
					MetaMap.class);//文档信息
			//ES返回结果遍历
			metaMapESDatas.getDatas().forEach(metaMap -> {
				logger.info("\n文档_source:{}", metaMap);
			});
		} catch (ElasticSearchException e) {
			logger.error("testParagraphQuery 执行失败",e);
		}
	}

	/**
	 * 创建simple2索引
	 */
	@Test
	public void dropAndCreateSample2Indice() {
		try {
			clientInterface = bbossESStarter.getConfigRestClient("esmapper/sentence_paragrah.xml");//bboss读取xml
			/*检查索引是否存在，存在就删除重建*/
			if (clientInterface.existIndice("sample2")) {
				clientInterface.dropIndice("sample2");
			}

			/*传参，创建索引*/
			clientInterface.createIndiceMapping("sample2", "createSample2Indice");
			logger.info("创建索引 sample2 成功");
		} catch (ElasticSearchException e) {
			logger.error("创建索引 sample2 执行失败", e);
		}
	}

	/**
	 * 添加simple2数据
	 */
	@Test
	public void insertSimple2IndiceData() {
		try {
			clientInterface = bbossESStarter.getConfigRestClient("esmapper/sentence_paragrah.xml");//bboss读取xml
			ClientInterface restClient = ElasticSearchHelper.getRestClientUtil();//插入数据用RestClient
			ESInfo esInfo = clientInterface.getESInfo("bulkSample2Data");//获取插入数据
			StringBuilder recipedata = new StringBuilder();
			recipedata.append(esInfo.getTemplate().trim())
					.append("\n");
			restClient.executeHttp("sample2" + "/_bulk?refresh", recipedata.toString(), ClientUtil.HTTP_POST);
		} catch (ElasticSearchException e) {
			logger.error("sample2 插入数据失败",e);
		}
		long recipeCount = clientInterface.countAll("sample2");
		logger.info("sample2 当前条数：{}", recipeCount);
	}

	/**
	 * 测试分词
	 */
	@Test
	public void testTextAnalyze() {
		try {
			clientInterface = bbossESStarter.getConfigRestClient("esmapper/sentence_paragrah.xml");//bboss读取xml
			String analyzeResult = clientInterface.executeHttp("sample1" + "/_analyze", "testTextAnalyze", ClientUtil.HTTP_POST);
			//分词结果
			logger.info("分词结果:{}", analyzeResult);
		} catch (ElasticSearchException e) {
			logger.error("testTextAnalyze 执行失败", e);
		}
	}

	/**
	 * 测试text同段搜索
	 */
	@Test
	public void testTextParagraphQuery() {
		try {
			clientInterface = bbossESStarter.getConfigRestClient("esmapper/sentence_paragrah.xml");
			//封装请求参数
			Map<String, String> queryParams = new HashMap<>(5);
			queryParams.put("spanTermValue1", "java");
			queryParams.put("spanTermValue2", "javascript");
			queryParams.put("slop", "3");
			queryParams.put("queryType", "paragraph");

			//Bboss执行查询DSL
			ESDatas<MetaMap> metaMapESDatas = clientInterface.searchList("sample2" + "/_search?search_type=dfs_query_then_fetch",
					"testParagraphQuery",//DSL模板ID
					queryParams,//查询参数
					MetaMap.class);//文档信息
			//ES返回结果遍历
			metaMapESDatas.getDatas().forEach(metaMap -> {
				logger.info("\n文档_source:{}", metaMap);
			});
		} catch (ElasticSearchException e) {
			logger.error("testParagraphQuery 执行失败", e);
		}
	}
}
