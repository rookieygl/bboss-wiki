package com.bboss.hellword.FunctionScore;

import com.bboss.hellword.po.Item;
import com.bboss.hellword.po.Student;
import org.frameworkset.elasticsearch.ElasticSearchException;
import org.frameworkset.elasticsearch.ElasticSearchHelper;
import org.frameworkset.elasticsearch.boot.BBossESStarter;
import org.frameworkset.elasticsearch.client.ClientInterface;
import org.frameworkset.elasticsearch.entity.ESDatas;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.text.SimpleDateFormat;
import java.util.*;

@SpringBootTest
@RunWith(SpringRunner.class)
public class FunctionScore {

    private Logger logger = LoggerFactory.getLogger(FunctionScore.class);//日志

    @Autowired
    private BBossESStarter bbossESStarter;//bboss启动器

    private ClientInterface clientInterface;//bboss dsl工具

    /**
     * 创建student索引
     */
    @Test
    public void dropAndCreateStudentIndice() {
        try {
            clientInterface = ElasticSearchHelper.getConfigRestClientUtil("esmapper/function_score.xml");
            /*检查索引是否存在，存在就删除重建*/
            if (clientInterface.existIndice("student")) {
                clientInterface.dropIndice("student");
            }
            //创建索引
            clientInterface.createIndiceMapping("student", "createStudentIndice");
        } catch (ElasticSearchException e) {
            logger.error("创建索引 student 执行失败", e);
        }
    }

    /**
     * 创建items索引
     */
    @Test
    public void dropAndCreateItemsIndice() {
        try {
            clientInterface = ElasticSearchHelper.getConfigRestClientUtil("esmapper/function_score.xml");
            if (clientInterface.existIndice("items")) {
                clientInterface.dropIndice("items");
            }
            //创建索引
            clientInterface.createIndiceMapping("items", "createItemsIndice");
            logger.info("创建索引 items 成功");
        } catch (ElasticSearchException e) {
            logger.error("创建索引 items 执行失败", e);
        }
    }

    /**
     * 导入items数据
     */
    @Test
    public void insertItemsData() {
        clientInterface = bbossESStarter.getConfigRestClient("esmapper/function_score.xml");
        List<Item> items = new ArrayList<>();
        Item item1 = new Item();
        Item item2 = new Item();
        Item item3 = new Item();
        Item item4 = new Item();

        item1.setDocId(1L);
        item1.setTitle("雨伞");
        item1.setName("天堂伞");
        item1.setSales(500L);

        item2.setDocId(2L);
        item2.setTitle("雨伞");
        item2.setName("宜家");
        item2.setSales(1000L);

        item3.setDocId(3L);
        item3.setTitle("巧克力");
        item3.setName("德芙");
        item3.setSales(100000L);

        item4.setDocId(4L);
        item4.setTitle("奶糖");
        item4.setName("大白兔");
        item4.setSales(1000000000L);


        items.add(item1);
        items.add(item2);
        items.add(item3);
        items.add(item4);
        try {
            //强制refresh，以便能够实时执行后面的检索操作，生产环境去掉"refresh=true"
            clientInterface.addDocuments("items", "item", items, "refresh=true");

            //统计当前索引数据
            long recipeCount = clientInterface.countAll("article");
            logger.info("items 当前条数：{}", recipeCount);
        } catch (ElasticSearchException e) {
            logger.error("items 插入数据失败", e);
        }
    }

    /**
     * 指定sales字段排序
     */
    @Test
    public void testFieldValueFactor() {
        try {
            clientInterface = bbossESStarter.getConfigRestClient("esmapper/function_score.xml");
            Map<String, Object> queryMap = new HashMap<>();
            // 指定商品类目作为过滤器
            queryMap.put("titleName", "雨伞");
            // 指定需要field_value_factor运算的参数
            queryMap.put("valueFactorName", "sales");

            // 设置分页
            queryMap.put("from", 0);
            queryMap.put("size", 10);
            //bboss执行查询DSL
            ESDatas<Item> esDatast = clientInterface.searchList("items/_search?search_type=dfs_query_then_fetch",
                    "testFieldValueFactor", //DSL id
                    queryMap,//查询条件
                    Item.class);
            List<Item> esCrmOrderStudentList = esDatast.getDatas();
            logger.info(esCrmOrderStudentList.toString());
        } catch (ElasticSearchException e) {
            logger.error("testFieldValueFactor 执行失败", e);
        }
    }

    /**
     * 测试RandomScore
     */
    @Test
    public void testRandomScoreDSL() {
        try {
            clientInterface = bbossESStarter.getConfigRestClient("esmapper/function_score.xml");
            Map<String, Object> queryMap = new HashMap<>();
            // 指定进行random_score运算的字段,这里以id为随机给文档评分

            // 如果指定seed种子.seed相等返回值顺序相同，默认为null
            queryMap.put("fieldName", "docId");

            // 设置分页
            queryMap.put("from", 0);
            queryMap.put("size", 10);

            //bboss执行查询DSL
            ESDatas<Student> esDatast =
                    clientInterface.searchList("items/_search?search_type=dfs_query_then_fetch",
                            "testRanodmScore",
                            queryMap,
                            Student.class);
            List<Student> esCrmOrderStudentList = esDatast.getDatas();
            logger.info(esCrmOrderStudentList.toString());
        } catch (ElasticSearchException e) {
            logger.error("testRanodmScore 执行失败", e);
        }

    }

    /**
     * 测试decayfunctions 地理类型
     */
    @Test
    public void testDecayFunctionsByGeoPonit() {
        try {
            clientInterface = bbossESStarter.getConfigRestClient("esmapper/function_score.xml");
            Map<String, Object> queryMap = new HashMap<>();
            // 设置理想的商品名字
            queryMap.put("titleName", "公寓");
            // origin 原点 设置北京的坐标
            queryMap.put("originLocation", "40,116");
            // offset 理想范围
            queryMap.put("offset", "3km");
            // scale 衰减临界点 注意该临界点为 origin±(offsetr+scale)
            queryMap.put("scale", "10km");
            // 衰减系数 decay 乘以临界处文档的分数
            queryMap.put("decay", 0.33);
            // 设置分页
            queryMap.put("from", 0);
            queryMap.put("size", 10);

            //bboss执行查询DSL
            ESDatas esDatast = clientInterface.searchList("hoses/_search?search_type=dfs_query_then_fetch",
                    "testDecayFunctionsByGeoPonit",
                    queryMap,
                    Object.class);
            logger.info(esDatast.getDatas().toString());
        } catch (ElasticSearchException e) {
            logger.error("testDecayFunctionsByGeoPonit 执行失败", e);
        }
    }

    /**
     * 测试script_score
     */
    @Test
    public void testScriptScore() {
        try {
            clientInterface = bbossESStarter.getConfigRestClient("esmapper/function_score.xml");
            Map<String, Object> queryMap = new HashMap<>();
            queryMap.put("cityName", "北京");
            queryMap.put("schoolName", "人大附中");
            // 设置分页
            queryMap.put("from", 0);
            queryMap.put("size", 10);

            ESDatas<Student> esDatast = clientInterface.searchList("student/_search?search_type=dfs_query_then_fetch",
                    "testScriptScore",
                    queryMap,
                    Student.class);
            List<Student> esCrmOrderStudentList = esDatast.getDatas();
            logger.info(esCrmOrderStudentList.toString());
        } catch (ElasticSearchException e) {
            logger.error("testScriptScore 执行失败", e);
        }
    }

    /**
     * 创建 schoolScore 脚本
     * 根据dsl生成对应的脚本文件
     */
    @Test
    public void testCreateSchoolScoreScript() {
        try {
            clientInterface = bbossESStarter.getConfigRestClient("esmapper/function_score.xml");
            //创建评分脚本函数testScriptScore
            clientInterface.executeHttp("_scripts/schoolScoreScript",
                    "schoolScoreScript",
                    ClientInterface.HTTP_POST);
            //获取刚才创建评分脚本函数testScriptScore
            String schoolScoreScript = clientInterface.executeHttp("_scripts/schoolScoreScript",
                    ClientInterface.HTTP_GET);
            logger.info(schoolScoreScript);
        } catch (ElasticSearchException e) {
            logger.error("schoolScoreScript 执行失败", e);
        }
    }

    /**
     * 测试schoolScore
     */
    @Test
    public void testScriptScoreByIncloudScript() {
        try {
            clientInterface = bbossESStarter.getConfigRestClient("esmapper/function_score.xml");
            Map<String, Object> queryMap = new HashMap<>();
            queryMap.put("cityName", "北京");
            queryMap.put("schoolName", "人大附中");
            // 设置分页
            queryMap.put("from", 0);
            queryMap.put("size", 10);

            ESDatas<Student> esDatas = clientInterface.searchList("student/_search?search_type=dfs_query_then_fetch",
                    "testScriptScoreByIncloudScript",
                    queryMap,
                    Student.class);
            List<Student> students = esDatas.getDatas();
            logger.info(students.toString());
        } catch (ElasticSearchException e) {
            logger.error("testScriptScoreByIncloudScript 执行失败", e);
        }
    }

    /**
     * 测试 餐厅评分 FunctionScore
     */
    @Test
    public void testHellFunctionScore() {
        try {
            clientInterface = bbossESStarter.getConfigRestClient("esmapper/function_score.xml");
            Map<String, Object> queryMap = new HashMap<>();
            queryMap.put("features", "停车位");
            queryMap.put("valueFactorFieldName", "score");
            queryMap.put("originLocation", "40,116");
            queryMap.put("scale", "10km");
            queryMap.put("docId", 15);
            // 设置分页
            queryMap.put("from", 0);
            queryMap.put("size", 10);

            ESDatas esDatast = clientInterface.searchList("hell/_search?search_type=dfs_query_then_fetch",
                    "testHellFunctionScore",
                    queryMap,
                    Object.class);
            logger.info(esDatast.getDatas().toString());
        } catch (ElasticSearchException e) {
            logger.error("testHellFunctionScore 执行失败", e);
        }
    }

    /**
     * 创建 新浪微博 脚本
     * 根据dsl生成对应的脚本文件
     */
    @Test
    public void testCreateSinaScript() {
        try {
            clientInterface = bbossESStarter.getConfigRestClient("esmapper/function_score.xml");
            //创建评分脚本函数testScriptScore
            clientInterface.executeHttp("_scripts/sinaScript",
                    "sinaScript",
                    ClientInterface.HTTP_POST);
            //获取刚才创建评分脚本函数testScriptScore
            String schoolScoreScript = clientInterface.executeHttp("_scripts/sinaScript",
                    ClientInterface.HTTP_GET);
            logger.info(schoolScoreScript);
        } catch (ElasticSearchException e) {
            logger.error("sinaScript 执行失败", e);
        }
    }

    /**
     * 测试 新浪微博评分 FunctionScore
     */
    @Test
    public void testSinaFunctionScore() {
        try {
            clientInterface = bbossESStarter.getConfigRestClient("esmapper/function_score.xml");
            Map<String, Object> queryMap = new HashMap<>();
            queryMap.put("content", "刘亦菲");

            Date date = new Date(); //获取当前的系统时间。
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd"); //使用了默认的格式创建了一个日期格式化对象。
            String time = dateFormat.format(date); //可以把日期转换转指定格式的字符串

            queryMap.put("createDate", time);
            queryMap.put("valueFactorFieldName", "like_count");
            queryMap.put("time", new Date().getTime());
            // 设置分页
            queryMap.put("from", 0);
            queryMap.put("size", 10);

            ESDatas esDatast = clientInterface.searchList("xinlang/_search?search_type=dfs_query_then_fetch",
                    "testSinaFunctionScore",
                    queryMap,
                    Object.class);
            logger.info(esDatast.getDatas().toString());
        } catch (ElasticSearchException e) {
            logger.error("testSinaFunctionScore 执行失败", e);
        }
    }
}
