package com.bboss.hellword.SpanQuery;

import com.bboss.hellword.FunctionScore.FunctionScoreTest;
import org.frameworkset.elasticsearch.ElasticSearchHelper;
import org.frameworkset.elasticsearch.boot.BBossESStarter;
import org.frameworkset.elasticsearch.client.ClientInterface;
import org.frameworkset.elasticsearch.client.ClientUtil;
import org.frameworkset.elasticsearch.template.ESInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @Author: ydzy-report
 * @Author: ygl
 * @Date: 2020/5/6 14:33
 * @Desc:
 */

@SpringBootTest
@RunWith(SpringRunner.class)
public class SpanQueryTest {
	@Autowired
	//bboss依赖
	private BBossESStarter bbossESStarter;

	//bboss dsl工具
	//@Autowired
	//private ClientInterface clientInterface;

	//索引名称
	private String indiceName = "article";

	//日志
	private Logger logger = LoggerFactory.getLogger(FunctionScoreTest.class);

	/**
	 * 创建student索引
	 */
	@Test
	public void dropAndCreateIndice() {
		ClientInterface clientInterface = bbossESStarter.getConfigRestClient("esmapper/span_query.xml");
		if (clientInterface.existIndice(indiceName)) {
			clientInterface.dropIndice(indiceName);
		}
		clientInterface.createIndiceMapping(indiceName, "createArticleIndice");
	}

	/**
	 * 添加article索引数据
	 */
	@Test
	public void insertRecipesData() {
		//clientInterface = bbossESStarter.getConfigRestClient("esmapper/field_collapsing.xml");
		//ClientInterface restClient = ElasticSearchHelper.getRestClientUtil();
		////导入数据,并且实时刷新，测试需要，实际环境不要带refresh
		//ESInfo esInfo = clientInterface.getESInfo("bulkInsertArticleData");
		//StringBuilder recipedata = new StringBuilder();
		//recipedata.append(esInfo.getTemplate().trim());
		//recipedata.append("\n");
		//restClient.executeHttp(indiceName +"/_bulk?refresh", recipedata.toString(), ClientUtil.HTTP_POST);
		//long recipeCount = clientInterface.countAll(indiceName);
		//System.out.println("recipes当前条数" + recipeCount);
	}
}
