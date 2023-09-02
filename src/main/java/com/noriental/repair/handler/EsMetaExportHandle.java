package com.noriental.repair.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.carrotsearch.hppc.cursors.ObjectCursor;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.client.GetAliasesResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetMappingsRequest;
import org.elasticsearch.client.indices.GetMappingsResponse;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.settings.Settings;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/***
 * *   ____  ___________  ___________           ________   ____  __.  _____ _____.___.
 * *   \   \/  /\______ \ \_   _____/           \_____  \ |    |/ _| /  _  \\__  |   |
 * *    \     /  |    |  \ |    __)     ______   /   |   \|      <  /  /_\  \/   |   |
 * *    /     \  |    `   \|     \     /_____/  /    |    \    |  \/    |    \____   |
 * *   /___/\  \/_______  /\___  /              \_______  /____|__ \____|__  / ______|
 * *    	 \_/        \/     \/                       \/        \/       \/\/
 * *
 * *   功能描述：把一个集群的索引结构，复制到另一个集群上。
 * *   只复制格式，不复制数据
 * *   具体：名称，别名，setting，mapping
 * *   @DATE    2021-12-02
 * *   @AUTHOR  lijianghu
 ***/
public class EsMetaExportHandle {
    /**
     * //1 获取索引名称，忽略 .开头，解析setting,mapping,alias，删除无用配置（index.creation_date等）
     * //2 组装put语句
     * //3 执行DSL脚本，创建索引
     *
     * @param fromEndPoint
     * @param shards
     * @param replicas
     * @param ignoreReg
     * @throws Exception
     */
    public static void printIndexMappings(String fromEndPoint, int shards, int replicas, String ignoreReg) {

        Set<String> successIndexs = new LinkedHashSet<>();
        Set<String> errorIndexs = new LinkedHashSet<>();
        Set<String> ignoreIndexs = new LinkedHashSet<>();
        Set<String> existsIndexs = new LinkedHashSet<>();
        RestHighLevelClient eSclientRead = null;
        try {
            eSclientRead = new EsConfig().buildHighLevelClient(fromEndPoint);
            //1
            Map<String, JSONObject> mappingDsls = new HashMap<>(); // key indexName ,value smappings
            Map<String, JSONObject> settingsDsls = new HashMap<>(); // key indexName ,value  settings
            Map<String, JSONObject> alias = new HashMap<>(); // key indexName ,value  alias
            // alias
            /**
             *  "aliases" : {
             *       "name1" : { },
             *       "name2" : { }
             *     }
             */
            GetMappingsRequest getMappingsRequest = new GetMappingsRequest();
            GetAliasesResponse aliasesResponse = eSclientRead.indices().getAlias(new GetAliasesRequest(), RequestOptions.DEFAULT);
            Map<String, Set<AliasMetaData>> aliases = aliasesResponse.getAliases();
            for (String indexName : aliases.keySet()) {
                if (indexName.contains(ignoreReg)) {
                    continue;
                }
                Set<AliasMetaData> aliMap = aliases.get(indexName);
                JSONObject aliasJson = new JSONObject();
                for (AliasMetaData data : aliMap) {
                    String aliasStr = data.getAlias();
                    aliasJson.put(aliasStr, Collections.EMPTY_MAP);
                }
                JSONObject aliDetail = new JSONObject();
                aliDetail.put("aliases", aliasJson);
                alias.put(indexName, aliDetail);
            }
            //mappings
            /**
             * {"properties": {
             *       "id": {
             *         "type": "long"
             *       },
             *       "name": {
             *         "type": "integer"
             *       }
             *    }
             *  }
             */
            GetMappingsResponse response = eSclientRead.indices().getMapping(getMappingsRequest, RequestOptions.DEFAULT);
            Map<String, MappingMetaData> mappings = response.mappings();
            for (String indexName : mappings.keySet()) {
                if (indexName.contains(ignoreReg)) {
                    continue;
                }
                String mappingsDetailStr = mappings.get(indexName).source().toString();
                JSONObject mappingsDetailObj = JSON.parseObject(mappingsDetailStr);
                JSONObject mapping = new JSONObject();
                mapping.put("mappings", mappingsDetailObj);
                mappingDsls.put(indexName, mapping);
            }
            //settings
            /**
             *  { "settings": {
             *     "index.max_result_window": "100000000",
             *     "index.analysis.tokenizer.my_tokenizer.min_gram": "1",
             *     "index.analysis.tokenizer.my_tokenizer.type": "ngram",
             *     "index.number_of_replicas": 0,
             *     "index.analysis.tokenizer.my_tokenizer.max_gram": "2",
             *     "index.analysis.analyzer.ngram_1_2.tokenizer": "my_tokenizer",
             *     "index.analysis.analyzer.ik.tokenizer": "ik_max_word",
             *     "index.analysis.analyzer.ngram_1_2.type": "custom",
             *     "index.number_of_shards": 2
             *   }
             *  }
             */
            GetSettingsRequest getSettingsRequest = new GetSettingsRequest();
            GetSettingsResponse settingsResponse = eSclientRead.indices().getSettings(getSettingsRequest, RequestOptions.DEFAULT);
            ImmutableOpenMap<String, Settings> settingMap = settingsResponse.getIndexToSettings();
            for (ObjectCursor<String> indexName : settingMap.keys()) {
                if (indexName.value.contains(ignoreReg)) {
                    ignoreIndexs.add(indexName.value);
                    continue;
                }
                Settings settings = settingMap.get(indexName.value);
                String settingStr = settings.toString();
                JSONObject jsonObject = JSON.parseObject(settingStr);
                jsonObject.remove("index.creation_date");
                jsonObject.remove("index.provided_name");
                jsonObject.remove("index.uuid");
                jsonObject.remove("index.version.created");
                jsonObject.put("index.number_of_shards", shards);
                jsonObject.put("index.number_of_replicas", replicas);
                JSONObject setting = new JSONObject();
                setting.put("settings", jsonObject);
                settingsDsls.put(indexName.value, setting);
            }
            //2
            for (String indexName : mappingDsls.keySet()) {
                if (indexName.contains(ignoreReg)) {
                    continue;
                }
                JSONObject settingJSON = settingsDsls.get(indexName);
                JSONObject mappingJSON = mappingDsls.get(indexName);
                JSONObject aliasJSON = alias.get(indexName);
                JSONObject esJsonContent = new JSONObject();
                esJsonContent.putAll(settingJSON);
                esJsonContent.putAll(mappingJSON);
                if (aliasJSON != null) { // 可能没别名
                    esJsonContent.putAll(aliasJSON);
                }
                System.out.println("PUT " + indexName + "\n" + esJsonContent.toJSONString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (eSclientRead != null) {
                try {
                    eSclientRead.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println("---------------------处理结果---------------------");
        System.out.println(">>>>>>>>【索引已忽略】：↓↓↓\n"+JSON.toJSONString(ignoreIndexs).replace(",","\n"));
    }


    public static void execute() {
        String readEndPoint = "127.0.0.1:9200";
        final int shards = 6;
        final int replicas = 0;
        final String ignoreReg = ".";
        printIndexMappings(readEndPoint, shards, replicas, ignoreReg);
    }
}
