# Elasticsearch：TF-IDF，BM25和相关度的控制

ES 5.0 之前，默认的相关性算分采用的是 TF-IDF，而之后则默认采用 BM25。对于相关度有以下三个问题：

1. 什么是相关性/相关度？Lucene 是如何计算相关度的？
2. TF-IDF 和 BM25 究竟是什么？
3. 相关度控制的方式有哪些？各自都有什么特点？

本文从相关性概念入手，到 TF-IDF 和 BM25 讲解和数学公式学习，再到详细介绍多种常用的相关度控制方式。相信对你一定有用！

# 案例工程

案例源码工程:

https://github.com/rookieygl/bboss-wiki

本案例以Elasticsearch开源java rest client客户端bboss开发：

https://esdoc.bbossgroups.com/#/README

开始之前要先创建DSL的配置文件，位置在案例工程resources/esmapper/doc_relevancy.xml，Git地址：https://github.com/rookieygl/bboss-wiki/blob/master/src/main/resources/esmapper/doc_relevancy.xml。本文涉及到的DSL都会放到该配置文件。

# 1.文档相关性

相关性描述的是⼀个⽂档和查询语句匹配的程度。ES 会对每个匹配查询条件的结果进⾏算分 的\_score。_score评分越高，相关度越高。

对于信息检索工具，衡量其性能有3大指标：

1. **查准率 Precision**：尽可能返回较少的无关文档；

2. **查全率 Recall**：尽可能返回较多的相关文档；

3. **排序 Ranking**：是否能按相关性排序。

前两者更多与分词匹配相关，而后者则与相关性的判断与算分相关。本文将详细介绍相关性系列知识点。

# 2.相似度理论

 Elasticsearch使用布尔模型（Boolean model）查找匹配文档，并用一个名为实用评分函数（practical scoring function）的公式来计算相关度。这个公式借鉴了 词频/逆向（TF/TDF）文档频率和 向量空间模型（vector space model），同时也加入了一些现代的新特性，如协调因子（coordination factor），字段长度归一化（field length normalization），以及词或查询语句权重提升。

[*向量空间模型（Boolean model）*](https://www.elastic.co/guide/cn/elasticsearch/guide/current/scoring-theory.html#scoring-theory) 和[*协调因子*](https://www.elastic.co/guide/cn/elasticsearch/guide/current/practical-scoring-function.html)这里不再介绍，详情请参考ES官网资料。

## 2.1.布尔模型

布尔模型（Boolean Model）只是在查询中使用 AND、 OR和 NOT（与、或和非）这样的条件来查找匹配的文档，以下查询：

```java
full AND text AND search AND (elasticsearch OR lucene)
```

会将所有包括词 `full` 、 `text` 和 `search` ，以及 `elasticsearch` 或 `lucene` 的文档作为结果集。这个过程简单且快速，它将所有可能不匹配的文档排除在外。

## 2.2.词频 TF（Term Frequency）

检索词在文档中出现的频度是多少？出现频率越高，相关性也越高。关于TF的数学表达式，参考ES官网，如下：

```java
tf(t in d) = √frequency
```

词 t 在文档 d 的词频（ tf ）是该词在文档中出现次数的平方根。

**概念理解**：比如说我们检索关键字“es”，“es”在文档A中出现了10次，在文档B中只出现了1次。我们认为文档A与“es”的相关性更高。

## 2.2.1.关闭词频

如果不在意词在某个字段中出现的频次，而只在意是否出现过，则可以在字段映射中禁用词频统计。DSL如下：

```java
<property name="closeTF" desc = "关闭词频TF">
        <![CDATA[{
            "mappings": {
                "properties": {
                  "text": {
                    "type": "keyword",
                    "index_options": "docs"
                  }
                }
            }
        }]]>
    </property>}
```

bboss执行上述模板：

```java
 private Logger logger = LoggerFactory.getLogger(FunctionScoreTest.class);//日志

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
```

将参数 `index_options` 设置为 `docs` 可以禁用词频统计及词频位置，这个映射的字段不会计算词的出现次数，对于短语或近似查询也不可用。要求精确查询的 `not_analyzed` 字符串字段会默认使用该设置。

## 2.2.2.注意事项

目前，Elasticsearch 不支持更改已有字段的相似度算法mapping（映射），只能通过为数据重新建立索引来达到目的。请谨慎设置您的mapping。

## 2.3.逆向⽂档频率 IDF（Inverse Document Frequency）

关于 IDF 的数学表达式，参考ES官网，如下：

```java
idf(t) = 1 + log ( numDocs / (docFreq + 1))
```

词 t 的逆向文档频率（ idf ）是：索引中文档数量（numDocs）除以包含该词的文档数（docFreq），然后求其对数。

**注意: 这里的log是指以e为底的对数,不是以10为底的对数。**

**概念理解：**比如说检索词“学习ES”，按照Ik分词会得到两个Token【学习】【ES】，假设在当前索引下有100个文档包含Token“学习”，只有10个文档包含Token“ES”。那么对于【学习】【ES】这两个Token来说，出现次数较少的 Token【ES】就可以帮助我们快速缩小范围找到我们想要的文档，所以说此时“ES”的权重就比“学习”的权重要高。

## 2.4 字段长度归一值 Norm

字段长度归一值之前也称为**字段长度准则 field-length norm**

字段的长度是多少？字段越短，字段的权重越高。检索词出现在一个内容短的 title 要比同样的词出现在一个内容长的 content 字段权重更大。关于 norm 的数学表达式，参考ES官网，如下：

```java
norm(d) = 1 / √numTerms 
```

 字段长度归一值（ norm ）是字段中词数平方根的倒数。

### 2.4.1. 关闭归一值

字段长度的归一值对全文搜索非常重要，许多其他字段不需要有归一值。无论文档是否包括这个字段，索引中每个文档的每个 `string` 字段都大约占用 1 个 byte 的空间。对于 `not_analyzed` 字符串字段的归一值默认是禁用的，而对于 `analyzed` 字段也可以通过修改字段映射禁用归一值。DSL如下：

```java
 <property name="closeNorms" desc = "关闭字段长度归一值">
        <![CDATA[{
            "mappings": {
              "properties": {
                "text": {
                  "type": "text",
                  "norms":  false
                }
              }

            }
        }]]>
    </property>
```

bboss执行上述模板：

```java
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
```

禁用归一值的字段，长字段和短字段会以相同长度计算评分。

对于有些应用场景如日志，归一值不是很有用，要关心的只是字段是否包含特殊的错误码，字段的长度对结果没有影响，禁用归一值可以节省大量内存空间。

## 2.5.结合使用

词频（term frequency）、逆向文档频率（inverse document frequency）和字段长度归一值（field-length norm）是在索引时计算并存储的。最后将它们结合在一起计算单个词在特定文档中的 权重 。

## 2.6.Lucene中的 评分公式

对于多词查询，Lucene 使用布尔模型（Boolean model） 、 TF/IDF以及向量空间模型（vector space model），然后将它们组合到单个高效的文档集合里并进行评分计算。

评分公式参考自官网：

```java
score(q,d)  =  
            queryNorm(q)  
          · coord(q,d)    
          · ∑ (           
                tf(t in d)   
              · idf(t)²      
              · t.getBoost() 
              · norm(t,d)    
            ) (t in q)    
```

1. score(q,d) 是文档 d 与查询 q 的相关度评分总分。

2. queryNorm(q)是 [*查询归一化* 因子](https://www.elastic.co/guide/cn/elasticsearch/guide/current/practical-scoring-function.html#query-norm) （新）。
3. 
   coord(q,d) 是 协调 因子 （新）。

4. 
   查询 q 中每个词 t 对于文档 d 的权重和。

5. 
   tf(t in d) 是词 t 在文档 d 中的词频 。

6. 
   idf(t) 是词 t 的 逆向文档频率 。

7. 
   t.getBoost() 是查询中使用的 [*boost*](https://www.elastic.co/guide/cn/elasticsearch/guide/current/query-time-boosting.html)（新）。

8. norm(t,d) 是 字段长度归一值 ，与 [*索引时字段层 boost*](https://www.elastic.co/guide/cn/elasticsearch/guide/current/practical-scoring-function.html#index-boost) （如果存在）的和（新）。

本文只讨论score、tf（词频）、idf（逆词频）和norm（字段长度归一值）。

## 2.7.BM25：可更改的相似度

### 2.7.1.BM25公式

关于BM25公式，倒不如将关注点放在BM25所能带来的实际好处上。BM25同样使用词频，逆向文档频率以及长度长归一化，但是每个因素的定义都有细微区别。

![](D:\Code\Bboss\bboss-wiki\src\main\resources\docs\images\bm25_function.png)

<center>BM25公式</center>

**该公式"."的前部分就是 IDF 的算法，后部分就是 TF+Norm 的算法。**

### 2.7.2.TF/IDF与BM25的词频饱和度

TF-IDF算法评分：TF（t）部分的值，随着文档里的某个词出现次数增多，导致整个公式返回的值越大。

BM25就针对这点进行来优化，转换TF（t）的逐步增大，该算法的返回值会趋于一个数值。整体而言BM25就是对TF-IDF算法的改进。

![](D:\Code\Bboss\bboss-wiki\src\main\resources\docs\images\tif-bm25.png)

<center>TF / IDF与BM25的词频饱和度曲线图</center>

值得一提的是，不像TF / IDF，BM25有一个比较好的特性就是它提供了两个可调参数：

**`k1`**

这个参数控制着词频结果在词频饱和度中的上升速度。默认值为 `1.2` 。值越小饱和度变化越快，值越大饱和度变化越慢。

**`b`**

这个参数控制着字段长归一值所起的作用， `0.0` 会禁用归一化， `1.0` 会启用完全归一化。默认值为 `0.75` 。

### 2.7.3指定BM25

```java
<property name="bm25Index" desc = "创建索引，指定字段为BM25评分算法">
        <![CDATA[{
            "mappings": {
            "properties": {
              "title": {
                "type": "text",
                "similarity": "BM25"
              },
              "body": {
                "type": "text",
                "similarity": "boolean"
              }
            }
            }
        }]]>
    </property>
```

es7x版本之前版本similarity默认值为**classic**，在7x移除该值并默认为BM25。详情参考官网[*similarity属性*](https://www.elastic.co/guide/en/elasticsearch/reference/current/similarity.html)。

bboss执行上述模板：

```java
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
```



### 2.7.4.配置BM25

配置相似度算法和配置分词器很相似，自定义相似度算法也可以在创建索引时指定，DSL如下：

```java
<property name="setBM25" desc = "设置BM25的参数">
        <![CDATA[{
            "settings": {
            "similarity": {
              "my_bm25": {
                "type": "BM25",
                "k1":2,
                "b": 0
              }
            }
            },
            "mappings": {
            "properties": {
              "title": {
                "type": "text",
                "similarity": "my_bm25"
              },
              "body": {
                "type": "text",
                "similarity": "BM25"
              }
            }
            }
        }]]>
    </property>
```

bboss执行上述模板：

```java
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
```



# 3.explain：ES执行计划

使用 explain查看搜索相关性分数的计算过程。这非常有助于我们理解ES的相关度计算过程。下面通过示例来学习：

## 3.1.创建索引

创建索引DSL如下：

```java
<property name="createExplainIndex" desc = "创建explain测试索引">
        <![CDATA[{
            "mappings": {
              "properties": {
                "id": {
                  "type": "integer"
                },
                "author": {
                  "type": "keyword"
                },
                "title": {
                  "type": "text",
                  "analyzer": "ik_smart"
                },
                "content": {
                  "type": "text",
                  "analyzer": "ik_max_word",
                  "search_analyzer": "ik_smart"
                },
                "tag": {
                  "type": "keyword"
                },
                "influence": {
                  "type": "integer"
                },
                "createAt": {
                  "type": "date",
                  "format": "yyyy-MM-dd HH:mm:ss"
                }
              }
            }
        }]]>
    </property>
```

bboss执行上述模板：

```java
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
```

## 3.2.导入测试数据

数据导入DSL如下：**一定要保证_bluk DSL的格式,一行索引，一行数据，不能换行，多行。**

```java
  <property name="blukExplainIndex" desc = "导入ExplainI索引数据">
        <![CDATA[
            {"index":{"_index":"explain_index","_id":"1"}}
            {"id":1,"author":"bboss开源引擎","title":"es的相关度","content":"这是关于es的相关度的文章","tag":[1,2,3],"influence":{"gte":10,"lte":12},"createAt":"2020-05-24 10:56:00"}
            {"index":{"_index":"explain_index","_id":"2"}}
            {"id":2,"author":"bboss开源引擎","title":"相关度","content":"这是关于相关度的文章","tag":[2,3,4],"influence":{"gte":12,"lte":15},"createAt":"2020-05-23 10:56:00"}
            {"index":{"_index":"explain_index","_id":"3"}}
            {"id":3,"author":"bboss开源引擎","title":"es","content":"这是关于关于es和编程的必看文章","tag":[2,3,4],"influence":{"gte":12,"lte":15},"createAt":"2020-05-22 10:56:00"}
            {"index":{"_index":"explain_index","_id":"4"}}
            {"id":4,"author":"bboss开源","title":"关注boss，系统学习es","content":"这是关于es的文章，介绍了一点相关度的知识","tag":[1,2,3],"influence":{"gte":10,"lte":15},"createAt":"2020-05-24 10:56:00"}
        ]]>
```

bboss执行上述模板：

```java
 /**
     * 添加article索引数据
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

```

执行完上述两个测试用例，你就用这个创建好的索引和索引数据，进行下一步的测试。

## 3.3.数据导入推荐

使用_bulk接口可以快速插入数据，对于大数据插入Bboss封装了bulkProcessor，支持多线程导入数据，性能非常可观。详情请参考

https://esdoc.bbossgroups.com/#/bulkProcessor

## 3.4 使用explain

先创建一个使用explain查询的DSL：

```java
    <property name="testExplain" desc = "测试explain查看ES查询执行计划">
        <![CDATA[{
             "explain": true,
              "query": {
                "match": {
                  "title": "es的相关度"
                }
              }
        }]]>
    </property>
```

bboss执行上述模板：

```java

```

根据explain分析结果，我们简单分析下文档1的相关性算分过程，去理解ES的相关性算分：

上述查询DSL中： **"title": "es的相关度"**这个查询条件，根据我们采用的是**ik_smart**分词器，会被分词为**es**、**的**、**相关**、**度**四个词元去查询，四个词元的总分就是该查询条件的总分。我们以**es**词元来讲解explain评分结果。

### 3.4.1.文档1的explain结果

```java
"value" : 2.5933092,
"description" : "sum of:",
"details" : [...]
```

### 3.4.2.词元得分explain结果

**es**词元得分分析：

```java
"value" : 0.31387398,
"description" : "score(freq=1.0), product of:",
"details" : [...]
```

1. idf得分

```java
"value" : 0.35667494,
"description" : "idf, computed as log(1 + (N - n + 0.5) / (n + 0.5)) from:",
"details" : [
    {
        "value" : 3,
        "description" : "n, number of documents containing term",
        "details" : [ ]
    },
    {
        "value" : 4,
        "description" : "N, total number of documents with field",
        "details" : [ ]
    }
]
```

根据idf公式，结合details信息得出：n（docFreq 包含该单词的文档数）= 3，N（numDocs文档总数） = 4，底数为e。计算出 
$$
_score(idf)= log(1+(4-3+0.5)/3+0.5)=ln(1.42)=0.35667494
$$
这就是**es**词元的idf得分

2. tf得分

```java
"value" : 0.4,
"description" : "tf, computed as freq / (freq + k1 * (1 - b + b * dl / avgdl)) from:",
"details" : [
    {
        "value" : 1.0,
        "description" : "freq, occurrences of term within document",
        "details" : [ ]
    },
    {
        "value" : 1.2,
        "description" : "k1, term saturation parameter",
        "details" : [ ] 
    },
    {
        "value" : 0.75,
        "description" : "b, length normalization parameter",
        "details" : [ ]
    },
    {
        "value" : 4.0,
        "description" : "dl, length of field",
        "details" : [ ]
    },
    {
        "value" : 3.0,
        "description" : "avgdl, average length of field",
        "details" : [ ]
    }
]
```

根据tf公式，结合details的信息，计算出
$$
_score(tf)=1/(1+1.2x(1-0.75+0.75x4/3 )=0.4
$$
这就是**es**词元的tf得分



_score（BM25）=*i*df *tfNorm =\* 0.3566749440 \* 0.88 = 0.3138739947

同理得到 BM25（的）= 1.059496，BM25（相关）= 0.6099695，BM25（度）= 0.6099695；

根据"description": "**sum** of:",当检索【es的相关度】，**文档1的_score = BM（es）+ BM25（的）+ BM25（相关）+ BM25（度）****= 2.5933092**

# 4.相关度控制

## 4.1.boost  参数【常用】

## 4.2.查询方式改变

### 4.2.1.constant_score查询

### 4.2.2.function_score查询

### 4.2.3.dis_max query

### 4.2.4.boosting query【常用】

## 4.3.rescore 结果集重新评分

## 4.4.更改BM25 参数 k1 和 b 的值

# 5.被破坏的相关度

## 5.1.现象示例

## 5.2.两种方式解决

### 5.3.该现象不用深究

# 6.相关度控制最后要做的事情

1. 理解评分过程是非常重要的，这样就可以根据具体的业务对评分结果进行调试、调节、减弱和定制。

2. 本文介绍的4种相关度控制方案，建议结合实践，根据自己的业务需求，多动手调试练习。

3. 最相关 这个概念是一个难以触及的模糊目标，通常不同人对文档排序又有着不同的想法，这很容易使人陷入持续反复调整而没有明显进展的怪圈。**强烈建议不要去追求最相关，而要监控测量搜索结果。**

4. **评价搜索结果与用户之间相关程度的指标。**如果查询能返回高相关的文档，用户会选择前五中的一个，得到想要的结果，然后离开。不相关的结果会让用户来回点击并尝试新的搜索条件。

5. 要想物尽其用并将搜索结果提高到 *极高的* 水平，唯一途径就是需要具备能评价度量用户行为的强大能力。