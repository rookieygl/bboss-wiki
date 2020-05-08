package com.bboss.hellword.FunctionScore;

import com.bboss.hellword.po.Item;
import com.bboss.hellword.po.Student;
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
public class FunctionScoreTest {
    @Autowired
    private BBossESStarter bbossESStarter;
    private Logger logger = LoggerFactory.getLogger(FunctionScoreTest.class);

    /**
     * 创建student索引
     */
    @Test
    public void dropAndCreateStudentIndice() {
        ClientInterface clientInterface = ElasticSearchHelper.getConfigRestClientUtil("esmapper/function_score.xml");
        if (clientInterface.existIndice("student")) {
            clientInterface.dropIndice("student");
        }
        clientInterface.createIndiceMapping("student", "createStudentIndice");
    }

    /**
     * 创建items索引
     */
    @Test
    public void dropAndCreateItemsIndice() {
        ClientInterface clientInterface = ElasticSearchHelper.getConfigRestClientUtil("esmapper/function_score.xml");
        if (clientInterface.existIndice("items")) {
            clientInterface.dropIndice("items");
        }
        clientInterface.createIndiceMapping("items", "createItemsIndice");
    }

    /**
     * 导入items数据
     */
    @Test
    public void insertItemsData() {
        ClientInterface clientInterface = bbossESStarter.getConfigRestClient("esmapper/function_score.xml");
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
        //强制refresh，以便能够实时执行后面的检索操作，生产环境去掉"refresh=true"
        String response = clientInterface.addDocuments("items", "item", items, "refresh=true");
        logger.debug(response);

    }

    /**
     * 指定sales字段排序
     */
    @Test
    public void testFieldValueFactor() {
        ClientInterface clientUtil = bbossESStarter.getConfigRestClient("esmapper/function_score.xml");
        Map<String, Object> queryMap = new HashMap<>();
        // 指定商品类目作为过滤器
        queryMap.put("titleName", "雨伞");
        // 指定需要field_value_factor运算的参数
        queryMap.put("valueFactorName", "sales");

        // 设置分页
        queryMap.put("from", 0);
        queryMap.put("size", 10);

        // testFieldValueFactor 就是上文定义的dsl模板名，queryMap 为查询条件，Item为实体类
        ESDatas<Item> esDatast = clientUtil.searchList("items/_search?search_type=dfs_query_then_fetch", "testFieldValueFactor", queryMap, Item.class);
        List<Item> esCrmOrderStudentList = esDatast.getDatas();
        logger.debug(esCrmOrderStudentList.toString());
        System.out.println(esCrmOrderStudentList.toString());
    }

    /**
     * 测试RandomScore
     */
    @Test
    public void testRandomScoreDSL() {
        ClientInterface clientUtil = bbossESStarter.getConfigRestClient("esmapper/function_score.xml");
        Map<String, Object> queryMap = new HashMap<>();
        // 指定进行random_score运算的字段,这里以id为随机给文档评分
        // 如果指定seed种子.seed相等返回值顺序相同，默认为null
        queryMap.put("fieldName", "docId");

        // 设置分页
        queryMap.put("from", 0);
        queryMap.put("size", 10);
        // testRanodmScore就是上文定义的dsl模板名，queryMap 为查询条件，Student为实体类
        ESDatas<Student> esDatast =
                clientUtil.searchList("student/_search?search_type=dfs_query_then_fetch", "testRanodmScore", queryMap, Student.class);
        List<Student> esCrmOrderStudentList = esDatast.getDatas();
        logger.debug(esCrmOrderStudentList.toString());
        System.out.println(esCrmOrderStudentList.toString());
    }

    /**
     * 测试decayfunctions 地理类型
     */
    @Test
    public void testDecayFunctionsByGeoPonit() {
        ClientInterface clientUtil = bbossESStarter.getConfigRestClient("esmapper/function_score.xml");
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

        ESDatas esDatast = clientUtil.searchList("hoses/_search?search_type=dfs_query_then_fetch", "testDecayFunctionsByGeoPonit", queryMap, Object.class);
        List datas = esDatast.getDatas();
        logger.debug(datas.toString());
        System.out.println(datas.toString());
    }

    /**
     * 测试script_score
     */
    @Test
    public void testScriptScore() {
        ClientInterface clientUtil = bbossESStarter.getConfigRestClient("esmapper/function_score.xml");
        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("cityName", "北京");
        queryMap.put("schoolName", "人大附中");
        // 设置分页
        queryMap.put("from", 0);
        queryMap.put("size", 10);

        ESDatas<Student> esDatast = clientUtil.searchList("student/_search?search_type=dfs_query_then_fetch", "testScriptScore", queryMap, Student.class);
        List<Student> esCrmOrderStudentList = esDatast.getDatas();
        logger.debug(esCrmOrderStudentList.toString());
        System.out.println(esCrmOrderStudentList.toString());
    }

    /**
     * 创建 schoolScore 脚本
     * 根据dsl生成对应的脚本文件
     */
    @Test
    public void testCreateSchoolScoreScript() {
        ClientInterface clientUtil = bbossESStarter.getConfigRestClient("esmapper/function_score.xml");
        //创建评分脚本函数testScriptScore
        clientUtil.executeHttp("_scripts/schoolScoreScript", "schoolScoreScript",
                ClientInterface.HTTP_POST);
        //获取刚才创建评分脚本函数testScriptScore
        String schoolScoreScript = clientUtil.executeHttp("_scripts/schoolScoreScript",
                ClientInterface.HTTP_GET);
        System.out.println(schoolScoreScript);
    }

    @Test
    public void testScriptScoreByIncloudScript() {
        ClientInterface clientUtil = bbossESStarter.getConfigRestClient("esmapper/function_score.xml");
        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("cityName", "北京");
        queryMap.put("schoolName", "人大附中");
        // 设置分页
        queryMap.put("from", 0);
        queryMap.put("size", 10);

        ESDatas<Student> esDatas = clientUtil.searchList("student/_search?search_type=dfs_query_then_fetch", "testScriptScoreByIncloudScript", queryMap, Student.class);
        List<Student> students = esDatas.getDatas();
        System.out.println(students);
    }

    /**
     * 测试 餐厅评分 FunctionScore
     */
    @Test
    public void testHellFunctionScore() {
        ClientInterface clientUtil = bbossESStarter.getConfigRestClient("esmapper/function_score.xml");
        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("features", "停车位");
        queryMap.put("valueFactorFieldName", "score");
        queryMap.put("originLocation", "40,116");
        queryMap.put("scale", "10km");
        queryMap.put("docId", 15);
        // 设置分页
        queryMap.put("from", 0);
        queryMap.put("size", 10);

        ESDatas esDatast = clientUtil.searchList("hell/_search?search_type=dfs_query_then_fetch", "testHellFunctionScore", queryMap, Object.class);
        List datas = esDatast.getDatas();
        logger.debug(datas.toString());
        System.out.println(datas.toString());
    }

    /**
     * 创建 新浪微博 脚本
     * 根据dsl生成对应的脚本文件
     */
    @Test
    public void testCreateSinaScript() {
        ClientInterface clientUtil = bbossESStarter.getConfigRestClient("esmapper/function_score.xml");
        //创建评分脚本函数testScriptScore
        clientUtil.executeHttp("_scripts/sinaScript", "sinaScript",
                ClientInterface.HTTP_POST);
        //获取刚才创建评分脚本函数testScriptScore
        String schoolScoreScript = clientUtil.executeHttp("_scripts/sinaScript",
                ClientInterface.HTTP_GET);
        System.out.println(schoolScoreScript);
    }

    /**
     * 测试 新浪微博评分 FunctionScore
     */
    @Test
    public void testSinaFunctionScore() {
        ClientInterface clientUtil = bbossESStarter.getConfigRestClient("esmapper/function_score.xml");
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

        ESDatas esDatast = clientUtil.searchList("xinlang/_search?search_type=dfs_query_then_fetch", "testSinaFunctionScore", queryMap, Object.class);
        List datas = esDatast.getDatas();
        logger.debug(datas.toString());
        System.out.println(datas.toString());
    }
}
