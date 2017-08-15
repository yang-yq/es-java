import java.io.*;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import dao.Doc;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import javax.mail.MessagingException;


public class EsClient {
    public static TransportClient getClient() {
        TransportClient client = null;
        try {
            //设置集群名称
            Settings settings = Settings.builder().put("cluster.name", "my-elasticsearch").build();
            //创建client
            client = new PreBuiltTransportClient(settings)
                    .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("master"), 9300))
                    .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("slave1"), 9300))
                    .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("slave2"), 9300));
        } catch (Exception  e) {
            e.printStackTrace();
        }
        return client;
    }

    public static void addEsIndex(String index) {
        IndicesExistsResponse indicesExist =  getClient().admin().indices().prepareExists(index).execute().actionGet();
        if (indicesExist.isExists()){
            deleteEsIndex(index);
        }
        //指定类型为mht
        String mappinginfo  = "{\"mappings\":{\"mht\":{\"properties\":{\"title\":{\"type\":\"string\",\"analyzer\":\"ik_max_word\"},\"content\":{\"type\":\"string\",\"analyzer\":\"ik_max_word\"}}}}}";

        CreateIndexResponse createIndex = getClient().admin().indices().prepareCreate(index).setSource(mappinginfo).execute().actionGet();
        if (createIndex.isAcknowledged()){
            System.out.println("已成功建立索引");
        }else {
            System.out.println("建立索引失败");
        }
    }

    //待定
    private static void deleteEsIndex(String index) {
        DeleteIndexResponse deleteIndex = getClient().admin().indices().prepareDelete(index).execute().actionGet();
        if (deleteIndex.isAcknowledged()){
            System.out.println("成功删除索引");
        }else{
            System.out.println("删除索引失败");
        }
    }

    public static void addEsData(String indexname,String type,Doc Docline){
        HashMap<String, Object> hashMapDoc = new HashMap<String, Object>();
        hashMapDoc.put("title",Docline.getTitle());
        hashMapDoc.put("content",Docline.getContent());
        IndexResponse response = getClient().prepareIndex(indexname, type)
                .setSource(hashMapDoc).execute().actionGet();
//        System.out.println("添加的索引id为"+ response.getId());
    }

    public static Map<String, Object> search(String key,String index,String type){
        QueryBuilder matchQuery = QueryBuilders.multiMatchQuery(key, "title", "content");
        HighlightBuilder hiBuilder=new HighlightBuilder();
        hiBuilder.preTags("<h2>");
        hiBuilder.postTags("</h2>");
        hiBuilder.field("title").field("content");
        SearchResponse response = getClient().prepareSearch(index)
                .setTypes(type)
                .setQuery(matchQuery)
                .highlighter(hiBuilder)
                .execute().actionGet();
        SearchHits searchHits = response.getHits();
        System.out.println("共搜到:"+searchHits.getTotalHits()+"条结果!");
        long total = searchHits.getTotalHits();
        Map<String, Object> map = new HashMap<String,Object>();
        SearchHit[] hits2 = searchHits.getHits();
        map.put("count", total);
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (SearchHit searchHit : hits2) {
            Map<String, HighlightField> highlightFields = searchHit.getHighlightFields();
            HighlightField highlightField = highlightFields.get("title");
            Map<String, Object> source = searchHit.getSource();
            if(highlightField!=null){
                Text[] fragments = highlightField.fragments();
                String name = "";
                for (Text text : fragments) {
                    name+=text;
                }
                source.put("title", name);
            }
            HighlightField highlightField2 = highlightFields.get("content");
            if(highlightField2!=null){
                Text[] fragments = highlightField2.fragments();
                String describe = "";
                for (Text text : fragments) {
                    describe+=text;
                }
                source.put("content", describe);
            }
            list.add(source);
        }
        map.put("dataList", list);
        return map;
    }

    //index:testindex type:mht
    public static void main(String[] args) throws IOException, MessagingException, InterruptedException {
        String filepath = "D:\\data_mht";
        ProcessMht.getFile(filepath);
        System.out.println(ProcessMht.arrayList.size());

        addEsIndex("testindex");
        for (Doc Docline : ProcessMht.arrayList){
            addEsData("testindex","mht",Docline);
        }
        Thread.sleep(10000);

        Map<String, Object> searchResult = search("西藏", "testindex","mht");
        List<Map<String, Object>> list = (List<Map<String, Object>>) searchResult.get("dataList");
        for(int i=0;i<list.size();i++)
        {
            System.out.println("第"+(i+1)+"条结果");
            System.out.println(list.get(i).get("title"));
            System.out.println(list.get(i).get("content"));
        }
    }
}
