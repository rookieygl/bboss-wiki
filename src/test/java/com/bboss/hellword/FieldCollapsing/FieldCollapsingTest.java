package com.bboss.hellword.FieldCollapsing;

import com.bboss.hellword.FunctionScore.FunctionScoreTest;
import com.bboss.hellword.po.RecipesPo;
import org.frameworkset.elasticsearch.ElasticSearchException;
import org.frameworkset.elasticsearch.ElasticSearchHelper;
import org.frameworkset.elasticsearch.boot.BBossESStarter;
import org.frameworkset.elasticsearch.client.*;
import org.frameworkset.elasticsearch.entity.ESDatas;
import org.frameworkset.elasticsearch.entity.MapRestResponse;
import org.frameworkset.elasticsearch.serial.ESInnerHitSerialThreadLocal;
import org.frameworkset.elasticsearch.serial.ESTypeReference;
import org.frameworkset.elasticsearch.template.ESInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
@RunWith(SpringRunner.class)
public class FieldCollapsingTest {

	private Logger logger = LoggerFactory.getLogger(FunctionScoreTest.class);//日志

	@Autowired
	private BBossESStarter bbossESStarter;//bboss依赖

	private ClientInterface clientInterface;//bboss dsl工具

	/**
	 * 创建recipes索引
	 */
	@Test
	public void dropAndRecipesIndice() {
		try {
			clientInterface = bbossESStarter.getConfigRestClient("esmapper/field_collapsing.xml");
			/*检查索引是否存在，存在就删除重建*/
			if (clientInterface.existIndice("recipes")) {
				logger.info("recipes" + "已存在，删除索引");
				clientInterface.dropIndice("recipes");
			}
			clientInterface.createIndiceMapping("recipes", "createRecipesIndice");
			logger.info("创建索引 recipes 成功");
		} catch (ElasticSearchException e) {
			logger.error("创建索引 article 执行失败", e);
		}
	}

	/**
	 * 添加recipes索引数据
	 */
	@Test
	public void insertRecipesData() {
        try {
            clientInterface = bbossESStarter.getConfigRestClient("esmapper/field_collapsing.xml");
            ClientInterface restClient = ElasticSearchHelper.getRestClientUtil();
            ESInfo esInfo = clientInterface.getESInfo("bulkImportRecipesData");
            StringBuilder recipedata = new StringBuilder();
            recipedata.append(esInfo.getTemplate().trim())
                        .append("\n");
            //插入数据
            restClient.executeHttp("recipes/_bulk?refresh", recipedata.toString(), ClientUtil.HTTP_POST);

            //统计当前索引数据
            long recipeCount = clientInterface.countAll("recipes");
            logger.info("article 当前条数：{}", recipeCount);
        } catch (ElasticSearchException e) {
            e.printStackTrace();
        }
    }

	/**
	 * 关键词查询
	 */
	@Test
	public void testQueryRecipesPoByField() {
        try {
            clientInterface = bbossESStarter.getConfigRestClient("esmapper/field_collapsing.xml");
            Map<String, Object> queryMap = new HashMap<>();
            //查询条件
            queryMap.put("recipeName", "鱼");

            //设置分页
            queryMap.put("from", 0);
            queryMap.put("size", 5);

            //testFieldValueFactor 就是上文定义的dsl模板名，queryMap 为查询条件，Item为实体类
            ESDatas<RecipesPo> esDatast = clientInterface.searchList("recipes/_search?search_type=dfs_query_then_fetch", "testQueryByField", queryMap, RecipesPo.class);
            List<RecipesPo> esRecipesPoList = esDatast.getDatas();
        } catch (ElasticSearchException e) {
            logger.error("testQueryByField 执行失败", e);
        }
    }

	/**
	 * 关键词查询,加入字段排序
	 */
	@Test
	public void testSortRecipesPoByField() {
		clientInterface = bbossESStarter.getConfigRestClient("esmapper/field_collapsing.xml");
		Map<String, Object> queryMap = new HashMap<>();
		//查询条件
		queryMap.put("recipeName", "鱼");
		queryMap.put("sortField", "rating");

		//设置分页
		queryMap.put("from", 0);
		queryMap.put("size", 5);

		//testFieldValueFactor 就是上文定义的dsl模板名，queryMap 为查询条件，Item为实体类
		ESDatas<RecipesPo> esDatast = clientInterface.searchList("recipes/_search?search_type=dfs_query_then_fetch", "testSortField", queryMap, RecipesPo.class);
		List<RecipesPo> esRecipesPoList = esDatast.getDatas();
		logger.debug(esRecipesPoList.toString());
		System.out.println(esRecipesPoList.toString());
	}

	/**
	 * 查询所有菜系打分最高的鱼食材菜品，返回结果按照打分排序
	 */
	@Test
	public void testQueryRecipesPoAllType() {
		clientInterface = ElasticSearchHelper.getConfigRestClientUtil("esmapper/field_collapsing.xml");
		Map<String, Object> queryMap = new HashMap<>();
		//查询条件
		queryMap.put("recipeName", "鱼");
		queryMap.put("sortField", "rating");

		//聚合参数
		String typeAggName = "all_type";
		String typeTopAggName = "recipes_top";
		queryMap.put("typeAggName", typeAggName);
		queryMap.put("typeTopAggName", typeTopAggName);
		queryMap.put("topHitsSortField", "rating");
		queryMap.put("topHitsSzie", 2);

		//设置分页
		queryMap.put("from", 0);
		//不能设置size，会返回多余数据
		queryMap.put("size", 0);

		//通过下面的方法先得到查询的json报文，然后再通过MapRestResponse查询遍历结果
		MapRestResponse restResponse = clientInterface.search("recipes/_search?search_type=dfs_query_then_fetch", "testQueryAllType", queryMap);

		//获取聚合桶,一次聚合只要一个桶,从桶中获取聚合信息和元数据
		List<Map<String, Object>> recipesAggs = restResponse.getAggBuckets(typeAggName, new ESTypeReference<List<Map<String, Object>>>() {
		});

		//获取失败数和成功数
		Integer doc_count_error_upper_bound = restResponse.getAggAttribute(typeAggName, "doc_count_error_upper_bound", Integer.class);
		Integer sum_other_doc_count = restResponse.getAggAttribute(typeAggName, "sum_other_doc_count", Integer.class);
		System.out.println("doc_count_error_upper_bound:" + doc_count_error_upper_bound);
		System.out.println("sum_other_doc_count:" + sum_other_doc_count);

		//取出元数据
		recipesAggs.forEach(typeAggBucketsMap -> {
			//菜系名
			String recipesAggName = (String) typeAggBucketsMap.get("key");
			System.out.println("菜系名recipesAggName: " + recipesAggName);
			//菜系总数
			Integer recipesAggTotalSize = (Integer) typeAggBucketsMap.get("doc_count");
			//System.out.println("recipesAggTotalSize: " + recipesAggTotalSize);
			//解析json 获取菜品
			Map<String, ?> recipesTypeAggBucketsMap = (Map<String, ?>) typeAggBucketsMap.get(typeTopAggName);
			Map<String, ?> recipesRatedHitsMap = (Map<String, ?>) recipesTypeAggBucketsMap.get("hits");
			List<Map<String, ?>> recipesTophitsList = (List<Map<String, ?>>) recipesRatedHitsMap.get("hits");
			recipesTophitsList.forEach(recipePoMap -> {
				Map<String, Object> recipeMap = (Map<String, Object>) recipePoMap.get("_source");
				RecipesPo recipesPo = transMap2Bean2(recipeMap, RecipesPo.class);
				System.out.println(recipesPo.toString());
			});
		});
	}

	/**
	 * map转化为Bean
	 *
	 * @param beanMap
	 * @param clz
	 * @param <T>
	 * @return
	 */
	public static <T> T transMap2Bean2(Map<String, Object> beanMap, Class<T> clz) {
		//创建JavaBean对象
		//获取指定类的BeanInfo对象
		T poFromMap = null;
		BeanInfo beanInfo = null;

		try {
			poFromMap = clz.newInstance();
			beanInfo = Introspector.getBeanInfo(clz, Object.class);
			//获取所有的属性描述器
			PropertyDescriptor[] pds = beanInfo.getPropertyDescriptors();
			for (PropertyDescriptor pd : pds) {
				Object value = beanMap.get(pd.getName());
				Method setter = pd.getWriteMethod();
				setter.invoke(poFromMap, value);
			}
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IntrospectionException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return poFromMap;
	}

	/**
	 * 字段折叠
	 */
	@Test
	public void testFieldCollapsing() {
		clientInterface = bbossESStarter.getConfigRestClient("esmapper/field_collapsing.xml");
		Map<String, Object> queryMap = new HashMap<>();
		//查询条件
		queryMap.put("recipeName", "鱼");

		//字段折叠(field_collapsing)参数
		queryMap.put("collapseField", "type");
		queryMap.put("sortField", "rating");
		//设置分页
		queryMap.put("from", 0);
		queryMap.put("size", 10);

		//testFieldValueFactor 就是上文定义的dsl模板名，queryMap 为查询条件，Item为实体类
		ESDatas<RecipesPo> esDatast = clientInterface.searchList("recipes/_search?search_type=dfs_query_then_fetch", "testFieldCollapsing", queryMap, RecipesPo.class);
		List<RecipesPo> esRecipesPoList = esDatast.getDatas();
		logger.debug(esRecipesPoList.toString());
		System.out.println(esRecipesPoList.toString());
	}

	/**
	 * 字段折叠 控制组内数据
	 */
	@Test
	public void testFieldCollapsingInnerHits() {
		clientInterface = bbossESStarter.getConfigRestClient("esmapper/field_collapsing.xml");
		Map<String, Object> queryMap = new HashMap<>();
		//查询条件
		queryMap.put("recipeName", "鱼");
		queryMap.put("sortField", "rating");

		//字段折叠(field_collapsing)参数
		queryMap.put("collapseField", "type");
		queryMap.put("innerHitsName", "sort_rated");

		//innerHits参数
		String collapseInnerHitsName = "sort_rated";
		queryMap.put("typeInnerHitsName", collapseInnerHitsName);
		queryMap.put("typeInnerHitsSize", 2);
		queryMap.put("collapseSortField", "rating");

		//设置分页
		queryMap.put("from", 0);
		queryMap.put("size", 10);

		try {
			ESInnerHitSerialThreadLocal.setESInnerTypeReferences(RecipesPo.class);
			ESDatas<RecipesPo> esDatast = clientInterface.searchList("recipes/_search?search_type=dfs_query_then_fetch", "testFieldCollapsingInnerHits", queryMap, RecipesPo.class);
			List<RecipesPo> recipesPoList = esDatast.getDatas();
			recipesPoList.forEach(recipesPo -> {
				List innerHitsRecipesPoList = ResultUtil.getInnerHits(recipesPo.getInnerHitsRecipesPo(), collapseInnerHitsName);
				if (innerHitsRecipesPoList != null && innerHitsRecipesPoList.size() > 0) {
					innerHitsRecipesPoList.forEach(innerHitsRecipesPo -> {
						System.out.println(innerHitsRecipesPo.toString());
					});
				}
			});
		} finally {
			//清除缓存
			ESInnerHitSerialThreadLocal.clean();
		}

	}
}
