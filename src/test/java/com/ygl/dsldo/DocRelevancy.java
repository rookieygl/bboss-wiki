package com.ygl.dsldo;

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

import java.util.HashMap;
import java.util.Map;

/**
 * @USER: rookie_ygl
 * @DATE: 2020/6/6
 * @TIME: 23:42
 * @DESC: open stack
 **/
@SpringBootTest
@RunWith(SpringRunner.class)
public class DocRelevancy {
	private Logger logger = LoggerFactory.getLogger(DocRelevancy.class);//日志

	@Autowired
	private BBossESStarter bbossESStarter;//bboss启动器

	private ClientInterface clientInterface;//bboss dsl工具

	/**
	 * 关闭词频TF
	 */
	@Test
	public void closeTF() {
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
	public void closeNorms() {
		try {
			clientInterface = bbossESStarter.getConfigRestClient("esmapper/doc_relevancy.xml");//bboss读取xml
			/*检查索引是否存在，存在就删除重建*/
			if (clientInterface.existIndice("close_norms")) {
				clientInterface.dropIndice("close_orms");
			}
			clientInterface.createIndiceMapping("close_norms", "closeTF");
			logger.info("创建索引 close_norms 成功");
		} catch (ElasticSearchException e) {
			logger.error("创建索引 close_norms 执行失败", e);
		}
	}

	/**
	 * 创建索引，指定字段为BM25评分算法
	 */
	@Test
	public void bm25Index() {
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
	 * 创建explain测试索引
	 */
	@Test
	public void createExplainIndex() {
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
			logger.error("testExplain 执行失败", e);
		}
	}

	/**
	 * boost 测试字段权重
	 */
	@Test
	public void testBoost() {
		try {
			clientInterface = bbossESStarter.getConfigRestClient("esmapper/doc_relevancy.xml");
			//查询参数
			Map<String, Object> queryParamsMap = new HashMap<>();
			queryParamsMap.put("title", "es");
			queryParamsMap.put("boost", 2);
			queryParamsMap.put("content", "es");
			ESDatas<MetaMap> metaMapESDatas = clientInterface.searchList("explain_index/_search?search_type=dfs_query_then_fetch",
					"testBoost",//DSL模板ID
					queryParamsMap,//查询参数
					MetaMap.class);//文档信息

			//ES返回结果遍历 结果集不能为空，否则会报空指针
			metaMapESDatas.getDatas().forEach(metaMap -> {
				logger.info("\n文档_source:{} \n_explanation:\n{}", metaMap,
						SimpleStringUtil.object2json(metaMap.getExplanation())
				);
			});
		} catch (ElasticSearchException e) {
			logger.error("testBoost 执行失败", e);
		}
	}

	/**
	 * constant_score 指定分数打分测试
	 */
	@Test
	public void testConstantScore() {
		try {
			clientInterface = bbossESStarter.getConfigRestClient("esmapper/doc_relevancy.xml");


			//查询参数
			Map<String, Object> queryParamsMap = new HashMap<>();
			queryParamsMap.put("title", "es");
			queryParamsMap.put("boost", 1.2);

			ESDatas<MetaMap> metaMapESDatas = clientInterface.searchList("explain_index/_search?search_type=dfs_query_then_fetch",
					"testConstantScore",//DSL模板ID
					queryParamsMap,//查询参数
					MetaMap.class);//文档信息

			//ES返回结果遍历 结果集不能为空，否则会报空指针
			metaMapESDatas.getDatas().forEach(metaMap -> {
				logger.info("\n文档_source:{} \n_explanation:\n{}", metaMap,
						SimpleStringUtil.object2json(metaMap.getExplanation())
				);
			});
		} catch (ElasticSearchException e) {
			logger.error("testConstantScore 执行失败", e);
		}
	}

	/**
	 * FunctionScore 函数评分测试
	 */
	@Test
	public void testFunctionScore() {
		try {
			clientInterface = bbossESStarter.getConfigRestClient("esmapper/doc_relevancy.xml");

			//查询参数
			Map<String, Object> queryParamsMap = new HashMap<>();
			queryParamsMap.put("title", "es");
			queryParamsMap.put("weightTitle", "相关度");
			queryParamsMap.put("boost", 5);

			ESDatas<MetaMap> metaMapESDatas = clientInterface.searchList("explain_index/_search?search_type=dfs_query_then_fetch",
					"testFunctionScore",//DSL模板ID
					queryParamsMap,//查询参数
					MetaMap.class);//文档信息

			//ES返回结果遍历
			metaMapESDatas.getDatas().forEach(metaMap -> {
				logger.info("\n文档_source:{} \n_explanation:\n{}", metaMap,
						SimpleStringUtil.object2json(metaMap.getExplanation())
				);
			});
		} catch (ElasticSearchException e) {
			logger.error("testFunctionScore 执行失败", e);
		}
	}

	/**
	 * dis_max 最佳字段得分测试
	 */
	@Test
	public void testDisMax() {
		try {
			clientInterface = bbossESStarter.getConfigRestClient("esmapper/doc_relevancy.xml");
			//查询参数
			Map<String, Object> queryParamsMap = new HashMap<>();
			queryParamsMap.put("content1", "es");
			queryParamsMap.put("content2", "相关度");
			queryParamsMap.put("tie_breaker", 0.5);
			ESDatas<MetaMap> metaMapESDatas = clientInterface.searchList("explain_index/_search?search_type=dfs_query_then_fetch",
					"testDisMax",//DSL模板ID
					queryParamsMap,//查询参数
					MetaMap.class);//文档信息

			//ES返回结果遍历
			metaMapESDatas.getDatas().forEach(metaMap -> {
				logger.info("\n文档_source:{} \n_explanation:\n{}", metaMap,
						SimpleStringUtil.object2json(metaMap.getExplanation())
				);
			});
		} catch (ElasticSearchException e) {
			logger.error("testDisMax 执行失败", e);
		}
	}

	/**
	 * boosting 结果集权重测试
	 */
	@Test
	public void testBoosting() {
		try {
			clientInterface = bbossESStarter.getConfigRestClient("esmapper/doc_relevancy.xml");

			//查询参数
			Map<String, Object> queryParamsMap = new HashMap<>();
			queryParamsMap.put("positive1", "es");
			queryParamsMap.put("positive2", "相关性");
			queryParamsMap.put("negative", "编程");
			queryParamsMap.put("negative_boost", 0.2);
			ESDatas<MetaMap> metaMapESDatas = clientInterface.searchList("explain_index/_search?search_type=dfs_query_then_fetch",
					"testBoosting",//DSL模板ID
					queryParamsMap,//查询参数
					MetaMap.class);//文档信息

			//ES返回结果遍历
			metaMapESDatas.getDatas().forEach(metaMap -> {
				logger.info("\n文档_source:{} \n_explanation:\n{}", metaMap,
						SimpleStringUtil.object2json(metaMap.getExplanation())
				);
			});
		} catch (ElasticSearchException e) {
			logger.error("testBoosting 执行失败", e);
		}
	}

	/**
	 * rescore 结果集重新打分
	 */
	@Test
	public void testRescore() {
		try {
			clientInterface = bbossESStarter.getConfigRestClient("esmapper/doc_relevancy.xml");

			//查询参数
			Map<String, Object> queryParamsMap = new HashMap<>();
			queryParamsMap.put("content", "es的相关度");
			queryParamsMap.put("title", "es");
			queryParamsMap.put("rescore_query", "es的相关度");
			queryParamsMap.put("window_size", 2);
			queryParamsMap.put("query_weight", 0.7);
			queryParamsMap.put("rescore_query_weight", 1.2);
			ESDatas<MetaMap> metaMapESDatas = clientInterface.searchList("explain_index/_search?search_type=dfs_query_then_fetch",
					"testRescore",//DSL模板ID
					queryParamsMap,//查询参数
					MetaMap.class);//文档信息

			//ES返回结果遍历
			metaMapESDatas.getDatas().forEach(metaMap -> {
				logger.info("\n文档_source:{} \n_explanation:\n{}", metaMap,
						SimpleStringUtil.object2json(metaMap.getExplanation())
				);
			});
		} catch (ElasticSearchException e) {
			logger.error("testRescore 执行失败", e);
		}
	}

	/**
	 * 设置BM25的参数
	 */
	@Test
	public void setBM25() {
		try {
			clientInterface = bbossESStarter.getConfigRestClient("esmapper/doc_relevancy.xml");//bboss读取xml
			/*检查索引是否存在，存在就删除重建*/
			if (clientInterface.existIndice("set_bm25_index")) {
				clientInterface.dropIndice("set_bm25_index");
			}
			Map<String, Object> indexParms = new HashMap<>();
			indexParms.put("my_bm25", "my_bm25");
			indexParms.put("k1", 2);
			indexParms.put("b", 0);

			clientInterface.createIndiceMapping("set_bm25_index", "setBM25", indexParms);
			logger.info("创建索引 set_bm25_index 成功");
		} catch (ElasticSearchException e) {
			logger.error("创建索引 set_bm25_index 执行失败", e);
		}
	}

	/**
	 * 重建explain测试索引
	 */
	@Test
	public void rebuildExplainIndex() {
		try {
			clientInterface = bbossESStarter.getConfigRestClient("esmapper/doc_relevancy.xml");//bboss读取xml
			/*检查索引是否存在，存在就删除重建*/
			if (clientInterface.existIndice("explain_index")) {
				clientInterface.dropIndice("explain_index");
			}
			Map<String, Object> indexParms = new HashMap<>();
			indexParms.put("number_of_shards", 10);
			indexParms.put("number_of_replicas", 2);

			clientInterface.createIndiceMapping("explain_index", "rebuildExplainIndex", indexParms);
			logger.info("重建索引 explain_index 成功");
		} catch (ElasticSearchException e) {
			logger.error("重建索引 explain_index 执行失败", e);
		}
	}
}
